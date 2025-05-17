import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CoordinateSystem extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    private double scale = 0.05;
    private double offsetX = 0;
    private double offsetY = 0;
    private double debugMouseX;
    private double debugMouseY;

    private Point lastMouse;
    private final List<Point2D.Double> points = new ArrayList<>();
    Point2D.Double selectedPoint = null;

    private static final int BASE_STEP = 512;
    
    private static final float COLOR_SATURATION = 0.8f;  // 饱和度保持较高
    private static final float COLOR_BRIGHTNESS = 0.9f;  // 亮度稍高保证可见性

    private Color generateUniqueColor(double x, double y) {
        // 将坐标转换为唯一哈希值
        long hash = Double.hashCode(x) ^ (Double.hashCode(y) << 17);
        Random rand = new Random(hash);
        
        // 生成HSL颜色参数
        float hue = rand.nextFloat();       // 0.0-1.0全色相范围
        float saturation = COLOR_SATURATION - rand.nextFloat() * 0.2f; // ±10%饱和度变化
        float brightness = COLOR_BRIGHTNESS - rand.nextFloat() * 0.1f; // ±5%亮度变化
        
        // 转换为RGB颜色
        return Color.getHSBColor(hue, saturation, brightness);
    }
    
    private boolean renderLegacyBlueArea = true;
    
    public void setRenderLegacyBlueArea(boolean render) {
        this.renderLegacyBlueArea = render;
        repaint();
    }

    public CoordinateSystem() {
        setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    
    public interface PointProvider {
        List<Point2D.Double> getVisiblePoints(double left, double right, double bottom, double top);
    }

    private PointProvider pointProvider;

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
        g2.scale(-scale, -scale); // y向上，x向左为正
        g2.translate(offsetX, offsetY);

        drawQuadrants(g2, w, h);
        drawGridAndAxes(g2, w, h);
        drawPoints(g2);

        g2.dispose();

        drawLabels((Graphics2D) g, w, h);
    }

    private void drawQuadrants(Graphics2D g2, int w, int h) {
        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2.setColor(Color.PINK);

        // 当前世界坐标边界
        double worldLeft   = (-w / 2.0) / scale - offsetX;
        double worldRight  = ( w / 2.0) / scale - offsetX;
        double worldTop    = ( h / 2.0) / scale - offsetY;
        double worldBottom = (-h / 2.0) / scale - offsetY;

        // 第二象限：x < 0 && y > 0
        if (worldLeft < 0 && worldTop > 0) {
            double x = worldLeft;
            double y = 0;
            double width = Math.min(0, worldRight) - x;
            double height = worldTop;
            g2.fill(new Rectangle2D.Double(x, y, width, height));
        }

        // 第三象限：x < 0 && y < 0
        if (worldLeft < 0 && worldBottom < 0) {
            double x = worldLeft;
            double y = worldBottom;
            double width = Math.min(0, worldRight) - x;
            double height = -y;
            g2.fill(new Rectangle2D.Double(x, y, width, height));
        }

        // 第四象限：x > 0 && y < 0
        if (worldRight > 0 && worldBottom < 0) {
            double x = 0;
            double y = worldBottom;
            double width = worldRight - x;
            double height = -y;
            g2.fill(new Rectangle2D.Double(x, y, width, height));
        }
        
        if (renderLegacyBlueArea) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(new Color(0, 0, 255));
            
            // 定义蓝色区域边界（X/Z范围-512到1024）
            double blueLeft = -512;
            double blueRight = 1024;
            double blueBottom = -512;
            double blueTop = 1024;

            // 确定实际绘制范围
            double renderLeft = Math.max(blueLeft, worldLeft);
            double renderRight = Math.min(blueRight, worldRight);
            double renderBottom = Math.max(blueBottom, worldBottom);
            double renderTop = Math.min(blueTop, worldTop);

            // 当可视区域与蓝色区域有交集时绘制
            if (renderLeft < renderRight && renderBottom < renderTop) {
                g2.fill(new Rectangle2D.Double(
                    renderLeft,
                    renderBottom,
                    renderRight - renderLeft,
                    renderTop - renderBottom
                ));
            }
        }

        g2.setComposite(original);
    }

    private void drawGridAndAxes(Graphics2D g2, int w, int h) {
        g2.setStroke(new BasicStroke(1 / (float) scale));
        g2.setColor(Color.LIGHT_GRAY);

        double pixelStep = BASE_STEP * scale;
        double step = BASE_STEP;

        // 动态调整步长，使像素距离在合理范围
        while (pixelStep < 30) {
            step *= 2;
            pixelStep = step * scale;
        }
        while (pixelStep > 150) {
            step /= 2;
            pixelStep = step * scale;
        }

        double left = (-w / 2.0) / scale - offsetX;
        double right = (w / 2.0) / scale - offsetX;
        double top = (h / 2.0) / scale - offsetY;
        double bottom = (-h / 2.0) / scale - offsetY;

        // 垂直网格线（X方向）
        for (double x = Math.floor(left / step) * step; x <= right; x += step) {
            g2.draw(new Line2D.Double(x, bottom, x, top));
        }

        // 水平网格线（Y方向）
        for (double y = Math.floor(bottom / step) * step; y <= top; y += step) {
            g2.draw(new Line2D.Double(left, y, right, y));
        }

        // 坐标轴
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2 / (float) scale));
        g2.draw(new Line2D.Double(left, 0, right, 0)); // X轴
        g2.draw(new Line2D.Double(0, bottom, 0, top)); // Y轴
    }

    private void drawLabels(Graphics2D g, int w, int h) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 12));

        double step = BASE_STEP;
        double pixelStep = step * scale;
        while (pixelStep < 30) {
            step *= 2;
            pixelStep = step * scale;
        }
        while (pixelStep > 150) {
            step /= 2;
            pixelStep = step * scale;
        }

        // 遍历坐标轴刻度
        double left = (-w / 2.0) / scale - offsetX;
        double right = (w / 2.0) / scale - offsetX;
        double top = (h / 2.0) / scale - offsetY;
        double bottom = (-h / 2.0) / scale - offsetY;

        // X轴刻度
        for (double x = Math.floor(left / step) * step; x <= right; x += step) {
            if (Math.abs(x) < 1e-6) continue;
            Point p = worldToScreen(x, 0, w, h);
            g.drawString(String.format("%.0f", x), p.x - 10, p.y + 15);
        }

        // Y轴刻度
        for (double y = Math.floor(bottom / step) * step; y <= top; y += step) {
            if (Math.abs(y) < 1e-6) continue;
            Point p = worldToScreen(0, y, w, h);
            g.drawString(String.format("%.0f", y), p.x + 5, p.y + 5);
        }

        // 绘制选中点坐标
        if (selectedPoint != null) {
            Point p = worldToScreen(selectedPoint.x, selectedPoint.y, w, h);
            g.setColor(Color.BLUE);
            g.drawString(String.format("(%.0f, %.0f)", selectedPoint.x, selectedPoint.y), p.x + 5, p.y - 5);
        }
        
        Point2D.Double viewportCenter = screenToWorld(w/2, h/2);
        
        g.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        String debugText = String.format("缩放倍数：%.2f  视点坐标：(%.0f, %.0f)", 
                          scale, viewportCenter.x, viewportCenter.y);
        int x = 10;
        int y = h - fm.getDescent() - 5;
        g.drawString(debugText, x, y);
    }

    private void drawPoints(Graphics2D g2) {
    	if (pointProvider == null) return;
    	
    	int w = getWidth();
        int h = getHeight();
        double left = (-w / 2.0) / scale - offsetX;
        double right = (w / 2.0) / scale - offsetX;
        double top = (h / 2.0) / scale - offsetY;
        double bottom = (-h / 2.0) / scale - offsetY;
        
        List<Point2D.Double> visiblePoints = pointProvider.getVisiblePoints(left, right, bottom, top);
    	
        double r = 10 / scale;
        for (Point2D.Double pt : visiblePoints) {
        	int chunkX = (int)(pt.x) >> 10; // 等价于除以1024
            int chunkZ = (int)(pt.y) >> 10;
            
            g2.setColor(pt.equals(selectedPoint) ? 
                    Color.BLUE : 
                    generateUniqueColor(pt.x, pt.y));
            g2.fill(new Ellipse2D.Double(pt.x - r / 2, pt.y - r / 2, r, r));
        }
    }

    private Point2D.Double screenToWorld(int x, int y) {
        // 修正Y轴坐标转换
        double wx = -(x - getWidth() / 2.0) / scale - offsetX;
        double wy = (getHeight() / 2.0 - y) / scale - offsetY - 10;
        return new Point2D.Double(wx, wy);
    }

    private Point worldToScreen(double wx, double wy, int w, int h) {
        // 修正Y轴坐标转换
        int sx = (int) ((-wx - offsetX) * scale + w / 2.0);
        int sy = (int) (h / 2.0 - (wy + offsetY) * scale);
        return new Point(sx, sy);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMouse = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        double dx = (p.x - lastMouse.x) / scale;
        double dy = (p.y - lastMouse.y) / scale;
        offsetX -= dx;
        offsetY -= dy;
        lastMouse = p;
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = Math.pow(1.1, -e.getPreciseWheelRotation());
        Point p = e.getPoint();
        Point2D.Double beforeZoom = screenToWorld(p.x, p.y);

        scale *= factor;
        if (scale < 0.05) scale = 0.05;
        if (scale > 0.25) scale = 0.25;

        Point2D.Double afterZoom = screenToWorld(p.x, p.y);
        offsetX += (afterZoom.x - beforeZoom.x);
        offsetY += (afterZoom.y - beforeZoom.y);

        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point2D.Double world = screenToWorld(e.getX(), e.getY());

        if (SwingUtilities.isLeftMouseButton(e)) {
            selectedPoint = null;
            if (pointProvider != null) {
                int w = getWidth();
                int h = getHeight();
                double left = (-w / 2.0) / scale - offsetX;
                double right = (w / 2.0) / scale - offsetX;
                double bottom = (-h / 2.0) / scale - offsetY;
                double top = (h / 2.0) / scale - offsetY;

                List<Point2D.Double> visiblePoints = pointProvider.getVisiblePoints(left, right, bottom, top);
                double threshold = 10 / scale; // 匹配点的实际半径

                for (Point2D.Double pt : visiblePoints) {
                    if (world.distance(pt) < threshold) {
                        selectedPoint = pt;
                        break;
                    }
                }
            }
            repaint();
        }
    }
    
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
        scale = zoomLevel;
        offsetX = -centerX;
        offsetY = -centerY;
        repaint();
    }
    
    public void resetViewport() {
        scale = 0.05;
        offsetX = 0;
        offsetY = 0;
        selectedPoint = null;
        repaint();
    }

    public void centerOn(double worldX, double worldY) {
        offsetX = -worldX;
        offsetY = -worldY;
        repaint();
    }

    // Unused
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}
