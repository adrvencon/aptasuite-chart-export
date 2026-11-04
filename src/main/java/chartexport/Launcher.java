package chartexport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javafx.application.Platform;


public class Launcher {

    private static final String APTASUITE_MAIN = "aptasuite.Aptasuite";

    public static void main(String[] args) throws Throwable {
        Thread installer = new Thread(Launcher::installWhenToolkitReady, "chart-export-installer");
        installer.setDaemon(true);
        installer.start();

        Class<?> app = Class.forName(APTASUITE_MAIN);
        Method main = app.getMethod("main", String[].class);
        try {
            main.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** Platform.runLater throws IllegalStateException until the JavaFX toolkit has started. */
    private static void installWhenToolkitReady() {
        for (int i = 0; i < 600; i++) {
            try {
                Platform.runLater(ChartExportInstaller::install);
                return;
            } catch (IllegalStateException toolkitNotReady) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }
}
