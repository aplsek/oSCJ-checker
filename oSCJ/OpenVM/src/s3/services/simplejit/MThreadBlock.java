package s3.services.simplejit;

public class MThreadBlock {

    int stack_top;          /* pointer to top of the call stack 
			    (actually lower address end) */
    int redzone;            /* stack_top + MTHREAD_ROOM_FOR_STACK_OVERFLOW_PROCESSING */
    int stack_size;            /* the call stack size (in bytes) */
    int sp;                    /* saved stack pointer */
    NativeContext ctx;         /* pointer to the corresponding native context */
    int bottom_frame_pointer;  /* pointer to the frame pointer (ebp) of the initial frame */
}
