package pamdog;

public enum UdpCommands {
	PING("ping"),
	EXIT("Exit"),
	START("start"),
	STOP("stop"),
	STATUS("Status"),
	SUMMARY("summary"),
	KILL("kill");

	private final String value;

	UdpCommands(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	/** Return the raw string value for this command. */
	public String value() {
		return value;
	}

	/**
	 * Return the enum matching the given string (matches either the enum name or the configured string),
	 * or null if none match.
	 */
	public static UdpCommands fromString(String s) {
		if (s == null) return null;
		for (UdpCommands c : values()) {
			if (c.value.equalsIgnoreCase(s) || c.name().equalsIgnoreCase(s)) return c;
		}
		return null;
	}
	
	public static boolean isValidCommand(String command) {
		for (UdpCommands c : values()) {
			if (c.value.equalsIgnoreCase(command) || c.name().equalsIgnoreCase(command)) return true;
		}
		return false;
	}

	// Keep integer status constants for backward compatibility
	public static final int PAM_IDLE = 0;
	public static final int PAM_RUNNING = 1;
	public static final int PAM_STALLED = 3;
	public static final int PAM_INITIALISING = 4;
}