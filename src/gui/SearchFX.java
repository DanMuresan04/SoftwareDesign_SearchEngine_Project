package gui;

import handler.DatabaseFileHandler;
import handler.DatabaseManager;
import handler.Searcher;
import crawler.FileCrawler;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchFX extends Application {

    private Searcher searcher = new Searcher();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        VBox topArea = new VBox();

        HBox indexBar = new HBox(10);
        indexBar.setPadding(new Insets(10));

        TextField indexField = new TextField();
        indexField.setPrefWidth(450);
        indexField.setPromptText("Enter directory absolute path...");

        Button indexButton = new Button("Index");
        Label statusLabel = new Label("Ready");

        indexButton.setOnAction(e -> {
            String dirPath = indexField.getText().trim();
            if (dirPath.isEmpty()) {
                statusLabel.setText("Please enter a path.");
                return;
            }
            Path path = Paths.get(dirPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                statusLabel.setText("Invalid directory path.");
                return;
            }
            statusLabel.setText("Indexing...");
            DatabaseManager.initializeSchema();
            DatabaseFileHandler handler = new DatabaseFileHandler();
            FileCrawler crawler = new FileCrawler(handler);
            crawler.crawl(path);
            DatabaseFileHandler.finish();
            statusLabel.setText("Indexing complete!");
        });

        indexBar.getChildren().addAll(indexField, indexButton, statusLabel);

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPrefWidth(400);
        searchField.setPromptText("Try: ext:java public");

        javafx.scene.control.ComboBox<String> sortBox = new javafx.scene.control.ComboBox<>();
        sortBox.getItems().addAll("Relevance", "Date Modified", "Alphabetical", "Size");
        sortBox.setValue("Relevance");

        Button searchButton = new Button("Search");

        topBar.getChildren().addAll(searchField, sortBox, searchButton);

        WebView webView = new WebView();

        searchButton.setOnAction(e -> {
            String query = searchField.getText();
            String strategy = sortBox.getValue();
            if (!query.isBlank()) {
                webView.getEngine().loadContent("<html><body><i>Searching...</i></body></html>");

                String rawResults = searcher.search(query, strategy);

                String htmlFixed = "<html><head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: sans-serif; padding: 10px;'>" +
                        "<pre style='font-family: monospace; white-space: pre-wrap; font-size: 14px;'>" +
                        rawResults +
                        "</pre></body></html>";

                webView.getEngine().loadContent(htmlFixed);
            }
        });

        searchButton.setDefaultButton(true);

        topArea.getChildren().addAll(indexBar, topBar);
        root.setTop(topArea);
        root.setCenter(webView);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Search Engine");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}