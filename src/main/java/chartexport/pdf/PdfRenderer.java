package chartexport.pdf;

import java.io.File;
import java.io.FileOutputStream;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.scene.chart.Chart;

public final class PdfRenderer {

    private PdfRenderer() {
    }

    public static void write(Chart chart, File file) throws Exception {
        double width = chart.getWidth();
        double height = chart.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("The chart has no size yet.");
        }

        Document document = new Document(
                new com.itextpdf.text.Rectangle(
                        (float) (width * PdfCanvas.PX_TO_PT), (float) (height * PdfCanvas.PX_TO_PT)),
                0, 0, 0, 0);
        boolean completed = false;
        try (FileOutputStream stream = new FileOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(document, stream);
            document.addCreator("AptaSUITE Chart Export");
            document.open();

            PdfCanvas canvas = new PdfCanvas(writer.getDirectContent(), height);
            SceneGraphPainter.paint(chart, canvas);
            canvas.finish();

            document.close();
            completed = true;
        } finally {
            if (!completed) {
                file.delete();
            }
        }
    }
}
