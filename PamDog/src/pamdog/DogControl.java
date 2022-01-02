package pamdog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import pamdog.RestartInfo.RestartType;
import Logging.DogLog;

public class DogControl extends SwingWorker<Integer, ControlMessage> {

	private IdleFunction idleFunction;

	protected boolean keepRunning = true;

	private DogUDP dogUDP;

	protected Process process;

	private Object processSynch = new Object();

	private DogLog commandLog = new DogLog("Commands", true);

	private long controlStart;

	public DogControl(IdleFunction idleFunction) {
		this.idleFunction = idleFunction;
		dogUDP = idleFunction.getDogUDP();
		controlStart = System.currentTimeMillis();
		setBroadcast();
	
	}

	/**
	 * Turns on/off the UDP command broadcast, depending on the current parameters
	 */
	private void setBroadcast() {
		// if we are broadcasting the errors/commands, tell commandLog
		if (idleFunction.dogParams.isBroadcastErrors()) {
			commandLog.setupBroadcast(idleFunction.dogParams.getUdpPortErrors());
		} else {
			commandLog.stopBroadcasting();
		}
	}

	@Override
	protected Integer doInBackground() throws Exception {
		
		int stallCount = 0;
		
		int majorErrorCount = 0;
		
		int startFailures = 0;
		
		boolean wasRunning = false;
		
		while (keepRunning) {
			// always start with a little rest !
			sleep(1000);

			if (idleFunction.dogParams.isActiveDog() == false ||
					System.currentTimeMillis()-controlStart < idleFunction.dogParams.getStartWait() * 1000) {
				//				idleFunction.launchPamguard();
				sleep(1000);
				continue;
			}
			

			// ask twice if it was running lat loop through this function. 
			int tries = wasRunning ? 5 : 1;
			int timeout = wasRunning ? 5000 : 1000;
			if (isRunning(tries, timeout) == false) {
				if (shouldLaunch()) {
					// kill old one first - could be because UDP is not responding. 
					commandLog.logItem("In call from isRunning() == false and shouldLaunch() == true");
					killPamguard();
					launchPamguard(5000);
				}
				
				continue;
			}
			
			wasRunning = true;

			int status = getStatus();
			switch (status) {
			case UdpCommands.PAM_IDLE:
				if (shouldStart()) {
					stopPamguard(5000);
					if (startPamguard(20000) == false) {
						startFailures++;
					}
					else {
						startFailures = 0;
					};
				}
				if (startFailures >= 5) {
					majorErrorCount++;
					idleFunction.dogParams.addRestart(new RestartInfo(RestartType.RESTARTPAMGUARD, "PAMGuard won't start running"));
					commandLog.logItem("In startFailures >= 5");
					killPamguard();			
					/**
					 * Consider a total restart of the PC if the number of times the process has had to 
					 * be killed is high. 
					 */
					if (condsiderRestart()) {
						if (restartPC()) {
							continue;
						}
					}
				}
				continue;
			case UdpCommands.PAM_STALLED:
				if (++stallCount > 10) {
					stallCount = 0;
					if (++majorErrorCount > 10) {
						majorErrorAction(majorErrorCount);
					}
					idleFunction.dogParams.addRestart(new RestartInfo(RestartType.RESTARTPAMGUARD, "PAMGuard Stalled"));
					commandLog.logItem("Because UdpCommands.PAM_STALLED");	
					killPamguard();		
					/**
					 * Consider a total restart of the PC if the number of times the process has had to 
					 * be killed is high. 
					 */
					if (condsiderRestart()) {
						if (restartPC()) {
							continue;
						}
					}
				}
				break;
			case UdpCommands.PAM_RUNNING:
				stallCount = 0;
				majorErrorCount = 0;
				startFailures = 0;
				break;
			}
		}
		
		commandLog.closeFile();

		return null;
	}
	
	/**
	 * Rules for restarting PC. 
	 * Restart it if it's failed and needed a total kill 5 times in the last 10 minutes<p>
	 * If however, it's already restarted 5 times in 2 hours, then give up. 
	 * @return true if it's worth a restart
	 */
	private boolean condsiderRestart() {
		long now = System.currentTimeMillis();
		int killCount = idleFunction.dogParams.getRestartCount(RestartType.RESTARTPAMGUARD, now - 600000);
		if (killCount < 5) {
			return false;
		}
		int pcStartCount = idleFunction.dogParams.getRestartCount(RestartType.RESTARTPC, now - 3600*1000*2);
		if (pcStartCount > 5) {
			return false;
		}
		return true;
	}

	/**
	 * this gets called when it's already restarted 
	 * PAMguard 10 times - basically, consider rebooting !
	 * @param majorErrorCount
	 */
	private void majorErrorAction(int majorErrorCount) {
//		Runtime.getRuntime().
		
	}

	private boolean reboot() {

		 String shutdownCommand = null;

		 String osName = System.getProperty("os.name");        
		 if (osName.startsWith("Win")) {
		   shutdownCommand = "shutdown.exe -r -t 5";
		 } else if (osName.startsWith("Linux") || osName.startsWith("Mac")) {
		   shutdownCommand = "reboot -h now";
		 } else {
		   System.err.println("Shutdown unsupported operating system ...");
		    return false;
		 }
		 commandLog.logItem("Major problem. Restart computer");
		 
		 keepRunning = false;
		 
		 try {
			Runtime.getRuntime().exec(shutdownCommand);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		 return true;
 }
	/**
	 * Don't try to start if we've already tried in the last
	 * minute
	 * @return 
	 */
	private boolean shouldLaunch() {

		int nTries = commandLog.countLogsStarting("Launch", 10);
		return nTries == 0;
	}

	/**
	 * Only try to actually start the software if 
	 * it hasn't been attempted in the last 10 seconds. 
	 * @return
	 */
	private boolean shouldStart() {
		int nStarts = commandLog.countLogsStarting("Start", 10);
		return nStarts == 0;
	}
	
	/**
	 * Check to see fi the software is running, trying a number of times. 
	 * @param nTries number of times to ping PAMgaurd
	 * @param pingTimeout timout for each ping. 
	 * @return true if any return true. 
	 */
	public boolean isRunning(int nTries, int pingTimeout) {
		for (int i = 0; i < nTries; i++) {
			if (isRunning(pingTimeout)) {
				return true;
			}
			sleep(5000);	// wait 5 seconds between ping attempts, just in case			
		}
		return false;
	}

	
	/**
	 * 
	 * @return true if the program is running. (not necessarily
	 * doing anything, but there is a program there that we can ping).
	 */
	public boolean isRunning() {
		return isRunning(1000);
	}
	/**
	 * 
	 * @return true if the program is running. (not necessarily
	 * doing anything, but there is a program there that we can ping).
	 */
	
	public boolean isRunning(int pingTimeout) {
		String ans = dogUDP.sendCommand(UdpCommands.PING, pingTimeout);
		if (ans == null) {
//			publish(new ControlMessage("Ping Error from isRunning(): " + dogUDP.getLastError()));
			commandLog.logItem("Ping Error from isRunning(): " + dogUDP.getLastError());
		}
		if (ans!=null && !ans.equals(UdpCommands.PING)) {
//			publish(new ControlMessage("False response from Ping; getting " + ans + " instead"));
			commandLog.logItem("False response from Ping; getting " + ans + " instead");
		}
		if (!(ans != null && ans.equals(UdpCommands.PING))) {
			commandLog.logItem("Just failed isRunning check, ans = " + ans);
		}
		return (ans != null && ans.equals(UdpCommands.PING));
	}
	
	/**
	 * check to see if PAMguard has finished initialising
	 * it's unwise to tray to start it until this process has completed. 
	 * @return true if it's initialised. 
	 */
	public boolean isInitialised() {
		int status = getStatus();
		return status >= 0 && getStatus() != UdpCommands.PAM_INITIALISING;
	}

	public int getStatus() {
		/**
		 * Returned strings are in the form "status 1", etc.
		 */
		String ans = dogUDP.sendCommand(UdpCommands.STATUS, 1000);
		if (ans == null || ans.length() < 8) {
//			publish(new ControlMessage("Status Error: " + dogUDP.getLastError()));
			commandLog.logItem("Status Error: " + dogUDP.getLastError());
			return -1;
		}
		int status = -1;
		String statNum = ans.substring(7);
		try {
			status = Integer.valueOf(statNum);
			commandLog.logItem("Status Check returned: " + ans);
		}
		catch (NumberFormatException e) {
//			publish(new ControlMessage("Unknown Status " + ans));
			commandLog.logItem("Unknown Status " + ans);
			return -1;
		}
		return status;
	}
	
	/**
	 * Restart the PC. This function is pretty brutal and will cause the PC to restart
	 * in 1 minute. <p>
	 * In future, rewrite this to show a dialog to give the user a few seconds to override the command. 
	 * @return true if command issues to system successfully 
	 */
	public boolean restartPC() {
		long lastRestartTime = idleFunction.dogParams.getLastRestartTime();
		long minRestartTime = idleFunction.dogParams.getMinRestartMinutes() * 60000L;
		if (System.currentTimeMillis() - lastRestartTime < minRestartTime) {
			return false;
		}
		if (idleFunction.dogParams.isAllowSystemRestarts() == false) {
			commandLog.logItem("System restart required but not enabled.");
			return false;
		}
		idleFunction.dogParams.addRestart(new RestartInfo(RestartType.RESTARTPC, "Restart PC"));
		idleFunction.dogParams.setLastRestartTime(System.currentTimeMillis());
		// since the PC is about to be restarted, force immediate saving of the params
		idleFunction.saveConfig();
		
		String cmd = "shutdown -r -f";
		commandLog.logItem(cmd);
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		quitDog();
		return true;
	}

	private boolean launchPamguard(long waitTime) {
		int freePort = DogUDP.findFreePort(idleFunction.dogParams.getUdpPort(), idleFunction.dogParams.getUdpPort()+10);
		System.out.println("Free UDP port id is " + freePort);
		if (freePort <= 0) {
			commandLog.logItem("Launch Failed: Unable to find free UDP port for comms");
			return false;
		}
		dogUDP.setCurrentUdpPort(freePort);
		String commandLine = idleFunction.createLaunchString(idleFunction.dogParams, freePort);
		try {
			process = Runtime.getRuntime().exec(commandLine, null, new File(idleFunction.dogParams.getWorkingFolder()));
		} catch (IOException e) {
			e.printStackTrace();
			commandLog.logItem("Launch Failed: " + commandLine);
			return false;
		}
		if (process != null) {
			/*
			 * Launch threads to capture the output and errors from PAMguard 
			 */
			LogCaptureThread logThread = new LogCaptureThread(idleFunction, process, false);
			LogCaptureThread errThread = new LogCaptureThread(idleFunction, process, true);
			logThread.execute();
			errThread.execute();
		}
		commandLog.logItem("Launch Ok: " + commandLine);
		commandLog.logItem("Process Name: " + process.toString());
		long now = System.currentTimeMillis();
		boolean isRunning = isRunning();
		boolean isInitialised = isInitialised();
		while (isRunning == false && isInitialised == false && System.currentTimeMillis() - now < waitTime) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isRunning = isRunning();
			isInitialised = isInitialised();
		}
		isRunning = isRunning();
		isInitialised = isInitialised();
		return isRunning && isInitialised;
	}

	/**
	 * Kill PAMguard. Tell it once to stop, then exit anyway. 
	 */
	protected void killPamguard() {
		commandLog.logItem("Kill Pamguard");
		stopPamguard(2000);
		String ans = dogUDP.sendCommand(UdpCommands.EXIT, 1000);
		if (ans != null) {
			
			// publish the message
//			publish(new ControlMessage("Kill command returned message: " + dogUDP.getLastError()));
			commandLog.logItem("Kill command returned message: " + dogUDP.getLastError());
			
			// give it a few seconds to exit by itself, 
			// then kill it anyway. 
			for (int i = 0; i < 10; i++) {
				if (dogUDP.sendCommand(UdpCommands.PING, 100) == null) {
					break;
				}
				// still there, so sleep and try again. 
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		// if ans was null, there was an error with the commmand
		else {
//			publish(new ControlMessage("Kill command returned error: " + dogUDP.getLastError()));
			commandLog.logItem("Kill command returned error: " + dogUDP.getLastError());
		}
		synchronized (processSynch) {
			if (process == null) {
				commandLog.logItem("Pamguard Shut Down");
				return;
			}
			commandLog.logItem("Force Close");
			process.destroy();
			boolean reallyKilled=false;
			try {
				reallyKilled = process.waitFor(5000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (!reallyKilled) {
				commandLog.logItem("Error - process may still be active");
				this.extremeKill();
			}
			process = null;
		}
	}

	/**
	 * Start PAMguard and wait up to waitTime milliseconds for
	 * it to report that it is actually running. 
	 * @param waitTime MAx time to wait for it to be running
	 * @return true if it's in STATUS_RUNNING
	 */
	private boolean startPamguard(long waitTime) {
		String ans = dogUDP.sendCommand(UdpCommands.START, 1000);
		idleFunction.dogParams.addRestart(new RestartInfo(RestartType.RESTARTRUN, "Sart PAMGuard"));
		long now = System.currentTimeMillis();
		int curState;
		while ((curState=getStatus()) != UdpCommands.PAM_RUNNING && 
				System.currentTimeMillis() - now < waitTime) {
			System.out.printf("Dog status after %3.1f s = %d\n", (System.currentTimeMillis() - now)/1000., curState);
			sleep(500);
		}
		if (curState == UdpCommands.PAM_RUNNING) {
			commandLog.logItem("Start Ok");
			return true;
		}
		else {
			commandLog.logItem("Start Failed status " + curState);
			return false;
		}
	}
	
	/**
	 * Stop PAMguard and wait up to waitTime milliseconds for
	 * it to report that it is actually running. 
	 * @param waitTime MAx time to wait for it to be running
	 * @return true if it's in STATUS_RUNNING
	 */
	private boolean stopPamguard(long waitTime) {
		String ans = dogUDP.sendCommand(UdpCommands.STOP, 1000);
		long now = System.currentTimeMillis();
		int curState;
		while ((curState=getStatus()) != UdpCommands.PAM_IDLE && 
				System.currentTimeMillis() - now < waitTime) {
			sleep(500);
		}
		if (curState == UdpCommands.PAM_IDLE) {
			commandLog.logItem("Stop Ok");
			return true;
		}
		else {
			commandLog.logItem("Stop Failed status " + curState);
			return false;
		}
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


	}

	/* (non-Javadoc)
	 * @see javax.swing.SwingWorker#done()
	 */
	@Override
	protected void done() {
		idleFunction.destroy();
	}

	/* (non-Javadoc)
	 * @see javax.swing.SwingWorker#process(java.util.List)
	 */
	@Override
	protected void process(List<ControlMessage> chunks) {
		for (ControlMessage msg:chunks) {
			idleFunction.controlMessage(msg);
			commandLog.logItem(msg.msg);
		}
	}

	public void processEnded() {
		synchronized (processSynch) {
			process = null;			
		}
	}

	public void quitDog() {
		keepRunning = false;	
//		if (process != null) {
//			process.destroy();
//		}
	}

	/**
	 * Called directly from the dialog when the activate checkbox is changed
	 * @param isActive
	 */
	public boolean activateWatchDog(boolean isActive) {
//		if (isActive) {
//			if (checkDialogParams() == false) {
//				return false;
//			}
//		}
		idleFunction.dogParams.setActiveDog(isActive);		
		return isActive;
	}

	/**
	 * Called from the control panel in the dialog when dog is activated
	 * forces immediate use of parameters. 
	 * @param dogParams
	 */
	public void setNewParams(DogParams dogParams) {
		idleFunction.setParams(dogParams);
		setBroadcast();
	}
	
	
	/**
	 * Try to forcefully kill the Pamguard process.  Need to get a list of all java processes currently running,
	 * and look for the dummy keyword AutoPamguard.  Once found, parse the PID from the line and perform
	 * a taskkill.  This requires the java program jps.exe, which is typically distributed with the jdk (and not
	 * necessarily with the jre).
	 * 
	 * @return true is successful, false otherwise
	 */
	public boolean extremeKill() {
		File javaEXE = new File(idleFunction.dogParams.getJre());
		String javaPath = javaEXE.getParent();
		File jpsEXE = new File(javaPath,"jps.exe");
		if (jpsEXE.exists()) {
			String commandLine = String.format("\"%s\" -v", jpsEXE.getAbsolutePath());
			Process p;
			try {
				p = Runtime.getRuntime().exec(commandLine);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					commandLog.logItem("***Java Process Found = " + line);
					if (line.contains("AutoPamguard")) {
						String PID = line.substring(0, line.indexOf(" ")); 
						commandLog.logItem("***Killing Task PID = " + PID);
						Runtime.getRuntime().exec("taskkill /F /PID " + Long.parseLong(PID));
					}
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		else {
			commandLog.logItem("Error cannot find " + jpsEXE.getAbsolutePath());
			commandLog.logItem("jps.exe is distributed with the Java JDK and not JRE.  Try pointing the PamDog params to a java.exe within a JDK"); 
		}
		return false;
	}


//	private boolean checkDialogParams() {
//		DogDialog dogDialog = 
//	}
}