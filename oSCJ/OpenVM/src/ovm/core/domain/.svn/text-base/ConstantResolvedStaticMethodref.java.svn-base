package ovm.core.domain;

import ovm.services.bytecode.JVMConstants;

/**
 * The type of an object stored in a constant pool entry of tag
 * CONSTANT_ResolvedStaticMethod. Referred to by the resolved version
 * of INVOKESTATIC.
 */
public class ConstantResolvedStaticMethodref extends ConstantResolvedMethodref {
 
    private final int nvtable_index;
    private final Oop sharedState;

    static private final int MASK_ = 0x3ff;
    static private final int SIZE_ = MASK_ + 1;
    static ConstantResolvedStaticMethodref[] cache_ = new ConstantResolvedStaticMethodref[SIZE_];

    static public ConstantResolvedStaticMethodref make(Method m, int index, Oop sh) {
        int i = (m.hashCode() + index) & MASK_;
        ConstantResolvedStaticMethodref c = cache_[i];
        if (c != null && c.equals(m, index, sh)) return c;
        c = new ConstantResolvedStaticMethodref(m, index, sh);
        cache_[i] = c;
        return c;
    }
 
    private ConstantResolvedStaticMethodref(Method m, int idx, Oop shst) {
        super(m);
        nvtable_index = idx;
        sharedState = shst;
    }
    
    public byte getTag()        { return JVMConstants.CONSTANT_ResolvedStaticMethod; }
    public int getOffset()      { return nvtable_index;  }
    public Oop getSharedState() { return sharedState;   }
    private boolean equals(Method m, int i, Oop sh) {
        return getMethod() == m && i == nvtable_index && sh == sharedState;
    }

    public String toString() {
	return "ResolvedStaticMethod{" + getMethod().toString() + "," + nvtable_index + "}";
    }
  
}
