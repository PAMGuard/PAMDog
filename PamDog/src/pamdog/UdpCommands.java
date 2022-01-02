package pamdog;

public class UdpCommands {

	public static final String PING = "ping";
	public static final String EXIT = "Exit";
	public static final String START = "start";
	public static final String STOP = "stop";
	public static final String STATUS = "Status";
	public static final String SUMMARY = "summary";
	public static final String KILL = "kill";

	public static final int PAM_IDLE = 0;
	public static final int PAM_RUNNING = 1;
	public static final int PAM_STALLED = 3;
	public static final int PAM_INITIALISING = 4;
}
