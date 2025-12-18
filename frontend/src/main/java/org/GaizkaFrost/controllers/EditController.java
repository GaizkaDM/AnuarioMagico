package org.GaizkaFrost.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.GaizkaFrost.models.Personaje;
import org.GaizkaFrost.services.HarryPotterAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.GaizkaFrost.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Controlador para la vista de edición y creación de personajes.
 * Gestiona el formulario, validación y comunicación con la API.
 * 
 * @author Gaizka
 * @author Diego
 * @author Xiker
 */
public class EditController {

    private static final Logger logger = LoggerFactory.getLogger(EditController.class);

    @FXML
    private Label lblTitulo;
    @FXML
    private ImageView imgPreview;
    @FXML
    private Button btnSeleccionarFoto;
    @FXML
    private Label lblError;

    // Basic Info
    @FXML
    private TextField txtNombre;
    @FXML
    private ComboBox<String> comboCasa;
    @FXML
    private ComboBox<String> comboEstado;
    @FXML
    private TextField txtEspecie;
    @FXML
    private ComboBox<String> comboGenero;
    @FXML
    private TextField txtPatronus;

    // Physical
    @FXML
    private TextField txtOjos;
    @FXML
    private TextField txtPelo;
    @FXML
    private TextField txtPiel;
    @FXML
    private TextField txtAltura;
    @FXML
    private TextField txtPeso;

    // Magic
    @FXML
    private TextField txtNacimiento;
    @FXML
    private TextField txtMuerte;
    @FXML
    private TextField txtVarita;
    @FXML
    private TextField txtBoggart;
    @FXML
    private TextField txtAnimago;

    // Additional
    @FXML
    private TextField txtNacionalidad;
    @FXML
    private TextArea txtAlias;
    @FXML
    private TextArea txtTitulos;
    @FXML
    private TextArea txtFamilia;
    @FXML
    private TextArea txtTrabajos;
    @FXML
    private TextArea txtRomances;

    // Advanced Section Toggle
    @FXML
    private javafx.scene.layout.VBox boxAdvanced;
    @FXML
    private Button btnToggleAdvanced;

    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnGuardar;

    @FXML
    private ScrollPane editScrollPane;

    private Personaje currentPersonaje;
    private String currentImageUrl;
    private File selectedImageFile;
    private Runnable onSaveSuccess;

    public void setOnSaveSuccess(Runnable onSaveSuccess) {
        this.onSaveSuccess = onSaveSuccess;
    }

    @FXML
    public void initialize() {
        // Initialize ComboBoxes with houses from main view if available
        List<String> houses = App.getAvailableHouses();
        if (houses != null && !houses.isEmpty()) {
            comboCasa.setItems(FXCollections.observableArrayList(houses));
        } else {
            // Fallback
            comboCasa.setItems(FXCollections.observableArrayList(
                    "Gryffindor", "Slytherin", "Hufflepuff", "Ravenclaw", "Unknown", "None"));
        }

        // Initialize Status ComboBox with Localized Strings
        String alive = App.getBundle().getString("combo.status.alive");
        String deceased = App.getBundle().getString("combo.status.deceased");
        String unknown = App.getBundle().getString("combo.status.unknown");

        comboEstado.setItems(FXCollections.observableArrayList(alive, deceased, unknown));

        comboGenero.setItems(FXCollections.observableArrayList(
                "Male", "Female", "Other"));

        // Setup auto-formatting for dates
        setupDateFormatter(txtNacimiento);
        setupDateFormatter(txtMuerte);

        // Setup buttons
        btnCancelar.setOnAction(e -> closeWindow());
        btnGuardar.setOnAction(e -> guardarPersonaje());
        btnSeleccionarFoto.setOnAction(e -> seleccionarFoto());

        // Setup Advanced Toggle
        if (btnToggleAdvanced != null && boxAdvanced != null) {
            btnToggleAdvanced.setOnAction(e -> toggleAdvanced());
            updateToggleUI();
        }

        // Increase Scroll Speed
        if (editScrollPane != null) {
            editScrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
                if (event.getDeltaY() != 0) {
                    double delta = event.getDeltaY() * 3.0; // 3x speed
                    double height = editScrollPane.getContent().getBoundsInLocal().getHeight();
                    double vValue = editScrollPane.getVvalue();
                    if (height > 0) {
                        editScrollPane.setVvalue(vValue + -delta / height);
                        event.consume();
                    }
                }
            });
        }
    }

    private void toggleAdvanced() {
        boolean isVisible = boxAdvanced.isVisible();
        boxAdvanced.setVisible(!isVisible);
        boxAdvanced.setManaged(!isVisible);
        updateToggleUI();
    }

    private void updateToggleUI() {
        boolean isVisible = boxAdvanced.isVisible();
        if (isVisible) {
            btnToggleAdvanced.setText(App.getBundle().getString("edit.toggle.advanced.hide"));
        } else {
            btnToggleAdvanced.setText(App.getBundle().getString("edit.toggle.advanced.show"));
        }
    }

    public void setPersonaje(Personaje p) {
        this.currentPersonaje = p;
        if (p != null) {
            lblTitulo.setText(App.getBundle().getString("edit.title.edit"));
            cargarDatos(p);
        } else {
            lblTitulo.setText(App.getBundle().getString("edit.title.add"));
            limpiarFormulario();
        }
    }

    private void cargarDatos(Personaje p) {
        txtNombre.setText(p.getNombre());
        comboCasa.setValue(p.getCasa());

        // Map backend status to localized status
        String estadoBackend = p.getEstado(); // Usually "Vivo" or "Fallecido" or "Unknown" / empty
        String alive = App.getBundle().getString("combo.status.alive");
        String deceased = App.getBundle().getString("combo.status.deceased");
        String unknown = App.getBundle().getString("combo.status.unknown");

        if ("Fallecido".equalsIgnoreCase(estadoBackend) || "Deceased".equalsIgnoreCase(estadoBackend)) {
            comboEstado.setValue(deceased);
        } else if ("Vivo".equalsIgnoreCase(estadoBackend) || "Alive".equalsIgnoreCase(estadoBackend)) {
            comboEstado.setValue(alive);
        } else {
            comboEstado.setValue(unknown);
        }

        txtEspecie.setText(p.getSpecies());
        comboGenero.setValue(p.getGender());
        txtPatronus.setText(p.getPatronus());

        txtOjos.setText(p.getEyeColor());
        txtPelo.setText(p.getHairColor());
        txtPiel.setText(p.getSkinColor());
        txtAltura.setText(p.getHeight());
        txtPeso.setText(p.getWeight());

        txtNacimiento.setText(p.getBorn());
        txtMuerte.setText(p.getDied());
        txtVarita.setText(p.getWand());
        txtBoggart.setText(p.getBoggart());
        txtAnimago.setText(p.getAnimagus());

        txtNacionalidad.setText(p.getNationality());

        txtAlias.setText(p.getAlias());
        txtTitulos.setText(p.getTitles());
        txtFamilia.setText(p.getFamily());
        txtTrabajos.setText(p.getJobs());
        txtRomances.setText(p.getRomances());

        if (p.getImagenUrl() != null && !p.getImagenUrl().isEmpty()) {
            currentImageUrl = p.getImagenUrl();
            try {
                imgPreview.setImage(new Image(currentImageUrl, true));
            } catch (Exception e) {
                logger.error("Error loading image from URL {}: {}", currentImageUrl, e.getMessage());
            }
        }
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        comboCasa.getSelectionModel().clearSelection();
        // Default to Alive
        comboEstado.setValue(App.getBundle().getString("combo.status.alive"));

        imgPreview.setImage(null);
        currentImageUrl = null;
        selectedImageFile = null;
        // ... clear others if needed, new instance starts empty anyway
    }

    private void seleccionarFoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(App.getBundle().getString("edit.image.select"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fileChooser.showOpenDialog(btnSeleccionarFoto.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            imgPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    private void guardarPersonaje() {
        if (!validarEntradas())
            return;

        JsonObject json = new JsonObject();

        json.addProperty("name", txtNombre.getText());
        json.addProperty("house", comboCasa.getValue());

        // Map localized status back to logic
        String estadoSelected = comboEstado.getValue();
        String deceased = App.getBundle().getString("combo.status.deceased");

        boolean isDead = deceased.equals(estadoSelected);

        if (isDead || (txtMuerte.getText() != null && !txtMuerte.getText().isEmpty())) {
            json.addProperty("died", txtMuerte.getText().isEmpty() ? "Unknown" : txtMuerte.getText());
        } else {
            json.addProperty("died", "");
        }

        json.addProperty("species", txtEspecie.getText());
        json.addProperty("gender", comboGenero.getValue());
        json.addProperty("patronus", txtPatronus.getText());

        // Physical
        json.addProperty("eye_color", txtOjos.getText());
        json.addProperty("hair_color", txtPelo.getText());
        json.addProperty("skin_color", txtPiel.getText());
        json.addProperty("height", txtAltura.getText());
        json.addProperty("weight", txtPeso.getText());

        // Magic / Bio
        json.addProperty("born", txtNacimiento.getText());
        json.addProperty("boggart", txtBoggart.getText());
        json.addProperty("animagus", txtAnimago.getText());
        json.addProperty("nationality", txtNacionalidad.getText());

        // Lists (Comma separated TextAreas)
        addJsonList(json, "alias_names", txtAlias.getText());
        addJsonList(json, "titles", txtTitulos.getText());
        addJsonList(json, "jobs", txtTrabajos.getText());
        addJsonList(json, "romances", txtRomances.getText());
        addJsonList(json, "family_members", txtFamilia.getText()); // Note API expects "family_members" or
                                                                   // "family_member"? Check backend...
        // Backend 'characters.py' mapping: 'family_member': attr.get('family_members',
        // [])
        // Backend PersonajeService/DaoSQLite maps 'family_member' to JSON.
        // Let's use 'family_member' as key to match internal model if that's what
        // add_character expects
        // Looking at DaoSQLite line 62: personaje.get('family_member', [])
        // So key should be 'family_member'
        addJsonList(json, "family_member", txtFamilia.getText());

        // Wand is usually an object or string list.
        // Model says: List<String> items.
        addJsonList(json, "wand", txtVarita.getText());

        // Image
        if (currentPersonaje != null) {
            json.addProperty("image", currentPersonaje.getImagenUrl());
        }
        // If we selected a new file, we can't easily upload it with current API.
        // For now, let's just keep existing image or valid URL if typed.
        // NOTE: Providing a local file path won't work for a remote server, but might
        // for localhost.
        if (selectedImageFile != null) {
            // Very hacky: sending local path. Backend won't likely be able to serve it
            // unless it's uploaded.
            // But user didn't ask for full upload implementation, just fixing the
            // "Unresolved type".
            json.addProperty("image", selectedImageFile.toURI().toString());
        } else if (currentImageUrl == null && (currentPersonaje == null || currentPersonaje.getImagenUrl() == null)) {
            // Placeholder if no image
            json.addProperty("image", "https://via.placeholder.com/300x400?text=No+Image");
        }

        boolean success;
        String characterId = null;
        try {
            if (currentPersonaje != null) {
                // Editing
                characterId = currentPersonaje.getApiId();
                success = HarryPotterAPI.editCharacter(characterId, json);
            } else {
                // Adding
                characterId = HarryPotterAPI.addCharacter(json);
                success = (characterId != null);
            }

            if (success) {
                // If editing, update the local object so DetailView can refresh immediately
                if (currentPersonaje != null) {
                    updateLocalModel();
                }

                // If an image was selected, upload it now
                if (selectedImageFile != null) {
                    try {
                        lblError.setText("Subiendo imagen...");
                        boolean uploadSuccess = HarryPotterAPI.uploadImage(characterId, selectedImageFile);
                        if (!uploadSuccess) {
                            logger.warn("Image upload failed for character {}", characterId);
                        } else {
                            // Update image URL in local model if upload success (assuming predictable path
                            // or need fetch)
                            // For now, assume reload will handle image eventually, or set temp file URI
                            currentPersonaje.setImagenUrl(selectedImageFile.toURI().toString());
                        }
                    } catch (Exception e) {
                        logger.error("Error uploading image: {}", e.getMessage());
                    }
                } else if (currentImageUrl == null
                        && (currentPersonaje == null || currentPersonaje.getImagenUrl() == null)) {
                    // If we set a placeholder in JSON, maybe update model too?
                    // Not critical.
                }

                if (onSaveSuccess != null) {
                    onSaveSuccess.run();
                }
                closeWindow();
            } else {
                lblError.setText(App.getBundle().getString("edit.error.server"));
            }
        } catch (Exception e) {
            logger.error("Error saving character: {}", e.getMessage(), e);
            lblError.setText(App.getBundle().getString("error.title") + ": " + e.getMessage());
        }
    }

    private void updateLocalModel() {
        if (currentPersonaje == null)
            return;

        currentPersonaje.setNombre(txtNombre.getText());
        currentPersonaje.setCasa(comboCasa.getValue());

        // Status map
        String estadoSelected = comboEstado.getValue();
        String deceased = App.getBundle().getString("combo.status.deceased");
        String alive = App.getBundle().getString("combo.status.alive");

        if (deceased.equals(estadoSelected)) {
            currentPersonaje.setEstado("Fallecido");
        } else if (alive.equals(estadoSelected)) {
            currentPersonaje.setEstado("Vivo");
        } else {
            currentPersonaje.setEstado("Desconocido");
        }

        currentPersonaje.setSpecies(txtEspecie.getText());
        currentPersonaje.setGender(comboGenero.getValue());
        currentPersonaje.setPatronus(txtPatronus.getText());

        currentPersonaje.setEyeColor(txtOjos.getText());
        currentPersonaje.setHairColor(txtPelo.getText());
        currentPersonaje.setSkinColor(txtPiel.getText());
        currentPersonaje.setHeight(txtAltura.getText());
        currentPersonaje.setWeight(txtPeso.getText());

        currentPersonaje.setBorn(txtNacimiento.getText());
        currentPersonaje.setDied(txtMuerte.getText());
        currentPersonaje.setWand(txtVarita.getText());
        currentPersonaje.setBoggart(txtBoggart.getText());
        currentPersonaje.setAnimagus(txtAnimago.getText());
        currentPersonaje.setNationality(txtNacionalidad.getText());

        currentPersonaje.setAlias(txtAlias.getText());
        currentPersonaje.setTitles(txtTitulos.getText());
        currentPersonaje.setFamily(txtFamilia.getText());
        currentPersonaje.setJobs(txtTrabajos.getText());
        currentPersonaje.setRomances(txtRomances.getText());
    }

    private void addJsonList(JsonObject json, String key, String text) {
        JsonArray array = new JsonArray();
        if (text != null && !text.trim().isEmpty()) {
            String[] parts = text.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    array.add(p.trim());
                }
            }
        }
        json.add(key, array);
    }

    private boolean validarEntradas() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            lblError.setText(App.getBundle().getString("edit.error.name"));
            return false;
        }
        lblError.setText("");
        return true;
    }

    private void setupDateFormatter(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Allow deletion
            if (newValue == null || (oldValue != null && newValue.length() < oldValue.length())) {
                return;
            }

            // Clean noise and limit length
            String clean = newValue.replaceAll("[^\\d]", "");
            if (clean.length() > 8) {
                clean = clean.substring(0, 8);
            }

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < clean.length(); i++) {
                formatted.append(clean.charAt(i));
                if ((i == 1 || i == 3) && i < clean.length() - 1) {
                    formatted.append("-");
                }
            }

            // Update only if changed to avoid infinite loop
            if (!newValue.equals(formatted.toString())) {
                String finalFormatted = formatted.toString();
                javafx.application.Platform.runLater(() -> {
                    textField.setText(finalFormatted);
                    textField.positionCaret(finalFormatted.length());
                });
            }
        });
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
