package test;

public class TestInitializers extends TestBase {
    boolean doThrow;

    public TestInitializers(Harness domain, long disabled) {
	super("Initializers", domain);
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }
    
    static class ThrowError {
	static {
	    if (true)
		throw new Error("hmm");
	}
	static int field = 0;
    } 

    void testErrorInInitializer() {
	try {
	    ThrowError.field = 2;
	    COREassert(false, "not reached");
	} catch (Error e) {
	     //p("--- OK: caught error from initializer "  + e + "\n");
	}

    }


    static class ThrowRuntimeException {
	static {
	    if (true)
		throw new RuntimeException("hmm");
	}
	static int field = 0;
    } 

    void testRuntimeExceptionInInitializer() {
	try {
	    ThrowRuntimeException.field = 2;
	    COREassert(false, "not reached");
	} catch (ExceptionInInitializerError e) {
	   //  p("--- OK: caught runtime exception from initializer " + e + "\n");
            // p("    Original cause: " + e.getCause() + "\n");
	}

    }
    
    static class A {
	static {
	    field = 10;
	}
	static int field;
    }


    static class B extends A {
	static {
	    field = 9;
	    if (true) 
		throw new RuntimeException();
	    otherfield = 8;
	}
	static int field;
	static int otherfield;
    }

    static class C extends B {
	static {
	    field = 7;
	}
	static int field;
	static int otherfield;
    }


    void testRTInHierarchy() {
	try {
	    C.otherfield = 7;
	    COREassert(false, "Shouldn't be able to access C");
	} catch (ExceptionInInitializerError e) {
            // B is in an erroneous state and so is C
            // if we access A directly then it will initialize ok
	    COREassert(A.field == 10, "A not initialized");

            try {
                COREassert(B.field == 9, "Shouldn't be able to access B");
            }
            catch(NoClassDefFoundError e2) {
              //  p("--- OK: accessing B caught:" + e2 + "\n");
            }
            try {
                COREassert(C.field == 0, "Shouldn't be able to access C");
            }
            catch(NoClassDefFoundError e2) {
                //p("--- OK: accessing B caught:" + e2 + "\n");
            }
	}
    }

    public void run() {
	if (doThrow) {
	    testErrorInInitializer();
	    testRuntimeExceptionInInitializer();
	    testRTInHierarchy();
	}
	// missing tests on initialization order
    }



}
