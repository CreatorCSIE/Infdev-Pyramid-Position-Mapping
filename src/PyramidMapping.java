import javax.swing.*;
import java.util.Random;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class PyramidMapping {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Minecraft Infdev砖块金字塔中心坐标分布");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());
            
            JPanel searchPanel = new JPanel(new GridBagLayout());
            searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);

            // 创建控制面板
            JPanel controlPanel = new JPanel();
            JTextField xField = new JTextField(8);
            JTextField zField = new JTextField(8);
            JButton searchBtn = new JButton("定位");
            JButton resetBtn = new JButton("回到原点");
            String[] rangeOptions = {"Inf 0227", "Inf 0313~0325"};
            JComboBox<String> rangeCombo = new JComboBox<>(rangeOptions);

            controlPanel.add(new JLabel("X:"));
            controlPanel.add(xField);
            controlPanel.add(new JLabel("Z:"));
            controlPanel.add(zField);
            controlPanel.add(searchBtn);
            controlPanel.add(resetBtn);
            controlPanel.add(rangeCombo);
            
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 0.0;
            searchPanel.add(controlPanel, gbc);
            
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            searchPanel.add(Box.createHorizontalGlue(), gbc);

            CoordinateSystem panel = new CoordinateSystem();
            CoordinateSystem.PointProvider provider = createPointProvider(rangeCombo); // 提取生成逻辑
            panel.setPointProvider(provider);
            
            JLabel authorLabel = new JLabel("<html><div style='text-align:right;'>"
                    + "Made by CreatorCSIE<br>"
                    + "<small>版本：1.2.2</small></div></html>");
                authorLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                authorLabel.setForeground(new Color(100, 100, 150));

            // 组装面板
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.LINE_END;
            searchPanel.add(authorLabel, gbc);

            frame.add(searchPanel, BorderLayout.NORTH);
            frame.add(panel, BorderLayout.CENTER);
            frame.setVisible(true);

            // 事件监听
            searchBtn.addActionListener(e -> handleSearch(xField, zField, panel, provider,rangeCombo));
            resetBtn.addActionListener(e -> panel.resetViewport());
            rangeCombo.addActionListener(e -> {
            	String selected = (String) rangeCombo.getSelectedItem();
                // 0227版本启用蓝色区域，0313~0325禁用
                boolean shouldRender = "Inf 0227".equals(selected);
                panel.setRenderLegacyBlueArea(shouldRender);
            	
                CoordinateSystem.PointProvider newProvider = createPointProvider(rangeCombo);
                panel.setPointProvider(newProvider); // 这会自动清除选中状态
                panel.selectedPoint = null;
                panel.repaint();
            });
        });
    }

    private static CoordinateSystem.PointProvider createPointProvider(JComboBox<String> rangeCombo) {
        return new CoordinateSystem.PointProvider() {
            @Override
            public List<Point2D.Double> getVisiblePoints(double left, double right, double bottom, double top) {
                List<Point2D.Double> list = new ArrayList<>();
                int startX = (int) Math.floor(left / 1024) - 1;
                int endX = (int) Math.ceil(right / 1024) + 1;
                int startZ = (int) Math.floor(bottom / 1024) - 1;
                int endZ = (int) Math.ceil(top / 1024) + 1;

                int max = 0;
                if ("Inf 0227".equals(rangeCombo.getSelectedItem())) {
                    max = 33554432;
                }
                if ("Inf 0313~0325".equals(rangeCombo.getSelectedItem())) {
                    max = 32000000;
                }

                for (int i7 = startX; i7 <= endX; i7++) {
                    for (int i8 = startZ; i8 <= endZ; i8++) {
                        long seed = (long) i7 + (long) i8 * 13871L;
                        Random rand = new Random(seed);
                        int centerX = (i7 << 10) + 128 + rand.nextInt(512);
                        int centerZ = (i8 << 10) + 128 + rand.nextInt(512);
                        
                        if (centerX < 0 || centerZ < 0 || centerX > max || centerZ > max) continue;
                        
                        if (centerX >= left && centerX <= right &&
                            centerZ >= bottom && centerZ <= top) {
                            list.add(new Point2D.Double(centerX, centerZ));
                        }
                    }
                }
                return list;
            }
        };
    }

    private static void handleSearch(JTextField xField, JTextField zField,
            CoordinateSystem panel, CoordinateSystem.PointProvider provider, JComboBox<String> rangeCombo) {
    		// 获取并清理输入
    		String xInput = xField.getText().replace(",", "").trim();
    			String zInput = zField.getText().replace(",", "").trim();

    			// 空输入检查
    			if (xInput.isEmpty() && zInput.isEmpty()) {
    					showErrorDialog("X坐标不能为空", "输入错误");
    					return;
    			}
    			else
    				if(xInput.isEmpty()) {
    					showErrorDialog("X坐标不能为空", "输入错误");
    					return;
    				}
    				else
    					if (zInput.isEmpty()) {
    						showErrorDialog("Z坐标不能为空", "输入错误");
    						return;
    					}
    			
    			try {
    				// 格式验证
    				if (!isValidNumber(xInput) || !isValidNumber(zInput)) {
    					showErrorDialog("请输入有效的数字（示例：12345 或 12345.67）", "格式错误");
    					return;
    				}
    				
    				// 数值转换
    				double targetX = Double.parseDouble(xInput);
    				double targetZ = Double.parseDouble(zInput);
    				
    				// 范围验证
    				String rangeError = checkCoordinateRange(targetX, targetZ,rangeCombo);
    				if (rangeError != null) {
    					showErrorDialog(rangeError, "范围错误");
    					return;
    				}
    				
    				// 执行搜索逻辑...
    		        List<Point2D.Double> candidates = provider.getVisiblePoints(
    		            targetX - 1024, targetX + 1024,
    		            targetZ - 1024, targetZ + 1024
    		        );
    		        
    		        Point2D.Double nearest = findNearestPoint(targetX, targetZ, candidates);
    		        
    		        if (nearest != null) {
    		            panel.selectedPoint = nearest;
    		            panel.centerOn(nearest.x, nearest.y);
    		            panel.repaint();
    		        } else {
    		            System.out.println("未找到附近点");
    		            JOptionPane.showMessageDialog(null, "该区域未发现金字塔结构");
    		        }
    				
    			} catch (NumberFormatException ex) {
    				showErrorDialog("发生未知解析错误", "系统错误");
    			}
    }

    //数字格式验证（支持整数和小数）
    private static boolean isValidNumber(String input) {
    	return input.matches("-?\\d+(\\.\\d+)?");
    }

    //坐标范围验证（返回具体错误描述）
    private static String checkCoordinateRange(double x, double z, JComboBox<String> rangeCombo) {
    	int max = "Inf 0313~0325".equals(rangeCombo.getSelectedItem()) ? 32000000 : 33554432;
    	if (x < 0 || z < 0) {
    		return "坐标不能为负数";
    	}
    	if (x > max || z > max) {
    		return String.format("坐标不能超过 %,d", max);
    	}
    	return null;
    }
    
    //统一错误提示方法
    private static void showErrorDialog(String message, String title) {
    	JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
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
