package gui;

import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

public class DogFileFilter extends FileFilter {

	private ArrayList<String> masks = new ArrayList<>();
	private boolean acceptFolders;
	
	public DogFileFilter(String mask) {
		if (mask != null) {
			masks.add(mask.toLowerCase());
		}
	}
	
	public DogFileFilter(boolean acceptFolders) {
		this.acceptFolders = acceptFolders;
	}
	
	public void addMask(String mask) {
		masks.add(mask);
	}

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		for (String mask:masks) {
			if (file.getAbsolutePath().toLowerCase().endsWith(mask)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		String desc = "";
		int n = 0;
		for (String mask:masks) {
			if (n++ != 0) {
				desc += "; ";
			}
			desc += mask;
		}
		if (n == 0) return null;
		return desc;
	}

}
