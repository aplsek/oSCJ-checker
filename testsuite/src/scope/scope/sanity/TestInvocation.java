package scope.scope.sanity;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestInvocation {

    @RunsIn(CALLER)
    public static final void log(@Scope(UNKNOWN) Foo os,@Scope(UNKNOWN)  String s) {
        os.print(s);
        os.print(": ");
    }
}


class Foo {
    @RunsIn(CALLER)
    public void print(@Scope(UNKNOWN) String s) {
    }
}