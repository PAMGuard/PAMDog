package command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import pamdog.ControlMessage;
import pamdog.DogControl;


/**
 * Command manager which gets commands from the terminal
 * 
 * @author Jamie Macaulay
 *
 */
public class TerminalCommands extends DogCommandAdapter {

	
	private BufferedReader reader;
	
	public TerminalCommands(DogControl dogControl, DogCommandManager dogCommandManager) {
		super(dogControl);
		// TODO Auto-generated constructor stub
		getTerminalCommands();
	}

	@Override
	public boolean sendData(ControlMessage extCommand) {
		System.out.println(extCommand.msg);
		System.out.println(extCommand.getData());
		return true;
	}
	
	

	@Override
	public void dogClose() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getTerminalCommands() {
		Thread t = new Thread(new TerminalTread());
		t.start();
	}
	
	private class TerminalTread implements Runnable {

		@Override
		public void run() {
			readCommands();
		}
		
	}
	
	
	private void readCommands() {

        reader = new BufferedReader(
        		new InputStreamReader(System.in));
        try {
        	while (true) {
        		String command = reader.readLine();
        		if (command != null && command.length() > 0) {
        			interpretCommand(new ControlMessage(command));
        		}
//        		System.out.println("you typed: " + inLine);
//        		if (inLine.contains("exit")) {
//        			break;
//        		}
        	}
        } catch (IOException e) {
        	e.printStackTrace();
        } 
        System.out.println("Exiting PAMGuard, leave control thread");
	}
}


