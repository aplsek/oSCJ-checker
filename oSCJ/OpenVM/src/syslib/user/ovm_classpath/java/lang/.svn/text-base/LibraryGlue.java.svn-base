package java.lang;

import org.ovmj.java.Opaque;

/**
 * Provides redirection of native methods in this package. These can be
 * implemented directly here in Java code, redirected to OVM services via
 * <tt>LibraryImports</tt> methods, or redefined as native for actual native
 * code invocation - or some combination thereof.
 */
class LibraryGlue {
    // Math:  First declare the libm functions
    static native double cos(double d);
    static native double atan(double d);
    static native double rint(double d);
    static native double floor(double d);
    static native double ceil(double d);
    static native double fmod(double d, double d2);
    static native double pow(double d, double d2);
    static native double sqrt(double d);
    static native double log(double d);
    static native double exp(double d);
    static native double atan2(double d, double d2);
    static native double acos(double d);
    static native double asin(double d);
    static native double tan(double d);
    static native double sin(double d);

    static double parseDouble(String s) {
	if (s == "")
	    throw new NumberFormatException();
	s = s.trim(); // will catch null
	int len = s.length();
	char last = s.charAt(len - 1);
	if (last == 'f'
	    || last == 'F'
	    || last == 'd'
	    || last == 'D')
	    s = s.substring(0, len - 1);
	return LibraryImports.parseDouble(s);
    }

    
    /* Thread-Safe free list of "scratchpads" for Double.toString.
     * Probably unnecessary, but it improves throughput if more
     * than one thread decides to call Double.toString() concurrently.
     */
    static class ArrayWrapper {
	char [] arr;
	ArrayWrapper next;

	static final int LENGTH = 20;
	private static Object lock = new Object();
	private static ArrayWrapper head;
	static {
	    for (int i = 0; i < 2; i++) {
		ArrayWrapper tmp = new ArrayWrapper();
		tmp.arr = new char[ArrayWrapper.LENGTH];
		tmp.next = head;
		head = tmp;
	    }
	}

	static ArrayWrapper get() {
	    ArrayWrapper aw;
	    synchronized (lock) {
		while (head == null) {
		    try {
			lock.wait();
		    } catch (InterruptedException e) {
                        // re-assert interrupt status if we won't throw IE
                        Thread.currentThread().interrupt();
                    }
		} 
		aw = head;
		head = head.next;
	    }
	    return aw;
	}
	static void put(ArrayWrapper aw) {
	    synchronized (lock) {
		aw.next = head;
		head = aw;
		lock.notify();
	    }
	} 
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
     * @param isFloat not used in this implementation
     */
    static String toString(double d, boolean isFloat) {
        double MAX_VAL = 1e7; // d > this -> use scientific notation
        double MIN_VAL = 1e-3; // d < this -> use scientific notation
        double POSITIVE_INFINITY = 1.0 / 0.0;
        double NEGATIVE_INFINITY = -1.0 / 0.0;
        double NaN = 0.0d / 0.0;

        if (d != d) return "NaN";
        if (d == POSITIVE_INFINITY) return "Infinity";
        if (d == NEGATIVE_INFINITY) return "-Infinity";
        if (Double.doubleToRawLongBits(d) ==
            Double.doubleToRawLongBits(-0.0)) return "-0.0";

	String retval;

	ArrayWrapper aw = ArrayWrapper.get();
	char [] buf = aw.arr;

	int index = ArrayWrapper.LENGTH;

	boolean sign = ( d < 0.0 ? false : true);
        int exponent = 0;
        double dd = ( d < 0.0 ? -d : d);  // make it positive
        if (dd > 0 && dd <= MIN_VAL) {
            while (dd < 1) {
                dd *= 10;
                exponent -= 1;
            }
	    index = itoa(buf, index, exponent);
	    buf[--index] = 'e';
            index = doubleToMdotN(buf, index, dd);
	    if (!sign) buf[--index] = '-';
	    retval = 
		new String(buf, index, ArrayWrapper.LENGTH - index);
	    ArrayWrapper.put(aw);
	    return retval;
        }
        else if ( dd < MAX_VAL) { // handles dd == 0
	    index = doubleToMdotN(buf, index, dd);
            if (!sign) buf[--index] = '-';
	    retval = 
		new String(buf, index, ArrayWrapper.LENGTH - index);
	    ArrayWrapper.put(aw);
	    return retval;
        }
        else {
            while (dd > 10) {
                dd /= 10;
                exponent += 1;
            }
	    index = itoa(buf, index, exponent);
	    buf[--index] = 'e';
	    index = doubleToMdotN(buf, index, dd);
	    if (!sign) buf[--index] = '-';
	    retval =
		new String(buf, index, ArrayWrapper.LENGTH - index);
	    ArrayWrapper.put(aw);
	    return retval;
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

    static final char[] digits = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    static int itoa(char [] cb, int index, long num) {
        int radix = 10;

        // For negative numbers, print out the absolute value w/ a leading '-'.
        // Use an array large enough for a binary number.
        boolean isNeg = false;
        if (num < 0) {
            isNeg = true;
            num = -num;
            
            // When the value is MIN_VALUE, it overflows when made positive
            if (num < 0) {
                cb[--index] = digits[(int) (-(num + radix) % radix)];
                num = -(num / radix);
            }
        }

        do {
            cb[--index] = digits[(int) (num % radix)];
            num /= radix;
        } while (num > 0);

        if (isNeg)
            cb[--index] = '-';

         return index;
    }

    private static int doubleToMdotN(char [] arr, int offset, double d) {
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

        if (l_fraction == 0) {
            arr[--offset] = '0';
        } else {
            offset = itoa(arr, offset, l_fraction);
            for (int j = 0; j < leadingZeroes; j++)
                arr[--offset] = '0';
        }
        // note that we truncate rather than rounding - rounding is hard :)
        arr[--offset] = '.';
        offset = itoa(arr, offset, (long) d);
        return offset;
    }

    static native float intBitsToFloat(int i);
    static native double longBitsToDouble(long l);
    static native long doubleToRawLongBits(double d);
    static native int floatToRawIntBits(float f);


    // define wrappers like Math_cos(double d) to check all the
    // boundary cases

    // Or just pretend libm does the right thing
    static double IEEEremainder(double d, double d2) {
	return fmod(d, d2);
    }

    static Class getClass(Object o) {
	return LibraryImports.classFor(o);
    }

    static void Double_initIDs(Opaque o) { }
}
