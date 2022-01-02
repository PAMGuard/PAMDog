package Logging;

public class LoggedItem {

	protected String logString;
	protected long logTime;
	
	public LoggedItem(String logString) {
		this.logString = logString;
		logTime = System.currentTimeMillis();
	}
	
	public LoggedItem(long logTime, String logString) {
		super();
		this.logTime = logTime;
		this.logString = logString;
	}

}
