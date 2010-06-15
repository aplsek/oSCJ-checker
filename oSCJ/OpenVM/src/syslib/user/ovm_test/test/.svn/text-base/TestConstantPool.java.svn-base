/**
 **/
package test;

/**
 * @author Hiroshi Yamauchi
 * @author Gergana Markova
 **/
public class TestConstantPool 
    extends TestBase {

    public TestConstantPool(Harness domain) {
	super("ConstantPool", domain);
    }

    /**
     * This runs a series of tests to see if constant pool works. 
     **/ 
    public void run() {
	testInt();
	testLong();
	testFloat();
	testDouble();
	testString();
	testChar();
    }

    /* booleans, shorts and bytes don't come from constant pools */
    public void testChar() {
	setModule("char");
	char c0 = (char)9;
	char c1 = (char)18;
	char c2 = (char)100;
	char c3 = 'A'; // 65
	char c4 = (char)50000;
	COREassert((c0 + c2) == (char)109);
	COREassert((c0 + c3) == (char)74);
	COREassert((c2 - c0) == (char)91);
	COREassert((c4 - c2) == (char)49900);
    }

    public void testInt() {
	setModule("int");
	int int0 = 0x12345678;
	byte byte0 = (byte)(int0 & 0xFF);
	byte byte1 = (byte)((int0 >> 8) & 0xFF);
	byte byte2 = (byte)((int0 >> 16) & 0xFF);
	byte byte3 = (byte)((int0 >> 24) & 0xFF);
	COREassert(byte0 == 0x78);
	COREassert(byte1 == 0x56);
	COREassert(byte2 == 0x34);
	COREassert(byte3 == 0x12);
    }

    public void testLong() {
	setModule("long");
	long long0 = 0L;
	int low0 = 0;
	int high0 = 0;
	long long1 = 1L;
	int low1 = 1;
	int high1 = 0;
	long long2 = -1L;
	int low2 = 0xFFFFFFFF;
	int high2 = 0xFFFFFFFF;
	long long3 = Long.MAX_VALUE;
	int low3 = -1;
	int high3 = Integer.MAX_VALUE;
	long long4 = Long.MIN_VALUE;
	int low4 = 0;
	int high4 = Integer.MIN_VALUE;
	long long5 = 0x00FFFFFFFFFFFFFFL;
	int low5 = -1;
	int high5 = 0x00FFFFFF;
	long long6 = 0xCCFFFFFFFFFFFFFFL;
	int low6 = -1;
	int high6 = 0xCCFFFFFF;
	long long7 = 6L;

	COREassert(((int)(long0 & 0xFFFFFFFF) == low0 &&
		    ((int)((long0 >> 32) & 0xFFFFFFFF)) == high0));
	COREassert(((int)(long1 & 0xFFFFFFFF) == low1 &&
		    ((int)((long1 >> 32) & 0xFFFFFFFF)) == high1));
	COREassert(((int)(long2 & 0xFFFFFFFF) == low2 &&
		    ((int)((long2 >> 32) & 0xFFFFFFFF)) == high2));
	COREassert(((int)(long3 & 0xFFFFFFFF) == low3 &&
		    ((int)((long3 >> 32) & 0xFFFFFFFF)) == high3));
	COREassert(((int)(long4 & 0xFFFFFFFF) == low4 &&
		    ((int)((long4 >> 32) & 0xFFFFFFFF)) == high4));
	COREassert(((int)(long5 & 0xFFFFFFFF) == low5 &&
		    ((int)((long5 >> 32) & 0xFFFFFFFF)) == high5));
	COREassert(((int)(long6 & 0xFFFFFFFF) == low6 &&
		    ((int)((long6 >> 32) & 0xFFFFFFFF)) == high6));
    }

    public void testFloat() {
	setModule("float");
	float f0 = 1.0F;
	float f1 = -1.0F;
	float f2 = 0.0F;
	float f3 = Float.MAX_VALUE;
	float f4 = Float.MIN_VALUE;
	float f5 = 0.1F;
	float f6 = 2.0F + f5;
	float f7 = 4.0F;
	float f8 = 150.0F;
	float f9 = 154.0F;
	COREassert((2.1F == f6));
	COREassert(((f7 + f8) == f9));
    }

    public void testDouble() {
	setModule("double");
	double d0 = 1.0;
	double d1 = -1.0;
	double d2 = 0.0;
	double d3 = Double.MAX_VALUE;
	double d4 = Double.MIN_VALUE;
	double d5 = 0.1;
	double d6 = 2.0 + d5;
	double d7 = 4.5;
	double d8 = 150.5;
	double d9 = 155.0;
	COREassert(2.1 == d6);
	COREassert((d7 + d8) == d9);
    }

    public void testString() {
	setModule("string");
	String s = "hello";
	String s2 = " world";
	COREassert(s.length() == 5);
	COREassert(s2.charAt(1) == 'w');
	COREassert("".length() == 0);
    }

    // Yes, it works.
//      public static void main(String[] args) {
// 	 Harness h = new Harness() {
// 		 public void print(String s) { System.out.print(s); }
// 		 public String getDomain() { return "jdk"; }
// 		 public void exitOnFailure() {
// 		     System.exit(failures);
// 		 }
// 	     };
// 	 new TestSuite("jdk", h) {
// 	     public long disabledTests() {
// 		 return ~DISABLE_CONSTANT_POOL;
// 	     }
// 	 }.runTest();
//      }
} // end of TestConstantPool
