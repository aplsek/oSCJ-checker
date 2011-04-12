package checkers;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public enum SCJMission {
    CYCLIC_EXECUTIVE("javax.safetycritical.CyclicExecutive", "initialize"),
    MISSION("javax.safetycritical.Mission", "initialize"),
    DEFAULT(null, null);

    public final String clazz;
    public final String signature;

    SCJMission(String clazz, String signature) {
        this.clazz = clazz;
        this.signature = signature;
    }

    @Override
    public String toString() {
        return clazz + "." + signature;
    }
    public static SCJMission fromMethod(ExecutableElement m, Elements elements,
            Types types) {
        boolean isStatic = Utils.isStatic(m);
        String signature = Utils.buildSignatureString(m);
        TypeMirror t = Utils.getMethodClass(m).asType();

        for (SCJMission sm : SCJMission.values()) {
            if (sm.equals(DEFAULT))
                continue;
            TypeMirror superType = Utils.getTypeMirror(
                    elements, sm.clazz);
            if (types.isSubtype(t,superType)) {
                return sm;
            }
        }
        return DEFAULT;
    }


    protected boolean isMissionInitialization(TypeElement t, ExecutableElement m) {
        //TypeMirror m = t.asType();
        //return types.isSubtype(m, schedulable);
        return false;
    }
}