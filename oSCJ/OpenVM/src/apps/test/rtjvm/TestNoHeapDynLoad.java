package test.rtjvm;

import java.lang.reflect.*;
import test.jvm.TestBase;
import test.jvm.TestDynLoad;

/**
 * A simple test of dynamic loading.  Currently, this just loads
 * classes and makes sure that they look OK under the reflection API.
 * It does not execute any dynamically loaded code.
 **/
public class TestNoHeapDynLoad extends TestBase {
    // bogus constructor for subtypes
    TestNoHeapDynLoad() { super("dummy", null); }

    static public class A extends TestNoHeapDynLoad {
	public void m1() { }
	public void m2() { }
    }
    static public class B extends A {
	public void m2() { }
	public void m3() { }
    }

    public TestNoHeapDynLoad(Harness domain) {
	super("NHRT dynnamic loading", domain);
    }

    public void run() {
	// First, make sure that we can examine previously loaded
	// classes
	check_condition(TestDynLoad.class.isAssignableFrom(TestDynLoad.A.class),
			"inherit dynamic < static");
	check_condition(TestDynLoad.A.class.isAssignableFrom(TestDynLoad.B.class),
			"inherit dynamic < dynamic");
	check_condition(!Cloneable.class.isAssignableFrom(TestDynLoad.A.class),
			"none-inherit dynamic < static");
	check_condition(!TestDynLoad.B.class.isAssignableFrom(TestDynLoad.A.class),
			"none-inherit dynamic < dynamic");
	try {
	    Method am1 = TestDynLoad.A.class.getMethod("m1", null);
	    Method am2 = TestDynLoad.A.class.getMethod("m2", null);
	    Method bm1 = TestDynLoad.B.class.getMethod("m1", null);
	    Method bm2 = TestDynLoad.B.class.getMethod("m2", null);
	    Method bm3 = TestDynLoad.B.class.getMethod("m3", null);

	    check_condition(am1 == bm1, "non-override");
	    check_condition(am2 != bm2, "override");
	    check_condition(bm2.getDeclaringClass() == TestDynLoad.B.class,
			    "method declaring class");
	    check_condition(am2.getDeclaringClass() == TestDynLoad.A.class,
			    "method declaring class");
	} catch (NoSuchMethodException e) {
	    fail(e);
	}

	try {
	    Method am3 = TestDynLoad.A.class.getMethod("m3", null);
	    fail("reverse inheritence " + am3);
	} catch (NoSuchMethodException _) {
	}

	// Next, make sure we can load classes in this thread
	check_condition(TestNoHeapDynLoad.class.isAssignableFrom(A.class),
			"inherit dynamic < static");
	check_condition(A.class.isAssignableFrom(B.class),
			"inherit dynamic < dynamic");
	check_condition(!Cloneable.class.isAssignableFrom(A.class),
			"none-inherit dynamic < static");
	check_condition(!B.class.isAssignableFrom(A.class),
			"none-inherit dynamic < dynamic");
	try {
	    Method am1 = A.class.getMethod("m1", null);
	    Method am2 = A.class.getMethod("m2", null);
	    Method bm1 = B.class.getMethod("m1", null);
	    Method bm2 = B.class.getMethod("m2", null);
	    Method bm3 = B.class.getMethod("m3", null);

	    check_condition(am1 == bm1, "non-override");
	    check_condition(am2 != bm2, "override");
	    check_condition(bm2.getDeclaringClass() == B.class,
			    "method declaring class");
	    check_condition(am2.getDeclaringClass() == A.class,
			    "method declaring class");
	} catch (NoSuchMethodException e) {
	    fail(e);
	}

	try {
	    Method am3 = A.class.getMethod("m3", null);
	    fail("reverse inheritence " + am3);
	} catch (NoSuchMethodException _) {
	}
    }
}