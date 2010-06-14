package checkers.scope;

import java.util.Properties;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import com.sun.source.tree.CompilationUnitTree;

public class DefineScopeChecker extends SourceChecker {
    public DefineScopeChecker() {
        ScopeTree.initialize();
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new DefineScopeVisitor<Void, Void>(this, root);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put("bad.scope.name", "Reserved scope name used in @DefineScope.");
        p.put("duplicate.scope.name", "Duplicate scope name from @DefineScope.");
        p.put("cyclical.scopes", "Cyclical scope names detected.");
        p.put("privateMem.no.DefineScope", "PrivatemMemory variable must have a @DefineScope annotation.");
        return p;
    }
}
