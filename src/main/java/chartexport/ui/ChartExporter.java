package chartexport.ui;

import java.io.File;
import java.util.Locale;

import chartexport.pdf.PdfRenderer;
import chartexport.png.PngRenderer;

import javafx.scene.chart.Chart;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

final class ChartExporter {

    private ChartExporter() {
    }

    static boolean pdfAvailable() {
        try {
            Class.forName("com.itextpdf.text.pdf.PdfWriter");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    static void savePng(Chart chart) {
        Window owner = ownerOf(chart);

        PngRenderer.Options options = PngOptionsDialog.ask(chart, owner);
        if (options == null) {
            return;
        }
        File file = chooseFile(chart, owner, "Save chart as PNG", "PNG image", "png");
        if (file == null) {
            return;
        }

        try {
            int used = PngRenderer.write(chart, file, options);
            if (used < options.scale) {
                alert(Alert.AlertType.INFORMATION, "Chart saved",
                        "The image was too large for this chart, so it was saved at " + used
                                + "x instead of " + options.scale + "x.", owner);
            }
        } catch (Exception | OutOfMemoryError ex) {
            failed(ex, owner);
        }
    }

    static void savePdf(Chart chart) {
        Window owner = ownerOf(chart);

        File file = chooseFile(chart, owner, "Save chart as PDF", "PDF document", "pdf");
        if (file == null) {
            return;
        }
        try {
            PdfRenderer.write(chart, file);
        } catch (Exception | OutOfMemoryError ex) {
            failed(ex, owner);
        }
    }

    private static File chooseFile(Chart chart, Window owner, String title, String description,
            String extension) {
        String dotExtension = "." + extension;

        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*" + dotExtension));
        chooser.setInitialFileName(suggestName(chart) + dotExtension);
        File lastDirectory = Settings.lastDirectory();
        if (lastDirectory != null) {
            chooser.setInitialDirectory(lastDirectory);
        }

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return null;
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(dotExtension)) {
            file = new File(file.getPath() + dotExtension);
        }
        Settings.setLastDirectory(file.getParentFile());
        return file;
    }

    private static String suggestName(Chart chart) {
        String title = chart.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return "chart";
        }
        return title.trim().replaceAll("[^\\p{L}\\p{N}_-]+", "_");
    }

    private static Window ownerOf(Chart chart) {
        return chart.getScene() == null ? null : chart.getScene().getWindow();
    }

    private static void failed(Throwable ex, Window owner) {
        String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        alert(Alert.AlertType.ERROR, "Chart export failed", "The file could not be saved. " + detail, owner);
    }

    private static void alert(Alert.AlertType type, String title, String message, Window owner) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.show();
    }
}
