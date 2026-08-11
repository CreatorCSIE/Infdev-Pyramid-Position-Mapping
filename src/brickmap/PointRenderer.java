package brickmap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Random;

/**
 * 负责基于当前可视区域与 PointProvider 绘制散点。
 */
public class PointRenderer {

    private static final float COLOR_SATURATION = 0.8f;  // 饱和度保持较高
    private static final float COLOR_BRIGHTNESS = 0.9f;  // 亮度稍高保证可见性

    public void draw(Graphics2D g2, int w, int h, Viewport viewport,
                     PointProvider provider, Point2D.Double selectedPoint) {
        if (provider == null) return;

        double scale = viewport.getScale();
        double ox = viewport.getOffsetX();
        double oy = viewport.getOffsetY();

        double left = (-w / 2.0) / scale - ox;
        double right = (w / 2.0) / scale - ox;
        double top = (h / 2.0) / scale - oy;
        double bottom = (-h / 2.0) / scale - oy;

        List<Point2D.Double> visiblePoints = provider.getVisiblePoints(left, right, bottom, top);

        double r = 10 / scale;
        for (Point2D.Double pt : visiblePoints) {
            g2.setColor(pt.equals(selectedPoint) ?
                    Color.BLUE :
                    generateUniqueColor(pt.x, pt.y));
            g2.fill(new Ellipse2D.Double(pt.x - r / 2, pt.y - r / 2, r, r));
        }
    }

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
}