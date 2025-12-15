package org.GaizkaFrost;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

/**
 * Controlador para la vista de detalle de un personaje.
 * Muestra información extendida y permite marcar como favorito.
 *
 * @author GaizkaFrost
 * @version 1.0
 * @since 2025-12-14
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
                System.out.println("Generar PDF solicitado para: "
                        + (currentPersonaje != null ? currentPersonaje.getNombre() : "Unknown"));
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

    /**
     * Actualiza la apariencia del botón de favoritos según el estado del personaje.
     */
    private void updateFavoriteUI() {
        if (currentPersonaje.isFavorite()) {
            btnFavorite.setText("★ Favorito");
            btnFavorite.setStyle(
                    "-fx-font-size: 14; -fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
        } else {
            btnFavorite.setText("☆ Marcar como Favorito");
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
            mostrarAlerta("No autorizado", "Debes iniciar sesión para eliminar personajes.");
            return;
        }

        if (currentPersonaje == null) {
            mostrarAlerta("Error", "No hay personaje seleccionado.");
            return;
        }

        // Confirmación
        javafx.scene.control.Alert confirmacion = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Eliminar personaje?");
        confirmacion.setContentText("¿Estás seguro de que quieres eliminar a " + currentPersonaje.getNombre() + "?");

        if (confirmacion.showAndWait()
                .orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK) {
            new Thread(() -> {
                try {
                    boolean success = HarryPotterAPI.deleteCharacter(currentPersonaje.getApiId());
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            mostrarAlerta("Éxito", "Personaje eliminado correctamente.");
                            // Volver a la vista principal
                            try {
                                App.setRoot("Main_view", "Anuario Hogwarts");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            mostrarAlerta("Error", "No se pudo eliminar el personaje.");
                        }
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        mostrarAlerta("Error", "Error al eliminar: " + e.getMessage());
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
            mostrarAlerta("No autorizado", "Debes iniciar sesión para editar personajes.");
            return;
        }

        if (currentPersonaje == null) {
            mostrarAlerta("Error", "No hay personaje seleccionado.");
            return;
        }

        // Crear un diálogo con ScrollPane para todos los campos
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Editar Personaje");
        dialog.setHeaderText("Editar datos de " + currentPersonaje.getNombre());

        javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("Guardar",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, javafx.scene.control.ButtonType.CANCEL);

        // Crear formulario completo con ScrollPane
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));

        // Crear campos de texto para todos los atributos editables
        javafx.scene.control.TextField txtNombre = new javafx.scene.control.TextField(currentPersonaje.getNombre());
        javafx.scene.control.TextField txtBorn = new javafx.scene.control.TextField(currentPersonaje.getBorn());
        javafx.scene.control.TextField txtDied = new javafx.scene.control.TextField(currentPersonaje.getDied());
        javafx.scene.control.TextField txtGender = new javafx.scene.control.TextField(currentPersonaje.getGender());
        javafx.scene.control.TextField txtSpecies = new javafx.scene.control.TextField(currentPersonaje.getSpecies());
        javafx.scene.control.TextField txtAnimagus = new javafx.scene.control.TextField(currentPersonaje.getAnimagus());
        javafx.scene.control.TextField txtNationality = new javafx.scene.control.TextField(
                currentPersonaje.getNationality());
        javafx.scene.control.TextField txtHouse = new javafx.scene.control.TextField(currentPersonaje.getCasa());
        javafx.scene.control.TextField txtPatronus = new javafx.scene.control.TextField(currentPersonaje.getPatronus());
        // Usar TextArea para campos que pueden ser largos (arrays convertidos a string)
        javafx.scene.control.TextArea txtAlias = new javafx.scene.control.TextArea(currentPersonaje.getAlias());
        txtAlias.setPrefRowCount(2);
        txtAlias.setWrapText(true);
        txtAlias.setPrefWidth(300);

        javafx.scene.control.TextArea txtTitles = new javafx.scene.control.TextArea(currentPersonaje.getTitles());
        txtTitles.setPrefRowCount(2);
        txtTitles.setWrapText(true);
        txtTitles.setPrefWidth(300);

        javafx.scene.control.TextArea txtWand = new javafx.scene.control.TextArea(currentPersonaje.getWand());
        txtWand.setPrefRowCount(2);
        txtWand.setWrapText(true);
        txtWand.setPrefWidth(300);

        // Establecer ancho preferido para los campos de texto simples
        txtNombre.setPrefWidth(300);
        txtBorn.setPrefWidth(300);
        txtDied.setPrefWidth(300);
        txtGender.setPrefWidth(300);
        txtSpecies.setPrefWidth(300);
        txtAnimagus.setPrefWidth(300);
        txtNationality.setPrefWidth(300);
        txtHouse.setPrefWidth(300);
        txtPatronus.setPrefWidth(300);

        // Añadir campos al grid
        int row = 0;
        grid.add(new javafx.scene.control.Label("Nombre:"), 0, row);
        grid.add(txtNombre, 1, row++);

        grid.add(new javafx.scene.control.Label("Born:"), 0, row);
        grid.add(txtBorn, 1, row++);

        grid.add(new javafx.scene.control.Label("Died:"), 0, row);
        grid.add(txtDied, 1, row++);

        grid.add(new javafx.scene.control.Label("Gender:"), 0, row);
        grid.add(txtGender, 1, row++);

        grid.add(new javafx.scene.control.Label("Species:"), 0, row);
        grid.add(txtSpecies, 1, row++);

        grid.add(new javafx.scene.control.Label("Animagus:"), 0, row);
        grid.add(txtAnimagus, 1, row++);

        grid.add(new javafx.scene.control.Label("Nationality:"), 0, row);
        grid.add(txtNationality, 1, row++);

        grid.add(new javafx.scene.control.Label("House:"), 0, row);
        grid.add(txtHouse, 1, row++);

        grid.add(new javafx.scene.control.Label("Patronus:"), 0, row);
        grid.add(txtPatronus, 1, row++);

        grid.add(new javafx.scene.control.Label("Alias:"), 0, row);
        grid.add(txtAlias, 1, row++);

        grid.add(new javafx.scene.control.Label("Titles:"), 0, row);
        grid.add(txtTitles, 1, row++);

        grid.add(new javafx.scene.control.Label("Wand:"), 0, row);
        grid.add(txtWand, 1, row++);

        scrollPane.setContent(grid);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(500);

        dialog.setResultConverter(dialogButton -> {
            return dialogButton == btnGuardar;
        });

        dialog.showAndWait().ifPresent(save -> {
            if (save) {
                // Construir JSON con todos los datos actualizados
                com.google.gson.JsonObject updates = new com.google.gson.JsonObject();
                updates.addProperty("name", txtNombre.getText());
                updates.addProperty("born", txtBorn.getText());
                updates.addProperty("died", txtDied.getText());
                updates.addProperty("gender", txtGender.getText());
                updates.addProperty("species", txtSpecies.getText());
                updates.addProperty("animagus", txtAnimagus.getText());
                updates.addProperty("nationality", txtNationality.getText());
                updates.addProperty("house", txtHouse.getText());
                updates.addProperty("patronus", txtPatronus.getText());
                updates.addProperty("alias_names", txtAlias.getText());
                updates.addProperty("titles", txtTitles.getText());
                updates.addProperty("wand", txtWand.getText());

                new Thread(() -> {
                    try {
                        boolean success = HarryPotterAPI.editCharacter(currentPersonaje.getApiId(), updates);
                        javafx.application.Platform.runLater(() -> {
                            if (success) {
                                mostrarAlerta("Éxito", "Personaje actualizado correctamente.");
                                // Actualizar datos locales
                                currentPersonaje.setNombre(txtNombre.getText());
                                currentPersonaje.setBorn(txtBorn.getText());
                                currentPersonaje.setDied(txtDied.getText());
                                currentPersonaje.setGender(txtGender.getText());
                                currentPersonaje.setSpecies(txtSpecies.getText());
                                currentPersonaje.setAnimagus(txtAnimagus.getText());
                                currentPersonaje.setNationality(txtNationality.getText());
                                currentPersonaje.setCasa(txtHouse.getText());
                                currentPersonaje.setPatronus(txtPatronus.getText());
                                currentPersonaje.setAlias(txtAlias.getText());
                                currentPersonaje.setTitles(txtTitles.getText());
                                currentPersonaje.setWand(txtWand.getText());
                                // Refrescar vista
                                setPersonaje(currentPersonaje);
                            } else {
                                mostrarAlerta("Error", "No se pudo actualizar el personaje.");
                            }
                        });
                    } catch (Exception e) {
                        javafx.application.Platform.runLater(() -> {
                            mostrarAlerta("Error", "Error al actualizar: " + e.getMessage());
                        });
                        e.printStackTrace();
                    }
                }).start();
            }
        });
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