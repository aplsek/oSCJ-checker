package scope.defineScope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestUnusedDefineScope extends Mission {
    @DefineScope(name="b", parent=IMMORTAL)
    //## warning: checkers.scope.DefineScopeChecker.ERR_UNUSED_DEFINE_SCOPE
    public void handleAsyncEvent() {
    }
}
