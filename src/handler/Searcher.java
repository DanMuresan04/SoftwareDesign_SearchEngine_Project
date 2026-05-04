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
        StringBuilder sql = new StringBuilder("SELECT f.file_name, f.file_path, f.size_bytes, f.mime_type FROM files f ");
        List<Object> params = new ArrayList<>();

        List<String> pathPatterns = new ArrayList<>();
        List<String> contentKeywords = new ArrayList<>();
        List<String> extPatterns = new ArrayList<>();
        List<String> mimePatterns = new ArrayList<>();
        List<String> ownerPatterns = new ArrayList<>();

        String currentCategory = "content";
        String[] tokens = query.split("\\s+");
        for (String token : tokens) {
            String val = token;
            if (token.startsWith("path:")) {
                currentCategory = "path";
                val = token.substring(5);
            } else if (token.startsWith("content:")) {
                currentCategory = "content";
                val = token.substring(8);
            } else if (token.startsWith("ext:")) {
                currentCategory = "ext";
                val = token.substring(4);
            } else if (token.startsWith("mime:")) {
                currentCategory = "mime";
                val = token.substring(5);
            } else if (token.startsWith("owner:")) {
                currentCategory = "owner";
                val = token.substring(6);
            }

            if (!val.isEmpty()) {
                switch (currentCategory) {
                    case "path": pathPatterns.add(val); break;
                    case "content": contentKeywords.add(val.toLowerCase()); break;
                    case "ext": extPatterns.add(val.toLowerCase()); break;
                    case "mime": mimePatterns.add(val.toLowerCase()); break;
                    case "owner": ownerPatterns.add(val); break;
                }
            }
        }

        if (!contentKeywords.isEmpty()) {
            sql.append("JOIN postings p ON f.id = p.file_id ");
            sql.append("JOIN words w ON p.word_id = w.id ");
        }

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        for (String p : pathPatterns) {
            whereClause.append(" AND f.file_path LIKE ?");
            params.add("%" + p + "%");
        }
        for (String e : extPatterns) {
            whereClause.append(" AND f.extension = ?");
            params.add(e);
        }
        for (String m : mimePatterns) {
            whereClause.append(" AND f.mime_type LIKE ?");
            params.add("%" + m + "%");
        }
        for (String o : ownerPatterns) {
            whereClause.append(" AND f.owner = ?");
            params.add(o);
        }

        if (!contentKeywords.isEmpty()) {
            whereClause.append(" AND w.term IN (");
            for (int i = 0; i < contentKeywords.size(); i++) {
                whereClause.append("?");
                if (i < contentKeywords.size() - 1) whereClause.append(",");
                params.add(contentKeywords.get(i));
            }
            whereClause.append(")");
        }

        sql.append(whereClause);

        if (!contentKeywords.isEmpty()) {
            sql.append(" GROUP BY f.id HAVING COUNT(DISTINCT w.term) = ?");
            params.add(contentKeywords.size());
        }

        sql.append(" LIMIT 50");

        StringBuilder resultsOutput = new StringBuilder();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            resultsOutput.append("<html><head><meta charset='UTF-8'></head>");
            resultsOutput.append("<body style='font-family: Arial, sans-serif; padding: 15px; color: darkslategray;'>");
            resultsOutput.append("<h2 style='margin-top: 0; color: blue;'>Search Results</h2>");

            boolean found = false;

            while (rs.next()) {
                found = true;
                long sizeKB = rs.getLong("size_bytes") / 1024;
                String filePath = rs.getString("file_path");
                String fileName = rs.getString("file_name");

                resultsOutput.append("<div style='margin-bottom: 20px; padding: 10px; border: 1px solid lightgray; border-radius: 5px; background-color: lightsmoke;'>");
                resultsOutput.append("<div style='font-size: 16px; font-weight: bold; color: #royalblue;'>");
                resultsOutput.append(fileName);
                resultsOutput.append(" <span style='font-size: 12px; color: gray; font-weight: normal;'>(").append(sizeKB).append(" KB)</span>");
                resultsOutput.append("</div>");

                resultsOutput.append("<div style='font-size: 12px; color: forestgreen; margin-bottom: 8px; word-wrap: break-word;'>");
                resultsOutput.append(filePath);
                resultsOutput.append("</div>");

                if (!contentKeywords.isEmpty()) {
                    String snippet = extractSnippet(filePath, contentKeywords);
                    if (!snippet.isEmpty()) {
                        resultsOutput.append("<div style='font-family: monospace; font-size: 13px; color: darkslategray; background-color: white; padding: 8px; border-left: 3px solid blue; overflow-wrap: break-word;'>");
                        resultsOutput.append(snippet);
                        resultsOutput.append("</div>");
                    }
                }

                resultsOutput.append("</div>");
            }

            if (!found) {
                resultsOutput.append("<i style='color: gray;'>No matching files found.</i>");
            }
            resultsOutput.append("</body></html>");

        } catch (SQLException e) {
            return "<html><body><p style='color: red;'>Search query failed: " + e.getMessage() + "</p></body></html>";
        }

        return resultsOutput.toString();
    }

    private String extractSnippet(String filePath, List<String> keywords) {
        try {
            String content;
            try {
                content = Files.readString(Path.of(filePath));
            } catch (MalformedInputException e) {
                content = Files.readString(Path.of(filePath), StandardCharsets.ISO_8859_1);
            }

            String lowerContent = content.toLowerCase();
            int firstIndex = -1;
            String foundWord = "";

            for (String word : keywords) {
                int idx = lowerContent.indexOf(word);
                if (idx != -1 && (firstIndex == -1 || idx < firstIndex)) {
                    firstIndex = idx;
                    foundWord = word;
                }
            }

            if (firstIndex != -1) {
                int start = Math.max(0, firstIndex - 60);
                int end = Math.min(content.length(), firstIndex + foundWord.length() + 60);

                String rawSnippet = content.substring(start, end);
                rawSnippet = rawSnippet.replaceAll("[\\r\\n\\t]+", " ");
                rawSnippet = rawSnippet.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

                String highlightedSnippet = rawSnippet;
                for (String word : keywords) {
                    highlightedSnippet = highlightedSnippet.replaceAll(
                            "(?i)(" + Pattern.quote(word) + ")",
                            "<b style='color: crimson; background-color: mistyrose; padding: 0 2px;'>$1</b>"
                    );
                }

                return "... " + highlightedSnippet + " ...";
            }
        } catch (Exception e) {
        }
        return "";
    }
}