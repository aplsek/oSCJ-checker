package java.lang.annotation;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
public enum RetentionPolicy {
    CLASS, RUNTIME, SOURCE
}
