package robot;

import java.util.ArrayList;

import map.MapConstants;
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
	
	// Some memory here
	// Previous left wall? Previous front wall?

	public Robot(int robotPosRow, int robotPosCol, DIRECTION robotDirection) {
		
		_robotPosRow = robotPosRow;
		_robotPosCol = robotPosCol;
		
		_robotDirection = robotDirection;
		
		_sensors = new ArrayList<Sensor>();
		
		// Will need to rotate the sensors based on the robot's starting direction
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
	
	public void removeSensor() {
		
	}
	
	public ArrayList<Sensor> getSensors() {
		return _sensors;
	}
	
	public void makeNextMove() {
		// Sense
		// Logic
		// Movement + Remember to move the sensors as well!
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
	
	/** For placing sensors */
	public int getRobotPosRow() {
		return _robotPosRow;
	}
	
	/** For placing sensors */
	public int getRobotPosCol() {
		return _robotPosCol;
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
		
		_robotPosRow = newRobotPosRow;
		_robotPosCol = newRobotPosCol;
		
		// Move the sensors here?
	}
	
	/**
	 * Function for turning the robot
	 * 
	 * @param bClockwise True if robot is turning in the clockwise direction
	 */
	private void turn(boolean bClockwise) {
		
		DIRECTION prevDirection = _robotDirection;
		_robotDirection = bClockwise ? DIRECTION.getNext(_robotDirection)
				: DIRECTION.getPrevious(_robotDirection);
		
		// Remaining 'size' after taking into consideration the reference grid
		int robotRemainingSize = RobotConstants.ROBOT_SIZE - 1;
		
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
		
		// Rotate the sensors here??
	}
	
}
