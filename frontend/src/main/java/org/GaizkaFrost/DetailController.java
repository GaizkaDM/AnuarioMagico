package org.GaizkaFrost;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class DetailController {

    @FXML
    private Button btnVolver;
    @FXML
    private Label lblNombre;
    @FXML
    private ImageView imgGrande;
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
    private Label lblAlias;
    @FXML
    private Label lblTitles;
    @FXML
    private Label lblWand;

    @FXML
    public void initialize() {
        btnVolver.setOnAction(event -> {
            try {
                App.setRoot("Main_view");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void setPersonaje(Personaje p) {
        lblNombre.setText(p.getNombre());
        if (p.getImagenUrl() != null && !p.getImagenUrl().isEmpty()) {
            try {
                imgGrande.setImage(new Image(p.getImagenUrl()));
            } catch (Exception e) {
                // Ignore image load error
            }
        }

        lblBorn.setText(p.getBorn());
        lblDied.setText(p.getDied());
        lblGender.setText(p.getGender());
        lblSpecies.setText(p.getSpecies());
        lblAnimagus.setText(""); // Field not available
        lblNationality.setText(p.getBloodStatus()); // Showing Blood Status in Nationality label for now
        lblAlias.setText(""); // Field not available
        lblTitles.setText(p.getRole());
        lblWand.setText(""); // Field not available
    }
}
