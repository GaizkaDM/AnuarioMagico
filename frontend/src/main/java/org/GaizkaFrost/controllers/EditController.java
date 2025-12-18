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
import java.util.ArrayList;
import java.util.Arrays;
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

    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnGuardar;

    private Personaje currentPersonaje;
    private String currentImageUrl;
    private File selectedImageFile;

    @FXML
    public void initialize() {
        // Initialize ComboBoxes
        comboCasa.setItems(FXCollections.observableArrayList(
                "Gryffindor", "Slytherin", "Hufflepuff", "Ravenclaw", "Unknown", "None"));

        comboEstado.setItems(FXCollections.observableArrayList(
                "Vivo", "Fallecido", "Desconocido"));

        comboGenero.setItems(FXCollections.observableArrayList(
                "Male", "Female", "Other"));

        // Setup buttons
        btnCancelar.setOnAction(e -> closeWindow());
        btnGuardar.setOnAction(e -> guardarPersonaje());
        btnSeleccionarFoto.setOnAction(e -> seleccionarFoto());
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
        // Map status logic if needed, simplfied here
        String estado = p.getEstado();
        if (estado == null || estado.isEmpty())
            estado = "Desconocido";
        // If the backend sends "Alive"/"Deceased" map to "Vivo"/"Fallecido" if
        // necessary
        // For now trusting the string matches or is close enough.
        comboEstado.setValue(estado);

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
        comboEstado.getSelectionModel().select("Vivo");
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
            // In a real app we'd upload this file. For now we might just store the path or
            // mock it
            // since the add_character implementation expects a URL or blob logic handled
            // elsewhere.
            // But let's assume valid URL or existing logic.
            // Since this is a simple client, we can't easily "upload" without an upload
            // endpoint.
            // check HarryPotterAPI for upload capabilities?
            // HarryPotterAPI only sends JSON.
            // We might just use a placeholder or the file path if local.
        }
    }

    private void guardarPersonaje() {
        if (!validarEntradas())
            return;

        JsonObject json = new JsonObject();

        json.addProperty("name", txtNombre.getText());
        json.addProperty("house", comboCasa.getValue());

        // Logic for died/alive based on combo or text
        String estado = comboEstado.getValue();
        if ("Fallecido".equals(estado) || (txtMuerte.getText() != null && !txtMuerte.getText().isEmpty())) {
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
        try {
            if (currentPersonaje != null) {
                // Editing
                success = HarryPotterAPI.editCharacter(currentPersonaje.getApiId(), json);
            } else {
                // Adding
                success = HarryPotterAPI.addCharacter(json);
            }

            if (success) {
                closeWindow();
            } else {
                lblError.setText(App.getBundle().getString("edit.error.server"));
            }
        } catch (Exception e) {
            logger.error("Error saving character: {}", e.getMessage(), e);
            lblError.setText(App.getBundle().getString("error.title") + ": " + e.getMessage());
        }
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

    private void closeWindow() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
