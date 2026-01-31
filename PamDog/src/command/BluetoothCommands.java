package command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import com.fazecast.jSerialComm.SerialPort;

import pamdog.ControlMessage;

/**
 * Send Bluetooth commands and recieve bluetooth data. 
 */
public class BluetoothCommands extends DogCommandAdapter {

	public BluetoothCommands(DogCommandManager dogCommandManager) {
		super(dogCommandManager);
		
		// start the bluetooth server on a different thread
		startBluetoothServer();
		// create a side panel for the bluetooth controller (optional)
	}


	@Override
	void dogClose() {
		stopBluetoothServer();
	}
	
	/**
	 * Send a PAMGuard command from a string received over Bluetooth.
	 */
	public void sendPAMGComand(String command) {

		//make sure this is done on the EDT
		//SwingUtilities.invokeLater(() -> {
			interpretCommand(new ControlMessage(command));
		//});
	}

	@Override
	public boolean sendData(ControlMessage extCommand) {
		// Send data received over Bluetooth to the PAMGuard command manager.
		printBT(" DATA SENT FROM COMMAND : " + extCommand.getCommand() + " DATA STRING: " +  extCommand.getData());

		/**
		 * Helps to print informational messages only when verbosity is enabled.
		 */
		try {
			comPort.getOutputStream().write(("RPLY: " + extCommand.getCommand()).getBytes());

			if (extCommand.getData()!=null) {
				comPort.getOutputStream().write(extCommand.getData().getBytes());
			}
		} catch (Exception e) {
			// If the write fails, the connection is definitely gone
			printBT("Write failed: Connection lost.");
		}

		return false;
	}
	
	
	private static Process rfcommProcess;

	public static String defaultName = "Bluetooth Controller";

	private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

	private final int MAX_MESSAGES = 50;

	private final Deque<String> recentMessages = new ArrayDeque<>();

	private final String portPath = "/dev/rfcomm0";

	private volatile SerialPort comPort = null;

	private volatile boolean serverRunning = false;

	private Thread serverThread;


	private PamBluetoothSettings bluetoothParams = new PamBluetoothSettings();

	public static final String unitType = "Bluetooth Control";

	// Verbosity flag - when true logging via log/logErr will print to stdout/stderr
	private volatile boolean verbose = false;

	/**
	 * Control verbosity for runtime logging coming from this controller.
	 * Default is true.
	 */
	public void setVerbose(boolean v) {
		this.verbose = v;
	}

	public boolean isVerbose() {
		return this.verbose;
	}


	private void printBT(String msg) {
		if (verbose) System.out.println("BluetoothControl: " + msg);
	}

	/**
	 * Helper to print error messages only when verbosity is enabled.
	 */
	private void printBTErr(String msg) {
		if (verbose) System.err.println("BluetoothControl: " + msg);
	}


	public boolean isPhoneConnected() {
		// Phone is connected if comPort is non-null and open
		return comPort != null && comPort.isOpen();
	}

	public void addStatusListener(Runnable listener) {
		listeners.addIfAbsent(listener);
	}

	public String[] getRecentMessages() {
		synchronized (this) {
			return recentMessages.toArray(new String[0]);
		}
	}

	/**
	 * Add a received message to the recent buffer and notify listeners.
	 */
	public synchronized void addReceivedMessage(String message) {
		if (message == null) return;
		// Keep newest messages at the front
		recentMessages.addFirst(message);
		while (recentMessages.size() > MAX_MESSAGES) {
			recentMessages.removeLast();
		}
		// Notify listeners
		for (Runnable r : listeners) {
			try { r.run(); } catch (Exception e) { /* ignore listener errors */ }
		}
	}

	public void removeStatusListener(Runnable r) { if (r != null) listeners.remove(r); }

	/**
	 * Start the Bluetooth server in a background thread. It will run until stopBluetoothServer()
	 * is called or the JVM exits.
	 */
	public synchronized void startBluetoothServer() {
		if (serverRunning) {
			printBT("Bluetooth server already running");
			return;
		}
		serverRunning = true;
		serverThread = new Thread(() -> {
			try {
				printBT("--- Starting Bluetooth Management (background thread) ---");
				try {
					setupBluetooth();
					if (bluetoothParams.bluetoothPairing) {
						printBT("Bluetooth pairing is enabled.");
						String[] commands = {
								"sudo bluetoothctl discoverable on",
						};

						sendBluetoothCommands( commands);
					} else {
						printBT("Bluetooth pairing is disabled.");
					}
				} catch (Exception e) {
					printBTErr("Failed to setup bluetooth: " + e.getMessage());
				}

				File portFile = new File(portPath);

				while (serverRunning) {
					printBT("--- READY --- Waiting for phone to connect...");

					while (serverRunning && !portFile.exists()) {
						try { Thread.sleep(1000); } catch (InterruptedException e) { /* ignore */ }
					}

					if (!serverRunning) break;

					printBT("Phone connected! Starting session...");

					try {
						// Attempt to open the serial port
						comPort = SerialPort.getCommPort(portPath);
						comPort.setBaudRate(115200);

						handleSession(comPort);

						comPort = null;

						int i = 0;
						while (portFile.exists()) {
							// give some time for the port to close properly
							printBT("Waiting for phone to disconnect..." + i);
							Thread.sleep(500);
							i++;
							if (i > 10) {
								printBTErr("Some big error had happened here...");
								break;
							}
						}
					}
					catch (Exception e) {
						// if there is an error opening the com port give the computer some time
						printBTErr("Error during session handling: " + e.getMessage());
						comPort = null;
						try { Thread.sleep(1000); } catch (InterruptedException ex) { /* ignore */ }
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (rfcommProcess != null) {
					rfcommProcess.destroy();
				}
				printBT("--- Bluetooth Management Stopped (background thread) ---");
			}
		});
		serverThread.setDaemon(true);
		serverThread.setName("PamBluetoothServer");
		serverThread.start();
	}

	/**
	 * Stop the background bluetooth server. This will attempt to terminate the rfcomm watch process
	 * and stop waiting for new connections.
	 */
	public synchronized void stopBluetoothServer() {
		if (!serverRunning) return;
		serverRunning = false;
		// Interrupt the thread in case it's sleeping
		if (serverThread != null) serverThread.interrupt();
		// Try to close any open port
		try {
			if (comPort != null && comPort.isOpen()) {
				comPort.closePort();
			}
		} catch (Exception e) { /* ignore */ }
		if (rfcommProcess != null) {
			rfcommProcess.destroy();
		}
	}

	/**
	 * Handle a session with a connected phone over the given com port.
	 * This method blocks until the session ends (phone disconnects).
	 */
	private boolean handleSession(SerialPort comPort) {
		int i = 0;

		if (comPort.openPort()) {
			printBT("SUCCESS: Connected to phone.");
			while (true) {
				// Read messages from phone
				try (Scanner scanner = new Scanner(comPort.getInputStream())) {
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						printBT("Phone/Tablet/Computer says: " + line);
						// Dispatch to handler
						try {
							addReceivedMessage(line);
							sendPAMGComand(line);
							comPort.getOutputStream().write(("ACK: " + line + "\n").getBytes());
						} catch (Exception ex) {
							printBTErr("Error handling command: " + ex.getMessage());
						}
					}
				} catch (Exception e) {
					printBTErr("Session read error: " + e.getMessage());
				}

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// restore interrupt status and break
					Thread.currentThread().interrupt();
					break;
				}

				// check if the port is still open
				if (!new File(portPath).exists()) {
					printBT("DISCONNECTED: Phone disconnected.");
					break;
				} else {
					if (i % 500 == 0) {
						try {
							//note - cannot send empty strings, so send newline as heartbeat
							comPort.getOutputStream().write("\n".getBytes());
						} catch (Exception e) {
							// If the write fails, the connection is definitely gone
							printBT("Bluetooth Control: Write failed: Connection lost.");
							break;
						}

						printBT("Listening for messages from phone...");
					}
				}
				i++;
			}

			// very important to close the port when done
			boolean portClosed = comPort.closePort();

			if (!portClosed) {
				printBTErr("ERROR: Could not close the port properly.");
			} else {
				printBT("Port closed successfully.");
			}

			// Notify listeners about disconnection
			for (Runnable r : listeners) { try { r.run(); } catch (Exception e) {} }

			return portClosed;
		}
		return false;
	}

	/**
	 * Setup Bluetooth services and start rfcomm listener. Note this is Linux-specific.
	 */
	private static void setupBluetooth() throws IOException, InterruptedException {
		// Force a clean state and register the service


		String[] commands = {
				"sudo bluetoothctl power on",
				//"sudo bluetoothctl discoverable on",
				"sudo bluetoothctl pairable on",
				"sudo sdptool add SP", // This registers the "Serial Port" service
				"sudo chmod 777 /var/run/sdp" // Fixes the permission bug
		};

		sendBluetoothCommands( commands);
	}

	private static void sendBluetoothCommands(String[ ] commands) throws IOException, InterruptedException {
		for (String cmd : commands) {
			Runtime.getRuntime().exec(cmd).waitFor();
		}

		// Start the listener on Channel 1
		ProcessBuilder pb = new ProcessBuilder("sudo", "rfcomm", "watch", "hci0", "1");
		rfcommProcess = pb.start();
	}


	/**
	 * @return the haggisParameters
	 */
	public PamBluetoothSettings getBluetoothParams() {
		return bluetoothParams;
	}


}
