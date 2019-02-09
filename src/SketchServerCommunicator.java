import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Handles communication between the server and one client, for SketchServer
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012; revised Winter 2014 to separate SketchServerCommunicator
 * @author Jared Cole, finished problem
 */
public class SketchServerCommunicator extends Thread {
	private Socket sock;					// to talk with client
	private BufferedReader in;				// from client
	private PrintWriter out;				// to client
	private SketchServer server;			// handling communication for

	public SketchServerCommunicator(Socket sock, SketchServer server) {
		this.sock = sock;
		this.server = server;
	}

	/**
	 * Sends a message to the client. Synchronized so this cannot run at the same time as update() below.
	 * @param msg
	 */
	public synchronized void send(String msg) {
		out.println(msg);
	}
	
	/**
	 * Updates a newly connected client with all of the information currently contained in the master sketch.
	 * Synchronized so regular broadcasts from the server cannot occur concurrently with the initial update.
	 */
	public synchronized void update() {
		out.println("clear");
		Map<Integer, Shape> shapes = server.getSketch().getShapes(); // Get the shapes map
		// Iterate over all IDs in the shapes map
		for(Integer id : shapes.keySet()){
			// Instruct the client to perform an ID-based add of the current id and its corresponding shape
			out.println(id + " " + shapes.get(id));
		}
		// Instruct the client to update its own curID to the master sketch's curID
		out.println("curId " + server.getSketch().getCurID());
	}
	
	/**
	 * Keeps listening for and handling (your code) messages from the client
	 */
	public void run() {
		try {
			System.out.println("someone connected");

			// Communication channel
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintWriter(sock.getOutputStream(), true);
			
			// Deal with setting/checking the password
			if (server.numConnections() == 1 && !server.hasPassword()) {
				out.println("print What would you like the password to be?");
				server.setPassword(in.readLine()); // Set the password to what the first editor says it should be
			} else {
				out.println("print Please enter the password to connect to this server.");
				if (!server.checkPassword(in.readLine())) { // If the password is wrong
					out.println("print Password invalid.");
					System.out.println("Client attempted connection, but failed password check.");
					// Clean up and close connection
					server.removeCommunicator(this);
					out.close();
					in.close();
					sock.close();
				}
			}
			
			// Tell the client the current state of the world
			//out.println("sketch_start"); deprecated start tokens for the client
			update(); // Update the newly connected client with the master sketch's information
			//out.println("eof"); deprecated
			
			// Keep getting and handling messages from the client
			String line; // Variable to hold the current line
			while ((line = in.readLine()) != null) { // Block until messages received by client
				// System.out.println("received \"" + line + "\"."); Useful for debugging, but floods console
				if (server.parseLine(line)) { // Call parseLine() to appropriately update the master sketch
					server.broadcast(line); // and broadcast the line received to all clients if the command is valid
				}
			}
			
			// Clean up -- note that also remove self from server's list so it doesn't broadcast here
			server.removeCommunicator(this);
			out.close();
			in.close();
			sock.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
