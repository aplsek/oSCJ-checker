package test.runtime;
import test.common.TestBase;


public class TestSubtypetestBytecodes extends TestBase {

    private TestBase thisAsTestBase;
    private TestSubtypetestBytecodes thisAsTsb; // just for extreme clarity
    private TestBase nullAsTestBase;
    private TestBase aTestBase;
    private TestSubtypetestBytecodes aTsb;
    
    public TestSubtypetestBytecodes() {
	super("TestSubtypetestBytecodes");
	thisAsTestBase = this;
	thisAsTsb = this; // just for extreme clarity
	nullAsTestBase = null;
	aTestBase = new TestBase("nonsense") { public void run() {} };
    }

    public void run() {
        testNullInstanceOf();
	testNullCheckCast();
	testNormalClassEqual();
	testNormalClassSuperClass();
	testNormalClassSubClass();
	testNormalClassNonSubtype();
	testNormalClassObjectParent();
	testInterfaceObjectParent();
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
	testInterfaceObject();
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
	//System.err.println("TestSubtypetestBytecodes passed");
    }

    /**
     * Null is not an instanceof anything.
     **/
    public void testNullInstanceOf() {
      Object o = null;
      check_condition( ! ( o instanceof TestBase ), "NullInstanceOf failed");
    }
    
    /**
     * Null can be cast to anything.
     **/
    public void testNullCheckCast() {
      aTsb = (TestSubtypetestBytecodes)nullAsTestBase;
      check_condition( null == aTsb, "you'll never see this message anyhow.");
    }
    
    /**
     * checks to see that a class is a subtype of itself
     **/
    public void testNormalClassEqual() {
      	Object o = this;
        check_condition( o instanceof TestSubtypetestBytecodes,
	      	    "NormalClassEqual failed");
	aTsb = (TestSubtypetestBytecodes)thisAsTestBase;
	check_condition( aTsb == thisAsTsb, "you'll never see this message anyhow.");
    }

    /**
     * tests that that a superclass is not a subclass
     * of a subclass 
     **/
    public void testNormalClassSuperClass() {
	check_condition( ! ( aTestBase instanceof TestSubtypetestBytecodes ),
	      	    "NormalClassSuperClass failed");
	// won't test checkcast, failure won't be graceful
    }

    /**
     * tests that a classes in a direct subtype relationship pass
     * a subtype test
     **/
    public void testNormalClassSubClass() {
        Object o = this;
	check_condition( o instanceof TestBase, "NormalClassSubClass failed");
	// test checkcast in a way the compiler won't remove
    }

    /**
     * checks two unrelated classes to make sure the subtype test fails
     **/
    public void testNormalClassNonSubtype() {
	// FIXME write this testcase
    }

    /**
     * tests that a subtype test with a class and Object passes
     **/
    public void testNormalClassObjectParent() {
	// FIXME write this testcase
    }

    /**
     * tests that a subtype test with an interface and Object passes
     **/
    public void testInterfaceObjectParent() {
	// FIXME write this testcase
    }

    /**
     * tests that a subtype test with an array class and Object passes
     **/
    public void testArrayObjectParent() {
	// FIXME write this testcase
    }

    /**
     * tests that two equal array classes pass subtype tests
     **/
    public void testArrayEqual() {
	// FIXME write this testcase
    }

    /**
     * tests that two equal array classes (which don't have
     * java.lang.Object as a basetype) with unequal dimensions
     * fail a subtype test
     **/
    public void testArrayNoEqualDimension() {
	// FIXME write this testcase
    }

    /**
     * tests that multidimensional array classes (which don't have
     * java.lang.Object as a basetype) are subtypes of
     * lower-dimensional arrays with java.lang.Object as a basetype.
     **/
    public void testArrayObjectArray() {
	// FIXME write this testcase
    }

    /**
     * tests that two unequal array classes (which don't have
     * java.lang.Object as a basetype) with unequal dimensions
     * fail a subtype test
     **/
    public void testArrayNoEqualBaseType() {
	// FIXME write this testcase
    }

    /**
     * tests that two unequal array classes with equal dimensions
     * whose basetypes are in a subtype relationship pass a subtype test
     **/
    public void testArrayObjectArraySubtype() {
	// FIXME write this testcase
    }

    /**
     * tests that two unequal array classes with equal dimensions
     * whose basetypes are not in a subtype relationship fail a subtype test
     **/
    public void testArrayNotSubtype() {
	// FIXME write this testcase
    }

    /**
     * tests that a primitive array of equal dimensions with an
     * object (not Object) array fails a subtype test
     **/
    public void testArrayPrimitiveObjectEqualDimension() {
	// FIXME write this testcase
    }

    /**
     * tests that an object (not Object) array of larger
     * dimensions than a primitive array is not a supertype of the primitive
     * array
     **/
    public void testArrayPrimitiveObjectGreaterDimension() {
	// FIXME write this testcase
    }

    /**
     * tests that an object (not \texttt{Object}) array of larger
     * dimensions than a primitive array is not a supertype of the 
     * primitive array
     **/
    public void testArrayPrimitiveObjectLessDimension() {
	// FIXME write this testcase
    }

    /**
     * tests that a java.lang.Object array with lesser
     * dimensions than a primitive array is a supertype of that array
     **/
    public void testArrayPrimitiveObjectObjectLessDimension() {
	// FIXME write this testcase
    }

    /**
     * tests that a primitive array of the same dimensions as a
     * java.lang.Object array is not a subtype of the Object
     * array
     **/
    public void testArrayObjectPrimitiveEqual() {
	// FIXME write this testcase
    }

    /**
     * tests that a \texttt{java.lang.Object} array with greater
     * dimensions than a primitive array is not a supertype of that 
     * array
     **/
    public void testArrayObjectPrimitiveGreater() {
	// FIXME write this testcase
    }

    /**
     * tests that a java.lang.Object array with greater
     * dimensions than a primitive array is not a subtype of that 
     * array
     **/
    public void testArrayObjectPrimitiveLess() {
	// FIXME write this testcase
    }

    /**
     * tests that an interface does not pass a subtype test with a class
     * as the target
     **/
    public void testInterfaceClass() {
	// FIXME write this testcase
    }

    /**
     *  tests that an interface is a subtype of java.lang.Object
     **/
    public void testInterfaceObject() {
	// FIXME write this testcase
    }

    /**
     * tests that equal interfaces pass a subtype test
     **/
    public void testInterfaceInterfaceEqual() {
	// FIXME write this testcase
    }

    /**
     *  tests that a subinterface is a subtype of its parent interface
     **/
    public void testInterfaceInterfaceParent() {
	// FIXME write this testcase
    }

    /**
     * tests that a subinterface is a subtype of some ancestor interface
     * (higher than parent in the hierarchy)
     **/
    public void testInterfaceInterfaceAncestor() {
	// FIXME write this testcase
    }

    /**
     * tests that a subinterface is not a subtype of its child
     **/
    public void testInterfaceInterfaceChild() {
	// FIXME write this testcase
    }

    /**
     * tests that a subinterface is not a subtype of some deeper descendant
     * than direct child
     **/
    public void testInterfaceInterfaceDescendent() {
	// FIXME write this testcase
    }

    /**
     * tests that unrelated interfaces fail a subtype test
     **/
    public void testInterfaceInterfaceNoRelation() {
	// FIXME write this testcase
    }

    /**
     * tests that a class that directly implements an interface is a
     * subtype of that interface
     **/
    public void testClassInterfaceDirect() {
	// FIXME write this testcase
    }

    /**
     * tests that a class whose ancestor directly implements an interface
     * is a subtype of that interface
     **/
    public void testAncestorInterfaceDirect() {
	// FIXME write this testcase
    }

    /**
     * tests that a class which directly implements a subinterface of
     * the target interface is a subtype of the target interface
     **/
    public void testClassInterfaceIndirect() {
	// FIXME write this testcase
    }

    /**
     * tests that a class which does not directly or indirectly implement
     * a target interface (and whose ancestors also do not do this) fails
     * a subtype test with the target interface
     **/
    public void testClassInterfaceNoRelation() {
	// FIXME write this testcase
    }
	
    /**
     * tests that an array of interfaces fail a subtype test against their
     * basetype
     **/
    public void testInterfaceArray() {
	// FIXME write this testcase
    }
	
    /**
     * tests that an array class is a subtype of Serializable
     **/
    public void testArrayInterfaceSerializable() {
	// FIXME write this testcase
    }
	
    /**
     * tests that an array class is a subtype of Cloneable
     **/
    public void testArrayInterfaceCloneable() {
	// FIXME write this testcase
    }
	
    /**
     * tests that an array class is not a subtype of some interface which
     * is not Serializable or Cloneable
     **/
    public void testArrayInterface() {
	// FIXME write this testcase
    }

}    
