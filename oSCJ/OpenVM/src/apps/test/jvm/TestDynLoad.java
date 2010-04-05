package test.jvm;

import java.lang.reflect.*;

/**
 * A simple test of dynamic loading.  Currently, this just loads
 * classes and makes sure that they look OK under the reflection API.
 * It does not execute any dynamically loaded code.
 **/
public class TestDynLoad extends TestBase {
    // bogus constructor for subtypes
    TestDynLoad() { super("dummy", null); }

    static public class A extends TestDynLoad {
	public void m1() { }
	public void m2() { }
    }
    static public class B extends A {
	public void m2() { }
	public void m3() { }
    }

    public TestDynLoad(Harness domain) {
	super("dynnamic loading", domain);
    }

    public void run() {
	check_condition(TestDynLoad.class.isAssignableFrom(A.class),
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