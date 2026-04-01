package handler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.charset.MalformedInputException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Searcher {

    public String search(String query) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT f.file_name, f.file_path, f.size_bytes, f.mime_type FROM files f ");
        List<Object> params = new ArrayList<>();

        String[] tokens = query.split(" ");
        List<String> textKeywords = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder("WHERE 1=1 ");

        for (String token : tokens) {
            if (token.startsWith("ext:")) {
                whereClause.append(" AND f.extension = ?");
                params.add(token.substring(4).toLowerCase());
            } else if (token.startsWith("mime:")) {
                whereClause.append(" AND f.mime_type LIKE ?");
                params.add("%" + token.substring(5).toLowerCase() + "%");
            } else if (token.startsWith("owner:")) {
                whereClause.append(" AND f.owner = ?");
                params.add(token.substring(6));
            } else {
                textKeywords.add(token.toLowerCase());
            }
        }

        String searchWord = null;
        if (!textKeywords.isEmpty()) {
            searchWord = textKeywords.get(0);
            sql.append("JOIN postings p ON f.id = p.file_id ");
            sql.append("JOIN words w ON p.word_id = w.id ");
            whereClause.append(" AND w.term = ?");
            params.add(searchWord);
        }

        sql.append(whereClause);
        sql.append(" LIMIT 50");

        StringBuilder resultsOutput = new StringBuilder();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            resultsOutput.append("<html><head><meta charset='UTF-8'></head>");
            resultsOutput.append("<body style='font-family: Arial, sans-serif; padding: 15px; color: #333;'>");
            resultsOutput.append("<h2 style='margin-top: 0; color: #2c3e50;'>Search Results</h2>");

            boolean found = false;

            while (rs.next()) {
                found = true;
                long sizeKB = rs.getLong("size_bytes") / 1024;
                String filePath = rs.getString("file_path");
                String fileName = rs.getString("file_name");

                resultsOutput.append("<div style='margin-bottom: 20px; padding: 10px; border: 1px solid #e0e0e0; border-radius: 5px; background-color: #f9f9f9;'>");

                resultsOutput.append("<div style='font-size: 16px; font-weight: bold; color: #0056b3;'>");
                resultsOutput.append("&#x1F4C4; ").append(fileName);
                resultsOutput.append(" <span style='font-size: 12px; color: #888; font-weight: normal;'>(").append(sizeKB).append(" KB)</span>");
                resultsOutput.append("</div>");

                resultsOutput.append("<div style='font-size: 12px; color: #28a745; margin-bottom: 8px; word-wrap: break-word;'>");
                resultsOutput.append(filePath);
                resultsOutput.append("</div>");

                if (searchWord != null) {
                    String snippet = extractSnippet(filePath, searchWord);
                    if (!snippet.isEmpty()) {
                        resultsOutput.append("<div style='font-family: monospace; font-size: 13px; color: #555; background-color: #fff; padding: 8px; border-left: 3px solid #007bff; overflow-wrap: break-word;'>");
                        resultsOutput.append(snippet);
                        resultsOutput.append("</div>");
                    }
                }

                resultsOutput.append("</div>");
            }

            if (!found) {
                resultsOutput.append("<i style='color: #666;'>No matching files found.</i>");
            }
            resultsOutput.append("</body></html>");

        } catch (SQLException e) {
            return "<html><body><p style='color: red;'>Search query failed: " + e.getMessage() + "</p></body></html>";
        }

        return resultsOutput.toString();
    }

    private String extractSnippet(String filePath, String searchWord) {
        try {
            String content;
            try {
                content = Files.readString(Path.of(filePath));
            } catch (MalformedInputException e) {
                content = Files.readString(Path.of(filePath), StandardCharsets.ISO_8859_1);
            }

            String lowerContent = content.toLowerCase();
            int index = lowerContent.indexOf(searchWord);

            if (index != -1) {
                int start = Math.max(0, index - 60);
                int end = Math.min(content.length(), index + searchWord.length() + 60);

                String rawSnippet = content.substring(start, end);

                rawSnippet = rawSnippet.replaceAll("[\\r\\n\\t]+", " ");

                rawSnippet = rawSnippet.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

                String highlightedSnippet = rawSnippet.replaceAll(
                        "(?i)(" + Pattern.quote(searchWord) + ")",
                        "<b style='color: #d93025; background-color: #fce8e6; padding: 0 2px;'>$1</b>"
                );

                return "... " + highlightedSnippet + " ...";
            }
        } catch (Exception e) {
        }
        return "";
    }
}
