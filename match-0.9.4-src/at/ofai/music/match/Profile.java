/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import java.io.PrintStream;

public class Profile {
    public static final int MAX_SIZE = 20;
    private static long[] tmin = new long[20];
    private static long[] tmax = new long[20];
    private static long[] tsum = new long[20];
    private static long[] tprev = new long[20];
    private static int[] tcount = new int[20];

    public static void report(int i) {
        if (i < 0 || i >= 20 || tcount[i] == 0) {
            return;
        }
        System.err.println("Profile " + i + ": " + tcount[i] + " calls;  " + (double)tmin[i] / 1000.0 + " - " + (double)tmax[i] / 1000.0 + ";  Av: " + (double)(tsum[i] / (long)tcount[i]) / 1000.0);
    }

    public static void report() {
        int i = 0;
        while (i < 20) {
            Profile.report(i);
            ++i;
        }
    }

    public static void start(int i) {
        Profile.tprev[i] = System.nanoTime();
    }

    public static void log(int i) {
        long tmp = System.nanoTime();
        long t = (tmp - tprev[i]) / 1000;
        Profile.tprev[i] = tmp;
        long[] arrl = tsum;
        int n = i;
        arrl[n] = arrl[n] + t;
        if (tcount[i] == 0 || t > tmax[i]) {
            Profile.tmax[i] = t;
        }
        if (tcount[i] == 0 || t < tmin[i]) {
            Profile.tmin[i] = t;
        }
        int[] arrn = tcount;
        int n2 = i;
        arrn[n2] = arrn[n2] + 1;
    }
}

