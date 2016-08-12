/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import java.io.PrintStream;
import java.util.LinkedList;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Peaks {
    public static boolean debug = false;
    public static int pre = 3;
    public static int post = 1;

    public static int findPeaks(double[] data, int[] peaks, int width) {
        int peakCount = 0;
        int maxp = 0;
        int mid = 0;
        int end = data.length;
        while (mid < end) {
            int stop;
            int i = mid - width;
            if (i < 0) {
                i = 0;
            }
            if ((stop = mid + width + 1) > data.length) {
                stop = data.length;
            }
            maxp = i++;
            while (i < stop) {
                if (data[i] > data[maxp]) {
                    maxp = i;
                }
                ++i;
            }
            if (maxp == mid) {
                int j = peakCount;
                while (j > 0) {
                    if (data[maxp] <= data[peaks[j - 1]]) break;
                    if (j < peaks.length) {
                        peaks[j] = peaks[j - 1];
                    }
                    --j;
                }
                if (j != peaks.length) {
                    peaks[j] = maxp;
                }
                if (peakCount != peaks.length) {
                    ++peakCount;
                }
            }
            ++mid;
        }
        return peakCount;
    }

    public static LinkedList<Integer> findPeaks(double[] data, int width, double threshold) {
        return Peaks.findPeaks(data, width, threshold, 0.0, false);
    }

    public static LinkedList<Integer> findPeaks(double[] data, int width, double threshold, double decayRate, boolean isRelative) {
        LinkedList<Integer> peaks = new LinkedList<Integer>();
        int maxp = 0;
        int mid = 0;
        int end = data.length;
        double av = data[0];
        while (mid < end) {
            int stop;
            int i;
            if ((av = decayRate * av + (1.0 - decayRate) * data[mid]) < data[mid]) {
                av = data[mid];
            }
            if ((i = mid - width) < 0) {
                i = 0;
            }
            if ((stop = mid + width + 1) > data.length) {
                stop = data.length;
            }
            maxp = i++;
            while (i < stop) {
                if (data[i] > data[maxp]) {
                    maxp = i;
                }
                ++i;
            }
            if (maxp == mid) {
                if (Peaks.overThreshold(data, maxp, width, threshold, isRelative, av)) {
                    if (debug) {
                        System.out.println(" peak");
                    }
                    peaks.add(new Integer(maxp));
                } else if (debug) {
                    System.out.println();
                }
            }
            ++mid;
        }
        return peaks;
    }

    public static double expDecayWithHold(double av, double decayRate, double[] data, int start, int stop) {
        while (start < stop) {
            if ((av = decayRate * av + (1.0 - decayRate) * data[start]) < data[start]) {
                av = data[start];
            }
            ++start;
        }
        return av;
    }

    public static boolean overThreshold(double[] data, int index, int width, double threshold, boolean isRelative, double av) {
        if (debug) {
            System.out.printf("%4d : %6.3f     Av1: %6.3f    ", index, data[index], av);
        }
        if (data[index] < av) {
            return false;
        }
        if (isRelative) {
            int iStop;
            int iStart = index - pre * width;
            if (iStart < 0) {
                iStart = 0;
            }
            if ((iStop = index + post * width) > data.length) {
                iStop = data.length;
            }
            double sum = 0.0;
            int count = iStop - iStart;
            while (iStart < iStop) {
                sum += data[iStart++];
            }
            if (debug) {
                System.out.printf("    %6.3f    %6.3f   ", sum / (double)count, data[index] - sum / (double)count - threshold);
            }
            if (data[index] > sum / (double)count + threshold) {
                return true;
            }
            return false;
        }
        if (data[index] > threshold) {
            return true;
        }
        return false;
    }

    public static void normalise(double[] data) {
        double sx = 0.0;
        double sxx = 0.0;
        int i = 0;
        while (i < data.length) {
            sx += data[i];
            sxx += data[i] * data[i];
            ++i;
        }
        double mean = sx / (double)data.length;
        double sd = Math.sqrt((sxx - sx * mean) / (double)data.length);
        if (sd == 0.0) {
            sd = 1.0;
        }
        int i2 = 0;
        while (i2 < data.length) {
            data[i2] = (data[i2] - mean) / sd;
            ++i2;
        }
    }

    public static void getSlope(double[] data, double hop, int n, double[] slope) {
        int i = 0;
        int j = 0;
        double sx = 0.0;
        double sxx = 0.0;
        double sy = 0.0;
        double sxy = 0.0;
        while (i < n) {
            double t = (double)i * hop;
            sx += t;
            sxx += t * t;
            sy += data[i];
            sxy += t * data[i];
            ++i;
        }
        double delta = (double)n * sxx - sx * sx;
        while (j < n / 2) {
            slope[j] = ((double)n * sxy - sx * sy) / delta;
            ++j;
        }
        while (j < data.length - (n + 1) / 2) {
            slope[j] = ((double)n * sxy - sx * sy) / delta;
            sxy += hop * ((double)n * data[i] - (sy += data[i] - data[i - n]));
            ++j;
            ++i;
        }
        while (j < data.length) {
            slope[j] = ((double)n * sxy - sx * sy) / delta;
            ++j;
        }
    }

    public static double min(double[] arr) {
        return arr[Peaks.imin(arr)];
    }

    public static double max(double[] arr) {
        return arr[Peaks.imax(arr)];
    }

    public static int imin(double[] arr) {
        int i = 0;
        int j = 1;
        while (j < arr.length) {
            if (arr[j] < arr[i]) {
                i = j;
            }
            ++j;
        }
        return i;
    }

    public static int imax(double[] arr) {
        int i = 0;
        int j = 1;
        while (j < arr.length) {
            if (arr[j] > arr[i]) {
                i = j;
            }
            ++j;
        }
        return i;
    }
}

