package command;

import pamdog.ControlMessage;
import pamdog.DogControl;

public class TerminalCommandManager extends DogCommandManager {



	public TerminalCommandManager(DogControl dogControl) {
		super(dogControl);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean sendData(ControlMessage extCommand, String dataString) {
		// TODO Auto-generated method stub
		return false;
	}

}
