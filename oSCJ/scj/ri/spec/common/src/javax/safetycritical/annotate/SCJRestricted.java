package javax.safetycritical.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface SCJRestricted {
    public Restrict[] value() default { Restrict.MAY_ALLOCATE, Restrict.MAY_BLOCK, Restrict.ANY_TIME };
}
