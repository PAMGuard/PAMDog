package pamdog.remote;

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
	
}
