package robot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import robot.RobotConstants;

@SuppressWarnings("serial")
public class RobotEditor extends JPanel {
	
	// The robot!
	@SuppressWarnings("unused")
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
	
	public RobotEditor(Robot robot) {
		
		// Stores the reference to the actual robot
		_robot = robot;
		
		_sensorDialog = new SensorDialog();
		_sensorDialog.pack();
		_sensorDialog.setResizable(false);
		
		this.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				if (_bMeasured) {
					boolean bControlDown = e.isControlDown();

					int robotClickX = e.getX();
					int robotClickY = e.getY();
					
					if(robotClickX < _offsetX || robotClickY < _offsetY ||
							robotClickX >= _offsetX + _robotWidth ||
							robotClickY >= _offsetY + _robotHeight)
						return;

					robotClickX -= _offsetX;
					robotClickY -= _offsetY;
					
					int robotRow = robotClickY / _robotGridSize;
					int robotCol = robotClickX / _robotGridSize;
					
					System.out.print(robotRow + ", ");
					System.out.print(robotCol);
					System.out.println(bControlDown ? ", Control down" : "");
					
					if(!bControlDown) {
						// Add sensors
						_sensorDialog.setLocationRelativeTo(RobotEditor.this);
						_sensorDialog.setVisible(true);
					}
					else {
						// Remove sensors
					}
				}
				
			}
		});
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
			
			_bMeasured = true;
		}

		// Clear the robot editor
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, _editorWidth, _editorHeight);
		
		
		// Draw the robot grids
		for(int robotRow = 0; robotRow < RobotConstants.ROBOT_SIZE; robotRow++)
		{
			for(int robotCol = 0; robotCol < RobotConstants.ROBOT_SIZE; robotCol++)
			{
				g.setColor(RobotConstants.C_LINE);
				g.fillRect(_offsetX + (robotRow * _robotGridSize),
						_offsetY + (robotCol * _robotGridSize),
						_robotGridSize, _robotGridSize);
				
				Color gridColor = RobotConstants.C_UNEXPLORED;
				g.setColor(gridColor);
				g.fillRect(
						(_offsetX + (robotRow * _robotGridSize) + RobotConstants.LINE_WEIGHT),
						(_offsetY + (robotCol * _robotGridSize) + RobotConstants.LINE_WEIGHT),
						(_robotGridSize - (RobotConstants.LINE_WEIGHT * 2)),
						(_robotGridSize - (RobotConstants.LINE_WEIGHT * 2)));
			}
		}
		
		// Draw the robot outline
		g.setColor(RobotConstants.C_ROBOT_OUTLINE);
		g.fillOval(_offsetX + RobotConstants.LINE_WEIGHT,
				_offsetY + RobotConstants.LINE_WEIGHT,
				_robotWidth - (RobotConstants.LINE_WEIGHT * 2),
				_robotHeight - (RobotConstants.LINE_WEIGHT * 2));
		
		// Draw the robot
		g.setColor(RobotConstants.C_ROBOT);
		g.fillOval(_offsetX + (RobotConstants.LINE_WEIGHT * 4),
				_offsetY + (RobotConstants.LINE_WEIGHT * 4),
				_robotWidth - (RobotConstants.LINE_WEIGHT * 8),
				_robotHeight - (RobotConstants.LINE_WEIGHT * 8));
		
		// Draw the front of the robot
		g.setColor(RobotConstants.C_ROBOT_FRONT);
		g.fillArc(
				_offsetX + (RobotConstants.LINE_WEIGHT * 4)
						+ (_robotWidth - (RobotConstants.LINE_WEIGHT * 8)) / 4,
				_offsetY + (RobotConstants.LINE_WEIGHT * 4)
						- (_robotHeight - (RobotConstants.LINE_WEIGHT * 8)) / 6,
				(_robotWidth - (RobotConstants.LINE_WEIGHT * 8)) / 2,
				(_robotHeight - (RobotConstants.LINE_WEIGHT * 8)) / 2, -80, -20);
		
		// Draw the sensors (if any)
		
		
	}
}
