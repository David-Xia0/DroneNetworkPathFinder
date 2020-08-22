import java.io.*;
import java.util.*;
import java.net.*;

public class DSRSNetwork {


	//Name of this drone
	private static final String name = "Relay1";
	private static final int localPort = 10120;

	public static void main(String args[]) {

		System.out.println("Starting ping process #1");

		List<String[]> clients = readCSV();

		System.out.println("Reading client list: starting");

		System.out.format("Reading client list: finished - %d clients read%n", clients.size());

		System.out.println("Pinging all clients: starting");

		List<Integer> pingData = pingDrones(clients);

		System.out.println("Pinging all clients: finished - "+pingData.size()+" clients pinged");

		writeCSV(pingData,clients);

		System.out.println("Writing client list: started");

		System.out.println("Writing client list: finished - "+pingData.size()+" clients written");

		System.out.println("Ping Process #1 finished");

		socketListen();
	}



	public static void sendUpdate(String[] changes, List<String[]> clients) {



		if(changes==null) {
			System.out.println("Skipping DV update send");
			return;
		}

		System.out.println("Sending updated DVs");
		//goes through all neighbors and checks if they are relays
		for(String[] client : clients) {
			if(client[1].equals("Relay") && !client[3].equals("-1")) {
				try {
					System.out.print("- Sending to "+client[0]);
					String[] ipAddress = client[2].split(":");
					Socket socket = new Socket(ipAddress[0], Integer.parseInt(ipAddress[1]));
					DataInputStream dataIn = new DataInputStream(socket.getInputStream());
					DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

					dataOut.writeUTF("UPDATE:"+name+":"+changes[0]+":"+changes[1]+"\n");
					dataOut.flush();



					String msg = "";
					while(!msg.equals("ACK\n")){
						msg = dataIn.readUTF();
						if(msg.equals("NAK\n")){
							System.out.println("...could not ping");
							break;
						}

					}

					dataOut.close();
					dataIn.close();
					socket.close();
					System.out.println("...done");
				} catch (IOException e) {
					System.out.println("...could not ping");
				}

			}
		}

	}






	/**
	 * 
	 * @param socket
	 * @return
	 * @throws IOException
	 */
	public static String[] handleUpdateRecieve(Socket socket) throws IOException {
		DataInputStream dataIn = new DataInputStream(socket.getInputStream());
		DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

		String[] msg = new String[0];
		while (msg.length!=4) {
			msg = dataIn.readUTF().split(":");
			if(msg.length==4) {
				if(msg[0].equals("UPDATE")) {
					dataOut.writeUTF("ACK\n");
					dataOut.flush();

				}else {
					dataIn.close();
					dataOut.close();
					socket.close();
					return new String[0];
				}
			}
		}

		dataIn.close();
		dataOut.close();
		socket.close();
		return msg;
	}


	/**
	 * 
	 */
	public static void socketListen() {

		List<String[]> clients = readCSV();
		ForwardingTable ft = new ForwardingTable(clients);

		while(true) {
			try {
				ServerSocket serverSocket = new ServerSocket(localPort);
				Socket socket = serverSocket.accept();
				System.out.println("New DVs received");




				String[] update = handleUpdateRecieve(socket);
				//ft.insertDvUpdate(update);			
				//ft.fullUpdate();  //Full Dijkstra's implementation
				ft.newUpdate(ft.insertDvUpdate(update), update[1]);
				//ft.printTable(); //used for testing
				ft.writeTable();
				sendUpdate(ft.getUpdate(), clients);

				System.out.println("DV update calculation finished");



				socket.close();
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**	This method writes all data into a CSV file "clients-[name].csv"
	 * 
	 * @param pingData contains time taken for each response ACK ping
	 * @param clients contains previous data about each drone
	 */
	public static void writeCSV(List<Integer> pingData, List<String[]> clients ) {
		File file = new File("clients-"+name+".csv");

		try {
			PrintWriter pw = new PrintWriter(file);
			Iterator<Integer> data = pingData.iterator();

			//prints each set of client drone data into a line seperated by commas
			for(String[] client : clients) {
				pw.println(client[0]+","+client[1]+","+client[2]+","+data.next());
			}
			pw.close();

		}catch(FileNotFoundException e) {
		}
	}

	//MAYBE CONVERT TO HASHTABLE
	/**	Reads CSV file "clients-[name].csv", data is return as a List<String[]>
	 * 
	 * @return
	 */
	public static List<String[]> readCSV(){

		try {
			BufferedReader br = new BufferedReader(new FileReader("clients-"+name+".csv"));
			String line;
			List<String[]> clients = new ArrayList<String[]>();

			while((line = br.readLine()) != null){
				String[] data = line.split(",");
				if(!data[0].equals(name)) {
					clients.add(data);
				}
			}
			br.close();
			return clients;
		}catch(IOException e) {
			e.printStackTrace();

		}
		return null;
	}


	/**	Pings ip addresses read from file
	 * 
	 * @param clients	file containing all address information of drones
	 * @return
	 */
	public static List<Integer> pingDrones(List<String[]> clients){

		String ping = "PING\n";
		List<Integer> pingCount = new ArrayList<Integer>();

		for(String[] client : clients){

			System.out.print("- Pinging " + client[0] + "...");

			try {
				String msg = "";
				String[] ipAddress = client[2].split(":");
				Socket socket = new Socket(ipAddress[0], Integer.parseInt(ipAddress[1]));
				DataInputStream dataIn = new DataInputStream(socket.getInputStream());
				DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
				long startTime = System.currentTimeMillis();
				boolean connected = true;

				dataOut.writeUTF(ping);
				dataOut.flush();

				//this loop waits until acknowledgement message is recieved
				while(!msg.equals("ACK\n")){
					msg = dataIn.readUTF();
					if(msg.equals("NAK\n")){
						connected=false;
						System.out.println("could not ping");
						pingCount.add(-1);
						break;
						/*
					 }else if(System.currentTimeMillis()-startTime > 5000) {
						connected=false;
						System.out.println("connection Timed out after 5s");
						pingCount.add(-1);
						break;
						 */
					}
				}

				if(connected) {
					//times how long it took to recieve acknowledgement message
					long endTime = System.currentTimeMillis();
					int time = (int)(endTime-startTime)/1000;
					System.out.println("ping received after "+time+"s");
					pingCount.add(time);
				}
				socket.close();
			}catch(IOException e) {
				System.out.println("could not ping");
				pingCount.add(-1);
			}
		}
		return pingCount;
	}
}
