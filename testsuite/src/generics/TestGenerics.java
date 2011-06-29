package generics;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import java.util.HashSet;
import java.util.Set;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
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
    class TT {

        Set<I> set = new HashSet();
        Set<?> set2 = new HashSet();

        void method() {
          Class<TestGenerics> cc = TestGenerics.class;
        }


        public void m() {
            I i = new I();
            Object o = new Object();
            set.add(i);
        }

    }

    @SCJAllowed(members = true)
    @Scope("A")
    class I {}
}

