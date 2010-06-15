package test.runtime;

/**
 * Check that the cross-package 'protected'
 * access modifier works. The test that is set up compiles and should build
 * okay. If the test 'fails' there will be an access warning from the rewriter 
 * during the build and a crash at runtime.
 */

public class TestAccess extends test.common.TestBase {

    public static class Base {
        // a subclass in a different package: P1.Derived will try to invoke
        // this from a static method, using a P1.Derived instance.
        protected void protectedMethod() {}
    }


    public TestAccess() {
        super("Access check for 'protected' members");
    }

    public void run() {
        // if we get a build error then this will crash at runtime
        test.runtime.P1.Derived.testAccess();
    }
}
