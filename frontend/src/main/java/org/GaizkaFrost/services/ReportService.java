package org.GaizkaFrost.services;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import org.GaizkaFrost.models.Personaje;
import org.GaizkaFrost.App;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio encargado de la generación de informes PDF utilizando JasperReports.
 * Maneja la compilación y rellenado de plantillas .jrxml en un hilo secundario.
 *
 * @author Diego
 * @author Gaizka
 * @author Xiker
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final String REPORT_DIR = "/reports/";

    public static void generateCharacterReport(Personaje p, Stage owner) {
        if (p == null)
            return;
        generateReport(Collections.singletonList(p), "character_detail.jrxml", "Character Sheet", owner);
    }

    public static void generateListReport(List<Personaje> list, Stage owner) {
        if (list == null || list.isEmpty())
            return;
        generateReport(list, "character_list.jrxml", "Character List", owner);
    }

    private static void generateReport(List<Personaje> data, String templateName, String title, Stage owner) {
        // 1. File Chooser (Main Thread)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(title.replace(" ", "_") + ".pdf");

        File file = fileChooser.showSaveDialog(owner);

        if (file == null)
            return;

        // Show Loading Dialog
        Alert loadingAlert = new Alert(Alert.AlertType.NONE);
        loadingAlert.setTitle("Generating PDF");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Please wait, creating report...");

        javafx.scene.control.ProgressIndicator progressIndicator = new javafx.scene.control.ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        loadingAlert.setGraphic(progressIndicator);

        // Prevent closing by user and remove buttons to make it look like a pure
        // waiting dialog
        loadingAlert.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        javafx.scene.Node closeButton = loadingAlert.getDialogPane()
                .lookupButton(javafx.scene.control.ButtonType.CLOSE);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);

        loadingAlert.initOwner(owner);
        loadingAlert.show(); // Non-blocking show

        // 2. Background Processing
        new Thread(() -> {
            try {
                // Notify start (Optional, but good for logs)
                logger.info("Starting PDF generation for {}...", file.getName());

                // 1. Load Template
                InputStream reportStream = ReportService.class.getResourceAsStream(REPORT_DIR + templateName);
                if (reportStream == null) {
                    javafx.application.Platform.runLater(() -> {
                        loadingAlert.close();
                        showAlert(Alert.AlertType.ERROR, "Error",
                                "Report template not found: " + templateName);
                    });
                    return;
                }

                // 2. Compile Report
                JasperDesign jasperDesign = JRXmlLoader.load(reportStream);
                JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

                // 3. Create Data Source
                JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

                // 4. Parameters
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("REPORT_TITLE", title);
                parameters.put("LABEL_NAME", "Name");
                parameters.put("LABEL_HOUSE", "House");
                parameters.put("LABEL_STATUS", "Status");
                parameters.put("LABEL_PATRONUS", "Patronus");
                // Add extended fields
                parameters.put("LABEL_BORN", "Born");
                parameters.put("LABEL_DIED", "Died");
                parameters.put("LABEL_GENDER", "Gender");
                parameters.put("LABEL_SPECIES", "Species");
                parameters.put("LABEL_ANIMAGUS", "Animagus");
                parameters.put("LABEL_NATIONALITY", "Nationality");
                parameters.put("LABEL_ALIAS", "Alias");
                parameters.put("LABEL_TITLES", "Titles");
                parameters.put("LABEL_WAND", "Wand");

                // 5. Fill Report (Heavy Lifting - fetching images etc)
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

                // 6. Save PDF
                JasperExportManager.exportReportToPdfFile(jasperPrint, file.getAbsolutePath());

                // Success Callback
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "PDF generated successfully:\n" + file.getAbsolutePath());
                });

            } catch (JRException e) {
                logger.error("Error generating report: {}", e.getMessage(), e);
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showAlert(Alert.AlertType.ERROR, "Error JasperReports",
                            "Error generating report: " + e.getMessage());
                });
            } catch (Exception e) {
                logger.error("Unexpected error during report generation: {}", e.getMessage(), e);
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showAlert(Alert.AlertType.ERROR, "Error", "Unexpected error: " + e.getMessage());
                });
            }
        }).start();
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        App.setIcon(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
