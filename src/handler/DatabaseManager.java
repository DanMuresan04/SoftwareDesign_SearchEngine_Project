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
        // Nuke the old table so the new columns are guaranteed to be created
        String dropSql = "DROP TABLE IF EXISTS files;";

        String createSql = "CREATE TABLE IF NOT EXISTS files (" +
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

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(dropSql);   // Run the drop first
            stmt.execute(createSql); // Then build the new schema
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema: " + e.getMessage());
        }
    }
}