package simulator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import robot.RobotConstants.DIRECTION;
import robot.Sensor;

import map.MapConstants;
import map.RealMap;

public class Simulator {
	
	private static RealMap _realMap = null;
	
	// Temporary for testing sensors
	private static Sensor _sensor1 = null;
	
	public static void main(String[] args) {
		
		int mapWidth = MapConstants.MAP_COLS * MapConstants.GRID_SIZE;
		int mapHeight = MapConstants.MAP_ROWS * MapConstants.GRID_SIZE;
		
		final JFrame appFrame = new JFrame();
		appFrame.setTitle("Almighty Simulator");
		appFrame.setSize(new Dimension(892, 780));
		appFrame.setResizable(false);
		
		// Real Map
		_realMap = new RealMap();
		_realMap.setSize(mapWidth, mapHeight);
		System.out.println("Map width: " + mapWidth + ", Map height: " + mapHeight);
		
		// Create the sensors [Temporary]
		_sensor1 = new Sensor(1, 10, 10, 5, DIRECTION.WEST);
		
		// Toolbar
		JPanel toolBar = new JPanel();
		toolBar.setSize(892, 180);
		
		// Toolbar buttons
		JButton btn_clearMap = new JButton("Clear Map");
		btn_clearMap.setFont(new Font("Arial", Font.BOLD, 18));
		btn_clearMap.setMargin(new Insets(10, 15, 10, 15));
		btn_clearMap.setFocusPainted(false);
		
		btn_clearMap.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Clear the current map
				System.out.println("Clearing Map..");
				
				_realMap.clearMap();
			}
		});
		toolBar.add(btn_clearMap);
		
		JButton btn_loadMap = new JButton("Load Map");
		btn_loadMap.setFont(new Font("Arial", Font.BOLD, 18));
		btn_loadMap.setMargin(new Insets(10, 15, 10, 15));
		btn_loadMap.setFocusPainted(false);
		
		btn_loadMap.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Load map from a map description string
				System.out.println("Loading map layout..");
				
				final JFileChooser fileDialog = new JFileChooser(System
						.getProperty("user.dir"));
				int returnVal = fileDialog.showOpenDialog(appFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fileDialog.getSelectedFile();
					
					try (BufferedReader br = new BufferedReader(new FileReader(
							file))) {
						_realMap.loadFromMapString(br.readLine());

					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					
					JOptionPane.showMessageDialog(appFrame,
							"Loaded map information from " + file.getName());
				} else {
					System.out.println("Open command cancelled by user.");
				}
			}
		});
		toolBar.add(btn_loadMap);
		
		JButton btn_saveMap = new JButton("Save Map");
		btn_saveMap.setFont(new Font("Arial", Font.BOLD, 18));
		btn_saveMap.setMargin(new Insets(10, 15, 10, 15));
		btn_saveMap.setFocusPainted(false);
		
		btn_saveMap.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Do something
				// Save current map layout to a map descriptor file
				System.out.println("Saving map layout..");

				final JFileChooser fileDialog = new JFileChooser(System
						.getProperty("user.dir"));
				int returnVal = fileDialog.showSaveDialog(appFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						String fileName = fileDialog.getSelectedFile() + "";
						
						// Allows overriding of existing text files
						if(!fileName.endsWith(".txt"))
							fileName += ".txt";
						
						// Change file writing part to a better implementation
						FileWriter fw = new FileWriter(fileName);
						fw.write(_realMap.generateMapString());
						fw.flush();
						fw.close();
						
						JOptionPane.showMessageDialog(appFrame,
								"Map information saved to " + fileName);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					System.out.println("Save command cancelled by user.");
				}
			}
		});
		toolBar.add(btn_saveMap);
		
		JButton btn_configRobot = new JButton("Configure Robot");
		btn_configRobot.setFont(new Font("Arial", Font.BOLD, 18));
		btn_configRobot.setMargin(new Insets(10, 15, 10, 15));
		btn_configRobot.setFocusPainted(false);
		
		btn_configRobot.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Do something
				System.out.println("Configuring Robot..");
			}
		});
		toolBar.add(btn_configRobot);
		
		JButton btn_explore = new JButton("Explore");
		btn_explore.setFont(new Font("Arial", Font.BOLD, 18));
		btn_explore.setMargin(new Insets(10, 15, 10, 15));
		btn_explore.setFocusPainted(false);
		
		btn_explore.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Do something
				System.out.println("Starting Exploration..");
				System.out.println("Sensor Position (row, col): "
						+ _sensor1.getSensorPosRow() + ", "
						+ _sensor1.getSensorPosCol());
				System.out.println("Sensor Range (min, max): "
						+ _sensor1.getMinRange() + ", "
						+ _sensor1.getMaxRange());
				System.out.println("Sensor Direction: " + _sensor1.getSensorDirection().toString());
				
				System.out.println("No obstacles within " + _sensor1.sense(_realMap) + " grids!");
			}
		});
		toolBar.add(btn_explore);
		
		JButton btn_shortestPath = new JButton("Shortest Path");
		btn_shortestPath.setFont(new Font("Arial", Font.BOLD, 18));
		btn_shortestPath.setMargin(new Insets(10, 15, 10, 15));
		btn_shortestPath.setFocusPainted(false);
		
		btn_shortestPath.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Do something
				System.out.println("Starting Shortest Path..");
			}
		});
		toolBar.add(btn_shortestPath);
		
		// Add to content pane
		Container contentPane = appFrame.getContentPane();
		contentPane.add(_realMap, BorderLayout.CENTER);
		contentPane.add(toolBar, BorderLayout.SOUTH);
		
		// Display the application
		appFrame.setVisible(true);
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
