package org.GaizkaFrost;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

/**
 * Controller for the character detail view.
 */
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
    public void initialize() {
        btnVolver.setOnAction(event -> {
            try {
                App.setRoot("Main_view");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Receives a character and fills the detail screen.
     *
     * @param p character to display
     */
    public void setPersonaje(Personaje p) {

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

    private String nullToDash(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }
}