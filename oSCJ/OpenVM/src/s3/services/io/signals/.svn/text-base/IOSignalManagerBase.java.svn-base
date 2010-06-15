
package s3.services.io.signals;

import ovm.core.execution.NativeConstants;
import ovm.services.io.signals.*;
import ovm.core.services.memory.*;

import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;
/**
 * Implements the link list management and other simple stuff that
 * every IO signal manager is probably going to have.
 * @author Filip Pizlo
 */
abstract class IOSignalManagerBase
    extends ovm.services.ServiceInstanceImpl
    implements IOSignalManager {
    
    private CallbackNode freeList_ = null;
    private CallbackNode[] cbackForFd_ = new CallbackNode[NativeConstants.FD_SETSIZE];
    
    public void start() {
        for (int i=0;i<cbackForFd_.length;++i) {
            if (!fdUsed(i)) {
                continue;
            }
//             BasicIO.out.println(this + " start() enabling fd " + i);
            enableFD(i);
        }
        super.start();
    }
    
    public void stop() {
        for (int i=0;i<cbackForFd_.length;++i) {
            if (!fdUsed(i)) {
                continue;
            }
//             BasicIO.out.println(this + " stop() disabling fd " + i);
            disableFD(i);
        }
        super.stop();
    }
    
    protected CallbackNode allocNode(int fd,Callback cback) {
        if (freeList_==null) {
            MemoryManager mm=MemoryManager.the();
            VM_Area prev=mm.setCurrentArea(mm.getImmortalArea());
            try {
                return new CallbackNode(fd,cback);
            } finally {
                mm.setCurrentArea(prev);
            }
        }
        CallbackNode result=freeList_;
        freeList_=result.next;
        
        result.fd=fd;
        result.setCback(cback);
        result.next=null;
        result.prev=null;
        
        return result;
    }
    
    protected void freeNode(CallbackNode n) {
        n.next=freeList_;
        n.prev=null;
        freeList_=n;
    }
    
    protected boolean fdUsed(int fd) throws PragmaNoPollcheck {
        return cbackForFd_[fd]!=null;
    }
    
    protected abstract void enableFD(int fd);
    protected abstract void disableFD(int fd);
    
    /**
     * Enables edge triggering on the file descriptor if not already enabled,
     * and adds the callback to the list of callbacks for the file descriptor.
     * If the callback is already in ANY of the lists of callbacks, things
     * will blow up.
     */
    protected void addCallbackImpl(int fd,Callback cback)
	throws PragmaAtomic {
        if (!fdUsed(fd)) {
            enableFD(fd);
        }
        CallbackNode cbackNode=allocNode(fd,cback);
        cbackNode.next=cbackForFd_[fd];
        cbackNode.prev=null;
        if (cbackForFd_[fd]!=null) {
            cbackForFd_[fd].prev=cbackNode;
        }
        cbackForFd_[fd]=cbackNode;
    }
    
    public void addCallbackForRead(int fd,Callback cback)
	throws PragmaAtomic
    {
        addCallbackImpl(fd,cback);
    }
    
    public void addCallbackForWrite(int fd,Callback cback)
	throws PragmaAtomic
    {
        addCallbackImpl(fd,cback);
    }
    
    public void addCallbackForExcept(int fd,Callback cback)
	throws PragmaAtomic
    {
        addCallbackImpl(fd,cback);
    }
    
    private void removeCallbackImpl(CallbackNode cbackNode) {
        if (cbackNode.prev==null) {
            cbackForFd_[cbackNode.fd]=cbackNode.next;
            if (cbackNode.next!=null) {
                cbackNode.next.prev=null;
            } else {
//                 BasicIO.out.println(this + 
//                                     " removeCallbackImpl() disabling fd " 
//                                     + cbackNode.fd);
                // now it's empty!
                disableFD(cbackNode.fd);
            }
        } else {
            cbackNode.prev.next=cbackNode.next;
            if (cbackNode.next!=null) {
                cbackNode.next.prev=cbackNode.prev;
            }
        }
        
        cbackNode.next=null;
        cbackNode.prev=null;
        
        freeNode(cbackNode);
    }
    
    protected void removeCallbackFromFDImpl(int fd,
                                            Callback cback,
                                            Object byWhat) {
        CallbackNode cur=cbackForFd_[fd];
        while (cur!=null) {
            CallbackNode next=cur.next;
            if (cur.getCback()==cback) {
                cback.removed(byWhat);
                removeCallbackImpl(cur);
            }
            cur=next;
        }
    }
    
    final public void removeCallbackFromFD(int fd,
					   Callback cback,
					   Object byWhat)
	throws PragmaAtomic
    {
        removeCallbackFromFDImpl(fd,cback,byWhat);
    }
    
    final public void removeCallback(Callback cback,
				     Object byWhat)
	throws PragmaAtomic
    {
        for (int i=0;i<cbackForFd_.length;++i) {
            removeCallbackFromFDImpl(i,cback,byWhat);
        }
    }
    
    public void removeFD(int fd,Object byWhat) {
        if (cbackForFd_[fd]==null) {
            return;
        }
        
        do {
            CallbackNode cur=cbackForFd_[fd];
            cur.getCback().removed(byWhat);
            cbackForFd_[fd]=cur.next;
            cur.next=null;
            cur.prev=null;
        } while (cbackForFd_[fd]!=null);
        
        disableFD(fd);
    }
    
    protected void callIOSignalOnFd(int fd) throws PragmaNoPollcheck {
        //ovm.core.OVMBase.d("callIOSignalOnFd("+fd+")");
        CallbackNode cur=cbackForFd_[fd];
        //int cnt=0;
        while (cur!=null) {
            //ovm.core.OVMBase.d("Calling signal()...");
            if (cur.getCback().signal(false)) {
                //ovm.core.OVMBase.d("holding.");
                cur=cur.next;
            } else {
                //ovm.core.OVMBase.d("removing.");
                CallbackNode next=cur.next;
                cur.getCback().removed(Callback.BY_SIGNAL);
                removeCallbackImpl(cur);
                cur=next;
            }
            //++cnt;
        }
        /*if (cnt>1) {
            OVMBase.d("callIOSignalOnFd("+fd+"): called "+cnt+" cbacks.");
        }*/
        //OVMBase.d("Done with "+fd);
    }
    
    public static class CallbackNode {
        
        int fd;
        private Callback cback;
        void setCback(Callback c) throws PragmaNoBarriers, PragmaNoPollcheck {
            cback=c;
        }
        Callback getCback() throws PragmaNoBarriers, PragmaNoPollcheck {
            return cback;
        }
        
        CallbackNode prev=null;
        CallbackNode next=null;
        
        public CallbackNode(int fd,
                            Callback cback) {
            this.fd = fd;
            this.setCback(cback);
        }

    }
    
}

