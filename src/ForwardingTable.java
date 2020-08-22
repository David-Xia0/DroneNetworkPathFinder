import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;



public class ForwardingTable {

	//Name of this drone
	private static final String droneName = "Relay1";

	private HashMap<String,HashMap<String,String[]>> dvUpdates = new HashMap<String,HashMap<String,String[]>>();
	private HashMap<String,String[]> fwTable = new HashMap<String,String[]>();
	private HashMap<String,String[]> clients = new HashMap<String,String[]>();
	private HashMap<String,String> changes = new HashMap<String,String>();
	private HashMap<String,Integer>  tempChanges = new HashMap<String,Integer>();

	ForwardingTable(List<String[]> clients){
		for(String[] client : clients) {

			if(!client[3].equals("-1")) {
				fwTable.put(client[0],new String[] {droneName,client[3], client[0]});	
				dvUpdates.put(client[0],new HashMap<String,String[]>());
				this.clients.put(client[0],new String[] {droneName,client[3],client[0]});
			}	

		}
		this.clients.put(droneName, new String[] {droneName, "0", droneName});
	}


	/**
	 * 
	 * @param drone
	 * @return
	 */
	private String[] previousUpdate(String drone) {


		int minCost = -1;
		String relay = "";


		for(Map.Entry<String, HashMap<String,String[]>> dvUpdate : dvUpdates.entrySet()) {

			HashMap<String,String[]> dest = dvUpdate.getValue();

			if(dest.containsKey(drone) && !dest.get(drone)[1].equals("-1")) {
				if(relay.length()==0 || minCost==-1) {
					relay=dvUpdate.getKey();
					minCost=Integer.parseInt(dest.get(drone)[1]) + Integer.parseInt(clients.get(relay)[1]) ;
				}else if(Integer.parseInt(dest.get(drone)[1])<minCost) {
					relay=dvUpdate.getKey();
					minCost=Integer.parseInt(dest.get(drone)[1])+ Integer.parseInt(clients.get(relay)[1]) ;
				}
			}
		}


		if(clients.containsKey(drone)) {
			if(minCost == -1) {
				relay=drone;
				minCost=Integer.parseInt(clients.get(drone)[1]);
			}else if(!clients.get(drone)[1].equals("-1") && Integer.parseInt(clients.get(drone)[1])<minCost) {
				relay=drone;
				minCost=Integer.parseInt(clients.get(drone)[1]);
			}
		}


		///////////////////////////////////////////////////////////////////////////////////////

		if(!(minCost==-1)) {
			if(fwTable.get(drone)[0].equals(relay)) {
				System.out.println("- Calculating cost for "
						+ drone
						+ "...no change");
			}else {
				fwTable.put(drone,new String[] { relay, minCost+"" });
				changes.put(relay,minCost +"");
				System.out.println("- Calculating cost for "
						+ drone
						+ "...cost updated to "+ minCost+ " via " 
						+ relay);

			}

		}else {

			if(fwTable.remove(drone)!=null) {
				changes.put(drone,-1+"");
				System.out.println("- Calculating cost for "
						+ drone
						+ "...cost updated to -1 via " 
						+ drone);
			}else {
				System.out.println("- Calculating cost for "
						+ drone
						+ "...no change");
			}
		}
		return new String[]{ relay, minCost+"" };
	}


	public List<String[]> insertDvUpdate(String[] update){

		List<String[]> dvUpdate = new ArrayList<String[]>();
		for(String dest : update[2].split(",")) {
			dvUpdate.add(dest.split("="));

		}

		HashMap<String,String[]> prev = new HashMap<String,String[]>();

		if(dvUpdates.containsKey(update[1])) {
			prev = dvUpdates.get(update[1]);
		}

		for(String[] newDvUpdate : dvUpdate) {

			if(!newDvUpdate[1].equals("-1")) {

				/*  Costs are no the same both ways so the below code is ommitted
				if(clients.containsKey(newDvUpdate[0])) {
					HashMap<String,String[]> reverseDvUpdate = dvUpdates.get(newDvUpdate[0]);
					reverseDvUpdate.put(update[1],new String[] {newDvUpdate[0],newDvUpdate[1]});
				}
				 */

				prev.put(newDvUpdate[0],new String[] {update[1],newDvUpdate[1],newDvUpdate[0]});

			}else {
				/*
				if(clients.containsKey(newDvUpdate[0])) {
					HashMap<String,String[]> reverseDvUpdate = dvUpdates.get(newDvUpdate[0]);
					reverseDvUpdate.remove(update[1]);
				}
				 */
				prev.remove(newDvUpdate[0]);
			}
		}

		dvUpdates.put(update[1],prev);
		return dvUpdate;
	}



	/**
	 * 
	 * @param update
	 */
	public void newUpdate(List<String[]> dvUpdate, String relayDrone) {

		System.out.println("Starting DV update calculation");

		String[] relay = clients.get(relayDrone);
		int[] dvCost = new int[dvUpdate.size()];

		if(relay==null) {
			for(int i = 0 ; i<dvUpdate.size();i++) {

				String name = dvUpdate.get(i)[0];
				System.out.println("- Calculating cost for "
						+ name
						+ "...no change");
			}
			return;
		}

		//loops through the number of update changes
		for(int i = 0 ; i<dvUpdate.size();i++) {

			String name = dvUpdate.get(i)[0];

			//IF a neighbour disconnects .
			if(dvUpdate.get(i)[0].equals(droneName)) {
				System.out.println("- Calculating cost for "
						+ name
						+ "...no change");
			}else if(dvUpdate.get(i)[1].equals("-1")) {
				previousUpdate(name);
			}else{


				dvCost[i] = Integer.parseInt(relay[1])+Integer.parseInt(dvUpdate.get(i)[1]);

				if(fwTable.containsKey(dvUpdate.get(i)[0])) {
					if(fwTable.get(dvUpdate.get(i)[0])[0].equals(relayDrone)){
						System.out.println("- Calculating cost for "
								+ name
								+ "...cost updated to "+ dvCost[i]+ " via " 
								+ relayDrone);

						changes.put(name,dvCost[i]+"");
						fwTable.put(name,new String[] {relayDrone,dvCost[i]+"",relayDrone});
					}else {

						String[] dest = fwTable.get(name);


						if(dvCost[i]>=Integer.parseInt(dest[1])) {
							System.out.println("- Calculating cost for "
									+ name
									+ "...no change");
						}else {
							System.out.println("- Calculating cost for "
									+ name
									+ "...cost updated to "+ dvCost[i]+ " via " 
									+ relayDrone);

							changes.put(name,dvCost[i]+"");
							fwTable.put(name,new String[] {relayDrone,dvCost[i]+"",relayDrone});
						}
					}
				}else {

					System.out.println("- Calculating cost for "
							+ name
							+ "...cost updated to "+ dvCost[i]+ " via " 
							+ relayDrone);

					changes.put(name,dvCost[i]+"");
					fwTable.put(name,new String[] {relayDrone,dvCost[i]+"",relayDrone});
				}
			}

		}

	}





	/**
	 * 
	 */
	public void printTable() {

		for(Map.Entry<String, String[]> entry : fwTable.entrySet()) {
			if(entry.getValue()[0].equals(droneName)) {
				System.out.println(entry.getKey()+","+entry.getKey());
			}else {
				System.out.println(entry.getKey()+","+entry.getValue()[0]);////////////////////////CHANGED
			}
		}

	}


	/**
	 * 
	 */
	public void writeTable() {
		File file = new File("forwarding-"+droneName+".csv");

		try {
			PrintWriter pw = new PrintWriter(file);

			//prints each set of client drone data into a line seperated by commas
			for(Map.Entry<String, String[]> entry : fwTable.entrySet()) {

				if(entry.getValue()[0].equals(droneName)) {
					pw.println(entry.getKey()+","+entry.getKey());
				}else {
					pw.println(entry.getKey()+","+entry.getValue()[0]);/////////////////////////////CHANGED
				}
			}


			pw.close();

		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}



	/**
	 * 
	 * 
	 * @return
	 */
	public String[] getUpdate() {

		if(changes.size()==0) {
			return null;
		}

		String update = "";
		String num = changes.size() +"";
		for(Map.Entry<String, String> change: changes.entrySet()) {
			update+=change.getKey() +"="+change.getValue()+",";
		}



		tempChanges.clear();
		changes.clear();

		return new String[] {update.substring(0, update.length()-1),num};
	}


	public void fullUpdate() {

		HashMap<String,HashMap<String,String[]>> discovered = new HashMap<String,HashMap<String,String[]>>();
		discovered.put(droneName, clients);
		HashMap<String,String[]> graph = new HashMap<String,String[]>();



		int minCost = -1;
		String dest = "";
		String relay = "";
		String firstHop = "";
		boolean loop = true;

		while(loop) {
			minCost = -1;
			dest = "";
			relay = "";
			firstHop = "";
			loop = false;

			//goes through all nodes and edges
			for(Map.Entry<String,HashMap<String,String[]>> N: discovered.entrySet()) {
				for(Map.Entry<String,String[]> E: N.getValue().entrySet()) {

					if(!graph.containsKey(E.getKey())) {

						if(!N.getKey().equals(droneName)) {
							loop=true;

							int cost = Integer.parseInt(E.getValue()[1]) + Integer.parseInt(graph.get(N.getKey())[1]);
							E.getValue()[2]=graph.get(N.getKey())[2];

							if(dest.length()==0 || minCost==-1) {
								firstHop = E.getValue()[2];
								relay=N.getKey();
								dest = E.getKey();
								minCost = cost;


							}else if (minCost>cost){
								firstHop = E.getValue()[2];
								relay=N.getKey();
								dest = E.getKey();
								minCost = cost;
							}


						}else if(!E.getKey().equals(droneName)){
							loop=true;

							int cost = Integer.parseInt(E.getValue()[1]) ;

							if(dest.length()==0 || minCost==-1) {
								firstHop = E.getValue()[2];
								relay=N.getKey();
								dest = E.getKey();
								minCost = cost;
							}else if (minCost>cost){
								firstHop = E.getValue()[2];
								relay=N.getKey();
								dest = E.getKey();
								minCost = cost;
							}

						}
					}

				}
			}

			if(dvUpdates.containsKey(dest)) {
				discovered.put(dest,dvUpdates.get(dest));
			}


			/*
			for(Map.Entry<String,HashMap<String,String[]>> N: discovered.entrySet()) {
				N.getValue().remove(dest);
				N.getValue().remove(relay);
				if(N.getValue().size()==0) {
					remove.add(N.getKey());
				}

			}
			 */
			if(minCost!=-1)
				graph.put(dest, new String[] {relay,minCost + "",firstHop});
		}

		graph.remove(droneName);
		fwTable.remove(droneName);

		for(Map.Entry<String,String[]> N: graph.entrySet()) {
			if(fwTable.containsKey(N.getKey())) {
				/*
				if(!fwTable.get(N.getKey())[2].equals(N.getValue()[2])) {
					System.out.println("- Calculating cost for "
							+ N.getKey()
							+ "...cost updated to1 "+ N.getValue()[1]+ " via " 
							+ N.getValue()[2]);
					changes.put(N.getKey(),N.getValue()[1]);
					fwTable.put(N.getKey(),N.getValue());
				}else 
				 */
				if(Integer.parseInt(fwTable.get(N.getKey())[1])!=Integer.parseInt(N.getValue()[1])) {
					System.out.println("- Calculating cost for "
							+ N.getKey()
							+ "...cost updated to "+ N.getValue()[1]+ " via " 
							+ N.getValue()[2]);
					changes.put(N.getKey(),N.getValue()[1]);
					fwTable.put(N.getKey(),N.getValue());
				}else {
					System.out.println("- Calculating cost for "
							+ N.getKey()
							+ "...no change");
				}
			}else {
				System.out.println("- Calculating cost for "
						+ N.getKey()
						+ "...cost updated to "+ N.getValue()[1]+ " via " 
						+ N.getValue()[2]);
				changes.put(N.getKey(),N.getValue()[1]);
				fwTable.put(N.getKey(),N.getValue());

			}
		}	


	}

}



