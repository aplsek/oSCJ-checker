package checkers.scope;

import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

public class DefineScopeChecker extends SinglePassChecker {
    public static final String ERR_CYCLICAL_SCOPES = "cyclical.scopes";
    public static final String ERR_DUPLICATE_SCOPE_NAME = "duplicate.scope.name";
    public static final String ERR_PRIVATE_MEM_NO_DEFINE_SCOPE = "privateMem.no.DefineScope";
    public static final String ERR_BAD_SCOPE_NAME = "bad.scope.name";

    private ScopeCheckerContext ctx;

    public DefineScopeChecker(ScopeCheckerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new DefineScopeVisitor<Void, Void>(this, root, ctx);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put(ERR_BAD_SCOPE_NAME, "Reserved scope name used in @DefineScope.");
        p.put(ERR_DUPLICATE_SCOPE_NAME, "Duplicate scope name from @DefineScope.");
        p.put(ERR_CYCLICAL_SCOPES, "Cyclical scope names detected.");
        p.put(ERR_PRIVATE_MEM_NO_DEFINE_SCOPE, "PrivatemMemory variable must have a @DefineScope annotation.");
        return p;
    }

    @Override
    public void typeProcessingOver() {
        super.typeProcessingOver();
        ScopeTree scopeTree = ctx.getScopeTree();
        if (!scopeTree.verifyScopeTree()) {
            scopeTree.reportErrors(this);
        }
    }
}
