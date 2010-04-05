package s3.services.simplejit.bytecode;

import ovm.util.IdentityHashMap;
import ovm.util.Iterator;
import ovm.util.Set;
import ovm.util.Collection;
import ovm.util.List;

/**
 * @author Hiroshi Yamauchi
 */
public class IdentityHashSet {

    public final static IdentityHashSet EMPTY = new IdentityHashSet();

    IdentityHashMap ihashmap;

    public IdentityHashSet() {
        ihashmap = new IdentityHashMap();
    }

    public IdentityHashSet(Object[] elements) {
        ihashmap = new IdentityHashMap();
        for (int i = 0; i < elements.length; i++)
            ihashmap.put(elements[i], Boolean.TRUE);
    }

    public IdentityHashSet(Collection c) {
        ihashmap = new IdentityHashMap();
        for (Iterator it = c.iterator(); it.hasNext(); )
            ihashmap.put(it.next(), Boolean.TRUE);
    }
    
    public String toString() {
        String r = "[ ";
        for (Iterator it = iterator(); it.hasNext();) {
            r += it.next() + " ";
        }
        r += "]";
        return r;
    }

    public void reset() {
        ihashmap = new IdentityHashMap();
    }

    public boolean contains(Object o) {
        return ihashmap.containsKey(o);
    }

    public void remove(Object o) {
        ihashmap.remove(o);
    }

    public boolean empty() {
        return ihashmap.size() == 0;
    }

    public Object remove() {
        Iterator it = ihashmap.keySet().iterator();
        if (it.hasNext()) {
            Object removed = it.next();
            remove(removed);
            return removed;
        } else {
            return null;
        }
    }

    public void addAll(Object[] o) {
        for (int i = 0; i < o.length; i++)
            ihashmap.put(o[i], Boolean.TRUE);
    }

    public void add(Object o) {
        ihashmap.put(o, Boolean.TRUE);
    }

    public void addAll(List l) {
        for (int i = 0; i < l.size(); i++)
            ihashmap.put(l.get(i), Boolean.TRUE);
    }

    public IdentityHashSet copy() {
        IdentityHashSet clone = new IdentityHashSet();
        clone.ihashmap = (IdentityHashMap) this.ihashmap.clone();
        return clone;
    }

    public void union(IdentityHashSet is) {
        IdentityHashMap ihashmap2 = is.ihashmap;
        for (Iterator it = ihashmap2.keySet().iterator(); it.hasNext();) {
            ihashmap.put(it.next(), Boolean.TRUE);
        }
    }

    public void intersection(IdentityHashSet is) {
        IdentityHashMap ihashmap2 = is.ihashmap;
        IdentityHashMap newihashmap = new IdentityHashMap();
        for (Iterator it = ihashmap2.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            if (ihashmap.containsKey(key)) {
                newihashmap.put(key, Boolean.TRUE);
            }
        }
        ihashmap = newihashmap;
    }

    public boolean include(IdentityHashSet is) {
        for (Iterator it = is.ihashmap.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            if (! this.ihashmap.containsKey(key)) {
		return false;
            }
        }
	return true;
    }

    public void diff(IdentityHashSet is) {
        IdentityHashMap ihashmap2 = is.ihashmap;
        for (Iterator it = ihashmap2.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            if (this.ihashmap.containsKey(key)) {
                this.ihashmap.remove(key);
            }
        }
    }

    public boolean equals(IdentityHashSet is) {
        Set keySet1 = this.ihashmap.keySet();
        Set keySet2 = is.ihashmap.keySet();
        return keySet1.equals(keySet2);
    }

    public int size() {
        return ihashmap.size();
    }

    public Iterator iterator() {
        return ihashmap.keySet().iterator();
    }

    public Object[] toArray(Object[] array) {
        int size = ihashmap.size();
        if (array.length < size)
            throw new IllegalArgumentException();
        if (array.length > size)
            array[size] = null;
        int i = 0;
        for (Iterator it = iterator(); it.hasNext();) {
            array[i++] = it.next();
        }
        return array;
    }

}
