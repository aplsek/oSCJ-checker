package scope.defineScope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

@DefineScope(name="a", parent=IMMORTAL)
@Scope("a")
public abstract class TestUnusedDefineScopeMethod extends Mission {
    @DefineScope(name="b", parent=IMMORTAL)
    //## warning: checkers.scope.DefineScopeChecker.ERR_UNUSED_DEFINE_SCOPE ## checkers.scope.ScopeRunsInVisitor.ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT
    public void handleAsyncEvent() {
    }
}
