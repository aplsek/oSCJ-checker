/**
 **/
package test.jvm;

/**
 * Test basic allocation methods.
 * @author Christian Grothoff
 * @author Filip Pizlo
 **/
public class TestAllocation 
    extends TestBase {

 
    public TestAllocation(Harness domain) {
	super("Allocation", domain);
    }

    /**
     * This runs a series of tests to see if allocation works. 
     **/ 
    public void run() {
	testObjectAllocation();
        testNegativeSize();
	testPrimitiveArrays();
	testMultiArray();
	testGC();
    }

    public void testObjectAllocation() {
	setModule("object allocation");
	A a1 = new A();
	A a2 = new A();
	a1.i = 4;
	check_condition( (a1.i == 4) &&
		   (a2.i == 0) );	
	a2.f = 0.4f;
	a2.s = 4;
	a2.l = 0L; 
	check_condition( (a2.f == 0.4f) &&
		   (a2.l == 0L) &&
		   (a2.s == 4) );
	// minimal sized object
	B b1 = new B();
	B b2 = new B();
	check_condition(b1 != b2);

	// inheritance and different field with identical name and type
	// in parent class
	E e1 = new E();
	e1.i = 4;
	A ae = e1;
	ae.i = 5;
	check_condition( (e1.i == 4) &&
		   (ae.i == 5) &&
		   (e1.b == 0) );
    }

    public void testNegativeSize() {
	setModule("negative size attempts");        
        try {
            int[] ia = new int[-1];
            check_condition(false, "negative sized array created (1)");
        }
        catch(NegativeArraySizeException t) {
        }

        try {
            int[][] ia = new int[1][-1];
            check_condition(false, "negative sized array created (2)");
        }
        catch(NegativeArraySizeException t) {
        }

        try {
            int[][] ia = new int[-1][1];
            check_condition(false, "negative sized array created (3)");
        }
        catch(NegativeArraySizeException t) {
        }

        try {
            int[][][] ia = new int[-1][1][2];
            check_condition(false, "negative sized array created (4)");
        }
        catch(NegativeArraySizeException t) {
        }

        try {
            int[][][] ia = new int[1][-1][2];
            check_condition(false, "negative sized array created (5)");
        }
        catch(NegativeArraySizeException t) {
        }

        try {
            int[][][] ia = new int[1][1][-2];
            check_condition(false, "negative sized array created (6)");
        }
        catch(NegativeArraySizeException t) {
        }
    }

    public void testPrimitiveArrays() {
	setModule("primitive array allocation");
	int[] ia1 = new int[4];
	int[] ia2 = new int[5];
	check_condition(ia1.length == 4);
	for (int i=0;i<ia1.length;i++) {
	    check_condition(ia1[i] == 0);
	    ia1[i] = i+1;
	}
	for (int i=0;i<ia2.length;i++) {
	    check_condition(ia2[i] == 0);
	    ia2[i] = i-1;
	}
	check_condition(ia1.length == 4);
	check_condition(ia2.length == 5);
	for (int i=0;i<ia1.length;i++) 
	    check_condition(ia1[i] == i+1);
	for (int i=0;i<ia2.length;i++) 
	    check_condition(ia2[i] == i-1);	

	long[] la1 = new long[4];
	long[] la2 = new long[5];
	check_condition(la1.length == 4);
	for (int i=0;i<la1.length;i++) {
	    check_condition(la1[i] == 0);
	    la1[i] = 0x1FFFFFFFFl + i+1;
	}
	for (int i=0;i<la2.length;i++) {
	    check_condition(la2[i] == 0);
	    la2[i] = i-0x1FFFFFFFFl;
	}
	check_condition(la1.length == 4);
	check_condition(la2.length == 5);
	for (int i=0;i<la1.length;i++) 
	    check_condition(la1[i] == 0x1FFFFFFFFl + i+1);
	for (int i=0;i<la2.length;i++) 
	    check_condition(la2[i] == i-0x1FFFFFFFFl);	
    }

    /**
     * Multidimensional array allocation
     **/
    public void testMultiArray() {
	setModule("multianewarray");
	int [][][] testArray = new int[4][3][5];
	check_condition(testArray.length == 4);
	for (int i = 0;i<4;i++) {
	    check_condition(testArray[i].length==3);
	    for (int j = 0;j<3;j++) {
		check_condition(testArray[i][j].length==5);
		for (int k = 0; k < 5; k ++) {
		    check_condition(testArray[i][j][k] == 0);
		}
	    }
					  
	}	
	int[] t2 = new int[5];
	t2[1] = 42;
	testArray[1][1] = t2;
	check_condition(testArray[1][1][1] == 42);

	B[][][] tca = new B[5][3][];
	check_condition(tca.length==5);
	for (int i = 0;i<5;i++) {
	    check_condition(tca[i].length==3);
	    for (int j = 0;j<3;j++) 
		check_condition(tca[i][j] == null);
	}
    }

    public static Integer[] array;

    public void testGC() {
        setModule("GC");
        Integer testarray[] = new Integer[10];
        array = testarray;
        for (int i = 0; i < 10; i++) 
            testarray[i] = new Integer(i);
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 10; j++) {
		check_condition(testarray[j] != null, "testarray entry null\n");
		check_condition(testarray[j].intValue() == j,
				"testarray entry corrupt");
	    }
            Integer[] _ = new Integer[100000];
            _[0] = _[0]; // Fool eclipse 
	    try {
		// give time-based GC the time it needs to do its work!
		Thread.sleep(10);
	    } catch (InterruptedException e) {
		fail(e);
	    }
        }
    }

    /* ************** helper classes ********************** */

    static class A {
	boolean bo;
	int i;
	long l;
	double d;
	float f;
	short s;
	byte b;
    }

    // empty class (minimal size)
    static class B {
    }

    static class E 
	extends A {
	int i;
    }


} // end of TestAllocation
