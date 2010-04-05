/*
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/events/SignalMonitor.java,v 1.8 2007/06/03 01:25:48 baker29 Exp $
 */
package ovm.services.events;

import ovm.services.ServiceInstance;
import ovm.util.OVMError;
/**
 * The signal monitor provides mechanisms for interacting with operating
 * system signals within the OVM. There are three flavours to choose from,
 * two of them similar to mechanisms defined for POSIX signals:
 * <ul>
 * <li>You can install a {@link SignalHandler} for a given signal; or
 * <li>A thread can block until one of a given set of signals occurs
 * </ul>
 * <p>A signal handler is limited to what it can do - just as native signal
 * handlers are, whilst the thread can do anything once it is unblocked.
 * <p>Additionally you can use a {@link SignalWatcher} to register interest
 * in one or more signals and then query how often the signal has occurred, or
 * if desired a thread can block until a registered signal occurs. This differs
 * from the <tt>waitForSignal</tt> methods which only responds to signals that
 * occur whilst the thread is waiting.
 * <p>Examples of using the signal monitor include
 * detecting ctrl-C (<tt>SIGINT</tt>) for shutting down the JVM; 
 * or firing RTSJ async
 * event handlers that have been attached to POSIX signals.
 *
 * <p>The potentially available signals are defined as a set of constants 
 * from zero to
 * the required maximum value, in {@link ovm.core.execution.OVMSignals}.
 * These numeric values do not map to the numeric values of signals as used in
 * the operating system - nor will all signals be available on any given
 * platform.
 * Internally the signal monitor must map the symbolic values for signals into
 * the actual operating system value.
 * Consequently, applications do not need to worry about signal values provided
 * they always use the symbolic constants.
 * By numbering signals in this way it is easy to use an array as a signal-set
 * where each position in the array corresponds to the signal with that same
 * numeric value.
 *
 * @author David Holmes
 * @author Filip Pizlo
 */
public interface SignalMonitor extends ServiceInstance {


    /**
     * Defines a helper object associated with the signal monitor, that can
     * be used to watch for when signals occurs. Such helper objects should
     * be obtained vai the {@link #newSignalWatcher} method.
     * <p>Signal watchers keep track of their own set of signals and are 
     * intended for use by multiple-threads in that any thread can add or
     * remove a watch, whilst generally only a single thread should get the
     * watch counts or wait for a signal.
     */
    interface SignalWatcher {
        /**
         * Add the given signal to the set of signals to watch. Adding the
         * same signal more than once has no affect.
         * @param sig the signal to watch
         * @throws OVMError.IllegalArgument if the signal <tt>sig</tt> can not 
         * be monitored.
         */
        // the throws clause prevents ICK from removing the import needed
        // by the javadocs
        void addWatch(int sig) throws OVMError.IllegalArgument;


        /**
         * Remove the given signal from the set of signals to watch
         * @param sig the signal to remove from the watch set
         * @throws OVMError.IllegalArgument if the signal <tt>sig</tt> is not a
         * valid signal value
         */
        void removeWatch(int sig);


        /**
         * Clears all watched signals.
         */
        void clearAllWatches();


        /**
         * For each signal watched, the number of times that signal has 
         * occurred since it was registered, or since the last query
         * (whichever is the most recent time), is reported. The internal
         * counts for each signal are reset to zero.
         *
         * @return an array of counts. The array is indexed using the
         * values from {@link ovm.core.execution.OVMSignals}. For each
         * registered signal, the value at that index reflects the
         * number of times the signal has occurred since the last
         * query, or since it was registered, if that is a shorter
         * period. The values for all signals not registered with this
         * watcher are -1.
	 */
        int[] getWatchCounts();


        /**
         * Query how many times each signal has occurred, blocking if necessary
         * until at least one signal has occurred.
         *
         * <p>This method is identical to {@link #getWatchCounts} except that
         * if no signal has occurred then the thread will block until at least
         * one signal does occur.
         */
        int[] waitForSignal();        
    }
        

    /**
     * Factory method to return a <tt>SignalWatcher</tt> for this
     * <tt>SignalMonitor</tt>.
     */
    SignalWatcher newSignalWatcher();

    /**
     * Defines a signal handler that will be executed as part of the low-level
     * signal handling code. These signal handlers are restricted in what they
     * can do, due to the low-level at which they execute. An implementation
     * of the <tt>SignalMonitor</tt> must document what those restrictions are.
     * <p><b>Note that it must be valid for a signal handler to remove itself.
     */
    interface SignalHandler {

        /**
         * Executed when the signal to which this handler is attached occurs.
         * @param sig the signal that occurred (to help when one handler
         * serves many signals)
         * @param count the number of times the signal has occurred since it
         * was last recognised by the OVM. Ideally this will always be one.
         */
        void fire(int sig, int count);
    }


    /**
     * Installs the given handler for the given signal.
     * @param handler the handler to install
     * @param sig the signal to attach the handler to
     * @throws OVMError.IllegalArgument if the signal <tt>sig</tt> can not be
     * monitored.
     */
    void addSignalHandler(SignalHandler handler, int sig);

    /**
     * Removes the given handler for the given signal.
     * @param handler the handler to remove
     * @param sig the signal the handler is supposed to be attached to
     * @throws OVMError.IllegalArgument if the signal <tt>sig</tt> is not a
     * valid signal value
     */
    void removeSignalHandler(SignalHandler handler, int sig);


    /**
     * Blocks the current thread until one of the signals specified
     * occurs. Not all signals defined in
     * {@link ovm.core.execution.OVMSignals} exist in a given platform.
     * Further, some signals can not be waited upon because they are either
     * used by the OVM, or are critical process signals (like <tt>SIGKILL</tt>
     * or <tt>SIGSEGV</tt>) that simply can't be waited upon.
     *
     * @param sigVector a set of flags indicating which signals are to be
     * waited upon. The index into the array corresponds to a signal value
     * in {@link ovm.core.execution.OVMSignals}. The size of the array can be 
     * limited to the maximum signal number that is of interest. The entries
     * in the array are modified by the call to this method.
     *
     * @return <tt>true</tt> if the requested signals were valid, and 
     * <tt>false</tt> if any of the requested signals could not be waited upon
     * on this system.
     * When <tt>true</tt> is returned <tt>sigVector</tt> contains the number 
     * of times each signal (that was being waited upon) fired while the
     * thread was waiting.
     * When <tt>false</tt> is returned  <tt>sigVector</tt> is set to all zeros 
     * except for the entry for the signal that caused the error. Processing of
     * signals stops upon the first error.
     *
     * @throws ArrayIndexOutOfBoundsException if <tt>sigMask</tt> is larger
     * than {@link ovm.core.execution.OVMSignals#NSIGNALS}.
     */
    boolean waitForSignal(int[] sigVector);

    /**
     * Blocks the current thread until the signal specified occurs. 
     * Not all signals defined in
     * {@link ovm.core.execution.OVMSignals} exist in a given platform.
     * Further, some signals can not be waited upon because they are either
     * used by the OVM, or are critical process signals (like <tt>SIGKILL</tt>
     * or <tt>SIGSEGV</tt>) that simply can't be waited upon.
     *
     * @param sig The signal to be waited upon (a value from
     * {@link ovm.core.execution.OVMSignals}.
     *
     * @return the number of times the signal occurred whilst the thread was
     * waiting - this should usually be one. If the signal can not be waited 
     * upon then zero is returned.
     */
    int waitForSignal(int sig);


    /**
     * Queries if the specified signal is valid on the current platform, and
     * can be monitored - that is, can be waited upon or have a handler
     * installed for it.
     * @param sig the signal value interpreted as being a constant defined in
     * {@link ovm.core.execution.OVMSignals}.
     *
     * @return <tt>true</tt> if the signal can be monitored and 
     * <tt>false</tt> otherwise.
     */
    boolean canMonitor(int sig);

}
