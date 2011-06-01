package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(value = LEVEL_2, members = true)
public class TestInvocation {

    @RunsIn(CALLER)
    public static final void log(@Scope(UNKNOWN) Foo os,@Scope(UNKNOWN)  String s) {
        os.print(s);
        os.print(": ");
    }
}

@SCJAllowed(value = LEVEL_2, members = true)
class Foo {
    @RunsIn(CALLER)
    public void print(@Scope(UNKNOWN) String s) {
    }
}