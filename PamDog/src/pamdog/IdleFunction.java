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
		dogUDP = new DogUDP(this);
		pamguardLog = new DogLog(dogControl, "Pamguard", true);
	}

	public void run() {
		/*
		 * Launch a swing worker thread to do the work. 
		 */
		dogControl.execute();
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
		// seem unable to wrap lib path in "" - worrying for default location of files. 
		String commandLine = String.format("\"%s\" -Dname=AutoPamguard -Xms%dm -Xmx%dm -Djava.library.path=%s %s -jar \"%s\"", 
				params.getJre(),
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

	public void prepare() {
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
		SystemTray.getSystemTray().remove(trayIcon);
		pamguardLog.closeFile();
	}

	/**
	 * @return the dogUDP
	 */
	public DogUDP getDogUDP() {
		return dogUDP;
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
