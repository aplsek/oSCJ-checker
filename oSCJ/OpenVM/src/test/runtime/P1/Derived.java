package test.runtime.P1;
import test.runtime.TestAccess;

public class Derived extends TestAccess.Base {

    public static void testAccess() {
        new Derived().protectedMethod();
    }
}

