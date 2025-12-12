package org.GaizkaFrost;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    public void setOnSuccessCallback(java.util.function.Consumer<String> callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void initialize() {
        chkRegistro.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boxMasterPassword.setVisible(newVal);
            boxMasterPassword.setManaged(newVal);
            btnAccion.setText(newVal ? "Registrarse" : "Entrar");
            lblStatus.setText("");
        });

        btnAccion.setOnAction(e -> handleAction());
    }

    private void handleAction() {
        String user = txtUsuario.getText();
        String pass = txtPassword.getText();
        boolean isRegister = chkRegistro.isSelected();

        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Por favor, rellena los campos.");
            return;
        }

        lblStatus.setText("Procesando...");
        btnAccion.setDisable(true);

        new Thread(() -> {
            try {
                if (isRegister) {
                    String master = txtMasterPassword.getText();
                    if (master.isEmpty()) {
                        updateStatus("Falta contraseña maestra.");
                        return;
                    }
                    boolean ok = HarryPotterAPI.register(user, pass, master);
                    if (ok) {
                        updateIO(() -> {
                            chkRegistro.setSelected(false);
                            lblStatus.setText("Registro exitoso. Inicia sesión.");
                            txtPassword.clear();
                        });
                    } else {
                        updateStatus("Error en registro. Verifica la contraseña maestra.");
                    }
                } else {
                    String token = HarryPotterAPI.login(user, pass);
                    if (token != null) {
                        updateIO(() -> {
                            if (onSuccessCallback != null)
                                onSuccessCallback.accept(user);
                            ((Stage) btnAccion.getScene().getWindow()).close();
                        });
                    } else {
                        updateStatus("Usuario o contraseña incorrectos.");
                    }
                }
            } catch (Exception ex) {
                updateStatus("Error de conexión: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void updateStatus(String msg) {
        javafx.application.Platform.runLater(() -> {
            lblStatus.setText(msg);
            btnAccion.setDisable(false);
        });
    }

    private void updateIO(Runnable r) {
        javafx.application.Platform.runLater(() -> {
            r.run();
            btnAccion.setDisable(false);
        });
    }
}
