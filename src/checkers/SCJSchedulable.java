package checkers;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

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

    public static SCJSchedulable fromMethod(TypeElement t, Elements elements,
            Types types) {
        TypeMirror m = t.asType();

        for (SCJSchedulable sm : SCJSchedulable.values()) {
            if (sm == null || sm.equals(DEFAULT))
                continue;
            TypeMirror schedulable = Utils.getTypeMirror(elements, sm.clazz);
            if (types.isSubtype(m, schedulable))
                return sm;
        }

        return DEFAULT;
    }
}