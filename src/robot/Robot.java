package robot;

import java.util.ArrayList;

import map.MapConstants;
import map.RealMap;
import robot.RobotConstants.*;

public class Robot {

	// Robot's position on the map (In grids)
	// NOTE: This will be the robot's bottom left corner
	private int _robotPosRow;
	private int _robotPosCol;

	// Robot's current direction
	private DIRECTION _robotDirection;
	
	// Robot's collection of sensors
	private ArrayList<Sensor> _sensors = null;
	
	// Robot's robot map
	private RobotMap _robotMap = null;
	private RealMap _realMap = null; // For testing purposes
	
	// Some memory here
	// Previous left wall? Previous front wall?

	public Robot(int robotPosRow, int robotPosCol, DIRECTION robotDirection) {
		
		_robotPosRow = robotPosRow;
		_robotPosCol = robotPosCol;
		
		_robotDirection = robotDirection;
		
		_sensors = new ArrayList<Sensor>();
	}
	
	public void setRealMap(final RealMap realMap) {
		_realMap = realMap;
	}
	
	/**
	 * Adds a new sensor to the robot<p>
	 * 
	 * Relative to the robot's position (i.e. its bottom left corner)
	 * 
	 * @param sensorRowOffset Offset in rows
	 * @param sensorColOffset Offset in columns
	 */
	public void addSensor(Sensor newSensor) {
		
		_sensors.add(newSensor);
	}
	
	public ArrayList<Sensor> getSensors() {
		return _sensors;
	}
	
	public void makeNextMove() {
		// Sense
		// Logic
		// Movement + Remember to move the sensors as well!
	}
	
	/**
	 * This should update the robot's map based on available sensor information
	 */
	public void sense() {
		for(Sensor s : _sensors) {
			int freeGrids = s.sense(_robotMap);
			int sensorPosRow = s.getSensorPosRow();
			int sensorPosCol = s.getSensorPosRow();
			DIRECTION sensorDir = s.getSensorDirection();
			int sensorMinRange = s.getMinRange();
			int sensorMaxRange = s.getMaxRange();
			
			if(freeGrids == 0) {
				
				int obstacleRow = sensorPosRow
						+ ((sensorDir == DIRECTION.NORTH) ? (-1 * sensorMinRange)
						: (sensorDir == DIRECTION.SOUTH) ? sensorMinRange : 0);

				int obstacleCol = sensorPosCol
						+ ((sensorDir == DIRECTION.WEST) ? (-1 * sensorMinRange)
						: (sensorDir == DIRECTION.EAST) ? sensorMinRange : 0);
				
				_robotMap.getMapGrids()[obstacleRow][obstacleCol].setExplored(true);
				_robotMap.getMapGrids()[obstacleRow][obstacleCol].markAsObstacle();
			}
			else {
				
				for(int grid = sensorMinRange; grid <= freeGrids; grid++) {
					
					int gridRow = sensorPosRow
							+ ((sensorDir == DIRECTION.NORTH) ? (-1 * grid)
							: (sensorDir == DIRECTION.SOUTH) ? grid : 0);

					int gridCol = sensorPosCol
							+ ((sensorDir == DIRECTION.WEST) ? (-1 * grid)
							: (sensorDir == DIRECTION.EAST) ? grid : 0);
					
					if(grid != freeGrids) {
						_robotMap.getMapGrids()[gridRow][gridCol].setExplored(true);
					}
					else {
						if(grid == sensorMaxRange)
							_robotMap.getMapGrids()[gridRow][gridCol].setExplored(true);
						else {
							_robotMap.getMapGrids()[gridRow][gridCol].setExplored(true);
							_robotMap.getMapGrids()[gridRow][gridCol].markAsObstacle();
						}
					}
				}
			}
		}
	}
	
	public void moveStraight() {
		
		int newRobotPosRow = _robotPosRow;
		int newRobotPosCol = _robotPosCol;
		
		newRobotPosRow += (_robotDirection == DIRECTION.NORTH) ? -1
				: (_robotDirection == DIRECTION.SOUTH) ? 1 : 0;
		
		newRobotPosCol += (_robotDirection == DIRECTION.WEST) ? -1
				: (_robotDirection == DIRECTION.EAST) ? 1 : 0;
		
		if(testNextMove(newRobotPosRow, newRobotPosCol)) {
			updatePosition(newRobotPosRow, newRobotPosCol);
		}
		else {
			System.out.println("INVALID MOVE! Robot will be out of bounds or "
					+ " bump into an known obstacle..");
		}
	}
	
	public void turnLeft() {
		turn(false);
	}
	
	public void turnRight() {
		turn(true);
	}
	
	public void turn180() {
		turn(true);
		turn(true);
	}
	
	/** For placing sensors and rendering */
	public int getRobotPosRow() {
		return _robotPosRow;
	}
	
	/** For placing sensors and rendering */
	public int getRobotPosCol() {
		return _robotPosCol;
	}
	
	/** Returns the current direction that the robot is facing */
	public DIRECTION getRobotDir() {
		return _robotDirection;
	}
	
	/** For initializing the robot map */
	public void setRobotMap(RobotMap robotMap) {
		_robotMap = robotMap;
		_robotMap.setRobot(this);
	}
	
	/**
	 * Simulate the robot's next move<br>
	 * 
	 * @return True if the next move is valid
	 */
	private boolean testNextMove(int newRobotPosRow, int newRobotPosCol) {
		
		// Ensures the robot's next move will NOT cause it to go out of bounds
		// or collide with any known obstacles
		return withinArena(newRobotPosRow, newRobotPosCol)
				&& checkForObstacles(newRobotPosRow, newRobotPosCol);
	}
	
	/**
	 * Function for validating the robot's next move<br>
	 * Ensures that the robot stays within the arena at all times
	 * 
	 * @return true if the robot will still be within the arena
	 */
	private boolean withinArena(int robotPosRow, int robotPosCol) {

		// Remaining 'size' after taking into consideration the reference grid
		int robotSize = RobotConstants.ROBOT_SIZE - 1;
		
		switch (_robotDirection) {

		case NORTH:
			return (((robotPosRow - robotSize) >= 1)
					|| ((robotPosCol + robotSize) <= (MapConstants.MAP_COLS - 2)));

		case SOUTH:
			return (((robotPosRow + robotSize) <= (MapConstants.MAP_ROWS - 2))
					|| ((robotPosCol - robotSize) >= 1));
			
		case EAST:
			return (((robotPosRow + robotSize) <= (MapConstants.MAP_ROWS - 2))
					|| ((robotPosCol + robotSize) <= (MapConstants.MAP_COLS - 2)));

		case WEST:
			return (((robotPosRow - robotSize) >= 1)
					|| ((robotPosCol - robotSize) >= 1));
			
		default:
			return false;
		}
	}
	
	/**
	 * Function for validating the robot's next move<br>
	 * Ensures that the robot does not collide with any known obstacles
	 * 
	 * @return true if the robot will be safe
	 */
	private boolean checkForObstacles(int robotPosRow, int robotPosCol) {

		// Remaining 'size' after taking into consideration the reference grid
		@SuppressWarnings("unused")
		int robotRemainingSize = RobotConstants.ROBOT_SIZE - 1;
		
		/**
		 * TO BE COMPLETED
		 */
		switch (_robotDirection) {

		case NORTH:
			// Use double for loop
			// return false if isExplored && isObstacle
			return true;

		case SOUTH:
			return true;
			
		case EAST:
			return true;

		case WEST:
			return true;
			
		default:
			return true;
		}
	}
	
	private void updatePosition(int newRobotPosRow, int newRobotPosCol) {
		
		// Determine the change in row/col of the robot
		int deltaRow = newRobotPosRow - _robotPosRow;
		int deltaCol = newRobotPosCol - _robotPosCol;
		
		_robotPosRow = newRobotPosRow;
		_robotPosCol = newRobotPosCol;
		
		// Move the sensors here
		for(Sensor s : _sensors) {
			s.updateSensorPos(s.getSensorPosRow() + deltaRow,
					s.getSensorPosCol() + deltaCol);
		}
	}
	
	/**
	 * Function for turning the robot
	 * 
	 * @param bClockwise True if robot is turning in the clockwise direction
	 */
	private void turn(boolean bClockwise) {
		
		DIRECTION prevDirection = _robotDirection;

		int xC = 0;
		int yC = 0;
		switch(prevDirection) {
		case EAST:
			xC = (_robotPosCol * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			yC = (_robotPosRow * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			break;
		case NORTH:
			xC = (_robotPosCol * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			yC = (_robotPosRow * MapConstants.GRID_SIZE);
			break;
		case SOUTH:
			xC = (_robotPosCol * MapConstants.GRID_SIZE);
			yC = (_robotPosRow * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			break;
		case WEST:
			xC = (_robotPosCol * MapConstants.GRID_SIZE);
			yC = (_robotPosRow * MapConstants.GRID_SIZE);
			break;
		}
		
		// x = ((x - x_origin) * cos(angle)) - ((y_origin - y) * sin(angle)) + x_origin
		// y = ((y_origin - y) * cos(angle)) - ((x - x_origin) * sin(angle)) + y_origin
		// Rotate sensors
		for(Sensor s : _sensors) {
			/*System.out.println("Old Pos: " + s.getSensorPosRow() + ", "
					+ s.getSensorPosCol() + " Old Direction: " +
					s.getSensorDirection().toString());*/
			
			int s_xC = (s.getSensorPosCol() * MapConstants.GRID_SIZE)
					+ (MapConstants.GRID_SIZE / 2);
			int s_yC = (s.getSensorPosRow() * MapConstants.GRID_SIZE)
					+ (MapConstants.GRID_SIZE / 2);
			
			// 90 degrees rotation
			double angle = Math.PI / 2.0;
			if(bClockwise)
				angle *= -1;
				
			double new_s_xC = ((s_xC - xC) * Math.cos(angle))
					- ((yC - s_yC) * Math.sin(angle)) + xC;
			double new_s_yC = ((yC - s_yC) * Math.cos(angle))
					- ((s_xC - xC) * Math.sin(angle)) + yC;
			
			int newSensorPosCol = (int) (new_s_xC / MapConstants.GRID_SIZE);
			int newSensorPosRow = (int) (new_s_yC / MapConstants.GRID_SIZE);
			
			s.updateSensorPos(newSensorPosRow, newSensorPosCol);
			s.updateSensorDirection(bClockwise ? DIRECTION.getNext(
					s.getSensorDirection()) : DIRECTION.getPrevious(
					s.getSensorDirection()));
			
			/*System.out.println("New Pos: " + s.getSensorPosRow() + ", "
					+ s.getSensorPosCol() + " New Direction: " +
							s.getSensorDirection().toString());*/
		}
		
		// Rotate the robot
		_robotDirection = bClockwise ? DIRECTION.getNext(_robotDirection)
				: DIRECTION.getPrevious(_robotDirection);
		
		// Remaining 'size' after taking into consideration the reference grid
		int robotRemainingSize = RobotConstants.ROBOT_SIZE - 1;
		
		// Update the 'reference grid' based on rotation
		if(bClockwise) {
			if(prevDirection == DIRECTION.NORTH) 		// N to E
				_robotPosRow -= robotRemainingSize;
			else if(prevDirection == DIRECTION.EAST) 	// E to S
				_robotPosCol += robotRemainingSize;
			else if(prevDirection == DIRECTION.SOUTH) 	// S to W
				_robotPosRow += robotRemainingSize;
			else										// W to N
				_robotPosCol -= robotRemainingSize;
		}
		else {
			if(prevDirection == DIRECTION.NORTH) 		// N to W
				_robotPosCol += robotRemainingSize;
			else if(prevDirection == DIRECTION.WEST)	// W to S
				_robotPosRow -= robotRemainingSize;
			else if(prevDirection == DIRECTION.SOUTH)	// S to E
				_robotPosCol -= robotRemainingSize;
			else										// E to N
				_robotPosRow += robotRemainingSize;
		}
	}
	
}
