package scope.defineScope.simple;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestDuplicateScopeName {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static abstract class X extends Mission { }
    @DefineScope(name="b", parent=Scope.IMMORTAL)
    static abstract class Y extends Mission { }
    @DefineScope(name="b", parent="a")
    //## checkers.scope.DefineScopeChecker.ERR_DUPLICATE_SCOPE_NAME
    static abstract class Z extends Mission { }
}
