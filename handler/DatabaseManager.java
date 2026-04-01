package handler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:search_engine.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeSchema() {

        String createFiles = "CREATE TABLE IF NOT EXISTS files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name TEXT NOT NULL, " +
                "file_path TEXT NOT NULL UNIQUE, " +
                "extension TEXT, " +
                "size_bytes INTEGER, " +
                "creation_time INTEGER, " +
                "last_modified INTEGER NOT NULL, " +
                "last_accessed INTEGER, " +
                "mime_type TEXT, " +
                "owner TEXT" +
                ");";

        String createWords = "CREATE TABLE IF NOT EXISTS words (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "term TEXT NOT NULL UNIQUE" +
                ");";

        String createPostings = "CREATE TABLE IF NOT EXISTS postings (" +
                "word_id INTEGER, " +
                "file_id INTEGER, " +
                "FOREIGN KEY(word_id) REFERENCES words(id), " +
                "FOREIGN KEY(file_id) REFERENCES files(id), " +
                "PRIMARY KEY(word_id, file_id)" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createFiles);
            stmt.execute(createWords);
            stmt.execute(createPostings);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema: " + e.getMessage());
        }
    }
}
