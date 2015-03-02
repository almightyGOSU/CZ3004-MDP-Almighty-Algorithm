package robot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.Timer;

import map.Grid;
import map.MapConstants;
import map.RealMap;
import robot.RobotConstants.*;

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
	
	// A reference point used for the sensors to rotate along with the robot (In grids)
	// NOTE: This will be the robot's bottom left corner
	//private int _robotRefPosRow;
	//private int _robotRefPosCol;

	// Robot's current direction
	private DIRECTION _robotDirection;
	
	// Robot's collection of sensors
	private ArrayList<Sensor> _sensors = null;
	
	// Robot's settings for exploration
	private int _stepsPerSecond = RobotConstants.DEFAULT_STEPS_PER_SECOND;
	private int _coverageLimit = RobotConstants.DEFAULT_COVERAGE_LIMIT;
	private int _timeLimit = RobotConstants.DEFAULT_TIME_LIMIT;
	private boolean _bCoverageLimited = false;
	private boolean _bTimeLimited = false;
	
	// Robot's robot map
	private transient RobotMap _robotMap = null;	// For determining next action
	private transient RealMap _realMap = null; 		// For detecting obstacles
	
	// Some memory for the robot here
	//private boolean _bPreviousFrontWall = false;
	//private boolean _bPreviousLeftWall = false;
	//private boolean _bPreviousRightWall = false;
	private transient boolean _bReachedGoal = false;
	private transient boolean _bExplorationComplete = false;
	
	// Timer for controlling robot movement
	private transient Timer _timer = null;
	private transient int _timerIntervals = 0;
	
	// Number of explored grids required to reach coverage limit
	private transient int _explorationTarget = 0;
	
	// Elapsed time for the exploration phase (in milliseconds)
	private transient int _elapsedExplorationTime = 0;

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
	 * Adds a new sensor to the robot
	 * 
	 * @param newSensor The sensor to be added
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
	 * Just a temporary function for testing exploration
	 */
	public void startExploration() {
		
		System.out.println("\nStarting exploration!");
		
		// Calculate timer intervals based on the user selected steps per second
		_timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
		System.out.println("Steps Per Second: " + _stepsPerSecond +
				", Timer Interval: " + _timerIntervals);
		
		// Calculate number of explored grids required
		_explorationTarget = (int)((_coverageLimit / 100.0) *
				((MapConstants.MAP_ROWS - 2) * (MapConstants.MAP_COLS - 2)));
		System.out.println("Exploration target (In grids): " + _explorationTarget);
		
		// Reset the elapsed exploration time (in milliseconds)
		_elapsedExplorationTime = 0;
		
		_timer = new Timer(_timerIntervals, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(_timer != null && _bExplorationComplete) {				
					_timer.stop();
					_timer = null;
				}
				else {
					// Make the next move
					makeNextMove();
					
					// Update elapsed time
					_elapsedExplorationTime += _timerIntervals;
				}
			}
		});
		_timer.setRepeats(true);
		_timer.setInitialDelay(1000);
		_timer.start();
	}
	
	/**
	 * Just a temporary function for testing exploration
	 */
	public void stopExploration() {
		
		if(_timer != null) {				
			_timer.stop();
			_timer = null;
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
	 * LIANGLIANG's PART STARTS HERE ******************************************
	 */
	private boolean _previousLeftWall = true;
	
	public Stack<Grid> findShortestPath(Grid x, Grid y, DIRECTION dir) {
		Grid[][] map = _robotMap.getMapGrids();
		// System.out.println("example:"+map[1][1].getCol()+map[2][2].getCol());
		Grid startGrid = x;
		Grid endGrid = y;
		// create the map info
		// Grid [][] map = _robotMap.getMapGrids();
		// A* search
		Stack<Grid> path = new Stack<Grid>();
		Stack<Grid> countedGrid = new Stack<Grid>();
		path.push(endGrid);
		countedGrid.push(startGrid);
		// System.out.println("Grid on stack: "+path.peek().getRow()+path.peek().getCol());
		// int [][] aLeaf = new int[20][15];

		Grid targetGrid;
		Grid nextGrid = startGrid;
		Grid neiGrid[] = new Grid[4];
		boolean findShortestPath = false;
		DIRECTION tempDir = dir;
		double[][] leaf = new double[MapConstants.MAP_ROWS][MapConstants.MAP_COLS];

		// leaf initialization
		for (int i = 0; i < MapConstants.MAP_ROWS; i++) {
			for (int j = 0; j < MapConstants.MAP_COLS; j++) {
				if (map[i][j].isObstacle())
					leaf[i][j] = Double.NEGATIVE_INFINITY;
				else
					leaf[i][j] = 0;
				// System.out.println("aleaf: "+i+j);
			}
		}

		while (!findShortestPath) {
			targetGrid = nextGrid;
			boolean searchedGrid[] = { false, false, false, false };
			System.out.println("                     targetGrid: "
					+ targetGrid.getRow() + "," + targetGrid.getCol());

			// fourCells around targetGrid;
			neiGrid[0] = map[targetGrid.getRow()][targetGrid.getCol() + 1];
			neiGrid[1] = map[targetGrid.getRow()][targetGrid.getCol() - 1];
			neiGrid[2] = map[targetGrid.getRow() + 1][targetGrid.getCol()];
			neiGrid[3] = map[targetGrid.getRow() - 1][targetGrid.getCol()];

			switch (tempDir) {
			case EAST:
				if (leaf[neiGrid[0].getRow()][neiGrid[0].getCol()] <= 0)// not
																		// searched
																		// yet
					leaf[neiGrid[0].getRow()][neiGrid[0].getCol()] = leaf[neiGrid[0]
							.getRow()][neiGrid[0].getCol()]
							+ leaf[targetGrid.getRow()][targetGrid.getCol()]
							+ 1;
				else
					searchedGrid[0] = true;
				for (int i = 1; i < 4; i++) {
					if (leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] <= 0)// not
																			// searched
																			// yet
						leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] = leaf[neiGrid[i]
								.getRow()][neiGrid[i].getCol()]
								+ leaf[targetGrid.getRow()][targetGrid.getCol()]
								+ 2;
					else
						searchedGrid[i] = true;
				}
				break;
			case WEST:
				if (leaf[neiGrid[1].getRow()][neiGrid[1].getCol()] <= 0)// not
																		// searched
																		// yet
					leaf[neiGrid[1].getRow()][neiGrid[1].getCol()] = leaf[neiGrid[1]
							.getRow()][neiGrid[1].getCol()]
							+ leaf[targetGrid.getRow()][targetGrid.getCol()]
							+ 1;
				else
					searchedGrid[1] = true;
				if (leaf[neiGrid[0].getRow()][neiGrid[0].getCol()] <= 0)// not
																		// searched
																		// yet
					leaf[neiGrid[0].getRow()][neiGrid[0].getCol()] = leaf[neiGrid[0]
							.getRow()][neiGrid[0].getCol()]
							+ leaf[targetGrid.getRow()][targetGrid.getCol()]
							+ 2;
				else
					searchedGrid[0] = true;
				for (int i = 2; i < 4; i++) {
					if (leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] <= 0)// not
																			// searched
																			// yet
						leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] = leaf[neiGrid[i]
								.getRow()][neiGrid[i].getCol()]
								+ leaf[targetGrid.getRow()][targetGrid.getCol()]
								+ 2;
					else
						searchedGrid[i] = true;
				}
				break;
			case SOUTH:
				if (leaf[neiGrid[2].getRow()][neiGrid[2].getCol()] <= 0)// not
																		// searched
																		// yet
					leaf[neiGrid[2].getRow()][neiGrid[2].getCol()] = leaf[neiGrid[2]
							.getRow()][neiGrid[2].getCol()]
							+ leaf[targetGrid.getRow()][targetGrid.getCol()]
							+ 1;
				else
					searchedGrid[2] = true;
				if (leaf[neiGrid[3].getRow()][neiGrid[3].getCol()] <= 0)// not
																		// searched
																		// yet
					leaf[neiGrid[3].getRow()][neiGrid[3].getCol()] = leaf[neiGrid[3]
							.getRow()][neiGrid[3].getCol()]
							+ leaf[targetGrid.getRow()][targetGrid.getCol()]
							+ 2;
				else
					searchedGrid[0] = true;
				for (int i = 0; i < 2; i++) {
					if (leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] <= 0)// not
																			// searched
																			// yet
						leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] = leaf[neiGrid[i]
								.getRow()][neiGrid[i].getCol()]
								+ leaf[targetGrid.getRow()][targetGrid.getCol()]
								+ 2;
					else
						searchedGrid[i] = true;
				}
				break;
			case NORTH:
				if (leaf[neiGrid[3].getRow()][neiGrid[3].getCol()] <= 0)// not
																		// searched
																		// yet
					leaf[neiGrid[3].getRow()][neiGrid[3].getCol()] = leaf[neiGrid[3]
							.getRow()][neiGrid[3].getCol()]
							+ leaf[targetGrid.getRow()][targetGrid.getCol()]
							+ 1;
				else
					searchedGrid[3] = true;
				for (int i = 0; i < 3; i++) {
					if (leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] <= 0)// not
																			// searched
																			// yet
						leaf[neiGrid[i].getRow()][neiGrid[i].getCol()] = leaf[neiGrid[i]
								.getRow()][neiGrid[i].getCol()]
								+ leaf[targetGrid.getRow()][targetGrid.getCol()]
								+ 2;
					else
						searchedGrid[i] = true;
				}
				break;

			}
			//
			System.out.println("neigrid[0]: " + neiGrid[0].getRow() + ","
					+ neiGrid[0].getCol());
			System.out.println("leaf for 0: "
					+ leaf[neiGrid[0].getRow()][neiGrid[0].getCol()]);
			System.out.println("searchedGrid[0]: " + searchedGrid[0]);
			//
			System.out.println("neigrid[1]: " + neiGrid[1].getRow() + ","
					+ neiGrid[1].getCol());
			System.out.println("leaf for 1: "
					+ leaf[neiGrid[1].getRow()][neiGrid[1].getCol()]);
			System.out.println("searchedGrid[1]: " + searchedGrid[1]);
			//
			System.out.println("neigrid[2]: " + neiGrid[2].getRow() + ","
					+ neiGrid[2].getCol());
			System.out.println("leaf for 2: "
					+ leaf[neiGrid[2].getRow()][neiGrid[2].getCol()]);
			System.out.println("searchedGrid[2]: " + searchedGrid[2]);
			//
			System.out.println("neigrid[3]: " + neiGrid[3].getRow() + ","
					+ neiGrid[3].getCol());
			System.out.println("leaf for 3: "
					+ leaf[neiGrid[3].getRow()][neiGrid[3].getCol()]);
			System.out.println("searchedGrid[3]: " + searchedGrid[3]);
			//
			nextGrid = minimum(targetGrid, endGrid, countedGrid, leaf);

			Grid nextNeiGrid[] = new Grid[4];

			nextNeiGrid[0] = map[nextGrid.getRow()][nextGrid.getCol() + 1];
			nextNeiGrid[1] = map[nextGrid.getRow()][nextGrid.getCol() - 1];
			nextNeiGrid[2] = map[nextGrid.getRow() + 1][nextGrid.getCol()];
			nextNeiGrid[3] = map[nextGrid.getRow() - 1][nextGrid.getCol()];
			double tempMin = leaf[nextNeiGrid[0].getRow()][nextNeiGrid[0]
					.getCol()];
			Grid tempGrid = nextNeiGrid[0];
			for (Grid nextNeiGrids : nextNeiGrid) {
				if (tempMin > leaf[nextNeiGrids.getRow()][nextNeiGrids.getCol()]) {
					tempMin = leaf[nextNeiGrids.getRow()][nextNeiGrids.getCol()];
					tempGrid = nextNeiGrids;
				}
			}
			if (tempGrid.getCol() - nextGrid.getCol() == -1)
				tempDir = DIRECTION.EAST;
			if (tempGrid.getCol() - nextGrid.getCol() == 1)
				tempDir = DIRECTION.WEST;
			if (tempGrid.getRow() - nextGrid.getRow() == -1)
				tempDir = DIRECTION.SOUTH;
			if (tempGrid.getRow() - nextGrid.getRow() == 1)
				tempDir = DIRECTION.NORTH;

			countedGrid.push(nextGrid);
			if (countedGrid.peek() == endGrid)
				findShortestPath = true;
			System.out.println("nextGrid: " + nextGrid.getRow() + ","
					+ nextGrid.getCol());
			System.out.println("Dir: " + tempDir);
		}
		System.out.println("path found!");
		
		
		Grid nextGrid1 = endGrid;
		while (path.peek() != startGrid) {
			Grid nextNeiGrid1[] = new Grid[4];
			nextNeiGrid1[0] = map[nextGrid1.getRow()][nextGrid1.getCol() + 1];
			nextNeiGrid1[1] = map[nextGrid1.getRow()][nextGrid1.getCol() - 1];
			nextNeiGrid1[2] = map[nextGrid1.getRow() + 1][nextGrid1.getCol()];
			nextNeiGrid1[3] = map[nextGrid1.getRow() - 1][nextGrid1.getCol()];
			double tempMin = Double.POSITIVE_INFINITY;
			Grid tempGrid = nextNeiGrid1[0];
			for (Grid nextNeiGrids : nextNeiGrid1) {
				if (tempMin > leaf[nextNeiGrids.getRow()][nextNeiGrids.getCol()]
						&& leaf[nextNeiGrids.getRow()][nextNeiGrids.getCol()] > 0
						&& countedGrid
								.contains(map[nextNeiGrids.getRow()][nextNeiGrids
										.getCol()])) {
					tempMin = leaf[nextNeiGrids.getRow()][nextNeiGrids.getCol()];
					tempGrid = nextNeiGrids;
					System.out.println("tempGrid: " + tempGrid.getRow() + ","
							+ tempGrid.getCol());
					System.out.println("tempMin: " + tempMin);
				}
			}
			System.out.println("          path peek: " + path.peek().getRow()
					+ "," + path.peek().getCol());
			path.push(tempGrid);
			nextGrid1 = tempGrid;
		}
		return path;
	}
	
	public Grid minimum(Grid y, Grid x, Stack<Grid> countedGrid, double[][] leaf) {
		System.out.println("I'm here!");
		Grid[][] map = _robotMap.getMapGrids();
		Grid minimumGrid = y;
		double minimumValue = Double.POSITIVE_INFINITY;
		for (int i = 1; i < MapConstants.MAP_ROWS - 1; i++) {
			for (int j = 1; j < MapConstants.MAP_COLS - 1; j++) {
				if (((minimumValue > leaf[i][j] + Math.abs(x.getRow() - i)
						+ Math.abs(x.getCol() - j))
						&& leaf[i][j] > 0 && (!countedGrid.contains(map[i][j])))
				/*
				 * ||
				 * ((minimumValue==leaf[i][j]+Math.abs(x.getRow()-i)+Math.abs(
				 * x.getCol()-j)) && leaf[i][j]>0 &&
				 * (i==y.getRow()||j==y.getCol()) )
				 */) {
					minimumValue = leaf[i][j] + Math.abs(x.getRow() - i)
							+ Math.abs(x.getCol() - j);
					minimumGrid = map[i][j];
					System.out.println("minimumValue: " + minimumValue);
					System.out.println("minimumGrid: " + minimumGrid.getRow()
							+ "," + minimumGrid.getCol());
				}

			}
		}
		return minimumGrid;
	}
	
	public void followThePath(Stack<Grid> path) {
		System.out.println("following path!");
		Grid grid;
		while (!path.isEmpty()) {
			grid = path.pop();
			this.sense();
			System.out.println("path: " + grid.getRow() + "," + grid.getCol());
			System.out.println("robot dir:" + this.getRobotDir());
			System.out.println("robot pos: " + this.getRobotMapPosRow() + ","
					+ this.getRobotMapPosCol());
			switch (this.getRobotDir()) {
			case EAST:
				if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() + 1) {
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() - 1) {
					turn180();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() + 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					turnRight();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() - 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					turnLeft();
					moveStraight();
				} else

					break;
			case WEST:
				if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() + 1) {
					turn180();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() - 1) {
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() + 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					turnLeft();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() - 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					turnRight();
					moveStraight();
				} else

					break;
			case SOUTH:
				if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() + 1) {
					turnLeft();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() - 1) {
					turnRight();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() + 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() - 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					turn180();
					moveStraight();
				} else

					break;
			case NORTH:
				if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() + 1) {
					turnRight();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow()
						&& grid.getCol() == this.getRobotMapPosCol() - 1) {
					turnLeft();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() + 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					turn180();
					moveStraight();
				} else if (grid.getRow() == this.getRobotMapPosRow() - 1
						&& grid.getCol() == this.getRobotMapPosCol()) {
					moveStraight();
				} else

					break;
			}
			//grid = path.pop();
		}
	}
	/**
	 * LIANGLIANG's PART ENDS HERE ********************************************
	 */
	
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
		
		/**
		 * LIANGLIANG's PART STARTS HERE **************************************
		 */
		// Robot reached goal zone and is back at the start zone
		int noOfExplored = 0;
		Stack<Grid> unexploredGrid = new Stack<Grid>();
		Grid[][] map = _robotMap.getMapGrids();
		if (_bReachedGoal && withinStartZone(_robotMapPosRow, _robotMapPosCol)) {
			for (int i = 1; i < MapConstants.MAP_ROWS - 1; i++) {
				for (int j = 1; j < MapConstants.MAP_COLS - 1; j++) {
					if (map[i][j].isExplored())
						noOfExplored++;
					else {
						unexploredGrid.push(map[i][j]);
						System.out.println("Unexplored grid: "
								+ unexploredGrid.peek().getRow()
								+ unexploredGrid.peek().getCol());
					}
				}
			}
			System.out.println("noOfExplored: " + noOfExplored);
			if (noOfExplored == 300)
				_bExplorationComplete = true;
			else {
				while (!unexploredGrid.isEmpty()) {
					System.out.println("while loops");
					followThePath(findShortestPath(
							map[_robotMapPosRow][_robotMapPosCol],
							unexploredGrid.pop(), _robotDirection));
				}
				// followThePath(findShortestPath(map[_robotMapPosRow][_robotMapPosCol],map[2][1],_robotDirection));
				_bExplorationComplete = true;
			}
			return;
		}

		boolean frontWall = hasFrontWall();
		boolean leftWall = hasLeftWall();
		boolean rightWall = hasRightWall();

		// (No frontWall AND No leftWall AND previousLeftWall) or
		// (frontWall AND No leftWall AND rightWall)
		if (!frontWall && !leftWall && _previousLeftWall || frontWall
				&& !leftWall && rightWall)
			turnLeft();
		
		// (frontWall AND leftWall AND No rightWall)
		else if (frontWall && leftWall && !rightWall)
			turnRight();
		
		// (frontWall AND leftWall AND rightWall)
		else if (frontWall && leftWall && rightWall)
			turn180();
		
		else
			moveStraight();
		
		// Save current leftWall state into previousLeftWall
		_previousLeftWall = leftWall;
		
		/**
		 * LIANGLIANG's PART ENDS HERE ****************************************
		 */
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
	
	public int getStepsPerSecond() {
		return _stepsPerSecond;
	}
	
	public int getCoverageLimit() {
		return _coverageLimit;
	}
	
	public int getTimeLimit() {
		return _timeLimit;
	}
	
	public boolean isCoverageLimited() {
		return _bCoverageLimited;
	}
	
	public boolean isTimeLimited() {
		return _bTimeLimited;
	}
	
	/**
	 * Update the exploration settings for the robot
	 * 
	 * @param stepsPerSecond 	Exploration speed, in steps per second
	 * @param coverageLimit 	Coverage Limit, [1, 100] percent
	 * @param timeLimit			Time Limit, [1, 360] seconds
	 * @param bCoverageLimited	True if using coverage limit
	 * @param bTimeLimited		True if using time limit
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
