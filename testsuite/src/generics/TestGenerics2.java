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
public class TestGenerics2 {

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
    class TT {

        Set<I> set = new HashSet();
        Set<?> set2 = new HashSet();

        void method() {
            Class<TestGenerics2> cc = TestGenerics2.class;
        }

        public void m() {
            I i = new I();
            Object o = new Object();
            set.add(i);
        }

    }

    @SCJAllowed(members = true)
    @Scope("A")
    class I {
        public <T> T m(T t) {
            return t;
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
    public static class V {}
}
