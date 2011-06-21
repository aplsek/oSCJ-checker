package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members = true)
public class TestBadVariableScope {

    @Scope(IMMORTAL)
    static class Foo {
        public void foo2() {
            //## checkers.scope.ScopeChecker.ERR_BAD_VARIABLE_SCOPE
            @Scope(IMMORTAL) X x;
        }
    }

    @Scope("a")
    static class X {}


    @Scope(IMMORTAL)
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class MS extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MS() {super(null, null);}
    }
}
