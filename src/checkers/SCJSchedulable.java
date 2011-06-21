package checkers;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * DEFINES schedulables and their corresponding entry-methods.
 */
public enum SCJSchedulable {
    PEH("javax.safetycritical.PeriodicEventHandler", "handleAsyncEvent"),
    APEH("javax.safetycritical.AperiodicEventHandler", "handleAsyncEvent"),
    MANAGED_THREAD("javax.safetycritical.ManagedThread", "run"),
    DEFAULT(null, null);

    public final String clazz;
    public final String signature;

    SCJSchedulable(String clazz, String signature) {
        this.clazz = clazz;
        this.signature = signature;
    }

    @Override
    public String toString() {
        return clazz + "." + signature;
    }

    public static SCJSchedulable fromMethod(TypeMirror m, Elements elements,
            Types types) {

        for (SCJSchedulable sm : SCJSchedulable.values()) {
            if (sm == null || sm == DEFAULT)
                continue;
            TypeMirror schedulable = Utils.getTypeMirror(elements, sm.clazz);
            if (types.isSubtype(m, schedulable))
                return sm;
        }

        return DEFAULT;
    }
}