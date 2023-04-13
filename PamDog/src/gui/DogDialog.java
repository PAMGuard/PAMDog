package gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import pamdog.DogControl;
import pamdog.DogParams;
import pamdog.DogVersion;

/**
 * dialog for controlling Watchdog. Has several panels of options
 * laid out in a pretty similar way to the PAMguard dialogs. 
 * @author Doug Gillespie
 *
 */
public class DogDialog extends JDialog {

	private static DogDialog singleInstance;
	
	private DogParams dogParams;

	private JButton okButton;

	private JButton cancelButton;
	
	private JButton helpButton;

	private ArrayList<DogDialogPanel> dialogPanels = new ArrayList<>();

	private JTabbedPane tabbedPane;

	private DogControl dogControl;

	private DogDialog(DogControl dogControl) {
		super();
		this.dogControl = dogControl;
		setTitle("PAMGuard Watchdog V" + DogVersion.version);
		setIconImage(PamDogGUI.getIconImageLarge());
		setLocation(100, 100);
		
		JPanel dPanel = new JPanel(new BorderLayout());
		JPanel sPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(new EmptyBorder(0, 0, 5, 5));
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(okButton = new JButton("Ok"));
		buttonPanel.add(cancelButton = new JButton("Cancel"));
		buttonPanel.add(helpButton = new JButton("Help..."));
		okButton.addActionListener(new OkAction());
		cancelButton.addActionListener(new CancelAction());
		helpButton.addActionListener(new HelpAction());
		sPanel.add(BorderLayout.EAST, buttonPanel);
		dPanel.add(BorderLayout.SOUTH, sPanel);
		setContentPane(dPanel);
		
		JPanel mainPanel = new JPanel();
		tabbedPane = new JTabbedPane();
		mainPanel.add(BorderLayout.NORTH, tabbedPane);
		addPanel(new ControlPanel(this));
		addPanel(new PamguardPanel(this));
		addPanel(new JavaPanel(this));
		addPanel(new RemotePanel(dogControl, this));
		dPanel.add(BorderLayout.CENTER, mainPanel);
		
		

		pack();
		setLocation(300, 200);
		this.setModal(true);
		this.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
		this.setResizable(false);
		setAlwaysOnTop(true);
	}
	
	/**
	 * 
	 * @return reference to the main dialog. 
	 */
	public static DogDialog getDogDialog() {
		return singleInstance;
	}

	public static DogParams showDialog(DogParams dogParams, DogControl dogControl) {
		if (singleInstance == null) {
			singleInstance = new DogDialog(dogControl);
		}
		singleInstance.dogParams = dogParams.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.dogParams;
	}
	private void setParams() {
		for (DogDialogPanel dp:dialogPanels) {
			dp.setParams(dogParams);
		}
	}
	
	public boolean getParams() {
		for (DogDialogPanel dp:dialogPanels) {
			if (dp.getParams(dogParams) == false) {
				return false;
			}
		}
		return true;
	}

	private class OkAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			okAction();
		}
	}

	public void okAction() {
		if (getParams()) {
			setVisible(false);
		}
		
	}

	private class CancelAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			cancelAction();
		}
	}

	private class HelpAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			DogHelp.getHelp().showHelp();
		}
	}

	public void cancelAction() {
		dogParams = null;
		setVisible(false);
	}
	
	private void addPanel(DogDialogPanel dogDialogPanel) {
		dialogPanels.add(dogDialogPanel);
		tabbedPane.add(dogDialogPanel.getComponent(), dogDialogPanel.getTitle());
	}
	/**
	 * Display a warning message with a default title
	 * @param warningText text
	 * @return false so these can be a single return line in dialog getParams funcs. 
	 */
	public boolean showWarning(String warningText) {
		return showWarning("PAMGuard watchdog", warningText);
	}
		
	public static boolean showWarning(Window owner, String warningTitle, String warningText) {
		JOptionPane.showMessageDialog(owner, warningText, warningTitle, JOptionPane.ERROR_MESSAGE);
		return false;
	}
	/**
	 * Display a warning message with given title and text
	 * @param warningTitle title of warning dialog
	 * @param warningText message of warning dialog
	 * @return false so these can be a single return line in dialog getParams funcs. 
	 */
	public boolean showWarning(String warningTitle, String warningText) {
		return showWarning(getOwner(), warningTitle, warningText);
	}

	/**
	 * @return the dogControl
	 */
	public DogControl getDogControl() {
		return dogControl;
	}

	/**
	 * @return the dogParams
	 */
	public DogParams getDogParams() {
		return dogParams;
	}

}
