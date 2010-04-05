// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestSocketpair.java,v 1.6 2004/05/28 06:17:09 jv Exp $

package test.runtime;

import ovm.core.execution.NativeConstants;

/**
 *
 * @author Filip Pizlo
 */
public class TestSocketpair extends TestPipeBase {
    public TestSocketpair(long disabled) {
        super("Socketpair",disabled);
    }
    protected void createPipe(int[] pipe) {
        check_err(LibraryImports.socketpair(NativeConstants.AF_UNIX,
                                        NativeConstants.SOCK_STREAM,
                                        0,
                                        pipe)==0,
              "Creating a socketpair with LibraryImports.socketpair()");
    }
}

