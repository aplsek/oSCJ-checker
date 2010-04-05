
package s3.core;

import ovm.core.Executive;
import ovm.core.domain.DomainDirectory;
import ovm.core.execution.Context;
import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.execution.NativeInterface;
import ovm.core.execution.OVMSignals;
import ovm.core.services.events.EventManager;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.threads.OVMDispatcher;
import ovm.core.services.timer.TimerManager;
import ovm.core.services.events.EventManager;
import ovm.core.services.events.PollcheckManager;
import ovm.core.stitcher.EventServicesFactory;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.core.stitcher.ServiceConfiguratorBase;
import ovm.core.stitcher.ServiceFactory;
import ovm.core.stitcher.SignalServicesFactory;
import ovm.core.stitcher.ThreadDispatchServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.services.ServiceInstance;
import ovm.services.events.SignalMonitor;
import ovm.util.CommandLine;
import s3.core.domain.S3JavaUserDomain;
import s3.services.java.ulv1.JavaMonitorImpl;
import s3.services.transactions.Transaction;
import s3.util.PragmaAtomic;

public class S3Executive extends S3Base implements Executive.Interface {


    // hook to the C code to set the native thread priority
    static final class NativeScheduler implements NativeInterface {
        static native int setPriority(int prio);
	
	static native void startProfilingEvents(int histo_size,
						int trace_buf_size,
						boolean skip_pa);
	
	static native void initPollcheckTimerProf(int trace_buf_size);
    }

    private volatile CommandLine processCommandLine;
    protected final String executionMode;

    /**
     * In the future the executionMode will (possibly) change from
     * OVM invocation to invocation, currently it determined at image build
     * time.<P> 
     * Used reflectively.
     * @param executionMode could be either null or nonnull :-)
    */ 
    public S3Executive(String executionMode) {
	this.executionMode = executionMode;
    }
    
    /** The configured dispatcher for the threading system */
    OVMDispatcher dispatcher;

    /** The configured event manager */
    EventManager em;


    /** All of the currently configured service instances - we assume/require that
        services are not added/removed dynamically
    */
    ServiceInstance[] services;

    private int calcNServices(ServiceConfiguratorBase config) {
        int nServices = 0;
        ServiceFactory[] factories = config.getServiceFactories();
        for (int i = 0; i < factories.length; i++) {
            ServiceInstance[] sa = factories[i].getServiceInstances();
            for (int j = 0; j < sa.length; j++) {
                if (sa[j] == null) {
                    continue;
                }
                
                nServices++;
            }
        }
        return nServices;
    }
    
    private int populateServices(int curIndex,
                                 ServiceConfiguratorBase config) {
        ServiceFactory[] factories = config.getServiceFactories();
        for (int i = 0; i < factories.length; i++) {
            ServiceInstance[] sa = factories[i].getServiceInstances();
            for (int j = 0; j < sa.length; j++) {
                if (sa[j] == null) {
                    continue;
                }
                
                services[curIndex++] = sa[j];
            }
        }
        return curIndex;
    }

    /**
     * if non-zero, call
     * {@link ovm.core.execution.Native#abortDelayed} rather than
     * calling abort directly.  Controlled by -panic-delay parameter
     **/
    private int panicDelay;
    /**
     * If true, be sure to print a stack trace on panic.
     **/
    private boolean panicTrace;

    public void startup() {
        
        Native.turnOffBuffering();
        
        try {
	    boolean imageBarrier = !getOptionEarly(DISABLE_IMAGE_BARRIER);
	    
            // This MUST always be the first call
            MemoryManager.the().boot(imageBarrier);

            // this must be initialized ASAP otherwise we'll crash trying to
            // process errors in the rest of the init code
            ovm.core.services.io.BasicIO.init();

	    Context.getCurrentContext().bootPrimordialContext();

            // start the executive domain
            DomainDirectory.getExecutiveDomain().startup();

            // tell the memory manager we're ready
	    MemoryManager.the().fullyBootedVM();

        } catch(Throwable t) { // typically to catch configuration errors
            // NOTE: depending on the problem it may be impossible
            //       to actually generate and throw an exception. If we
            //       try and fail we should get a recursive exception panic
	    throw Executive.panicOnException(t, "During boot process");
        }


        // get the dispatcher from the configuration
        try {
            ThreadDispatchServicesFactory tdsf = 
                (ThreadDispatchServicesFactory)ThreadServiceConfigurator.
                config.getServiceFactory(ThreadDispatchServicesFactory.name);
            dispatcher = tdsf.getThreadDispatcher();
        }
        catch(Throwable t) {
            throw Executive.panicOnException(t, "Locating Dispatcher");
        }


        // extract all the service instances
        try {
            services = 
                new ServiceInstance[calcNServices(ThreadServiceConfigurator.config) + calcNServices(IOServiceConfigurator.config)];
            
            populateServices(populateServices(0,
                                              ThreadServiceConfigurator.config),
                             IOServiceConfigurator.config);

            ThreadServiceConfigurator.config.printConfiguration();
            IOServiceConfigurator.config.printConfiguration();
        }
        catch(Throwable t) {
            throw Executive.panicOnException(t, "Reading service instances");
        }

        // initialise all the service instances - inter service dependencies
        // mean some services will be initialized before we get to them
        try {
            for( int i = 0; i < services.length; i++) {
                if (!services[i].isInited()) {
                    //d("Initializing service: " + services[i]);
                    services[i].init();
                }
            }
        }
        catch(Throwable t) {
            throw Executive.panicOnException(t, "Initializing service instances");
        }

	// get the event manager
	em = (EventManager)((EventServicesFactory)IOServiceConfigurator.config.
                            getServiceFactory(EventServicesFactory.name)).
                            getEventManager();


	getCommandLine().consumeOption(DISABLE_IMAGE_BARRIER_STRING);

	String panicDelay = getCommandLine().consumeOption("panic-delay");
	if (panicDelay != null)
	    try {
		this.panicDelay = Integer.parseInt(panicDelay);
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value on -panic-delay");
	    }
	panicTrace = getCommandLine().consumeOption("panic-trace") != null;
	
        // configure services with command-line options. It's not clear
        // whether S3Executive should take responsibility for understanding
        // command-line options and what services use them, or whether each
        // service should have access to the command-line and parse it itself.
	
	// check if we are profiling the timer pollcheck
	String timer_pc_trace_buf_size=
	    getCommandLine().consumeOption("timer-pc-trace-buf-size");
	if (timer_pc_trace_buf_size!=null) {
	    try {
		NativeScheduler
		    .initPollcheckTimerProf(Integer.parseInt(timer_pc_trace_buf_size));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value on "+
				    "-timer-pc-trace-buf-size");
	    }
	}

	// check if we are doing profiling of events
	int pcp_histo_size=0;
	int pcp_trace_buf_size=0;
	String pcp_histo_size_str = getCommandLine().consumeOption("pcp-histo-size");
	if (pcp_histo_size_str!=null) {
	    try {
		pcp_histo_size=Integer.parseInt(pcp_histo_size_str);
	    } catch (NumberFormatException e) {
                BasicIO.err.println("ERROR: Invalid value on -pcp-histo-size option");
	    }
	}
	String pcp_trace_buf_size_str = getCommandLine().consumeOption("pcp-trace-buf-size");
	if (pcp_trace_buf_size_str!=null) {
	    try {
		pcp_trace_buf_size=Integer.parseInt(pcp_trace_buf_size_str);
	    } catch (NumberFormatException e) {
                BasicIO.err.println("ERROR: Invalid value on -pcp-trace-buf-size option");
	    }
	}
	NativeScheduler
	    .startProfilingEvents(pcp_histo_size,
				  pcp_trace_buf_size,
				  getCommandLine().consumeOption("pcp-skip-pa")!=null);

	if (getCommandLine().consumeOption("disable-concurrent-gc")!=null) {
	    if (MemoryManager.the().caresAboutDisableConcurrency()) {
		MemoryManager.the().setDisableConcurrency(true);
	    } else {
		BasicIO.err.println("WARNING: -disable-concurrent-gc option ignored; not applicable in current memory manager configuration.");
	    }
	}

	if (getCommandLine().consumeOption("gc-stack-uninterruptible")!=null) {
	    if (MemoryManager.the().caresAboutStackUninterruptible()) {
		MemoryManager.the().setStackUninterruptible(true);
	    } else {
		BasicIO.err.println("WARNING: -gc-stack-uninterruptible option ignored; not applicable in current memory manager configuration.");
	    }
	}

	if (getCommandLine().consumeOption("gc-mark-uninterruptible")!=null) {
	    if (MemoryManager.the().caresAboutMarkUninterruptible()) {
		MemoryManager.the().setMarkUninterruptible(true);
	    } else {
		BasicIO.err.println("WARNING: -gc-mark-uninterruptible option ignored; not applicable in current memory manager configuration.");
	    }
	}

	if (getCommandLine().consumeOption("gc-sweep-uninterruptible")!=null) {
	    if (MemoryManager.the().caresAboutSweepUninterruptible()) {
		MemoryManager.the().setSweepUninterruptible(true);
	    } else {
		BasicIO.err.println("WARNING: -gc-sweep-uninterruptible option ignored; not applicable in current memory manager configuration.");
	    }
	}

	if (getCommandLine().consumeOption("gc-log-uninterruptible")!=null) {
	    if (MemoryManager.the().caresAboutLogUninterruptible()) {
		MemoryManager.the().setLogUninterruptible(true);
	    } else {
		BasicIO.err.println("WARNING: -gc-log-uninterruptible option ignored; not applicable in current memory manager configuration.");
	    }
	}

	if (getCommandLine().consumeOption("gc-compact-uninterruptible")!=null) {
	    if (MemoryManager.the().caresAboutCompactUninterruptible()) {
		MemoryManager.the().setCompactUninterruptible(true);
	    } else {
		BasicIO.err.println("WARNING: -gc-compact-uninterruptible option ignored; not applicable in current memory manager configuration.");
	    }
	}

	if (getCommandLine().consumeOption("gc-enable-time-trace")!=null) {
	    if (MemoryManager.the().caresAboutEnableTimeTrace()) {
		MemoryManager.the().setEnableTimeTrace(true);
	    } else {
		BasicIO.err.println("WARNING: -gc-enable-time-trace option ignored; not applicable in current memory manager configuration.");
	    }
	}

	if (getCommandLine().consumeOption("diagnose:gc") != null) {
	    MemoryManager.the().enableAllDebug();
	    getCommandLine().consumeOption("verbose:gc");
	} else 	if (getCommandLine().consumeOption("verbose:gc") == null) {
	    //MemoryManager.the().enableSilentMode();
	}
	
	if (getCommandLine().consumeOption("profile-alloc")!=null) {
	    MemoryManager.the().enableAllocProfiling();
	}

	if (getCommandLine().consumeOption("profile-mem-usage")!=null) {
	    MemoryManager.the().enableProfileMemUsage();
	}
	
	String gcThreshold = getCommandLine().consumeOption("gc-threshold");
	if (gcThreshold!=null) {
	    try {
		MemoryManager.the().setGCThreshold(CommandLine.parseSize(gcThreshold));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -gc-threshold option: "+e);
	    }
	}

	String gcMutatorDisableThreshold = getCommandLine().consumeOption("gc-mutator-disable-threshold");
	if (gcMutatorDisableThreshold!=null) {
	    try {
		MemoryManager.the().setMutatorDisableThreshold(CommandLine.parseSize(gcMutatorDisableThreshold));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -gc-mutator-disable-threshold option: "+e);
	    }
	}

	String compactionThreshold = getCommandLine().consumeOption("compaction-threshold");
	if (compactionThreshold!=null) {
	    try {
		MemoryManager.the().setCompactionThreshold(CommandLine.parseSize(compactionThreshold));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -compaction-threshold option: "+e);
	    }
	}

	String effectiveMemorySize = getCommandLine().consumeOption("effective-memory-size");
	if (effectiveMemorySize!=null) {
	    try {
		MemoryManager.the().setEffectiveMemorySize(CommandLine.parseSize(effectiveMemorySize));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -effective-memory-size  option: "+e);
	    }
	}

	String abortOnGcReentry = getCommandLine().consumeOption("gc-abort-on-gc-reentry");
	if (abortOnGcReentry!=null) {
	  if (MemoryManager.the().caresAboutAbortOnGcReentry()) {
	    try {
		MemoryManager.the().setAbortOnGcReentry(true);
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -gc-abort-on-gc-reentry  option: "+e);
	    }
          } else {
            BasicIO.err.println("WARNING: -gc-abort-on-gc-reentry ignored, not supported by memory manager");  
          }
	}

	String reportLongLatency = getCommandLine().consumeOption("gc-report-long-latency");
	if (reportLongLatency!=null) {
	  if (MemoryManager.the().caresAboutLongLatency()) {
	    try {
		MemoryManager.the().setLongLatency(Long.parseLong(reportLongLatency));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -gc-report-long-latency option: "+e);
	    }
          } else {
            BasicIO.err.println("WARNING: -gc-report-long-latency ignored, not supported by memory manager");  
          }
	}


	String reportLongPause = getCommandLine().consumeOption("gc-report-long-pause");
	if (reportLongPause!=null) {
	  if (MemoryManager.the().caresAboutLongPause()) {
	    try {
		MemoryManager.the().setLongPause(Long.parseLong(reportLongPause));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -gc-report-long-pause option: "+e);
	    }
          } else {
            BasicIO.err.println("WARNING: -gc-report-long-pause ignored, not supported by memory manager");  
          }
	}

	String sizeHisto = getCommandLine().consumeOption("size-histo");
	if (sizeHisto!=null) {
	    try {
		MemoryManager.the().enableSizeHisto(Integer.parseInt(sizeHisto));
	    } catch (NumberFormatException e) {
		BasicIO.err.println("ERROR: Invalid value for -size-histo option: "+e);
	    }
	}

	// read pc max count
	String pcMaxCount = getCommandLine().consumeOption("pc-max-count");
	if (pcMaxCount!=null) {
	    if (PollcheckManager.getSettings().supportsMaxCount()) {
		try {
		    PollcheckManager.getSettings()
			.setMaxCount((short)Integer.parseInt(pcMaxCount));
		} catch (NumberFormatException e) {
		    BasicIO.err.println("ERROR: Invalid value on -pc-max-count option");
		}
	    } else {
		BasicIO.err.println("WARNING: -pc-max-count option ignored; not applicable in current pollcheck configuration.");
	    }
	}

        // read timer interrupt period in microseconds
        String timerPeriod = getCommandLine().consumeOption("period");
        if (timerPeriod != null) {
            try {
                int period = Integer.parseInt(timerPeriod);
//                 BasicIO.out.print("Read timer interrupt period of ");
//                 BasicIO.out.println(period);
                TimerServicesFactory tsf = (TimerServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(TimerServicesFactory.name);
                TimerManager tm = tsf.getTimerManager();
                if (tm.canSetTimerInterruptPeriod()) {
                    if (!tm.setTimerInterruptPeriod(period * 1000L))
                        BasicIO.err.println("ERROR: Invalid value on -period option");
                }
            }
            catch(NumberFormatException ex) {
                BasicIO.err.println("ERROR: Invalid value on -period option");
            }
        }
	
	// timer multiplier
	String timerMultiplier = getCommandLine().consumeOption("period-multiplier");
	if (timerMultiplier != null) {
            try {
                int mult = Integer.parseInt(timerMultiplier);
                TimerServicesFactory tsf = (TimerServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(TimerServicesFactory.name);
                TimerManager tm = tsf.getTimerManager();
                if (tm.canSetTimerInterruptPeriod()) {
                    if (!tm.setTimerInterruptPeriodMultiplier(mult))
                        BasicIO.err.println("ERROR: Invalid value on -period-multiplier option");
                }
            }
            catch(NumberFormatException ex) {
                BasicIO.err.println("ERROR: Invalid value on -period-multiplier option");
            }
	}

        // read native thread priority
        String priorityVal = getCommandLine().consumeOption("priority");
        if (priorityVal != null) {
            int prio = 0;
            try {
                prio = Integer.parseInt(priorityVal);
                int ret = NativeScheduler.setPriority(prio);
                if (ret != 0) {
                    switch (ret) {
                    case NativeConstants.ENOSYS:
                        BasicIO.err.println("ERROR: -priority option not supported due to no priority scheduling in the OS");
                        break;
                    case NativeConstants.EINVAL:
                        BasicIO.err.println("ERROR: -priority value out of range - using default");
                        break;
                    default:
                        BasicIO.err.println("ERROR: unable to set native priority: errno = " + ret);
                    }
                }
                else {
                    d("Native thread priority set to " + prio);
                }
            }
            catch(NumberFormatException ex) {
                BasicIO.err.println("ERROR: Invalid value on -priority option - using default");
            }
        }


        // start all the service instances - again some will be started by
        // other services
        try {
            for( int i = 0; i < services.length; i++) {
                if (!services[i].isStarted()) {
                    //d("Starting service: " + services[i]);
                    services[i].start();
                }
            }
        }
        catch(Throwable t) {
            throw Executive.panicOnException(t, "starting service instances");
        }


	// If you need to test something without reference to threading
	// or synchronization then do it here - but make sure you
	// catch any exceptions.

	// Initialise the threading subsystem and 'run' the VM
	
	try {
            dispatcher.initializeThreading();
	    try {
		run(); // no user-domain exceptions possible
	    } 
            catch (Throwable e) {
		BasicIO.err.print(
                    "Uncaught exception in S3Executive.startup(): ");
                BasicIO.err.println(e);
		e.printStackTrace();
		shutdown(1);
	    } 
            finally {
                // never returns - terminates OVM if last kernel thread
                try {
                    dispatcher.terminateCurrentThread(); 
                } 
                catch (Throwable t) {
                    panicOnException(t, "terminating current thread");
                }
		panic("unexpected return from terminateCurrentThread");
	    }
	} 
        catch (Throwable t) {
            panicOnException(t, "Initializing threading system");
        } 
        

    }
    
    private boolean shuttingDown=false;

    // this is called as part of general termination (such as from a JVM's
    // System.exit and so multiple threads could still be using these services
    // as we shut them down. That would be a "Bad Thing" so we ensure this
    // method is executed atomically.
    public void shutdown(int reason) throws PragmaAtomic  {
	if (shuttingDown) {
	    throw Executive.panic("shutdown() called recursively.");
	}
	shuttingDown=true;

	// tell the memory manager we're going down
	MemoryManager.the().vmShuttingDown();

	JavaMonitorImpl.dumpMonitorStatistics();//FIXME
	if (Transaction.the().gatherStatistics())
	    Transaction.the().dumpStats();

	// run whatever cleanups are necessary
        try {
            for( int i = 0; i < services.length; i++) {
                try {
                    services[i].aboutToShutdown();
                }
                catch(Throwable t) {
                    BasicIO.err.println("Exception during S3Executive pre-shutdown hook");
                    BasicIO.err.println(t);
                    try {
                        t.printStackTrace();
                    }
                    catch (Throwable t2) {
                        BasicIO.err.println(" - error printing stack trace");
                    }
                }
            }
            for( int i = 0; i < services.length; i++) {
                try {
                    services[i].stop();
                    services[i].destroy();
                }
                catch(Throwable t) {
                    BasicIO.err.println("Exception during S3Executive shutdown");
                    BasicIO.err.println(t);
                    try {
                        t.printStackTrace();
                    }
                    catch (Throwable t2) {
                        BasicIO.err.println(" - error printing stack trace");
                    }
                }
            }
        }
        finally {
            // shutdown all the domains ...
            Native.exit_process(reason);
        }
    }

    public void shutdown() {
        shutdown(0);
    }

    private void _panic(boolean panicTrace) {
	try {
	    if (panicTrace) {
		MemoryPolicy.the().enterExceptionSafeArea();
		new Error("panic").printStackTrace();
	    } 
	} finally {
	    if (panicDelay > 0)
		Native.abortDelayed(panicDelay);
	    else
		Native.abort();
	}
    }

    public void panic(String message) {
        MemoryPolicy.the().enterExceptionSafeArea();
        try {
            BasicIO.err.print("PANIC: ");
            BasicIO.err.println(message);
        }
        finally {
	    _panic(panicTrace);
        }
    }
    
    public void panicOnErrno(String message, int errno) {
        MemoryPolicy.the().enterExceptionSafeArea();
        try {
            BasicIO.err.print("PANIC: ");
            BasicIO.err.println(message);
            BasicIO.err.print("ERRNO: ");
            BasicIO.err.println(errno);
        }
        finally {
            _panic(panicTrace);
        }
    }

    public void panicOnException(Throwable t, String message) {
        MemoryPolicy.the().enterExceptionSafeArea();
        try {
            BasicIO.err.print("PANIC: ");
            BasicIO.err.println(message);
            BasicIO.err.println("Unhandled Exception:");
            try {
                BasicIO.err.println(t);
                t.printStackTrace();
            } catch (Throwable t2) {
                BasicIO.err.print("\n<error printing stack trace>\n");
            }
        }
        finally {
	    _panic(false);
        }
    }
    
    public CommandLine getCommandLine() {
	if (processCommandLine == null) { // OK, a race - but benign
	    processCommandLine = parseCommandLine();
	}
	return processCommandLine;
    }

    private static CommandLine parseCommandLine() {
	String[] args = new String[Native.get_process_arg_count() - 1];
	byte[] buf = new byte[512];
	for (int i = 0; i < args.length; i++) {
	    // skip the process name (hence start from 1
	    int len = Native.get_process_arg(i + 1, buf, buf.length);
	    args[i] = new String(buf, 0, len);
	}
	return new CommandLine(args);
    }

    private static final String DISABLE_IMAGE_BARRIER_STRING =
	"disable-image-barrier";
    /**
     * The ascii representation of {@link #DISABLE_IMAGE_BARRIER_STRING}.
     * This option forces the garbage collector to scan the entire
     * bootimage for references into the heap, rather than using a
     * mprotect-based card marking scheme.
     * <p>
     * This code assumes the default encoding is a superset of 7-bit ascii.
     **/
    private static final byte[] DISABLE_IMAGE_BARRIER = 
	DISABLE_IMAGE_BARRIER_STRING.getBytes();
    private static final byte[] cmdBuf = new byte[512];

    /**
     * This method may be used to parse options before allocation is
     * possible.  Currently it is used to parse the
     * {@link #DISABLE_IMAGE_BARRIER} option which is used to
     * bootstrap the memory manager.
     *
     * @param option the option to get early, because this method is
     * typically used before runtime allocation is possible, option
     * should reside in the bootimage
     **/
    private static boolean getOptionEarly(byte[] option) {
	int argc = Native.get_process_arg_count();
outer:	for (int i = 1; i < argc; i++) {
	    int len = Native.get_process_arg(i, cmdBuf, cmdBuf.length);
	    if (len == -1 || cmdBuf[0] != '-')
		return false;
	    if (len !=  option.length + 1)
		continue;
	    for (int j = 0; j < option.length; j++)
		if (cmdBuf[j+1] != option[j])
		    continue outer;
	    return true;
	}
	return false;
    }

    private void run() {
        CommandLine cmd = getCommandLine();


        // Process all the command-line args.
        // FIXME: we need a more structured way to let services configure
        // themselves based on command-line args

        // SIGUSR1 causes thread stack dumps. In the future the signal to use
        // should be configurable.
	installDumpHandler(cmd.consumeOption("dumpAll")!=null,
			   cmd.consumeOption("dumpStacks")!=null,
			   cmd.consumeOption("dumpEvMan")!=null);

             
        // run the executive domain tests first. If something critical
        // fails here we may not get the user-domain up and running and may
        // not be able to see what's gone wrong if we ran the tests after the
        // user-domain code.
        // Additionally, until we have domain lifecycle management, when the
        // user-domain JVM terminates we'll invoke shutdown and so the call to
        // user-domain.run never returns

	if (cmd.consumeOption("dump-image-profile") != null)
	    dumpImageProfile();
	
	if (cmd.consumeOption("doexectests") != null)  // FIXME: !!! runs without working RTGC 
	    test.TestSuite.the().run();

	if (cmd.consumeOption("nouserdomain") != null)
	    return;

	// No engine-specific code is needed right now, but once we
	// can pass arguments into the userDomain, that may change.
	S3JavaUserDomain userDomain = (S3JavaUserDomain)
            DomainDirectory.getUserDomain(1);

	userDomain.getRuntimeExports().setGCTimerMutatorCounts(14);
	userDomain.getRuntimeExports().setGCTimerCollectorCounts(6);
	userDomain.getRuntimeExports().setGCTimerPeriod(-1);	
	
	if (MemoryManager.the().supportsPeriodicScheduler()) {
          userDomain.getRuntimeExports().setGCThreadPriority( 42 );
        } else {
          userDomain.getRuntimeExports().setGCThreadPriority( 1 );
        }
        
        userDomain.getRuntimeExports().setGCThreadLowPriority( 1 );

	String gcThreadPriority=getCommandLine().consumeOption("gc-thread-priority");
	if (gcThreadPriority!=null) {
	    if (MemoryManager.the().needsGCThread()) {
		try {
		    userDomain.getRuntimeExports().setGCThreadPriority(Integer.parseInt(gcThreadPriority));
		} catch (NumberFormatException e) {
		    BasicIO.err.println("ERROR: Invalid value for -gc-thread-priority");
		}
	    } else {
		BasicIO.err.println("WARNING: -gc-thread-priority ignored because we aren't using a GC thread.");
	    }
	}

	
	if (cmd.consumeOption("gc-periodic-scheduler")!=null) {
	  if (MemoryManager.the().supportsPeriodicScheduler()) {
	    MemoryManager.the().setPeriodicScheduler(true);
	  } else {
	    BasicIO.err.println("WARNING: -gc-periodic-scheduler ignored because GC does not support periodic scheduler/");
	  }
	}

	if (cmd.consumeOption("gc-aperiodic-scheduler")!=null) {
	  if (MemoryManager.the().supportsAperiodicScheduler()) {
	    MemoryManager.the().setAperiodicScheduler(true);
	  } else {
	    BasicIO.err.println("WARNING: -gc-aperiodic-scheduler ignored because GC does not support aperiodic scheduler");
	  }
	}

	if (cmd.consumeOption("gc-hybrid-scheduler")!=null) {
	  if (MemoryManager.the().supportsHybridScheduler()) {
	    MemoryManager.the().setHybridScheduler(true);
	  } else {
	    BasicIO.err.println("WARNING: -gc-hybrid-scheduler ignored because GC does not support hybrid scheduler");
	  }
	}

	String gcThreadLowPriority=getCommandLine().consumeOption("gc-thread-low-priority");
	if (gcThreadLowPriority!=null) {
	    if (MemoryManager.the().usesHybridScheduler()) {
		try {
		    userDomain.getRuntimeExports().setGCThreadLowPriority(Integer.parseInt(gcThreadLowPriority));
		} catch (NumberFormatException e) {
		    BasicIO.err.println("ERROR: Invalid value for -gc-thread-low-priority");
		}
	    } else {
		BasicIO.err.println("WARNING: -gc-thread-low-priority ignored because we aren't using a hybrid scheduler.");
	    }
	}



	String gcTimerMutatorCounts=getCommandLine().consumeOption("gc-timer-mutator-counts");
	if (gcTimerMutatorCounts!=null) {
	    if (MemoryManager.the().usesPeriodicScheduler() || MemoryManager.the().usesHybridScheduler()) {
		try {
		    userDomain.getRuntimeExports().setGCTimerMutatorCounts(Integer.parseInt(gcTimerMutatorCounts));
		} catch (NumberFormatException e) {
		    BasicIO.err.println("ERROR: Invalid value for -gc-timer-mutator-counts");
		}
	    } else {
		BasicIO.err.println("WARNING: -gc-timer-mutator-counts ignored because GC doesn't use periodic scheduling.");
	    }
	}

	String gcTimerCollectorCounts=getCommandLine().consumeOption("gc-timer-collector-counts");
	if (gcTimerCollectorCounts!=null) {
	    if (MemoryManager.the().usesPeriodicScheduler() || MemoryManager.the().usesHybridScheduler()) {
		try {
		    userDomain.getRuntimeExports().setGCTimerCollectorCounts(Integer.parseInt(gcTimerCollectorCounts));
		} catch (NumberFormatException e) {
		    BasicIO.err.println("ERROR: Invalid value for -gc-timer-collector-counts");
		}
	    } else {
		BasicIO.err.println("WARNING: -gc-timer-collector-counts ignored because GC doesn't use periodic scheduling.");
	    }
	}

	String gcTimerPeriod=getCommandLine().consumeOption("gc-timer-period");
	if (gcTimerPeriod!=null) {
		try {
		    userDomain.getRuntimeExports().setGCTimerPeriod(Integer.parseInt(gcTimerPeriod)*1000L); // convert from us to ns
		} catch (NumberFormatException e) {
		    BasicIO.err.println("ERROR: Invalid value for -gc-timer-period");
		}
	}

	String interruptibilityMask = getCommandLine().consumeOption("gc-interruptibility-mask");
	if (interruptibilityMask != null) {
	  MemoryManager.the().setInterruptibilityMask(interruptibilityMask);
	}

        MemoryManager.the().initWithCommandLineArguments();
        
	// Run simplejit
	userDomain.compile();
	
	userDomain.startup();

	// Unparse the command line, producing a new command line that
	// does not contain the options we consumed.
        String[] udArgs = cmd.unparse();
	
	userDomain.run(udArgs);
    }

    void dumpImageProfile() {
	MemoryManager.the().dumpExtent(Native.getImageBaseAddress(),
				       Native.getImageEndAddress());
    }

    static final boolean DEBUG_DUMP = true;

    void installDumpHandler(final boolean dumpAll,
			    final boolean dumpStacks,
			    final boolean dumpEvMan) {
	if (dumpEvMan && !em.hasNonTrivialDumpStateHook()) {
	    BasicIO.out.println("-dumpEvMan makes no sense, because configured "+
				"event manager does not have any information "+
				"to dump.");
	    Native.exit_process(1);
	}
	
        SignalMonitor sigMon = null;
        SignalServicesFactory ssf = (SignalServicesFactory)
            ThreadServiceConfigurator.config.getServiceFactory(SignalServicesFactory.name);
        if (ssf != null) {
            sigMon = ssf.getSignalMonitor();
            if (sigMon != null) {
                SignalMonitor.SignalHandler handler =
                    new SignalMonitor.SignalHandler() {
                        public void fire(int sig, int count) {
                            // we have no idea the context in which we will
                            // execute so we have to be careful not to trample
                            // on the current thread
                            Object r1 =
				MemoryPolicy.the().enterExceptionSafeArea();
			    try {
				if (dumpAll || dumpStacks) {
				    try {
					dispatcher.dumpStacks();
				    }
				    catch (Throwable t) {
					BasicIO.err.println("Exception occurred "+
							    "trying to dump "+
							    "stacks");
					if (DEBUG_DUMP) {
					    try {
						t.printStackTrace();
					    }
					    catch(Throwable t2) {
						BasicIO.err
						    .println("Exception occurred "+
							     "trying to print "+
							     "exception stack "+
							     "trace");
					    }
					}
				    }
				}
				if (dumpAll || dumpEvMan) {
				    try {
					em.dumpStateHook();
				    } catch (Throwable t) {
					BasicIO.err.println("Exception occurred "+
							    "trying to dump "+
							    "event manager state");
				    }
				}
			    }
                            finally {
                                MemoryPolicy.the().leave(r1); 
                            }
                        }
                    };
                sigMon.addSignalHandler(handler, OVMSignals.OVM_SIGUSR1);
            }
        }
    }
}
