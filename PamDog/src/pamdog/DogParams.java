package pamdog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ListIterator;

import pamdog.RestartInfo.RestartType;

/**
 * Saveable parameters for PamDog
 * @author Doug Gillespie
 *
 */
public class DogParams implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	private boolean activeDog = false;
	private String workingFolder = null;
	private String javaFile = "";
	private String jre = "";
	private String psfFile = ""; 
	private String libFolder = "lib6";
	private int mxMemory = 4096;
	private int msMemory = 2048;
	private int udpPort = 8000;
	private boolean broadcastErrors = false;
	private int udpPortErrors = 8100;
	private String otherOptions = ""; //"-smru";
	private String otherVMOptions = "";

	/**
	 * time to wait for after startup in seconds
	 * (Windows may need a couple of minutes to settle)
	 */
	private int startWait = 10;
	
	private boolean allowSystemRestarts = false;
	private int minRestartMinutes = 60;
	private long lastRestartTime = 0;
	
	/**
	 * now some status information, so that we can persistently keep a 
	 * record of how often PAMGuard has been restarted and more importantly
	 * how often the PC has been rebooted. Array lists for restarts and 
	 * max number of all types of restart to be stored.
	 */
	private ArrayList<RestartInfo> dogRestarts;

	/**
	 * Network interface name for remote control
	 */
	public String netInterfaceName;
	static final int maxRunRestarts = 20;
	static final int maxPamguardRestarts = 20;
	static final int maxPCRestarts = 20;
	
	
	
	public DogParams() {
		super();
	}
	
	/**
	 * @return the javaFile
	 */
	public String getJavaFile() {
		return javaFile;
	}
	/**
	 * @param javaFile the javaFile to set
	 */
	public void setJavaFile(String javaFile) {
		this.javaFile = javaFile;
	}
	/**
	 * @return the psfFile
	 */
	public String getPsfFile() {
		return psfFile;
	}
	/**
	 * @param psfFile the psfFile to set
	 */
	public void setPsfFile(String psfFile) {
		this.psfFile = psfFile;
	}
	/**
	 * @return the libFolder
	 */
	public String getLibFolder() {
		return libFolder;
	}
	/**
	 * @param libFolder the libFolder to set
	 */
	public void setLibFolder(String libFolder) {
		this.libFolder = libFolder;
	}
	/**
	 * @return the mxMemory
	 */
	public int getMxMemory() {
		return mxMemory;
	}
	/**
	 * @param mxMemory the mxMemory to set
	 */
	public void setMxMemory(int mxMemory) {
		this.mxMemory = mxMemory;
	}
	/**
	 * @return the min Memory
	 */
	public int getMsMemory() {
		return msMemory;
	}
	/**
	 * @param m2Memory the m2Memory to set
	 */
	public void setMsMemory(int msMemory) {
		this.msMemory = msMemory;
	}
	/**
	 * @return the udpPort
	 */
	public int getUdpPort() {
		return udpPort;
	}
	/**
	 * @return the broadcastErrors
	 */
	public boolean isBroadcastErrors() {
		return broadcastErrors;
	}
	/**
	 * @param broadcastErrors the broadcastErrors to set
	 */
	public void setBroadcastErrors(boolean broadcastErrors) {
		this.broadcastErrors = broadcastErrors;
	}
	/**
	 * @param udpPort the udpPort to set
	 */
	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}
	/**
	 * @return the udpPortErrors
	 */
	public int getUdpPortErrors() {
		return udpPortErrors;
	}
	/**
	 * @param udpPortErrors the udpPortErrors to set
	 */
	public void setUdpPortErrors(int udpPortErrors) {
		this.udpPortErrors = udpPortErrors;
	}
	/**
	 * @return the activeDog
	 */
	public boolean isActiveDog() {
		return activeDog;
	}
	/**
	 * @param activeDog the activeDog to set
	 */
	public void setActiveDog(boolean activeDog) {
		this.activeDog = activeDog;
	}
	/**
	 * @return the otherOptions
	 */
	public String getOtherOptions() {
		return otherOptions;
	}
	/**
	 * @param otherOptions the otherOptions to set
	 */
	public void setOtherOptions(String otherOptions) {
		this.otherOptions = otherOptions;
	}
	/**
	 * @return the jre
	 */
	public String getJre() {
		return jre;
	}
	/**
	 * @param jre the jre to set
	 */
	public void setJre(String jre) {
		this.jre = jre;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public DogParams clone()  {
		try {
			DogParams newDP = (DogParams) super.clone();
			if (newDP.dogRestarts == null) {
				newDP.dogRestarts = new ArrayList<>();
//				maxPamguardRestarts = 20;
//				maxRunRestarts = 20;
//				maxPCRestarts = 20;
			}
			return newDP;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * @return the workingFolder
	 */
	public String getWorkingFolder() {
		return workingFolder;
	}
	/**
	 * @param workingFolder the workingFolder to set
	 */
	public void setWorkingFolder(String workingFolder) {
		this.workingFolder = workingFolder;
	}
	public int getStartWait() {
		return startWait;
	}
	
	public void setStartWait(int startWait) {
		this.startWait = startWait;
	}
	
	public void addRestart(RestartInfo restartInfo) {
		if(dogRestarts==null) {
			dogRestarts = new ArrayList<RestartInfo>();
		}
		dogRestarts.add(restartInfo);
		clearOldRestartInfo();
	}
	/**
	 * Clear old restart infos so that there are never too many in the list. 
	 */
	private void clearOldRestartInfo() {
		if (dogRestarts.isEmpty()) {
			return;
		}
		int nRun = 0, nPamguard = 0, nPC = 0;
		ListIterator<RestartInfo> it = dogRestarts.listIterator(dogRestarts.size());
		while (it.hasPrevious()) {
			RestartInfo ri = it.previous();
			switch(ri.getRestartType()) {
			case RESTARTPAMGUARD:
				if (nPamguard ++ >= maxPamguardRestarts) {
					it.remove();
				}
				break;
			case RESTARTPC:
				if (nPC ++ >= maxPCRestarts) {
					it.remove();
				}
				break;
			case RESTARTRUN:
				if (nRun ++ >= maxRunRestarts) {
					it.remove();
				}
				break;
			default:
				break;
			}
			
		}
	}
	
	/**
	 * Get a count of restarts of a given type. 
	 * @param restartType type of restart. 
	 * @param sinceWhen start time of period we're interested in
	 * @return number of restarts of that type. 
	 */
	public int getRestartCount(RestartType restartType, long sinceWhen) {
		int n = 0;
		for (RestartInfo inf:dogRestarts) {
			if (inf.getRestartType() == restartType && inf.getRestartTime() >= sinceWhen) {
				n++;
			}
		}
		return n;
	}
	/**
	 * @return the allowSystemRestarts
	 */
	public boolean isAllowSystemRestarts() {
		return allowSystemRestarts;
	}
	/**
	 * @param allowSystemRestarts the allowSystemRestarts to set
	 */
	public void setAllowSystemRestarts(boolean allowSystemRestarts) {
		this.allowSystemRestarts = allowSystemRestarts;
	}
	
	public String getOtherVMOptions() {
		if (otherVMOptions == null) {
			otherVMOptions = "";
		}
		return otherVMOptions;
	}
	
	public void setOtherVMOptions(String otherVMOptions) {
		this.otherVMOptions = otherVMOptions;
	}
	/**
	 * @return the minRestartMinutes
	 */
	public int getMinRestartMinutes() {
		return minRestartMinutes;
	}
	/**
	 * @param minRestartMinutes the minRestartMinutes to set
	 */
	public void setMinRestartMinutes(int minRestartMinutes) {
		this.minRestartMinutes = minRestartMinutes;
	}
	/**
	 * @return the lastRestartTime
	 */
	public long getLastRestartTime() {
		return lastRestartTime;
	}
	/**
	 * @param lastRestartTime the lastRestartTime to set
	 */
	public void setLastRestartTime(long lastRestartTime) {
		this.lastRestartTime = lastRestartTime;
	}


}
