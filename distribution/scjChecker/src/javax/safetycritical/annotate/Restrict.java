package javax.safetycritical.annotate;

public enum Restrict {
    ALLOCATE_FREE, MAY_ALLOCATE,
    BLOCK_FREE, MAY_BLOCK,
    ANY_TIME, INITIALIZATION, EXECUTION, CLEANUP
}
