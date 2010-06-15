/*
 * The mthread header
 * @author Hiroshi Yamauchi
 */
#ifndef MTHREAD_H
#define MTHREAD_H

// in bytes
#define MTHREAD_MINIMUM_STACK_SIZE   (32 * 1024)
#define MTHREAD_DEFAULT_STACK_SIZE   (256 * 1024)
#define MTHREAD_ROOM_FOR_STACK_OVERFLOW_PROCESSING (32 * 1024)

/*
 * The stack pointer of the main process which initially starts
 * mthreading. This is the point to which control may want to go after
 * all mthreads terminate.
 */
extern int main_sp;

/*
 * Create a mthread block
 *
 * @param entry_point the entry (main) function for the thread
 * @param stack_size the desired stack size in bytes
 * @param arg the argument list for entry_point
 * 
 * Limitation: only integers and references are allowed in the argument list.
 * This is because the type information of the arguments are not available.
 */
struct s3_services_simplejit_MThreadBlock* 
    create_mthread(void* entry_point,
		   int stack_size,
		   int argWordSize,
		   void* arg[]);

/*
 * Return a suspended mthread's topmost frame pointer.  That is, the
 * frame pointer of the function that last called
 * mthread_context_switch.
 */
int mthread_top_frame(struct s3_services_simplejit_MThreadBlock* mtb);

/*
 * Free memory associated with an mthread.  Probably shouldn't be
 * called on the current thread.
 */
void destroy_mthread(struct s3_services_simplejit_MThreadBlock* mtb);

/*
 * Context-switch
 * 
 * @param old_sp the stack pointer (esp) of the old thread
 * @param new_sp the stack pointer (esp) of the new thread
 */
void mthread_context_switch(void* old_sp, void* new_sp);

#endif /* MTHREAD_H */
