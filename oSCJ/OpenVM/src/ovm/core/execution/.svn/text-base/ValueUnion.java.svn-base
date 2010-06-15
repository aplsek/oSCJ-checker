package ovm.core.execution;
import ovm.core.OVMBase;
import ovm.core.domain.Oop;
import ovm.core.repository.TypeCodes;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.util.OVMError;
/**
 * This is somehow like VM_Word except it can hold wide primitive values.
 * Also easier to access from the interpreter than java.lang.Integer etc
 **/
public class ValueUnion extends OVMBase {
    // convert this to VM_Word ?
    private int primitive;
    private long widePrimitive;
    private Oop reference;
    private final char tag;

    public ValueUnion(char tag) {
	this.tag = tag;
    }

    public ValueUnion(char tag, Oop ref) {
	this.tag = tag;
	this.reference = ref;
    }

    
    public char getTypeTag() {
	return tag;
    }

    private OVMError dieGracefully(String expected, char found) {
	Object r = MemoryPolicy.the().enterExceptionSafeArea();
	try {
	    return new OVMError.IllegalArgument("not a " + expected +
						" but " + tag);
	} finally { MemoryPolicy.the().leave(r); }
    }

    public int getInt() {
	if (tag != TypeCodes.INT) {
	    throw dieGracefully("int", tag);
	}
	return primitive;
    }

    public void setInt(int value) {
	if (tag != TypeCodes.INT) {
	    throw dieGracefully("int", tag);
	}
	primitive = value;
    }

    public byte getByte() {
	if (tag != TypeCodes.BYTE) {
	    throw dieGracefully("byte", tag);
	}
	return (byte)primitive;
    }

    public void setByte(byte value) {
	if (tag != TypeCodes.BYTE) {
	    throw dieGracefully("byte", tag);
	}
	primitive = value;
    }

    public short getShort() {
	if (tag != TypeCodes.SHORT) {
	    throw dieGracefully("short", tag);
	}
	return (short)primitive;
    }

    public void setShort(short value) {
	if (tag != TypeCodes.SHORT) {
	    throw dieGracefully("short", tag);
	}
	primitive = value;
    }

    public char getChar() {
	if (tag != TypeCodes.CHAR) {
	    throw dieGracefully("char", tag);
	}
	return (char)primitive;
    }

    public void setChar(char value) {
	if (tag != TypeCodes.CHAR) {
	    throw dieGracefully("char", tag);
	}
	primitive = value;
    }

    public void setBoolean(boolean flag) {
	if (tag != TypeCodes.BOOLEAN) {
	    throw dieGracefully("boolean", tag);
	}
	primitive = flag ? 1 : 0;
    }

    public boolean getBoolean() {
	if (tag != TypeCodes.BOOLEAN) {
	    throw dieGracefully("boolean", tag);
	}
	return primitive == 1;
    }

    public float getFloat() {
	if (tag != TypeCodes.FLOAT) {
	    throw dieGracefully("float", tag);
	}
	return Float.intBitsToFloat(primitive);
    }

    public void setFloat(float value) {
	if (tag != TypeCodes.FLOAT) {
	    throw dieGracefully("float", tag);
	}
	primitive = Float.floatToRawIntBits(value);
    }

    // set a float from a floating-point bit pattern coming in as an int
    public void setFloat(int value) {
	if (tag != TypeCodes.FLOAT) {
	    throw dieGracefully("float", tag);
	}
	primitive = value;
    }

    public double getDouble() {
	if (tag != TypeCodes.DOUBLE) {
	    throw dieGracefully("double", tag);
	}
	return Double.longBitsToDouble(widePrimitive);
    }

    public void setDouble(double value) {
	if (tag != TypeCodes.DOUBLE) {
	    throw dieGracefully("double", tag);
	}
	widePrimitive = Double.doubleToRawLongBits(value);
    }

    // set a double from a floating-point bit pattern coming in as a long
    public void setDouble(long value) {
	if (tag != TypeCodes.DOUBLE) {
	    throw dieGracefully("double", tag);
	}
	widePrimitive = value;
    }

    public long getLong() {
	if (tag != TypeCodes.LONG) {
	    throw dieGracefully("long", tag);
	}
	return widePrimitive;
    }

    public void setLong(long value) {
	if (tag != TypeCodes.LONG) {
	    throw dieGracefully("long", tag);
	}
	widePrimitive = value;
    }

    public Oop getOop() {
	switch (tag) {
	case TypeCodes.OBJECT:
	case TypeCodes.ARRAY:
	case TypeCodes.GEMEINSAM:
	    return reference;
	default:
	    throw dieGracefully("oop", tag);
	}
    }

    public void setOop(Oop oop)
	// Disable RTSJ store checks, since this object logically only
	// lives on the stack
	throws PragmaNoBarriers
    {
	switch (tag) {
	case TypeCodes.OBJECT:
	case TypeCodes.ARRAY:
	case TypeCodes.GEMEINSAM:
	    reference = oop;
	    return;
	default:
	    throw dieGracefully("oop", tag);
	}
    }
    
}


