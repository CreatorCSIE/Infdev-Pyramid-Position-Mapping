package brickmap;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * 负责绘制象限着色、网格坐标轴以及刻度标签。
 */
public class GridRenderer {

    private static final int BASE_STEP = 512;

    private boolean renderLegacyBlueArea = true;

    public void setRenderLegacyBlueArea(boolean render) {
        this.renderLegacyBlueArea = render;
    }

    /**
     * 在屏幕中心绘制反色十字准星（固定 32px 长、4px 宽，不随缩放变化）。
     * 使用 XOR 模式实现反色，确保在深色点或白色背景上都清晰可见。
     * 水平线分成左右两段并避开中心交叉区，避免交叉处被 XOR 两次而「负负得正」。
     */
    public void drawCrosshair(Graphics2D g, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;
        // 前景与 xorColor 必须相反才能实现反色：结果 = 黑 XOR 白 XOR 像素 = 像素反相
        g.setColor(Color.BLACK);
        g.setXORMode(Color.WHITE);
        // 垂直线完整贯穿：4 宽 × 32 高
        g.fillRect(cx - 2, cy - 16, 4, 32);
        // 水平线分两段（避开中心 4px 交叉区）：左段 14 宽 + 右段 14 宽 = 32 总长
        g.fillRect(cx - 16, cy - 2, 14, 4);
        g.fillRect(cx + 2, cy - 2, 14, 4);
        g.setPaintMode();
    }

    public void draw(Graphics2D g2, int w, int h, Viewport viewport) {
        drawQuadrants(g2, w, h, viewport);
        drawGridAndAxes(g2, w, h, viewport);
    }

    public void drawLabels(Graphics2D g, int w, int h, Viewport viewport, Point2D.Double selectedPoint) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();

        double scale = viewport.getScale();
        double step = computeStep(scale);

        // 世界坐标边界
        double left = (-w / 2.0) / scale - viewport.getOffsetX();
        double right = (w / 2.0) / scale - viewport.getOffsetX();
        double top = (h / 2.0) / scale - viewport.getOffsetY();
        double bottom = (-h / 2.0) / scale - viewport.getOffsetY();

        // X轴刻度（跳过与已放置标签重叠的，避免八位数标签互相遮盖）
        // 注意：世界 x 增大对应屏幕 x 减小，故按从右到左的顺序放置
        double lastLeft = Double.POSITIVE_INFINITY;
        for (double x = Math.floor(left / step) * step; x <= right; x += step) {
            if (Math.abs(x) < 1e-6) continue;
            Point p = viewport.worldToScreen(x, 0, w, h);
            String label = String.format("%.0f", x);
            int textLeft = p.x - 10;
            int textRight = textLeft + fm.stringWidth(label) + 4; // 4px 空隙
            if (textRight > lastLeft) continue; // 与右侧已放置标签重叠，跳过
            g.drawString(label, textLeft, p.y + 15);
            lastLeft = textLeft;
        }

        // Y轴刻度
        for (double y = Math.floor(bottom / step) * step; y <= top; y += step) {
            if (Math.abs(y) < 1e-6) continue;
            Point p = viewport.worldToScreen(0, y, w, h);
            g.drawString(String.format("%.0f", y), p.x + 5, p.y + 5);
        }

        // 绘制选中点坐标
        if (selectedPoint != null) {
            Point p = viewport.worldToScreen(selectedPoint.x, selectedPoint.y, w, h);
            g.setColor(Color.BLUE);
            g.drawString(String.format("(%.0f, %.0f)", selectedPoint.x, selectedPoint.y), p.x + 5, p.y - 5);
        }

        // 屏幕中心对应的世界坐标（不受窗口奇偶尺寸取整影响）
        double cx = -viewport.getOffsetX();
        double cy = -viewport.getOffsetY();
        // 归零处理，避免显示 -0
        if (Math.abs(cx) < 1) cx = 0;
        if (Math.abs(cy) < 1) cy = 0;

        g.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        String debugText = String.format("缩放倍数：%.2f  视点坐标：(%.0f, %.0f)",
                scale, cx, cy);
        int x = 10;
        int y = h - g.getFontMetrics().getDescent() - 5;
        g.drawString(debugText, x, y);
    }

    private void drawQuadrants(Graphics2D g2, int w, int h, Viewport viewport) {
        double scale = viewport.getScale();
        double ox = viewport.getOffsetX();
        double oy = viewport.getOffsetY();

        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2.setColor(Color.PINK);

        // 当前世界坐标边界
        double worldLeft = (-w / 2.0) / scale - ox;
        double worldRight = (w / 2.0) / scale - ox;
        double worldTop = (h / 2.0) / scale - oy;
        double worldBottom = (-h / 2.0) / scale - oy;

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

    private void drawGridAndAxes(Graphics2D g2, int w, int h, Viewport viewport) {
        double scale = viewport.getScale();
        double ox = viewport.getOffsetX();
        double oy = viewport.getOffsetY();

        g2.setStroke(new BasicStroke(1 / (float) scale));
        g2.setColor(Color.LIGHT_GRAY);

        double step = computeStep(scale);

        double left = (-w / 2.0) / scale - ox;
        double right = (w / 2.0) / scale - ox;
        double top = (h / 2.0) / scale - oy;
        double bottom = (-h / 2.0) / scale - oy;

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

    /** 根据当前缩放动态计算网格步长，使像素间距保持在合理范围 */
    private double computeStep(double scale) {
        double pixelStep = BASE_STEP * scale;
        double step = BASE_STEP;
        while (pixelStep < 30) {
            step *= 2;
            pixelStep = step * scale;
        }
        while (pixelStep > 150) {
            step /= 2;
            pixelStep = step * scale;
        }
        return step;
    }
}