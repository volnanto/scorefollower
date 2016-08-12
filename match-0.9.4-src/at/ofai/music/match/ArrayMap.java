package at.ofai.music.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class ArrayMap
implements Map<String, Object> {
    protected ArrayList<Entry> entries = new ArrayList();

    public ArrayMap() {
    }

    public ArrayMap(Map<String, Object> m) {
        this();
        this.putAll(m);
    }

    public int indexOf(String key) {
        int i = 0;
        while (i < this.size()) {
            if (key.equals(this.entries.get((int)i).key)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public Entry getEntry(int i) {
        return this.entries.get(i);
    }

    @Override
    public void clear() {
        this.entries.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        if (this.indexOf((String)key) >= 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        int i = 0;
        while (i < this.size()) {
            if (value.equals(this.entries.get((int)i).value)) {
                return true;
            }
            ++i;
        }
        return false;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        TreeSet<Map.Entry<String, Object>> s = new TreeSet<Map.Entry<String, Object>>();
        int i = 0;
        while (i < this.size()) {
            s.add(this.entries.get(i));
            ++i;
        }
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        int i = this.indexOf((String)key);
        if (i == -1) {
            return null;
        }
        return this.entries.get((int)i).value;
    }

    @Override
    public int hashCode() {
        int h = 0;
        int i = 0;
        while (i < this.size()) {
            h ^= this.entries.get(i).hashCode();
            ++i;
        }
        return h;
    }

    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        TreeSet<String> s = new TreeSet<String>();
        int i = 0;
        while (i < this.size()) {
            s.add(this.entries.get((int)i).key);
            ++i;
        }
        return s;
    }

    @Override
    public Object put(String key, Object value) {
        int i = this.indexOf(key);
        if (i < 0) {
            this.entries.add(new Entry(key, value));
            return null;
        }
        return this.entries.get(i).setValue(value);
    }

    @Override
   public void putAll(Map m) {
        Map m1 = m;
      //  for (Map.Entry me : m1.entrySet()) {
       //     this.put((String)me.getKey(), me.getValue());
        }

    @Override
    public Object remove(Object key) {
        int i = this.indexOf((String)key);
        if (i < 0) {
            return null;
        }
        return this.entries.remove(i);
    }

    @Override
    public int size() {
        return this.entries.size();
    }

    @Override
    public Collection<Object> values() {
        ArrayList<Object> s = new ArrayList<Object>();
        int i = 0;
        while (i < this.size()) {
            s.add(this.entries.get((int)i).value);
            ++i;
        }
        return s;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    protected class Entry
    implements Map.Entry<String, Object>,
    Comparable<Object> {
        protected String key;
        protected Object value;

        protected Entry(String k, Object v) {
            this.key = k;
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Entry && this.key.equals(((Entry)o).key) && this.value.equals(((Entry)o).value)) {
                return true;
            }
            return false;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        @Override
        public Object setValue(Object newValue) {
            Object oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }

        @Override
        public int hashCode() {
            return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value == null ? 0 : this.value.hashCode());
        }

        @Override
        public int compareTo(Object o) {
            return this.key.compareTo(((Entry)o).key);
        }
    }

}

