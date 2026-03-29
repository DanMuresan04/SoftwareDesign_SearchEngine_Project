package crawler;
import handler.FileHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileCrawler {
    private final FileHandler fileHandler;
    public FileCrawler(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    public void crawl(Path rootPath) {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            System.err.println("Error: The path provided does not exist or is not a directory: " + rootPath);
            return;
        }

        SafeFileVisitor visitor = new SafeFileVisitor(fileHandler);

        try {
            Files.walkFileTree(rootPath, visitor);
        } catch (IOException e) {
            System.err.println("A critical error occurred while initializing the crawl at " + rootPath);
            e.printStackTrace();
        }
    }
}
