package map;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.border.Border;

@SuppressWarnings("serial")
public class RealMap extends Map {
	
	// For measuring size of the canvas
	private boolean _bMeasured = false;

	// Size of the map
	private int _mapWidth = 0;
	private int _mapHeight = 0;

	// For rendering the map efficiently
	private MapGrid[][] _mapGrids = null;
	
	public RealMap() {
		super();
		buildDefaultMap();
		
		this.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				boolean bControlDown = e.isControlDown();
				
				int mouseClickX = e.getX();
				int mouseClickY = e.getY();
				
				/*System.out.print(mouseClickX + ", ");
				System.out.print(mouseClickY);
				System.out.println(bControlDown ? ", Control down" : "");*/
				
				int gridRow = mouseClickY / MapConstants.GRID_SIZE;
				int gridCol = mouseClickX / MapConstants.GRID_SIZE;
				
				if((gridRow < MapConstants.MAP_ROWS)
						&& (gridCol < MapConstants.MAP_COLS))
				{
					if(bControlDown)
						removeObstacle(gridRow, gridCol);
					else 
						addObstacle(gridRow, gridCol);
				}
			}
		});
	}
	
	private void buildDefaultMap() {
		
		for (int row = 0; row < MapConstants.MAP_ROWS; row++)
		{
			for (int col = 0; col < MapConstants.MAP_COLS; col++)
			{
				// Obstacle - Border walls
				if(isBorderWalls(row, col))
				{
					_grids[row][col].setObstacle(true);
				}
			}
		}
	}
	
	private void addObstacle(int row, int col) {
		if(_grids[row][col].isObstacle())
		{
			JOptionPane.showMessageDialog(this, "Why would you want to put an obstacle"
					+ " on an obstacle? Please try again!", "Warning",
				    JOptionPane.WARNING_MESSAGE);
		}
		else if(isStartZone(row, col) || isGoalZone(row, col))
		{
			JOptionPane.showMessageDialog(this, "Why would you want to put an obstacle"
					+ " in the start/goal zone? Please try again!", "Warning",
				    JOptionPane.WARNING_MESSAGE);
		}
		else
		{
			_grids[row][col].setObstacle(true);
		}
	}
	
	private void removeObstacle(int row, int col) {
		if(_grids[row][col].isObstacle())
		{
			if (isBorderWalls(row, col)) {
				JOptionPane.showMessageDialog(null,
						"Removing the border walls will cause the robot to"
						+ " fall off the edge of the arena. Please do not"
						+ " attempt to kill the robot!", "Warning",
					    JOptionPane.WARNING_MESSAGE);
			}
			else
			{
				_grids[row][col].setObstacle(false);
			}
		}
	}
	
	public void paintComponent(Graphics g) {
		
		if (!_bMeasured) {

			_mapWidth = this.getWidth();
			_mapHeight = this.getHeight();

			System.out.println("RealMap Graphics g; Map width: " + _mapWidth
					+ ", Map height: " + _mapHeight);

			// Calculate the map grids for rendering
			_mapGrids = new MapGrid[MapConstants.MAP_ROWS][MapConstants.MAP_COLS];
			for (int mapRow = 0; mapRow < MapConstants.MAP_ROWS; mapRow++) {
				for (int mapCol = 0; mapCol < MapConstants.MAP_COLS; mapCol++) {
					_mapGrids[mapRow][mapCol] = new MapGrid(mapCol
							* MapConstants.GRID_SIZE, mapRow
							* MapConstants.GRID_SIZE, MapConstants.GRID_SIZE);
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
        
        // Paint the grids
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
				else if(isStartZone(mapRow, mapCol))
					gridColor = MapConstants.C_START;
				else if(isGoalZone(mapRow, mapCol))
					gridColor = MapConstants.C_GOAL;
				else
				{
					if(_grids[mapRow][mapCol].isObstacle())
						gridColor = MapConstants.C_OBSTACLE;
					else
						gridColor = MapConstants.C_FREE;
				}
				
				g.setColor(gridColor);
				g.fillRect(_mapGrids[mapRow][mapCol].gridX,
						_mapGrids[mapRow][mapCol].gridY,
						_mapGrids[mapRow][mapCol].gridSize,
						_mapGrids[mapRow][mapCol].gridSize);
				
			}
		} // End outer for loop	
	} // End paintComponent
	
	/**
	 * Saves the current map to a map descriptor string<br>
	 * Not including the virtual border surrounding the area!
	 * 
	 * @return The map descriptor string
	 */
	public String generateMapString() {
		
		String mapString = "";
		
		for (int row = 1; row < (MapConstants.MAP_ROWS - 1); row++)
		{
			for (int col = 1; col < (MapConstants.MAP_COLS - 1); col++)
			{
				// Obstacle - Border walls
				if(!_grids[row][col].isObstacle())
					mapString += "0";
				else
					mapString += "1";
			}
		}
		
		return mapString;
	}
	
	/**
	 * Loads the map from a map descriptor string<br>
	 * Not including the virtual border surrounding the area!
	 */
	public void loadFromMapString(String mapString) {
		
		for (int row = 1; row < (MapConstants.MAP_ROWS - 1); row++)
		{
			for (int col = 1; col < (MapConstants.MAP_COLS - 1); col++)
			{
				int charIndex = ((row - 1) * (MapConstants.MAP_COLS - 2))
						+ (col - 1);
				
				// Obstacle - Border walls
				if(mapString.charAt(charIndex) == '1')
					_grids[row][col].setObstacle(true);
				else
					_grids[row][col].setObstacle(false);
			}
		}
	}
	
	public void clearMap() {
		
		for (int row = 1; row < (MapConstants.MAP_ROWS - 1); row++)
		{
			for (int col = 1; col < (MapConstants.MAP_COLS - 1); col++)
			{
				_grids[row][col].setObstacle(false);
			}
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
