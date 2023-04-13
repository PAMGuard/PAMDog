package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.NetworkInterface;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import pamdog.DogControl;
import pamdog.DogParams;
import pamdog.remote.NetInterfaceFinder;
import pamdog.remote.RemoteControlAgent;

public class RemotePanel implements DogDialogPanel {
	
	private DogControl dogControl;
	
	private DogDialog dogDialog;
	
	private JPanel mainPanel;
	
	private JComboBox<String> interfaceList;

	public RemotePanel(DogControl dogControl, DogDialog dogDialog) {
		super();
		this.dogControl = dogControl;
		this.dogDialog = dogDialog;
		
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Batch Process Control"));
		JPanel nPanel = new JPanel(new GridBagLayout());
		mainPanel.add(BorderLayout.NORTH, nPanel);
		GridBagConstraints c = new DogContraints();
		c.gridwidth = 2;
		nPanel.add(new JLabel("Control of batch processes from a remote machine", JLabel.LEFT), c);
		c.gridy++;
		nPanel.add(new JLabel("See PAMGuard Batch Contrller and help files for details", JLabel.LEFT), c);
		c.gridy++;
//		c.gridwidth = 1;
		nPanel.add(new JLabel("Network interface ", JLabel.LEFT), c);
		c.gridy++;
		nPanel.add(interfaceList = new JComboBox<String>(), c);
		fillList();
		interfaceList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectInterface();
			}
		});
		String tip = "This is the network device that is on the same subnet as the PAMGuard instance controlling batch processes";
		interfaceList.setToolTipText(tip);
	}

	protected void selectInterface() {
		NetworkInterface netInf = getSelectedInterface();
		RemoteControlAgent remoteAgent = dogControl.getRemoteControlAgent();
		dogControl.getParams().netInterfaceName = netInf == null ? null : netInf.getName();
		if (remoteAgent != null) {
			remoteAgent.restartRemoteInterface();
		}
	}

	@Override
	public Component getComponent() {
		return mainPanel;
	}

	@Override
	public String getTitle() {
		return "Batch";
	}

	private void fillList() {
		List<NetworkInterface> netInfs = NetInterfaceFinder.getIPV4Interfaces();
		for (int i = 0; i < netInfs.size(); i++) {
			interfaceList.addItem(netInfs.get(i).getDisplayName());
		}
	}
	
	@Override
	public void setParams(DogParams dogParams) {
		interfaceList.removeAllItems();
		List<NetworkInterface> netInfs = NetInterfaceFinder.getIPV4Interfaces();
		int selInd = -1;
		for (int i = 0; i < netInfs.size(); i++) {
			interfaceList.addItem(netInfs.get(i).getDisplayName());
			if (netInfs.get(i).getName().equals(dogParams.netInterfaceName)) {
				selInd = i;
			}
		}
		if (selInd >= 0) {
			interfaceList.setSelectedIndex(selInd);
		}
	}

	public NetworkInterface getSelectedInterface() {
		List<NetworkInterface> netInfs = NetInterfaceFinder.getIPV4Interfaces();
		int selInd = interfaceList.getSelectedIndex();
		if (selInd >= 0 && selInd < netInfs.size()) {
			return netInfs.get(selInd);
		}
		
		return null;
	}


	@Override
	public boolean getParams(DogParams dogParams) {
		NetworkInterface netInf = getSelectedInterface();
		if (netInf == null) {
			dogParams.netInterfaceName = null;
		}
		else {
			dogParams.netInterfaceName = netInf.getName();
		}
		// always true since this is not core operation. 
		return true; 
	}

}
