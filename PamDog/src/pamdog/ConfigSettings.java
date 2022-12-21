package pamdog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import javax.swing.JOptionPane;


/**
 * Functions for handling settings, including trying to find sensible defaults now that 
 * PAMDog is installed in same folder as PAMGuard with a proper launcher, etc. 
 * @author dg50
 *
 */
public class ConfigSettings {

	/**
	 * Save PamDog configuration
	 * @param dogParams configuration settings
	 * @return true if successful
	 */
	public boolean saveConfig(DogParams dogParams) {
		ObjectOutputStream os;
		try {
			os = new ObjectOutputStream(new FileOutputStream(getConfigFile()));
			os.writeObject(dogParams);
		} catch (Exception Ex) {
			System.out.println(Ex);
			return false;
		}
		try {
			os.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Load configuration settings
	 * @return Sonfiguration settings, or null if nothing to load. 
	 */
	public DogParams loadConfig() {
		DogParams dogParams = null;
		File conFile = getConfigFile();
		if (conFile.exists() == false) {
			System.out.println("No PamDog config file");
			return null;
		}
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(conFile));
			Object o = ois.readObject();
			if (o != null && DogParams.class == o.getClass()) {
				dogParams = (DogParams) o;
			}
			ois.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return dogParams;
	}
	
	/**
	 * Standard config file path
	 * @return
	 */
	private File getConfigFile() {
		String setFileName = getPamguardFolder() + File.separator + "PamDogSettings.pds";
		return new File(setFileName);
	}
	/**
	 * Get the settings folder name and if necessary, 
	 * create the folder since it may not exist. 
	 * @return folder name string, (with no file separator on the end)
	 */
	public static String getPamguardFolder() {
		String settingsFolder = System.getProperty("user.home");
		settingsFolder += File.separator + "Pamguard";
		// now check that folder exists
		File f = new File(settingsFolder);
		if (f.exists() == false) {
			f.mkdirs();
		}
		return settingsFolder;
	}
	
	/**
	 * Load default values for the working dir and 
	 * for the main PAMGUard executable. 
	 * @return true if both OK. 
	 */
	public boolean loadParmguardDefaults(DogParams dogParams) {
		DogParams iniParams = findIniSetings();
		if (iniParams == null || dogParams == null) {
			return false;
		}
		dogParams.setLibFolder(iniParams.getLibFolder());
		dogParams.setJavaFile(iniParams.getJavaFile());
		dogParams.setWorkingFolder(iniParams.getWorkingFolder());
		checkLibFolder(dogParams, iniParams);
		checkJarFile(dogParams, iniParams);
		checkWorkingDir(dogParams);
		return true;
	}

	/**
	 * Load defaults for the jre and vm options. 
	 * @return true if all ok
	 */
	public boolean loadJavaDefaults(DogParams dogParams) {
		DogParams iniParams = findIniSetings();
		if (iniParams == null || dogParams == null) {
			return false;
		}
		
		return true;
	}
	
	public boolean checkParams(DogParams dogParams) {
		boolean changes = false;
		DogParams iniParams = findIniSetings();
		checkWorkingDir(dogParams);
		checkJava(dogParams);
		if (iniParams != null) {
			checkJarFile(dogParams, iniParams);
			checkLibFolder(dogParams, iniParams);
		}
		return true;
	}
	
	/**
	 * Check the lib folder. This may be absolute, or may be relative to 
	 * currentdirectory (more likely). 
	 * @param dogParams 
	 * @param iniParams 
	 */
	private void checkLibFolder(DogParams dogParams, DogParams iniParams) {
		if (folderExists(dogParams.getLibFolder())) {
			return;
		}
		if (folderExists(getCurrentdirectory() + File.separator + dogParams.getLibFolder())) {
			return;
		}
		dogParams.setLibFolder(iniParams.getLibFolder());
	}

	/**
	 * Check the main PAMGuard Java executable file. 
	 * @param dogParams
	 * @param iniParams
	 */
	private void checkJarFile(DogParams dogParams, DogParams iniParams) {
		/*
		 * If there is a file and it exists, then leave it since it 
		 * may not be the default
		 */
		String jar = dogParams.getJavaFile();
		if (fileExists(jar)) {
			return;
		}
		/**
		 * Try to use the file name of the jar file in the ini files. 
		 */
		if (fileExists(iniParams.getJavaFile())) {
			dogParams.setJavaFile(iniParams.getJavaFile());
			return;
		}
		/**
		 * Or just try to find any bloody big jar file in the root folder 
		 */
		File anyFile = findFile(new File(getCurrentdirectory()), "*.jar", false, 200000000L);
		if (anyFile != null) {
			dogParams.setJavaFile(anyFile.getAbsolutePath());
			return;
		}
		
	}

	private void checkJava(DogParams dogParams) {
		String javaExe = dogParams.getJre();
		if (fileExists(javaExe)) {
			return;
		}
		File javaFile = findFile(new File(getCurrentdirectory()), "java.exe", true, 1);
		if (javaFile != null) {
			dogParams.setJre(javaFile.getAbsolutePath());
		}
	}
	

	private void checkWorkingDir(DogParams dogParams) {
		if (folderExists(dogParams.getWorkingFolder())) {
			return;
		}
		String currDir = getCurrentdirectory();
		if (folderExists(currDir)) {
			dogParams.setWorkingFolder(currDir);
			return;
		}
		return;
	}
	
	/**
	 * Check a folder exists. 
	 * @param path
	 * @return
	 */
	private boolean fileExists(String path) {
		if (path == null) {
			return false;
		}
		File fPath = new File(path);
		if (fPath.exists() && !fPath.isDirectory()) {
			return true;
		}
		else {
			return false;
		}
	}
	/**
	 * Check a folder exists. 
	 * @param path
	 * @return
	 */
	private boolean folderExists(String path) {
		if (path == null) {
			return false;
		}
		File fPath = new File(path);
		if (fPath.exists() && fPath.isDirectory()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private DogParams findIniSetings() {
		DogParams dogParams = new DogParams();
		File iniFile = findFile(new File(getCurrentdirectory()), "*ViewerMode.l4j.ini", false, 1);
		if (iniFile == null) {
			return null;
		}
		if (iniFile.exists() == false) {
			return null;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(iniFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("-Xms")) {
					Integer val = extractIntVal(line);
					if (val != null) {
						dogParams.setMsMemory(val);
					}
				}
				else if (line.contains("-Xmx")) {
					Integer val = extractIntVal(line);
					if (val != null) {
						dogParams.setMxMemory(val);
					}
				}
				else if (line.contains("-jar")) {
					String jarName = line.substring(4);
					jarName = jarName.replace('\"', ' ');
					jarName = jarName.trim();
					dogParams.setJavaFile(jarName);
				}
				else if (line.contains("-Djava.library.path")) {
					int lastSlash = line.lastIndexOf('\\');
					if (lastSlash > 0) {
						line = line.replace('\"', ' ');
						line = line.trim();
						dogParams.setLibFolder(line.substring(lastSlash+1));
					}
				}
			}			
		}
		catch (Exception e) {
			System.out.println("Error extracting dog values from ini file " + e.getMessage());
		}
		return dogParams;
	}
	
	/**
	 * Get the value of integer digits in a string
	 * @param line
	 * @return integer value therein. 
	 */
	private Integer extractIntVal(String line) {
		byte[] nb = line.getBytes();
		int f = 0;
		int l = line.length()-1;
		for (int i = 0; i < nb.length; i++) {
			if (nb[i] < '0' || nb[i] > '9') {
				f++;
			}
			else {
				break;
			}
		}
		for (int i = l; i >= 0; i--) {
			if (nb[i] < '0' || nb[i] > '9') {
				l--;
			}
			else {
				break;
			}			
		}
		if (l < f) {
			return null;
		}
		line =  line.substring(f, l+1);
		Integer val = null;
		try {
			val = Integer.valueOf(line);
		}
		catch (NumberFormatException e) {
			
		}
		
		return val;
	}

	private String getCurrentdirectory() {
		// current executable jar = System.out.println(Pamguard.class.getProtectionDomain().getCodeSource().getLocation());
		// curren dir = 		System.getProperty("user.dir"); 
		String currDir = System.getProperty("user.dir");
		/*
		 * the above gets the right folder, but for debugging it's going 
		 * be be easier to fudge it 
		 */
//		currDir = "C:\\Program Files\\Pamguard";
//		JOptionPane.showConfirmDialog(null, "Current dir is " + currDir);
		return currDir;
	}

	public File findFile(File root, String filePattern, boolean searchSubDirs, long minSize) {	/**
		 * Find a file. Ones we'll need to find automatically are the 
		 * PAMGuard main jar file and the the Java executable. PAMDog should now be
		 * running from the same root folder as the PAMGuard installation,so hopefully
		 * we at least know where to start the search now. 
		 * For the java runtime, it's always caled java.exe so it's just a case of 
		 * searching the folder structure (should be in C:\Program Files\Pamguard\jre64\bin)
		 * For the main PAMGuard jar it may be more complicated since it's name
		 * will vary with version number, e.g. Pamguard-2.02.03.jar and if we're messing
		 * with it, there may be multiple versions. 
		 * Also need to fine the working directory (which is the current directory). 
		 * For PAMGuard, is it worth taking the details from the ini file ? Not a bad place to start, 
		 * so find the ini file and read from there ...
		 * 
		 */
		if (root.exists() == false) {
			return null;
		}
		String pattern = "glob:" + filePattern;
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
		File[] files = root.listFiles();
		for (int i = 0; i < files.length; i++) {
			File aFile = files[i];
			if (aFile.isDirectory()) {
				continue;
			}
			if (getFileSize(aFile) < minSize) {
				continue;
			}
			if (aFile.getName().endsWith("java.exe")) {
				System.out.println(filePattern);
			}
			Path namePath = new File(aFile.getName()).toPath();
//					(aFile.getName());
			if (matcher.matches(namePath)) {
				return aFile;
			}
		}
		for (int i = 0; i < files.length; i++) {
			File aFile = files[i];
			if (!aFile.isDirectory()) {
				continue;
			}
			File foundFile = findFile(aFile, filePattern, searchSubDirs, minSize);
			if (foundFile != null) {
				return foundFile;
			}
		}
		return null;
	}
	
	private long getFileSize(File aFile) {
		long size = 0;
		try {
			size = Files.size(aFile.toPath());
		} catch (IOException e) {
		}
		return size;
	}
}
