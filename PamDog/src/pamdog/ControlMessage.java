package pamdog;

public class ControlMessage {

	public String msg;
	
	public String data;

	public ControlMessage(String string) {
		this.msg = string;
	}

	public String getData() {
		return data;
	}


	public void setData(String data) {
		this.data = data;
	}




	public String getCommand() {
		return msg;
	}

}
