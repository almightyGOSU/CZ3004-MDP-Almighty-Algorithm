package map;

public class Grid {
	
	private boolean _bExplored;
	private boolean _bObstacle;
	
	public Grid() {
		_bExplored = false;
		_bObstacle = false;
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
	
	public void setExplored(boolean bExplored) {
		_bExplored = bExplored;
	}
	
	public void setObstacle(boolean bObstacle) {
		
		_bExplored = false;
		_bObstacle = bObstacle;
	}
	
	public void resetGrid() {
		_bExplored = false;
		_bObstacle = false;
	}
}
