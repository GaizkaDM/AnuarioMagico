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

public class ReportService {

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
        try {
            // 1. Load Template
            InputStream reportStream = ReportService.class.getResourceAsStream(REPORT_DIR + templateName);
            if (reportStream == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No se encuentra la plantilla del reporte: " + templateName);
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
            // Add localized strings if needed
            parameters.put("LABEL_NAME", App.getBundle().getString("detail.name"));
            parameters.put("LABEL_HOUSE", App.getBundle().getString("detail.house"));
            parameters.put("LABEL_STATUS", App.getBundle().getString("edit.label.status"));
            // ... more params as needed for the template

            // 5. Fill Report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // 6. Save PDF
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fileChooser.setInitialFileName(title.replace(" ", "_") + ".pdf");

            File file = fileChooser.showSaveDialog(owner);

            if (file != null) {
                JasperExportManager.exportReportToPdfFile(jasperPrint, file.getAbsolutePath());
                showAlert(Alert.AlertType.INFORMATION, "Éxito", "PDF generado correctamente: " + file.getName());
            }

        } catch (JRException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error JasperReports", "Error al generar el reporte: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error inesperado: " + e.getMessage());
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
