package checkers.scope;


import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

public class ScopeTreeChecker extends SinglePassChecker {

    public static final String ERR_SCOPE_HAS_NO_PARENT = "scope.has.no.parent";
    private ScopeCheckerContext ctx;

    public ScopeTreeChecker(ScopeCheckerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new ScopeTreeVisitor(this, root, ctx);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();

        p.put(ERR_SCOPE_HAS_NO_PARENT, "Scope %s has a non-existent parent %s.");

        return p;
    }
}
