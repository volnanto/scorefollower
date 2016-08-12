/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

public class FrameMargins {
    protected static Insets i = null;
    protected static Dimension insetsWithMenu = null;
    protected static Dimension insetsWithoutMenu = null;
    protected static Dimension topLeftWithMenu = null;
    protected static Dimension topLeftWithoutMenu = null;

    public static Dimension get(boolean withMenuFlag) {
        if (i == null) {
            JFrame f = new JFrame("Get size of window borders");
            JMenuBar mb = new JMenuBar();
            f.setJMenuBar(mb);
            mb.add(new JMenu("OK"));
            f.setVisible(true);
            i = f.getInsets();
            f.dispose();
            if (FrameMargins.i.left > 100 || FrameMargins.i.right > 100 || FrameMargins.i.top > 100 || FrameMargins.i.bottom > 100) {
                FrameMargins.i.left = 10;
                FrameMargins.i.right = 10;
                FrameMargins.i.top = 20;
                FrameMargins.i.bottom = 10;
            }
            insetsWithMenu = new Dimension(FrameMargins.i.left + FrameMargins.i.right, FrameMargins.i.top + FrameMargins.i.bottom + mb.getHeight());
            insetsWithoutMenu = new Dimension(FrameMargins.i.left + FrameMargins.i.right, FrameMargins.i.top + FrameMargins.i.bottom);
            topLeftWithoutMenu = new Dimension(FrameMargins.i.left, FrameMargins.i.top);
            topLeftWithMenu = new Dimension(FrameMargins.i.left, FrameMargins.i.top + mb.getHeight());
        }
        return withMenuFlag ? insetsWithMenu : insetsWithoutMenu;
    }

    public static Dimension getOrigin(boolean withMenuFlag) {
        if (i == null) {
            FrameMargins.get(withMenuFlag);
        }
        return withMenuFlag ? topLeftWithMenu : topLeftWithoutMenu;
    }

    public static Insets getFrameInsets() {
        if (i == null) {
            FrameMargins.get(false);
        }
        return i;
    }
}

