import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * A multi-segment Shape, with straight lines connecting "joint" points -- (x1,y1) to (x2,y2) to (x3,y3) ...
 * 
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Spring 2016
 * @author CBK, updated Fall 2016
 * @author Jared Cole, implemented
 */
public class Polyline implements Shape {
	private List<Point> pointList; // A list of points that serve as endpoints of segments
	private Color color; // The color of the polyline
	private static final int TOLERANCE = 3; // Constant for the tolerance of the contains method
	
	/**
	 * Constructs a new Polyline object based on the given pointList and color.
	 * 
	 * @param pointList	list of initial points for this polyline
	 * @param color		color of the polyline
	 */
	public Polyline(List<Point> pointList, Color color) {
		this.pointList = pointList;
		this.color = color;
	}
	
	/**
	 * Adds the given point to the polyline.
	 * 
	 * @param p	the Point to be added to the polyline
	 */
	public void addPoint(Point p) {
		pointList.add(p);
	}
	
	@Override
	/**
	 * Move the Polyline by a given displacement.
	 */
	public void moveBy(int dx, int dy) {
		for (Point point : pointList) {
			point.translate(dx, dy);
		}
	}

	@Override
	/**
	 * Get the color of the Polyline.
	 */
	public Color getColor() {
		return color;
	}

	@Override
	/**
	 * Set the color of the Polyline.
	 */
	public void setColor(Color color) {
		this.color = color;
	}
	
	@Override
	/**
	 * Determines whether a given point "collides" (gets close enough to) a polyline based on the constant tolerance.
	 */
	public boolean contains(int x, int y) {
		// Iterate over the first n-1 points in the polyline
		for (int i = 0; i < pointList.size()-1; i++) {
			// Check if the segment from point i to point i+1 "collides" with the given point
			if (Segment.pointToSegmentDistance(x, y, (int) pointList.get(i).getX(),
					(int) pointList.get(i).getY(), (int) pointList.get(i+1).getX(),
					(int) pointList.get(i+1).getY()) <= TOLERANCE) { // TOLERANCE upper bound of collision distance
				return true; // Returns true if the given point gets close enough to any segment
			}
		}
		// Otherwise the given point didn't get close enough to any segment, so returns false
		return false;
	}

	@Override
	/**
	 * Draw the Polyline on the given Graphics object.
	 */
	public void draw(Graphics g) {
		g.setColor(color); // Set the color to the color of the current Polyline
		// Then iterate over the first n-1 points in the polyline
		for (int i = 0; i < pointList.size()-1; i++) {
			// And draw the line segment from point i to point i+1
			g.drawLine((int) pointList.get(i).getX(), (int) pointList.get(i).getY(),
					(int) pointList.get(i+1).getX(), (int) pointList.get(i+1).getY());
		}
	}
	
	@Override
	/**
	 * Clones this Polyline.
	 */
	public Polyline clone() {
		ArrayList<Point> newList = new ArrayList<Point>();
		newList.addAll(pointList);
		return new Polyline(newList, color);
	}
	
	@Override
	/**
	 * Returns a String representation that can fully reconstruct the current Polyline.
	 */
	public String toString() {
		// Use a StringBuilder for a mutable string (will be using a lot of concatenation)
		// Initialize with "polyline" to identify the shape type; points enclosed in []
		StringBuilder toReturn = new StringBuilder("polyline [");
		
		// Now iterate over all points in the polyline
		for (int i = 0; i < pointList.size(); i++) {
			// Append a string of the form "x,y" encapsulating the information in the given point
			toReturn.append((int) pointList.get(i).getX() + "," + (int) pointList.get(i).getY()
					+ (i == pointList.size()-1 ? "" : ";")); // Use a ; to delimit points unless this is the last point
		}
		
		// Close the points list with "]" and append the color of the polyline
		toReturn.append("] " + color.getRGB());
		// Make the StringBuilder back into a normal String and return it
		return toReturn.toString();
	}
}
