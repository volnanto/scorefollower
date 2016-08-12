/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;

public class Format {
    protected static NumberFormat intFormat = NumberFormat.getInstance();
    protected static NumberFormat doubleFormat = NumberFormat.getInstance();
    protected static char plusSign = 32;

    public static void setPostDigits(int dp) {
        doubleFormat.setMinimumFractionDigits(dp);
        doubleFormat.setMaximumFractionDigits(dp);
    }

    public static void setPreDigits(int dp) {
        doubleFormat.setMinimumIntegerDigits(dp);
    }

    public static void setIntDigits(int dp) {
        intFormat.setMinimumIntegerDigits(dp);
        intFormat.setMinimumFractionDigits(0);
        intFormat.setMaximumFractionDigits(0);
    }

    public static void setGroupingUsed(boolean flag) {
        doubleFormat.setGroupingUsed(flag);
        intFormat.setGroupingUsed(flag);
    }

    public static void setPlusSign(char c) {
        plusSign = c;
    }

    public static void init(int id, int did, int dfd, boolean grouping) {
        Format.setIntDigits(id);
        Format.setPreDigits(did);
        Format.setPostDigits(dfd);
        Format.setGroupingUsed(grouping);
    }

    public static String d(double n, int id, int fd) {
        Format.setPreDigits(id);
        return Format.d(n, fd);
    }

    public static String d(double n, int fd) {
        Format.setPostDigits(fd);
        return Format.d(n);
    }

    public static String d(double n) {
        if (Double.isNaN(n)) {
            return "NaN";
        }
        String s = doubleFormat.format(n);
        if (n >= 0.0) {
            s = String.valueOf(plusSign) + s;
        }
        char[] c = s.toCharArray();
        int i = 1;
        while (i < c.length - 1 && c[i] == '0' && c[i + 1] != '.') {
            c[i] = c[i - 1];
            c[i - 1] = 32;
            ++i;
        }
        if (i > 1) {
            s = new String(c);
        }
        return s;
    }

    public static String i(int n, int id) {
        Format.setIntDigits(id);
        return Format.i(n);
    }

    public static String i(int n) {
        return n < 0 ? intFormat.format(n) : String.valueOf(plusSign) + intFormat.format(n);
    }

    public static void matlab(double[] data, String name) {
        Format.matlab(data, name, 4);
    }

    public static void matlab(double[] data, String name, int dp) {
        PrintStream out;
        Format.setPostDigits(dp);
        try {
            out = new PrintStream(new FileOutputStream(String.valueOf(name) + ".m"));
        }
        catch (FileNotFoundException e) {
            out = System.out;
        }
        Format.matlab(data, name, out);
        if (out != System.out) {
            out.close();
        }
    }

    public static void matlab(double[] data, String name, PrintStream out) {
        out.println(String.valueOf(name) + " = [");
        Format.setGroupingUsed(false);
        int i = 0;
        while (i < data.length) {
            out.println(Format.d(data[i]));
            ++i;
        }
        out.println("];");
    }
}

