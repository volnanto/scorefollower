package at.ofai.music.match;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ListIterator;

import javax.swing.JFrame;
import javax.swing.JPanel;

import at.ofai.music.match.Event;
import at.ofai.music.match.WormEvent;
import at.ofai.music.match.PSPrinter;
//import at.ofai.music.match.WormFile;

class MatrixFrame extends JFrame implements MouseListener,
											MouseMotionListener,
											KeyListener {

	ScrollingMatrix sm;
	MatrixPanel p;
	ListIterator<Event> eventIterator1, eventIterator2;
	AudioFile map;				// only for mapping time with the path
	int wd, ht;					// size of whole Frame
	int originX, originY;		// for dragging window
	final int defaultMargin = 80;
	final int matchFileMargin = 90;
	int top;		// size of margin
	int bottom;		// size of margin
	int left;		// size of margin
	int right;		// size of margin
	public static boolean useSmoothPath = true;
	public final Color BACKGROUND = Color.black;
	public final Color FOREGROUND = Color.green;
	public final int[] flagsToLength = { 0, 5,10,10,15,15,15,15,
										20,20,20,20,20,20,20,20};
	static final double ln2 = Math.log(2.0);
	static final long serialVersionUID = 0;

	public MatrixFrame(ScrollingMatrix s, boolean makeVisible) {
		super(GUI.title);
		sm = s;
		bottom = defaultMargin;
		left = defaultMargin;
		if ((sm.pm2.metadata == PerformanceMatcher.MetaType.MATCH) ||
				(sm.pm2.metadata == PerformanceMatcher.MetaType.MIDI)) // hasMatchFile)
			top = matchFileMargin;
		else
			top = defaultMargin;
		if ((sm.pm1.metadata == PerformanceMatcher.MetaType.MATCH) ||
				(sm.pm1.metadata == PerformanceMatcher.MetaType.MIDI)) // hasMatchFile)
			right = matchFileMargin;
		else
			right = defaultMargin;
		wd = sm.xSize + left + right;
		ht = sm.ySize + top + bottom;
		originX = 0;
		originY = 0;
		sm.setBounds(left, top, wd - left - right, ht - top - bottom);
		sm.parent = this;
		if (sm.pm1.events != null) {
			eventIterator1 = sm.pm1.events.listIterator();
			//if (sm.pm1.hasWormFile)
			if (sm.pm1.metadata == PerformanceMatcher.MetaType.WORM)
				generateLabels(eventIterator1);
			//if (sm.pm1.hasMatchFile)
			if ((sm.pm1.metadata == PerformanceMatcher.MetaType.MATCH) ||
					(sm.pm1.metadata == PerformanceMatcher.MetaType.MIDI))
				correctTiming(eventIterator1, sm.pm1.matchFileOffset);
		} else
			eventIterator1 = null;
		if (sm.pm2.events != null) {
			eventIterator2 = sm.pm2.events.listIterator();
			//if (sm.pm2.hasWormFile)
			if (sm.pm2.metadata == PerformanceMatcher.MetaType.WORM)
				generateLabels(eventIterator2);
			//if (sm.pm2.hasMatchFile)
			if ((sm.pm2.metadata == PerformanceMatcher.MetaType.MATCH) ||
					(sm.pm2.metadata == PerformanceMatcher.MetaType.MIDI))
				correctTiming(eventIterator2, sm.pm2.matchFileOffset);
		} else
			eventIterator2 = null;
		map = new AudioFile();
		p = new MatrixPanel();
		p.add(sm);
		p.setLayout(null);
		p.setBounds(0, 0, wd, ht);
		add(p);
		getContentPane().setBackground(Color.white);
		setUndecorated(true);
		setLayout(null);
		setSize(wd, ht);
		setLocation(250,10);
		setResizable(false);
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		if (makeVisible) {
			setVisible(true);
			requestFocusInWindow();
		}
	} // constructor

 protected void generateLabels(ListIterator<Event> it) {
		int bar = 0;
		while (it.hasNext()) {
			WormEvent w = (WormEvent) it.next();
		/*	if ((w.flags & WormFile.BAR) != 0) {
				bar++;
				if (w.label != null)	// don't overwrite existing labels e.g. sapp data
					continue;
				if ((w.flags & WormFile.SEG1) != 0)
					w.label = "*" + bar;
				else
					w.label = "" + bar;
				if ((w.flags & WormFile.SEG2) != 0)
					w.label += "*";
			} */
		}
	} // generateLabels() 

	protected void correctTiming(ListIterator<Event> it, double offset) {
		if (it.hasNext()) {
			offset -= it.next().keyDown;
			it.previous();
		}
		while (it.hasNext()) {
			Event e = it.next();
			e.keyDown += offset;
			e.keyUp += offset;
			e.pedalUp += offset;
		}
	} // correctTiming()


	class MatrixPanel extends JPanel {

		FontMetrics fm;
		static final long serialVersionUID = 0;

		protected MatrixPanel() {
			fm = null;
		} // constructor

		/** Converts time in seconds to time in minutes and seconds: [-]mm:ss */
		protected String timeString(int t) {
			String s = "";
			if (t < 0) {
				s = "-";
				t = -t;
			}
			s += (t / 60) + ":";
			if (t % 60 < 10)
				s += "0";
			s += t % 60;
			return s;
		} // timeString()

		protected void addHTick(Graphics g, String s, int x) {
			g.drawLine(x, ht - bottom, x, ht - bottom + 8);
			x -= fm.stringWidth(s) / 2;
			int y = ht - bottom + fm.getHeight() + 8;
			g.drawString(s, x, y);
		} // addHTick()

		protected void addHMarker(Graphics g, String s, int x, int l) {
			g.drawLine(x, top - l, x, top);
			if (s != null) {
				x -= fm.stringWidth(s) / 2;
				int y = top - l - 4;
				g.drawString(s, x, y);
			}
		} // addHMarker()

		/** Displays bar and beat markers from a WormFile. */
		protected void addHWorm(Graphics g) {
			while (eventIterator2.hasPrevious()) {
				Event e = eventIterator2.previous();
				if (e.keyDown / sm.hop2 + sm.x0 < 0)
					break;
			}
			while (eventIterator2.hasNext()) {
				WormEvent e = (WormEvent) eventIterator2.next();
				int x = (int) Math.round(e.keyDown / sm.hop2) + sm.x0;
				if (x < 0)
					continue;
				else if (x > wd - left - right)
					break;
			//	if ((e.flags & WormFile.BAR) != 0)
		//			addHMarker(g, e.label, x + left, flagsToLength[e.flags&15]);
				else
					addHMarker(g, null, x + left, flagsToLength[e.flags&15]);
			}
		} // addHWorm()

		/** Displays tempo curve based on the OTHER file's WormFile. */
		protected void addHTempoCurve(Graphics g) {
			if (useSmoothPath)
				map.setMatch(sm.sPathY, sm.hop1, sm.sPathX, sm.hop2,
						 sm.sPathLength);	// x is reference
			else	// note bPath is in the opposite order
				map.setMatch(sm.bPathY, sm.hop1, sm.bPathX, sm.hop2, sm.bPathLength);
			double t1 = 0;
			while (eventIterator1.hasPrevious()) {
				Event e = eventIterator1.previous();
				t1 = map.toReferenceTimeD(e.keyDown);
				if (t1 / sm.hop1 + sm.x0 < 0)
					break;
			}
			int x1 = (int) Math.round(t1 / sm.hop1) + sm.x0;
			int y1 = -1;
			double stop = 0;
			if (useSmoothPath) {
				if (sm.sPathLength > 0)
					stop = sm.sPathY[sm.sPathLength - 1] * sm.hop1;
			} else if (sm.bPathLength > 0)
				stop = sm.bPathY[0] * sm.hop1;	// bPath is in reverse order
			while (eventIterator1.hasNext()) {
				WormEvent e = (WormEvent) eventIterator1.next();
				double t2 = map.toReferenceTimeD(e.keyDown);
				int x2 = (int) Math.round(t2 / sm.hop1) + sm.x0;
				int y2 = y1;
				if (t2 > t1) {
					y2 = 30 + (int) Math.round(10 * Math.log(t2 - t1) / ln2);
					if (y2 < 0)
						y2 = 0;
					else if (y2 > 50)
						y2 = 50;
				}
				if (x2 < 0)
					continue;
				else if (e.keyDown > stop)
					break;
			//	if ((e.flags & WormFile.BAR) != 0)
			//		addHMarker(g, e.label, x2 + left, flagsToLength[e.flags&15]);
				else
					addHMarker(g, null, x2 + left, flagsToLength[e.flags&15]);
				if (y1 >= 0)
					g.drawLine(x1 + left, y1, x2 + left, y2);
				t1 = t2;
				x1 = x2;
				y1 = y2;
			}
		} // addHTempoCurve()

		/** Displays piano roll notation from a match file. */
		protected void addHMatch(Graphics g) {
			while (eventIterator2.hasPrevious()) {
				Event e = eventIterator2.previous();
				if (e.pedalUp / sm.hop2 + sm.x0 < left + right - wd)
					break;
			}
			while (eventIterator2.hasNext()) {
				Event e = (Event) eventIterator2.next();
				int x1 = (int) Math.round(e.keyDown / sm.hop2) + sm.x0;
				int x2 = (int) Math.round(e.pedalUp / sm.hop2) + sm.x0;
				if (x2 < 0)
					continue;
				else if (x1 > wd - left - right)
					break;
				if (x1 < 0)
					x1 = 0;
				if (x2 > wd - left - right)
					x2 = wd - left - right;
				int y = top + 20 - e.midiPitch;
				g.drawLine(x1 + left, y, x2 + left, y);
			}
		} // addHMatch()

		// add tick marks and labels on the vertical axis without rotation
		protected void addVTickH(Graphics g, String s, int y) {
			g.drawLine(left - 8, y, left, y);
			if (s != null) {
				int x = left - 10 - fm.stringWidth(s);
				y += fm.getHeight() / 2;
				g.drawString(s, x, y);
			}
		} // addVTickH()

		protected void addVTick(Graphics g, String s, int y) {
			g.drawLine(-y, left - 8, -y, left);
			int x = -y - fm.stringWidth(s) / 2;
			y = left - 12;
			g.drawString(s, x, y);
		} // addVTick()

		// add markers on the right hand border without rotation
		protected void addVMarkerH(Graphics g, String s, int y, int l) {
			g.drawLine(wd - right, y, wd - right + l, y);
			if (s != null) {
				int x = wd - right + l + 4;
				y += fm.getHeight() / 2;
				g.drawString(s, x, y);
			}
		} // addVMarkerH()

		protected void addVMarker(Graphics g, String s, int y, int l) {
			g.drawLine(-y, wd - right, -y, wd - right + l);
			if (s != null) {
				int x = -y - fm.stringWidth(s) / 2;
				y = wd - right + l + 4 + fm.getHeight();
				g.drawString(s, x, y);
			}
		} // addVMarker()

		protected void addVWorm(Graphics g) {
			while (eventIterator1.hasPrevious()) {
				Event e = eventIterator1.previous();
				if (sm.y0 - e.keyDown / sm.hop1 > ht - top - bottom)
					break;
			}
			while (eventIterator1.hasNext()) {
				WormEvent e = (WormEvent) eventIterator1.next();
				int y = sm.y0 - (int) Math.round(e.keyDown / sm.hop1);
				if (y > ht - top - bottom)
					continue;
				else if (y < 0)
					break;
			//	if ((e.flags & WormFile.BAR) != 0)
			//		addVMarker(g, e.label, y + top, flagsToLength[e.flags&15]);
				else
					addVMarker(g, null, y + top, flagsToLength[e.flags&15]);
			}
		} // addVWorm()

		protected void addVMatch(Graphics g) {
			while (eventIterator1.hasPrevious()) {
				Event e = eventIterator1.previous();
				if (sm.y0 - e.pedalUp / sm.hop1 > 2 * (ht - top - bottom))
					break;
			}
			while (eventIterator1.hasNext()) {
				Event e = eventIterator1.next();
				int y1 = sm.y0 - (int) Math.round(e.keyDown / sm.hop1);
				int y2 = sm.y0 - (int) Math.round(e.pedalUp / sm.hop1);
				if (y2 > ht - top - bottom)
					continue;
				else if (y1 < 0)
					break;
				if (y1 > ht - top - bottom)
					y1 = ht - top - bottom;
				if (y2 < 0)
					y2 = 0;
				int x = wd + 20 - e.midiPitch;
				g.drawLine(-y1-top, x, -y2-top, x);	// axes have been rotated
			}
		} // addVMatch()

		protected void addHLabel(Graphics g, String s) {
			int x = (wd - fm.stringWidth(s)) / 2;
			int y = ht - bottom + 2 * fm.getHeight() + 12;
			g.drawString(s, x, y);
		} // addHLabel()

		protected void addVLabel(Graphics2D g, String s) {
			int y = left - fm.getHeight() - 16;
			int x = -(ht + fm.stringWidth(s)) / 2;
			g.drawString(s, x, y);
		} // addVLabel()

		public void paintComponent(Graphics g) {
			if (fm == null)
				fm = g.getFontMetrics();
			g.setColor(BACKGROUND);
			g.fillRect(0, 0, wd, top);
			g.fillRect(0, ht - bottom, wd, bottom);
			g.fillRect(0, top, left, ht - top - bottom);
			g.fillRect(wd - right, top, right, ht - top - bottom);
			g.setColor(FOREGROUND);
			g.drawRect(left-1, top-1, wd-left-right+1, ht-top-bottom+1);
			if (sm.pm2.audioFileName == null)
				addHLabel(g, "Live Input");
			else
				addHLabel(g, sm.pm2.audioFileName);
			int tickx = (int) Math.ceil((wd - left - right) * sm.hop2 / 10);
			int tx = (int) Math.ceil(-sm.x0 * sm.hop2 / tickx) * tickx;
			int x = (int) Math.round(tx / sm.hop2) + sm.x0;
			int dx = (int) Math.round(tickx / sm.hop2);
			while (x < wd - left - right) {
				addHTick(g, timeString(tx), x + left);
				tx += tickx;
				x += dx;
			}
			if (eventIterator2 != null) {
				//if (sm.pm2.hasWormFile)
				if (sm.pm2.metadata == PerformanceMatcher.MetaType.WORM)
					addHWorm(g);
				//if (sm.pm2.hasMatchFile)
				if ((sm.pm2.metadata == PerformanceMatcher.MetaType.MATCH) ||
						(sm.pm2.metadata == PerformanceMatcher.MetaType.MIDI))
					addHMatch(g);
			//} else if ((eventIterator1 != null) && sm.pm1.hasWormFile) {
			} else if ((eventIterator1 != null) &&
					(sm.pm1.metadata == PerformanceMatcher.MetaType.WORM)) {
				// TODO: implement addHTempoCurve and equiv for V
				// adds tempo curve based on worm
				addHTempoCurve(g);
			}
			int ticky = (int) Math.ceil((ht - top - bottom) * sm.hop1 / 10);
			int ty = (int) Math.floor(sm.y0 * sm.hop1 / ticky) * ticky;
			int y = (int) Math.round(sm.y0 - ty / sm.hop1);
			int dy = (int) Math.round(ticky / sm.hop1);
			Graphics2D g2 = (Graphics2D)g;
			g2.rotate(-Math.PI / 2);
			if (sm.pm1.audioFileName == null)
				addVLabel(g2, "Live Input");
			else
				addVLabel(g2, sm.pm1.audioFileName);
			while (y < ht - top - bottom) {
				addVTick(g2, timeString(ty), y + top);
				ty -= ticky;
				y += dy;
			}
			if (eventIterator1 != null) {
				//if (sm.pm1.hasWormFile)
				if (sm.pm1.metadata == PerformanceMatcher.MetaType.WORM)
					addVWorm(g2);
				//if (sm.pm1.hasMatchFile)
				if ((sm.pm1.metadata == PerformanceMatcher.MetaType.MATCH) ||
						(sm.pm1.metadata == PerformanceMatcher.MetaType.MIDI))
					addVMatch(g2);
			}
			g2.rotate(Math.PI / 2);
		} // paintComponent()

	} // class MatrixPanel

	// interface MouseMotionListener
	public void mouseDragged(MouseEvent e) {    // move GUI
		setLocation(getX() + e.getX() - originX, getY() + e.getY() - originY);
	} // mouseDragged()

	public void mouseMoved(MouseEvent e) {} // mouseMoved()

	// interface MouseListener
	public void mouseEntered(MouseEvent e) {
		requestFocusInWindow();     // for KeyEvents
	} // mouseEntered()

	public void mouseExited(MouseEvent e) {}

	public void mouseClicked(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {
		originX = 0;
		originY = 0;
	} // mouseReleased()

	public void mousePressed(MouseEvent e) {
		originX = e.getX();
		originY = e.getY();
	} // mouseClicked()

	// interface KeyListener
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
			case KeyEvent.VK_P:
				PSPrinter.print(this);
				break;
			case KeyEvent.VK_Q:
			case KeyEvent.VK_ESCAPE:
				setVisible(false);
				break;
		}
	} // keyPressed()

	public void keyTyped(KeyEvent e) {} // ignore
	public void keyReleased(KeyEvent e) {}  // ignore

} // class MatrixFrame
