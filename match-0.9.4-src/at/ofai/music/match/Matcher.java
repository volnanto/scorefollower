package at.ofai.music.match;

public class Matcher {
    protected String s;

    public Matcher(String data) {
        this.s = data;
    }

    public void set(String data) {
        this.s = data;
    }

    public String get() {
        return this.s;
    }

    public boolean hasData() {
        if (this.s != null && this.s.length() > 0) {
            return true;
        }
        return false;
    }

    public boolean matchString(String m) {
        if (this.s.startsWith(m)) {
            this.s = this.s.substring(m.length());
            return true;
        }
        return false;
    }

    public void skip(char c) {
        int index = this.s.indexOf(c);
        if (index < 0) {
            throw new RuntimeException("Parse error in skip(), expecting " + c);
        }
        this.s = this.s.substring(index + 1);
    }

    public void trimSpace() {
        this.s = this.s.trim();
    }

    public char getChar() {
        char c = this.s.charAt(0);
        this.s = this.s.substring(1);
        return c;
    }

    public int getInt() {
        int sz = 0;
        this.trimSpace();
        while (sz < this.s.length() && (Character.isDigit(this.s.charAt(sz)) || sz == 0 && this.s.charAt(sz) == '-')) {
            ++sz;
        }
        int val = Integer.parseInt(this.s.substring(0, sz));
        this.s = this.s.substring(sz);
        return val;
    }

    public double getDouble() {
        int sz = 0;
        this.trimSpace();
        while (sz < this.s.length() && (Character.isDigit(this.s.charAt(sz)) || sz == 0 && this.s.charAt(sz) == '-' || this.s.charAt(sz) == '.')) {
            ++sz;
        }
        double val = Double.parseDouble(this.s.substring(0, sz));
        this.s = this.s.substring(sz);
        return val;
    }

    public String getString() {
        return this.getString(false);
    }

    public String getString(boolean extraPunctuation) {
        char[] stoppers = new char[]{'(', '[', '{', ',', '}', ']', ')', '-', '.'};
        int index1 = this.s.indexOf(stoppers[0]);
        int i = 1;
        while (i < stoppers.length - (extraPunctuation ? 0 : 2)) {
            int index2 = this.s.indexOf(stoppers[i]);
            if (index1 >= 0) {
                if (index2 >= 0 && index1 > index2) {
                    index1 = index2;
                }
            } else {
                index1 = index2;
            }
            ++i;
        }
        if (index1 < 0) {
            throw new RuntimeException("getString(): no terminator: " + this.s);
        }
        String val = this.s.substring(0, index1);
        this.s = this.s.substring(index1);
        return val;
    }

    public ListTerm getList() {
        if ("([{".indexOf(this.s.charAt(0)) >= 0) {
            return new ListTerm(this.getChar());
        }
        return null;
    }

    public Predicate getPredicate() {
        return new Predicate();
    }

    class Predicate {
        String head;
        ListTerm args;

        protected Predicate() {
            this.head = Matcher.this.getString(true);
            this.args = Matcher.this.getList();
        }

        public Object arg(int index) {
            ListTerm t = this.args;
            int i = 0;
            while (i < index) {
                t = t.next;
                ++i;
            }
            return t.term;
        }

        public String toString() {
            return this.args == null ? this.head : String.valueOf(this.head) + this.args;
        }
    }

    class ListTerm {
        Object term;
        ListTerm next;
        char opener;
        char closer;

        protected ListTerm(char c) {
            this.opener = c;
            this.term = null;
            this.next = null;
            if (Matcher.this.hasData()) {
                switch (Matcher.this.s.charAt(0)) {
                    case '(': 
                    case '[': 
                    case '{': {
                        this.term = new ListTerm(Matcher.this.getChar());
                        break;
                    }
                    default: {
                        this.term = Matcher.this.getString();
                    }
                }
            }
            if (Matcher.this.hasData()) {
                this.closer = Matcher.this.getChar();
                switch (this.closer) {
                    case ')': {
                        if (this.opener != '(') break;
                        return;
                    }
                    case ']': {
                        if (this.opener != '[') break;
                        return;
                    }
                    case '}': {
                        if (this.opener != '{') break;
                        return;
                    }
                    case ',': {
                        this.next = new ListTerm(this.opener);
                        return;
                    }
                }
            }
            throw new RuntimeException("Parse error in ListTerm(): " + Matcher.this.s);
        }

        public String toString() {
            String s = "" + this.opener;
            ListTerm ptr = this;
            while (ptr != null) {
                s = String.valueOf(s) + ptr.term.toString() + ptr.closer;
                ptr = ptr.next;
            }
            return s;
        }
    }

}

