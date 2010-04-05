package s3.core.execution;

import ovm.core.execution.Native;

/**
 * This class defines the 'bottom' frame (the frame
 * under Java's catch-all frame) for S3/OVM.
 * The code should never be reached, but if it is, 
 * the VM is halted.
 *
 * @author Christian Grothoff
 **/
public class S3BottomFrame {

    public S3BottomFrame() {
    }

    /**
     * Do NOT do anything else in this method at the moment.
     * There is NO *this* pointer, there is NO constant
     * pool. You can NOT access statics either! Even a different
     * exit code can mekt trouble!
     * <pre>
     * Method void bottomFrame()
     * 0 iconst_0
     * 1 istore_1
     * 2 iinc 1 1
     * 5 jsr 17
     * 8 goto 24
     * 11 astore_2
     * 12 jsr 17
     * 15 aload_2
     * 16 athrow
     * 17 astore_3
     * 18 iconst_m1
     * 19 INVOKE_SYSTEM [PROCESS_EXIT, 0]
     * 22 ret 3
     * 24 return
     * Exception table:
     * from   to  target type
     * 0    11    11   any
     * </pre>
     **/
    public void bottomFrame() {
	try {
	    // code: ICONST 0, ISTORE_1, IINC 1, 1 [jsr, ...]
	    int i = 0;
	    i++; /* put some stupid bytecode */
	}  finally {
	    Native.abort();
	}
    }

} // end of S3BottomFrame
