package gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * dialog panel for finding files or folders. 
 * @author Doug Gillespie
 *
 */
public class FileDialogPanel {

	private ArrayList<String> fileMasks = new ArrayList<>();
	private boolean searchFolders;
	private JTextField file;
	private JButton browse;
	private int fieldLength;
	private RootProvider rootProvider;

	private JPanel mainPanel;
	private String title;
	private Component parent;

	public FileDialogPanel(Component parent, String title, String fileMask, int fieldLength) {
		this.parent = parent;
		this.title = title;
		if (fileMask != null) {
			this.fileMasks.add(fileMask);
		}
		this.fieldLength = fieldLength;
		makePanel();
	}

	public FileDialogPanel(Component parent, String title, boolean searchFolders, int fieldLength) {
		this.parent = parent;
		this.title = title;
		this.searchFolders = searchFolders;
		this.fieldLength = fieldLength;
		makePanel();
	}

	private void makePanel() {
		mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new DogContraints();
		c.anchor = GridBagConstraints.SOUTHWEST;
		mainPanel.add(new JLabel(title), c);
		c.gridx += 2;
		c.anchor = GridBagConstraints.SOUTHEAST;
//		mainPanel.add(browse = new JButton("Browse"), c);
//		browse.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(file = new JTextField(fieldLength), c);
		c.gridx+=c.gridwidth;
		mainPanel.add(browse = new JButton("Browse"), c);
		browse.addActionListener(new BrowseAction());

	}

	public Component getComponent() {
		return mainPanel;
	}

	public void setFile(String path) {
		file.setText(path);
	}

	public String getFile() {
		return file.getText();
	}
	
	public boolean exists() {

		String wd = getFile();
		if (wd == null) {
			return false;
		}
		File wdf = new File(wd);
		if (wdf.exists()) {
			return true;
		}
		// if that didn't work, try concatonating it with the root. 
		if (rootProvider != null) {
			wd = rootProvider.getRoot().getAbsolutePath() + File.separator + wd;
			wdf = new File(wd);
			return wdf.exists();
		}
		return false;
	}

	private class BrowseAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			browseFiles();
		}
	}
	
	/**
	 * Add an extra file mask to the list ...
	 * @param fileMask
	 */
	public void addFileMask(String fileMask) {
		fileMasks.add(fileMask);
	}

	public void browseFiles() {
		JFileChooser fileChooser = new JFileChooser();
		DogFileFilter fileFilter = new DogFileFilter(searchFolders);
		for (String aMask:fileMasks) {
			fileFilter.addMask(aMask);
		}
		fileChooser.setFileFilter(fileFilter);
		fileChooser.setCurrentDirectory(new File(file.getText()));
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setDialogTitle(title);
		if (rootProvider != null) {
			fileChooser.setCurrentDirectory(rootProvider.getRoot());
		}
		if (searchFolders) {
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		else {
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		}
		fileChooser.setSelectedFile(new File(file.getText()));

		int state= fileChooser.showOpenDialog(parent);
		if (state == JFileChooser.APPROVE_OPTION) {
			file.setText(stripRoot(fileChooser.getSelectedFile()));
		}
	}

	/**
	 * @return the rootProvider
	 */
	public RootProvider getRootProvider() {
		return rootProvider;
	}

	/**
	 * @param rootProvider the rootProvider to set
	 */
	public void setRootProvider(RootProvider rootProvider) {
		this.rootProvider = rootProvider;
	}

	/**
	 * If a root is specified, try to strip it
	 * off the start of the file name. 
	 * @param file
	 * @return
	 */
	public String stripRoot(File file) {
		if (file == null) {
			return null;
		}
		if (rootProvider == null) {
			return file.getAbsolutePath(); 
		}
		File rootFile = rootProvider.getRoot();
		String root = "";
		if (rootFile != null) {
			root = rootFile.getAbsolutePath();
		}
		String path = file.getAbsolutePath();
		if (path.startsWith(root)) {
			path = path.substring(root.length());
			if (path.startsWith(File.separator)) {
				path = path.substring(1);
			}
		}
		return path;
	}
	
	/**
	 * Set a tooltip on the main editible file name window. 
	 * @param tip
	 */
	public void setToolTipText(String tip) {
		file.setToolTipText(tip);
	}

}
