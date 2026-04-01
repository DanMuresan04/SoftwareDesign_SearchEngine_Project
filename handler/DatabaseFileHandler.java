package handler;

import crawler.ContentParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class DatabaseFileHandler implements FileHandler {

    private static Connection conn;
    private ContentParser parser;

    private static PreparedStatement pstmtFileUpsert;
    private static PreparedStatement pstmtGetFileId;
    private static PreparedStatement pstmtClearPostings;
    private static PreparedStatement pstmtWordInsert;
    private static PreparedStatement pstmtGetWordId;
    private static PreparedStatement pstmtPostingInsert;
    private static PreparedStatement pstmtCheckModified;

    private static int batchCount = 0;

    public DatabaseFileHandler() {
        parser = new ContentParser();
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String sqlFile = "INSERT INTO files(file_name, file_path, extension, size_bytes, creation_time, last_modified, last_accessed, mime_type, owner) " +
                    "VALUES(?,?,?,?,?,?,?,?,?) " +
                    "ON CONFLICT(file_path) DO UPDATE SET " +
                    "size_bytes = excluded.size_bytes, " +
                    "last_modified = excluded.last_modified, " +
                    "last_accessed = excluded.last_accessed, " +
                    "mime_type = excluded.mime_type, " +
                    "owner = excluded.owner " +
                    "WHERE files.last_modified < excluded.last_modified;";
            pstmtFileUpsert = conn.prepareStatement(sqlFile);

            pstmtGetFileId = conn.prepareStatement("SELECT id FROM files WHERE file_path = ?");
            pstmtClearPostings = conn.prepareStatement("DELETE FROM postings WHERE file_id = ?");
            pstmtWordInsert = conn.prepareStatement("INSERT OR IGNORE INTO words (term) VALUES (?)");
            pstmtGetWordId = conn.prepareStatement("SELECT id FROM words WHERE term = ?");
            pstmtPostingInsert = conn.prepareStatement("INSERT OR IGNORE INTO postings (word_id, file_id) VALUES (?, ?)");
            pstmtCheckModified = conn.prepareStatement("SELECT last_modified FROM files WHERE file_path = ?");

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    @Override
    public void handle(Path filePath, BasicFileAttributes attrs) {
        if (pstmtFileUpsert == null) return;

        try {
            String fileName = filePath.getFileName().toString();
            String fullPath = filePath.toAbsolutePath().toString();
            String ext = "";

            int i = fileName.lastIndexOf('.');
            if (i > 0) ext = fileName.substring(i + 1);

            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) mimeType = "unknown";

            String owner = "unknown";
            try {
                owner = Files.getOwner(filePath).getName();
            } catch (Exception e) {}

            pstmtCheckModified.setString(1, fullPath);
            try (ResultSet rsCheck = pstmtCheckModified.executeQuery()) {
                if (rsCheck.next()) {
                    long dbLastModified = rsCheck.getLong("last_modified");
                    if (dbLastModified == attrs.lastModifiedTime().toMillis()) {
                        return; // SKIP EVERYTHING!
                    }
                }
            }

            pstmtFileUpsert.setString(1, fileName);
            pstmtFileUpsert.setString(2, fullPath);
            pstmtFileUpsert.setString(3, ext);
            pstmtFileUpsert.setLong(4, attrs.size());
            pstmtFileUpsert.setLong(5, attrs.creationTime().toMillis());
            pstmtFileUpsert.setLong(6, attrs.lastModifiedTime().toMillis());
            pstmtFileUpsert.setLong(7, attrs.lastAccessTime().toMillis());
            pstmtFileUpsert.setString(8, mimeType);
            pstmtFileUpsert.setString(9, owner);

            pstmtFileUpsert.executeUpdate();

            int fileId;
            pstmtGetFileId.setString(1, fullPath);
            try (ResultSet rsFile = pstmtGetFileId.executeQuery()) {
                if (!rsFile.next()) return;
                fileId = rsFile.getInt("id");
            }

            pstmtClearPostings.setInt(1, fileId);
            pstmtClearPostings.executeUpdate();

            Set<String> words = parser.extractWords(filePath, mimeType);

            for (String word : words) {
                pstmtWordInsert.setString(1, word);
                pstmtWordInsert.executeUpdate();

                pstmtGetWordId.setString(1, word);
                try (ResultSet rsWord = pstmtGetWordId.executeQuery()) {
                    if (rsWord.next()) {
                        int wordId = rsWord.getInt("id");

                        pstmtPostingInsert.setInt(1, wordId);
                        pstmtPostingInsert.setInt(2, fileId);
                        pstmtPostingInsert.addBatch();
                        batchCount++;
                    }
                }
            }

            if (batchCount >= 10000) {
                pstmtPostingInsert.executeBatch();
                conn.commit();
                batchCount = 0;
            }

        } catch (SQLException e) {
            System.err.println("SQL error processing file: " + filePath.getFileName() + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("IO Error: " + e.getMessage());
        }
    }

    public static void finish() {
        try {
            if ( batchCount > 0) {
                pstmtPostingInsert.executeBatch();
            }
            conn.commit();

            pstmtFileUpsert.close();
            pstmtGetFileId.close();
            pstmtClearPostings.close();
            pstmtWordInsert.close();
            pstmtGetWordId.close();
            pstmtPostingInsert.close();
            pstmtCheckModified.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println("Failed to commit final batch: " + e.getMessage());
        }
    }
}
