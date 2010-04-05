
package test.runtime;
import ovm.core.execution.Context;
import ovm.core.execution.Processor;
import ovm.core.services.io.BasicIO;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.services.threads.OVMThreadCoreImpl;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import test.common.TestBase;
/**
 * This is a basic context switch test between the main thread (which invokes
 * run and a second thread that we create. Each thread prints a message then
 * switches to the other thread. We end up on the main thread which can then
 * do whatever else Main.boot wants it to do. The created thread must not
 * return from it's doRun method as that violates the contract of that method.
 *
 * @author David Holmes
 *
 */
public class TestThreadContextSwitch extends TestBase {

    public TestThreadContextSwitch() {
        super("Basic thread context switch test");
    }

    public void run() {
        // we need the current processor and context
        final Processor proc = Processor.getCurrentProcessor();
        final Context c = proc.getContext();

        if (!(c instanceof OVMThreadContext)) 
            COREfail("Configuration error: need OVMThreadContext");

        final OVMThreadContext ctx = (OVMThreadContext) c;
 
       // now we need to create the "primordial thread" ...
        ThreadServicesFactory tsf = (ThreadServicesFactory)ThreadServiceConfigurator.config.getServiceFactory(ThreadServicesFactory.name);
        OVMThread primThread = tsf.getPrimordialThread(ctx);
        BasicIO.out.print("Created main thread: " + primThread + "\n");

        // ... and bind the current context and primordial thread together
        ctx.setThread(primThread);

        BasicIO.out.print(ctx +"\n");

        // define a second thread to switch with the main thread
        OVMThreadCoreImpl thread2 = new OVMThreadCoreImpl() {
                protected void doRun() {
                    try {
                        for (int i =0; i < 5; i++) {
                            BasicIO.out.print("Thread 2: " + i + "\n");
                            proc.run(ctx);
                        }
                        BasicIO.out.print("If you see this the VM is about to abort\n");
                    }
                    catch(Throwable t) {
                        BasicIO.out.print("Exception in thread 2: " + t + "\n");
                    }
                }
            };

        // now set up the main thread to switch to the new thread
        Context ctx2 = thread2.getContext();
        for (int i =0; i < 5; i++) {
            BasicIO.out.print("Main thread " + i + "\n");
            proc.run(ctx2);
        }

        // now tidy up
        thread2 = null;
        ctx2.destroy();
        ctx2 = null;

    }
}


