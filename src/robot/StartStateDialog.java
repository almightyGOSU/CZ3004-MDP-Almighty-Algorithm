package robot;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;

import map.MapConstants;

import java.beans.*; 				// Property change stuff
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.*;


@SuppressWarnings("serial")
public class StartStateDialog extends JDialog implements PropertyChangeListener {
	
    private JTextField _startPosRowTf = null;
    private JTextField _startPosColTf = null;
    
    // Starts at (1, 1) by default
    private String _startPosRowText = Integer.toString(
    		RobotConstants.DEFAULT_START_ROW);
    private String _startPosColText = Integer.toString(
    		RobotConstants.DEFAULT_START_COL);
    
    private JOptionPane _optionPane;
 
    private String _btnString1 = "Save Changes";
    private String _btnString2 = "Cancel";
    
    // For robot's starting direction
    private String _dirOptNorth = "NORTH";
    private String _dirOptEast = "EAST";
    private String _dirOptSouth = "SOUTH";
    private String _dirOptWest = "WEST";
    private JLabel _currDirLabel = null;
    
    // Starts facing North by default
    private String _robotDir = RobotConstants.DEFAULT_START_DIR.toString();
 
    /** Creates the reusable dialog */
    public StartStateDialog(Frame frame) {
        super(frame, true);
        setTitle("Configure Starting State");
 
        _startPosRowTf = new JTextField(10);
        _startPosColTf = new JTextField(10);
        
        Font font = new Font("Arial", Font.PLAIN, 28);
        _startPosRowTf.setFont(font);
        _startPosColTf.setFont(font);
 
        // Create an array of the text and components to be displayed
        JLabel infoLabel = new JLabel("Starting Position is relative to the"
        		+ " map! (Top Left: 1, 1)");
        JLabel startPosRowLabel = new JLabel("Starting Row:");
        JLabel startPosColLabel = new JLabel("Starting Column:");
        JLabel robotDirLabel = new JLabel("Starting Direction:");
        _currDirLabel = new JLabel(_robotDir);
        
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 32));
        startPosRowLabel.setFont(font);
        startPosColLabel.setFont(font);
        robotDirLabel.setFont(font);
        _currDirLabel.setFont(new Font("Arial", Font.BOLD, 28));
        
        JButton currDirBtn = new JButton("Change Robot Direction");
        currDirBtn.setFont(new Font("Arial", Font.BOLD, 18));
        currDirBtn.setMargin(new Insets(5, 10, 5, 10));
        currDirBtn.setFocusPainted(false);

        currDirBtn.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				// Create a JOptionPane for selecting robot direction
		        Object [] dirOptions = {_dirOptNorth, _dirOptEast,
		        		_dirOptSouth, _dirOptWest};
		        
		        String newRobotDir = (String) JOptionPane.showInputDialog(
                        StartStateDialog.this,
                        "Select the starting direction",
                        "Robot Direction",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        dirOptions,
                        dirOptions[0]);
		        
		        if(newRobotDir != null) {
		        	_robotDir = newRobotDir;
		        	_currDirLabel.setText(_robotDir);
		        }
			}
		});
        
        Object [] array = {infoLabel, new JLabel("\n\n"), startPosRowLabel,
        		_startPosRowTf, startPosColLabel, _startPosColTf,
        		robotDirLabel, _currDirLabel, currDirBtn};
		Object [] options = {_btnString1, _btnString2};
 
        // Create the JOptionPane
        _optionPane = new JOptionPane(array,
                                    JOptionPane.PLAIN_MESSAGE,
                                    JOptionPane.YES_NO_OPTION,
                                    null,
                                    options,
                                    options[0]);
 
        // Make this dialog display it
        setContentPane(_optionPane);
 
		// Handle window closing correctly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property
				 */
				_optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});
 
        // Ensure the text field always gets the first focus
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
            	_startPosRowTf.requestFocusInWindow();
            }
        });
 
        // Register an event handler that reacts to option pane state changes
        _optionPane.addPropertyChangeListener(this);
    }
 
	/** This method reacts to state changes in the option pane */
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();

		if (isVisible()
				&& (e.getSource() == _optionPane)
				&& (JOptionPane.VALUE_PROPERTY.equals(prop) ||
						JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
			Object value = _optionPane.getValue();

			if (value == JOptionPane.UNINITIALIZED_VALUE) {
				// Ignore reset
				return;
			}

			// Reset the JOptionPane's value.
			// If you don't do this, then if the user
			// presses the same button next time, no
			// property change event will be fired.
			_optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			
			// User clicked on the 'Save changes' button
			if (_btnString1.equals(value)) {
				
				if (isValidRow(_startPosRowTf.getText()) &&
						isValidCol(_startPosColTf.getText())) {
					
					_startPosRowText = _startPosRowTf.getText();
					_startPosColText = _startPosColTf.getText();
					
					setVisible(false);
				}
				else {
					// Invalid row/col
					JOptionPane.showMessageDialog(
									StartStateDialog.this,
									"Please enter valid row/col"
									+ " values in the text field!",
									"Try again", JOptionPane.ERROR_MESSAGE);
					
					_startPosRowTf.requestFocusInWindow();
				}
			}
			else {
				
				// User clicked on 'Cancel' button
				setVisible(false);
			}
		}
	}
    
    public String getStartPosRow() {
    	return _startPosRowText;
    }
    
    public String getStartPosCol() {
    	return _startPosColText;
    }
    
    public String getStartDirection() {
    	return _robotDir;
    }
    
    public void setStartPosRow(int startPosRow) {
    	_startPosRowText = Integer.toString(startPosRow);
    	_startPosRowTf.setText(_startPosRowText);
    }
    
    public void setStartPosCol(int startPosCol) {
    	_startPosColText = Integer.toString(startPosCol);
    	_startPosColTf.setText(_startPosColText);
    }
    
    public void setStartDirection(RobotConstants.DIRECTION startDir) {
    	_robotDir = startDir.toString();
    	_currDirLabel.setText(_robotDir);
    }
    
    /** Ensures entered row is within allowed starting area */
    private boolean isValidRow(String input) {
        try {
        	// Remaining 'size' after taking into consideration the reference grid
    		int robotSize = RobotConstants.ROBOT_SIZE - 1;
    		
            int row = Integer.parseInt(input);
			if ((row < 1) || ((row + robotSize) > (MapConstants.MAP_ROWS - 2)))
				return false;
            
            return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }
    
    /** Ensures entered column is within allowed starting area */
    private boolean isValidCol(String input) {
        try {
        	// Remaining 'size' after taking into consideration the reference grid
    		int robotSize = RobotConstants.ROBOT_SIZE - 1;
    		
            int col = Integer.parseInt(input);
			if ((col < 1)
					|| ((col + robotSize) > ((MapConstants.MAP_COLS - 2) / 2)))
				return false;

			return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }
}
