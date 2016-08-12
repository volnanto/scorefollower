/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.Event;

public class WormEvent
extends Event {
    public double tempo;
    public double loudness;
    public String label;

    public WormEvent(double on, double off, double eoff, int pitch, int vel, double beat, double dur, int flags, int cmd, int ch, int tr) {
        super(on, off, eoff, pitch, vel, beat, dur, flags, cmd, ch, tr);
        this.tempo = -1.0;
        this.loudness = -1.0;
        this.label = null;
    }

    public WormEvent(double time, double t, double l, double beat, int flags) {
        super(time, 0.0, 0.0, 0, 0, beat, 0.0, flags);
        this.tempo = t;
        this.loudness = l;
        this.label = null;
    }
}

