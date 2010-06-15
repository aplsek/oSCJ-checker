
package s3.services.io.async;

import ovm.core.*;
import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.core.services.process.*;
import ovm.services.io.async.*;
import s3.util.*;

/**
 *
 * @author Filip Pizlo
 */
public class ForkingHostLookupManager
    extends AsyncIOManagerBase
    implements HostLookupManager {
    
    public static final boolean DEBUG=false;

    private static ForkingHostLookupManager instance_ =
	new ForkingHostLookupManager();
    public static HostLookupManager getInstance() {
        return instance_;
    }
    
    static byte[] bucket=new byte[1];
    static void writeCompletely(int fd,byte b) {
        bucket[0]=b;
        Native.write(fd,bucket,1);
    }
    static void writeCompletely(int fd,int i) {
        writeCompletely(fd,(byte)((i&0xff000000)>>24));
        writeCompletely(fd,(byte)((i&0x00ff0000)>>16));
        writeCompletely(fd,(byte)((i&0x0000ff00)>>8));
        writeCompletely(fd,(byte)(i&0x000000ff));
    }
    static void writeCompletely(int fd,byte[] arr) {
        writeCompletely(fd,arr,0,arr.length);
    }
    static void writeCompletely(int fd,byte[] arr,int offset,int length) {
        for (int i=0;i<length;++i) {
            bucket[0]=arr[i+offset];
            Native.write(fd,bucket,1);
        }
    }
    
    static int reconsInt(byte[] data,int offset) {
        return (data[offset+0]<<24)
             | (data[offset+1]<<16)
             | (data[offset+2]<<8)
             | data[offset+3];
    }
    
    static class MessageBase {
        IOException error;

        boolean parse(IOMemoryBuffer buf) {
            if (buf.getOffset()==0) {
                return false;
            }
            if (buf.getData()[0]=='e') {
                for (int i=1;
                     i<buf.getOffset();
                     ++i) {
                    if (buf.getData()[i]==0) {
                        error=IOException.fromCode(new String(buf.getData(),1,i-1));
                        return true;
                    }
                }
                return false;
            } else if (buf.getData()[0]=='s') {
                // do nothing; let the subclass handle it
            } else {
                error=IOException.Internal.getInstance(
                    "unrecognized code from child in "+
                    "ForkingHostLookupManager.MessageBase.parse()");
            }
            return true;
        }
    }
    
    static class ByAddrMessage extends MessageBase {
        byte[] res;
        
        boolean parse(IOMemoryBuffer buf) {
            if (!super.parse(buf)) {
                return false;
            }
            if (error!=null) {
                return true;
            }
            
            for (int i=1;i<buf.getOffset();++i) {
                if (buf.getData()[i]==0) {
                    res=new byte[i];
                    System.arraycopy(buf.getData(), 1,
                                     res, 0,
                                     i-1);
                    res[i-1]=0;
                    return true;
                }
            }
            
            return false;
        }
    }
    
    static class ByNameMessage extends MessageBase {
        HostLookupUtil.RawResult res;
        
        boolean parse(IOMemoryBuffer buf) {
            if (!super.parse(buf)) {
                return false;
            }
            if (error!=null) {
                return true;
            }
            
            if (res==null) {
                if (buf.getOffset()<9) {
                    return false;
                }
                int addrLen=reconsInt(buf.getData(),1);
                int numEntries=reconsInt(buf.getData(),5);
                res=new HostLookupUtil.RawResult(addrLen,numEntries,
                                                 new byte[addrLen*numEntries]);
            }
            
            if (buf.getOffset()<res.data.length+9) {
                return false;
            }
            
            System.arraycopy(buf.getData(), 9,
                             res.data, 0,
                             res.data.length);
            
            return true;
        }
    }
    
    private static interface InChildCallback {
        public void doStuff(int writePipe) throws IOException;
    }
    
    private static interface OnceDoneCallback {
        public AsyncFinalizer doStuff(MessageBase msg) throws IOException;
    }
    
    private AsyncHandle forkAndDo(final MessageBase msg,
				  final InChildCallback inChild,
				  final OnceDoneCallback onceDone,
				  final AsyncCallback cback) throws IOException {
        int[] pipe=new int[2];
        IOException.System.check(Native.mySocketpair(pipe),
                                 Native.getErrno());
        
        final int[] childPid=new int[1];
        
        // this part would probably be easier to code as a native call.
        boolean enabled=tm.setReschedulingEnabled(false);
        try {
            int res=ForkManager.fork();
            if (res<0) {
                Native.close(pipe[0]);
                Native.close(pipe[1]);
                throw IOException.System.make(-res);
            } else if (res==0) {
                // we are in child.  don't re-enabled scheduling.  just do the
                // host lookup and be happy.
                try {
                    Native.close(pipe[0]);
                    
                    inChild.doStuff(pipe[1]);
                } catch (IOException e) {
                    writeCompletely(pipe[1],(byte)'e');
                    writeCompletely(pipe[1],e.getCode().getBytes());
                    writeCompletely(pipe[1],(byte)0);
                } catch (Throwable e) {
                    // crap out
                    Native._exit(1);
                }
                Native._exit(0);
            } else {
                childPid[0]=res;
            }
        } finally {
            tm.setReschedulingEnabled(enabled);
        }
        
        Native.close(pipe[1]);

        final RWIODescriptor io=(RWIODescriptor)wrapifier.wrapNow(pipe[0]);
        
        final IOMemoryBuffer buf=new IOMemoryBuffer(1024);
        
        // the final finalizer that we pass on after the process dies.
        final AsyncFinalizer finfin[]=new AsyncFinalizer[1];
        
        final WaitManager.WaitCallback waiter[]=
	    new WaitManager.WaitCallback[1];

	final WaitManager.WaitCallback cancelWaiter[]=
	    new WaitManager.WaitCallback[1];
        
        final AsyncCallback reader[]=new AsyncCallback[1];
	
	final AsyncFinalizer.Delegate readerFin;
        
	final AsyncFinalizer closer=new AsyncFinalizer(){
		public boolean finish() {
		    buf.free();
		    io.close();
		    cback.ready(finfin[0]);
		    return false;
		}
		public IOException getError() {
		    throw Executive.panic("getError() called on a finalizer that never "+
					  "finishes!");
		}
	    };

	class MyAsyncHandle implements AsyncHandle {
	    boolean done = false;
	    boolean canceled = false;
	    
	    public boolean canCancelQuickly() {
		return true;
	    }
	    
	    public synchronized void cancel(IOException error) 
		throws PragmaAtomic {
		if (canceled || done) {
		    return;
		}
		
		io.cancel(IOException.Canceled.getInstance());
		WaitManager.removeWaitCallback(childPid[0],
					       waiter[0]);
		
		WaitManager.waitForPid(childPid[0],
				       WaitManager.noOpCallback);
		
		canceled = true;
		
		finfin[0]=AsyncFinalizer.Error.make(error);
		cback.ready(closer);
	    }
	}
	
	final MyAsyncHandle asyncHandle=new MyAsyncHandle();

        waiter[0]=new WaitManager.WaitCallback(){
		public void died(int pid,int status) {
		    // NOTE: scheduling is disabled here.
		    
		    if (DEBUG) {
			Native.print_string("in died()\n");
		    }
		    
		    asyncHandle.done = true;
		    
		    if (Native.WIFEXITED(status)==0 ||
			Native.WEXITSTATUS(status)!=0) {
			if (DEBUG) {
			    Native.print_string("erroroneous exit\n");
			}
			finfin[0]=AsyncFinalizer.Error
			    .make(IOException.Internal
				  .getInstance("bad exit status from child in "+
					       "ForkingHostLookupManager."+
					       "forkAndDo()"));
		    }
		    
		    cback.ready(closer);
		}
	    };

	readerFin=new AsyncFinalizer.Delegate(){
		void registerWaiter() {
		    if (DEBUG) {
			Native.print_string("in registerWaiter()\n");
		    }
		    synchronized (asyncHandle) {
			if (DEBUG) {
			    Native.print_string("holding lock\n");
			}
			if (!asyncHandle.canceled) {
			    if (DEBUG) {
				Native.print_string("calling waitForPid()\n");
			    }
			    WaitManager.waitForPid(childPid[0],waiter[0]);
			    if (DEBUG) {
				Native.print_string("waitForPid() returned.\n");
			    }
			}
		    }
		}
		public boolean finish() {
		    VM_Area prev=U.e(cback);
		    try {
			if (DEBUG) {
			    Native.print_string("in finish()\n");
			}
			if (!getTarget().finish()) {
			    return false;
			}
			IOException error=getTarget().getError();
			if (error instanceof IOException.Canceled) {
			    if (DEBUG) {
				Native.print_string("canceled!\n");
			    }
			    throw Executive.panic("Got canceled but caught it "+
						  "too late");
			} else if (error!=null) {
			    if (DEBUG) {
				Native.print_string("non-null error!\n");
			    }
			    finfin[0]=getTarget();
			    registerWaiter();
			    return false;
			}
			
			int numBytes=
			    ((RWIODescriptor.ReadFinalizer)getTarget())
			    .getNumBytes();
			
			if (DEBUG) {
			    Native.print_string("numBytes = "+numBytes+"\n");
			}
			
			if (numBytes==0) {
			    if (DEBUG) {
				Native.print_string("bailing out...\n");
			    }
			    finfin[0]=AsyncFinalizer.Error
				.make(IOException.Internal
				      .getInstance("unexpected EOF in "+
						   "ForkingHostLookupManager."+
						   "forkAndDo()"));
			    registerWaiter();
			    return false;
			}
			
			buf.incOffset(numBytes);
			
			if (DEBUG) {
			    Native.print_string("parsing...\n");
			}
			if (msg.parse(buf)) {
			    if (DEBUG) {
				Native.print_string("done parsing!\n");
			    }
			    try {
				if (msg.error!=null) {
				    if (DEBUG) {
					Native.print_string("processing "+
							    "error\n");
				    }
				    finfin[0]=AsyncFinalizer
					.Error.make(msg.error);
				} else {
				    if (DEBUG) {
					Native.print_string("really done!\n");
				    }
				    finfin[0]=onceDone.doStuff(msg);
				}
				if (DEBUG) {
				    Native.print_string("made the finalizer\n");
				}
			    } catch (IOException e) {
				if (DEBUG) {
				    Native.print_string("got an IOException\n");
				}
				finfin[0]=AsyncFinalizer.Error.make(e);
			    }
			    
			    if (DEBUG) {
				Native.print_string("registering waiter\n");
			    }
			    registerWaiter();
			    return false;
			}
			
			if (buf.getRemaining()==0) {
			    if (DEBUG) {
				Native.print_string("growing buffer\n");
			    }
			    buf.grow(1024);
			}
			
			synchronized (asyncHandle) {
			    if (DEBUG) {
				Native.print_string("reading more\n");
			    }
			    if (!asyncHandle.canceled) {
				io.read(buf,buf.getRemaining(),reader[0]);
			    }
			}
			return false;
		    } catch (Throwable e) {
			throw Executive.panicOnException
			    (e,
			     "in ForkingHostLookupManager.finish()");
		    } finally {
			U.l(prev);
		    }
		}
	    };
	
        reader[0]=new AsyncCallback.Delegate(cback){
		public void ready(final AsyncFinalizer fin) {
		    if (asyncHandle.canceled) {
			return;
		    }
		    readerFin.setTarget(fin);
		    cback.ready(readerFin);
		}
	    };
		
        io.read(buf,buf.getSize(),reader[0]);

	return asyncHandle;
    }
    
    public AsyncHandle getHostByName(final byte[] name,
				     final AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return forkAndDo(new ByNameMessage(),
			     new InChildCallback(){
				 public void doStuff(int pipe) throws IOException {
				     HostLookupUtil.RawResult rawres=
					 HostLookupUtil.getHostByNameRaw(name);
				     writeCompletely(pipe,(byte)'s');
				     writeCompletely(pipe,rawres.addrLen);
				     writeCompletely(pipe,rawres.numEntries);
				     writeCompletely(pipe,rawres.data,0,
						     rawres.addrLen*rawres.numEntries);
				 }
			     },
			     new OnceDoneCallback(){
				 public AsyncFinalizer doStuff(MessageBase msg)
				     throws IOException {
				     return new SimpleGetHostByNameFinalizer
					 (HostLookupUtil
					  .fromRawResult(((ByNameMessage)msg).res));
				 }
			     },
			     cback);
        } catch (IOException e) {
            cback.ready(AsyncFinalizer.Error.make(e));
        } catch (ClassCastException e) {
            cback.ready(AsyncFinalizer.Error.make(
                IOException.Internal.getInstance(
                    "class cast exception in "+
                    "ForkingHostLookupManager.getHostByName()")));
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }
    
    public AsyncHandle getHostByAddr(final InetAddress addr,
				     final AsyncCallback cback) {
        VM_Area prev=U.e(cback);
        try {
            return forkAndDo(new ByAddrMessage(),
			     new InChildCallback(){
				 public void doStuff(int pipe) throws IOException {
				     byte[] hostname=HostLookupUtil.getHostByAddrNow(addr);
				     writeCompletely(pipe,(byte)'s');
				     writeCompletely(pipe,hostname);
				 }
			     },
			     new OnceDoneCallback(){
				 public AsyncFinalizer doStuff(MessageBase msg) {
				     return new SimpleGetHostByAddrFinalizer
					 (((ByAddrMessage)msg).res);
				 }
			     },
			     cback);
        } catch (IOException e) {
            cback.ready(AsyncFinalizer.Error.make(e));
        } catch (ClassCastException e) {
            cback.ready(AsyncFinalizer.Error.make(
                IOException.Internal.getInstance(
                    "class cast exception in "+
                    "ForkingHostLookupManager.getHostByAddr()")));
        } finally {
            U.l(prev);
        }
	return StallingUtil.asyncHandle;
    }
}

