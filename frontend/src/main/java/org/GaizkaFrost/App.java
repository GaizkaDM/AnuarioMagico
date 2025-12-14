package org.GaizkaFrost;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Stage stage;
    private static Scene scene;

    @Override
    public void start(Stage s) throws IOException {
        stage = s;

        Parent root = loadFXML("Main_view");
        scene = new Scene(root);

        // CSS global una sola vez
        scene.getStylesheets().add(
                App.class.getResource("/styles/estilos.css").toExternalForm()
        );

        stage.setTitle("Anuario Hogwarts");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        setRoot(fxml, stage.getTitle());
    }

    static void setRoot(String fxml, String title) throws IOException {
        Parent root = loadFXML(fxml);
        scene.setRoot(root);

        stage.setTitle(title);

        // Mantén maximizado siempre (sin recrear Scene)
        stage.setMaximized(true);
    }

    static <T> T setRootAndGetController(String fxml, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        Parent root = loader.load();

        scene.setRoot(root);
        stage.setTitle(title);
        stage.setMaximized(true);

        return loader.getController();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
