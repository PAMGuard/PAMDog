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

	DogParams dogParams = new DogParams();

	private TrayIcon trayIcon;

	private DogControl dogControl;
	
	private DogUDP dogUDP;
	
	private DogLog pamguardLog;

	public IdleFunction() {
		dogUDP = new DogUDP(this);
		if (loadConfig() == false) {
			saveConfig();
		}
		pamguardLog = new DogLog("Pamguard", true);
	}

	public void run() {
		/*
		 * Launch a swing worker thread to do the work. 
		 */
		dogControl = new DogControl(this);
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
				configure();
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
			configure();	
		}
	}

	public void configure() {
		DogParams newParams = DogDialog.showDialog(dogParams, dogControl);
		if (newParams != null) {
			setParams(newParams);
		}
	}
	
	private class PamdogHelp implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			DogHelp.getHelp().showHelp();	
		}
	}
	
	public void setParams(DogParams newParams) {
		dogParams = newParams;
		saveConfig();
	}
	
	public void saveConfig() {
		ObjectOutputStream os;
		try {
			os = new ObjectOutputStream(new FileOutputStream(getConfigFile()));
			os.writeObject(dogParams);
		} catch (Exception Ex) {
			System.out.println(Ex);
			return;
		}
		try {
			os.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private boolean loadConfig() {
		File conFile = getConfigFile();
		if (conFile.exists() == false) {
			System.out.println("No PamDog config file");
			return false;
		}
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(conFile));
			Object o = ois.readObject();
			if (o != null && DogParams.class == o.getClass()) {
				dogParams = (DogParams) o;
			}
			ois.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private File getConfigFile() {
		String setFileName = getPamguardFolder() + File.separator + "PamDogSettings.pds";
		return new File(setFileName);
	}
	/**
	 * Get the settings folder name and if necessary, 
	 * create the folder since it may not exist. 
	 * @return folder name string, (with no file separator on the end)
	 */
	public static String getPamguardFolder() {
		String settingsFolder = System.getProperty("user.home");
		settingsFolder += File.separator + "Pamguard";
		// now check that folder exists
		File f = new File(settingsFolder);
		if (f.exists() == false) {
			f.mkdirs();
		}
		return settingsFolder;
	}
	
}
