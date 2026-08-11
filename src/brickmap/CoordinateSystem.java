package brickmap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 主绘图面板：负责组装 Viewport / GridRenderer / PointRenderer 并处理鼠标交互。
 */
public class CoordinateSystem extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    private final Viewport viewport = new Viewport();
    private final GridRenderer gridRenderer = new GridRenderer();
    private final PointRenderer pointRenderer = new PointRenderer();

    private PointProvider pointProvider;
    private Point lastMouse;
    Point2D.Double selectedPoint = null;

    private final List<Point2D.Double> points = new ArrayList<>();

    public void setRenderLegacyBlueArea(boolean render) {
        gridRenderer.setRenderLegacyBlueArea(render);
        repaint();
    }

    public CoordinateSystem() {
        setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void setPointProvider(PointProvider provider) {
        this.pointProvider = provider;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2.translate(w / 2, h / 2);
        g2.scale(-viewport.getScale(), -viewport.getScale()); // y向上，x向左为正
        g2.translate(viewport.getOffsetX(), viewport.getOffsetY());

        gridRenderer.draw(g2, w, h, viewport);
        pointRenderer.draw(g2, w, h, viewport, pointProvider, selectedPoint);

        g2.dispose();

        gridRenderer.drawLabels((Graphics2D) g, w, h, viewport, selectedPoint);
        gridRenderer.drawCrosshair((Graphics2D) g, w, h);
    }

    // ------------------------------------------------------------------
    // 鼠标交互
    // ------------------------------------------------------------------

    @Override
    public void mousePressed(MouseEvent e) {
        lastMouse = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        viewport.pan(p.x - lastMouse.x, p.y - lastMouse.y);
        lastMouse = p;
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = Math.pow(1.1, -e.getPreciseWheelRotation());
        // 围绕屏幕中心（十字准星处）缩放，而非光标位置
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        viewport.zoomAt(cx, cy, factor, getWidth(), getHeight());
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            selectedPoint = null;
            if (pointProvider != null) {
                int w = getWidth();
                int h = getHeight();
                double left = (-w / 2.0) / viewport.getScale() - viewport.getOffsetX();
                double right = (w / 2.0) / viewport.getScale() - viewport.getOffsetX();
                double bottom = (-h / 2.0) / viewport.getScale() - viewport.getOffsetY();
                double top = (h / 2.0) / viewport.getScale() - viewport.getOffsetY();

                List<Point2D.Double> visiblePoints = pointProvider.getVisiblePoints(left, right, bottom, top);
                // 用屏幕像素距离判断，与渲染圆大小一致，避免视图微移导致的反算坐标错位
                Point click = e.getPoint();
                double threshold = 10; // 屏幕像素半径

                for (Point2D.Double pt : visiblePoints) {
                    Point sp = viewport.worldToScreen(pt.x, pt.y, w, h);
                    if (sp.distance(click) <= threshold) {
                        selectedPoint = pt;
                        break;
                    }
                }
            }
            repaint();
        }
    }

    // Unused
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}

    // ------------------------------------------------------------------
    // 视口控制（委托给 Viewport）
    // ------------------------------------------------------------------

    public void addPoint(double x, double y) {
        points.add(new Point2D.Double(x, y));
        repaint();
    }

    public void clearPoints() {
        points.clear();
        selectedPoint = null;
        repaint();
    }

    public void setViewport(double centerX, double centerY, double zoomLevel) {
        viewport.setViewport(centerX, centerY, zoomLevel);
        repaint();
    }

    public void resetViewport() {
        viewport.reset();
        selectedPoint = null;
        repaint();
    }

    public void centerOn(double worldX, double worldY) {
        viewport.centerOn(worldX, worldY);
        repaint();
    }
}