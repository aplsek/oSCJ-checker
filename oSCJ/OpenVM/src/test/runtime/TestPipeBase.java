// $Header: /p/sss/cvs/OpenVM/src/test/runtime/TestPipeBase.java,v 1.22 2004/10/10 03:01:18 pizlofj Exp $

package test.runtime;

import s3.services.threads.JLThread;
import test.common.TestSuite;
import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;

/**
 *
 * @author Filip Pizlo
 */
// FIXME: this needs major refactoring.
public abstract class TestPipeBase extends TestSyncBase {
    
    protected static final class StatusBits {
        public boolean done=false;
    }
    
    boolean doThrow;
    
    public TestPipeBase(String name, long disabled) {
        super(name);
        doThrow = (disabled & TestSuite.DISABLE_EXCEPTIONS) == 0;
    }
    
    protected boolean needsIsolation() { return true; }
    
    protected abstract void createPipe(int[] pipe);
    
    protected void writeCompletely(int fd,byte[] buf) {
        int offset=0;
        while (offset<buf.length) {
            int res=LibraryImports.write(fd,buf,offset,buf.length-offset,true);
            if (res==0) {
                COREfail("write() returned 0, so no progress was made.  "+
                         "This means that blocking IO is not working.");
            }
            if (res<0) {
                check_err(false,"Calling LibraryImports.write() in writeCompletely()");
            }
            offset+=res;
        }
    }
    
    protected void readCompletely(int fd,byte[] buf) {
        int offset=0;
        while (offset<buf.length) {
            int res=LibraryImports.read(fd,buf,offset,buf.length-offset,true);
            if (res==0) {
                COREfail("read() returned 0, so no progress was made.  "+
                         "This means that blocking IO is not working.");
            }
            if (res<0) {
                check_err(false,"Calling LibraryImports.read() in readCompletely()");
            }
            offset+=res;
        }
    }
    
    protected boolean byteArrEqual(byte[] one,
                                   byte[] two) {
        if (one.length!=two.length) {
            return false;
        }
        for (int i=0;i<one.length;++i) {
            if (one[i]!=two[i]) {
                return false;
            }
        }
        return true;
    }
    
    public void testBasic(int[] pipe) {
        setModule("testBasic");
        
        // first test to see if the pipe works at all.
        final byte[] hello=new byte[]{'h', 'e', 'l', 'l', 'o'};
        writeCompletely(pipe[1],hello);
        
        byte[] received=new byte[hello.length];
        readCompletely(pipe[0],received);
        
        check_condition(byteArrEqual(hello,received),"Did not receive \'hello\'");
    }
    
    public void testOneThreadOnePendingOneDescriptor(final int[] pipe) {
        setModule("testOneThreadOnePendingOneDescriptor");
        
        final byte[] hello=new byte[]{'h', 'e', 'l', 'l', 'o'};

        // now do a test using threads
        final StatusBits bits=new StatusBits();
        
        // reading thread
        new JLThread() {
            public void run() {
                try {
                    byte[] received=new byte[hello.length];
                    readCompletely(pipe[0],received);
                    
                    check_condition(byteArrEqual(hello,received),
                               "Did not receive \'hello\'");
                    
                } catch (Throwable e) {
		    COREfail("Error in thread: "+e);
                } finally {
                    bits.done=true;
                }
            }
        }.start();
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }
        
        if (bits.done) {
	    COREfail("Other thread thinks it already received \'hello\', "+
		     "but we haven't sent it yet!");
        }
        
        writeCompletely(pipe[1],hello);
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }
        
        if (!bits.done) {
            COREfail("Other thread has not received \'hello\' after it has "+
                        "been sent and after we yielded to it.");
        }
    }
    
    private class ReadOneByteThread extends JLThread {
        private int[] pipe_;
        private StatusBits bits_;
        private byte[] possibleBytes_;
        private boolean[] byteSeen_;
        public ReadOneByteThread(int[] pipe,
                                 StatusBits bits,
                                 byte[] possibleBytes,
                                 boolean[] byteSeen) {
            this.pipe_=pipe;
            this.bits_=bits;
            this.possibleBytes_=possibleBytes;
            this.byteSeen_=byteSeen;
        }
        public void run() {
            try {
                byte[] received=new byte[1];
                readCompletely(pipe_[0],received);
                for (int i=0;i<possibleBytes_.length;++i) {
                    if (possibleBytes_[i]==received[0]) {
                        byteSeen_[i]=true;
                        return;
                    }
                }
                COREfail("Did not receive any of the expected bytes.");
            } catch (Throwable e) {
		COREfail("Error in thread: "+e);
            } finally {
                bits_.done=true;
            }
        }
    }
    
    public void testNThreadsNPendingOneDescriptor(int[] pipe,int n) {
        setModule("testNThreadsNPendingOneDescriptor");

        final byte[] possibleBytes=new byte[n];
        final boolean[] byteSeen=new boolean[n];
        final StatusBits[] bits=new StatusBits[n];
        for (int i=0;i<n;++i) {
            possibleBytes[i]=(byte)('0'+i);
            byteSeen[i]=false;
            bits[i]=new StatusBits();
        }
        
        for (int i=0;i<n;++i) {
            new ReadOneByteThread(pipe,bits[i],possibleBytes,byteSeen).start();
        }
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }
        
        for (int i=0;i<n;++i) {
            if (bits[i].done) {
		COREfail("At least one thread thinks it received something, when "+
			"we haven't sent anything.");
            }
            
            if (byteSeen[i]) {
                COREfail("At least one thread thinks it received something, when "+
                         "we haven't sent anything.");
            }
        }
        
        writeCompletely(pipe[1],possibleBytes);
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }
        
        for (int i=0;i<n;++i) {
            if (!bits[i].done) {
                COREfail("One or more of the threads didn't receive their "+
                         "bytes.");
            }
            if (!byteSeen[i]) {
                COREfail("At least one of the bytes that we sent was not "+
                         "received.");
            }
        }
    }
    
    public void testThroughput(final int[] pipe) {
        setModule("testThroughput");
        
        final byte[] hello=new byte[128];
        for (int i=0;i<hello.length;++i) {
            hello[i]=(byte)i;
        }

        // now do a test using threads
        final StatusBits bits=new StatusBits();
        
        // count of how many times we receive hello
        final int count=1000;
        
        // time when we started writing
        long beginWrite;
        
        // time when we started reader
        final long[] endRead=new long[1];
        
        // reading thread
        new JLThread() {
            public void run() {
                try {
                    byte[] received=new byte[hello.length];
                    
                    for (int i=0;i<count;++i) {
                        readCompletely(pipe[0],received);
                        
                        check_condition(byteArrEqual(hello,received),
                                "Did not receive \'hello\'");
                    }
                    
                    endRead[0]=Native.getCurrentTime();
                    
                } catch (Throwable e) {
		    COREfail("Error in thread: "+e);
                } finally {
                    bits.done=true;
                }
            }
        }.start();
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }
        
        if (bits.done) {
	    COREfail("Other thread thinks it already received \'hello\', "+
			"but we haven't sent it yet!");
        }
        
        beginWrite=Native.getCurrentTime();
        
        for (int i=0;i<count;++i) {
            writeCompletely(pipe[1],hello);
        }
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }
        
        if (bits.done) {
            p("Throughput: "+((count*hello.length)/
                              ((endRead[0]-beginWrite)/1000000000.0))
              +" bytes/sec\n");
        } else {
            COREfail("Other thread has not received \'hello\' after it has "+
                        "been sent and after we yielded to it.");
        }
    }
    
    public void testCancel(final int[] pipe) {
        setModule("testCancel");

        // now do a test using threads
        final StatusBits bits=new StatusBits();
        
        // reading thread
        new JLThread() {
            public void run() {
                try {
                    byte[] received=new byte[1];
                    
                    int result=LibraryImports.read(pipe[0],received,0,1,true);
                    int errno=LibraryImports.getErrno();
                    
                    check_condition(result==-1,
				    "Result does not equal -1 in read()");
                    
                    check_condition(errno==NativeConstants.ECANCELED,
				    "Errno does not equal ECANCELED in read()");
                    
                } catch (Throwable e) {
		    COREfail("Error in thread: "+e);
                } finally {
                    bits.done=true;
                }
            }
        }.start();
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }

        if (bits.done) {
	    COREfail("Other thread thinks it has already been interrupted, "+
                     "but we haven't interrupted it yet!");
        }
        
        check_condition(LibraryImports.cancel(pipe[0])==0,
			"cancel() failed to return 0");
        
        try {
            JLThread.sleep(1000);
        } catch (InterruptedException e) {
            p("Interrupted!  (Didn't expect that...)\n");
        }
        
        if (!bits.done) {
            COREfail("Other thread has not acknowledged the interrupt after we "+
                     "interrupted it and waited for 1 second.");
        }
    }
    
    public void testEBADF(int fd) {
        setModule("testEBADF");
        byte[] dummy=new byte[1];
	check_condition(LibraryImports.read(fd,dummy,0,1,true)==-1,
			"read() did not fail.");
	check_condition(LibraryImports.getErrno()==NativeConstants.EBADF,
			"read() failed, but not with EBADF.");
	check_condition(LibraryImports.write(fd,dummy,0,1,true)==-1,
			"write() did not fail.");
	check_condition(LibraryImports.getErrno()==NativeConstants.EBADF,
			"read() failed, but not with EBADF.");
    }
    
    public void run() {
        if (dispatcher == null ||
            !(dispatcher.getCurrentThread() instanceof JLThread)) {
            p(" SKIPPED: not working with JLThreads");
            return;
        }
        if (!doThrow) {
            p(" SKIPPED: requires exceptions");
            return;
        }
        
        final int[] pipe=new int[2];
	
	//d("Creating pair of file descriptors...");
        createPipe(pipe);
	//d("File descriptor pair created.  Running tests...");
        try {
            
            // the reason why testCancel() is being called multiple times is that
            // we wish to see if using the cancelation feature impacts the ability
            // of the other tests to pass.
            
            // a possible FIXME is to have two versions of this test: one that does
            // testCancel() and the other than doesn't, since testCancel() could
            // conceivably help the tests pass by resetting the IOSignalManager's
            // link lists.
            
            testCancel(pipe);
            testBasic(pipe);
            testCancel(pipe);
            testOneThreadOnePendingOneDescriptor(pipe);
            testNThreadsNPendingOneDescriptor(pipe,2);
            testNThreadsNPendingOneDescriptor(pipe,10);
            testCancel(pipe);
            testThroughput(pipe);
            testCancel(pipe);
        
        } finally {
            LibraryImports.close(pipe[0]);
            LibraryImports.close(pipe[1]);
        }
	
	//d("running EBADF tests...");
	testEBADF(pipe[0]);
	testEBADF(pipe[1]);
	//d("ok.");
    }
}

