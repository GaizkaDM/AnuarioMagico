package org.GaizkaFrost.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.Locale;
import java.text.MessageFormat;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.GaizkaFrost.App;
import org.GaizkaFrost.models.Personaje;
import org.GaizkaFrost.services.HarryPotterAPI;
import org.GaizkaFrost.services.ReportService;

/**
 * Controlador principal de la aplicación.
 * Gestiona la interfaz principal, listado de personajes, filtrado y navegación
 * 
 * a detalles.
 * 
 * @author Gaizka
 * @author Diego
 * @author Xiker
 */
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<String> comboCasa;
    @FXML
    private ComboBox<String> comboEstado;
    @FXML
    private TextField txtPatronus;
    @FXML
    private CheckBox checkFavoritos;
    @FXML
    private Button btnLimpiar;
    @FXML
    private Button btnSincronizar;
    @FXML
    private Button btnGenerarPDF;
    @FXML
    private Button btnAnadir;

    @FXML
    private VBox filtersBar;
    @FXML
    private FlowPane contenedorTarjetas;

    @FXML
    private Button btnPaginaAnterior;
    @FXML
    private Button btnPaginaSiguiente;
    @FXML
    private TextField txtPagina;
    @FXML
    private TextField txtPaginaSidebar;
    @FXML
    private Label lblTotalPaginas;
    @FXML
    private Label statusBar;
    @FXML
    private VBox loadingBox;
    @FXML
    private Button btnThemeToggle;
    @FXML
    private HBox sidebarContainer;

    private final ObservableList<Personaje> masterData = FXCollections.observableArrayList();
    @FXML
    private Button btnSidebarToggle;

    private List<Personaje> listaFiltrada = new ArrayList<>();

    private int paginaActual = 0;
    // Estado de filtros guardado
    private static String savedSearch = "";
    private static String savedHouse = null;
    private static String savedStatus = null;
    private static boolean savedFavorite = false;

    private static final int PERSONAJES_POR_PAGINA = 20;

    @FXML
    private MenuItem menuLogin;
    @FXML
    private MenuItem menuExit;
    @FXML
    private MenuItem menuAbout;
    @FXML
    private MenuItem menuManual;
    @FXML
    private MenuItem menuLangEn;
    @FXML
    private MenuItem menuLangEs;

    // Font Size Menu
    @FXML
    private RadioMenuItem menuFontSmall;
    @FXML
    private RadioMenuItem menuFontMedium;
    @FXML
    private RadioMenuItem menuFontLarge;
    @FXML
    private Label lblUsuario;
    @FXML
    private ScrollPane scrollPane;

    private boolean isLoggedIn = false;
    private String currentUser = null;

    /**
     * Inicializa el controlador. Configura listeners, carga datos iniciales y
     * configura la UI.
     *
     * @param location  La ubicación utilizada para resolver rutas relativas para el
     *                  objeto raíz, o null si no se conoce.
     * @param resources Los recursos utilizados para localizar el objeto raíz, o
     *                  null si no se conoce.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Setup Font Size Actions
        if (menuFontSmall != null) {
            menuFontSmall.setOnAction(e -> setAppFontSize(12)); // Small
            menuFontMedium.setOnAction(e -> setAppFontSize(14)); // Normal (Default)
            menuFontLarge.setOnAction(e -> setAppFontSize(18)); // Large

            // Sync menu with current state
            int currentSize = App.getFontSize();
            if (currentSize == 12)
                menuFontSmall.setSelected(true);
            else if (currentSize == 18)
                menuFontLarge.setSelected(true);
            else
                menuFontMedium.setSelected(true);
        }

        // Aumentar velocidad de desplazamiento
        if (scrollPane != null) {
            scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
                if (event.getDeltaY() != 0) {
                    double delta = event.getDeltaY() * 3.0; // 3x más rápido
                    double height = scrollPane.getContent().getBoundsInLocal().getHeight();
                    double vValue = scrollPane.getVvalue();
                    // Prevenir división por cero
                    if (height > 0) {
                        scrollPane.setVvalue(vValue + -delta / height);
                        event.consume(); // Consumir evento para prevenir desplazamiento lento por defecto
                    }
                }
            });
        }

        // Verificar si hay una sesión activa previa (cuando se vuelve de otra vista)
        if (HarryPotterAPI.isLoggedIn()) {
            // Restaurar estado de sesión en la UI
            isLoggedIn = true;
            currentUser = HarryPotterAPI.getUsername();
            menuLogin.setText("Cerrar Sesión");
            menuLogin.setOnAction(e -> cerrarSesion());

            if (currentUser != null) {
                lblUsuario.setText(App.getBundle().getString("main.user.prefix") + " " + currentUser);
                statusBar
                        .setText(MessageFormat.format(App.getBundle().getString("main.status.logged_in"), currentUser));
            } else {
                lblUsuario.setText(App.getBundle().getString("main.user.prefix") + " "
                        + App.getBundle().getString("main.session.active"));
                statusBar.setText(App.getBundle().getString("main.session.active"));
            }
            if (btnAnadir != null) {
                btnAnadir.setVisible(true);
                btnAnadir.setManaged(true);
            }
        } else {
            // Configurar menú de inicio de sesión
            menuLogin.setOnAction(e -> mostrarLogin());

            // Configurar menú de ayuda
            if (menuManual != null) {
                menuManual.setOnAction(e -> mostrarAyuda());
            }
            if (btnAnadir != null) {
                btnAnadir.setVisible(false);
                btnAnadir.setManaged(false);
            }
        }

        // Configurar menú salir
        if (menuExit != null) {
            menuExit.setOnAction(e -> {
                Platform.exit();
            });
        }

        // Configurar menú About
        if (menuAbout != null) {
            menuAbout.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                App.setIcon(alert);
                alert.setTitle(App.getBundle().getString("main.menu.about"));
                alert.setHeaderText("Aplicación desarrollada por el Equipo Hagrid");
                alert.setContentText("Desarrolladores:\n\n- Diego\n- Xiker\n- Gaizka\n\n© 2025 Anuario Hogwarts");
                alert.showAndWait();
            });
        }

        // House and Status combos will be populated dynamically or locally
        comboEstado.getItems().addAll(
                App.getBundle().getString("combo.status.alive"),
                App.getBundle().getString("combo.status.deceased"));

        // Intentar sincronizar datos de la nube al inicio (Pull)
        setCargando(true); // Mostrar spinner mientras se intenta el pull
        new Thread(() -> {
            // 1. Esperar a que el backend esté listo (Max 15 segundos)
            logger.info("Waiting for backend to be ready...");
            int retries = 0;
            boolean backendReady = false;
            while (retries < 15) {
                if (HarryPotterAPI.isBackendReady()) {
                    backendReady = true;
                    break;
                }
                try {
                    Thread.sleep(1000); // Esperar 1 segundo
                } catch (InterruptedException ignored) {
                }
                retries++;
            }

            if (!backendReady) {
                logger.error("Backend not reachable after 15 seconds. Proceeding anyway (might fail).");
            } else {
                logger.info("Backend is UP!");
            }

            // 2. Proceder con el flujo normal de inicio
            logger.info("Attempting initial synchronization (Pull)...");
            boolean synced = HarryPotterAPI.syncPull();
            if (synced) {
                logger.info("Synchronization complete. Reloading local data...");
                Platform.runLater(this::sincronizar);
            } else {
                logger.warn("Sync failed (Offline?). Loading local data...");
                Platform.runLater(this::sincronizar);
            }
        }).start();

        // Filtros
        txtBuscar.textProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboCasa.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        txtPatronus.textProperty().addListener((obs, o, n) -> aplicarFiltros());
        checkFavoritos.selectedProperty().addListener((obs, o, n) -> aplicarFiltros());

        // Restaurar estado de filtros si existe
        if (savedSearch != null)
            txtBuscar.setText(savedSearch);
        if (savedHouse != null)
            comboCasa.setValue(savedHouse);
        if (savedStatus != null)
            comboEstado.setValue(savedStatus);
        checkFavoritos.setSelected(savedFavorite);

        // Listener para el botón de tema
        actualizarIconoTema();
        btnThemeToggle.setOnAction(e -> toggleTheme());

        // Sidebar Toggle
        btnSidebarToggle.setOnAction(e -> toggleSidebar());
        // Asegurar que el VBox se oculte y no ocupe espacio
        filtersBar.managedProperty().bind(filtersBar.visibleProperty());

        // Listener para el menú de idioma
        if (menuLangEn != null)
            menuLangEn.setOnAction(e -> changeLanguage(java.util.Locale.ENGLISH));
        if (menuLangEs != null)
            menuLangEs.setOnAction(e -> changeLanguage(java.util.Locale.forLanguageTag("es")));

        btnLimpiar.setOnAction(e -> limpiarFiltros());

        // Paginación
        btnPaginaAnterior.setOnAction(e -> {
            paginaActual--;
            actualizarPagina();
        });

        btnPaginaSiguiente.setOnAction(e -> {
            paginaActual++;
            actualizarPagina();
        });

        // Listener para el campo de número de página
        txtPagina.setOnAction(e -> manejarCambioPagina(txtPagina));
        txtPagina.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Si pierde el foco
                manejarCambioPagina(txtPagina);
            }
        });

        // Jumper de página (Sidebar)
        txtPaginaSidebar.setOnAction(e -> manejarCambioPagina(txtPaginaSidebar));
        txtPaginaSidebar.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                manejarCambioPagina(txtPaginaSidebar);
            }
        });

        btnSincronizar.setOnAction(e -> sincronizar());
        if (btnAnadir != null) {
            btnAnadir.setOnAction(e -> abrirFormularioAnadir());
        }
        if (btnGenerarPDF != null) {
            btnGenerarPDF.setOnAction(e -> {
                logger.debug("Button PDF clicked in MainController");
                try {
                    ReportService.generateListReport(listaFiltrada, (Stage) btnGenerarPDF.getScene().getWindow());
                } catch (Throwable t) {
                    logger.error("Error generating character list PDF: {}", t.getMessage(), t);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    App.setIcon(alert);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error al generar PDF");
                    alert.setContentText(
                            "No se pudo generar el reporte.\nPosible causa: Librerías faltantes o error en plantilla.\n\nDetalle: "
                                    + t.getMessage());
                    alert.showAndWait();
                }
            });
        }
    }

    private void changeLanguage(Locale locale) {
        App.setLocale(locale);
        try {
            // Reload Main_view
            App.setRoot("Main_view", "Anuario Hogwarts");
        } catch (IOException e) {
            logger.error("Error changing language: {}", e.getMessage(), e);
        }
    }

    /**
     * Maneja el cambio manual de página desde el TextField.
     */
    private void manejarCambioPagina(TextField source) {
        if (source == null)
            return;

        try {
            int nuevaPagina = Integer.parseInt(source.getText()) - 1;
            int totalPaginas = (int) Math.ceil(listaFiltrada.size() / (double) PERSONAJES_POR_PAGINA);

            if (nuevaPagina < 0)
                nuevaPagina = 0;
            if (nuevaPagina >= totalPaginas)
                nuevaPagina = totalPaginas - 1;

            if (nuevaPagina != paginaActual) {
                paginaActual = nuevaPagina;
                actualizarPagina();
            } else {
                // Even if page didn't change, we might want to restore the text field value
                // in case user typed "999" and we clamped it to "10",
                // but we DON'T want to re-render the whole page (flicker).
                source.setText(String.valueOf(paginaActual + 1));
            }

            // Sync the other text field
            if (source == txtPagina && txtPaginaSidebar != null) {
                txtPaginaSidebar.setText(String.valueOf(paginaActual + 1));
            } else if (source == txtPaginaSidebar && txtPagina != null) {
                txtPagina.setText(String.valueOf(paginaActual + 1));
            }

        } catch (NumberFormatException e) {
            // Restore current page if invalid input
            source.setText(String.valueOf(paginaActual + 1));
        }
    }

    @FXML
    private void mostrarLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login_view.fxml"), App.getBundle());
            Parent root = loader.load();

            LoginController loginCtrl = loader.getController();
            loginCtrl.setOnSuccessCallback(this::onLoginRealizado);

            // Aplicar tema correcto
            App.applyTheme(root, "Login_view");
            App.applyFontSize(root);

            Stage stage = new Stage();
            App.setWindowIcon(stage);
            stage.setTitle("Login / Registro");

            // Modal y no redimensionable
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            // Crear escena y aplicar tema correcto
            Scene loginScene = new Scene(root, 400, 550);
            if (App.isDarkMode()) {
                loginScene.getStylesheets().add(getClass().getResource("/styles/login_ravenclaw.css").toExternalForm());
            } else {
                loginScene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
            }

            stage.setScene(loginScene);
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Error displaying login view: {}", e.getMessage(), e);
        }
    }

    @FXML
    private javafx.scene.layout.BorderPane root; // Injected root

    // START DARK MODE LOGIC
    private void toggleTheme() {
        // Animación de transición (Fade Out -> Cambio -> Fade In)
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            boolean newMode = !App.isDarkMode();
            App.setDarkMode(newMode);
            actualizarIconoTema();

            // Apply to current view immediately
            if (root != null) {
                App.applyTheme(root, "Main_view");
            } else if (contenedorTarjetas.getScene() != null) {
                App.applyTheme(contenedorTarjetas.getScene().getRoot(), "Main_view");
            }

            // Restore font size if needed (optional, but good for consistency)
            // For now, simple theme toggle overrides style, so we might need to re-apply
            // font size logic
            // But let's wait for user feedback.
            // Better: update setAppFontSize to store current size

            // Fade In
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private void setAppFontSize(int size) {
        App.setFontSize(size);
        if (root != null) {
            App.applyFontSize(root);
        }
    }

    private void actualizarIconoTema() {
        if (btnThemeToggle != null) {
            boolean isDark = App.isDarkMode();
            btnThemeToggle.setText(isDark ? "🌙" : "☀");

            String tooltipKey = isDark ? "main.theme.dark" : "main.theme.light";
            btnThemeToggle.setTooltip(new Tooltip(App.getBundle().getString(tooltipKey)));
        }
    }

    private void toggleSidebar() {
        boolean isVisible = filtersBar.isVisible();
        filtersBar.setVisible(!isVisible);
        btnSidebarToggle.setText(isVisible ? "▶" : "◀");

        // Ajustar el margen del botón cuando está oculto para que sea visible
        StackPane.setMargin(btnSidebarToggle, new javafx.geometry.Insets(10, isVisible ? -25 : -12, 0, 0));
    }
    // END DARK MODE LOGIC

    /**
     * Callback ejecutado cuando el inicio de sesión es exitoso.
     * Actualiza la interfaz para reflejar el estado de usuario autenticado.
     *
     * @param username El nombre del usuario que ha iniciado sesión.
     */
    private void onLoginRealizado(String username) {
        isLoggedIn = true;
        currentUser = username;
        Platform.runLater(() -> {
            menuLogin.setText(App.getBundle().getString("main.menu.logout"));
            menuLogin.setOnAction(e -> cerrarSesion());
            lblUsuario.setText(App.getBundle().getString("main.user.prefix") + " " + username);
            statusBar.setText(MessageFormat.format(App.getBundle().getString("main.status.logged_in"), username));
            // Aquí se activarían los botones de edición si existieran
            if (btnAnadir != null) {
                btnAnadir.setVisible(true);
                btnAnadir.setManaged(true);
            }
        });
    }

    /**
     * Cierra la sesión del usuario actual y restaura el estado de la interfaz.
     */
    private void cerrarSesion() {
        isLoggedIn = false;
        currentUser = null;
        HarryPotterAPI.clearToken(); // Limpiar token de sesión
        menuLogin.setText(App.getBundle().getString("main.menu.login"));
        menuLogin.setOnAction(e -> mostrarLogin());
        lblUsuario.setText("");
        statusBar.setText(App.getBundle().getString("main.status.logged_out"));
        // Aquí se desactivarían los botones de edición
        if (btnAnadir != null) {
            btnAnadir.setVisible(false);
            btnAnadir.setManaged(false);
        }
    }

    /**
     * Limpia todos los filtros activos y reinicia la lista.
     */
    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        comboCasa.getSelectionModel().clearSelection();
        comboEstado.getSelectionModel().clearSelection();
        txtPatronus.clear();
        checkFavoritos.setSelected(false);

        // Reset page history
        App.setLastPage(0);
        paginaActual = 0;

        aplicarFiltros();
    }

    /**
     * Aplica los filtros (búsqueda, casa, estado, favoritos) a la lista maestra de
     * personajes.
     * Actualiza `listaFiltrada` y la paginación.
     */
    private void aplicarFiltros() {
        aplicarFiltros(true);
    }

    private void aplicarFiltros(boolean resetPage) {
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().toLowerCase().trim() : "";
        String casa = comboCasa.getValue();
        String unknownLabel = App.getBundle().getString("combo.house.unknown");
        String noneLabel = App.getBundle().getString("combo.house.none");
        String estado = comboEstado.getValue();
        String patronusBusqueda = txtPatronus.getText() != null ? txtPatronus.getText().toLowerCase().trim() : "";
        boolean soloFavoritos = checkFavoritos.isSelected();

        List<Personaje> filtrados = new ArrayList<>();

        for (Personaje p : masterData) {
            boolean coincideTexto = texto.isEmpty()
                    || p.getNombre().toLowerCase().contains(texto);

            boolean coincideCasa = casa == null || casa.isEmpty();
            if (!coincideCasa) {
                if (casa.equals(noneLabel)) {
                    // "Sin casa" matches null or empty
                    coincideCasa = (p.getCasa() == null || p.getCasa().trim().isEmpty());
                } else if (casa.equals(unknownLabel)) {
                    // "Desconocido" matches explicit "unknown" or "desconocido"
                    coincideCasa = (p.getCasa() != null && (p.getCasa().toLowerCase().contains("unknown")
                            || p.getCasa().toLowerCase().contains("desconocido")));
                } else {
                    // Regular house name match
                    coincideCasa = (p.getCasa() != null && p.getCasa().toLowerCase().contains(casa.toLowerCase()));
                }
            }

            String estadoFiltro = null;
            if (estado != null) {
                if (estado.equals(App.getBundle().getString("combo.status.alive"))) {
                    estadoFiltro = "Vivo";
                } else if (estado.equals(App.getBundle().getString("combo.status.deceased"))) {
                    estadoFiltro = "Fallecido";
                } else {
                    estadoFiltro = estado;
                }
            }

            boolean coincideEstado = estadoFiltro == null || estadoFiltro.isEmpty()
                    || (p.getEstado() != null && p.getEstado().equalsIgnoreCase(estadoFiltro));

            boolean coincideFavorito = !soloFavoritos || p.isFavorite();

            boolean coincidePatronus = patronusBusqueda.isEmpty()
                    || (p.getPatronus() != null && p.getPatronus().toLowerCase().contains(patronusBusqueda));

            if (coincideTexto && coincideCasa && coincideEstado && coincideFavorito && coincidePatronus) {
                filtrados.add(p);
            }
        }

        listaFiltrada = filtrados;

        if (resetPage) {
            paginaActual = 0;
        }

        actualizarPagina();
    }

    /**
     * Actualiza la vista de tarjetas para mostrar la página actual de resultados.
     */
    private void actualizarPagina() {
        contenedorTarjetas.getChildren().clear();

        int total = listaFiltrada.size();
        if (total == 0) {
            txtPagina.setText("1");
            lblTotalPaginas.setText("de 1");
            btnPaginaAnterior.setDisable(true);
            btnPaginaSiguiente.setDisable(true);
            // Solo mostrar mensaje si no se está cargando
            if (!btnSincronizar.isDisabled()) {
                statusBar.setText(App.getBundle().getString("main.status.no_results"));
            }
            return;
        }

        int totalPaginas = (int) Math.ceil(total / (double) PERSONAJES_POR_PAGINA);

        if (paginaActual < 0)
            paginaActual = 0;
        if (paginaActual >= totalPaginas)
            paginaActual = totalPaginas - 1;

        int inicio = paginaActual * PERSONAJES_POR_PAGINA;
        int fin = Math.min(inicio + PERSONAJES_POR_PAGINA, total);

        List<Personaje> pagina = listaFiltrada.subList(inicio, fin);

        for (Personaje p : pagina) {
            contenedorTarjetas.getChildren().add(crearTarjeta(p));
        }

        txtPagina.setText(String.valueOf(paginaActual + 1));
        lblTotalPaginas.setText(App.getBundle().getString("main.pagination.of") + " " + totalPaginas);
        btnPaginaAnterior.setDisable(paginaActual == 0);
        btnPaginaSiguiente.setDisable(paginaActual >= totalPaginas - 1);

        statusBar.setText(App.getBundle().getString("main.status.ready"));
    }

    /**
     * Crea un componente visual (tarjeta) para un personaje.
     *
     * @param p El personaje a mostrar.
     * @return Un objeto VBox que contiene la representación visual del personaje.
     */
    private VBox crearTarjeta(Personaje p) {
        VBox tarjeta = new VBox();
        tarjeta.setPrefWidth(200);
        tarjeta.getStyleClass().add("card");

        ImageView img = new ImageView();
        img.setFitWidth(160);
        img.setPreserveRatio(true);

        try {
            if (p.getImagenUrl() != null && !p.getImagenUrl().isEmpty()) {
                Image image = new Image(p.getImagenUrl(), true);
                img.setImage(image);
            }
        } catch (Exception e) {
            logger.error("Error loading image for {}: {}", p.getNombre(), e.getMessage());
        }

        Label lblNombre = new Label(p.getNombre());
        lblNombre.getStyleClass().add("card-title");

        Label lblCasa = new Label(
                App.getBundle().getString("detail.house") + " " + formatField(p.getCasa()));
        lblCasa.getStyleClass().add("card-meta");

        Label lblEstado = new Label(
                App.getBundle().getString("edit.label.status") + " " + formatField(p.getEstado()));
        lblEstado.getStyleClass().add("card-meta");

        Label lblPatronus = new Label(
                App.getBundle().getString("detail.patronus") + " " + formatField(p.getPatronus()));
        lblPatronus.getStyleClass().add("card-meta");

        Button btnDetalles = new Button(App.getBundle().getString("card.button.details"));
        btnDetalles.getStyleClass().add("card-button");
        btnDetalles.setOnAction(e -> abrirDetalles(p));

        // Tooltips
        String cardTooltipText = MessageFormat.format(App.getBundle().getString("card.tooltip.info"), p.getNombre());
        Tooltip.install(tarjeta, new Tooltip(cardTooltipText));

        String btnTooltipText = MessageFormat.format(App.getBundle().getString("card.tooltip.details"), p.getNombre());
        btnDetalles.setTooltip(new Tooltip(btnTooltipText));

        // Espaciador para empujar el botón al fondo
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        tarjeta.getChildren().addAll(img, lblNombre, lblCasa, lblEstado, lblPatronus, spacer, btnDetalles);

        // --- MICRO-ANIMACIÓN HOVER ---
        // Escala suave al pasar el ratón (1.0 -> 1.05)
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(200), tarjeta);
        scaleIn.setToX(1.05);
        scaleIn.setToY(1.05);

        javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(200), tarjeta);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        tarjeta.setOnMouseEntered(e -> {
            scaleOut.stop();
            scaleIn.playFromStart();
            tarjeta.setStyle("-fx-cursor: hand;"); // Cambiar cursor
        });

        tarjeta.setOnMouseExited(e -> {
            scaleIn.stop();
            scaleOut.playFromStart();
        });
        // -----------------------------

        return tarjeta;
    }

    private String formatField(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }

    /**
     * Abre la vista detallada para el personaje seleccionado.
     *
     * @param p El personaje cuyos detalles se mostrarán.
     */
    private void abrirDetalles(Personaje p) {
        try {
            App.setLastPage(paginaActual); // Guardar página antes de ir a detalles
            DetailController controller = App.setRootAndGetController("Detail_view", "Detalles del Personaje");
            controller.setPersonaje(p);
        } catch (IOException e) {
            logger.error("Error opening detail view", e);
            statusBar.setText("Error al abrir detalles.");
        }
    }

    /**
     * Abre el formulario para añadir un nuevo personaje.
     */
    private void abrirFormularioAnadir() {
        if (!isLoggedIn)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Edit_view.fxml"), App.getBundle());
            Parent root = loader.load();

            EditController controller = loader.getController();
            controller.setPersonaje(null); // Modo añadir
            controller.setOnSaveSuccess(this::sincronizar);

            App.applyTheme(root, "Edit_view");

            Stage stage = new Stage();
            App.setWindowIcon(stage);
            stage.setTitle("Añadir Personaje");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root, 900, 700);
            stage.setScene(scene);
            stage.showAndWait(); // Esperar a que cierre

        } catch (IOException e) {
            logger.error("Error opening add character form: {}", e.getMessage(), e);
            statusBar.setText("Error al abrir formulario de añadir.");
        }
    }

    /**
     * Importa personajes desde la API de Harry Potter en un hilo en segundo plano.
     * Actualiza la interfaz gráfica una vez completada la carga.
     */
    /**
     * Importa personajes desde la API y sincroniza imágenes.
     */
    private void sincronizar() {
        // 1. Carga inicial rápida (datos existentes)
        setCargando(true);
        btnSincronizar.setDisable(true);
        statusBar.setText("Cargando datos locales...");

        new Thread(() -> {
            try {
                // Fetch fast
                List<Personaje> cachedData = HarryPotterAPI.fetchCharacters();
                if (cachedData.isEmpty()) {
                    // First run ever?
                } else {
                    Platform.runLater(() -> {
                        masterData.setAll(cachedData);
                        actualizarComboCasas();
                        // Restore page immediately for initial view
                        paginaActual = App.getLastPage();
                        aplicarFiltros(false);
                        setCargando(false); // Desbloquear UI inmediatamente
                        statusBar.setText("Datos locales cargados. Buscando actualizaciones...");
                    });
                }

                // 2. Sincronización en segundo plano (Lento)
                boolean pullSuccess = HarryPotterAPI.syncPull();

                if (pullSuccess) {
                    boolean downloading = true;
                    int noStatusCount = 0;

                    while (downloading) {
                        try {
                            JsonObject status = HarryPotterAPI.getImageSyncStatus();

                            if (status != null) {
                                boolean running = status.get("running").getAsBoolean();
                                int current = status.get("current").getAsInt();
                                int total = status.get("total").getAsInt();
                                Platform.runLater(() -> {
                                    statusBar.setText(
                                            String.format("Descargando nuevas imágenes: %d/%d...", current, total));
                                });

                                if (!running)
                                    downloading = false;
                            } else {
                                noStatusCount++;
                                if (noStatusCount > 5)
                                    downloading = false;
                            }

                            if (downloading)
                                Thread.sleep(1000); // Polling más lento
                        } catch (Exception e) {
                            downloading = false;
                        }
                    }

                    // 3. Recarga final tras sincronización
                    List<Personaje> freshData = HarryPotterAPI.fetchCharacters();

                    // Comprobar si hay cambios reales para evitar parpadeo
                    Platform.runLater(() -> {
                        boolean hasChanges = !cachedData.equals(freshData);

                        // Si hay cambios, actualizar datos
                        if (hasChanges) {
                            masterData.setAll(freshData);
                            actualizarComboCasas();

                            // Restaurar última página global (o mantenerla si ya es correcta)
                            paginaActual = App.getLastPage();

                            // Aplicar filtros SIN reiniciar la página (false)
                            aplicarFiltros(false);
                        }

                        statusBar.setText(App.getBundle().getString("main.status.ready"));
                        btnSincronizar.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        statusBar.setText("Modo Offline (Sincronización fallida)");
                        btnSincronizar.setDisable(false);
                    });
                }
            } catch (Exception e) {
                logger.error("Error during synchronization: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    statusBar.setText("Error al sincronizar.");
                    btnSincronizar.setDisable(false);
                    setCargando(false);
                });
            }
        }).start();
    }

    /**
     * Muestra el manual de usuario en una nueva ventana.
     * Guía detallada "paso a paso" para usuarios principiantes.
     */
    private void mostrarAyuda() {
        Stage helpStage = new Stage();
        App.setWindowIcon(helpStage);
        helpStage.setTitle(App.getBundle().getString("help.title"));

        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        javafx.scene.web.WebEngine webEngine = webView.getEngine();

        String lang = App.getLocale().getLanguage();
        String manualFile = "manual_en.html";
        if ("es".equalsIgnoreCase(lang)) {
            manualFile = "manual_es.html";
        }

        URL url = getClass().getResource("/docs/" + manualFile);
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            webEngine.loadContent("<h1>Error</h1><p>Manual not found.</p>");
        }

        Scene scene = new Scene(webView, 950, 800);
        helpStage.setScene(scene);

        // Centrar en pantalla
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        helpStage.setX((screenBounds.getWidth() - 950) / 2);
        helpStage.setY((screenBounds.getHeight() - 800) / 2);

        // Evitar que sea más alta que la pantalla
        if (800 > screenBounds.getHeight()) {
            helpStage.setHeight(screenBounds.getHeight() * 0.9);
            helpStage.setY((screenBounds.getHeight() - helpStage.getHeight()) / 2);
        }

        helpStage.show();
    }

    /**
     * Muestra u oculta el indicador de carga.
     *
     * @param cargando true para mostrar el indicador, false para ocultarlo.
     */
    private void setCargando(boolean cargando) {
        if (loadingBox != null) {
            loadingBox.setVisible(cargando);
            loadingBox.setManaged(cargando);
        }
        if (contenedorTarjetas != null) {
            // Opcional: ocultar el contenido mientras carga para que sea más limpio
            // contenedorTarjetas.setVisible(!cargando);
        }

        // Deshabilitar controles mientras carga
        if (btnSincronizar != null)
            btnSincronizar.setDisable(cargando);
        if (txtBuscar != null)
            txtBuscar.setDisable(cargando);
        if (comboCasa != null)
            comboCasa.setDisable(cargando);
        if (comboEstado != null)
            comboEstado.setDisable(cargando);
        if (txtPatronus != null)
            txtPatronus.setDisable(cargando);
    }

    /**
     * Extrae todas las casas/instituciones únicas de masterData y las añade al
     * ComboBox.
     */
    private void actualizarComboCasas() {
        if (comboCasa == null)
            return;

        String currentSelection = comboCasa.getValue();

        // Usar un Set para evitar duplicados y ordenar alfabéticamente
        Set<String> casas = new TreeSet<>();
        boolean hasNone = false;
        boolean hasUnknownExplicit = false;

        for (Personaje p : masterData) {
            String rawCasa = p.getCasa();
            if (rawCasa == null || rawCasa.trim().isEmpty()) {
                hasNone = true;
                continue;
            }

            // Separadores comunes: " or ", " y ", " / ", ",", " & "
            String[] parts = rawCasa.split("(?i)\\s+or\\s+|\\s+y\\s+|/|,|\\s*&\\s*");

            for (String part : parts) {
                String cleanCasa = part.trim();
                // Limpiar: truncar en el primer paréntesis o corchete
                int posParen = cleanCasa.indexOf('(');
                int posBracket = cleanCasa.indexOf('[');
                int splitPos = -1;

                if (posParen != -1 && posBracket != -1)
                    splitPos = Math.min(posParen, posBracket);
                else if (posParen != -1)
                    splitPos = posParen;
                else if (posBracket != -1)
                    splitPos = posBracket;

                if (splitPos != -1) {
                    cleanCasa = cleanCasa.substring(0, splitPos).trim();
                }

                if (!cleanCasa.isEmpty()) {
                    String lower = cleanCasa.toLowerCase();
                    if (lower.equals("unknown") || lower.equals("desconocido")) {
                        hasUnknownExplicit = true;
                    } else {
                        casas.add(cleanCasa);
                    }
                }
            }
        }

        final boolean finalHasNone = hasNone;
        final boolean finalHasUnknown = hasUnknownExplicit;
        String unknownLabel = App.getBundle().getString("combo.house.unknown");
        String noneLabel = App.getBundle().getString("combo.house.none");

        Platform.runLater(() -> {
            comboCasa.getItems().clear();
            comboCasa.getItems().addAll(casas);
            if (finalHasUnknown) {
                comboCasa.getItems().add(unknownLabel);
            }
            if (finalHasNone) {
                comboCasa.getItems().add(noneLabel);
            }
            // Save houses to shared state
            App.setAvailableHouses(new java.util.ArrayList<>(comboCasa.getItems()));

            // Intentar restaurar la selección previa
            if (currentSelection != null && (casas.contains(currentSelection) || currentSelection.equals(unknownLabel)
                    || currentSelection.equals(noneLabel))) {
                comboCasa.setValue(currentSelection);
            }
        });
    }
}
