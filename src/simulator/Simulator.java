package simulator;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import map.MapConstants;
import map.RealMap;
import robot.Robot;
import robot.RobotConstants;
import robot.RobotEditor;
import robot.RobotConstants.DIRECTION;
import robot.RobotMap;
import robot.StartStateDialog;

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
	private static RobotMap _robotMap = null;
	
	// The frame used for main menu buttons
	private static JPanel _mainButtons = null;
	
	// The frame used for robot configuration menu buttons
	private static JPanel _robotConfigButtons = null;
	
	// The frame used for exploration & shortest path buttons
	private static JPanel _robotMapButtons = null;
	
	// The robot (Yes, the robot)
	private static Robot _almightyRobot = null;
	
	// Dialog used to configure the robot's starting position and direction
	private static StartStateDialog _startStateDialog = null;
	
	// Robot's starting position and direction
	private static int _startPosRow = RobotConstants.DEFAULT_START_ROW;
    private static int _startPosCol = RobotConstants.DEFAULT_START_COL;
    private static DIRECTION _startDir = RobotConstants.DEFAULT_START_DIR;
    
    /**
	 * The file path for the file used to store the robot information
	 */
	private static final String ROBOT_FILE_PATH = "robot.dat";
	
	public static void main(String[] args) {
		
		// Tries to load the robot if there is one
		loadRobot();
		
		// Creates the robot at the top-left corner of the map
		// facing North by default to facilitate robot configuration
		// if it failed to load the robot
		if(_almightyRobot == null) {
			_almightyRobot = new Robot(RobotConstants.DEFAULT_START_ROW,
					RobotConstants.DEFAULT_START_COL,
					RobotConstants.DEFAULT_START_DIR);
		}
		
		// --------------------------------------------------------------------
		// Everything below is just for the layout
		
		// Calculate map width & height based on grid size
		_mapWidth = MapConstants.MAP_COLS * MapConstants.GRID_SIZE;
		_mapHeight = MapConstants.MAP_ROWS * MapConstants.GRID_SIZE;
		
		// Main frame for displaying everything
		_appFrame = new JFrame();
		_appFrame.setTitle("Almighty Simulator");
		_appFrame.setSize(new Dimension(886, 771));
		_appFrame.setResizable(false);
		
		// Center the main frame in the middle of the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		_appFrame.setLocation(dim.width/2 - _appFrame.getSize().width/2,
				dim.height/2 - _appFrame.getSize().height/2);
		
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
		_robotConfig = new RobotEditor(_almightyRobot, _appFrame);
		_mainCards.add(_robotConfig, SimulatorConstants.ROBOT_CONFIG);
		
		// Initialize the robot map, used for exploration and shortest path
		_robotMap = new RobotMap(_realMap);
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
		
		// Initialize the buttons used in exploration and shortest path menu
		_robotMapButtons = new JPanel();
		addRobotMapButtons();
		_buttonsCards.add(_robotMapButtons,
				SimulatorConstants.ROBOT_MAP_BUTTONS);
		
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
							"Loaded map information from " + file.getName(),
							"Loaded Map Information",
							JOptionPane.PLAIN_MESSAGE);
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
				
				// Save current map layout to a map descriptor file
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
								"Map information saved to " + fileName,
								"Saved Map Information",
								JOptionPane.PLAIN_MESSAGE);
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
				
				// Set up the robot
				_almightyRobot.resetRobotState(1, 1, DIRECTION.EAST);
				_robotMap.resetRobotMap();
				_almightyRobot.setRobotMap(_robotMap);
				_almightyRobot.setRealMap(_realMap);
				_almightyRobot.markStartAsExplored();
				
				// Show the robot map frame
				CardLayout cl = ((CardLayout) _mainCards.getLayout());
			    cl.show(_mainCards, SimulatorConstants.ROBOT_MAP);
			    
			    // Show the robot map buttons frame
			    cl = ((CardLayout) _buttonsCards.getLayout());
			    cl.show(_buttonsCards, SimulatorConstants.ROBOT_MAP_BUTTONS);
			    
			    // Give the robot map focus
			    _robotMap.setFocusable(true);
			    _robotMap.requestFocusInWindow();
			    
			    // ASK THE ROBOT TO START EXPLORATION
				_almightyRobot.startExploration();
			    
			}
		});
		_mainButtons.add(btn_explore);

		JButton btn_shortestPath = new JButton("Shortest Path");
		btn_shortestPath.setFont(new Font("Arial", Font.BOLD, 18));
		btn_shortestPath.setMargin(new Insets(10, 15, 10, 15));
		btn_shortestPath.setFocusPainted(false);

		btn_shortestPath.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				// Show the robot map frame
				CardLayout cl = ((CardLayout) _mainCards.getLayout());
			    cl.show(_mainCards, SimulatorConstants.ROBOT_MAP);
			    
			    // Show the robot map buttons frame
			    cl = ((CardLayout) _buttonsCards.getLayout());
			    cl.show(_buttonsCards, SimulatorConstants.ROBOT_MAP_BUTTONS);
			    
			    // ASK THE ROBOT TO EMBARK ON SHORTEST PATH...
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
				
				// Saves the robot information
				if(_almightyRobot != null)
					saveRobot();
				
			    // Show the real map (main menu) frame
				CardLayout cl = ((CardLayout) _mainCards.getLayout());
			    cl.show(_mainCards, SimulatorConstants.MAIN);
				
			    // Show the real map (main menu) buttons frame
				cl = ((CardLayout) _buttonsCards.getLayout());
				cl.show(_buttonsCards, SimulatorConstants.MAIN_BUTTONS);
			}
		});
		_robotConfigButtons.add(btn_backToRealMap);
		
		// Create the startStateDialog
		_startStateDialog = new StartStateDialog(_appFrame);
		_startStateDialog.pack();
		_startStateDialog.setResizable(false);
		
		JButton btn_robotStartState = new JButton("Robot Starting State");
		btn_robotStartState.setFont(new Font("Arial", Font.BOLD, 18));
		btn_robotStartState.setMargin(new Insets(10, 15, 10, 15));
		btn_robotStartState.setFocusPainted(false);

		btn_robotStartState.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				_startStateDialog.setLocationRelativeTo(_appFrame);
				_startStateDialog.setVisible(true);
				
				String startPosRow = _startStateDialog.getStartPosRow();
				String startPosCol = _startStateDialog.getStartPosCol();
				
				if(startPosRow != null && startPosCol != null) {
				
					_startPosRow = Integer.parseInt(startPosRow);
					_startPosCol = Integer.parseInt(startPosCol);
					
					_startDir = RobotConstants.DIRECTION.fromString(
							_startStateDialog.getStartDirection());
					
					System.out.println("Starting Row: " + _startPosRow
							+ ", Starting Col: " + _startPosCol
							+ ", Starting Direction: " + _startDir.toString());
				}
			}
		});
		_robotConfigButtons.add(btn_robotStartState);
		
		JButton btn_exploreStrategy = new JButton("Exploration Strategy");
		btn_exploreStrategy.setFont(new Font("Arial", Font.BOLD, 18));
		btn_exploreStrategy.setMargin(new Insets(10, 15, 10, 15));
		btn_exploreStrategy.setFocusPainted(false);

		btn_exploreStrategy.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				// Temporary
				JLabel wipLabel = new JLabel("Work In Progress!");
				wipLabel.setFont(new Font("Arial", Font.BOLD, 24));
				JOptionPane.showMessageDialog(_appFrame,
						wipLabel,
						"Exploration Strategy",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		_robotConfigButtons.add(btn_exploreStrategy);
		
		JButton btn_shortestPathStrategy = new JButton("Shortest Path Strategy");
		btn_shortestPathStrategy.setFont(new Font("Arial", Font.BOLD, 18));
		btn_shortestPathStrategy.setMargin(new Insets(10, 15, 10, 15));
		btn_shortestPathStrategy.setFocusPainted(false);

		btn_shortestPathStrategy.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				// Temporary
				JLabel wipLabel = new JLabel("Work In Progress!");
				wipLabel.setFont(new Font("Arial", Font.BOLD, 24));
				JOptionPane.showMessageDialog(_appFrame,
						wipLabel,
						"Shortest Path Strategy",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		_robotConfigButtons.add(btn_shortestPathStrategy);
		
	}
	
	/**
	 * Robot map buttons
	 */
	private static void addRobotMapButtons() {
		
		JButton btn_backToRealMap = new JButton("Back to Real Map");
		btn_backToRealMap.setFont(new Font("Arial", Font.BOLD, 18));
		btn_backToRealMap.setMargin(new Insets(10, 15, 10, 15));
		btn_backToRealMap.setFocusPainted(false);

		btn_backToRealMap.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				// ASK THE ROBOT TO STOP EXPLORATION
				_almightyRobot.stopExploration();
				
			    // Show the real map (main menu) frame
				CardLayout cl = ((CardLayout) _mainCards.getLayout());
			    cl.show(_mainCards, SimulatorConstants.MAIN);
				
			    // Show the real map (main menu) buttons frame
				cl = ((CardLayout) _buttonsCards.getLayout());
				cl.show(_buttonsCards, SimulatorConstants.MAIN_BUTTONS);
			}
		});
		_robotMapButtons.add(btn_backToRealMap);
		
	}
	
	/**
	 * Loads the robot information from the robot file, if it exists
	 * <br>Should be called at the start of the application
	 */
	private static void loadRobot()
	{
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try
		{
			fis = new FileInputStream(ROBOT_FILE_PATH);
			in = new ObjectInputStream(fis);
			
			Object obj = in.readObject();

			if (obj instanceof Robot)
			{
				_almightyRobot = (Robot) obj;
			}

			in.close();
			
			if(_almightyRobot != null)
				System.out.println("'Robot' data loaded successfully!");
			
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to load 'Robot' data!");
		} catch (IOException ex) {
			System.out.println("Unable to load 'Robot' data!");
		} catch (ClassNotFoundException ex) {
			System.out.println("Unable to load 'Robot' data!");
		} catch (Exception ex) {
			System.out.println("Unable to load 'Robot' data!");
		}
	}
	
	/**
	 * Saves the robot information to the robot file<br>
	 * Should be called before exiting the application
	 */
	private static void saveRobot() {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;

		try {
			fos = new FileOutputStream(ROBOT_FILE_PATH);
			out = new ObjectOutputStream(fos);
			
			out.writeObject(_almightyRobot);
			out.close();
			
			System.out.println("Saved 'Robot' data successfully!");
			
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to save 'Robot' data!");
		} catch (IOException ex) {
			System.out.println("Unable to save 'Robot' data!");
		} catch (Exception ex) {
			System.out.println("Unable to save 'Robot' data!");
		}
	}

}
