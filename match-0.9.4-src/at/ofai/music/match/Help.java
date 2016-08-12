package at.ofai.music.match;

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

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Help extends JFrame implements ActionListener,
											KeyListener,
											MouseListener,
											MouseMotionListener {

	static final String[] helpText = {
		"The following shortcut keys are defined:",
		"'z' - play",
		"'x' - pause",
		"'c' - stop",
		"'v','LEFT' - go to previous mark",
		"'b' - add/remove mark at current position",
		"'n','RIGHT' - go to next mark",
		"'m' - load new audio file",
		"'o' - clear all audio files",
		"'s' - save session",
		"'r' - restore session",
		"'w' - write worm file format",
		"'p' - print screenshot",
		"'SPACE' - toggle play/pause",
		"',' - continue mode: plays from current position",
		"'.' - repeat mode: plays from previous mark ",
		"'UP' - go to previous file",
		"'DOWN' - go to next file",
		"'1'...'9' - go to file number n",
		"'0' - go to file number 10",
		"'h','/' - show this help screen",
		"'q','ESCAPE' - exit"};
	static final int xSize = 300;
	static final int ySize = 18 * (helpText.length + 3);
	static final int buttonWd = 25;
	static final int buttonHt = 15;
	private int originX, originY;
	static final long serialVersionUID = 0;

	public static void main(String[] args) { new Help(); } // main()

	protected Help() {
		super(GUI.title + "  -  Help");
		originX = 0;
		originY = 0;
		setUndecorated(true);
		setLayout(null);
		setSize(xSize,ySize);
		getContentPane().setBackground(GUI.BACKGROUND);
		addButton();
		JLabel l = new JLabel(GUI.title + "  -  Help");
		l.setBackground(GUI.BACKGROUND);
		l.setForeground(GUI.FOREGROUND);
		l.setBounds(10, 10, xSize - buttonWd - 30, buttonHt + 10);
		add(l);
		HelpText t = new HelpText();
		t.setBounds(0, buttonHt + 15, xSize, ySize - buttonHt - 15);
		add(t);
		setLocation(10,10);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		setResizable(false);
		setVisible(true);
		requestFocusInWindow();
	} // constructor

	protected void addButton() {
		GraphicsConfiguration gc = getGraphicsConfiguration();
		Image image = gc.createCompatibleImage(buttonWd, buttonHt);
		Graphics g = image.getGraphics();
		g.setColor(GUI.BACKGROUND);
		g.fillRect(0,0,buttonWd, buttonHt);
		g.setColor(GUI.HIGHLIGHT);
		g.drawRect(0,0,buttonWd-1, buttonHt-1);
		g.setColor(GUI.HIGHLIGHT);
		g.drawLine(8,3,15,11);
		g.drawLine(9,3,16,11);
		g.drawLine(8,11,15,3);
		g.drawLine(9,11,16,3);
		JButton button = new JButton(new ImageIcon(image));
		String text = "close";
		button.setActionCommand(text);
		button.setToolTipText(text);
		button.setBorder(null);
		button.setBounds(xSize - buttonWd - 10, 10, buttonWd, buttonHt);
		button.addActionListener(this);
		add(button);
	} // addButtons
	
	class HelpText extends JComponent {

		static final long serialVersionUID = 0;
		public void paint(Graphics g) {
			Font f = g.getFont();
			g.setFont(f.deriveFont(Font.PLAIN, 12.0f));
			g.setColor(GUI.BACKGROUND);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(GUI.FOREGROUND);
			for (int i = 0; i < helpText.length; i++)
				g.drawString(helpText[i], 10, 18 * (i + 1));
		} // paint()

	} // inner class HelpText

	// interface ActionListener
	public void actionPerformed(ActionEvent e) {
		setVisible(false);
	} // actionPerformed

	// interface KeyListener
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
			case KeyEvent.VK_X:
			case KeyEvent.VK_Q:
			case KeyEvent.VK_ESCAPE:
				setVisible(false);
				break;
		//	case KeyEvent.VK_DOWN:
		//	case KeyEvent.VK_KP_DOWN:
		//		break;
		//	case KeyEvent.VK_UP:
		//	case KeyEvent.VK_KP_UP:
		//		break;
		}
	} // keyPressed()
	
	public void keyTyped(KeyEvent e) {}	// ignore
	public void keyReleased(KeyEvent e) {}	// ignore

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
			
	// interface MouseMotionListener
	public void mouseMoved(MouseEvent e) {
		// requestFocusInWindow();		// for KeyEvents
	} // mouseMoved()
	
	public void mouseDragged(MouseEvent e) {	// move GUI
		setLocation(getX() + e.getX() - originX, getY() + e.getY() - originY);
	} // mouseDragged()

} // class Help
