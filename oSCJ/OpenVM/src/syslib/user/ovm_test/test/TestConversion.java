package test;

public class TestConversion extends TestBase {
    boolean doThrow;
    
    public TestConversion(Harness domain, long disabled) {
	super("Conversion", domain);
    }
    
    public void run() {
	testParseInt();
	testParseDouble();
	testIntBitsToFloat();
	testLongBitsToDouble();
	testFloatToIntBits();
	testDoubleToLongBits();
    }

    public void testIntBitsToFloat() {
	int x = 0x40000000;
	float y = Float.intBitsToFloat(x);
	COREassert(y == 2.0F, "testIntBitsToFloat failed");
    }

    public void testLongBitsToDouble() {
	long x = 0x4000000000000000L;
	double y = Double.longBitsToDouble(x);
	COREassert(y == 2.0, "testLongBitsToDouble failed");
    }

    public void testFloatToIntBits() {
	float x = 2.0F;
	int y = Float.floatToIntBits(x);
	COREassert(y == 0x40000000, "testFloatToIntBits failed");
    }

    public void testDoubleToLongBits() {
	double x = 2.0;
	long y = Double.doubleToLongBits(x);
	COREassert(y == 0x4000000000000000L, "testDoubleToLongBits failed");
    }

    public void testParseDouble() {
	String s = "128.0";
	double d = Double.parseDouble(s);
	COREassert(d == 128.0D, "parse double failed");
    }

    public void testParseInt() {
	String s = "128";
	int i = Integer.parseInt(s);
	COREassert(i == 128, "parse int failed");
    }

}
