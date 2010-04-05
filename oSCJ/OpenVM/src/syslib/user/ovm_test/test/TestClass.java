/**
 * CORE testsuite.
 **/
package test;
/**
 * Test the java.lang.Class methods.
 **/
public class TestClass extends TestBase {

    public TestClass(Harness domain) {
	super("Class", domain);
    }

    public void run() {
	//p("running TestClass\n");
	// testNames();  // FIXME: this blows up because of me --jv
	// testSynchro();
	// testPrimitives(); -- same
    }
    
    public void testNames() {
	setModule("test Class.getName()");
	testClassName(this.getClass(), "test.userlevel.TestClass");
	testClassName(new String("blah").getClass(), "java.lang.String");
    }


    private void testPrimitives() {
	setModule("test primitives");
	testClassName(int.class, "int");
	testClassName(long.class, "long");
	testClassName(double.class, "double");
	testClassName(char.class, "char");
	testClassName(void.class, "void");
    }

    
    // just check if it doesn't bomb.
    public void testSynchro() {
	synchronized (TestClass.class) {
	}
	synchronized (String.class) {
	}
	synchMethod();
    }

    private static synchronized void synchMethod() {}
    
    private void testClassName(Class c, String clsName) {
	COREassert(c != null, "Class object is null!");
	String s = c.getName();
	COREassert(s != null, "Class name is null!");
	// p("got name " + s + "\n");
	COREassert(s.equals(clsName), 
		   "actual class name " + s + " is not " + clsName);
    }


}
