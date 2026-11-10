package chartexport.ui;

import java.util.Locale;
import java.util.Optional;

import chartexport.png.PngRenderer;

import javafx.geometry.Insets;
import javafx.scene.chart.Chart;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.StringConverter;

final class PngOptionsDialog {

    private PngOptionsDialog() {
    }

    static PngRenderer.Options ask(Chart chart, Window owner) {
        double w = chart.getWidth();
        double h = chart.getHeight();

        ComboBox<Integer> scaleBox = new ComboBox<>();
        int saved = Settings.pngScale(PngRenderer.DEFAULT_SCALE);
        int initial = 1;
        for (int s : PngRenderer.allowedScales(w, h)) {
            scaleBox.getItems().add(s);
            if (s <= saved) {
                initial = s;
            }
        }
        scaleBox.setValue(initial);
        scaleBox.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value + "x";
            }

            @Override
            public Integer fromString(String text) {
                return Integer.valueOf(text.replace("x", "").trim());
            }
        });

        CheckBox transparentBox = new CheckBox("Transparent background");
        transparentBox.setSelected(Settings.pngTransparent());

        Label sizeLabel = new Label();
        Runnable refresh = () -> {
            Integer s = scaleBox.getValue();
            if (s == null) {
                return;
            }
            long pw = (long) Math.ceil(w * s);
            long ph = (long) Math.ceil(h * s);
            sizeLabel.setText(String.format(Locale.ROOT,
                    "%d x %d px, about %.1f x %.1f cm at 300 dpi",
                    pw, ph, pw / 300.0 * 2.54, ph / 300.0 * 2.54));
        };
        scaleBox.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        refresh.run();

        Label hint = new Label("1x is the size on screen. Use 4x or more for print.\n"
                + "Transparent removes the chart and plot-area backgrounds; the legend keeps its own.");
        hint.setWrapText(true);
        hint.setMaxWidth(380);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Resolution"), 0, 0);
        grid.add(scaleBox, 1, 0);
        grid.add(transparentBox, 0, 1, 2, 1);
        grid.add(sizeLabel, 0, 2, 2, 1);
        grid.add(hint, 0, 3, 2, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Save chart as PNG");
        dialog.setHeaderText(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) {
            return null;
        }

        int scale = scaleBox.getValue() == null ? 1 : scaleBox.getValue();
        boolean transparent = transparentBox.isSelected();
        Settings.setPngScale(scale);
        Settings.setPngTransparent(transparent);
        return new PngRenderer.Options(scale, transparent);
    }
}
