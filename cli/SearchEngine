package cli;

import crawler.FileCrawler;
import handler.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class SearchEngineCLI {

    public static void main(String[] args) {
        DatabaseManager.initializeSchema();

        String pathString = (args.length > 0) ? args[0] : ".";
        Path startPath = Paths.get(pathString);

       FileHandler handler = new DatabaseFileHandler();
        FileCrawler crawler = new FileCrawler(handler);

        System.out.println("Updating index... (Scanning for changes)");
        crawler.crawl(startPath);
        DatabaseFileHandler.finish();

        System.out.println("Index is up to date!");

        Searcher searcher = new Searcher();
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nDatabase loaded. Type a file name to search (or 'exit' to quit).");

        while (true) {
            System.out.print("Search> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Shutting down. Goodbye!");
                break;
            }

            if (!input.isEmpty()) {
                searcher.searchByName(input);
            }
        }

        scanner.close();
    }

}
