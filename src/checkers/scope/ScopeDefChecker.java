package checkers.scope;

import java.util.Properties;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import com.sun.source.tree.CompilationUnitTree;

public class ScopeDefChecker extends SourceChecker {
    public ScopeDefChecker() {
        ScopeTree.initialize();
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new ScopeDefVisitor<Void, Void>(this, root);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put("bad.scope.name", "Reserved scope name used in @ScopeDef.");
        p.put("bad.scopedef.location", "@ScopeDef only allowed on variable declarations.");
        p.put("duplicate.scope.name", "Duplicate scope names with different parents.");
        p.put("cyclical.scopes", "Cyclical scope names detected.");
        p.put("bad.privatememory.assignment", "Cannot assign to a private memory with a different @ScopeDef.");
        return p;
    }
}
