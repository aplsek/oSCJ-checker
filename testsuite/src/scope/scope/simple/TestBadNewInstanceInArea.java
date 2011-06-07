package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.realtime.MemoryArea;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
@DefineScope(name="a", parent=IMMORTAL)
@Scope(IMMORTAL)
public abstract class TestBadNewInstanceInArea extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestBadNewInstanceInArea() {super(null, null);}

    @SCJAllowed(members = true)
    @Scope("a")
    @DefineScope(name="b", parent="a")
    static abstract class X extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public X() {super(null, null);}

        Y y;

        public void foo() throws InstantiationException, IllegalAccessException {
                MemoryArea.newInstanceInArea(y, Y.class);
                //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
                MemoryArea.newInstanceInArea(y, Z.class);
        }


        @RunsIn("b")
        public void bar() throws InstantiationException, IllegalAccessException {
            Z z = new Z();
            //## checkers.scope.ScopeChecker.ERR_BAD_NEW_INSTANCE
            MemoryArea.newInstanceInArea(z, Y.class);
        }
    }

    @Scope("a")
    static class Y { }

    @Scope("b")
    static class Z { }
}
