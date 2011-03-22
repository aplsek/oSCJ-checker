package checkers;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public enum SCJMethod {
    DEFAULT(null, null),

    /* MemoryArea */
    NEW_INSTANCE("javax.realtime.MemoryArea", "newInstance(java.lang.Class)"),

    NEW_INSTANCE_IN_AREA("javax.realtime.MemoryArea",
        "newInstanceInArea(java.lang.Object,java.lang.Class)"),

    NEW_ARRAY("javax.realtime.MemoryArea", "newArray(java.lang.Class,int)"),

    NEW_ARRAY_IN_AREA("javax.realtime.MemoryArea",
        "newArrayInArea(java.lang.Object,java.lang.Class,int)"),

    ENTER("javax.realtime.MemoryArea", "enter(int,java.lang.Runnable)"),

    GET_MEMORY_AREA("javax.realtime.MemoryArea",
        "getMemoryArea(java.lang.Object)"),

    /* AllocationContext */
    EXECUTE_IN_AREA("javax.realtime.AllocationContext",
        "executeInArea(javax.safetycritical.SCJRunnable)"),

    /* ManagedMemory */
    ENTER_PRIVATE_MEMORY("javax.safetycritical.ManagedMemory",
        "enterPrivateMemory(long,javax.safetycritical.SCJRunnable)"),

    GET_CURRENT_MANAGED_MEMORY("ManagedMemory", "getCurrentManagedMemory()"),

    ALLOC_IN_SAME("javax.safetycritical.ManagedMemory",
        "allocInSame(java.lang.Object,java.lang.Object)"),

    ALLOC_IN_PARENT("javax.safetycritical.ManagedMemory",
        "allocInParent(java.lang.Object,java.lang.Object)"),

    /* ImmortalMemory */
    IMMORTAL_MEMORY_INSTANCE("javax.realtime.ImmortalMemory", "instance()");


    private final String clazz;
    private final String signature;

    SCJMethod(String clazz, String signature) {
        this.clazz = clazz;
        this.signature = signature;
    }

    @Override
    public String toString() {
        return clazz + "." + signature;
    }

    public static SCJMethod fromMethod(ExecutableElement m, Elements elements,
            Types types) {
        boolean isStatic = Utils.isStatic(m);
        String signature = Utils.buildSignatureString(m);
        TypeMirror t = Utils.getMethodClass(m).asType();

        for (SCJMethod sm : SCJMethod.values()) {
            if (sm.equals(DEFAULT))
                continue;
            TypeMirror s = Utils.getTypeMirror(elements, sm.clazz);
            if (signature.equals(sm.signature)) {
                if (isStatic && types.isSameType(s, t))
                    return sm;
                if (!isStatic && types.isSubtype(t, s))
                    return sm;
            }
        }
        return DEFAULT;
    }
}