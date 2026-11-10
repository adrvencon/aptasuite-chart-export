package chartexport.ui;

import java.io.File;
import java.util.prefs.Preferences;

final class Settings {

    private static final String PNG_SCALE = "pngScale";
    private static final String PNG_TRANSPARENT = "pngTransparent";
    private static final String LAST_DIRECTORY = "lastDirectory";

    private Settings() {
    }

    private static Preferences prefs() {
        return Preferences.userNodeForPackage(Settings.class);
    }

    static int pngScale(int defaultValue) {
        return prefs().getInt(PNG_SCALE, defaultValue);
    }

    static void setPngScale(int scale) {
        prefs().putInt(PNG_SCALE, scale);
    }

    static boolean pngTransparent() {
        return prefs().getBoolean(PNG_TRANSPARENT, false);
    }

    static void setPngTransparent(boolean transparent) {
        prefs().putBoolean(PNG_TRANSPARENT, transparent);
    }

    static File lastDirectory() {
        String path = prefs().get(LAST_DIRECTORY, null);
        if (path == null) {
            return null;
        }
        File dir = new File(path);
        return dir.isDirectory() ? dir : null;
    }

    static void setLastDirectory(File directory) {
        if (directory != null) {
            prefs().put(LAST_DIRECTORY, directory.getPath());
        }
    }
}
