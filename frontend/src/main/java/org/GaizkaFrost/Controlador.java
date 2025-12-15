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
    private TextField txtPagina;
    @FXML
    private Label lblTotalPaginas;
    @FXML
    private Label statusBar;

    private final ObservableList<Personaje> masterData = FXCollections.observableArrayList();
    private List<Personaje> listaFiltrada = new ArrayList<>();

    private int paginaActual = 0;
    private static int savedPage = -1;
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

            // Configurar menú de ayuda
            if (menuManual != null) {
                menuManual.setOnAction(e -> mostrarAyuda());
            }
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

        // Filtros
        txtBuscar.textProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboCasa.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        checkFavoritos.selectedProperty().addListener((obs, o, n) -> aplicarFiltros());

        // Restaurar estado de filtros si existe
        if (savedSearch != null)
            txtBuscar.setText(savedSearch);
        if (savedHouse != null)
            comboCasa.setValue(savedHouse);
        if (savedStatus != null)
            comboEstado.setValue(savedStatus);
        checkFavoritos.setSelected(savedFavorite);

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

        // Listener para el campo de número de página
        txtPagina.setOnAction(e -> manejarCambioPagina());
        txtPagina.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Si pierde el foco
                manejarCambioPagina();
            }
        });

        btnSincronizar.setOnAction(e -> sincronizar());
    }

    /**
     * Maneja el cambio manual de página desde el TextField.
     */
    private void manejarCambioPagina() {
        try {
            int targetPage = Integer.parseInt(txtPagina.getText());
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
        } catch (NumberFormatException ex) {
            // Si el usuario escribe texto inválido, restaurar valor actual
            txtPagina.setText(String.valueOf(paginaActual + 1));
        }
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

            // Modal y no redimensionable
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            // Tamaño fijo un poco más grande
            stage.setScene(new Scene(root, 400, 550));
            stage.showAndWait();
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
            txtPagina.setText("1");
            lblTotalPaginas.setText("de 1");
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

        txtPagina.setText(String.valueOf(paginaActual + 1));
        lblTotalPaginas.setText("de " + totalPaginas);
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
        // Guardar estado
        savedPage = paginaActual;
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
                    // Aplicar filtros actuales a los nuevos datos (esto repuebla listaFiltrada)
                    aplicarFiltros();

                    // Restaurar página si corresponde (aplicarFiltros la habrá reseteado a 0)
                    if (savedPage != -1) {
                        paginaActual = savedPage;
                        savedPage = -1;
                    }
                    // Si no hay pagina guardada, aplicarFiltros ya la dejó en 0, correcto.

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

    /**
     * Muestra el manual de usuario en una nueva ventana.
     * Guía detallada "paso a paso" para usuarios principiantes.
     */
    private void mostrarAyuda() {
        Stage helpStage = new Stage();
        helpStage.setTitle("Manual de Usuario - Anuario Mágico (Guía Detallada)");

        VBox content = new VBox(20); // Más espacio entre secciones
        content.setPadding(new javafx.geometry.Insets(25));
        content.setStyle("-fx-background-color: #fafafa;");

        // Título Principal
        Label title = new Label("📖 Guía de Uso del Anuario Mágico");
        title.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #5a3e1b; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(javafx.geometry.Pos.CENTER);

        // Intro
        Label intro = new Label(
                "¡Hola! Bienvenido a tu enciclopedia mágica. No te preocupes si no eres un experto en ordenadores, esta guía te explicará todo paso a paso.");
        intro.setWrapText(true);
        intro.setStyle("-fx-font-size: 15px; -fx-padding: 0 0 10 0;");

        // Sección 1: La Pantalla Principal
        Label sec1 = new Label("1. La Pantalla Principal");
        sec1.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text1 = new Label(
                "Lo primero que ves son las **Tarjetas de Personajes**.\n" +
                        "• Cada recuadro es un personaje (Harry, Hermione, etc.).\n" +
                        "• **Para ver más:** Usa la rueda de tu ratón para bajar y subir, o arrastra la barra gris de la derecha.\n"
                        +
                        "• **Páginas:** Abajo del todo hay botones 'Anterior' y 'Siguiente'. Si no encuentras a alguien, ¡prueba en la siguiente página!");
        text1.setWrapText(true);
        text1.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 2: Cómo Buscar
        Label sec2 = new Label("2. ¿Cómo busco a alguien?");
        sec2.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text2 = new Label(
                "Arriba a la izquierda tienes varias herramientas:\n" +
                        "• **Casilla 'Buscar':** Haz clic ahí y escribe un nombre (ej. 'Potter'). La lista cambiará sola mientras escribes.\n"
                        +
                        "• **Menú 'Casa':** Pincha y elige una casa (ej. 'Gryffindor') para ver solo a sus miembros.\n"
                        +
                        "• **Menú 'Estado':** Elige 'Vivo' o 'Fallecido' si quieres filtrar así.\n" +
                        "• **Botón Limpiar:** Si te lías con tanto filtro, pulsa este botón para borrar todo y ver la lista completa otra vez.");
        text2.setWrapText(true);
        text2.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 3: Ver Detalles y Fotos
        Label sec3 = new Label("3. Ver Detalles y Fotos");
        sec3.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text3 = new Label(
                "¿Quieres saber más de un personaje?\n" +
                        "1. Busca su tarjeta en la lista.\n" +
                        "2. Pulsa el botón **'Ver detalles'** que tiene cada tarjeta.\n" +
                        "3. Se abrirá una pantalla nueva con su foto grande, varita, patronus y más datos.\n" +
                        "4. Para volver, pulsa el botón **'Volver'** arriba a la izquierda.");
        text3.setWrapText(true);
        text3.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 4: Favoritos
        Label sec4 = new Label("4. Guardar mis Favoritos");
        sec4.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text4 = new Label(
                "Para no perder de vista a tus preferidos:\n" +
                        "• Entra en los detalles de un personaje y pulsa el botón **'Corazón / Añadir a Favoritos'**.\n"
                        +
                        "• Luego, en la pantalla principal, marca la cajita **'Ver solo favoritos'** (a la izquierda) y solo saldrán ellos.");
        text4.setWrapText(true);
        text4.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 5: Sincronización (La Nube)
        Label sec5 = new Label("5. Botón Sincronizar (La Nube)");
        sec5.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text5 = new Label(
                "La aplicación guarda los datos en tu ordenador para que funcione aunque se vaya internet.\n" +
                        "• Si crees que faltan datos nuevos, pulsa el botón **'Sincronizar'**.\n" +
                        "• Espérate un poco a que termine la barra de carga.\n" +
                        "• **Nota:** Las imágenes se descargan la primera vez que las ves, así que si alguna no sale, espera unos segundos con internet conectado.");
        text5.setWrapText(true);
        text5.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 10;");

        // Sección 6: Preguntas (FAQ)
        Label sec6 = new Label("6. Preguntas Frecuentes");
        sec6.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: transparent transparent #d4af37 transparent; -fx-border-width: 0 0 2 0;");
        Label text6 = new Label(
                "• ¿Por qué algunos no tienen foto? No todos los magos se han hecho fotos para el anuario.\n" +
                        "• ¿Cómo arreglo un dato mal puesto? Solo los profesores (administradores) pueden cambiar datos.");
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
}