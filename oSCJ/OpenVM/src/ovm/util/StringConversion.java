/*
 * $Header: /p/sss/cvs/OpenVM/src/ovm/util/StringConversion.java,v 1.7 2006/04/16 23:59:24 cunei Exp $
 *
 */
package ovm.util;

//import ovm.core.execution.Native;

/**
 * A utility class to convert different primitive types into strings.
 * This mimics the functionality of the various Type.toString(primitiveType)
 * methods (such as <code>Long.toString(long)</code>) but removes a dependency
 * between the core OVM kernel and those JDK classes.
 *
 * @author David Holmes
 *
 */
public final class StringConversion {

    /**
     * Returns a string representation of the given boolean value.
     * @param b the boolean
     * @return if <code>b == true</code> then &quot;true&quot; else
     * &quot;false&quot;.
     *
     */
    public static final String toString(boolean b) {
        return b ? "true" : "false";
    }

    /**
     * Returns a string containing the single given character
     * @return  a string containing the single given character
     * @param c the character
     */
    public static final String toString(char c) {
        char buf[] = {c};
        return new String(buf);
    }

    /**
     * Returns a string representation of the given byte.
     * A negative value is prefixed with a minus ('-') character.
     * @return a string representation of the given byte
     * @param b the byte
     */
    public static final String toString(byte b) {
        return toString(b, 10);
    }

    /**
     * Returns a string representation of the given short.
     * A negative value is prefixed with a minus ('-') character.
     * @return a string representation of the given short
     * @param s the short
     */
    public static final String toString(short s) {
        return toString(s, 10);
    }

    private static final char[] digits = 
    { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
      'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
      'u', 'v', 'w', 'x', 'y', 'z' };

    // inspired by the JDK version :)
    public static String toUnsignedString(long value, int bits) {
        char[] buf = new char[64];
        int charPos = 64;
        int radix = 1 << bits;
        long mask = radix - 1;
        do {
            buf[--charPos] = digits[(int)(value & mask)];
            value >>>= bits;
        } while (value != 0);
        return new String(buf, charPos, (64 - charPos));
    }

    public static String toUnsignedString(int value, int bits) {
        char[] buf = new char[32];
        int charPos = 32;
        int radix = 1 << bits;
        int mask = radix - 1;
        do {
            buf[--charPos] = digits[value & mask];
            value >>>= bits;
        } while (value != 0);
        return new String(buf, charPos, (32 - charPos));
    }

    /** The minimum radix that can be used with the conversion routines */
    public static final int MIN_RADIX = 2;
    /** The maximum radix that can be used with the conversion routines */
    public static final int MAX_RADIX = 36;

    /** 
     * Returns a string representation of the given integral number in
     * the given radix (base).
     * If radix is < 2 or > 36 then radix 10 is used.
     * @return a string representation of the given number in the given 
     * radix
     * @param i the integral value
     * @param radix the numeric radix (base) to use
     *
     */
    public static String toString(long i, int radix) {
        StringBuffer tmp = new StringBuffer();

        if (radix < MIN_RADIX || radix > MAX_RADIX)
            radix = 10;
    
        boolean negative = (i < 0);
        i = i < 0 ? -i : i; // make positive

        // if i == MIN_VALUE then -i overflows
        if (i < 0) {
            tmp.append(digits[(int) (-(i+radix) % radix)]);
            i = - (i/radix);
        }
                       
        do {
            tmp.append(digits[(int)(i % radix)]);
        } while ((i /= radix) != 0);
        if (negative) tmp.append('-');
        return tmp.reverse().toString();
    }

    /**
     * Returns a string representation of the given int.
     * A negative value is prefixed with a minus ('-') character.
     * @return a string representation of the given int
     * @param i the int
     */
    public static final String toString(int i) {
        return toString(i, 10);
    }

    /**
     * Returns a string representation of the given int as a hexadecimal
     * value.
     * @return a string representation of the given int in hex
     * @param i the int
     */
    public static final String toHexString(int i) {
        return toUnsignedString(i, 4);
    }


    /**
     * Returns a string representation of the given long
     * A negative value is prefixed with a minus ('-') character.
     * @return a string representation of the given long
     * @param l the long
     */
    public static final String toString(long l) {
        return toString(l, 10);
    }

    /**
     * Returns a string representation of the given long as a hexadecimal
     * value.
     * @return a string representation of the given long in hex
     * @param l the long
     */
    public static final String toHexString(long l) {
        return toUnsignedString(l, 4);
    }

    /**
     * Returns a string representation of the given floating-point
     * value. 
     * @return a string representation of the given floating-point
     * value.  
     * This is a crude representation of the form <code>m.n</code> 
     * where <code>m</code> is the whole part and <code>n</code> the
     * fractional part to 6 decimal places. If the whole value is larger
     * than 10^7, or smaller than 10^-3 then scientific notation is used.
     * If the value is a NaN then &quot;NaN&quot; is returned.
     * If the value is infinity then &quot;Infinity&quot; is returned.
     *
     * @param f the float
     */
    public static final String toString(float f) {
        return toString((double)f);
    }

    /**
     * Returns a string representation of the given floating-point
     * value. 
     * @return a string representation of the given floating-point
     * value.  
     * This is a crude representation of the form <code>m.n</code> 
     * where <code>m</code> is the whole part and <code>n</code> the
     * fractional part to 6 decimal places. If the whole value is larger
     * than 10^7, or smaller than 10^-3 then scientific notation is used.
     * If the value is a NaN then &quot;NaN&quot; is returned.
     * If the value is infinity then &quot;Infinity&quot; is returned.
     *
     * @param d the double
     */
    public static final String toString(double d) {

        double MAX_VAL = 1e7; // d > this -> use scientific notation
        double MIN_VAL = 1e-3; // d < this -> use scientific notation
        double POSITIVE_INFINITY = 1.0 / 0.0;
        double NEGATIVE_INFINITY = -1.0 / 0.0;
       // double NaN = 0.0d / 0.0;

        if (d != d) return "NaN";
        if (d == POSITIVE_INFINITY) return "Infinity";
        if (d == NEGATIVE_INFINITY) return "-Infinity";
        if (Double.doubleToRawLongBits(d) ==
	    Double.doubleToRawLongBits(-0.0)) return "-0.0";

        String sign = ( d < 0.0 ? "-" : "");
        String body = null;
        String exp = "e";
        int exponent = 0;
        double dd = ( d < 0.0 ? -d : d);  // make it positive
        if (dd > 0 && dd <= MIN_VAL) {
            while (dd < 1) {
                dd *= 10;
                exponent -= 1;
            }
            body = doubleToMdotN(dd);
            return sign + body + exp + toString(exponent);
        }
        else if ( dd < MAX_VAL) { // handles dd == 0
            return sign + doubleToMdotN(dd);
        }
        else {
            while (dd > 10) {
                dd /= 10;
                exponent += 1;
            }
            body = doubleToMdotN(dd);
            return sign + body + exp + toString(exponent);
        }
    }

    private static final int decPlaces = 6;
    private static final String[] zeroes = new String[decPlaces+1];

    static {
        String z = "";
        for (int i = 0; i < zeroes.length; i++) {
            zeroes[i] = z;
            z += "0";
        }
    }

    private static String doubleToMdotN(double d) {
        // d is >= 0 && d <= MAX_VAL
        long whole = (long) d;
        double fraction = d - whole;

        // watch for leading zeroes in the fraction
        int leadingZeroes = 0;
        for (int i = 0, m = 10; i < decPlaces; i++, m *= 10) {
            long f = (long)(fraction*m);
            if (f == 0) {
                leadingZeroes++;
            }
            else {
                break;
            }
        }

        int multiplier = 10*10*10*10*10*10;  // 10^decPlaces
        long l_fraction = (long)(fraction*multiplier);

        // note that we truncate rather than rounding - rounding is hard :)

        return toString(whole) + "." + 
            ( l_fraction == 0 ? "0" : 
              (zeroes[leadingZeroes] + toString(l_fraction)));
    }

}





























































