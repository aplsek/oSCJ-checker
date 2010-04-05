
package test.jvm;

import java.lang.reflect.*;
/**
 * JVM level reflection tests. While the UD reflection tests mainly test
 * correct behaviour of valid invocations, these tests checks all the error
 * conditions to ensure we meet the spec.
 *
 * @author David Holmes
 */
public class TestReflection extends TestBase {

    public TestReflection(Harness domain) {
        super("java.lang.reflect Tests", domain);
    }


    // custom runtime exception we can catch
    static class MyException extends RuntimeException {
        public MyException(String msg) {
            super(msg);
        }
    }

    // a class that can't be successfully initialized
    static class ErrorClass {
	static {
	    if (true)
		throw new RuntimeException("hmm");
	}
	static int field = 0;

        static void method() {}
    } 


    static class PkgConstructor {
	PkgConstructor() { }
    }

    static class PvtConstructor {
	private PvtConstructor() { }
	static void makeLocally()
	    throws IllegalAccessException, InstantiationException
	{
	    PvtConstructor.class.newInstance();
	}
    }

    // class that performs reflection within its constructor. This allows
    // testing of reflective invocation of methods that reflectively invoke
    // methods, which may throw exceptions.
    static class Helper {

        Helper(boolean useReflection, boolean doThrow) {
            if (useReflection) {
                try {
                    Method m = this.getClass().
                        getDeclaredMethod("setField", 
                                          new Class[]{ boolean.class });
                    m.invoke(this, new Object[] { 
                        (doThrow ? Boolean.TRUE : Boolean.FALSE)});
                }
                catch (InvocationTargetException ite) {
                    Throwable cause = ite.getCause();
                    if (cause instanceof MyException)
                        throw (MyException) cause;
                    else if (cause instanceof Error) 
                        throw (Error) cause;
                    else if (cause instanceof RuntimeException)
                        throw (RuntimeException) cause;
                }
                catch(Throwable t) {
                    System.out.println("Unexpected exception: ");
                    t.printStackTrace();
                }
            }
            else { // make sure these methods get in the image
                setField(false);
            }
        }
            
        // dummy value to set via reflection
        int field;
        
        void setField(boolean throwException) {
            field = 1;
            if (throwException)
                throw new MyException("exception setField");
        }
    }


    public void run() {
        recursiveReflectionTest();
        testGetClassAfterError();

        testPrimitiveField();
        testReferenceField();

	testNewInstance();
        testMethodInvoke();
        testStaticMethodInvoke();

        testPrimitiveStaticField();
        testReferenceStaticField();


        testArray();
    }


    void recursiveReflectionTest() {
        Class c = Helper.class;

        setModule("Recursive reflect test - no exceptions");
        try {
            Constructor cons = c.getDeclaredConstructor(new Class[] {
                boolean.class, boolean.class});

            try {
                Helper h = (Helper) cons.newInstance(new Object[] {
                    Boolean.TRUE, Boolean.FALSE});
                // ok
            }
            catch(Throwable t) {
                check_condition(false, t.toString());
                t.printStackTrace();
            }

            setModule("Recursive reflect test - exceptions thrown");
            try {
                Helper h = (Helper) cons.newInstance(new Object[] {
                    Boolean.TRUE, Boolean.TRUE});
            }
            catch (InvocationTargetException ite) {
                if (ite.getCause() instanceof MyException)
                    ; // ok
                else {
                    fail(ite.toString());
                    ite.printStackTrace();
                }
            }
        }
        catch(Throwable t) {
            fail(t.toString());
            t.printStackTrace();
        }

    }

    void testGetClassAfterError() {
        setModule("Test Class that can't be initialized");
        try {
            // First try to load and initialize - which should fail
            try {
                Class bad = Class.forName("test.jvm.TestReflection$ErrorClass");
                if (!check_condition(false, "Error: forName succeeded")) 
                    return;
            }
            catch (ExceptionInInitializerError e) {
                // ok
            }
            
            // now try implicit initialization
            
            // note: after trying to initialize a class once, the error will
            // change to NoClassDefFound
            
            try {
                ErrorClass.field = 2;
                if (!check_condition(false, "Error: set of field succeeded")) 
                    return;
            }
            catch(NoClassDefFoundError e) {
                // ok
            }
            
            
            // A class that can't be initialized should still be obtainable
            // via forName as long as we don't ask for it to be initialized.
            // Subsequent use should of course try to initialize and hence get
            // an exception.
            Class bad = null;
            try {
                bad = Class.forName("test.jvm.TestReflection$ErrorClass",
                                    false,
                                    getClass().getClassLoader());
                // ok
            }
            catch(Throwable t) {
                check_condition(false, "forName(x, false, ...) unexpected exception " + t);
            }
            
            try {
                Object o = bad.newInstance();
                if (!check_condition(false, "Error: newInstance succeeded")) 
                    return;
            } catch (NoClassDefFoundError e) {
                // ok
            }
            
            // it is not clear from the JDK API's if these getXXX methods 
            // constitute an active use of a class - the JDK does not treat 
            // them so, and neither do we. Using them certainly is though.
            try {
                Constructor cons = bad.getDeclaredConstructor(null);
                // ok
                try {
                    Object o = cons.newInstance(null);
                    if (!check_condition(false, 
                                         "Error: cons.newInstance succeeded")) 
                        return;

                }
                catch(NoClassDefFoundError e) {
                    // ok
                }
            }
            catch(Throwable e) {
                check_condition(false, "cons.newInstance unexpected exception " + e);
            }

            try {
                Field f = bad.getDeclaredField("field");
                // ok
                try {
                    int i = f.getInt(null);
                    if (!check_condition(false, "Error: getInt succeeded")) 
                        return;
                }
                catch(NoClassDefFoundError e) {
                    // ok
                }
            }
            catch(Throwable e) {
                check_condition(false, "field.getInt unexpected exception " + e);
                e.printStackTrace();
            }

            try {
                Method m = bad.getDeclaredMethod("method", null);
                // ok
                try {
                    m.invoke(null, null);
                    if (!check_condition(false, "Error: invoke succeeded")) 
                        return;
                }
                catch(NoClassDefFoundError e) {
                    // ok
                }
            }
            catch(Throwable e) {
                check_condition(false, "method.invoke unexpected exception " + e);
                e.printStackTrace();
            }


        }
        catch (Throwable t) {
            check_condition(false, "Unexpected exception: " + t);
            t.printStackTrace();
        }
    }


    Helper helperField;
    // varied, but not exhaustive, tests for Field.set/get 
    void testReferenceField() {
        setModule("Test ReferenceField");
        try {
            Field f = this.getClass().getDeclaredField("helperField");

            try { // null receiver
                f.set(null, null);
                check_condition(false, "Error: set(null,...) succeeded!");
            }
            catch(NullPointerException npe){
                // ok
            }

            try {  // wrong receiver type
                f.set("", new Long(1));
                check_condition(false, "Error: set(String,...) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

            // some valid ops
            try {
                f.set(this, null); // ok
                f.set(this, new Helper(false, false)); // ok
                f.set(this, new Helper(false, false) { } ); // ok -subclass
            }
            catch(IllegalArgumentException iae){
                check_condition(false, "Error: set(int) failed!");
            }

            try { // wrong type
                f.set(this, "");
                check_condition(false, "Error: set(\"\") succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

            // valid op
            try {
                Helper h = (Helper)f.get(this);
            }
            catch(Throwable t) {
                check_condition(false, t.toString());
                t.printStackTrace();
            }

            try { // invalid op
                byte b = f.getByte(this);
                check_condition(false, "Error: getByte succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }

    }

    long longField;
    // varied, but not exhaustive, tests for Field.set/get 
    void testPrimitiveField() {
        setModule("Test Primitive Field");
        try {
            Field f = this.getClass().getDeclaredField("longField");

            try { // null receiver
                f.set(null, new Long(1));
                check_condition(false, "Error: set(null,...) succeeded!");
            }
            catch(NullPointerException npe){
                // ok
            }

            try {  // wrong receiver type
                f.set("", new Long(1));
                check_condition(false, "Error: set(String,...) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

            // some valid ops
            try {
                f.set(this, new Long(1)); // ok
                f.set(this, new Byte((byte)1)); // ok - widening conversion
                f.set(this, new Character('1')); // ok - widening conversion
                f.set(this, new Integer(1)); // ok - widening conversion
            }
            catch(IllegalArgumentException iae){
                check_condition(false, "Error: set(int) failed!");
            }

            try { // wrong type
                f.set(this, "");
                check_condition(false, "Error: set(\"\") succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try { // wrong type
                f.set(this, null);
                check_condition(false, "Error: set(null) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try { // narrowing conversion
                f.set(this, new Float(1.0f));
                check_condition(false, "Error: set(Float) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

            // some valid ops
            try {
                Long L = (Long)f.get(this);
                long l = f.getLong(this);
                float ff = f.getFloat(this); // ok - widening conversion
                double d = f.getDouble(this); // ok - widening conversion
            }
            catch(Throwable t) {
                check_condition(false, t.toString());
                t.printStackTrace();
            }

            try { // narrowing conversion
                byte b = f.getByte(this);
                check_condition(false, "Error: getByte succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }
    }

    void testNewInstance() {
	setModule("test newInstance()");
	try {
	    try {
		PkgConstructor.class.newInstance();
	    } catch (IllegalAccessException e) {
		fail(e);
	    }
	    try {
		PvtConstructor.class.newInstance();
		fail("successfully invoked private constructor");
	    } catch (IllegalAccessException e) {
		// OK
	    }
	    try {
		PvtConstructor.makeLocally();
	    } catch (IllegalAccessException e) {
		fail(e);
	    }
	} catch (InstantiationException e) {
	    fail(e);
	}
    }

    // our target method
    int method(Helper h, int i) {
        return i;
    }

    void testMethodInvoke() {
        setModule("Test Method.invoke()");

        Helper h1 = new Helper(false, false);
        Helper h2 = new Helper(false, false) {}; // subclass
        try {
            Method m = this.getClass().getDeclaredMethod("method", 
                                  new Class[]{ Helper.class, int.class });

            try { // wrong receiver type
                m.invoke(new Object(), new Object[] { null, new Integer(1)});
                check_condition(false, "Error: invoke(Object, ..) succeeded!");
            }
            catch(IllegalArgumentException iae) {
                // ok
            }

            try { // null receiver type
                m.invoke(null, new Object[] { null, new Integer(1)});
                check_condition(false, "Error: invoke(null, ..) succeeded!");
            }
            catch(NullPointerException npe){
                // ok
            }

            // some valid ops
            try {
                m.invoke(this, new Object[] { null, new Integer(1)});  // ok
                m.invoke(this, new Object[] { h1, new Integer(1)});  // ok
                m.invoke(this, new Object[] { h2, new Integer(1)});  // ok
                m.invoke(this, new Object[] { h2, new Byte((byte)1)});  // ok - widening conversion
            }
            catch(IllegalArgumentException iae){
                check_condition(false, "Error: invoke(int) failed!");
            }

            // wrong args types
            try {
                m.invoke(this, new Object[] { h1, ""});
                check_condition(false, "Error: invoke(h1, \"\") succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try {
                m.invoke(this, new Object[] { new Integer(1), new Integer(1)});
                check_condition(false, "Error: invoke(Integer, 1) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try {
                m.invoke(this, new Object[] { new Object(), null });
                check_condition(false, "Error: invoke(Object, null) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try {
                m.invoke(this, new Object[] { h1, new Long(1) });
                check_condition(false, "Error: invoke(h1, long) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }
    }


    // this is far from complete
    void testArray() {
        setModule("Test Array");
        try {
            Integer[] arrayInt;

            int len = 3;

            Object array = Array.newInstance(Integer.class, len);
            check_condition(array != null, "null from Array.newInstance");
            check_condition(Array.getLength(array) == len,
                            "wrong array length: " + Array.getLength(array) +
                            " expected " + len);
            check_condition(array.getClass().isArray(), "!getClass().isArray()");
            check_condition(array instanceof Integer[], "wrong array type");
            try {
                arrayInt = (Integer[]) array;
            }
            catch (ClassCastException e) {
                check_condition(false, "ClassCastException on direct cast");
            }

        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }

        try {
            Helper[] arrayHelper = new Helper[0]; // force array type into img

            int len = 3;

            Object array = Array.newInstance(Helper.class, len);
            check_condition(array != null, "null from Array.newInstance");
            check_condition(Array.getLength(array) == len,
                            "wrong array length: " + Array.getLength(array) +
                            " expected " + len);
            check_condition(array.getClass().isArray(), "!getClass().isArray()");
            check_condition(array instanceof Helper[], 
                            "wrong array type for Helper[] - expected: " 
                            + Helper[].class + 
                            "\n                                              got: " +
                            array.getClass());
            try {
                arrayHelper = (Helper[]) array;
            }
            catch (ClassCastException e) {
                check_condition(false, "ClassCastException on direct cast");
            }

        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }

    }            


    // static tests

    static Helper staticHelperField;

    // varied, but not exhaustive, tests for Field.set/get 
    void testReferenceStaticField() {
        setModule("Test Static Reference Field");
        try {
            Field f = this.getClass().getDeclaredField("staticHelperField");

            // some valid ops
            try {
                f.set(null, null); // ok
                f.set(null,new Helper(false, false)); // ok
                f.set(null, new Helper(false, false) { } ); // ok -subclass
            }
            catch(IllegalArgumentException iae){
                check_condition(false, "Error: static set(int) failed!");
            }

            try { // wrong type
                f.set(null, "");
                check_condition(false, "Error: static set(\"\") succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

            // valid op
            try {
                Helper h = (Helper)f.get(null);
            }
            catch(Throwable t) {
                check_condition(false, t.toString());
                t.printStackTrace();
            }

            try { // invalid op
                byte b = f.getByte(null);
                check_condition(false, "Error: static getByte succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }

    }

    static long staticLongField;
    // varied, but not exhaustive, tests for Field.set/get 
    void testPrimitiveStaticField() {
        setModule("Test Static Primitive Field");
        try {
            Field f = this.getClass().getDeclaredField("staticLongField");


            // some valid ops
            try {
                f.set(null, new Long(1)); // ok
                f.set(null, new Byte((byte)1)); // ok - widening conversion
                f.set(null, new Character('1')); // ok - widening conversion
                f.set(null, new Integer(1)); // ok - widening conversion
            }
            catch(IllegalArgumentException iae){
                check_condition(false, "Error: static set(int) failed!");
            }

            try { // wrong type
                f.set(null, "");
                check_condition(false, "Error: static prim set(\"\") succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try { // wrong type
                f.set(null, null);
                check_condition(false, "Error: static set(null) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try { // narrowing conversion
                f.set(null, new Float(1.0f));
                check_condition(false, "Error: static set(Float) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

            // some valid ops
            try {
                Long L = (Long)f.get(null);
                long l = f.getLong(null);
                float ff = f.getFloat(null); // ok - widening conversion
                double d = f.getDouble(null); // ok - widening conversion
            }
            catch(Throwable t) {
                check_condition(false, t.toString());
                t.printStackTrace();
            }

            try { // narrowing conversion
                byte b = f.getByte(null);
                check_condition(false, "Error: static getByte succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }
    }

    // our target method
    static int staticMethod(Helper h, int i) {
        return i;
    }

    void testStaticMethodInvoke() {
        setModule("Test Static Method.invoke()");

	try {
	    // Verify that we can reflectively find and invoke a
	    // method that takes a synthetic caller-class parameter.
	    Method m = Class.class.getMethod("forName",
					     new Class[] { String.class });
	    Object ret = m.invoke(null, new Object[] { getClass().getName() });
	    check_condition(ret == getClass(), "can't reflectively call forName");
	} catch (InvocationTargetException e) {
	    fail(e.getTargetException());
	} catch (Exception e) {
	    fail(e);
	}
        Helper h1 = new Helper(false, false);
        Helper h2 = new Helper(false, false) {}; // subclass
        try {
            Method m = this.getClass().getDeclaredMethod("staticMethod", 
                                  new Class[]{ Helper.class, int.class });

            // some valid ops
            try {
                m.invoke(null, new Object[] { null, new Integer(1)});  // ok
                m.invoke(null, new Object[] { h1, new Integer(1)});  // ok
                m.invoke(null, new Object[] { h2, new Integer(1)});  // ok
                m.invoke(null, new Object[] { h2, new Byte((byte)1)});  // ok - widening conversion
            }
            catch(IllegalArgumentException iae){
                check_condition(false, "Error: static invoke(int) failed!");
            }

            // wrong args types
            try {
                m.invoke(null, new Object[] { h1, ""});
                check_condition(false, "Error: static invoke(h1, \"\") succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try {
                m.invoke(null, new Object[] { new Integer(1), new Integer(1)});
                check_condition(false, "Error: static invoke(Integer, 1) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try {
                m.invoke(null, new Object[] { new Object(), null });
                check_condition(false, "Error: static invoke(Object, null) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }
            try {
                m.invoke(null, new Object[] { h1, new Long(1) });
                check_condition(false, "Error: static invoke(h1, long) succeeded!");
            }
            catch(IllegalArgumentException iae){
                // ok
            }

        }
        catch(Throwable t) {
            check_condition(false, t.toString());
            t.printStackTrace();
        }
    }

}
