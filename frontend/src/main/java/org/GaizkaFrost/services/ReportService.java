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

public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final String REPORT_DIR = "/reports/";

    public static void generateCharacterReport(Personaje p, Stage owner) {
        if (p == null)
            return;
        generateReport(Collections.singletonList(p), "character_detail.jrxml", "Ficha de Personaje", owner);
    }

    public static void generateListReport(List<Personaje> list, Stage owner) {
        if (list == null || list.isEmpty())
            return;
        generateReport(list, "character_list.jrxml", "Listado de Personajes", owner);
    }

    private static void generateReport(List<Personaje> data, String templateName, String title, Stage owner) {
        // 1. File Chooser (Main Thread)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(title.replace(" ", "_") + ".pdf");

        File file = fileChooser.showSaveDialog(owner);

        if (file == null)
            return;

        // 2. Background Processing
        new Thread(() -> {
            try {
                // Notify start (Optional, but good for logs)
                logger.info("Starting PDF generation for {}...", file.getName());

                // 1. Load Template
                InputStream reportStream = ReportService.class.getResourceAsStream(REPORT_DIR + templateName);
                if (reportStream == null) {
                    javafx.application.Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error",
                            "No se encuentra la plantilla del reporte: " + templateName));
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
                parameters.put("LABEL_NAME", App.getBundle().getString("detail.name"));
                parameters.put("LABEL_HOUSE", App.getBundle().getString("detail.house"));
                parameters.put("LABEL_STATUS", App.getBundle().getString("edit.label.status"));
                parameters.put("LABEL_PATRONUS", App.getBundle().getString("detail.patronus"));
                // Add extended fields
                parameters.put("LABEL_BORN", App.getBundle().getString("detail.born"));
                parameters.put("LABEL_DIED", App.getBundle().getString("detail.died"));
                parameters.put("LABEL_GENDER", App.getBundle().getString("detail.gender"));
                parameters.put("LABEL_SPECIES", App.getBundle().getString("detail.species"));
                parameters.put("LABEL_ANIMAGUS", App.getBundle().getString("detail.animagus"));
                parameters.put("LABEL_NATIONALITY", App.getBundle().getString("detail.nationality"));
                parameters.put("LABEL_ALIAS", App.getBundle().getString("detail.alias"));
                parameters.put("LABEL_TITLES", App.getBundle().getString("detail.titles"));
                parameters.put("LABEL_WAND", App.getBundle().getString("detail.wand"));

                // 5. Fill Report (Heavy Lifting - fetching images etc)
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

                // 6. Save PDF
                JasperExportManager.exportReportToPdfFile(jasperPrint, file.getAbsolutePath());

                // Success Callback
                javafx.application.Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Éxito",
                        "PDF generado correctamente:\n" + file.getAbsolutePath()));

            } catch (JRException e) {
                logger.error("Error generating report: {}", e.getMessage(), e);
                javafx.application.Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error JasperReports",
                        "Error al generar el reporte: " + e.getMessage()));
            } catch (Exception e) {
                logger.error("Unexpected error during report generation: {}", e.getMessage(), e);
                javafx.application.Platform.runLater(
                        () -> showAlert(Alert.AlertType.ERROR, "Error", "Error inesperado: " + e.getMessage()));
            }
        }).start();
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
