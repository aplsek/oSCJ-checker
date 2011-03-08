package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestBadVariableScope {

    @Scope(IMMORTAL)
    class Foo {
        @RunsIn("a")
        public void foo2() {
            //## checkers.scope.ScopeChecker.ERR_BAD_VARIABLE_SCOPE
            @Scope("a") Baz baz;
        }
    }

    @Scope("a")
    @DefineScope(name="a", parent=IMMORTAL)
    static abstract class Bar extends Mission {
        public Foo a;
    }

    @Scope("b")
    @DefineScope(name="b", parent=IMMORTAL)
    static abstract class Baz extends Mission {
    }
}
