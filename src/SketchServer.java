import java.net.*;
import java.util.*;
import java.awt.Color;
import java.awt.Point;
import java.io.*;

/**
 * A server to handle sketches: getting requests from the clients,
 * updating the overall state, and passing them on to the clients
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012; revised Winter 2014 to separate SketchServerCommunicator
 * @author Jared Cole, finished problem
 */
public class SketchServer {
	private ServerSocket listen;						// for accepting connections
	private ArrayList<SketchServerCommunicator> comms;	// all the connections with clients
	private Sketch[] sketchStates;						// all of the saved states of the world
	private int currState;
	private static final int MAX_SAVED_STATES = 15;
	private String password;
	
	public SketchServer(ServerSocket listen) {
		this.listen = listen;
		currState = 0;
		sketchStates = new Sketch[MAX_SAVED_STATES];
		sketchStates[0] = new Sketch();
		comms = new ArrayList<SketchServerCommunicator>();
	}

	public Sketch getSketch() {
		return sketchStates[currState];
	}
	
	/**
	 * The usual loop of accepting connections and firing off new threads to handle them
	 */
	public void getConnections() throws IOException {
		System.out.println("server ready for connections");
		while (true) {
			SketchServerCommunicator comm = new SketchServerCommunicator(listen.accept(), this);
			comm.setDaemon(true);
			comm.start();
			addCommunicator(comm);
		}
	}

	/**
	 * Returns the number of connections this server has.
	 */
	public synchronized int numConnections() {
		return comms.size();
	}
	
	/**
	 * Sets the server's password to the given input.
	 */
	public synchronized void setPassword(String password) {
		this.password = password;
		System.out.println("password set to " + password);
	}
	
	public synchronized boolean hasPassword() {
		if (password == null) return false;
		return true;
	}
	
	/**
	 * Checks if the given input matches the server's password.
	 */
	public synchronized boolean checkPassword(String input) {
		return input.equals(password);
	}
	
	/**
	 * Adds the communicator to the list of current communicators
	 */
	public synchronized void addCommunicator(SketchServerCommunicator comm) {
		comms.add(comm);
	}

	/**
	 * Removes the communicator from the list of current communicators
	 */
	public synchronized void removeCommunicator(SketchServerCommunicator comm) {
		comms.remove(comm);
	}

	/**
	 * Sends the message from the one communicator to all (including the originator)
	 */
	public synchronized void broadcast(String msg) {
		for (SketchServerCommunicator comm : comms) {
			comm.send(msg);
		}
	}
	
	/**
	 * Parses input received from a client and uses it to appropriately update the server's master sketch.
	 * Essentially identical to the every client's parseLine() to update their local sketches.
	 * 
	 * @param input	the input string as received by the server
	 * @return	boolean indicating whether the received command was valid
	 */
	public boolean parseLine(String input) {
		String[] words = input.split(" "); // Split the input string into tokens based on the space character
		switch (words[0]) { // switch-case conditional based on the first token in the input string
		case "add": // If it's add
			String newShape = words[1]; // Find the shape type in the second token
			switch (newShape) { // and use a second switch-case conditional based on the shape type
			
				// Once again, the rectangle, ellipse, and segment behave generally similarly
				// Because their toString() representations contain a fixed number of tokens (constant
				// amount of information required to properly reconstruct the shape)
				case "rect": // If the shape is a rectangle
					// Construct a Rectangle based on the remaining tokens (which in the toString() representation
					// of a rectangle contain its essential information, in the order required)
					sketchStates[currState].addShape(new Rectangle(Integer.parseInt(words[2]),Integer.parseInt(words[3]),
							Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6]))));
					break; // Break to prevent falling through (only breaks out of shape type switch case)
				case "ellipse": // If the shape is an ellipse
					// Construct an Ellipse based on the remaining tokens, as above
					sketchStates[currState].addShape(new Ellipse(Integer.parseInt(words[2]),Integer.parseInt(words[3]),
							Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6]))));
					break; // Prevent falling through
				case "segment": // If the shape is a segment
					// Construct a Segment based on the remaining tokens, as above
					sketchStates[currState].addShape(new Segment(Integer.parseInt(words[2]),Integer.parseInt(words[3]),
							Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6]))));
					break; // Prevent falling through
				
				// While Polylines behave differently because they require a variable-length amount of information
				case "polyline":
					List<Point> locs = new ArrayList<Point>(); // Start a temporary list of point locations
					// Now remove the [] in the second token from the toString() representation of the Polyline
					// and split the rest of the token into points in the form x,y
					String[] points = words[2].substring(1, words[2].length()-1).split(";");
					
					// For each String-represented point in points
					for (String s : points) {
						String[] point = s.split(","); // Split the point String across the comman
						// and construct a Point based on the information inside the String, add it to locs
						locs.add(new Point(Integer.parseInt(point[0]),Integer.parseInt(point[1])));
					}
					
					// Once the point list is done being populated, construct the Polyline and add it to sketch
					sketchStates[currState].addShape(new Polyline(locs, new Color(Integer.parseInt(words[3]))));
					break; // Prevent falling through
			}
			// Notify the user that someone has added a new shape
			System.out.println("New " + words[1] + " added at ID " + (sketchStates[currState].getCurID() - 1));
			return true; // Command was valid
			
		case "move": // If the first token was move
			// Just move the shape in the sketch based on the information given by the broadcast
			sketchStates[currState].moveShape(Integer.parseInt(words[1]), Integer.parseInt(words[2]), Integer.parseInt(words[3]));
			return true; // Command was valid
			
		case "recolor": // If the first token was recolor
			// Just recolor the shape in the sketch based on the information given in the broadcast
			sketchStates[currState].recolorShape(Integer.parseInt(words[1]), new Color(Integer.parseInt(words[2])));
			return true; // Command was valid
			
		case "remove": // If the first token was remove
			// Remove the appropriate shape from the sketch based on the broadcast
			sketchStates[currState].removeShape(Integer.parseInt(words[1]));
			return true; // Command was valid
			
		case "bottom": // If the first token was bottom
			sketchStates[currState].sendToBottom(Integer.parseInt(words[1]));
			return true;
				
		case "top": // If the first token was top
			sketchStates[currState].sendToTop(Integer.parseInt(words[1]));
			return true;
			
		case "save_state": // If the first token was save_state
			if (currState == MAX_SAVED_STATES - 1) { // If this is the last saved state
				for (int i = 1; i < MAX_SAVED_STATES; i++) {
					sketchStates[i-1] = sketchStates[i]; // Shift all states down 1
				} // Note that the current state is still the last state, and it hasn't changed
			} else { // Otherwise
				sketchStates[currState + 1] = sketchStates[currState].clone(); // Make a copy of the current state
				currState++; // and set the current state to that copy
				// If there are non-null future states
				if (currState == MAX_SAVED_STATES -1 || sketchStates[currState + 1] != null) { 
					for (int i = currState + 1; i < MAX_SAVED_STATES; i++) {
						sketchStates[i] = null; // Delete them (new branch)
					}
				}
			}
			return false; // Don't rebroadcast this command to the clients
			
		case "undo": // If the first token was undo
			if (currState == 0) { // If this is the 0th state
				System.out.println("received \"undo\", but nothing to undo");
			} else { // Otherwise
				currState--; // and set the current state to the previous one
				for (SketchServerCommunicator comm : comms) { // Now, for all communicators
					comm.update(); // Tell their clients to reset their shapes appropriately
				}
			}
			return false; // Don't rebroadcast this command to the clients
			
		case "redo": // If the first token was redo
			// Check if there's no valid state to redo to (there's not a next state)
			if (currState == MAX_SAVED_STATES - 1 || sketchStates[currState + 1] == null) { 
				System.out.println("received \"redo\", but nothing to redo");
			} else { // Otherwise
				currState++; // Move the current state to the next state
				for (SketchServerCommunicator comm : comms) {
					comm.update(); // Update all editors
				}
			}
			return false; // Don't rebroadcast this command to the clients
			
		// The server has no need for curID or ID-based add commands; those are necessary for new clients to
		// match their internal sketches with the server's sketch, but the server's Master sketch is the
		// ultimate determiner of Shape IDs and it never receives any ID-manipulating inputs as such.
		default:
			System.out.println("Invalid command " + words[0] + " received."); // Notify user that something's wrong
			return false; // Invalid command received from client
			// No need to break since this is the last case and no fall through occurs
		}
	}
	
	public static void main(String[] args) throws Exception {
		new SketchServer(new ServerSocket(4242)).getConnections();
	}
}
