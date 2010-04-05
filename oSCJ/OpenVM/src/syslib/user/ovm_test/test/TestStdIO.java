// $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_test/test/TestStdIO.java,v 1.3 2004/06/21 02:04:36 jv Exp $

package test;

/**
 *
 * @author Filip Pizlo
 */
public class TestStdIO extends TestBase {
    boolean doThrow;
    
    public TestStdIO(Harness domain, long disabled) {
	super("StdIO", domain);
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }
    
    public void run() {
        System.out.println(" io works! ");
    }

}

