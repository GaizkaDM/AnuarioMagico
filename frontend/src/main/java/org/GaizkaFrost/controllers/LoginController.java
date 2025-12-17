package org.GaizkaFrost.controllers;

import org.GaizkaFrost.services.HarryPotterAPI;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controlador para la vista de inicio de sesión y registro.
 * Gestiona la autenticación de usuarios contra el backend.
 *
 * @author Gaizka
 * @author Diego
 * @author Xiker
 */
import org.GaizkaFrost.App;
import java.text.MessageFormat;

public class LoginController {

    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private CheckBox chkRegistro;
    @FXML
    private VBox boxMasterPassword;
    @FXML
    private PasswordField txtMasterPassword;
    @FXML
    private Button btnAccion;
    @FXML
    private Label lblStatus;

    private java.util.function.Consumer<String> onSuccessCallback;

    /**
     * Establece el callback que se ejecutará tras un inicio de sesión exitoso.
     *
     * @param callback Función que recibe el nombre de usuario autenticado.
     */
    public void setOnSuccessCallback(java.util.function.Consumer<String> callback) {
        this.onSuccessCallback = callback;
    }

    /**
     * Inicializa el controlador.
     * Configura la lógica de alternancia entre modo Login y Registro.
     */
    @FXML
    public void initialize() {
        chkRegistro.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boxMasterPassword.setVisible(newVal);
            boxMasterPassword.setManaged(newVal);
            btnAccion.setText(App.getBundle().getString("login.button"));
            lblStatus.setText("");
        });

        btnAccion.setOnAction(e -> {
            System.out.println("DEBUG: Button ACCEDER clicked");
            handleAction();
        });
        System.out.println("DEBUG: LoginController initialized");
    }

    /**
     * Maneja la acción del botón principal (Entrar o Registrarse).
     * Valida entradas y realiza la llamada a la API correspondiente en un hilo
     * separado.
     */
    private void handleAction() {
        String user = txtUsuario.getText();
        String pass = txtPassword.getText();
        boolean isRegister = chkRegistro.isSelected();

        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText(App.getBundle().getString("login.status.fill"));
            return;
        }

        lblStatus.setText(App.getBundle().getString("login.status.processing"));
        btnAccion.setDisable(true);

        new Thread(() -> {
            try {
                if (isRegister) {
                    String master = txtMasterPassword.getText();
                    if (master.isEmpty()) {
                        updateStatus("Missing master password."); // Need key
                        return;
                    }
                    boolean ok = HarryPotterAPI.register(user, pass, master);
                    if (ok) {
                        updateIO(() -> {
                            chkRegistro.setSelected(false);
                            chkRegistro.setSelected(false);
                            lblStatus.setText("Registration successful. Please login."); // Need key
                            txtPassword.clear();
                        });
                    } else {
                        updateStatus("Registration error. Check master password."); // Need key
                    }
                } else {
                    String token = HarryPotterAPI.login(user, pass);
                    if (token != null) {
                        // Guardar token y username en HarryPotterAPI
                        HarryPotterAPI.setToken(token, user);
                        updateIO(() -> {
                            if (onSuccessCallback != null)
                                onSuccessCallback.accept(user);
                            ((Stage) btnAccion.getScene().getWindow()).close();
                        });
                    } else {
                        updateStatus(App.getBundle().getString("login.status.error"));
                    }
                }
            } catch (Exception ex) {
                updateStatus(MessageFormat.format(App.getBundle().getString("login.status.connection_error"),
                        ex.getMessage()));
                ex.printStackTrace();
            }
        }).start();
    }

    /**
     * Actualiza el mensaje de estado en la interfaz (hilo UI).
     *
     * @param msg Mensaje a mostrar.
     */
    private void updateStatus(String msg) {
        javafx.application.Platform.runLater(() -> {
            lblStatus.setText(msg);
            btnAccion.setDisable(false);
        });
    }

    /**
     * Ejecuta una tarea en el hilo de la interfaz de usuario (Platform.runLater).
     *
     * @param r Runnable a ejecutar.
     */
    private void updateIO(Runnable r) {
        javafx.application.Platform.runLater(() -> {
            r.run();
            btnAccion.setDisable(false);
        });
    }
}
