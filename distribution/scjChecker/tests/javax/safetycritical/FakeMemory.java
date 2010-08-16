package javax.safetycritical;

import java.util.LinkedList;
import java.util.List;

import javax.realtime.MyMemory;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SuppressSCJ;

/**
 * 
 * tests/javax/safetycritical/FakeMemory.java:26: warning: Illegal method call of an SCJ method.
        super(size);
             ^
 * 
 * 
 * @author plsek
 *
 */
@SCJAllowed
public class FakeMemory extends MyMemory {

    @SCJAllowed
    public FakeMemory(long size) {
        super(size);
        // TODO Auto-generated constructor stub
    }

    @SCJAllowed(Level.LEVEL_1)
    @SuppressSCJ
    public void enter(Runnable logic) {
        
        if (true) {
            // do something
        }
        
        //super.enter(logic);
    }
    
    @SCJAllowed(Level.LEVEL_1)
    public void foo() {      
    }
    
    // This does not work in a real SCJ environment (memory issue)
    // However, in Java SE we don't care on memory - the GC will do it.
    @SuppressSCJ
    @SCJAllowed
    List<ManagedEventHandler> handlers = new LinkedList<ManagedEventHandler>();
    
    
    
    
}
