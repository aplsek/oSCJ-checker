package test.runtime;

import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.threads.ThreadManager;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.threads.UserLevelThreadManager;
import test.common.TestBase;
/**
 * Abstract base class for all tests that rely on the threading and
 * synchronization subsystems being properly initialized. It provides
 * access to all the key objects for debugging purposes.
 * <p>Threading tests may not require synchronization so absence of sync
 * is only a warning. If you must have sync then extend {@link TestSyncBase}.
 * Note that threading tests that utilise synchronization but which don't
 * try and explore its innards can extend this class - of course if sync is
 * not configured then you probably won't pass your own tests.
 * <p><b>NOTE:</b>We rely on userlevel threading and certain other 
 * configuration settings.
 *
 * @author David Holmes
 *
 */
public abstract class TestThreadsBase extends TestBase {

    /** Convenience method for assertions */
    protected void A(boolean condition, String msg) {
        check_condition(condition, msg);
    }

    /** Current thread manager */
    protected UserLevelThreadManager threadMan;

    /** Current dispatcher */
    protected OVMDispatcher dispatcher;
    
    /** Current monitor mapper */
//  protected MonitorMapper mapper;

    /**
     * Initialize the threading system, if not already done so, and
     * set up the references to the key objects.
     */
    protected void init() {
        ThreadServicesFactory tsf = (ThreadServicesFactory) 
            ThreadServiceConfigurator.config.getServiceFactory(ThreadServicesFactory.name);
        ThreadManager tm = tsf.getThreadManager();
        if (tm == null) {
            COREfail("Configuration error: no thread manager defined");
        }
        if ( !(tm instanceof UserLevelThreadManager)) {
            COREfail("Configuration error: need userlevel thread manager");
        }
        threadMan = (UserLevelThreadManager) tm;

        ThreadDispatchServicesFactory tdsf = (ThreadDispatchServicesFactory) 
            ThreadServiceConfigurator.config.getServiceFactory(ThreadDispatchServicesFactory.name);
        dispatcher = tdsf.getThreadDispatcher();
        if (dispatcher == null) {
            COREfail("Configuration error: no dispatcher defined");
        }

        if (threadMan.getCurrentThread() == null) {
            COREfail("Configuration error: threading not initialized");
        }
        check_condition(threadMan.getCurrentThread() != null);

        // no sync is not fatal for thread tests but it is for sync tests.
        // so TestSyncBase will fail in that case
        MonitorServicesFactory _ = (MonitorServicesFactory) 
            ThreadServiceConfigurator.config.getServiceFactory(MonitorServicesFactory.name);
        if (false) d(""+_);// get Eclipse to think we need _  
    }

    TestThreadsBase(String description) {
        super(description);
    }

}
        

