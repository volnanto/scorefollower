/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.BTFileParseException;
import at.ofai.music.match.Event;
import at.ofai.music.match.Flags;
import at.ofai.music.match.MatchFileParseException;
import at.ofai.music.match.Matcher;
import at.ofai.music.match.WormEvent;
import at.ofai.music.match.WormFileParseException;
//import at.ofai.music.match.Worm;
//import at.ofai.music.match.WormFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class EventList
implements Serializable {
    public LinkedList<Event> l = new LinkedList();
    protected static boolean timingCorrection = false;
    protected static double timingDisplacement = 0.0;
    protected static int clockUnits = 480;
    protected static int clockRate = 500000;
    protected static double metricalLevel = 0.0;
    public static final double UNKNOWN = Double.NaN;
    protected static boolean noMelody = false;
    protected static boolean onlyMelody = false;
    protected static Flags flags = new Flags();

    public EventList() {
    }

    public EventList(EventList e) {
        this();
        ListIterator<Event> it = e.listIterator();
        while (it.hasNext()) {
            this.add(it.next());
        }
    }

    public EventList(Event[] e) {
        this();
        int i = 0;
        while (i < e.length) {
            this.add(e[i]);
            ++i;
        }
    }

    public void add(Event e) {
        this.l.add(e);
    }

    public void add(EventList ev) {
        this.l.addAll(ev.l);
    }

    public void insert(Event newEvent, boolean uniqueTimes) {
        ListIterator<Event> li = this.l.listIterator();
        while (li.hasNext()) {
            int sgn = newEvent.compareTo(li.next());
            if (sgn < 0) {
                li.previous();
                break;
            }
            if (!uniqueTimes || sgn != 0) continue;
            li.remove();
            break;
        }
        li.add(newEvent);
    }

    public ListIterator<Event> listIterator() {
        return this.l.listIterator();
    }

    public Iterator<Event> iterator() {
        return this.l.iterator();
    }

    public int size() {
        return this.l.size();
    }

    public Event[] toArray() {
        return this.toArray(0);
    }

    public double[] toOnsetArray() {
        double[] d = new double[this.l.size()];
        int i = 0;
        Iterator<Event> it = this.l.iterator();
        while (it.hasNext()) {
            d[i] = it.next().keyDown;
            ++i;
        }
        return d;
    }

    public Event[] toArray(int match) {
        int count = 0;
        for (Event e : this.l) {
            if (match != 0 && e.midiCommand != match) continue;
            ++count;
        }
        Event[] a = new Event[count];
        int i = 0;
        for (Event e2 : this.l) {
            if (match != 0 && e2.midiCommand != match) continue;
            a[i++] = e2;
        }
        return a;
    }

    public void writeBinary(String fileName) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));
            oos.writeObject(this);
            oos.close();
        }
        catch (IOException e) {
            System.err.println(e);
        }
    }

    public static EventList readBinary(String fileName) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
            EventList e = (EventList)ois.readObject();
            ois.close();
            return e;
        }
        catch (IOException e) {
            System.err.println(e);
            return null;
        }
        catch (ClassNotFoundException e) {
            System.err.println(e);
            return null;
        }
    }

    public void writeMIDI(String fileName) {
        this.writeMIDI(fileName, null);
    }

    public void writeMIDI(String fileName, EventList pedal) {
        try {
            MidiSystem.write(this.toMIDI(pedal), 1, new File(fileName));
        }
        catch (Exception e) {
            System.err.println("Error: Unable to write MIDI file " + fileName);
            e.printStackTrace();
        }
    }

    public Sequence toMIDI(EventList pedal) throws InvalidMidiDataException {
        ShortMessage sm;
        int midiTempo = 1000000;
        Sequence s = new Sequence(0.0f, 1000);
        Track[] tr = new Track[16];
        tr[0] = s.createTrack();
        MetaMessage mm = new MetaMessage();
        byte[] b = new byte[]{15, 66, 64};
        mm.setMessage(81, b, 3);
        tr[0].add(new MidiEvent(mm, 0));
        for (Event e2 : this.l) {
            if (e2.midiCommand == 0) break;
            if (tr[e2.midiTrack] == null) {
                tr[e2.midiTrack] = s.createTrack();
            }
            sm = new ShortMessage();
            sm.setMessage(e2.midiCommand, e2.midiChannel, e2.midiPitch, e2.midiVelocity);
            tr[e2.midiTrack].add(new MidiEvent(sm, Math.round(1000.0 * e2.keyDown)));
            if (e2.midiCommand != 144) continue;
            sm = new ShortMessage();
            sm.setMessage(128, e2.midiChannel, e2.midiPitch, 0);
            tr[e2.midiTrack].add(new MidiEvent(sm, Math.round(1000.0 * e2.keyUp)));
        }
        if (pedal != null) {
            for (Event e2 : pedal.l) {
                if (tr[e2.midiTrack] == null) {
                    tr[e2.midiTrack] = s.createTrack();
                }
                sm = new ShortMessage();
                sm.setMessage(e2.midiCommand, e2.midiChannel, e2.midiPitch, e2.midiVelocity);
                tr[e2.midiTrack].add(new MidiEvent(sm, Math.round(1000.0 * e2.keyDown)));
                if (e2.midiCommand != 144) continue;
                sm = new ShortMessage();
                sm.setMessage(128, e2.midiChannel, e2.midiPitch, e2.midiVelocity);
                tr[e2.midiTrack].add(new MidiEvent(sm, Math.round(1000.0 * e2.keyUp)));
            }
        }
        return s;
    }

    public static EventList readMidiFile(String fileName) {
        return EventList.readMidiFile(fileName, 0);
    }

    public static EventList readMidiFile(String fileName, int skipTrackFlag) {
        Sequence s;
        EventList list = new EventList();
        try {
            s = MidiSystem.getSequence(new File(fileName));
        }
        catch (Exception e) {
            e.printStackTrace();
            return list;
        }
        double midiTempo = 500000.0;
        double tempoFactor = midiTempo / (double)s.getResolution() / 1000000.0;
        Event[][] noteOns = new Event[128][16];
        Track[] tracks = s.getTracks();
        int t = 0;
        while (t < tracks.length) {
            if ((skipTrackFlag & 1) != 1) {
                int e = 0;
                while (e < tracks[t].size()) {
                    int pitch;
                    MidiEvent me = tracks[t].get(e);
                    MidiMessage mm = me.getMessage();
                    double time = (double)me.getTick() * tempoFactor;
                    byte[] mesg = mm.getMessage();
                    int channel = mesg[0] & 15;
                    int command = mesg[0] & 240;
                    if (command == 144) {
                        pitch = mesg[1] & 127;
                        int velocity = mesg[2] & 127;
                        if (noteOns[pitch][channel] != null) {
                            if (velocity == 0) {
                                noteOns[pitch][channel].keyUp = time;
                                noteOns[pitch][channel].pedalUp = time;
                                noteOns[pitch][channel] = null;
                            } else {
                                System.err.println("Double note on: n=" + pitch + " c=" + channel + " t1=" + noteOns[pitch][channel] + " t2=" + time);
                            }
                        } else {
                            Event n;
                            noteOns[pitch][channel] = n = new Event(time, 0.0, 0.0, pitch, velocity, -1.0, -1.0, 0, 144, channel, t);
                            list.add(n);
                        }
                    } else if (command == 128) {
                        pitch = mesg[1] & 127;
                        noteOns[pitch][channel].keyUp = time;
                        noteOns[pitch][channel].pedalUp = time;
                        noteOns[pitch][channel] = null;
                    } else if (command == 240) {
                        if (channel == 15 && mesg[1] == 81) {
                            midiTempo = mesg[5] & 255 | (mesg[4] & 255) << 8 | (mesg[3] & 255) << 16;
                            tempoFactor = midiTempo / (double)s.getResolution() / 1000000.0;
                        }
                    } else if (mesg.length > 3) {
                        System.err.println("midi message too long: " + mesg.length);
                        System.err.println("\tFirst byte: " + mesg[0]);
                    } else {
                        int b0 = mesg[0] & 255;
                        int b1 = -1;
                        int b2 = -1;
                        if (mesg.length > 1) {
                            b1 = mesg[1] & 255;
                        }
                        if (mesg.length > 2) {
                            b2 = mesg[2] & 255;
                        }
                        list.add(new Event(time, time, -1.0, b1, b2, -1.0, -1.0, 0, b0 & 240, b0 & 15, t));
                    }
                    ++e;
                }
            }
            ++t;
            skipTrackFlag >>= 1;
        }
        int pitch = 0;
        while (pitch < 128) {
            int channel = 0;
            while (channel < 16) {
                if (noteOns[pitch][channel] != null) {
                    System.err.println("Missing note off: n=" + noteOns[pitch][channel].midiPitch + " t=" + noteOns[pitch][channel].keyDown);
                }
                ++channel;
            }
            ++pitch;
        }
        return list;
    }

    public void print() {
        Iterator<Event> i = this.l.iterator();
        while (i.hasNext()) {
            i.next().print(flags);
        }
    }

    public static void setTimingCorrection(double corr) {
        timingCorrection = corr >= 0.0;
        timingDisplacement = corr;
    }

    public static EventList readBeatsAsText(String fileName) throws Exception {
        EventList list = new EventList();
        BufferedReader inputFile = new BufferedReader(new FileReader(fileName));
        String s = inputFile.readLine();
        if (s.startsWith("###")) {
            return EventList.readLabelFile(fileName);
        }
        int beats = 0;
        int pitch = 56;
        int vol = 80;
        int ch = 10;
        int track = 0;
        int fl = 1;
        while (s != null) {
            String tmp;
            int ind = s.indexOf(44);
            if (ind < 0) {
                ind = s.indexOf(32);
            }
            double time = 0.0;
            if (ind >= 0) {
                tmp = s.substring(0, ind).trim();
                if (tmp.length() == 0) {
                    s = inputFile.readLine();
                    continue;
                }
                time = Double.parseDouble(tmp);
                s = s.substring(ind + 1);
            } else {
                tmp = s.trim();
                if (tmp.length() > 0) {
                    time = Double.parseDouble(tmp);
                }
                s = inputFile.readLine();
            }
            list.add(new Event(time, time, time, pitch, vol, ++beats, 1.0, fl, 144, ch, track));
        }
        return list;
    }

    public static EventList readBeatTrackFile(String fileName) throws Exception {
        if (!fileName.endsWith(".tmf")) {
            return EventList.readBeatsAsText(fileName);
        }
        EventList list = new EventList();
        BufferedReader inputFile = new BufferedReader(new FileReader(fileName));
        Matcher s = new Matcher(inputFile.readLine());
        if (!s.matchString("MFile")) {
            throw new BTFileParseException("Header not found");
        }
        s.getInt();
        int tracks = s.getInt();
        int div = s.getInt();
        int tempo = 500000;
        double tf = 1000000.0 / (double)tempo * (double)div;
        int lineCount = 1;
        int beats = 0;
        int track = 0;
        while (track < tracks) {
            s.set(inputFile.readLine());
            ++lineCount;
            if (!s.matchString("MTrk")) {
                throw new BTFileParseException("MTrk not found");
            }
            s.set(inputFile.readLine());
            ++lineCount;
            while (!s.matchString("TrkEnd")) {
                double time = (double)s.getInt() / tf;
                s.trimSpace();
                if (s.matchString("Tempo")) {
                    tempo = s.getInt();
                    tf = 1000000.0 / (double)tempo * (double)div;
                } else if (s.matchString("On")) {
                    s.trimSpace();
                    s.matchString("ch=");
                    int ch = s.getInt();
                    s.trimSpace();
                    if (!s.matchString("n=")) {
                        s.matchString("note=");
                    }
                    int pitch = s.getInt();
                    s.trimSpace();
                    if (!s.matchString("v=")) {
                        s.matchString("vol=");
                    }
                    int vol = s.getInt();
                    s.set(inputFile.readLine());
                    ++lineCount;
                    s.getInt();
                    s.trimSpace();
                    s.matchString("Off");
                    s.skip('v');
                    s.matchString("ol");
                    s.matchString("=");
                    int flags = s.getInt();
                    list.add(new Event(time, time, time, pitch, vol, ++beats, 1.0, flags, 144, ch, track));
                } else if (!s.matchString("Meta TrkEnd")) {
                    System.err.println("Unmatched text on line " + lineCount + ": " + s.get());
                }
                s.set(inputFile.readLine());
                ++lineCount;
            }
            ++track;
        }
        return list;
    }

    public void writeBeatsAsText(String fileName) throws Exception {
        PrintStream out = new PrintStream(new File(fileName));
        char separator = '\n';
        if (fileName.endsWith(".csv")) {
            separator = ',';
        }
        Iterator<Event> it = this.iterator();
        while (it.hasNext()) {
            Event e = it.next();
            Object[] arrobject = new Object[2];
            arrobject[0] = e.keyDown;
            arrobject[1] = Character.valueOf(it.hasNext() ? separator : '\n');
            out.printf("%5.3f%c", arrobject);
        }
        out.close();
    }

    public void writeBeatTrackFile(String fileName) throws Exception {
        if (fileName.endsWith(".txt") || fileName.endsWith(".csv")) {
            this.writeBeatsAsText(fileName);
        } else {
            PrintStream out = new PrintStream(new File(fileName));
            out.println("MFile 0 1 500");
            out.println("MTrk");
            out.println("     0 Tempo 500000");
            int time = 0;
            Iterator<Event> it = this.iterator();
            while (it.hasNext()) {
                Event e = it.next();
                time = (int)Math.round(1000.0 * e.keyDown);
                out.printf("%6d On   ch=%3d n=%3d v=%3d\n", time, e.midiChannel, e.midiPitch, e.midiVelocity);
                out.printf("%6d Off  ch=%3d n=%3d v=%3d\n", time, e.midiChannel, e.midiPitch, e.flags);
            }
            out.printf("%6d Meta TrkEnd\nTrkEnd\n", time);
            out.close();
        }
    }

    public static EventList readLabelFile(String fileName) throws Exception {
        EventList list = new EventList();
        BufferedReader inputFile = new BufferedReader(new FileReader(fileName));
        Matcher s = new Matcher(inputFile.readLine());
        int prevBar = 0;
        int beats = 0;
        int pitch = 56;
        int vol = 80;
        int ch = 10;
        int track = 0;
        while (s.hasData()) {
            if (!s.matchString("#")) {
                double time = s.getDouble();
                String label = s.get().trim();
                int colon = label.indexOf(58);
                int beat = 0;
                if (colon < 0) {
                    colon = label.length();
                } else {
                    beat = Integer.parseInt(label.substring(colon + 1));
                }
                int bar = Integer.parseInt(label.substring(0, colon));
                int flags = 2;
                if (bar != prevBar) {
                    flags |= 4;
                    prevBar = bar;
                }
                WormEvent ev = new WormEvent(time, time, time, pitch, vol, ++beats, 1.0, flags, 144, ch, track);
                ev.label = label;
                list.add(ev);
            }
            s.set(inputFile.readLine());
        }
        return list;
    }

    public void writeLabelFile(String fileName) throws Exception {
        PrintStream out = new PrintStream(new File(fileName));
        out.printf("###Created automatically\n", new Object[0]);
        for (Event ev : this.l) {
            out.printf("%5.3f\t%s\n", ev.keyDown, ((WormEvent)ev).label);
        }
        out.close();
    }

    public static EventList readWormFile(String fileName) throws Exception {
        EventList list = new EventList();
        BufferedReader inputFile = new BufferedReader(new FileReader(fileName));
        Matcher s = new Matcher(inputFile.readLine());
        int lineCount = 1;
        if (!s.matchString("WORM Version:")) {
            throw new WormFileParseException("WORM format: header not found");
        }
        if (s.getDouble() < 1.01) {
            throw new WormFileParseException("WORM format: v1.0 not supported");
        }
        int dataCountDown = -1;
        int beat = 0;
        do {
            s.set(inputFile.readLine());
            ++lineCount;
            if (dataCountDown == 0) {
                if (s.hasData()) {
                    System.err.println("Ignoring trailing data past line " + lineCount);
                }
                return list;
            }
            if (!s.hasData()) {
                throw new WormFileParseException("Unexpected EOF");
            }
            if (dataCountDown < 0) {
                if (!s.matchString("Length:")) continue;
                dataCountDown = s.getInt();
                continue;
            }
            double time = s.getDouble();
            double tempo = s.getDouble();
            double loudness = s.getDouble();
            int flags = s.getInt();
            if ((flags & 1) != 0) {
                ++beat;
            }
            list.add(new WormEvent(time, tempo, loudness, beat, flags));
            --dataCountDown;
        } while (true);
    }

    public static String getAudioFileFromWormFile(String wormFile) {
        return EventList.getWormFileAttribute(wormFile, "AudioFile");
    }

    public static double getTrackLevelFromWormFile(String wormFile) {
        String level = EventList.getWormFileAttribute(wormFile, "TrackLevel");
        try {
            int i = level.indexOf("/");
            if (i >= 0) {
                return Double.parseDouble(level.substring(0, i)) / Double.parseDouble(level.substring(i + 1));
            }
            return Double.parseDouble(level);
        }
        catch (Exception e) {
            System.err.println("Error getting TrackLevel:\n" + e);
            return 1.0;
        }
    }

    public static String getWormFileAttribute(String wormFile, String attr) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(wormFile));
            String line = r.readLine();
            attr = String.valueOf(attr) + ":";
            while (line != null) {
                if (line.startsWith(attr)) {
                    return line.substring(attr.length()).trim();
                }
                line = r.readLine();
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    public static EventList readMatchFile(String fileName) throws Exception {
        EventList list = new EventList();
        boolean startNote = timingCorrection;
        BufferedReader inputFile = new BufferedReader(new FileReader(fileName));
        double versionNumber = 1.0;
        int lineCount = 1;
        Matcher s = new Matcher(inputFile.readLine());
        while (s.hasData()) {
            int eventFlags = 0;
            double beat = Double.NaN;
            double duration = Double.NaN;
            if (s.matchString("info(")) {
                if (s.matchString("timeSignature,")) {
                    int numerator = s.getInt();
                    s.skip('/');
                    int denominator = s.getInt();
                } else if (s.matchString("beatSubdivision,")) {
                    s.skip(']');
                } else if (s.matchString("matchFileVersion,")) {
                    versionNumber = s.getDouble();
                } else if (s.matchString("midiClockUnits,")) {
                    clockUnits = s.getInt();
                } else if (s.matchString("midiClockRate,")) {
                    clockRate = s.getInt();
                }
                s.set("%");
            } else if (s.matchString("snote(")) {
                s.skip(',');
                s.skip(']');
                s.skip(',');
                s.skip(',');
                s.skip(',');
                boolean isBt = s.matchString("0");
                s.skip(',');
                s.skip(',');
                try {
                    beat = s.getDouble();
                }
                catch (NumberFormatException e) {
                    System.err.println("Bad beat number on line " + lineCount);
                    beat = Double.NaN;
                }
                if (beat == Math.rint(beat) != isBt) {
                    System.err.println("Inconsistent beats on line " + lineCount);
                }
                s.skip(',');
                try {
                    duration = s.getDouble() - beat;
                }
                catch (NumberFormatException e) {
                    System.err.println("Bad duration on line " + lineCount);
                    duration = Double.NaN;
                }
                s.skip(',');
                s.skip('[');
                do {
                    String element = s.getString();
                    eventFlags |= flags.getFlag(element);
                } while (s.matchString(","));
                s.skip('-');
            } else if (s.matchString("trill(")) {
                eventFlags |= flags.getFlag("trill");
                s.skip('-');
            } else if (s.matchString("ornament(")) {
                eventFlags |= flags.getFlag("ornament");
                s.skip('-');
            } else if (s.matchString("trailing_played_note-") || s.matchString("hammer_bounce-") || s.matchString("no_score_note-") || s.matchString("insertion-")) {
                eventFlags |= flags.getFlag("unscored");
            } else if (!s.matchString("%")) {
                throw new MatchFileParseException("error 4; line " + lineCount);
            }
            if (s.matchString("note(")) {
                int pitch;
                double eOffset;
                int m;
                s.skip('[');
                String note = s.getString();
                switch (Character.toUpperCase(note.charAt(0))) {
                    case 'A': {
                        pitch = 9;
                        break;
                    }
                    case 'B': {
                        pitch = 11;
                        break;
                    }
                    case 'C': {
                        pitch = 0;
                        break;
                    }
                    case 'D': {
                        pitch = 2;
                        break;
                    }
                    case 'E': {
                        pitch = 4;
                        break;
                    }
                    case 'F': {
                        pitch = 5;
                        break;
                    }
                    case 'G': {
                        pitch = 7;
                        break;
                    }
                    default: {
                        throw new MatchFileParseException("Bad note on line " + lineCount);
                    }
                }
                s.skip(',');
                String mod = s.getString();
                int i = 0;
                while (i < mod.length()) {
                    switch (mod.charAt(i)) {
                        case '#': {
                            ++pitch;
                            break;
                        }
                        case 'b': {
                            --pitch;
                            break;
                        }
                        case 'n': {
                            break;
                        }
                        default: {
                            throw new MatchFileParseException("error 5 " + lineCount);
                        }
                    }
                    ++i;
                }
                s.skip(',');
                int octave = s.getInt();
                pitch += 12 * octave;
                s.skip(',');
                double onset = s.getInt();
                s.skip(',');
                double offset = s.getInt();
                if (versionNumber > 1.0) {
                    s.skip(',');
                    eOffset = s.getInt();
                } else {
                    eOffset = offset;
                }
                s.skip(',');
                int velocity = s.getInt();
                onset /= (double)clockUnits * 1000000.0 / (double)clockRate;
                offset /= (double)clockUnits * 1000000.0 / (double)clockRate;
                eOffset /= (double)clockUnits * 1000000.0 / (double)clockRate;
                if (timingCorrection) {
                    if (startNote) {
                        timingDisplacement = onset - timingDisplacement;
                        startNote = false;
                    }
                    onset -= timingDisplacement;
                    offset -= timingDisplacement;
                    eOffset -= timingDisplacement;
                }
                if ((eventFlags & (m = flags.getFlag("s"))) != 0 && !noMelody || (eventFlags & m) == 0 && !onlyMelody) {
                    Event e = new Event(onset, offset, eOffset, pitch, velocity, beat, duration, eventFlags);
                    list.add(e);
                }
            } else if (!(s.matchString("no_played_note.") || s.matchString("trailing_score_note.") || s.matchString("deletion.") || s.matchString("%"))) {
                throw new MatchFileParseException("error 6; line " + lineCount);
            }
            s.set(inputFile.readLine());
            ++lineCount;
        }
        return list;
    }

    public static void main(String[] args) throws Exception {
        EventList.readLabelFile(args[0]).print();
        System.exit(0);
        EventList el = EventList.readMatchFile(args[0]);
     /*   WormFile wf = new WormFile(null, el);
        if (args.length >= 2) {
            double sm = Double.parseDouble(args[1]);
            wf.smooth(3, sm, sm, 0);
        } else {
            wf.smooth(0, 0.0, 0.0, 0);
        }
        wf.write("worm.out");
        if (args.length == 3) {
            el.print();
        } */
    }
}

