/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import at.ofai.music.match.ArrayMap;
import at.ofai.music.match.Colors;
import at.ofai.music.match.FrameMargins;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class Parameters
extends JDialog
implements ActionListener {
    protected ArrayMap map;
    protected Frame parent;
    protected JLabel[] keyFields;
    protected JComponent[] valueFields;
    protected int sz;
    protected Colors colors;
    protected JPanel panel1;
    protected JPanel panel2;
    protected JButton okButton;
    protected JButton cancelButton;
    protected boolean cancelled;
    static final long serialVersionUID = 0;

    public Parameters(Frame f, String name) {
        this(f, name, new Colors(){

            public Color getBackground() {
                return Color.white;
            }

            public Color getForeground() {
                return Color.black;
            }

            public Color getButton() {
                return Color.white;
            }

            public Color getButtonText() {
                return Color.black;
            }
        });
    }

    public Parameters(Frame f, String name, Colors c) {
        super(f, name, true);
        this.colors = c;
        this.setLocationRelativeTo(f);
        Container pane = this.getContentPane();
        pane.setLayout(new BoxLayout(pane, 0));
        this.panel1 = new JPanel();
        this.panel2 = new JPanel();
        pane.add(this.panel1);
        pane.add(this.panel2);
        this.panel1.setBackground(this.colors.getBackground());
        this.panel2.setBackground(this.colors.getBackground());
        this.getRootPane().setBorder(BorderFactory.createLineBorder(this.colors.getBackground(), 10));
        this.map = new ArrayMap();
        this.okButton = new JButton("OK");
        this.okButton.setBackground(this.colors.getButton());
        this.okButton.setForeground(this.colors.getButtonText());
        this.okButton.addActionListener(this);
        this.cancelButton = new JButton("Cancel");
        this.cancelButton.setBackground(this.colors.getButton());
        this.cancelButton.setForeground(this.colors.getButtonText());
        this.cancelButton.addActionListener(this);
        this.parent = f;
        this.cancelled = false;
        this.setVisible(false);
    }

    public void print() {
        this.sz = this.map.size();
        System.out.println("at.ofai.music.util.Parameters: size = " + this.sz);
        int i = 0;
        while (i < this.sz) {
            ArrayMap.Entry e = this.map.getEntry(i);
            System.out.println(String.valueOf(e.getKey()) + " : " + e.getValue());
            ++i;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.okButton) {
            int i = 0;
            while (i < this.sz) {
                ((Value)this.map.getEntry(i).getValue()).update();
                ++i;
            }
            this.cancelled = false;
        } else {
            this.cancelled = true;
        }
        this.setVisible(false);
    }

    public boolean wasCancelled() {
        return this.cancelled;
    }

    public void setVisible(boolean flag) {
        if (!flag) {
            super.setVisible(false);
            return;
        }
        this.sz = this.map.size();
        this.keyFields = new JLabel[this.sz];
        this.valueFields = new JComponent[this.sz];
        this.panel1.removeAll();
        this.panel2.removeAll();
        this.panel1.setLayout(new GridLayout(this.sz + 1, 1, 10, 5));
        this.panel2.setLayout(new GridLayout(this.sz + 1, 1, 10, 5));
        int i = 0;
        while (i < this.sz) {
            ArrayMap.Entry e = this.map.getEntry(i);
            this.keyFields[i] = new JLabel(e.getKey());
            this.panel1.add(this.keyFields[i]);
            this.valueFields[i] = ((Value)e.getValue()).component;
            this.panel2.add(this.valueFields[i]);
            ++i;
        }
        this.panel1.add(this.okButton);
        this.panel2.add(this.cancelButton);
        this.pack();
        Dimension dim = this.getContentPane().getSize();
        Dimension margins = FrameMargins.get(false);
        int wd = dim.width + margins.width + 20;
        int ht = dim.height + margins.height + 20;
        int x = 0;
        int y = 0;
        if (this.parent != null) {
            x = this.parent.getLocation().x + (this.parent.getWidth() - wd) / 2;
            y = this.parent.getLocation().y + (this.parent.getHeight() - ht) / 2;
        }
        super.setLocation(x, y);
        super.setSize(wd, ht);
        super.setVisible(true);
    }

    public boolean contains(String key) {
        return this.map.containsKey(key);
    }

    public String getString(String key) {
        return ((StringValue)this.map.get((Object)key)).currentValue;
    }

    public double getDouble(String key) {
        return ((DoubleValue)this.map.get((Object)key)).currentValue;
    }

    public int getInt(String key) {
        return ((IntegerValue)this.map.get((Object)key)).currentValue;
    }

    public boolean getBoolean(String key) {
        return ((BooleanValue)this.map.get((Object)key)).currentValue;
    }

    public String getChoice(String key) {
        return (String)((ChoiceValue)this.map.get(key)).getValue();
    }

    public void setString(String key, String value) {
        this.map.put(key, (Object)new StringValue(value));
    }

    public void setDouble(String key, double value) {
        this.map.put(key, (Object)new DoubleValue(value));
    }

    public void setInt(String key, int value) {
        this.map.put(key, (Object)new IntegerValue(value));
    }

    public void setBoolean(String key, boolean value) {
        this.map.put(key, (Object)new BooleanValue(value));
    }

    public void setChoice(String key, String[] choices, int value) {
        this.map.put(key, (Object)new ChoiceValue(choices, value));
    }

    abstract class Value {
        protected JComponent component;

        Value() {
        }

        protected abstract Object getValue();

        protected abstract void update();
    }

    class ChoiceValue
    extends Value {
        String[] choices;
        int currentChoice;

        protected ChoiceValue(String[] values) {
            this(values, 0);
        }

        protected ChoiceValue(String[] values, int init) {
            this.choices = values;
            this.currentChoice = init;
            this.component = new JComboBox<String>(values);
            ((JComboBox)this.component).setSelectedIndex(this.currentChoice);
            this.component.setBackground(Parameters.this.colors.getBackground());
            this.component.setForeground(Parameters.this.colors.getForeground());
        }

        protected Object getValue() {
            return this.choices[this.currentChoice];
        }

        public String toString() {
            return this.choices[this.currentChoice];
        }

        protected void update() {
            int tmp = ((JComboBox)this.component).getSelectedIndex();
            if (tmp >= 0) {
                this.currentChoice = tmp;
            }
        }
    }

    class StringValue
    extends Value {
        String currentValue;

        protected StringValue() {
            this("");
        }

        protected StringValue(String init) {
            this.currentValue = init;
            this.component = new JTextField(this.currentValue);
            this.component.setBackground(Parameters.this.colors.getBackground());
            this.component.setForeground(Parameters.this.colors.getForeground());
        }

        protected Object getValue() {
            return this.currentValue;
        }

        public String toString() {
            return this.currentValue;
        }

        protected void update() {
            this.currentValue = ((JTextField)this.component).getText();
        }
    }

    class DoubleValue
    extends Value {
        double currentValue;

        protected DoubleValue() {
            this(0.0);
        }

        protected DoubleValue(double init) {
            this.currentValue = init;
            this.component = new JTextField(Double.toString(this.currentValue));
            this.component.setBackground(Parameters.this.colors.getBackground());
            this.component.setForeground(Parameters.this.colors.getForeground());
        }

        protected Object getValue() {
            return new Double(this.currentValue);
        }

        public String toString() {
            return "" + this.currentValue;
        }

        protected void update() {
            try {
                double tmp;
                this.currentValue = tmp = Double.parseDouble(((JTextField)this.component).getText());
            }
            catch (NumberFormatException tmp) {
                // empty catch block
            }
        }
    }

    class IntegerValue
    extends Value {
        int currentValue;

        protected IntegerValue() {
            this(0);
        }

        protected IntegerValue(int init) {
            this.currentValue = init;
            this.component = new JTextField(Integer.toString(this.currentValue));
            this.component.setBackground(Parameters.this.colors.getBackground());
            this.component.setForeground(Parameters.this.colors.getForeground());
        }

        protected Object getValue() {
            return new Integer(this.currentValue);
        }

        public String toString() {
            return "" + this.currentValue;
        }

        protected void update() {
            try {
                int tmp;
                this.currentValue = tmp = Integer.parseInt(((JTextField)this.component).getText());
            }
            catch (NumberFormatException tmp) {
                // empty catch block
            }
        }
    }

    class BooleanValue
    extends ChoiceValue {
        boolean currentValue;

        protected BooleanValue() {
            this(true);
        }

        protected BooleanValue(boolean init) {
            super(new String[]{"True", "False"}, init ? 0 : 1);
            this.currentValue = init;
        }

        protected Object getValue() {
            return new Boolean(this.currentValue);
        }

        public String toString() {
            return "" + this.currentValue;
        }

        protected void update() {
            super.update();
            this.currentValue = this.currentChoice == 0;
        }
    }

}

