package ovm.core.domain;

import ovm.services.bytecode.JVMConstants;

/**
 * The type of an object stored in a constant pool entry of tag
 * CONSTANT_ResolvedStaticField. Referred to by the resolved version
 * of GETSTATIC and PUTSTATIC.
 */
public class ConstantResolvedStaticFieldref extends ConstantResolvedFieldref {
    
    protected final int offset;
    protected final Oop sharedState;

    static private final int SIZE_ = 1024;
    static ConstantResolvedStaticFieldref[] cache_ = new ConstantResolvedStaticFieldref[SIZE_];

    static public ConstantResolvedStaticFieldref make(Field f, int o, Oop sh) {
        int i = f.hashCode() + o;
        ConstantResolvedStaticFieldref c = cache_[i % SIZE_];
        if (c != null && c.equals(f, o, sh))   return c;
        c = new ConstantResolvedStaticFieldref(f, o, sh);
        cache_[i % SIZE_] = c;
        return c;
    }
 
    private ConstantResolvedStaticFieldref(Field f, int o, Oop shst) {
        super(f);
        offset = o;
        sharedState = shst;
    }    
 
    public int getOffset()      { return offset;  }
    public Oop getSharedState() { return sharedState;  }
    public byte getTag()        { return JVMConstants.CONSTANT_ResolvedStaticField; }
    private boolean equals(Field f, int o, Oop sh) {
        return field == f && (offset == o) && sh == sharedState;
    }

    public String toString() {
	return "ResolvedStaticField{" + field.toString() + "," + offset + "}";
    }
}
