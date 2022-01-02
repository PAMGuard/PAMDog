package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import Logging.DogLog;
import pamdog.DogParams;

public class ControlPanel implements DogDialogPanel {

	private DogDialog dogDialog;

	private JPanel mainPanel;
	
	private JCheckBox activeDog;
	
	private JCheckBox allowRestarts;
	
	private JTextField udpPort;
	
	private JCheckBox broadcastErrors;
	
	private JTextField udpPortErrors;

	private JLabel broadcastLabel;
	
	private JTextField minRestartMinutes;
	private JLabel lastRestartTime;
	
	public ControlPanel(DogDialog dogDialog) {
		this.dogDialog = dogDialog;
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Turn PAM Dog on and off"));
		JPanel westPanel = new JPanel(new BorderLayout());
		JPanel northPanel = new JPanel(new GridBagLayout());
		westPanel.add(BorderLayout.NORTH, northPanel);
		mainPanel.add(westPanel, BorderLayout.WEST);
		GridBagConstraints c = new DogContraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		northPanel.add(activeDog = new JCheckBox("Activate Watchdog"), c);
		activeDog.setToolTipText("Checking this box will enable the watchdog, which will keep PAMGuard running whatever may happen");
		activeDog.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				activateWatchdog();
			}
		});
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy++;
		northPanel.add(new JLabel("UDP Port (Communications) ", SwingConstants.RIGHT), c);
		c.gridx++;
		northPanel.add(udpPort = new JTextField(4), c);
		udpPort.setToolTipText("Port number for UDP communications between the watchdog and PAMGuard");
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		northPanel.add(broadcastErrors = new JCheckBox("Broadcast commands/errors to UDP port"), c);
		String brd = "Broadcast error messages and commands to UDP port";
		broadcastErrors.setToolTipText(brd);
		broadcastErrors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleBroadcast();
			}
		});
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy++;
		broadcastLabel = new JLabel("UDP Port (Command logging) ", SwingConstants.RIGHT);
		northPanel.add(broadcastLabel, c);
		c.gridx++;
		northPanel.add(udpPortErrors = new JTextField(4), c);
		udpPortErrors.setToolTipText("Port number for UDP broadcast of Pamguard commands and error messages");
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		northPanel.add(allowRestarts = new JCheckBox("Allow system restarts"), c);
		String srt = "<html>Checking this box will cause PAMDog to restart your system should PAMGuard fail to "
				+ "start multiple times.<br>"
				+ "Note that you must ensure that your system does not require a password on startup for this option "
				+ "to be used (Windows only)</html>";
		allowRestarts.setToolTipText(srt);
		allowRestarts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				allowRestarts();
			}
		});
		c.gridx = 0;
		c.gridy ++;
		c.gridwidth = 1;
		minRestartMinutes = new JTextField(4);
		northPanel.add(new JLabel("Minimum time between system restarts ", JLabel.RIGHT), c);
		c.gridx++;
		northPanel.add(minRestartMinutes, c);
		c.gridx++;
		northPanel.add(new JLabel(" minutes"), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		northPanel.add(lastRestartTime = new JLabel(), c);
		
		
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy++;
		JButton logButton = new JButton("Open log file folder");
		northPanel.add(logButton, c);
		String tip = "<html>PAMGuard terminal output and PAMDog log files are stored in the folder<br>"
				+ DogLog.getLogFolder() + "</html>";
		logButton.setToolTipText(tip);
		logButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openDogLogFolder();
			}
		});
		
//		c.gridx = 0;
//		c.gridwidth = 2;
//		c.gridy++;
////		JButton restartButton = new JButton("Restart Test");
//		northPanel.add(restartButton, c);
//		restartButton.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				restartPC();
//			}
//		});
	}
	
	/**
	 * 
	 */
	protected void toggleBroadcast() {
		udpPortErrors.setEnabled(broadcastErrors.isSelected());
		broadcastLabel.setEnabled(broadcastErrors.isSelected());
	}

	protected void openDogLogFolder() {
		String cmd = "explorer.exe /root," + DogLog.getLogFolder();
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void allowRestarts() {
		enableControls();
	}

	protected void activateWatchdog() {
		boolean isActive = activeDog.isSelected();
		if (isActive) {
			if (dogDialog.getParams() == false) {
				dogDialog.showWarning("Error", "PamDog Cannot be activated since there is an error in the configuration");
				activeDog.setSelected(false);
				return;
			}
			else {
				dogDialog.getDogControl().setNewParams(dogDialog.getDogParams());
			}
		}
		
		dogDialog.getDogControl().activateWatchDog(isActive);
		enableControls();
	}

	private void enableControls() {
//		allowRestarts.setEnabled(activeDog.isSelected());
		udpPortErrors.setEnabled(broadcastErrors.isSelected());
		broadcastLabel.setEnabled(broadcastErrors.isSelected());
		minRestartMinutes.setEnabled(allowRestarts.isSelected());
	}
	
	void restartPC() {
		dogDialog.getDogControl().restartPC();
	}

	@Override
	public Component getComponent() {
		return mainPanel;
	}

	@Override
	public String getTitle() {
		return "Control";
	}

	@Override
	public void setParams(DogParams dogParams) {
		activeDog.setSelected(dogParams.isActiveDog());
		udpPort.setText(String.format("%d", dogParams.getUdpPort()));
		broadcastErrors.setSelected(dogParams.isBroadcastErrors());
		udpPortErrors.setText(String.format("%d", dogParams.getUdpPortErrors()));
		allowRestarts.setSelected(dogParams.isAllowSystemRestarts());
		minRestartMinutes.setText(String.format("%d", dogParams.getMinRestartMinutes()));
		long lastRestart = dogParams.getLastRestartTime();
		if (lastRestart == 0) {
			lastRestartTime.setText("          ");
		}
		else {
			lastRestartTime.setText(String.format("Last system restart %s UTC", DogLog.getLogDate(lastRestart)));
		}
		enableControls();
	}

	@Override
	public boolean getParams(DogParams dogParams) {
		dogParams.setActiveDog(activeDog.isSelected());
		dogParams.setBroadcastErrors(broadcastErrors.isSelected());
		try {
			dogParams.setUdpPort(Integer.valueOf(udpPort.getText()));
		}
		catch (NumberFormatException e) {
			return dogDialog.showWarning("Invalid UDP Communications Port");
		}
		try {
			dogParams.setUdpPortErrors(Integer.valueOf(udpPortErrors.getText()));
		}
		catch (NumberFormatException e) {
			return dogDialog.showWarning("Invalid Command-logging UDP Port");
		}
		try {
			dogParams.setMinRestartMinutes(Integer.valueOf(minRestartMinutes.getText()));
		}
		catch (NumberFormatException e) {
			return dogDialog.showWarning("Invalid Minimum restart time");
		}
		dogParams.setAllowSystemRestarts(allowRestarts.isSelected());
		return true;
	}

}
