import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * Client-server graphical editor
 * 
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012; loosely based on CS 5 code by Tom Cormen
 * @author CBK, winter 2014, overall structure substantially revised
 * @author Travis Peters, Dartmouth CS 10, Winter 2015; remove EditorCommunicatorStandalone (use echo server for testing)
 * @author CBK, spring 2016 and Fall 2016, restructured Shape and some of the GUI
 * @author Jared Cole, finished problem
 */
public class Editor extends JFrame {	
	private static String serverIP = "localhost";			// IP address of sketch server
	// "localhost" for your own machine;
	// or ask a friend for their IP address

	private static final int width = 800, height = 800;		// canvas size

	// Current settings on GUI
	public enum Mode {
		DRAW, MOVE, RECOLOR, DELETE, BOTTOM, TOP
	}
	private Mode mode = Mode.DRAW;				// drawing/moving/recoloring/deleting/changing order of objects
	private String shapeType = "ellipse";		// type of object to add
	private Color color = Color.black;			// current drawing color

	// Drawing state
	// these are remnants of my implementation; take them as possible suggestions or ignore them
	private Shape curr = null;					// current shape (if any) being drawn
	private Sketch sketch;						// holds and handles all the completed objects
	private int movingId = -1;					// current shape id (if any; else -1) being moved
	private Point drawFrom = null;				// where the drawing started
	private Point moveFrom = null;				// where object is as it's being dragged
	

	// Communication
	private EditorCommunicator comm;			// communication with the sketch server

	public Editor() {
		super("Graphical Editor");

		sketch = new Sketch();

		// Connect to server
		comm = new EditorCommunicator(serverIP, this);
		comm.start();

		// Helpers to create the canvas and GUI (buttons, etc.)
		JComponent canvas = setupCanvas();
		JComponent gui = setupGUI();

		// Put the buttons and canvas together into the window
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(canvas, BorderLayout.CENTER);
		cp.add(gui, BorderLayout.NORTH);

		// Usual initialization
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}

	/**
	 * Creates a component to draw into
	 */
	private JComponent setupCanvas() {
		JComponent canvas = new JComponent() {
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawSketch(g);
			}
		};
		
		canvas.setPreferredSize(new Dimension(width, height));

		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent event) {
				handlePress(event.getPoint());
			}

			public void mouseReleased(MouseEvent event) {
				handleRelease();
			}
		});		

		canvas.addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent event) {
				handleDrag(event.getPoint());
			}
		});
		
		return canvas;
	}

	/**
	 * Creates a panel with all the buttons
	 */
	private JComponent setupGUI() {
		// Select type of shape
		String[] shapes = {"ellipse", "freehand", "rectangle", "segment"};
		JComboBox<String> shapeB = new JComboBox<String>(shapes);
		shapeB.addActionListener(e -> shapeType = (String)((JComboBox<String>)e.getSource()).getSelectedItem());

		// Select drawing/recoloring color
		// Following Oracle example
		JButton chooseColorB = new JButton("choose color");
		JColorChooser colorChooser = new JColorChooser();
		JLabel colorL = new JLabel();
		colorL.setBackground(Color.black);
		colorL.setOpaque(true);
		colorL.setBorder(BorderFactory.createLineBorder(Color.black));
		colorL.setPreferredSize(new Dimension(25, 25));
		JDialog colorDialog = JColorChooser.createDialog(chooseColorB,
				"Pick a Color",
				true,  //modal
				colorChooser,
				e -> { color = colorChooser.getColor(); colorL.setBackground(color); },  // OK button
				null); // no CANCEL button handler
		chooseColorB.addActionListener(e -> colorDialog.setVisible(true));

		// Mode: draw, move, recolor, delete, bottom, or top
		JRadioButton drawB = new JRadioButton("draw");
		drawB.addActionListener(e -> mode = Mode.DRAW);
		drawB.setSelected(true);
		JRadioButton moveB = new JRadioButton("move");
		moveB.addActionListener(e -> mode = Mode.MOVE);
		JRadioButton recolorB = new JRadioButton("recolor");
		recolorB.addActionListener(e -> mode = Mode.RECOLOR);
		JRadioButton deleteB = new JRadioButton("delete");
		deleteB.addActionListener(e -> mode = Mode.DELETE);
		JRadioButton bottomB = new JRadioButton("bottom");
		bottomB.addActionListener(e -> mode = Mode.BOTTOM);
		JRadioButton topB = new JRadioButton("top");
		topB.addActionListener(e -> mode = Mode.TOP);
		ButtonGroup modes = new ButtonGroup(); // make them act as radios -- only one selected
		modes.add(drawB);
		modes.add(moveB);
		modes.add(recolorB);
		modes.add(deleteB);
		modes.add(bottomB);
		modes.add(topB);
		JPanel modesP = new JPanel(new GridLayout(1, 0)); // group them on the GUI
		modesP.add(drawB);
		modesP.add(moveB);
		modesP.add(recolorB);
		modesP.add(deleteB);
		modesP.add(bottomB);
		modesP.add(topB);
		JButton undo = new JButton("undo");
		JButton redo = new JButton("redo");
		undo.addActionListener(e -> comm.requestUndo());
		redo.addActionListener(e -> comm.requestRedo());
		
		// Put all the stuff into a panel
		JComponent gui = new JPanel();
		gui.setLayout(new FlowLayout());
		gui.add(shapeB);
		gui.add(chooseColorB);
		gui.add(colorL);
		gui.add(modesP);
		gui.add(undo);
		gui.add(redo);
		return gui;
	}

	/**
	 * Getter for the sketch instance variable
	 */
	public Sketch getSketch() {
		return sketch;
	}

	/**
	 * Draws all the shapes in the sketch,
	 * along with the object currently being drawn in this editor (not yet part of the sketch)
	 */
	public synchronized void drawSketch(Graphics g) {
		for(int id : sketch.getShapes().navigableKeySet()){
			sketch.getShapes().get(id).draw(g);
		}
		if(curr != null) curr.draw(g);
		// repaint(); no need for paintComponent to (indirectly) call repaint(), instead just repaint()
		// when significant actions occur (i.e. shapes are changed)
	}

	// Helpers for event handlers
	
	/**
	 * Helper method for press at point
	 * In drawing mode, start a new object;
	 * in moving mode, (request to) start dragging if clicked in a shape;
	 * in recoloring mode, (request to) change clicked shape's color
	 * in deleting mode, (request to) delete clicked shape
	 */
	private void handlePress(Point p) {
		// In drawing mode, start drawing a new shape
				// In moving mode, start dragging if clicked in the shape
				// In recoloring mode, change the shape's color if clicked in it
				// In deleting mode, delete the shape if clicked in it
				// Be sure to refresh the canvas (repaint) if the appearance has changed
		/*
		 * There is no need to repaint on the initial click in DRAW mode since none of the shapes can be
		 * properly defined with a single point (i.e. they're degenerate); instead repaint() once they
		 * are fully defined (the mouse has been dragged at least once)
		 * There is also no need to repaint on recolor or delete because those make server requests, and
		 * server requests will invoke repaint() downstream (see appropriate method below)
		 */
		// If draw mode is active
		if (mode == Mode.DRAW) {
			comm.requestSaveState(); // Request save state
			// Ellipse, rectangle, and segment are defined by two points and are thus largely analogous
			if (shapeType.equals("ellipse")) {
				curr = new Ellipse(p.x,p.y, color); // Make a degenerate ellipse at this point
				drawFrom = p;						// and start drawing with this corner "anchored"
			} else if (shapeType.equals("rectangle")) {
				curr = new Rectangle(p.x,p.y, color); // Make a degenerate rectangle at this point
				drawFrom = p;						  // and start drawing with this corner "anchored"
			} else if (shapeType.equals("segment")) {
				curr = new Segment(p.x,p.y, color); // Make a degenerate line segment at this point
				drawFrom = p;						// and anchor this endpoint
		
			// Polyline needs to behave somewhat differently
			} else if (shapeType.equals("freehand")) {
				curr = new Polyline(new ArrayList<Point>(), color); // Start a new polyline
				((Polyline) curr).addPoint(p); // and add the current point to it (it's degenerate at this stage)
			
			// This should never run, but just in case the client was somehow modified and the shape
			// type is something not recognized, notify the user in console
			} else {
				System.err.println("Undefined shape type.");
			}
		
		// If move mode is active
		} else if (mode == Mode.MOVE) {
			// Find the ID of the uppermost shape that was clicked on, if it exists
			movingId = sketch.getUppermostCollision(p.x, p.y);
			if (movingId != -1) comm.requestSaveState(); // If user actually clicked something, request save state
			moveFrom = p; // and set moveFrom to the initial point
		}
		
		// If recolor mode is active
		else if (mode == Mode.RECOLOR) {
			// If the user actually clicked something, request a save state
			if (sketch.getUppermostCollision(p.x, p.y) != -1) comm.requestSaveState();
			// Request a recolor for the current color and the uppermost shape that was clicked on
			comm.requestRecolor(sketch.getUppermostCollision(p.x, p.y), color.getRGB());
		}
		
		// If delete mode is active
		else if (mode == Mode.DELETE) {
			// If the user actually clicked something, request a save state
			if (sketch.getUppermostCollision(p.x, p.y) != -1) comm.requestSaveState();
			// Request a removal of the uppermost shape that was clicked on
			comm.requestRemove(sketch.getUppermostCollision(p.x, p.y));
		}
		
		// If send to bottom mode is active
		else if (mode == Mode.BOTTOM) {
			// If the user actually clicked something, request a save state
			if (sketch.getUppermostCollision(p.x, p.y) != -1) comm.requestSaveState();
			// Request a send-to-bottom of the uppermost shape that was clicked on
			comm.requestSendToBottom(sketch.getUppermostCollision(p.x, p.y));
		}
		
		// If send to top mode is active
		else if (mode == Mode.TOP) {
			// If the user actually clicked something, request a save state
			if (sketch.getUppermostCollision(p.x, p.y) != -1) comm.requestSaveState();
			// Request a send-to-top of the uppermost shape that was clicked on
			comm.requestSendToTop(sketch.getUppermostCollision(p.x, p.y));
		}
		
		// This should never run, but just in case the client was somehow modified and the editor mode
		// becomes something unrecognized, notify the user in console
		else {
			System.err.println("Undefined editor mode.");
		}
	}

	/**
	 * Helper method for drag to new point
	 * In drawing mode, update the other corner of the object;
	 * in moving mode, (request to) drag the object/
	 */
	private void handleDrag(Point p) {
		// If draw mode is active
		if (mode == Mode.DRAW) {
			
			// Ellipse, rectangle, and segment again behave relatively similarly
			if (shapeType.equals("ellipse")) {
				// Cast to ellipse as not all shapes have a setCorners; then set the current corner (p)
				((Ellipse) curr).setCorners(drawFrom.x, drawFrom.y, p.x, p.y); // drawFrom is initial corner
			} else if (shapeType.equals("rectangle")) { // Same as ellipse
				((Rectangle) curr).setCorners(drawFrom.x, drawFrom.y, p.x, p.y);
			} else if (shapeType.equals("segment")) {
				// Same as ellipse, but set endpoint instead of setting both corners (initial endpoint
				// was the first point declared in the constructor inside handlePress())
				((Segment) curr).setEnd(p.x, p.y);
				
			// While polyline behaves somewhat differently
			} else if (shapeType.equals("freehand")) {
				// Cast to polyline because superclass Shape doesn't have addPoint() method
				((Polyline) curr).addPoint(p); // Then just add this point as the next point in the polyline
				
			// Just in case the shape type is for whatever reason something else
			} else {
				System.err.println("Undefined shape type."); // Notify the user
			}
			repaint(); // Repaint as the state of the canvas has changed
		}
		
		// IF move mode is active
		else if (mode == Mode.MOVE) {
			if (movingId != -1) { // If the movingId is defined (i.e. not -1)
				// Request the shape to move the dragged displacement from the previous point
				comm.requestMove(movingId, p.x-moveFrom.x, p.y-moveFrom.y);
				moveFrom = p; // Now set the current point as the new previous point
				repaint(); // and repaint as the state of the canvas has changed
			}
		}
		
		// If either delete or recolor mode is active, just do nothing (and no need to repaint)
		// If any other mode that's for whatever reason not properly defined (see above) is active
		else if (mode != Mode.DELETE && mode != Mode.RECOLOR && mode != Mode.BOTTOM && mode != Mode.TOP) {
			System.err.println("Undefined editor mode.");
		}
	}

	/**
	 * Helper method for release
	 * In drawing mode, pass the add new object request on to the server;
	 * in moving mode, release it		
	 */
	private void handleRelease() {
		// If draw mode is active
		if (mode == Mode.DRAW){
			comm.requestAdd(curr); // Request the shape to be formally broadcast by the server
			curr = null; // and delete the temporary shape
			repaint(); // Repaint to immediately stop rendering the temporary shape (just to be safe)
		
		// If move mode is active
		} else if (mode == Mode.MOVE) {
			movingId = -1; // Set the movingId to -1 to denote no objects currently being moved
		}
		
		// If either delete or recolor mode is active, just do nothing (and no need to repaint)
		// If any other mode that's for whatever reason not properly defined (see above) is active
		else if (mode != Mode.DELETE && mode != Mode.RECOLOR && mode != Mode.BOTTOM && mode != Mode.TOP) {
			System.err.println("Undefined editor mode.");
		}
	}
	
	/**
	 * Parses input broadcasted from the server and uses it to appropriately update this
	 * editor's internal sketch based on the server requests.
	 * Essentially identical to the server's own parseLine() to update the master sketch.
	 * 
	 * @param input	the input string as broadcasted by the server
	 */
	public synchronized void parseLine(String input) {
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
					sketch.addShape(new Rectangle(Integer.parseInt(words[2]),Integer.parseInt(words[3]),
							Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6]))));
					break; // Break to prevent falling through (only breaks out of shape type switch case)
				case "ellipse": // If the shape is an ellipse
					// Construct an Ellipse based on the remaining tokens, as above
					sketch.addShape(new Ellipse(Integer.parseInt(words[2]),Integer.parseInt(words[3]),
							Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6]))));
					break; // Prevent falling through
				case "segment": // If the shape is a segment
					// Construct a Segment based on the remaining tokens, as above
					sketch.addShape(new Segment(Integer.parseInt(words[2]),Integer.parseInt(words[3]),
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
					sketch.addShape(new Polyline(locs, new Color(Integer.parseInt(words[3]))));
					break; // Prevent falling through
			}
			// Notify the user that someone has added a new shape
			System.out.println("New " + words[1] + " added at ID " + (sketch.getCurID() - 1));
			break; // Break out of first switch-case (will repaint below)
			
		case "move": // If the first token was move
			// Just move the shape in the sketch based on the information given by the broadcast
			sketch.moveShape(Integer.parseInt(words[1]), Integer.parseInt(words[2]), Integer.parseInt(words[3]));
			break; // Break to prevent falling through (breaks out of first switch case)
			
		case "recolor": // If the first token was recolor
			// Just recolor the shape in the sketch based on the information given in the broadcast
			sketch.recolorShape(Integer.parseInt(words[1]), new Color(Integer.parseInt(words[2])));
			break; // Break to precent falling through
			
		case "remove": // If the first token was remove
			// Remove the appropriate shape from the sketch based on the broadcast
			sketch.removeShape(Integer.parseInt(words[1]));
			break; // Break to prevent falling through
			
		// Sometimes we must set the curID manually; this occurs if the curID in the master sketch is not
		// the same as the ID of the most recent object (i.e. a more recent object was deleted)
		case "curId": // If the first token was curId
			sketch.setCurID(Integer.parseInt(words[1])); // Set the CurID as appropriate
			break; // Break to prevent falling through
		
		case "bottom": // If the first token was bottom
			sketch.sendToBottom(Integer.parseInt(words[1]));
			break;
			
		case "top": // If the first token was top
			sketch.sendToTop(Integer.parseInt(words[1]));
			break;
			
		case "print": // If the first token was print
			System.out.println(input.split(" ", 2)[1]); // Print everything after "print "
			break;
		
		case "clear": // If the first token was clear
			sketch = new Sketch(); // Clear the sketch
			break;
			
		// If the first token was none of the above, the input string should represent an ID-based add
		// (i.e. adding a shape with a specific ID and not the default automatic ID-finding behavior)
		default: // Most of the code here will be similar to "add" case because it's mostly the same as an add
			String IDShape = words[1]; // Get the token representing the shape type
			switch (IDShape) { // Second switch-case conditional based on the shape type
			
				// Once again, the rectangle, ellipse, and segment behave similarly
				case "rect": // If it's a rectangle
					// Use Sketch.updateShape() to add the new rectangle at a specific ID; when the server
					// broadcasts an ID-based add, the first token is the ID to place it at
					sketch.updateShape(Integer.parseInt(words[0]), new Rectangle(Integer.parseInt(words[2]),
							Integer.parseInt(words[3]), Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6])))); // Otherwise similar to add above
					break; // Break to prevent falling through
				case "ellipse": // If it's a rectangle
					// Update the shape in the map appropriately
					sketch.updateShape(Integer.parseInt(words[0]), new Ellipse(Integer.parseInt(words[2]),
							Integer.parseInt(words[3]), Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6]))));
					break; // Break to prevent falling through
				case "segment":
					sketch.updateShape(Integer.parseInt(words[0]), new Segment(Integer.parseInt(words[2]),
							Integer.parseInt(words[3]), Integer.parseInt(words[4]),Integer.parseInt(words[5]),
							new Color(Integer.parseInt(words[6]))));
					break;
					
				// And the polyline behaves somewhat differently
				case "polyline":
					// First populate the list of points just like with "add"
					List<Point> locs = new ArrayList<Point>();
					String[] points = words[2].substring(1, words[2].length()-1).split(";");
					for(String s : points){
						String[] point = s.split(",");
						locs.add(new Point(Integer.parseInt(point[0]),Integer.parseInt(point[1])));
					}
					
					// Now ID-add based on the given ID and the newly found list of points
					sketch.updateShape(Integer.parseInt(words[0]), new Polyline(locs,
							new Color(Integer.parseInt(words[3]))));
					break; // Break to prevent falling through
			}
			// Let the user know that a new shape has been ID-added and the ID it was added at
			// Provides some level of useful data without flooding the console
			System.out.println("New " + words[1] + " added at server-given ID " + words[0]);
			// No need to break; since this is the last case
		}
		repaint(); // Repaint because almost all inputs broadcast from the server will change the state of the canvas
		// System.out.println(sketch.getShapes()); Useful for debugging, but floods the console
	}
	
	/**
	 * Handle hanging up safely by removing the JFrame and notifying the user in console.
	 */
	public void hangUp(){
		setVisible(false); // Make the JFrame invisible
		dispose(); // and then destroy it
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Editor();
			}
		});	
	}
}
