package at.ofai.music.match;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import at.ofai.music.match.RandomAccessInputStream;

public class AudioFile {

	protected String path;
	protected GUI.FileNameSelection selector;
	private RandomAccessInputStream underlyingStream;
	protected AudioInputStream audioIn;
	protected AudioFormat format;
	protected long length;
	protected int frameSize;
	protected float frameRate;
	protected int[] thisIndex;
	protected double thisHopTime;
	protected int[] refIndex;
	protected double refHopTime;
	protected int pathLength;
	protected boolean isReference;
	protected boolean orientationX;
	protected FixedPoint fixedPoints;

	public AudioFile(String pathName, GUI.FileNameSelection jb) {
		this();
		path = pathName;
		selector = jb;
		try {
			underlyingStream = new RandomAccessInputStream(pathName);
    	    audioIn = AudioSystem.getAudioInputStream(underlyingStream);
			audioIn.mark(0);
			underlyingStream.mark();	// after the audio header
			format = audioIn.getFormat();
			frameSize = format.getFrameSize();
			frameRate = format.getFrameRate();
			length = audioIn.getFrameLength() * frameSize;
		} catch (java.io.IOException e) {	// includes FileNotFound
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	} // constructor

	public AudioFile() {	// used for worm alignment (no real file!)
		thisIndex = refIndex = null;
		thisHopTime = refHopTime = 0;
		isReference = true;		// until aligned
		orientationX = true;
		fixedPoints = null;
	} // default constructor

	public void setMatch(int[] idx1, double ht1, int[] idx2,double ht2,int ln) {
		thisIndex = idx1;
		thisHopTime = ht1;
		refIndex = idx2;
		refHopTime = ht2;
		pathLength = ln;
		isReference = false;	// alignment complete: ready for use
	} // setMatch()

	public void setFixedPoints(FixedPoint p, boolean isX) {
		fixedPoints = p;
		orientationX = isX;
	} // setFixedPoints()

	public void print() {
		for (int i = 0; i < pathLength; i++) {
			System.err.print(i + "  " + thisIndex[i]+"  "+refIndex[i]+"  :   ");
			if (i % 4 == 3)
				System.err.println();
		}
		System.err.println();
	} // print()

	/** Performs a binary search for a value in an array and returns its index.
	 *  If the value does not exist in the array, the index of the nearest
	 *  element is returned.  If the value occurs multiple times in the array,
	 *  the centre index is returned.  Note that we can't use
	 *  Arrays.binarySearch() because the array might not be full.
	 */
	public int search(int[] arr, int val) {
		int max = pathLength - 1;
		int min = 0;
		if ((max > min) && (arr[max] > arr[min])) {
			while (max > min) {
				int mid = (max + min) / 2;
				if (val > arr[mid])
					min = mid + 1;
				else
					max = mid;
			} // max = MIN_j (arr[j] >= val)   i.e. the first equal or next highest
		} else {	// elements in reverse order (bPath)
			while (max > min) {
				int mid = (max + min) / 2;
				if (val < arr[mid])
					min = mid + 1;
				else
					max = mid;
			} // max = MIN_j (arr[j] <= val)   i.e. the first equal or next lowest
		}
		while ((max + 1 < pathLength) && (arr[max + 1] == val))
			max++;
		return (min + max) / 2;
	} // search()

	public long fromReferenceTime(double time) {
		return (long) Math.round(fromReferenceTimeD(time)*frameRate)*frameSize;
	} // fromReferenceTime()

	public double fromReferenceTimeD(double time) {
		if (!isReference && (pathLength != 0)) {
			int refI = (int) Math.round(time / refHopTime);
			int index = search(refIndex, refI);
			time = thisIndex[index] * thisHopTime;
		}
		return time;
	} // fromReferenceTimeD()

	public double toReferenceTime(long ltime) {
		return toReferenceTimeD(ltime / frameSize / frameRate);
	} // toReferenceTime()

	public double toReferenceTimeD(double time) {
		if (!isReference && (pathLength != 0)) {
			int thisI = (int) Math.round(time / thisHopTime);
			int index = search(thisIndex, thisI);
			time = refIndex[index] * refHopTime;
		}
		return time;
	} // toReferenceTimeD()

	public long setPosition(long position) throws java.io.IOException {
		audioIn.reset();
		// must be multiple of frameSize
		return underlyingStream.seekFromMark(position / frameSize * frameSize);
	} // skip()

} // class AudioFile
