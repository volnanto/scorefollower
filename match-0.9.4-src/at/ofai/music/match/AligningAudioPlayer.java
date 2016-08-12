package at.ofai.music.match;

import java.util.LinkedList;
import java.util.ListIterator;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;

public class AligningAudioPlayer implements ChangeListener, Runnable {

	protected GUI gui;
	protected PerformanceMatcher pm1, pm2;
	protected ScrollingMatrix sm;
	protected Matcher matcher;
	protected Thread matchThread;
	protected LinkedList<AudioFile> files;
	protected LinkedList<Long> marks;
	protected JFileChooser jfc;
	protected long currentPosition, requestedPosition;
	protected boolean playFromMark;
	protected boolean stopRequested;
	protected AudioFile currentFile, requestedFile;
	protected boolean playing;
	protected SourceDataLine audioOut;
	protected int outputBufferSize;
	protected long audioLength;
	protected byte[] readBuffer, readBuffer2;
//	public static boolean showMatch = false;
	protected static final int readBufferSize = 2048;	// 46ms@44.1kHz
	protected static final int defaultOutputBufferSize = 16384;
//	protected static final String[] pmArgs = {"-b","-q"};	// use PM defaults
							// ,"-n1","-n4","-s","90","-h",".02","-f",".046"};

	public AligningAudioPlayer(GUI g, PerformanceMatcher p1,
									  PerformanceMatcher p2,
									  ScrollingMatrix s) {
		gui = g;
		pm1 = p1;
		pm2 = p2;
		sm = s;
	//	PerformanceMatcher.processArgs(pm1, pm2, pmArgs);
		files = new LinkedList<AudioFile>();
		currentFile = null;
		requestedFile = null;
		currentPosition = 0;
		playFromMark = false;
		requestedPosition = -1;
		stopRequested = false;
		playing = false;
		outputBufferSize = 0;
		marks = new LinkedList<Long>();
		readBuffer = new byte[readBufferSize];
		readBuffer2 = new byte[readBufferSize];
		matcher = new Matcher(g);
		jfc = gui.fileChooser;
		new Thread(this).start();			// for audio playback
		matchThread = new Thread(matcher);	// for aligning audio files
		matchThread.start();
	} // constructor

	public int getFileCount() {
		return files.size();
	} // getFileCount()

	public void play() {
		synchronized(this) {
			if (!playing)
				notify();
		}
	} // play()
	
	public void pause() {
		if (playing)
			stopRequested = true;
	} // pause()

	public void stop() {
		if (playing) {
			stopRequested = true;
			requestedPosition = 0;
		} else
			setPosition(0);
	} // stop()

	public void togglePlay() {
		if (playing)
			pause();
		else
			play();
	} // togglePlay()

	public void setMode(boolean fromMark) {
		playFromMark = fromMark;
	} // setMode

	protected void setPosition(long positionRequested) {
		if (requestedFile != null) {
			currentFile = requestedFile;
			requestedFile = null;
		}
		if (currentFile != null) {
			try {
				currentPosition = currentFile.setPosition(positionRequested);
				if (currentPosition != positionRequested)
					System.err.println("setPosition() failed: " +
						currentPosition + " instead of " + positionRequested);
				// else System.err.println("setPosition: " + currentPosition);
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
			updateGUI();
		}
	} // setPosition()

	protected void updateGUI() {
		gui.setSlider(((double)currentPosition) / currentFile.length);
		gui.setTimer(currentPosition / currentFile.frameSize /
										currentFile.frameRate, currentFile);
	} // updateGUI()

	public void skipToNextMark() {
		if (currentFile != null) {
			ListIterator<Long> i = marks.listIterator();
			long newPosn = currentFile.length;
			while (i.hasNext()) {
				long l = i.next().longValue();
				if (l > currentPosition) {
					newPosn = l;
					break;
				}
			}
			if (playing)
				requestedPosition = newPosn;
			else
				setPosition(newPosn);
		}
	} // skipToNextMark()

	public void skipToPreviousMark() {
		if (currentFile != null) {
			ListIterator<Long> i = marks.listIterator();
			long newPosn = 0;
			while (i.hasNext()) {
				long l = i.next().longValue();
				if (l >= currentPosition)
					break;
				newPosn = l;
			}
			if (playing)
				requestedPosition = newPosn;
			else
				setPosition(newPosn);
		}
	} // skipToPreviousMark()

	public void addMark() {
		if (currentFile != null) {
			long newMark = correctedPosition();
			ListIterator<Long> i = marks.listIterator();
			while (i.hasNext()) {
				long l = i.next().longValue();
				if (l == newMark) {
					i.remove();
					gui.updateMarks();
					return;
				}
				if (l > newMark) {
					i.previous();
					break;
				}
			}
			i.add(new Long(newMark));
			gui.updateMarks();
		}
	} // addMark()

	public long correctedPosition() {
		if (audioOut == null)
			return currentPosition;
		return currentPosition - (outputBufferSize - audioOut.available());
	} // correctedPosition()

	public ListIterator<Long> getMarkListIterator() {
		return marks.listIterator();
	} // getMarkListIterator()

	public long getCurrentFileLength() {
		if (requestedFile != null) {
			try {	// avoid race conditions
				return requestedFile.length;
			} catch (NullPointerException e) {}
		}
		try {	// avoid race conditions
			return currentFile.length;
		} catch (NullPointerException e) {}
		return 0;
	} // getCurrentFileLength()

	public void save() {
		File f = null;
		if ((jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) &&
				(!jfc.getSelectedFile().exists() ||
					(JOptionPane.showConfirmDialog(null,
					"File " + jfc.getSelectedFile().getAbsolutePath() +
					" exists.\nDo you want to replace it?", "Are you sure?",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)))
			f = jfc.getSelectedFile();
		if (f == null)
			return;
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f)); 
			ListIterator<AudioFile> iter = files.listIterator();
			while (iter.hasNext()) {
				AudioFile af = iter.next();
				out.write("File: " + af.path + "\n");
				if (af == currentFile) {
					out.write("Marks: " + marks.size() + "\n");
					ListIterator<Long> iter2 = marks.listIterator();
					while (iter2.hasNext())
						out.write(iter2.next().longValue() + "\n");
				} else
					out.write("Marks: -1\n");
				int count = 0;
				FixedPoint p = af.fixedPoints;
				while (p != null) {
					p = p.prev;
					count++;
				}
				out.write("FixedPoints: " + af.orientationX + " " + count+"\n");
				p = af.fixedPoints;
				while (p != null) {
					out.write(p.x + "\n");
					out.write(p.y + "\n");
					p = p.prev;
				}
				if (af.isReference)
					out.write("0\n0\n0\n0\n");
				else {
					out.write(af.thisHopTime + "\n");
					out.write(af.refHopTime + "\n");
					out.write(af.pathLength + "\n");
					for (int i = 0; i < af.pathLength; i++)
						out.write(af.thisIndex[i] + "\n");
					out.write(af.pathLength + "\n");
					for (int i = 0; i < af.pathLength; i++)
						out.write(af.refIndex[i] + "\n");
				}
			}
			out.close();
		} catch (java.io.IOException e) {
			System.err.println("IOException while saving data");
		}
	} // save()

	public void load() {
		File f = null;
		if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			load(jfc.getSelectedFile().getAbsolutePath());
	} // load()

	public static String checkString(String in, String prefix) {
		return checkString(in, prefix, true);
	} // checkString()

	public static String checkString(String in, String prefix, boolean exc) {
		if ((in != null) && in.startsWith(prefix))
			return in.substring(prefix.length());
		if (!exc)
			return null;
		throw new IllegalArgumentException("Expecting: "+prefix+"; got: "+in);
	} // checkString()

	public void load(String fileName) {
		File f = new File(fileName);
		if (!f.exists()) {
			System.err.println("File " + fileName + " does not exist.");
			return;
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(f));
			gui.setStatus(GUI.LOADING);
			clearFiles();
			marks.clear();
			int[] thisIndex, refIndex;
			int len;
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;
				line = checkString(line, "File: ");
				GUI.FileNameSelection selector = gui.addFile(line, false);
				AudioFile af = new AudioFile(line, selector);
				files.add(af);
				line = checkString(in.readLine(), "Marks: ");
				len = Integer.parseInt(line);
				if (len >= 0) {
					setCurrentFile(selector);
					for (int i = 0; i < len; i++)
						marks.add(new Long(Long.parseLong(in.readLine())));
				}
				String tmp = in.readLine();
				line = checkString(tmp, "FixedPoints: ", false);
				if (line != null) {
					boolean isX = true;
					if (line.startsWith("true ")) {
						line = line.substring(5);
					} else {
						isX = false;
						line = line.substring(6);
					}
					int ln = Integer.parseInt(line);
					FixedPoint p = null;
					FixedPoint q = null;
					for (int i = 0; i < ln; i++) {
						int x = Integer.parseInt(in.readLine());
						int y = Integer.parseInt(in.readLine());
						if (p == null)
							p = FixedPoint.newList(0,0,x,y);	// dummy start
						else
							q = p.insert(x,y);
					}
					if (q != null)
						q.prev = null;	// remove dummy start
					af.setFixedPoints(p, isX);
					line = in.readLine();
				} else
					line = tmp;
				double thisHopTime = Double.parseDouble(line);
				double refHopTime = Double.parseDouble(in.readLine());
				len = Integer.parseInt(in.readLine());
				if (len > 0) {
					thisIndex = new int[len];
					for (int i = 0; i < len; i++)
						thisIndex[i] = Integer.parseInt(in.readLine());
				} else
					thisIndex = null;
				len = Integer.parseInt(in.readLine());
				if (len > 0) {
					refIndex = new int[len];
					for (int i = 0; i < len; i++)
						refIndex[i] = Integer.parseInt(in.readLine());
					af.setMatch(thisIndex, thisHopTime,refIndex,refHopTime,len);
				}
				selector.setFraction(1.0);
			}
			gui.setStatus(GUI.READY);
			gui.updateMarks();
		} catch (IllegalArgumentException e) {
			System.err.println("Load error: " + e);
			clearFiles();
			marks.clear();
		} catch (java.io.IOException e) {
			System.err.println("IOException while loading data");
			clearFiles();
			marks.clear();
		}
	} // load()

	protected void clearFiles() {	// not thread-safe
		stop();
		while (playing) {
			try { Thread.sleep(100); } catch (InterruptedException e) {}
			stop();		// in case play() is called while clearing
		}
		matcher.stop();
		ListIterator<AudioFile> iter = files.listIterator(files.size());
		while (iter.hasPrevious()) {	// remove backwards for gui consistency
			AudioFile af = iter.previous();
			iter.remove();
			gui.removeFile(af.selector);
		}
		currentFile = null;	// not thread-safe
	} // clearFiles()

	public void addFile(String fileName, GUI.FileNameSelection selector) {
		AudioFile af = new AudioFile(fileName, selector);
		files.add(af);
		if (files.size() == 1) {
			setCurrentFile(selector);
			selector.setFraction(1.0);
		}
		else
			matcher.enqueue(af, files.get(0));	// align in background Thread
	} // addFile()

	class Matcher implements Runnable {
		
		LinkedList<AudioFile> reference;
		LinkedList<AudioFile> other;
		GUI gui;

		public Matcher(GUI g) {
			gui = g;
			reference = new LinkedList<AudioFile>();
			other = new LinkedList<AudioFile>();
		} // constructor

		public void enqueue(AudioFile af, AudioFile ref) {
			synchronized(this) {
				reference.add(ref);
				other.add(af);
				notify();
			}
		} // enqueue()

		public void stop() {
			synchronized(this) {
				reference.clear();
				other.clear();
				matchThread.interrupt();
			}
		} // stop()

		public void run() {
			AudioFile af, ref;
			while (true) {
				synchronized(this) {
					if (reference.size() == 0) {
						gui.setStatus(GUI.READY);
						try {
							wait();
						} catch (InterruptedException e) {
							continue;	// skip remove() since size is still 0
						}
						gui.setStatus(GUI.ALIGNING);
					}
					ref = reference.remove();
					af = other.remove();
				}
				try {
					match(af, ref);
				} catch (FileNotFoundException | URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // run()

		public void match(AudioFile af, AudioFile ref) throws FileNotFoundException, URISyntaxException {
			sm.init();
			pm1.setInputFile(ref);
			pm2.setInputFile(af);
			pm2.setProgressCallback(af.selector);
			PerformanceMatcher.doMatch(pm1, pm2, sm);
			if (Thread.interrupted()) {
				sm.setVisible(false);
				return;
			}
			if (sm.isVisible()) {
				sm.updateMatrix(playing);
			//	if (!playing)
			//		updateGUI();	// to set the time in the MatrixFrame
			} else
				sm.updatePaths(false);
		} // match()

	} // inner class Matcher

	public void setCurrentFile(int index) {
		if (index < files.size())
			setCurrentFile(files.get(index));
	} // setCurrentFile()

	public void setCurrentFile(GUI.FileNameSelection selector) {
		AudioFile newFile = null;
		for (ListIterator<AudioFile> i = files.listIterator(); i.hasNext(); ) {
			AudioFile f = i.next();
			if (f.selector == selector) {
				newFile = f;
				break;
			}
		}
		setCurrentFile(newFile);
	} // setCurrentFile()

	public void setCurrentFile(AudioFile newFile) {
		if (newFile == null)
			throw new RuntimeException("setCurrentFile(): null");
		if (newFile == currentFile)
			return;
		newFile.selector.setSelected(true);
		if (currentFile != null) {
			currentFile.selector.setSelected(false);
			// translate marks to new sampling rate and tempo
			for (ListIterator<Long> i = marks.listIterator(); i.hasNext(); ) {
				long mark = i.next().longValue();
				double time = currentFile.toReferenceTime(mark);
				mark = newFile.fromReferenceTime(time);
				i.set(new Long(mark));
			}
			// translate current playback position
			double time = currentFile.toReferenceTime(correctedPosition());
			long newCurrentPosition = newFile.fromReferenceTime(time);
			// rewind to previous mark if playing from mark
			if (playFromMark) {
				ListIterator<Long> i = marks.listIterator();
				long newPosn = 0;
				while (i.hasNext()) {
					long l = i.next().longValue();
					if (l >= newCurrentPosition)
						break;
					newPosn = l;
				}
				newCurrentPosition = newPosn;
			}
			// change file and position
			synchronized(this) {	// try to make it thread-safe
				if (playing) {
					requestedFile = newFile;
					requestedPosition = newCurrentPosition;
				} else {
					currentFile = newFile;
					setPosition(newCurrentPosition);
				}
			}
			gui.updateMarks();
		} else
			currentFile = newFile;
	} // setCurrentFile()

	public void previousFile() {
		if (files.size() > 1) {
			int index = files.indexOf(currentFile) - 1;
			if (index < 0)
				index = files.size() - 1;
			setCurrentFile(files.get(index));
		}
	} // previousFile()

	public void nextFile() {
		if (files.size() > 1) {
			int index = files.indexOf(currentFile) + 1;
			if (index == files.size())
				index = 0;
			setCurrentFile(files.get(index));
		}
	} // nextFile()

	/** Code for audio playback thread.  Implements Runnable interface. */
	public void run() {
		int bytesRead, bytesWritten;
		while (true) {
			try {
				if ((currentFile == null) || stopRequested || !playing) {
					synchronized(this) {
						playing = false;
						wait();
						playing = true;
						stopRequested = false;
					}
					if (currentFile == null)
						continue;
					if (currentPosition == currentFile.length)
						setPosition(0);
				}
				if (audioOut != null) {
					audioOut.stop();
					audioOut.flush();
				}
				if ((audioOut == null) ||
							!currentFile.format.matches(audioOut.getFormat())) {
					audioOut= AudioSystem.getSourceDataLine(currentFile.format);
					audioOut.open(currentFile.format, defaultOutputBufferSize);
					outputBufferSize = audioOut.getBufferSize();
				}
				audioOut.start();
				while (true) {			// PLAY loop
					synchronized(this) {
						if ((requestedPosition < 0) && !stopRequested)
							bytesRead = currentFile.audioIn.read(readBuffer);
						else if (stopRequested ||
									((requestedPosition >= 0) &&
										(requestedFile != null) &&
										!currentFile.format.matches(
											requestedFile.format))) {
							audioOut.stop();
							audioOut.flush();	// ?correct posn before flush?
							if (requestedPosition >= 0) {
								setPosition(requestedPosition);
								requestedPosition = -1;
							}
							break;
						} else {	// requestedPosition >= 0 && format matches
							bytesRead = currentFile.audioIn.read(readBuffer);
							setPosition(requestedPosition);
							requestedPosition = -1;
							if (bytesRead == readBuffer.length) {
								int read =currentFile.audioIn.read(readBuffer2);
								if (read == bytesRead) {	// linear crossfade
									int sample, sample2;
									for (int i = 0; i < read; i += 2) {
										if (currentFile.format.isBigEndian()) {
											sample = (readBuffer[i+1] & 0xff) |
													 (readBuffer[i] << 8);
											sample2= (readBuffer2[i+1] & 0xff) |
													 (readBuffer2[i] << 8);
											sample = ((read-i) * sample +
														i * sample2) / read;
											readBuffer[i] = (byte)(sample >> 8);
											readBuffer[i+1] = (byte)sample;
										} else {
											sample = (readBuffer[i] & 0xff) |
													 (readBuffer[i+1] << 8);
											sample2 = (readBuffer2[i] & 0xff) |
													 (readBuffer2[i+1] << 8);
											sample = ((read-i) * sample +
														i * sample2) / read;
											readBuffer[i+1] = (byte)(sample>>8);
											readBuffer[i] = (byte)sample;
										}
									}
								} else {
									bytesRead = read;
									for (int i = 0; i < read; i++)
										readBuffer[i] = readBuffer2[i];
								}
							} else
								bytesRead =currentFile.audioIn.read(readBuffer);
						}
					}
					bytesWritten = 0;
					if (bytesRead > 0)
						bytesWritten = audioOut.write(readBuffer, 0,bytesRead);
					if (bytesWritten > 0) {
						currentPosition += bytesWritten;
						updateGUI();
					}
					if (bytesWritten < readBufferSize) {
						if (currentPosition != currentFile.length)
							System.err.println("read error: unexpected EOF");
						stopRequested = true;
						break;
					}
				}
			} catch (InterruptedException e) {
				playing = false;
				e.printStackTrace();
			} catch (LineUnavailableException e) {
				playing = false;
				e.printStackTrace();
			} catch (java.io.IOException e) {
				playing = false;
				e.printStackTrace();
			}
		}
	} // run

	/** Implements ChangeListener interface */
	public void stateChanged(ChangeEvent e) {
		int value = gui.playSlider.getValue();
		if ((value == gui.oldSlider) || (currentFile == null))
			return;
		if (gui.playSlider.getValueIsAdjusting())
			stopRequested = true;
		else {
			long newPosn = currentFile.length * value / GUI.maxSlider;
			newPosn = newPosn / currentFile.frameSize * currentFile.frameSize;
			if (playing)
				requestedPosition = newPosn;
			else
				setPosition(newPosn);
		}
	} // stateChanged()
	
} // class AligningAudioPlayer
