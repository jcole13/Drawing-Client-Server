import java.awt.Color;
import java.awt.Graphics;

/**
 * A rectangle-shaped Shape
 * Defined by an upper-left corner (x1,y1) and a lower-right corner (x2,y2)
 * with x1<=x2 and y1<=y2
 * 
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012
 * @author CBK, updated Fall 2016
 * @author Jared Cole, implemented
 */
public class Rectangle implements Shape {
	private int x1, y1, x2, y2;
	private Color color;
	
	/**
	 * A degenerate point rectangle defined by a single corner
	 */
	public Rectangle(int x1, int y1, Color color) {
		this.x1 = x1; this.x2 = x1;
		this.y1 = y1; this.y2 = y1;
		this.color = color;
	}

	/**
	 * An rectangle defined by two corners
	 */
	public Rectangle(int x1, int y1, int x2, int y2, Color color) {
		setCorners(x1, y1, x2, y2);
		this.color = color;
	}
	
	/**
	 * Redefines the rectangle based on new corners
	 */
	public void setCorners(int x1, int y1, int x2, int y2) {
		// Ensure correct upper left and lower right, same as in Ellipse
		this.x1 = Math.min(x1, x2);
		this.y1 = Math.min(y1, y2);
		this.x2 = Math.max(x1, x2);
		this.y2 = Math.max(y1, y2);		
	}
	
	@Override
	/**
	 * Move this Rectangle by a given displacement.
	 */
	public void moveBy(int dx, int dy) {
		x1 += dx; y1 += dy;
		x2 += dx; y2 += dy;
	}
	
	// Color getters and setters, same as for any other shape
	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setColor(Color color) {
		this.color = color;
	}
		
	@Override
	/**
	 * Checks whether the given point is contained within the rectangle.
	 */
	public boolean contains(int x, int y) {
		// If the point is between the left and right sides of the rectangle, and between the top and
		// bottom sides of the rectangle (both inclusive)
		if ((x <= x2 && x >= x1) && (y <= y2 && y >= y1)) {
			return true; // Return true
		}
		// Otherwise the Rectangle does not contain the given point
		return false;
	}

	@Override
	/**
	 * Draws the Rectangle on the given Graphics object.
	 */
	public void draw(Graphics g) {
		g.setColor(color); // Set the color to the current Rectangle's color
		g.fillRect(x1, y1, x2 - x1, y2 - y1); // And fill in the appropriate rectangle
	}
	
	@Override
	/**
	 * Clones this Rectangle.
	 */
	public Rectangle clone() {
		return new Rectangle(x1, y1, x2, y2, color);
	}
	
	@Override
	/**
	 * Returns a String representation that can fully reconstruct the current Rectangle.
	 */
	public String toString() {
		// First token "rect" as shape type identifier; then, points representing the two "defining
		// corners" of the rectange; finally, the color of the rectangle
		return "rect " + x1 + " " + y1 + " " + x2 + " " + y2 + " " + color.getRGB();
	}
}
