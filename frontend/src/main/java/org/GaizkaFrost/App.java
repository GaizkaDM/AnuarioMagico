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

    /**
     * Punto de entrada principal para la aplicación JavaFX.
     *
     * @param s El escenario (Stage) inicial.
     * @throws IOException Si falla la carga del archivo FXML.
     */
    @Override
    public void start(@SuppressWarnings("exports") Stage s) throws IOException {
        stage = s;
        setRoot("Main_view", "Anuario Hogwarts");
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
        Scene scene = new Scene(loadFXML(fxml));
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
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
