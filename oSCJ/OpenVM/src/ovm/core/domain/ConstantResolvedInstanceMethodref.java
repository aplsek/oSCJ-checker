package ovm.core.domain;

import ovm.services.bytecode.JVMConstants;

/**
 * The type of an object stored in a constant pool entry of tag
 * CONSTANT_ResolvedInstanceMethod. Referred to by the resolved version
 * of INVOKESPECIAL and INVOKEVIRTUAL.
 */
public class ConstantResolvedInstanceMethodref   extends ConstantResolvedMethodref {
    
    /**true if this is a private/constructor call in an INVOKESPECIAL, false if a super 
     * call in an INVOKESPECIAL or an INVOKEVIRTUAL call */
    public  final boolean   isNonVirtual; 
    private final int       table_index; 
    private final Blueprint staticDefinerBlueprint;

    static private final int MASK_ = 0x3ff;
    static private final int SIZE_ = MASK_ + 1;
    static ConstantResolvedInstanceMethodref[] cache_ = new ConstantResolvedInstanceMethodref[SIZE_];
    
    static public ConstantResolvedInstanceMethodref make(Method m, int index, Blueprint bp) {
        int i = (m.hashCode() + index) & MASK_;
        ConstantResolvedInstanceMethodref c = cache_[i];
        if (c != null && c.equals(m, index))   return c;
        c = new ConstantResolvedInstanceMethodref(m, index, bp);
        cache_[i] = c;
        return c;
    }

    private ConstantResolvedInstanceMethodref(Method m, int index, Blueprint bp) {
        super(m);
        this.isNonVirtual = m.isNonVirtual();
        staticDefinerBlueprint = bp;
        table_index = index;
    }

    public byte getTag() { return JVMConstants.CONSTANT_ResolvedInstanceMethod;  }
    public int getOffset() {  return table_index; }
    public Blueprint getStaticDefinerBlueprint() { return staticDefinerBlueprint; }
    private boolean equals(Method m, int i) {
        return getMethod() == m && i == table_index ;
    }

    public String toString() {
	return "ResolvedInstanceMethod{" + getMethod().toString() + "," + table_index + "}";
    }
    
}
