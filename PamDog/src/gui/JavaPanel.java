package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import pamdog.DogParams;

public class JavaPanel implements DogDialogPanel {

	private JPanel mainPanel;
	private FileDialogPanel javaPanel;
	private JTextField minMem, maxMem;
	private DogDialog dogDialog;
	private JTextField otherVMOptions;
	public JavaPanel(DogDialog dogDialog) {
		this.dogDialog = dogDialog;
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Chose Java version and VM options"));
		JPanel northPanel = new JPanel(new GridBagLayout());
		mainPanel.add(BorderLayout.NORTH, northPanel);
		javaPanel = new FileDialogPanel(dogDialog, "Java executable", "java.exe", 30);
		GridBagConstraints c = new DogContraints();
		c.gridwidth = 4;
		northPanel.add(javaPanel.getComponent(), c);
		c.gridy++;
		c.gridwidth = 1;
		northPanel.add(new JLabel("Min VM Memory ", SwingConstants.RIGHT), c);
		c.gridx++;
		northPanel.add(minMem = new JTextField(4), c);
		c.gridx++;
		northPanel.add(new JLabel(" Mega bytes", SwingConstants.LEFT), c);
		c.gridx = 0;
		c.gridy++;
		northPanel.add(new JLabel("Max VM Memory ", SwingConstants.RIGHT), c);
		c.gridx++;
		northPanel.add(maxMem = new JTextField(4), c);
		c.gridx++;
		northPanel.add(new JLabel(" Mega bytes", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridwidth = 4;
		c.gridx = 0;
		northPanel.add(new JLabel("Other VM command line options", SwingConstants.LEFT), c);
		c.gridy++;
		northPanel.add(otherVMOptions = new JTextField(35), c);
		
		String tip = "<html>Select a Java executable (java.exe) installed on your computer. This will usually<br>"
				+ "be found in C:\\Program Files\\Java\\jre****\\bin\\java.exe for the 64 bit<br>"
				+ "or in C:\\Program Files (x86)\\Java\\jre****\\bin for the 32 bit version.<br>"
				+ "For advanced functionality, use the java.exe found in a jdk**** instead of a jre**** folder.";
		javaPanel.setToolTipText(tip);
		minMem.setToolTipText("For 32 bit Java this can be up to 1024m, for 64 bit Java up to 8192m");
		maxMem.setToolTipText(minMem.getToolTipText());
		otherVMOptions.setToolTipText("Other options for the Java virtual machine (generally not needed)");
		
	}

	@Override
	public Component getComponent() {
		return mainPanel;
	}

	@Override
	public String getTitle() {
		return "Java";
	}

	@Override
	public void setParams(DogParams dogParams) {
		javaPanel.setFile(dogParams.getJre());
		minMem.setText(new Integer(dogParams.getMsMemory()).toString());
		maxMem.setText(new Integer(dogParams.getMxMemory()).toString());
		otherVMOptions.setText(dogParams.getOtherVMOptions());
	}

	@Override
	public boolean getParams(DogParams dogParams) {
		if (javaPanel.exists() == false) {
			return dogDialog.showWarning("You must select a valid java executable file");
		}
		dogParams.setJre(javaPanel.getFile());
		try {
			dogParams.setMsMemory(Integer.valueOf(minMem.getText()));
			dogParams.setMxMemory(Integer.valueOf(maxMem.getText()));
		}
		catch (NumberFormatException e) {
			return dogDialog.showWarning("You must specify integer min and max vm memories (up to 8192 Mega bytes)");
		}
		if (dogParams.getMxMemory() < dogParams.getMsMemory()) {
			return dogDialog.showWarning("You must specify valid min and max vm memories (up to 8192 Mega bytes)");
		}
		if (dogParams.getMxMemory() > 8192) {
			return dogDialog.showWarning("You must specify valid min and max vm memories (up to 8192 Mega bytes)");
		}
		dogParams.setOtherVMOptions(otherVMOptions.getText());
		return true;
	}

}
