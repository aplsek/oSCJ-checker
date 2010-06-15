// $Header: /p/sss/cvs/OpenVM/src/test/common/ForkingHarnessImpl.java,v 1.3 2004/03/16 08:36:31 pizlofj Exp $

package test.common;

import ovm.core.execution.*;
import ovm.core.*;
import ovm.core.services.process.*;

/**
 * A <code>Harness</code> implementation that provides isolation while still
 * being able to run all tests.  It implements this by forking a new process for
 * each test.
 *
 * @author Filip Pizlo
 */
public class ForkingHarnessImpl extends HarnessImplBase {
    private static byte[] MAGIC =
        {'O', 'V', 'M', ' ', 'T', 'E', 'S', 'T', ' ', 'M', 'A', 'G', 'I', 'C'};
    
    void bailSafely(int fd,boolean ok) {
        byte[] holder=new byte[1];
        for (int i=0;
             i<MAGIC.length;
             ++i) {
            holder[0]=MAGIC[i];
            if (Native.write(fd, holder, 1)!=1) {
                break;
            }
        }
        Native._exit(ok?0:1);
    }
    
    boolean didBailSafely(int fd) {
        byte[] holder=new byte[1];
        for (int i=0;
             i<MAGIC.length;
             ++i) {
            int res=Native.read(fd, holder, 1);
            if (res==1) {
                if (holder[0]!=MAGIC[i]) {
                    printError("Sub-process wrote wrong data into pipe.  This probably "+
                               "means that some I/O code hijacked the pipe's file "+
                               "descriptor.");
                    return false;
                }
            } else if (res==0) {
                // if i==0 then the process did not bail safely, but without doing anything
                // else that is stupid.
                if (i!=0) {
                    printError("Sub-process wrote some, but not all, of the data into the "+
                               "pipe.  This may mean that some I/O code hijacked the pipe's "+
                               "file descriptor.  It could also mean that the VM got so badly "+
                               "messed up that it couldn't even complete the trivial write "+
                               "loop.");
                }
                return false;
            } else if (res==-1) {
                if (Native.getErrno() == NativeConstants.EINTR) {
                    continue;
                }
                if (Native.getErrno() == NativeConstants.EWOULDBLOCK ||
                    Native.getErrno() == NativeConstants.EAGAIN) {
                    Executive.panic("got EAGAIN reading the pipe although opposite end "+
                                    "should have been closed.");
                }
                Executive.panicOnErrno("error reading pipe",Native.getErrno());
            } else {
                Executive.panic("Weird error return from read (in loop)");
            }
        }
        
        for (;;) {
            int res=Native.read(fd,holder,1);
            if (res==1) {
                printError("Sub-process wrote too much data into pipe.  This probably "+
                           "means that some I/O code hijacked the pipe's file "+
                           "descriptor.");
                return false;
            } else if (res==0) {
                // ok.  this means that the whole magic string got written.
                return true;
            } else if (res==-1) {
                if (Native.getErrno() == NativeConstants.EINTR) {
                    continue;
                }
                if (Native.getErrno() == NativeConstants.EWOULDBLOCK ||
                    Native.getErrno() == NativeConstants.EAGAIN) {
                    Executive.panic("got EAGAIN reading the pipe although opposite end "+
                                    "should have been closed.");
                }
                Executive.panicOnErrno("error reading pipe",Native.getErrno());
            } else {
                Executive.panic("Weird error return from read (after loop)");
            }
        }
    }
    
    class FailureCallbackImpl implements FailureCallback {
        int fd;
        FailureCallbackImpl(int fd) {
            this.fd=fd;
        }
        public void fail(String test,
                         String module,
                         String description) {
            printFailure(test, module, description);
            bailSafely(fd,false);
        }
    }
    
    public ForkingHarnessImpl(String domain) {
        super(domain);
    }
    
    public void run(Test t) {
        printBegin(t.getName());
        
        int[] pipe=new int[2];
        
        if (Native.pipe(pipe)<0) {
            throw Executive.panicOnErrno("Could not create pipe",
                                         Native.getErrno());
        }
        
        if (Native.makeNonBlocking(pipe[0])<0) {
            throw Executive.panicOnErrno("Could not make reading end non-blocking.",
                                         Native.getErrno());
        }
        
        if (Native.makeNonBlocking(pipe[1])<0) {
            throw Executive.panicOnErrno("Could not make writing end non-blocking.",
                                         Native.getErrno());
        }
        
        int res=ForkManager.fork();
        
        if (res<0) {
            throw Executive.panicOnErrno("Could not fork",
                                         Native.getErrno());
        } else if (res==0) {
            // WE ARE IN CHILD PROCESS NOW
            Native.close(pipe[0]);

            try {
                t.runTest(new FailureCallbackImpl(pipe[1]),
                          true);
                
                bailSafely(pipe[1],true);
            } catch (Throwable e) {
                printException();
                
                e.printStackTrace();
                
                bailSafely(pipe[1],false);
            } finally {
                // just in case
                Native._exit(1);
            }
        }
        
        Native.close(pipe[1]);
        
        // ok.  here we first must do a waitpid.  then we read from the pipe.  if
        // the data on the pipe is our magic string, then that means we exited via
        // bailSafely.  If so, then we check the exit code.  If it is 0 then all
        // is fine; otherwise it isn't.  if we don't see the magic string, then we
        // log an error saying that the test exited prematurely, and we give details
        // about the nature of the exit.  if there was data on the pipe but it was
        // not the magic string, then we also log an error saying that the test
        // wrote to a pipe that it wasn't supposed to write to.
        
        int[] status=new int[1];
        
        for (;;) {
            if (Native.waitpid(res,status,0)<0) {
                if (Native.getErrno()
                    == NativeConstants.EINTR) {
                    continue;
                }
                throw Executive.panicOnErrno("waitpid failed",
                                             Native.getErrno());
            }
            break;
        }
        
        boolean bailedSafely=didBailSafely(pipe[0]);
        Native.close(pipe[0]);
        
        boolean error=true;

        if (bailedSafely) {
            if (Native.WIFEXITED(status[0])!=0) {
                if (Native.WEXITSTATUS(status[0])==0) {
                    // all good!
                    error=false;
                } else if (Native.WEXITSTATUS(status[0])==1) {
                    // error!
                    // but at least the sub-process already reported it.
                } else {
                    printError("After bailing out, sub-process exited with exit code: "+
                               Native.WEXITSTATUS(status[0]));
                }
            } else if (Native.WIFSIGNALED(status[0])!=0) {
                printError("After bailing out, sub-process was killed by signal "+
                           Native.WTERMSIG(status[0])+
                           (Native.WCOREDUMP(status[0])!=0?
                            " (and a core file was generated)":""));
            } else {
                printError("After bailing out, sub-process exited with unrecognized "+
                           "status: "+status[0]);
            }
        } else {
            if (Native.WIFEXITED(status[0])!=0) {
                if (Native.WEXITSTATUS(status[0])==0) {
                    printError("Sub-process exited claiming success before returning "+
                               "from runTest().");
                } else {
                    printError("Sub-process exited with exit code: "+
                               Native.WEXITSTATUS(status[0]));
                }
            } else if (Native.WIFSIGNALED(status[0])!=0) {
                printError("Sub-process was killed by signal "+
                           Native.WTERMSIG(status[0])+
                           (Native.WCOREDUMP(status[0])!=0?
                            " (and a core file was generated)":""));
            } else {
                printError("Sub-process exited with unrecognized status: "+status[0]);
            }
        }
        
        printEnd(t.getName(),!error);
        
        if (error) {
            allGood=false;
        }
    }
}

