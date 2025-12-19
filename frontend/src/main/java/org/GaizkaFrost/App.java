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

    private static int fontSize = 14;

    public static int getFontSize() {
        return fontSize;
    }

    public static void setFontSize(int size) {
        fontSize = size;
    }

    private static int lastPage = 0;

    public static int getLastPage() {
        return lastPage;
    }

    public static void setLastPage(int page) {
        lastPage = page;
    }

    public static void applyFontSize(Parent root) {
        if (root != null) {
            root.setStyle("-fx-font-size: " + fontSize + "px;");
        }
    }

    /**
     * Punto de entrada principal para la aplicación JavaFX.
     *
     * @param s El escenario (Stage) inicial.
     * @throws IOException Si falla la carga del archivo FXML.
     */
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
        stage.setTitle("Anuario Hogwarts");

        // Mostrar portada (Splash Screen) antes de la app principal
        showSplashScreen();
    }

    private void showSplashScreen() {
        try {
            URL imageUrl = App.class.getResource("/images/portada.png");
            if (imageUrl == null) {
                showMainView();
                return;
            }

            // Usar StackPane con imagen de fondo
            javafx.scene.layout.StackPane splashRoot = new javafx.scene.layout.StackPane();
            splashRoot.setStyle("-fx-background-image: url('" + imageUrl.toExternalForm() + "'); " +
                    "-fx-background-size: cover; " +
                    "-fx-background-position: center center; " +
                    "-fx-background-repeat: no-repeat;");

            // Crear la escena UNA SOLA VEZ
            scene = new Scene(splashRoot, 850, 700);
            stage.setScene(scene);

            // Mostrar maximizado
            stage.show();
            stage.setMaximized(true);

            // Transición
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2));
            delay.setOnFinished(event -> {
                try {
                    showMainView();
                } catch (IOException e) {
                    logger.error("Error transitioning to main view", e);
                }
            });
            delay.play();

        } catch (Exception e) {
            logger.error("Splash screen error", e);
            try {
                showMainView();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void showMainView() throws IOException {
        Parent root = loadFXML("Main_view");

        // Si la escena ya existe (viniendo del splash), solo cambiamos la raíz
        if (scene != null) {
            scene.setRoot(root);
        } else {
            // Si por alguna razón no existe (ej: fallo splash), la creamos
            scene = new Scene(root);
            stage.setScene(scene);
        }

        // CSS global
        URL cssUrl = App.class.getResource("/styles/estilos.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Configurar mínimos
        stage.setMinWidth(850);
        stage.setMinHeight(700);

        // Asegurar maximizado (sin parpadeo, ya que la ventana ya lo estaba)
        if (!stage.isMaximized()) {
            stage.setMaximized(true);
        }

        if (!stage.isShowing()) {
            stage.show();
        }
    }

    /**
     * Establece el icono de la aplicación en el escenario proporcionado.
     * Carga múltiples resoluciones para asegurar que se vea bien en todas partes
     * (barra de tareas, ventana, etc).
     * 
     * @param stage El escenario al que aplicar el icono.
     */
    public static void setWindowIcon(Stage stage) {
        try {
            URL iconUrl = App.class.getResource("/images/anuario_magico.png");
            if (iconUrl != null) {
                String url = iconUrl.toExternalForm();
                stage.getIcons().clear();
                stage.getIcons().addAll(
                        new javafx.scene.image.Image(url, 16, 16, true, true),
                        new javafx.scene.image.Image(url, 32, 32, true, true),
                        new javafx.scene.image.Image(url, 48, 48, true, true),
                        new javafx.scene.image.Image(url, 64, 64, true, true),
                        new javafx.scene.image.Image(url, 128, 128, true, true),
                        new javafx.scene.image.Image(url, 256, 256, true, true));
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
        applyFontSize(root);

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
        applyFontSize(root);
        return root;
    }

    /**
     * Se llama cuando la aplicación se detiene.
     * Forzamos la salida del sistema para asegurar que el Lanzador mate el backend.
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        System.out.println("Applicación FX detenida. Forzando salida del sistema...");
        System.exit(0);
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