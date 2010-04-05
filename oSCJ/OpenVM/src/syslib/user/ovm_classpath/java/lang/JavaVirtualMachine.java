package java.lang;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import edu.purdue.scj.utils.Utils;
import gnu.classpath.SystemProperties;
import org.ovmj.java.Opaque;
import org.ovmj.java.OVMSignals;

/**
 * Represents the Java Virtual Machine (JVM). This class defines the life cycle
 * management of a JVM. It provides for the start-up and initialisation of the
 * JVM, and provides the means for terminating a JVM.
 * <p>
 * This class utilise the underlying OVM kernel services to actually provide the
 * necessary start-up and shut-down functionality.
 * <p>
 * This interface provides support for the JVM shutdown hook architecture as
 * defined in JDK 1.3. The VM terminates when either all user-level threads have
 * exited, the {@link #exit} or {@link #halt} methods are invoked or the
 * underlying OVM is terminated (presumably due to an external signal). Except
 * in the case of {@link #halt} the shutdown sequence causes all registered
 * shutdown hook threads to execute to completion. As per the JDK if any
 * shutdown hook threads fail to terminate the JVM may fail to terminate. An
 * invocation of {@link #halt} does not cause shutdown hook threads to execute,
 * but abruptly terminates the Java virtual machine.
 * 
 * <p>
 * It is expected that an OVM thread created for this purpose, initialises the
 * Java Virtual Machine by executing it's {@link #run} method and so becomes the
 * primordial JVM thread. That thread is then free to do as it pleases to ensure
 * the Java application is executed correctly. The termination of the Java
 * Virtual Machine does not imply the termination of the OVM itself. This allows
 * for the implementation of Isolates between the OVM and the Java Virtual
 * Machine.
 * 
 * <h3>Initialization</h3>
 * <p>
 * The JVM is notionally a singleton, but initialization of that singleton can't
 * take place until class initialization is enabled, which can't take place
 * until JVM initialization has reached a particular state. To deal with this
 * the JVM is initialized through the static {@link #init()} method. Once
 * initialization reaches the point where class initialization is enabled then
 * static initialization of the singleton instance will occur. The singleton JVM
 * instance can then use any static data that was pre-initialized by the
 * <tt>init</tt> method.
 * 
 * <h3>Implementation Notes</h3>
 * <p>
 * Both the {@link #exit} and {@link #halt} methods are defined never to return.
 * We must ensure that we can still cleanup any threads that invoke these
 * methods, even if used incorrectly. For example, the JDK allows the VM to hang
 * on shutdown if a shutdown hook invokes <code>exit</code>. This typically
 * occurs because an attempt is made to <code>join</code> all shutdown hooks,
 * but the hook that calls <code>exit</code> won't terminate until after the
 * thread waiting for it to terminate releases a lock. While this is okay in the
 * JDK up to 1.4, it does not seem good enough for a world that includes
 * Isolates.
 * <p>
 * Relatedly, in the JDK <code>halt</code> can simply terminate the process to
 * halt the virtual machine, but with Isolates this is not possible and we must
 * do a lot more work to cleanly terminate the VM. This means that we need to
 * deal with active user-threads when shutdown occurs. For a user-level thread
 * implementation this should not be too difficult (remove them from the ready
 * queue) but for native threads it is akin to setting an asynchronous
 * termination.
 * 
 * <h3>To-Do</h3>
 * <ul>
 * <li>Actually do finalization
 * <li>Ensure clean termination
 * </ul>
 * 
 * <p>
 * Fix me: currently we have to make things public so the launcher can access
 * them.
 * 
 * @see java.lang.Runtime#exit
 * @see java.lang.Runtime#halt
 * @see java.lang.Runtime#addShutdownHook
 * @see java.lang.Runtime#removeShutdownHook
 * 
 * @author David Holmes
 */


public class JavaVirtualMachine {

	/*
	 * This is the entry point for the JVM at which time class initialization is
	 * disabled. Consequently this class can not have any static initialization
	 * requirements. Instead, all initialization is placed in the init method
	 * that will enable class initialization once it has initialized threading
	 * etc.
	 */

	// This is the thread object that becomes the "current thread"
	static VMThread.PrimordialVMThread main;

	/**
	 * Arguments passed to the JVM. This doesn't include application arguments,
	 * just those args used to configure the JVM itself.
	 */
	static String[] jvmArgs;

	/**
	 * The name of the jar file to exeucute
	 **/
	static String mainJar;
	/**
	 * The name of the application class as specified in the arguments extacted
	 * by <tt>init</tt>
	 */
	static String mainClassName;

	/**
	 * The arguments to be passed to the application classes main method
	 */
	static String[] appArgs;

	/**
	 * The final exit status of the VM. This is the value passed to a direct
	 * call to <tt>exit</tt> or <tt>halt</tt>, or else a value determined by the
	 * VM depending on why termination is occurring. We use a global variable
	 * for simplicity as we'd need to use one anyway to communicate with the
	 * shutdown thread used by the RT-JVM.
	 */
	static volatile int exitStatus = 0;

	/**
	 * Performs initialization of the <tt>JavaVirtualMachine</tt> instance at a
	 * time when class initialization is disabled. This method can not rely on
	 * any use of static information in other classes until class initialization
	 * has been enabled. The main objective of this method is to bind the
	 * primordial thread so that any thread related actions in class
	 * initializers will execute correctly.
	 * <p>
	 * Once class initialization is enabled we complete the initialization of
	 * the primordial thread, read in all the command-line parameters, process
	 * the JVM parameters appropriately, and setup the application parameters.
	 * 
	 * <p>
	 * <b>IMPORTANT:</b> No String literals can be accessed before class
	 * initialization has been enabled. Any attempt to do so will result in a VM
	 * crash due to a NullPointerException in String.intern that itself causes a
	 * NPE in Throwable.<clinit> and which then causes a panic. If we want to
	 * debug we'll have to use byte[].
	 * 
	 * <p>
	 * <b>Note:</b> we can't pass args as we can't create Strings until after
	 * class initialization is enabled.
	 */
	public static void init() {
		
		//LibraryImports
       // .printString("[DBG] VM.init\n");
		
		// prevent anyone from calling this after the launcher does
		if (main != null) {
			// this is safe because it's not called first time through
			throw new IllegalAccessError("This isn't for public use");
		}

		//LibraryImports
        //.printString("[DBG] init SCJ VM.. getting main \n");
		
		// this only partially initializes the primordial thread
		// object - the rest is done after class initialization
		// is enabled below. Note we can't pass the thread name yet.
		main = new VMThread.PrimordialVMThread(Thread.NORM_PRIORITY, false);

		//LibraryImports
        //.printString("[DBG] init SCJ VM.. main returned\n");
		
		//LibraryImports
        //.printString("[DBG] init SCJ VM.. binding with underlining thread\n");
		
		// bind the underlying VM thread to our current thread
		main.vmThread = LibraryImports.getCurrentVMThread();
		if (main.vmThread == null) {
			// this will crash but so what
			throw new Error("currentVMThread returned null\n");
		}
		
		//LibraryImports
        //.printString("[DBG] init SCJ VM.. main.vmThread set\n");
		
		
		//LibraryImports
        //.printString("[DBG] init SCJ VM.. bind primordial .....\n");
		
		LibraryImports.bindPrimordialJavaThread(main);

		//LibraryImports
       // .printString("[DBG] init SCJ VM.. bind primordial OK\n");
		
		
		
		
		//LibraryImports
        //.printString("[DBG] init SCJ VM.. enable class init.\n");
		
		// now enable class initialization
		LibraryImports.enableClassInitialization();

		
		//LibraryImports
       // .printString("[DBG] init SCJ VM.. class init OK\n");
		
		// Make sure to bind the internal boot classloader to the
		// corresponding Type.Context.
		// ClassLoader.getSystemClassLoader();

		// And force
		// SystemProperties.getProperty("file.encoding");
		
		LibraryImports
        .printString("\n\n[DBG] init SCJ VM.. system in...sOK\n");
		
		
		// Now force System to initialize first
		Object nil = System.in;
		
		
		//LibraryImports
        //.printString("[DBG] init SCJ VM.. main.complete... \n");
		
		// now it is safe to do arbitrary synchronization so we can
		// complete the initialization of the primordial thread:
		main.completeInitialization("main");

		//LibraryImports
        //.printString("[DBG] init SCJ VM.. main complete OK\n");
		
		// Force System.class to be initialized. This calls
		// processJVMArgs as a side effect, but it should have already
		// happened in main.completeInitialization().
		System.getProperty("java.home");
		// Avoid assertions inside the initialization sequence
		// assert(mainClassName != null);
		if (mainClassName == null)
			throw new Error("arguments not parsed!");

		// install the actual JVM instance now. This class would eventually
		// get statically initialized but we don't want to take any chances
		// with the timing because the RT JVM will overwrite this setting
		// when it has init() invoked.
		
		//LibraryImports
        //.printString("[DBG] init SCJ VM.. init new VM\n");
		
		instance = new JavaVirtualMachine();
		
		
		//LibraryImports
       // .printString("[DBG] init SCJ VM.. init END.\n");
	}

	static public void processJVMArgs() {
		/*
		 * The argument list is of the form: <user-domain and/or JVM args
		 * starting with - > <application-class-name> <application args>
		 * 
		 * The user-domain should already have acted upon any args it is
		 * interested in. We only look for JVM args that we are interested in
		 * and pass everything else on to the application.
		 * 
		 * <application-class-name> may be replaced by --, in which case the
		 * default main class (first to appear in gen-ovm's -main argument) will
		 * be run.
		 * 
		 * Furthermore <application-args> is emtpy, <application-class-name> can
		 * be omitted entirely, and the default main class will be run.
		 */

		String[] args = LibraryImports.getCommandlineArgumentStringArray();

		int i;
		for (i = 0; i < args.length && args[i].charAt(0) == '-'; i++) {
			String arg = args[i];
			if (arg.charAt(1) == 'D') {
				int equalIndex = arg.indexOf('=');
				if (equalIndex != -1) {
					if (equalIndex > 2) {
						setprop(arg.substring(2, equalIndex), arg.substring(
								equalIndex + 1, arg.length()));
					} else {
						usage("empty property name: " + arg);
					}
				} else { // -Dxxx ?
					setprop(arg.substring(2, arg.length()), "");
				}
			} else if (arg.equals("-classpath") || arg.equals("-cp")) {
				if (i + 1 == args.length)
					usage("classpath not specified");
				setprop("java.class.path", args[++i]);
			} else if (false && arg.equals("-jar")) {
				if (i + 1 == args.length)
					usage("jar file not specified");
				mainJar = args[++i];
				break;
			} else if (arg.equals("-help") || arg.equals("-?")) {
				usage(null);
			} else if (arg.equals("-timing")) {
				doTiming = true;
			} else if (arg.equals("--")) {
				// Use default main class
				break;
			} else
				usage("unrecognized option: " + args[i]);
		}

		if (mainJar == null) {
			if (i == args.length || args[i].equals("--")) {
				mainClassName = LibraryImports.defaultMainClass();
				if (mainClassName == null)
					usage("no main class specified");
				if (i < args.length)
					i++;
			} else {
				mainClassName = args[i++];
			}
		}

		appArgs = new String[args.length - i];
		for (int j = 0; j < appArgs.length; j++)
			appArgs[j] = args[i + j];
	}

	static void setprop(String key, String val) {
		java.util.Properties p = System.getProperties();

		// System properties defined by the VM cannot be
		// overriden on the command line. In particular, the
		// VM will define java.class.path if it was specified
		// at build time. And, if the classpath was specified
		// at build time, we cannot change it at runtime.
		if (p.getProperty(key) != null && LibraryImports.isVMProperty(key))
			usage("can't redefined VM property " + key);

		// This is a bit of a hack. Use of System properties by
		// no-heap threads in the RTSJ is very fragile unless
		// we force all allocation into immortal for all setting of
		// the properties. We don't go that far but do ensure all
		// command-line properties are immortal - that means we intern
		// the strings and then change memory area to do the set
		key = key.intern();
		val = val.intern();
		Opaque current = LibraryImports.setCurrentArea(LibraryImports
				.getImmortalArea());
		try {
			p.setProperty(key, val);
		} finally {
			LibraryImports.setCurrentArea(current);
		}
	}

	public static void printString(String s) {
		LibraryImports.printString(s);
	}

	static void usage(String error) {
		if (error != null)
			LibraryImports.printString("error: " + error + "\n");
		LibraryImports
				.printString("usage: ovm [-options] class [args...]\n"
						+
						// "   or: ovm [-options] -jar jarfile [args...]\n" +
						"   or: ovm [-options] -- [args...]\n"
						+ "   or: ovm [-options]\n"
						+ " (the latter two variants run the main class provided to gen-ovm)\n"
						+ "\n"
						+ "where options include but are not limited to:\n"
						+ "  -D<name>=<value> set a system property\n"
						+ "  -cp          <class search path>\n"
						+ "  -classpath   <class search path>\n"
						+ "  -verbose:gc  enable verbose output\n"
						+ "  -diagnose:gc enable diagnostic checks\n"
						+ "  -timing      measure time spent in application\n"
						+ "  -dumpStacks  dump all thread stacks on SIGUSR1\n"
						+ "  -disable-image-barrier don't use SIGSEGV/SIGBUS handlers in GC\n"
						+ "  -?, -help     print this message\n"
						+ "See the info documentation for a more complete list of options.\n");
		System.exit(error == null ? 0 : 1);
	}

	/**
	 * The singleton instance of this class. This instance may be replaced by a
	 * subclass instance.
	 */
	static JavaVirtualMachine instance;

	/**
	 * Returns the singleton instance of this class
	 * 
	 * @return the singleton instance of this class
	 * 
	 */
	public static JavaVirtualMachine getInstance() {
		return instance;
	}

	/**
	 * Flag indicating whether shutdown sequence has commenced Only read or
	 * write when synchronized on <tt>this</tt>.
	 */
	protected boolean shutdownInitiated = false;

	/**
	 * Object that is locked during shutdown so that only one thread can perform
	 * the shutdown.
	 */
	private final Object shutdownLock = new Object();

	/** The number of active user threads */
	private int userThreads = 0;

	/** The number of active daemon threads */
	private int daemonThreads = 0;

	/** Flag indicating whether finalizers are to be run on exit */
	private boolean runFinalizersOnExit = false;

	/**
	 * Flag indicating if finalizers are running as part of shutdown. This
	 * causes {@link #exit} to behave differently.
	 */
	private volatile boolean runningFinalizers = false;

	/** Direct construction of a JVM is not allowed */
	JavaVirtualMachine() {
	}

	private static boolean doTiming = false;
	private long startTime = 0;

	// invoked by the OVM runtime to start the JVM
	public void run() {
		Utils.debugPrint("[SCJ] JavaVirtualMachine.run() started");

		// prevent anyone from calling this after the launcher does
		if (false) {
			throw new IllegalAccessError("This isn't for public use");
		}

		// This is the code for the main thread
		Runnable mainCode = new Runnable() {
			// NOTE: Any exceptions that propagate out of run will be
			// caught by the Thread code and dealt with correctly.
			public void run() {
				Utils
						.debugPrint("[SCJ] VMThread.PrimordialVMThread<mainCode>.run() started");

				// initialize all VM internal threads - this is overridden
				// by subclasses as needed. We need to do this first
				// to ensure any errors below can be dealt with eg. if
				// we initiate shutdown due to an exception.

				// initializeVMThreads();

				if (mainClassName == null) {
					System.err
							.println("Error: No class specified for OVM to execute");
					System.exit(-1);
				}

				ClassLoader scl = ClassLoader.StaticData.systemClassLoader;
				Class mainClass = null;
				try {
					mainClass = scl.loadClass(mainClassName, true);
				} catch (ClassNotFoundException ex) {
					System.err.println("Exception in thread "
							+ Thread.currentThread().getName() + ": " + ex
							+ " for " + mainClassName);
					System.exit(-1);
				}

				Method main = null;
				try {
					main = mainClass.getDeclaredMethod("main",
							new Class[] { String[].class });
				} catch (NoSuchMethodException e) {
					System.err.println("Exception in thread "
							+ Thread.currentThread().getName() + ": " + e);
					System.exit(-1);
				}

				// check main is public static void
				int mods = main.getModifiers();
				if (!(Modifier.isStatic(mods) && Modifier.isPublic(mods) && (main
						.getReturnType() == void.class))) {
					System.err
							.println("No such method: public static void main(String[] args)");
					System.exit(-1);
				}

				// intantiate Safelet reflectively..
				// invoke safelet.star0();

				try {
					try {
						if (doTiming)
							startTime = LibraryImports.getCurrentTime();

						main.invoke(null, new Object[] { appArgs });

					} catch (InvocationTargetException ex) {
						throw ex.getCause();
					}
				} catch (Throwable t) {
					System.err.println("Exception in thread "
							+ Thread.currentThread().getName() + ": " + t);
					t.printStackTrace();
					System.exit(-1);
				}

				Utils
						.debugPrint("[SCJ] VMThread.PrimordialVMThread<mainCode>.run() done");
			}
		};

		try {
			main.setRunnable(mainCode);

			// add main thread to the JVM
			addThread(main);

			// now we emulate the execution of the main thread by invoking
			// runThread directly
			try {
				main.run(); // should never return, or throw exception
			} finally {
				// something has gone very wrong
				LibraryImports
						.printString("FATAL Error: main.runThread returned\n");
				LibraryImports.halt(-1);
			}
		} catch (Throwable t) {
			// something went badly wrong and we don't know what we can use
			// safely
			LibraryImports.printString("FATAL Error during JVM.run()\n");
			try {
				LibraryImports.printString(t.toString());
				LibraryImports.printString("\n");
			} catch (Throwable t2) {
				LibraryImports.printString("Unable to print exception info\n");
			}
		} finally {
			LibraryImports.halt(-1);
		}

		Utils.debugPrint("[SCJ] JavaVirtualMachine.run() done");
	}

	protected int getSystemPriority_() {
		return Thread.MAX_PRIORITY + 1;
	}

	private void highPriority(int priOffset, String name, Thread t) {
		t.setName(name);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Called by the dispatcher to register an about-to-be started thread with
	 * the JVM.
	 * <p>
	 * Note that this can be called while shutdown is in progress.
	 * 
	 * @param t
	 *            the thread that is being started
	 */
	synchronized void addThread(VMThreadBase t) {
		// simply update the thread counts. We could add to a global thread
		// list if needed
		if (t.isDaemon()) {
			daemonThreads++;
		} else {
			userThreads++;
		}
	}

	/**
	 * Called by the dispatcher to unregister a terminating thread. If this
	 * thread is the last non-daemon thread to be removed, and the shutdown
	 * sequence is not in progress, then the shutdown sequence is initiated and
	 * this method never returns.
	 * <p>
	 * Note that shutdown hook threads will always invoke this when shutdown has
	 * been initiated.
	 * 
	 * @param t
	 *            the thread that is terminating
	 */
	void removeThread(VMThreadBase t) {
		synchronized (this) {
			if (t.isDaemon()) { // simple case
				daemonThreads--;
				return;
			} else {
				userThreads--;
				if (userThreads > 0 || shutdownInitiated) {
					return;
				} else {
					shutdownInitiated = true;
				}
			}
		}

		performShutdown();
	}

	/**
	 * Used to specify whether the JVM should run the finalizers for all
	 * finalizable objects during the shutdown sequence. If shutdown is already
	 * in progress then the setting is left unchanged.
	 * 
	 * @param enabled
	 *            if <tt>true</tt> then finalizers will be executed, otherwise
	 *            they won't.
	 * 
	 * @see Runtime#runFinalizersOnExit
	 */
	synchronized void runFinalizersOnExit(boolean enabled) {
		// don't let it change once shutdown has commenced
		if (!shutdownInitiated) {
			runFinalizersOnExit = enabled;
		}
	}

	/**
	 * Performs the shutdown sequence and then halts the JVM. The
	 * <tt>shutdownInitiated</tt> flag should already have been set by the
	 * caller. The first thread to gain the shutdown-lock will perform the
	 * shutdown. Any other thread will hang trying to get a lock that will never
	 * be released.
	 */
	void performShutdown() {
		synchronized (shutdownLock) {
			try {
				long time;

				time = LibraryImports.getCurrentTime() - startTime;
				LibraryImports.printString("OVM shutdown initiated by ");
				LibraryImports.printString(Thread.currentThread().getName());
				LibraryImports.printString("\n");
				shutdown();
				if (startTime != 0) {
					LibraryImports.printString("\n\n\n");

					LibraryImports
							.printString("Nanosecs from the Epoch right before the beginning of main(): ");
					LibraryImports.printLong(startTime);
					LibraryImports.printString("\n\n");

					LibraryImports
							.printString("Time used by the main method (excluding finalization and shutdown hooks): ");
					LibraryImports.printLong((time) / 60000000000L);
					LibraryImports.printString("m");
					LibraryImports
							.printLong((time % 60000000000L) / 1000000000L);
					LibraryImports.printString(".");
					LibraryImports
							.printLong(((time % 60000000000L) / 1000000L) % 1000);
					LibraryImports.printString("s\n(");
					LibraryImports.printLong(time);
					LibraryImports.printString("ns)\n");
				}
			} finally {
				halt(exitStatus);
			}
		}
	}

	/**
	 * The actual implementation of the shutdown sequence.
	 * <p>
	 * In phase 1 all hooks are started in sequence. If any hook fails to
	 * terminate (eg. if it invokes exit) then phase 1 never terminates and
	 * shutdown never completes and {@link #halt} must be invoked to cause
	 * termination. This is as designed.
	 * <p>
	 * In phase 2 finalizers are run if we have been set to run finalizers on
	 * exit. Unlike the JDK we always run finalizers even if exit is called with
	 * a non-zero status. If finalizers are being used for resource cleanup then
	 * that should occur under all exit conditions.
	 * <p>
	 * If a finalizer never returns then phase 2 will never complete and again
	 * {@link #halt} will have to be invoked to cause termination. Note however,
	 * that if a finalizer invokes {@link #exit} with a non-zero status, then we
	 * must invoke {@link #halt} straight-away.
	 * <p>
	 * The actual calling of {@link #halt} is arranged by our caller.
	 */
	void shutdown() {
		// assert: shutdownInitiated

		// note only 1 thread can ever execute this method and that hooks
		// can no longer be modified.

		// Phase 1: run all hooks.
		Runtime.getRuntime().runShutdownHooks();
		// phase 2: run finalizers

		if (runFinalizersOnExit) {
			// note that we are executing finalizers
			runningFinalizers = true;
			// TODO: actually execute finalizers
		}

	}

	/**
	 * Terminates the currently running Java virtual machine by initiating its
	 * shutdown sequence, as per the specification of
	 * {@link java.lang.Runtime#exit}. This method never returns.
	 * 
	 * @param status
	 *            the value to be passed back to the host executing environment
	 */
	void exit(int status) {
		boolean haltJVM = false;

		// use main lock to figure out what state we are in but don't
		// hold during the actual shutdown
		synchronized (this) {
			if (shutdownInitiated) {
				// if finalizers are being run and status is non-zero we
				// must halt
				if (runningFinalizers && status != 0) {
					haltJVM = true;
				}
				// else nothing for this thread to do so we'll 'hang'
				// acquiring the shutdown lock
			} else {
				shutdownInitiated = true;
				exitStatus = status;
			}
		}

		if (haltJVM) {
			halt(status); // never returns
			throw new InternalError("halt returned");
		}

		performShutdown();
	}

	/**
	 * Terminates the currently running Java virtual machine immediately as per
	 * the specification of {@link java.lang.Runtime#halt}. This method never
	 * returns.
	 * 
	 * @param status
	 *            the value to be passed back to the host executing environment
	 */
	void halt(int status) {
		// Note that we could get here while shutdown is in progress. In
		// that case we still forcefully halt the VM.
		// It's not clear exactly what that will involve.
		// we don't try to acquire any locks here as that would disallow an
		// immediate shutdown. So the kernel services must deal with the
		// potential for concurrent invocations
		LibraryImports.halt(status);
	}

	/**
	 * Invoke the GC. This is defined as a method on the JVM instance so that we
	 * can override for a Real-time JVM to deal with no-heap entities
	 */
	void gc() {
		LibraryImports.gc();
	}

}
