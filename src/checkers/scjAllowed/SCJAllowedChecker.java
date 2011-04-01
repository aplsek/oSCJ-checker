package checkers.scjAllowed;

import java.util.Properties;

import javax.annotation.processing.SupportedOptions;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

/**
 * When checking a SCJ application the implementation verifies that for any field selection(x.f),
 * methodinvocation(x.m(...)),newexpression(new T(...)), the declared compliance level of the field, method,
 * constructor, is smaller or equal to the compliance level of the expression. Casts ((T)), type tests (instanceof T),
 * extends clauses and uses of class objects (e.g. synchronized(T), or T.class) are allowed only if the level of the
 * class is smaller or equal to the level of the expression. The compliance level of an expression is the first of: 1.
 * If the expression is in the body of a conditional with a guard of the form if(Safelet.getDeploymentLevel()==n), and n
 * represents level 0, 1 or 2, then the compliance level of the expression is n. If there are several nested
 * conditionals, the closest is used. 2. If the expression occurs within the body of a method that is annotated
 *
 * @SCJAllowed(n) then compliance level is n. 3. If the expression occurs within the body of a class annotated
 * @SCJAllowed(n) or a class nested within a class annotated
 * @SCJAllowed(n), and the Members attribute of the annotation is set to true then the expression is n. If none of the
 *                 above holds, then the expression has no compliance level. The compliance level of a member (field,
 *                 method or nested class) is the first of: 1. If the member is annotated
 * @SCJAllowed(n), the compliance level of the member is n. 2. If the enclosing class is annotated
 * @SCJAllowed(n), and the Members attribute of the annotation is set to true, the the compliance level of the member is
 *                 n.
 * @author plsek, dtang
 */

@SupportedOptions("scjlevel")
public class SCJAllowedChecker extends SinglePassChecker {
    // Please keep alphabetized
    public static final String ERR_BAD_ENCLOSED = "scjallowed.badenclosed";
    public static final String ERR_BAD_FIELD_ACCESS = "scjallowed.badfieldaccess";
    public static final String ERR_BAD_INFRASTRUCTURE_CALL = "scjallowed.infrastructure.call";
    public static final String ERR_BAD_INFRASTRUCTURE_OVERRIDE = "scjallowed.badoverrideInfrastructure";
    public static final String ERR_BAD_METHOD_CALL = "scjallowed.bad.method.call";
    public static final String ERR_BAD_NEW_CALL = "scjallowed.bad.new.call";
    public static final String ERR_BAD_OVERRIDE = "scjallowed.bad.override";
    public static final String ERR_BAD_OVERRIDE_SUPPORT = "scjallowed.bad.override.support";
    public static final String ERR_BAD_SUBCLASS = "scjallowed.bad.subclass";
    public static final String ERR_BAD_SUPPORT = "scjallowed.bad.support";
    public static final String ERR_BAD_USER_LEVEL = "scjallowed.bad.user.level";

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree tree) {
        return new SCJAllowedVisitor<Void, Void>(this, tree);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        // Please keep alphabetized
        p.put(ERR_BAD_ENCLOSED,
                "Nested elements may not increase visibility of their outer elements.");
        p.put(ERR_BAD_FIELD_ACCESS,
                "Field access is not allowed at level %s.");
        p.put(ERR_BAD_INFRASTRUCTURE_CALL,
                "@SCJAllowed(INFRASTRUCTURE) methods may not be called outside of javax.realtime or javax.safetycritical packages.");
        p.put(ERR_BAD_INFRASTRUCTURE_OVERRIDE,
                "Method outside of SCJ packages can not override methods @SCJAllowed(INFRASTRUCTURE).");
        p.put(ERR_BAD_METHOD_CALL,
                "Method call is not allowed at level %s.");
        p.put(ERR_BAD_NEW_CALL,
                "Constructor call is not allowed at level %s.");
        p.put(ERR_BAD_OVERRIDE,
                "Method may not decrease visibility of their overrides.");
        p.put(ERR_BAD_OVERRIDE_SUPPORT,
            "Method may override @SCJAllowed(SUPPORT) method but must restate the annotation.");
        p.put(ERR_BAD_SUBCLASS,
                "Subclasses may not decrease visibility of their superclasses.");
        p.put(ERR_BAD_SUPPORT,
                "Methods outside of javax.realtime or javax.safetycritical packages cannot be annotated with @SCJAllowed(SUPPORT).");
        p.put(ERR_BAD_USER_LEVEL,
                "Elements outside of javax.realtime or javax.safetycritical packages cannot be annotated with @SCJAllowed(INFRASTRUCTURE).");

        return p;
    }
}
