package checkers.scjRestricted;

import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

@SuppressWarnings("restriction")
public class SCJRestrictedChecker extends SinglePassChecker {
    // Please keep alphabetized
    public static final String ERR_ILLEGAL_OVERRIDE = "illegal.override";
    public static final String ERR_ILLEGAL_OVERRIDE1 = "illegal.override1";
    public static final String ERR_TOO_MANY_VALUES = "too.many.values";
    public static final String ERR_UNALLOWED_ALLOCATION = "unallowed.allocation";
    public static final String ERR_UNALLOWED_FOREACH = "unallowed.foreach";
    public static final String ERR_UNALLOWED_METHODCALL = "unallowed.methodcall";

    @Override
	protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree tree) {
		//System.out.println(tree);
		return new SCJRestrictedVisitor<Void, Void>(this, tree);
	}

	@Override
	public Properties getMessages() {
		Properties p = new Properties();
		// Please keep alphabetized
        p.put(ERR_ILLEGAL_OVERRIDE, "May not override %s with %s");
        p.put(ERR_ILLEGAL_OVERRIDE1,
                "The inherited/overridden annotations must be restate. The overriding method is missing the @SCJRestricted annotation while the overriden method has one.");
        p.put(ERR_TOO_MANY_VALUES,
                "@SCJRestricted annotation may only contain one of: %s");
        p.put(ERR_UNALLOWED_ALLOCATION,
                "Illegal allocation in a method marked ALLOCATE_FREE");
        p.put(ERR_UNALLOWED_FOREACH,
                "For each loops not allowed in ALLOCATE_FREE methods");
        p.put(ERR_UNALLOWED_METHODCALL,
                "Illegal invocation of a method annotated %s from within a method annotated %s");
		return p;
	}
}
