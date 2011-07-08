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
public class TestGenerics4 {

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

    /**
     * bounded generic type:
     */
    @Scope("B")
    @SCJAllowed(members = true)
    public static class W <T extends V> {
        @Scope("A") T t;
        T ft;

        public T mA(T t) {
            T tt = t;
            this.t = t;
            this.ft = t;
            return t;
        }

        public void assignT(@Scope("A") T t) {
            this.t = t;
        }

        @RunsIn(CALLER)
        public T m(T t) {
            this.ft = t;

            return t;
        }

        public T mm(T t) {
           V v = t;
           T tt = (T) v;
           return (T) v;
        }
    }

    //@Scope("B")
    //@SCJAllowed(members = true)
    //public static class WW <@Scope("A") T> {
    //}
}
