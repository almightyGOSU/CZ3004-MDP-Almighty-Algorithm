package map;

public class Grid {
	
	private boolean _bExplored = false;
	private boolean _bObstacle = false;
	
	public Grid() {
		
	}
	
	public boolean isExplored() {
		return _bExplored;
	}
	
	public boolean isObstacle() {
		return _bObstacle;
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
