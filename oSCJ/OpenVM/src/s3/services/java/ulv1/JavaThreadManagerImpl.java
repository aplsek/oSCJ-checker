
package s3.services.java.ulv1;

import ovm.services.java.JavaUserLevelThreadManager;
import ovm.services.java.JavaOVMThread;
import s3.services.threads.TimedSuspensionUserLevelThreadManagerImpl;
import s3.util.Visitor;
import s3.util.PragmaNoPollcheck;
/**
 * A user-level thread manager for the Java Virtual Machine built directly
 * on the {@link TimedSuspensionUserLevelThreadManagerImpl} class of the
 * OVM threading core.
 *
 */
public class JavaThreadManagerImpl 
    extends TimedSuspensionUserLevelThreadManagerImpl
    implements JavaUserLevelThreadManager
{

    /** 
     * The visitor class used to deal with waking threads.
     * This visitor makes each of the awoken threads ready to run and
     * appropriately sets their state.
     * 
     */
    protected class ThreadWaker 
        extends TimedSuspensionUserLevelThreadManagerImpl.ThreadWaker {

            // Note: rescheduling is disabled when this is called
            public void visit(Object thread) throws PragmaNoPollcheck {
                super.visit(thread);
                JavaOVMThread jthread = (JavaOVMThread) thread;
                jthread.setState(JavaOVMThread.READY);
            }
        }

    /**
     * Factory for getting the visitor instance
     */
    protected Visitor getVisitor() {
        return new ThreadWaker();
    }


}
        








