package org.GaizkaFrost;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.File;
import java.net.URL;

/**
 * Clase principal de la aplicación JavaFX.
 * Inicia el Stage principal y gestiona la carga de vistas FXML.
 *
 * @author Gaizka
 * @author Diego
 * @author Xiker
 */
public class App extends Application {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /** El escenario principal de la aplicación. */

    private static Stage stage;
    private static Scene scene;
    private static App instance;
    private static boolean isDarkMode = false;
    private static java.util.List<String> availableHouses = new java.util.ArrayList<>();

    public static java.util.List<String> getAvailableHouses() {
        return availableHouses;
    }

    public static void setAvailableHouses(java.util.List<String> houses) {
        availableHouses = houses;
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    public static void setDarkMode(boolean mode) {
        isDarkMode = mode;
    }

    /**
     * Punto de entrada principal para la aplicación JavaFX.
     *
     * @param s El escenario (Stage) inicial.
     * @throws IOException Si falla la carga del archivo FXML.
     */
    @Override
    public void start(Stage s) throws IOException {
        logger.info("Starting frontend application...");
        instance = this;
        stage = s;

        setWindowIcon(stage);

        Parent root = loadFXML("Main_view");
        scene = new Scene(root);

        // CSS global una sola vez
        scene.getStylesheets().add(
                App.class.getResource("/styles/estilos.css").toExternalForm());

        stage.setTitle("Anuario Hogwarts");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    /**
     * Establece el icono de la aplicación en el escenario proporcionado.
     * 
     * @param stage El escenario al que aplicar el icono.
     */
    public static void setWindowIcon(Stage stage) {
        try {
            URL iconUrl = App.class.getResource("/images/anuario_magico.png");
            if (iconUrl != null) {
                stage.getIcons().clear();
                stage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
            } else {
                logger.warn("Application icon not found at /images/anuario_magico.png");
            }
        } catch (Exception e) {
            logger.error("Error setting window icon: {}", e.getMessage());
        }
    }

    /**
     * Establece el icono de la aplicación en un diálogo.
     * 
     * @param dialog El diálogo al que aplicar el icono.
     */
    public static void setIcon(javafx.scene.control.Dialog<?> dialog) {
        try {
            if (dialog.getDialogPane() != null && dialog.getDialogPane().getScene() != null) {
                setWindowIcon((Stage) dialog.getDialogPane().getScene().getWindow());
            }
        } catch (Exception e) {
            logger.error("Error setting dialog icon: {}", e.getMessage());
        }
    }

    /**
     * Establece la raíz de la escena actual a una vista FXML específica.
     * Mantiene el título actual del escenario.
     *
     * @param fxml El nombre del archivo FXML (sin extensión).
     * @throws IOException Si no se puede cargar el archivo.
     */
    public static void setRoot(String fxml) throws IOException {
        setRoot(fxml, stage.getTitle());
    }

    /**
     * Establece la raíz de la escena actual y actualiza el título.
     *
     * @param fxml  El nombre del archivo FXML (sin extensión).
     * @param title El nuevo título para la ventana.
     * @throws IOException Si no se puede cargar el archivo.
     */
    public static void setRoot(String fxml, String title) throws IOException {
        Parent root = loadFXML(fxml);
        scene.setRoot(root);

        stage.setTitle(title);

        // Mantén maximizado siempre (sin recrear Scene)
        stage.setMaximized(true);
    }

    public static <T> T setRootAndGetController(String fxml, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        loader.setResources(getBundle());
        Parent root = loader.load();
        applyTheme(root, fxml);

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
    /**
     * Applies the correct theme (Normal/Dark) based on global state.
     */
    public static void applyTheme(Parent root, String fxml) {
        root.getStylesheets().clear();
        String cssPath = "";

        switch (fxml) {
            case "Main_view":
                cssPath = isDarkMode ? "/styles/estilos_ravenclaw.css" : "/styles/estilos.css";
                break;
            case "Detail_view":
                cssPath = isDarkMode ? "/styles/estilos_detalles_ravenclaw.css" : "/styles/estilos_detalles.css";
                break;
            case "Login_view":
                cssPath = isDarkMode ? "/styles/login_ravenclaw.css" : "/styles/login.css";
                break;
            case "Edit_view":
                cssPath = isDarkMode ? "/styles/form_ravenclaw.css" : "/styles/form.css";
                break;
        }

        if (!cssPath.isEmpty()) {
            URL resource = App.class.getResource(cssPath);
            if (resource != null) {
                root.getStylesheets().add(resource.toExternalForm());
            }
        }
    }

    private static java.util.Locale currentLocale = java.util.Locale.ENGLISH;
    private static java.util.ResourceBundle bundle;

    public static void setLocale(java.util.Locale locale) {
        currentLocale = locale;
        bundle = java.util.ResourceBundle.getBundle("i18n.messages", currentLocale);
    }

    public static java.util.ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = java.util.ResourceBundle.getBundle("i18n.messages", currentLocale);
        }
        return bundle;
    }

    public static java.util.Locale getLocale() {
        return currentLocale;
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
        fxmlLoader.setResources(getBundle());
        Parent root = fxmlLoader.load();
        applyTheme(root, fxml);
        return root;
    }

    /**
     * Método main estándar para iniciar la aplicación.
     *
     * @param args Argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        java.util.Locale.setDefault(java.util.Locale.ENGLISH); // Force English default
        launch(args);
    }
}