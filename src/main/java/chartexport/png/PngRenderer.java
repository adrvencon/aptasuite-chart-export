package chartexport.png;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

public final class PngRenderer {

    static final int[] SCALES = {1, 2, 4, 6, 8};
    public static final int DEFAULT_SCALE = 4;

    static final double MAX_SIDE_PX = 16000;

    private static final double SCREEN_DPI = 96.0;

    private static final String[] BACKGROUND_SELECTORS = {".chart-plot-background"};

    private PngRenderer() {
    }

    public static final class Options {
        public final int scale;
        public final boolean transparent;

        public Options(int scale, boolean transparent) {
            this.scale = scale;
            this.transparent = transparent;
        }
    }

    public static int[] allowedScales(double width, double height) {
        int[] result = new int[SCALES.length];
        int count = 0;
        for (int s : SCALES) {
            if (s == 1 || Math.max(width, height) * s <= MAX_SIDE_PX) {
                result[count++] = s;
            }
        }
        return Arrays.copyOf(result, count);
    }

    static int lowerScale(int scale) {
        int lower = scale;
        for (int s : SCALES) {
            if (s < scale) {
                lower = s;
            }
        }
        return lower;
    }

    public static int write(Chart chart, File file, Options options) throws IOException {
        int scale = options.scale;
        while (true) {
            try {
                WritableImage image = snapshot(chart, scale, options.transparent);
                BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
                writeWithDpi(buffered, file, SCREEN_DPI * scale);
                return scale;
            } catch (RuntimeException | OutOfMemoryError e) {
                int lower = lowerScale(scale);
                if (lower == scale) {
                    throw e;
                }
                scale = lower;
            }
        }
    }

    private static WritableImage snapshot(Chart chart, int scale, boolean transparent) {
        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(Transform.scale(scale, scale));
        if (!transparent) {
            params.setFill(Color.WHITE);
            return chart.snapshot(params, null);
        }

        params.setFill(Color.TRANSPARENT);
        Map<Node, String> originalStyles = new LinkedHashMap<>();
        try {
            clearBackground(chart, originalStyles);
            for (String selector : BACKGROUND_SELECTORS) {
                for (Node n : chart.lookupAll(selector)) {
                    clearBackground(n, originalStyles);
                }
            }
            return chart.snapshot(params, null);
        } finally {
            for (Map.Entry<Node, String> e : originalStyles.entrySet()) {
                e.getKey().setStyle(e.getValue());
            }
        }
    }

    private static void clearBackground(Node node, Map<Node, String> originalStyles) {
        if (originalStyles.containsKey(node)) {
            return;
        }
        String base = node.getStyle() == null ? "" : node.getStyle();
        originalStyles.put(node, base);
        String separator = base.isEmpty() || base.trim().endsWith(";") ? "" : ";";
        node.setStyle(base + separator + "-fx-background-color: transparent;");
    }

    private static void writeWithDpi(BufferedImage image, File file, double dpi) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            IIOMetadata metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(image), param);

            String format = "javax_imageio_png_1.0";
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            int pixelsPerMeter = (int) Math.round(dpi / 0.0254);
            IIOMetadataNode phys = new IIOMetadataNode("pHYs");
            phys.setAttribute("pixelsPerUnitXAxis", Integer.toString(pixelsPerMeter));
            phys.setAttribute("pixelsPerUnitYAxis", Integer.toString(pixelsPerMeter));
            phys.setAttribute("unitSpecifier", "meter");
            root.appendChild(phys);
            metadata.mergeTree(format, root);

            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, metadata), param);
        } finally {
            writer.dispose();
        }
    }
}
