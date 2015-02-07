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
	
	// Temporary
	private boolean _bDrawn = false;
	
	public RealMap() {
		super();
		buildDefaultMap();
		
		this.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				boolean bControlDown = e.isControlDown();
				
				int mouseClickX = e.getX();
				int mouseClickY = e.getY();
				
				System.out.print(mouseClickX + ", ");
				System.out.print(mouseClickY);
				System.out.println(bControlDown ? ", Control down" : "");
				
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
		
		int mapWidth = this.getWidth();
		int mapHeight = this.getHeight();
		
		if (!_bDrawn) {
			System.out.println("RealMap Graphics g; Map width: " + mapWidth
					+ ", Map height: " + mapHeight);
			_bDrawn = true;
		}
		
		// Clear the map
		g.setColor(Color.BLACK);
        g.fillRect(0, 0, mapWidth, mapHeight);
        
        Border border = BorderFactory.createLineBorder(
				MapConstants.C_GRID_LINE, MapConstants.GRID_LINE_WEIGHT);
        this.setBorder(border);
        
        // Paint the grids
        for (int row = 0; row < MapConstants.MAP_ROWS; row++)
		{
			for (int col = 0; col < MapConstants.MAP_COLS; col++)
			{
				g.setColor(MapConstants.C_GRID_LINE);
				g.fillRect(col * MapConstants.GRID_SIZE,
						row * MapConstants.GRID_SIZE,
						MapConstants.GRID_SIZE, MapConstants.GRID_SIZE);
				
				Color gridColor = null;
				
				// Determine what color to fill grid
				if(isBorderWalls(row, col))
					gridColor = MapConstants.C_BORDER;
				else if(isStartZone(row, col))
					gridColor = MapConstants.C_START;
				else if(isGoalZone(row, col))
					gridColor = MapConstants.C_GOAL;
				else
				{
					if(_grids[row][col].isObstacle())
						gridColor = MapConstants.C_OBSTACLE;
					else
						gridColor = MapConstants.C_FREE;
				}
				
				g.setColor(gridColor);
				g.fillRect(
						(col * MapConstants.GRID_SIZE + MapConstants.GRID_LINE_WEIGHT),
						(row * MapConstants.GRID_SIZE + MapConstants.GRID_LINE_WEIGHT),
						(MapConstants.GRID_SIZE - MapConstants.GRID_LINE_WEIGHT * 2),
						(MapConstants.GRID_SIZE - MapConstants.GRID_LINE_WEIGHT * 2));
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
	
	public Grid [][] getMapGrids() {
		return _grids;
	}
	
}
