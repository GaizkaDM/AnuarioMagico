package org.GaizkaFrost.controllers;

import org.GaizkaFrost.models.Personaje;
import org.GaizkaFrost.services.HarryPotterAPI;
import org.GaizkaFrost.App;
import org.GaizkaFrost.services.ReportService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Controlador para la vista de detalle de un personaje.
 * Muestra información extendida y permite marcar como favorito.
 *
 * @author Gaizka
 * @version 1.0
 * @since 2025-12-14
 */
/**
 * Controlador de la vista de detalles.
 * Muestra la información completa de un personaje, incluyendo imagen, ficha
 * biográfica y opciones de exportación.
 * 
 * @author Diego
 * @author Gaizka
 * @author Xiker
 */
public class DetailController {

    @FXML
    private Button btnVolver;
    @FXML
    private Button btnFavorite;
    @FXML
    private Label lblNombre;
    @FXML
    private ImageView imgGrande;
    @FXML
    private Button btnGenerarPDFDetail;
    @FXML
    private Button btnEditar;
    @FXML
    private Button btnEliminar;

    @FXML
    private Label lblBorn;
    @FXML
    private Label lblDied;
    @FXML
    private Label lblGender;
    @FXML
    private Label lblSpecies;
    @FXML
    private Label lblAnimagus;
    @FXML
    private Label lblNationality;
    @FXML
    private Label lblHouse;
    @FXML
    private Label lblPatronus;

    @FXML
    private Label lblAlias;
    @FXML
    private Label lblTitles;
    @FXML
    private Label lblWand;
    @FXML
    private javafx.scene.control.ScrollPane detailScrollPane;

    private Personaje currentPersonaje;

    /**
     * Inicializa el controlador configuando los eventos de los botones.
     */
    @FXML
    public void initialize() {
        btnVolver.setOnAction(event -> {
            try {
                App.setRoot("Main_view", "Anuario Hogwarts");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        btnFavorite.setOnAction(event -> toggleFavorite());

        // Botones de CRUD: solo visibles si hay sesión activa
        if (btnEditar != null) {
            btnEditar.setOnAction(event -> editarPersonaje());
        }

        if (btnEliminar != null) {
            btnEliminar.setOnAction(event -> eliminarPersonaje());
        }

        if (btnGenerarPDFDetail != null) {
            btnGenerarPDFDetail.setOnAction(event -> {
                System.out.println("DEBUG: Button PDF clicked in DetailController");
                try {
                    if (currentPersonaje != null) {
                        ReportService.generateCharacterReport(currentPersonaje,
                                (javafx.stage.Stage) btnGenerarPDFDetail.getScene().getWindow());
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    mostrarAlerta("Error", "No se pudo generar el reporte.\n" + t.getMessage());
                }
            });
        }

        // Aumentar velocidad de desplazamiento
        if (detailScrollPane != null) {
            detailScrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
                if (event.getDeltaY() != 0) {
                    double delta = event.getDeltaY() * 3.0; // 3x más rápido
                    double height = detailScrollPane.getContent().getBoundsInLocal().getHeight();
                    double vValue = detailScrollPane.getVvalue();
                    // Prevenir división por cero
                    if (height > 0) {
                        detailScrollPane.setVvalue(vValue + -delta / height);
                        event.consume();
                    }
                }
            });
        }
    }

    /**
     * Alterna el estado de favorito del personaje actual llamando a la API.
     * Actualiza la interfaz según el resultado.
     */
    private void toggleFavorite() {
        if (currentPersonaje == null) {
            System.out.println("DEBUG: currentPersonaje is null");
            return;
        }

        System.out.println("DEBUG: Toggling favorite for ID: " + currentPersonaje.getApiId());

        try {
            boolean success = HarryPotterAPI.toggleFavorite(currentPersonaje.getApiId());
            System.out.println("DEBUG: API call success? " + success);

            if (success) {
                boolean oldState = currentPersonaje.isFavorite();
                currentPersonaje.setFavorite(!oldState);
                System.out.println("DEBUG: Toggled state from " + oldState + " to " + !oldState);
                updateFavoriteUI();
            } else {
                System.out.println("DEBUG: Request failed with non-200 code");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in toggleFavorite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setDarkMode(boolean isDarkMode) {
        javafx.application.Platform.runLater(() -> {
            if (btnVolver.getScene() == null)
                return;
            javafx.scene.Parent root = btnVolver.getScene().getRoot();
            root.getStylesheets().clear();
            if (isDarkMode) {
                root.getStylesheets()
                        .add(getClass().getResource("/styles/estilos_detalles_ravenclaw.css").toExternalForm());
            } else {
                root.getStylesheets().add(getClass().getResource("/styles/estilos_detalles.css").toExternalForm());
            }
        });
    }

    /**
     * Actualiza la apariencia del botón de favoritos según el estado del personaje.
     */
    private void updateFavoriteUI() {
        if (currentPersonaje.isFavorite()) {
            btnFavorite.setText(App.getBundle().getString("detail.favorite.remove"));
            btnFavorite.setStyle(
                    "-fx-font-size: 14; -fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
        } else {
            btnFavorite.setText(App.getBundle().getString("detail.favorite.add"));
            btnFavorite.setStyle("-fx-font-size: 14; -fx-background-color: #e0e0e0; -fx-text-fill: black;");
        }
    }

    /**
     * Recibe un objeto Personaje y rellena los campos de la interfaz con sus datos.
     *
     * @param p El personaje a mostrar.
     */
    public void setPersonaje(Personaje p) {
        this.currentPersonaje = p;
        updateFavoriteUI();

        // Controlar visibilidad de botones CRUD según autenticación
        boolean loggedIn = HarryPotterAPI.isLoggedIn();
        if (btnEditar != null) {
            btnEditar.setVisible(loggedIn);
            btnEditar.setManaged(loggedIn);
        }
        if (btnEliminar != null) {
            btnEliminar.setVisible(loggedIn);
            btnEliminar.setManaged(loggedIn);
        }

        lblNombre.setText(p.getNombre());

        if (p.getImagenUrl() != null && !p.getImagenUrl().isEmpty()) {
            try {
                imgGrande.setImage(new Image(p.getImagenUrl(), true));
            } catch (Exception ignored) {
            }
        }

        lblBorn.setText(nullToDash(p.getBorn()));
        lblDied.setText(nullToDash(p.getDied()));
        lblGender.setText(nullToDash(p.getGender()));
        lblSpecies.setText(nullToDash(p.getSpecies()));
        lblAnimagus.setText(nullToDash(p.getAnimagus()));
        lblNationality.setText(nullToDash(p.getNationality()));
        lblHouse.setText(nullToDash(p.getCasa()));
        lblPatronus.setText(nullToDash(p.getPatronus()));

        lblAlias.setText(nullToDash(p.getAlias()));
        lblTitles.setText(nullToDash(p.getTitles()));
        lblWand.setText(nullToDash(p.getWand()));
    }

    /**
     * Convierte valores nulos o vacíos en un guion "-" para su visualización.
     *
     * @param s La cadena a verificar.
     * @return La cadena original o "-".
     */
    private String nullToDash(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    /**
     * Elimina el personaje actual tras confirmar con el usuario.
     * Requiere sesión activa.
     */
    private void eliminarPersonaje() {
        if (!HarryPotterAPI.isLoggedIn()) {
            mostrarAlerta(App.getBundle().getString("error.title"), "Unauthorized action."); // Need key, assuming
                                                                                             // generic error title
            return;
        }

        if (currentPersonaje == null) {
            mostrarAlerta(App.getBundle().getString("error.title"), "No character selected.");
            return;
        }

        // Confirmación
        javafx.scene.control.Alert confirmacion = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(App.getBundle().getString("detail.delete.confirm.title"));
        confirmacion.setHeaderText(App.getBundle().getString("detail.delete.confirm.header"));
        confirmacion.setContentText(MessageFormat.format(App.getBundle().getString("detail.delete.confirm.content"),
                currentPersonaje.getNombre()));

        if (confirmacion.showAndWait()
                .orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK) {
            new Thread(() -> {
                try {
                    boolean success = HarryPotterAPI.deleteCharacter(currentPersonaje.getApiId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            mostrarAlerta("Info", App.getBundle().getString("detail.delete.success"));
                            // Volver a la vista principal
                            try {
                                App.setRoot("Main_view", "Anuario Hogwarts");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            mostrarAlerta(App.getBundle().getString("error.title"),
                                    App.getBundle().getString("detail.delete.error"));
                        }
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        mostrarAlerta(App.getBundle().getString("error.title"), "Error: " + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Abre un formulario para editar el personaje actual.
     * Requiere sesión activa.
     */
    private void editarPersonaje() {
        if (!HarryPotterAPI.isLoggedIn()) {
            mostrarAlerta(App.getBundle().getString("error.title"), "Unauthorized.");
            return;
        }

        if (currentPersonaje == null) {
            mostrarAlerta(App.getBundle().getString("error.title"), "No character selected.");
            return;
        }

        // OPEN EDIT FORM
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/Edit_view.fxml"));
            javafx.scene.Parent root = loader.load();

            EditController controller = loader.getController();
            controller.setPersonaje(currentPersonaje);

            // Apply theme
            App.applyTheme(root, "Edit_view");

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Editar Personaje - " + currentPersonaje.getNombre());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 700);

            stage.setScene(scene);
            stage.showAndWait();

            // Refresh current view details if changed
            setPersonaje(currentPersonaje);

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(App.getBundle().getString("error.title"), App.getBundle().getString("detail.edit.error"));
        }
    }

    /**
     * Muestra una alerta informativa al usuario.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}