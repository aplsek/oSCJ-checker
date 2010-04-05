package ovm.core.domain;

import ovm.services.bytecode.JVMConstants;

/**
 * The type of an object stored in a constant pool entry of tag
 * CONSTANT_ResolvedInstanceField. Referred to by the resolved
 * versions of GETFIELD and PUTFIELD.
 */
public class ConstantResolvedInstanceFieldref extends ConstantResolvedFieldref {

    private final int       offset;
    /**This appears to be nowhere used except for one error message.*/
    private final Blueprint staticDefinerBlueprint;

    static private final int SIZE_ = 1024;
    static ConstantResolvedInstanceFieldref[] cache_ = new ConstantResolvedInstanceFieldref[SIZE_];

    static public ConstantResolvedInstanceFieldref make(Field f, int o, Blueprint b) {
        int i = f.hashCode() + o;
        ConstantResolvedInstanceFieldref c = cache_[i % SIZE_];
        if (c != null && c.equals(f, o))
            return c;
        c = new ConstantResolvedInstanceFieldref(f, o, b);
        cache_[i % SIZE_] = c;
        return c;
    }

    
    private ConstantResolvedInstanceFieldref(Field f, int o, Blueprint b) {
        super(f);
        offset = o;
        staticDefinerBlueprint = b;
    }
    private boolean equals(Field f, int o) {  return field == f && (offset == o); }
    public int getOffset() {  return offset;  }
    public byte getTag() { return JVMConstants.CONSTANT_ResolvedInstanceField; }
    /**This appears to be nowhere used except for one error message. */
    public Blueprint getStaticDefinerBlueprint() { return staticDefinerBlueprint; }
}
