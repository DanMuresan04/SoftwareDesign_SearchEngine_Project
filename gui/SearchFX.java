package gui;

import handler.Searcher;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class SearchFX extends Application {

    private Searcher searcher = new Searcher();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPrefWidth(500);
        searchField.setPromptText("Try: ext:java public");

        Button searchButton = new Button("Search");

        topBar.getChildren().addAll(searchField, searchButton);

        WebView webView = new WebView();

        searchButton.setOnAction(e -> {
            String query = searchField.getText();
            if (!query.isBlank()) {
                // Temporary loading message
                webView.getEngine().loadContent("<html><body><i>Searching...</i></body></html>");

                String rawResults = searcher.search(query);

                String htmlFixed = "<html><head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: sans-serif; padding: 10px;'>" +
                        "<pre style='font-family: monospace; white-space: pre-wrap; font-size: 14px;'>" +
                        rawResults +
                        "</pre></body></html>";

                webView.getEngine().loadContent(htmlFixed);
            }
        });

        searchButton.setDefaultButton(true);

        root.setTop(topBar);
        root.setCenter(webView);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Modern JavaFX Search Engine");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
