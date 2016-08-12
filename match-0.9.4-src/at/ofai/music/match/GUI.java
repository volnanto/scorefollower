package at.ofai.music.match;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ListIterator;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;

import at.ofai.music.match.PSPrinter;

public class GUI extends JFrame implements ActionListener,
										   MouseListener,
										   MouseMotionListener,
										   KeyListener {

	public static final String version = "0.9.4";
	public static final String title = "MATCH " + version;
	protected static final int xSize = 230;
	protected static final int ySize = 150;
	protected static final int fileNameHeight = 20;
	protected static final int buttonWd = 25;
	protected static final int buttonHt = 15;
	protected static final int maxSlider = 1000;
	public static boolean DEBUG = true;
	protected static String loadFile = null;
	protected static final String READY = "Status: Ready";
	protected static final String LOADING = "Status: Loading";
	protected static final String ALIGNING = "Status: Aligning";
	public static final Color BACKGROUND = Color.black;
	public static final Color BACKGROUND2 = Color.gray;		// unmatched files
	public static final Color FOREGROUND = Color.green;		// text
	public static final Color HIGHLIGHT = Color.red;		// buttons, status
	static final long serialVersionUID = 0;
	
	protected PerformanceMatcher pm1, pm2;
	protected AligningAudioPlayer audioPlayer;
	protected JFileChooser fileChooser;
	protected MarkDisplay markDisplay;
	protected Help help;
	protected ScrollingMatrix scrollingMatrix;
	protected JSlider playSlider;
	protected TimePanel timePanel;
	protected JLabel readyLabel;
	protected JLabel modeLabel;
	protected int oldTime;
	protected double oldTimeDouble;
	protected int oldSlider;
	protected int originX, originY;

	protected GUI(PerformanceMatcher p1, PerformanceMatcher p2,
					ScrollingMatrix sm, boolean makeVisible) {
		super(title);
		fileChooser = new JFileChooser();	// NOTE: needed by AAP constructor
		audioPlayer = new AligningAudioPlayer(this, p1, p2, sm);
		pm1 = p1;
		pm2 = p2;
		scrollingMatrix = sm;
		oldTime = 0;
		oldTimeDouble = -1;
		oldSlider = 0;
		originX = 0;
		originY = 0;
		setUndecorated(true);
		setLayout(null);
		setSize(xSize,ySize);	// in case no files are given
		getContentPane().setBackground(BACKGROUND);
		addLabels();
		addTimePanel();
		addSlider();
		addMarkDisplay();
		addButtons();
		setLocation(10,10);
		addMouseMotionListener(this);
		addMouseListener(this);
		addKeyListener(this);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		if (makeVisible) {
			setVisible(true);
			requestFocusInWindow();
		}
		if (loadFile != null)
			audioPlayer.load(loadFile);
	} // constructor

	protected void addFiles(String[] files, int index) {
		for ( ; index < files.length; index++)
			addFile(files[index]);
	} // addFiles()

	protected void addFile(String pathName) {
		addFile(pathName, true);
	} // addFile()/1
	
	protected FileNameSelection addFile(String pathName, boolean addToPlayer) {
		String sep = System.getProperty("file.separator");
		int start = pathName.lastIndexOf(sep);
		int end = pathName.lastIndexOf(".");
		String baseName;
		if (end <= start)
			baseName = pathName.substring(start+1);
		else
			baseName = pathName.substring(start+1, end);
		FileNameSelection b = new FileNameSelection(baseName, this);
		b.setBounds(10, ySize + audioPlayer.getFileCount() * fileNameHeight,
					xSize - 20, fileNameHeight);
		if (addToPlayer) {
			audioPlayer.addFile(pathName, b);
			setSize(xSize, ySize+audioPlayer.getFileCount()*fileNameHeight+10);
		} else
			setSize(xSize, ySize + (audioPlayer.getFileCount()+1) *
									fileNameHeight + 10);
		add(b);
		validate();		// necessary in Windows, but not Linux
		return b;
	} // addFile()

	protected void removeFile(FileNameSelection b) {	// only remove the last!
		remove(b);
		setSize(xSize, ySize + audioPlayer.getFileCount() * fileNameHeight +10);
		validate();     // necessary in Windows, but not Linux
	} // removeFile()

	protected void addSlider() {
		playSlider = new JSlider(JSlider.HORIZONTAL, 0, maxSlider, oldSlider);
		playSlider.setBounds(10, ySize - buttonHt - 60, xSize - 20, 30);
		playSlider.setBackground(BACKGROUND);
		playSlider.addChangeListener(audioPlayer);
		playSlider.addKeyListener(this);
		add(playSlider);
	} // addSlider()

	class MarkDisplay extends JComponent {

		static final long serialVersionUID = 0;
		
		public void paint(Graphics g) {
			g.setColor(FOREGROUND);
			g.fillRect(0, 0, getWidth(), getHeight());
			long length = audioPlayer.getCurrentFileLength();
			if (length == 0)
				return;
			ListIterator<Long> i = audioPlayer.getMarkListIterator();
			g.setColor(HIGHLIGHT);
			while (i.hasNext()) {
				int x = (int) (i.next().longValue() * (getWidth()-1) / length);
				g.drawLine(x, 0, x, getHeight()-1);
			}
		} // paint()

	} // inner class MarkDisplay

	protected void addMarkDisplay() {
		final int thumbSize = 14;// est. width in pixels of the slider's pointer
		markDisplay = new MarkDisplay();
		markDisplay.setBounds(10 + thumbSize / 2, ySize - buttonHt - 30,
								xSize - 20 - thumbSize, 10);
		add(markDisplay);
	} // addMarkDisplay()

	protected void addLabels() {
		addLabel(title, 10, 5, xSize - 30 - buttonWd, 25, FOREGROUND);
		readyLabel = addLabel("", 20, 30, 110, 20, HIGHLIGHT);
		setStatus(READY);
		modeLabel = addLabel("", 20, 50, 110, 20, HIGHLIGHT);
		setMode(false);
	} // addLabels()
	
	protected JLabel addLabel(String text, int x,int y,int wd,int ht, Color c) {
		JLabel label = new JLabel(text);
		label.setForeground(c);
		label.setBounds(x, y, wd, ht);
		add(label);
		return label;
	} // addLabel()

	class TimePanel extends JComponent {	// simulates LCD timer display
		
		byte[] digits = {119,36,93,109,46,107,123,37,127,111};
		static final long serialVersionUID = 0;

		public void paint(Graphics g) {
			g.setColor(BACKGROUND);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(FOREGROUND);
			paintDigit(g, oldTime/600%6, 5);
			paintDigit(g, oldTime/60%10, 20);
			paintDigit(g, oldTime/10%6, 45);
			paintDigit(g, oldTime%10, 60);
			g.fillRoundRect(38, 15, 4, 4, 3, 3);
			g.fillRoundRect(38, 25, 4, 4, 3, 3);
		} // paint()

		public void paintDigit(Graphics g, int d, int x) {
			if ((digits[d] & 1) != 0)		// top
				g.fillRoundRect(x+1, 5, 10, 3, 3, 3);
			if ((digits[d] & 2) != 0)		// upper left
				g.fillRoundRect(x, 6, 3, 10, 3, 3);
			if ((digits[d] & 4) != 0)		// upper right
				g.fillRoundRect(x+10, 6, 3, 10, 3, 3);
			if ((digits[d] & 8) != 0)		// centre
				g.fillRoundRect(x+1, 15, 10, 3, 3, 3);
			if ((digits[d] & 16) != 0)		// lower left
				g.fillRoundRect(x, 16, 3, 10, 3, 3);
			if ((digits[d] & 32) != 0)		// lower right
				g.fillRoundRect(x+10, 16, 3, 10, 3, 3);
			if ((digits[d] & 64) != 0)		// bottom
				g.fillRoundRect(x+1, 25, 10, 3, 3, 3);
		}
	} // inner class TimePanel

	protected void addTimePanel() {
		timePanel = new TimePanel();
		timePanel.setBounds(140, 35, 80, 35);
		add(timePanel);
	} // addTimePanel()

	protected void addButtons() {
		GraphicsConfiguration gc = getGraphicsConfiguration();
		for (int i = 0; i < 9; i++) {
			Image image = gc.createCompatibleImage(buttonWd, buttonHt);
			Graphics g = image.getGraphics();
			g.setColor(BACKGROUND);
			g.fillRect(0,0,buttonWd, buttonHt);
			g.setColor(HIGHLIGHT);
			g.drawRect(0,0,buttonWd-1, buttonHt-1);
			g.setColor(HIGHLIGHT);
			int x = 10 + i * (buttonWd + 2);
			int y = ySize - buttonHt - 10;
			int[] x1, y1;
			String text = null;
			switch (i) {
				case 0:
					text = "play";
					x1 = new int[]{8,16,8};
					y1 = new int[]{3,7,11};
					g.fillPolygon(x1,y1,3);
					break;
				case 1:
					text = "pause";
					g.fillRect(9,4,2,7);
					g.fillRect(14,4,2,7);
					break;
				case 2:
					text = "stop";
					g.fillRect(9,4,7,7);
					break;
				case 3:
					text = "previous";
					x1 = new int[]{18,12,18};
					y1 = new int[]{3,7,11};
					g.fillPolygon(x1,y1,3);
					x1 = new int[]{12,6,12};
					g.fillPolygon(x1,y1,3);
					break;
				case 4:
					text = "mark";
					g.drawLine(9,4,15,10);
					g.drawLine(8,7,16,7);
					g.drawLine(9,10,15,4);
					g.drawLine(12,3,12,11);
					break;
				case 5:
					text = "next";
					x1 = new int[]{6,12,6};
					y1 = new int[]{11,7,3};
					g.fillPolygon(x1,y1,3);
					x1 = new int[]{12,18,12};
					g.fillPolygon(x1,y1,3);
					break;
				case 6:
					x = xSize - buttonWd - 10;
					text = "load";
					g.fillRect(12,3,2,8);
					g.fillRect(9,6,8,2);
					break;
				case 7:
					x = xSize - 2 * buttonWd - 12;
					y = 10;
					text = "help";
					Font f = g.getFont();
					g.setFont(f.deriveFont(Font.BOLD, 10.0f));
					g.drawString("?", 10, 12);
					break;
				case 8:
					x = xSize - buttonWd - 10;
					y = 10;
					text = "exit";
					g.drawLine(8,3,15,11);
					g.drawLine(9,3,16,11);
					g.drawLine(8,11,15,3);
					g.drawLine(9,11,16,3);
					break;
			}
			JButton button = new JButton(new ImageIcon(image));
			button.setActionCommand(text);
			button.setToolTipText(text);
			button.setBorder(null);
			button.setBounds(x,y,buttonWd,buttonHt);
			button.addActionListener(this);
			button.addKeyListener(this);
			add(button);
		}
	} // addButtons

	class FileNameSelection extends JButton {

		String name;
		boolean selected;
		double fractionMatched;
		static final long serialVersionUID = 0;

		public FileNameSelection(String n, GUI a) {
			name = n;
			selected = false;
			fractionMatched = 0;
			setActionCommand(name);
			setBorder(null);
			addActionListener(a);
			addKeyListener(a);
		} // constructor

		public void setSelected(boolean b) {
			selected = b;
			repaint();
		} // setSelected()

		public void setFraction(double d) {
			fractionMatched = d;
			repaint();
		} // setFraction()

		public void paint(Graphics g) {
			Font f = g.getFont();
			g.setFont(f.deriveFont(Font.PLAIN, 12.0f));
			int wd = (int)(fractionMatched * getWidth());
			if (fractionMatched != 1.0) {
				g.setColor(BACKGROUND2);
				g.fillRect(wd, 0, getWidth(), getHeight());
			}
			if (fractionMatched != 0.0) {
				g.setColor(BACKGROUND);
				g.fillRect(0, 0, wd, getHeight());
			}
			if (selected) {
				g.setColor(HIGHLIGHT);
				g.drawRect(0, 0, getWidth()-1, getHeight()-1);
			}
			g.setColor(FOREGROUND);
			g.drawString(name, 5, 14);
		} // paint()

	} // inner class FileNameSelection

	protected void setStatus(String status) {
		if (readyLabel == null)	// match Thread starts before constructor ends
			return;
		readyLabel.setText(status);
		readyLabel.repaint();
	} // setStatus()

	protected void setMode(boolean fromMark) {
		audioPlayer.setMode(fromMark);
		if (modeLabel == null)	// match Thread starts before constructor ends
			return;
		if (fromMark)
			modeLabel.setText("Mode: Repeat");
		else
			modeLabel.setText("Mode: Continue");
		modeLabel.repaint();
	} // setMode()

	public void setTimer(double time, AudioFile currentFile) {	// in seconds
		int newTime = (int) time;
		if (newTime != oldTime) {
			oldTime = newTime;
			timePanel.repaint();
		}
		if ((scrollingMatrix != null) && (currentFile != null) &&
										 (Math.abs(oldTimeDouble-time) > 0.1)) {
			oldTimeDouble = time;
			scrollingMatrix.setTime(time, currentFile);
		}
	} // setTimer()

	public void setSlider(double position) {	// position in [0,1]
		int newSlider = (int) (position * maxSlider);
		if (newSlider != oldSlider) {
			oldSlider = newSlider;
			playSlider.setValue(newSlider);
			playSlider.repaint();
		}
	} // setSlider()

	public void updateMarks() {
		markDisplay.repaint();
	} // updateMarks()

	protected void showHelp() {
		if (help == null)
			help = new Help();
		help.setVisible(true);
	} // showHelp()

	protected void loadFile() {
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			addFile(fileChooser.getSelectedFile().getAbsolutePath());
	} // loadFile()

/*	protected void saveWormFile() {
		if ((scrollingMatrix != null) && (scrollingMatrix.wormHandler != null)){
			if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			   scrollingMatrix.wormHandler.write(
			   				fileChooser.getSelectedFile(), true);
		}
	} // saveWormFile()  */

	// interface ActionListener
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		// System.err.println("Button Pressed: " + command +
		// 					" Modifiers: " + e.getModifiers());
		if (e.getModifiers() == 0)
			return;		// ignore key-generated actions
		if (command.equals("exit"))
			System.exit(0);
		else if (command.equals("help"))
			showHelp();
		else if (command.equals("play"))
			audioPlayer.play();
		else if (command.equals("pause"))
			audioPlayer.pause();
		else if (command.equals("stop"))
			audioPlayer.stop();
		else if (command.equals("previous"))
			audioPlayer.skipToPreviousMark();
		else if (command.equals("mark"))
			audioPlayer.addMark();
		else if (command.equals("next"))
			audioPlayer.skipToNextMark();
		else if (command.equals("load"))
			loadFile();
		else if (e.getSource() instanceof FileNameSelection)
			audioPlayer.setCurrentFile((FileNameSelection)e.getSource());
		else
			System.err.println("Unknown ActionEvent: " + e);
	} // actionPerformed

	// interface MouseMotionListener
	public void mouseMoved(MouseEvent e) {
		// requestFocusInWindow();		// for KeyEvents
	} // mouseMoved()
	
	public void mouseDragged(MouseEvent e) {	// move GUI
		setLocation(getX() + e.getX() - originX, getY() + e.getY() - originY);
	} // mouseDragged()

	// interface MouseListener
	public void mouseEntered(MouseEvent e) {
		requestFocusInWindow();		// for KeyEvents
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
		// System.err.println("keyPressed(): " + e);
		switch(e.getKeyCode()) {
			case KeyEvent.VK_Z:
				audioPlayer.play();
				break;
			case KeyEvent.VK_X:
				audioPlayer.pause();
				break;
			case KeyEvent.VK_C:
				audioPlayer.stop();
				break;
			case KeyEvent.VK_V:
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_KP_LEFT:
				audioPlayer.skipToPreviousMark();
				break;
			case KeyEvent.VK_B:
				audioPlayer.addMark();
				break;
			case KeyEvent.VK_N:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_KP_RIGHT:
				audioPlayer.skipToNextMark();
				break;
			case KeyEvent.VK_M:
				loadFile();
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_KP_DOWN:
				audioPlayer.nextFile();
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_KP_UP:
				audioPlayer.previousFile();
				break;
			case KeyEvent.VK_SPACE:
				audioPlayer.togglePlay();
				break;
			case KeyEvent.VK_COMMA:
				setMode(false);
				break;
			case KeyEvent.VK_PERIOD:
				setMode(true);
				break;
			case KeyEvent.VK_O:
				audioPlayer.clearFiles();
				break;
			case KeyEvent.VK_H:
			case KeyEvent.VK_SLASH:
				showHelp();
				break;
			case KeyEvent.VK_Q:
			case KeyEvent.VK_ESCAPE:
				System.exit(0);
				break;
			case KeyEvent.VK_1:
				audioPlayer.setCurrentFile(0);
				break;
			case KeyEvent.VK_2:
				audioPlayer.setCurrentFile(1);
				break;
			case KeyEvent.VK_3:
				audioPlayer.setCurrentFile(2);
				break;
			case KeyEvent.VK_4:
				audioPlayer.setCurrentFile(3);
				break;
			case KeyEvent.VK_5:
				audioPlayer.setCurrentFile(4);
				break;
			case KeyEvent.VK_6:
				audioPlayer.setCurrentFile(5);
				break;
			case KeyEvent.VK_7:
				audioPlayer.setCurrentFile(6);
				break;
			case KeyEvent.VK_8:
				audioPlayer.setCurrentFile(7);
				break;
			case KeyEvent.VK_9:
				audioPlayer.setCurrentFile(8);
				break;
			case KeyEvent.VK_0:
				audioPlayer.setCurrentFile(9);
				break;
			case KeyEvent.VK_P: // print
				PSPrinter.print(getContentPane());
				break;
			case KeyEvent.VK_S: // save
				audioPlayer.save();
				break;
			case KeyEvent.VK_R:	// restore
				audioPlayer.load();
				break;
	/*		case KeyEvent.VK_W:	// save wormfile
				saveWormFile();  
				break;  */
		}
	} // keyPressed()
	
	public void keyTyped(KeyEvent e) {}	// ignore
	public void keyReleased(KeyEvent e) {}	// ignore

} // class GUI
