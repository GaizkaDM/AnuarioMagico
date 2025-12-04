package org.GaizkaFrost;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class Controlador implements javafx.fxml.Initializable {

    @FXML
    private Button btnLimpiar;
    @FXML
    private Button btnVerDetalles;
    @FXML
    private TableColumn<Personaje, String> colCasa;
    @FXML
    private TableColumn<Personaje, String> colEstado;
    @FXML
    private TableColumn<Personaje, String> colImagen; // We might want to show an image view here, but for now string
                                                      // url or simple cell factory
    @FXML
    private TableColumn<Personaje, String> colNombre;
    @FXML
    private TableColumn<Personaje, String> colPatronus;
    @FXML
    private ComboBox<String> comboCasa;
    @FXML
    private ComboBox<String> comboEstado;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblVivos;
    @FXML
    private Label lblMuertos;
    @FXML
    private Label lblCasas;
    @FXML
    private Label lblInfoTabla;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private Button btnImportarAPI;
    @FXML
    private TableView<Personaje> tablaPersonajes;
    @FXML
    private TextField txtBuscar;

    private javafx.collections.ObservableList<Personaje> masterData = javafx.collections.FXCollections
            .observableArrayList();

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        // Setup columns
        colNombre.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nombre"));
        colCasa.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("casa"));
        colEstado.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("estado"));
        colPatronus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("patronus"));
        // colImagen could be a custom cell factory to show image, but let's stick to
        // simple property for now or skip
        colImagen.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("imagenUrl"));

        tablaPersonajes.setItems(masterData);
        updateCount();

        // Filters
        comboCasa.getItems().addAll("Gryffindor", "Slytherin", "Ravenclaw", "Hufflepuff");
        comboEstado.getItems().addAll("Vivo", "Fallecido");

        // Listeners
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> filter());
        comboCasa.valueProperty().addListener((obs, oldVal, newVal) -> filter());
        comboEstado.valueProperty().addListener((obs, oldVal, newVal) -> filter());

        btnLimpiar.setOnAction(e -> {
            txtBuscar.clear();
            comboCasa.setValue(null);
            comboEstado.setValue(null);
        });

        btnVerDetalles.setOnAction(e -> {
            Personaje selected = tablaPersonajes.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    showDetails(selected);
                } catch (java.io.IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        btnImportarAPI.setOnAction(e -> loadData());

        // Auto-load data on startup (since we have local persistence now)
        loadData();
    }

    private void loadData() {
        btnImportarAPI.setDisable(true);
        btnImportarAPI.setText("Cargando...");

        // Ejecutar en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                java.util.List<Personaje> personajes = HarryPotterAPI.fetchCharacters();

                // Actualizar la UI en el hilo de JavaFX
                javafx.application.Platform.runLater(() -> {
                    masterData.clear();
                    masterData.addAll(personajes);
                    tablaPersonajes.setItems(masterData);
                    updateCount();
                    btnImportarAPI.setText("Cargar Personajes");
                    btnImportarAPI.setDisable(false);
                    lblSubtitulo.setText("Cargados " + personajes.size() + " personajes");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    btnImportarAPI.setText("Error - Reintentar");
                    btnImportarAPI.setDisable(false);
                    lblSubtitulo.setText("Error al cargar: " + ex.getMessage());
                });
            }
        }).start();
    }

    /**
     * Helper method to toggle favorite status for a character via the API
     */
    public void toggleFavorite(Personaje personaje) {
        if (personaje == null || personaje.getId() == null) {
            return;
        }

        new Thread(() -> {
            try {
                // Toggle the state (we'll send the new state to backend)
                boolean newFavoriteState = !personaje.isFavorite();

                java.net.URL url = new java.net.URL(
                        "http://localhost:8000/characters/" + personaje.getId() + "/favorite");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // Send JSON body
                String jsonBody = "{\"is_favorite\": " + newFavoriteState + "}";
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    // Update the local state
                    javafx.application.Platform.runLater(() -> {
                        personaje.setFavorite(newFavoriteState);
                        tablaPersonajes.refresh(); // Refresh table to show updated data
                    });
                }
                conn.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void filter() {
        javafx.collections.transformation.FilteredList<Personaje> filtered = new javafx.collections.transformation.FilteredList<>(
                masterData, p -> true);

        filtered.setPredicate(personaje -> {
            String search = txtBuscar.getText().toLowerCase();
            String casa = comboCasa.getValue();
            String estado = comboEstado.getValue();

            boolean matchSearch = search.isEmpty() ||
                    personaje.getNombre().toLowerCase().contains(search) ||
                    personaje.getCasa().toLowerCase().contains(search);
            boolean matchCasa = casa == null || personaje.getCasa().equalsIgnoreCase(casa);
            boolean matchEstado = estado == null ||
                    (estado.equals("Vivo") && "Vivo".equalsIgnoreCase(personaje.getEstado())) ||
                    (estado.equals("Fallecido") && !"Vivo".equalsIgnoreCase(personaje.getEstado()));

            return matchSearch && matchCasa && matchEstado;
        });

        tablaPersonajes.setItems(filtered);
        updateCount();
    }

    private void updateCount() {
        int total = tablaPersonajes.getItems().size();
        long vivos = tablaPersonajes.getItems().stream()
                .filter(p -> "Vivo".equalsIgnoreCase(p.getEstado()))
                .count();
        long muertos = total - vivos;
        long casas = tablaPersonajes.getItems().stream()
                .map(Personaje::getCasa)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .count();

        lblTotal.setText(String.valueOf(total));
        lblVivos.setText(String.valueOf(vivos));
        lblMuertos.setText(String.valueOf(muertos));
        lblCasas.setText(String.valueOf(casas));
        lblInfoTabla.setText(total + " resultados encontrados");
    }

    private void showDetails(Personaje p) throws java.io.IOException {
        // We need to load the detail view and pass the character
        // Since App.setRoot just replaces the scene, we need a way to get the
        // controller of the new view
        // We will modify App to help us or do it manually here.
        // Let's do it manually here for simplicity of not changing App too much yet,
        // OR we can use a static holder in App or here.
        // Actually, let's use the FXMLLoader manually here to get the controller.

        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(App.class.getResource("/fxml/Detail_view.fxml"));
        javafx.scene.Parent root = loader.load();
        DetailController controller = loader.getController();
        controller.setPersonaje(p);

        // Get current stage from a node
        javafx.stage.Stage stage = (javafx.stage.Stage) btnVerDetalles.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

}
