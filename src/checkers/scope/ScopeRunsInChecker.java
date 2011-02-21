package checkers.scope;

import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

public class ScopeRunsInChecker extends SinglePassChecker {
    public static final String ERR_BAD_LIBRARY_ANNOTATION = "bad.library.annotation";
    public static final String ERR_BAD_SCOPE_NAME = "bad.scope.name";
    public static final String ERR_ILLEGAL_FIELD_SCOPE_OVERRIDE = "illegal.field.scope.override";
    public static final String ERR_ILLEGAL_RUNS_IN_OVERRIDE = "illegal.scope.override";
    public static final String ERR_ILLEGAL_SCOPE_OVERRIDE = "illegal.scope.override";
    public static final String ERR_RUNS_IN_ON_CLASS = "err.runs.in.on.class";

    private ScopeCheckerContext ctx;

    public ScopeRunsInChecker(ScopeCheckerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new ScopeRunsInVisitor(this, root, ctx);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put(ERR_BAD_LIBRARY_ANNOTATION, "Library superclass has malformed annotations.");
        p.put(ERR_BAD_SCOPE_NAME, "Scope %s does not exist.");
        // TODO: Fix error messages
        p.put(ERR_ILLEGAL_FIELD_SCOPE_OVERRIDE, "Field annotations may not override the annotations of their types.");
        p.put(ERR_ILLEGAL_RUNS_IN_OVERRIDE, "Illegal RunsIn annotation override on a method not annotated SUPPORT.");
        p.put(ERR_ILLEGAL_SCOPE_OVERRIDE, "Illegal Scope annotation override on a method not annotated SUPPORT.");
        p.put(ERR_RUNS_IN_ON_CLASS, "RunsIn annotations are ignored on classes.");
        return p;
    }
}
