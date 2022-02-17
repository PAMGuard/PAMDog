package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import pamdog.DogParams;

public class PamguardPanel implements DogDialogPanel, RootProvider {

	private JPanel mainPanel;
	private FileDialogPanel workingDirectory;
	private FileDialogPanel pamguardJar;
	private FileDialogPanel psfFile;
	private FileDialogPanel libFolder;
	private DogDialog dogDialog; 
	private JTextField otherOptions;
	private JButton defaultsButton;
	
	public PamguardPanel(DogDialog dogDialog) {
		this.dogDialog = dogDialog;
		mainPanel = new JPanel();		
		mainPanel.setBorder(new TitledBorder("Chose PAMGuard version and configuration options"));
		workingDirectory = new FileDialogPanel(dogDialog, "Working Directory", true, 30);
		pamguardJar = new FileDialogPanel(dogDialog, "Pamguard executable jar", ".jar", 30);
		psfFile = new FileDialogPanel(dogDialog, "Pamguard configuration file", ".psf", 30);
		psfFile.addFileMask(".psfx");
		libFolder = new FileDialogPanel(dogDialog, "Library path", true, 30);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
//		workingDirectory.setRootProvider(this);
		pamguardJar.setRootProvider(this);
//		psfFile.setRootProvider(this);
		libFolder.setRootProvider(this);
		
		mainPanel.add(workingDirectory.getComponent());
		mainPanel.add(pamguardJar.getComponent());
		mainPanel.add(psfFile.getComponent());
		mainPanel.add(libFolder.getComponent());
		
		
		JPanel optspanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new DogContraints();
		optspanel.add(new JLabel("Other PAMGuard command line options", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridwidth = 3;
		optspanel.add(otherOptions = new JTextField(35), c);
		JPanel optsLeft = new JPanel(new BorderLayout());
		optsLeft.add(BorderLayout.WEST, optspanel);
		mainPanel.add(optsLeft);
		
		JPanel defaultsPanel = new JPanel(new BorderLayout());
		defaultsPanel.add(BorderLayout.WEST, defaultsButton = new JButton("Restore defaults"));
		mainPanel.add(defaultsPanel);

		workingDirectory.setToolTipText("Folder containing the PAMGuard executable jar file, e.g. C:\\Program Files\\Pamguard64");
		pamguardJar.setToolTipText("PAMGuard executable jar file, e.g. Pamguard_*_**_**.jar");
		psfFile.setToolTipText("PAMGuard configuration (*.psf) file");
		libFolder.setToolTipText("PAMGuard library path (usually lib for 32 bit and lib64 for 64 bit versions)");
		otherOptions.setToolTipText("Other PAMGuard options (generally not required)");
		defaultsButton.setToolTipText("Restore values to default settings from the current PAMGuard installation");
		
		defaultsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				restoreDefaults();
			}
		});
	}

	protected void restoreDefaults() {
		dogDialog.getDogControl().restorePamguardDefaults();		
		setParams(dogDialog.getDogControl().getParams());
	}

	@Override
	public Component getComponent() {
		return mainPanel;
	}

	@Override
	public void setParams(DogParams dogParams) {
		workingDirectory.setFile(dogParams.getWorkingFolder());
		pamguardJar.setFile(dogParams.getJavaFile());
		psfFile.setFile(dogParams.getPsfFile());
		libFolder.setFile(dogParams.getLibFolder());
		if (dogParams.getOtherOptions() == null) {
			otherOptions.setText("");
		}
		else {
			otherOptions.setText(dogParams.getOtherOptions());
		}
	}

	@Override
	public boolean getParams(DogParams dogParams) {
		if (workingDirectory.exists() == false) {
			return dogDialog.showWarning("You must specify a valid working directory");
		}
		dogParams.setWorkingFolder(workingDirectory.getFile());
		
		if (pamguardJar.exists() == false) {
			return dogDialog.showWarning("You must specify a valid executable PAMGuard jar file");
		}
		dogParams.setJavaFile(pamguardJar.getFile());

		if (psfFile.exists() == false) {
			return dogDialog.showWarning("You must specify a valid PAMGuard configuration file");
		}
		if (psfxFileCheck(psfFile, workingDirectory.getFile(), pamguardJar.getFile()) == false) {
			return false;
		};
		
		dogParams.setPsfFile(psfFile.getFile());
		
		if (libFolder.exists() == false) {
			return dogDialog.showWarning("You must specify a valid lib folder for PAMGuard C external libraries\n"
					+ "(Generally this is the lib folder in the same root as the executable PAMGuard jar file)");
		}
		if (libFolder.getFile().contains(" ")) {
			return dogDialog.showWarning("The library path must not contain spaces.\n"
				+ "A valid work around is to specify the libraries parent folder as the root folder (which can contain speces)"
				+ "and then set a relative path to the actual library folder");
		}
		dogParams.setLibFolder(libFolder.getFile());
		dogParams.setOtherOptions(otherOptions.getText());
		return true;
	}
	

	private boolean psfxFileCheck(FileDialogPanel psfFile, String workingFolder, String pamguardName) {
		/*
		 * psfx files will only work with recent versions of PAMGuard beta....
		 * So check for Beta in the file path and for 
		 */
		if (psfFile == null) return false;
		if (psfFile.exists() == false) return false;
		String psfPath = psfFile.getFile();
		if (psfPath.endsWith(".psf")) return true;
		if (psfPath.endsWith(".psfx") == false) return false; // this should never happen !!!
		
		File pamguardFile = new File(workingFolder, pamguardName);
		if (pamguardName == null || pamguardFile.exists() == false) {
			return dogDialog.showWarning("Note that psfx files are only compatible with recent Beta versions of PAMGuard");
		}
		String fullPGName = pamguardFile.getAbsolutePath();
		boolean isBeta = fullPGName.toLowerCase().contains("beta");
		long fileDate = pamguardFile.lastModified();
		long firstpsfx = 1535587200000L; // 30 august 2018
		long lastMod = pamguardFile.lastModified();
		boolean dateOk =  lastMod >= firstpsfx;
		if (isBeta && dateOk) {
			return true;
		}
		else {
			 dogDialog.showWarning("Warning: psfx files are only compatible with recent Beta versions of PAMGuard");
		}
		return true;
	}

	@Override
	public String getTitle() {
		return "Pamguard";
	}

	@Override
	public File getRoot() {
		return new File(workingDirectory.getFile());
	}

}
