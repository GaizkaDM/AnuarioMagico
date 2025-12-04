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
 * Main controller: shows characters as cards with pagination.
 */
public class Controlador implements Initializable {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> comboCasa;
    @FXML private ComboBox<String> comboEstado;
    @FXML private Button btnLimpiar;
    @FXML private Button btnImportarAPI;

    @FXML private FlowPane contenedorTarjetas;

    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;
    @FXML private Label lblPagina;
    @FXML private Label statusBar;

    private final ObservableList<Personaje> masterData = FXCollections.observableArrayList();
    private List<Personaje> listaFiltrada = new ArrayList<>();

    private int paginaActual = 0;
    private static final int PERSONAJES_POR_PAGINA = 20;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        comboCasa.getItems().addAll("Gryffindor", "Slytherin", "Ravenclaw", "Hufflepuff");
        comboEstado.getItems().addAll("Vivo", "Muerto", "Fallecido");

        // Datos de prueba iniciales (puedes quitarlo si solo quieres API/BD)
        initMockData();

        listaFiltrada = new ArrayList<>(masterData);
        actualizarPagina();

        // Filtros
        txtBuscar.textProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboCasa.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltros());

        btnLimpiar.setOnAction(e -> {
            txtBuscar.clear();
            comboCasa.getSelectionModel().clearSelection();
            comboEstado.getSelectionModel().clearSelection();
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

        // Importar desde API (si falla la conexión, simplemente no remplaza los datos)
        btnImportarAPI.setOnAction(e -> importarDesdeAPI());
    }

    /**
     * Applies search, house and status filters to the full list.
     */
    private void aplicarFiltros() {
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().toLowerCase().trim() : "";
        String casa = comboCasa.getValue();
        String estado = comboEstado.getValue();

        List<Personaje> filtrados = new ArrayList<>();

        for (Personaje p : masterData) {
            boolean coincideTexto =
                    texto.isEmpty()
                            || p.getNombre().toLowerCase().contains(texto)
                            || (p.getCasa() != null && p.getCasa().toLowerCase().contains(texto))
                            || (p.getPatronus() != null && p.getPatronus().toLowerCase().contains(texto));

            boolean coincideCasa =
                    casa == null || casa.isEmpty()
                            || (p.getCasa() != null && p.getCasa().equalsIgnoreCase(casa));

            boolean coincideEstado =
                    estado == null || estado.isEmpty()
                            || (p.getEstado() != null && p.getEstado().equalsIgnoreCase(estado));

            if (coincideTexto && coincideCasa && coincideEstado) {
                filtrados.add(p);
            }
        }

        listaFiltrada = filtrados;
        paginaActual = 0;
        actualizarPagina();
    }

    /**
     * Updates the card view for the current page.
     */
    private void actualizarPagina() {
        contenedorTarjetas.getChildren().clear();

        int total = listaFiltrada.size();
        if (total == 0) {
            lblPagina.setText("Página 0 de 0");
            btnPaginaAnterior.setDisable(true);
            btnPaginaSiguiente.setDisable(true);
            statusBar.setText("No se han encontrado personajes.");
            return;
        }

        int totalPaginas = (int) Math.ceil(total / (double) PERSONAJES_POR_PAGINA);

        if (paginaActual < 0) paginaActual = 0;
        if (paginaActual >= totalPaginas) paginaActual = totalPaginas - 1;

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
     * Creates a visual card for a character.
     *
     * @param p character to display
     * @return VBox node representing the card
     */
    private VBox crearTarjeta(Personaje p) {
        VBox tarjeta = new VBox();
        tarjeta.setSpacing(6);
        tarjeta.setPrefWidth(200);
        tarjeta.setStyle(
                "-fx-padding: 10;" +
                "-fx-background-color: rgba(255,255,255,0.9);" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-border-color: #cccccc;" +
                "-fx-border-width: 1;"
        );

        ImageView img = new ImageView();
        img.setFitWidth(160);
        img.setPreserveRatio(true);

        try {
            if (p.getImagenUrl() != null && !p.getImagenUrl().isEmpty()) {
                img.setImage(new Image(p.getImagenUrl(), true));
            }
        } catch (Exception ignored) {}

        Label lblNombre = new Label(p.getNombre());
        lblNombre.setStyle("-fx-font-weight: bold;");

        Label lblCasa = new Label("Casa: " + (p.getCasa() == null ? "-" : p.getCasa()));
        Label lblEstado = new Label("Estado: " + (p.getEstado() == null ? "-" : p.getEstado()));
        Label lblPatronus = new Label("Patronus: " + (p.getPatronus() == null ? "-" : p.getPatronus()));

        Button btnDetalles = new Button("Ver detalles");
        btnDetalles.setOnAction(e -> abrirDetalles(p));

        tarjeta.getChildren().addAll(img, lblNombre, lblCasa, lblEstado, lblPatronus, btnDetalles);

        return tarjeta;
    }

    /**
     * Opens the detail view for the given character.
     *
     * @param p character to show
     */
    private void abrirDetalles(Personaje p) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/Detail_view.fxml"));
            Parent root = loader.load();

            DetailController controller = loader.getController();
            controller.setPersonaje(p);

            Stage stage = (Stage) contenedorTarjetas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(p.getNombre());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Imports characters from the Harry Potter API in a background thread.
     */
    private void importarDesdeAPI() {
        btnImportarAPI.setDisable(true);
        statusBar.setText("Importando personajes desde la API...");

        new Thread(() -> {
            try {
                List<Personaje> personajesAPI = HarryPotterAPI.fetchCharacters();

                Platform.runLater(() -> {
                    masterData.setAll(personajesAPI);
                    listaFiltrada = new ArrayList<>(masterData);
                    paginaActual = 0;
                    actualizarPagina();
                    statusBar.setText("Personajes importados desde la API.");
                    btnImportarAPI.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusBar.setText("Error al importar datos desde la API.");
                    btnImportarAPI.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Sample data for testing before using the real API or DB.
     */
    private void initMockData() {
        Personaje p1 = new Personaje();
        p1.setNombre("Harry Potter");
        p1.setCasa("Gryffindor");
        p1.setEstado("Vivo");
        p1.setPatronus("Ciervo");
        p1.setImagenUrl("https://ik.imagekit.io/hpapi/harry.jpg");

        Personaje p2 = new Personaje();
        p2.setNombre("Hermione Granger");
        p2.setCasa("Gryffindor");
        p2.setEstado("Vivo");
        p2.setPatronus("Nutria");
        p2.setImagenUrl("https://ik.imagekit.io/hpapi/hermione.jpg");

        Personaje p3 = new Personaje();
        p3.setNombre("Draco Malfoy");
        p3.setCasa("Slytherin");
        p3.setEstado("Vivo");
        p3.setPatronus("");
        p3.setImagenUrl("https://ik.imagekit.io/hpapi/draco.jpg");

        masterData.addAll(p1, p2, p3);
    }
}
