package handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Searcher {

    public void searchByName(String keyword){
        String sql = "SELECT file_name, file_path FROM files WHERE file_name LIKE ? LIMIT 50";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n=== Search Results for '" + keyword + "' ===");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("   File: " + rs.getString("file_name") + " at: " + rs.getString("file_path"));
            }

            if (!found) {
                System.out.println("No matching files found.");
            }

        } catch (SQLException e) {
            System.err.println("Search query failed: " + e.getMessage());
        }
    }
}