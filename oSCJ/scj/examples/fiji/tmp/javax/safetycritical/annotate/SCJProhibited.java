package javax.safetycritical.annotate;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * 
 * 
 * @author plsek
 *
 */
@Retention(CLASS)
@Target( { CONSTRUCTOR, METHOD })
public @interface SCJProhibited {

}
