package goDaddy;

/*
 * Project 2: Asynchronous Bellman Ford Algorithm 
 * 
 * Authors: Sarat Chandra Varanasi, Abhishek Thangudu, Manoj Kumar Natha
 * 
 * Bellman Ford Algorithm is implemented in asynchronous networks inducing
 * a random transmission time while transmitting messages from one process 
 * to other. The Algorithm is executed in Synchronous manner which is controlled
 * by a Master thread.
 * 
 * Termination: If there is no message in the input Queue of any process and no process
 * have relaxed in that round, then the Master collects the Parent details and send 
 * terminate message to all the Processes.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

/*
 * Structure of the Message that is passed in between the Processes
 * 
 * @fromID: ID from which message is sent
 * @startTime: Time at which message is sent
 * @transmissionTime: Randomly induced transmission time
 * @currDistance: Current distance of the Process from the Source
 */
class Message {

	private int fromID;
	private long sentRound;
	private long transmissionTime;
	private long currDistance;

	public int getFromID() {
		return fromID;
	}

	public void setFromID(int fromID) {
		this.fromID = fromID;
	}

	public long getSentRound() {
		return sentRound;
	}

	public void setSentRound(long sentRound) {
		this.sentRound = sentRound;
	}

	public long getTransmissionTime() {
		return transmissionTime;
	}

	public void setTransmissionTime(long transmissionTime) {
		this.transmissionTime = transmissionTime;
	}

	public long getCurrDistance() {
		return currDistance;
	}

	public void setCurrDistance(long currDistance) {
		this.currDistance = currDistance;
	}

}

/*
 * This class represents each Process in the network
 * 
 * @id: Unique ID of the process
 * 
 * @parent: Parent Process in the Shortest Path Tree
 * 
 * @distance: Distance from the source
 * 
 * @master: Master Process that is controlling the execution in the Network
 * 
 * @p[]: Reference of all processes in the network(but only neighbor processes
 * are accessed
 * 
 * @neighbors: List of neighbors with their weights
 * 
 * @nCount: number of neighbors
 * 
 * @messageQueue: Queue to collect messages from all the neighbors
 * 
 * @round: Execution round
 * 
 * @algoDone: Boolean to check the end of Algorithm(for terminating the
 * algorithm)
 * 
 * @messageFromMaster: To receive message from the Master Process
 * 
 * @random: Random number generator to induce random Transmission time
 */
class Process implements Runnable {

	private int id;
	private int parent;
	private long distance;

	private MasterProcess master;
	private Process p[];
	private HashMap<Integer, Integer> neighbors;
	// private volatile Queue<Message> messageQueue = new LinkedList<>();
	private volatile HashMap<Integer, Queue<Message>> link = new HashMap<>();
	private int round = 0;

	private boolean algoDone = false;
	private volatile String messageFromMaster = "";
	private Random random = new Random();

	public String getMessageFromMaster() {
		return messageFromMaster;
	}

	public void setMessageFromMaster(String messageFromMaster) {
		this.messageFromMaster = messageFromMaster;
	}

	public Process(int id, HashMap<Integer, Integer> neighbors, int source, MasterProcess master) {
		this.id = id;
		this.master = master;
		this.neighbors = new HashMap<>();
		this.neighbors = neighbors;
		if (source == id) {
			distance = 0;
			parent = id;
		} else {
			distance = Integer.MAX_VALUE;
			parent = -2;
		}
		for (Integer neighbor : neighbors.keySet()) {
			Queue<Message> queue = new LinkedList<>();
			link.put(neighbor, queue);
		}
	}

	public void setProcessNeighbors(Process p[]) {
		this.p = p;
	}

	/*
	 * To modify the input queue of the process
	 * 
	 * @insert = true implies to insert message into the Queue (Message sent
	 * from neighbor)
	 * 
	 * @insert = false implies to remove the first message in the Queue (Message
	 * received by the Process)
	 */
	public synchronized Message modifyQueue(Message message, boolean insert, int fromid, boolean check) {
		if (check) {
			for (Queue<Message> queue : link.values()) {
				if (!queue.isEmpty())
					return queue.peek();
			}
			return null;
		} else {
			Queue<Message> queue = link.get(fromid);
			if (insert) {
				queue.add(message);
				link.put(fromid, queue);
				return message;
			} else {
				if (!queue.isEmpty()) {
					Message m = queue.peek();
					if (round >= m.getSentRound() + m.getTransmissionTime()) {
						return queue.remove();
					} else
						return null;
				} else
					return null;
			}
		}
	}

	// To check if there are any messages yet to be received
	private boolean checkQueue() {
		boolean empty = false;
		for (Queue<Message> queue : link.values()) {
			if (!queue.isEmpty())
				return true;
		}
		return empty;
	}

	@Override
	public void run() {
		master.roundCompletion(id, true); // To inform Master that the Process
											// is ready !!
		while (!algoDone) {
			if (getMessageFromMaster().equals("Initiate")) {
				setMessageFromMaster("");
				if (distance == 0) {
					sendMessages();
				}
				master.roundCompletion(id, true);
			} else if (getMessageFromMaster().equals("StartRound")) {
				/*
				 * Messages in the Queue are processed based on FIFO and then
				 * the current distance is sent to all the neighbors
				 */
				round++;
				setMessageFromMaster("");
				boolean change = receiveMessages();
				if (change) {
					sendMessages();
					master.roundCompletion(id, change);
				} else {
					Message m = modifyQueue(null, false, id, true);
					if(m == null)
						master.roundCompletion(id, false);
					else
						master.roundCompletion(id, true);
				}
			} else if (getMessageFromMaster().equals("SendParent")) {
				// Sending Parent details to Master after completing the
				// algorithm
				setMessageFromMaster("");
				int weight;
				if (id == parent)
					weight = 0;
				else
					weight = neighbors.get(parent);
				master.assignParents(id, parent, weight);
				master.roundCompletion(id, true);
			} else if (getMessageFromMaster().equals("AlgoDone")) {
				// Terminating the Algorithm
				System.out.println("Process: " + id + "; Parent: " + parent + "; Distance from Source: " + distance);
				algoDone = true;
			}
		}
	}

	// Processing messages in the Queue and updating the current distance
	private boolean receiveMessages() {

		boolean change = false;
		for (Integer neighbor : link.keySet()) {
			Message message = new Message();
			message = modifyQueue(null, false, neighbor, false);
			while (message != null) {
				long newDistance = message.getCurrDistance() + neighbors.get(message.getFromID());
				if (newDistance < distance) { // Relaxation
					change = true;
					this.distance = newDistance;
					this.parent = message.getFromID();
					// System.out.println("ID: " + id + " From ID: " +
					// message.getFromID() + " Parent: " + parent+ " Distance: "
					// + distance);
				}
				message = new Message();
				message = modifyQueue(null, false, neighbor, false);
			}
		}
		return change;
	}

	// Sending current distance to all neighbors
	private void sendMessages() {
		Message message = new Message();
		message.setFromID(this.id);
		message.setCurrDistance(this.distance);
		for (int n : neighbors.keySet()) {
			message.setTransmissionTime(random.nextInt(15));
			message.setSentRound(round);
			p[n].modifyQueue(message, true, this.id, false);

		}
	}

}

/*
 * Master Process to control the execution in the network
 * 
 * @roundConf: To record the round completion from each process
 * 
 * @changes: To record changes in distances of each process in a particular
 * round
 * 
 * @parents: To record the parent of each Process in the Shortest Path Tree
 * 
 * @processCount: Number of Processes in the network
 * 
 * @algoDone: Boolean for terminating the algorithm
 * 
 * @rCount: Number of rounds executed
 * 
 * @p[]: Reference to all the Processes in the network
 */
class MasterProcess implements Runnable {

	private volatile HashMap<Integer, Boolean> roundConf = new HashMap<>();
	private volatile HashMap<Integer, Boolean> changes = new HashMap<>();
	private volatile HashMap<Integer, ArrayList<Integer>> parents = new HashMap<>();
	private int processCount;
	private boolean algoDone = false;
	private int roundCount = 0;
	private Process p[];

	public MasterProcess(int processCount) {
		this.processCount = processCount;
		for (int i = 1; i <= processCount; i++) {
			roundConf.put(i, false);
			changes.put(i, true);
		}
	}

	// To start next round
	private void startRound() {
		for (int i = 1; i <= processCount; i++) {
			p[i].setMessageFromMaster("StartRound");
		}
	}

	// To start next round
	private void initiate() {
		for (int i = 1; i <= processCount; i++) {
			p[i].setMessageFromMaster("Initiate");
		}
	}

	// Sending Terminate message to all the Processes
	private void stopAlgo() {
		for (int i = 1; i <= processCount; i++) {
			p[i].setMessageFromMaster("AlgoDone");
		}
	}

	// Request for parent Process for each Process for building the Shortest
	// path tree
	private void collectParents() {
		for (int i = 1; i <= processCount; i++) {
			p[i].setMessageFromMaster("SendParent");
		}
	}

	// Collecting round completion information from each Process
	public synchronized void roundCompletion(int id, boolean change) {
		// System.out.println("Process " + id + " completed round "
		// +roundCount);
		roundConf.put(id, true);
		changes.put(id, change);

	}

	// Assigning parents for each Process
	public synchronized void assignParents(int id, int parent, int weight) {
		ArrayList<Integer> value = new ArrayList<>();
		value.add(parent);
		value.add(weight);
		parents.put(id, value);
	}

	// Checking the change of distances for each Process
	private boolean checkChanges() {
		for (boolean b : changes.values()) {
			if (b == true) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void run() {

		while (!roundDone()) {
			// Waiting till all the Processes have started
		}
		// System.out.println("All Processes started !!");
		resetBool();
		initiate();
		while (!roundDone()) {
			// Initiating the messages from all the Processes
		}
		// System.out.println("Initiation round completed !!");
		resetBool();
		roundCount++;
		startRound();
		while (!algoDone) {
			while (!roundDone()) {
				// Waiting till all the Processes complete one round
			}
			// System.out.println("Round " +roundCount +" completed !!");
			if (checkChanges()) { // If the distance of any Process is updated,
									// cont. the algorithm
				resetBool();
				roundCount++;
				startRound();
			} else { // No more changes signifies the algorithm have completed
				resetBool();
				collectParents();
				while (!roundDone()) {
					// Waiting for all Processes to send parents
				}
				// constructTree();
				printTree();
				stopAlgo();
				algoDone = true;
			}
		}
	}

	// Constructing and printing the Shortest Paths Tree
	public void printTree() {
		System.out.println("Bellman Ford Algorithm Executed !!");
		// System.out.println("Number of rounds executed: " + roundCount);
		int resultMatrix[][] = new int[processCount][processCount];
		for (int i = 0; i < processCount; i++)
			for (int j = 0; j < processCount; j++)
				resultMatrix[i][j] = -1;

		for (Integer key : parents.keySet()) {
			int id = key;
			ArrayList<Integer> parentDetails = parents.get(key);
			int parent = parentDetails.get(0);
			resultMatrix[id - 1][parent - 1] = parentDetails.get(1);
			resultMatrix[parent - 1][id - 1] = parentDetails.get(1);
		}
		System.out.println("Following is the resultant Shortest Path Tree as a Connectivity Matrix: ");
		for (int i = 0; i < processCount; i++) {
			for (int j = 0; j < processCount; j++) {
				System.out.print(resultMatrix[i][j] + "\t");
			}
			System.out.println();
		}
	}

	// Passing the Reference of the Process
	public void setProcesses(Process p[]) {
		this.p = p;
		for (int i = 1; i <= processCount; i++) {
			p[i].setProcessNeighbors(p);
		}
	}

	// To check the completion of the Round
	private boolean roundDone() {
		for (boolean b : roundConf.values()) {
			if (b == false) {
				return false;
			}
		}
		return true;
	}

	// Reset the Round confirmation after each round
	private void resetBool() {
		for (int i = 1; i <= processCount; i++) {
			roundConf.put(i, false);
			changes.put(i, true);
		}
	}
}

public class AsynchBellmanFord {

	public static int source;
	public static int processCount;
	public static Process p[];

	public static void main(String[] args) throws FileNotFoundException {

		if (args.length == 0)
			System.out.println("Give file name in the arguments and execute");
		else {
			File file = new File(args[0]);
			Scanner sc = new Scanner(file);

			processCount = sc.nextInt();
			source = sc.nextInt();
			p = new Process[processCount + 1];

			HashMap<Integer, Integer> neighbors;
			MasterProcess master = new MasterProcess(processCount);
			int temp = 0;
			for (int i = 1; i <= processCount; i++) {
				neighbors = new HashMap<>();
				for (int j = 1; j <= processCount; j++) {
					temp = sc.nextInt();
					if (temp != -1) {
						neighbors.put(j, temp);
					}
				}
				p[i] = new Process(i, neighbors, source, master);
			}
			// Sending the reference of all Processes to Master and all Slave
			// processes
			master.setProcesses(p);
			// Starting Master Thread
			Thread t = new Thread(master);
			t.start();
			// Starting all Processes threads
			for (int i = 1; i <= processCount; i++) {
				Thread tp = new Thread(p[i]);
				tp.start();
			}
			sc.close();
		}
	}
}
