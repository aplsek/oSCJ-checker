package java.util;

import static javax.safetycritical.annotate.Scope.CALLER;

import java.util.Iterator;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public interface List {

    @SCJAllowed
    public Iterator iterator();
    
    @SCJAllowed
    public int size();
    
    @SCJAllowed
    boolean isEmpty();
    
    @SCJAllowed
    boolean add(Object e);
    
    @SCJAllowed
    public Object get(int i);

    @SCJAllowed
    public Object[] toArray(Object[] a);
    
    @SCJAllowed
    @RunsIn(CALLER)
    public boolean equals(Object o);
}
