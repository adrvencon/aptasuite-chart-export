package chartexport;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;

final class ChartExporter {

    private static final double SCALE = 4.0;

    private ChartExporter() {
    }

    static void savePng(Chart chart) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save chart as PNG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        chooser.setInitialFileName(suggestName(chart) + ".png");

        File file = chooser.showSaveDialog(chart.getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getPath() + ".png");
        }

        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.WHITE);
            params.setTransform(Transform.scale(SCALE, SCALE));
            WritableImage image = chart.snapshot(params, null);
            boolean written = ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            if (!written) {
                throw new IOException("No PNG writer is available in this Java runtime.");
            }
        } catch (IOException | RuntimeException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "The chart could not be saved. " + ex.getMessage());
            alert.setTitle("Chart export failed");
            alert.initOwner(chart.getScene().getWindow());
            alert.show();
        }
    }

    private static String suggestName(Chart chart) {
        String title = chart.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return "chart";
        }
        return title.trim().replaceAll("[^\\p{L}\\p{N}_-]+", "_");
    }
}
