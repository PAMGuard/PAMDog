package pamdog.remote;

import java.io.File;

import pamdog.DogControl;

/**
 * Listen for multicast messages from the PAMGuard batch controller
 * @author dg50
 *
 */
public class RemoteControlAgent {

	private DogControl dogControl;
	
	private RemoteMulticastListener remoteListener;

	public static final int defaultAgentPort = 12347;
	
	public static final String defaultAgentAddr = "230.1.1.1";

	private ComputerInfo computerInfo;

	public RemoteControlAgent(DogControl dogControl) {
		super();
		this.dogControl = dogControl;
		computerInfo = new ComputerInfo();
		remoteListener = new RemoteMulticastListener(dogControl, this);
	}

	/**
	 * @return the dogControl
	 */
	public DogControl getDogControl() {
		return dogControl;
	}

	/**
	 * @return the computerInfo
	 */
	public ComputerInfo getComputerInfo() {
		return computerInfo;
	}
	
	/**
	 * Get somewhere to store a pssf file that's being transferred from 
	 * the batch controller. This can be the pamguard home folder for now. 
	 * @return uer.home\Pamguard
	 */
	public String psfStorageFolder() {
		String settingsFolder = System.getProperty("user.home");
		settingsFolder += File.separator + "Pamguard";
		File sf = new File(settingsFolder);
		if (sf.exists() == false) {
			sf.mkdirs();
		}
		return settingsFolder;
	}

	/**
	 * Called when anything for the remote interface is changed. 
	 */
	public void restartRemoteInterface() {
		remoteListener.restart();
	}
	
}
