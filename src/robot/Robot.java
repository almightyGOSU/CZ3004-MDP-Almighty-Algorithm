package robot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.Timer;

import map.Grid;
import map.MapConstants;
import map.RealMap;

import robot.RobotConstants.*;

public class Robot implements Serializable {
	
	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = 5648007771671641286L;
	
	// Robot's actual position on the map (In grids)
	// NOTE: This will be the robot's position relative to the origin grid,
	// i.e. the robot's closest grid to the origin grid (Row 1, Col 1)
	private int _robotMapPosRow;
	private int _robotMapPosCol;	
	
	// A reference point used for the sensors to rotate along with the robot (In grids)
	// NOTE: This will be the robot's bottom left corner
	//private int _robotRefPosRow;
	//private int _robotRefPosCol;

	// Robot's current direction
	private DIRECTION _robotDirection;
	
	// Robot's collection of sensors
	private ArrayList<Sensor> _sensors = null;
	
	// Robot's robot map
	private transient RobotMap _robotMap = null;	// For determining next action
	private transient RealMap _realMap = null; 		// For detecting obstacles
	
	// Some memory for the robot here
	//private boolean _bPreviousFrontWall = false;
	//private boolean _bPreviousLeftWall = false;
	//private boolean _bPreviousRightWall = false;
	private transient boolean _bReachedGoal = false;
	private transient boolean _bExplorationComplete = false;
	
	// Temporary
	private transient Timer timer = null;

	public Robot(int robotMapPosRow, int robotMapPosCol, DIRECTION robotDirection) {
		
		_robotMapPosRow = robotMapPosRow;
		_robotMapPosCol = robotMapPosCol;
		
		_robotDirection = robotDirection;
		
		_sensors = new ArrayList<Sensor>();
	}
	
	public Robot(int robotMapPosRow, int robotMapPosCol, DIRECTION robotDirection,
			ArrayList<Sensor> sensors) {
		
		_robotMapPosRow = robotMapPosRow;
		_robotMapPosCol = robotMapPosCol;
		
		_robotDirection = robotDirection;
		
		if(sensors != null)
			_sensors = new ArrayList<Sensor>();
		else
			_sensors = new ArrayList<Sensor>();
	}
	
	/**
	 * Provide the robot with a copy of the real map,
	 * to be used with the sensors
	 * 
	 * @param realMap The real map with the obstacles
	 */
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
	
	/**
	 * Just a temporary function for testing exploration
	 */
	public void startExploration() {
		
		System.out.println("\nStarting exploration!");
		
		timer = new Timer(200, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(timer != null && _bExplorationComplete) {				
					timer.stop();
					timer = null;
				}
				
				makeNextMove();
			}
		});
		timer.setRepeats(true);
		timer.setInitialDelay(1000);
		timer.start();
	}
	
	/**
	 * Just a temporary function for testing exploration
	 */
	public void stopExploration() {
		
		if(timer != null) {				
			timer.stop();
			timer = null;
		}
	}
	
	/**
	 * Instructs the robot to make the next move<p>
	 * 
	 * Consists of 2 parts:<br>
	 * 1. Sense - Use sensors to update robot map information<br>
	 * 2. Logic - Decide on the next move to make
	 * 
	 */
	public void makeNextMove() {
		// Sense its surroundings
		this.sense();
		
		_robotMap.revalidate();
		_robotMap.repaint();
		
		// Logic to make the next move
		this.logic();
	}
	
	/**
	 * This is where all the logic will go<p>
	 * NOTE: Call this after invoking {@link #sense}
	 */
	public void logic() {
		
		// Exploration complete, do nothing
		if(_bExplorationComplete)
			return;
		
		// Robot reached goal zone
		if(withinGoalZone(_robotMapPosRow, _robotMapPosCol))
			_bReachedGoal = true;
		
		// Robot reached goal zone and is back at the start zone
		if(_bReachedGoal && withinStartZone(_robotMapPosRow, _robotMapPosCol)) {
			_bExplorationComplete = true;
			return;
		}
		
		boolean frontWall = hasFrontWall();
		boolean leftWall = hasLeftWall();
		boolean rightWall = hasRightWall();
		
		// Temporary simple (and naive) logic just for testing
		// Insert logic here
		if(!frontWall) {
			
			// No front wall
			moveStraight();
		}
		else if(!rightWall) {
			
			// Front wall and no right wall
			turnRight();
		}
		else if(!leftWall) {
			
			// Front wall, right wall, and no left wall
			turnLeft();
		}
		else {
			// Front wall, right wall, and left wall
			turn180();
		}
	}
	
	/**
	 * This should update the robot's map based on available sensor information
	 */
	public void sense() {
		for (Sensor s : _sensors) {
			int freeGrids = s.sense(_realMap);
			int sensorPosRow = s.getSensorPosRow();
			int sensorPosCol = s.getSensorPosCol();
			DIRECTION sensorDir = s.getSensorDirection();
			int sensorMinRange = s.getMinRange();
			int sensorMaxRange = s.getMaxRange();

			/*
			System.out.println("Sensor - " + sensorPosRow + ", " + sensorPosCol
					+ ", " + sensorDir.toString() + ", Free Grids: "
					+ freeGrids);*/

			Grid [][] robotMapGrids = _robotMap.getMapGrids();
			for (int currGrid = sensorMinRange; currGrid <= sensorMaxRange; currGrid++) {

				int gridRow = sensorPosRow
						+ ((sensorDir == DIRECTION.NORTH) ? (-1 * currGrid)
								: (sensorDir == DIRECTION.SOUTH) ? currGrid : 0);

				int gridCol = sensorPosCol
						+ ((sensorDir == DIRECTION.WEST) ? (-1 * currGrid)
								: (sensorDir == DIRECTION.EAST) ? currGrid : 0);

				// If the current grid is within number of free grids detected
				if (currGrid <= freeGrids) {
					robotMapGrids[gridRow][gridCol].setExplored(true);
				} else {

					// Current grid is less than or equal to max sensor range,
					// but greater than number of free grids
					// i.e. current grid is an obstacle
					robotMapGrids[gridRow][gridCol].setExplored(true);
					robotMapGrids[gridRow][gridCol].markAsObstacle();

					break;
				}
			}
		}
	}
	
	/** Check for walls directly in front of the robot
	 * 
	 * @return True if there is a wall/obstacle in front of the robot
	 */
	public boolean hasFrontWall() {
		
		Grid [][] robotMapGrids = _robotMap.getMapGrids();
		int frontWallRow, frontWallCol;
		
		switch(_robotDirection) {
		case EAST:
			frontWallRow = _robotMapPosRow;
			frontWallCol = _robotMapPosCol + RobotConstants.ROBOT_SIZE;
			for (int currRow = frontWallRow; currRow < frontWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][frontWallCol].isExplored()
						&& robotMapGrids[currRow][frontWallCol].isObstacle())
					return true;
			}
			break;
		case NORTH:
			frontWallRow = (_robotMapPosRow - 1);
			frontWallCol = _robotMapPosCol;
			for (int currCol = frontWallCol; currCol < frontWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[frontWallRow][currCol].isExplored()
						&& robotMapGrids[frontWallRow][currCol].isObstacle())
					return true;
			}
			break;
		case SOUTH:
			frontWallRow = _robotMapPosRow + RobotConstants.ROBOT_SIZE;
			frontWallCol = _robotMapPosCol;
			for (int currCol = frontWallCol; currCol < frontWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[frontWallRow][currCol].isExplored()
						&& robotMapGrids[frontWallRow][currCol].isObstacle())
					return true;
			}
			break;
		case WEST:
			frontWallRow = _robotMapPosRow;
			frontWallCol = (_robotMapPosCol - 1);
			for (int currRow = frontWallRow; currRow < frontWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][frontWallCol].isExplored()
						&& robotMapGrids[currRow][frontWallCol].isObstacle())
					return true;
			}
			break;
		}
		
		return false;
	}
	
	/** Check for walls on the left of the robot
	 * 
	 * @return True if there is a wall/obstacle on the left of the robot
	 */
	public boolean hasLeftWall() {
		Grid [][] robotMapGrids = _robotMap.getMapGrids();
		int leftWallRow, leftWallCol;
		
		switch(_robotDirection) {
		case EAST:
			leftWallRow = _robotMapPosRow - 1;
			leftWallCol = _robotMapPosCol;
			for (int currCol = leftWallCol; currCol < leftWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[leftWallRow][currCol].isExplored()
						&& robotMapGrids[leftWallRow][currCol].isObstacle())
					return true;
			}
			break;
		case NORTH:
			leftWallRow = _robotMapPosRow;
			leftWallCol = _robotMapPosCol - 1;
			for (int currRow = leftWallRow; currRow < leftWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][leftWallCol].isExplored()
						&& robotMapGrids[currRow][leftWallCol].isObstacle())
					return true;
			}
			break;
		case SOUTH:
			leftWallRow = _robotMapPosRow;
			leftWallCol = (_robotMapPosCol + RobotConstants.ROBOT_SIZE);
			for (int currRow = leftWallRow; currRow < leftWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][leftWallCol].isExplored()
						&& robotMapGrids[currRow][leftWallCol].isObstacle())
					return true;
			}
			break;
		case WEST:
			leftWallRow = (_robotMapPosRow + RobotConstants.ROBOT_SIZE);
			leftWallCol = _robotMapPosCol;
			for (int currCol = leftWallCol; currCol < leftWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[leftWallRow][currCol].isExplored()
						&& robotMapGrids[leftWallRow][currCol].isObstacle())
					return true;
			}
			break;
		}
		
		return false;
	}
	
	/** Check for walls on the right of the robot
	 * 
	 * @return True if there is a wall/obstacle on the right of the robot
	 */
	public boolean hasRightWall() {
		Grid [][] robotMapGrids = _robotMap.getMapGrids();
		int rightWallRow, rightWallCol;
		
		switch(_robotDirection) {
		case EAST:
			rightWallRow = _robotMapPosRow + RobotConstants.ROBOT_SIZE;
			rightWallCol = _robotMapPosCol;
			for (int currCol = rightWallCol; currCol < rightWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[rightWallRow][currCol].isExplored()
						&& robotMapGrids[rightWallRow][currCol].isObstacle())
					return true;
			}
			break;
		case NORTH:
			rightWallRow = _robotMapPosRow;
			rightWallCol = _robotMapPosCol + RobotConstants.ROBOT_SIZE;
			for (int currRow = rightWallRow; currRow < rightWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][rightWallCol].isExplored()
						&& robotMapGrids[currRow][rightWallCol].isObstacle())
					return true;
			}
			break;
		case SOUTH:
			rightWallRow = _robotMapPosRow;
			rightWallCol = (_robotMapPosCol - 1);
			for (int currRow = rightWallRow; currRow < rightWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][rightWallCol].isExplored()
						&& robotMapGrids[currRow][rightWallCol].isObstacle())
					return true;
			}
			break;
		case WEST:
			rightWallRow = (_robotMapPosRow - 1);
			rightWallCol = _robotMapPosCol;
			for (int currCol = rightWallCol; currCol < rightWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[rightWallRow][currCol].isExplored()
						&& robotMapGrids[rightWallRow][currCol].isObstacle())
					return true;
			}
			break;
		}
		
		return false;
	}
	
	/**
	 * Instructs the robot to move straight by 1 grid in its current direction
	 */
	public void moveStraight() {
		
		int newRobotMapPosRow = _robotMapPosRow;
		int newRobotMapPosCol = _robotMapPosCol;
		
		newRobotMapPosRow += (_robotDirection == DIRECTION.NORTH) ? -1
				: (_robotDirection == DIRECTION.SOUTH) ? 1 : 0;
		
		newRobotMapPosCol += (_robotDirection == DIRECTION.WEST) ? -1
				: (_robotDirection == DIRECTION.EAST) ? 1 : 0;
		
		// Tests the next move before updating its position
		if(testNextMove(newRobotMapPosRow, newRobotMapPosCol)) {
			updatePosition(newRobotMapPosRow, newRobotMapPosCol);
		}
		else {
			System.out.println("INVALID MOVE! Robot will be out of bounds or"
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
	
	/** For getting the robot's position relative the the map */
	public int getRobotMapPosRow() {
		return _robotMapPosRow;
	}
	
	/** For getting the robot's position relative the the map */
	public int getRobotMapPosCol() {
		return _robotMapPosCol;
	}
	
	/** For placing sensors and rendering *//*
	public int getRobotRefPosRow() {
		return _robotRefPosRow;
	}
	
	*//** For placing sensors and rendering *//*
	public int getRobotRefPosCol() {
		return _robotRefPosCol;
	}*/
	
	/** Returns the current direction that the robot is facing */
	public DIRECTION getRobotDir() {
		return _robotDirection;
	}
	
	/** For initializing the robot map */
	public void setRobotMap(RobotMap robotMap) {
		_robotMap = robotMap;
		
		// Pass a reference of the robot to the robot map
		// Just for rendering purposes
		_robotMap.setRobot(this);
	}
	
	/** To reset the robot's starting state */
	public void resetRobotState(int startMapPosRow, int startMapPosCol,
			DIRECTION startDir) {
		
		// Turn the robot to match the specified starting direction
		while (_robotDirection != startDir) {
			this.turnRight();
		}
		
		// Update the robot's position to match the specified starting position
		this.updatePosition(startMapPosRow, startMapPosCol);
		
		// Reset variables used for exploration
		_bReachedGoal = false;
		_bExplorationComplete = false;
	}
	
	public void markStartAsExplored() {
		
		// Gets information about the robot
        int robotMapPosRow = this.getRobotMapPosRow();
        int robotMapPosCol = this.getRobotMapPosCol();
        
        /*
        DIRECTION robotDir = this.getRobotDir();
        
        // Change the 'robot's position' to get the actual area!
        switch(robotDir) {
		case EAST:
			// Nothing to be changed if facing East
			break;
		case NORTH:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case SOUTH:
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case WEST:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
        }*/
        
        Grid [][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {
				
				robotMapGrids[mapRow][mapCol].setExplored(true);
			}
		}
	}
	
	/**
	 * Check if the robot has completed exploration
	 * 
	 * @return True if the robot has completed exploration
	 */
	public boolean isExplorationComplete() {
		return _bExplorationComplete;
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
		
		return ((robotPosRow >= 1) &&
				((robotPosRow + robotSize) <= (MapConstants.MAP_ROWS - 2)) &&
				(robotPosCol >= 1) &&
				((robotPosCol + robotSize) <= (MapConstants.MAP_COLS - 2)));
		
		/*
		switch (_robotDirection) {

		case NORTH:
			return (((robotPosRow - robotSize) >= 1)
					&& ((robotPosCol + robotSize) <= (MapConstants.MAP_COLS - 2)));

		case SOUTH:
			return (((robotPosRow + robotSize) <= (MapConstants.MAP_ROWS - 2))
					&& ((robotPosCol - robotSize) >= 1));
			
		case EAST:
			return (((robotPosRow + robotSize) <= (MapConstants.MAP_ROWS - 2))
					&& ((robotPosCol + robotSize) <= (MapConstants.MAP_COLS - 2)));

		case WEST:
			return (((robotPosRow - robotSize) >= 1)
					&& ((robotPosCol - robotSize) >= 1));
			
		default:
			return false;
		}*/
	}
	
	/**
	 * Function for validating the robot's next move<br>
	 * Ensures that the robot does not collide with any known obstacles
	 * 
	 * @return true if the robot will be safe
	 */
	private boolean checkForObstacles(int robotPosRow, int robotPosCol) {
		
		/*
        DIRECTION robotDir = this.getRobotDir();
        
        // Change the 'robot's position' to get the actual area!
        switch(robotDir) {
		case EAST:
			// Nothing to be changed if facing East
			break;
		case NORTH:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case SOUTH:
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case WEST:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
        }*/
        
        // Check for obstacles within robot's new position
        Grid [][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotPosRow; mapRow < robotPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotPosCol; mapCol < robotPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {
				
				if(robotMapGrids[mapRow][mapCol].isExplored() &&
						robotMapGrids[mapRow][mapCol].isObstacle())
					return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Check if the robot is within the start zone
	 * 
	 * @param robotMapPosRow The robot's current row on the map
	 * @param robotMapPosCol The robot's current column on the map
	 * 
	 * @return True if the robot is within the start zone
	 */
	private boolean withinStartZone(int robotMapPosRow, int robotMapPosCol) {

		/*
		DIRECTION robotDir = this.getRobotDir();
        
        // Change the 'robot's position' to get the actual area!
        switch(robotDir) {
		case EAST:
			// Nothing to be changed if facing East
			break;
		case NORTH:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case SOUTH:
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case WEST:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
        }*/
        
        // Check if the entire robot is within the start zone
        Grid [][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {
				
				if(!robotMapGrids[mapRow][mapCol].isExplored())
					return false;
				else if(!_robotMap.isStartZone(mapRow, mapCol))
					return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Check if the robot is within the goal zone
	 *
	 * @param robotMapPosRow The robot's current row
	 * @param robotMapPosCol The robot's current column
	 * 
	 * @return True if the robot is within the goal zone
	 */
	private boolean withinGoalZone(int robotMapPosRow, int robotMapPosCol) {

		/*
		DIRECTION robotDir = this.getRobotDir();
        
        // Change the 'robot's position' to get the actual area!
        switch(robotDir) {
		case EAST:
			// Nothing to be changed if facing East
			break;
		case NORTH:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case SOUTH:
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
		case WEST:
			robotPosRow -= (RobotConstants.ROBOT_SIZE - 1);
			robotPosCol -= (RobotConstants.ROBOT_SIZE - 1);
			break;
        }*/
        
        // Check if the entire robot is within the start zone
        Grid [][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {
				
				if(!robotMapGrids[mapRow][mapCol].isExplored())
					return false;
				else if(!_robotMap.isGoalZone(mapRow, mapCol))
					return false;
			}
		}
		
		return true;
	}
	
	private void updatePosition(int newRobotMapPosRow, int newRobotMapPosCol) {
		
		// Determine the change in row/column of the robot
		int deltaRow = newRobotMapPosRow - _robotMapPosRow;
		int deltaCol = newRobotMapPosCol - _robotMapPosCol;
		
		// Update the path in the robot map
		RobotMap.PathGrid[][] pathGrids = null;
		if (_robotMap != null)
			pathGrids = _robotMap.getPathGrids();
		if (pathGrids != null) {
			switch (_robotDirection) {
			case EAST:
				pathGrids[_robotMapPosRow][_robotMapPosCol].cE = true;
				pathGrids[newRobotMapPosRow][newRobotMapPosCol].cW = true;
				break;
			case NORTH:
				pathGrids[_robotMapPosRow][_robotMapPosCol].cN = true;
				pathGrids[newRobotMapPosRow][newRobotMapPosCol].cS = true;
				break;
			case SOUTH:
				pathGrids[_robotMapPosRow][_robotMapPosCol].cS = true;
				pathGrids[newRobotMapPosRow][newRobotMapPosCol].cN = true;
				break;
			case WEST:
				pathGrids[_robotMapPosRow][_robotMapPosCol].cW = true;
				pathGrids[newRobotMapPosRow][newRobotMapPosCol].cE = true;
				break;
			}
		}
		
		// Update the actual position of the robot
		_robotMapPosRow = newRobotMapPosRow;
		_robotMapPosCol = newRobotMapPosCol;
		
		// Update the positions of the sensors
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
		
		/*DIRECTION prevDirection = _robotDirection;*/

		// Center of robot
		int xC = 0;
		int yC = 0;
		
		xC = (_robotMapPosCol * MapConstants.GRID_SIZE)
				+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
		yC = (_robotMapPosRow * MapConstants.GRID_SIZE)
				+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
		
		/*
		switch(prevDirection) {
		case EAST:
			xC = (_robotRefPosCol * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			yC = (_robotRefPosRow * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			break;
		case NORTH:
			xC = (_robotRefPosCol * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			yC = ((_robotRefPosRow + 1) * MapConstants.GRID_SIZE)
					- (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			break;
		case SOUTH:
			xC = ((_robotRefPosCol + 1) * MapConstants.GRID_SIZE)
					- (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			yC = (_robotRefPosRow * MapConstants.GRID_SIZE)
					+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			break;
		case WEST:
			xC = ((_robotRefPosCol + 1) * MapConstants.GRID_SIZE)
					- (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			yC = ((_robotRefPosRow + 1) * MapConstants.GRID_SIZE)
					- (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
			break;
		}*/
		
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
			
			/*System.out.println("turn() -> New Pos: " + s.getSensorPosRow() + ", "
					+ s.getSensorPosCol() + " New Direction: " +
					s.getSensorDirection().toString());*/
		}
		
		// Rotate the robot
		_robotDirection = bClockwise ? DIRECTION.getNext(_robotDirection)
				: DIRECTION.getPrevious(_robotDirection);
		
		/*
		// Remaining 'size' after taking into consideration the reference grid
		int robotRemainingSize = RobotConstants.ROBOT_SIZE - 1;
		
		// Update the 'reference grid' based on rotation
		if(bClockwise) {
			if(prevDirection == DIRECTION.NORTH) 		// N to E
				_robotRefPosRow -= robotRemainingSize;
			else if(prevDirection == DIRECTION.EAST) 	// E to S
				_robotRefPosCol += robotRemainingSize;
			else if(prevDirection == DIRECTION.SOUTH) 	// S to W
				_robotRefPosRow += robotRemainingSize;
			else										// W to N
				_robotRefPosCol -= robotRemainingSize;
		}
		else {
			if(prevDirection == DIRECTION.NORTH) 		// N to W
				_robotRefPosCol += robotRemainingSize;
			else if(prevDirection == DIRECTION.WEST)	// W to S
				_robotRefPosRow -= robotRemainingSize;
			else if(prevDirection == DIRECTION.SOUTH)	// S to E
				_robotRefPosCol -= robotRemainingSize;
			else										// E to N
				_robotRefPosRow += robotRemainingSize;
		}*/
	}
	
}
