package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;
import static javax.safetycritical.annotate.Level.SUPPORT;

import javax.realtime.MemoryArea;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="A", parent=IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestUnoknownReceiver extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestUnoknownReceiver() { super(null, null); }

    @Scope("A")
    @DefineScope(name="B", parent="A")
    @SCJAllowed(members = true)
    abstract static class MS extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS() { super(null, null); }
    }

    static class X {

        void method(@Scope(THIS) Y y) {

        }

    }

    static class Y {
        void method() {

        }
    }

    static class Z {

        @RunsIn(CALLER)
        void method(@Scope(UNKNOWN) X x) {
            Y y = new Y();
            x.method(y);
        }
    }

}
