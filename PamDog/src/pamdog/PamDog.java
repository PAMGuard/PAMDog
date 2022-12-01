package pamdog;

import javax.swing.UIManager;

public class PamDog {

	public static void main(String[] args) {
		
		boolean runGUI = true;
		String configPath = null;
		
		try {
//		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//		        if ("Nimbus".equals(info.getName())) {
//		            UIManager.setLookAndFeel(info.getClassName());
//		            break;
//		        }
//		    }
			if (System.getProperty("os.name").startsWith("windows")) {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		    }
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
			if(!System.getProperty("os.name").equals("Linux")) {
				
			}
		} catch (Exception e) {
		    // If Nimbus is not available, you can set the GUI to another look and feel.
		}
		
		DogControl dogControl = new DogControl(runGUI,configPath);
		
	}

}
