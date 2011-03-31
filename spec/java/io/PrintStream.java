package java.io;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


@SCJAllowed
public class PrintStream {

    @RunsIn(CALLER) 
    public void print(@Scope(UNKNOWN) String s) {}
    
    @RunsIn(CALLER) 
    public void println(@Scope(UNKNOWN) String s) {}
}
