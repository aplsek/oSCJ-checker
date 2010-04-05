package ovm.services.bytecode;


import ovm.core.OVMBase;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeCodes;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.AbstractValueError;
import ovm.util.ByteBuffer;
import ovm.util.OVMError;
import ovm.util.NumberRanges;
import org.ovmj.util.Runabout;
import s3.util.PragmaTransformCallsiteIR.BCdead;

public class SpecificationIR extends OVMBase {

    public static class Value extends OVMBase implements AbstractValue {
	final public ValueSource source;

	public int getId() {
	    throw new OVMError.UnsupportedOperation();
	}

	public boolean includes (AbstractValue v) {
	    throw new OVMError.UnsupportedOperation();
	}

	public AbstractValue merge (AbstractValue e) {
	    throw new OVMError.UnsupportedOperation();
	}

	public boolean isJumpTarget() {
	    return false;
	}

	public AbstractValue.JumpTarget getJumpTarget() {
	    throw new OVMError.UnsupportedOperation();
	}

	public boolean isWidePrimitive() {
	    return false;
	}

	public AbstractValue.WidePrimitive getWidePrimitive() {
	    throw new OVMError.UnsupportedOperation();
	}   

	
	public boolean isPrimitive() {
	    return false;
	}


	public AbstractValue.Primitive getPrimitive() {
	    throw new OVMError.UnsupportedOperation();
	}

	public boolean isReference() {
	    return false;
	}

	public AbstractValue.Reference getReference() {
	    throw new OVMError.UnsupportedOperation();
	}

	public boolean isInvalid() {
	    return false;
	}
	public AbstractValue.Invalid getInvalid() {
	    throw new OVMError.UnsupportedOperation();
	}
	
	public boolean equals(AbstractValue v) {
	    throw new OVMError.UnsupportedOperation();
	}

	static final Value[] EMPTY_ARRAY = IntValue.EMPTY_IARRAY;
	public Value(ValueSource source) {
	    this.source = source;
	    id = count;
	    count ++;
	}
	public boolean isWide() {
	    return false;
	}
	public Value() {
	    this(null);
	}

	public LocalExp localsSlot() {
	    if (source instanceof LocalExp) {
		return (LocalExp) source;
	    } else {
		return null;
	    }
	}

	public Object concreteValue() {
	    return null;
	}

	public int intValue() {
	    throw new OVMError.UnsupportedOperation("Concrete int value unknown at this time");
	}

	public String toString() throws BCdead {
	    String qname = getClass().getName();
	    String name = qname.substring(qname.lastIndexOf('$') + 1,
					  qname.length());
	    return name + "(" + id + ")";
	}
	int id;
	static int count = 0;
    }

    public interface StreamableValue {
        char getType();

        int bytestreamSize();

        Number decodeStream(MethodInformation iv, int offset);

	int decodeInt(MethodInformation iv, int offset);

        ConcreteStreamableValue concretize(MethodInformation iv,
					   int offset);
    }

    public static char toTypeCode(SpecificationIR.Value v) {
        if (v instanceof SpecificationIR.IntValue) {
            return TypeCodes.INT;
        } else if (v instanceof SpecificationIR.FloatValue) {
            return TypeCodes.FLOAT;
        } else if (v instanceof SpecificationIR.RefValue) {
            return TypeCodes.REFERENCE;
        } else if (v instanceof SpecificationIR.LongValue) {
            return TypeCodes.LONG;
        } else if (v instanceof SpecificationIR.DoubleValue) {
            return TypeCodes.DOUBLE;
        } else
            throw new Error("Unexpected " + v);
    }

    public static void encode(ByteBuffer code, int value, char type) {
        switch (type) {
        case TypeCodes.UBYTE:
            code.put(NumberRanges.checkUnsignedByte(value));
        break;
        case TypeCodes.BYTE:   
            code.put(NumberRanges.checkByte(value));
        break;
        case TypeCodes.CHAR:
        case TypeCodes.USHORT:
            code.putChar(NumberRanges.checkUnsignedShort(value));
            break;
        case TypeCodes.SHORT:
            code.putShort(NumberRanges.checkShort(value));
            break;
        case TypeCodes.UINT:
        case TypeCodes.INT:
            code.putInt(value);
            break;
        default:
            throw new Error();
        }
    }
    
    public interface ConcreteStreamableValue extends StreamableValue {
        public void encode(ByteBuffer code);
    }
    
    public static class IntValue extends Value
	implements AbstractValue.Int, StreamableValue {
	char type = TypeCodes.INT;
	static final IntValue[] EMPTY_IARRAY = new IntValue[0];

	public IntValue(ValueSource source) {
	    super(source);
	}
	
	public void setType(char type) {
	    this.type = type;
	}

	public IntValue() {
	    super();
	}
	public IntValue(char type, ValueSource source) {
	    super(source);
	    this.type = type;

	}
	public IntValue(char type) {
	    this(type, null);
	}

	public char getType() {
	    return type;
	}
	public String toString() {
	    return "INT";
	}

	private Error unexpectedTypeError() throws BCdead {
	    return new OVMError("oops, type " + type + " in " + getClass());
	}

	public Number decodeStream(MethodInformation iv, int offset) {
	    return new Integer(decodeInt(iv, offset));
	}

	public int decodeInt(MethodInformation iv, int offset)
	{
	    ByteBuffer code = iv.getCode();
	    offset += iv.getPC();
	    int mask = 0xffffffff;
	    int ret;
	    switch (type) {
	    case TypeCodes.UBYTE:  mask = 0xff;
	    case TypeCodes.BYTE:   ret = code.get(offset);
		break;
	    case TypeCodes.CHAR:
	    case TypeCodes.USHORT: mask = 0xffff;
	    case TypeCodes.SHORT:  ret = code.getShort(offset);
		break;
	    case TypeCodes.UINT:
	    case TypeCodes.INT:    ret = code.getInt(offset);
		break;
	    default:
		throw unexpectedTypeError();
		
	    }
	    return ret & mask;
	}
		
		
	public int bytestreamSize() {
	    switch (type) {
	    case TypeCodes.UBYTE:
	    case TypeCodes.BYTE:
		return 1;
	    case TypeCodes.SHORT:
	    case TypeCodes.USHORT:
	    case TypeCodes.CHAR:
		return 2;
	    case TypeCodes.UINT:
	    case TypeCodes.INT:
		return 4;
	    default:
		throw unexpectedTypeError();
	    }
	}

	public boolean isInt() { 
	    return true; 
	}
	public AbstractValue.Int getInt() { 
	    return this;
	}
	public boolean isFloat() { 
	    return false; 
	}
	public AbstractValue.Float getFloat() { 
	    throw new AbstractValueError(); 
	}

	public boolean isPrimitive() { return true; }
	public AbstractValue.Primitive getPrimitive() { return this; }
    public ConcreteStreamableValue concretize(MethodInformation iv,
                int offset) {
            return new ConcreteIntValue(decodeInt(iv, offset), type);
        }
    
    }

    // I think Padding shouldn't be an IntValue
    public static class Padding extends IntValue implements StreamableValue {
        public Padding() {
            super();
        }

        public char getType() {
            throw new Error();
        }

        public int bytestreamSize() {
            throw new Error();
        }

        public Number decodeStream(MethodInformation iv, int offset) {
            throw new Error();
        }
	public int decodeInt(MethodInformation iv, int offset) {
	    throw new Error();
	}

        public ConcreteStreamableValue concretize(MethodInformation iv,
                int offset) {
            return new ConcretePadding();
        }
    }

    public static class ConcretePadding extends Padding implements
            ConcreteStreamableValue {
        public ConcretePadding() {
            super();
        }
        public void encode(ByteBuffer code) {
            while (code.position() % 4 != 0) {
                code.put((byte)0);
            }
        }
    }
    
    public static class PCValue extends IntValue {
	boolean relative = true;

	public boolean isRelative() {
	    return relative;
	}
	public PCValue(ValueSource source) {
	    super(TypeCodes.INT, source);
	}

	public PCValue(ValueSource source, boolean relative) {
	    super(TypeCodes.INT, source);
	    this.relative = relative;
	}

	public PCValue() {
	    super(TypeCodes.INT, null);
	}
	
	public PCValue(char typeTag) {
	    super(typeTag, null);
	}

	// null source means current PC 
	public ValueSource offset() {
	    return source;
	}
    
    public ConcreteStreamableValue concretize(MethodInformation iv,
            int offset) {
        return new ConcretePCValue(decodeInt(iv, offset), type);
    }

    }

    public static class ConcretePCValue extends PCValue implements ConcreteStreamableValue {
        java.lang.Integer concreteValue;
        
        ConcretePCValue(int val, char type) {
            super(type);
            concreteValue = new java.lang.Integer(val);
        }
        ConcretePCValue(int val) {
            super();
            concreteValue = new java.lang.Integer(val);
        }

        public Object concreteValue() {
            return concreteValue;
        }
        public int intValue() {
            return concreteValue.intValue();
        }
        public int decodeInt(MethodInformation iv, int offset) {
            return concreteValue.intValue();
        }        
        public String toString() {
            return concreteValue.toString();
        }

        public void encode(ByteBuffer code) {
            SpecificationIR.encode(code, concreteValue.intValue(), type);
        }
        public void encode(ByteBuffer code, int startOfInstruction) {
            SpecificationIR.encode(code, concreteValue.intValue(), type);
        }
    }
    
    public static class CPIndexValue extends IntValue {
	static final byte CONSTANT_Any = -1;
	byte expectedTag;
	public CPIndexValue() {
	    this(CONSTANT_Any, TypeCodes.USHORT);
	}
	public CPIndexValue(byte expectedTag) {
	    this(expectedTag, TypeCodes.USHORT);
	}
	public CPIndexValue(byte expectedTag, char typeTag) {
	    super(typeTag);
	    this.expectedTag = expectedTag;
	}
	public CPIndexValue(char typeTag) {
	    super(typeTag);
	    this.expectedTag = CONSTANT_Any;
	}
	public byte getExpectedTag() {
	    return expectedTag;
	}
	public Selector.Field getFieldSelector() {
	    throw new OVMError.UnsupportedOperation();
	}
	
	public TypeName.Compound getClassName() {
	    throw new OVMError.UnsupportedOperation();
	}
    public ConcreteStreamableValue concretize(MethodInformation iv, int offset) {
        return new ConcreteCPIndexValue(decodeInt(iv, offset), type);
    }
    }

    public static class ConcreteCPIndexValue extends CPIndexValue 
    implements ConcreteStreamableValue  {
        java.lang.Integer concreteValue;
        
        ConcreteCPIndexValue(int val, char type) {
            super(type);
            concreteValue = new java.lang.Integer(val);
        }

        ConcreteCPIndexValue(int val) {
            super();
            concreteValue = new java.lang.Integer(val);
        }

        public Object concreteValue() {
            return concreteValue;
        }
        public int intValue() {
            return concreteValue.intValue();
        }
        
        public String toString() {
            return concreteValue.toString();
        }

	public Number decodeStream(MethodInformation iv, int offset) {
	    return concreteValue;
	}
	public int decodeInt(MethodInformation iv, int offset) {
	    return concreteValue.intValue();
	}
        public void encode(ByteBuffer code) {
            SpecificationIR.encode(code, concreteValue.intValue(), type);
        }
    }
    
    
    public static  class ConcreteIntValue extends IntValue implements ConcreteStreamableValue {
	java.lang.Integer concreteValue;

	public Object concreteValue() {
	    return concreteValue;
	}
	public int intValue() {
	    return concreteValue.intValue();
	}
	public Number decodeStream(MethodInformation iv, int offset) {
	    return concreteValue;
	}
	public int decodeInt(MethodInformation iv, int offset) {
	    return concreteValue.intValue();
	}
	
    public ConcreteIntValue(int val, char type) {        
        super(type);
        concreteValue = new java.lang.Integer(val);
    }
	public ConcreteIntValue(int val) {
	    concreteValue = new java.lang.Integer(val);
	}
	public String toString() {
	    if ( this == MAX_VALUE )
		return " ((jint) 0x7fffffffL) ";
	    if ( this == MIN_VALUE )
		return " ((jint) 0x80000000L) ";
	    return concreteValue.toString();
	}

	public static final ConcreteIntValue MINUSONE = new ConcreteIntValue(-1);
	public static final ConcreteIntValue ZERO = new ConcreteIntValue(0);
	public static final ConcreteIntValue ONE = new ConcreteIntValue(1);
	public static final ConcreteIntValue TWO = new ConcreteIntValue(2);
	public static final ConcreteIntValue THREE = new ConcreteIntValue(3);
	public static final ConcreteIntValue FOUR = new ConcreteIntValue(4);
	public static final ConcreteIntValue FIVE = new ConcreteIntValue(5);
	public static final ConcreteIntValue EIGHT = new ConcreteIntValue(8);

	static final ConcreteIntValue[] vals = new ConcreteIntValue[] {
		MINUSONE, ZERO, ONE, TWO, THREE, FOUR,
		FIVE,  null, null, EIGHT
	};

	static final ConcreteIntValue MAX_VALUE = new ConcreteIntValue(0x7fffffff);
	static final ConcreteIntValue MIN_VALUE = new ConcreteIntValue(0x80000000);

	public static ConcreteIntValue make(int val) {
	    int valp = val + 1;
	    if (valp >= 0 && valp < vals.length	&& vals[valp] != null)
		return vals[valp];
	    else
		return new ConcreteIntValue(val);
	}
    
    public void encode(ByteBuffer code) {
        SpecificationIR.encode(code, concreteValue.intValue(), type);
    }
    }

    
    public static class ValueList extends Value {
	public IntValue valueCount;
	public ValueList(IntValue valueCount) {
	    this.valueCount = valueCount;
	}
    public IntValue sizeValue() { return valueCount; }
    }

    public static class IntValueList extends ValueList implements StreamableValue {
        public IntValueList(IntValue valueCount) {
            super(valueCount);
        }
        public IntValue sizeValue() {
            return valueCount;
        }
        public char getType() {
            return TypeCodes.INT; // ?
        }
        public int bytestreamSize() {
            return 4; // ?
        }
        public Number decodeStream(MethodInformation iv, int offset) {
            throw new Error();
        }
        public int decodeInt(MethodInformation iv, int offset) {
            throw new Error();
        }
        public ConcreteStreamableValue concretize(MethodInformation iv, int offset) {
            throw new Error();
        }
        public ConcreteStreamableValue concretize(MethodInformation iv, int offset, int length) {
            offset += iv.getPC();
            ConcreteIntValue[] elems = new ConcreteIntValue[length];
            for(int i = 0; i < length; i++) {
                int e = iv.getCode().getInt(offset);
                elems[i] = new ConcreteIntValue(e);
                offset += 4;
            }
            return new ConcreteIntValueList(elems);
        }
    }

    public static class ConcreteIntValueList extends IntValueList 
        implements ConcreteStreamableValue {
        ConcreteIntValue[] values;
        public ConcreteIntValueList(ConcreteIntValue[] values) {
            super(new ConcreteIntValue(values.length));
            this.values = values;
        }
        public int count() {
            return countConcreteValue().intValue();
        }
        public ConcreteIntValue countConcreteValue() {
            return (ConcreteIntValue)valueCount;
        }
        public int bytestreamSize() {
            return count() * 4;
        }
        public ConcreteIntValue[] getElements() {
            return values;
        }
        public void encode(ByteBuffer code) {
            for (int i = 0; i < values.length; i++) {
                SpecificationIR.encode(code, values[i].intValue(), TypeCodes.INT);
            }
        }
    }

    // Used in TABLESWITCH
    public static class PCValueList extends ValueList implements StreamableValue {
        public PCValueList(IntValue valueCount) {
            super(valueCount);
        }
        public IntValue sizeValue() {
            return valueCount;
        }
        public char getType() {
            return TypeCodes.INT; // ?
        }
        public int bytestreamSize() {
            return 4; // ?
        }
        public Number decodeStream(MethodInformation iv, int offset) {
            throw new Error();
        }
	public int decodeInt(MethodInformation iv, int offset) {
	    throw new Error();
	}
        public ConcreteStreamableValue concretize(MethodInformation iv, int offset) {
            throw new Error();
        }
        public ConcreteStreamableValue concretize(MethodInformation iv, int offset, int length) {
            offset += iv.getPC();
            ConcretePCValue[] elems = new ConcretePCValue[length];
            for(int i = 0; i < length; i++) {
                int e = iv.getCode().getInt(offset);
                elems[i] = new ConcretePCValue(e);
                offset += 4;
            }
            return new ConcretePCValueList(elems);
        }
    }

    public static class ConcretePCValueList extends PCValueList 
        implements ConcreteStreamableValue {
        ConcretePCValue[] values;
        public ConcretePCValueList(ConcretePCValue[] values) {
            super(new ConcreteIntValue(values.length));
            this.values = values;
        }
        public int count() {
            return countConcreteValue().intValue();
        }
        public ConcreteIntValue countConcreteValue() {
            return (ConcreteIntValue)valueCount;
        }
        public int bytestreamSize() {
            return count() * 4;
        }
        public ConcretePCValue[] getElements() {
            return values;
        }
        public void encode(ByteBuffer code) {
            for (int i = 0; i < values.length; i++) {
                values[i].encode(code);
            }
        }
        public void encode(ByteBuffer code, int startOfInstruction) {
            for (int i = 0; i < values.length; i++) {
                values[i].encode(code, startOfInstruction);
            }
        }
    }

    // List of (int, pcvalue) pairs. Used in LOOKUPSWITCH
    public static class IntPCValuePairList extends ValueList implements StreamableValue {
        public IntPCValuePairList(IntValue valueCount) {
            super(valueCount);
        }
        public IntValue sizeValue() {
            return valueCount;
        }
        public char getType() {
            return TypeCodes.INT; // ?
        }
        public int bytestreamSize() {
            return 4; // ?
        }
        public Number decodeStream(MethodInformation iv, int offset) {
            throw new Error();
        }
        public int decodeInt(MethodInformation iv, int offset) {
            throw new Error();
        }
        public ConcreteStreamableValue concretize(MethodInformation iv, int offset) {
            throw new Error();
        }
        public ConcreteStreamableValue concretize(MethodInformation iv, int offset, int length) {
            offset += iv.getPC();
            ConcreteIntValue[] ints = new ConcreteIntValue[length];
            ConcretePCValue[] pcs = new ConcretePCValue[length];
            for(int i = 0; i < length; i++) {
                int e = iv.getCode().getInt(offset);
                ints[i] = new ConcreteIntValue(e);
                offset += 4;
                e = iv.getCode().getInt(offset);
                pcs[i] = new ConcretePCValue(e);
                offset += 4;
            }
            return new ConcreteIntPCValuePairList(ints, pcs);
        }
    }

    public static class ConcreteIntPCValuePairList extends IntPCValuePairList 
        implements ConcreteStreamableValue {
        ConcreteIntValue[] ints;
        ConcretePCValue[] pcs;
        public ConcreteIntPCValuePairList(ConcreteIntValue[] ints, ConcretePCValue[] pcs) {
            super(new ConcreteIntValue(ints.length * 2));
            if (ints.length != pcs.length) throw new Error("length does not match");
            this.ints = ints;
            this.pcs = pcs;
        }
        public int count() {
            return countConcreteValue().intValue();
        }
        public ConcreteIntValue countConcreteValue() {
            return (ConcreteIntValue)valueCount;
        }
        public int bytestreamSize() {
            return count() * 4;
        }
        public ConcreteIntValue[] getIntValues() {
            return ints;
        }
        public ConcretePCValue[] getPCValues() {
            return pcs;
        }
        public void encode(ByteBuffer code) {
            for (int i = 0; i < ints.length; i++) {
                ints[i].encode(code);
                pcs[i].encode(code);
            }
        }
        public void encode(ByteBuffer code, int startOfInstruction) {
            for (int i = 0; i < ints.length; i++) {
                ints[i].encode(code);
                pcs[i].encode(code, startOfInstruction);
            }
        }
    }

    public static class FloatValue extends Value 
	implements StreamableValue {
	static final FloatValue[] EMPTY_FARRAY = new FloatValue[0];

	public FloatValue(ValueSource source) {
	    super(source);
	}
	public char getType() {
	    return TypeCodes.FLOAT;
	}

	public FloatValue() {
	}

	public String toString() {
	    return "FLOAT";
	}
 	public int bytestreamSize() {
	    return 4;
	}
	public Number decodeStream(MethodInformation iv,
				   int offset) {
	    offset += iv.getPC();
	    return new java.lang.Float(iv.getCode().getFloat(offset));
	}
	public int decodeInt(MethodInformation iv, int offset) {
	    throw new Error();
	}
 	
    public float decode(MethodInformation iv, int offset) {
        return ((java.lang.Float)decodeStream(iv, offset)).floatValue();
    }
    public ConcreteStreamableValue concretize(MethodInformation iv,
            int offset) {
        return new ConcreteFloatValue(decode(iv, offset));
    }

    }

    public static class ConcreteFloatValue extends FloatValue
	implements ConcreteStreamableValue
    {
	java.lang.Float concreteValue;
	public Object concreteValue() {
	    return concreteValue;
	}
	public ConcreteFloatValue(float val) {
	    concreteValue = new java.lang.Float(val);
	}
	public String toString() {
	    if ( this == POSITIVE_INFINITY )
		return " ((jfloat)HUGE_VAL) ";
	    if ( this == NEGATIVE_INFINITY )
		return " ((jfloat)-HUGE_VAL) ";
	    return concreteValue.toString();
	}
	static final ConcreteFloatValue ZERO = new ConcreteFloatValue(0f);
	static final ConcreteFloatValue ONE = new ConcreteFloatValue(1f);
	static final ConcreteFloatValue TWO = new ConcreteFloatValue(2f);
	static final ConcreteFloatValue POSITIVE_INFINITY
	     = new ConcreteFloatValue(1.0f / 0.0f);
	static final ConcreteFloatValue NEGATIVE_INFINITY
	     = new ConcreteFloatValue(-1.0f / 0.0f);

    public void encode(ByteBuffer code) {
        code.putFloat(concreteValue.floatValue());
    }
    }

    public static class SecondHalf extends Value {
	SecondHalf(ValueSource source) {
	    super(source);
	}
	public boolean isWide() {
	    return false;
	}
    }

    public abstract static class WideValue extends Value {
	final SecondHalf secondHalf;
	public WideValue(ValueSource localsSlot) {
	    super(localsSlot);
	    secondHalf = new SecondHalf(localsSlot);
	}
	public WideValue() {
	    secondHalf = new SecondHalf(null);
	}
	public boolean isWide() {
	    return true;
	}
	public Value getSecondHalf() {
	    return secondHalf;
	}
    }

    public static class DoubleValue extends WideValue
	implements StreamableValue {
	static final DoubleValue[] EMPTY_DARRAY = new DoubleValue[0];
	static final char type = TypeCodes.DOUBLE;
	public DoubleValue(ValueSource source) {
	    super(source);
	}
	public DoubleValue() {
	}

	public char getType() { return type; }
	public String toString() {
	    return "DOUBLE";
	}
 	public int bytestreamSize() {
	    return 8;
	}
	public Number decodeStream(MethodInformation iv,
				   int offset) {
	    offset += iv.getPC();
	    return new java.lang.Double(iv.getCode().getDouble(offset));
	}
	public int decodeInt(MethodInformation iv, int offset) {
	    throw new Error();
	}
    public double decode(MethodInformation iv, int offset) {
        return ((java.lang.Double)decodeStream(iv, offset)).doubleValue();
    }
    public ConcreteStreamableValue concretize(MethodInformation iv,
            int offset) {
        return new ConcreteDoubleValue(decode(iv, offset));
    }

   }

    public static class ConcreteDoubleValue extends DoubleValue
	implements ConcreteStreamableValue
    {
	java.lang.Double concreteValue;
	public static final ConcreteDoubleValue ZERO 
	    = new ConcreteDoubleValue(0D);
	public static final ConcreteDoubleValue ONE 
	    = new ConcreteDoubleValue(1D);
	public static final ConcreteDoubleValue POSITIVE_INFINITY
	    = new ConcreteDoubleValue(1.0D / 0.0D);
	public static final ConcreteDoubleValue NEGATIVE_INFINITY
	    = new ConcreteDoubleValue(-1.0D / 0.0D);

	public ConcreteDoubleValue(double d) {
	    concreteValue = new java.lang.Double(d);
	}
	public Object concreteValue() {
	    return concreteValue;
	}
	public String toString() {
	    if ( this == POSITIVE_INFINITY )
		return " ((jdouble)HUGE_VAL) ";
	    if ( this == NEGATIVE_INFINITY )
		return " ((jdouble)-HUGE_VAL) ";
	    return concreteValue.toString();
	}
    public void encode(ByteBuffer code) {
        code.putDouble(concreteValue.doubleValue());
    }

    }


    public static class LongValue extends WideValue 
	implements StreamableValue {
	static final LongValue[] EMPTY_LARRAY = new LongValue[0];
	char type = TypeCodes.LONG;

	public char getType() { return type; }

	public LongValue(ValueSource source) {
	    super(source);
	}

	public LongValue(char type, ValueSource source) {
	    super(source);
	    this.type = type;
	}


	public LongValue(char type) {
	    this.type = type;
	}

	public LongValue() {
	}

	public String toString() {
	    return type == TypeCodes.LONG ? "LONG" : "ULONG";
	}
	public int bytestreamSize() {
	    return 8;
	}
	public Number decodeStream(MethodInformation iv,
				   int offset) {
	    offset += iv.getPC();
	    return new java.lang.Long(iv.getCode().getLong(offset));
	}
	public int decodeInt(MethodInformation iv, int offset) {
	    throw new Error();
	}
    public long decode(MethodInformation iv, int offset) {
        return ((java.lang.Long)decodeStream(iv, offset)).longValue();
    }
    public ConcreteStreamableValue concretize(MethodInformation iv,
            int offset) {
        return new ConcreteLongValue(decode(iv, offset));
    }

    }

    public static class ConcreteLongValue extends LongValue implements ConcreteStreamableValue {
	java.lang.Long concreteValue;
	public Object concreteValue() {
	    return concreteValue;
	}
	public ConcreteLongValue(long val) {
	    concreteValue = new java.lang.Long(val);
	}
	public String toString() {
	    if ( this == MAX_VALUE )
		return " ((jlong) 0x7fffffffffffffffLL) ";
	    if ( this == MIN_VALUE )
		return " ((jlong) 0x8000000000000000LL) ";
	    return concreteValue.toString();
	}
	static final ConcreteLongValue ZERO = new ConcreteLongValue(0L);
	static final ConcreteLongValue ONE = new ConcreteLongValue(1L);
	static final ConcreteLongValue MAX_VALUE = new ConcreteLongValue(0x7fffffffffffffffL);
	static final ConcreteLongValue MIN_VALUE = new ConcreteLongValue(0x8000000000000000L);

    public void encode(ByteBuffer code) {
        code.putLong(concreteValue.longValue());
    }

    }

    public static class RefValue extends Value 
	implements AbstractValue.Reference {
	public RefValue() {}
	public RefValue(ValueSource vs) {
	    super(vs);
	}
	public boolean isReference() {
	    return true;
	}

	public AbstractValue.Reference getReference() {
	    return this;
	}

	public TypeName.Compound getCompoundTypeName() {
	    throw new AbstractValueError(); 
	}


	public boolean isArray() {
	    throw new AbstractValueError(); 
	}

	public AbstractValue.Array getArray() {
	    throw new AbstractValueError();
	}

	public boolean isInitialized() {
	    throw new AbstractValueError();
	}

	public void initialize() {
	    throw new AbstractValueError();
	}

	public boolean isNull() {
	    throw new AbstractValueError();
	}

	public AbstractValue.Null getNull() {
	    throw new AbstractValueError();
	}	    

    }

    public static class NullRefValue extends RefValue 
	implements AbstractValue.Null, StreamableValue, ConcreteStreamableValue {
	String concreteValue = "NULL_REFERENCE";
	static final NullRefValue INSTANCE = new NullRefValue();
	public Object concreteValue() {
	    return concreteValue;
	}
	public int intValue() {
	    return 0;
	}
	public char getType() {
	    return TypeCodes.THENULL;
	}
	public boolean isNull() {
	    return true;
	}
	public AbstractValue.Null getNull() {
	    return this;
	}
 	public int bytestreamSize() {
	    return 0;
	}
	public Number decodeStream(MethodInformation iv,
				   int offset) {
	    return null;
	}
	public int decodeInt(MethodInformation iv, int offset) {
	    // An different than returning null above?
	    throw new Error();
	}

        public String toString() {
            return concreteValue;
        }

    public ConcreteStreamableValue concretize(MethodInformation iv,
            int offset) {
        return this;
    }
    public void encode(ByteBuffer code) {
        throw new Error();
    }
    }

    /*
     * FIXME This type is used in two ways: In input specs, it signals that a value
     * must be checked for null-ness, and in
     * output specs it signals that a value is not null.  Don't we
     * really need two separate types to keep everything straight?
     *
     * FIXME some more:  I don't think it appears in output specs or
     * evals everywhere that it could.
     */
    public static class NonnulRefValue extends RefValue {
	public NonnulRefValue(LocalExp localsSlot) {
	    super(localsSlot);
	}
	public NonnulRefValue(SymbolicConstant constant) {
	    super(constant);
	}
	public NonnulRefValue(MemExp memExp) {
	    super(memExp);
	}

	public NonnulRefValue(BlueprintAccessExp exp) {
	    super(exp);
	}
	public NonnulRefValue(FieldAccessExp exp) {
	    super(exp);
	}

	public NonnulRefValue(ArrayAccessExp exp) {
	    super(exp);
	}

	public NonnulRefValue(CSACallExp exp) {
	    super(exp);
	}
	public boolean isNull() {
	    return false;
	}


	public NonnulRefValue() {}
    }

    public static class NonnulArrayRefValue extends NonnulRefValue {
	public NonnulArrayRefValue() {}
	public NonnulArrayRefValue(SymbolicConstant constant) {
	    super(constant);
	}
	public NonnulArrayRefValue(MemExp memExp) {
	    super(memExp);
	}

	public NonnulArrayRefValue(FieldAccessExp exp) {
	    super(exp);
	}
	public boolean isNull() {
	    return false;
	}
    }

    public static class ValueSourceVisitor extends Runabout {
	protected void visitDefault(Object o) {
	}
    }

    public static class EmptyValueSourceVisitor extends ValueSourceVisitor {
	public void visit(ValueSource source) {}
    }


    public static interface ValueSource {
    }

    public static class CurrentPC extends OVMBase implements ValueSource {
    }

    public static class SymbolicConstant extends OVMBase implements ValueSource {
	public final String name;
	public SymbolicConstant(String name) {
	    this.name = name;
	}
    }

    public static class CPAccessExp extends OVMBase implements ValueSource {
	public /*final*/ Value value;
	public final boolean isWide;
	public CPAccessExp(Value value, boolean isWide) {
	    this.value = value;
	    this.isWide = isWide;
	}
	
	public CPAccessExp(Value value) {
	    this(value, false);
	}
    }

    public static class FieldAccessExp extends OVMBase implements ValueSource {
	public Selector.Field selector;
	public /*final*/ Value obj;
	public FieldAccessExp(Selector.Field selector,
			      Value obj) {
	    this.selector = selector;
	    this.obj = obj;
	}

	public FieldAccessExp(String defClassSel, 
			      String selStr,
			      Value obj) {
	    this.selector = RepositoryUtils.fieldSelectorFor(defClassSel,
							     selStr);
	    this.obj = obj;
	}
    }

    public static class LookupExp extends OVMBase implements ValueSource {
	public /*final*/ Value bp;
	public final String   tableName;
	public /*final*/ Value index;

	public LookupExp(Value bp, String tableName, Value index) {
	    this.bp = bp;
	    this.tableName = tableName;
	    this.index = index;
	}
    }
    public static class LinkSetAccessExp extends OVMBase implements ValueSource {
	public /*final*/ Value index;
	public LinkSetAccessExp(Value index) {
	    this.index = index;
	}
	
    }

    public static class Temp extends OVMBase implements ValueSource {
	public String type;	// Hmm.  what should this be?
	public String name;
	public Value init;

	public Temp(String type, String name, Value init) {
	    this.type = type;
	    this.name = name;
	    this.init = init;
	}
    }

    public static class LocalExp extends OVMBase implements ValueSource  {
	public static final LocalExp[] EMPTY_ARRAY = new LocalExp[0];
	public /*final*/ Value number;
	public LocalExp(Value number) {
	    this.number = number;
	}
	public int Value() {
	    return number.intValue();
	}
    }

    public static class BinExp extends OVMBase implements ValueSource {
	public String operator;
	public /*final*/ Value lhs;
	public /*final*/ Value rhs;
	public BinExp(Value lhs, Value rhs) {
	    this.lhs = lhs;
	    this.rhs = rhs;
	}
	public BinExp(Value lhs, String operator, Value rhs) {
	    this.lhs = lhs;
	    this.operator = operator;
	    this.rhs = rhs;
	}
    }

    public static class UnaryExp extends OVMBase implements ValueSource {
	public String operator;
	public /*final*/ Value arg;
	public UnaryExp(String operator, Value arg) {
	    this.arg = arg;
	    this.operator = operator;
	}
    }

    // Convert the before value to a new type.  The type is defined in
    // the our parent Value node, so f2i looks something like
    // new IntValue(new ConversionExp(new FloatValue()))
    public static class ConversionExp extends OVMBase implements ValueSource {
	public Value before;
	public ConversionExp(Value before) {
	    this.before = before;
	}
    }

    // A ReinterpretExp is similar to a ConversionExp, except that it
    // does not perform numeric conversions.  floatToIntBits can be
    // definined in terms of a ReinterpretExp, but not a ConversionExp.
    public static class ReinterpretExp extends OVMBase
	implements ValueSource
    {
	public Value before;
	public ReinterpretExp(Value before) {
	    this.before = before;
	}
    }

    // Processor-specific.  Mask higher-order bits of shift exponent
    // if the processor doesn't do it for us.
    public static class ShiftMaskExp extends OVMBase implements ValueSource {
	public Value exponent;
	public Value sizeType;
	public ShiftMaskExp(Value exponent, Value sizeType) {
	    this.exponent = exponent;
	    this.sizeType = sizeType;
	}
    }
    public static class ListElementExp extends OVMBase implements ValueSource {
	//public IntValueList list;
        public PCValueList list;
	public Value index;
	//public ListElementExp(IntValueList list, Value index) {
//	    this.list = list;
//	    this.index = index;
//	}
    public ListElementExp(PCValueList list, Value index) {
        this.list = list;
        this.index = index;
    }
    }

    public static class LocalStore extends OVMBase implements ValueSource {
	public /*final*/ Value index;
	public /*final*/ Value value;

	public LocalStore(Value index, Value value) {
	    this.index = index;
	    this.value = value;
	}	
	
    }

    public static class AssignmentExp extends OVMBase implements ValueSource {
	public /*final*/ Value dest;
	public /*final*/ Value src;

	public AssignmentExp(Value dest, Value src) {
	    this.dest = dest;
	    this.src = src;
	}

    }

    public static class CondExp extends OVMBase implements ValueSource {
	public /*final*/ Value lhs;
	public /*final*/ Value rhs;
	public final String operator;
	public CondExp(Value lhs, String operator, Value rhs) {
	    this.lhs = lhs;
	    this.operator = operator;
	    this.rhs = rhs;
	}
    }

    public static class IfExp extends OVMBase implements ValueSource {
	public /*final*/ Value cond;
	public /*final*/ Value ifTrue;
	public /*final*/ Value ifFalse;
	public IfExp(CondExp cond, Value ifTrue, Value ifFalse) {
	    this(new Value(cond), ifTrue, ifFalse);
	}
	public IfExp(Value cond, Value ifTrue, Value ifFalse) {
	    this.cond = cond;
	    this.ifTrue = ifTrue;
	    this.ifFalse = ifFalse;
	}
    }

    public static class MemExp extends OVMBase implements ValueSource {
	public /*final*/ Value addr;
	public /*final*/ Value offset;
	public MemExp(Value addr, Value offset) {
	    this.addr = addr;
	    this.offset = offset;
	}
    }

    public static class ArrayAccessExp extends OVMBase implements ValueSource {
	public /*final*/ Value arr;
	public /*final*/ Value index;
        public final boolean isStore;
	public ArrayAccessExp(Value arr, Value index) {
            this(arr, index, false);
	}
	public ArrayAccessExp(Value arr, Value index, boolean isStore) {
	    this.arr = arr;
	    this.index = index;
            this.isStore = isStore;
	}

    }



    public static class ArrayLengthExp extends OVMBase implements ValueSource {
	public /*final*/ Value arr;
	public ArrayLengthExp(Value arr) {
	    this.arr = arr;
	}
    }

    public static class BlueprintAccessExp extends OVMBase implements ValueSource {
	public /*final*/ Value ref;
	public BlueprintAccessExp(Value ref) {
	    this.ref = ref;
	}
    }

    public static class BitFieldExp extends OVMBase implements ValueSource {
	public /*final*/ Value word;
	public final int shift;
	public final int mask;

	public BitFieldExp(Value word, int shift, int mask) {
	    this.shift = shift;
	    this.mask = mask;
	    this.word = word;
	}
    }

    public static class CallExp extends OVMBase implements ValueSource {
	public final String fname;
	public final Value[] args;
	public CallExp(String fname, Value[] args) {
	    this.fname = fname;
	    this.args = args;
	}
	public CallExp(String fname) {
	    this(fname, new Value[0]);
	}
	public CallExp(String fname, Value arg1) {
	    this(fname, new Value[] {arg1});
	}
	public CallExp(String fname, Value arg1, Value arg2) {
	    this(fname, new Value[] {arg1, arg2});
	}

	public CallExp(String fname, Value arg1, Value arg2, Value arg3) {
	    this(fname, new Value[] {arg1, arg2, arg3});
	}

	public CallExp(String fname, Value arg1, Value arg2, Value arg3,
		       Value arg4) {
	    this(fname, new Value[] {arg1, arg2, arg3, arg4});
	}

	public CallExp(String fname, Value arg1, Value arg2, Value arg3,
		       Value arg4, Value arg5) {
	    this(fname, new Value[] {arg1, arg2, arg3, arg4, arg5});
	}

    }

    public static class CurrentConstantPool 
	implements ValueSource {
    }
    
    public static class RootsArrayBaseAccessExp implements ValueSource {
    }

    public static class RootsArrayOffsetExp implements ValueSource {
      public final int idx;
      
      public RootsArrayOffsetExp( int idx ) {
        this.idx = idx;
      }
    }


    public static class CSACallExp extends OVMBase implements ValueSource {
    
        public static class Names {
            // string names for the calls that we want to identify by == on string
            public static final String aastoreBarrier = "aastoreBarrier";
            public static final String bastoreBarrier = "bastoreBarrier";            
            public static final String castoreBarrier = "castoreBarrier";            
            public static final String dastoreBarrier = "dastoreBarrier";            
            public static final String fastoreBarrier = "fastoreBarrier";
            public static final String iastoreBarrier = "iastoreBarrier";            
            public static final String sastoreBarrier = "sastoreBarrier";            
            public static final String lastoreBarrier = "lastoreBarrier";            
            public static final String aaloadBarrier = "aaloadBarrier";
            public static final String baloadBarrier = "baloadBarrier";            
            public static final String caloadBarrier = "caloadBarrier";            
            public static final String daloadBarrier = "daloadBarrier";            
            public static final String faloadBarrier = "faloadBarrier";
            public static final String ialoadBarrier = "ialoadBarrier";            
            public static final String saloadBarrier = "saloadBarrier";            
            public static final String laloadBarrier = "laloadBarrier";            
            public static final String acmpneBarrier = "acmpneBarrier";
            public static final String acmpeqBarrier = "acmpeqBarrier";            
        }
        
	public final String fname;
	public final Value[] args;
	public CSACallExp(String fname, Value[] args) {
	    this.fname = fname;
	    this.args = args;
	}
	public CSACallExp(String fname) { this(fname, new Value[0]); }
	public CSACallExp(String fname, Value arg1) {
	    this(fname, new Value[] {arg1});
	}
	public CSACallExp(String fname, Value arg1, Value arg2) {
	    this(fname, new Value[] {arg1, arg2});
	}
	public CSACallExp(String fname, Value arg1, Value arg2, Value arg3) {
	    this(fname, new Value[] {arg1, arg2, arg3});
	}
    }

    // if for effect.  else may be null, and types need not match
    public static class IfCmd extends IfExp {
	public IfCmd(Value c, Value t, Value e) { super(c, t, e); }
	public IfCmd(Value c, Value t)       { super(c, t, null); }
    }
}
