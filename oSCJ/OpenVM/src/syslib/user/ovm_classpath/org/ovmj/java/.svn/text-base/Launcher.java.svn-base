

package org.ovmj.java;


/**
 * The entry point for executing a user-domain execution personality
 * (such as a Java Virtual Machine) for the OVM.
 *
 * <p>The {@link#main} method of this class is the execution entry point
 * for user-domain code.
 *
 * @author Krzysztof Palacz
 * @author David Holmes
 */
public final class Launcher {
 
    /** No instantiation */
    private Launcher() {}

    /**
     * The entry point for execution of the user-domain code. This is
     * invoked by the OVM.
     *
     * <p><b>Important:</b> Note that class initialization is not enabled
     * when we invoke this method. This means that we can not rely on any
     * static initialization until after we have enabled class initialization
     * - which is done either here or in the JVM depending on the config.
     * It also means that it's probably impossible to actually throw any of the
     * exceptions we try to catch (before class initialization) as the creation
     * of the exception will trigger a nested NPE.
     *
     * <p><b>Note:</b> we can't pass in args from the ED as we can't create
     * UD strings until after class initialization is enabled and we can deal
     * with encoding. We keep the typical "main" signature for convenience.
     *
     */
    static void main(String[] notUsed) {
        // no exceptions should escape
        try {
	    LibraryImports.printString("in Launcher.main\n");
	    java.lang.JavaVirtualMachine.init(); 
	    java.lang.JavaVirtualMachine.getInstance().run();
        } catch(Throwable t) {
	    LibraryImports.enableClassInitialization(); // may be necessary -HY

            // If this fails due to exceptions in the String handling then
            // we have a problem. Of course if the problem is in the
            // exception handling then we'll probably never get here anyway.
            // If we're really paranoid we should avoid string concat too.

            // this is deliberately two calls in-case t.toString fails.
            // Exceptions will propagate and cause a panic - which is fine
            // as the user-domain has crashed.
            LibraryImports.printString(
                "LAUNCHER: Uncaught user-domain exception: ");
            LibraryImports.printString(t + "\n");
            // try and do stack trace the simple way
            try {
                t.printStackTrace(System.out);
            }
            catch(Throwable t1) {
                LibraryImports.printString(
                    "\nFailure printing stack trace - trying again\n");
                try {
                    StackTraceElement[] st = t.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            LibraryImports.printString(st[i] + "\n");
                        }
                    }
                }
                catch(Throwable t2) {
                    LibraryImports.printString(
                        "-- Failure printing stack trace\n");
                }
            }
        }
    }


    /** Debug utility */
    public static void printReschedulingState() {
        if (LibraryImports.isReschedulingEnabled())
            LibraryImports.printString("\nRescheduling Enabled\n");
        else
            LibraryImports.printString("\nRescheduling Disabled\n");
    }
}



