//./tests/scjAllowed/TestInfrastructure.java:15: Elements outside of javax.realtime or javax.safetycritical packages cannot be annotated with @SCJAllowed(SUPPORT), @SCJAllowed(INFRASTRUCTURE), @SCJAllowed(HIDDEN).
//public class TestInfrastructure extends SCJInfrastructure {    // ERROR - can not override infrastructure
//       ^
//1 error

package scjAllowed;

import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.HIDDEN;

import javax.safetycritical.SCJInfrastructure;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(HIDDEN)
public class TestInfrastructure extends SCJInfrastructure {    // ERROR - can not override infrastructure
}
