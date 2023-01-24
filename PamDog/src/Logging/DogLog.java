package Logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;

import pamdog.ConfigSettings;
import pamdog.DogControl;
import pamdog.DogUDP;
import pamdog.IdleFunction;
import pamdog.UdpCommands;

public class DogLog {

	private String logTitle;
	
	private List<LoggedItem> loggedItems = new LinkedList<LoggedItem>();

	private boolean logToFile;
	
	private boolean broadcastUDP;
	
	private DogUDP dogUDPErrors;
	
	String newline = System.getProperty("line.separator");
	
	private static final long oneHour = 3600000;
	
	private static final long oneDay = oneHour*24;
	
	/**
	 * How long to keep local list in seconds. 
	 */
	private long logHistory = 600;

	private DogControl dogControl;

	public DogLog(DogControl dogControl, String logTitle, boolean logToFile) {
		this.dogControl = dogControl;
		this.logTitle = logTitle;
		this.logToFile = logToFile;
		this.broadcastUDP = false;
	}
	
	synchronized public void logItem(String logText) {
		removeOldItems();
		LoggedItem loggedItem;
		loggedItems.add(loggedItem = new LoggedItem(logText));
		if (logToFile) {
			fileLog(loggedItem);
		}
		if(broadcastUDP) {
			broadcastMessage(loggedItem);
		}
	}
	
	synchronized public void logItem(String format, Object... objects) {
		String string = String.format(format, objects);
		logItem(string);
	}
	
	synchronized public void removeOldItems() {
		long start = System.currentTimeMillis() - logHistory * 1000;
		ListIterator<LoggedItem> it = loggedItems.listIterator();
		while (it.hasNext()) {
			LoggedItem logItem = it.next();
			if (logItem.logTime >= start) {
				break;
			}
			else {
				it.remove();
			}
		}
	}
	
	/**
	 * Count how many recent logs contain the string
	 * @param string string to search for
	 * @param countSeconds number of seconds to search
	 * @return count of strings containing. 
	 */
	synchronized public int countLogsContaining(String string, int countSeconds) {
		long start = System.currentTimeMillis() - countSeconds * 1000;
		ListIterator<LoggedItem> it = loggedItems.listIterator();
		int n = 0;
		while (it.hasNext()) {
			LoggedItem logItem = it.next();
			if (logItem.logTime < start) {
				continue;
			}
			String logLine = logItem.logString;
			if (logLine == null) {
				continue;
			}
			if (logLine.contains(string)) {
				n++;
			}
		}
		return n;
	}
	/**
	 * Count how many recent logs start with the string
	 * @param string string to search for
	 * @param countSeconds number of seconds to search
	 * @return count of strings starting with string. 
	 */
	synchronized public int countLogsStarting(String string, int countSeconds) {
		long start = System.currentTimeMillis() - countSeconds * 1000;
		ListIterator<LoggedItem> it = loggedItems.listIterator();
		int n = 0;
		while (it.hasNext()) {
			LoggedItem logItem = it.next();
			if (logItem.logTime < start) {
				continue;
			}
			String logLine = logItem.logString;
			if (logLine == null) {
				continue;
			}
			if (logLine.startsWith(string)) {
				n++;
			}
		}
		return n;
	}

	BufferedWriter writer;

	private long logFileStart = 0;
	/**
	 * Log an item to file. 
	 * @param loggedItem
	 */
	private void fileLog(LoggedItem loggedItem) {
		checkFile();
		String strp = String.format("%s %s", getLogDate(loggedItem.logTime), loggedItem.logString);
		System.out.println(strp);
		String str = String.format("%s %s%s", getLogDate(loggedItem.logTime), loggedItem.logString, newline);
		try {
			writer.write(str);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		closeFile();
	}
	
	private void checkFile() {
		long now = System.currentTimeMillis();	
		long fileStart = getNewFileName(now);
		if (logFileStart != fileStart) {
			closeFile();
		}
		if (writer == null) {
			File logFile = new File(createFileName(fileStart));
			try {
				writer = new BufferedWriter(new FileWriter(logFile, true));
			} catch (IOException e) {
				e.printStackTrace();
			}
			logFileStart = now;
		}
	}
	
	/**
	 * Work out  aname for a new file. If we're just starting, it 
	 * will be the current time. If we've rolled over a day, then 
	 * it will be the start of today. 
	 * @param now current time
	 * @return time for creation of file start name.
	 */
	private long getNewFileName(long now) {
		if (logFileStart == 0) {
			return now;
		}
		long today = floorTime(now, oneDay);
		long fileDay = floorTime(logFileStart, oneDay);
		if (today != fileDay) {
			return today;
		}
		else {
			return logFileStart;
		}
	}

	private long floorTime(long time, long roundFactor) {
		time = time / roundFactor;
		return time*roundFactor;
	}
	
	public void closeFile() {
		if (writer == null) return;
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer = null;
	}
	
	public static String getLogFolder() {
		return ConfigSettings.getPamguardFolder() + File.separator + "doglogs";
	}

	private String createFileName(long now) {
		String name = getLogFolder();
		File path = new File(name);
		if (path.exists() == false) {
			path.mkdirs();
		}
		name += File.separator + logTitle.replace(" ", "_") + getLogDate(now) + ".txt";
		return name;
	}
	
	public static String getLogDate(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		TimeZone defaultTimeZone = TimeZone.getTimeZone("UTC");
		c.setTimeZone(defaultTimeZone);

		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();
		//		return String.format("%tY-%<tm-%<td %<tH:%<tM:%<tS", d);
		return df.format(d);
	}

	/**
	 * Set up the DogUDP object to broadcast all commands to UDP port
	 * 
	 * @param udpPortErrors port number to send errors/commands to
	 */
	public void setupBroadcast(int udpPortErrors) {
		this.broadcastUDP = true;
		dogUDPErrors = new DogUDP(dogControl, null);

		// find the next open UDP port
		int freePort = DogUDP.findFreePort(udpPortErrors, udpPortErrors+10);
		System.out.println("Free UDP port for command broadcast is " + freePort);
		if (freePort <= 0) {
			broadcastUDP = false;
			logItem("Error: Unable to find free UDP port for command logging");
			return;
		}
		dogUDPErrors.setCurrentUdpPort(freePort);
		logItem("Beginning PamDog command broadcast");

	}
	
	/**
	 * Broadcast the loggedItem information to the UDP port
	 * 
	 * @param loggedItem
	 */
	private void broadcastMessage(LoggedItem loggedItem) {
		String str = String.format("%s %s%s", getLogDate(loggedItem.logTime), loggedItem.logString, newline);
//		System.out.println("Broadcasting: " + str);
		String ans = dogUDPErrors.sendCommand(str, 10);
	}

	/**
	 * Sets the boolean controlling the UDP command broadcast to false
	 */
	public void stopBroadcasting() {
		this.broadcastUDP = false;
	}


}
