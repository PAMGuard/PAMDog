package pamdog.remote;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetInterfaceFinder {
	private static List<NetworkInterface> ipv4Interfaces;

	/**
	 * Check to see if a given interface name is available. If a config was made on 
	 * a different computer or someone has messed with hardware (e.g. removing from 
	 * a docking station) then a chose interface may no longer be available. 
	 * @param interfaceName
	 * @return true if interface is currently available. 
	 */
	public static boolean isAvailable(String interfaceName) {
		List<NetworkInterface> intfs = getIPV4Interfaces();
		for (NetworkInterface intf : intfs) {
			try {
				if (intf.getName().equals(interfaceName)) {
					return true;
				}
			}
			catch (Exception e) {
				// just in case, but can't see how - continue to next item. 
			}
		}
		return false;
	}
	
	/**
	 * Get the ipv4 address of an interface, or null if it doesn't have one
	 * @param inf Network interface
	 * @return ipv4 address. 
	 */
	public static Inet4Address getIPV4Address(NetworkInterface inf) {
		List<InterfaceAddress> addrs = inf.getInterfaceAddresses();
		for (InterfaceAddress addr : addrs) {
			InetAddress inetAddr = addr.getAddress();
			if (inetAddr instanceof Inet4Address) {
				return (Inet4Address) inetAddr;
			}
		}
		return null;
	}
	
	/**
	 * Static function to get all network interfaces that can handle ipv4. 
	 * Listing them seems to take a while, it it enumerates on the first call, 
	 * then uses old list unless told to relist.
	 * @param relist always relist interfaces, otherwise use previously returned list 
	 * @return list of interfaces that can handle ipv4 addresses. 
	 */
	public synchronized static List<NetworkInterface> getIPV4Interfaces(boolean relist) {
		if (ipv4Interfaces == null || relist) {
			ipv4Interfaces = findIPV4Interfaces();
		}
		return ipv4Interfaces;
	}
	
	/**
	 * Static function to get all network interfaces that can handle ipv4. 
	 * Listing them seems to take a while, it it enumerates on the first call, then remembers.
	 * @return list of interfaces that can handle ipv4 addresses. 
	 */
	public synchronized static List<NetworkInterface> getIPV4Interfaces() {
		return getIPV4Interfaces(false);
	}
	
	/**
	 * Find all ipv4 Network interfaces
	 * @return
	 */
	private static List<NetworkInterface> findIPV4Interfaces() {
		ArrayList<NetworkInterface> nifs = new ArrayList<>();
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface inf = interfaces.nextElement();
				if (getIPV4Address(inf) != null) {
					nifs.add(inf);
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		return nifs;
	}
}
