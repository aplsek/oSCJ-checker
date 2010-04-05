package test.runtime;

import ovm.core.Executive;
import ovm.core.domain.Type;
import ovm.core.services.memory.VM_Address;
import s3.core.domain.S3Type;
import test.common.TestBase;
import test.common.TestSuite;

public class TestSubtypetest extends TestBase {

    boolean doThrow;

    static class A { }
    static class B extends A { }
    static class C  { }
    static class D implements I { }
    static class E implements J { }
    static class F implements K { }
    static class G extends D { }
    static interface I { }
    static interface J extends I { }
    static interface K extends J { }
    static interface L { }

    private Object a = new A();
    private Object b = new B();
    private Object c = new C();
    private Object d = new D();
    private Object e = new E();
    private Object f = new F();
    private Object g = new G();
    private Object nullObj = null;
    private Object arrA3 = new A[1][1][1];
    private Object arrA5 = new A[1][1][1][1][1];
    private Object arrB5 = new B[1][1][1][1][1];
    private Object arrInteger5 = new int[1][1][1][1][1];
    private Object arrObject3 = new Object[1][1][1];
    private Object arrObject5 = new Object[1][1][1][1][1];
    private Object arrI3 = new I[1][1][1];

    public TestSubtypetest(long disabled) {
	super("Subtypetest");
    }

    public void run() {
	testNullInstanceOf();
	testNullCheckCast();
	testGoodCast();
	testNormalClassEqual();
	testNormalClassSuperClass();
	testNormalClassSubClass();
	testNormalClassNonSubtype();
	testNormalClassObjectParent();
	testArrayObjectParent();
	testArrayEqual();
	testArrayNoEqualDimension();
	testArrayObjectArray();
	testArrayNoEqualBaseType();
	testArrayObjectArraySubtype();
	testArrayNotSubtype();
	testArrayPrimitiveObjectEqualDimension();
	testArrayPrimitiveObjectGreaterDimension();
	testArrayPrimitiveObjectLessDimension();
	testArrayPrimitiveObjectObjectLessDimension();
	testArrayObjectPrimitiveEqual();
	testArrayObjectPrimitiveGreater();
	testArrayObjectPrimitiveLess();
	testInterfaceClass();
	testInterfaceInterfaceEqual();
	testInterfaceInterfaceParent();
	testInterfaceInterfaceAncestor();
	testInterfaceInterfaceChild();
	testInterfaceInterfaceDescendent();
	testInterfaceInterfaceNoRelation();
	testClassInterfaceDirect();
	testAncestorInterfaceDirect();
	testClassInterfaceIndirect();
	testClassInterfaceNoRelation();
	testInterfaceArray();
	testArrayInterfaceSerializable();
	testArrayInterfaceCloneable();
	testArrayInterface();
	testLeastCommonSupertypes_1();
	testLeastCommonSupertypes_2();
	testLeastCommonSupertypes_3();
	testLeastCommonSupertypes_4();
	testLeastCommonSupertypes_5();
	testLeastCommonSupertypes_6();
	testLeastCommonSupertypes_7();
	testLeastCommonSupertypes_8();
	testLeastCommonSupertypes_9();
	testLeastCommonSupertypes_10();

   }

    public void testAssert(boolean cond, String msg) {
	if (! cond) Executive.panic("TestSubtypetest assertion failure: " + msg);
    }

    /** Null is not an instanceof anything.*/
    public void testNullInstanceOf() {
        Object o = null;
	testAssert( ! (o instanceof A ), "testNullInstanceOf failed");
    }
    
    /**
     * Null can be cast to anything.
     **/
    public void testNullCheckCast() {
	Object o = this;
	o = (A)nullObj;
	testAssert( null == o, "testNullCheckCast failed");
    }
    
    /**
     * Tests one simple case of a good cast. (On the assumption that checkcast
     * and instanceof share an underlying mechanism, it should not be
     * necessary to duplicate all of the instanceof tests with checkcasts; but
     * it does seem a good idea to test at least one nontrivial case each of
     * good and bad casts.)
     */
    public void testGoodCast() {
      	Object o;
	if ( false ) o = arrA3; else o = b;
	testAssert( VM_Address.fromObject(o).asOop().getBlueprint().getType()
	            .isSubtypeOf(VM_Address.fromObject(a).asOop()
		    .getBlueprint().getType()), "slow path broken");
	A aa = (A) o;
	testAssert( aa != null, "good cast failed"); // won't get here anyway
    }
     
    /** checks to see that a class is a subtype of itself*/
    public void testNormalClassEqual() {testAssert(a instanceof A, "testNormalClassEqual fails with equal classes");}

    /** tests that that a superclass is not a subclass of a subclass */
    public void testNormalClassSuperClass() {testAssert( ! (a instanceof B), "testNormalClassSuperClass fails with equal classes");}

    /** tests that a classes in a direct subtype relationship pass a subtype test*/
    public void testNormalClassSubClass() {testAssert(b instanceof A, "testNormalClassSubClass() fails");}

    /** checks two unrelated classes to make sure the subtype test fails*/
    public void testNormalClassNonSubtype() {testAssert(! (c instanceof A), "testNormalClassNonSubtype fails");}

    /** tests that a subtype test with a class and Object passes*/
    public void testNormalClassObjectParent() {testAssert((b instanceof Object), "testNormalClassObjectParent fails");}


    /**tests that a subtype test with an array class and Object passes*/
    public void testArrayObjectParent() {testAssert(arrA3 instanceof Object, "testArrayObjectParent fails");}

    /** tests that two equal array classes pass subtype tests*/
    public void testArrayEqual() {testAssert(arrA3 instanceof A[][][], "testArrayEqual fails");}

    /** tests that two equal array classes with unequal dimensions fail a subtype test*/
    public void testArrayNoEqualDimension() {
	Object o = arrA3;
	testAssert( ! (o instanceof A[][][][][]), "testArrayNoEqualDimension fails");
    }

    /** tests that multidimensional array classes are subtypes of
     * lower-dimensional arrays with java.lang.Object as a basetype.
     **/
    public void testArrayObjectArray() {testAssert((arrA3 instanceof Object[]), "testArrayObjectArray fails");}

    /**tests that two unequal array classes with unequal dimensions fail a subtype test*/
    public void testArrayNoEqualBaseType() {testAssert((arrB5 instanceof A[][][][][]), "testArrayNoEqualBaseType fails");}

    /** tests that two unequal array classes with equal dimensions
     * whose basetypes are in a subtype relationship pass a subtype test*/
    public void testArrayObjectArraySubtype() {testAssert((arrA5 instanceof Object[][][][][]), "testArrayObjectArraySubtype fails");}

    /** tests that two unequal array classes with equal dimensions
     * whose basetypes are not in a subtype relationship fail a subtype test*/
    public void testArrayNotSubtype() {
	Object o = arrB5;
	testAssert(! (o instanceof C[][][][][]), "testArrayNotSubtype fails");
    }

    /** tests that a primitive array of equal dimensions with an object (not Object) array fails a subtype test*/
    public void testArrayPrimitiveObjectEqualDimension() {testAssert(! (arrInteger5 instanceof A[][][][][]), "testArrayPrimitiveObjectEqualDimension fails");}

    /** tests that an object (not Object) array of larger dimensions than a primitive array is 
     * not a supertype of the primitive array*/
    public void testArrayPrimitiveObjectGreaterDimension() {testAssert(! (arrInteger5 instanceof A[][][][][][][]),"testArrayPrimitiveObjectGreaterDimension fails");}

    /** tests that an object array of larger dimensions than a primitive 
     * array is not a supertype of the primitive array */
    public void testArrayPrimitiveObjectLessDimension() {
	testAssert(! (arrInteger5 instanceof A[][]), "testArrayPrimitiveObjectLessDimension fails");
    }

    /** tests that a java.lang.Object array with lesser dimensions than a primitive array is 
     * a supertype of that array */
    public void testArrayPrimitiveObjectObjectLessDimension() {testAssert((arrInteger5 instanceof Object[][][]), "testArrayPrimitiveObjectObjectLessDimension fails");}

    /** tests that a primitive array of the same dimensions as a java.lang.Object array is not 
     * a subtype of the Object array */
    public void testArrayObjectPrimitiveEqual() {testAssert(! (arrInteger5 instanceof Object[][][][][]), "testArrayObjectPrimitiveEqual");}

    /**
     * tests that a \texttt{java.lang.Object} array with greater
     * dimensions than a primitive array is not a supertype of that 
     * array
     **/
    public void testArrayObjectPrimitiveGreater() {
	Object o = arrObject3;
	testAssert(! (o instanceof int[][][][][]), 
		  "testArrayObjectPrimitiveGreater");
    }

    /**
     * tests that a java.lang.Object array with greater
     * dimensions than a primitive array is not a subtype of that 
     * array
     **/
    public void testArrayObjectPrimitiveLess() {
	Object o = arrObject5;
	testAssert(! (o instanceof int[][][][]), 
		  "testArrayObjectPrimitiveLess");
    }

    /** tests that an interface does not pass a subtype test with a class as the target */
    public void testInterfaceClass() { testAssert(! (d instanceof A), "testInterfaceClass"); }

    /** tests that equal interfaces pass a subtype test */
    public void testInterfaceInterfaceEqual() {testAssert(d instanceof I, "testInterfaceInterfaceEqual"); }

    /** tests that a subinterface is a subtype of its parent interface */
    public void testInterfaceInterfaceParent() {testAssert(e instanceof I, "testInterfaceInterfaceParent"); }

    /** tests that a subinterface is a subtype of some ancestor interface
     * (higher than parent in the hierarchy) */
    public void testInterfaceInterfaceAncestor() {testAssert(f instanceof I, "testInterfaceInterfaceAncestor");}

    /** tests that a subinterface is not a subtype of its child */
    public void testInterfaceInterfaceChild() {testAssert(!(d instanceof J), "testInterfaceInterfaceChild");}

    /** tests that a subinterface is not a subtype of some deeper descendant than direct child*/
    public void testInterfaceInterfaceDescendent() {testAssert(! (d instanceof K), "testInterfaceInterfaceDescendent");}

    /** tests that unrelated interfaces fail a subtype test */
    public void testInterfaceInterfaceNoRelation() {testAssert(!(d instanceof L), "testInterfaceInterfaceNoRelation");}

    /** tests that a class that directly implements an interface is a subtype of that interface*/
    public void testClassInterfaceDirect() {testAssert((d instanceof I), "testClassInterfaceDirect");}

    /**tests that a class whose ancestor directly implements an interface is a subtype of that interface*/
    public void testAncestorInterfaceDirect() {testAssert((g instanceof I), "testAncestorInterfaceDirect");}

    /** tests that a class which directly implements a subinterface of
     * the target interface is a subtype of the target interface*/
    public void testClassInterfaceIndirect() {testAssert((e instanceof I), "testClassInterfaceIndirect");}

    /** tests that a class which does not directly or indirectly implement a target interface 
     * (and whose ancestors also do not do this) fails a subtype test with the target interface*/
    public void testClassInterfaceNoRelation() {testAssert(! (b instanceof K), "testClassInterfaceNoRelation");}
	
    /**tests that an array of interfaces fail a subtype test against their basetype*/
    public void testInterfaceArray() {testAssert(! (d instanceof I[][][]), "testInterfaceArray");}
	
    /**tests that an array class is a subtype of Serializable*/
    public void testArrayInterfaceSerializable() {
	testAssert((arrI3 instanceof java.io.Serializable), "testArrayInterfaceSerializable");
    }
	
    /** tests that an array class is a subtype of Cloneable */
    public void testArrayInterfaceCloneable() {
	testAssert((arrI3 instanceof java.lang.Cloneable), "testArrayInterfaceSerializable");
    }

    /** tests that an array class is not a subtype of some interface which is not Serializable or Cloneable*/
    public void testArrayInterface() {
	Object o = arrI3;
	testAssert(! (o instanceof K), "testArrayInterface");
    }
    /** Two unrelated type -- go up to the root */
    public void testLeastCommonSupertypes_1() {
        S3Type.Class cl_c1 = (S3Type.Class) typeOf(new C1());
        S3Type.Class cl_c2 = (S3Type.Class) typeOf(new C2());
        Type.Reference[] sup = cl_c1.getLeastCommonSupertypes(cl_c2);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_1");
        testAssert(sup[0] == cl_c1.getDomain().getHierarchyRoot(), "testLeastCommonSupertypes_1");
     }
    /** Two types in direct subtype relationship */
    public void testLeastCommonSupertypes_2() {
        S3Type.Class cl_c1 = (S3Type.Class) typeOf(new C1());
        S3Type.Class cl_c1c = (S3Type.Class) typeOf(new C1C());
        Type.Reference[] sup = cl_c1.getLeastCommonSupertypes(cl_c1c);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_2");
        testAssert(sup[0] == cl_c1, "testLeastCommonSupertypes_2");
    }
    /** Same type */
    public void testLeastCommonSupertypes_3() {
        S3Type.Class cl_c1 = (S3Type.Class) typeOf(new C1());
        S3Type.Class cl_c1prime = (S3Type.Class) typeOf(new C1());
        Type.Reference[] sup = cl_c1.getLeastCommonSupertypes(cl_c1prime);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_3");
        testAssert(sup[0] == cl_c1, "testLeastCommonSupertypes_3");
    }
    /** Twice the same array type */
    public void testLeastCommonSupertypes_4() {
        S3Type cl_ac1 = (S3Type) typeOf(new C1[0]);
        S3Type cl_ac1prime = (S3Type) typeOf(new C1[0]);
        Type.Reference[] sup = cl_ac1.getLeastCommonSupertypes(cl_ac1prime);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_4");
        testAssert(sup[0] == cl_ac1, "testLeastCommonSupertypes_4");
    }
    /** Reference Array types in subtype realtion ship */
    public void testLeastCommonSupertypes_5() {
        S3Type cl_ao = (S3Type) typeOf(new Object[0]);
        S3Type cl_ac1 = (S3Type) typeOf(new C1[0]);
        Type.Reference[] sup = cl_ac1.getLeastCommonSupertypes(cl_ao);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_5");
        testAssert(sup[0] == cl_ao, "testLeastCommonSupertypes_5");
    }
    /** Unrelated reference array types, go to the root array type */
    public void testLeastCommonSupertypes_6() {
        S3Type cl_ao = (S3Type) typeOf(new Object[0]);
        S3Type cl_ac1 = (S3Type) typeOf(new C1[0]);
        S3Type cl_ac2 = (S3Type) typeOf(new C2[0]);
        Type.Reference[] sup = cl_ac1.getLeastCommonSupertypes(cl_ac2);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_6");
        testAssert(sup[0] == cl_ao, "testLeastCommonSupertypes_6");
    }
    /** Reference and primitive array types, go to the root */
    public void testLeastCommonSupertypes_7() {
        S3Type.Class cl_o = (S3Type.Class) typeOf(new Object());
        S3Type cl_ac1 = (S3Type) typeOf(new C1[0]);
        S3Type cl_ai = (S3Type) typeOf(new int[0]);
        Type.Reference[] sup = cl_ac1.getLeastCommonSupertypes(cl_ai);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_7");
        testAssert(sup[0] == cl_o, "testLeastCommonSupertypes_7");
    }
    /** Unrelated reference array types, matched dimensions, go to the root array type */
    public void testLeastCommonSupertypes_8() {
        S3Type cl_ao = (S3Type) typeOf(new Object[1][0]);
        S3Type cl_ac1 = (S3Type) typeOf(new C1[1][0]);
        S3Type cl_ac2 = (S3Type) typeOf(new C2[1][0]);
        Type.Reference[] sup = cl_ac1.getLeastCommonSupertypes(cl_ac2);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_8");
        testAssert(sup[0] == cl_ao, "testLeastCommonSupertypes_8");
    }
    /** Unrelated reference array types, mismatched dimensions, go to the root array type
     *  of the array with the smallest dimension. */
    public void testLeastCommonSupertypes_9() {
        S3Type cl_ao = (S3Type) typeOf(new Object[0]);
        S3Type cl_ac1 = (S3Type) typeOf(new C1[0]);
        S3Type cl_ac2 = (S3Type) typeOf(new C2[1][0]);
        Type.Reference[] sup = cl_ac1.getLeastCommonSupertypes(cl_ac2);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_9");
        testAssert(sup[0] == cl_ao, "testLeastCommonSupertypes_9");
    }
    /** Two types with a shared interface */
    public void testLeastCommonSupertypes_10() {
        S3Type.Class cl_c1 = (S3Type.Class) typeOf(new C1I1());
        S3Type.Class cl_c2 = (S3Type.Class) typeOf(new C2I1());
        Type.Reference[] sup = cl_c1.getLeastCommonSupertypes(cl_c2);
        testAssert(sup.length == 1, "testLeastCommonSupertypes_10");
        Type.Reference i1 = cl_c1.getAllInterfaces()[0];
        testAssert(sup[0] == i1, "testLeastCommonSupertypes_10");
    }
    
    public boolean isIn(Object o, Object[] os) {
        boolean found = false;
        for (int i = 0; i < os.length; i++) 
            if (os[i] == o) return true;
        return found;
    } 
    static ovm.core.domain.Type typeOf(Object o) { 
        return VM_Address.fromObject(o).asOop().getBlueprint().getType(); 
    }
    static class C1 {}
    static class C2 {}
    static class C1C extends  C1 {}
    
    static interface I1 {}
    static interface I1I extends I1{}
    static interface I2 {} 
    
    static class C2I1 extends C2 implements I1 {}
    static class C2I1I extends C2 implements I1I {}
    static class C1I1 extends C1 implements I1 {}
    
}
