package robot;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;

import java.beans.*; 				// Property change stuff
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.*;


@SuppressWarnings("serial")
public class SensorDialog extends JDialog implements PropertyChangeListener {
	
    private JTextField _minRangeTf = null;
    private JTextField _maxRangeTf = null;
    
    private String _minRangeText = null;
    private String _maxRangeText = null;
    
    private JOptionPane _optionPane;
 
    private String _btnString1 = "Add";
    private String _btnString2 = "Cancel";
    
    // For sensor direction
    private String _dirOptNorth = "North";
    private String _dirOptEast = "East";
    private String _dirOptSouth = "South";
    private String _dirOptWest = "West";
    private JLabel _currDirLabel = null;
    private String _sensorDir = _dirOptNorth;
 
    /** Creates the reusable dialog */
    public SensorDialog(Frame frame) {
        super(frame, true);
        setTitle("Add new sensor");
 
        _minRangeTf = new JTextField(10);
        _maxRangeTf = new JTextField(10);
        
        Font font = new Font("Arial", Font.PLAIN, 28);
        _minRangeTf.setFont(font);
        _maxRangeTf.setFont(font);
 
        // Create an array of the text and components to be displayed
        JLabel minRangeLabel = new JLabel("Minimum Range:");
        JLabel maxRangeLabel = new JLabel("Maximum Range:");
        JLabel sensorDirLabel = new JLabel("Sensor Direction:");
        _currDirLabel = new JLabel("North");
        
        minRangeLabel.setFont(font);
        maxRangeLabel.setFont(font);
        sensorDirLabel.setFont(font);
        _currDirLabel.setFont(new Font("Arial", Font.BOLD, 28));
        
        JButton currDirBtn = new JButton("Change Sensor Direction");
        currDirBtn.setFont(new Font("Arial", Font.BOLD, 18));
        currDirBtn.setMargin(new Insets(5, 10, 5, 10));
        currDirBtn.setFocusPainted(false);

        currDirBtn.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				
				// Create a JOptionPane for selecting sensor direction
		        Object [] dirOptions = {_dirOptNorth, _dirOptEast,
		        		_dirOptSouth, _dirOptWest};
		        
		        String newSensorDir = (String) JOptionPane.showInputDialog(
                        SensorDialog.this,
                        "Select the sensor direction",
                        "Sensor Direction",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        dirOptions,
                        dirOptions[0]);
		        
		        if(newSensorDir != null) {
		        	_sensorDir = newSensorDir;
		        	_currDirLabel.setText(_sensorDir);
		        }
			}
		});
        
        Object [] array = {minRangeLabel, _minRangeTf, maxRangeLabel,
        		_maxRangeTf, sensorDirLabel, _currDirLabel, currDirBtn};
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
            	_minRangeTf.requestFocusInWindow();
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
			
			if (_btnString1.equals(value)) {
				
				// User clicked on the 'Add' button
				_minRangeText = _minRangeTf.getText();
				_maxRangeText = _maxRangeTf.getText();
				
				if (isPosInteger(_minRangeText) &&
						isPosInteger(_maxRangeText)) {
					clearAndHide();
				}
				else {
					JOptionPane.showMessageDialog(
									SensorDialog.this,
									"Please enter positive integer"
									+ " values in the text field!",
									"Try again", JOptionPane.ERROR_MESSAGE);
					
					_minRangeText = null;
					_maxRangeText = null;
					
					_minRangeTf.requestFocusInWindow();
				}
			}
			else {
				// User closed dialog or clicked cancel
				_minRangeText = null;
				_maxRangeText = null;
				
				clearAndHide();
			}
		}
	}
	
	/** This method clears the dialog and hides it */
    public void clearAndHide() {
        _minRangeTf.setText(null);
        _maxRangeTf.setText(null);
        
        // Reset direction label to default direction (North)
        _currDirLabel.setText(_dirOptNorth);
        
        setVisible(false);
    }
    
    public String getMinRange() {
    	return _minRangeText;
    }
    
    public String getMaxRange() {
    	return _maxRangeText;
    }
    
    public String getDirection() {
    	return _sensorDir;
    }
    
    /**
     * Resets any previously stored direction
     */
    public void resetSensorDialog() {
    	_sensorDir = _dirOptNorth;
    }
    
    private boolean isPosInteger(String input) {
        try {
            int i = Integer.parseInt(input);
            if(i < 1)
            	return false;
            
            return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }
}
