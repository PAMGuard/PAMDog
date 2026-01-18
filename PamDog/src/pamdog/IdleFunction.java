package pamdog;

import gui.DogDialog;
import gui.DogHelp;
import gui.PamDogGUI;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import Logging.DogLog;

/**
 * Idle function that does nothing until it's told to do something
 * @author Doug Gillespie
 *
 */
public class IdleFunction extends PamDog {

	private TrayIcon trayIcon;

	private DogControl dogControl;
	
	private DogUDP dogUDP;
	
	private DogLog pamguardLog;

	public IdleFunction(DogControl dogControl) {
		this.dogControl = dogControl;
		dogUDP = new DogUDP(dogControl, this);
		pamguardLog = new DogLog(dogControl, "Pamguard", true);
	}

	public void run() {
		/*
		 * Launch a swing worker thread to do the work. 
		 */
		if(dogControl.isRunGUI()) {
			dogControl.execute();
		}else {
			try {
				dogControl.doInBackground();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String createLaunchString(DogParams params) {
		return createLaunchString(params, params.getUdpPort());
	}
	/**
	 * Create a command line to launch PAMGuard. 
	 * @param params parameters
	 * @return command line. 
	 */
	public String createLaunchString(DogParams params, int port) {
		// If JRE path is empty, fall back to the system 'java' on PATH.
		String jre = params.getJre();
		if (jre == null || jre.trim().isEmpty()) {
			jre = "java";
		}
		// do not wrap the jre in quotes; wrapping it as "" when empty caused
		// the shell to try to execute an empty command which yields
		// "/bin/sh: 1: : Permission denied" on some systems.
		String commandLine = String.format("%s -Dname=AutoPamguard -Xms%dm -Xmx%dm -Djava.library.path=%s %s -jar \"%s\"", 
				jre,
				params.getMsMemory(), params.getMxMemory(), 
				params.getLibFolder(), params.getOtherVMOptions(), params.getJavaFile());
		String psf = params.getPsfFile();
		if (psf != null) {
			commandLine += String.format(" -psf \"%s\"", psf);
		}
		if (port > 0) {
			commandLine += String.format(" -port %d", port);
		}
		String opt = params.getOtherOptions();
		if (opt != null) {
			commandLine += " " + opt;
		}
		return commandLine;
	}

//	/**
//	 * Create a command-argument array to launch PAMGuard without invoking a shell.
//	 * This avoids quoting issues and shell injection, and works on Linux and Windows.
//	 */
//	public String[] createLaunchArgs(DogParams params, int port) {
//		java.util.List<String> args = new java.util.ArrayList<>();
//		// JRE
//		String jre = params.getJre();
//		if (jre == null || jre.trim().isEmpty()) {
//			jre = "java";
//		}
//		args.add(jre);
//		// VM properties and memory
//		args.add("-Dname=AutoPamguard");
//		args.add(String.format("-Xms%dm", params.getMsMemory()));
//		args.add(String.format("-Xmx%dm", params.getMxMemory()));
//		args.add(String.format("-Djava.library.path=%s", params.getLibFolder()));
//		// other VM options: split on whitespace but keep it simple
//		String vmOpts = params.getOtherVMOptions();
//		if (vmOpts != null && !vmOpts.trim().isEmpty()) {
//			for (String s: vmOpts.trim().split("\\s+")) {
//				if (!s.isEmpty()) args.add(s);
//			}
//		}
//		// -jar <jarfile>
//		args.add("-jar");
//		args.add(params.getJavaFile());
//		// psf file
//		String psf = params.getPsfFile();
//		if (psf != null && !psf.trim().isEmpty()) {
//			args.add("-psf");
//			args.add(psf);
//		}
//		// port
//		if (port > 0) {
//			args.add("-port");
//			args.add(Integer.toString(port));
//		}
//		// other options
//		String opt = params.getOtherOptions();
//		if (opt != null && !opt.trim().isEmpty()) {
//			for (String s: opt.trim().split("\\s+")) {
//				if (!s.isEmpty()) args.add(s);
//			}
//		}
//		return args.toArray(new String[0]);
//	}

	/**
	 * Create the popup menu for the tray
	 * @return
	 */
	private PopupMenu getTrayMenu() {
		PopupMenu pop = new PopupMenu();
		MenuItem mi = new MenuItem("Quit PAMDog");
		mi.addActionListener(new QuitDog());
		pop.add(mi);
		mi = new MenuItem("PAMDog Settings ...");
		mi.addActionListener(new ConfigureDog());
		pop.add(mi);
		mi = new MenuItem("PAMDog Help");
		mi.addActionListener(new PamdogHelp());
		pop.add(mi);
		return pop;
	}

	/**
	 * Called once at start up to set up the tray icon. 
	 */
	public void prepare() {
		if (SystemTray.isSupported() == false) {
			System.out.println("System tray not supported!");
			return;
		}
		trayIcon = new TrayIcon(PamDogGUI.getIconImageSmall(), "PAMGuard watchdog", getTrayMenu());
		trayIcon.addMouseListener(new TrayMouse());
		trayIcon.addActionListener(new ConfigureDog());
		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called before exit - removes tray icon. this will 
	 * happen anyway, but this makes it happen quicker. 
	 */
	public void destroy() {
		if (SystemTray.isSupported()) {
			SystemTray.getSystemTray().remove(trayIcon);
		}
		pamguardLog.closeFile();
	}

	/**
	 * @return the dogUDP
	 */
	public DogUDP getDogUDP() {
		return dogUDP;
	}

	/**
	 * Called at start and whenever the configuration changes
	 * Finds a free udp port. not sure we've been calling this in recent versions 
	 * since it continually cranks up the port number. 
	 */
	public void configure() {
		DogParams dogParams = dogControl.getParams();
		int port = DogUDP.findFreePort(dogParams.getUdpPort(), dogParams.getUdpPort()+10);
		if (port < 0) {
			System.out.printf("Unable to find free UDP port on range %d to %d\n",
					dogParams.getUdpPort(), dogParams.getUdpPort()+10);
		}
		dogUDP.setCurrentUdpPort(port);
	}

	/**
	 * Called when the PAMGuard process ends and no more strings can be read PAMguard 
	 * output stream. 
	 * @param pamProcess
	 */
	public void processEnded(Process pamProcess) {
		dogControl.processEnded();
	}



	/**
	 * Called when PAMguard writes a line to it's output stream.
	 * @param msg
	 */
	synchronized public void pamguardMessage(LogCaptureMessage msg) {
		String typ;
		if (msg.isError) {
			typ = "Err:";
		}
		else {
			typ = "Pam:";
		}
		pamguardLog.logItem(typ + msg.line);
	}

	public void controlMessage(ControlMessage msg) {
//		System.out.println(msg.msg);
	}
	
	private class TrayMouse extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent me) {
			if (me.getButton() == MouseEvent.BUTTON1) {
				dogControl.configure();
			}
		}
	}

	private class QuitDog implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			dogControl.quitDog();
		}
	}
	
	private class ConfigureDog implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			dogControl.configure();	
		}
	}

	
	private class PamdogHelp implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			DogHelp.getHelp().showHelp();	
		}
	}
	

	
}