/* VMThread -- VM interface for Thread of executable code
   Copyright (C) 2003, 2004, 2005 Free Software Foundation

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package java.lang;

import javax.realtime.RealtimeThread;

import org.ovmj.java.Opaque;

import edu.purdue.scj.utils.Utils;

/**
 * VM interface for Thread of executable code. Holds VM dependent state. It is
 * deliberately package local and final and should only be accessed by the
 * Thread class.
 * <p>
 * This is the GNU Classpath reference implementation, it should be adapted for
 * a specific VM.
 * <p>
 * The following methods must be implemented:
 * <ul>
 * <li>native void start(long stacksize);
 * <li>native void interrupt();
 * <li>native boolean isInterrupted();
 * <li>native void suspend();
 * <li>native void resume();
 * <li>native void nativeSetPriority(int priority);
 * <li>native void nativeStop(Throwable t);
 * <li>native static Thread currentThread();
 * <li>static native void yield();
 * <li>static native boolean interrupted();
 * </ul>
 * All other methods may be implemented to make Thread handling more efficient
 * or to implement some optional (and sometimes deprecated) behaviour. Default
 * implementations are provided but it is highly recommended to optimize them
 * for a specific VM.
 * 
 * @author Jeroen Frijters (jeroen@frijters.net)
 * @author Dalibor Topic (robilad@kaffe.org)
 */
abstract class VMThreadBase {

    /**
     * Defines a simple VMthread subtype for use as the primordial thread which
     * allows late binding of the Runnable, and which can have its state
     * pre-initialized directly. Note that when we create this we cannot refer
     * to any String literals in the execution path.
     */
    static final class PrimordialVMThread extends VMThread {
        PrimordialVMThread(int priority, boolean daemon) {
            super(priority, daemon);
        }

        void setRunnable(Runnable logic) {
            thread.runnable = logic;
        }

        /**
         * Performs the remainder of the Thread initialization that can only be
         * done after class initialization has been enabled
         */
        void completeInitialization(String name) {
            
//        	 LibraryImports
//             .printString(">>> [VMThread] completing init \n");
        	
        	
        	thread.name = name ;
            thread.group = ThreadGroup.root;
            if (ThreadGroup.root == null) {
                LibraryImports.printString("Root was null :( ");
            }
            ThreadGroup.root.addThread(thread);
            thread.contextClassLoader = ClassLoader.getSystemClassLoader();
            setPriority(thread.priority); // force the vm thread to update
            
            
            
            thread.completeInitialization();
            
//            LibraryImports
//            .printString(">>> [VMThread] completing init OK. \n");
       	
        }
    }

    /**
     * A reference that hooks back to the underlying thread implementation. It
     * is set when the thread is started and cleared as part of thread
     * termination - in both cases by the dispatcher. We don't use this field
     * directly, internally.
     */
    volatile Opaque vmThread;

    /**
     * Set in run() just after checking for a pending Thread.stop() throwable.
     **/
    volatile boolean running;

    /**
     * Set if InterruptedException is pending
     */
    volatile boolean interrupted;

    // setter method so that the RT version of this can elide scope checks
    final void setVMThread(Opaque vmThread) {
        this.vmThread = vmThread;
    }

    // This method is called from the VM. It captures changes to a
    // thread's priority between the call to Thread.start() and calls
    // to Dispatcher.bindVMThread(). It probably is not correct in
    // terms of the java memory model, but it should work with every
    // possible compiler on a cache-coherent architecture.
    //
    // In particular, it should work unless this method and the Thread
    // constructor that sets priority are inlined into the same caller
    // and the field read below gets CSEd. This mehtod won't be
    // inlined unless ovm starts trying to inline
    // ReflectiveMethod.call() calls from the kernel.
    //
    // One thing is certain: calling a synchronized method like
    // Thread.getPriority from uninterruptable code is bad news.
    final int getStartupPriority() {
        return thread.priority;
    }

   

    static final String EXCEPTION_UNCAUGHT_EXCEPTION = "WARNING: Thread - "
            + "Exception occurred handling uncaught exception\n";
    static final String OOME_UNCAUGHT_EXCEPTION = "WARNING: Thread - "
            + "OutOfMemoryError occurred handling uncaught exception\n";
    static final String EXCEPTION_GROUP_REMOVE = "WARNING: Thread - "
            + "Exception removing terminating thread from ThreadGroup\n";
    static final String EXCEPTION_EXCEPTION_TO_STRING = "WARNING: Thread - "
            + "Exceptiontrying to print previous exception\n";
    static final String TERMINATE_EXCEPTION = "ERROR: Thread - "
            + "terminate current thread threw exception!\n";
    static final String OOME_TERMINATION = "FATAL: Thread - "
            + "OutOfMemoryError during final part of termination - "
            + "thread can not terminate properly\n";
    static final String ABORT = "FATAL: Thread - "
            + "unrecoverable internal error - "
            + "calling System.exit to terminate the JVM\n";

    /**
     * The current dispatcher that knows how to perform actual thread operations
     */
    private static JavaDispatcher dispatcher = JavaDispatcher.getInstance();

    /**
     * Helper method to print raw string charaters as bytes directly with no
     * conversion. This is for use in situations where we think any Library call
     * could fail - eg due to memory exhaustion.
     */
    static void rawPrint(String msg) {
        int len = msg.length();
        for (int i = 0; i < len; i++)
            LibraryImports.printCharAsByte(msg.charAt(i));
    }

    /**
     * Support for home-brewed ThreadLocal. See <mail-link> for details on how
     * to remove this stuff.
     **/
    private static int threadNumber;

    /**
     * Support for home-brewed ThreadLocal. See <mail-link> for details on how
     * to remove this stuff.
     **/
    private static synchronized int nextThreadNum() {
        return threadNumber++;
    }

    /**
     * Support for home-brewed ThreadLocal. See <mail-link> for details on how
     * to remove this stuff.
     **/
    final int uniqueID = nextThreadNum();

    /**
     * Private constructor, create VMThreads with the static create method.
     * 
     * @param thread
     *            The Thread object that was just created.
     */
    VMThreadBase(Thread thread) {
        //this.thread = thread;
        //thread.vmThread = (VMThread) this;
    }

    //RealtimeThread rtThread;
   
    /**
     * The Thread object that this VM state belongs to. Used in currentThread()
     * and start(). Note: when this thread dies, this reference is *not* cleared
     */
    volatile RealtimeThread thread;
    
    VMThreadBase(int priority, boolean daemon) {
        

		// LibraryImports
		// .printString(">>> [VM] VMThreadBase - PrimordialThread init\n");
    	
    	//LibraryImports
        //.printString(">>> [VM] VMThreadBase - PrimordialThread init, priority" + priority + "\n");
    	
    	//LibraryImports
        //.printString(">>> \t\t " + this.toString()  + "\n");
    	
    	//thread = new Thread((VMThread) this, null, priority, daemon);
        //rtThread = new RealtimeThread((VMThread) this, null, priority, daemon);
        thread = new RealtimeThread((VMThread) this, null, priority, daemon);
        

		// LibraryImports
		// .printString(">>> [VM] VMThreadBase - PrimordialThread init OK\n");
    }

    void preRun() {
    }

    void postRun() {
    }

    /**
     * This method is the initial Java code that gets executed when a native
     * thread starts. It's job is to coordinate with the rest of the VMThread
     * logic and to start executing user code and afterwards handle clean up.
     * 
     * It is also called from JavaVirtualMachine to begin the normal execution
     * path of the main thread, so it cannot be private.
     */
    void run() {
        Utils.debugPrint("[SCJ] VMThreadBase.run() started");

        // assert: isAlive() && Thread.currentThread() == thread
        try {
            try {
                preRun();
                synchronized (thread) {
                    Throwable t = thread.stillborn;
                    if (t != null) {
                        thread.stillborn = null;
                        throw t;
                    }
                    running = true;
                }
                thread.run();
            } catch (Throwable t) {
                try {
                    if (thread.group != null)
                        thread.group.uncaughtException(thread, t);
                } catch (Throwable t2) {
                    // should be logged somewhere - in case this is OOME we use
                    // the raw I/O system with no allocation or conversions
                    if (t2 instanceof OutOfMemoryError)
                        rawPrint(OOME_UNCAUGHT_EXCEPTION);
                    else
                        rawPrint(EXCEPTION_UNCAUGHT_EXCEPTION);
                }
            }
        } finally {
            postRun();
            // Setting runnable to false is partial protection against stop
            // being called while we're cleaning up. To be safe all code in
            // VMThread be unstoppable.
            synchronized (this) {
                // release the threads waiting to join us
                thread.die(); // isAlive will now return false
                notifyAll();
                // we need to update local priority with real priority
                thread.priority = dispatcher.getThreadPriority(this);
            }

            // NOTE: our scheduling priority can no longer change

            // actually terminate the thread
            try {
                dispatcher.terminateCurrentThread(); // never returns
                throw new InternalError("terminate current thread returned");
            } catch (Throwable t) {
                rawPrint(TERMINATE_EXCEPTION);
                try {
                    System.err.println(t);
                    t.printStackTrace(System.err);
                } finally {
                    rawPrint(ABORT);
                    System.exit(-1);
                }
            }
        }

        Utils.debugPrint("[SCJ] VMThreadBase.run() done");
    }

    static ThreadGroup chooseGroup(Thread child, ThreadGroup group,
            Thread current) {
        SecurityManager sm = SecurityManager.current;
        if (group == null) {
            if (sm != null)
                group = sm.getThreadGroup();
            if (group == null)
                group = current.group;
        }
        return group;
    }

    static int choosePriority(Thread child, Thread parent) {
        return parent.getPriority();
    }

    /**
     * Creates a native Thread. This is called from the start method of Thread.
     * The Thread is started.
     * 
     * @param thread
     *            The newly created Thread object
     * @param stacksize
     *            Indicates the requested stacksize. Normally zero, non-zero
     *            values indicate requested stack size in bytes but it is up to
     *            the specific VM implementation to interpret them and may be
     *            ignored.
     */
    static void create(Thread thread, long stacksize) {
        VMThread vmThread = new VMThread(thread);
        vmThread.start(stacksize);
    }

    /**
     * Gets the name of the thread. Usually this is the name field of the
     * associated Thread object, but some implementation might choose to return
     * the name of the underlying platform thread.
     */
    String getName() {
        return thread.name;
    }

    /**
     * Set the name of the thread. Usually this sets the name field of the
     * associated Thread object, but some implementations might choose to set
     * the name of the underlying platform thread.
     * 
     * @param name
     *            The new name
     */
    void setName(String name) {
        thread.name = name;
    }

    /**
     * Set the thread priority field in the associated Thread object and calls
     * the native method to set the priority of the underlying platform thread.
     * 
     * @param priority
     *            The new priority
     */
    void setPriority(int priority) {
        // the thread is definitely already started but could terminate
        if (!JavaDispatcher.setThreadPriorityStatic(this, priority)) {
            // thread has terminated so return to the !isAlive case
            // assert: !isAlive()
            thread.priority = priority;
        }
    }

    /**
     * Returns the priority. Usually this is the priority field from the
     * associated Thread object, but some implementation might choose to return
     * the priority of the underlying platform thread.
     * 
     * @return this Thread's priority
     */
    int getPriority() {
        return dispatcher.getThreadPriority(this);
    }

    /**
     * Returns true if the thread is a daemon thread. Usually this is the daemon
     * field from the associated Thread object, but some implementation might
     * choose to return the daemon state of the underlying platform thread.
     * 
     * @return whether this is a daemon Thread or not
     */
    boolean isDaemon() {
        return thread.daemon;
    }

    /**
     * Returns the number of stack frames in this Thread. Will only be called
     * when when a previous call to suspend() returned true.
     * 
     * @deprecated unsafe operation
     */
    int countStackFrames() {
        return -1; // No obligation to do anything meaningful
    }

    /**
     * Wait the specified amount of time for the Thread in question to die.
     * 
     * <p>
     * Note that 1,000,000 nanoseconds == 1 millisecond, but most VMs do not
     * offer that fine a grain of timing resolution. Besides, there is no
     * guarantee that this thread can start up immediately when time expires,
     * because some other thread may be active. So don't expect real-time
     * performance.
     * 
     * @param ms
     *            the number of milliseconds to wait, or 0 for forever
     * @param ns
     *            the number of extra nanoseconds to sleep (0-999999)
     * @throws InterruptedException
     *             if the Thread is interrupted; it's <i>interrupted status</i>
     *             will be cleared
     */
    synchronized void join(long millis, int nanos) throws InterruptedException {
        long start = LibraryImports.getCurrentTime();
        long waited = 0;
        if (millis == 0 && nanos == 0) {
            while (!thread.stopped) {
                wait();
            }
        } else {
            while (!thread.stopped) {
                long timeleft = millis * 1000 * 1000 + nanos - waited;
                if (timeleft <= 0) {
                    break;
                }
                wait(timeleft / (1000 * 1000), (int) (timeleft % (1000 * 1000)));
                waited = LibraryImports.getCurrentTime() - start;
            }
        }
    }

    /**
     * Cause this Thread to stop abnormally and throw the specified exception.
     * If you stop a Thread that has not yet started, the stop is ignored
     * (contrary to what the JDK documentation says). <b>WARNING</b>This
     * bypasses Java security, and can throw a checked exception which the call
     * stack is unprepared to handle. Do not abuse this power.
     * 
     * <p>
     * This is inherently unsafe, as it can interrupt synchronized blocks and
     * leave data in bad states.
     * 
     * <p>
     * <b>NOTE</b> stop() should take care not to stop a thread if it is
     * executing code in this class.
     * 
     * @param t
     *            the Throwable to throw when the Thread dies
     * @deprecated unsafe operation, try not to use
     */
    void stop(Throwable t) {
        // Note: we assume that we own the lock on thread
        // (i.e. that Thread.stop() is synchronized)
        if (running)
            dispatcher.stopThread(this, t);
        else
            thread.stillborn = t;
    }

    /**
     * Create a native thread on the underlying platform and start it executing
     * on the run method of this object.
     * 
     * @param stacksize
     *            the requested size of the native thread stack
     */
    void start(long stacksize) {
  Utils.debugPrint("[OVM] VMThreadBase.start()...");
        
        Utils.debugPrint("[OVM] calling dispatcher.bindVMThread...");
        dispatcher.bindVMThread(this); // bind and register thread
        Utils.debugPrint("[OVM] calling dispatcher.bindVMThread... OK");
        
        Utils.debugPrint("[OVM] calling dispatcher.startThread ...");
        dispatcher.startThread(this);
        Utils.debugPrint("[OVM] calling dispatcher.startThread OK");
        
        Utils.debugPrint("[OVM] VMThreadBase.start() done");
    }

    /**
     * Interrupt this thread.
     */
    void interrupt() {
        if (interrupted) {
            // if we are interrupted then we don't have to do anything.
            // If we enter an interruptible method then we check for
            // interrupt on entry. If we are already in an
            // interruptible method then the call that set the interrupt
            // flag did what was needed.
            return;
        } else {
            interrupted = true;
            dispatcher.interruptThread(this);
        }
    }

    /**
     * Determine whether this Thread has been interrupted, but leave the
     * <i>interrupted status</i> alone in the process.
     * 
     * @return whether the Thread has been interrupted
     */
    boolean isInterrupted() {
        return isInterrupted(false);
    }

    boolean isInterrupted(boolean shouldClear) {
        synchronized (thread) {
            boolean interrupted = this.interrupted;
            if (shouldClear)
                this.interrupted = false;
            return interrupted;
        }
    }

    /**
     * Suspend this Thread. It will not come back, ever, unless it is resumed.
     * 
     * OVM does not want to suspend the current thread with its lock held. That
     * would make resume impossible.
     */
    void suspend() {
        dispatcher.suspendThread(this);
    }

    /**
     * Resume this Thread. If the thread is not suspended, this method does
     * nothing.
     */
    void resume() {
        dispatcher.resumeThread(this);
    }

    /**
     * Return the Thread object associated with the currently executing thread.
     * 
     * @return the currently executing Thread
     */
    static Thread currentThread() {
        return dispatcher.getCurrentThread().thread;
    }

    /**
     * Yield to another thread. The Thread will not lose any locks it holds
     * during this time. There are no guarantees which thread will be next to
     * run, and it could even be this one, but most VMs will choose the highest
     * priority thread that has been waiting longest.
     */
    static void yield() {
        dispatcher.yieldCurrentThread();
    }

    /**
     * Suspend the current Thread's execution for the specified amount of time.
     * The Thread will not lose any locks it has during this time. There are no
     * guarantees which thread will be next to run, but most VMs will choose the
     * highest priority thread that has been waiting longest.
     * 
     * <p>
     * Note that 1,000,000 nanoseconds == 1 millisecond, but most VMs do not
     * offer that fine a grain of timing resolution. Besides, there is no
     * guarantee that this thread can start up immediately when time expires,
     * because some other thread may be active. So don't expect real-time
     * performance.
     * 
     * @param ms
     *            the number of milliseconds to sleep.
     * @param ns
     *            the number of extra nanoseconds to sleep (0-999999)
     * @throws InterruptedException
     *             if the Thread is (or was) interrupted; it's <i>interrupted
     *             status</i> will be cleared
     */
    static void sleep(long ms, int ns) throws InterruptedException {
        if (!dispatcher.sleep(ms, ns)) {
            // interrupted while sleeping so clear and throw
            dispatcher.getCurrentThread().interrupted = false;
            throw new InterruptedException();
        }
    }

    /**
     * Determine whether the current Thread has been interrupted, and clear the
     * <i>interrupted status</i> in the process.
     * 
     * @return whether the current Thread has been interrupted
     */
    static boolean interrupted() {
        return currentThread().vmThread.isInterrupted(true);
    }

    /**
     * Checks whether the current thread holds the monitor on a given object.
     * This allows you to do <code>assert Thread.holdsLock(obj)</code>.
     * 
     * @param obj
     *            the object to check
     * @return true if the current thread is currently synchronized on obj
     * @throws NullPointerException
     *             if obj is null
     */
    static boolean holdsLock(Object obj) {
        return dispatcher.holdsLock(obj);
    }
}
