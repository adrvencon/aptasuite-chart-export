package chartexport.pdf;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

final class PdfCanvas {

    static final float PX_TO_PT = 72f / 96f;

    private static final String[] FALLBACK_FAMILIES =
            {"Segoe UI", "Arial", "Helvetica", "Liberation Sans", "DejaVu Sans"};

    private static boolean systemFontsRegistered;

    private final PdfContentByte cb;
    private final Map<Float, PdfGState> opacityStates = new HashMap<>();
    private final Map<String, BaseFont> baseFonts = new HashMap<>();

    PdfCanvas(PdfContentByte cb, double heightPx) {
        this.cb = cb;
        cb.saveState();
        cb.concatCTM(PX_TO_PT, 0f, 0f, -PX_TO_PT, 0f, (float) (heightPx * PX_TO_PT));
    }

    void finish() {
        cb.restoreState();
    }

    void save() {
        cb.saveState();
    }

    void restore() {
        cb.restoreState();
    }

    void transform(AffineTransform t) {
        double[] m = new double[6];
        t.getMatrix(m);
        cb.concatCTM((float) m[0], (float) m[1], (float) m[2], (float) m[3], (float) m[4], (float) m[5]);
    }

    void clip(Shape area) {
        appendPath(area);
        cb.clip();
        cb.newPath();
    }

    void fill(Shape shape, Color color) {
        cb.saveState();
        cb.setColorFill(toBaseColor(color));
        applyOpacity(color.getOpacity());
        appendPath(shape);
        if (shape.getPathIterator(null).getWindingRule() == PathIterator.WIND_EVEN_ODD) {
            cb.eoFill();
        } else {
            cb.fill();
        }
        cb.restoreState();
    }

    void stroke(Shape shape, BasicStroke stroke, Color color) {
        cb.saveState();
        cb.setColorStroke(toBaseColor(color));
        applyOpacity(color.getOpacity());
        cb.setLineWidth(stroke.getLineWidth());
        cb.setLineCap(stroke.getEndCap());
        cb.setLineJoin(stroke.getLineJoin());
        cb.setMiterLimit(stroke.getMiterLimit());
        float[] dash = stroke.getDashArray();
        if (dash != null) {
            cb.setLineDash(dash, stroke.getDashPhase());
        }
        appendPath(shape);
        cb.stroke();
        cb.restoreState();
    }

    void text(String text, double x, double baselineY, Font font, Color color) {
        BaseFont base = baseFont(font);
        cb.saveState();
        cb.setColorFill(toBaseColor(color));
        applyOpacity(color.getOpacity());
        cb.beginText();
        cb.setFontAndSize(base, (float) font.getSize());
        cb.setTextMatrix(1f, 0f, 0f, -1f, (float) x, (float) baselineY);
        cb.showText(text);
        cb.endText();
        cb.restoreState();
    }

    private void appendPath(Shape shape) {
        double[] c = new double[6];
        double px = 0;
        double py = 0;
        double startX = 0;
        double startY = 0;
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO:
                    cb.moveTo((float) c[0], (float) c[1]);
                    px = c[0];
                    py = c[1];
                    startX = px;
                    startY = py;
                    break;
                case PathIterator.SEG_LINETO:
                    cb.lineTo((float) c[0], (float) c[1]);
                    px = c[0];
                    py = c[1];
                    break;
                case PathIterator.SEG_QUADTO: {
                    double x1 = px + 2.0 / 3.0 * (c[0] - px);
                    double y1 = py + 2.0 / 3.0 * (c[1] - py);
                    double x2 = c[2] + 2.0 / 3.0 * (c[0] - c[2]);
                    double y2 = c[3] + 2.0 / 3.0 * (c[1] - c[3]);
                    cb.curveTo((float) x1, (float) y1, (float) x2, (float) y2, (float) c[2], (float) c[3]);
                    px = c[2];
                    py = c[3];
                    break;
                }
                case PathIterator.SEG_CUBICTO:
                    cb.curveTo((float) c[0], (float) c[1], (float) c[2], (float) c[3],
                            (float) c[4], (float) c[5]);
                    px = c[4];
                    py = c[5];
                    break;
                case PathIterator.SEG_CLOSE:
                    cb.closePath();
                    px = startX;
                    py = startY;
                    break;
                default:
                    break;
            }
        }
    }

    private static BaseColor toBaseColor(Color c) {
        return new BaseColor((int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }

    private void applyOpacity(double alpha) {
        if (alpha >= 1.0) {
            return;
        }
        float key = Math.round(alpha * 1000) / 1000f;
        PdfGState state = opacityStates.get(key);
        if (state == null) {
            state = new PdfGState();
            state.setFillOpacity(key);
            state.setStrokeOpacity(key);
            opacityStates.put(key, state);
        }
        cb.setGState(state);
    }

    private BaseFont baseFont(Font f) {
        String style = f.getStyle() == null ? "" : f.getStyle().toLowerCase(Locale.ROOT);
        boolean bold = style.contains("bold");
        boolean italic = style.contains("italic") || style.contains("oblique");
        String key = f.getFamily() + "|" + bold + "|" + italic;
        BaseFont base = baseFonts.get(key);
        if (base == null) {
            base = createBaseFont(f.getFamily(), bold, italic);
            baseFonts.put(key, base);
        }
        return base;
    }

    private static BaseFont createBaseFont(String family, boolean bold, boolean italic) {
        String resolved = resolveFamily(family);
        if (resolved != null) {
            int style = (bold ? com.itextpdf.text.Font.BOLD : 0) | (italic ? com.itextpdf.text.Font.ITALIC : 0);
            com.itextpdf.text.Font font =
                    FontFactory.getFont(resolved, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10f, style);
            if (font.getBaseFont() != null) {
                return font.getBaseFont();
            }
        }
        String standard;
        if (bold) {
            standard = italic ? BaseFont.HELVETICA_BOLDOBLIQUE : BaseFont.HELVETICA_BOLD;
        } else {
            standard = italic ? BaseFont.HELVETICA_OBLIQUE : BaseFont.HELVETICA;
        }
        try {
            return BaseFont.createFont(standard, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No font available for the PDF export", e);
        }
    }

    private static String resolveFamily(String family) {
        registerSystemFonts();
        if (family != null && FontFactory.isRegistered(family.toLowerCase(Locale.ROOT))) {
            return family;
        }
        for (String candidate : FALLBACK_FAMILIES) {
            if (FontFactory.isRegistered(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    private static synchronized void registerSystemFonts() {
        if (!systemFontsRegistered) {
            systemFontsRegistered = true;
            FontFactory.registerDirectories();
        }
    }
}
