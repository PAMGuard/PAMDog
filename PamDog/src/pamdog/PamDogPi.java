package pamdog;

import java.io.File;
import java.io.IOException;


public class PamDogPi {
	
//	// arg name for global change to store name. 
//	public static final String GlobalFolderArg = "-binaryfolder";
//	
//	public static final String GlobalDatabaseNameArg = "-databasefile";
//	
//	public static final String GlobalWavFolderArg = "-wavfilefolder";


	/**
	 * Testing PamDoug for raspberryPI 
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("Starting PamDog for Raspberry Pi");
		

		boolean runGUI = true;
		String configPath = null;
		
		if (args != null) {
			int nArgs = args.length;
			int iArg = 0;
			String anArg;
			while (iArg < nArgs) {
				anArg = args[iArg++];
				if(anArg.equals("-nogui")) {
					runGUI = false;
				}
				if(anArg.equals("-configPath")) {
					configPath = args[iArg++];
				}
			}
		}
		
		if (configPath == null) {
			System.err.println("No config path provided, exiting");
			return;
		}
		
		File file = new File(configPath);
		
		//makeJSONParams(file); //TEMP
		if (!file.exists()) {
			System.err.println("Config file does not exist, exiting");
		}
				
		DogParams dogParams =  ConfigJSONSettings.readDogParams(file);
	
		DogControl dogControl = new DogControl(runGUI,dogParams, true);
		
		// Override the default config path with the one provided in the arguments.
		dogControl.getConfigSettings().setConfigOverridePath(configPath);
		
		System.out.println("Loaded PamDog parameters");
		dogControl.getConfigSettings().saveConfig(dogControl.getParams());
		
		dogControl.activateWatchDog(runGUI); 
		//dogControl.configure();
		
		System.out.println("PamDog for Raspberry Pi init complete. Watchdog thread activating");
	}
	
	
	/**
	 * Make a JSON file with parameters for PamDog. This is just for testing and should
	 * not be used in production.
	 * */
	public static void makeJSONParams(File fie) {
		
		DogParams dogParams = new DogParams();	
		
		String pamguardJar = "/home/jdjm/Desktop/pamguard_pi5/Pamguard-2.02.17ffc.jar";
		String panguardPSFX = "/home/jdjm/Desktop/pamguard_pi5/pamguard_pi5.psfx";
		String pamguardlib = "/home/jdjm/Desktop/pamguard_pi5/liblinux";
		dogParams.setPsfFile(panguardPSFX);
		dogParams.setJavaFile(pamguardJar);
		dogParams.setLibFolder(pamguardlib);

		dogParams.setWorkingFolder("/home/jdjm/Desktop/pamguard_pi5");
	
		dogParams.setDeploy(true);
		dogParams.setStartWait(20); // wait up to 60 seconds for pamguard to start.
		
		try {
			ConfigJSONSettings.writeDogParams(fie, dogParams);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
