/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.Format;
import at.ofai.music.match.TempoMap;
import java.io.PrintStream;
import java.util.Arrays;

public class MatchTempoMap
implements TempoMap {
    protected double[] realTime;
    protected double[] scoreTime;
    protected int[] repeats;
    protected int size;

    public MatchTempoMap() {
        this(5000);
    }

    public MatchTempoMap(int sz) {
        this.realTime = new double[sz];
        this.scoreTime = new double[sz];
        this.repeats = new int[sz];
        this.size = 0;
    }

    protected void makeSpace() {
        if (this.size == this.realTime.length) {
            this.resize(new MatchTempoMap(2 * this.size));
        }
    }

    protected void closeList() {
        if (this.size != this.realTime.length) {
            this.resize(new MatchTempoMap(this.size));
        }
    }

    protected void resize(MatchTempoMap newList) {
        int i = 0;
        while (i < this.size) {
            newList.realTime[i] = this.realTime[i];
            newList.scoreTime[i] = this.scoreTime[i];
            newList.repeats[i] = this.repeats[i];
            ++i;
        }
        this.realTime = newList.realTime;
        this.scoreTime = newList.scoreTime;
        this.repeats = newList.repeats;
    }

    public double toRealTime(double sTime) {
        this.closeList();
        return this.lookup(sTime, this.scoreTime, this.realTime);
    }

    public double toScoreTime(double rTime) {
        this.closeList();
        return this.lookup(rTime, this.realTime, this.scoreTime);
    }

    public double lookup(double value, double[] domain, double[] range) {
        int index = Arrays.binarySearch(domain, value);
        if (index >= 0) {
            return range[index];
        }
        if (this.size == 0 || this.size == 1 && (range[0] == 0.0 || domain[0] == 0.0)) {
            throw new RuntimeException("Insufficient entries in tempo map");
        }
        if (this.size == 1) {
            return value * range[0] / domain[0];
        }
        if ((index = -1 - index) == 0) {
            ++index;
        } else if (index == this.size) {
            --index;
        }
        return (range[index] * (value - domain[index - 1]) + range[index - 1] * (domain[index] - value)) / (domain[index] - domain[index - 1]);
    }

    public void add(double rTime, double sTime) {
        if (Double.isNaN(sTime)) {
            return;
        }
        this.makeSpace();
        int index = 0;
        while (index < this.size) {
            if (sTime <= this.scoreTime[index]) break;
            ++index;
        }
        if (index == this.size || sTime != this.scoreTime[index]) {
            int j = this.size;
            while (j > index) {
                this.scoreTime[j] = this.scoreTime[j - 1];
                this.realTime[j] = this.realTime[j - 1];
                this.repeats[j] = this.repeats[j - 1];
                --j;
            }
            ++this.size;
            this.scoreTime[index] = sTime;
            this.realTime[index] = rTime;
            this.repeats[index] = 1;
        } else {
            this.realTime[index] = ((double)this.repeats[index] * this.realTime[index] + rTime) / (double)(this.repeats[index] + 1);
            int[] arrn = this.repeats;
            int n = index;
            arrn[n] = arrn[n] + 1;
        }
    }

    /*
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Lifted jumps to return sites
     */
//    public void dump(double[] tempo, double step) {
//        if (this.size < 2) {
//            System.err.println("dump() failed: Empty tempo map");
//            return;
//        }
//        tmp = new double[tempo.length];
//        i = 0;
//        j = 1;
//        ** GOTO lbl13
//        {
//            tmp[i] = (this.realTime[j] - this.realTime[j - 1]) / (this.scoreTime[j] - this.scoreTime[j - 1]);
//            ++i;
//            do {
//                if ((double)i * step < this.realTime[j]) continue block0;
//                ++j;
//lbl13: // 2 sources:
//            } while (j < this.size);
//        }
//        while (i < tmp.length) {
//            tmp[i] = (this.realTime[this.size - 1] - this.realTime[this.size - 2]) / (this.scoreTime[this.size - 1] - this.scoreTime[this.size - 2]);
//            ++i;
//        }
//        window = (int)(0.1 / step);
//        sum = 0.0;
//        i = 0;
//        while (i < tmp.length) {
//            tempo[i] = i >= window ? (sum -= tmp[i - window]) / (double)window : (sum += tmp[i]) / (double)(i + 1);
//            if (tempo[i] != 0.0) {
//                tempo[i] = 60.0 / tempo[i];
//            }
//            ++i;
//        }
//    }

    public void print() {
        System.out.println("Score  |  Perf.\n-------+-------");
        int i = 0;
        while (i < this.size) {
            System.out.println(String.valueOf(Format.d(this.scoreTime[i], 3)) + "  |  " + Format.d(this.realTime[i], 3));
            ++i;
        }
    }

    public static void main(String[] args) {
        MatchTempoMap mtm = new MatchTempoMap();
        mtm.add(0.6, 1.0);
        mtm.add(0.8, 2.0);
        mtm.add(0.95, 2.5);
        mtm.add(1.0, 3.0);
        double[] st = new double[]{0.0, 1.0, 2.0, 3.0, 4.0};
        int i = 0;
        while (i < st.length) {
            System.out.println(String.valueOf(st[i]) + " -> " + mtm.toRealTime(st[i]) + " -> " + mtm.toScoreTime(mtm.toRealTime(st[i])));
            ++i;
        }
        double[] rt = new double[]{0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1};
        int i2 = 0;
        while (i2 < rt.length) {
            System.out.println(String.valueOf(rt[i2]) + " => " + mtm.toScoreTime(rt[i2]) + " => " + mtm.toRealTime(mtm.toScoreTime(rt[i2])));
            ++i2;
        }
    }
}

