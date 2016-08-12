package at.ofai.music.match;

/** Maintains a list of fixed points through which all alignment paths must pass.
 *  This class assumes that the list of points has an unchangeable first and last
 *  element, corresponding to the beginnings and ends of both files. It is
 *  implemented as a doubly linked list. 
 */
public class FixedPoint {

	protected int x;
	protected int y;
	protected FixedPoint next, prev;

	private FixedPoint(int x, int y) {
		this.x = x;
		this.y = y;
		next = null;
		prev = null;
	} // constructor

	private void insert(FixedPoint p) {
		p.next = this;
		p.prev = prev;
		prev.next = p;
		prev = p;
	} // insert()

	/** Remove the current point from the list containing it.
	 *  It is assumed that this point is not the first or last
	 *  in the list.
	 */
	public void remove() {		// not for the first or last
		prev.next = next;
		next.prev = prev;
	} // remove()

	/** Inserts a new point into the list in sorted (ascending) order
	 *  of both coordinates. The new point will be rejected if it can
	 *  not be inserted into the list such that both the x and y coordinates
	 *  are monotonically non-decreasing.
	 *  @param x The x-coordinate of the new point
	 *  @param y The y-coordinate of the new point
	 *  @return Indicates whether the insertion was successful:
	 *          it will be unsuccessful if the new point would make
	 *          the list non-monotonic
	 */
	public FixedPoint insert(int x, int y) {
		FixedPoint p = new FixedPoint(x,y);
		insert(p);
		if (p.sort())
			return p;
		return null;
	} // insert()

	private boolean sort() {
		FixedPoint posn = next;
		if ((prev.prev != null) && ((x < prev.x) ||
										  ((x == prev.x) && (y < prev.y)))) {
			posn = prev;
			while ((posn.prev != null) && ((x < posn.prev.x) ||
									(x == posn.prev.x) && (y < posn.prev.y)))
				posn = posn.prev;
		} else {
			while ((posn.next != null) && ((x > posn.x) ||
										  ((x == posn.x) && (y > posn.y))))
				posn = posn.next;
		}
		if (posn != next) {
			next.prev = prev;
			prev.next = next;
			posn.insert(this);
		}
		if ((x < prev.x) || (y < prev.y) || (x > next.x) || (y > next.y)) {
			remove();	// paths must be monotonic
			return false;
		}
		return true;
	} // sort()

	/** This class assumes a list of points has an unchangeable first and last
	 *  element, corresponding to the beginnings and ends of both files; this
	 *  method creates and initialises such a list with the two end points.
	 *  @param x1 x-coordinate of the start point (usually 0)
	 *  @param y1 y-coordinate of the start point (usually 0)
	 *  @param x2 x-coordinate of the end point (usually length of file x)
	 *  @param y2 y-coordinate of the end point (usually length of file y)
	 */
	public static FixedPoint newList(int x1, int y1, int x2, int y2) {
		FixedPoint p = new FixedPoint(x2, y2);
		p.prev = new FixedPoint(x1, y1);
		p.prev.next = p;
		return p;
	} // newList()

} // class FixedPoint
