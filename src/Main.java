import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * A quick Main method to test the construction, toString, and contains of a Polyline.
 * 
 * @author Jared Cole
 */
public class Main {
	public static void main(String[] args) {
		List<Point> testList = new ArrayList<Point>();
		testList.add(new Point(0,0));
		testList.add(new Point(5,0));
		testList.add(new Point(5,5));
		testList.add(new Point(0,5));
		
		Polyline testPoly = new Polyline(testList, Color.BLACK);
		
		System.out.println(testPoly);
		System.out.println(testPoly.contains(2, 3));
		System.out.println(testPoly.contains(6, 7));
		System.out.println(testPoly.contains(-4, 0));
	}
}
