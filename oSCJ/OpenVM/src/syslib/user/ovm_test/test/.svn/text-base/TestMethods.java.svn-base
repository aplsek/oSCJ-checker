package test;

public class TestMethods extends TestBase {
    public TestMethods(Harness domain) {
	super("Methods", domain);
    }
    static TestMethods me;
    public void run() {
	me = this;
	testInterfaces();
	testStaticMethods();
    }

    void testInterfaces() {
	IFace1 i1 = c;
	int testval = 5;
	COREassert(42 == i1.imethod2(testval));
	IFace2 i2 = c;
	COREassert(42 == i2.imethod4(testval));
	IFace3 i3 = c;
	COREassert(testval == i3.imethod5(testval));

	COREassert(42 == i3.imethod6());

    }

    void testStaticMethods() {
	COREassert(2 == Class1.smethodA(1, 2, 3));
	COREassert(2 == Class2.smethodB(1, 2, 3, 4));
	COREassert(3 == Class2.smethodA(1, 2));
	COREassert(0 == Class2.smethodC(1, 2));
    }
    
    interface IFace1 {
	int imethod1(int arg);
	int imethod2(int arg);
    }

    interface IFace2 {
	int imethod3(int arg);
	int imethod4(int arg);
    }

    interface IFace3 extends IFace1, IFace2 {
	int imethod5(int arg);
	int imethod6();
    }
	

    static class Class1 implements IFace1 {
	public Class1() {
	}
	int identity(int arg) {
	    return arg;
	}
	final static int int1 = 42;
	final static int int2 = 40;

	public int imethod1(int arg) {
	    return identity(arg);
	}
	public int imethod2(int arg) {
	    return imethod1(arg);
	}
	static int smethodA(int arg, int dummy1, int dummy2) {
	    me.COREassert( int1 - int2 == 2);
	    return arg + 1;
	}
	static int smethodC(int arg, int dummy) {
	    return arg - 1;
	}

    }
    
    static class Class2 extends Class1 implements IFace2 {
	int field;
	public Class2() { }

	public int imethod3(int arg) {
	    return imethod2(arg);
	}
	public int imethod4(int arg) {
	    return imethod2(arg);
	}
	static int smethodA(int arg, int dummy) {
	    return arg + 2;
	}
	static int smethodB(int arg, int dummy1, int dummy2, int dummy3) {
	    return arg + 1;
	}


    }

    static class Class3 extends Class2 implements IFace3 {
	public Class3() { }
	public int imethod5(int arg) {
	    return imethod1(arg);
	}
	public int imethod2(int arg) {
	    // super.imethod2(arg);
	    return 42;
	}
	public int imethod6() {
	    return imethod2(10);
	}
    }
    
    Class3 c = new Class3();

}
