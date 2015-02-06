package robot;

import java.awt.Color;

public final class RobotConstants {
	
	// Robot size
	public static final int ROBOT_SIZE = 2;
	
	// Sensors default range (In grids)
	public static final int SHORT_IR_MIN = 1;
	public static final int SHORT_IR_MAX = 8;
	
	public static final int LONG_IR_MIN = 2;
	public static final int LONG_IR_MAX = 15;
	
	public static enum DIRECTION {NORTH, EAST, SOUTH, WEST};
	
	// Might not need to  be stored in RobotConstants
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
	private RobotConstants() {}
}
