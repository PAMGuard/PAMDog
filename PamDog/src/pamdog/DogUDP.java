package pamdog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class DogUDP {

	private IdleFunction idleFunction;
//	private int portId;
	private InetAddress inetAddr;
	DatagramSocket socket;
	private String lastError;
	private DogControl dogControl;
	private int currentUdpPort;

	public DogUDP(DogControl dogControl, IdleFunction idleFunction) {
		this.idleFunction = idleFunction;
		this.dogControl = dogControl;
		currentUdpPort = dogControl.getParams().getUdpPort();
		inetAddr = InetAddress.getLoopbackAddress();
//		try {
//			inetAddr = InetAddress.getLocalHost();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}


	/**
	 * Send a command to PAMGuard. 
	 * @param command
	 * @param timeout
	 * @return
	 */
	synchronized public String sendCommand(String command, int timeout) {
		int port = currentUdpPort;
		if(currentUdpPort==0) {
			return null;
		}
		
		flushSocket(port);
		
//		System.out.printf("Send command %s to %s port %d\n", command, inetAddr.toString(), port);
//		if (curidleFunction.dogParams.getUdpPort();
		byte[] bytes = command.getBytes();
		DatagramPacket outPacket = new DatagramPacket(bytes, bytes.length, inetAddr, port);
		try {
			socket.send(outPacket);
		} catch (IOException e) {
			e.printStackTrace();
			lastError = e.getMessage();
			return null;
		}
		/*
		 * Now read back from the server. 
		 */
		byte[] returnBytes = new byte[128];
		DatagramPacket inPacket = new DatagramPacket(returnBytes, returnBytes.length);
		try {
			socket.setSoTimeout(timeout);
			socket.receive(inPacket);
		} catch (IOException e) {
//			e.printStackTrace();
			lastError = e.getMessage();
			System.out.println(lastError + " from command \"" + command + "\"");
			return null;
		}
		String received = new String(inPacket.getData(), 0, inPacket.getLength());
		lastError = null;
		return received;
	}


	/**
	 * flush any old data out of the socket that might have been 
	 * left there if PAMguard was slow to respond to an earlier request. 
	 * @param port
	 */
	private void flushSocket(int port) {
		/*
		 * Now read back from the server. 
		 */
		byte[] returnBytes = new byte[128];
		DatagramPacket inPacket = new DatagramPacket(returnBytes, returnBytes.length);
		
		while(true) {
		try {
			socket.setSoTimeout(1);
			socket.receive(inPacket);
			System.out.println("Packet flushed from socket: " + new String(inPacket.getData(), 0, inPacket.getLength()));
		} catch (IOException e) {
//			e.printStackTrace();
			lastError = e.getMessage();
			return;
		}
		}
		// TODO Auto-generated method stub
		
	}


	/**
	 * @return the lastError
	 */
	public String getLastError() {
		return lastError;
	}


	/**
	 * @param lastError the lastError to set
	 */
	public void setLastError(String lastError) {
		this.lastError = lastError;
	}
	
	/**
	 * find a free port between the given range
	 * @param startId
	 * @param endId
	 * @return -1 if no port can be found
	 */
	public static int findFreePort(int startId, int endId) {
		for (int p = startId; p<= endId; p++) {
			if (portAvailable(p)) {
				return p;
			}
		}
		return -1;
	}
	
	/**
	 * check to see if a port is available. 
	 * @param portId
	 * @return true if it's available. 
	 */
	public static boolean portAvailable(int portId) {
		DatagramSocket ds = null;
		boolean socketOk = true;
	    try {
	        ds = new DatagramSocket(portId);
	        ds.setReuseAddress(true);
	        socketOk = true;
	    } 
	    catch (IOException e) {
	    	socketOk = false;
	    } 
	    if (ds != null) {
	    	ds.close();
	    }
	    return socketOk;
	}


	public void setCurrentUdpPort(int freePort) {
//		currentUdpPort = freePort;
	}
}
