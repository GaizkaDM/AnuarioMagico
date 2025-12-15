package org.GaizkaFrost;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Clase principal de la aplicación JavaFX.
 * Inicia el Stage principal y gestiona la carga de vistas FXML.
 *
 * @author GaizkaFrost
 * @version 1.0
 * @since 2025-12-14
 */
public class App extends Application {

    /** El escenario principal de la aplicación. */

    private static Stage stage;
    private static Scene scene;

    /**
     * Punto de entrada principal para la aplicación JavaFX.
     *
     * @param s El escenario (Stage) inicial.
     * @throws IOException Si falla la carga del archivo FXML.
     */
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

    /**
     * Establece la raíz de la escena actual a una vista FXML específica.
     * Mantiene el título actual del escenario.
     *
     * @param fxml El nombre del archivo FXML (sin extensión).
     * @throws IOException Si no se puede cargar el archivo.
     */
    static void setRoot(String fxml) throws IOException {
        setRoot(fxml, stage.getTitle());
    }

    /**
     * Establece la raíz de la escena actual y actualiza el título.
     *
     * @param fxml  El nombre del archivo FXML (sin extensión).
     * @param title El nuevo título para la ventana.
     * @throws IOException Si no se puede cargar el archivo.
     */
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

    /**
     * Carga un archivo FXML desde los recursos.
     *
     * @param fxml El nombre del archivo FXML.
     * @return El nodo raíz (Parent) cargado.
     * @throws IOException Si ocurre un error de E/S.
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Método main estándar para iniciar la aplicación.
     *
     * @param args Argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        launch(args);
    }
}