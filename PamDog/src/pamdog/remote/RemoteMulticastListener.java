package pamdog.remote;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import pamdog.DogControl;

/**
 * Multicast listener for PAMDog which will listen for contacts and instructions 
 * from the PAMGuard batch job controller. 
 * @author dg50
 *
 */
public class RemoteMulticastListener {

	private DogControl dogControl;
	private RemoteControlAgent remoteControlAgent;
	
	private Thread listenerThread;
	
	private volatile boolean keepListening;
	private MulticastSocket socket;
	
	static private final int MAX_COMMAND_LENGTH = 65000;
	
	private byte[] byteBuffer = new byte[MAX_COMMAND_LENGTH];
	
	private ArrayList<byte[]> psfxBits;

	public RemoteMulticastListener(DogControl dogControl, RemoteControlAgent remoteControlAgent) {
		this.dogControl = dogControl;
		this.remoteControlAgent = remoteControlAgent;
		
		startListening();
	}

	private void startListening() {
		stopListening();
		
		listenerThread = new Thread(new Listener(), "Multicast control listener");
		listenerThread.start();
	}

	private void stopListening() {
		// TODO Auto-generated method stub
		
	}

	private class Listener implements Runnable {

		@Override
		public void run() {
			multicastListen();
		}
		
	}

	public void multicastListen() {
		keepListening = true;

		InetAddress mcastaddr;
		try {
			mcastaddr = InetAddress.getByName(RemoteControlAgent.defaultAgentAddr);
			InetSocketAddress group = new InetSocketAddress(mcastaddr, RemoteControlAgent.defaultAgentPort);
			NetworkInterface netIf = NetworkInterface.getByName("eth8");
			socket = new MulticastSocket(RemoteControlAgent.defaultAgentPort);
			socket.joinGroup(group, netIf);
			socket.setSoTimeout(0);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		System.out.printf("Waiting for multicast messages at %s port %d\n", RemoteControlAgent.defaultAgentAddr, RemoteControlAgent.defaultAgentPort);
		
		while (keepListening) {
			try {
				DatagramPacket datagram = new DatagramPacket(byteBuffer, MAX_COMMAND_LENGTH);
				socket.receive(datagram);
				processDatagram(datagram);
			}
			catch (IOException ioE) {
				ioE.printStackTrace();
				break;
			}
		}
	}

	private void processDatagram(DatagramPacket datagram) {
//		String message = new String(datagram.getData(), 0, datagram.getLength());
//		System.out.println("Dog Agent Message received " + message);
//		if (message == null) {
//			return;
//		}
		byte[] data = Arrays.copyOf(datagram.getData(), datagram.getLength());
		String command = getStringItem(data, 0);
		switch (command) {
		case "hello":
			helloCommand(datagram);
			return;
		case "psfxdata":
			psfxData(datagram);
			return;
		case "launchjob":
			launchBatchJob(datagram);
		default:
			System.out.println("Unknown multicast command: " + command);
		}
	}
	
	/**
	 * Launch a command. Command format is launchjob,computername,commandparams
	 * @param datagram
	 */
	private void launchBatchJob(DatagramPacket datagram) {
		ComputerInfo computerInfo = remoteControlAgent.getComputerInfo();
		byte[] data = Arrays.copyOf(datagram.getData(), datagram.getLength());
		String command = getStringItem(data, 0);
		String computer = getStringItem(data, 1);
		if (computerInfo.getComputerName().equals(computer) ==  false) {
			return;
		}
		String commands = getStringItem(data, 2);
		
	}

	private void psfxData(DatagramPacket datagram) {
		// unpack the psfx data. 
		ComputerInfo computerInfo = remoteControlAgent.getComputerInfo();
		byte[] data = Arrays.copyOf(datagram.getData(), datagram.getLength());
		String command = getStringItem(data, 0);
		String computer = getStringItem(data, 1);
		if (computerInfo.getComputerName().equals(computer) ==  false) {
			return;
		}
		String bitNo = getStringItem(data, 2);
		String bitNos = getStringItem(data, 3);
		String checkSum = getStringItem(data,4);
		int iBit = Integer.valueOf(bitNo);
		int nBits = Integer.valueOf(bitNos);
		int lastComma = getCommaPos(data, 4);
		byte[] psfxBit = Arrays.copyOfRange(data, lastComma+1, data.length);
		byte chkSum = 0;
		for (int j = 0; j < psfxBit.length; j++) {
			chkSum ^= psfxBit[j];
		}
		String rxCheck = String.format("%02X", chkSum);
		if (rxCheck.equalsIgnoreCase(checkSum) == false) {
			sendReply(datagram, "PSFX Data Checksum Error");
		}
		
		if (iBit == 1) {
			psfxBits = new ArrayList<>();
		}
		if (psfxBits == null) {
			sendReply(datagram, "PSFX Data Error");
		}
		if (iBit != psfxBits.size()+1) {
			String error = String.format("PSFX Data Error. Expecting part %d of %d but got part %d", psfxBits.size()+1, nBits, iBit);
			sendReply(datagram, "PSFX Data Error");
		}
		psfxBits.add(psfxBit);
		
		if (iBit == nBits) {
			boolean ok = createPSFXFile(psfxBits);
			psfxBits = null;
			sendReply(datagram, "PSFXOK");
		}
		else {
			sendReply(datagram, "PSFXBITOK");
		}
	}

	private boolean createPSFXFile(ArrayList<byte[]> psfxBits) {
		// got the data ok. 
		// what folder and name are we going to give this thing ? 
		String folder = remoteControlAgent.psfStorageFolder();
		String psfName = folder + File.separator + "BatchSettings.psfx";
		File psxFile = new File(psfName);
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(psxFile));
			for (int i = 0; i < psfxBits.size(); i++) {
				bos.write(psfxBits.get(i));
			}
			bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private String getStringItem(byte[] data, int iItem) {
		byte[] dataBit = getDataItem(data, iItem);
		String str = new String(dataBit);
		return str;
	}
	
	private byte[] getDataItem(byte[] data, int iItem) {
		int c1, c2;
		c1 = getCommaPos(data, iItem-1);
		c2 = getCommaPos(data, iItem);
		byte[] dataBit = Arrays.copyOfRange(data, c1+1, c2);
		return dataBit;
	}
	
	/**
	 * Get the position of a comma in the data. Note that
	 * everything is 0 index, so if you want the start of the
	 * array, don't call this. commaIndex 0 will return the position
	 * of the first actual comma.  
	 * @param data
	 * @param commaIndex
	 * @return
	 */
	private int getCommaPos(byte[] data, int commaIndex) {
		if (commaIndex < 0) {
			return -1;
		}
		int found = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == ',') {
				if (found++ == commaIndex) {
					return i;
				}
			}
		}
		return data.length;
	}


	private void helloCommand(DatagramPacket datagram) {
		// need to get some information about this computer and say hello back. 
		// I guess the main identifier we want is the computers ip addess. I think this
		// only comes in the message sent back though, so probably no need to find it. 
		
		ComputerInfo computerInfo = remoteControlAgent.getComputerInfo();
		
		String replyStr = String.format("helloback,%s,%s,%s,%d", computerInfo.getComputerName(), computerInfo.getOsName(), computerInfo.getOsArch(), computerInfo.getnProcessors());
//		System.out.println("Dog reply:" + replyStr);
		sendReply(datagram, replyStr);
	}

	private boolean sendReply(DatagramPacket rxDatagram, String replyStr) {
		DatagramPacket packet = new DatagramPacket(replyStr.getBytes(), replyStr.length());
		packet.setAddress(rxDatagram.getAddress());
		packet.setPort(rxDatagram.getPort());
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
