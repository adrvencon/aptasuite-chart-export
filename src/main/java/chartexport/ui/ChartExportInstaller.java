package chartexport.ui;

import java.util.ArrayList;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Window;

public final class ChartExportInstaller {

    private static final String MARK = "chartexport.hooked";
    private static ContextMenu openMenu;

    private ChartExportInstaller() {
    }

    public static void install() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Window w : change.getAddedSubList()) {
                        watch(w);
                    }
                }
            }
        });
        for (Window w : new ArrayList<>(Window.getWindows())) {
            watch(w);
        }
    }

    private static void watch(Window window) {
        window.sceneProperty().addListener((obs, oldScene, newScene) -> hook(newScene));
        hook(window.getScene());
    }

    private static void hook(Scene scene) {
        if (scene == null || scene.getProperties().containsKey(MARK)) {
            return;
        }
        scene.getProperties().put(MARK, Boolean.TRUE);

        scene.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            Chart chart = findChart(e.getTarget());
            if (chart == null) {
                return;
            }
            if (openMenu != null) {
                openMenu.hide();
            }
            openMenu = buildMenu(chart);
            openMenu.show(chart, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private static ContextMenu buildMenu(Chart chart) {
        MenuItem png = new MenuItem("Save chart as PNG...");
        png.setOnAction(a -> ChartExporter.savePng(chart));
        ContextMenu menu = new ContextMenu(png);

        if (ChartExporter.pdfAvailable()) {
            MenuItem pdf = new MenuItem("Save chart as PDF...");
            pdf.setOnAction(a -> ChartExporter.savePdf(chart));
            menu.getItems().add(pdf);
        }
        return menu;
    }

    private static Chart findChart(Object target) {
        Node n = (target instanceof Node) ? (Node) target : null;
        while (n != null) {
            if (n instanceof Chart) {
                return (Chart) n;
            }
            n = n.getParent();
        }
        return null;
    }
}
