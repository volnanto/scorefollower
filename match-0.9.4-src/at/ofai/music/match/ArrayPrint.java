/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import java.io.PrintStream;

public class ArrayPrint {
    public static void show(String s, double[] arr, int line) {
        System.out.println(String.valueOf(s) + " (length = " + arr.length + ")");
        int i = 0;
        while (i < arr.length) {
            System.out.printf("%7.3f ", arr[i]);
            if (i % line == line - 1) {
                System.out.println();
            }
            ++i;
        }
        System.out.println();
    }

    void show(String s, int[] arr, int line) {
        System.out.println(String.valueOf(s) + " (length = " + arr.length + ")");
        int i = 0;
        while (i < arr.length) {
            System.out.printf("%7d ", arr[i]);
            if (i % line == line - 1) {
                System.out.println();
            }
            ++i;
        }
        System.out.println();
    }
}

