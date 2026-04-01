package crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.charset.MalformedInputException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ContentParser {

    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "md", "csv", "log",
            "html", "css", "json", "xml", "yaml", "yml", "ini", "properties", "env",
            "java", "c", "h", "cpp", "hpp", "cs", "py", "js", "ts", "php", "sh", "bat", "vhd"
    ));

    public Set<String> extractWords(Path filePath, String mimeType) {
        Set<String> uniqueWords = new HashSet<>();
        String content = "";

        String fileName = filePath.getFileName().toString().toLowerCase();
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }

        boolean isText = (mimeType != null && mimeType.startsWith("text/"))
                || TEXT_EXTENSIONS.contains(extension);

        if (!isText) {
            return uniqueWords;
        }

        try {
            try {
                content = Files.readString(filePath);
            } catch (MalformedInputException e) {
                content = Files.readString(filePath, StandardCharsets.ISO_8859_1);
            }

            if (!content.isBlank()) {
                String cleanText = content.replaceAll("[^a-zA-Z0-9_\\s]", " ").toLowerCase();
                String[] words = cleanText.split("\\s+");

                for (String word : words) {
                    if (!word.isBlank() && word.matches(".*[a-z].*")) {
                        uniqueWords.add(word);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Could not parse file: " + fileName);
        }

        return uniqueWords;
    }
}
