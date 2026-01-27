package pamdog;


public class PamDogPi {
	
	/**
	 * Testing PamDoug for raspberryPI 
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("Starting PamDog for Raspberry Pi");
		
		boolean runGUI = false;
		String configPath = null;
		
		DogParams dogParams = new DogParams();
		
		//String osName = System.getProperty("os.name").toLowerCase();
		
		String pamguardJar = "/home/jdjm/Desktop/pamguard_pi5/Pamguard-2.02.17ffc.jar";
		String panguardPSFX = "/home/jdjm/Desktop/pamguard_pi5/pamguard_pi5.psfx";
		String pamguardlib = "/home/jdjm/Desktop/pamguard_pi5/liblinux";

		
		dogParams.setPsfFile(panguardPSFX);
		dogParams.setJavaFile(pamguardJar);
		dogParams.setLibFolder(pamguardlib);
		dogParams.setWorkingFolder("/home/jdjm/Desktop/pamguard_pi5");
		
		
		dogParams.setStartWait(60); // wait up to 60 seconds for pamguard to start.

		
		DogControl dogControl = new DogControl(runGUI,dogParams);
		
		dogControl.activateWatchDog(runGUI); 
		//dogControl.configure();
		
		System.out.println("Ending PamDog for Raspberry Pi");

		
		
	}

}
