package ovm.util;

public final class NumberRanges {
    public static class NumberRangeException extends OVMError {
	final long value;
	final String typeName;
	public NumberRangeException(long value, String typeName) {
	    this.value = value;
	    this.typeName = typeName;
	}
	public String getMessage() {
	    return value + " is not " + typeName;
	}
    } 

    private NumberRanges() {}
    public static boolean isShort(int value) {
	return (value <= Short.MAX_VALUE) && (value >= Short.MIN_VALUE);
    }
    public static short checkShort(int value) {
	assertShort(value);
	return (short)value;
    }

    public static void assertShort(int value) {
	if (!isShort(value)) {
	    throw new NumberRangeException(value, "short");
	}
    }


    public static boolean isByte(int value) {
	return (value <= Byte.MAX_VALUE) && (value >= Byte.MIN_VALUE);
    }

    public static boolean isUnsignedByte(int value) {
	return (value <= 255) && (value >= 0);
    }

    public static byte checkUnsignedByte(int value) {
	if (!isUnsignedByte(value)) {
	    throw new NumberRangeException(value, "unsigned byte");
	}
	return (byte)(value & 0xFF);
    }

    public static byte checkByte(int value) {
	if (!isByte(value)) {
	    throw new NumberRangeException(value, "byte");
	}
	return (byte)value;
    }



    public static boolean isUnsignedShort(int value) {
	return (value <= 0xFFFF) && (value >= 0);
    }

    public static void assertUnsignedShort(int value) {
	if (!isUnsignedShort(value)) {
	    throw new NumberRangeException(value, "unsigned short");
	}
    }

    public static char checkUnsignedShort(int value) {
	assertUnsignedShort(value);
	return (char)value;
    }


    public static boolean isChar(int value) {
	return isUnsignedShort(value);
    }

    public static char checkChar(int value) {
	assertChar(value);
	return (char)value;
    }

    public static void assertChar(int value) {
	if (!isChar(value)) {
	    throw new NumberRangeException(value, "char");
	}
    }
    
    public static int wordAlign(int value) {

	if (value == 0) {
	    return 0;
	} 

	return (value + 0x3) & ~0x3;
    }
    
    public static int asInt(long value) {
	int conv = (int)value;
	if (conv != value) {
	    throw new NumberRangeException(value, "int");
	}
	return conv;
    }

}
