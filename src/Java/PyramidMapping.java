import javax.swing.*;

import java.util.Random;
import java.awt.BorderLayout;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class PyramidMapping {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Minecraft Infdev砖块金字塔坐标分布");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());

            // 创建控制面板
            JPanel controlPanel = new JPanel();
            JTextField xField = new JTextField(8);
            JTextField zField = new JTextField(8);
            JButton searchBtn = new JButton("定位");
            JButton resetBtn = new JButton("回到原点");

            controlPanel.add(new JLabel("X:"));
            controlPanel.add(xField);
            controlPanel.add(new JLabel("Z:"));
            controlPanel.add(zField);
            controlPanel.add(searchBtn);
            controlPanel.add(resetBtn);

            CoordinateSystem panel = new CoordinateSystem();
            CoordinateSystem.PointProvider provider = createPointProvider(); // 提取生成逻辑
            panel.setPointProvider(provider);

            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(panel, BorderLayout.CENTER);
            frame.setVisible(true);

            // 事件监听
            searchBtn.addActionListener(e -> handleSearch(xField, zField, panel, provider));
            resetBtn.addActionListener(e -> panel.resetViewport());
        });
    }

    private static CoordinateSystem.PointProvider createPointProvider() {
        return (left, right, bottom, top) -> {
            List<Point2D.Double> list = new ArrayList<>();
            int startX = (int) Math.floor(left / 1024) - 1;
            int endX = (int) Math.ceil(right / 1024) + 1;
            int startZ = (int) Math.floor(bottom / 1024) - 1;
            int endZ = (int) Math.ceil(top / 1024) + 1;

            for (int i7 = startX; i7 <= endX; i7++) {
                for (int i8 = startZ; i8 <= endZ; i8++) {
                    long seed = (long) i7 + (long) i8 * 13871L;
                    Random rand = new Random(seed);
                    int centerX = (i7 << 10) + 128 + rand.nextInt(512);
                    int centerZ = (i8 << 10) + 128 + rand.nextInt(512);
                    
                    if (centerX < 0 || centerZ < 0 || centerX > 33554432 || centerZ > 33554432) continue;
                    
                    if (centerX >= left && centerX <= right &&
                        centerZ >= bottom && centerZ <= top) {
                        list.add(new Point2D.Double(centerX, centerZ));
                    }
                }
            }
            return list;
        };
    }

    private static void handleSearch(JTextField xField, JTextField zField, 
                                   CoordinateSystem panel, CoordinateSystem.PointProvider provider) {
        try {
            double targetX = Double.parseDouble(xField.getText());
            double targetZ = Double.parseDouble(zField.getText());
            
            // 生成搜索区域（±1024范围）
            List<Point2D.Double> candidates = provider.getVisiblePoints(
                targetX - 1024, targetX + 1024,
                targetZ - 1024, targetZ + 1024
            );
            
            // 查找最近点
            Point2D.Double nearest = findNearestPoint(targetX, targetZ, candidates);
            
            if (nearest != null) {
                panel.selectedPoint = nearest;
                panel.centerOn(nearest.x, nearest.y);
                panel.repaint();
            } else {
                JOptionPane.showMessageDialog(null, "该区域未发现金字塔结构");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "请输入有效数字坐标");
        }
    }

    private static Point2D.Double findNearestPoint(double x, double z, 
                                                 List<Point2D.Double> points) {
        Point2D.Double closest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Point2D.Double pt : points) {
            double dx = pt.x - x;
            double dz = pt.y - z;
            double dist = dx * dx + dz * dz;
            
            if (dist < minDistance) {
                minDistance = dist;
                closest = pt;
            }
        }
        return closest;
    }
}
