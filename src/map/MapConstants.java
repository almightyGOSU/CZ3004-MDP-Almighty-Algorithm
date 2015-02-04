package map;

import java.awt.Color;

public final class MapConstants {
	
	// Grid size
	public static final int GRID_SIZE = 40;
	
	// Map size
	public static final int MAP_COLS = 22;
	public static final int MAP_ROWS = 17;
	
	// Colors for rendering the map
	public static final Color C_BORDER = Color.BLACK;
	
	public static final Color C_GRID_LINE = Color.ORANGE;
	public static final int GRID_LINE_WEIGHT = 2;
	
	public static final Color C_START = Color.BLUE;
	public static final Color C_GOAL = Color.GREEN;
	
	public static final Color C_UNEXPLORED = Color.LIGHT_GRAY;
	public static final Color C_FREE = Color.WHITE;
	public static final Color C_OBSTACLE = Color.DARK_GRAY;

	public static final Color C_ROBOT = Color.YELLOW;
	public static final Color C_PATH = Color.RED;
	
	
	// Prevent instantiation
	private MapConstants() {}
}
