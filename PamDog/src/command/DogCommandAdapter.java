package command;

import pamdog.ControlMessage;
import pamdog.DogControl;

/**
 *
 * A command manager to handle commands from an external interface - for example UDP or terminal.
 * 
 * The command manager is responsible for interpreting commands and sending data back to whatever interface is used. 
 * Note that this is an abstract class - specific implementations must be provided for each interface type.
 * 
 * Add interfaces by extending this class and then adding it to the commadnINterfaces list in DogControl.
 * 
 * @author Jamie Macaulay
 *
 */
public abstract class DogCommandAdapter {
	
	public ControlMessage lastSentMessage = null;
	
	private DogControl dogControl;

	public DogCommandAdapter(DogControl dogControl) {
		this.dogControl = dogControl;
	}
	
	/**
	 * Send data received from PAMGuard to whatever interface is being used - ofr example printing to screen. 
	 * @param extCommand - the command that requested the data
	 * @param dataString - the data to be sent
	 * @return
	 */
	abstract public boolean sendData(ControlMessage extCommand); 
	
	/**
	 * Close any resources used by this command interface. 
	 */
	abstract void dogClose(); 

	/**
	 * Interpret and act on a udp command string. 
	 * @param commandBytes 
	 * @param command command string
	 * @return false if the command was to exit
	 * the program (in which case this thread will
	 * exit and close the port). True otherwise. 
	 */
	public boolean interpretCommand(ControlMessage commandString) {
		//need t create a function in dogControl which allows for any command to be sent. 
		
		//Then we record these commands so that we always know what has been sent - probably just do this via a 
		//text log That way we know if we have deployed the device .i.e. that it should be running or whether the device should 
		//not start
		
		
		ControlMessage controlMessage = dogControl.sendPamguardCommand(commandString.getCommand(), 1000);
		
		sendData(controlMessage);
		
		return true; 
	 }
	
}
