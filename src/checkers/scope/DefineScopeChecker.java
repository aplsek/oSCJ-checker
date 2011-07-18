package checkers.scope;

import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

public class DefineScopeChecker extends SinglePassChecker {
    public static final String ERR_CYCLICAL_SCOPES = "cyclical.scopes";
    public static final String ERR_DUPLICATE_SCOPE_NAME = "duplicate.scope.name";
    public static final String ERR_ENTER_PRIVATE_MEMORY_NO_DEFINE_SCOPE = "epm.no.ds";
    public static final String ERR_PRIVATE_MEM_NO_DEFINE_SCOPE = "privateMem.no.define.scope";
    public static final String ERR_RESERVED_SCOPE_NAME = "reserved.scope.name";
    public static final String ERR_SCOPE_HAS_NO_PARENT = "scope.has.no.parent";
    public static final String ERR_UNUSED_DEFINE_SCOPE = "unused.define.scope";
    public static final String ERR_SCHEDULABLE_NO_DEFINE_SCOPE = "schedulable.no.define.scope";
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
        p.put(ERR_DUPLICATE_SCOPE_NAME, "Duplicate scope definition for %s.");
        p.put(ERR_ENTER_PRIVATE_MEMORY_NO_DEFINE_SCOPE, "Runnables used in enterPrivateMemory() must have a @DefineScope annotation.");
        p.put(ERR_CYCLICAL_SCOPES, "Cyclical scope names detected (%s is a parent of %s).");
        p.put(ERR_PRIVATE_MEM_NO_DEFINE_SCOPE, "PrivateMemory variable must have a @DefineScope annotation.");
        p.put(ERR_RESERVED_SCOPE_NAME, "Invalid use of reserved scope name %s used in @DefineScope.");
        p.put(ERR_SCOPE_HAS_NO_PARENT, "Scope %s has a non-existent parent %s.");
        p.put(ERR_UNUSED_DEFINE_SCOPE, "Unused DefineScope annotation: %s.");
        p.put(ERR_SCHEDULABLE_NO_DEFINE_SCOPE, "Classes implementing javax.realtime.Schedulable must have a @DefineScope annotation.");
        return p;
    }

    @Override
    public void typeProcessingOver() {
        super.typeProcessingOver();
        ScopeTree scopeTree = ctx.getScopeTree();
        scopeTree.checkScopeTree(this);
    }
}
