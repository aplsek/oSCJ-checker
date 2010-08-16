
package javax.realtime; import
javax.safetycritical.annotate.SCJAllowed;

  /**
   * Note:
   */
@SCJAllowed public interface EventExaminer {
  /**
   *
   */
  @SCJAllowed
  Object visit(AsyncEvent ae);
}
