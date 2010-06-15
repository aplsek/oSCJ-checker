package test.runtime;
/**
 * Abstract base class for all tests that rely on the threading and
 * synchronization subsystems being properly initialized. It provides
 * access to all the key objects for debugging purposes.
 * Unlike threading tests that may succeed with sync not configured, all
 * sync tests must be have sync configured.
 * <p><b>NOTE:</b>We rely on userlevel threading and certain other 
 * configuration settings.
 *
 * @author David Holmes
 */
public abstract class TestSyncBase extends TestThreadsBase {
    public TestSyncBase(String desc) {
        super(desc);
    }
}