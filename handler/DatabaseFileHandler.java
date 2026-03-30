package handler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseFileHandler implements FileHandler {

    private static Connection conn;
    private static PreparedStatement pstmt;
    private int batchCount = 0;

    public DatabaseFileHandler() {
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String sql = "INSERT INTO files(file_name, file_path, extension, size_bytes, creation_time, last_modified, last_accessed, mime_type, owner) " +
                    "VALUES(?,?,?,?,?,?,?,?,?) " +
                    "ON CONFLICT(file_path) DO UPDATE SET " +
                    "size_bytes = excluded.size_bytes, " +
                    "last_modified = excluded.last_modified, " +
                    "last_accessed = excluded.last_accessed, " +
                    "mime_type = excluded.mime_type, " +
                    "owner = excluded.owner " +
                    "WHERE files.last_modified < excluded.last_modified;";

            pstmt = conn.prepareStatement(sql);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    @Override
    public void handle(Path filePath, BasicFileAttributes attrs) {
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
            } catch (Exception e) {
            }

            pstmt.setString(1, fileName);
            pstmt.setString(2, fullPath);
            pstmt.setString(3, ext);
            pstmt.setLong(4, attrs.size());
            pstmt.setLong(5, attrs.creationTime().toMillis());
            pstmt.setLong(6, attrs.lastModifiedTime().toMillis());
            pstmt.setLong(7, attrs.lastAccessTime().toMillis()); 
            pstmt.setString(8, mimeType);                        
            pstmt.setString(9, owner);                           

            pstmt.addBatch();
            batchCount++;

            if (batchCount % 10000 == 0) {
                pstmt.executeBatch();
            }

        } catch (SQLException e) {
            System.err.println("Batch error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("IO Error reading file metadata: " + e.getMessage());
        }
    }

    public static void finish() {
        try {
            pstmt.executeBatch();
            conn.commit();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println("Failed to commit final batch: " + e.getMessage());
        }
    }
}
