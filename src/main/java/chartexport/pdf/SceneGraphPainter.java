package chartexport.pdf;

import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Objects;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Chart;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;

final class SceneGraphPainter {

    private final PdfCanvas canvas;

    private SceneGraphPainter(PdfCanvas canvas) {
        this.canvas = canvas;
    }

    static void paint(Chart chart, PdfCanvas canvas) {
        new SceneGraphPainter(canvas).paintNode(chart, false, 1.0);
    }

    private void paintNode(Node node, boolean withTransform, double inheritedOpacity) {
        if (!node.isVisible()) {
            return;
        }
        double opacity = inheritedOpacity * node.getOpacity();
        if (opacity <= 0.0) {
            return;
        }

        canvas.save();
        if (withTransform) {
            canvas.transform(toAwt(node.getLocalToParentTransform()));
        }
        Node clip = node.getClip();
        if (clip instanceof Rectangle) {
            Rectangle r = (Rectangle) clip;
            Rectangle2D area = new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
            canvas.clip(toAwt(clip.getLocalToParentTransform()).createTransformedShape(area));
        }

        if (node instanceof Region) {
            paintRegion((Region) node, opacity);
        }
        if (node instanceof Text) {
            paintText((Text) node, opacity);
        } else if (node instanceof Shape) {
            paintShape((Shape) node, opacity);
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                paintNode(child, true, opacity);
            }
        }
        canvas.restore();
    }

    private static AffineTransform toAwt(Transform t) {
        return new AffineTransform(t.getMxx(), t.getMyx(), t.getMxy(), t.getMyy(), t.getTx(), t.getTy());
    }

    private void paintRegion(Region r, double opacity) {
        double w = r.getWidth();
        double h = r.getHeight();

        Background bg = r.getBackground();
        if (bg != null) {
            for (BackgroundFill f : bg.getFills()) {
                Color fill = solid(f.getFill(), opacity);
                if (fill == null) {
                    continue;
                }
                Insets in = f.getInsets();
                double rw = w - in.getLeft() - in.getRight();
                double rh = h - in.getTop() - in.getBottom();
                if (rw <= 0 || rh <= 0) {
                    continue;
                }
                canvas.fill(roundRect(in.getLeft(), in.getTop(), rw, rh, f.getRadii(), 0), fill);
            }
        }

        Border border = r.getBorder();
        if (border != null) {
            for (BorderStroke s : border.getStrokes()) {
                paintBorder(s, w, h, opacity);
            }
        }
    }

    private static java.awt.Shape roundRect(double x, double y, double w, double h,
            CornerRadii cr, double shrink) {
        double rx = cr.getTopLeftHorizontalRadius();
        if (cr.isTopLeftHorizontalRadiusAsPercentage()) {
            rx *= w;
        }
        double ry = cr.getTopLeftVerticalRadius();
        if (cr.isTopLeftVerticalRadiusAsPercentage()) {
            ry *= h;
        }
        rx = Math.max(0, Math.min(rx - shrink, w / 2));
        ry = Math.max(0, Math.min(ry - shrink, h / 2));
        if (rx > 0 || ry > 0) {
            return new RoundRectangle2D.Double(x, y, w, h, rx * 2, ry * 2);
        }
        return new Rectangle2D.Double(x, y, w, h);
    }

    private void paintBorder(BorderStroke s, double w, double h, double opacity) {
        BorderWidths bw = s.getWidths();
        Insets in = s.getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double rw = w - in.getLeft() - in.getRight();
        double rh = h - in.getTop() - in.getBottom();
        if (rw <= 0 || rh <= 0) {
            return;
        }
        double t = bw.getTop();
        double r = bw.getRight();
        double b = bw.getBottom();
        double l = bw.getLeft();

        boolean uniform = t == r && r == b && b == l
                && Objects.equals(s.getTopStroke(), s.getRightStroke())
                && Objects.equals(s.getTopStroke(), s.getBottomStroke())
                && Objects.equals(s.getTopStroke(), s.getLeftStroke())
                && Objects.equals(s.getTopStyle(), s.getRightStyle())
                && Objects.equals(s.getTopStyle(), s.getBottomStyle())
                && Objects.equals(s.getTopStyle(), s.getLeftStyle());

        if (uniform) {
            Color color = solid(s.getTopStroke(), opacity);
            if (t <= 0 || s.getTopStyle() == BorderStrokeStyle.NONE || color == null) {
                return;
            }
            java.awt.Shape outline = roundRect(x + t / 2, y + t / 2, rw - t, rh - t, s.getRadii(), t / 2);
            canvas.stroke(outline, new BasicStroke((float) t), color);
        } else {
            side(x, y + t / 2, x + rw, y + t / 2, t, s.getTopStroke(), s.getTopStyle(), opacity);
            side(x, y + rh - b / 2, x + rw, y + rh - b / 2, b, s.getBottomStroke(), s.getBottomStyle(), opacity);
            side(x + l / 2, y, x + l / 2, y + rh, l, s.getLeftStroke(), s.getLeftStyle(), opacity);
            side(x + rw - r / 2, y, x + rw - r / 2, y + rh, r, s.getRightStroke(), s.getRightStyle(), opacity);
        }
    }

    private void side(double x1, double y1, double x2, double y2, double width,
            Paint paint, BorderStrokeStyle style, double opacity) {
        Color color = solid(paint, opacity);
        if (width <= 0 || style == BorderStrokeStyle.NONE || color == null) {
            return;
        }
        canvas.stroke(new Line2D.Double(x1, y1, x2, y2), new BasicStroke((float) width), color);
    }

    private void paintText(Text t, double opacity) {
        String content = t.getText();
        if (content == null || content.isEmpty()) {
            return;
        }
        Color fill = solid(t.getFill(), opacity);
        if (fill == null) {
            return;
        }
        Bounds lb = t.getLayoutBounds();
        canvas.text(content, lb.getMinX(), lb.getMinY() + t.getBaselineOffset(), t.getFont(), fill);
    }

    private void paintShape(Shape s, double opacity) {
        java.awt.Shape geometry = toGeometry(s);
        if (geometry == null) {
            return;
        }
        Color fill = solid(s.getFill(), opacity);
        if (fill != null) {
            canvas.fill(geometry, fill);
        }
        Color stroke = solid(s.getStroke(), opacity);
        if (stroke != null && s.getStrokeWidth() > 0) {
            canvas.stroke(geometry, toStroke(s), stroke);
        }
    }

    private static java.awt.Shape toGeometry(Shape s) {
        if (s instanceof Rectangle) {
            Rectangle r = (Rectangle) s;
            double d = 0;
            if (s.getStroke() != null && s.getStrokeWidth() > 0) {
                if (s.getStrokeType() == StrokeType.INSIDE) {
                    d = s.getStrokeWidth() / 2;
                } else if (s.getStrokeType() == StrokeType.OUTSIDE) {
                    d = -s.getStrokeWidth() / 2;
                }
            }
            double x = r.getX() + d;
            double y = r.getY() + d;
            double w = r.getWidth() - 2 * d;
            double h = r.getHeight() - 2 * d;
            if (r.getArcWidth() > 0 || r.getArcHeight() > 0) {
                return new RoundRectangle2D.Double(x, y, w, h, r.getArcWidth(), r.getArcHeight());
            }
            return new Rectangle2D.Double(x, y, w, h);
        }
        if (s instanceof Line) {
            Line l = (Line) s;
            return new Line2D.Double(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
        }
        if (s instanceof Circle) {
            Circle c = (Circle) s;
            return new Ellipse2D.Double(c.getCenterX() - c.getRadius(), c.getCenterY() - c.getRadius(),
                    2 * c.getRadius(), 2 * c.getRadius());
        }
        if (s instanceof Ellipse) {
            Ellipse e = (Ellipse) s;
            return new Ellipse2D.Double(e.getCenterX() - e.getRadiusX(), e.getCenterY() - e.getRadiusY(),
                    2 * e.getRadiusX(), 2 * e.getRadiusY());
        }
        if (s instanceof Polygon) {
            return polyline(((Polygon) s).getPoints(), true);
        }
        if (s instanceof Polyline) {
            return polyline(((Polyline) s).getPoints(), false);
        }
        if (s instanceof Path) {
            return toPath2D((Path) s);
        }
        return null;
    }

    private static Path2D polyline(List<Double> points, boolean close) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i + 1 < points.size(); i += 2) {
            if (i == 0) {
                path.moveTo(points.get(0), points.get(1));
            } else {
                path.lineTo(points.get(i), points.get(i + 1));
            }
        }
        if (close && points.size() >= 4) {
            path.closePath();
        }
        return path;
    }

    private static Path2D toPath2D(Path p) {
        Path2D path = new Path2D.Double(
                p.getFillRule() == FillRule.EVEN_ODD ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
        for (PathElement e : p.getElements()) {
            boolean abs = e.isAbsolute();
            Point2D cur = path.getCurrentPoint();
            if (cur == null && !(e instanceof MoveTo)) {
                continue; // a path must start with a MoveTo
            }
            double cx = cur == null ? 0 : cur.getX();
            double cy = cur == null ? 0 : cur.getY();
            double ox = abs ? 0 : cx;
            double oy = abs ? 0 : cy;

            if (e instanceof MoveTo) {
                MoveTo m = (MoveTo) e;
                path.moveTo(ox + m.getX(), oy + m.getY());
            } else if (e instanceof LineTo) {
                LineTo l = (LineTo) e;
                path.lineTo(ox + l.getX(), oy + l.getY());
            } else if (e instanceof HLineTo) {
                path.lineTo(ox + ((HLineTo) e).getX(), cy);
            } else if (e instanceof VLineTo) {
                path.lineTo(cx, oy + ((VLineTo) e).getY());
            } else if (e instanceof QuadCurveTo) {
                QuadCurveTo q = (QuadCurveTo) e;
                path.quadTo(ox + q.getControlX(), oy + q.getControlY(), ox + q.getX(), oy + q.getY());
            } else if (e instanceof CubicCurveTo) {
                CubicCurveTo c = (CubicCurveTo) e;
                path.curveTo(ox + c.getControlX1(), oy + c.getControlY1(),
                        ox + c.getControlX2(), oy + c.getControlY2(), ox + c.getX(), oy + c.getY());
            } else if (e instanceof ArcTo) {
                ArcTo a = (ArcTo) e;
                path.lineTo(ox + a.getX(), oy + a.getY());
            } else if (e instanceof ClosePath) {
                path.closePath();
            }
        }
        return path;
    }

    private static BasicStroke toStroke(Shape s) {
        int cap = BasicStroke.CAP_BUTT;
        if (s.getStrokeLineCap() == StrokeLineCap.ROUND) {
            cap = BasicStroke.CAP_ROUND;
        } else if (s.getStrokeLineCap() == StrokeLineCap.SQUARE) {
            cap = BasicStroke.CAP_SQUARE;
        }
        int join = BasicStroke.JOIN_MITER;
        if (s.getStrokeLineJoin() == StrokeLineJoin.ROUND) {
            join = BasicStroke.JOIN_ROUND;
        } else if (s.getStrokeLineJoin() == StrokeLineJoin.BEVEL) {
            join = BasicStroke.JOIN_BEVEL;
        }
        float miter = (float) Math.max(1.0, s.getStrokeMiterLimit());

        float[] dash = null;
        List<Double> dashes = s.getStrokeDashArray();
        if (dashes != null && !dashes.isEmpty()) {
            float[] values = new float[dashes.size()];
            float sum = 0;
            for (int i = 0; i < values.length; i++) {
                values[i] = (float) Math.max(0.0, dashes.get(i));
                sum += values[i];
            }
            if (sum > 0) {
                dash = values;
            }
        }
        float phase = (float) Math.max(0.0, s.getStrokeDashOffset());
        return new BasicStroke((float) s.getStrokeWidth(), cap, join, miter, dash, phase);
    }

    private static Color solid(Paint p, double opacity) {
        Color c = null;
        if (p instanceof Color) {
            c = (Color) p;
        } else if (p instanceof LinearGradient) {
            c = firstStop(((LinearGradient) p).getStops());
        } else if (p instanceof RadialGradient) {
            c = firstStop(((RadialGradient) p).getStops());
        }
        if (c == null) {
            return null;
        }
        double alpha = c.getOpacity() * opacity;
        if (alpha <= 0.0) {
            return null;
        }
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(1.0, alpha));
    }

    private static Color firstStop(List<Stop> stops) {
        return stops == null || stops.isEmpty() ? null : stops.get(0).getColor();
    }
}
