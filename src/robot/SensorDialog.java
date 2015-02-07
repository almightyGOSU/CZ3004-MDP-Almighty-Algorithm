package robot;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;

import java.beans.*; 				// Property change stuff
import java.awt.Font;
import java.awt.event.*;


@SuppressWarnings("serial")
public class SensorDialog extends JDialog implements ActionListener,
		PropertyChangeListener {
	
    private JTextField _minRangeTf = null;
    private JTextField _maxRangeTf = null;
    
    private String _minRangeText = null;
    private String _maxRangeText = null;
    
    private JOptionPane optionPane;
 
    private String btnString1 = "Add";
    private String btnString2 = "Cancel";
 
    /** Creates the reusable dialog */
    public SensorDialog() {
        super();
        setTitle("Add new sensor");
 
        _minRangeTf = new JTextField(10);
        _maxRangeTf = new JTextField(10);
        
        Font tfFont = new Font("Arial", Font.PLAIN, 28);
        _minRangeTf.setFont(tfFont);
        _maxRangeTf.setFont(tfFont);
 
        // Create an array of the text and components to be displayed
        JLabel minRangeLabel = new JLabel("Minimum Range:");
        JLabel maxRangeLabel = new JLabel("Maximum Range:");
        JLabel sensorDirLabel = new JLabel("Sensor Direction:");
        
        Font labelFont = new Font("Arial", Font.BOLD, 28);
        minRangeLabel.setFont(labelFont);
        maxRangeLabel.setFont(labelFont);
        sensorDirLabel.setFont(labelFont);
        
        Object[] array = {minRangeLabel, _minRangeTf, maxRangeLabel,
        		_maxRangeTf, sensorDirLabel};
		Object[] options = { btnString1, btnString2 };
 
        // Create the JOptionPane
        optionPane = new JOptionPane(array,
                                    JOptionPane.PLAIN_MESSAGE,
                                    JOptionPane.YES_NO_OPTION,
                                    null,
                                    options,
                                    options[0]);
 
        // Make this dialog display it
        setContentPane(optionPane);
 
		// Handle window closing correctly
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property
				 */
				optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});
 
        // Ensure the text field always gets the first focus
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
            	_minRangeTf.requestFocusInWindow();
            }
        });
 
        // Register an event handler that puts the text into the option pane
        _minRangeTf.addActionListener(this);
        _maxRangeTf.addActionListener(this);
 
        // Register an event handler that reacts to option pane state changes
        optionPane.addPropertyChangeListener(this);
    }
 
    /** This method handles events for the text field */
    public void actionPerformed(ActionEvent e) {

    	if(e.getSource().equals(_minRangeTf)) {
    		
    	}
    	else if(e.getSource().equals(_maxRangeTf)) {
    		
    	}
    	else {
    		// Do nothing
    	}
    }
 
	/** This method reacts to state changes in the option pane */
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();

		if (isVisible()
				&& (e.getSource() == optionPane)
				&& (JOptionPane.VALUE_PROPERTY.equals(prop) ||
						JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
			Object value = optionPane.getValue();

			if (value == JOptionPane.UNINITIALIZED_VALUE) {
				// Ignore reset
				return;
			}

			// Reset the JOptionPane's value.
			// If you don't do this, then if the user
			// presses the same button next time, no
			// property change event will be fired.
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			
			if (btnString1.equals(value)) {
				
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
        setVisible(false);
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
