package gui;

import java.io.File;

/**
 * Get a root directory for file and folder searches. 
 * @author Doug Gillespie
 *
 */
public interface RootProvider {

	/**
	 * Get a root folder. 
	 * @return root folder. 
	 */
	public File getRoot();
}
