/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_test/test/TestSynchronization.java,v 1.4 2004/10/04 03:09:28 dholmes Exp $
 */
package test;

/**
 * Provides some basic test of monitor entry and exit in 
 * uncontended cases. We can't do contention tests in a generic way because
 * we don't know what type of thread to create.
 * Also we don't know what type of thread is executing at the VM level so
 * we can't do any type of direct tests for ownership. Everything defers to
 * the kernel via the LibraryImports methods.
 * <p><b>Note:</b>We don't test static synchronized methods as that relies on
 * a JVM that may not be present when this test is run.
 * A special JVM version of this test can do that.
 *
 * @author David Holmes
 */
public class TestSynchronization extends TestBase {

    boolean doThrow;

    public TestSynchronization(Harness domain, long disabled) {
        super("Synchronized block and methods (uncontended)", domain);
	doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }

    protected TestSynchronization(String description, Harness domain) {
        super(description, domain);
    }


    public void run() {
        nocontentiontest_block();
	if (doThrow)
	    nocontentiontest_block_with_exception();
        recursive_nocontentiontest_block();
	if (doThrow)
	    recursive_nocontentiontest_block_with_exception();
        nocontentiontest_method(1);
	if (doThrow)
	    nocontentiontest_method_with_exception(1);
        nocontentiontest_method(5);
	if (doThrow)
	    nocontentiontest_method_with_exception(5);
        if (doThrow)
            testNull();
    }


    // utility functions for grouping assertions

    protected void checkCurrentIsOwner(Object mon) {
        COREassert(LibraryImports.currentThreadOwnsMonitor(mon), 
          "current thread not owner");
    }

    protected void checkCurrentNotOwner(Object mon) {
        COREassert(!LibraryImports.currentThreadOwnsMonitor(mon), 
          "current thread owner");
    }

    protected void checkUnowned(Object mon) {
        COREassert(LibraryImports.isUnownedMonitor(mon), 
          "monitor is owned");
    }
    
    protected void checkEntryCount(Object mon, int entryCount) {
        int count = LibraryImports.getEntryCountForMonitor(mon);
        COREassert(count == entryCount, 
          "entry count " + count + " != " + entryCount);
    }        

    
    protected void checkIdleMonitor(Object mon) {
        checkUnowned(mon);
        checkEntryCount(mon, 0);
    }

    protected void checkOwnedUncontended(Object mon, int entryCount) {
        checkCurrentIsOwner(mon);
        checkEntryCount(mon, entryCount);
    }


    Object nullField = new Object();
    void setNullField() {
        nullField = null;
    }

    Object[] getArray(boolean returnNull) {
        if (returnNull) return null;
        return new Object[0];
    }

    Object getObject(boolean returnNull) {
        if (returnNull) return null;
        return new Object();
    }

    void testNull() {
        setModule("synchronized(null) test");
        setNullField();
        try {
            synchronized(nullField) {
                COREassert(false, "sync on null field !");
            }
        }
        catch (NullPointerException npe) {
        }
        try {
            synchronized(getObject(true)) {
                COREassert(false, "sync on null method return!");
            }
        }
        catch (NullPointerException npe) {
        }
        try {
            synchronized(getArray(true)) {
                COREassert(false, "sync on null array!");
            }
        }
        catch (NullPointerException npe) {
        }
    }
        

    void nocontentiontest_block() {
        setModule("no contention test - sync block");

        Object lock = new Object();
        synchronized(lock) {
            checkOwnedUncontended(lock, 1);
        }
        checkIdleMonitor(lock);
    }

    void nocontentiontest_block_with_exception() {
        setModule("no contention test - sync block - with exception");

        Object lock = new Object();
        try {
            synchronized(lock) {
                checkOwnedUncontended(lock, 1);
                throw new Error("Oops");
            }
        }
        catch(Error e) {
            checkIdleMonitor(lock);
        }
    }

    void recursive_nocontentiontest_block() {
        setModule("recursive - no contention test - sync block");

        Object lock = new Object();
        synchronized(lock) {
            checkOwnedUncontended(lock, 1);
            synchronized(lock) {
                checkOwnedUncontended(lock, 2);
                synchronized(lock) {
                    checkOwnedUncontended(lock, 3);
                    synchronized(lock) {
                        checkOwnedUncontended(lock, 4);
                    }
                    checkOwnedUncontended(lock, 3);
                }
                checkOwnedUncontended(lock, 2);
            }
            checkOwnedUncontended(lock, 1);
        }
        checkIdleMonitor(lock);
    }

    void recursive_nocontentiontest_block_with_exception() {
        setModule("recursive - no contention test - sync block - with exception");

        Object lock = new Object();
        try {
            synchronized(lock) {
                checkOwnedUncontended(lock, 1);
                try {
                    synchronized(lock) {
                        checkOwnedUncontended(lock, 2);
                        try {
                            synchronized(lock) {
                                checkOwnedUncontended(lock, 3);
                                try {
                                    synchronized(lock) {
                                        checkOwnedUncontended(lock, 4);
                                        throw new Error("oops");
                                    }
                                }
                                catch(Error e) {
                                    checkOwnedUncontended(lock, 3);
                                    throw e;
                                }
                            }
                        }
                        catch(Error e) {
                            checkOwnedUncontended(lock, 2);
                            throw e;
                        }
                    }
                }
                catch(Error e) {
                    checkOwnedUncontended(lock, 1);
                    throw e;
                }
            }
        }
        catch(Error e) {
            checkIdleMonitor(lock);
        }
    }

    synchronized void syncMethod(int depth, int max) {
        checkOwnedUncontended(this, depth);
        if (depth == max) return;
        else { 
            syncMethod(depth+1, max);
            checkOwnedUncontended(this, depth);
        }
    }

    synchronized void syncMethodWithThrow(int depth, int max) {
        checkOwnedUncontended(this, depth);
        if (depth == max) throw new Error("oops");
        else { 
            try {
                syncMethodWithThrow(depth+1, max);
            }
            catch(Error e) {
                checkOwnedUncontended(this, depth);
                throw e;
            }
        }
    }

    // with methods we can use the same structure for recursive and 
    // non-recursive usage

    void nocontentiontest_method(int recursionLevel) {
        setModule("no contention test - sync method - recursion level " + recursionLevel);
        
        syncMethod(1, recursionLevel);
        checkIdleMonitor(this);
    }

    void nocontentiontest_method_with_exception(int recursionLevel) {
        setModule("no contention test - sync method - with exception - recursion level " + recursionLevel);

        try {
            syncMethodWithThrow(1, recursionLevel);
        }
        catch(Error e) {
            checkIdleMonitor(this);
        }
    }
}





