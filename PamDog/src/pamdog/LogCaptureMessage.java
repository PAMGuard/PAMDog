package pamdog;

public class LogCaptureMessage {

	public String line;
	public boolean isError;
	public LogCaptureMessage(boolean isError, String line) {
		this.isError = isError;
		this.line = line;
	}

}
