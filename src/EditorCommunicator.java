import java.awt.Color;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Handles communication to/from the server for the editor
 * 
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012
 * @author Chris Bailey-Kellogg; overall structure substantially revised Winter 2014
 * @author Travis Peters, Dartmouth CS 10, Winter 2015; remove EditorCommunicatorStandalone (use echo server for testing)
 * @author Jared Cole, finished problem
 */
public class EditorCommunicator extends Thread {
	private PrintWriter out;		// to server
	private BufferedReader in;		// from server
	protected Editor editor;		// handling communication for

	/**
	 * Establishes connection and in/out pair
	 */
	public EditorCommunicator(String serverIP, Editor editor) {
		this.editor = editor;
		System.out.println("connecting to " + serverIP + "...");
		try {
			Socket sock = new Socket(serverIP, 4242);
			out = new PrintWriter(sock.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			System.out.println("...connected");
		}
		catch (IOException e) {
			System.err.println("couldn't connect");
			System.exit(-1);
		}
	}

	/**
	 * Sends message to the server
	 */
	public synchronized void send(String msg) {
		out.println(msg);
	}

	/**
	 * Keeps listening for and handling (your code) messages from the server
	 */
	public void run() {
		try {
			// Handle the initial password check
			editor.parseLine(in.readLine());
			Scanner sc = new Scanner(System.in);
			send(sc.nextLine());
			sc.close();	// Only need one line

			String line;
			// Handle messages
			while ((line = in.readLine()) != null) {
				// System.out.println(line); Floods console with lines
				editor.parseLine(line);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			editor.hangUp();
			System.out.println("server hung up");
			System.exit(0);
		}
	}	

	// Send editor requests to the server
	
	/**
	 * Requests the server to add a new shape to the master sketch and broadcast it to all other clients.
	 * 
	 * @param shape	the Shape object to be added server-side
	 */
	public void requestAdd(Shape shape) {
		// Print "add" as a command and the shape's toString() representation with the PrintWriter
		send("add " + shape); // All shapes should be fully reconstructible with their toString()
	}
	
	/**
	 * Requests the server remove the shape with the given id and broadcast the removal to all other clients.
	 * 
	 * @param id	the id of the shape to be removed
	 */
	public void requestRemove(int id) {
		send("remove " + id); // Print "remove" as a command and give the id to remove
	}
	
	/**
	 * Requests the server recolor the shape with the given id and the given color, and broadcast
	 * the recolor request to all other clients.
	 * 
	 * @param id	the id of the shape to be recolored
	 * @param color	the new color to change it to
	 */
	public void requestRecolor(int id, int color) {
		send("recolor " + id + " " + color); // Print "recolor" as a command and give the id and color
	}
	
	/**
	 * Requests the server move the shape with the given id by the given amount, and broadcast the move
	 * request to all other clients.
	 * 
	 * @param id	the id of the shape to be moved
	 * @param dx	the x-axis displacement of the move
	 * @param dy	the y-axis displacement of the move
	 */
	public void requestMove(int id, int dx, int dy) {
		send("move " + id + " " + dx + " " + dy); // Print "move" as a command, give the id and the displacement
	}
	
	/**
	 * Requests the server move the shape with the given id to the bottommost layer.
	 * 
	 * @param id	the id of the shape to be sent to bottom
	 */
	public void requestSendToBottom(int id) {
		send("bottom " + id);
	}
	
	/**
	 * Requests the server move the shape with teh given id to the uppermost layer.
	 * 
	 * @param id	the id of the shape to be brought to top
	 */
	public void requestSendToTop(int id) {
		send("top " + id);
	}
	
	/**
	 * Requests the server to save the current state.
	 */
	public void requestSaveState() {
		send("save_state");
	}
	
	/**
	 * Requests the server to undo the last action taken.
	 */
	public void requestUndo() {
		send("undo");
	}
	
	/**
	 * Requests the server to redo the last action taken.
	 */
	public void requestRedo() {
		send("redo");
	}
}
