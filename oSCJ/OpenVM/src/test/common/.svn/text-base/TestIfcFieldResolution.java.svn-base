package test.common;

public class TestIfcFieldResolution extends TestBase {

    public TestIfcFieldResolution() {
        super( "IfcFieldResolution");
    }

    // It appears that we currently (august 2003) pass these tests purely because
    // javac has very cooperatively resolved every reference to its exact
    // declaring type (as we know 1.2 doesn't always do with ordinary static
    // references), and not because we're actually smart enough not to fail if
    // it didn't do that.  But that's cool as long as javac keeps doing that.
    public void run() {
        check_condition( 6 == Z.ii );
        
        check_condition( 6 == A.ii );
        check_condition( 7 == A.jj );
        
        check_condition( 6 == B.ii );
        check_condition( 8 == B.jj );
        
        check_condition( 6 == C.ii );
        check_condition(-6 == C.kk );
        check_condition(15 == C.qq );
        
        check_condition( 6 == D.ii );
        check_condition(-6 == D.kk );
        check_condition(15 == D.qq );
        
        check_condition(-6 == E.ii );
    }

    interface Z {
      int ii = D.go();
    }
  
    interface A extends Z {
      int jj = 1 + ii;
    }
  
    interface B extends Z {
      int jj = 2 + ii;
    }
  
    interface C extends A, B {
      int kk = -ii;
      int qq = A.jj + B.jj;
    }
  
    static class D implements C {
      static int go() { return 6; }
    }
  
    static class E {
      static int ii = -C.ii;
    }
}
