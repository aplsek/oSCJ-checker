
package java.lang;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.MemoryAreaEncloses;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

import static javax.safetycritical.annotate.Allocate.Area.CURRENT;
import static javax.safetycritical.annotate.Allocate.Area.THIS;

import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.LEVEL_2;

@SCJAllowed(LEVEL_0)
public class Thread implements Runnable {

  @SCJAllowed(LEVEL_2)
  public static interface UncaughtExceptionHandler {
    /**
     *@memory Allocates no memory. Does not allow implicit argument this, or explit
     *        arguments t and e to escape local variables.
     */
    @SCJAllowed(LEVEL_2)
    public void uncaughtException(Thread t, Throwable e);
  }

  /**
   * @memory  Allocates internal lock structure within scope of "this". Does not allow
   *          "this" to escape local variables.
   */
  @Allocate({THIS})
  @BlockFree
  public Thread() {
  }

  /**
   * @memory Allocates internal lock structure within scope of "this". Does not allow
   *         "this" to escape local variables. Requires that argument "name" reside in
   *         a scope that encloses the scope of "this".
   */
  @Allocate({THIS})
  @BlockFree
  public Thread(String name) {
  }

  /**
   * @memory Allocates no memory. The returned Thread reference may refer to scoped
   *         memory.
   */
  public static Thread currentThread() {
    return null;
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables. The
   *         result returned from this method may reside in scoped memory in some
   *         scope that encloses "this".
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
    return null;
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables. The
   *         return value may reside in scoped memory that encloses the scope of
   *         "this".
   */
  public String getName() {
    return null; // skeleton
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables. The
   *         result returned from this method may reside in scoped memory in some
   *         scope that encloses "this".
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public static UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return null;
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public void interrupt() {
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public static boolean interrupted() {
    return false; // skeleton
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public final boolean isAlive() {
    return false; // skeleton
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public boolean isDaemon() {
    return false; // skeleton
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public boolean isInterrupted() {
    return false;
  }
  
  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed(LEVEL_2)
    public final void join() throws InterruptedException {
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed(LEVEL_2)
  public final void join(long millis) throws InterruptedException {
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables.
   */
  @SCJAllowed(LEVEL_2)
  public final void join(long millis, int nanos) throws InterruptedException {
  }

  /**
   * @memory Does not allow "this" to escape local variables.
   */
  @SCJProtected
  public void run() {
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables. The
   * eh argument must reside in immortal memory.
   */
  @BlockFree
  @MemoryAreaEncloses(inner = {"@immortal"}, outer = {"eh"})
  @SCJAllowed(LEVEL_2)
  public static void setDefaultUncaughtExceptionHandler(
    UncaughtExceptionHandler eh) {
  }

  /**
   * @memory Allocates no memory. Does not allow "this" to escape local variables. The
   * eh argument must reside in a scope that encloses the scope of "this".
   */
  @BlockFree
  @MemoryAreaEncloses(inner = {"this"}, outer = {"eh"})
  @SCJAllowed(LEVEL_2)
  public void setUncaughtExceptionHandler(
    UncaughtExceptionHandler eh) {
  }

  /**
   * @memory Allocates no memory.
   */
  /*@SCJAllowed(LEVEL_1)*/
  public static void sleep(long millis) throws InterruptedException {
  }

  /**
   * @memory Allocates no memory.
   */
  /* @SCJAllowed(LEVEL_1)*/
  public static void sleep(long millis, int nanos)
    throws InterruptedException {
  }

  /**
   * @memory Allocates no memory.
   */
  @BlockFree
  @SCJProtected
  @SCJAllowed(LEVEL_2)
  public void start() {
  }

  /**
   * @memory Does not allow "this" to escape local variables. Allocates a String and
   *         associated internal "structure" (e.g. char[]) in caller's scope.
   */
  @Allocate({CURRENT})
  @BlockFree
  @SCJAllowed
  public String toString() {
    return null; // skeleton
  }
  
  /**
   * @memory Allocates no memory.
   */
  @BlockFree
  @SCJAllowed(LEVEL_2)
  public static void yield() {
  }

}
