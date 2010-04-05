package javax.safetycritical.annotate;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.CONSTRUCTOR;

/**
 * This annotation distinguishes constructors that must be present in
 * the public API because of the inheritance hierarchy under which
 * certain classes of javax.safetycritical are defined as subclasses
 * of existing infrastructure.  Marking a constructor as
 * SCJProtected instead of as SCJAllowed indicates that the
 * constructor may only be called from infrastructure code and shall
 * not be called directly from user code.
 */
@Retention(CLASS)
@Target({CONSTRUCTOR, METHOD})
public @interface SCJProtected
{
  public Level value() default Level.LEVEL_0;
}




