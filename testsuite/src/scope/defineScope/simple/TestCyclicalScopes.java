package scope.defineScope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestCyclicalScopes {
    @DefineScope(name="a", parent="b")
    static abstract class X extends Mission { }
    @DefineScope(name="b", parent="c")
    static abstract class Y extends Mission { }
    @DefineScope(name="c", parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_CYCLICAL_SCOPES
    static abstract class Z extends Mission { }
    // Suppresses the error on class Y.
    @DefineScope(name="c", parent=Scope.IMMORTAL)
    static abstract class W extends Mission { }
}
