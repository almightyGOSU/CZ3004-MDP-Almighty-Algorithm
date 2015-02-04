package simulator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JFrame;

import map.MapConstants;
import map.RealMap;

public class Simulator {
	
	private static RealMap _realMap = null;
	
	public static void main(String[] args) {
		
		int mapWidth = MapConstants.MAP_COLS * MapConstants.GRID_SIZE;
		int mapHeight = MapConstants.MAP_ROWS * MapConstants.GRID_SIZE;
		
		JFrame appFrame = new JFrame();
		appFrame.setTitle("Almighty Simulator");
		appFrame.setSize(new Dimension(892, 718));
		appFrame.setResizable(false);
		
		_realMap = new RealMap();
		_realMap.setSize(mapWidth, mapHeight);

		Container contentPane = appFrame.getContentPane();
		contentPane.add(_realMap, BorderLayout.CENTER);
		
		// Display the application
		appFrame.setVisible(true);
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
