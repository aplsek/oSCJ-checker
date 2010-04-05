package ovm.core.domain;

import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.Selector;

public abstract class ConstantResolvedMethodref
    implements ConstantMethodref, ResolvedConstant
{
 
    private final Method method;

    public ConstantResolvedMethodref(Method m) { method = m;  }
    public abstract int getOffset();
    public Selector.Method asSelector() { return method.getSelector();     }
    public Method getMethod()           { return method;   }
    public boolean isResolved()         { return true;     }

    /**
     * Called when no further constant pool resolution will be done to
     * discard caches.
     **/
    public static void dropCaches() {
	ConstantResolvedInstanceMethodref.cache_ = null;
	ConstantResolvedInterfaceMethodref.cache_ = null;
	ConstantResolvedStaticMethodref.cache_ = null;
    }
}
