/**
 **/
package test;

/**
 * Test exception handling
 * @author Christian Grothoff
 * @author Hiroshi Yamauchi
 **/
public class TestExceptions
    extends TestBase {

    boolean doOverflow;
    
    public TestExceptions(Harness domain, boolean doOverflow) {
	super("Exceptions",domain);
	this.doOverflow = doOverflow;
    }

    public TestExceptions(Harness domain) {
	this(domain, true);
    }

    public TestExceptions(Harness domain, long disable) {
	this(domain, (disable & TestSuite.DISABLE_STACK_OVERFLOW) == 0);
    }

    public void run() {
	testSimpleTryCatch();
	testSubtypedCatch();
	testMultiCatch();
	testCallerCatch();
	testCallerCatchWithNoUnreachable();
	testNestedTryCatch();
	testNullDereference();
        testNPE();
	testArithmeticException();
	testArrayStore();
	testTrace();
	if (doOverflow)
	    testOverflow();
	testArrayIndex();
    }

    private int id(int x) { return x; }

    public void testArithmeticException() {
	int divisor = id(0);
	int foo = 3;
	try {
	    foo /= divisor;
	    id(foo);
	    COREfail("arithmetic exception not thrown");
	} catch (ArithmeticException e) {
	}
    }

    private void doStore(Object [] arr, Object o, boolean expected)
    {
	try {
	    arr[0] = o;
	    COREassert(!expected, "missing array store exception");
	} catch (ArrayStoreException _) {
	    COREassert(expected, "unexpected array store exception");
	}
    }

    public void testArrayStore() {
	doStore(new String[1], new Object(), true);
	doStore(new Object[1], new String("a string"), false);
	doStore(new Object[1], new Object(), false);
    }

    // Note that we return Object, then cast to Object[].  This is yet
    // another j2c workaround.  If returnNull where known to return an
    // Object[], j2c would devirtualize the toString() call, but omit
    // the check for null receiver.
    private static Object returnNull() { return null; }
    
    public void testNullDereference() {
	setModule("null dereference");
        Object o = returnNull();
	try {
	    o.toString();
	    COREfail("null pointer exception not thrown");
	} catch (NullPointerException npe) {
	    // npe.printStackTrace();
	}
    }


    // classes for testing null pointer exception throwing
    
    public class NPEClass {
      public boolean npeFieldBoolean=true;
      public Integer npeFieldInteger=null;
      public void foo() {
        return ;
      }
      public NPEClass() {
      };
    }
    
    public interface NPEInterface {
      public void foo();
    }
    
    public class NPEClassChild extends NPEClass implements NPEInterface {
      public void foo() {
        return;
      }
    }

    public abstract class NPECBase implements NPEInterface {
    }
    
    public class NPECMain extends NPECBase implements NPEInterface {
      public void foo() {
        return ;
      }
    }
    
    // the same classes, but these are used differently to prevent
    // de-virtualization ("T" stands for "with Tricks")
    
    public class NPEClassT {
      public boolean npeFieldBoolean=true;
      public Integer npeFieldInteger=null;
      public void foo() {
        System.out.print("");
        return ;
      }
      public NPEClassT() {
      };
    }
    
    public interface NPEInterfaceT {
      public void foo();
    }
    
    public class NPEClassChildT extends NPEClassT implements NPEInterfaceT {
      public void foo() {
        System.out.print("");      
        return;
      }
    }

    public class NPEClassChild2T extends NPEClassT implements NPEInterfaceT {
      public void foo() {
        System.out.print("");      
        return;
      }
    }

    public abstract class NPECBaseT implements NPEInterfaceT {
    }
    
    public class NPECMainT extends NPECBaseT implements NPEInterfaceT {
      public void foo() {
        System.out.print("");      
        return ;
      }
    }

    public class NPECMain2T extends NPECBaseT implements NPEInterfaceT {
      public void foo() {
        System.out.print("");      
        return ;
      }
    }

    public boolean dummy = true;
    
    public void testNPE() {
	setModule("null reference checks");
	Object o = returnNull();
	Object[] oa = (Object[]) returnNull();
	try {
	    synchronized(o) {
                COREfail("NPE check missing");
	    }
	    COREfail("NPE check missing (1)");
	} catch (NullPointerException npe) {
	}
	try {
	    o.toString();
	    COREfail("NPE check missing (2)");
	} catch (NullPointerException npe) {
	}
	try {
	    if (oa[1] == o)
                COREfail("NPE check missing (3)");

	    COREfail("NPE check missing (4)");
	} catch (NullPointerException npe) {
	}
	
	// throwing a null object shall result in throwing NPE
	ArrayIndexOutOfBoundsException nex = null;
	try {
	  if (dummy) throw nex;
	  COREfail("NPE check - failed to throw an exception (5)");
	} catch (NullPointerException npe) {
	} catch (Exception e) {
	  COREfail("Throwing null did not produce NPE (6) "+e);
	}
	
	// explicit throwing of NPE should work
	try {
	  if (dummy) throw new NullPointerException();
	  COREfail("Failed to throw NPE (7)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
          COREfail("Bad exception thrown - should be NPE (8) "+e);
        }
	
	// invoking a method on null class instance should
	// produce NPE (invokevirtual)
        NPEClass nc = (NPEClass)returnNull();
        try {
          nc.foo();
          COREfail("NPE check missing (9)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (10) "+e);
        }
        
        // writing to a field of null class instance should
        // produce NPE (putfield)
        try {
          nc.npeFieldBoolean = false;
          COREfail("NPE check missing (11)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (12) "+e);
        }
  
        // reading a field of null class instance should
        // produce NPE (getfield)
        try {
          boolean b = nc.npeFieldBoolean;
          COREfail("NPE check missing (13)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (14) "+e);
        }
       
        double[] dArray = null;
        float[] fArray = null;
        char[]  cArray = null;
        int[] iArray = null;
        long[] lArray = null;
        byte[] bArray = null;
        String[] rArray = null;
        
        // reading and writing an array field of null array 
        // should produce NPE
        
        // reading
        try {
          System.out.print(dArray[0]);
          COREfail("NPE check missing (A1)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A1) "+e);
        }

        try {
          System.out.print(fArray[0]);
          COREfail("NPE check missing (A2)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A2) "+e);
        }


        try {
          System.out.print(cArray[0]);
          COREfail("NPE check missing (A3)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A3) "+e);
        }


        try {
          System.out.print(iArray[0]);
          COREfail("NPE check missing (A4)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A4) "+e);
        }


        try {
          System.out.print(rArray[0]);
          COREfail("NPE check missing (A5)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A5) "+e);
        }
      
        try {
          System.out.print(bArray[0]);
          COREfail("NPE check missing (A6)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A6) "+e);
        }
       
        try {
          System.out.print(lArray[0]);
          COREfail("NPE check missing (A7)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A7) "+e);
        }

        // writing

        try {
          dArray[0]=0.1;
          COREfail("NPE check missing (A8)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A8) "+e);
        }

        try {
          fArray[0]=0.1f;
          COREfail("NPE check missing (A9)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A9) "+e);
        }
        
        try {
          cArray[0]='a';
          COREfail("NPE check missing (A10)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A10) "+e);
        }                

        try {
          iArray[0]=1;
          COREfail("NPE check missing (A11)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A11) "+e);
        }

        try {
          lArray[0]=1;
          COREfail("NPE check missing (A12)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A12) "+e);
        }

        try {
          bArray[0]=1;
          COREfail("NPE check missing (A13)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A13) "+e);
        }

        try {
          rArray[0]="Hello";
          COREfail("NPE check missing (A14)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A14) "+e);
        } 

        // trying to read an array length of a null array
        // should result in NPE
        
        try {
          System.out.print(rArray.length);
          COREfail("NPE check missing (A15)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A15) "+e);
        } 

        try {
          System.out.print(lArray.length);
          COREfail("NPE check missing (A16)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (A16) "+e);
        } 
        

        // invoking a method on null class instance shall 
        // result in NPE (invoke virtual, slightly more
        // complex case than before - though unsure if it 
        // makes any difference for the exception handling
        // code)
        NPEClassChild nccReal = (NPEClassChild)returnNull();
        NPEClass ncc = nccReal;
        try {
          ncc.foo();
          COREfail("NPE check missing (15)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (16) "+e);
        }
        
        // invoking a method on null class instance through
        // an interface should result in NPE (invokeinterface)
        NPEInterface nci = nccReal;
        try {
          nci.foo();
          COREfail("NPE check missing (17)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (18) "+e);
        }
	
	// this is intended to test method resolving through
	// interfaces, it should excercise the Miranda workaround
	// for older VM bugs (see IRewriter.java for more info)
	
	NPECMain npecm = new NPECMain();
	NPECBase npecb = npecm;
	NPEInterface npeci = npecb;
	
	
	try {
	  npeci.foo();
        } catch (Exception e) {
          COREfail("Error in method lookup (19) "+e);
        }
        
        // and now the same, but try to invoke it on null class instance,
        // which shall result in NPE
        
        npecm = (NPECMain)returnNull();
        npecb = npecm;
        npeci = npecb;
        
        try {
	  npeci.foo();
          COREfail("NPE check missing (20)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (21) "+e);
        }

        // try to prevent de-virtualization
        // (some of the trick might be not needed with the current
        // optimization code)
        
        Object[] dummyBox = new Object[5];
        dummyBox[0] = new NPEClassChildT();
        dummyBox[1] = new NPEClassChild2T();
        dummyBox[2] = new NPEClassT();
        dummyBox[3] = new NPECMainT();
        dummyBox[4] = new NPECMain2T();
        
        if (!dummy) {
          // never reached
          System.out.print(dummyBox[0]);
          System.out.print(dummyBox[1]);
          System.out.print(dummyBox[2]);
          System.out.print(dummyBox[3]);
          System.out.print(dummyBox[4]);
        }
          

        // invoking a method on null class instance shall 
        // result in NPE (invoke virtual, slightly more
        // complex case than before - though unsure if it 
        // makes any difference for the exception handling
        // code)
        // 
        // this includes tricks to prevent de-virtualization
        //
        
        Object[] box = new Object[1];
        
        // avoid copy propagation
        box[0] = (NPEClassChildT)returnNull();
        NPEClassT ncct = (NPEClassT)box[0];
        
        try {
          ncct.foo();
          COREfail("NPE check missing (30)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (30) "+e);
        }
        
        // invoking a method on null class instance through
        // an interface should result in NPE (invokeinterface)
        NPEInterfaceT ncit = (NPEInterfaceT)box[0];
        try {
          ncit.foo();
          COREfail("NPE check missing (31)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (31) "+e);
        }
	
	// this is intended to test method resolving through
	// interfaces, it should excercise the Miranda workaround
	// for older VM bugs (see IRewriter.java for more info)
	// it does not really test NPEs
	
	box[0] = new NPECMainT();
	NPECBaseT npecbt = (NPECBaseT)box[0];
	NPEInterfaceT npecit = (NPEInterfaceT) npecbt;
	
	
	try {
	  npecit.foo();
        } catch (Exception e) {
          COREfail("Error in method lookup (32) "+e);
        }
        
        // and now the same, but try to invoke it on null class instance,
        // which shall result in NPE
        
        box[0] = (NPECMainT)returnNull();
        box[0] = (NPECBaseT) box[0];
        npecit = (NPEInterfaceT)box[0];
        
        try {
	  npecit.foo();
          COREfail("NPE check missing (33)");
        } catch (NullPointerException npe) {
        } catch (Exception e) {
         COREfail("Bad exception thrown - should be NPE (33) "+e);
        }
        
    }

    public void testSimpleTryCatch() {
	setModule("simpleTryCatch");
	AException ae = new AException();
	try {	    
	    if (true)  /* make compiler happy! */
		throw ae;
	    COREfail("this code should be unreachable!");
	} catch (AException aeII) {
	    COREassert(aeII == ae,
		       "exception caught is not exception thrown!");
	}
    }

    public void testSubtypedCatch() {
	setModule("subtypedTryCatch");
	AException ae = new AException();
	try {	    
	    if (true)  /* make compiler happy! */
		throw ae;
	    COREfail("this code should be unreachable!");
	} catch (Exception aeII) {
	    COREassert(aeII == ae,
		       "exception caught is not exception thrown!");
	}
    }

    public void testMultiCatch() {
	setModule("multiCatch");
	BException be = new BException();
	try {	    
	    if (true)  /* make compiler happy! */
		throw be;
	    else
		if (false) 
		    throw new CException();
		else
		    COREfail("this code should be unreachable!");
	} catch (CException ce) {
	    COREfail("wrong catch clause!");
	} catch (BException beII) {
	    COREassert(beII == be,
		       "exception caught is not exception thrown!");
	}
    }

    public void testCallerCatch() {
	setModule("callerCatch");
	try {	    
	    m();
	    COREfail("this code should be unreachable!");
	} catch (CException ce) {
	    COREfail("wrong catch clause!");
	} catch (AException beII) {
	} catch (Exception e) {
	    COREfail("wrong catch clause!");
	}
    }

    public void testCallerCatchWithNoUnreachable() {
	setModule("callerCatchWithNoUnreachable");
	try {	    
	    m();
	    //  method call is last to test if PC updates are handled correctly
	} catch (CException ce) {
	    COREfail("wrong catch clause!");
	} catch (AException beII) {
	} catch (Exception e) {
	    COREfail("wrong catch clause!");
	}
    }



    public void testNestedTryCatch() {
	setModule("nestedTryCatch");
	try {	    
	    try {
		try {
		    m();
		    COREfail("this code should be unreachable!");
		} catch (CException ce) {
		    COREfail("wrong catch clause!");
		}
	    } catch (CException ce) {
		COREfail("wrong catch clause!");
	    }
	} catch (AException beII) {
	} catch (Exception e) {
	    COREfail("wrong catch clause!");
	}
    }

   
    public void testTrace() {
	try {
	    n();
	} catch (Exception e) {
	    StackTraceElement[] ste = e.getStackTrace();
	    for (int i = 0; i < ste.length; i++) {
		ste[i].toString(); // just test if it crashes ...
	    }
	}
    }

    private void overflow() {
	overflow();
    }

    public void testOverflow() {
	setModule("stack overflow");
	try {
	    overflow();
            COREfail("No StackOverFlowError");
	} catch (StackOverflowError sof) {
	}
    }

    public void testArrayIndex() {
        setModule("array range checks");
	int[] ia = new int[5];
	ia[0] = 1;
	try {
	    ia[5] = 1;
	    COREfail("ArrayIndex check bad - index too big");
	} catch (ArrayIndexOutOfBoundsException a) {
	}
	try {
	    ia[-1] = 1;
	    COREfail("ArrayIndex check bad - negative index");
	} catch (ArrayIndexOutOfBoundsException a) {
	}
	try {
	    ia[6] = 1;
	    COREfail("ArrayIndex check bad - index too big ");
	} catch (ArrayIndexOutOfBoundsException a) {
	}
	Object[] oa = new Object[5];
	try {
	    oa[5] = null;
	    COREfail("ArrayIndex check bad - index too big");
	} catch (ArrayIndexOutOfBoundsException a) {
	}
	try {
	    oa[-1] = null;
	    COREfail("ArrayIndex check bad - negative index");
	} catch (ArrayIndexOutOfBoundsException a) {
	}
	try {
	    oa[6] = null;
	    COREfail("ArrayIndex check bad - index too big");
	} catch (ArrayIndexOutOfBoundsException a) {
	}
    }


    /* ************** helper methods ********************** */

    private void n() throws Exception {
	m();
    }


    private void m() throws Exception {
	if (true)
	    throw new BException();
	COREfail("this code should be unreachable!");
    }
    
    /* ************** helper classes ********************** */

    static class AException extends Exception {
	AException() {}
    }

    static class BException extends AException {
	BException() {}
    }

    static class CException extends Exception {
 	CException() {}
   }


} // end of TestException
