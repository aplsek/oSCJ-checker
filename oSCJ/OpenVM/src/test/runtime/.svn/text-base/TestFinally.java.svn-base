// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestFinally.java,v 1.2 2004/06/12 23:54:09 jv Exp $

package test.runtime;

import test.common.*;

/**
 *
 * @author Filip Pizlo
 */
public class TestFinally extends TestBase {
    public TestFinally() {
	super("Finally");
    }
    
    public void run() {
	boolean success=false;
	try {
	    success=true;
	} finally {
	    if (!success) {
		COREfail("success flag was set to true but I observed it being false.");
	    }
	}
    }
}

