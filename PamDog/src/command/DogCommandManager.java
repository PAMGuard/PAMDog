package command;

import java.util.ArrayList;
import java.util.List;

import pamdog.ControlMessage;
import pamdog.DogControl;
import pamdog.UdpCommands;

/**
 * Manages commands from different command adapters so that the DogControl can make decision 
 * on how the watchdog should respond.
 */
public class DogCommandManager {
	
	public List<DogCommandAdapter> commandManagers;
	
	/** 
	 * Reference to the DogControl
	 */
	public DogControl dogControl; 

	
	public DogCommandManager(DogControl dogControl) {
		this.dogControl = dogControl; 
		createCommadnManagers();
	}

	
	/**
	 * Creates the implmented commands for the DogControl
	 */
	private void createCommadnManagers(){
		commandManagers = new ArrayList<DogCommandAdapter>();
		
		/***Add any new command managers here****/
		commandManagers.add(new TerminalCommands(this));
		commandManagers.add(new BluetoothCommands(this));
	}
	
	
	/**
	 * Interpret and act on a udp command string. 
	 * @param commandBytes 
	 * @param command command string
	 * @return false if the command was to exit
	 * the program (in which case this thread will
	 * exit and close the port). True otherwise. 
	 */
	public ControlMessage interpretCommand(ControlMessage commandMessage) {
		//need t create a function in dogControl which allows for any command to be sent. 
		
		//Then we record these commands so that we always know what has been sent - probably just do this via a 
		//text log That way we know if we have deployed the device .i.e. that it should be running or whether the device should 
		//not start;
		
		//now it's very important that we check for a start and stop commands. These set the deploy flag in the params.
		//If someone manually sets PAMguard to stop or start the watchdog needs to remember that, even if the computer 
		//reboots
		if (commandMessage.getCommand().equalsIgnoreCase(UdpCommands.START)) {
			//If a manual stop flag has been sent then we need to make sure that the deploy flag is set to false
			//so that PamDog keeps PAMGuard open but does NOT start it running. 
			dogControl.getParams().setDeploy(true); 
			this.dogControl.getConfigSettings().saveConfig(dogControl.getParams());
		}
		
		if (commandMessage.getCommand().equalsIgnoreCase(UdpCommands.STOP)) {
			//If a manual stop flag has been sent then we need to make sure that the deploy flag is set to false
			//so that PamDog keeps PAMGuard open but does NOT start it running. 
			dogControl.getParams().setDeploy(false); 
			this.dogControl.getConfigSettings().saveConfig(dogControl.getParams());
		}

		
		//Now the command can be sent to PamGuard via the DogControl. If a start or stop then the deploy flag should mean the watchdog
		//sits and waits for the user to start or stop. 
		ControlMessage controlMessage = dogControl.sendPamguardCommand(commandMessage.getCommand(), 1000);
		
		
		return controlMessage; 
	 }

}
