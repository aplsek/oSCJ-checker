//./tests/scjAllowed/SCJSupport.java:27: Method outside of SCJ packages can not override methods with @SCJAllowed(INFRASTRUCTURE) and higher level.
//    public int bar() {         // TODO :error, can not be overriden
//               ^
//./tests/scjAllowed/SCJSupport.java:32: Methods outside of javax.realtime or javax.safetycritical packages cannot be annotated with @SCJAllowed(SUPPORT).
//    public int foobar() {         // ERROR
//               ^
//2 errors

package scjAllowed;


import javax.safetycritical.FakeSupport;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.SUPPORT;

@SCJAllowed(value = LEVEL_1, members = true)
public class SCJSupport extends FakeSupport {

    public int foo() {         // this is NOT AN ERROR, we should be able to override this
        return 1;
    }
    
    @SCJAllowed(LEVEL_1)
    public int foo2() {         // NOT error, we should be able to override this
        return 1;
    }
    
    public int bar() {         // ERROR, can not be overriden
        return 1;
    }
    
    @SCJAllowed(SUPPORT)
    public int foobar() {         // ERROR
        return 1;
    }
    
}

