package checkers;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public enum SCJMission {
    CYCLIC_EXECUTIVE("javax.safetycritical.CyclicExecutive", "initialize",""),
    MISSION("javax.safetycritical.Mission", "initialize",""),
    MS_NEXT_MISSION("javax.safetycritical.MissionSequencer", "getNextMission",""),
    DEFAULT(null, null,null);

    public final String clazz;
    public final String signature;
    public final String params;

    SCJMission(String clazz, String signature, String params) {
        this.clazz = clazz;
        this.signature = signature;
        this.params = params;
    }

    @Override
    public String toString() {
        return clazz + "." + signature;
    }
    public static SCJMission fromMethod(ExecutableElement m, Elements elements,
            Types types) {
        TypeMirror t = Utils.getMethodClass(m).asType();

        for (SCJMission sm : SCJMission.values()) {
            if (sm.equals(DEFAULT))
                continue;
            TypeMirror superType = Utils.getTypeMirror(
                    elements, sm.clazz);
            if (types.isSubtype(t,superType)) {
                if (m.toString().equals(Utils.buildSignatureString(sm.signature, sm.params)))
                    return sm;
                else
                    continue;
            }
        }
        return DEFAULT;
    }
}