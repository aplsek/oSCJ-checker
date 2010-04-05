package test.rtjvm;

import javax.realtime.*;
import test.jvm.TestBase;
/**
 * Basic tests using a no-heap real-time thread.
 *
 * The test instance is expected/required to be allocated in immortal memory.
 * @author David Holmes
 */
public class TestNoHeapThread extends TestBase {

    public TestNoHeapThread(Harness domain) {
        super("NoHeapRealtimeThread tests", domain);
    }


    static TestNoHeapThread thisTest;

    public void run() {
        thisTest = this;
        noHeapThreadTest();
    }


    static Object heapRef = null;
    
    static Runnable noHeapRunnable = new Runnable() {
            public void run() {
		boolean readFailed = false;
                try {
                    Object o = heapRef;
                }
                catch (MemoryAccessError mae) {
		    readFailed = true;
                }
		thisTest.check_condition(readFailed, "read heap reference!");

                // now try via reflection
                if (false) {
                    try {
                        java.lang.reflect.Field f = 
                            TestNoHeapThread.class.getDeclaredField("heapRef");
                        Object o = f.get(null);
                        thisTest.check_condition(false, 
                                             "reflection read heap reference");
                    }
                    catch (MemoryAccessError mae) {
                        //System.out.println("Okay - caught MemoryAccessError");
                    }
                    catch (Exception e) {
                        throw (Error) new Error().initCause(e);
                    }
                }

                try {
                    heapRef = null;
                    //System.out.println("Okay set heapRef to null");
                }
                catch (MemoryAccessError mae) {
                    thisTest.check_condition(false, "got MemoryAccessError on write");
                }
                
                try {
                    Object o = heapRef;
                    //System.out.println("Okay - read null reference!");
                }
                catch (MemoryAccessError mae) {
                    thisTest.check_condition(false, "got MemoryAccessError on null read");
                }
            }
        };

    static Runnable setup = new Runnable() {
            public void run() {
                int size = 1024*1024; // big enough for exception be thrown
                MemoryArea ma = new LTMemory(size,size);
                NoHeapRealtimeThread t1 = 
                    new NoHeapRealtimeThread(null,null,null,ma,null,
                                             noHeapRunnable);
                t1.start();
            }
        };


    void noHeapThreadTest() {
        setModule("Basic test reading/writing a field that may refer to a heap object");
        heapRef = new Object();
        ImmortalMemory.instance().executeInArea(setup);
    }

}
