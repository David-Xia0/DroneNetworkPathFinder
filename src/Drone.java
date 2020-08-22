
public class Drone {


	private String name;
	private String type;
	private String ip;
	private String port;
	private String cost;


	Drone(String name, String type, String ip, String port, String cost){
		this.name=name;
		this.type=type;
		this.ip=ip;
		this.port=port;
		this.cost=cost;
	}


	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getIP() {
		return ip;
	}

	public String getPort() {
		return port;
	}

	public String getCost() {
		return cost;
	}

	public void setCost(String cost) {
		this.cost=cost;
	}




}
