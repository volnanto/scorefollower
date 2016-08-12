/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.TempoMap;
import java.io.PrintStream;

public class ConstantTempoMap
implements TempoMap {
    protected double interBeatInterval;

    public ConstantTempoMap(double bpm) {
        this.interBeatInterval = 60.0 / bpm;
    }

    public ConstantTempoMap() {
        this(120.0);
    }

    public void add(double time, double tempo) {
        throw new RuntimeException("ConstantTempoMap: cannot change tempo");
    }

    public double toRealTime(double value) {
        return value * this.interBeatInterval;
    }

    public double toScoreTime(double value) {
        return value / this.interBeatInterval;
    }

    public static void main(String[] args) {
        ConstantTempoMap mtm = new ConstantTempoMap(100.0);
        System.out.println(mtm.toRealTime(1.0));
        System.out.println(mtm.toScoreTime(mtm.toRealTime(1.0)));
        System.out.println(mtm.toScoreTime(4.0));
        System.out.println(mtm.toRealTime(mtm.toScoreTime(4.0)));
        mtm.add(5.0, 120.0);
    }
}

