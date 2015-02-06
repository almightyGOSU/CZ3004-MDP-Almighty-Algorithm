package simulator;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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

import map.MapConstants;
import map.RealMap;

public class Simulator {
	
	// JFrame for the application
	private static JFrame _appFrame = null;
	
	// JPanel for laying out different views
	private static JPanel _mainCards = null;
	private static JPanel _buttonsCards = null;
	
	// Map width & height used to render real & robot map
	private static int _mapWidth;
	private static int _mapHeight;
	
	// The actual map used for placing obstacles
	private static RealMap _realMap = null;
	
	// The frame used for configuring the robot
	private static JPanel _robotConfig = null;
	
	// The robot map used for exploration & shortest path
	private static JPanel _robotMap = null;
	
	// The frame used for main menu buttons
	private static JPanel _mainButtons = null;
	
	// The frame used for robot configuration menu buttons
	private static JPanel _robotConfigButtons = null;
	
	public static void main(String[] args) {
		
		// Calculate map width & height based on grid size
		_mapWidth = MapConstants.MAP_COLS * MapConstants.GRID_SIZE;
		_mapHeight = MapConstants.MAP_ROWS * MapConstants.GRID_SIZE;
		
		// Main frame for displaying everything
		_appFrame = new JFrame();
		_appFrame.setTitle("Almighty Simulator");
		_appFrame.setSize(new Dimension(892, 780));
		_appFrame.setResizable(false);
		
		// Create the CardLayouts for storing the different views
		_mainCards = new JPanel(new CardLayout());
		_buttonsCards = new JPanel(new CardLayout());
		
		// Initialize the main CardLayout
		initMainLayout();
		
		// Initialize the buttons CardLayout
		initButtonsLayout();
		
		// Add CardLayouts to content pane
		Container contentPane = _appFrame.getContentPane();
		contentPane.add(_mainCards, BorderLayout.CENTER);
		contentPane.add(_buttonsCards, BorderLayout.SOUTH);
		
		// Display the application
		_appFrame.setVisible(true);
		_appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private static void initMainLayout() {
		
		// Initialize the real map, used for placing obstacles
		_realMap = new RealMap();
		System.out.println("Map width: " + _mapWidth + ", Map height: "
				+ _mapHeight);
		_mainCards.add(_realMap, SimulatorConstants.MAIN);
		
		// Initialize the robot configuration frame
		_robotConfig = new JPanel();
		_mainCards.add(_robotConfig, SimulatorConstants.ROBOT_CONFIG);
		
		// Initialize the robot map, used for exploration and shortest path
		_robotMap = new JPanel();
		_mainCards.add(_robotMap, SimulatorConstants.ROBOT_MAP);
		
		// Show the real map (main menu) by default
		CardLayout cl = ((CardLayout) _mainCards.getLayout());
	    cl.show(_mainCards, SimulatorConstants.MAIN);
		
	}
	
	private static void initButtonsLayout() {
		
		// Initialize the buttons used in main menu
		_mainButtons = new JPanel();
		addMainMenuButtons();
		_buttonsCards.add(_mainButtons, SimulatorConstants.MAIN_BUTTONS);
		
		// Initialize the buttons used in robot configuration menu
		_robotConfigButtons = new JPanel();
		addRobotConfigButtons();
		_buttonsCards.add(_robotConfigButtons, SimulatorConstants.ROBOT_CONFIG_BUTTONS);
		
		// Show the real map (main menu) buttons by default
		CardLayout cl = ((CardLayout) _buttonsCards.getLayout());
		cl.show(_buttonsCards, SimulatorConstants.MAIN_BUTTONS);
		
	}
	
	private static void addMainMenuButtons() {
		// Main menu buttons
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
		_mainButtons.add(btn_clearMap);

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
				int returnVal = fileDialog.showOpenDialog(_appFrame);
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

					JOptionPane.showMessageDialog(_appFrame,
							"Loaded map information from " + file.getName());
				} else {
					System.out.println("Open command cancelled by user.");
				}
			}
		});
		_mainButtons.add(btn_loadMap);

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
				int returnVal = fileDialog.showSaveDialog(_appFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						String fileName = fileDialog.getSelectedFile() + "";

						// Allows overriding of existing text files
						if (!fileName.endsWith(".txt"))
							fileName += ".txt";

						// Change file writing part to a better implementation
						FileWriter fw = new FileWriter(fileName);
						fw.write(_realMap.generateMapString());
						fw.flush();
						fw.close();

						JOptionPane.showMessageDialog(_appFrame,
								"Map information saved to " + fileName);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					System.out.println("Save command cancelled by user.");
				}
			}
		});
		_mainButtons.add(btn_saveMap);

		JButton btn_configRobot = new JButton("Configure Robot");
		btn_configRobot.setFont(new Font("Arial", Font.BOLD, 18));
		btn_configRobot.setMargin(new Insets(10, 15, 10, 15));
		btn_configRobot.setFocusPainted(false);

		btn_configRobot.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Do something
				System.out.println("Configuring Robot..");
				
				// Show the robot configuration frame
				CardLayout cl = ((CardLayout) _mainCards.getLayout());
			    cl.show(_mainCards, SimulatorConstants.ROBOT_CONFIG);
			    
			    // Show the robot configuration buttons frame
			    cl = ((CardLayout) _buttonsCards.getLayout());
			    cl.show(_buttonsCards, SimulatorConstants.ROBOT_CONFIG_BUTTONS);
			}
		});
		_mainButtons.add(btn_configRobot);

		JButton btn_explore = new JButton("Explore");
		btn_explore.setFont(new Font("Arial", Font.BOLD, 18));
		btn_explore.setMargin(new Insets(10, 15, 10, 15));
		btn_explore.setFocusPainted(false);

		btn_explore.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// Do something
				/*
				 * System.out.println("Starting Exploration..");
				 * System.out.println("Sensor Position (row, col): " +
				 * _sensor1.getSensorPosRow() + ", " +
				 * _sensor1.getSensorPosCol());
				 * System.out.println("Sensor Range (min, max): " +
				 * _sensor1.getMinRange() + ", " + _sensor1.getMaxRange());
				 * System.out.println("Sensor Direction: " +
				 * _sensor1.getSensorDirection().toString());
				 * 
				 * System.out.println("No obstacles within " +
				 * _sensor1.sense(_realMap) + " grids!");
				 */
			}
		});
		_mainButtons.add(btn_explore);

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
		_mainButtons.add(btn_shortestPath);
		
	}
	
	/**
	 * Robot configuration buttons
	 */
	private static void addRobotConfigButtons() {
		
		JButton btn_backToRealMap = new JButton("Back to Real Map");
		btn_backToRealMap.setFont(new Font("Arial", Font.BOLD, 18));
		btn_backToRealMap.setMargin(new Insets(10, 15, 10, 15));
		btn_backToRealMap.setFocusPainted(false);

		btn_backToRealMap.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
			    // Show the real map (main menu) frame
				CardLayout cl = ((CardLayout) _mainCards.getLayout());
			    cl.show(_mainCards, SimulatorConstants.MAIN);
				
			    // Show the real map (main menu) buttons frame
				cl = ((CardLayout) _buttonsCards.getLayout());
				cl.show(_buttonsCards, SimulatorConstants.MAIN_BUTTONS);
			}
		});
		_robotConfigButtons.add(btn_backToRealMap);
		
	}

}
