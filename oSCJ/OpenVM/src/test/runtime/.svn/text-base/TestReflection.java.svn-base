package test.runtime;

import ovm.core.domain.DomainDirectory;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.ReflectiveMethod;
import ovm.core.domain.WildcardException;
import ovm.core.execution.InvocationMessage;
import ovm.core.execution.ReturnMessage;
import ovm.core.services.memory.VM_Address;
import test.common.TestBase;

/**
 * Test OVM executive domain reflection.
 * @author Krzysztof Palacz, David Holmes
 **/
public class TestReflection extends TestBase {
    private Oop asOop(Object o) {
	return VM_Address.fromObject(o).asOop();
    }

    static int val;

    public void doSomething() {
	val = 43;
    }

    public static void doSomethingStatic() {
	val = 666;
    }

    public int increment(int argument) {
	return argument + 1;
    }
    
     void addToVal(int dummy0, int toAdd, int dummy1) {
	val += toAdd;
    }

    public String incrementedString(int argument) {
	return "" + increment(argument);
    }

    public void throwSomething() {
	throw new Error();

    }

    public TestReflection() {
	super("Reflection");
    }

    static TestReflection thiz;

    public void run() {
        thiz = this;
	try {
	    testInstance();
	    testStatic();
	    testIntArg();
	    testIncrement(0);
	    testIncrement(10);
	    testString();
	    testThrowing();
	    testAllArgs();
	    // Any method that uses all callee-saved regs causes
	    // problems with apple's gcc on PPC.
	    // testManyArgs();
	    testAllReturns();
	    testBadReceiver();
	} catch (LinkageException e) {
	    throw e.unchecked();
	}
    }

    private static ReflectiveMethod doSomething
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Ltest/runtime/TestReflection;",
			       "doSomething:()V");
    
    void testInstance() {
	doSomething.call(asOop(this));
	check_condition(val == 43, "instance method effect");
    }


    private static ReflectiveMethod throwSomething
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Ltest/runtime/TestReflection;",
			       "throwSomething:()V");
    
    void testThrowing() {
	try {
	    throwSomething.call(asOop(this));
	    COREfail("no exception thrown");
	} catch (WildcardException msg) {
	    check_condition(msg.getExecutiveThrowable() instanceof Error, " executive exception");
	    check_condition(msg.getUserThrowable() == null, "user exception");
	}
    }

    private static ReflectiveMethod addToVal
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Ltest/runtime/TestReflection;",
			       "addToVal:(III)V");
    
    void testIntArg() throws LinkageException {
	int delta = 10;
	int oldVal = val;
        InvocationMessage msg = addToVal.makeMessage();
        msg.getInArgAt(0).setInt(-1);
        msg.getInArgAt(1).setInt(delta);
        msg.getInArgAt(2).setInt(-1);
        msg.invoke(asOop(this));
	check_condition(val == oldVal + delta, "return value "
		   + val + " != " + (oldVal + delta));
    }


    private static ReflectiveMethod doSomethingStatic
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "doSomethingStatic:()V");
    
    void testStatic() {
	doSomethingStatic.call(null);
	check_condition(val == 666, "static method effect");
    }


    private static ReflectiveMethod increment
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Ltest/runtime/TestReflection;",
			       "increment:(I)I");
    
    void testIncrement(int value) throws LinkageException {
        InvocationMessage msg = increment.makeMessage();
        msg.getInArgAt(0).setInt(value);
        ReturnMessage rmsg = msg.invoke(asOop(this));
        int v = rmsg.getReturnValue().getInt();
        check_condition(v == value + 1,
                        "ret value " + v + " != " + (value+1));
    }
    
    private static ReflectiveMethod incrementedString
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Ltest/runtime/TestReflection;",
			       "incrementedString:(I)Ljava/lang/String;");
    
    void testString() throws LinkageException {
        InvocationMessage msg = incrementedString.makeMessage();
        msg.getInArgAt(0).setInt(10);
        ReturnMessage rmsg = msg.invoke(asOop(this));
        Object o = rmsg.getReturnValue().getOop();
        String str = (String)o;
        check_condition(str.equals("11"),
                        " got string \"" + str + "\" not \"11\"");
    }


        
    // two values per type
    static final int[] intArg = {1, -1};
    static final long[] longArg = {1L, -1L};
    static final float[] floatArg =  {1.0f, -1.0f};
    static final double[] doubleArg = {1.0d, -1.0d};
    static final byte[] byteArg = {1, -1};
    static final char[] charArg = { 'A', 'Z'};
    static final short[] shortArg = {1, -1};
    static final boolean[] booleanArg = {true, false};


    static void testMany( int _0, int _1, long _2, long _3,
                          float _4, float _5, double _6, double _7,
                          char _8, char _9, short _10, short _11,
                          byte _12, byte _13, boolean _14, boolean _15) 
        {
        
        thiz.check_condition( _0 == intArg[0], "int Expected " + intArg[0] + " got " + _0);
        thiz.check_condition( _1 == intArg[1], "int Expected " + intArg[1] + " got " + _1);
        thiz.check_condition( _2 == longArg[0], "long Expected " + longArg[0] + " got " + _2);
        thiz.check_condition( _3 == longArg[1], "long Expected " + longArg[1] + " got " + _3);
        thiz.check_condition( _4 == floatArg[0], "float Expected " + floatArg[0] + " got " + _4);
        thiz.check_condition( _5 == floatArg[1], "float Expected " + floatArg[1] + " got " + _5);
        thiz.check_condition( _6 == doubleArg[0], "double Expected " + doubleArg[0] + " got " + _6);
        thiz.check_condition( _7 == doubleArg[1], "double Expected " + doubleArg[1] + " got " + _7);
        thiz.check_condition( _8 == charArg[0], "char Expected " + charArg[0] + " got " + _8);
        thiz.check_condition( _9 == charArg[1], "char Expected " + charArg[1] + " got " + _9);
        thiz.check_condition( _10 == shortArg[0], "short Expected " + shortArg[0] + " got " + _10);
        thiz.check_condition( _11 == shortArg[1], "short Expected " + shortArg[1] + " got " + _11);
        thiz.check_condition( _12 == byteArg[0], "byte Expected " + byteArg[0] + " got " + _12);
        thiz.check_condition( _13== byteArg[1], "byte Expected " + byteArg[1] + " got " + _13);
        thiz.check_condition( _14 == booleanArg[0], "boolean Expected " + booleanArg[0] + " got " + _14);
        thiz.check_condition( _15 == booleanArg[1], "boolean Expected " + booleanArg[1] + " got " + _15);
        
    }        


    static void testInt( int _0, int _1) {
        thiz.check_condition( _0 == intArg[0], "int Expected " + intArg[0] + " got " + _0);
        thiz.check_condition( _1 == intArg[1], "int Expected " + intArg[1] + " got " + _1);
    }

    static void testLong( long _0, long _1) {
        thiz.check_condition( _0 == longArg[0], "long Expected " + longArg[0] + " got " + _0);
        thiz.check_condition( _1 == longArg[1], "long Expected " + longArg[1] + " got " + _1);
    }

    static void testFloat( float _0, float _1) {
        thiz.check_condition( _0 == floatArg[0], "float Expected " + floatArg[0] + " got " + _0);
        thiz.check_condition( _1 == floatArg[1], "float Expected " + floatArg[1] + " got " + _1);
    }

    static void testDouble( double _0, double _1) {
        thiz.check_condition( _0 == doubleArg[0], "double Expected " + doubleArg[0] + " got " + _0);
        thiz.check_condition( _1 == doubleArg[1], "double Expected " + doubleArg[1] + " got " + _1);
    }

    static void testByte( byte _0, byte _1) {
        thiz.check_condition( _0 == byteArg[0], "byte Expected " + byteArg[0] + " got " + _0);
        thiz.check_condition( _1 == byteArg[1], "byte Expected " + byteArg[1] + " got " + _1);
    }

    static void testChar( char _0, char _1) {
        thiz.check_condition( _0 == charArg[0], "char Expected " + charArg[0] + " got " + _0);
        thiz.check_condition( _1 == charArg[1], "char Expected " + charArg[1] + " got " + _1);
    }

    static void testShort( short _0, short _1) {
        thiz.check_condition( _0 == shortArg[0], "short Expected " + shortArg[0] + " got " + _0);
        thiz.check_condition( _1 == shortArg[1], "short Expected " + shortArg[1] + " got " + _1);
    }

    static void testBoolean( boolean _0, boolean _1) {
        thiz.check_condition( _0 == booleanArg[0], "boolean Expected " + booleanArg[0] + " got " + _0);
        thiz.check_condition( _1 == booleanArg[1], "boolean Expected " + booleanArg[1] + " got " + _1);
    }
    

    private static ReflectiveMethod testMany
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testMany:(IIJJFFDDCCSSBBZZ)V");
    
    void testManyArgs() throws LinkageException {
        InvocationMessage msg = testMany.makeMessage();
        msg.getInArgAt(0).setInt(intArg[0]);
        msg.getInArgAt(1).setInt(intArg[1]);
        msg.getInArgAt(2).setLong(longArg[0]);
        msg.getInArgAt(3).setLong(longArg[1]);
        msg.getInArgAt(4).setFloat(floatArg[0]);
        msg.getInArgAt(5).setFloat(floatArg[1]);
        msg.getInArgAt(6).setDouble(doubleArg[0]);
        msg.getInArgAt(7).setDouble(doubleArg[1]);
        msg.getInArgAt(8).setChar(charArg[0]);
        msg.getInArgAt(9).setChar(charArg[1]);
        msg.getInArgAt(10).setShort(shortArg[0]);
        msg.getInArgAt(11).setShort(shortArg[1]);
        msg.getInArgAt(12).setByte(byteArg[0]);
        msg.getInArgAt(13).setByte(byteArg[1]);
        msg.getInArgAt(14).setBoolean(booleanArg[0]);
        msg.getInArgAt(15).setBoolean(booleanArg[1]);
        msg.invoke(testMany.getStaticReceiver());
    }

    private static ReflectiveMethod testByte
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testByte:(BB)V");
    private static ReflectiveMethod testChar
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testChar:(CC)V");
    private static ReflectiveMethod testShort
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testShort:(SS)V");
    private static ReflectiveMethod testInt
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testInt:(II)V");
    private static ReflectiveMethod testFloat
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testFloat:(FF)V");
    private static ReflectiveMethod testBoolean
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testBoolean:(ZZ)V");
    private static ReflectiveMethod testLong
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testLong:(JJ)V");
    private static ReflectiveMethod testDouble
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "testDouble:(DD)V");

    void testAllArgs() throws LinkageException {
        InvocationMessage msg = null;

        msg = testByte.makeMessage();
        msg.getInArgAt(0).setByte(byteArg[0]);
        msg.getInArgAt(1).setByte(byteArg[1]);
        msg.invoke(testByte.getStaticReceiver());

        msg = testChar.makeMessage();
        msg.getInArgAt(0).setChar(charArg[0]);
        msg.getInArgAt(1).setChar(charArg[1]);
        msg.invoke(testByte.getStaticReceiver());

        msg = testShort.makeMessage();
        msg.getInArgAt(0).setShort(shortArg[0]);
        msg.getInArgAt(1).setShort(shortArg[1]);
        msg.invoke(testByte.getStaticReceiver());

        msg = testInt.makeMessage();
        msg.getInArgAt(0).setInt(intArg[0]);
        msg.getInArgAt(1).setInt(intArg[1]);
        msg.invoke(testByte.getStaticReceiver());

        msg = testFloat.makeMessage();
        msg.getInArgAt(0).setFloat(floatArg[0]);
        msg.getInArgAt(1).setFloat(floatArg[1]);
        msg.invoke(testByte.getStaticReceiver());

        msg = testBoolean.makeMessage();
        msg.getInArgAt(0).setBoolean(booleanArg[0]);
        msg.getInArgAt(1).setBoolean(booleanArg[1]);
        msg.invoke(testByte.getStaticReceiver());

        msg = testLong.makeMessage();
        msg.getInArgAt(0).setLong(longArg[0]);
        msg.getInArgAt(1).setLong(longArg[1]);
        msg.invoke(testByte.getStaticReceiver());

        msg = testDouble.makeMessage();
        msg.getInArgAt(0).setDouble(doubleArg[0]);
        msg.getInArgAt(1).setDouble(doubleArg[1]);
        msg.invoke(testByte.getStaticReceiver());
    }

    int testVal = 0;

    void updateTestVal() {
        testVal++;
    }

    private static ReflectiveMethod updateTestVal
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Ltest/runtime/TestReflection;",
			       "updateTestVal:()V");
    

    // this test assumes we have receiver checking enabled - which by default
    // in the Executive domain it won't be
    void testBadReceiver() {
        try {
	    updateTestVal.call(asOop(new Object()));
            check_condition(false, "illegal receiver accepted");
        }
        catch (ovm.util.OVMError.IllegalArgument expected) {}
    }
    

    static byte byteReturn(String arg) {
        if (arg == "min") return Byte.MIN_VALUE;
        else if (arg == "max") return Byte.MAX_VALUE;
        else return 0;
    }
    static char charReturn(String arg) {
        if (arg == "min") return Character.MIN_VALUE;
        else if (arg == "max") return Character.MAX_VALUE;
        else return 0;
    }
    static short shortReturn(String arg) {
        if (arg == "min") return Short.MIN_VALUE;
        else if (arg == "max") return Short.MAX_VALUE;
        else return 0;
    }
    static int intReturn(String arg) {
        if (arg == "min") return Integer.MIN_VALUE;
        else if (arg == "max") return Integer.MAX_VALUE;
        else return 0;
    }
    static long longReturn(String arg) {
        if (arg == "min") return Long.MIN_VALUE;
        else if (arg == "max") return Long.MAX_VALUE;
        else return 0;
    }
    static float floatReturn(String arg) {
        if (arg == "min") return Float.MIN_VALUE;
        else if (arg == "max") return Float.MAX_VALUE;
        else return 0;
    }
    static double doubleReturn(String arg) {
        if (arg == "min") return Double.MIN_VALUE;
        else if (arg == "max") return Double.MAX_VALUE;
        else return 0;
    }

    private static ReflectiveMethod testByteReturn
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "byteReturn:(Ljava/lang/String;)B");
    private static ReflectiveMethod testCharReturn
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "charReturn:(Ljava/lang/String;)C");
    private static ReflectiveMethod testShortReturn
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "shortReturn:(Ljava/lang/String;)S");
    private static ReflectiveMethod testIntReturn
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "intReturn:(Ljava/lang/String;)I");
    private static ReflectiveMethod testFloatReturn
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "floatReturn:(Ljava/lang/String;)F");
    private static ReflectiveMethod testLongReturn
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "longReturn:(Ljava/lang/String;)J");
    private static ReflectiveMethod testDoubleReturn
	= new ReflectiveMethod(DomainDirectory.getExecutiveDomain(),
			       "Gtest/runtime/TestReflection;",
			       "doubleReturn:(Ljava/lang/String;)D");

    void testAllReturns() {
	// - byte
        byte byteResult = testByteReturn.call(null, asOop("min")).getByte();
        check_condition(byteResult==Byte.MIN_VALUE, 
                        "byte return: expected " + Byte.MIN_VALUE + 
                        " got " + byteResult);
        byteResult = testByteReturn.call(null, asOop("max")).getByte();
        check_condition(byteResult==Byte.MAX_VALUE, 
                        "byte return: expected " + Byte.MAX_VALUE + 
                        " got " + byteResult);
        byteResult = testByteReturn.call(null, (Oop)null).getByte();
        check_condition(byteResult==0,
                        "byte return: expected " + 0 + 
                        " got " + byteResult);

	// - short
        short shortResult = testShortReturn.call(null, asOop("min")).getShort();
        check_condition(shortResult==Short.MIN_VALUE, 
                        "short return: expected " + Short.MIN_VALUE + 
                        " got " + shortResult);
        shortResult = testShortReturn.call(null, asOop("max")).getShort();
        check_condition(shortResult==Short.MAX_VALUE, 
                        "short return: expected " + Short.MAX_VALUE + 
                        " got " + shortResult);
        shortResult = testShortReturn.call(null, (Oop)null).getShort();
        check_condition(shortResult==0,
                        "short return: expected " + 0 + 
                        " got " + shortResult);

	// - char
        char charResult = testCharReturn.call(null, asOop("min")).getChar();
        check_condition(charResult==Character.MIN_VALUE, 
                        "char return: expected " + Character.MIN_VALUE + 
                        " got " + charResult);
        charResult = testCharReturn.call(null, asOop("max")).getChar();
        check_condition(charResult==Character.MAX_VALUE, 
                        "char return: expected " + Character.MAX_VALUE + 
                        " got " + charResult);
        charResult = testCharReturn.call(null, (Oop)null).getChar();
        check_condition(charResult==0,
                        "char return: expected " + 0 + 
                        " got " + charResult);

	// - int
        int intResult = testIntReturn.call(null, asOop("min")).getInt();
        check_condition(intResult==Integer.MIN_VALUE, 
                        "int return: expected " + Integer.MIN_VALUE + 
                        " got " + intResult);
        intResult = testIntReturn.call(null, asOop("max")).getInt();
        check_condition(intResult==Integer.MAX_VALUE, 
                        "int return: expected " + Integer.MAX_VALUE + 
                        " got " + intResult);
        intResult = testIntReturn.call(null, (Oop)null).getInt();
        check_condition(intResult==0,
                        "int return: expected " + 0 + 
                        " got " + intResult);

	// - long
        long longResult = testLongReturn.call(null, asOop("min")).getLong();
        check_condition(longResult==Long.MIN_VALUE, 
                        "long return: expected " + Long.MIN_VALUE + 
                        " got " + longResult);
        longResult = testLongReturn.call(null, asOop("max")).getLong();
        check_condition(longResult==Long.MAX_VALUE, 
                        "long return: expected " + Long.MAX_VALUE + 
                        " got " + longResult);
        longResult = testLongReturn.call(null, (Oop)null).getLong();
        check_condition(longResult==0,
                        "long return: expected " + 0 + 
                        " got " + longResult);

	// - float
        float floatResult = testFloatReturn.call(null, asOop("min")).getFloat();
        check_condition(floatResult==Float.MIN_VALUE, 
                        "float return: expected " + Float.MIN_VALUE + 
                        " got " + floatResult);
        floatResult = testFloatReturn.call(null, asOop("max")).getFloat();
        check_condition(floatResult==Float.MAX_VALUE, 
                        "float return: expected " + Float.MAX_VALUE + 
                        " got " + floatResult);
        floatResult = testFloatReturn.call(null, (Oop)null).getFloat();
        check_condition(floatResult==0,
                        "float return: expected " + 0 + 
                        " got " + floatResult);

	// - double
        double doubleResult = testDoubleReturn.call(null, asOop("min")).getDouble();
        check_condition(doubleResult==Double.MIN_VALUE, 
                        "double return: expected " + Double.MIN_VALUE + 
                        " got " + doubleResult);
        doubleResult = testDoubleReturn.call(null, asOop("max")).getDouble();
        check_condition(doubleResult==Double.MAX_VALUE, 
                        "double return: expected " + Double.MAX_VALUE + 
                        " got " + doubleResult);
        doubleResult = testDoubleReturn.call(null, (Oop)null).getDouble();
        check_condition(doubleResult==0,
                        "double return: expected " + 0 + 
                        " got " + doubleResult);

    }



} // end of TestReflection
