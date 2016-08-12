/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.Format;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.PrintStream;

public class PSPrinter
implements Printable {
    Component component;
    int resolution;

    public static void print(Component c, int r) {
        new PSPrinter(c, r).doPrint();
    }

    public static void print(Component c) {
        new PSPrinter(c, -1).doPrint();
    }

    public PSPrinter(Component c, int res) {
        this.component = c;
        this.resolution = res;
    }

    public void doPrint() {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(this);
        if (printJob.printDialog()) {
            try {
                printJob.print();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public int print(Graphics g, PageFormat f, int pg) throws PrinterException {
        if (pg >= 1) {
            return 1;
        }
        Graphics2D g2 = (Graphics2D)g;
        double wd = this.component.getWidth();
        double ht = this.component.getHeight();
        double imwd = f.getImageableWidth();
        double imht = f.getImageableHeight();
        double corr = (double)this.resolution / 72.0;
        double scaleFactor = corr * Math.min(imwd / wd, imht / ht);
        double xmin = f.getImageableX();
        double ymin = f.getImageableY();
        AffineTransform scale = new AffineTransform(scaleFactor, 0.0, 0.0, scaleFactor, corr * xmin, corr * ymin);
        Format.setGroupingUsed(false);
        double pgHt = f.getHeight();
        if (this.resolution > 0) {
            g2.setTransform(scale);
            System.out.println("%%BoundingBox: " + Format.d(xmin, 0) + " " + Format.d(pgHt - ymin - ht * scaleFactor / corr, 0) + " " + Format.d(xmin + wd * scaleFactor / corr, 0) + " " + Format.d(pgHt - ymin, 0));
        } else {
            g2.setClip(0, 0, (int)wd, (int)ht);
            System.out.println("%%BoundingBox: " + Format.d(0.0, 0) + " " + Format.d(pgHt - ht, 0) + " " + Format.d(wd, 0) + " " + Format.d(pgHt, 0));
        }
        this.component.printAll(g2);
        return 0;
    }
}

