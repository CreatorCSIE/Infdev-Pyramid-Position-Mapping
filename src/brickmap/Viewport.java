package brickmap;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * 封装视口变换：缩放(scale)、偏移(offsetX/offsetY)以及
 * 屏幕坐标与世界坐标之间的互逆转换。
 */
public class Viewport {

    private double scale = 0.05;
    private double offsetX = 0;
    private double offsetY = 0;

    public double getScale() {
        return scale;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    /** 屏幕坐标 -> 世界坐标（与 worldToScreen 严格互逆） */
    public Point2D.Double screenToWorld(int x, int y, int width, int height) {
        double wx = -(x - width / 2.0) / scale - offsetX;
        double wy = (height / 2.0 - y) / scale - offsetY;
        return new Point2D.Double(wx, wy);
    }

    /** 世界坐标 -> 屏幕坐标 */
    public Point worldToScreen(double wx, double wy, int width, int height) {
        int sx = (int) ((-wx - offsetX) * scale + width / 2.0);
        int sy = (int) (height / 2.0 - (wy + offsetY) * scale);
        return new Point(sx, sy);
    }

    /** 按像素位移平移视口 */
    public void pan(int dxPixels, int dyPixels) {
        offsetX -= dxPixels / scale;
        offsetY -= dyPixels / scale;
    }

    /** 以鼠标位置为中心缩放视口 */
    public void zoomAt(int mouseX, int mouseY, double factor, int width, int height) {
        Point2D.Double before = screenToWorld(mouseX, mouseY, width, height);
        scale *= factor;
        if (scale < 0.05) scale = 0.05;
        if (scale > 0.25) scale = 0.25;
        Point2D.Double after = screenToWorld(mouseX, mouseY, width, height);
        offsetX += (after.x - before.x);
        offsetY += (after.y - before.y);
    }

    public void setViewport(double centerX, double centerY, double zoomLevel) {
        scale = zoomLevel;
        offsetX = -centerX;
        offsetY = -centerY;
    }

    public void reset() {
        scale = 0.05;
        offsetX = 0;
        offsetY = 0;
    }

    public void centerOn(double worldX, double worldY) {
        offsetX = -worldX;
        offsetY = -worldY;
    }
}