package test;

public class TestIfcFieldResolution extends TestBase {

    public TestIfcFieldResolution( Harness h) {
        super( "IfcFieldResolution", h);
    }

    // It appears that we currently (august 2003) pass these tests purely because
    // javac has very cooperatively resolved every reference to its exact
    // declaring type (as we know 1.2 doesn't always do with ordinary static
    // references), and not because we're actually smart enough not to fail if
    // it didn't do that.  But that's cool as long as javac keeps doing that.
    public void run() {
        COREassert( 6 == Z.ii );
        
        COREassert( 6 == A.ii );
        COREassert( 7 == A.jj );
        
        COREassert( 6 == B.ii );
        COREassert( 8 == B.jj );
        
        COREassert( 6 == C.ii );
        COREassert(-6 == C.kk );
        COREassert(15 == C.qq );
        
        COREassert( 6 == D.ii );
        COREassert(-6 == D.kk );
        COREassert(15 == D.qq );
        
        COREassert(-6 == E.ii );
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
