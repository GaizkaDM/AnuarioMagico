package org.GaizkaFrost.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.text.MessageFormat;

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

    private final ObservableList<Personaje> masterData = FXCollections.observableArrayList();
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
    private MenuItem menuManual;
    @FXML
    private MenuItem menuLangEn;
    @FXML
    private MenuItem menuLangEs;
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

        // House and Status combos will be populated dynamically or locally
        comboEstado.getItems().addAll(
                App.getBundle().getString("combo.status.alive"),
                App.getBundle().getString("combo.status.deceased"));

        // Intentar sincronizar datos de la nube al inicio (Pull)
        setCargando(true); // Mostrar spinner mientras se intenta el pull
        new Thread(() -> {
            System.out.println("Intentando sincronización inicial (Pull)...");
            boolean synced = HarryPotterAPI.syncPull();
            if (synced) {
                System.out.println("Sincronización completada. Recargando datos locales...");
                Platform.runLater(this::sincronizar);
            } else {
                System.out.println("No se pudo sincronizar (¿Offline?). Cargando local...");
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

        // Listener para el menú de tema
        menuTemaOscuro.setSelected(App.isDarkMode());
        menuTemaOscuro.setOnAction(e -> toggleTheme());

        // Listener para el menú de idioma
        if (menuLangEn != null)
            menuLangEn.setOnAction(e -> changeLanguage(java.util.Locale.ENGLISH));
        if (menuLangEs != null)
            menuLangEs.setOnAction(e -> changeLanguage(java.util.Locale.forLanguageTag("es")));

        btnLimpiar.setOnAction(e -> {
            txtBuscar.clear();
            comboCasa.getSelectionModel().clearSelection();
            comboEstado.getSelectionModel().clearSelection();
            txtPatronus.clear();
            checkFavoritos.setSelected(false);
            aplicarFiltros();
        });

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
                System.out.println("DEBUG: Button PDF clicked in MainController");
                try {
                    ReportService.generateListReport(listaFiltrada, (Stage) btnGenerarPDF.getScene().getWindow());
                } catch (Throwable t) {
                    t.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
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
            e.printStackTrace();
        }
    }

    /**
     * Maneja el cambio manual de página desde el TextField.
     */
    private void manejarCambioPagina(TextField source) {
        try {
            int targetPage = Integer.parseInt(source.getText());
            int total = listaFiltrada.size();
            int totalPaginas = (int) Math.ceil(total / (double) PERSONAJES_POR_PAGINA);
            if (totalPaginas == 0)
                totalPaginas = 1;

            if (targetPage < 1)
                targetPage = 1;
            if (targetPage > totalPaginas)
                targetPage = totalPaginas;

            paginaActual = targetPage - 1;
            actualizarPagina();
        } catch (NumberFormatException e) {
            // Restore previous valid page
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

            Stage stage = new Stage();
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
            e.printStackTrace();
        }
    }

    @FXML
    private javafx.scene.layout.BorderPane root; // Injected root

    // START DARK MODE LOGIC
    @FXML
    private javafx.scene.control.CheckMenuItem menuTemaOscuro;

    private void toggleTheme() {
        boolean newMode = menuTemaOscuro.isSelected();
        App.setDarkMode(newMode);

        // Apply to current view immediately
        if (root != null) {
            App.applyTheme(root, "Main_view");
        } else if (contenedorTarjetas.getScene() != null) {
            // Fallback if root not injected for some reason
            App.applyTheme(contenedorTarjetas.getScene().getRoot(), "Main_view");
        }
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
     * Aplica los filtros (búsqueda, casa, estado, favoritos) a la lista maestra de
     * personajes.
     * Actualiza `listaFiltrada` y reinicia la paginación.
     */
    private void aplicarFiltros() {
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
        paginaActual = 0;
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
            e.printStackTrace();
        }

        Label lblNombre = new Label(p.getNombre());
        lblNombre.getStyleClass().add("card-title");

        Label lblCasa = new Label(
                App.getBundle().getString("detail.house") + " " + (p.getCasa() == null ? "-" : p.getCasa()));
        lblCasa.getStyleClass().add("card-meta");

        Label lblEstado = new Label(
                App.getBundle().getString("edit.label.status") + " " + (p.getEstado() == null ? "-" : p.getEstado()));
        lblEstado.getStyleClass().add("card-meta");

        Label lblPatronus = new Label(
                App.getBundle().getString("detail.patronus") + " " + (p.getPatronus() == null ? "-" : p.getPatronus()));
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
        return tarjeta;
    }

    /**
     * Abre la vista detallada para el personaje seleccionado.
     *
     * @param p El personaje cuyos detalles se mostrarán.
     */
    private void abrirDetalles(Personaje p) {
        // Guardar estado
        savedSearch = txtBuscar.getText();
        savedHouse = comboCasa.getValue();
        savedStatus = comboEstado.getValue();
        savedFavorite = checkFavoritos.isSelected();

        try {
            DetailController controller = App.setRootAndGetController("Detail_view", p.getNombre());
            controller.setPersonaje(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Abre el formulario para añadir un nuevo personaje.
     */
    private void abrirFormularioAnadir() {
        if (!isLoggedIn)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Edit_view.fxml"));
            Parent root = loader.load();

            EditController controller = loader.getController();
            controller.setPersonaje(null); // Modo añadir

            App.applyTheme(root, "Edit_view");

            Stage stage = new Stage();
            stage.setTitle("Añadir Personaje");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root, 900, 700);
            stage.setScene(scene);
            stage.showAndWait(); // Esperar a que cierre

            // Recargar datos para mostrar el nuevo personaje
            sincronizar();

        } catch (IOException e) {
            e.printStackTrace();
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
                List<Personaje> localData = HarryPotterAPI.fetchCharacters();

                Platform.runLater(() -> {
                    masterData.setAll(localData);
                    actualizarComboCasas();
                    aplicarFiltros();
                    actualizarPagina();
                    setCargando(false); // Desbloquear UI inmediatamente
                    statusBar.setText("Datos locales cargados. Buscando actualizaciones...");
                });

                // 2. Sincronización en segundo plano (Lento)
                boolean pullSuccess = HarryPotterAPI.syncPull();

                if (pullSuccess) {
                    boolean downloading = true;
                    int noStatusCount = 0;

                    while (downloading) {
                        try {
                            com.google.gson.JsonObject status = HarryPotterAPI.getImageSyncStatus();

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
                    Platform.runLater(() -> {
                        masterData.setAll(freshData);
                        actualizarComboCasas();
                        aplicarFiltros();
                        actualizarPagina();
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
                e.printStackTrace();
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
        helpStage.setTitle(App.getBundle().getString("help.title"));

        VBox content = new VBox(20); // Más espacio entre secciones
        content.setPadding(new javafx.geometry.Insets(25));
        content.setStyle("-fx-background-color: #fafafa;");

        // Título Principal
        Label title = new Label(App.getBundle().getString("help.header"));
        title.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #5a3e1b; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(javafx.geometry.Pos.CENTER);

        // Intro
        Label intro = new Label(App.getBundle().getString("help.intro"));
        intro.setWrapText(true);
        intro.setStyle("-fx-font-size: 15px; -fx-padding: 0 0 10 0;");

        // Sección 1: La Pantalla Principal
        Label sec1 = new Label(App.getBundle().getString("help.sec1.title"));
        sec1.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text1 = new Label(App.getBundle().getString("help.sec1.text"));
        text1.setWrapText(true);
        text1.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 2: Cómo Buscar
        Label sec2 = new Label(App.getBundle().getString("help.sec2.title"));
        sec2.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text2 = new Label(App.getBundle().getString("help.sec2.text"));
        text2.setWrapText(true);
        text2.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 3: Ver Detalles y Fotos
        Label sec3 = new Label(App.getBundle().getString("help.sec3.title"));
        sec3.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text3 = new Label(App.getBundle().getString("help.sec3.text"));
        text3.setWrapText(true);
        text3.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 4: Favoritos
        Label sec4 = new Label(App.getBundle().getString("help.sec4.title"));
        sec4.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text4 = new Label(App.getBundle().getString("help.sec4.text"));
        text4.setWrapText(true);
        text4.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 5: Sincronización (La Nube)
        Label sec5 = new Label(App.getBundle().getString("help.sec5.title"));
        sec5.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text5 = new Label(App.getBundle().getString("help.sec5.text"));
        text5.setWrapText(true);
        text5.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 6: Preguntas (FAQ)
        Label sec6 = new Label(App.getBundle().getString("help.sec6.title"));
        sec6.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text6 = new Label(App.getBundle().getString("help.sec6.text"));
        text6.setWrapText(true);
        text6.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        content.getChildren().addAll(title, intro, sec1, text1, sec2, text2, sec3, text3, sec4, text4, sec5, text5,
                sec6, text6);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #fafafa; -fx-border-color: transparent;");

        Scene scene = new Scene(scroll, 700, 800);
        helpStage.setScene(scene);
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
            // Intentar restaurar la selección previa
            if (currentSelection != null && (casas.contains(currentSelection) || currentSelection.equals(unknownLabel)
                    || currentSelection.equals(noneLabel))) {
                comboCasa.setValue(currentSelection);
            }
        });
    }
}
