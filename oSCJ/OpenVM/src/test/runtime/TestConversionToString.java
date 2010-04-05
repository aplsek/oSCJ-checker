/*
 * $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestConversionToString.java,v 1.15 2006/04/16 23:59:25 cunei Exp $
 *
 */
package test.runtime;
import ovm.util.StringConversion;
import test.common.TestBase;
/**
 * Test the string conversion routines of StringConversion
 * This is a reasonable set of tests but far from complete coverage.
 * Whitebox testing would be bested suited for this, rather than black
 * box - at least for the floating point conversions.
 *
 * <p><b>NOTE:</b> Floats are converted by casting to double. This makes the
 * float look more accurate than it actually is (non-terminating binary
 * fractions appear to terminate), hence the results for float and double
 * need <b>not</b> match.
 * @author David Holmes
 */
public class TestConversionToString extends TestBase {

    long disable;

    public TestConversionToString(long disable) {
        super("Primitive to String conversion tests");
	this.disable = disable;
    }


    static class IntegralPair {
        long val;
        String str_val;
        IntegralPair(long v, String s) {
            val = v; str_val = s;
        }
        
    }

    static class FloatPair {
        float val;
        String str_val;
        FloatPair(float v, String s) {
            val = v; str_val = s;
        }
    }

    static class DoublePair {
        double val;
        String str_val;
        DoublePair(double v, String s) {
            val = v; str_val = s;
        }
    }
    

    public void run() {

        // we test the explicit StringConversion calls and make sure the
        // implicit string concatenation calls are correct

        String str = "Oops";

        boolean notTrue = false;
        String notTrue_str = "false";
        boolean notFalse = true;
        String notFalse_str = "true";

        str = StringConversion.toString(notTrue);
        check_condition(str.equals(notTrue_str), str + " != " + notTrue_str);
        str = "" + notTrue;
        check_condition(str.equals(notTrue_str), str + " != " + notTrue_str);

        str = StringConversion.toString(notFalse);
        check_condition(str.equals(notFalse_str), str + " != " + notFalse_str);
        str = "" + notFalse;
        check_condition(str.equals(notFalse_str), str + " != " + notFalse_str);
        char c =       'c';
        String c_str = "c";

        str = StringConversion.toString(c);
        check_condition(str.equals(c_str), str + " != " + c_str);
        str = "" + c;
        check_condition(str.equals(c_str), str + " != " + c_str);

        IntegralPair[] pairs = { // MUST fit in byte!
            new IntegralPair(-128, "-128"),
            new IntegralPair(127, "127"),
            new IntegralPair(0, "0"),
            new IntegralPair(-1, "-1"),
            new IntegralPair(1, "1"),
            new IntegralPair(-33, "-33"),
            new IntegralPair(87, "87"),
        };

        // test the above using each of the integral conversion methods
        for (int i = 0; i < pairs.length; i++) {
            String s1 = pairs[i].str_val;
            String s2 = StringConversion.toString((byte)pairs[i].val);
            check_condition( s1.equals(s2), s1 + " (byte) != " + s2 );
            // only need to test this for one loop set
            String s3 = "" + pairs[i].val;
            check_condition( s1.equals(s3), s1 + " != " + s3 );
        }

        for (int i = 0; i < pairs.length; i++) {
            String s1 = pairs[i].str_val;
            String s2 = StringConversion.toString((short)pairs[i].val);
            check_condition( s1.equals(s2), s1 + " (short)!= " + s2 );
        }

        for (int i = 0; i < pairs.length; i++) {
            String s1 = pairs[i].str_val;
            String s2 = StringConversion.toString((int)pairs[i].val);
            check_condition( s1.equals(s2), s1 + " (int)!= " + s2 );
        }

        for (int i = 0; i < pairs.length; i++) {
            String s1 = pairs[i].str_val;
            String s2 = StringConversion.toString(pairs[i].val);
            check_condition( s1.equals(s2), s1 + " (long) != " + s2 );
        }

        // try some bigger int values
        IntegralPair[] bigpairs = {
            new IntegralPair(-1234567, "-1234567"),
            new IntegralPair(9876543, "9876543"),
        };

        for (int i = 0; i < bigpairs.length; i++) {
            check_condition( bigpairs[i].str_val.equals(StringConversion.toString((int)bigpairs[i].val)), "toString(int) - big");
            check_condition( bigpairs[i].str_val.equals(StringConversion.toString(bigpairs[i].val)), "toString(long) - big");
        }

        IntegralPair[] inthexpairs = {
            new IntegralPair(0x0, "0"),
            new IntegralPair(0xFFFFFFFF, "ffffffff"),
            new IntegralPair(0x80000000, "80000000"),
            new IntegralPair(0xCAFEBABE, "cafebabe"),
            new IntegralPair(0xDEADBEEF, "deadbeef"),
            new IntegralPair(0x1, "1"),
            new IntegralPair(0xAA, "aa"),
            new IntegralPair(0xBBB, "bbb"),
            new IntegralPair(0xDDDD, "dddd"),
            new IntegralPair(0xEEEEE, "eeeee"),
            new IntegralPair(0xFFFFFF, "ffffff"),
            new IntegralPair(0xCCCCCCC, "ccccccc"),
            new IntegralPair(0x12345678, "12345678"),
            new IntegralPair(0x87654321, "87654321"),
        };

        for (int i = 0; i < inthexpairs.length; i++) {
            String s1 = inthexpairs[i].str_val;
            String s2 = StringConversion.toHexString((int)inthexpairs[i].val);
            check_condition( s1.equals(s2), s1 + " (hex-int) != " + s2 );
        }

        IntegralPair[] longhexpairs = {
            // remember int -> long is sign extended
            // and a hex value without
            new IntegralPair(0x0, "0"),
            new IntegralPair(0xFFFFFFFF, "ffffffffffffffff"),
            new IntegralPair(0x80000000, "ffffffff80000000"),
            new IntegralPair(0xCAFEBABE, "ffffffffcafebabe"),
            new IntegralPair(0xDEADBEEF, "ffffffffdeadbeef"),
            new IntegralPair(0x1EADBEEF, "1eadbeef"),
            new IntegralPair(0x1, "1"),
            new IntegralPair(0xAA, "aa"),
            new IntegralPair(0xBBB, "bbb"),
            new IntegralPair(0xDDDD, "dddd"),
            new IntegralPair(0xEEEEE, "eeeee"),
            new IntegralPair(0xFFFFFF, "ffffff"),
            new IntegralPair(0xCCCCCCC, "ccccccc"),
            new IntegralPair(0x12345678, "12345678"),
            new IntegralPair(0x87654321, "ffffffff87654321"),
            new IntegralPair(0xDEADBEEFDEADBEEFL, "deadbeefdeadbeef"),
            new IntegralPair(0xA5A5A5A5A5A5A5A5L, "a5a5a5a5a5a5a5a5"),
        };

        for (int i = 0; i < longhexpairs.length; i++) {
            String s1 = longhexpairs[i].str_val;
            String s2 = StringConversion.toHexString(longhexpairs[i].val);
            check_condition( s1.equals(s2), s1 + " (hex-long) != " + s2 );
        }


        // StringConversion always does 6 decimal places - DH
        // unless they are all zeroes - in which case it does 1 zero
        // NOTE: Unless the values can be represented as powers of 2 you'll
        // get (platform specific?) rounding affects
        // NOTE 2: The conversion routine truncates at 6 decimal places
        FloatPair[] floatpairs = new FloatPair[] {
            new FloatPair(0f, "0.0"),
            new FloatPair(1/0.0f, "Infinity"),
            new FloatPair(0.0f/0.0f, "NaN"),
            new FloatPair(1f, "1.0"),
            new FloatPair(0.25f, "0.250000"),
            new FloatPair(2f, "2.0"),
            new FloatPair(0.0125f, "0.012500"),
            new FloatPair(1.0125f, "1.012500"),
            new FloatPair(5.62e3f, "5620.0"),
            new FloatPair(9.123456e8f, "9.123456e8"),
            new FloatPair(9.1234561e8f, "9.123456e8"),
            new FloatPair(9.1234567e8f, "9.123456e8"),
            new FloatPair(7.8125e-3f, "0.007812"), // close to min
            new FloatPair(9.629649e-35f, "9.629649e-35"),
            new FloatPair(12345.265625f, "12345.265625"),

            new FloatPair(-0f, "-0.0"),
            new FloatPair(-1/0.0f, "-Infinity"),
            new FloatPair(-0.0f/0.0f, "NaN"),
            new FloatPair(-1f, "-1.0"),
            new FloatPair(-0.25f, "-0.250000"),
            new FloatPair(-2f, "-2.0"),
            new FloatPair(-0.0125f, "-0.012500"),
            new FloatPair(-1.0125f, "-1.012500"),
            new FloatPair(-5.62e3f, "-5620.0"),
            new FloatPair(-9.123456e8f, "-9.123456e8"),
            new FloatPair(-9.1234561e8f, "-9.123456e8"),
            new FloatPair(-9.1234567e8f, "-9.123456e8"),
            new FloatPair(-7.8125e-3f, "-0.007812"), // close to min
            new FloatPair(-9.629649e-35f, "-9.629649e-35"),
            new FloatPair(-12345.265625f, "-12345.265625"),
        };


        for (int i = 0; i < floatpairs.length; i++) {
            String s1 = floatpairs[i].str_val;
            String s2 = StringConversion.toString(floatpairs[i].val);
            check_condition( s1.equals(s2), s1 + " (float) != " + s2 );
            String s3 = "" + floatpairs[i].val;
            check_condition( s1.equals(s3), s1 + " (float)!= " + s3 );

        }


	if ((disable & TestSuite.DISABLE_DOUBLE_TO_STRING) == 0) {
	    // StringConversion always does 6 decimal places - DH
	    // unless they are all zeroes - in which case it does 1 zero
	    // NOTE: Unless the values can be represented as powers of 2 you'll
	    // get (platform specific?) rounding affects
	    // NOTE 2: The conversion routine truncates at 6 decimal places
	    DoublePair[] doublepairs = new DoublePair[] {
		new DoublePair(0d, "0.0"),
		new DoublePair(1/0.0, "Infinity"),
		new DoublePair(0.0/0.0, "NaN"),
		new DoublePair(1d, "1.0"),
		new DoublePair(0.25, "0.250000"),
		new DoublePair(2d, "2.0"),
		new DoublePair(0.0125, "0.012500"),
		new DoublePair(1.0125, "1.012499"), 
		new DoublePair(5.62e3, "5620.0"),
		new DoublePair(9.123456e8, "9.123456e8"),
		new DoublePair(9.1234561e8, "9.123456e8"),
		new DoublePair(9.1234567e8, "9.123456e8"),
		new DoublePair(7.8125e-3, "0.007812"), // close to min
		new DoublePair(9.629649e-35, "9.629649e-35"),
		new DoublePair(12345.265625, "12345.265625"),

		new DoublePair(-0d, "-0.0"),
		new DoublePair(-1/0.0, "-Infinity"),
		new DoublePair(-0.0/0.0, "NaN"),
		new DoublePair(-1d, "-1.0"),
		new DoublePair(-0.25, "-0.250000"),
		new DoublePair(-2d, "-2.0"),
		new DoublePair(-0.0125, "-0.012500"),
		new DoublePair(-1.0125, "-1.012499"),
		new DoublePair(-5.62e3, "-5620.0"),
		new DoublePair(-9.123456e8, "-9.123456e8"),
		new DoublePair(-9.1234561e8, "-9.123456e8"),
		new DoublePair(-9.1234567e8, "-9.123456e8"),
		new DoublePair(-7.8125e-3, "-0.007812"), // close to min
		new DoublePair(-9.629649e-35, "-9.629649e-35"),
		new DoublePair(-12345.265625, "-12345.265625"),
	    };


	    for (int i = 0; i < doublepairs.length; i++) {
		String s1 = doublepairs[i].str_val;
		String s2 = StringConversion.toString(doublepairs[i].val);
		check_condition( s1.equals(s2), s1 + " (double) != " + s2 );
                String s3 = "" + doublepairs[i].val;
                check_condition( s1.equals(s3), s1 + " (double)!= " + s3 );

	    }
	}
    }
}
