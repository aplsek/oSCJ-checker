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
public class TestGenerics3 {

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

    @SCJAllowed(members = true)
    public static class B <T> {

        public T m(T t) {
            return t;
        }

        public @Scope("A") T mA(@Scope("A") T t) {
            @Scope("A") T tt = t;
            return t;
        }

        @RunsIn(CALLER) @Scope("A")
        public T mAAA(@Scope("A") T t) {
            //@Scope("A") T tt = t;
            return t;
        }
    }

    @SCJAllowed(members = true)
    @Scope("A")
    public static class C <T> {
        public T m(T t) {
            return t;
        }

        public @Scope("A") T mA(@Scope("A") T t) {
            //@Scope("A") T tt = t;

            return t;
        }
    }

    @Scope("A")
    @SCJAllowed(members = true)
    public static class V {}

    @Scope("B")
    @SCJAllowed(members = true)
    public static class W <T> {
        @Scope("A") T ft;
    }

    /**
     * bounded generic type:
     */
    @Scope("B")
    @SCJAllowed(members = true)
    public static class WW <T extends V> {
        @Scope("A") T t;

        public T mA(T t) {
            this.t = t;
            return t;
        }
    }
}
