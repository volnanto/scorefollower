/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.Event;
import at.ofai.music.match.EventList;
import at.ofai.music.match.Format;
import java.io.PrintStream;

class MIDI2Matlab {
    MIDI2Matlab() {
    }

    public static void main(String[] args) {
        EventList e = EventList.readMidiFile(args[0]);
        Event[] events = e.toArray(144);
        int len = events.length;
        double[] pitch = new double[len];
        double[] vel = new double[len];
        double[] onset = new double[len];
        double[] offset = new double[len];
        int i = 0;
        while (i < len) {
            pitch[i] = events[i].midiPitch;
            vel[i] = events[i].midiVelocity;
            onset[i] = events[i].keyDown;
            offset[i] = events[i].keyUp;
            ++i;
        }
        Format.init(1, 5, 3, false);
        System.out.println("notes = zeros(" + len + ",4);");
        Format.matlab(onset, "notes(:,1)", System.out);
        Format.matlab(offset, "notes(:,2)", System.out);
        Format.matlab(pitch, "notes(:,3)", System.out);
        Format.matlab(vel, "notes(:,4)", System.out);
    }
}

