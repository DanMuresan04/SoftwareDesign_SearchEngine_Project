package handler;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseFileHandler implements FileHandler {

    @Override
    public void handle(Path filePath) {
        String sql = "INSERT OR IGNORE INTO files(file_name, file_path, extension, last_modified) VALUES(?,?,?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String fileName = filePath.getFileName().toString();
            String fullPath = filePath.toAbsolutePath().toString();
            String ext = "";

            int i = fileName.lastIndexOf('.');
            if (i > 0) ext = fileName.substring(i + 1);

            pstmt.setString(1, fileName);
            pstmt.setString(2, fullPath);
            pstmt.setString(3, ext);
            pstmt.setLong(4, filePath.toFile().lastModified());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("SQL Error saving file: " + filePath + " - " + e.getMessage());
        }
    }
}