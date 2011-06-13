package checkers;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public enum SCJSafeUpcastDefinition {
    DEFAULT(null, null),

    GET_NEXT_MISSION("javax.safetycritical.MissionSequencer",
            "getNextMission()"),

    IMMORTAL_MEMORY_INSTANCE("javax.safetycritical.Safelet", "getSequencer()");

    private final String clazz;
    private final String signature;

    SCJSafeUpcastDefinition(String clazz, String signature) {
        this.clazz = clazz;
        this.signature = signature;
    }

    @Override
    public String toString() {
        return clazz + "." + signature;
    }

    public static SCJSafeUpcastDefinition fromMethod(ExecutableElement m,
            Elements elements, Types types) {
        boolean isStatic = Utils.isStatic(m);
        String signature = Utils.buildSignatureString(m);
        TypeMirror t = Utils.getMethodClass(m).asType();

        for (SCJSafeUpcastDefinition sm : SCJSafeUpcastDefinition.values()) {
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