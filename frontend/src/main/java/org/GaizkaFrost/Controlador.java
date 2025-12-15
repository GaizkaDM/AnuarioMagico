package org.GaizkaFrost;

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

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Controlador principal: muestra los personajes como tarjetas con paginación y
 * filtros.
 * Gestiona la interacción principal del usuario con el anuario.
 *
 * @author GaizkaFrost
 * @version 1.0
 * @since 2025-12-14
 */
public class Controlador implements Initializable {

    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<String> comboCasa;
    @FXML
    private ComboBox<String> comboEstado;
    @FXML
    private CheckBox checkFavoritos;
    @FXML
    private Button btnLimpiar;
    @FXML
    private Button btnSincronizar;
    @FXML
    private Button btnGenerarPDF;

    @FXML
    private FlowPane contenedorTarjetas;

    @FXML
    private Button btnPaginaAnterior;
    @FXML
    private Button btnPaginaSiguiente;
    @FXML
    private Label lblPagina;
    @FXML
    private Label statusBar;

    private final ObservableList<Personaje> masterData = FXCollections.observableArrayList();
    private List<Personaje> listaFiltrada = new ArrayList<>();

    private int paginaActual = 0;
    private static final int PERSONAJES_POR_PAGINA = 20;

    @FXML
    private MenuItem menuLogin;
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
                lblUsuario.setText("Usuario: " + currentUser);
                statusBar.setText("Sesión iniciada como " + currentUser + ".");
            } else {
                lblUsuario.setText("Usuario: [Sesión activa]");
                statusBar.setText("Sesión activa.");
            }
        } else {
            // Configurar menú de inicio de sesión
            menuLogin.setOnAction(e -> mostrarLogin());
        }

        comboCasa.getItems().addAll("Gryffindor", "Slytherin", "Ravenclaw", "Hufflepuff");
        comboEstado.getItems().addAll("Vivo", "Fallecido");

        // Intentar sincronizar datos de la nube al inicio (Pull)
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

        // Cargar datos automáticamente desde la API al inicio (esto se llama arriba
        // tras sync, o aquí si quitamos la llamada directa)
        // Lo dejamos comentado porque lo llamamos en el Thread
        // importarDesdeAPI();

        // Filtros
        txtBuscar.textProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboCasa.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        checkFavoritos.selectedProperty().addListener((obs, o, n) -> aplicarFiltros());

        btnLimpiar.setOnAction(e -> {
            txtBuscar.clear();
            comboCasa.getSelectionModel().clearSelection();
            comboEstado.getSelectionModel().clearSelection();
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

        btnSincronizar.setOnAction(e -> sincronizar());
    }

    /**
     * Muestra la ventana modal de inicio de sesión o registro.
     */
    private void mostrarLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login_view.fxml"));
            Parent root = loader.load();

            LoginController loginCtrl = loader.getController();
            loginCtrl.setOnSuccessCallback(this::onLoginRealizado);

            Stage stage = new Stage();
            stage.setTitle("Login / Registro");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            menuLogin.setText("Cerrar Sesión");
            menuLogin.setOnAction(e -> cerrarSesion());
            lblUsuario.setText("Usuario: " + username);
            statusBar.setText("Sesión iniciada como " + username + ". Acceso completo habilitado.");
            // Aquí se activarían los botones de edición si existieran
        });
    }

    /**
     * Cierra la sesión del usuario actual y restaura el estado de la interfaz.
     */
    private void cerrarSesion() {
        isLoggedIn = false;
        currentUser = null;
        HarryPotterAPI.clearToken(); // Limpiar token de sesión
        menuLogin.setText("Iniciar Sesión");
        menuLogin.setOnAction(e -> mostrarLogin());
        lblUsuario.setText("");
        statusBar.setText("Sesión cerrada.");
        // Aquí se desactivarían los botones de edición
    }

    /**
     * Aplica los filtros (búsqueda, casa, estado, favoritos) a la lista maestra de
     * personajes.
     * Actualiza `listaFiltrada` y reinicia la paginación.
     */
    private void aplicarFiltros() {
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().toLowerCase().trim() : "";
        String casa = comboCasa.getValue();
        String estado = comboEstado.getValue();
        boolean soloFavoritos = checkFavoritos.isSelected();

        List<Personaje> filtrados = new ArrayList<>();

        for (Personaje p : masterData) {
            boolean coincideTexto = texto.isEmpty()
                    || p.getNombre().toLowerCase().contains(texto)
                    || (p.getCasa() != null && p.getCasa().toLowerCase().contains(texto))
                    || (p.getPatronus() != null && p.getPatronus().toLowerCase().contains(texto));

            boolean coincideCasa = casa == null || casa.isEmpty()
                    || (p.getCasa() != null && p.getCasa().equalsIgnoreCase(casa));

            boolean coincideEstado = estado == null || estado.isEmpty()
                    || (p.getEstado() != null && p.getEstado().equalsIgnoreCase(estado));

            boolean coincideFavorito = !soloFavoritos || p.isFavorite();

            if (coincideTexto && coincideCasa && coincideEstado && coincideFavorito) {
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
            lblPagina.setText("Página 0 de 0");
            btnPaginaAnterior.setDisable(true);
            btnPaginaSiguiente.setDisable(true);
            // Solo mostrar mensaje si no se está cargando
            if (!btnSincronizar.isDisabled()) {
                statusBar.setText("No se han encontrado personajes.");
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

        lblPagina.setText("Página " + (paginaActual + 1) + " de " + totalPaginas);
        btnPaginaAnterior.setDisable(paginaActual == 0);
        btnPaginaSiguiente.setDisable(paginaActual >= totalPaginas - 1);

        statusBar.setText("Mostrando " + pagina.size() + " de " + total + " personajes filtrados.");
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

        Label lblCasa = new Label("Casa: " + (p.getCasa() == null ? "-" : p.getCasa()));
        lblCasa.getStyleClass().add("card-meta");

        Label lblEstado = new Label("Estado: " + (p.getEstado() == null ? "-" : p.getEstado()));
        lblEstado.getStyleClass().add("card-meta");

        Label lblPatronus = new Label("Patronus: " + (p.getPatronus() == null ? "-" : p.getPatronus()));
        lblPatronus.getStyleClass().add("card-meta");

        Button btnDetalles = new Button("Ver detalles");
        btnDetalles.getStyleClass().add("card-button");
        btnDetalles.setOnAction(e -> abrirDetalles(p));

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
        try {
            DetailController controller = App.setRootAndGetController("Detail_view", p.getNombre());
            controller.setPersonaje(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Importa personajes desde la API de Harry Potter en un hilo en segundo plano.
     * Actualiza la interfaz gráfica una vez completada la carga.
     */
    private void sincronizar() {
        btnSincronizar.setDisable(true);
        statusBar.setText("Cargando personajes...");

        new Thread(() -> {
            try {
                List<Personaje> personajesAPI = HarryPotterAPI.fetchCharacters();

                Platform.runLater(() -> {
                    masterData.setAll(personajesAPI);
                    listaFiltrada = new ArrayList<>(masterData);
                    paginaActual = 0;
                    actualizarPagina();
                    statusBar.setText("Personajes cargados.");
                    btnSincronizar.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusBar.setText("Error al cargar datos.");
                    btnSincronizar.setDisable(false);
                });
            }
        }).start();
    }
}