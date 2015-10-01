package robot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.Stack;

import javax.swing.Timer;

import leaderboard.CommMgr;
import map.Grid;
import map.MapConstants;
import map.RealMap;
import robot.RobotConstants.DIRECTION;

public class Robot implements Serializable {

	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = 893013115674116452L;

	// Robot's actual position on the map (In grids)
	// NOTE: This will be the robot's position relative to the origin grid,
	// i.e. the robot's closest grid to the origin grid (Row 1, Col 1)
	private int _robotMapPosRow;
	private int _robotMapPosCol;

	// Robot's current direction
	private DIRECTION _robotDirection;
	
	// Robot's starting direction
	private transient DIRECTION _robotStartDir;

	// Robot's collection of sensors
	private ArrayList<Sensor> _sensors = null;

	// Robot's settings for exploration
	private int _stepsPerSecond = RobotConstants.DEFAULT_STEPS_PER_SECOND;
	private int _coverageLimit = RobotConstants.DEFAULT_COVERAGE_LIMIT;
	private int _timeLimit = RobotConstants.DEFAULT_TIME_LIMIT;
	private boolean _bCoverageLimited = false;
	private boolean _bTimeLimited = false;

	// Robot's robot map
	private transient RobotMap _robotMap = null; // For determining next action
	private transient RealMap _realMap = null; // For detecting obstacles

	// Some memory for the robot here
	private transient boolean _bPreviousLeftWall = false;
	private transient boolean _bReachedGoal = false;
	private transient boolean _bExplorationComplete = false;

	// Timer for controlling robot movement
	private transient Timer _exploreTimer = null;
	private transient int _timerIntervals = 0;

	// Number of explored grids required to reach coverage limit
	private transient int _explorationTarget = 0;

	// Elapsed time for the exploration phase (in milliseconds)
	private transient int _elapsedExplorationTime = 0;

	// For performing shortest path
	private transient Queue<INSTRUCTION> _shortestPathInstructions = null;
	private transient Timer _shortestPathTimer = null;

	// For unexploring unexplored areas
	private transient Queue<INSTRUCTION> _exploreUnexploredInstructions = null;
	private transient Timer _exploreUnexploredTimer = null;
	private transient Stack<Grid> _unexploredGrids = null;

	// For physical exploration
	private transient Timer _phyExploreTimer = null;
	private transient boolean _bPhyExConnected = false;
	private transient int _phyExErrors = 0;
	private transient String _phyExRcvMsg = null;
	private transient boolean _bPhyExStarted = false;
	private transient static final String START_PHY_EXPLORE = "1,EXPLORE";
	private transient String _phyExCmdMsg = null;
	private transient int _movesSinceLastCalibration = 0;
	private transient static final int MAX_MOVES_BEFORE_CALIBRATION = 5;

	// For physical shortest path
	private transient Timer _phySpTimer = null;
	private transient boolean _bPhySpConnected = false;
	private transient int _phySpErrors = 0;
	private transient String _phySpRcvMsg = null;
	private transient boolean _bPhySpStarted = false;
	private transient static final String START_PHY_SP = "1,SHORTESTPATH";
	private transient String _phySpCmdMsg = null;

	public Robot(int robotMapPosRow, int robotMapPosCol,
			DIRECTION robotDirection) {

		_robotMapPosRow = robotMapPosRow;
		_robotMapPosCol = robotMapPosCol;

		_robotDirection = robotDirection;

		_sensors = new ArrayList<Sensor>();
	}

	public Robot(int robotMapPosRow, int robotMapPosCol,
			DIRECTION robotDirection, ArrayList<Sensor> sensors) {

		_robotMapPosRow = robotMapPosRow;
		_robotMapPosCol = robotMapPosCol;

		_robotDirection = robotDirection;

		if (sensors != null)
			_sensors = new ArrayList<Sensor>();
		else
			_sensors = new ArrayList<Sensor>();
	}

	/**
	 * Provide the robot with a copy of the real map, to be used with the
	 * sensors - ONLY FOR SIMULATOR
	 * 
	 * @param realMap
	 *            The real map with the obstacles
	 */
	public void setRealMap(final RealMap realMap) {
		_realMap = realMap;
	}

	/**
	 * Adds a new sensor to the robot
	 * 
	 * @param newSensor
	 *            The sensor to be added
	 */
	public void addSensor(Sensor newSensor) {

		_sensors.add(newSensor);
	}

	/**
	 * Gets the list of the robot's sensors
	 * 
	 * @return The list of sensors on the robot
	 */
	public ArrayList<Sensor> getSensors() {
		return _sensors;
	}

	/**
	 * For starting exploration
	 */
	public void startExploration() {

		System.out.println("\nStarting exploration!");

		// Calculate timer intervals based on the user selected steps per second
		_timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
		System.out.println("Steps Per Second: " + _stepsPerSecond
				+ ", Timer Interval: " + _timerIntervals);

		// Calculate number of explored grids required
		_explorationTarget = (int) ((_coverageLimit / 100.0) * ((MapConstants.MAP_ROWS - 2) * (MapConstants.MAP_COLS - 2)));
		System.out.println("Exploration target (In grids): "
				+ _explorationTarget);

		// Reset the elapsed exploration time (in milliseconds)
		_elapsedExplorationTime = 0;

		_exploreTimer = new Timer(_timerIntervals, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (_exploreTimer != null && _bExplorationComplete) {
					_exploreTimer.stop();
					_exploreTimer = null;
				} else {
					// Make the next move
					makeNextMove();

					// Update elapsed time
					_elapsedExplorationTime += _timerIntervals;
				}
			}
		});
		_exploreTimer.setRepeats(true);
		_exploreTimer.setInitialDelay(1000);
		_exploreTimer.start();
	}

	/**
	 * For stopping exploration
	 */
	public void stopExploration() {

		if (_exploreTimer != null) {
			_exploreTimer.stop();
			_exploreTimer = null;
		}
	}

	/** For exploring any unexplored area */
	public void startExploringUnexplored(Grid current, DIRECTION currDir,
			Grid target, Grid[][] robotMap) {

		Stack<Grid> exploreUnexploredPath = findShortestPath(current, target,
				currDir, robotMap);

		if (exploreUnexploredPath == null) {

			System.out.println("startExploringUnexplored()"
					+ " -> shortestPath is NULL");

			if (_unexploredGrids.isEmpty()) {
				// Start the shortest path back to the starting grid
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
				Grid startingGrid = robotMap[1][1];

				if (!withinStartZone(_robotMapPosRow, _robotMapPosCol)) {
					System.out.println("I need to go back to the start");
					startShortestPath(currentGrid, _robotDirection,
							startingGrid, robotMap);
				}
			} else {
				startExploringUnexplored(current, currDir,
						_unexploredGrids.pop(), robotMap);
			}
		} else {

			_exploreUnexploredInstructions = generateThePath(exploreUnexploredPath);

			// Calculate timer intervals based on the user
			// selected steps per second
			_timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);

			_exploreUnexploredTimer = new Timer(_timerIntervals,
					new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {

							if (_exploreUnexploredInstructions.isEmpty()) {
								
								if(_exploreUnexploredTimer != null) {
									_exploreUnexploredTimer.stop();
									_exploreUnexploredTimer = null;
								}

								// Check to see if there are
								// anymore unexplored grids
								_unexploredGrids = getUnexploredGrids();
								if (!_unexploredGrids.isEmpty()) {

									// Start shortest path to the next
									// unexplored grid
									Grid[][] robotMap = _robotMap.getMapGrids();
									Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];

									startExploringUnexplored(currentGrid,
											_robotDirection,
											_unexploredGrids.pop(), robotMap);
								} else {

									// Start the shortest path back to the
									// starting grid
									Grid[][] robotMap = _robotMap.getMapGrids();
									Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
									Grid startingGrid = robotMap[1][1];

									if (currentGrid != startingGrid) {
										startShortestPath(currentGrid,
												_robotDirection, startingGrid,
												robotMap);
									}
								}

							} else {

								// Perform next instruction
								switch (_exploreUnexploredInstructions.poll()) {
								case MOVE_STRAIGHT:
									moveStraight();
									break;
								case TURN_LEFT:
									turnLeft();
									break;
								case TURN_RIGHT:
									turnRight();
									break;
								}
								Robot.this.sense();
							}
						}
					});
			_exploreUnexploredTimer.setRepeats(true);
			_exploreUnexploredTimer.setInitialDelay(0);
			_exploreUnexploredTimer.start();
		}
	}

	/** For triggering the shortest path algorithm */
	public void startShortestPath() {

		Grid[][] robotMap = _robotMap.getMapGrids();

		Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];

		int goalGridRow = MapConstants.GOAL_GRID_ROW; // Row 13
		int goalGridCol = MapConstants.GOAL_GRID_COL; // Column 18
		Grid goalGrid = robotMap[goalGridRow][goalGridCol];

		System.out
				.println("\nstartShortestPath(void) -> Starting row, col: "
						+ _robotMapPosRow + ", " + _robotMapPosCol
						+ ", Goal row, col: " + goalGridRow + ", "
						+ goalGridCol + "\n");

		startShortestPath(currentGrid, _robotDirection, goalGrid, robotMap);
	}

	/** For starting shortest path */
	private void startShortestPath(Grid current, DIRECTION currDir,
			Grid target, Grid[][] robotMap) {

		Stack<Grid> shortestPath = findShortestPath(current, target, currDir,
				robotMap);

		if (shortestPath == null) {

			System.out.println("startShortestPath() -> shortestPath is NULL");
			return;
		}

		_shortestPathInstructions = generateThePath(shortestPath);

		// Calculate timer intervals based on the user selected steps per second
		_timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
		System.out.println("Steps Per Second: " + _stepsPerSecond
				+ ", Timer Interval: " + _timerIntervals);

		_shortestPathTimer = new Timer(_timerIntervals, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (_shortestPathInstructions.isEmpty()) {
					
					// If target grid is within the start zone, i.e.
					// shortestPath is being used to go back to the start zone
					if(_robotMap.isStartZone(_robotMapPosRow, _robotMapPosCol)) {
						System.out.println("startShortestPath()->"
								+ " Current Direction: " + _robotDirection);
						
						// If the robot is not facing the specified starting direction
						if (_robotDirection != _robotStartDir) {

							// Turn the robot to match the specified starting direction
							while (_robotDirection != _robotStartDir) {
								turnRight();
							}
							
							System.out.println("startShortestPath()->"
									+ " Final Ending Direction: " + _robotDirection);
						}
					}
					
					if(_shortestPathTimer != null) {
						_shortestPathTimer.stop();
						_shortestPathTimer = null;
					}
				} else {

					// Perform next instruction
					switch (_shortestPathInstructions.poll()) {
					case MOVE_STRAIGHT:
						moveStraight();
						break;
					case TURN_LEFT:
						turnLeft();
						break;
					case TURN_RIGHT:
						turnRight();
						break;
					}
				}
			}
		});
		_shortestPathTimer.setRepeats(true);
		_shortestPathTimer.setInitialDelay(0);
		_shortestPathTimer.start();
	}

	/**
	 * Instructs the robot to make the next move
	 * <p>
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
	 * This is where all the logic will go
	 * <p>
	 * NOTE: Call this after invoking {@link #sense}
	 */
	public void logic() {

		// Exploration complete, do nothing
		if (_bExplorationComplete)
			return;

		// Robot reached goal zone
		if (withinGoalZone(_robotMapPosRow, _robotMapPosCol))
			_bReachedGoal = true;

		if (_bCoverageLimited) {
			if (coverageLimitReached()) {

				// Stop exploration
				_bExplorationComplete = true;

				// Start the shortest path back to the starting grid
				Grid[][] robotMap = _robotMap.getMapGrids();
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
				Grid startingGrid = robotMap[1][1];

				startShortestPath(currentGrid, _robotDirection, startingGrid,
						robotMap);

				return;
			}
		}

		if (_bTimeLimited) {
			if ((_elapsedExplorationTime / 1000) >= _timeLimit) {

				// Stop exploration
				_bExplorationComplete = true;

				// Start the shortest path back to the starting grid
				Grid[][] robotMap = _robotMap.getMapGrids();
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
				Grid startingGrid = robotMap[1][1];

				startShortestPath(currentGrid, _robotDirection, startingGrid,
						robotMap);

				return;
			}
		}

		if (_bReachedGoal && withinStartZone(_robotMapPosRow, _robotMapPosCol)) {

			_bExplorationComplete = true;

			_unexploredGrids = getUnexploredGrids();
			if (!_unexploredGrids.isEmpty()) {

				// Start shortest path to the first unexplored grid
				Grid[][] robotMap = _robotMap.getMapGrids();
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];

				startExploringUnexplored(currentGrid, _robotDirection,
						_unexploredGrids.pop(), robotMap);
			}

			return;
		}

		boolean frontWall = hasFrontWall();
		boolean leftWall = hasLeftWall();
		boolean rightWall = hasRightWall();

		// (No leftWall AND previousLeftWall) OR
		// (frontWall AND No leftWall AND rightWall)
		if ((!leftWall && _bPreviousLeftWall) || (frontWall && !leftWall
				&& rightWall))
			turnLeft();

		// (frontWall AND No rightWall)
		else if (frontWall && !rightWall)
			turnRight();

		// (frontWall AND leftWall AND rightWall)
		else if (frontWall && leftWall && rightWall)
			turn180();

		else
			moveStraight();

		// Save current leftWall state into _bPreviousLeftWall
		_bPreviousLeftWall = leftWall;
	}

	/**
	 * This should update the robot's map based on available sensor information
	 */
	public void sense() {
		
		// Weightage of the sensors
		double[] sensorWeightage = {3.0, 3.0, 3.0, 1.5, 1.0, 1.0};
		
		int sensorIndex = 0;
		
		for (Sensor s : _sensors) {
			int freeGrids = s.sense(_realMap);
			int sensorPosRow = s.getSensorPosRow();
			int sensorPosCol = s.getSensorPosCol();
			DIRECTION sensorDir = s.getSensorDirection();
			int sensorMinRange = s.getMinRange();
			int sensorMaxRange = s.getMaxRange();

			/*
			 * System.out.println("Sensor - " + sensorPosRow + ", " +
			 * sensorPosCol + ", " + sensorDir.toString() + ", Free Grids: " +
			 * freeGrids);
			 */

			Grid[][] robotMapGrids = _robotMap.getMapGrids();
			for (int currGrid = sensorMinRange; currGrid <= sensorMaxRange; currGrid++) {

				int gridRow = sensorPosRow
						+ ((sensorDir == DIRECTION.NORTH) ? (-1 * currGrid)
								: (sensorDir == DIRECTION.SOUTH) ? currGrid : 0);

				int gridCol = sensorPosCol
						+ ((sensorDir == DIRECTION.WEST) ? (-1 * currGrid)
								: (sensorDir == DIRECTION.EAST) ? currGrid : 0);
				
				// Calculate the truth value to be used for the current reading
				double truthValue = 1.0/((double) currGrid);
				truthValue *= sensorWeightage[sensorIndex];

				// If the current grid is within number of free grids detected
				if (currGrid <= freeGrids) {
					//robotMapGrids[gridRow][gridCol].setExplored(true);
					
					robotMapGrids[gridRow][gridCol].markAsFreeGrid(
							truthValue);
					
				} else {

					// Current grid is less than or equal to max sensor range,
					// but greater than number of free grids
					// i.e. current grid is an obstacle
					//robotMapGrids[gridRow][gridCol].setExplored(true);
					
					if(!_robotMap.isStartZone(gridRow, gridCol) &&
							!_robotMap.isGoalZone(gridRow, gridCol)) {
						//robotMapGrids[gridRow][gridCol].markAsObstacle();
						
						robotMapGrids[gridRow][gridCol]
								.markAsObstacle(truthValue);
					}

					break;
				}
			}
		}
	}

	/**
	 * Check for walls directly in front of the robot
	 * 
	 * @return True if there is a wall/obstacle in front of the robot
	 */
	public boolean hasFrontWall() {

		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		int frontWallRow, frontWallCol;

		switch (_robotDirection) {
		case EAST:
			frontWallRow = _robotMapPosRow;
			frontWallCol = _robotMapPosCol + RobotConstants.ROBOT_SIZE;
			for (int currRow = frontWallRow; currRow < frontWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][frontWallCol].isExplored()
						&& robotMapGrids[currRow][frontWallCol].isObstacle())
					return true;
				else if(_robotMap.isBorderWalls(currRow, frontWallCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(frontWallRow, currCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(frontWallRow, currCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(currRow, frontWallCol)) {
					return true;
				}
			}
			break;
		}

		return false;
	}

	/**
	 * Check for walls on the left of the robot
	 * 
	 * @return True if there is a wall/obstacle on the left of the robot
	 */
	public boolean hasLeftWall() {
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		int leftWallRow, leftWallCol;

		switch (_robotDirection) {
		case EAST:
			leftWallRow = _robotMapPosRow - 1;
			leftWallCol = _robotMapPosCol;
			for (int currCol = leftWallCol; currCol < leftWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[leftWallRow][currCol].isExplored()
						&& robotMapGrids[leftWallRow][currCol].isObstacle())
					return true;
				else if(_robotMap.isBorderWalls(leftWallRow, currCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(currRow, leftWallCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(currRow, leftWallCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(leftWallRow, currCol)) {
					return true;
				}
			}
			break;
		}

		return false;
	}

	/**
	 * Check for walls on the right of the robot
	 * 
	 * @return True if there is a wall/obstacle on the right of the robot
	 */
	public boolean hasRightWall() {
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		int rightWallRow, rightWallCol;

		switch (_robotDirection) {
		case EAST:
			rightWallRow = _robotMapPosRow + RobotConstants.ROBOT_SIZE;
			rightWallCol = _robotMapPosCol;
			for (int currCol = rightWallCol; currCol < rightWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[rightWallRow][currCol].isExplored()
						&& robotMapGrids[rightWallRow][currCol].isObstacle())
					return true;
				else if(_robotMap.isBorderWalls(rightWallRow, currCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(currRow, rightWallCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(currRow, rightWallCol)) {
					return true;
				}
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
				else if(_robotMap.isBorderWalls(rightWallRow, currCol)) {
					return true;
				}
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
		if (testNextMove(newRobotMapPosRow, newRobotMapPosCol)) {
			updatePosition(newRobotMapPosRow, newRobotMapPosCol);
			
			markCurrentPosAsVisited();

			_phyExCmdMsg = "f;"; // Move straight
		} else {
			System.out.println("INVALID MOVE! Robot will be out of bounds or"
					+ " bump into an known obstacle..");

			requestSensorReadings();
		}
	}

	public void turnLeft() {
		turn(false);

		// Send out turning commands
		_phyExCmdMsg = "l;";
	}

	public void turnRight() {
		turn(true);

		// Send out turning commands
		_phyExCmdMsg = "o;";
	}

	public void turn180() {
		turn(true);
		turn(true);

		// Send out turning commands
		_phyExCmdMsg = "o;o;";
	}

	/** For getting the robot's position relative the the map */
	public int getRobotMapPosRow() {
		return _robotMapPosRow;
	}

	/** For getting the robot's position relative the the map */
	public int getRobotMapPosCol() {
		return _robotMapPosCol;
	}

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
		
		_robotStartDir = startDir;

		// Turn the robot to match the specified starting direction
		while (_robotDirection != startDir) {
			this.turnRight();
		}

		// Update the robot's position to match the specified starting position
		this.updatePosition(startMapPosRow, startMapPosCol);

		// Reset variables used for exploration
		_bReachedGoal = false;
		_bExplorationComplete = false;
		_bPreviousLeftWall = false;
	}

	/**
	 * Sets the starting grids as explored
	 */
	public void markStartAsExplored() {

		// Gets information about the robot
		int robotMapPosRow = this.getRobotMapPosRow();
		int robotMapPosCol = this.getRobotMapPosCol();

		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {

				robotMapGrids[mapRow][mapCol].setExplored(true);
			}
		}
	}

	/**
	 * Gets the user-selected steps per second
	 * 
	 * @return The steps per second selected by the user
	 */
	public int getStepsPerSecond() {
		return _stepsPerSecond;
	}

	/**
	 * Gets the user-selected coverage limit
	 * 
	 * @return The coverage limit for the robot to explore before
	 *         auto-termination
	 */
	public int getCoverageLimit() {
		return _coverageLimit;
	}

	/**
	 * Gets the user-selected time limit
	 * 
	 * @return The time limit for the robot to explore before auto-termination
	 */
	public int getTimeLimit() {
		return _timeLimit;
	}

	/**
	 * Indicates whether the robot is going to adhere to the coverage limit
	 * 
	 * @return True if robot is going to terminate exploration automatically
	 *         once the coverage limit has been reached
	 */
	public boolean isCoverageLimited() {
		return _bCoverageLimited;
	}

	/**
	 * Indicates whether the robot is going to adhere to the time limit
	 * 
	 * @return True if the robot is going to terminate exploration automatically
	 *         once the time limit has been reached
	 */
	public boolean isTimeLimited() {
		return _bTimeLimited;
	}

	/**
	 * Update the exploration settings for the robot
	 * 
	 * @param stepsPerSecond
	 *            Exploration speed, in steps per second
	 * @param coverageLimit
	 *            Coverage Limit, [1, 100] percent
	 * @param timeLimit
	 *            Time Limit, [1, 360] seconds
	 * @param bCoverageLimited
	 *            True if using coverage limit
	 * @param bTimeLimited
	 *            True if using time limit
	 */
	public void setExplorationSettings(int stepsPerSecond, int coverageLimit,
			int timeLimit, boolean bCoverageLimited, boolean bTimeLimited) {

		_stepsPerSecond = stepsPerSecond;
		_coverageLimit = coverageLimit;
		_timeLimit = timeLimit;
		_bCoverageLimited = bCoverageLimited;
		_bTimeLimited = bTimeLimited;
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
	 * Simulate the robot's next move
	 * <p>
	 * This is used to prevent the robot from moving out of the arena,<br>
	 * or moving straight into known obstacles
	 * 
	 * @param newRobotPosRow
	 *            The new row
	 * @param newRobotPosCol
	 *            The new column
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

		return ((robotPosRow >= 1)
				&& ((robotPosRow + robotSize) <= (MapConstants.MAP_ROWS - 2))
				&& (robotPosCol >= 1) && ((robotPosCol + robotSize) <= (MapConstants.MAP_COLS - 2)));
	}

	/**
	 * Function for validating the robot's next move<br>
	 * Ensures that the robot does not collide with any known obstacles
	 * 
	 * @return true if the robot will be safe
	 */
	private boolean checkForObstacles(int robotPosRow, int robotPosCol) {

		// Check for obstacles within robot's new position
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotPosRow; mapRow < robotPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotPosCol; mapCol < robotPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {

				if (robotMapGrids[mapRow][mapCol].isExplored()
						&& robotMapGrids[mapRow][mapCol].isObstacle())
					return false;
			}
		}
		return true;
	}

	/**
	 * Check if the robot is within the start zone
	 * 
	 * @param robotMapPosRow
	 *            The robot's current row on the map
	 * @param robotMapPosCol
	 *            The robot's current column on the map
	 * 
	 * @return True if the robot is within the start zone
	 */
	private boolean withinStartZone(int robotMapPosRow, int robotMapPosCol) {

		// Check if the entire robot is within the start zone
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {

				if (!robotMapGrids[mapRow][mapCol].isExplored())
					return false;
				else if (!_robotMap.isStartZone(mapRow, mapCol))
					return false;
			}
		}
		return true;
	}

	/**
	 * Check if the robot is within the goal zone
	 *
	 * @param robotMapPosRow
	 *            The robot's current row
	 * @param robotMapPosCol
	 *            The robot's current column
	 * 
	 * @return True if the robot is within the goal zone
	 */
	private boolean withinGoalZone(int robotMapPosRow, int robotMapPosCol) {

		// Check if the entire robot is within the start zone
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {

				if (!robotMapGrids[mapRow][mapCol].isExplored())
					return false;
				else if (!_robotMap.isGoalZone(mapRow, mapCol))
					return false;
			}
		}
		return true;
	}

	/**
	 * Function for updating the robot's position on the map
	 * 
	 * @param newRobotMapPosRow
	 *            The new row of the robot on the map
	 * @param newRobotMapPosCol
	 *            The new column of the robot on the map
	 */
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
		for (Sensor s : _sensors) {
			s.updateSensorPos(s.getSensorPosRow() + deltaRow,
					s.getSensorPosCol() + deltaCol);
		}
	}

	/**
	 * Function for turning the robot
	 * 
	 * @param bClockwise
	 *            True if robot is turning in the clockwise direction
	 */
	private void turn(boolean bClockwise) {

		// Center of robot
		int xC = 0;
		int yC = 0;

		// Determine the center of the robot based on its current position
		xC = (_robotMapPosCol * MapConstants.GRID_SIZE)
				+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);
		yC = (_robotMapPosRow * MapConstants.GRID_SIZE)
				+ (RobotConstants.ROBOT_SIZE * MapConstants.GRID_SIZE / 2);

		// x = ((x - x_origin) * cos(angle)) - ((y_origin - y) * sin(angle)) +
		// x_origin
		// y = ((y_origin - y) * cos(angle)) - ((x - x_origin) * sin(angle)) +
		// y_origin

		// Rotate sensors
		for (Sensor s : _sensors) {
			/*
			 * System.out.println("Old Pos: " + s.getSensorPosRow() + ", " +
			 * s.getSensorPosCol() + " Old Direction: " +
			 * s.getSensorDirection().toString());
			 */

			int s_xC = (s.getSensorPosCol() * MapConstants.GRID_SIZE)
					+ (MapConstants.GRID_SIZE / 2);
			int s_yC = (s.getSensorPosRow() * MapConstants.GRID_SIZE)
					+ (MapConstants.GRID_SIZE / 2);

			// 90 degrees rotation
			double angle = Math.PI / 2.0;
			if (bClockwise)
				angle *= -1;

			double new_s_xC = ((s_xC - xC) * Math.cos(angle))
					- ((yC - s_yC) * Math.sin(angle)) + xC;
			double new_s_yC = ((yC - s_yC) * Math.cos(angle))
					- ((s_xC - xC) * Math.sin(angle)) + yC;

			int newSensorPosCol = (int) (new_s_xC / MapConstants.GRID_SIZE);
			int newSensorPosRow = (int) (new_s_yC / MapConstants.GRID_SIZE);

			s.updateSensorPos(newSensorPosRow, newSensorPosCol);
			s.updateSensorDirection(bClockwise ? DIRECTION.getNext(s
					.getSensorDirection()) : DIRECTION.getPrevious(s
					.getSensorDirection()));

			/*
			 * System.out.println("turn() -> New Pos: " + s.getSensorPosRow() +
			 * ", " + s.getSensorPosCol() + " New Direction: " +
			 * s.getSensorDirection().toString());
			 */
		}

		// Rotate the robot
		_robotDirection = bClockwise ? DIRECTION.getNext(_robotDirection)
				: DIRECTION.getPrevious(_robotDirection);
	}

	/** LiangLiang's part starts here ************************************* */

	private Stack<Grid> findShortestPath(Grid startingGrid, Grid endingGrid,
			DIRECTION dir, Grid[][] map) {

		Grid endGrid = null;
		Grid startGrid = startingGrid;

		int endingGridRow = endingGrid.getRow();
		int endingGridCol = endingGrid.getCol();

		if (testNextMove(endingGridRow, endingGridCol, true)) {
			endGrid = endingGrid;

		} else {
			endGrid = findReachableGrid(endingGrid);
			if (endGrid == null)
				return null;
		}

		System.out.println("findShortestPath() -> Reachable end grid: "
				+ endGrid.getRow() + ", " + endGrid.getCol());

		Stack<Grid> shortestPath = new Stack<Grid>();
		Stack<Grid> checkedGrids = new Stack<Grid>();

		shortestPath.push(endGrid);
		checkedGrids.push(startGrid);

		Grid targetGrid = null;
		Grid nextGrid = startGrid;
		Grid[] neighbouringGrids = new Grid[4];

		boolean bFoundShortestPath = false;
		Stack<DIRECTION> currDir = new Stack<DIRECTION>();
		currDir.push(dir);
		// Array of direction on each grid that moves to it
		// 1: east, 2: west, 4: south, 8: north;
		int[][] gridDir = new int[MapConstants.MAP_ROWS][MapConstants.MAP_COLS];
		for (int i = 0; i < MapConstants.MAP_ROWS; i++) {
			for (int j = 0; j < MapConstants.MAP_COLS; j++) {
				gridDir[i][j] = 0;
			}
		}

		double[][] gValues = new double[MapConstants.MAP_ROWS][MapConstants.MAP_COLS];

		// Initialization of gValues array
		for (int i = 0; i < MapConstants.MAP_ROWS; i++) {
			for (int j = 0; j < MapConstants.MAP_COLS; j++) {
				if (map[i][j].isObstacle() || _robotMap.isBorderWalls(i, j))
					gValues[i][j] = Double.NEGATIVE_INFINITY;
				else
					gValues[i][j] = 0;
			}
		}

		System.out.println("\nfindShortestPath() ->"
				+ " Starting search for shortest path!");
		
		// Start looking for the shortest path
		while (!bFoundShortestPath) {

			targetGrid = nextGrid;
			int targetGridRow = targetGrid.getRow();
			int targetGridCol = targetGrid.getCol();

			/*
			 * System.out.println("\t\ttargetGrid (row, col): " + targetGridRow
			 * + ", " + targetGridCol);
			 */

			// The four direct neighbouring grids of targetGrid
			// 0 - Eastern Grid, 1 - Western Grid
			// 2 - Southern Grid, 3 - Northest Grid
			neighbouringGrids[0] = map[targetGrid.getRow()][targetGrid.getCol() + 1];
			neighbouringGrids[1] = map[targetGrid.getRow()][targetGrid.getCol() - 1];
			neighbouringGrids[2] = map[targetGrid.getRow() + 1][targetGrid
					.getCol()];
			neighbouringGrids[3] = map[targetGrid.getRow() - 1][targetGrid
					.getCol()];

			for (int neighbourGridIndex = 0; neighbourGridIndex < 4; neighbourGridIndex++) {

				int deltaG = 0;
				if ((neighbourGridIndex == 0 && currDir
						.contains(DIRECTION.EAST))
						|| (neighbourGridIndex == 1 && currDir
								.contains(DIRECTION.WEST))
						|| (neighbourGridIndex == 2 && currDir
								.contains(DIRECTION.SOUTH))
						|| (neighbourGridIndex == 3 && currDir
								.contains(DIRECTION.NORTH))) {

					deltaG = RobotConstants.MOVE_COST;
				} else {
					deltaG = RobotConstants.MOVE_COST
							+ RobotConstants.TURN_COST;
				}

				int neighbourGridRow = neighbouringGrids[neighbourGridIndex]
						.getRow();
				int neighbourGridCol = neighbouringGrids[neighbourGridIndex]
						.getCol();

				// If this grid has not been explored, give it an initial value
				if (gValues[neighbourGridRow][neighbourGridCol] == 0) {

					gValues[neighbourGridRow][neighbourGridCol] = gValues[targetGridRow][targetGridCol]
							+ deltaG;
				}
				// Check if the new value is lesser than the currently assigned
				// value
				else if ((gValues[targetGridRow][targetGridCol] + deltaG) <
						gValues[neighbourGridRow][neighbourGridCol]) {

					gValues[neighbourGridRow][neighbourGridCol] = gValues[targetGridRow][targetGridCol]
							+ deltaG;
				}
			}

			// Keep startGrid gValue unchanged
			gValues[startGrid.getRow()][startGrid.getCol()] = 0;

			// Use minimum to find the next grid to go to
			nextGrid = minimum(targetGrid, endGrid, checkedGrids, gValues);

			// Determine the direct neighbours of the next grid
			Grid[] nextGridNeighbours = new Grid[4];

			// The four direct neighbouring grids of nextGrid
			// 0 - Eastern Grid, 1 - Western Grid
			// 2 - Southern Grid, 3 - Northest Grid
			nextGridNeighbours[0] = map[nextGrid.getRow()][nextGrid.getCol() + 1];
			nextGridNeighbours[1] = map[nextGrid.getRow()][nextGrid.getCol() - 1];
			nextGridNeighbours[2] = map[nextGrid.getRow() + 1][nextGrid
					.getCol()];
			nextGridNeighbours[3] = map[nextGrid.getRow() - 1][nextGrid
					.getCol()];

			// Find the grid that it came from
			double tempMin = Double.POSITIVE_INFINITY;
			Stack<Grid> tempGrids = new Stack<Grid>();
			for (Grid nextGridNeighbour : nextGridNeighbours) {
				int nextGridNeighbourRow = nextGridNeighbour.getRow();
				int nextGridNeighbourCol = nextGridNeighbour.getCol();

				if ((tempMin > gValues[nextGrid.getRow()][nextGrid.getCol()]
						- gValues[nextGridNeighbourRow][nextGridNeighbourCol])
						&& (checkedGrids.contains(nextGridNeighbour))) {

					if(gValues[nextGrid.getRow()][nextGrid.getCol()]
							- gValues[nextGridNeighbourRow][nextGridNeighbourCol] 
							== RobotConstants.MOVE_COST
							||
							gValues[nextGrid.getRow()][nextGrid.getCol()]
							- gValues[nextGridNeighbourRow][nextGridNeighbourCol] 
							== RobotConstants.MOVE_COST + RobotConstants.TURN_COST) {
						tempMin = gValues[nextGrid.getRow()][nextGrid.getCol()]
								- gValues[nextGridNeighbourRow][nextGridNeighbourCol];
					
					}
				}
			}
			if (tempMin == RobotConstants.MOVE_COST + RobotConstants.TURN_COST) {
				for (Grid nextGridNeighbour : nextGridNeighbours) {
					int nextGridNeighbourRow = nextGridNeighbour.getRow();
					int nextGridNeighbourCol = nextGridNeighbour.getCol();

					if (gValues[nextGridNeighbourRow][nextGridNeighbourCol] == gValues[nextGrid
							.getRow()][nextGrid.getCol()] - tempMin
							&& checkedGrids.contains(nextGridNeighbour)) {
						tempGrids.push(nextGridNeighbour);
					}
				}
			} else if (tempMin == RobotConstants.MOVE_COST) {
				for (Grid nextGridNeighbour : nextGridNeighbours) {
					int nextGridNeighbourRow = nextGridNeighbour.getRow();
					int nextGridNeighbourCol = nextGridNeighbour.getCol();

					if (gValues[nextGridNeighbourRow][nextGridNeighbourCol] == gValues[nextGrid
							.getRow()][nextGrid.getCol()] - tempMin
							&& checkedGrids.contains(nextGridNeighbour)
							&& (transI2D(gridDir[nextGridNeighbourRow][nextGridNeighbourCol]) == null
							|| transI2D(
									gridDir[nextGridNeighbourRow][nextGridNeighbourCol])
									.contains(
											getDirFromXToY(nextGridNeighbour,
													nextGrid)))) {
						tempGrids.push(nextGridNeighbour);
					} else if (gValues[nextGridNeighbourRow][nextGridNeighbourCol] == gValues[nextGrid
							.getRow()][nextGrid.getCol()] - 21
							&& checkedGrids.contains(nextGridNeighbour)) {
						tempGrids.push(nextGridNeighbour);
					}
				}
			}

			// Update the direction that the robot is currently facing
			currDir.clear();
			while (!tempGrids.isEmpty()) {
				Grid tempGrid = tempGrids.pop();
				if (tempGrid.getCol() != nextGrid.getCol()) {

					currDir.push((tempGrid.getCol() - nextGrid.getCol() == -1) ? DIRECTION.EAST
							: DIRECTION.WEST);
					gridDir[nextGrid.getRow()][nextGrid.getCol()] += (tempGrid
							.getCol() - nextGrid.getCol() == -1) ? 1 : 2;
				} else if (tempGrid.getRow() != nextGrid.getRow()) {

					currDir.push((tempGrid.getRow() - nextGrid.getRow() == -1) ? DIRECTION.SOUTH
							: DIRECTION.NORTH);
					gridDir[nextGrid.getRow()][nextGrid.getCol()] += (tempGrid
							.getRow() - nextGrid.getRow() == -1) ? 4 : 8;
				}
			}
			if (checkedGrids.contains(nextGrid)) {
				System.out.println("findShortestPath() -> Path not found!");
				return null;
			}

			checkedGrids.push(nextGrid);

			if (checkedGrids.peek() == endGrid)
				bFoundShortestPath = true;
		}

		System.out.println("findShortestPath() -> Path found!");

		// Generating actual shortest Path by tracing from end to start
		Grid currentGrid = endGrid;
		int pathLength = 0;
		DIRECTION tempDir = currDir.peek();
		while (shortestPath.peek() != startGrid) {

			// Determine the direct neighbours of the current grid
			Grid[] currGridNeighbours = new Grid[4];

			// The four direct neighbouring grids of currentGrid
			// 0 - Eastern Grid, 1 - Western Grid
			// 2 - Southern Grid, 3 - Northest Grid
			currGridNeighbours[0] = map[currentGrid.getRow()][currentGrid
					.getCol() + 1];
			currGridNeighbours[1] = map[currentGrid.getRow()][currentGrid
					.getCol() - 1];
			currGridNeighbours[2] = map[currentGrid.getRow() + 1][currentGrid
					.getCol()];
			currGridNeighbours[3] = map[currentGrid.getRow() - 1][currentGrid
					.getCol()];

			double tempMin = Double.POSITIVE_INFINITY;
			Grid tempGrid = null;

			for (Grid currGridNeighbour : currGridNeighbours) {
				int currGridNeighbourRow = currGridNeighbour.getRow();
				int currGridNeighbourCol = currGridNeighbour.getCol();

				if ((tempMin > gValues[currentGrid.getRow()][currentGrid
						.getCol()]
						- gValues[currGridNeighbourRow][currGridNeighbourCol])
						&& (checkedGrids.contains(currGridNeighbour))
						&& gValues[currentGrid.getRow()][currentGrid.getCol()]
								- gValues[currGridNeighbourRow][currGridNeighbourCol] > 0) {

					if(gValues[currentGrid.getRow()][currentGrid.getCol()]
							- gValues[currGridNeighbourRow][currGridNeighbourCol] 
							== RobotConstants.MOVE_COST
							||
							gValues[currentGrid.getRow()][currentGrid.getCol()]
							- gValues[currGridNeighbourRow][currGridNeighbourCol] 
							== RobotConstants.MOVE_COST + RobotConstants.TURN_COST) {
						tempMin = gValues[currentGrid.getRow()][currentGrid.getCol()]
								- gValues[currGridNeighbourRow][currGridNeighbourCol];
					
					}
				}
			}

			if (tempMin == RobotConstants.MOVE_COST + RobotConstants.TURN_COST) {
				for (Grid currGridNeighbour : currGridNeighbours) {
					int currGridNeighbourRow = currGridNeighbour.getRow();
					int currGridNeighbourCol = currGridNeighbour.getCol();

					if (gValues[currGridNeighbourRow][currGridNeighbourCol] == gValues[currentGrid
							.getRow()][currentGrid.getCol()] - tempMin
							&& checkedGrids.contains(currGridNeighbour)) {
						tempGrid = currGridNeighbour;
					}
				}
			} else if (tempMin == RobotConstants.MOVE_COST) {
				for (Grid currGridNeighbour : currGridNeighbours) {
					int currGridNeighbourRow = currGridNeighbour.getRow();
					int currGridNeighbourCol = currGridNeighbour.getCol();

					if (gValues[currGridNeighbourRow][currGridNeighbourCol] == gValues[currentGrid
							.getRow()][currentGrid.getCol()] - tempMin
							&& checkedGrids.contains(currGridNeighbour)
							&& (transI2D(gridDir[currGridNeighbourRow][currGridNeighbourCol]) == null
							|| transI2D(
									gridDir[currGridNeighbourRow][currGridNeighbourCol])
									.contains(
											getDirFromXToY(currGridNeighbour,
													currentGrid)))
							&& tempDir == getDirFromXToY(currGridNeighbour,
									currentGrid)) {
						tempGrid = currGridNeighbour;
					} else if (gValues[currGridNeighbourRow][currGridNeighbourCol] == gValues[currentGrid
							.getRow()][currentGrid.getCol()] - 21
							&& checkedGrids.contains(currGridNeighbour)) {
						tempGrid = currGridNeighbour;
					}
				}

				if (tempGrid == null) {
					for (Grid currGridNeighbour : currGridNeighbours) {
						int currGridNeighbourRow = currGridNeighbour.getRow();
						int currGridNeighbourCol = currGridNeighbour.getCol();
						if (gValues[currGridNeighbourRow][currGridNeighbourCol] == gValues[currentGrid
								.getRow()][currentGrid.getCol()] - tempMin
								&& checkedGrids.contains(currGridNeighbour)
								&& (transI2D(gridDir[currGridNeighbourRow][currGridNeighbourCol]) == null
								|| transI2D(
										gridDir[currGridNeighbourRow][currGridNeighbourCol])
										.contains(
												getDirFromXToY(
														currGridNeighbour,
														currentGrid)))) {
							tempGrid = currGridNeighbour;
						}
					}
				}
			}
			if (tempGrid != null) {
				shortestPath.push(tempGrid);
			}

			tempDir = getDirFromXToY(tempGrid, currentGrid);
			currentGrid = tempGrid;
			pathLength += 1;
		}

		System.out.println("findShortestPath() -> Generated Path length: "
				+ pathLength);
		return shortestPath;
	}

	// Finds the grid with the best f value
	public Grid minimum(Grid startingGrid, Grid endingGrid,
			Stack<Grid> checkedGrids, double[][] gValues) {

		Grid[][] map = _robotMap.getMapGrids();
		Grid minimumGrid = startingGrid;
		double minimumValue = Double.POSITIVE_INFINITY;

		int startingGridRow = startingGrid.getRow();
		int startingGridCol = startingGrid.getCol();

		int endingGridRow = endingGrid.getRow();
		int endingGridCol = endingGrid.getCol();

		for (int currRow = 1; currRow < MapConstants.MAP_ROWS - 1; currRow++) {
			for (int currCol = 1; currCol < MapConstants.MAP_COLS - 1; currCol++) {

				// hValue determined using the most direct path to the
				// endingGrid
				// 'without obstacles' and 'minimal number of turns'

				double rowDifference = Math.abs(endingGridRow - currRow);
				double colDifference = Math.abs(endingGridCol - currCol);

				rowDifference *= rowDifference;
				colDifference *= colDifference;

				double hValue = Math.sqrt(rowDifference + colDifference);

				if (gValues[currRow][currCol] > 0
						&& (!checkedGrids.contains(map[currRow][currCol]))
						&& testNextMove(currRow, currCol, true)) {

					if ((minimumValue > (gValues[currRow][currCol] + hValue) || ((minimumValue == (gValues[currRow][currCol] + hValue)) && (((currRow == startingGridRow) && (currCol == startingGridCol - 1 || currCol == startingGridCol + 1)) || ((currCol == startingGridCol) && (currRow == startingGridRow - 1 || currRow == startingGridRow + 1)))))) {

						minimumValue = gValues[currRow][currCol] + hValue;
						minimumGrid = map[currRow][currCol];
					}
				}
			}
		}

		return minimumGrid;
	}

	public Grid findReachableGrid(Grid target) {

		Grid[][] map = _robotMap.getMapGrids();
		Grid endGrid = null;
		int endingGridRow = target.getRow();
		int endingGridCol = target.getCol();
		
		// Scenario 1: (Row , Col - RobotConstants.ROBOT_SIZE) reachable
		if ((endingGridRow >= 1 && endingGridCol - RobotConstants.ROBOT_SIZE >= 1)
				&& testNextMove(endingGridRow , endingGridCol - RobotConstants.ROBOT_SIZE, true)) {

			endGrid = map[endingGridRow ][endingGridCol - RobotConstants.ROBOT_SIZE];
		}
		// Scenario 2: (Row - 1, Col - RobotConstants.ROBOT_SIZE) reachable
		else if ((endingGridRow - 1 >= 1 && endingGridCol - RobotConstants.ROBOT_SIZE >= 1)
				&& testNextMove(endingGridRow - 1, endingGridCol - RobotConstants.ROBOT_SIZE, true)) {

			endGrid = map[endingGridRow - 1][endingGridCol - RobotConstants.ROBOT_SIZE];
		}
		// Scenario 3: (Row - 2, Col - RobotConstants.ROBOT_SIZE) reachable
		else if ((endingGridRow - 2 >= 1 && endingGridCol - RobotConstants.ROBOT_SIZE >= 1)
				&& testNextMove(endingGridRow - 2, endingGridCol - RobotConstants.ROBOT_SIZE, true)) {

			endGrid = map[endingGridRow - 2][endingGridCol - RobotConstants.ROBOT_SIZE];
		}
		// Scenario 4: (Row - RobotConstants.ROBOT_SIZE, Col) reachable
		else if ((endingGridRow - RobotConstants.ROBOT_SIZE >= 1 && endingGridCol  >= 1)
				&& testNextMove(endingGridRow - RobotConstants.ROBOT_SIZE, endingGridCol , true)) {

			endGrid = map[endingGridRow - RobotConstants.ROBOT_SIZE][endingGridCol];
		}
		// Scenario 5: (Row - RobotConstants.ROBOT_SIZE, Col - 1) reachable
		else if ((endingGridRow - RobotConstants.ROBOT_SIZE >= 1 && endingGridCol - 1 >= 1)
				&& testNextMove(endingGridRow - RobotConstants.ROBOT_SIZE, endingGridCol - 1, true)) {
			endGrid = map[endingGridRow - RobotConstants.ROBOT_SIZE][endingGridCol - 1];
		}
		// Scenario 6: (Row - RobotConstants.ROBOT_SIZE, Col - 2) reachable
		else if ((endingGridRow - RobotConstants.ROBOT_SIZE >= 1 && endingGridCol - 2 >= 1)
				&& testNextMove(endingGridRow - RobotConstants.ROBOT_SIZE, endingGridCol - 2, true)) {

			endGrid = map[endingGridRow - RobotConstants.ROBOT_SIZE][endingGridCol - 2];
		}
		// Scenario 7: (Row , Col + 1) reachable
		else if ((endingGridRow >= 1 && endingGridCol + 1 <= 20)
				&& testNextMove(endingGridRow , endingGridCol + 1, true)) {

			endGrid = map[endingGridRow ][endingGridCol + 1];
		}
		// Scenario 8: (Row - 1, Col + 1) reachable
		else if ((endingGridRow - 1 >= 1 && endingGridCol + 1 <= 20)
				&& testNextMove(endingGridRow - 1, endingGridCol + 1, true)) {

			endGrid = map[endingGridRow - 1][endingGridCol + 1];
		}
		// Scenario 9: (Row - 2, Col + 1) reachable
		else if ((endingGridRow - 2 >= 1 && endingGridCol + 1 <= 20)
				&& testNextMove(endingGridRow - 2, endingGridCol + 1, true)) {

			endGrid = map[endingGridRow - 2][endingGridCol + 1];
		}
		// Scenario 10: (Row + 1, Col ) reachable
		else if ((endingGridRow + 1 <= 15 && endingGridCol  >= 1)
				&& testNextMove(endingGridRow + 1, endingGridCol , true)) {

			endGrid = map[endingGridRow + 1][endingGridCol ];
		}
		// Scenario 11: (Row + 1, Col - 1) reachable
		else if ((endingGridRow + 1 <= 15 && endingGridCol - 1 >= 1)
				&& testNextMove(endingGridRow + 1, endingGridCol - 1, true)) {
			
			endGrid = map[endingGridRow + 1][endingGridCol - 1];
		}
		// Scenario 12: (Row + 1, Col - 2) reachable
		else if ((endingGridRow + 1 <= 15 && endingGridCol - 2 >= 1)
				&& testNextMove(endingGridRow + 1, endingGridCol - 2, true)) {
			
			endGrid = map[endingGridRow + 1][endingGridCol - 2];
		}
		
		return endGrid;
	}

	// Queue of Instructions to move along the shortest path
	public Queue<INSTRUCTION> generateThePath(Stack<Grid> path) {

		System.out.println("\ngenerateThePath() -> Generating path!");
		Grid nextGrid = null;
		Queue<INSTRUCTION> shortestPath = new ArrayDeque<INSTRUCTION>();

		DIRECTION currDir = this.getRobotDir();
		int currRow = this.getRobotMapPosRow();
		int currCol = this.getRobotMapPosCol();

		// Remove current grid
		path.pop();

		while (!path.isEmpty()) {
			nextGrid = path.pop();

			int nextGridRow = nextGrid.getRow();
			int nextGridCol = nextGrid.getCol();

			// East
			if (nextGridRow == currRow && nextGridCol == (currCol + 1)) {
				switch (currDir) {
				case EAST:
					break;
				case NORTH:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				case SOUTH:
					shortestPath.add(INSTRUCTION.TURN_LEFT);
					break;
				case WEST:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				}

				currDir = DIRECTION.EAST;
			}

			// West
			else if (nextGridRow == currRow && nextGridCol == (currCol - 1)) {
				switch (currDir) {
				case EAST:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				case NORTH:
					shortestPath.add(INSTRUCTION.TURN_LEFT);
					break;
				case SOUTH:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				case WEST:
					break;
				}

				currDir = DIRECTION.WEST;
			}

			// North
			else if (nextGridCol == currCol && nextGridRow == (currRow - 1)) {
				switch (currDir) {
				case EAST:
					shortestPath.add(INSTRUCTION.TURN_LEFT);
					break;
				case NORTH:
					break;
				case SOUTH:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				case WEST:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				}

				currDir = DIRECTION.NORTH;
			}

			// South
			else if (nextGridCol == currCol && nextGridRow == (currRow + 1)) {
				switch (currDir) {
				case EAST:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				case NORTH:
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					shortestPath.add(INSTRUCTION.TURN_RIGHT);
					break;
				case SOUTH:
					break;
				case WEST:
					shortestPath.add(INSTRUCTION.TURN_LEFT);
					break;
				}

				currDir = DIRECTION.SOUTH;
			}

			// Correct Direction now, move straight
			shortestPath.add(INSTRUCTION.MOVE_STRAIGHT);

			// Update the current position
			currRow = nextGridRow;
			currCol = nextGridCol;
		}

		return shortestPath;
	}

	/** LiangLiang's part ends here ************************************* */

	private boolean coverageLimitReached() {

		Grid[][] map = _robotMap.getMapGrids();
		int noOfExploredGrids = 0;

		for (int i = 1; i < MapConstants.MAP_ROWS - 1; i++) {
			for (int j = 1; j < MapConstants.MAP_COLS - 1; j++) {
				if (map[i][j].isExplored())
					noOfExploredGrids++;
			}
		}

		return noOfExploredGrids >= _explorationTarget;
	}

	private Stack<Grid> getUnexploredGrids() {
		Stack<Grid> unexploredGrids = new Stack<Grid>();

		Grid[][] map = _robotMap.getMapGrids();
		for (int i = 1; i < MapConstants.MAP_ROWS - 1; i++) {
			for (int j = 1; j < MapConstants.MAP_COLS - 1; j++) {
				if (!map[i][j].isExplored()) {
					unexploredGrids.push(map[i][j]);
				}
			}
		}

		if (!unexploredGrids.isEmpty()) {
			Collections.reverse(unexploredGrids);
		}

		return unexploredGrids;
	}

	public static enum INSTRUCTION {
		MOVE_STRAIGHT, TURN_RIGHT, TURN_LEFT;
	};

	/**
	 * Simulate the robot's next move
	 * <p>
	 * This is used to prevent the robot from moving out of the arena,<br>
	 * or moving straight into known obstacles
	 * 
	 * @param newRobotPosRow
	 *            The new row
	 * @param newRobotPosCol
	 *            The new column
	 * @param bUnexploredAsObstacle
	 *            True if unexplored areas will be treated as obstacles
	 * 
	 * @return True if the next move is valid
	 */
	private boolean testNextMove(int newRobotPosRow, int newRobotPosCol,
			boolean bUnexploredAsObstacle) {

		// Ensures the robot's next move will NOT cause it to go out of bounds
		// or collide with any known obstacles or go into unexplored area
		return withinArena(newRobotPosRow, newRobotPosCol)
				&& checkForObstacles(newRobotPosRow, newRobotPosCol,
						bUnexploredAsObstacle);
	}

	/**
	 * Function for validating the robot's next move<br>
	 * Ensures that the robot does not collide with any known obstacles
	 * 
	 * @return true if the robot will be safe
	 */
	private boolean checkForObstacles(int robotPosRow, int robotPosCol,
			boolean bUnexploredAsObstacle) {

		// Check for obstacles within robot's new position
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		for (int mapRow = robotPosRow; mapRow < robotPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = robotPosCol; mapCol < robotPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {

				if (bUnexploredAsObstacle) {
					if (!robotMapGrids[mapRow][mapCol].isExplored())
						return false;
				}

				if (robotMapGrids[mapRow][mapCol].isExplored()
						&& robotMapGrids[mapRow][mapCol].isObstacle())
					return false;
			}
		}
		return true;
	}

	private DIRECTION getDirFromXToY(Grid x, Grid y) {
		if (y.getRow() == x.getRow()) {
			if (y.getCol() == x.getCol() + 1) {
				return DIRECTION.EAST;
			} else if (y.getCol() == x.getCol() - 1) {
				return DIRECTION.WEST;
			}
		} else if (y.getCol() == x.getCol()) {
			if (y.getRow() == x.getRow() + 1) {
				return DIRECTION.SOUTH;
			} else if (y.getRow() == x.getRow() - 1) {
				return DIRECTION.NORTH;
			}
		}

		// Shouldn't happen unless Grid x == Grid y
		return null;
	}

	// Translate integer to direction
	public Stack<DIRECTION> transI2D(int num) {
		Stack<DIRECTION> dir = new Stack<DIRECTION>();
		switch (num) {
		case 0:
			dir = null;
			break;
		case 1:
			dir.push(DIRECTION.EAST);
			break;
		case 2:
			dir.push(DIRECTION.WEST);
			break;
		case 3:
			dir.push(DIRECTION.EAST);
			dir.push(DIRECTION.WEST);
			break;
		case 4:
			dir.push(DIRECTION.SOUTH);
			break;
		case 5:
			dir.push(DIRECTION.EAST);
			dir.push(DIRECTION.SOUTH);
			break;
		case 6:
			dir.push(DIRECTION.WEST);
			dir.push(DIRECTION.SOUTH);
			break;
		case 8:
			dir.push(DIRECTION.NORTH);
			break;
		case 9:
			dir.push(DIRECTION.EAST);
			dir.push(DIRECTION.NORTH);
			break;
		case 10:
			dir.push(DIRECTION.WEST);
			dir.push(DIRECTION.NORTH);
			break;
		case 12:
			dir.push(DIRECTION.SOUTH);
			dir.push(DIRECTION.NORTH);
			break;
		}
		return dir;
	}

	/** Wifi connection related functions starts here ********************** */

	/**
	 * For starting exploration
	 */
	public void startPhysicalExploration() {

		System.out.println("\nStarting physical exploration!");

		// Calculate timer intervals based on the user selected steps per second
		_timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
		System.out.println("Steps Per Second: " + _stepsPerSecond
				+ ", Timer Interval: " + _timerIntervals);

		// Calculate number of explored grids required
		_explorationTarget = (int) ((_coverageLimit / 100.0) * ((MapConstants.MAP_ROWS - 2) * (MapConstants.MAP_COLS - 2)));
		System.out.println("Exploration target (In grids): "
				+ _explorationTarget);

		// Reset the elapsed exploration time (in milliseconds)
		_elapsedExplorationTime = 0;

		// Reset all variables used
		_phyExploreTimer = null;
		_bPhyExConnected = false;
		_phyExErrors = 0;
		_phyExRcvMsg = null;
		_bPhyExStarted = false;
		_movesSinceLastCalibration = 0;

		_phyExploreTimer = new Timer(_timerIntervals, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (!_bPhyExConnected) {
					CommMgr mgr = CommMgr.getCommMgr();
					_bPhyExConnected = mgr.setConnection(_timerIntervals - 20);
					if (_bPhyExConnected) {
						System.out.println("CONNECTED!!");
					}

					if (!_bPhyExConnected) {
						_phyExErrors++;

						if (_phyExErrors >= 30) {
							System.out
									.println("Too many errors, stopped reconnection!");
							mgr.closeConnection();

							if (_phyExploreTimer != null) {
								_phyExploreTimer.stop();
								_phyExploreTimer = null;
							}
						}
					}
					return;
				} else {
					if (_phyExploreTimer != null && _bExplorationComplete) {
						_phyExploreTimer.stop();
						_phyExploreTimer = null;
					} else {

						if (_bPhyExStarted) {
							// Make the next move
							makeNextPhysicalMove();

							// Update elapsed time
							_elapsedExplorationTime += _timerIntervals;
							
						} else {
							// Try to get message
							_phyExRcvMsg = CommMgr.getCommMgr().recvMsg();

							if (_phyExRcvMsg != null
									&& _phyExRcvMsg.equals(START_PHY_EXPLORE)) {
								_bPhyExStarted = true;

								System.out.println("_bPhyExStarted is TRUE!");

								// Send out first message to Arduino to
								// do initial calibration and get sensor reading
								String outputMsg = "l;l;c;o;c;o;m;";
								CommMgr.getCommMgr().sendMsg(outputMsg,
										CommMgr.MSG_TYPE_ARDUINO, false);
							}
						}
					}
				}
			}
		});
		_phyExploreTimer.setRepeats(true);
		_phyExploreTimer.setInitialDelay(0);
		_phyExploreTimer.start();
	}

	/** Function for stopping physical exploration */
	public void stopPhysicalExploration() {

		if (_phyExploreTimer != null) {
			_phyExploreTimer.stop();
			_phyExploreTimer = null;
		}

		System.out.println(CommMgr.getCommMgr().recvMsg());

		// Reset all variables
		_phyExploreTimer = null;
		_bPhyExConnected = false;
		_phyExErrors = 0;
		_phyExRcvMsg = null;
		_bPhyExStarted = false;
		_movesSinceLastCalibration = 0;

		System.out.println("Stopping physical exploration!!");
	}

	/** For physically exploring any unexplored area */
	public void startPhyExploringUnexplored(Grid current, DIRECTION currDir,
			Grid target, Grid[][] robotMap) {

		Stack<Grid> exploreUnexploredPath = findShortestPath(current, target,
				currDir, robotMap);

		if (exploreUnexploredPath == null) {

			System.out.println("startPhyExploringUnexplored()"
					+ " -> exploreUnexploredPath is NULL");

			if (_unexploredGrids.isEmpty()) {
				// Start the shortest path back to the starting grid
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
				Grid startingGrid = robotMap[1][1];

				if (!withinStartZone(_robotMapPosRow, _robotMapPosCol)) {
					System.out.println("I need to go back to the start");
					startPhysicalShortestPath(currentGrid, _robotDirection,
							startingGrid, robotMap);
				}
				else {
					// Within start zone, perform end of exploration calibration
					endOfExplorationCalibration();
				}
			} else {
				startPhyExploringUnexplored(current, currDir,
						_unexploredGrids.pop(), robotMap);
			}
		} else {

			_exploreUnexploredInstructions = generateThePath(exploreUnexploredPath);

			//requestSensorReadings();

			// Calculate timer intervals based on the user
			// selected steps per second
			_timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);

			_exploreUnexploredTimer = new Timer(_timerIntervals,
					new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {

							if (_exploreUnexploredInstructions.isEmpty()) {
								_exploreUnexploredTimer.stop();
								_exploreUnexploredTimer = null;

								// Check to see if there are
								// anymore unexplored grids
								_unexploredGrids = getUnexploredGrids();
								if (!_unexploredGrids.isEmpty()) {

									// Start shortest path to the next
									// unexplored grid
									Grid[][] robotMap = _robotMap.getMapGrids();
									Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];

									startPhyExploringUnexplored(currentGrid,
											_robotDirection,
											_unexploredGrids.pop(), robotMap);
								} else {

									// Start the shortest path back to the
									// starting grid
									Grid[][] robotMap = _robotMap.getMapGrids();
									Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
									Grid startingGrid = robotMap[1][1];

									if (currentGrid != startingGrid) {
										startPhysicalShortestPath(currentGrid,
												_robotDirection, startingGrid,
												robotMap);
									}
									else {
										// Within start zone, perform end of
										// exploration calibration
										endOfExplorationCalibration();
									}
								}

							} else {

								// Try to get message
								_phyExRcvMsg = CommMgr.getCommMgr().recvMsg();

								if (_phyExRcvMsg != null) {

									// Sense its surroundings using actual
									// sensor readings
									Robot.this.physicalSense(_phyExRcvMsg);

									_robotMap.revalidate();
									_robotMap.repaint();

									// Perform next instruction
									switch (_exploreUnexploredInstructions
											.poll()) {
									case MOVE_STRAIGHT:
										moveStraight();
										break;
									case TURN_LEFT:
										turnLeft();
										break;
									case TURN_RIGHT:
										turnRight();
										break;
									}

									if (_phyExCmdMsg != null) {
										String outputMsg = _phyExCmdMsg;
										
										CommMgr.getCommMgr()
												.sendMsg(
														outputMsg,
														CommMgr.MSG_TYPE_ARDUINO,
														false);
										_phyExCmdMsg = null;
									}
								}
							}
						}
					});
			_exploreUnexploredTimer.setRepeats(true);
			_exploreUnexploredTimer.setInitialDelay(0);
			_exploreUnexploredTimer.start();
		}
	}

	/** For triggering the leaderboard shortest path algorithm */
	public void startPhysicalShortestPath() {

		Grid[][] robotMap = _robotMap.getMapGrids();
		Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];

		int goalGridRow = MapConstants.GOAL_GRID_ROW; // Row 13
		int goalGridCol = MapConstants.GOAL_GRID_COL; // Column 18
		Grid goalGrid = robotMap[goalGridRow][goalGridCol];

		System.out.println("\nstartPhysicalSP(void)"
				+ " -> Starting row, col: " + _robotMapPosRow + ", "
				+ _robotMapPosCol + ", Goal row, col: " + goalGridRow + ", "
				+ goalGridCol + "\n");

		startPhysicalShortestPath(currentGrid, _robotDirection, goalGrid,
				robotMap);
	}

	/** For starting the leaderboard shortest path */
	private void startPhysicalShortestPath(final Grid current, final DIRECTION currDir,
			final Grid target, final Grid[][] robotMap) {

		Stack<Grid> shortestPath = findShortestPath(current, target, currDir,
				robotMap);

		if (shortestPath == null) {

			System.out.println("startPhysicalShortestPath()"
					+ " -> shortestPath is NULL");
			return;
		}

		_shortestPathInstructions = generateThePath(shortestPath);

		// Calculate timer intervals based on the user selected steps per second
		_timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
		System.out.println("Steps Per Second: " + _stepsPerSecond
				+ ", Timer Interval: " + _timerIntervals);

		// Reset all variables used
		_phySpTimer = null;
		_bPhySpConnected = false;
		_phySpErrors = 0;
		_phySpRcvMsg = null;
		_bPhySpStarted = false;
		_phySpCmdMsg = null;
		
		// Create the shortest path command string
		DIRECTION endingDir = currDir;
		_phySpCmdMsg = (_bPhyExStarted ? "s;" : "s;");
		
		ArrayDeque<INSTRUCTION> setOfInst = new ArrayDeque<INSTRUCTION>();
		if(!_shortestPathInstructions.isEmpty())
			setOfInst.add(_shortestPathInstructions.poll());
		
		while (!_shortestPathInstructions.isEmpty()) {
			
			INSTRUCTION currInst = _shortestPathInstructions.poll();
			if(currInst.toString().equals(setOfInst.peekLast().toString())) {
				setOfInst.add(currInst);
			}
			else {
				INSTRUCTION nextInst = setOfInst.peekLast();
				boolean bIsDirection = false;
				switch (nextInst) {
				case MOVE_STRAIGHT:
					_phySpCmdMsg += "f";
					break;
				case TURN_LEFT:
					_phySpCmdMsg += "l";
					endingDir = DIRECTION.getPrevious(endingDir);
					bIsDirection = true;
					break;
				case TURN_RIGHT:
					_phySpCmdMsg += "o";
					endingDir = DIRECTION.getNext(endingDir);
					bIsDirection = true;
					break;
				}
				int size = setOfInst.size();
				
				if(!bIsDirection) {
					_phySpCmdMsg += ((size > 1) ? (size + ";") : ";");
				}
				else {
					_phySpCmdMsg += ((size > 1) ? ((size*90) + ";") : ";");
				}
				setOfInst.clear();
				setOfInst.add(currInst);
			}
		}
		
		if(!setOfInst.isEmpty()) {
			INSTRUCTION nextInst = setOfInst.peekLast();
			boolean bIsDirection = false;
			switch (nextInst) {
			case MOVE_STRAIGHT:
				_phySpCmdMsg += "f";
				break;
			case TURN_LEFT:
				_phySpCmdMsg += "l";
				endingDir = DIRECTION.getPrevious(endingDir);
				bIsDirection = true;
				break;
			case TURN_RIGHT:
				_phySpCmdMsg += "o";
				endingDir = DIRECTION.getNext(endingDir);
				bIsDirection = true;
				break;
			}
			int size = setOfInst.size();
			if(!bIsDirection) {
				_phySpCmdMsg += ((size > 1) ? (size + ";") : ";");
			}
			else {
				_phySpCmdMsg += ((size > 1) ? ((size*90) + ";") : ";");
			}
			setOfInst.clear();
		}
		
		System.out.println("Check physyical sp: Out of sp inst generation loop!!");
		
		
		// If target grid is within the start zone, i.e.
		// shortestPath is being used to go back to the start zone
		if(_robotMap.isStartZone(target.getRow(), target.getCol())) {
			System.out.println("startPhysicalSP() -> "
					+ "Current Ending Direction: " + endingDir);
			
			// If the robot is not facing the specified starting direction
			if (endingDir != _robotStartDir) {

				// Turn the robot to match the specified starting direction
				while (endingDir != _robotStartDir) {
					endingDir = DIRECTION.getNext(endingDir);
					_phySpCmdMsg += "o;";
				}

				_phySpCmdMsg += "l;c;o;";
			}
			
			System.out.println("startPhysicalSP() -> "
					+ "Final Ending Direction: " + endingDir);
		}
		
		System.out.println("startPhysicalSP() -> Command string to be sent: "
				+ _phySpCmdMsg);

		_phySpTimer = new Timer(_timerIntervals, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (!_bPhySpConnected && !_bPhyExConnected) {
					CommMgr mgr = CommMgr.getCommMgr();
					
					_bPhySpConnected = mgr.isConnected();
					if(!_bPhySpConnected) {
						_bPhySpConnected = mgr.setConnection(_timerIntervals - 20);
					}
					
					if (_bPhySpConnected) {
						System.out.println("startPhysicalSP()"
								+ " -> CONNECTED!!");
					}

					if (!_bPhySpConnected) {
						_phySpErrors++;

						if (_phySpErrors >= 30) {
							System.out.println("Too many errors,"
									+ " stopped reconnection!");
							mgr.closeConnection();

							if (_phySpTimer != null) {
								_phySpTimer.stop();
								_phySpTimer = null;
							}
						}
					}
					return;
				}

				if (!_bPhySpStarted && !_bPhyExStarted) {
					// Try to get message
					_phySpRcvMsg = CommMgr.getCommMgr().recvMsg();

					if (_phySpRcvMsg == null) {
						_phySpErrors++;
						return;
					} else {
						if (!_bPhySpStarted
								&& _phySpRcvMsg.equals(START_PHY_SP)) {

							_bPhySpStarted = true;
							System.out.println("Rcv Msg: " + _phySpRcvMsg);
							System.out.println("_bPhySpStarted is TRUE!");
							
							//requestSensorReadings();
						}
					}
				}

				System.out.println("startPhysicalSP() -> _bPhySpStarted = "
						+ (_bPhySpStarted ? "True" : "False") +
						" _bPhyExStarted = " + (_bPhyExStarted ? "True" : "False"));
				
				if (_bPhySpStarted || _bPhyExStarted) {

					CommMgr.getCommMgr().sendMsg(_phySpCmdMsg,
							CommMgr.MSG_TYPE_ARDUINO, false);
					
					// For the simulator to execute the shortest path as well
					startShortestPath(current, currDir, target, robotMap);
					
					// Timer can be stopped once shortest path command
					// has been sent out
					if (_phySpTimer != null) {
						_phySpTimer.stop();
						_phySpTimer = null;
					}
				}
			}
		});
		_phySpTimer.setRepeats(true);
		_phySpTimer.setInitialDelay(0);
		_phySpTimer.start();
	}

	/**
	 * Instructs the robot to make the next physical move
	 * <p>
	 * 
	 * Consists of 2 parts:<br>
	 * 1. Sense - Use sensors to update robot map information<br>
	 * 2. Logic - Decide on the next move to make
	 * 
	 */
	public void makeNextPhysicalMove() {

		// Try to get message
		_phyExRcvMsg = CommMgr.getCommMgr().recvMsg();

		if (_phyExRcvMsg != null) {

			// Sense its surroundings using actual sensor readings
			this.physicalSense(_phyExRcvMsg);

			_robotMap.revalidate();
			_robotMap.repaint();

			// Logic to make the next move
			this.physicalLogic();
		}
	}

	/**
	 * This should update the robot's map based on actual sensor information
	 * 
	 * @param sensorStr
	 *            The string containing all sensor readings<br>
	 *            (e.g. "3,5;5;0;0;5;5;")
	 */
	private void physicalSense(String sensorStr) {

		sensorStr = sensorStr.substring(2, sensorStr.length());
		String[] sensorReadings = sensorStr.split(";");
		int sensorIndex = 0;
		
		// Weightage of the sensors
		double[] sensorWeightage = {3.0, 3.0, 3.0, 1.5, 1.0, 1.0};

		for (Sensor s : _sensors) {

			int freeGrids = 0;
			try {
				freeGrids = Integer.parseInt(sensorReadings[sensorIndex]);
				
				// Not using the reading of the 'left-back' sensor
				if(sensorIndex == 4) {
					sensorIndex++;
					continue;
				}
				
				sensorIndex++;
			} catch (NumberFormatException e) {
				return;
			}

			int sensorPosRow = s.getSensorPosRow();
			int sensorPosCol = s.getSensorPosCol();
			DIRECTION sensorDir = s.getSensorDirection();
			int sensorMinRange = s.getMinRange();
			int sensorMaxRange = s.getMaxRange();

			/*
			 * System.out.println("Sensor - " + sensorPosRow + ", " +
			 * sensorPosCol + ", " + sensorDir.toString() + ", Free Grids: " +
			 * freeGrids);
			 */

			Grid[][] robotMapGrids = _robotMap.getMapGrids();
			for (int currGrid = sensorMinRange; currGrid <= sensorMaxRange; currGrid++) {

				int gridRow = sensorPosRow
						+ ((sensorDir == DIRECTION.NORTH) ? (-1 * currGrid)
								: (sensorDir == DIRECTION.SOUTH) ? currGrid : 0);

				int gridCol = sensorPosCol
						+ ((sensorDir == DIRECTION.WEST) ? (-1 * currGrid)
								: (sensorDir == DIRECTION.EAST) ? currGrid : 0);
				
				// Calculate the truth value to be used for the current reading
				double truthValue = 1.0/((double) currGrid);
				truthValue *= sensorWeightage[sensorIndex-1];
				
				// Do a check here to prevent index out of bounds
				if(gridRow < 0 || gridRow >= MapConstants.MAP_ROWS ||
						gridCol < 0 || gridCol >= MapConstants.MAP_COLS) {
					continue;
				}

				try {
					// If the current grid is within number of free grids
					// detected
					if (currGrid <= freeGrids) {
						
						// NEVER allow the robot to mark border walls as free grids
						if(!_robotMap.isBorderWalls(gridRow, gridCol)) {
							
							robotMapGrids[gridRow][gridCol].markAsFreeGrid(
									truthValue);
						}
					} else {

						// Current grid is less than or equal to max sensor
						// range, but greater than number of free grids
						// i.e. current grid is an obstacle
						
						// Not a visited grid, not start zone, and not goal zone
						if (!robotMapGrids[gridRow][gridCol].isVisited()) {
							if (!_robotMap.isStartZone(gridRow, gridCol)
									&& !_robotMap.isGoalZone(gridRow, gridCol)) {
								
								robotMapGrids[gridRow][gridCol]
										.markAsObstacle(truthValue);
							}
						}

						break;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println("physicalSense()->"
							+ "ArrayIndexOutOfBoundsException");
				} catch (Exception e) {
					System.out.println("physicalSense()->"
							+ "Exception");
				}
			}
		}
	}

	/**
	 * This is where all the logic will go
	 * <p>
	 * NOTE: Call this after invoking {@link #physicalSense}
	 */
	public void physicalLogic() {

		// Exploration complete, do nothing
		if (_bExplorationComplete)
			return;

		// Robot reached goal zone
		if (withinGoalZone(_robotMapPosRow, _robotMapPosCol))
			_bReachedGoal = true;

		if (_bCoverageLimited) {
			if (coverageLimitReached()) {

				// Stop exploration
				_bExplorationComplete = true;

				// Start the shortest path back to the starting grid
				Grid[][] robotMap = _robotMap.getMapGrids();
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
				Grid startingGrid = robotMap[1][1];

				startPhysicalShortestPath(currentGrid, _robotDirection,
						startingGrid, robotMap);

				return;
			}
		}

		if (_bTimeLimited) {
			if ((_elapsedExplorationTime / 1000) >= _timeLimit) {
				
				// Stop exploration
				_bExplorationComplete = true;

				// Start the shortest path back to the starting grid
				Grid[][] robotMap = _robotMap.getMapGrids();
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];
				Grid startingGrid = robotMap[1][1];

				startPhysicalShortestPath(currentGrid, _robotDirection,
						startingGrid, robotMap);

				return;
			}
		}

		if (_bReachedGoal && withinStartZone(_robotMapPosRow, _robotMapPosCol)) {

			_bExplorationComplete = true;

			/*
			_unexploredGrids = getUnexploredGrids();
			if (!_unexploredGrids.isEmpty()) {

				// Start shortest path to the first unexplored grid
				Grid[][] robotMap = _robotMap.getMapGrids();
				Grid currentGrid = robotMap[_robotMapPosRow][_robotMapPosCol];

				startPhyExploringUnexplored(currentGrid, _robotDirection,
						_unexploredGrids.pop(), robotMap);
			}
			else {
				// No unexplored grids to be explored
				endOfExplorationCalibration();
			}*/
			
			endOfExplorationCalibration();

			return;
		}
		
		int currRobotMapPosRow = _robotMapPosRow;
		int currRobotMapPosCol = _robotMapPosCol;
		DIRECTION currRobotDir = _robotDirection;

		boolean frontWall = hasFrontWall();
		boolean leftWall = hasLeftWall();
		boolean rightWall = hasRightWall();
		
		System.out.println("physicalLogic() -> " + 
				"FrontWall: " + (frontWall ? "True" : "False") +
				" LeftWall: " + (leftWall ? "True" : "False") +
				" RightWall: " + (rightWall ? "True" : "False")
				+ " PrevLeftWall: " + (_bPreviousLeftWall ? "True" : "False"));

		// (No leftWall AND previousLeftWall) OR
		// (frontWall AND No leftWall AND rightWall)
		if ( (!leftWall && _bPreviousLeftWall) ||
				(frontWall && !leftWall && rightWall) ) {
			turnLeft();
		}

		// (frontWall AND No rightWall)
		else if (frontWall && !rightWall) {
			turnRight();
		}

		// (frontWall AND leftWall AND rightWall)
		else if (frontWall && leftWall && rightWall) {
			turn180();
		}

		else {
			moveStraight();
		}
		
		// Save current leftWall state into _bPreviousLeftWall
		_bPreviousLeftWall = leftWall;
		
		// Increment number of moves made since last calibration
		_movesSinceLastCalibration++;
		
		if (_phyExCmdMsg != null) {
			String outputMsg = _phyExCmdMsg;
			
			// If calibration is required
			if(_movesSinceLastCalibration >= MAX_MOVES_BEFORE_CALIBRATION) {
				
				boolean bFrontCalibration = checkCalibrateFront(
						currRobotMapPosRow, currRobotMapPosCol, currRobotDir);
				boolean bLeftCalibration = checkCalibrateLeft(
						currRobotMapPosRow, currRobotMapPosCol, currRobotDir);
				boolean bRightCalibration = checkCalibrateRight(
						currRobotMapPosRow, currRobotMapPosCol, currRobotDir);
				
				if(bFrontCalibration && bLeftCalibration) {
					// In a corner with complete walls in front and on the left
					// Turn left, calibrate, turn right, calibrate
					outputMsg = "l;c;o;c;" + outputMsg;
					_movesSinceLastCalibration = 0;
				}
				else if(bFrontCalibration && bRightCalibration) {
					// In a corner with complete walls in front and on the right
					// Calibrate, turn right, calibrate, turn left, calibrate
					outputMsg = "c;o;c;l;c;" + outputMsg;
					_movesSinceLastCalibration = 0;
				}
				else {
					if(bFrontCalibration) {
						// Just calibrate
						outputMsg = "c;" + outputMsg;
						_movesSinceLastCalibration = 0;
					}
					else if(bLeftCalibration) {
						// Turn left, calibrate, turn right
						outputMsg = "l;c;o;" + outputMsg;
						_movesSinceLastCalibration = 0;
					}
				}
			}
			else {
				
				// Special condition for mandatory re-calibration
				boolean bFrontCalibration = checkCalibrateFront(
						currRobotMapPosRow, currRobotMapPosCol, currRobotDir);
				boolean bLeftCalibration = checkCalibrateLeft(
						currRobotMapPosRow, currRobotMapPosCol, currRobotDir);
				boolean bRightCalibration = checkCalibrateRight(
						currRobotMapPosRow, currRobotMapPosCol, currRobotDir);
				
				/*String status = "";
				status += bFrontCalibration ? "Front! " : "";
				status += bLeftCalibration ? "Left! " : "";
				status += bRightCalibration ? "Right!" : "";
				System.out.println("physicalLogic() -> Status: " + status);*/
				
				if(bFrontCalibration && bLeftCalibration) {
					// In a corner with complete walls in front and on the left
					// Turn left, calibrate, turn right, calibrate
					outputMsg = "l;c;o;c;" + outputMsg;
					_movesSinceLastCalibration = 0;
					System.out.println("Calibrate front&left!");
				}
				else if(bFrontCalibration && bRightCalibration) {
					// In a corner with complete walls in front and on the right
					// Calibrate, turn right, calibrate, turn left, calibrate
					outputMsg = "c;o;c;l;c;" + outputMsg;
					_movesSinceLastCalibration = 0;
					System.out.println("Calibrate front&right!");
				}
			}
			
			CommMgr.getCommMgr().sendMsg(outputMsg, CommMgr.MSG_TYPE_ARDUINO,
					false);
			_phyExCmdMsg = null;
		}
	}
	
	/**
	 * Check if the robot can do front calibration
	 * 
	 * @return True if there is a COMPLETE wall in front of the robot
	 */
	public boolean checkCalibrateFront(int robotMapPosRow, int robotMapPosCol,
			DIRECTION robotDir) {

		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		int frontWallRow, frontWallCol;	
		int numObstacles = 0;
		
		switch (robotDir) {
		case EAST:
			frontWallRow = robotMapPosRow;
			frontWallCol = robotMapPosCol + RobotConstants.ROBOT_SIZE;
			for (int currRow = frontWallRow; currRow < frontWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][frontWallCol].isExplored()
						&& robotMapGrids[currRow][frontWallCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(currRow, frontWallCol)) {
					numObstacles++;
				}
			}
			break;
		case NORTH:
			frontWallRow = (robotMapPosRow - 1);
			frontWallCol = robotMapPosCol;
			for (int currCol = frontWallCol; currCol < frontWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[frontWallRow][currCol].isExplored()
						&& robotMapGrids[frontWallRow][currCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(frontWallRow, currCol)) {
					numObstacles++;
				}
			}
			break;
		case SOUTH:
			frontWallRow = robotMapPosRow + RobotConstants.ROBOT_SIZE;
			frontWallCol = robotMapPosCol;
			for (int currCol = frontWallCol; currCol < frontWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[frontWallRow][currCol].isExplored()
						&& robotMapGrids[frontWallRow][currCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(frontWallRow, currCol)) {
					numObstacles++;
				}
			}
			break;
		case WEST:
			frontWallRow = robotMapPosRow;
			frontWallCol = (robotMapPosCol - 1);
			for (int currRow = frontWallRow; currRow < frontWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][frontWallCol].isExplored()
						&& robotMapGrids[currRow][frontWallCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(currRow, frontWallCol)) {
					numObstacles++;
				}
			}
			break;
		}
		
		return (numObstacles == RobotConstants.ROBOT_SIZE);
	}

	/**
	 * Check if the robot can do left calibration
	 * 
	 * @return True if there is a COMPLETE wall on the left of the robot
	 */
	public boolean checkCalibrateLeft(int robotMapPosRow, int robotMapPosCol,
			DIRECTION robotDir) {
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		int leftWallRow, leftWallCol;
		int numObstacles = 0;

		switch (robotDir) {
		case EAST:
			leftWallRow = robotMapPosRow - 1;
			leftWallCol = robotMapPosCol;
			for (int currCol = leftWallCol; currCol < leftWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[leftWallRow][currCol].isExplored()
						&& robotMapGrids[leftWallRow][currCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(leftWallRow, currCol)) {
					numObstacles++;
				}
			}
			break;
		case NORTH:
			leftWallRow = robotMapPosRow;
			leftWallCol = robotMapPosCol - 1;
			for (int currRow = leftWallRow; currRow < leftWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][leftWallCol].isExplored()
						&& robotMapGrids[currRow][leftWallCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(currRow, leftWallCol)) {
					numObstacles++;
				}
			}
			break;
		case SOUTH:
			leftWallRow = robotMapPosRow;
			leftWallCol = (robotMapPosCol + RobotConstants.ROBOT_SIZE);
			for (int currRow = leftWallRow; currRow < leftWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][leftWallCol].isExplored()
						&& robotMapGrids[currRow][leftWallCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(currRow, leftWallCol)) {
					numObstacles++;
				}
			}
			break;
		case WEST:
			leftWallRow = (robotMapPosRow + RobotConstants.ROBOT_SIZE);
			leftWallCol = robotMapPosCol;
			for (int currCol = leftWallCol; currCol < leftWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[leftWallRow][currCol].isExplored()
						&& robotMapGrids[leftWallRow][currCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(leftWallRow, currCol)) {
					numObstacles++;
				}
			}
			break;
		}

		return (numObstacles == RobotConstants.ROBOT_SIZE);
	}
	
	/**
	 * Check if the robot can do right wall calibration
	 * 
	 * @return True if there is a COMPLETE wall on the right of the robot
	 */
	public boolean checkCalibrateRight(int robotMapPosRow, int robotMapPosCol,
			DIRECTION robotDir) {
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		int rightWallRow, rightWallCol;
		int numObstacles = 0;

		switch (robotDir) {
		case EAST:
			rightWallRow = robotMapPosRow + RobotConstants.ROBOT_SIZE;
			rightWallCol = robotMapPosCol;
			for (int currCol = rightWallCol; currCol < rightWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[rightWallRow][currCol].isExplored()
						&& robotMapGrids[rightWallRow][currCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(rightWallRow, currCol)) {
					numObstacles++;
				}
			}
			break;
		case NORTH:
			rightWallRow = robotMapPosRow;
			rightWallCol = robotMapPosCol + RobotConstants.ROBOT_SIZE;
			for (int currRow = rightWallRow; currRow < rightWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][rightWallCol].isExplored()
						&& robotMapGrids[currRow][rightWallCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(currRow, rightWallCol)) {
					numObstacles++;
				}
			}
			break;
		case SOUTH:
			rightWallRow = robotMapPosRow;
			rightWallCol = (robotMapPosCol - 1);
			for (int currRow = rightWallRow; currRow < rightWallRow
					+ RobotConstants.ROBOT_SIZE; currRow++) {
				if (robotMapGrids[currRow][rightWallCol].isExplored()
						&& robotMapGrids[currRow][rightWallCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(currRow, rightWallCol)) {
					numObstacles++;
				}
			}
			break;
		case WEST:
			rightWallRow = (robotMapPosRow - 1);
			rightWallCol = robotMapPosCol;
			for (int currCol = rightWallCol; currCol < rightWallCol
					+ RobotConstants.ROBOT_SIZE; currCol++) {
				if (robotMapGrids[rightWallRow][currCol].isExplored()
						&& robotMapGrids[rightWallRow][currCol].isObstacle())
					numObstacles++;
				else if(_robotMap.isBorderWalls(rightWallRow, currCol)) {
					numObstacles++;
				}
			}
			break;
		}

		return (numObstacles == RobotConstants.ROBOT_SIZE);
	}
	
	/**
	 * Mark the robot's current position as visited
	 */
	private void markCurrentPosAsVisited() {
		Grid[][] robotMapGrids = _robotMap.getMapGrids();
		
		for (int mapRow = _robotMapPosRow; mapRow < _robotMapPosRow
				+ RobotConstants.ROBOT_SIZE; mapRow++) {
			for (int mapCol = _robotMapPosCol; mapCol < _robotMapPosCol
					+ RobotConstants.ROBOT_SIZE; mapCol++) {

				robotMapGrids[mapRow][mapCol].markAsVisited();
			}
		}
	}
	
	/**
	 * Perform end of exploration calibration
	 */
	private void endOfExplorationCalibration() {
		boolean bFrontCalibration = checkCalibrateFront(
				_robotMapPosRow, _robotMapPosCol, _robotDirection);
		boolean bLeftCalibration = checkCalibrateLeft(
				_robotMapPosRow, _robotMapPosCol, _robotDirection);
		
		// Start message with s; to turn off timer, and turn off exploration
		// mode for Arduino
		String outputMsg = "s;";
		
		if(bFrontCalibration && bLeftCalibration) {
			// In a corner with complete walls in front and on the left
			// Turn left, calibrate, turn right, calibrate
			outputMsg += "l;c;o;c;";
		}
		else {
			if(bFrontCalibration) {
				// Just calibrate
				outputMsg += "c;";
			}
			else if(bLeftCalibration) {
				// Turn left, calibrate, turn right
				outputMsg += "l;c;o;";
			}
		}
		
		// Reset back to the starting state to prepare for the shortest path
		if(outputMsg != null) {
			
			// If the robot is not facing the specified starting direction
			if(_robotDirection != _robotStartDir) {
			
				// Turn the robot to match the specified starting direction
				while (_robotDirection != _robotStartDir) {
					this.turnRight();
					outputMsg += "o;";
					
					_robotMap.revalidate();
					_robotMap.repaint();
				}
				
				outputMsg += "l;c;o;";
			}
		}
		
		if(outputMsg != null) {
			CommMgr.getCommMgr().sendMsg(outputMsg,
					CommMgr.MSG_TYPE_ARDUINO, false);
		}
	}

	private void requestSensorReadings() {
		CommMgr.getCommMgr().sendMsg("m;", CommMgr.MSG_TYPE_ARDUINO, false);
	}
	
	public void performEmergencyRecalibration() {
		
		requestSensorReadings();
		
		while(true) {
			
			// Try to get message
			_phyExRcvMsg = CommMgr.getCommMgr().recvMsg();
			
			if(_phyExRcvMsg != null) {
				this.physicalSense(_phyExRcvMsg);
				
				boolean frontWall = hasFrontWall();
				boolean leftWall = hasLeftWall();
				boolean rightWall = hasRightWall();
				
				System.out.println("physicalLogic() -> " + 
						"FrontWall: " + (frontWall ? "True" : "False") +
						" LeftWall: " + (leftWall ? "True" : "False") +
						" RightWall: " + (rightWall? "True" : "False"));
				
				// Ensure facing North, in order to have front wall &
				// left wall for calibration
				if(!(frontWall && leftWall && !rightWall)) {
					turnLeft();
				}
				else {
					
					// Added this to ensure almightyRobot and the
					// physical robot has the same direction
					_robotDirection = DIRECTION.NORTH;
					
					endOfExplorationCalibration();
					return;
				}
				
				if (_phyExCmdMsg != null) {
					String outputMsg = _phyExCmdMsg;
					outputMsg += "m;";
					
					CommMgr.getCommMgr().sendMsg(outputMsg,
							CommMgr.MSG_TYPE_ARDUINO, false);
					_phyExCmdMsg = null;
				}
				
				_robotMap.revalidate();
				_robotMap.repaint();
				
			} // End if(_phyExCmdMsg != null)
		} // End while(true)
	}

	/** Wifi connection related functions ends here ************************ */

}
