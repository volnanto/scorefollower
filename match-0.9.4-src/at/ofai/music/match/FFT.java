package at.ofai.music.match;

import java.io.PrintStream;

public class FFT {
    public static final int FORWARD = -1;
    public static final int REVERSE = 1;
    public static final int RECT = 0;
    public static final int HAMMING = 1;
    public static final int BH3 = 2;
    public static final int BH4 = 3;
    public static final int BH3MIN = 4;
    public static final int BH4MIN = 5;
    public static final int GAUSS = 6;
    static final double twoPI = 6.283185307179586;

    public static void fft(double[] re, double[] im, int direction) {
        int n = re.length;
        int bits = (int)Math.rint(Math.log(n) / Math.log(2.0));
        if (n != 1 << bits) {
            throw new IllegalArgumentException("FFT data must be power of 2");
        }
        int j = 0;
        int i = 0;
        while (i < n - 1) {
            if (i < j) {
                double temp = re[j];
                re[j] = re[i];
                re[i] = temp;
                temp = im[j];
                im[j] = im[i];
                im[i] = temp;
            }
            int k = n / 2;
            while (k >= 1 && k - 1 < j) {
                j -= k;
                k /= 2;
            }
            j += k;
            ++i;
        }
        int m = 1;
        while (m <= bits) {
            int localN = 1 << m;
            double Wjk_r = 1.0;
            double Wjk_i = 0.0;
            double theta = 6.283185307179586 / (double)localN;
            double Wj_r = Math.cos(theta);
            double Wj_i = (double)direction * Math.sin(theta);
            int nby2 = localN / 2;
            j = 0;
            while (j < nby2) {
                int k = j;
                while (k < n) {
                    int id = k + nby2;
                    double tempr = Wjk_r * re[id] - Wjk_i * im[id];
                    double tempi = Wjk_r * im[id] + Wjk_i * re[id];
                    re[id] = re[k] - tempr;
                    im[id] = im[k] - tempi;
                    double[] arrd = re;
                    int n2 = k;
                    arrd[n2] = arrd[n2] + tempr;
                    double[] arrd2 = im;
                    int n3 = k;
                    arrd2[n3] = arrd2[n3] + tempi;
                    k += localN;
                }
                double wtemp = Wjk_r;
                Wjk_r = Wj_r * Wjk_r - Wj_i * Wjk_i;
                Wjk_i = Wj_r * Wjk_i + Wj_i * wtemp;
                ++j;
            }
            ++m;
        }
    }

    public static void powerFFT(double[] re) {
        double[] im = new double[re.length];
        FFT.fft(re, im, -1);
        int i = 0;
        while (i < re.length) {
            re[i] = re[i] * re[i] + im[i] * im[i];
            ++i;
        }
    }

    public static void toMagnitude(double[] re) {
        int i = 0;
        while (i < re.length) {
            re[i] = Math.sqrt(re[i]);
            ++i;
        }
    }

    public static void magnitudeFFT(double[] re) {
        FFT.powerFFT(re);
        FFT.toMagnitude(re);
    }

    public static void powerPhaseFFT(double[] re, double[] im) {
        FFT.fft(re, im, -1);
        int i = 0;
        while (i < re.length) {
            double pow = re[i] * re[i] + im[i] * im[i];
            im[i] = Math.atan2(im[i], re[i]);
            re[i] = pow;
            ++i;
        }
    }

    public static void powerPhaseIFFT(double[] pow, double[] ph) {
        FFT.toMagnitude(pow);
        int i = 0;
        while (i < pow.length) {
            double re = pow[i] * Math.cos(ph[i]);
            ph[i] = pow[i] * Math.sin(ph[i]);
            pow[i] = re;
            ++i;
        }
        FFT.fft(pow, ph, 1);
    }

    public static void magnitudePhaseFFT(double[] re, double[] im) {
        FFT.powerPhaseFFT(re, im);
        FFT.toMagnitude(re);
    }

    static void hamming(double[] data, int size) {
        int start = (data.length - size) / 2;
        int stop = (data.length + size) / 2;
        double scale = 1.0 / (double)size / 0.54;
        double factor = 6.283185307179586 / (double)size;
        int i = 0;
        while (start < stop) {
            data[i] = scale * (0.5434782608695652 - 0.45652173913043476 * Math.cos(factor * (double)i));
            ++start;
            ++i;
        }
    }

    static void blackmanHarris4sMin(double[] data, int size) {
        int start = (data.length - size) / 2;
        int stop = (data.length + size) / 2;
        double scale = 1.0 / (double)size / 0.36;
        int i = 0;
        while (start < stop) {
            data[i] = scale * (0.35875 - 0.48829 * Math.cos(6.283185307179586 * (double)i / (double)size) + 0.14128 * Math.cos(12.566370614359172 * (double)i / (double)size) - 0.01168 * Math.cos(18.84955592153876 * (double)i / (double)size));
            ++start;
            ++i;
        }
    }

    static void blackmanHarris4s(double[] data, int size) {
        int start = (data.length - size) / 2;
        int stop = (data.length + size) / 2;
        double scale = 1.0 / (double)size / 0.4;
        int i = 0;
        while (start < stop) {
            data[i] = scale * (0.40217 - 0.49703 * Math.cos(6.283185307179586 * (double)i / (double)size) + 0.09392 * Math.cos(12.566370614359172 * (double)i / (double)size) - 0.00183 * Math.cos(18.84955592153876 * (double)i / (double)size));
            ++start;
            ++i;
        }
    }

    static void blackmanHarris3sMin(double[] data, int size) {
        int start = (data.length - size) / 2;
        int stop = (data.length + size) / 2;
        double scale = 1.0 / (double)size / 0.42;
        int i = 0;
        while (start < stop) {
            data[i] = scale * (0.42323 - 0.49755 * Math.cos(6.283185307179586 * (double)i / (double)size) + 0.07922 * Math.cos(12.566370614359172 * (double)i / (double)size));
            ++start;
            ++i;
        }
    }

    static void blackmanHarris3s(double[] data, int size) {
        int start = (data.length - size) / 2;
        int stop = (data.length + size) / 2;
        double scale = 1.0 / (double)size / 0.45;
        int i = 0;
        while (start < stop) {
            data[i] = scale * (0.44959 - 0.49364 * Math.cos(6.283185307179586 * (double)i / (double)size) + 0.05677 * Math.cos(12.566370614359172 * (double)i / (double)size));
            ++start;
            ++i;
        }
    }

    static void gauss(double[] data, int size) {
        int start = (data.length - size) / 2;
        int stop = (data.length + size) / 2;
        double delta = 5.0 / (double)size;
        double x = (double)(1 - size) / 2.0 * delta;
        double c = -3.141592653589793 * Math.exp(1.0) / 10.0;
        double sum = 0.0;
        int i = start;
        while (i < stop) {
            data[i] = Math.exp(c * x * x);
            x += delta;
            sum += data[i];
            ++i;
        }
        i = start;
        while (i < stop) {
            double[] arrd = data;
            int n = i++;
            arrd[n] = arrd[n] / sum;
        }
    }

    static void rectangle(double[] data, int size) {
        int start = (data.length - size) / 2;
        int stop = (data.length + size) / 2;
        int i = start;
        while (i < stop) {
            data[i] = 1.0 / (double)size;
            ++i;
        }
    }

    public static double[] makeWindow(int choice, int size, int support) {
        double[] data = new double[size];
        if (support > size) {
            support = size;
        }
        switch (choice) {
            case 0: {
                FFT.rectangle(data, support);
                break;
            }
            case 1: {
                FFT.hamming(data, support);
                break;
            }
            case 2: {
                FFT.blackmanHarris3s(data, support);
                break;
            }
            case 3: {
                FFT.blackmanHarris4s(data, support);
                break;
            }
            case 4: {
                FFT.blackmanHarris3sMin(data, support);
                break;
            }
            case 5: {
                FFT.blackmanHarris4sMin(data, support);
                break;
            }
            case 6: {
                FFT.gauss(data, support);
                break;
            }
            default: {
                FFT.rectangle(data, support);
            }
        }
        return data;
    }

    public static void applyWindow(double[] data, double[] window) {
        int i = 0;
        while (i < data.length) {
            double[] arrd = data;
            int n = i;
            arrd[n] = arrd[n] * window[i];
            ++i;
        }
    }

    public static void main(String[] args) {
        int SZ = 1048576;
        double[] r1 = new double[1048576];
        double[] i1 = new double[1048576];
        double[] r2 = new double[1048576];
        double[] i2 = new double[1048576];
        int j = 0;
        while (j < 1048576) {
            r1[j] = r2[j] = Math.random();
            i1[j] = i2[j] = Math.random();
            ++j;
        }
        System.out.println("start");
        FFT.fft(r2, i2, -1);
        System.out.println("reverse");
        FFT.fft(r2, i2, 1);
        System.out.println("result");
        double err = 0.0;
        int j2 = 0;
        while (j2 < 1048576) {
            err += Math.abs(r1[j2] - r2[j2] / 1048576.0) + Math.abs(i1[j2] - i2[j2] / 1048576.0);
            ++j2;
        }
        System.out.printf("Err: %12.10f   Av: %12.10f\n", err, err / 1048576.0);
    }
}

