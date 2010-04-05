package s3.services.fastLock;

import ovm.core.domain.Domain;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.execution.Context;
import ovm.core.execution.Native;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.services.threads.OVMThread;
import ovm.core.services.threads.OVMThreadContext;
import ovm.services.monitors.FastLockable;
import ovm.services.monitors.Monitor;
import ovm.core.services.memory.VM_Address;
import s3.core.domain.S3Domain;
import s3.core.execution.S3CoreServicesAccess;
import s3.util.PragmaAtomic;
import s3.util.PragmaInline;
import s3.util.PragmaNoInline;
import s3.util.PragmaNoPollcheck;
import ovm.core.domain.ObjectModel;

public class FastLockCoreServicesAccess extends S3CoreServicesAccess {
    private static final boolean VERBOSE = false;


    public void dbgMon(String str, FastLockable obj) {
      Native.print_string(str);
      Native.print_string(": ");
      dumpMonitor(obj);
      
    }
    public void dumpMonitor(FastLockable obj) {

      if (VERBOSE) {
        Native.print_string("Object:");
        Native.print_ptr(VM_Address.fromObject(obj));
        Native.print_string(" FL-Field:");
        Native.print_hex_int(VM_Address.fromObject(obj).add(ObjectModel.getObjectModel().getMonitorOffset()).getInt());
        Native.print_string("\n");
      }
    }
    
    public void monitorEnterSlow(FastLockable obj) throws PragmaNoInline, PragmaNoPollcheck {
    
        if (VERBOSE) {
          dbgMon("In FCSA, monitorEnterSlow", obj);
        }
        
	int rc = obj.getRecursionCount();
	
	if (VERBOSE) {
  	  Native.print_string("recursion count is ");
  	  Native.print_int(rc);
        }
	
	if (obj.isMine()
	    && rc < ((FastLockable.Model) ObjectModel.getObjectModel()).maxRecursionCount())
	{
	    obj.setRecursionCount(rc + 1);
	    if (VERBOSE) {
	      dbgMon("In FCSA, monitorEnterSlow, Increased recursion count",obj);
	    }
	} else {
	    if (VERBOSE) {
  	      dbgMon("In FCSA, monitorEnterSlow, Calling CSA monitorEnter",obj);
            }
            super.monitorEnter(obj);
            if (VERBOSE) {
              dbgMon("In FCSA, monitorEnterSlow, returned from CSA monitorEnter",obj);
            }
	}
    }

    public void monitorEnter(Oop _obj) throws PragmaInline, PragmaNoPollcheck
    {
	FastLockable obj = (FastLockable) _obj.asAnyOop();
	if (VERBOSE) Native.print_string("doing fastLock\n");
	if (VERBOSE) {
          dbgMon("In FCSA, monitorEnter",obj);
        }
	boolean result=obj.fastLock();
	if (VERBOSE) {
          dbgMon("In FCSA, monitorEnter, after fastLock",obj);
        }	
	if (VERBOSE) Native.print_string("done fastLock\n");
	if (!result) {
            if (VERBOSE) {
              dbgMon("In FCSA, monitorEnter, calling monitorEnterSlow (failed fast fastlock)",obj);
            }   
	    monitorEnterSlow(obj);
            if (VERBOSE) {
              dbgMon("In FCSA, monitorEnter, returned from monitorEnterSlow",obj);
            }   	    
        }
    }


    public void monitorTransfer(Oop _obj, OVMThread newOwner) 
        throws PragmaInline {
	FastLockable obj = (FastLockable) _obj.asAnyOop();
        // do this first so no pollchecks occur once we start checking bits
        Context ctx = newOwner.getContext();

	if (!obj.isMine() || obj.isInflated()) {
	    super.monitorTransfer(obj, newOwner);
            return;
        }

        // assert: recursion-count == 0
        obj.setOwner(ctx);
    }

	
    public Monitor ensureMonitor(Oop _obj) throws PragmaAtomic {
	FastLockable obj = (FastLockable) _obj.asAnyOop();
	
	if (VERBOSE) {
	  dbgMon("In FCSA, ensureMonitor",obj);
	}
	Monitor ret = obj.getMonitor();
	if (VERBOSE) {
	  dbgMon("In FCSA, ensureMonitor, returned from getMonitor",obj);
	  Native.print_string("In FCSA, ensureMonitor, the getMonitor returned ");
	  Native.print_ptr(VM_Address.fromObjectNB(ret));
	  Native.print_string("\n");
	}	
	if (ret == null) {
            if (VERBOSE) {
	    	  dbgMon("In FCSA, ensureMonitor, returned from getMonitor getting null",obj);
            }	
	    Context owner = obj.getOwner();
            if (VERBOSE) {
	    	  dbgMon("In FCSA, ensureMonitor, returned from getOwner",obj);
                  Native.print_string("In FCSA, ensureMonitor, getOwner returned ");
                  Native.print_ptr(VM_Address.fromObjectNB(owner));                  
                  Native.print_string("\n");                  
            }	    
	    int rc = obj.getRecursionCount();
            if (VERBOSE) {
	    	  dbgMon("In FCSA, ensureMonitor, returned from getRecursionCount",obj);
                  Native.print_string("In FCSA, ensureMonitor, getRecursionCount returned ");
                  Native.print_int(rc);                  
                  Native.print_string("\n");                    
            }	    	    
	    ret = super.ensureMonitor(obj);
            if (VERBOSE) {
	    	  dbgMon("In FCSA, ensureMonitor, returned from CSAs ensureMonitor",obj);
                  Native.print_string("In FCSA, ensureMonitor, ensureMonitor returned ");
                  Native.print_ptr(VM_Address.fromObjectNB(ret));               
                  Native.print_string("\n");                    
            }	    	    	    
	    if (owner != null) {
//                 BasicIO.out.println("ensureMonitor: inflating owned monitor");
                if (VERBOSE) {
                  dbgMon("In FCSA, ensureMonitor, returned from CSAs ensureMonitor, replaying the lock recursion count",obj);
                }	    	    	    
		OVMThread t = ((OVMThreadContext) owner).getThread();
		do {
		    ret.enter(t);
		} while (rc-- > 0);
                if (VERBOSE) {
                  dbgMon("In FCSA, ensureMonitor, replayed the lock recursion count",obj);
                }	    	    	    		
	    }
            else {
//                 BasicIO.out.println("ensureMonitor: inflating unowned monitor");
            }
        }

        if (VERBOSE) {
          dbgMon("In FCSA, ensureMonitor, returning",obj);
        }	    	    	            
        
	return ret;
    }

    // override to avoid unnecessary inflation as we do not want allocation
    // to occur unnecessarily during thread finalization.
    public boolean currentThreadOwnsMonitor(Oop _obj) {
	FastLockable obj = (FastLockable) _obj.asAnyOop();
        // explicitly check for inflation so that a false answer to isMine
        // doesn't force inflation through the super call
        if (!obj.isInflated())
            return obj.isMine();
        else
            return super.currentThreadOwnsMonitor(_obj);
    }


    public Monitor aliasMonitor(Oop obj) {
	return new ForwardingJavaMonitor(this, obj);
    }

    public void monitorExitSlow(FastLockable obj) throws PragmaNoInline {
	if (obj.isMine()) {
	    obj.setRecursionCount(obj.getRecursionCount() - 1);
	} else {
	    super.monitorExit(obj);
	}
    }

    public void monitorExit(Oop _obj) throws PragmaInline, PragmaNoPollcheck {
	FastLockable obj = (FastLockable) _obj.asAnyOop();
	if (VERBOSE) Native.print_string("doing fastUnlock\n");
	boolean result=obj.fastUnlock();
	if (VERBOSE) Native.print_string("done fastUnlock\n");
	if (!result)
	    monitorExitSlow(obj);
    }

    public void monitorSignal(Oop _obj) {
	FastLockable obj = (FastLockable) _obj.asAnyOop();
	if (!obj.isMine())
	    super.monitorSignal(obj);
    }

    public void monitorSignalAll(Oop _obj) {
	FastLockable obj = (FastLockable) _obj.asAnyOop();
	if (!obj.isMine())
	    super.monitorSignalAll(obj);
    }

    protected FastLockCoreServicesAccess(S3Domain d) {
	super(d);
    }

    static public class FLFactory extends Factory {
	public CoreServicesAccess make(Domain d) {
	    return new FastLockCoreServicesAccess((S3Domain)d);
	}
    }

}
