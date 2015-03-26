package map;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class Map extends JPanel {

	protected Grid [][] _grids = null;

	public Map() {
		
		_grids = new Grid [MapConstants.MAP_ROWS][MapConstants.MAP_COLS];
		
		for (int row = 0; row < MapConstants.MAP_ROWS; row++) {
			for (int col = 0; col < MapConstants.MAP_COLS; col++) {
				_grids[row][col] = new Grid(row, col);
			}
		}
	}

	public void resetMap() {
		
		for (int row = 0; row < MapConstants.MAP_ROWS; row++) {
			for (int col = 0; col < MapConstants.MAP_COLS; col++) {
				_grids[row][col].resetGrid();
			}
		}
	}
	
	public boolean isBorderWalls(int row, int col) {
		return (row == 0 || row == (MapConstants.MAP_ROWS - 1) ||
				col == 0 || col == (MapConstants.MAP_COLS - 1));
	}
	
	/**
	 * Checks if a given row and column is within the start zone
	 * 
	 * @param row The specified row to check
	 * @param col The specified col to check
	 * @return True if the specified row and column is within the start zone
	 */
	public boolean isStartZone(int row, int col) {
		return (row >= 1 && row <= 3 && col >= 1 && col <= 3);
	}
	
	/**
	 * Checks if a given row and column is within the goal zone
	 * 
	 * @param row The specified row to check
	 * @param col The specified col to check
	 * @return True if the specified row and column is within the goal zone
	 */
	public boolean isGoalZone(int row, int col) {
		return ((row <= (MapConstants.MAP_ROWS - 2))
				&& (row >= (MapConstants.MAP_ROWS - 4))
				&& (col <= (MapConstants.MAP_COLS - 2))
				&& (col >= (MapConstants.MAP_COLS - 4)));
	}
	
	public Grid [][] getMapGrids() {
		return _grids;
	}

}
