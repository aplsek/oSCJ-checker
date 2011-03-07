package checkers.scjRestricted;

import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

public class SCJRestrictedChecker extends SinglePassChecker {
    // Please keep alphabetized
    public static final String ERR_ILLEGAL_ALLOCATION = "illegal.allocation";
    public static final String ERR_ILLEGAL_FOREACH = "illegal.foreach";
    public static final String ERR_ILLEGAL_METHOD_CALL = "illegal.method.call";
    public static final String ERR_ILLEGAL_OVERRIDE = "illegal.override";
    public static final String ERR_ILLEGAL_NO_OVERRIDE = "illegal.no.override";
    public static final String ERR_TOO_MANY_VALUES = "too.many.values";

    @Override
	protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree tree) {
		//System.out.println(tree);
		return new SCJRestrictedVisitor<Void, Void>(this, tree);
	}

	@Override
	public Properties getMessages() {
		Properties p = new Properties();
		// Please keep alphabetized
        p.put(ERR_ILLEGAL_ALLOCATION,
                "Illegal allocation in a method marked ALLOCATE_FREE");
        p.put(ERR_ILLEGAL_FOREACH,
                "For each loops not allowed in ALLOCATE_FREE methods");
        p.put(ERR_ILLEGAL_METHOD_CALL,
                "Illegal invocation of a method annotated %s from within a method annotated %s");
        p.put(ERR_ILLEGAL_OVERRIDE, "May not override %s with %s");
        p.put(ERR_ILLEGAL_NO_OVERRIDE,
                "The inherited/overridden annotations must be restate. The overriding method is missing the @SCJRestricted annotation while the overriden method has one.");
        p.put(ERR_TOO_MANY_VALUES,
                "@SCJRestricted annotation may only contain one of: %s");
		return p;
	}
}
