package java.lang;

import static javax.safetycritical.annotate.Scope.CALLER;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@SCJAllowed(members=true)
public abstract class Enum<E extends Enum<E>> {
    protected Enum(String name, int ordinal) { }
    @Scope(IMMORTAL)
    @RunsIn(CALLER)
    public final String name() { return null; }
    @RunsIn(CALLER)
    public final int ordinal() { return 0; }
    @Override
    @RunsIn(CALLER)
    public final boolean equals(Object other) { return false; }
    @Override
    @RunsIn(CALLER)
    public final int hashCode() { return 0; }
    @Override
    //@Scope(IMMORTAL)
    @RunsIn(CALLER)
    protected final Object clone() { return null; }
    @RunsIn(CALLER)
    public final int compareTo(E o) { return 0; }
    @RunsIn(CALLER)
    public final Class<E> getDeclaringClass() { return null; }
    @Scope(IMMORTAL)
    @RunsIn(CALLER)
    public static <T extends Enum<T>> T valueOf(Class<T> enumType,
            String name) { return null; }
}
