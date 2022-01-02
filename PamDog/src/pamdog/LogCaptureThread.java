package pamdog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.swing.SwingWorker;

/**
 * Capture the output of PAMguard and log it to a file + optionally
 * display it in a window. 
 * @author Doug Gillespie
 *
 */
public class LogCaptureThread extends SwingWorker<Integer, LogCaptureMessage> {

	private Process pamProcess;
	private InputStream procInputStream;
	private BufferedReader bufferedReader;
	private IdleFunction idleFunction;
	private boolean isError;

	public LogCaptureThread(IdleFunction idleFunction, Process process, boolean isError) {
		this.idleFunction = idleFunction;
		this.pamProcess = process;
		this.isError = isError;
		if (isError) {
			procInputStream = process.getErrorStream();
		}
		else {
			procInputStream = process.getInputStream();
		}
		InputStreamReader isr = new InputStreamReader(procInputStream);
		bufferedReader = new BufferedReader(isr);
	}

	/* (non-Javadoc)
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected Integer doInBackground() {
		String line;
//		publish(new LogCaptureMessage("Enter log capture thread"));
		try {
			while ((line = bufferedReader.readLine()) != null) {
				publish(new LogCaptureMessage(isError, line));
			}
		}
		catch (IOException e) {
			publish(new LogCaptureMessage(isError, e.getMessage()));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.swing.SwingWorker#done()
	 */
	@Override
	protected void done() {
//		System.out.println("Log capture complete");
//		idleFunction.processEnded(pamProcess);
		super.done();
	}

	/* (non-Javadoc)
	 * @see javax.swing.SwingWorker#process(java.util.List)
	 */
	@Override
	protected void process(List<LogCaptureMessage> chunks) {
		for (LogCaptureMessage msg:chunks) {
//			System.out.println("Pam: " + msg.line);
			idleFunction.pamguardMessage(msg);
		}
	}


}
