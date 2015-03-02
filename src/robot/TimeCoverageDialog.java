package robot;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;

import java.beans.*; 				// Property change stuff
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;

@SuppressWarnings("serial")
public class TimeCoverageDialog extends JDialog implements PropertyChangeListener,
	ItemListener {
	
    private JTextField _userSelectedSpeedTf = null;
    private JTextField _coverageTf = null;
    private JTextField _timeLimitTf = null;
    
    private String _userSelectedSpeedText = Integer.toString(
    		RobotConstants.DEFAULT_STEPS_PER_SECOND);
    private String _coverageText = Integer.toString(
    		RobotConstants.DEFAULT_COVERAGE_LIMIT);
    private String _timeLimitText = Integer.toString(
    		RobotConstants.DEFAULT_TIME_LIMIT);
    
    private JOptionPane _optionPane;
 
    private String _btnString1 = "Save";
    private String _btnString2 = "Cancel";
    
    private JCheckBox _coverageCb = null;
    private JCheckBox _timeLimitCb = null;
    
    private boolean _bCoverageLimited = false;
    private boolean _bTimeLimited = false;
 
    /** Creates the reusable dialog */
    public TimeCoverageDialog(Frame frame) {
        super(frame, true);
        setTitle("Time and coverage-limited exploration");
 
        _userSelectedSpeedTf = new JTextField(10);
        _coverageTf = new JTextField(10);
        _timeLimitTf = new JTextField(10);
        
        Font font = new Font("Arial", Font.PLAIN, 28);
        _userSelectedSpeedTf.setFont(font);
        _coverageTf.setFont(font);
        _timeLimitTf.setFont(font);
 
        JLabel userSelectedSpeedLabel = new JLabel("Robot speed (In steps per second):");
        JLabel coverageLabel = new JLabel("Coverage (In % of the maze):");
        JLabel timeLimitLabel = new JLabel("Time Limit (In seconds):");
        
        userSelectedSpeedLabel.setFont(font);
        coverageLabel.setFont(font);
        timeLimitLabel.setFont(font);
        
        _coverageCb = new JCheckBox("Coverage-limited Exploration");
        _coverageCb.setFont(font);
        _coverageCb.setSelected(false);
        
        _timeLimitCb = new JCheckBox("Time-limited Exploration");
        _timeLimitCb.setFont(font);
        _timeLimitCb.setSelected(false);
        
        _coverageCb.addItemListener(this);
        _timeLimitCb.addItemListener(this);
        
        // Create an array of the text and components to be displayed
        Object [] array = {userSelectedSpeedLabel, _userSelectedSpeedTf, coverageLabel,
        		_coverageTf, timeLimitLabel, _timeLimitTf, _coverageCb, _timeLimitCb};
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
            	_userSelectedSpeedTf.requestFocusInWindow();
            }
        });
 
        // Register an event handler that reacts to option pane state changes
        _optionPane.addPropertyChangeListener(this);
    }
    
    /** Listens to the check boxes. */
    public void itemStateChanged(ItemEvent e) {
    	
        Object source = e.getItemSelectable();
 
        if (source == _coverageCb) {
            if(e.getStateChange() == ItemEvent.DESELECTED) {
            	_bCoverageLimited = false;
            }
            else if(e.getStateChange() == ItemEvent.SELECTED) {
            	_bCoverageLimited = true;
            }
        }
        else if (source == _timeLimitCb) {
        	if(e.getStateChange() == ItemEvent.DESELECTED) {
            	_bTimeLimited = false;
            }
            else if(e.getStateChange() == ItemEvent.SELECTED) {
            	_bTimeLimited = true;
            }
        }
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
				
				// User clicked on the 'Save' button
				if (isValidSpeed(_userSelectedSpeedTf.getText()) &&
						isValidCoverage(_coverageTf.getText()) &&
						isValidTimeLimit(_timeLimitTf.getText())) {
					
					_userSelectedSpeedText = _userSelectedSpeedTf.getText();
					_coverageText = _coverageTf.getText();
					_timeLimitText = _timeLimitTf.getText();
					
					setVisible(false);
				}
				else {
					
					if(!isValidSpeed(_userSelectedSpeedTf.getText())) {
						
						JOptionPane.showMessageDialog(
								TimeCoverageDialog.this,
								"Please enter a valid speed! Valid values: [1, 100]",
								"User-selected Speed", JOptionPane.ERROR_MESSAGE);
				
						_userSelectedSpeedTf.requestFocusInWindow();
					}
					else if(!isValidCoverage(_coverageTf.getText())) {
						
						JOptionPane.showMessageDialog(
								TimeCoverageDialog.this,
								"Please enter a valid coverage limit! Valid values: [1, 100]",
								"Coverage Limit", JOptionPane.ERROR_MESSAGE);
				
						_coverageTf.requestFocusInWindow();
					}
					else if(isValidTimeLimit(_timeLimitTf.getText())) {
						
						JOptionPane.showMessageDialog(
								TimeCoverageDialog.this,
								"Please enter a valid time limit! Valid values: [1, 360]",
								"Time Limit", JOptionPane.ERROR_MESSAGE);
				
						_timeLimitTf.requestFocusInWindow();
					}
				}
			}
			else {
				
				// User clicked on 'Cancel' button
				setVisible(false);
			}
		}
	}
    
    public String getUserSelectedSpeed() {
    	return _userSelectedSpeedText;
    }
    
    public String getCoverageLimit() {
    	return _coverageText;
    }
    
    public String getTimeLimit() {
    	return _timeLimitText;
    }
    
    public boolean isCoverageLimited() {
    	return _bCoverageLimited;
    }
    
    public boolean isTimeLimited() {
    	return _bTimeLimited;
    }
    
    public void setUserSelectedSpeed(int userSelectedSpeed) {
    	_userSelectedSpeedText = Integer.toString(userSelectedSpeed);
    	_userSelectedSpeedTf.setText(_userSelectedSpeedText);
    }
    
    public void setCoverageLimit(int coverageLimit) {
    	_coverageText = Integer.toString(coverageLimit);
    	_coverageTf.setText(_coverageText);
    }
    
    public void setTimeLimit(int timeLimit) {
    	_timeLimitText = Integer.toString(timeLimit);
    	_timeLimitTf.setText(_timeLimitText);
    }
    
    public void setCoverageLimited(boolean bCoverageLimited) {
    	_bCoverageLimited = bCoverageLimited;
    	_coverageCb.setSelected(bCoverageLimited);
    }
    
    public void setTimeLimited(boolean bTimeLimited) {
    	_bTimeLimited = bTimeLimited;
    	_timeLimitCb.setSelected(bTimeLimited);
    }
    
    /** Ensures entered speed is achievable (Allowed: 1 - 100 steps per second) */
    private boolean isValidSpeed(String input) {
        try {
        	
            int speed = Integer.parseInt(input);
			if (speed <= 0 || speed > 100)
				return false;

			return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }
    
    /** Ensures entered coverage is reasonable (Allowed: 1% - 100%) */
    private boolean isValidCoverage(String input) {
        try {
        	
            int coverage = Integer.parseInt(input);
			if (coverage <= 0 || coverage > 100)
				return false;

			return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }
    
    /** Ensures entered time limit is reasonable (Allowed: 1s - 360s) */
    private boolean isValidTimeLimit(String input) {
        try {
        	
            int timeLimit = Integer.parseInt(input);
			if (timeLimit <= 0 || timeLimit > 360)
				return false;

			return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }
}
