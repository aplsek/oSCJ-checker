package checkers;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.safetycritical.PeriodicEventHandler;

public enum SCJMission {

    CYCLIC_EXECUTIVE_INIT("javax.safetycritical.CyclicExecutive", "initialize",""),

    CYCLIC_EXECUTIVE_GET_SCHEDULE("javax.safetycritical.CyclicExecutive", "getSchedule","javax.safetycritical.PeriodicEventHandler[]"),

    MISSION_INIT("javax.safetycritical.Mission", "initialize",""),

    MS_NEXT_MISSION("javax.safetycritical.MissionSequencer", "getNextMission",""),

    SAFELET_GET_SEQUENCER("javax.safetycritical.Safelet","getSequencer",""),
    SAFELET_SET_UP("javax.safetycritical.Safelet","setUp",""),
    SAFELET_TEAR_DOWN("javax.safetycritical.Safelet","tearDown",""),

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
            if (types.isSubtype(t,superType) ) {
                if (m.toString().equals(Utils.buildSignatureString(sm.signature, sm.params))) {
                    return sm;
                }
                else
                    continue;
            }
        }
        return DEFAULT;
    }

    static void pln(String str) {System.out.println("\t" + str);}
}