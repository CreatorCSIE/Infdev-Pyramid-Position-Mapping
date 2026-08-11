package brickmap;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * 提供当前可视区域内需要绘制的点。
 */
public interface PointProvider {
    List<Point2D.Double> getVisiblePoints(double left, double right, double bottom, double top);
}