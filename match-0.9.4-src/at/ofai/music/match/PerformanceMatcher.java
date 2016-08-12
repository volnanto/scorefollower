package at.ofai.music.match;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import at.ofai.music.match.FFT;
import at.ofai.music.match.Format;
import at.ofai.music.match.Event;
import at.ofai.music.match.EventList;
import at.ofai.music.match.Profile;
import at.ofai.music.match.WormEvent;

/** Represents an audio stream that can be matched to another audio stream of
 *  the same piece of music.  The matching algorithm uses dynamic time warping.
 *  The distance metric is a Euclidean metric on the first difference of the
 *  magnitude spectrum with the lower frequencies on a linear scale and the
 *  higher frequencies mapped onto a logarithmic scale.
 */
public class PerformanceMatcher {

	/** Points to the other performance with which this one is being compared.
	 *  The data for the distance metric and the dynamic time warping is shared
	 *  between the two matchers. In the original version, only one of the two
	 *  performance matchers contained the distance metric. (See
	 *  <code>firstPM</code>) */
	protected PerformanceMatcher otherMatcher;

	/** Indicates which performance is considered primary (the score). This is
	 *  the performance shown on the vertical axis, and referred to as "this" in
	 *  the codes for the direction of DTW steps. */
	protected boolean firstPM;

	/** Input data for this performance (possibly in compressed format) */
	protected AudioInputStream rawInputStream;
	
	/** Uncompressed version of <code>rawInputStream</code>.
	 *  In the (normal) case where the input is already PCM data,
	 *  <code>rawInputStream == pcmInputStream</code> */
	protected AudioInputStream pcmInputStream;

	/** Line for audio output (only one PerformanceMatcher should set this!) */
	protected SourceDataLine audioOut;
	
	/** Flag to select audio output from this PerformanceMatcher */
	protected boolean audioOutputRequested;
	
	/** Format of the audio data in <code>pcmInputStream</code> */
	protected AudioFormat audioFormat;
	
	/** Number of channels of audio in <code>audioFormat</code> */
	protected int channels;

	/** Sample rate of audio in <code>audioFormat</code> */
	protected float sampleRate;
	
	/** Source of input data.
	 *  Later to be extended to include live input from the sound card. */
	protected String audioFileName;

	/** The AudioFile object from the AligningAudioPlayer, or null if the
	 *  command line version is being used.
	 */
	protected AudioFile audioFile;
	
	/** For assessing the matching algorithm, match files are used, which give
	 *  the times and velocities of all notes (as recorded by the Boesendorfer
	 *  SE290). */
	protected String matchFileName;
	protected String outputFileName;
	protected enum PathType {BACKWARD, FORWARD, SMOOTHED};
	protected PathType outputType;
	protected enum MetaType {NONE, MATCH, MIDI, WORM, LABEL};
	protected MetaType metadata;
	//protected boolean hasWormFile;
	//protected boolean hasMatchFile;
	//protected boolean hasLabelFile;
	protected EventList events;
	//protected WormHandler wormHandler;
	protected boolean liveWorm;
	protected double referenceFrequency;
	
	/** Onset time of the first note in the audio file, in order to establish
	 *  synchronisation between the match file and the audio data. */
	protected double matchFileOffset;

	/** Flag (command line options <b>-n1</b> and <b>-N1</b>) indicating whether
	 *  or not each frame of audio should be normalised to have a sum of 1.
	 *  (Default = true). */
	protected boolean normalise1;
	
	/** Flag (command line options <b>-n2</b> and <b>-N2</b>) indicating whether
	 *  or not the distance metric for pairs of audio frames should be
	 *  normalised by the sum of the two frames.
	 *  (Default = false). */
	protected boolean normalise2;

	/** Flag (command line options <b>-n3</b> and <b>-N3</b>) indicating whether
	 *  or not each frame of audio should be normalised by the long term average
	 *  of the summed energy.
	 *  (Default = false; assumes normalise1 == false). */
	protected boolean normalise3;
	
	/** Flag (command line options <b>-n4</b> and <b>-N4</b>) indicating whether
	 *  or not the distance metric for pairs of audio frames should be
	 *  normalised by the log of the sum of the frames.
	 *  (Default = true; assumes normalise2 == false). */
	protected boolean normalise4;

	/** Flag (command line options <b>-n5</b> and <b>-N5</b>) indicating whether
	 *  or not the distance metric for pairs of audio frames should be
	 *  set to zero between annotated positions.
	 *  (Default = false). */
	protected boolean normalise5;

	/** Flag (command line options <b>-d</b> and <b>-D</b>) indicating whether
	 *  or not the half-wave rectified spectral difference should be used in
	 *  calculating the distance metric for pairs of audio frames, instead of
	 *  the straight spectrum values. (Default = true). */
	protected boolean useSpectralDifference;

	protected boolean useChromaFrequencyMap;

	/** Scaling factor for distance metric; must guarantee that the final value
	 *  fits in the data type used, that is, (unsigned) byte. (Default = 16).
	 */
	protected double scale;

	/** Spacing of audio frames (determines the amount of overlap or skip
	 *  between frames). This value is expressed in seconds and can be set by
	 *  the command line option <b>-h hopTime</b>. (Default = 0.020s) */
	protected double hopTime;

	/** The size of an FFT frame in seconds, as set by the command line option
	 *  <b>-f FFTTime</b>. (Default = 0.04644s).  Note that the value is not
	 *  taken to be precise; it is adjusted so that <code>fftSize</code> is
	 *  always a power of 2. */
	protected double fftTime;

	/** The width of the search band (error margin) around the current match
	 *  position, measured in seconds. Strictly speaking the width is measured
	 *  backwards from the current point, since the algorithm has to work
	 *  causally.
	 */
	protected double blockTime;

	/** Spacing of audio frames in samples (see <code>hopTime</code>) */
	protected int hopSize;

	/** The size of an FFT frame in samples (see <code>fftTime</code>) */
	protected int fftSize;		// in samples

	/** Width of the search band in FFT frames (see <code>blockTime</code>) */
	protected int blockSize;	// in frames

	/** The number of frames of audio data which have been read. */
	protected int frameCount;

	/** RMS amplitude of the current frame. */
	protected double frameRMS;

	/** Long term average frame energy (in frequency domain representation). */
	protected double ltAverage;

	/** The number of frames sequentially processed by this matcher, without a
	 *  frame of the other matcher being processed.
	 */
	protected int runCount;

	/** Interactive control of the matching process allows pausing computation
	 *  of the cost matrices in one direction.
	 */
	protected boolean paused;

	/** The total number of frames of audio data to be read. */
	protected int maxFrames;

	/** Audio data is initially read in PCM format into this buffer. */
	protected byte[] inputBuffer;
	
	/** Audio data is scaled to the range [0,1] and averaged to one channel and
	 *  stored in a circular buffer for reuse (if hopTime &lt; fftTime). */
	protected double[] circBuffer;

	/** The index of the next position to write in the circular buffer. */
	protected int cbIndex;

	/** The window function for the STFT, currently a Hamming window. */
	protected double[] window;

	/** The real part of the data for the in-place FFT computation.
	 *  Since input data is real, this initially contains the input data. */
	protected double[] reBuffer;

	/** The imaginary part of the data for the in-place FFT computation.
	 *  Since input data is real, this initially contains zeros. */
	protected double[] imBuffer;

	/** A mapping function for mapping FFT bins to final frequency bins.
	 *  The mapping is linear (1-1) until the resolution reaches 2 points per
	 *  semitone, then logarithmic with a semitone resolution.  e.g. for
	 *  44.1kHz sampling rate and fftSize of 2048 (46ms), bin spacing is
	 *  21.5Hz, which is mapped linearly for bins 0-34 (0 to 732Hz), and
	 *  logarithmically for the remaining bins (midi notes 79 to 127, bins 35 to
	 *  83), where all energy above note 127 is mapped into the final bin. */
	protected int[] freqMap;

	/** The number of entries in <code>freqMap</code>. Note that the length of
	 *  the array is greater, because its size is not known at creation time. */
	protected int freqMapSize;

	/** The most recent frame; used for calculating the frame to frame
	 *  spectral difference. */
	protected double[] prevFrame;
	protected double[] newFrame;

	/** A block of previously seen frames are stored in this structure for
	 *  calculation of the distance matrix as the new frames are read in.
	 *  One can think of the structure of the array as a circular buffer of
	 *  vectors. The last element of each vector stores the total energy. */
	protected double[][] frames;

	/** The best path cost matrix. */
	protected int[][] bestPathCost;

	/** The distance matrix. */
	protected byte[][] distance;

	/** The bounds of each row of data in the distance and path cost matrices.*/
	protected int[] first, last;
	
	/** The frames to ignore because they are not annotated. */
	protected boolean[] ignore;

	/** GUI component which shows progress of alignment. */
	protected GUI.FileNameSelection progressCallback;

	/** Total number of audio frames, or -1 for live or compressed input. */
	protected long fileLength;

	/** Disable or enable debugging output */
	protected static boolean silent = true;

	public static boolean batchMode = false;
	public static boolean matrixVisible = false;
	public static boolean guiVisible = true;
	public static boolean stop = false;
	
	public static final int liveInputBufferSize = 32768; /* ~195ms buffer @CD */
	public static final int outputBufferSize = 32768;	 /* ~195ms buffer @CD */

	/** Encoding of minimum-cost steps performed in DTW algorithm. */
	protected static final int ADVANCE_THIS = 1;
	protected static final int ADVANCE_OTHER = 2;
	protected static final int ADVANCE_BOTH = ADVANCE_THIS | ADVANCE_OTHER;
	protected static final int MASK = 0xFC;
	protected static final double decay = 0.99;
	protected static final double silenceThreshold = 0.0004;
	protected static final int MAX_RUN_COUNT = 3;
	protected static final int MAX_LENGTH = 3600;	// seconds, i.e. 1 hour

	/** Constructor for PerformanceMatcher.
	 *  @param p The PerformanceMatcher representing the performance with which
	 *  this one is going to be matched.  Some information is shared between the
	 *  two matchers (currently one possesses the distance matrix and optimal
	 *  path matrix).
	 */
	public PerformanceMatcher(PerformanceMatcher p) {
		otherMatcher = p;	// the first matcher will need this to be set later
		firstPM = (p == null);
		matchFileOffset = 0;
		cbIndex = 0;
		frameRMS = 0;
		ltAverage = 0;
		frameCount = 0;
		runCount = 0;
		paused = false;
		hopSize = 0;
		fftSize = 0;
		blockSize = 0;
		hopTime = 0.020;	// DEFAULT, overridden with -h
		fftTime = 0.04644;	// DEFAULT, overridden with -f
		blockTime = 10.0;	// DEFAULT, overridden with -c
		normalise1 = true;
		normalise2 = false;
		normalise3 = false;
		normalise4 = true;
		normalise5 = false;
		useSpectralDifference = true;
		useChromaFrequencyMap = false;
		audioOutputRequested = false;
		scale = 90;
		maxFrames = 0;	// stop at EOF
		progressCallback = null;
		metadata = MetaType.NONE;
		//hasMatchFile = false;
		//hasWormFile = false;
		//hasLabelFile = false;
		liveWorm = false;
		matchFileName = null;
		outputFileName = null;
		outputType = PathType.BACKWARD;
		events = null;
		referenceFrequency = 440.0;
	} // default constructor

	/** For debugging, outputs information about the PerformanceMatcher to
	 *  standard error.
	 */
	public void print() {
		System.err.println(this);
	} // print()

	/** Gives some basic `header' information about the PerformanceMatcher. */
	public String toString() {
		return "PerformanceMatcher\n\tAudio file: " + audioFileName +
				" (" + Format.d(sampleRate/1000,1).substring(1) + "kHz, " +
				channels + " channels)" +
				"\n\tHop size: " + hopSize +
				"\n\tFFT size: " + fftSize +
				"\n\tBlock size: " + blockSize;
	} // toString()

	/** Adds a link to the PerformanceMatcher object representing the
	 *  performance which is going to be matched to this one.
	 *  @param p the PerformanceMatcher representing the other performance 
	 */
	public void setOtherMatcher(PerformanceMatcher p) {
		otherMatcher = p;
	} // setOtherMatcher()

	/** Adds a link to the GUI component which shows the progress of matching.
	 *  @param c the PerformanceMatcher representing the other performance 
	 */
	public void setProgressCallback(GUI.FileNameSelection c) {
		progressCallback = c;
	} // setProgressCallback()

	/** Sets the match file for automatic evaluation of the PerformanceMatcher.
	 *  @param fileName The path name of the match file
	 *  @param tStart The offset of the audio recording, that is, the time of
	 *  the first note onset relative to the beginning of the audio file. This
	 *  is required for precise synchronisation of the audio and match files.
	 *  @param isWorm Indicates whether the match file is in Worm file format.
	 */
	public void setMatchFile(String fileName, double tStart, boolean isWorm) {
		matchFileName = fileName;
		matchFileOffset = tStart;
		metadata = isWorm? MetaType.WORM: MetaType.MATCH;
		//hasWormFile = isWorm;
		//hasMatchFile = !isWorm;
		try {
			if (isWorm) {
				setInputFile(EventList.getAudioFileFromWormFile(matchFileName));
				events = EventList.readWormFile(matchFileName);
			/*	if (otherMatcher.wormHandler != null)
					otherMatcher.wormHandler.init(); */
			} else if (fileName.endsWith(".mid")) {
				events = EventList.readMidiFile(matchFileName);
				metadata = MetaType.MIDI;
				//hasMatchFile = false;
			} else
				events = EventList.readMatchFile(matchFileName);
		} catch (Exception e) {
			System.err.println("Error reading matchFile: " + fileName + "\n"+e);
			events = null;
		}
	} // setMatchFile()

	public void setMatchFile(String fileName, double tStart) {
		setMatchFile(fileName, tStart, false);
	} // setMatchFile()

	public void setLabelFile(String fileName) {
		//hasMatchFile = false;
		//hasWormFile = true;
		//hasLabelFile = true;
		matchFileName = fileName;
		metadata = MetaType.LABEL;
		try {
			events = EventList.readLabelFile(fileName);
		} catch (Exception e) {
			System.err.println("Error reading labelFile: " + fileName + "\n"+e);
			events = null;
		}
	} // setLabelFile()
	
	public void writeLabelFile(ScrollingMatrix sm) {
		EventList el = new EventList();
		AudioFile af = new AudioFile();
		if (MatrixFrame.useSmoothPath)
			af.setMatch(sm.sPathX, sm.hop2, sm.sPathY, sm.hop1, sm.sPathLength);
		else
			af.setMatch(sm.bPathX, sm.hop2, sm.bPathY, sm.hop1, sm.bPathLength);
		for (Event ev: otherMatcher.events.l) {
			WormEvent we = new WormEvent(af.fromReferenceTimeD(ev.keyDown),0,0,0,0);
			we.label = ((WormEvent)ev).label;
			el.add(we);
		}
		try {
			el.writeLabelFile(matchFileName);
		} catch (Exception e) {
			System.err.println("Unable to write output file: " + e);
		}
	} // writeLabelFile()
	
	public void writeMidiFile(ScrollingMatrix sm) {
		EventList el = new EventList();
		AudioFile af = new AudioFile();
		if (MatrixFrame.useSmoothPath)
			af.setMatch(sm.sPathX, sm.hop2, sm.sPathY, sm.hop1, sm.sPathLength);
		else
			af.setMatch(sm.bPathX, sm.hop2, sm.bPathY, sm.hop1, sm.bPathLength);
		for (Event ev: otherMatcher.events.l) {
			Event copy = ev.clone();
			copy.keyDown = af.fromReferenceTimeD(ev.keyDown);
			copy.keyUp = af.fromReferenceTimeD(ev.keyUp);
			el.add(copy);
		}
		try {
			el.writeMIDI(matchFileName);
		} catch (Exception e) {
			System.err.println("Unable to write output file: " + e);
		}
	} // writeMidiFile()

	/** Sets up the streams and buffers for live audio input (CD quality).
	 *  If any Exception is thrown within this method, it is caught, and any
	 *  opened streams are closed, and <code>pcmInputStream</code> is set to
	 *  <code>null</code>, indicating that the method did not complete
	 *  successfully.
	 */
	public void setLiveInput() {
		try {
			channels = 2;
			sampleRate = 44100;
			AudioFormat desiredFormat = new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
						channels, channels * 2, sampleRate, false);
			TargetDataLine tdl = AudioSystem.getTargetDataLine(desiredFormat);
			tdl.open(desiredFormat, liveInputBufferSize);
			pcmInputStream = new AudioInputStream(tdl);
			audioFormat = pcmInputStream.getFormat();
			init();
			tdl.start();
		} catch (Exception e) {
			e.printStackTrace();
			closeStreams();	// make sure it exits in a consistent state
		}
	} // setLiveInput()

	public void setInputFile(AudioFile f) {
		audioFile = f;
		setInputFile(f.path);
	} // setInputFile()

	/** Sets up the streams and buffers for audio file input.
	 *  If any Exception is thrown within this method, it is caught, and any
	 *  opened streams are closed, and <code>pcmInputStream</code> is set to
	 *  <code>null</code>, indicating that the method did not complete
	 *  successfully.
	 *  @param fileName The path name of the input audio file.
	 */
	public void setInputFile(String fileName) {
		closeStreams();		// release previously allocated resources
		audioFileName = fileName;
		try {
			if (audioFileName == null)
				throw new Exception("No input file specified");
			File audioFile = new File(audioFileName);
			if (!audioFile.isFile())
				throw new FileNotFoundException(
							"Requested file does not exist: " + audioFileName);
			rawInputStream = AudioSystem.getAudioInputStream(audioFile);
			audioFormat = rawInputStream.getFormat();
			channels = audioFormat.getChannels();
			sampleRate = audioFormat.getSampleRate();
			pcmInputStream = rawInputStream;
			if ((audioFormat.getEncoding()!=AudioFormat.Encoding.PCM_SIGNED) ||
					(audioFormat.getFrameSize() != channels * 2) ||
					audioFormat.isBigEndian()) {
				AudioFormat desiredFormat = new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
						channels, channels * 2, sampleRate, false);
				pcmInputStream = AudioSystem.getAudioInputStream(desiredFormat,
																rawInputStream);
				audioFormat = desiredFormat;
			}
			init();
		} catch (Exception e) {
			e.printStackTrace();
			closeStreams();	// make sure it exits in a consistent state
		}
	} // setInputFile()

	protected void init() {
		hopSize = (int) Math.round(sampleRate * hopTime);
		fftSize = (int) Math.round(Math.pow(2,
				Math.round( Math.log(fftTime * sampleRate) / Math.log(2))));
		blockSize = (int) Math.round(blockTime / hopTime);
		makeFreqMap(fftSize, sampleRate, referenceFrequency);
		int buffSize = hopSize * channels * 2;
		if ((inputBuffer == null) || (inputBuffer.length != buffSize))
			inputBuffer = new byte[buffSize];
		if ((circBuffer == null) || (circBuffer.length != fftSize)) {
			circBuffer = new double[fftSize];
			reBuffer = new double[fftSize];
			imBuffer = new double[fftSize];
			window = FFT.makeWindow(FFT.HAMMING, fftSize, fftSize);
			for (int i=0; i < fftSize; i++)
				window[i] *= Math.sqrt(fftSize);
		}
		if ((prevFrame == null) || (prevFrame.length != freqMapSize)) {
			prevFrame = new double[freqMapSize];
			newFrame = new double[freqMapSize];
			frames = new double[blockSize][freqMapSize+1];
		} else if (frames.length != blockSize)
			frames = new double[blockSize][freqMapSize+1];
		int len = (int) (MAX_LENGTH / hopTime);
		distance = new byte[len][];
		bestPathCost = new int[len][];
		first = new int[len];
		last = new int[len];
		if (normalise5 && (events != null)) { // skip frames without onsets
			ignore = new boolean[len];
			Arrays.fill(ignore, true);
			for (Event e: events.l)			//TODO: check if should be corrected
				ignore[(int)Math.round(e.keyDown / hopTime)] = false;
		}
		for (int i = 0; i < blockSize; i++) {
			distance[i] = new byte[(MAX_RUN_COUNT+1) * blockSize];
			bestPathCost[i] = new int[(MAX_RUN_COUNT+1) * blockSize];
		}
		frameCount = 0;
		runCount = 0;
		cbIndex = 0;
		frameRMS = 0;
		ltAverage = 0;
		paused = false;
		progressCallback = null;
		// hasMatchFile = false;	// For consistency, it would be good to
		// hasWormFile = false;		//  clear these, but they have to be set
		// matchFileName = null;	//  before init() is called, so we rely on
		// events = null;			//  the user to maintain consistency.
		if (pcmInputStream == rawInputStream)
			fileLength = pcmInputStream.getFrameLength() / hopSize;
		else
			fileLength = -1;
		if (!silent)
			print();
		try {
			if (audioOutputRequested) {
				audioOut = AudioSystem.getSourceDataLine(audioFormat);
				audioOut.open(audioFormat, outputBufferSize);
				audioOut.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			audioOut = null;
		}
	} // init()

	/** Closes the input stream(s) associated with this object. */
	public void closeStreams() {
		if (pcmInputStream != null) {
			try {
				pcmInputStream.close();
				if (pcmInputStream != rawInputStream)
					rawInputStream.close();
				if (audioOut != null) {
					audioOut.drain();
					audioOut.close();
				}
			} catch (Exception e) {}
			pcmInputStream = null;
			audioOut = null;
		}
	} // closeStreams()

	protected void makeFreqMap(int fftSize, float sampleRate, double refFreq) {
		freqMap = new int[fftSize/2+1];
		if (useChromaFrequencyMap)
			makeChromaFrequencyMap(fftSize, sampleRate, refFreq);
		else
			makeStandardFrequencyMap(fftSize, sampleRate, refFreq);
	} // makeFreqMap()

	/** Creates a map of FFT frequency bins to comparison bins.
	 *  Where the spacing of FFT bins is less than 0.5 semitones, the mapping is
	 *  one to one. Where the spacing is greater than 0.5 semitones, the FFT
	 *  energy is mapped into semitone-wide bins. No scaling is performed; that
	 *  is the energy is summed into the comparison bins. See also
	 *  processFrame()
	 */
	protected void makeStandardFrequencyMap(int fftSize, float sampleRate,
											double refFreq) {
		double binWidth = sampleRate / fftSize;
		int crossoverBin = (int)(2 / (Math.pow(2, 1/12.0) - 1));
		int crossoverMidi = (int)Math.round(Math.log(
					crossoverBin * binWidth / refFreq) / Math.log(2) * 12 + 69);
		// freq = refFreq * Math.pow(2, (midi-69)/12.0) / binWidth;
		int i = 0;
		while (i <= crossoverBin)
			freqMap[i++] = (int)Math.round(i*440/refFreq);
		while (i <= fftSize/2) {
			double midi = Math.log(i*binWidth/refFreq) / Math.log(2) * 12 + 69;
			if (midi > 127)
				midi = 127;
			freqMap[i++] = crossoverBin + (int)Math.round(midi) - crossoverMidi;
		}
		freqMapSize = freqMap[i-1] + 1;
		if (!silent) {
			System.err.println("Map size: " + freqMapSize +
							   ";  Crossover at: " + crossoverBin);
			int stopAt = Math.min(freqMap.length, 500);
			for (i = 0; i < stopAt; i++) // fftSize / 2; i++)
				System.err.println("freqMap[" + i + "] = " + freqMap[i]);
			System.err.println("Reference frequency: " + refFreq);
		}
	} // makeStandardFrequencyMap()

	// Test whether chroma is better or worse
	protected void makeChromaFrequencyMap(int fftSize,float sR,double refFreq) {
		double binWidth = sR / fftSize;
		int crossoverBin = (int)(1 / (Math.pow(2, 1/12.0) - 1));
	//	int crossoverMidi = (int)Math.round(Math.log(crossoverBin*binWidth/440)/
	//													Math.log(2) * 12 + 69);
		// freq = refFreq * Math.pow(2, (midi-69)/12.0) / binWidth;
		int i = 0;
		while (i <= crossoverBin)
			freqMap[i++] = 0;
		while (i <= fftSize/2) {
			double midi = Math.log(i*binWidth/refFreq) / Math.log(2) * 12 + 69;
			freqMap[i++] = ((int)Math.round(midi)) % 12 + 1;
		}
		freqMapSize = 13;
	} // makeChromaFrequencyMap()

	/** Reads a frame of input data, averages the channels to mono, scales
	 *  to a maximum possible absolute value of 1, and stores the audio data
	 *  in a circular input buffer. Assumes 16 bit PCM, any number of channels.
	 *  @return true if a frame (or part of a frame, if it is the final frame)
	 *  is read. If a complete frame cannot be read, the InputStream is set
	 *  to null.
	 */
	public boolean getFrame() {
		if (pcmInputStream == null)
			return false;
		try {
			int bytesRead = (int) pcmInputStream.read(inputBuffer);
			if ((audioOut != null) && (bytesRead > 0))
				if (audioOut.write(inputBuffer, 0, bytesRead) != bytesRead)
					System.err.println("Error writing to audio device");
			if (bytesRead < inputBuffer.length) {
				if (!silent)
					System.err.println("End of input: " + audioFileName);
				closeStreams();
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			closeStreams();
			return false;
		}
		frameRMS = 0;
		double sample;
		switch(channels) {
			case 1:
				for (int i = 0; i < inputBuffer.length; i += 2) {
					sample = ((inputBuffer[i+1]<<8) |
							  (inputBuffer[i]&0xff)) / 32768.0;
					frameRMS += sample * sample;
					circBuffer[cbIndex++] = sample;
					if (cbIndex == fftSize)
						cbIndex = 0;
				}
				break;
			case 2: // saves ~0.1% of RT (total input overhead ~0.4%) :)
				for (int i = 0; i < inputBuffer.length; i += 4) {
					sample = (((inputBuffer[i+1]<<8) | (inputBuffer[i]&0xff)) +
							  ((inputBuffer[i+3]<<8) | (inputBuffer[i+2]&0xff)))
								/ 65536.0;
					frameRMS += sample * sample;
					circBuffer[cbIndex++] = sample;
					if (cbIndex == fftSize)
						cbIndex = 0;
				}
				break;
			default:
				for (int i = 0; i < inputBuffer.length; ) {
					sample = 0;
					for (int j = 0; j < channels; j++, i+=2)
						sample += (inputBuffer[i+1]<<8) | (inputBuffer[i]&0xff);
					sample /= 32768.0 * channels;
					frameRMS += sample * sample;
					circBuffer[cbIndex++] = sample;
					if (cbIndex == fftSize)
						cbIndex = 0;
				}
		}
		frameRMS = Math.sqrt(frameRMS / inputBuffer.length);
		return true;
	} // getFrame()

//	Plot plot = null;
	double[] plotX, plotY;

	/** Processes a frame of audio data by first computing the STFT with a
	 *  Hamming window, then scaling the frequency axis with an arbitrary
	 *  mapping, then (optionally) computing the half-wave
	 *  rectified spectral difference from the previous frame, then (optionally)
	 *  normalising to a sum of 1, then calculating the distance to all frames
	 *  stored in the otherMatcher and storing them in the distance matrix, and
	 *  finally updating the optimal path matrix using the dynamic time warping
	 *  algorithm.
	 */
	protected void processFrame() {
		if (getFrame()) {
			for (int i = 0; i < fftSize; i++) {
				reBuffer[i] = window[i] * circBuffer[cbIndex];
				if (++cbIndex == fftSize)
					cbIndex = 0;
			}
			Arrays.fill(imBuffer, 0);
			FFT.fft(reBuffer, imBuffer, FFT.FORWARD);
			Arrays.fill(newFrame, 0);
			for (int i = 0; i <= fftSize/2; i++) {
				newFrame[freqMap[i]] += reBuffer[i] * reBuffer[i] +
										imBuffer[i] * imBuffer[i];
			}
			int frameIndex = frameCount % blockSize;
			if (firstPM && (frameCount >= blockSize)) {
				int len = last[frameCount - blockSize] -
							first[frameCount - blockSize];
				byte[] dOld = distance[frameCount - blockSize];
				byte[] dNew = new byte[len];
				int[] bpcOld = bestPathCost[frameCount - blockSize];
				int[] bpcNew = new int[len];
				for (int i = 0; i < len; i++) {
					dNew[i] = dOld[i];
					bpcNew[i] = bpcOld[i];
				}
				distance[frameCount] = dOld;
				distance[frameCount - blockSize] = dNew;
				bestPathCost[frameCount] = bpcOld;
				bestPathCost[frameCount - blockSize] = bpcNew;
			}
			double totalEnergy = 0;		// for normalisation
			if (useSpectralDifference) {
				for (int i = 0; i < freqMapSize; i++) {
					totalEnergy += newFrame[i];
					if (newFrame[i] > prevFrame[i]) {
						frames[frameIndex][i] = newFrame[i] - prevFrame[i];
					} else
						frames[frameIndex][i] = 0;
				}
			} else {
				for (int i = 0; i < freqMapSize; i++) {
					frames[frameIndex][i] = newFrame[i];
					totalEnergy += frames[frameIndex][i];
				}
			}
/*			if (plot != null) {
				if (plotX == null) {
					plotX = new double[freqMapSize];
					plotY = new double[freqMapSize];
					for (int j=0; j < freqMapSize; j++)
						plotX[j] = j;
					plot.addPlot(plotX, plotY);
				}
				for (int i = 0; i < freqMapSize; i++)
					plotY[i] = frames[frameIndex][i];
				plot.update();
			}
			frames[frameIndex][freqMapSize] = totalEnergy;
			if (wormHandler != null)
				wormHandler.addPoint(totalEnergy);  */
			double decay = frameCount >= 200? 0.99:
						(frameCount < 100? 0: (frameCount - 100) / 100.0);
			if (ltAverage == 0)
				ltAverage = totalEnergy;
			else
				ltAverage = ltAverage * decay + totalEnergy * (1.0 - decay);
			if (totalEnergy <= silenceThreshold)		// was frameRMS
				for (int i = 0; i < freqMapSize; i++)
					frames[frameIndex][i] = 0;
			else if (normalise1)
				for (int i = 0; i < freqMapSize; i++)
					frames[frameIndex][i] /= totalEnergy;
			else if (normalise3)
				for (int i = 0; i < freqMapSize; i++)
					frames[frameIndex][i] /= ltAverage;
			int stop = otherMatcher.frameCount;
			int index = stop - blockSize;
			if (index < 0)
				index = 0;
			first[frameCount] = index;
			last[frameCount] = stop;
			boolean overflow = false;
			int mn=-1;
			int mx=-1;
			for ( ; index < stop; index++) {
//				tmp = stop - index;
				int dMN = calcDistance(frames[frameIndex],
										otherMatcher.frames[index % blockSize]);
				if (((ignore != null) && ignore[frameCount]) ||
						((otherMatcher.ignore != null) && otherMatcher.ignore[index]))
					dMN /= 4;
				if (mx < 0)
					mx = mn = dMN;
				else if (dMN > mx)
					mx = dMN;
				else if (dMN < mn)
					mn = dMN;
				if (dMN >= 255) {
					overflow = true;
					dMN = 255;
				}
				if ((frameCount == 0) && (index == 0))	// first element
					setValue(0, 0, 0, 0, dMN);
				else if (frameCount == 0)				// first row
					setValue(0, index, ADVANCE_OTHER,
									getValue(0, index-1, true), dMN);
				else if (index == 0)					// first column
					setValue(frameCount, index, ADVANCE_THIS,
									getValue(frameCount - 1, 0, true), dMN);
				else if (index == otherMatcher.frameCount - blockSize) {
					// missing value(s) due to cutoff
					//  - no previous value in current row (resp. column)
					//  - no diagonal value if prev. dir. == curr. dirn
					int min2 = getValue(frameCount - 1, index, true);
				//	if ((firstPM && (first[frameCount - 1] == index)) ||
				//			(!firstPM && (last[index-1] < frameCount)))
					if (first[frameCount - 1] == index)
						setValue(frameCount, index, ADVANCE_THIS, min2, dMN);
					else {
						int min1 = getValue(frameCount - 1, index - 1, true);
						if (min1 + dMN <= min2)
							setValue(frameCount, index, ADVANCE_BOTH, min1,dMN);
						else
							setValue(frameCount, index, ADVANCE_THIS, min2,dMN);
					}
				} else {
					int min1 = getValue(frameCount, index-1, true);
					int min2 = getValue(frameCount - 1, index, true);
					int min3 = getValue(frameCount - 1, index-1, true);
					if (min1 <= min2) {
						if (min3 + dMN <= min1)
							setValue(frameCount, index, ADVANCE_BOTH, min3,dMN);
						else
							setValue(frameCount, index, ADVANCE_OTHER,min1,dMN);
					} else {
						if (min3 + dMN <= min2)
							setValue(frameCount, index, ADVANCE_BOTH, min3,dMN);
						else
							setValue(frameCount, index, ADVANCE_THIS, min2,dMN);
					}
				}
				otherMatcher.last[index]++;
			} // loop for row (resp. column)
			double[] tmp = prevFrame;
			prevFrame = newFrame;
			newFrame = tmp;
			frameCount++;
			runCount++;
			otherMatcher.runCount = 0;
			if (overflow && !silent)
				System.err.println("WARNING: overflow in distance metric: " +
									"frame " + frameCount + ", val = " + mx);
		//	if (debug)
		//		System.err.println("Frame " + frameCount + ", d = " + (mx-mn));
			if ((frameCount % 100) == 0) {
				if (!silent) {
					System.err.println("Progress:" + frameCount + " " +
										Format.d(ltAverage, 3));
					Profile.report();
				}
				if ((progressCallback != null) && (fileLength > 0))
					progressCallback.setFraction((double)frameCount/fileLength);
			}
			if (frameCount == maxFrames)
				closeStreams();
		}
	} // processFrame()

	/** Calculates the Manhattan distance between two vectors, with an optional
	 *  normalisation by the combined values in the vectors. Since the
	 *  vectors contain energy, this could be considered as a squared Euclidean
	 *  distance metric. Note that normalisation assumes the values are all
	 *  non-negative.
	 *  @param f1 one of the vectors involved in the distance calculation
	 *  @param f2 one of the vectors involved in the distance calculation
	 *  @return the distance, scaled and truncated to an integer
	 */
	protected int calcDistance(double[] f1, double[] f2) {
		double d = 0;
		double sum = 0;
		for (int i=0; i < freqMapSize; i++) {
			d += Math.abs(f1[i] - f2[i]);
			sum += f1[i] + f2[i];
		}
		if (sum == 0)
			return 0;
		if (normalise2)
			return (int)(scale * d / sum);	// 0 <= d/sum <= 2
		if (!normalise4)
			return (int)(scale * d);
		double weight = (8 + Math.log(sum)) / 10.0;
		if (weight < 0)
			weight = 0;
		else if (weight > 1)
			weight = 1;
//		if ((frameCount > 1000) && (tmp < 50) && (d/sum < 0.8)) {
//			double[] x = new double[f1.length];
//			for (int i=0; i < x.length; i++)
//				x[i] = i;
//			Plot p = new Plot();
//			p.addPlot(x, f1, Color.blue);
//			p.setLength(0, freqMapSize-2);
//			p.addPlot(x, f2, Color.red);
//			p.setLength(1, freqMapSize-2);
//			p.fitAxes();
//			p.setTitle(String.format("%5d %5d  %5.3f %5.3f %5.3f  %5.3f  %5.3f\n",
//					frameCount, tmp, d, sum, weight, d/sum, d/sum*weight));
//			try{System.in.read();}catch(Exception e){}
//			p.close();
//		}
		return (int)(scale * d / sum * weight);
	} // calcDistance()

	/** Retrieves values from the minimum cost matrix.
	 *  @param i the frame number of this PerformanceMatcher
	 *  @param j the frame number of the other PerformanceMatcher
	 *  @return the cost of the minimum cost path to this location
	 */
	protected int getValue(int i, int j, boolean firstAttempt) {
		if (firstPM)
			return bestPathCost[i][j - first[i]];
		else
			return otherMatcher.bestPathCost[j][i - otherMatcher.first[j]];
	} // getValue()

	/** Stores entries in the distance matrix and the optimal path matrix.
	 *  @param i the frame number of this PerformanceMatcher
	 *  @param j the frame number of the other PerformanceMatcher
	 *  @param dir the direction from which this position is reached with
	 *  minimum cost
	 *  @param value the cost of the minimum path except the current step
	 *  @param dMN the distance cost between the two frames
	 */
	protected void setValue(int i, int j, int dir, int value, int dMN) {
		if (firstPM) {
			distance[i][j - first[i]] = (byte)((dMN & MASK) | dir);
			bestPathCost[i][j - first[i]] =
									(value + (dir==ADVANCE_BOTH? dMN*2: dMN));
		} else {
			if (dir == ADVANCE_THIS)
				dir = ADVANCE_OTHER;
			else if (dir == ADVANCE_OTHER)
				dir = ADVANCE_THIS;
			int idx = i - otherMatcher.first[j];
			if (idx == otherMatcher.distance[j].length) {
				//	This should never happen, but if we allow arbitrary pauses
				//	in either direction, and arbitrary lengths at end, it is
				//	better than an IndexOutOfBoundsException
				int[] tmp1 = new int[idx*2];
				byte[] tmp2 = new byte[idx*2];
				for (int k = 0; k < idx; k++) {
					tmp1[k] = otherMatcher.bestPathCost[j][k];
					tmp2[k] = otherMatcher.distance[j][k];
				}
				otherMatcher.bestPathCost[j] = tmp1;
				otherMatcher.distance[j] = tmp2;
			}
			otherMatcher.distance[j][idx] = (byte)((dMN & MASK) | dir);
			otherMatcher.bestPathCost[j][idx] =
								(value + (dir==ADVANCE_BOTH? dMN*2: dMN));
		}
	} // setValue()

	/** Inner class for representing the corresponding note onset times in two
	 *  audio files. Each Onset object represents a score position, with a
	 *  beat number (numbered from 0 at the beginning of the first full bar),
	 *  and the time (in seconds) of the average onset time of the notes at this
	 *  score position in the respective audio files.
	 */
	class Onset {
		double beat, time1, time2;
		/** Constructor for Onset.
		 *  @param beat the score position in beats, numbered from 0 at the
		 *  beginning of the first complete bar
		 *  @param time1 the average onset time of notes at score position b in
		 *  the audio file corresponding to PerformanceMatcher pm1
		 *  @param time2 the average onset time of notes at score position b in
		 *  the audio file corresponding to PerformanceMatcher pm2
		 */
		public Onset(double beat, double time1, double time2) {
			this.beat = beat;
			this.time1 = time1;
			this.time2 = time2;
		}
	} // class Onset

	/** Matches two match files, creating a composite list of corresponding
	 *  onset times, consisting of the mean onset times of the notes in each
	 *  notated score position. This is the basis for evaluating the alignment
	 *  of the two performances. The asynchrony of notationally simultaneous
	 *  notes limits the accuracy achievable by the PerformanceMatcher.
	 */
	public LinkedList<Onset> evaluateMatch(PerformanceMatcher pm) {
		if ((events == null) || (pm.events == null))
			return null;
		Event current;
		double prevBeat = Double.NaN;
		int count = 0;
		double sum = 0;
		double correction = 0;
		LinkedList<Onset> l = new LinkedList<Onset>();
		for (Iterator<Event> i = events.iterator(); i.hasNext(); ) {
			current = i.next();
			if (count == 0) {
				sum = current.keyDown;	// will average "simultaneous" onsets
				//if (hasMatchFile)
				if ((metadata == MetaType.MATCH) || (metadata == MetaType.MIDI))
					correction = matchFileOffset - current.keyDown;
				count = 1;
				prevBeat = current.scoreBeat;
			} else if (current.scoreBeat == prevBeat) {
				sum += current.keyDown;
				count++;
			} else {
				l.add(new Onset(prevBeat, sum/count + correction, -1));
				sum = current.keyDown;
				count = 1;
				prevBeat = current.scoreBeat;
			}
		}
		if (count != 0)
			l.add(new Onset(prevBeat, sum/count + correction, -1));
		if (l.size() == 0)
			return null;
		count = 0;
		sum = 0;
		correction = 0;
		ListIterator<Onset> li = l.listIterator();
		Onset onset = li.next();
		prevBeat = Double.NaN;
		for (Iterator<Event> i = pm.events.iterator(); i.hasNext(); ) {
			current = i.next();
			if (count == 0) {
				sum = current.keyDown;
				//if (pm.hasMatchFile)
				if ((pm.metadata == MetaType.MATCH) ||
						(pm.metadata == MetaType.MIDI))
					correction = pm.matchFileOffset - current.keyDown;
				count = 1;
				prevBeat = current.scoreBeat;
			} else if (current.scoreBeat == prevBeat) {
				sum += current.keyDown;
				count++;
			} else {	// insert in list sorted (keyed) by beat number
				while ((onset.beat < prevBeat) && li.hasNext())
					onset = li.next();
				while ((onset.beat > prevBeat) && li.hasPrevious())
					onset = li.previous();
				if (onset.beat == prevBeat) {
					onset.time2 = sum/count + correction;
				} else {
					li.add(new Onset(prevBeat, -1, sum/count + correction));
				}
				sum = current.keyDown;
				count = 1;
				prevBeat = current.scoreBeat;
			}
		}
		if (count != 0) {	// insert last event
			while ((onset.beat < prevBeat) && li.hasNext())
				onset = li.next();
			while ((onset.beat > prevBeat) && li.hasPrevious())
				onset = li.previous();
			if (onset.beat == prevBeat) {
				onset.time2 = sum/count + correction;
			} else {
				li.add(new Onset(prevBeat, -1, sum/count + correction));
			}
		}
		li = l.listIterator();
		while (li.hasNext()) {
			onset = li.next();
			String s = String.format("%8.3f %8.3f %8.3f",
									onset.beat, onset.time1, onset.time2);
			if ((onset.time1 < 0) || (onset.time2 < 0) || (onset.beat < 0)) {
				System.err.println("Match Error: " + s);
				li.remove();	// notes must exist in both performances
			}
		}
		return l;
	} // evaluateMatch()

	/** Tracks a performance by choosing a likely band for the optimal path. 
	 * @throws FileNotFoundException 
	 * @throws URISyntaxException */
	public static void doMatch(PerformanceMatcher pm1, PerformanceMatcher pm2,
							   ScrollingMatrix s) throws FileNotFoundException, URISyntaxException {
		Finder f = new Finder(pm1, pm2);
		while ((pm1.pcmInputStream != null) || (pm2.pcmInputStream != null)) {
		//	Profile.start(0);
			if (pm1.frameCount < pm1.blockSize) {		// fill initial block
				pm1.processFrame();
				pm2.processFrame();
			} else if (pm1.pcmInputStream == null) { 	// stream 1 at end
			//	int index = pm1.first[pm1.frameCount - pm1.blockSize];
			//	if (pm2.frameCount < index + (MAX_RUN_COUNT+1) * pm2.blockSize)
				pm2.processFrame();	// see setValue() for alternative fix
			//	else {
			//		if (!silent)
			//			System.err.println("Closing streams early");
			//		pm2.closeStreams();
			//	}
			} else if (pm2.pcmInputStream == null)		// stream 2 at end
				pm1.processFrame();
			else if (pm1.paused) {
				if (pm2.paused)
					try {
						if (stop)
							break;
						Thread.sleep(100);
						continue;	// no update
					} catch (InterruptedException e) {}
				else
					pm2.processFrame();
			} else if (pm2.paused)
				pm1.processFrame();
			else if (pm1.runCount >= MAX_RUN_COUNT)		// slope constraints
				pm2.processFrame();
			else if (pm2.runCount >= MAX_RUN_COUNT)
				pm1.processFrame();
			else
				switch(f.getExpandDirection(pm1.frameCount-1,pm2.frameCount-1)){
					case ADVANCE_THIS:
						pm1.processFrame();
						break;
					case ADVANCE_OTHER:
						pm2.processFrame();
						break;
					case ADVANCE_BOTH:
						pm1.processFrame();
						pm2.processFrame();
						break;
				}
		//	Profile.log(0);
			if (Thread.currentThread().isInterrupted()) {
				System.err.println("info: INTERRUPTED in doMatch()");
				return;
			}
			if (!batchMode)
				s.updateMatrix(true);
		}
		if (pm2.progressCallback != null)
			pm2.progressCallback.setFraction(1.0);
		if (!batchMode) {
			s.updatePaths(false);	// calculate complete paths
			s.repaint();
		}
	} // doMatch()

	

	/** Processes command line arguments.
	 *  @param pm1 The first PerformanceMatcher
	 *  @param pm2 The second PerformanceMatcher
	 *  @param args Command line arguments<br>
	 *  Usage: java PerformanceMatcher [optional-args] inputFile1 inputFile2<br>
	 *  Optional args are:<ul>
	 *  <li><b>-h hopTime</b> spacing of audio frames (in seconds, default 0.01)
	 *  <li><b>-f frameTime</b> size of FFT (in seconds, default 0.01161)
	 *  <li><b>-x maxFrames</b> stop after maxFrames frames have been processed
	 *  <li><b>-m1 matchFile1 offset1</b> matchFile + start time for inputFile1
	 *  <li><b>-m2 matchFile2 offset2</b> matchFile + start time for inputFile2
	 *  <li><b>-w1 wormFile1</b> wormFile for inputFile1
	 *  <li><b>-w2 wormFile2</b> wormFile for inputFile2
	 *  <li><b>-[nN]<i>k</i></b> Various options for normalisation, where
	 *    <b>n</b> switches on, <b>N</b> switches off each option.
	 *    <b><i>k</i></b> can have the following values:<ul>
	 *    <li>1: normalise each FFT frame (sum of energy = 1) before comparison (default=true)
	 *    <li>2: normalise distance metric by sum of energies of both frames (default=false)
	 *    <li>3: normalise each FFT frame by the medium-term average energy (default=false)
	 *    <li>4: normalise distance metric by thresholded log of sum of frames (default=true)
	 *    <li>5: set distance to zero for non-annotated positions in either file (default=false)
	 *    </ul>
	 *  <li><b>-d</b> use half-wave rectified spectral difference (default)
	 *  <li><b>-D</b> do not use half-wave rectified spectral difference
	 *  <li><b>-s scale</b> set scaling factor for distance metric
	 *  <li><b>-b</b> set batch mode
	 *  <li><b>-B</b> unset batch mode (default)
	 *  <li><b>-smooth length</b> set smoothing window size to length
	 *  <li><b>-a</b> audio out (file 1)
	 *  <li><b>-A</b> audio out (file 2)
	 *  <li><b>-l</b> live input (file 1)
	 *  <li><b>-L</b> live input (file 2)
	 *  <li><b>-w</b> live worm output (file 1)
	 *  <li><b>-W</b> live worm output (file 2)
	 *  <li><b>-z fileName</b> worm output (map file 1 to 2)
	 *  <li><b>-Z fileName</b> worm output (map file 2 to 1)
	 *  <li><b>-ob fileName</b> output backward match path
	 *  <li><b>-of fileName</b> output forward match path
	 *  <li><b>-os fileName</b> output smoothed match path
	 *  <li><b>-rf1 value</b> set A4 reference frequency (default 440) for pm1
	 *  <li><b>-rf2 value</b> set A4 reference frequency for pm2
	 *  </ul>
	 */
	public static int processArgs(PerformanceMatcher pm1,
									PerformanceMatcher pm2, String[] args) {
		for (int i=0; i<args.length; i++) {
			if (!silent)
				System.err.println("args["+i+"] = "+args[i]);
			if (args[i].equals("-h")) {
				try {
					pm1.hopTime = pm2.hopTime = Double.parseDouble(args[++i]);
				} catch (RuntimeException e) { // NumberFormat/ArrayOutOfBounds
					System.err.println(e);
				}
			} else if (args[i].equals("-f")) {
				try {
					pm1.fftTime = pm2.fftTime = Double.parseDouble(args[++i]);
				} catch (RuntimeException e) {
					System.err.println(e);
				}
			} else if (args[i].equals("-c")) {
				try {
					pm1.blockTime = pm2.blockTime=Double.parseDouble(args[++i]);
				} catch (RuntimeException e) {
					System.err.println(e);
				}
			} else if (args[i].equals("-s")) {
				try {
					pm1.scale = pm2.scale = Double.parseDouble(args[++i]);
				} catch (RuntimeException e) {
					System.err.println(e);
				}
			} else if (args[i].equals("-x")) {
				try {
					pm1.maxFrames = pm2.maxFrames = Integer.parseInt(args[++i]);
				} catch (RuntimeException e) {
					System.err.println(e);
				}
			} else if (args[i].equals("-m1")) {
				try {
					pm1.setMatchFile(args[++i], Double.parseDouble(args[++i]));
				} catch (RuntimeException e) {
					System.err.println(e);
				}
			} else if (args[i].equals("-m2")) {
				try {
					pm2.setMatchFile(args[++i], Double.parseDouble(args[++i]));
				} catch (RuntimeException e) {
					System.err.println(e);
				}
			} else if (args[i].equals("-w1")) {
				pm1.setMatchFile(args[++i], 0, true);
			} else if (args[i].equals("-w2")) {
				pm2.setMatchFile(args[++i], 0, true);
			} else if (args[i].equals("-M1")) {
				pm1.setLabelFile(args[++i]);
				MatrixFrame.useSmoothPath = false;
			} else if (args[i].equals("-M2")) {
				pm2.setLabelFile(args[++i]);
				MatrixFrame.useSmoothPath = false;
			} else if (args[i].equals("-d")) {
				pm1.useSpectralDifference = pm2.useSpectralDifference = true;
			} else if (args[i].equals("-D")) {
				pm1.useSpectralDifference = pm2.useSpectralDifference = false;
			} else if (args[i].equals("-n1")) {
				pm1.normalise1 = pm2.normalise1 = true;
			} else if (args[i].equals("-N1")) {
				pm1.normalise1 = pm2.normalise1 = false;
			} else if (args[i].equals("-n2")) {
				pm1.normalise2 = pm2.normalise2 = true;
			} else if (args[i].equals("-N2")) {
				pm1.normalise2 = pm2.normalise2 = false;
			} else if (args[i].equals("-n3")) {
				pm1.normalise3 = pm2.normalise3 = true;
			} else if (args[i].equals("-N3")) {
				pm1.normalise3 = pm2.normalise3 = false;
			} else if (args[i].equals("-n4")) {
				pm1.normalise4 = pm2.normalise4 = true;
			} else if (args[i].equals("-N4")) {
				pm1.normalise4 = pm2.normalise4 = false;
			} else if (args[i].equals("-k1")) {
				pm1.normalise5 = true;
				pm2.normalise5 = false;
			} else if (args[i].equals("-k2")) {
				pm1.normalise5 = false;
				pm2.normalise5 = true;
		/*	} else if (args[i].equals("--plot1")) {
				pm1.plot = new Plot();
			} else if (args[i].equals("--plot2")) {
				pm2.plot = new Plot();
		*/	} else if (args[i].equals("--use-chroma-map")) {
				pm1.useChromaFrequencyMap = pm2.useChromaFrequencyMap = true;
			} else if (args[i].equals("-b")) {
				batchMode = true;
				matrixVisible = false;
				guiVisible = false;
			} else if (args[i].equals("-B")) {
				batchMode = false;
			} else if (args[i].equals("-v")) {
				matrixVisible = true;
				batchMode = false;
			} else if (args[i].equals("-V")) {
				matrixVisible = false;
			} else if (args[i].equals("-g")) {
				guiVisible = true;
				batchMode = false;
			} else if (args[i].equals("-G")) {
				guiVisible = false;
			} else if (args[i].equals("-q")) {
				silent = true;
			} else if (args[i].equals("-Q")) {
				silent = false;
			} else if (args[i].equals("-a")) {
				pm1.audioOutputRequested = true;
				pm2.audioOutputRequested = false;
			} else if (args[i].equals("-A")) {
				pm2.audioOutputRequested = true;
				pm1.audioOutputRequested = false;
			} else if (args[i].equals("-r")) {
				guiVisible = true;
				batchMode = false;
				GUI.loadFile = args[++i];
			} else if (args[i].equals("-l")) {
				pm1.audioOutputRequested = true;
				pm2.audioOutputRequested = false;
				pm1.setLiveInput();
			} else if (args[i].equals("-L")) {
				pm2.audioOutputRequested = true;
				pm1.audioOutputRequested = false;
				pm2.setLiveInput();

				
//			} else if (args[i].equals("-l")) {
//				pm1.audioOutputRequested = false;
//				pm2.audioOutputRequested = true;
//				pm1.setLiveInput();
//				pm2.setInputFile("C:/Users/Bairong/Desktop/MACBOOKAIR_JW/Mozart_b.wav");
//			} else if (args[i].equals("-L")) {
//				pm2.audioOutputRequested = false;
//				pm1.audioOutputRequested = true;
//				pm2.setLiveInput();
//				pm1.setInputFile("C:/Users/Bairong/Desktop/MACBOOKAIR_JW/Bosi_clarinet.wav");

				
/*			} else if (args[i].equals("-w")) {
				pm1.wormHandler = new WormHandler(pm1);
				pm1.liveWorm = true;
			} else if (args[i].equals("-W")) {
				pm2.wormHandler = new WormHandler(pm2);
				pm2.liveWorm = true;
			} else if (args[i].equals("-z")) {
				//batchMode = true;			//TODO check if scripts are broken
				//if (!pm2.hasLabelFile)	//TODO why !hLabel instead of hWorm
				if (pm2.metadata == MetaType.WORM)
					pm1.wormHandler = new WormHandler(pm1);
				pm1.matchFileName = args[++i];
			} else if (args[i].equals("-Z")) {
				//batchMode = true;			//TODO check if scripts are broken
				//if (!pm1.hasLabelFile)
				if (pm1.metadata == MetaType.WORM)
					pm2.wormHandler = new WormHandler(pm2);
				pm2.matchFileName = args[++i];
*/			} else if (args[i].equals("-ob")) {
				pm1.outputFileName = args[++i];
				pm1.outputType = PathType.BACKWARD;
			} else if (args[i].equals("-of")) {
				pm1.outputFileName = args[++i];
				pm1.outputType = PathType.FORWARD;
			} else if (args[i].equals("-os")) {
				pm1.outputFileName = args[++i];
				pm1.outputType = PathType.SMOOTHED;
			} else if (args[i].equals("-rf1")) {
				pm1.referenceFrequency = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-rf2")) {
				pm2.referenceFrequency = Double.parseDouble(args[++i]);
			} else
				return i;
			//	System.err.println("WARNING: Ignoring argument: " + args[i]);
		}
		return args.length;
	} // processArgs()

	/** Entry point for command line version of performance matcher.
	 * @throws FileNotFoundException 
	 * @throws URISyntaxException 
	 *  @see #processArgs(PerformanceMatcher,PerformanceMatcher,String[])
	 *  processArgs(PerformanceMatcher,PerformanceMatcher,String[]) for
	 *  documentation of command-line arguments.
	 */
	public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
		PerformanceMatcher pm1 = new PerformanceMatcher(null);
		PerformanceMatcher pm2 = new PerformanceMatcher(pm1);
		pm1.setOtherMatcher(pm2);
		int argc = processArgs(pm1, pm2, args);
		ScrollingMatrix s = new ScrollingMatrix(pm1, pm2); // pm1 vertical
		MatrixFrame m = new MatrixFrame(s, matrixVisible);
		GUI g = new GUI(pm1, pm2, s, guiVisible);
		if (guiVisible && (argc != args.length))
			g.addFiles(args, argc);
		else {
			if ((argc < args.length) && (pm1.pcmInputStream == null))
				pm1.setInputFile(args[argc++]);
			if ((argc < args.length) && (pm2.pcmInputStream == null))
				pm2.setInputFile(args[argc++]);
			if (pm2.pcmInputStream != null) {
				doMatch(pm1, pm2, s);
				s.updatePaths(false);
				//if ((pm1.hasMatchFile && pm2.hasMatchFile) ||
				//		(pm1.hasWormFile && pm2.hasWormFile))
				if ((pm1.metadata == pm2.metadata) &&
						((pm1.metadata == MetaType.MATCH) ||
							(pm1.metadata == MetaType.WORM)))
					s.evaluatePaths();
				//else if (pm1.hasWormFile && !pm2.hasWormFile) {
				else if ((pm1.metadata != MetaType.NONE) &&
						(pm2.metadata == MetaType.NONE)) {
					//if (pm1.hasLabelFile)
					if (pm1.metadata == MetaType.LABEL)
						pm2.writeLabelFile(s);
					else if (pm1.metadata == MetaType.MIDI)
						pm2.writeMidiFile(s);
					//else if ((pm2.matchFileName != null) && !pm2.hasMatchFile)
				/*	else if (pm2.matchFileName != null)
						s.wormHandler.write(new File(pm2.matchFileName), false);
					else
						g.saveWormFile();*/
				} else if (pm1.outputFileName != null) {
					switch (pm1.outputType) {
						case BACKWARD:
							s.saveBackwardPath(pm1.outputFileName);
							break;
						case FORWARD:
							s.saveForwardPath(pm1.outputFileName);
							break;
						case SMOOTHED:
							s.saveSmoothedPath(pm1.outputFileName);
							break;
					}
				}
			}
			if (!silent)
				System.err.println("Processed " + pm1.frameCount + " and " +
						pm2.frameCount + " frames of "+pm1.fftSize+" samples");
		}
		if (batchMode)	// ScrollThread is running
			System.exit(0);
		
	} // main()

} // class PerformanceMatcher
