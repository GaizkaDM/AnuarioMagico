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
    private Button btnImportarAPI;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        comboCasa.getItems().addAll("Gryffindor", "Slytherin", "Ravenclaw", "Hufflepuff");
        comboEstado.getItems().addAll("Vivo", "Muerto", "Fallecido");

        // Cargar datos automáticamente desde la API al inicio
        importarDesdeAPI();

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

        // Importar desde API (botón manual por si acaso)
        btnImportarAPI.setOnAction(e -> importarDesdeAPI());
    }

    /**
     * Applies search, house and status filters to the full list.
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
     * Updates the card view for the current page.
     */
    private void actualizarPagina() {
        contenedorTarjetas.getChildren().clear();

        int total = listaFiltrada.size();
        if (total == 0) {
            lblPagina.setText("Página 0 de 0");
            btnPaginaAnterior.setDisable(true);
            btnPaginaSiguiente.setDisable(true);
            // Solo mostrar mensaje si no se está cargando
            if (!btnImportarAPI.isDisabled()) {
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
                        "-fx-border-width: 1;");

        ImageView img = new ImageView();
        img.setFitWidth(160);
        img.setPreserveRatio(true);

        try {
            if (p.getImagenUrl() != null && !p.getImagenUrl().isEmpty()) {
                System.out.println("DEBUG: Cargando imagen para " + p.getNombre() + ": " + p.getImagenUrl());
                Image image = new Image(p.getImagenUrl(), true);
                image.errorProperty().addListener((obs, err, hasErr) -> {
                    if (hasErr) {
                        System.err.println(
                                "DEBUG: Error cargando imagen para " + p.getNombre() + ": " + image.getException());
                    }
                });
                img.setImage(image);
            } else {
                System.out.println("DEBUG: URL nula para " + p.getNombre());
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Excepción cargando imagen: " + e.getMessage());
            e.printStackTrace();
        }

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
                    btnImportarAPI.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusBar.setText("Error al cargar datos.");
                    btnImportarAPI.setDisable(false);
                });
            }
        }).start();
    }
}
