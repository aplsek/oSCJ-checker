package test.common;

import test.common.TestBase;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;

/**
 * Test basic allocation methods.
 * @author Christian Grothoff
 */
public class TestAllocation extends TestBase {
    private final long flag;

    public TestAllocation(long flag) {
        super("Allocation");
        this.flag = flag;
    }

    /**
     * This runs a series of tests to see if allocation works.
     */
    public void run() {
        testObjectAllocation();
        testPrimitiveArrays();
        testMultiArray();
        if ((flag & TestSuite.DISABLE_GC) == 0 &&
	    LibraryImports.canCollect())  testGC();
    }

    public void testObjectAllocation() {
        setModule("object allocation");
        A a1 = new A();
        A a2 = new A();
        a1.i = 4;
        check_condition((a1.i == 4) && (a2.i == 0));
        a2.f = 0.4f;
        a2.s = 4;
        a2.l = 0L;
        check_condition((a2.f == 0.4f) && (a2.l == 0L) && (a2.s == 4));
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
        check_condition((e1.i == 4) && (ae.i == 5) && (e1.b == 0));
    }

    public void testPrimitiveArrays() {
        setModule("primitive array allocation");

        int[] ia1 = new int[4];
        int[] ia2 = new int[5];
        check_condition(ia1.length == 4);
        for (int i = 0; i < ia1.length; i++) {
            check_condition(ia1[i] == 0);
            ia1[i] = i + 1;
        }
        for (int i = 0; i < ia2.length; i++) {
            check_condition(ia2[i] == 0);
            ia2[i] = i - 1;
        }
        check_condition(ia1.length == 4);
        check_condition(ia2.length == 5);
        for (int i = 0; i < ia1.length; i++)
            check_condition(ia1[i] == i + 1);
        for (int i = 0; i < ia2.length; i++)
            check_condition(ia2[i] == i - 1);

        long[] la1 = new long[4];
        long[] la2 = new long[5];
        check_condition(la1.length == 4);
        for (int i = 0; i < la1.length; i++) {
            check_condition(la1[i] == 0);
            la1[i] = 0x1FFFFFFFFl + i + 1;
        }
        for (int i = 0; i < la2.length; i++) {
            check_condition(la2[i] == 0);
            la2[i] = i - 0x1FFFFFFFFl;
        }
        check_condition(la1.length == 4);
        check_condition(la2.length == 5);
        for (int i = 0; i < la1.length; i++)
            check_condition(la1[i] == 0x1FFFFFFFFl + i + 1);
        for (int i = 0; i < la2.length; i++)
            check_condition(la2[i] == i - 0x1FFFFFFFFl);
    }

    /**
     * Multidimensional array allocation
     */
    public void testMultiArray() {
        setModule("multianewarray");
        int[][][] testArray = new int[4][3][5];
        check_condition(testArray.length == 4);
        for (int i = 0; i < 4; i++) {
            check_condition(testArray[i].length == 3);
            for (int j = 0; j < 3; j++) {
                check_condition(testArray[i][j].length == 5);
                for (int k = 0; k < 5; k++) {
                    check_condition(testArray[i][j][k] == 0);
                }
            }

        }
        int[] t2 = new int[5];
        t2[1] = 42;
        testArray[1][1] = t2;
        check_condition(testArray[1][1][1] == 42);

        B[][][] tca = new B[5][3][];
        check_condition(tca.length == 5);
        for (int i = 0; i < 5; i++) {
            check_condition(tca[i].length == 3);
            for (int j = 0; j < 3; j++)
                check_condition(tca[i][j] == null);
        }
    }

    private VM_Address getDisplacedPointer() {
	Integer secret = null;
	// Assuming a 20 byte Integer and 4096 byte block size, 204
	// Integers can be allocated on each block.  We need to make
	// sure that there are no stack references to the same block
	// as our secrete value in order to get a meaningful test.
	for (int i = 0; i < 500; i++) {
	    Integer wrapped = new Integer(i);
	    if (i == 250)
		secret = wrapped;
	}
	MemoryManager.the().pin(secret);
	return VM_Address.fromObject(secret).sub(4096);
    }

    private void checkDisplacedPointer(VM_Address secret) {
	if (secret != null) {
	    Object o = secret.add(4096).asObject();
	    check_condition(o instanceof Integer, "pinned object type");
	    check_condition(((Integer) o).intValue() == 250,
			    "pinned object value");
	}
    }
	    
    public static Integer[] array;

    public void testGC() {
        setModule("GC");
	VM_Address secret = getDisplacedPointer();
        Integer testarray[] = new Integer[10];
        array = testarray;
        for (int i = 0; i < 10; i++) 
            testarray[i] = new Integer(i);
        for (int i = 0; i < 200; i++) {
	    checkDisplacedPointer(secret);
            for (int j = 0; j < 10; j++) {
		check_condition(testarray[j] != null, "testarray entry null\n");
		check_condition(testarray[j].intValue() == j,
				"testarray entry corrupt");
	    }
            Integer[] _ = new Integer[100000];
            _[0] = _[0]; // Fool eclipse 
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
    static class E extends A {
        int i;
    }
} // end of TestAllocation
