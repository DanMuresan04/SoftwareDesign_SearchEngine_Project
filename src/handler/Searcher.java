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

    public String search(String query, String sortStrategy) {
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

        switch (sortStrategy) {
            case "Date Modified":
                sql.append(" ORDER BY f.last_modified DESC");
                break;
            case "Alphabetical":
                sql.append(" ORDER BY f.file_name ASC");
                break;
            case "Size":
                sql.append(" ORDER BY f.size_bytes DESC");
                break;
            default:
                sql.append(" ORDER BY f.rank_score DESC");
                break;
        }

        sql.append(" LIMIT 50");

        StringBuilder resultsOutput = new StringBuilder();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            resultsOutput.append("<html><head><meta charset='UTF-8'>");
            resultsOutput.append("<style>");
            resultsOutput.append("body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8f9fa; color: #333; line-height: 1.6; padding: 20px; }");
            resultsOutput.append(".result-card { background: white; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); border-left: 4px solid #4a90e2; transition: transform 0.2s, box-shadow 0.2s; }");
            resultsOutput.append(".result-card:hover { transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.1); border-left-color: #357abd; }");
            resultsOutput.append(".file-name { font-size: 18px; font-weight: 600; color: #2c3e50; margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between; }");
            resultsOutput.append(".file-size { font-size: 13px; color: #7f8c8d; font-weight: 400; }");
            resultsOutput.append(".file-path { font-size: 13px; color: #27ae60; margin-bottom: 12px; word-break: break-all; }");
            resultsOutput.append(".snippet { font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; background-color: #f1f3f5; padding: 12px; border-radius: 4px; border: 1px solid #e9ecef; color: #495057; }");
            resultsOutput.append(".highlight { color: #d63031; background-color: #ffead0; font-weight: bold; padding: 0 2px; border-radius: 2px; }");
            resultsOutput.append("h2 { color: #2c3e50; margin-top: 0; font-weight: 300; border-bottom: 2px solid #e9ecef; padding-bottom: 10px; }");
            resultsOutput.append("::-webkit-scrollbar { width: 8px; }");
            resultsOutput.append("::-webkit-scrollbar-track { background: #f1f1f1; }");
            resultsOutput.append("::-webkit-scrollbar-thumb { background: #ccc; border-radius: 4px; }");
            resultsOutput.append("::-webkit-scrollbar-thumb:hover { background: #999; }");
            resultsOutput.append("</style></head><body>");

            resultsOutput.append("<h2>Search Results</h2>");

            boolean found = false;

            while (rs.next()) {
                found = true;
                long sizeBytes = rs.getLong("size_bytes");
                String sizeStr = sizeBytes < 1024 ? sizeBytes + " B" : (sizeBytes / 1024) + " KB";
                if (sizeBytes > 1024 * 1024) sizeStr = String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
                
                String filePath = rs.getString("file_path");
                String fileName = rs.getString("file_name");

                resultsOutput.append("<div class='result-card'>");
                resultsOutput.append("<div class='file-name'>");
                resultsOutput.append("<span>").append(fileName).append("</span>");
                resultsOutput.append("<span class='file-size'>").append(sizeStr).append("</span>");
                resultsOutput.append("</div>");

                resultsOutput.append("<div class='file-path'>").append(filePath).append("</div>");

                if (!contentKeywords.isEmpty()) {
                    String snippet = extractSnippet(filePath, contentKeywords);
                    if (!snippet.isEmpty()) {
                        resultsOutput.append("<div class='snippet'>").append(snippet).append("</div>");
                    }
                }

                resultsOutput.append("</div>");
            }

            if (!found) {
                resultsOutput.append("<p style='text-align: center; color: #95a5a6; margin-top: 40px;'><i>No matching files found for your query.</i></p>");
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