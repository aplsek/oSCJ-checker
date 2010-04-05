package test;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.lang.Integer;

// authors: JT, HY, DH

// FIXME: this is a JVM test not a basic user-domain test
public class TestReflection extends TestBase {
    
    static TestReflection instance;
    
    public TestReflection(Harness domain) {
	super("Reflection", domain);
	instance = this;
    }
    
    public void run() {
        testBasicProperties();
	testClass();
	testConstructor();
	testMethod();
	testField();
        testFieldTypes();
	testArray();
    }

    // a class not referred to other than as a field type. This class may
    // not be loaded when we ask the field for its type.

    // Except that we don't support classloading!
    private static ExternalClass _ = new ExternalClass();
    public static class ExternalClass {
    }
    
    MyClass myclass = new MyClass(); // add to closure ...
    public static class MyClass { 
	public int field;
	public MyClass field1;
	public String field2;
	private int field3;

	public int     intField;
	public boolean booleanField;
	public short   shortField;
	public char    charField;
	public byte    byteField;
	public float   floatField;
	public double  doubleField;
	public long    longField;
	public Object  objectField;

        // we never set this
	ExternalClass ecField = null;


        // note: all three constructors are used directly as well as
        // reflectively so we don't need to flag them for j2c

	public MyClass() {
	    // instance.p("MyClass constructor\n");
	    field = 10;
	    intField = 1;
	    booleanField = true;
	    shortField = 1;
	    charField = 'c';
	    byteField = 1;
	    floatField = 1.0f;
	    doubleField = 1.0;
	    longField = 1;
	    objectField = this;
	}
	public MyClass(String s) {
	    field2 = s;
	}
	public MyClass(String s, int i) {
	    this(s);
	    field = i;
	}

	public static int staticMethod(String s) {
	    return 0;
	}
	public int methodA(String s) {
	    return 0;
	}
	public int methodA(String s, int b) {
	    return 0;
	}
	public static int staticAdd(int a, int b) {
	    return (a+b);
	}
    	public int nonstaticAdd(int a, int b) {
	    return (a+b);
	}
    }

    MyFinalClass myFinalClass; // add to closure...
    public static final class MyFinalClass extends MyClass{}

    MyAbstractClass myAbstractClass = new MyAbstractClass() { };
    public abstract class MyAbstractClass{}

    MyInterface myInterface = new MyInterface() { };
    public interface MyInterface{}

    ExceptionFoo  myExceptionFoo = new ExceptionFoo();
    private static class ExceptionFoo extends Exception{};
    ExceptionBar  myExceptionBar = new ExceptionBar();
    private static class ExceptionBar extends Exception{};

    MyUnrelatedClass myUnrelatedClass = new MyUnrelatedClass(null, null);
    public static class MyUnrelatedClass{
	private MyUnrelatedClass(Integer foo, String bar){}
	public MyUnrelatedClass(Integer foo, String bar, Integer foobar)
	    throws ExceptionFoo, ExceptionBar
	{}
    }


    // the the basic queries about a class: isInterface, isArray, getModifiers
    // etc
    void testBasicProperties() {
        checkPrimitiveClass(void.class);
        checkPrimitiveClass(int.class);
        checkObjectClass();
        checkArrayClass(Object[].class);
        checkArrayClass(int[].class);
        checkComparableInterfaceClass();
    }


    void checkPrimitiveClass(Class c) {
        COREassert(c.isPrimitive(), "not primitive");
        COREassert(!c.isArray(), "was array");
        COREassert(!c.isInterface(), "was interface");
        int mods = c.getModifiers();
        COREassert(Modifier.isPublic(mods), "not public");
        COREassert(!Modifier.isProtected(mods), "was protected");
        COREassert(!Modifier.isPrivate(mods), "was private");
        COREassert(Modifier.isFinal(mods), "not final");
        COREassert(!Modifier.isInterface(mods), "was interface (mods)");
    }

    void checkArrayClass(Class c) {
        Class t = c.getComponentType();
        COREassert(!c.isPrimitive(), "was primitive");
        COREassert(c.isArray(), "not array");
        COREassert(!c.isInterface(), "was interface");
        int mods = c.getModifiers();
        int t_mods = t.getModifiers();
        COREassert(Modifier.isPublic(mods) == Modifier.isPublic(t_mods) &&
                   Modifier.isProtected(mods) == Modifier.isProtected(t_mods) &&
                   Modifier.isPrivate(mods) == Modifier.isPrivate(t_mods),
                   "access modifier mismatch");
        COREassert(Modifier.isFinal(mods), "not final");
        COREassert(!Modifier.isInterface(mods), "was interface (mods)");
    }


    void checkObjectClass() {
        Class c = Object.class;
        COREassert(!c.isPrimitive(), "was primitive");
        COREassert(!c.isArray(), "was array");
        COREassert(!c.isInterface(), "was interface");
        int mods = c.getModifiers();
        COREassert(Modifier.isPublic(mods), "not public");
        COREassert(!Modifier.isProtected(mods), "was protected");
        COREassert(!Modifier.isPrivate(mods), "was private");
        COREassert(!Modifier.isFinal(mods), "was final");
        COREassert(!Modifier.isInterface(mods), "was interface (mods)");
        COREassert(!Modifier.isAbstract(mods), "was abstract");
        COREassert(!Modifier.isStrict(mods), "was strict");

        // these shouldn't be set for a (top-level) class or interface
        COREassert(!Modifier.isNative(mods), "was native");
        COREassert(!Modifier.isStatic(mods), "was static");
        COREassert(!Modifier.isSynchronized(mods), "was synchronized");
        COREassert(!Modifier.isTransient(mods), "was transient");
        COREassert(!Modifier.isVolatile(mods), "was volatile");
    }

    void checkComparableInterfaceClass() {
        Class c = Comparable.class;
        COREassert(!c.isPrimitive(), "was primitive");
        COREassert(!c.isArray(), "was array");
        COREassert(c.isInterface(), "not interface");
        int mods = c.getModifiers();
        COREassert(Modifier.isPublic(mods), "not public");
        COREassert(!Modifier.isProtected(mods), "was protected");
        COREassert(!Modifier.isPrivate(mods), "was private");
        COREassert(!Modifier.isFinal(mods), "was final");
        COREassert(Modifier.isInterface(mods), "not interface (mods)");
        COREassert(Modifier.isAbstract(mods), "not abstract");

        // these shouldn't be set for a (top-level) class or interface
        COREassert(!Modifier.isNative(mods), "was native");
        COREassert(!Modifier.isStatic(mods), "was static");
        COREassert(!Modifier.isSynchronized(mods), "was synchronized");
        COREassert(!Modifier.isTransient(mods), "was transient");
        COREassert(!Modifier.isVolatile(mods), "was volatile");
    }



    public void testClass() {
	MyClass myclass = new MyClass();
	MyFinalClass myfinalclass = new MyFinalClass();

        try {
            COREassert(TestReflection.class == Class.forName("test.TestReflection"),".class != forName");
            COREassert(Class.forName("test.TestReflection") == Class.forName("test.TestReflection"),"forName != forName");
        }
        catch(ClassNotFoundException ex) {
            COREassert(false, "got ClassNotFoundException");
        }
	COREassert(! TestReflection.class.isAssignableFrom(TestBase.class));
	COREassert(TestBase.class.isAssignableFrom(TestReflection.class));
	COREassert(TestReflection.class.isAssignableFrom(TestReflection.class));
	COREassert(MyClass.class.isInstance(myclass), "isInstance failed");
	COREassert(MyClass.class.isInstance(myfinalclass), "isInstance failed");
	COREassert(! MyFinalClass.class.isInstance(myclass), "isInstance failed");
	COREassert(! MyUnrelatedClass.class.isInstance(myclass ), "isInstance failed");
	

	//d("testing Class.getDeclaredConstructors()");
	Constructor[] ucCons;
	ucCons = MyUnrelatedClass.class.getDeclaredConstructors();
	//for(int i=0; i<ucCons.length; i++){
	//    d(" " + ucCons[i].toString());
	//}
	
	//d("testing Class.getDeclaredConstructor");
	try{
	    for(int i=0; i<ucCons.length; i++){
		Constructor cons1 = ucCons[i];
		Class[] args = cons1.getParameterTypes();
		Constructor cons2 = MyUnrelatedClass.class.getDeclaredConstructor(args);
		//d("retrieved cons: " + cons2.toString());
		COREassert(cons1==cons2, "asked for the same constructor; did not get it");
	    }
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get declared constructor" + e.getMessage());
	}


	//d("testing Class.getConstructor");
	// FIXME check that the private constructor can't be accessed (conformance to spec)
	try{
	    Class[] args = {Integer.class, String.class, Integer.class};
	    Constructor cons = MyUnrelatedClass.class.getConstructor(args);
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get public constructor" + e.getMessage());
	}
	String ss="";
	//d("testing Class.getDeclaredMethods()");
	Method[] ucMeths;
	ucMeths = MyClass.class.getDeclaredMethods();
	for(int i=0; i<ucMeths.length; i++){
	    ss+=(" " + ucMeths[i].toString());
	}

	//d("testing Class.getMethods()");
	Method[] ucMeths2;
	ucMeths2 = MyClass.class.getMethods();
	for(int i=0; i<ucMeths2.length; i++){
	    ss+=(" " + ucMeths2[i].toString());
	}
	
	//d("testing Class.getModifiers");
	int modifiers = MyFinalClass.class.getModifiers();
// 	d("modifiers for MyFinalClass: " + Integer.toBinaryString(modifiers));
	COREassert(Modifier.isPublic(modifiers),
		   "Modifier.isPublic(modifiers) should be true");
	COREassert(Modifier.isFinal(modifiers),
		   "Modifier.isFinal(modifiers) should be true");
	modifiers = MyAbstractClass.class.getModifiers();
	COREassert(Modifier.isAbstract(modifiers),
		   "Modifier.isAbstract(modifiers) should be true");
	modifiers = MyInterface.class.getModifiers();
	COREassert(Modifier.isInterface(modifiers),
		   "Modifier.isInterface(modifiers) should be true");
    }

    public void testConstructor() {
	Constructor cons;
	MyClass o;
	try {
	    cons = MyClass.class.getConstructor(new Class[0]);
	    o = (MyClass)cons.newInstance(new Object[0]);
	    COREassert(o.field == 10);
	    cons = MyClass.class.getConstructor(new Class[] {String.class});
	    o = (MyClass)cons.newInstance(new Object[]{"MYCLASS"});
	    COREassert(o.field2.equals("MYCLASS"));
	    cons = MyClass.class.getConstructor(new Class[] {String.class, Integer.TYPE});
	    o = (MyClass)cons.newInstance(new Object[]{"MYCLASS", new Integer(100)});
	    COREassert(o.field2.equals("MYCLASS"));
	    COREassert(o.field == 100);
	} catch (NoSuchMethodException e) {
	    COREfail("Could not get public constructor" + e.getMessage());
	} catch (IllegalAccessException e) {
	    COREfail("Tryed to invoke a private or protected constructor");
 	} catch (InvocationTargetException e) {
 	    COREfail("InvocationTargetException");
	} catch (InstantiationException e) {
	    COREfail("exception initializing " + e);
	}

	//d("testing Constructor.getParameterTypes()");
	try{
	    Class[] args = {Integer.class, String.class, Integer.class};
	    Constructor cons2 = MyUnrelatedClass.class.getConstructor(args);
	    Class[] params = cons2.getParameterTypes();
	    
	    for (int i=0; i<args.length ; i++){
// 		d("argument_"+i+" : "
// 		  + args[i].toString() + " parameter_"+i+" : " 
// 		  + params[i].toString());
		COREassert(args[i] == params[i], "argument_"+i+" : "
			   + args[i].toString() + " parameter_"+i+" : " 
			   + params[i].toString());
	    }
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get public constructor" + e.getMessage());
	}

	//d("testing Constructor.getExceptionTypes()");
	try{
	    Class[] args = {Integer.class, String.class, Integer.class};
	    Constructor cons2 = MyUnrelatedClass.class.getConstructor(args);
	    Class[] exceptions = cons2.getExceptionTypes();
	    
	  //  for (int i=0; i<exceptions.length ; i++){
	//	d(" exception_" + i + " : " + exceptions[i].toString());
	  //  }
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get public constructor" + e.getMessage());
	}
    }

    public void testField() {

	try {
	    Field[] df = MyClass.class.getDeclaredFields();
	    //d("Local fields");
	  String s = null;
	    for(int i = 0; i < df.length; i++) {
		s += (df[i].getName() + " : ");
                s += (df[i].getType().toString());
	    }
	    Field[] f = MyClass.class.getFields();
	    for(int i = 0; i < f.length; i++) {
		s+= (f[i].getName() + " : ");
		s+=(f[i].getType().toString());
	    }

	    MyClass mc = new MyClass("MYCLASS", 99);
	    Field field = MyClass.class.getDeclaredField("field");
	    Field field2 = MyClass.class.getDeclaredField("field2");
	    field.setInt(mc, 2);
	    COREassert(mc.field == 2);
	    field2.set(mc, "MC");
	    COREassert(mc.field2.equals("MC"));
	    field.set(mc, new Integer(34));
	    COREassert(mc.field == 34);

	    Field field3 = MyClass.class.getDeclaredField("field3");
	    int mod3 = field3.getModifiers();
	    COREassert(Modifier.isPrivate(mod3));

//	    d("Field.getxxxx");
	    mc = new MyClass();

	    Field intField = MyClass.class.getDeclaredField("intField");
	    int intFieldValue = intField.getInt(mc);
	    COREassert(intFieldValue == 1, "Field.getInt");

	    Field booleanField = MyClass.class.getDeclaredField("booleanField");
	    boolean booleanFieldValue = booleanField.getBoolean(mc);
	    COREassert(booleanFieldValue == true, "Field.getBoolean");

	    Field shortField = MyClass.class.getDeclaredField("shortField");
	    short shortFieldValue = shortField.getShort(mc);
	    COREassert(shortFieldValue == 1, "Field.getShort");

	    Field charField = MyClass.class.getDeclaredField("charField");
	    char charFieldValue = charField.getChar(mc);
	    COREassert(charFieldValue == 'c', "Field.getChar");

	    Field byteField = MyClass.class.getDeclaredField("byteField");
	    byte byteFieldValue = byteField.getByte(mc);
	    COREassert(byteFieldValue == 1, "Field.getByte");

	    Field floatField = MyClass.class.getDeclaredField("floatField");
	    float floatFieldValue = floatField.getFloat(mc);
	    COREassert(floatFieldValue == 1.0f, "Field.getFloat");

	    Field doubleField = MyClass.class.getDeclaredField("doubleField");
	    double doubleFieldValue = doubleField.getDouble(mc);
	    COREassert(doubleFieldValue == 1.0, "Field.getDouble");

	    Field longField = MyClass.class.getDeclaredField("longField");
	    long longFieldValue = longField.getLong(mc);
	    COREassert(longFieldValue == 1, "Field.getLong");

	    Field objectField = MyClass.class.getDeclaredField("objectField");
	    Object objectFieldValue = objectField.get(mc);
	    COREassert(objectFieldValue == mc, "Field.get");

	} catch(Exception e) {
	    COREfail(""+e);
	}
    }


    void testFieldTypes() {
        setModule("Field Types");
        Field[] fields = MyClass.class.getDeclaredFields();
        for(int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals("ecField")) 
                COREassert(fields[i].getType() == ExternalClass.class, "ExternalClass type failure");
            if (fields[i].getName().equals("byteField")) 
                COREassert(fields[i].getType() == Byte.TYPE, "byte type failure");
            else if (fields[i].getName().equals("shortField")) 
                COREassert(fields[i].getType() == Short.TYPE, "short type failure");
            else if (fields[i].getName().equals("intField")) 
                COREassert(fields[i].getType() == Integer.TYPE, "int type failure");
            else if (fields[i].getName().equals("longField")) 
                COREassert(fields[i].getType() == Long.TYPE, "long type failure");
            else if (fields[i].getName().equals("charField")) 
                COREassert(fields[i].getType() == Character.TYPE, "char type failure");
            else if (fields[i].getName().equals("booleanField")) 
                COREassert(fields[i].getType() == Boolean.TYPE, "boolean type failure");
            else if (fields[i].getName().equals("floatField")) 
                COREassert(fields[i].getType() == Float.TYPE, "float type failure");
            else if (fields[i].getName().equals("doubleField")) 
                COREassert(fields[i].getType() == Double.TYPE, "double type failure");
            else if (fields[i].getName().equals("objectField")) 
                COREassert(fields[i].getType() == Object.class, "Object type failure");
        }
    }

    
    public void testArray(){
        setModule("Testing Array");

	/* just to include the array types in the type closure.
	 * otherwise, the VM dies with :
	 * # OVM Log: will die: trying to load  Type{[xxxxxxxxx,SysTC{UserDomain_1::BundlePath[Bundle[Sealed]]}}
	 * where xxxxxxxxx is the base type of the array that you try to create
	 */
	Integer arrayInt[];
	int arrayint[];

        int len = 3;

	/* array of object */
	Object myArray1 = Array.newInstance(Integer.class, len);
	COREassert(myArray1 != null, "null from Array.newInstance");
	COREassert(Array.getLength(myArray1) == len,
		   "wrong array length: " + Array.getLength(myArray1) +
                   " expected " + len);
        COREassert(myArray1.getClass().isArray(), "!getClass().isArray()");
        COREassert(myArray1 instanceof Integer[], "wrong array type");
        try {
            arrayInt = (Integer[]) myArray1;
        }
        catch (ClassCastException e) {
            COREassert(false, "ClassCastException on direct cast");
        }
            

	/* array of base type */ 
	Object myArray2 = Array.newInstance(Integer.TYPE, len);
	COREassert(myArray2 != null, "Could not create an array reflectively");
	COREassert(Array.getLength(myArray2) == len,
		   "The returned array is not of the proper length");

    }

    public void testMethod() {
        setModule("Testing Method");
	Method  method;
	MyClass o = new MyClass();

	try{
	    Class[] args = {String.class};
	    method = MyClass.class.getMethod("methodA", args);
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get public method methodA(String)" + e.getMessage());
	}
	try{
	    Class[] args = {String.class, Integer.TYPE};
	    method = MyClass.class.getMethod("methodA", args);
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get public method methodA(String, int)" + e.getMessage());
	}

	try{
	    Class[] params = {Integer.TYPE, Integer.TYPE};
	    method = MyClass.class.getDeclaredMethod("nonstaticAdd", params);
	    Object[] args = {new Integer(2), new Integer(3)};
	    Object result = method.invoke(o,args);
	    COREassert(((Integer) result).intValue() == 5,
		       "Invalid addition for nonstaticAdd: " + result);
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get public instance method nonstaticAdd(int,int)" + e.getMessage());
	} catch (IllegalAccessException e) {
	    COREassert( false, "Trying to invoke an inacessible method");
	} catch (InvocationTargetException e) {
	    COREassert( false, "Could not invoke method");
	}

	try{
	    Class[] params = {Integer.TYPE, Integer.TYPE};
	    method = MyClass.class.getDeclaredMethod("staticAdd", params);
	    Object[] args = {new Integer(2), new Integer(3)};
	    /* we also check that our implementation handles null pointers
	     * when invoking a static method (conformance to JDK 1.4.2 spec) */
	    Object result = method.invoke(null ,args);
	    COREassert(((Integer) result).intValue() == 5,
		       "Invalid addition for staticAdd");
	} catch (NoSuchMethodException e) {
	    COREassert( false, "Could not get public static method staticAdd(int,int)" + e.getMessage());
	} catch (IllegalAccessException e) {
	    COREassert( false, "Trying to invoke an inacessible method");
	} catch (InvocationTargetException e) {
	    COREassert( false, "Could not invoke method");
	}

    }

}
