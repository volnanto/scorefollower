//package at.ofai.music.match;
//
//import java.util.ListIterator;
//import java.io.File;
//import java.io.BufferedReader;
//import java.io.PrintStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.FileNotFoundException;
//
//import at.ofai.music.worm.Worm;
//import at.ofai.music.worm.WormParameters;
//import at.ofai.music.worm.WormFile;
//import at.ofai.music.util.Event;
//import at.ofai.music.util.WormEvent;
//
//class WormHandler {
//
//	PerformanceMatcher parent, reference;
//	Worm w;
//	ScrollingMatrix sm;
//	double framePeriod;
//	int hopsPerFrame;
//	int hopCount, frameCount, prevFrameCount, nextIndex, nextRefIndex;
//	double energy;
//	double[] loudness, tempo, refEventTime, eventTime;
//	double refMaxTime, prevRefMaxTime, prevMaxTime, maxTime;
//	int[] refFlag, flag;
//	String[] strFlag, refStrFlag;
//	AudioFile audioFile;
//	WormParameters param;
//	double trackLevel;
//	static int recalc = 20;	// number of frames to recalculate
//	static final double frameTime = 0.1;	// 10 FPS (or nearest mult of hop)
//	static final int MAX_LENGTH = 36000;	// 1 hour
//
//	/** Assumes live input against a reference worm file */
//	public WormHandler(PerformanceMatcher pm) {
//		parent = pm;
//		reference = pm.otherMatcher;
//		w = Worm.createInFrame(new String[0]);
//		sm = null;
//		hopsPerFrame = (int) Math.round(frameTime / pm.hopTime);
//		framePeriod = hopsPerFrame * pm.hopTime;
//		w.setDelay(0);
//		w.setFramePeriod(framePeriod);
//		audioFile = new AudioFile();
//		loudness = new double[MAX_LENGTH];
//		tempo = new double[MAX_LENGTH];
//		flag = new int[MAX_LENGTH];
//		strFlag = new String[MAX_LENGTH];
//	} // constructor
//
//	public void init() {	// can't be run in constructor - null pointers
//		hopCount = 0;
//		frameCount = 0;
//		prevFrameCount = 0;
//		nextRefIndex = 0;
//		prevRefMaxTime = 0;
//		prevMaxTime = 0;
//		energy = 0;
//		param = new WormParameters(null);
//		try {
//			param.read(new BufferedReader(new FileReader(
//						new File(reference.matchFileName))));
//			trackLevel = param.getTrackLevel();
//		} catch (FileNotFoundException e) {
//			System.err.println("No reference file in WormHandler.init()");
//			trackLevel = 1;
//		} catch (IOException e) {
//			trackLevel = 1;
//		}
//		refEventTime = new double[reference.events.size()];
//		eventTime = new double[reference.events.size()];
//		refFlag = new int[reference.events.size()];
//		refStrFlag = new String[reference.events.size()];
//		ListIterator<Event> iterator = reference.events.listIterator();
//		int i = 0;
//		int bar = 0, beat = 0;
//		while (iterator.hasNext()) {
//			WormEvent e = (WormEvent) iterator.next();
//			if (e.flags == 0)
//				continue;
//			refEventTime[i] = e.keyDown;
//			if ((e.flags & WormFile.BAR) != 0)
//				bar++;
//			if ((e.flags & WormFile.BEAT) != 0)
//				beat++;
//			refStrFlag[i] = bar + ":" + beat + ":0:"; // ignore track level
//			refFlag[i++] = e.flags;
//		}
//	} // init()
//
//	public void setScrollingMatrix(ScrollingMatrix s) {
//		sm = s;
//	} // setScrollingMatrix()
//
//	public void addPoint(double hopEnergy) {
//		energy += hopEnergy;
//		if (++hopCount >= hopsPerFrame) {
//			loudness[frameCount] = 80 +10 * Math.log10(energy / hopsPerFrame);
//			if (loudness[frameCount] < 0)
//				loudness[frameCount] = 0;
//			frameCount++;
//			energy = 0;
//			hopCount = 0;
//		}
//	} // addPoint()
//
//	private int findFirstGE(double t) {
//		int i = nextRefIndex;
//		while ((i < eventTime.length) && (refEventTime[i] < t))
//			i++;
//		while ((i > 0) && (refEventTime[i-1] >= t))
//			i--;
//		return i;
//	} // findFirstGE()
//
//	public void update() {
//		if (prevFrameCount != frameCount) {
//			int recalcPoint = prevFrameCount - recalc;
//			if (recalcPoint < 0)
//				recalcPoint = 0;
//			prevMaxTime = recalcPoint * framePeriod;
//			prevRefMaxTime = audioFile.toReferenceTimeD(prevMaxTime);
//			if (parent == sm.pm1)
//				audioFile.setMatch(sm.sPathY, sm.hop1,
//									sm.sPathX, sm.hop2, sm.sPathLength);
//			else
//				audioFile.setMatch(sm.sPathX, sm.hop2,
//									sm.sPathY, sm.hop1, sm.sPathLength);
//			maxTime = frameCount * framePeriod;
//			refMaxTime = audioFile.toReferenceTimeD(maxTime);
//			double refPrevMaxTime = audioFile.toReferenceTimeD(prevMaxTime);
//			if (prevRefMaxTime < refPrevMaxTime) {	// insert missing events
//				int start = findFirstGE(prevRefMaxTime);
//				int stop = findFirstGE(refPrevMaxTime);
//				int dest = recalcPoint;
//				nextRefIndex = stop;
//				if (start < stop) {
//					System.out.println("Insert: " + start + " " + stop + " " +
//							prevRefMaxTime + " " + refPrevMaxTime + " " + dest);
//				}
//				while ((start < stop--) && (--dest >= 0)) {
//					if (flag[dest] != 0)
//						start--;
//					else {
//						flag[dest] = refFlag[stop];
//						strFlag[dest] = refStrFlag[stop];
//					}
//					System.out.println(start + " " + stop + " " + dest + " " +
//							flag[dest] + " " + strFlag[dest]);
//				}
//			} else if (prevRefMaxTime > refPrevMaxTime) {  // delete doubles
//				int start = findFirstGE(refPrevMaxTime);
//				int stop = findFirstGE(prevRefMaxTime);
//				int dest = recalcPoint;
//				nextRefIndex = start;
//				if (start < stop) {
//					System.out.println("Delete: " + start + " " + stop + " " +
//							refPrevMaxTime + " " + prevRefMaxTime + " " + dest);
//				}
//				while ((start < stop) && (--dest >= 0)) {
//					if (flag[dest] != 0) {
//						flag[dest] = 0;
//						stop--;
//					}
//				}
//				String current = "0:0:0:";
//				if (dest > 0)
//					current = strFlag[dest-1];
//				if (dest < 0)
//					dest = 0;
//				while (dest < recalcPoint)
//					strFlag[dest++] = current;
//			} else
//				nextRefIndex = findFirstGE(refMaxTime);
//			if (nextRefIndex < eventTime.length) {
//				eventTime[nextRefIndex] =
//					audioFile.fromReferenceTimeD(refEventTime[nextRefIndex]);
//				nextIndex =(int)Math.round(eventTime[nextRefIndex]/framePeriod);
//			} else
//				nextIndex = frameCount;
//			double currentTempo = 0;
//			String currentLabel = "0:0:0:";
//			if (recalcPoint != 0) {
//				currentTempo = tempo[recalcPoint-1];
//				currentLabel = strFlag[recalcPoint-1];
//			}
//			for (int i = recalcPoint; i < frameCount; i++) {
//				if (i == nextIndex) {
//					flag[i] = refFlag[nextRefIndex];
//					currentLabel = refStrFlag[nextRefIndex];
//					double tDiff = 0;
//					int count = 1;
//					while ((tDiff == 0) && (nextRefIndex >= count))
//						tDiff = eventTime[nextRefIndex] -
//									eventTime[nextRefIndex - count++];
//					if (count > 1)
//						tDiff /= count - 1;
//					if (tDiff == 0)
//						currentTempo = 0;
//					else
//						currentTempo = 60 * trackLevel / tDiff;
//					if (++nextRefIndex < eventTime.length) {
//						eventTime[nextRefIndex] = audioFile.fromReferenceTimeD(
//													refEventTime[nextRefIndex]);
//						nextIndex = (int) Math.round(eventTime[nextRefIndex] /
//																framePeriod);
//						if (i >= nextIndex)	// multiple flags on a frame
//							nextIndex = i+1;
//					} else
//						nextIndex = frameCount;
//				} else {
//					flag[i] = 0;
//				}
//				tempo[i] = currentTempo;
//				strFlag[i] = currentLabel;
//			}
//			System.out.println(recalcPoint + " " + frameCount + " " +
//								nextIndex + " " + nextRefIndex + " " +
//								maxTime + " " + refMaxTime + " " +
//								currentTempo + " " + currentLabel);
//			w.setPoints(tempo, loudness, strFlag, recalcPoint, frameCount);
//			prevFrameCount = frameCount;
//		}
//	} // update()
//
//	public void write(File f, boolean doEdit) {
//		update();
//		param.print();
//		param.editParameters(doEdit);
//		param.print();
//		try {
//			PrintStream out = new PrintStream(f);
//			param.write(out, frameCount, framePeriod);
//			for (int i = 0; i < frameCount; i++)
//				out.printf("%5.3f\t%5.3f\t%5.3f\t%1d\n",
//							i * framePeriod, tempo[i], loudness[i], flag[i]);
//			out.close();
//		} catch (IOException e) {
//			System.err.println("Unable to write worm file: " + e);
//		}
//	} // write()
//
//} // class WormHandler
