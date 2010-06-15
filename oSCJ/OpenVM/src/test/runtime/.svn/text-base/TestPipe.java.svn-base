// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestPipe.java,v 1.6 2004/05/28 06:17:08 jv Exp $

package test.runtime;

/**
 *
 * @author Filip Pizlo
 */
public class TestPipe extends TestPipeBase {
    public TestPipe(long disabled) {
        super("Pipe",disabled);
    }
    protected void createPipe(int[] pipe) {
        check_err(LibraryImports.pipe(pipe)==0,
              "Creating a pipe with LibraryImports.pipe()");
    }
}

