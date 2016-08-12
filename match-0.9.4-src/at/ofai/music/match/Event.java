/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.Flags;
import java.io.PrintStream;
import java.io.Serializable;

public class Event
implements Comparable,
Cloneable,
Serializable {
    public double keyDown;
    public double keyUp;
    public double pedalUp;
    public double scoreBeat;
    public double scoreDuration;
    public double salience;
    public int midiPitch;
    public int midiVelocity;
    public int flags;
    public int midiCommand;
    public int midiChannel;
    public int midiTrack;

    public Event(double onset, double offset, double eOffset, int pitch, int velocity, double beat, double duration, int eventFlags, int command, int channel, int track) {
        this(onset, offset, eOffset, pitch, velocity, beat, duration, eventFlags);
        this.midiCommand = command;
        this.midiChannel = channel;
        this.midiTrack = track;
    }

    public Event(double onset, double offset, double eOffset, int pitch, int velocity, double beat, double duration, int eventFlags) {
        this.keyDown = onset;
        this.keyUp = offset;
        this.pedalUp = eOffset;
        this.midiPitch = pitch;
        this.midiVelocity = velocity;
        this.scoreBeat = beat;
        this.scoreDuration = duration;
        this.flags = eventFlags;
        this.midiCommand = 144;
        this.midiChannel = 1;
        this.midiTrack = 0;
        this.salience = 0.0;
    }

    public Event clone() {
        return new Event(this.keyDown, this.keyUp, this.pedalUp, this.midiPitch, this.midiVelocity, this.scoreBeat, this.scoreDuration, this.flags, this.midiCommand, this.midiChannel, this.midiTrack);
    }

    public int compareTo(Object o) {
        Event e = (Event)o;
        return (int)Math.signum(this.keyDown - e.keyDown);
    }

    public String toString() {
        return "n=" + this.midiPitch + " v=" + this.midiVelocity + " t=" + this.keyDown + " to " + this.keyUp + " (" + this.pedalUp + ")";
    }

    public void print(Flags f) {
        System.out.printf("Event:\n", new Object[0]);
        System.out.printf("\tkeyDown / Up / pedalUp: %5.3f / %5.3f /  %5.3f\n", this.keyDown, this.keyUp, this.pedalUp);
        System.out.printf("\tmidiPitch: %d\n", this.midiPitch);
        System.out.printf("\tmidiVelocity: %d\n", this.midiVelocity);
        System.out.printf("\tmidiCommand: %02x\t", this.midiCommand | this.midiChannel);
        System.out.printf("\tmidiTrack: %d\n", this.midiTrack);
        System.out.printf("\tsalience: %5.3f\t", this.salience);
        System.out.printf("\tscoreBeat: %5.3f\t", this.scoreBeat);
        System.out.printf("\tscoreDuration: %5.3f\n", this.scoreDuration);
        System.out.printf("\tflags: %X", this.flags);
        if (f != null) {
            int ff = this.flags;
            int i = 0;
            while (ff != 0) {
                if (ff % 2 == 1) {
                    System.out.print(" " + f.getLabel(i));
                }
                ff >>>= 1;
                ++i;
            }
        }
        System.out.print("\n\n");
    }
}

