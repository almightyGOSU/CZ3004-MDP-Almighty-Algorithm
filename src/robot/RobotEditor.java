package robot;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import robot.RobotConstants;
import robot.RobotConstants.DIRECTION;

@SuppressWarnings("serial")
public class RobotEditor extends JPanel {
	
	// The robot!
	private Robot _robot = null;	
	
	// For measuring size of the canvas
	private boolean _bMeasured = false;
	
	// Size of the editor
	private int _editorWidth = 0;
	private int _editorHeight = 0;
	
	// Just for rendering the robot
	private int _robotWidth = 600;
	private int _robotHeight = 600;
	private int _robotGridSize = 0;
	
	// Offset to center the robot in editor
	private int _offsetX = 0;
	private int _offsetY = 0;
	
	// Custom dialog to allow user to add sensors
	private SensorDialog _sensorDialog = null;
	private RobotGrid [][] _robotGrids = null;
	private int _sensorGridSize = 0;
	
	public RobotEditor(Robot robot, Frame appFrame) {
		
		// Stores the reference to the actual robot
		_robot = robot;
		
		_sensorDialog = new SensorDialog(appFrame);
		_sensorDialog.pack();
		_sensorDialog.setResizable(false);
		
		this.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				if (_bMeasured) {
					boolean bControlDown = e.isControlDown();

					int robotClickX = e.getX();
					int robotClickY = e.getY();
					
					// Ensure the mouse click is on the robot
					if(robotClickX < _offsetX || robotClickY < _offsetY ||
							robotClickX >= _offsetX + _robotWidth ||
							robotClickY >= _offsetY + _robotHeight)
						return;

					robotClickX -= _offsetX;
					robotClickY -= _offsetY;
					
					int robotRow = robotClickY / _robotGridSize;
					int robotCol = robotClickX / _robotGridSize;
					
					System.out.print("\nRobot Editor -> " + robotRow + ", ");
					System.out.print(robotCol);
					System.out.println(bControlDown ? ", Control down" : "");
					
					if(!bControlDown) {
						// Add sensors
						_sensorDialog.resetSensorDialog();
						_sensorDialog.setLocationRelativeTo(RobotEditor.this);
						_sensorDialog.setVisible(true);
						
						String minRange = _sensorDialog.getMinRange();
						String maxRange = _sensorDialog.getMaxRange();
						
						if(minRange != null && maxRange != null) {
							
							// Determine sensor's position relative to robot
							int sensorPosRow = _robot.getRobotMapPosRow() +
									robotRow;
							int sensorPosCol = _robot.getRobotMapPosCol() +
									robotCol;
							
							Sensor newSensor = new Sensor(
									Integer.parseInt(minRange),
									Integer.parseInt(maxRange),
									sensorPosRow, sensorPosCol,
									DIRECTION.fromString(
											_sensorDialog.getDirection()));
							_robot.addSensor(newSensor);
							
							// Temporary
							newSensor.printSensorInfo();
							
							RobotEditor.this.revalidate();
							RobotEditor.this.repaint();
						}
					}
					else {
						// Remove sensors
						
						// No sensors on the robot
						if(_robot.getSensors().isEmpty())
							return;
						
						// Determine the selected grid
						int sensorPosRow = _robot.getRobotMapPosRow() + robotRow;
						int sensorPosCol = _robot.getRobotMapPosCol() + robotCol;
						
						// Get the list of sensors within the selected grid
						ArrayList<Sensor> _selectedSensors =
								new ArrayList<Sensor>();
						for(Sensor sensor : _robot.getSensors()) {
							if(sensor.getSensorPosRow() == sensorPosRow &&
									sensor.getSensorPosCol() == sensorPosCol) {
								_selectedSensors.add(sensor);
							}
						}
						
						// No sensors within the selected grid
						if(_selectedSensors.isEmpty())
							return;
						
						// Create a JOptionPane for removing sensor
						Object [] sensorOptions = _selectedSensors.toArray();			        
						Sensor removedSensor = (Sensor) JOptionPane
								.showInputDialog(RobotEditor.this,
										"Select the sensor to be removed",
										"Remove sensor",
										JOptionPane.PLAIN_MESSAGE, null,
										sensorOptions, sensorOptions[0]);
				        
						if (removedSensor != null) {
							JOptionPane.showMessageDialog(
											RobotEditor.this,
											"Successfully removed sensor with"
											+ " \'" + removedSensor.toString()
											+ "\'!", "Removed Sensor",
											JOptionPane.PLAIN_MESSAGE);
							
							_robot.getSensors().remove(removedSensor);
							RobotEditor.this.revalidate();
							RobotEditor.this.repaint();
						}
					} // End else
				} // End if(bMeasured)
				
			}
		}); // End MouseListener declaration
	}
	
	public void paintComponent(Graphics g) {
		
		if (!_bMeasured) {
			
			_editorWidth = this.getWidth();
			_editorHeight = this.getHeight();
			
			System.out.println("Editor Graphics g; Editor width: " + _editorWidth
					+ ", Editor height: " + _editorHeight);
			
			_offsetX = (_editorWidth - _robotWidth) / 2;
			_offsetY = (_editorHeight - _robotHeight) / 2;
			
			_robotGridSize = _robotWidth / RobotConstants.ROBOT_SIZE;
			_sensorGridSize = _robotGridSize / 6;
			
			// Calculate the robot grids
			_robotGrids = new RobotGrid[RobotConstants.ROBOT_SIZE][RobotConstants.ROBOT_SIZE];
			for(int robotRow = 0; robotRow < RobotConstants.ROBOT_SIZE; robotRow++)
			{
				for(int robotCol = 0; robotCol < RobotConstants.ROBOT_SIZE; robotCol++)
				{
					_robotGrids[robotRow][robotCol] = new RobotGrid(
							_offsetX + (robotCol * _robotGridSize),
							_offsetY + (robotRow * _robotGridSize));
				}
			}
			
			_bMeasured = true;
		}

		// Clear the robot editor
		g.setColor(Color.DARK_GRAY);
		g.fillRect(0, 0, _editorWidth, _editorHeight);
		
		
		// Draw the robot grids
		for(int robotRow = 0; robotRow < RobotConstants.ROBOT_SIZE; robotRow++)
		{
			for(int robotCol = 0; robotCol < RobotConstants.ROBOT_SIZE; robotCol++)
			{
				g.setColor(RobotConstants.C_LINE);
				g.fillRect(_robotGrids[robotRow][robotCol].x,
						_robotGrids[robotRow][robotCol].y,
						_robotGridSize, _robotGridSize);
				
				Color gridColor = RobotConstants.C_UNEXPLORED;
				g.setColor(gridColor);
				g.fillRect(
						(_robotGrids[robotRow][robotCol].x + RobotConstants.LINE_WEIGHT),
						(_robotGrids[robotRow][robotCol].y + RobotConstants.LINE_WEIGHT),
						(_robotGridSize - (RobotConstants.LINE_WEIGHT * 2)),
						(_robotGridSize - (RobotConstants.LINE_WEIGHT * 2)));
			}
		}
		
		// Draw the robot outline - Reduced size
		g.setColor(RobotConstants.C_ROBOT_OUTLINE_EDITOR);
		g.fillOval(_offsetX + RobotConstants.LINE_WEIGHT + 20,
				_offsetY + RobotConstants.LINE_WEIGHT + 20,
				_robotWidth - (RobotConstants.LINE_WEIGHT * 2) - 40,
				_robotHeight - (RobotConstants.LINE_WEIGHT * 2) - 40);
		
		// Draw the robot - Reduced size
		g.setColor(RobotConstants.C_ROBOT_EDITOR);
		g.fillOval(_offsetX + (RobotConstants.LINE_WEIGHT * 4) + 20,
				_offsetY + (RobotConstants.LINE_WEIGHT * 4) + 20,
				_robotWidth - (RobotConstants.LINE_WEIGHT * 8) - 40,
				_robotHeight - (RobotConstants.LINE_WEIGHT * 8) - 40);
		
		// Draw the front of the robot
		g.setColor(RobotConstants.C_ROBOT_FRONT_EDITOR);
		g.fillArc(
				_offsetX + (RobotConstants.LINE_WEIGHT * 4)
						+ (_robotWidth - (RobotConstants.LINE_WEIGHT * 8)) / 4,
				_offsetY + (RobotConstants.LINE_WEIGHT * 4)
						- (_robotHeight - (RobotConstants.LINE_WEIGHT * 8)) / 6,
				(_robotWidth - (RobotConstants.LINE_WEIGHT * 8)) / 2,
				(_robotHeight - (RobotConstants.LINE_WEIGHT * 8)) / 2 + 80, -80, -20);
		
		// Draw the sensors (if any)
		for(Sensor sensor : _robot.getSensors()) {
			
			// Determine sensor's position relative to robot
			int sensorPosRow = (sensor.getSensorPosRow()
					- _robot.getRobotMapPosRow());
			int sensorPosCol = (sensor.getSensorPosCol()
					- _robot.getRobotMapPosCol());
			
			int arcStartAngle = 0;
			switch(sensor.getSensorDirection()) {
			case EAST:
				arcStartAngle = 25;
				break;
			case NORTH:
				arcStartAngle = 115;
				break;
			case SOUTH:
				arcStartAngle = -65;
				break;
			case WEST:
				arcStartAngle = 205;
				break;
			}
			
			g.setColor(RobotConstants.C_SENSOR_BEAM_OUTER);
			g.fillArc(_robotGrids[sensorPosRow][sensorPosCol].x,
					_robotGrids[sensorPosRow][sensorPosCol].y,
					_robotGridSize, _robotGridSize,
					arcStartAngle, -50);
			
			g.setColor(RobotConstants.C_SENSOR_BEAM_OUTER);
			g.fillArc(_robotGrids[sensorPosRow][sensorPosCol].x,
					_robotGrids[sensorPosRow][sensorPosCol].y,
					_robotGridSize, _robotGridSize,
					arcStartAngle - 10, -30);
			
			g.setColor(RobotConstants.C_SENSOR);
			g.fillOval(_robotGrids[sensorPosRow][sensorPosCol].sensorX,
					_robotGrids[sensorPosRow][sensorPosCol].sensorY,
					_sensorGridSize, _sensorGridSize);
		}
		
	}
	
	private class RobotGrid {
		public int x;
		public int y;
		
		public int sensorX;
		public int sensorY;
		
		public RobotGrid(int x, int y) {
			this.x = x;
			this.y = y;
			
			this.sensorX = x + ((_robotGridSize - _sensorGridSize) / 2);
			this.sensorY = y + ((_robotGridSize - _sensorGridSize) / 2);
		}
	}
}
