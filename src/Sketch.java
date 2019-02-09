import java.awt.Color;
import java.util.TreeMap;

/**
 * Holds the current list of shapes in a convenient data structure, along with some useful commands.
 * Used both server-side (for the Master sketch) and client-side (for the local sketch).
 * 
 * @author Jared Cole
 */
public class Sketch {
	private TreeMap<Integer, Shape> shapes; // Maps global ids to shapes; TreeMap for ordering functionality
	private int curID; // The ID of the next shape to be added; can be thought of as the "maxiumum ID" + 1
	private int leastID;
	
	/**
	 * Constructs a new Sketch object with default values.
	 */
	public Sketch() {
		shapes = new TreeMap<Integer, Shape>(); // Empty id to shape map
		curID = 0; // Current id set to 0
		leastID = -2;
	}
	
	/**
	 * Constructs a Sketch object based on a given map and current ID.
	 */
	public Sketch(TreeMap<Integer, Shape> shapes, int curID, int leastID) {
		this.shapes = shapes; // Set shapes to given shapes
		this.curID = curID; // Set curID to given curID
		this.leastID = leastID;
	}
	
	 // Almost all methods that can access the internal TreeMap or curID must be synchronized so that
	 // they properly reflect the state of the Sketch at any given time without possibility of overwriting.
		
	/**
	 * Getter for the internal ids to shapes TreeMap. Returns as a TreeMap instead of the superclass Map
	 * so that useful functions like TreeMap.navigableKeySet() or TreeMap.descendingKeySet() can be called.
	 *
	 * @return	the internal ids to shapes TreeMap
	 */
	public synchronized TreeMap<Integer, Shape> getShapes() {
		return shapes;
	}
	
	/**
	 * Adds the given shape to the internal TreeMap at the default ID.
	 * 
	 * @param shape	the shape to be added to the TreeMap
	 * @return	true of the shape was successfully added, or false otherwise
	 */
	public synchronized boolean addShape(Shape shape) {
		if (shape == null) return false; // If the shape was null, terminate and return false
		shapes.put(curID++, shape); // Otherwise add the shape at curID, then increment it
		return true; // Return true to signify success
	}
	
	/**
	 * Removes the shape with the given id from the internal TreeMap.
	 * 
	 * @param id	the ID of the shape to be removed
	 * @return	true if the shape was properly removed, or false otherwise
	 */
	public synchronized boolean removeShape(int id) {
		if (id == -1 || id < leastID || id > curID) return false; // If the given ID is -1 or too big, return false
		if (!shapes.containsKey(id)) return false; // If shapes doesn't have the given ID, return false
		shapes.remove(id); // Otherwise remove it
		return true; // Return true to signify success
	}

	/**
	 * Moves the shape with the given ID by the given displacement.
	 * 
	 * @param id	the ID of the shape to be moved
	 * @param dx	the x-direction displacement of the move
	 * @param dy	the y-direction displacement of the move
	 * @return	true if the shape was successfully added, or false otherwise
	 */
	public synchronized boolean moveShape(int id, int dx, int dy) {
		if (id == -1 || id < leastID || id > curID) return false; // If the given ID is invalid, return false
		if (!shapes.containsKey(id)) return false; // If shapes doesn't have the given ID, return false
		shapes.get(id).moveBy(dx, dy); // Otherwise get the shape and call its own moveBy by the given amount
		return true; // Return true to signify success
	}
	
	/**
	 * Recolors the shape with the given id with the color represented by the given Color object.
	 * 
	 * @param id	the ID of the shape to be recolored
	 * @param color	the Color to set the shape to
	 * @return	true if the shape was successfully recolored, or false otherwise
	 */
	public synchronized boolean recolorShape(int id, Color color) {
		if (id == -1 || id < leastID || id > curID || color == null) return false; // If invalid parameters, return false
		if (!shapes.containsKey(id)) return false; // If shapes doesn't have the given ID, return false
		shapes.get(id).setColor(color); // Otherwise set the color as specified
		return true; // Return true to signify success
	}
	
	/**
	 * Recolors the shape with the given id with the color represented by the given RGB integer.
	 * 
	 * @param id	the ID of the shape to be recolored
	 * @param color	int RGB of the color to set the shape to
	 * @return	true if the shape was successfully recolored, or false otherwise
	 */
	public synchronized boolean recolorShape(int id, int color) {
		if (id == -1 || id < leastID || id > curID) return false; // If the given ID is invalid, return false
		if (!shapes.containsKey(id)) return false; // If shapes doesn't have the given ID, return false
		shapes.get(id).setColor(new Color(color)); // Otherwise set the color as specified
		return true; // Return true to signify success
	}
	
	/**
	 * Updates the given entry in the TreeMap of id to shape, or creates the entry if it doesn't exist.
	 * Can be thought of as an addShape that adds at a specific ID instead of one determined by the Sketch.
	 * 
	 * @param id	the ID to map the given shape to
	 * @param shape	the shape to be added to the map
	 * @return	true if the entry was successfully updated, or false otherwise
	 */
	public synchronized boolean updateShape(int id, Shape shape) {
		if (id == -1 || shape == null) return false; // If the ID or shape are invalid, return false
		shapes.put(id, shape); // Put a new entry into the map for the given id and shape
		if (id >= curID) curID = id + 1; // If given ID is larger than curID (max ID), update curID to match
		return true; // Return true to signify success
	}
	
	public synchronized boolean sendToBottom(int id) {
		if (id > curID || id < leastID || id == -1) return false; // If ID is invalid, return false
		shapes.put(leastID--, shapes.get(id)); // Put the shape to the current least ID, then decrement leastID
		removeShape(id); // Then remove the old entry for the shape
		return true; // Return true to signify success
	}
	
	public synchronized boolean sendToTop(int id) {
		if (id > curID || id < leastID || id == -1) return false; // If ID is invalid, return false
		addShape(shapes.get(id)); // Add this shape again at the top
		removeShape(id); // Then remove the old entry for the shape
		return true; // Return true to signify success
	}
	
	/**
	 * Finds the most recent Shape that contains the given point, and returns its ID.
	 * 
	 * @param x	x-coordinate of the point
	 * @param y	y-coordinate of the point
	 * @return	ID of the uppermost colliding shape
	 */
	public synchronized int getUppermostCollision(int x, int y) {
		// Iterate over all IDs, from greatest to least
		for (Integer id : shapes.descendingKeySet()) {
			// If the shape corresponding to this ID contains the point,
			if (shapes.get(id).contains(x, y)) {
				return id; // return it (so the first such shape has its ID returned)
			}
		}
		return -1; // If no shape has contains this point, return -1
	}
	
	/**
	 * Getter for the current ID of this sketch.
	 * 
	 * @return	the current ID of this sketch
	 */
	public synchronized int getCurID() {
		return curID;
	}
	
	/**
	 * Setter for the current ID of this sketch.
	 * 
	 * @param curID	the new curID
	 */
	public synchronized void setCurID(int curID) {
		this.curID = curID;
	}
	
	/**
	 * Create a clone of this Sketch object.
	 */
	public synchronized Sketch clone() {
		TreeMap<Integer, Shape> newMap = new TreeMap<Integer, Shape>();
		for (int id : shapes.keySet()) {
			newMap.put(id, shapes.get(id) == null ? null : shapes.get(id).clone());
		}
		return new Sketch(newMap, curID, leastID);
	}
}
