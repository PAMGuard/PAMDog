package command;

import java.util.ArrayList;
import java.util.List;

import pamdog.DogControl;

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
		commandManagers.add(new TerminalCommands(dogControl, this));
	}

}
