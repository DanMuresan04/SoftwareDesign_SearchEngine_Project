package cli;
import crawler.FileCrawler;
import handler.FileHandler;
import handler.ConsoleFileHandler;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchEngineCLI {
    public static void main(String[] args) {
        System.out.println("=== Local Search Engine: Phase 1 ===");

        String pathString = (args.length > 0) ? args[0] : ".";
        Path startPath = Paths.get(pathString);

        FileHandler handler = new ConsoleFileHandler();
        FileCrawler crawler = new FileCrawler(handler);

        System.out.println("Starting crawl at: " + startPath.toAbsolutePath());
        crawler.crawl(startPath);

        crawler.crawl(startPath);
    }
}
