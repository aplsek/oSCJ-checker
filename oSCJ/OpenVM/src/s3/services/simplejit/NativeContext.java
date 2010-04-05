package s3.services.simplejit;

import ovm.core.execution.Context;

public class NativeContext {

    Context ovm_context;
    //void* current_frame; /* a dummy value */
    int bottom_frame;      /* pointer to the frame pointer */

    MThreadBlock mtb;

    /* for exception handling */
    int cut_to_frame_set;
    int cut_to_frame;
    int new_stack_depth;

    /* Used for reflective calls
     *
     * [0] = native code
     * [1] = code pointer
     * [2] = saved edi
     * [3] = saved esi
     * [4] = saved ebx
     * [5] = return address
     * [6] = arg length
     * [7] = arg word 0
     * [8] = arg word 1
     * ...
     */
    int reflection_invocation_buffer;

    /* for exception throwing from native methods */
    byte[] exceptionMsg;
    int   exinfo_type;

    /* flag that indicates we are handling stack overflow */
    int processing_stack_overflow;

}
