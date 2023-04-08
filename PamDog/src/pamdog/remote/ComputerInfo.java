package pamdog.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Static data about the computer which will be sent back in messages. 
 * @author dg50
 *
 */
public class ComputerInfo {

	private String computerName = "Unknown";
	private String osName, osArch;
	private int nProcessors;
	
	public ComputerInfo() {
		// get the basic data about the computer. This only needs to be done once. 
		try {
			// this gets the name of the computer, not an ip address, something like PC22586 for my laptop. 
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
		osName = System.getProperty("os.name");
		osArch = System.getProperty("os.arch");
		nProcessors = Runtime.getRuntime().availableProcessors();
			
	}

	/**
	 * @return the computerName
	 */
	public String getComputerName() {
		return computerName;
	}

	/**
	 * @return the osName
	 */
	public String getOsName() {
		return osName;
	}

	/**
	 * @return the osArch
	 */
	public String getOsArch() {
		return osArch;
	}

	/**
	 * @return the nProcessors
	 */
	public int getnProcessors() {
		return nProcessors;
	}
}
