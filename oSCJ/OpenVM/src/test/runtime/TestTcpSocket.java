
package test.runtime;
 
/**
 *
 * @author Filip Pizlo
 */
public class TestTcpSocket extends TestPipeBase {
    public TestTcpSocket(long disabled) {
        super("TCP Socket",disabled);
    }
    protected void createPipe(final int[] pipe) {
	TcpSocketUtil.createTcpSocket(this,pipe);
    }
}

