package pamdog;

import java.io.Serializable;

public class RestartInfo implements Serializable {

	/**
	 * Information about PAMGuard and PC restarts. 
	 */
	
	public enum RestartType {RESTARTRUN, RESTARTPAMGUARD, RESTARTPC};
	
	private RestartType restartType;
	private long restartTime;
	private String restartMessage;
	public RestartInfo(RestartType restartType, long restartTime, String restartMessage) {
		this.restartType = restartType;
		this.restartTime = restartTime;
		this.restartMessage = restartMessage;
	}

	public RestartInfo(RestartType restartType, String restartMessage) {
		this.restartType = restartType;
		this.restartTime = System.currentTimeMillis();
		this.restartMessage = restartMessage;
	}

	/**
	 * @return the restartType
	 */
	public RestartType getRestartType() {
		return restartType;
	}
	/**
	 * @return the restartTime
	 */
	public long getRestartTime() {
		return restartTime;
	}
	/**
	 * @return the restartMessage
	 */
	public String getRestartMessage() {
		return restartMessage;
	}

}
