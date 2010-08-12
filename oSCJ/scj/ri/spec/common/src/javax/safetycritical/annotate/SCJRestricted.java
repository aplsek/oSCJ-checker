package javax.safetycritical.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface SCJRestricted {
    public Phase[] value() default { Phase.ALL };
    public boolean mayAllocate()    default true;
    public boolean maySelfSuspend() default false;
}
