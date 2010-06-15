package ovm.core.domain;

import ovm.services.bytecode.JVMConstants;

/**
 * The type of an object stored in a constant pool entry of tag
 * CONSTANT_ResolvedInterfaceMethod. Referred to by the resolved
 * version of INVOKEINTERFACE.
 */
public class ConstantResolvedInterfaceMethodref extends ConstantResolvedMethodref {

    private final int iftable_index;
    private final Blueprint staticDefinerBlueprint;

    static private final int MASK_ = 0x3ff;
    static private final int SIZE_ = MASK_ + 1;
    static ConstantResolvedInterfaceMethodref[] cache_ = 
                                new ConstantResolvedInterfaceMethodref[SIZE_];

    
    static public ConstantResolvedInterfaceMethodref make(Method m, int index, Blueprint bp) {
        int i = (m.hashCode() + index) & MASK_;
        ConstantResolvedInterfaceMethodref c = cache_[i];
        if ( c != null && c.equals(m, index) ) return c;
        c = new ConstantResolvedInterfaceMethodref(m, index, bp);
        cache_[i] = c;
        return c;
    }
        
    private ConstantResolvedInterfaceMethodref(Method m, int ifidx, Blueprint bp) {
        super(m);
        iftable_index = ifidx;
        staticDefinerBlueprint = bp;
    }

    private boolean equals(Method m, int i) { return getMethod() == m && i == iftable_index; }
    public byte getTag()   { return JVMConstants.CONSTANT_ResolvedInterfaceMethod; }
    public Blueprint getStaticDefinerBlueprint() { return staticDefinerBlueprint; }
    public int getOffset() { return iftable_index; }
    
    public String toString() {
        return "ResolvedInterfaceMethod{" + getMethod().toString() + "," + iftable_index + "}";
    }
}
