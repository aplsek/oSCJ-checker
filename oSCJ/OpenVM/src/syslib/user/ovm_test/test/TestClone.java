/**
 **/
package test;


/**
 * Test basic allocation methods.
 * @author Christian Grothoff
 **/
public class TestClone 
    extends TestBase {

 
    public TestClone(Harness domain) {
	super("Clone",domain);
    }

    /**
     * This runs a series of tests to see if allocation works. 
     **/ 
    public void run() {
	testObject();
	testPrimitiveArray();
	testReferenceArray();
	testMultiPrimitiveArray();
	testMultiReferenceArray();
    }

    private void testObject() {
	setModule("object cloning");
	A a = new A();
	a.i = 4;
	a.f = 0.4f;
	a.s = 4;
	a.l = 3L; 
	a.bo = true;
	a.b = 1;
	a.d = 3.0;
	A c = null;
	try {
	    c = a.cloneThis();
	} catch (CloneNotSupportedException e) {
	    COREfail(e.toString());
	}
	COREassert(c != a);
	COREassert(c.i == 4);
	COREassert(c.f == 0.4f);
	COREassert(c.s == 4);
	COREassert(c.l == 3L);
	COREassert(c.bo == true);
	COREassert(c.b == 1);
	COREassert(c.d == 3.0);
    }

    public void testPrimitiveArray() {
	setModule("primitive array cloning");
	int[] a = new int[4];
	for(int i = 0; i < a.length; i++) {
	    a[i] = i;
	}
	int[] c = (int[])a.clone();
	COREassert(c != a, "not distinct");
	COREassert(c.length == a.length, "length mismatch");
	for(int i = 0; i < a.length; i++) {
	    COREassert(a[i] == c[i], "content mismatch");
	}
    }

    public void testReferenceArray() {
	setModule("reference array cloning");
	String[] a = new String[4];
	for(int i = 0; i < a.length; i++) {
	    a[i] = "str" + i;
	}
	String[] c = (String[])a.clone();
	COREassert(c != a, "not distinct");
	COREassert(c.length == a.length, "length mismatch");
	for(int i = 0; i < a.length; i++) {
	    COREassert(a[i] == c[i], "content mismatch");
	}
    }

    public void testMultiPrimitiveArray() {
	setModule("multi primitive array cloning");
	int[][] a = new int[4][4];
	for(int i = 0; i < a.length; i++) {
	    for(int j = 0; j < a[i].length; j++) {
		a[i][j] = i * j;
	    }
	}
	int[][] c = (int[][])a.clone();
	COREassert(c != a, "not distinct");
	COREassert(c[0] == a[0], "not shallow"); // means a shallow copy
	COREassert(c.length == a.length, "length mismatch");
	for(int i = 0; i < a.length; i++) {
	    COREassert(a[i].length == c[i].length, "length mismatch");
	    for(int j = 0; j < a[i].length; j++) {
		COREassert(a[i][j] == c[i][j], "content mismatch");
	    }
	}
    }

    public void testMultiReferenceArray() {
	setModule("multi reference array cloning");
	String[][] a = new String[4][4];
	for(int i = 0; i < a.length; i++) {
	    for(int j = 0; j < a[i].length; j++) {
		a[i][j] = "str" + i * j;
	    }
	}
	String[][] c = (String[][])a.clone();
	COREassert(c != a, "not distinct");
	COREassert(c[0] == a[0], "not shallow"); // means a shallow copy
	COREassert(c.length == a.length, "length mismatch");
	for(int i = 0; i < a.length; i++) {
	    COREassert(a[i].length == c[i].length, "length mismatch");
	    for(int j = 0; j < a[i].length; j++) {
		COREassert(a[i][j] == c[i][j], "content mismatch");
	    }
	}
    }

    /* ************** helper classes ********************** */

    static class A implements Cloneable {
	boolean bo;
	int i;
	long l;
	double d;
	float f;
	short s;
	byte b;
	public A cloneThis() throws CloneNotSupportedException {
	    return (A)clone();
	}
    }

    // empty class (minimal size)
    static class B {
    }

    static class E 
	extends A {
	int i;
    }

    // this would pull userlevel into the executive domain
//     public static void main(String[] args) {
// 	new test.userlevel.TestSuite(~TestSuite.DISABLE_CLONE).run();
//     }


} // end of TestClone
