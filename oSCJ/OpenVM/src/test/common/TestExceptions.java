/**
 **/
package test.common;

import ovm.core.execution.NativeInterface;
import s3.util.PragmaNoInline;

/**
 * Test exception handling
 * @author Christian Grothoff
 * @author Hiroshi Yamauchi
 **/
public class TestExceptions
    extends TestBase {

    boolean doOverflow;
    
    public TestExceptions(boolean doOverflow) {
	super("Exceptions");
	this.doOverflow = doOverflow;
    }

    public TestExceptions() {
	this(true);
    }

    public TestExceptions(long disable) {
	this((disable & TestSuite.DISABLE_STACK_OVERFLOW) == 0);
    }

    public void run() {
	testSimpleTryCatch();
	testSubtypedCatch();
	testMultiCatch();
	testCallerCatch();
	testCallerCatchWithNoUnreachable();
	testNestedTryCatch();
	testTrace();
	if (doOverflow)
	    testOverflow();
    }

    public void testSimpleTryCatch() {
	setModule("simpleTryCatch");
	AException ae = new AException();
	try {	    
	    if (true)  /* make compiler happy! */
		throw ae;
	    COREfail("this code should be unreachable!");
	} catch (AException aeII) {
	    check_condition(aeII == ae,
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
	    check_condition(aeII == ae,
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
	    check_condition(beII == be,
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
	} catch (StackOverflowError sof) {
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
