/*
 * The mthread header
 * @author Hiroshi Yamauchi
 */
#ifndef MTHREAD
#define MTHREAD

/* 
 * If this flag is defined, the interpreter runs using mthreads
 */
#define MTHREAD_MINIMUM_STACK_SIZE   (32 * 1024)
#define MTHREAD_DEFAULT_STACK_SIZE   (128 * 1024)

/*
 * The stack pointer of the main process which initially starts
 * mthreading. This is the point to which control may want to go after
 * all mthreads terminate.
 */
extern int main_sp;

/*
 * A mthread control block
 */
struct mthread_block {
    int* stack_bottom;     /* pointer to the bottom of the call stack */
    int  stack_size;       /* the call stack size (in bytes) */
    int* sp;               /* saved stack pointer */
    void* ctx;             /* pointer to the corresponding native context */
};

/*
 * Create a mthread block
 *
 * @param entry_point the entry (main) function for the thread
 * @param stack_size the desired stack size in bytes
 * @param arg the argument for entry_point
 */
struct mthread_block * create_mthread(void* entry_point,
				      int stack_size,
				      void* arg);

/*
 * Free memory associated with an mthread.  Probably shouldn't be
 * called on the current thread.
 */
void destroy_mthread(struct mthread_block *mtb);

/*
 * Context-switch
 * 
 * @param old_sp the stack pointer (esp) of the old thread
 * @param new_sp the stack pointer (esp) of the new thread
 */
void mthread_context_switch(void* old_sp, void* new_sp);



#endif /* MTHREAD */
