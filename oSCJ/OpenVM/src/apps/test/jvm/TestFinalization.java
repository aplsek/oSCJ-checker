package test.jvm;

public class TestFinalization extends TestBase {
    public TestFinalization(Harness domain) {
	super("finalizer tests", domain);
    }

    static int created = 0;
    static volatile int destroyed = 0;
    
    static class Finalizable {
	public Finalizable() {
	    created++;
	}

	public void finalize() {
	    destroyed++;
	}
    }

    void runReflectively() {
	int lastDestroyed = destroyed;
	boolean done = false;
	while (!done) {
	    while (lastDestroyed == destroyed) {
		try {
		    Finalizable.class.newInstance();
		    new Finalizable();
		} catch (InstantiationException _) {
		} catch (IllegalAccessException _) {
		}
		byte[] _ = new byte[4096];
	    }
	    done = lastDestroyed != 0;
	    lastDestroyed = destroyed;
	    System.out.println("created " + created + " objects, " +
			       "finalized " + destroyed);
	}
	System.gc();
	System.out.println("created " + created + " objects, " +
			   "finalized " + destroyed);
    }
	
    public void run() {
	Runtime r = Runtime.getRuntime();
	check_condition (r.freeMemory() < r.totalMemory()
			 && r.totalMemory() < r.maxMemory(),
			 "Runtime.freeMemory and friends broken");
	if (System.getProperty("org.ovmj.supportsGC", "false").equals("true")
	    && System.getProperty("org.ovmj.supportsFinalizers",
				  "false").equals("true"))
	    runReflectively();
    }
}
