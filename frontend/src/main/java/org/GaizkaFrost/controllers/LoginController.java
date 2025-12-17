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
        System.out.println("DEBUG: LoginController initialized");
    }

    /**
     * Maneja la acción del botón principal (Entrar o Registrarse).
     * Valida entradas y realiza la llamada a la API correspondiente en un hilo
     * separado.
     */
    @FXML
    public void onLoginButtonClick(javafx.event.ActionEvent event) {
        System.out.println("onLoginButtonClick called (public)");
        try {
            String user = txtUsuario.getText();
            String pass = txtPassword.getText();
            boolean isRegister = chkRegistro.isSelected();
            System.out.println("User: " + user + ", Register: " + isRegister);

            if (user.isEmpty() || pass.isEmpty()) {
                System.out.println("Empty fields");
                lblStatus.setText(App.getBundle().getString("login.status.fill"));
                return;
            }

            lblStatus.setText(App.getBundle().getString("login.status.processing"));
            btnAccion.setDisable(true);
            System.out.println("Button disabled, starting background thread");

            new Thread(() -> {
                System.out.println("Thread started");
                try {
                    if (isRegister) {
                        String master = txtMasterPassword.getText();
                        if (master.isEmpty()) {
                            updateStatus("Missing master password.");
                            return;
                        }
                        boolean ok = HarryPotterAPI.register(user, pass, master);
                        System.out.println("Register result: " + ok);
                        if (ok) {
                            updateIO(() -> {
                                chkRegistro.setSelected(false);
                                lblStatus.setText("Registro exitoso. Por favor identifícate.");
                                txtPassword.clear();
                            });
                        } else {
                            updateStatus("Error en registro. Verifica la clave maestra.");
                        }
                    } else {
                        System.out.println("Attempting login...");
                        String token = HarryPotterAPI.login(user, pass);
                        System.out.println("Login info received: " + (token != null ? "Token OK" : "Null"));

                        if (token != null) {
                            HarryPotterAPI.setToken(token, user);
                            updateIO(() -> {
                                System.out.println("Login success - closing window");
                                if (onSuccessCallback != null)
                                    onSuccessCallback.accept(user);
                                ((Stage) btnAccion.getScene().getWindow()).close();
                            });
                        } else {
                            System.out.println("Login failed - Invalid credentials");
                            updateStatus(App.getBundle().getString("login.status.error"));
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Exception in login thread: " + ex.getMessage());
                    ex.printStackTrace();
                    updateStatus(MessageFormat.format(App.getBundle().getString("login.status.connection_error"),
                            ex.getMessage() != null ? ex.getMessage() : "Unknown Error"));
                }
            }).start();
        } catch (Exception e) {
            System.err.println("Exception in onLoginButtonClick main thread: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Actualiza el mensaje de estado en la interfaz (hilo UI).
     *
     * @param msg Mensaje a mostrar.
     */
    private void updateStatus(String msg) {
        javafx.application.Platform.runLater(() -> {
            System.out.println("Updating status: " + msg);
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
            System.out.println("Executing IO update");
            r.run();
            btnAccion.setDisable(false);
        });
    }
}
