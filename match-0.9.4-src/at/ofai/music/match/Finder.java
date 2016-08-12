package at.ofai.music.match;

/** Maps cost matrix coordinates into an efficient
 *  (linear instead of quadratic space) representation.
 *  Stores result of most recent mapping for fast
 *  sequential access.
 */
class Finder {

	PerformanceMatcher pm1, pm2;
	int index1, index2, bestRow, bestCol;
	int[] rowRange;
	int[] colRange;

	public Finder(PerformanceMatcher p1, PerformanceMatcher p2) {
		if (!p1.firstPM)
			System.err.println("Warning: wrong args in Finder()");
		pm1 = p1;
		pm2 = p2;
		index1 = 0;
		index2 = 0;
		rowRange = new int[2];
		colRange = new int[2];
	} // constructor

	/** Sets up the instance variables to point to the given coordinate in the
	 *  distance matrix.
	 *  @param i1 frameNumber in the first PerformanceMatcher
	 *  @param i2 frameNumber in the second PerformanceMatcher
	 *  @return true iff the point (i2,i1) is represented in the distance matrix
	 */
	public boolean find(int i1, int i2) {
		if (i1 >= 0) {
			index1 = i1;
			index2 = i2 - pm1.first[i1];
		}
		return (i1 >= 0) && (i2 >= pm1.first[i1]) && (i2 < pm1.last[i1]);
	} // find()

	/** Returns the range [lo,hi) of legal column indices for the given row. */
	public void getColRange(int row, int[] range) {
		range[0] = pm1.first[row];
		range[1] = pm1.last[row];
	} // getColRange()

	/** Returns the range [lo,hi) of legal row indices for the given column. */
	public void getRowRange(int col, int[] range) {
		range[0] = pm2.first[col];
		range[1] = pm2.last[col];
	} // getRowRange()

	public int getExpandDirection(int row, int col) {
		return getExpandDirection(row, col, false);
	} // getExpandDirection()

	public int getExpandDirection(int row, int col, boolean check) {
		int min = getPathCost(row, col);
		bestRow = row;
		bestCol = col;
		getRowRange(col, rowRange);
		if (rowRange[1] > row+1)
			rowRange[1] = row+1;	// don't cheat by looking at future :)
		for (int index = rowRange[0]; index < rowRange[1]; index++) {
			int tmp = getPathCost(index, col);
			if (tmp < min) {
				min = tmp;
				bestRow = index;
			}
		}
		getColRange(row, colRange);
		if (colRange[1] > col+1)
			colRange[1] = col+1;	// don't cheat by looking at future :)
		for (int index = colRange[0]; index < colRange[1]; index++) {
			int tmp = getPathCost(row, index);
			if (tmp < min) {
				min = tmp;
				bestCol = index;
				bestRow = row;
			}
		}
	//	System.err.print("  BEST: " + bestRow + " " + bestCol + " " + check);
	//	System.err.println(" " + pm1.frameCount + " " + pm2.frameCount);
		if (check) {
	//		System.err.println(find(row+1, col) + " " + find(row, col+1));
			if (!find(row, col+1))
				return PerformanceMatcher.ADVANCE_THIS;
			if (!find(row+1, col))
				return PerformanceMatcher.ADVANCE_OTHER;
		}
		return ((bestRow==row)? PerformanceMatcher.ADVANCE_THIS: 0) |
			   ((bestCol==col)? PerformanceMatcher.ADVANCE_OTHER: 0);
	} // getExpandDirection()
	
	public byte getDistance(int row, int col) {
		if (find(row, col))
			return pm1.distance[row][col - pm1.first[row]];
		throw new IndexOutOfBoundsException("getDistance("+row+","+col+")");
	} // getDistance()/2

	public void setDistance(int row, int col, byte b) {
		if (find(row, col))
			pm1.distance[row][col - pm1.first[row]] = b;
		throw new IndexOutOfBoundsException("setDistance("+
											row+","+col+","+b+")");
	} // setDistance()

	public int getPathCost(int row, int col) {
		if (find(row, col))							// "1" avoids div by 0 below
			return pm1.bestPathCost[row][col - pm1.first[row]]*100/ (1+row+col);
		throw new IndexOutOfBoundsException("getPathCost("+row+","+col+")");
	} // getPathCost()
	
	public int getRawPathCost(int row, int col) {
		if (find(row, col))
			return pm1.bestPathCost[row][col - pm1.first[row]];
		throw new IndexOutOfBoundsException("getPathCost("+row+","+col+")");
	} // getRawPathCost()

	public void setPathCost(int row, int col, int i) {
		if (find(row, col))
			pm1.bestPathCost[row][col - pm1.first[row]] = i;
		throw new IndexOutOfBoundsException("setPathCost("+
											row+","+col+","+i+")");
	} // setPathCost()

	public byte getDistance() {
		return pm1.distance[index1][index2];
	} // getDistance()/0

	public void setDistance(int b) {
		pm1.distance[index1][index2] = (byte)b;
	} // setDistance()

	public int getPathCost() {
		return pm1.bestPathCost[index1][index2];
	} // getPathCost()

	public void setPathCost(int i) {
		pm1.bestPathCost[index1][index2] = i;
	} // setPathCost()

	/** Calculates a rectangle of the path cost matrix so that the minimum cost
	 *  path between the bottom left and top right corners can be computed.
	 *  Caches previous values to avoid calling find() multiple times, and is
	 *  several times faster as a result.
	 *  @param r1 the bottom of the rectangle to be calculated
	 *  @param c1 the left side of the rectangle to be calculated
	 *  @param r2 the top of the rectangle to be calculated
	 *  @param c2 the right side of the rectangle to be calculated
	 */
	public void recalculatePathCostMatrix(int r1, int c1, int r2, int c2) {
		if (!find(r1,c1))
			throw new IndexOutOfBoundsException(r1+ "," + c1 + " out of range");
/*/REMOVE
		System.err.print("Recalc: " + c1 + "," + r1 + " to " + c2 + "," + r2);
		long startTime = System.nanoTime();
		long currentTime;
//--REMOVE */
		int thisRowStart, c;
		int prevRowStart = 0, prevRowStop = 0;
		for (int r = r1; r <= r2; r++) {
			thisRowStart = pm1.first[r];
			if (thisRowStart < c1)
				thisRowStart = c1;
			for (c = thisRowStart; c <= c2; c++) {
				if (find(r,c)) {
					int i2 = index2;
					int newCost = pm1.distance[r][i2];
					int dir = 0;
					if (r > r1) {	// not first row
						int min = -1;
						if ((c > prevRowStart) && (c <= prevRowStop)) {
							// diagonal from (r-1,c-1)
							min = pm1.bestPathCost[r-1][c-pm1.first[r-1]-1] +
																	newCost * 2;
							dir = PerformanceMatcher.ADVANCE_BOTH;
						}
						if ((c >= prevRowStart) && (c < prevRowStop)) {
							// vertical from (r-1,c)
							int cost = pm1.bestPathCost[r-1][c-pm1.first[r-1]] +
																	newCost;
							if ((min == -1) || (cost < min)) {
								min = cost;
								dir = PerformanceMatcher.ADVANCE_THIS;
							}
						}
						if (c > thisRowStart) {
							// horizontal from (r,c-1)
							int cost =pm1.bestPathCost[r][i2-1]+newCost;
							if ((min == -1) || (cost < min)) {
								min = cost;
								dir = PerformanceMatcher.ADVANCE_OTHER;
							}
						}
						pm1.bestPathCost[r][i2] = min;
					} else if (c > thisRowStart) {	// first row
						// horizontal from (r,c-1)
						pm1.bestPathCost[r][i2] = pm1.bestPathCost[r][i2-1] +
														newCost;
						dir = PerformanceMatcher.ADVANCE_OTHER;
					}
					if ((r != r1) || (c != c1))
						pm1.distance[r][i2] = (byte) ((pm1.distance[r][i2] &
												PerformanceMatcher.MASK) | dir);
				} else
					break;	// end of row
			}
			prevRowStart = thisRowStart;
			prevRowStop = c;
		}
/*/REMOVE
		currentTime = System.nanoTime();
		System.err.println("  Time: " + ((currentTime-startTime)/1e6));
		startTime = currentTime;
//--REMOVE */
	} // recalculatePathCostMatrix()

} // class Finder
