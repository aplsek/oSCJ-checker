package s3.services.fastLock;

import ovm.core.domain.Oop;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.services.threads.OVMThread;
import ovm.services.monitors.Monitor;
import ovm.services.java.JavaMonitor;
import java.util.Comparator;	// FIXME: what's this doing here?
import s3.util.PragmaTransformCallsiteIR;

public class ForwardingJavaMonitor implements JavaMonitor {
    static native void doEnter(Oop obj) throws BCmonitorenter;
    static native void doExit(Oop obj) throws BCmonitorexit;

    CoreServicesAccess csa;
    Oop real;

    public ForwardingJavaMonitor(CoreServicesAccess csa, Oop real) {
	this.csa = csa;
	this.real = real;
    }

    public void enter()          { doEnter(real); }
    public void enterRecursive() { doEnter(real); }
    public void exit()           { doExit(real); }
    public void exitRecursive()  { doExit(real); }

    JavaMonitor realMonitor() {
	return (JavaMonitor) csa.ensureMonitor(real);
    }

    public void enter(OVMThread thread) {
	realMonitor().enter(thread);
    }

    public boolean waitAbortable(Monitor m) {
	return realMonitor().waitAbortable(m);
    }

    public boolean abortWait(OVMThread thread) {
	return realMonitor().abortWait(thread);
    }

    public int waitTimedAbortable(Monitor m, long timeout) {
	return realMonitor().waitTimedAbortable(m, timeout);
    }

    public void signal() {
	realMonitor().signal();
    }

    public void signalAll() {
	realMonitor().signalAll();
    }

    public OVMThread getOwner() {
	return realMonitor().getOwner();
    }

    public int entryCount() {
	return realMonitor().entryCount();
    }

    public int getEntryQueueSize() {
	return realMonitor().getEntryQueueSize();
    }

    public boolean isEntering(OVMThread t) {
	return realMonitor().isEntering(t);
    }

    public void setComparator(Comparator comp) {
	realMonitor().setComparator(comp);
    }

    public Comparator getComparator() {
	return realMonitor().getComparator();
    }
    
    public void changeNotification(Object o) {
	realMonitor().changeNotification(o);
    }

    static class BCmonitorenter extends PragmaTransformCallsiteIR {
	static {
	    register(BCmonitorenter.class.getName(),
		     new byte[] { (byte) MONITORENTER });
	}
    }
    static class BCmonitorexit extends PragmaTransformCallsiteIR {
	static {
	    register(BCmonitorexit.class.getName(),
		     new byte[] { (byte) MONITOREXIT });
	}
    }
}
