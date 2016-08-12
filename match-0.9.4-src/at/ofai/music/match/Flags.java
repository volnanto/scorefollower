/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import java.io.PrintStream;

class Flags {
    String[] labels = new String[32];
    int size = 0;

    Flags() {
    }

    int getFlag(String s) {
        if (s == null || s.equals("")) {
            return 0;
        }
        int i = 0;
        while (i < this.size) {
            if (s.equals(this.labels[i])) {
                return 1 << i;
            }
            ++i;
        }
        if (this.size == 32) {
            System.err.println("Overflow: Too many flags: " + s);
            --this.size;
        }
        this.labels[this.size] = s;
        return 1 << this.size++;
    }

    String getLabel(int i) {
        if (i >= this.size) {
            return "ERROR: Unknown flag";
        }
        return this.labels[i];
    }
}

