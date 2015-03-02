package map;

public class Grid {
	
	private boolean _bExplored;		// Indicates whether this grid has been explored
	private boolean _bObstacle;		// Indicates whether this grid is an obstacle
	
	private int _row;				// This grid's row within the map
	private int _col;				// This grid's column within the map
	
	/**
	 * Default Constructor
	 * 
	 * @author Jin Yao
	 */
	public Grid() {
		_bExplored = false;
		_bObstacle = false;
	}
	
	/**
	 * Constructor which initializes the grid's position within the map
	 * 
	 * @param row 	The grid's row within the map
	 * @param col	The grid's column within the map
	 * 
	 * @author Liang Liang
	 */
	public Grid(int row, int col) {
		_row = row;
		_col = col;
		
		_bExplored = false;
		_bObstacle = false;
	}
	
	/**
	 * Gets the Grid's row within the Map
	 * 
	 * @return The Grid's row within the Map
	 */
	public int getRow() {
		return _row;
	}
	
	/**
	 * Gets the Grid's column within the Map
	 * 
	 * @return The Grid's column within the Map
	 */
	public int getCol() {
		return _col;
	}
	
	/**
	 * Sets the Grid's row within the Map
	 * 
	 * @param newRow The Grid's new row within the Map
	 */
	public void setRow(int newRow) {
		_row = newRow;
	}
	
	/**
	 * Sets the Grid's column within the Map
	 * 
	 * @param newCol The Grid's new column within the Map
	 */
	public void setCol(int newCol) {
		_col = newCol;
	}
	
	/**
	 * Indicates whether this Grid has been explored
	 * 
	 * @return True if this Grid has been explored
	 */
	public boolean isExplored() {
		return _bExplored;
	}
	
	/**
	 * Indicates whether this Grid contains an obstacle
	 * 
	 * @return True if this Grid contains an obstacle
	 */
	public boolean isObstacle() {
		return _bObstacle;
	}
	
	/**
	 * Mark this Grid as explored
	 * 
	 * @param bExplored True if this grid has been explored, false otherwise
	 */
	public void setExplored(boolean bExplored) {
		_bExplored = bExplored;
	}
	
	/**
	 * Mark this Grid as an obstacle
	 */
	public void markAsObstacle() {
		_bObstacle = true;
	}
	
	/**
	 * Set this grid as an obstacle
	 * 
	 * @param bObstacle True if this grid is an obstacle, false otherwise
	 */
	public void setObstacle(boolean bObstacle) {
		
		_bExplored = false;
		_bObstacle = bObstacle;
	}
	
	/**
	 * Resets this Grid<p>
	 * This grid will be unexplored, and not an obstacle
	 */
	public void resetGrid() {
		_bExplored = false;
		_bObstacle = false;
	}
}
