package scope.defineScope.simple;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;

public class TestReservedScopeNameParentImmortal {
    @DefineScope(name="a", parent=Scope.IMMORTAL)
    static class X { }
}
