package robot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import robot.RobotConstants.DIRECTION;
import map.Map;
import map.MapConstants;
import map.RealMap;

@SuppressWarnings("serial")
public class RobotMap extends Map {
	
	// For measuring size of the canvas
	private boolean _bMeasured = false;

	// Size of the map
	private int _mapWidth = 0;
	private int _mapHeight = 0;
	
	// For rendering the map efficiently
	private MapGrid [][] _mapGrids = null;
	
	// Reference to the robot
	private Robot _robot = null;
	
	// For rendering the robot
	private int _robotOutlineSize = 0;
	private int _robotSize = 0;
	private int [] _arrowX = null;
	private int [] _arrowY = null;
	
	public RobotMap(final RealMap realMap) {
		super();
		
		this.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				//System.out.println("Robot sensing..");
				/*_robot.sense();
				
				// For display wall info when there is one
				String wallInfo = "";
				if(_robot.hasFrontWall())
					wallInfo += "Front Wall! ";
				if(_robot.hasLeftWall())
					wallInfo += "Left Wall! ";
				if(_robot.hasRightWall())
					wallInfo += "Right Wall!";
				
				if(!wallInfo.equals(""))
					System.out.println(wallInfo);*/
				
				//_robot.makeNextMove();
				
				RobotMap.this.revalidate();
            	RobotMap.this.repaint();
            	
            	//showSensorsInfo();
			}
		});
		
		this.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                
                /*if(e.getKeyCode() == KeyEvent.VK_UP ||
                		e.getKeyCode() == KeyEvent.VK_W) {
                	_robot.moveStraight();
                	RobotMap.this.revalidate();
                	RobotMap.this.repaint();
                }
                else if(e.getKeyCode() == KeyEvent.VK_RIGHT ||
                		e.getKeyCode() == KeyEvent.VK_D) {
                	_robot.turnRight();
                	RobotMap.this.revalidate();
                	RobotMap.this.repaint();
                }
                else if (e.getKeyCode() == KeyEvent.VK_LEFT ||
                		e.getKeyCode() == KeyEvent.VK_A) {
                	_robot.turnLeft();
                	RobotMap.this.revalidate();
                	RobotMap.this.repaint();
                }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN ||
                		e.getKeyCode() == KeyEvent.VK_S) {
                	_robot.turn180();
                	RobotMap.this.revalidate();
                	RobotMap.this.repaint();
                }*/
                
                //showSensorsInfo();
            }
        });
	}
	
	public void paintComponent(Graphics g) {
		
		if (!_bMeasured) {
			
			_mapWidth = this.getWidth();
			_mapHeight = this.getHeight();
			
			System.out.println("\nRobotMap Graphics g; Map width: " + _mapWidth
					+ ", Map height: " + _mapHeight);
			
			// Calculate the map grids for rendering
			_mapGrids = new MapGrid[MapConstants.MAP_ROWS][MapConstants.MAP_COLS];
			for (int mapRow = 0; mapRow < MapConstants.MAP_ROWS; mapRow++) {
				for (int mapCol = 0; mapCol < MapConstants.MAP_COLS; mapCol++) {
					_mapGrids[mapRow][mapCol] = new MapGrid(
							mapCol * MapConstants.GRID_SIZE,
							mapRow * MapConstants.GRID_SIZE,
							MapConstants.GRID_SIZE);
				}
			}
			
			_bMeasured = true;
		}
		
		// Clear the map
		g.setColor(Color.BLACK);
        g.fillRect(0, 0, _mapWidth, _mapHeight);
        
        Border border = BorderFactory.createLineBorder(
				MapConstants.C_GRID_LINE, MapConstants.GRID_LINE_WEIGHT);
        this.setBorder(border);
        
        // Paint the map grids
        for (int mapRow = 0; mapRow < MapConstants.MAP_ROWS; mapRow++)
		{
			for (int mapCol = 0; mapCol < MapConstants.MAP_COLS; mapCol++)
			{
				g.setColor(MapConstants.C_GRID_LINE);
				g.fillRect(_mapGrids[mapRow][mapCol].borderX,
						_mapGrids[mapRow][mapCol].borderY,
						_mapGrids[mapRow][mapCol].borderSize,
						_mapGrids[mapRow][mapCol].borderSize);
				
				Color gridColor = null;
				
				// Determine what color to fill grid
				if(isBorderWalls(mapRow, mapCol))
					gridColor = MapConstants.C_BORDER;
				else if(_grids[mapRow][mapCol].isExplored())
				{
					if(isStartZone(mapRow, mapCol))
						gridColor = MapConstants.C_START;
					else if(isGoalZone(mapRow, mapCol))
						gridColor = MapConstants.C_GOAL;
					else if(_grids[mapRow][mapCol].isObstacle())
						gridColor = MapConstants.C_OBSTACLE;
					else
						gridColor = MapConstants.C_FREE;
				}
				else
				{
					gridColor = MapConstants.C_UNEXPLORED;
				}
				
				g.setColor(gridColor);
				g.fillRect(_mapGrids[mapRow][mapCol].gridX,
						_mapGrids[mapRow][mapCol].gridY,
						_mapGrids[mapRow][mapCol].gridSize,
						_mapGrids[mapRow][mapCol].gridSize);
			}
		} // End outer for loop
        
        // Gets information about the robot
        int robotPosRow = _robot.getRobotPosRow();
        int robotPosCol = _robot.getRobotPosCol();        
        DIRECTION robotDir = _robot.getRobotDir();
        
        // Change the 'robot's position' for rendering!
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
        }
        
        // Draw the robot outline
		g.setColor(RobotConstants.C_ROBOT_OUTLINE);
		g.fillOval(_mapGrids[robotPosRow][robotPosCol].gridX,
				_mapGrids[robotPosRow][robotPosCol].gridY,
				_robotOutlineSize,
				_robotOutlineSize);
		
		// Draw the robot
		g.setColor(RobotConstants.C_ROBOT);
		g.fillOval(_mapGrids[robotPosRow][robotPosCol].gridX + 5,
				_mapGrids[robotPosRow][robotPosCol].gridY + 5,
				_robotSize,
				_robotSize);
		
		// Draw an arrow indicating the robot's direction
		calculateArrowPos(robotPosRow, robotPosCol, robotDir);
		g.setColor(RobotConstants.C_ROBOT_FRONT);
		g.fillPolygon(_arrowX, _arrowY, 3);
        
	} // End paintComponent
	
	/**
	 * Resets the robot map such that all grids are unexplored!
	 */
	public void resetRobotMap() {
		
		for (int row = 1; row < (MapConstants.MAP_ROWS - 1); row++)
		{
			for (int col = 1; col < (MapConstants.MAP_COLS - 1); col++)
			{
				_grids[row][col].resetGrid();
			}
		}
	}
	
	/**
	 * Generate MDF string for Part 1
	 */
	public String generateMDFStringPart1() {
		
		String mapString = "11"; // First two bits set to 11
		
		for (int col = 1; col < (MapConstants.MAP_COLS - 1); col++)
		{
			for (int row = 1; row < (MapConstants.MAP_ROWS - 1); row++)
			{
				mapString += _grids[row][col].isExplored() ? "1" : "0";
			}
		}
		
		// Last two bits set to 11
		return binaryToHex(mapString + "11");
	}
	
	/**
	 * Generate MDF string for Part 2
	 */
	public String generateMDFStringPart2() {
		
		String mapString = "";
		
		for (int col = 1; col < (MapConstants.MAP_COLS - 1); col++)
		{
			for (int row = 1; row < (MapConstants.MAP_ROWS - 1); row++)
			{
				if(_grids[row][col].isExplored())
					mapString += _grids[row][col].isObstacle() ? "1" : "0";
			}
		}
		
		int mapStringLength = mapString.length();
		int paddingLength = mapStringLength % 8;
		for(int i = 0; i < paddingLength; i++) {
			mapString += "0";
		}
		
		return binaryToHex(mapString);
	}
	
	public void setRobot(Robot robot) {
		_robot = robot;
		
		_robotOutlineSize = (MapConstants.GRID_SIZE -
				(MapConstants.GRID_LINE_WEIGHT * 2)) * RobotConstants.ROBOT_SIZE;
		_robotSize = _robotOutlineSize - 10;	
	}
	
	private String binaryToHex(String binaryString) {
		
		int binStrLength = binaryString.length();
		StringBuilder hexSb = new StringBuilder();
		
		for(int charIndex = 0; charIndex < binStrLength; charIndex += 4) {
			
			int value = 0;
			if(binaryString.charAt(charIndex) == '1')
				value += 8;
			if(binaryString.charAt(charIndex + 1) == '1')
				value += 4;
			if(binaryString.charAt(charIndex + 2) == '1')
				value += 2;
			if(binaryString.charAt(charIndex + 3) == '1')
				value += 1;
			
			hexSb.append(Integer.toHexString(value).toUpperCase());
		}
		
		return hexSb.toString();
	}
	
	private void calculateArrowPos(int robotPosRow, int robotPosCol,
			DIRECTION robotDir) {
		
		int quarterRobotSize = _robotOutlineSize / 4;
		int halfRobotSize = _robotOutlineSize / 2;
		
		int x1 = quarterRobotSize + _mapGrids[robotPosRow][robotPosCol].gridX;
		int y1 = halfRobotSize + _mapGrids[robotPosRow][robotPosCol].gridY;
		
		int x2 = halfRobotSize + _mapGrids[robotPosRow][robotPosCol].gridX;
		int y2 = quarterRobotSize + _mapGrids[robotPosRow][robotPosCol].gridY;
		
		int x3 = x1 + halfRobotSize;
		int y3 = y1;
		
		int x4 = x2;
		int y4 = y2 + halfRobotSize;
		
		switch(robotDir) {
		case EAST:
			_arrowX = new int [] {x2, x3, x4};
			_arrowY = new int [] {y2, y3, y4};
			break;
		case NORTH:
			_arrowX = new int [] {x1, x2, x3};
			_arrowY = new int [] {y1, y2, y3};
			break;
		case SOUTH:
			_arrowX = new int [] {x1, x3, x4};
			_arrowY = new int [] {y1, y3, y4};
			break;
		case WEST:
			_arrowX = new int [] {x1, x2, x4};
			_arrowY = new int [] {y1, y2, y4};
			break;
		}
	}
	
	private class MapGrid {
		public int borderX;
		public int borderY;
		public int borderSize;
		
		public int gridX;
		public int gridY;
		public int gridSize;
		
		public MapGrid(int borderX, int borderY, int borderSize) {
			this.borderX = borderX;
			this.borderY = borderY;
			this.borderSize = borderSize;
			
			this.gridX = borderX + MapConstants.GRID_LINE_WEIGHT;
			this.gridY = borderY + MapConstants.GRID_LINE_WEIGHT;
			this.gridSize = borderSize - (MapConstants.GRID_LINE_WEIGHT * 2);
		}
	}
}
