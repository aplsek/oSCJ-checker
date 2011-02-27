package scope.override;

import static javax.safetycritical.annotate.Scope.IMMORTAL;
import static javax.safetycritical.annotate.Scope.CURRENT;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

public class SimpleOverride2 {
}


class A {
    @RunsIn(CURRENT)
    void foo() { }
}

@DefineScope(name="b",parent=IMMORTAL)
@Scope("b")
class B extends A {

    void bar(B b) {
        b.foo();
    }

}