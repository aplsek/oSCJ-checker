package generics;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;

import java.util.HashSet;
import java.util.Set;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@Scope("A")
public class TestGenerics {

    @Scope(IMMORTAL)
    @DefineScope(name = "A", parent = IMMORTAL)
    @SCJAllowed(members = true)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {
            super(null, null);
        }
    }

    @Scope("A")
    @DefineScope(name = "B", parent = "A")
    @SCJAllowed(members = true)
    static abstract class Y extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public Y() {
            super(null, null);
        }
    }

    @Scope("A")
    @SCJAllowed(members = true)
    public static class V {}



    @Scope("B")
    @SCJAllowed(members = true)
    public static class WW <@Scope("A") T,@Scope("B") K extends V> {

        T t;

        @RunsIn(CALLER)
        public void mA(T t) {
           this.t = t;
        }
    }
}
