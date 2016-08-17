package at.ofai.music.match;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;


import javax.swing.JFrame;

//import at.ofai.music.util.Profile;
import at.ofai.music.match.PSPrinter;
import at.ofai.music.match.WebsocketClientEndpoint;

/** Displays a 2D cost matrix in greyscale, and shows the minimum cost paths
 *  calculated forwards and backwards in real time, scrolling as necessary.
 *  The backward path algorithm uses DTW; the forward paths use OLTW and OLTW
 *  with smoothing. The latter are (causal) real-time estimates of the optimal
 *  path.
 */
public class ScrollingMatrix extends Canvas implements KeyListener,
												MouseListener,
												MouseMotionListener {


	protected JFrame parent;
	protected BufferedImage img;
	protected Graphics gImage, gThis;
	protected Dimension sz;
	protected Rectangle clipRect;
	protected LinkedList<PerformanceMatcher.Onset> correct;
	protected ListIterator<PerformanceMatcher.Onset> listIterator;
	
	protected WebsocketClientEndpoint clientEndPoint; //WebSocket
	
	protected double hop1, hop2;
	protected int x0prev, y0prev;
	protected int showHorizontal, showVertical;
	protected int x0, y0;	// origin
	protected int xscroll, yscroll;
	protected int pathStartX, pathStartY; // , pathEndX, pathEndY;
	protected int[] fPathX, fPathY, bPathX, bPathY, sPathX, sPathY; 
	protected int fPathLength, bPathLength, sPathLength;
	protected PerformanceMatcher pm1, pm2;
	protected Finder finder;
	protected boolean showForwardPath, showBackwardPath, showSmoothedPath;
	protected boolean showCorrect;
	protected boolean showCentreCrossbar;
	private transient int[] range = new int[2];
	protected FixedPoint lastPoint;
	protected int currentX, currentY;
	protected int xSize, ySize;
//	protected WormHandler wormHandler;
	protected boolean liveWorm;
	protected int paintCount;
	
	static final int white = 0x00FFFFFF;
	static final int red = 0x00FF0000;
	static final int green = 0x0000FF00;
	static final int blue = 0x000000FF;
	static final int yellow = 0x00FFFF00;
	static final int cyan = 0x0000FFFF;
	static final int magenta = 0x00FF00FF;
	static final int black = 0x00000000;
	static final int scrollFactor = 10;
	static final int DEFAULT_HEIGHT = 600;
	static final int DEFAULT_WIDTH = 600;
	static final long serialVersionUID = 0;
	
	

	/** Creates a window containing the similarity matrix (distance matrix) for
	 *  the two PerformanceMatchers showing the minimum cost paths.
	 *  By pressing button 1, the minimum cost function at the current mouse
	 *  position can be shown.
	 *  Note that if <code>PerformanceMatcher.batchMode</code> is
	 *  <code>true</code> then no display
	 *  will be opened, but the optimal path will be calculated and printed to
	 *  standard output, including the evaluation based on the match files.
	 *  @param pm1 the PerformanceMatcher corresponding to the vertical axis of
	 *  the display
	 *  @param pm2 the PerformanceMatcher corresponding to the horizontal axis
	 *  of the display
	 * @throws URISyntaxException 
	 */
//	public static ScrollingMatrix showInFrame(PerformanceMatcher pm1,
//											   PerformanceMatcher pm2) {
//		ScrollingMatrix sm = new ScrollingMatrix(pm1, pm2);
//		if (!PerformanceMatcher.batchMode)
//			new MatrixFrame(sm);
//		return sm;
//	} // showInFrame()

	public ScrollingMatrix(PerformanceMatcher pm1, PerformanceMatcher pm2) throws URISyntaxException {
		this(pm1, pm2, DEFAULT_HEIGHT, DEFAULT_WIDTH, pm1.evaluateMatch(pm2));
	} // constructor

	/** Constructor, usually called by showInFrame().
	 *  The two performance matchers contain m, the minimum path cost matrix,
	 *  and d, the distance matrix (bits 2-7) and the direction of the minimum
	 *  cost path (bits 0-1), which are accessed by finder. The first dimension
	 *  of the matrices corresponds to the first performance matcher, which is
	 *  indexed by row number (i.e. shown on the vertical axis). 
	 *  @param pm1 the first PerformanceMatcher
	 *  @param pm2 the second PerformanceMatcher
	 *  @param sz1 the vertical size of the display window
	 *  @param sz2 the horizontal size of the display window
	 *  @param l the list of paired onset times from the match files of the two
	 *  performances
	 * @throws URISyntaxException 
	 */
	public ScrollingMatrix(PerformanceMatcher pm1, PerformanceMatcher pm2,
				int sz1, int sz2, LinkedList<PerformanceMatcher.Onset> l) throws URISyntaxException {
		
		parent = null;
		this.pm1 = pm1;
		this.pm2 = pm2;
		liveWorm = pm1.liveWorm || pm2.liveWorm;
//		wormHandler = pm1.wormHandler;
//		if (wormHandler == null)
//			wormHandler = pm2.wormHandler;
	//	if (wormHandler != null)
		//	wormHandler.setScrollingMatrix(this);
		xSize = sz2;
		ySize = sz1;
		
		clientEndPoint = new WebsocketClientEndpoint(new URI("ws://localhost:8080/WebApp/echo"));
		//WebSocket
		
		finder = new Finder(pm1, pm2);
		clipRect = new Rectangle(0, 0, xSize, ySize);
		correct = l;
		if (correct != null)
			listIterator = correct.listIterator();
		else
			listIterator = null;
		sz = new Dimension(xSize, ySize);
		showForwardPath = false;
		showBackwardPath = true;
		showSmoothedPath = false;
		showCorrect = true;
		new Thread(new ScrollThread(this)).start();
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		init();
	} // constructor

	public void init() {
		hop1 = pm1.hopTime;
		hop2 = pm2.hopTime;
		showHorizontal = showVertical = -1;
		pathStartX = pathStartY = -1;
		fPathLength = 0;
		bPathLength = 0;
		sPathLength = 0;
		fPathX = null;	// forces creation of arrays in checkArray
		paintCount = 0;
		x0 = xSize;
		y0 = -1;
		x0prev = x0;
		y0prev = y0;
		
		
		
		showCentreCrossbar = false;
		lastPoint = null;
		currentX = -1;
		currentY = -1;
		clearImage();
	} // init()

	class ScrollThread implements Runnable {
		
		ScrollingMatrix sm;
		
		ScrollThread(ScrollingMatrix s) {
			sm = s;
		}
		
		public void run() {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			while (true) {
				try {
					synchronized (sm) {
						sm.wait();
					}
					sm.scroll(sm.xscroll, sm.yscroll);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	
	} // inner class ScrollThread

	protected void setTime(double t1, AudioFile current) {
		double t2;
		showCentreCrossbar = true;
		if (pm1.audioFile == current) {
			t2 = pm2.audioFile.fromReferenceTimeD(current.toReferenceTimeD(t1));
		} else if (pm2.audioFile == current) {
			t2 = t1;
			t1 = pm1.audioFile.fromReferenceTimeD(current.toReferenceTimeD(t1));
		} else
			return;
		x0 = (int)(-t2 / hop2 + xSize / 2);
		y0 = (int)(t1 / hop1 + ySize / 2); 
		redrawImage();
		repaint();
	} // setTime()

	protected void scroll(int x, int y) {
		if ((x != 0) || (y != 0)) {
			x0 -= x;
			y0 -= y;
			redrawImage();
			repaint();
		}
	} // scroll()

	protected void updateMatrix(boolean playing) throws FileNotFoundException, URISyntaxException {
	//	Profile.start(1);
		if (playing) {	// scroll to end
		//	if (liveWorm && (wormHandler != null))
		//		wormHandler.update();
			if (++paintCount >= 4) {
				x0 = sz.width - pm2.frameCount;
				y0 = pm1.frameCount - 1;
				redrawImage();
				paintCount = 0;
				updatePaths(playing);
				repaint();	// hack because it is too slow
			}
		} else {
			updatePaths(playing);
			setTime(0, pm1.audioFile);
		}
	//	Profile.log(1);
	} // updateMatrix()

	protected void clearImage() {
		if (gImage != null) {
			gImage.setColor(Color.white);
			gImage.fillRect(0, 0, sz.width, sz.height);
		}
	} // clearImage()
	
	protected void redrawImage() {
		if (img ==  null) {
			img = getGraphicsConfiguration().createCompatibleImage(sz.width,
																   sz.height);
			gImage = img.getGraphics();
			clearImage();
		}
		int dx = x0 - x0prev;
		int dy = y0 - y0prev;
		if ((dx != 0) || (dy != 0)) {
	/*		if (gThis == null)
				gThis = getGraphics();
			try {
				gThis.copyArea(0, 0, sz.width, sz.height, dx, dy);//much faster
			} catch (Exception e) {
				if (!PerformanceMatcher.silent)
					System.err.print(e);
				repaint();
			}
	*/		gImage.copyArea(0, 0, sz.width, sz.height, dx, dy);
		}
		int start, stop, colour;
		if (dx != 0) {
			if (dx < 0) {
				start = Math.max(0, sz.width + dx);
				stop = sz.width;
			} else {
				start = 0;
				stop = Math.min(dx, sz.width);
			}
			for (int x = start; x < stop; x++)
				for (int y = 0; y < sz.height; y++) {
					if (finder.find(y0 - y, x - x0)) {
						colour = finder.getDistance() & PerformanceMatcher.MASK;
						colour |= (colour << 8) | (colour << 16);
						img.setRGB(x,y,colour);
					} else
						img.setRGB(x,y,white);
				}
		}
		if (dy != 0) {
			if (dy < 0) {
				start = Math.max(0, sz.height + dy);
				stop = sz.height;
			} else {
				start = 0;
				stop = Math.min(dy, sz.height);
			}
			for (int y = start; y < stop; y++)
				for (int x = 0; x < sz.width; x++) {
					if (finder.find(y0 - y, x - x0)) {
						colour = finder.getDistance() & PerformanceMatcher.MASK;
						colour |= (colour << 8) | (colour << 16);
						img.setRGB(x,y,colour);
					} else
						img.setRGB(x,y,white);
				}
		}
	/*
		if (dx > 0)
			repaint(0, 0, dx, sz.height);
		else if (dx < 0)
			repaint(sz.width+dx, 0, sz.width, sz.height);
		if (dy > 0)
			repaint(0, 0, sz.width, dy);
		else if (dy < 0)
			repaint(0, sz.height+dy, sz.width, sz.height);
/*	*/
		x0prev = x0;
		y0prev = y0;
// Do we want this? Certainly not while playing. Would need to set slider too.
//		if (gui != null) {
//			if (gui.audioPlayer.currentFile == pm1.audioFile)
//				gui.setTimer((y0 - ySize / 2) * hop1, null);
//			else if (gui.audioPlayer.currentFile == pm2.audioFile)
//				gui.setTimer((xSize / 2 - x0) * hop2, null);
//		}
	} // redrawImage()

	protected static int[] replace(int[] old, int oldSize, int newSize) {
		return replace(old, oldSize, newSize, false);
	} // replace()/3

	protected static int[] replace(int[] old,int oldSz,int newSz,boolean rev) {
		if ((oldSz != newSz) || rev) {
			int[] tmp = new int[newSz];
			if (rev)
				for (int i = 0; i < oldSz; i++)
					tmp[i] = old[oldSz-1-i];
			else
				for (int i = 0; i < oldSz; i++)
					tmp[i] = old[i];
			return tmp;
		} else if (rev) {	// not used
			for (int i = 0; i < oldSz / 2; i++) {
				int tmp = old[i];
				old[i] = old[oldSz-1-i];
				old[oldSz-1-i] = tmp;
			}
		}
		return old;
	} // replace()/4

	protected void checkPathArrays(boolean playing) {
		int len = 2 * (pm1.frameCount + pm2.frameCount + 1);
		if ((fPathX == null) && (len < 2 * (sz.width + sz.height))) {
			len = 2 * (sz.width + sz.height);		// initial size
			playing = false;		// so the arrays are created below
		}
		if ((fPathX == null)||(fPathX.length < pm1.frameCount +pm2.frameCount)){
			fPathX = replace(fPathX, fPathLength, len);
			fPathY = replace(fPathY, fPathLength, len);
		}
		if (!playing || (bPathX.length < pm1.frameCount + pm2.frameCount)) {
			bPathX = replace(bPathX, bPathLength, len);
			bPathY = replace(bPathY, bPathLength, len);
			sPathX = replace(sPathX, sPathLength, len);
			sPathY = replace(sPathY, sPathLength, len);
		}
	} // checkPathArrays()

	public void adjustCurrentPoint(int x, int y) {
		currentX = x;
		currentY = y;
	} // adjustCurrentPoint()

	public void finaliseCurrentPoint(int x, int y) {
		if (finder.find(y,x)) {
			FixedPoint p = lastPoint.insert(x,y);
			if (p != null) {
				finder.recalculatePathCostMatrix(p.y, p.x, p.next.y, p.next.x);
				updateBackwardPath(false);
				updateSmoothedPath(false);
			}
		}
	} // finaliseCurrentPoint()

	public void updatePaths(boolean playing) throws FileNotFoundException, URISyntaxException {
	//	Profile.start(2);
		int tmpx = finder.bestCol;
		int tmpy = finder.bestRow;
		
		DataStorage x = new DataStorage();
		HashMap <Integer, Integer> map  = x.getHashmap();
		
		clientEndPoint.sendMessage(map.get((int)tmpx/50));
	
	    //final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI("ws://localhost:8080/WebApp/echo"));
	  
	    
       // clientEndPoint.sendMessage(map.get((int)tmpx/50));
        
 		//System.out.println((int)tmpx);
 	
 		//System.out.println(tmpx/50);
 		
 		updateForwardPath(playing);
 		if (playing) {
 			pathStartX = tmpx;
 			pathStartY = tmpy;
 		}
 		updateBackwardPath(playing);
 		updateSmoothedPath(playing);
 	//	Profile.log(2);
 	} // updatePaths()

         
         
         
//     } catch (InterruptedException ex) {
//       System.err.println("InterruptedException exception: " + ex.getMessage());
		
		
		

	/** Recalculates the OLTW path.
	 *  Note that the forward path calculation is very slow; it takes ~1.5 s
	 *  for a 90 second piece (Chopin Etude).
	 */
	public void updateForwardPath(boolean playing) {
		checkPathArrays(playing);
		int x, y;
		if (fPathLength == 0) {
			if (pathStartX > 0)
				x = pathStartX;
			else
				x = 0;
			if (pathStartY > 0)
				y = pathStartY;
			else
				y = 0;
		} else {
			x = fPathX[fPathLength - 1];
			y = fPathY[fPathLength - 1];
		}
		if (!finder.find(y,x))
			return;
		while ((x < pm2.frameCount) && (y < pm1.frameCount)) {
			int to = finder.getExpandDirection(y,x,true);
			switch (to) {
				case PerformanceMatcher.ADVANCE_THIS: y++; break;
				case PerformanceMatcher.ADVANCE_OTHER: x++; break;
				case PerformanceMatcher.ADVANCE_BOTH: y++; x++; break;
			}
			if (finder.find(y,x)) {
				fPathX[fPathLength] = x;
				fPathY[fPathLength] = y;
				fPathLength++;
			} else if ((x < pm2.frameCount) && (y < pm1.frameCount))
				System.err.println("updateForwardPath(): path error at (" +
					y + ", " + x + ")");
			else
				break;
		}
	} // updateForwardPath()

	/** Recalculates the DTW path, given a list of constraints, that is, points
	 *  that the path should pass through.  Note that the forward path
	 *  recalculation had to be removed from this method because it doesn't
	 *  need to change and it takes 1.5 seconds to calculate for a 90 second
	 *  piece (Chopin Etude).
	 */
	public void updateBackwardPathOld(boolean playing) {
		checkPathArrays(playing);
		int x, y, xStop, yStop;
		if ((pathStartX >= 0) && (pathStartY >= 0)) {
			x = pathStartX;
			y = pathStartY;
		} else {
			x = pm2.frameCount - 1;
			y = pm1.frameCount - 1;
		}
		if (playing && !(pm1.paused && pm2.paused)) {
			xStop = x - sz.width - 1;
			if (xStop < 0)
				xStop = 0;
			yStop = y - sz.height - 1;
			if (yStop < 0)
				yStop = 0;
		} else
			xStop = yStop = 0;
		if ((bPathX[0] != x) || (bPathY[0] != y) || (bPathLength == 0) ||
				(bPathX[bPathLength-1] > xStop) ||
				(bPathY[bPathLength-1] > yStop)) {
			bPathLength = 0;
			while (finder.find(y,x) && ((x > xStop) || (y > yStop))) {
				bPathX[bPathLength] = x;
				bPathY[bPathLength] = y;
				bPathLength++;
				switch (finder.getDistance() & PerformanceMatcher.ADVANCE_BOTH){
					case PerformanceMatcher.ADVANCE_THIS: y--; break;
					case PerformanceMatcher.ADVANCE_OTHER: x--; break;
					case PerformanceMatcher.ADVANCE_BOTH: x--; y--; break;
				}
			}
		}
	} // updateBackwardPathOld()

	public void updateBackwardPath(boolean playing) {
		if (lastPoint == null) {
			updateBackwardPathOld(playing);
			return;
		}
		checkPathArrays(playing);
		FixedPoint p = lastPoint.prev;
		while (p.prev != null)
			p = p.prev;
		bPathLength = 0;
//		long startTime = System.nanoTime();
//		long currentTime;	
//		while (p.next != null) {	// restrict matrix to paths through all p's
//			System.err.print("Recalc: " + p.x + "," + p.y + " to " +
//								p.next.x + "," + p.next.y);
//			finder.recalculatePathCostMatrix(p.y, p.x, p.next.y, p.next.x);
//			currentTime = System.nanoTime();
//			System.err.println("  Time: " + ((currentTime - startTime) / 1e6));
//			startTime = currentTime;
//			p = p.next;
//		}
		p = lastPoint;
		while (p.prev != null) {	// calculate subpaths
			int x = p.x;
			int y = p.y;
			p = p.prev;
//			System.err.print("Path from: " +x+ "," +y+ " to " +p.x+ "," + p.y);
			loop: while (finder.find(y,x) && ((x > p.x) || (y > p.y))) {
				bPathX[bPathLength] = x;
				bPathY[bPathLength] = y;
				bPathLength++;
				switch (finder.getDistance() & PerformanceMatcher.ADVANCE_BOTH){
					case PerformanceMatcher.ADVANCE_THIS: y--; break;
					case PerformanceMatcher.ADVANCE_OTHER: x--; break;
					case PerformanceMatcher.ADVANCE_BOTH: x--; y--; break;
					case 0: System.err.println("\n" + x + "," + y); break loop;
				}
			}
//			currentTime = System.nanoTime();
//			System.err.println("  Time: " + ((currentTime - startTime) / 1e6));
//			startTime = currentTime;
		}
	} // updateBackwardPath()

	public void updateSmoothedPath(boolean playing) {
		for (int i = 0; i < bPathLength; i++) {
			sPathX[i] = bPathX[bPathLength-1-i];
			sPathY[i] = bPathY[bPathLength-1-i];
		}
		sPathLength = Path.smooth(sPathX, sPathY, bPathLength);
		if ((pm1.audioFile == null) || (pm2.audioFile == null))
			return;
		//if (MatrixFrame.useSmoothPath) {
		if (pm1.audioFile.isReference) {
			pm2.audioFile.setMatch(sPathX, pm2.hopTime, sPathY, pm1.hopTime,
									sPathLength);
			pm2.audioFile.setFixedPoints(lastPoint, true);
		} else if (pm2.audioFile.isReference) {
			pm1.audioFile.setMatch(sPathY, pm1.hopTime, sPathX, pm2.hopTime,
									sPathLength);
			pm2.audioFile.setFixedPoints(lastPoint, false);
			System.err.println("Warning: pm1 is not reference file");
		}
		//}
	} // updateSmoothedPath()

	public void saveBackwardPath(String fileName) {
		savePath(fileName, bPathX, bPathY, bPathLength);
	} // saveBackwardPath()

	public void saveForwardPath(String fileName) {
		savePath(fileName, fPathX, fPathY, fPathLength);
	} // saveForwardPath()

	public void saveSmoothedPath(String fileName) {
		savePath(fileName, sPathX, sPathY, sPathLength);
	} // saveSmoothedPath()

	protected void savePath(String fileName, int[] pathX, int[] pathY, int len){
		try {
			java.io.PrintStream out = new java.io.PrintStream(fileName);
			for (int i = len-1; i >= 0; i--)
				out.printf(" %7.3f %7.3f\n", hop1 * pathY[i], hop2 * pathX[i]);
		} catch (Exception e) {
			System.err.println("Error saving path: " + e);
		}
	} // savePath()

	public int[] forwardPathX() {
		return replace(fPathX, fPathLength, fPathLength);
	} // forwardPathX()

	public int[] forwardPathY() {
		return replace(fPathY, fPathLength, fPathLength);
	} // forwardPathY()

	public int[] backwardPathX() {
		return replace(bPathX, bPathLength, bPathLength, true);
	} // backwardPathX()

	public int[] backwardPathY() {
		return replace(bPathY, bPathLength, bPathLength, true);
	} // backwardPathY()

	public int[] smoothedPathX() {
		return replace(sPathX, sPathLength, sPathLength);
	} // smoothedPathX()

	public int[] smoothedPathY() {
		return replace(sPathY, sPathLength, sPathLength);
	} // smoothedPathY()

	public void evaluatePaths() {
		if ((correct != null) && (correct.size() > 0)) {
			System.out.println("Evaluation of forward (OLTW) path");
			evaluateForwards(fPathX, fPathY, fPathLength);
			System.out.println("Evaluation of smoothed DTW path");
			evaluateForwards(sPathX, sPathY, sPathLength);
			System.out.println("Evaluation of backward (DTW) path");
			evaluateBackwards(bPathX, bPathY, bPathLength);
		} else
			System.err.println("evaluation error: match file empty or missing");
	} // evaluatePaths()

	protected void evaluateForwards(int[] pathx, int[] pathy, int pathLength) {
		listIterator = correct.listIterator();
		PerformanceMatcher.Onset onset;
		int xc, yc, dist, best, besti;
		int count = 0;
		int sumDist = 0;
		int i = 0;
		System.out.println("note  err cum.err av(s)   beat   y   yc    x   xc");
		while (listIterator.hasNext()) {	// evaluate best path
			onset = listIterator.next();
			yc = (int) Math.round(onset.time1 / hop1);
			xc = (int) Math.round(onset.time2 / hop2);
			while (((pathx[i] >= xc) || (pathy[i] >= yc)) && (i > 0))
				i--;
			best = Math.abs(pathy[i]-yc) + Math.abs(pathx[i]-xc); // Manhattan
			besti = i;
			while (((pathx[i] <= xc) || (pathy[i] <= yc)) && (i < pathLength)) {
				i++;
				dist = Math.abs(pathy[i]-yc) + Math.abs(pathx[i]-xc);
				if (dist < best) {
					best = dist;
					besti = i;
				}
			}
			sumDist += best;
			count++;
			System.out.println(String.format(
					"%4d %4d %6d %5.3f %7.2f %4d %4d %4d %4d",
					count, best, sumDist, hop1*sumDist/count,
					onset.beat, pathy[besti], yc, pathx[besti], xc));
		}
		System.out.println("Summary[" + count + "] " + sumDist);
	} // evaluateForwards()

	protected void evaluateBackwards(int[] pathx, int[] pathy, int pathLength) {
		listIterator = correct.listIterator();	// was listIter(correct.size())
		PerformanceMatcher.Onset onset;
		int xc, yc, dist, best, besti;
		int count = 0;
		int sumDist = 0;
		int i = pathLength - 1;
		System.out.println("note  err cum.err av(s)   beat   y   yc    x   xc");
		while (listIterator.hasNext()) {    // evaluate best path
			onset = listIterator.next();
			yc = (int) Math.round(onset.time1 / hop1);
			xc = (int) Math.round(onset.time2 / hop2);
			while (((pathx[i] >= xc) || (pathy[i] >= yc)) && (i < pathLength-1))
				i++;
			best = Math.abs(pathy[i]-yc) + Math.abs(pathx[i]-xc);
			besti = i;
			while (((pathx[i] <= xc) || (pathy[i] <= yc)) && (i > 0)) {
				i--;
				dist = Math.abs(pathy[i]-yc) + Math.abs(pathx[i]-xc);
				if (dist < best) {
					best = dist;
					besti = i;
				}
			}
			sumDist += best;
			count++;
			System.out.println(String.format(
					"%4d %4d %6d %5.3f %7.2f %4d %4d %4d %4d",
					count, best, sumDist, hop1*sumDist/count,
					onset.beat, pathy[besti], yc, pathx[besti], xc));
		}
		System.out.println("Summary[" + count + "] " + sumDist);
	} // evaluateBackwards()

/*
	protected void paintPixel(int x, int y, int colour) {
		x = x0 + x;
		y = y0 - y;
		if ((x >= 0) && (y >= 0) && (x < sz.width) && (y < sz.height))
			img.setRGB(x,y,colour);
	} // paintPixel()

	public void paintPathForwards(int x, int y) {
		while (finder.find(y,x)) {
			paintPixel(x, y, red);
			switch (finder.getExpandDirection(y,x)) {
				case PerformanceMatcher.ADVANCE_THIS: y++; break;
				case PerformanceMatcher.ADVANCE_OTHER: x++; break;
				case PerformanceMatcher.ADVANCE_BOTH: x++; y++; break;
			}
		}
	} // paintPathForwards()

	public void paintPathBackwards(int x, int y) {
		while (finder.find(y,x) && (x+y > 0)) {	// trace best path
			paintPixel(x, y, magenta);
			switch (finder.getDistance() & PerformanceMatcher.ADVANCE_BOTH) {
				case PerformanceMatcher.ADVANCE_THIS: y--; break;
				case PerformanceMatcher.ADVANCE_OTHER: x--; break;
				case PerformanceMatcher.ADVANCE_BOTH: x--; y--; break;
			}
		}
	} // paintPathBackwards()
*/
	
	/** Overrides Component.update() to stop flashing due to clearing bkgnd.
	 *  Only called for non-Swing Components, in response to a repaint().
	 */
	public void update(Graphics g) {
		paint(g);
	} // update()

	public void paint(Graphics g) {
	//	Profile.start(3);
		if (img != null)
			g.drawImage(img, 0, 0, null);
		paintCorrect(g);
		paintPaths(g);
		paintPathCostFunctions(g);
		paintFixedPoints(g);
		if (parent != null)
			parent.repaint();
		synchronized (this) {	// Scroller needs to know
			notify();
		}
	//	Profile.log(3);
	} // paint()

	/** Paints the minimum cost paths forwards, smoothed and backwards.
	 *  @param g the Graphics context for painting
	 */
	protected void paintPaths(Graphics g) {
		if (g.getClipBounds(clipRect) == null)
			clipRect.setRect(0, 0, sz.width, sz.height);
		if (showForwardPath)
			paintPath(g, fPathX, fPathY, fPathLength, Color.blue, false);
		if (showSmoothedPath)
			paintPath(g, sPathX, sPathY, sPathLength, Color.yellow, false);
		if (showBackwardPath)
			paintPath(g, bPathX, bPathY, bPathLength, Color.green, true);
	} // paintPaths()

	/** Paints a sequence of points.
	 *  @param g the Graphics context for painting
	 *  @param xp the x-coordinates of the points
	 *  @param yp the y-coordinates of the points
	 *  @param len the number of points to paint
	 *  @param c the colour to paint the points
	 */
	protected void paintPath(Graphics g, int[] xp, int[] yp,
							 int len, Color c, boolean reverse) {
		g.setColor(c);
		for (int i = 0; i < len; i++) {
			int x = x0 + xp[i];
			int y = y0 - yp[i];
			if (reverse) {
				if ((x < clipRect.x) && (y >= clipRect.y + clipRect.height))
					break;
				if ((x < clipRect.x + clipRect.width) || (y >= clipRect.y))
					g.drawLine(x, y, x, y);	// why is there no drawPixel()??!
			} else {
				if ((x >= clipRect.x + clipRect.width) || (y < clipRect.y))
					break;
				if ((x >= clipRect.x) && (y < clipRect.y + clipRect.height))
					g.drawLine(x, y, x, y);	// why is there no drawPixel()??!
			}
		}
	} // paintPath()
	
	/** Paints the onsets of corresponding chords as red '+'s.
	 *  @param g the Graphics object for painting this object
	 */
	protected void paintCorrect(Graphics g) {
		g.setColor(Color.red);
		if (showCentreCrossbar) {
			g.drawLine(xSize/2, 0, xSize/2, ySize);
			g.drawLine(0, ySize/2, xSize, ySize/2);
		}
		if (showCorrect && (listIterator != null)) {
			if (g.getClipBounds(clipRect) == null)
				clipRect.setRect(0, 0, sz.width, sz.height);
			while (listIterator.hasPrevious()) {
				PerformanceMatcher.Onset onset = listIterator.previous();
				int y = y0 - (int) (onset.time1 / hop1);
				int x = x0 + (int) (onset.time2 / hop2);
				if ((x < clipRect.x) || (y >= clipRect.y + clipRect.height))
					break;
				if ((x < clipRect.x + clipRect.width) && (y >= clipRect.y)) {
					g.drawLine(x-5,y,x+5,y);
					g.drawLine(x,y-5,x,y+5);
				}
			}
			while (listIterator.hasNext()) {
				PerformanceMatcher.Onset onset = listIterator.next();
				int y = y0 - (int) (onset.time1 / hop1);
				int x = x0 + (int) (onset.time2 / hop2);
				if ((x >= clipRect.x + clipRect.width) || (y < clipRect.y))
					break;
				if ((x >= clipRect.x) && (y < clipRect.y + clipRect.height)) {
					g.drawLine(x-5,y,x+5,y);
					g.drawLine(x,y-5,x,y+5);
				}
			}
		}
	} // paintCorrect()

	protected void paintFixedPoints(Graphics g) {
		int rad = 5;
		if (currentX >= 0) {
			int x = x0 + currentX;
			int y = y0 - currentY;
			g.setColor(Color.red);
			g.drawOval(x - rad, y - rad, 2 * rad, 2 * rad);
		}
		if (lastPoint != null) {
			g.setColor(Color.blue);
			for (FixedPoint p = lastPoint.prev; p.prev != null; p = p.prev) {
				int x = x0 + p.x;
				int y = y0 - p.y;
				g.drawOval(x - rad, y - rad, 2 * rad, 2 * rad);
			}
		}
	} // paintFixedPoints()

	/** Shows the matrix values along a vertical and horizontal axis.
	 *  Note: The matrix point (x,y) is mapped on the screen to (x+x0, -y+y0)
	 */
	protected void paintPathCostFunctions(Graphics g) {
		int scale = 1;
		int x = showVertical - x0;
		if ((showVertical >= 0) && (showVertical < sz.width) &&
					(x >= 0) && (x < pm2.frameCount)) {
			g.setColor(Color.cyan);
			g.drawLine(showVertical, 0, showVertical, sz.height);
			finder.getRowRange(x, range);
			int min = finder.getPathCost(range[0], x);
			for (int y = range[0] + 1; y < range[1]; y++) {
				int tmp = finder.getPathCost(y, x);
				if ((tmp > 0) && (tmp < min))
					min = tmp;
			}
			int prev = (finder.getPathCost(range[0], x) - min) / scale;
			for (int y = range[0] + 1; y < range[1]; y++) {
				int curr = (finder.getPathCost(y, x) - min) / scale;
				int y1 = y0 - y;
				g.drawLine(showVertical + prev, y1, showVertical + curr, y1-1);
				prev = curr;
			}
		}
		int y = y0 - showHorizontal;
		if ((showHorizontal >= 0) && (showHorizontal < sz.height) &&
					(y >= 0) && (y < pm1.frameCount)) {
			g.setColor(Color.magenta);
			g.drawLine(0, showHorizontal, sz.width, showHorizontal);
			finder.getColRange(y, range);
			int min = finder.getPathCost(y, range[0]);
			for (int xx = range[0] + 1; xx < range[1]; xx++) {
				int tmp = finder.getPathCost(y, xx);
				if ((tmp > 0) && (tmp < min))
					min = tmp;
			}
			int prev = (finder.getPathCost(y, range[0]) - min) / scale;
			for (int xx = range[0] + 1; xx < range[1]; xx++) {
				int curr = (finder.getPathCost(y, xx) - min) / scale;
				int x1 = xx + x0;
				g.drawLine(x1-1, showHorizontal-prev, x1, showHorizontal-curr);
				prev = curr;
			}
		}
	} // paintPathCostFunctions()

	public void setVisible(boolean v) {
		if (parent != null)
			parent.setVisible(v);
	} // setVisible()

	// interface MouseListener *************************************************
	
	public void mouseEntered(MouseEvent e) { requestFocusInWindow(); }
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) { mouseDragged(e); }
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			showVertical = -1;
			showHorizontal = -1;
		} else if (e.getButton() == MouseEvent.BUTTON2) {
			if (lastPoint == null)
				lastPoint = FixedPoint.newList(0, 0, pm2.frameCount - 1,
													 pm1.frameCount - 1);
			if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
				for (FixedPoint p = lastPoint.prev; p.prev != null; p= p.prev) {
					if ((Math.abs(e.getX() - x0 - p.x) < 5) &&
						(Math.abs(y0 - e.getY() - p.y) < 5) &&
						(Math.abs(pathStartX - p.x) < 5) &&
						(Math.abs(pathStartY - p.y) < 5)) {
						p.remove();
						finder.recalculatePathCostMatrix(p.prev.y, p.prev.x,
														 p.next.y, p.next.x);
						updateBackwardPath(false);
						updateSmoothedPath(false);
						break;
					}
				}
			} else
				finaliseCurrentPoint(e.getX() - x0, y0 - e.getY());
			currentX = -1;
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			// // fPathLength = 0;
			// pathStartX = -1;
			// pathStartY = -1;
			// updateBackwardPath();
			// updateSmoothedPath();
		}
		repaint();
	} // mouseReleased()
	
	// interface MouseMotionListener *******************************************
	
	public void mouseMoved(MouseEvent e) {} // mouseMoved()
	
	/** Button 1: shows best path cost curves for the current line and column;
	 *  Button 2: shows the optimal paths through the current point;
	 *  Button 3: not used
	 */
	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
			showHorizontal = e.getY();
			showVertical = e.getX();
		} else if ((e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) {
			// move the current fixed point
			if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
				pathStartX = e.getX() - x0;
				pathStartY = y0 - e.getY();
			}
			adjustCurrentPoint(e.getX() - x0, y0 - e.getY());
		} else if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0) {
			// // fPathLength = 0;
			// pathStartX = e.getX() - x0;
			// pathStartY = y0 - e.getY();
			// updateBackwardPath();
			// updateSmoothedPath();
		}
		repaint();
	} // mouseDragged()

	// interface KeyListener ***************************************************

	public void keyPressed(KeyEvent e) {
		processKey(e, true);
	} // keyPressed()

	public void keyReleased(KeyEvent e) {
		processKey(e, false);
	} // keyReleased()

	public void keyTyped(KeyEvent e) {}

	public void processKey(KeyEvent e, boolean down) {
		int sign = down? 1:-1;
		switch (e.getKeyCode()) {
			case KeyEvent.VK_P:
				if (down)
					PSPrinter.print(this);
				break;
			case KeyEvent.VK_Q:
				setVisible(false);
				return;
			case KeyEvent.VK_H:
				pm1.paused = true;
				pm2.paused = false;
				break;
			case KeyEvent.VK_W:
				pm1.paused = true;
				pm2.paused = true;
				break;
			case KeyEvent.VK_V:
				pm1.paused = false;
				pm2.paused = true;
				break;
			case KeyEvent.VK_SPACE:
				pm1.paused = false;
				pm2.paused = false;
				break;
			case KeyEvent.VK_X:
				pm1.paused = true;
				pm2.paused = true;
				PerformanceMatcher.stop = true;
				break;
			case KeyEvent.VK_C:
				if (down)
					showCorrect = !showCorrect;
				repaint();
				break;
			case KeyEvent.VK_B:
				if (down)
					showBackwardPath = !showBackwardPath;
				repaint();
				break;
			case KeyEvent.VK_F:
				if (down)
					showForwardPath = !showForwardPath;
				repaint();
				break;
			case KeyEvent.VK_S:
				if (down)
					showSmoothedPath = !showSmoothedPath;
				repaint();
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_KP_UP:
			case KeyEvent.VK_NUMPAD8:
				yscroll += -scrollFactor * sign;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_KP_DOWN:
			case KeyEvent.VK_NUMPAD2:
				yscroll += scrollFactor * sign;
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_KP_LEFT:
			case KeyEvent.VK_NUMPAD4:
				xscroll += -scrollFactor * sign;
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_KP_RIGHT:
			case KeyEvent.VK_NUMPAD6:
				xscroll += scrollFactor * sign;
				break;
			case KeyEvent.VK_NUMPAD1:
				xscroll += -scrollFactor * sign;
				yscroll += scrollFactor * sign;
				break;
			case KeyEvent.VK_NUMPAD3:
				xscroll += scrollFactor * sign;
				yscroll += scrollFactor * sign;
				break;
			case KeyEvent.VK_NUMPAD7:
				xscroll += -scrollFactor * sign;
				yscroll += -scrollFactor * sign;
				break;
			case KeyEvent.VK_NUMPAD9:
				xscroll += scrollFactor * sign;
				yscroll += -scrollFactor * sign;
				break;
			case KeyEvent.VK_END:
				if (down) {
					x0 = sz.width - pm2.frameCount;
					y0 = pm1.frameCount - 1;
					redrawImage();
				}
				repaint();
				return;
			case KeyEvent.VK_HOME:
				if (down) {
					x0 = 0;
					y0 = sz.height - 1;
					redrawImage();
				}
				repaint();
				return;
			case KeyEvent.VK_PAGE_UP:
				xscroll += sz.width / 2 * sign;
				yscroll += -sz.height / 2 * sign;
				break;
			case KeyEvent.VK_PAGE_DOWN:
				xscroll += -sz.width / 2 * sign;
				yscroll += sz.height / 2 * sign;
				break;
		}
		synchronized (this) {
			notify();
		}
	} // processKey()

} // class ScrollingMatrix

