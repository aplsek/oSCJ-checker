
package test.common;

import test.common.TestBase;

/**
 * This class is not meant to be testing Ovm execution engines, but
 * for testing abstract intepretation. -HY
 */
public class TestAbstractInterpretation extends TestBase {
    private final long flag;

     static void donothing() {
    }

    private static int add(int a, int b) {
	return a + b; 
    }

    public TestAbstractInterpretation(long flag) {
        super("AbstractInterpretation");
        this.flag = flag;
        flag = this.flag;  // Fool eclipse into thinking we are doing something
    }

    public void run() {
	testLoop();
	testConstantPropagation();
	testSynch();
	testTryCatchFinally();
	testConvergence();
	testDUChains();
	testNestedLoop();
	testInlining();
	testInliningMini0();
	testInliningMini1();
	testInliningLoop();
    }

    private int mul(int i, int j) {
	return i * j;
    }

    private void testInliningMini0() {
	int i = mul(3, 4);
        i = i+1;
    }

    private void testInliningMini1() {
	int i = add(2, 3);
        i = i + 1;  // Fool eclipse into thinking we are doing something
    }

    private void testInliningLoop() {
	int j = 0;
	for(int i = 0; i < 10; i++) {
	    j = add(1, j);
	    for(int k = 0; k < 10; k++) {
		j = add(1, k);
	    }
	}
    }

    private void testInlining() {
	int i = 100;
	int j = add(1, 1);
	if (l() > 0) {
	    j = add(1, j);
	    use(i);
	} else {
	    use(i);
	    j = add(2, j);
	}
    }

    private Object[] arrayfield;
    private void dosomething() {
    }

    private void use(int i) {
    }

    public void testNestedLoop() {
	for(int i = 0; i < 10; i++) {
	    use(i);
	    for(int j = 0; j < 11; j++) {
		use(j);
		for(int k = 0; k < 12; k++) {
		    use(k);
		    if (k > 3) {
			use(i);
		    } else {
			use(j);
		    }
		}
	    }
	}
    }

    public void testWebs() {
	int i = 1;
	int j = 2;
	if (l() > 10) {
	    use(i);
	} else {
	    int k = 4;
	    j = k + 3;
	    i = 3;
	}
	use(i);
    }

    public void testDUChains() {
	int i = 100;
	if (l() > 100) {
	    use(i);
	    i = 120;
	} else {
	    use(i);
	    i = 149;
	}
	use(i);
	if (l() > 101) {
	    use(i);
	} else {
	    i = 1000;
	}
	use(i);
    }

    public void testConstantPropagation() {
	int a = 10;
	int b = 100 + a;
	int c = 1000 + b;
	int d = 1 + c;
    }

    public void testLoop() {
	for(int i = 0; i < 100; i++) {
	    dosomething();
	    if (i == 1000)
		return;
	}
    }

    public void testSynch() {
	synchronized(this) {
	    dosomething();
	    if (false)
		return;
	}
    }

    public void testTryCatchFinally() {
	int i = 100;
	try {
	    double d = 2.0;
	    dosomething();
	    String s = "abc";
	    if (d == 2.0)
		return;
	} catch(Throwable t) {
	    float f = 2.0F;
	    String s = "abc";
	    dosomething();
	    if (f == 2.0F)
		return;
	} finally {
	    dosomething();
	}
    }

    public void testConvergence() {
        if (arrayfield == null) {
            arrayfield = new Object[200];
            arrayfield[0] = this;
            for (int i = 1; i < arrayfield.length; i++) {
                arrayfield[i] = arrayfield[i-1];
            }
        } else {
	    Object[] temp =
		new Object[arrayfield.length + arrayfield.length];
            System.arraycopy(arrayfield, 0, temp, 0, arrayfield.length);
            System.arraycopy(arrayfield, 0, temp, arrayfield.length, 
			     arrayfield.length);
            arrayfield = temp;
        }
    }

    private static class A {
    }
    private static class B extends A {
    }
    private static class C extends A {
    }

    private int l() {
	return 34;
    }
    public void testLUB() {
	A o;
	if (l() > 99) {
	    o = new B();
	} else {
	    o = new C();
	}
	A p;
	if (l() < 99) {
	    p = new B();
	} else {
	    p = new A();
	}
         p = o; o = p; // make the var look useful ;-)
    }
}
