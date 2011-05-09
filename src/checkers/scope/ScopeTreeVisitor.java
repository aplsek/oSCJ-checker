package checkers.scope;

import static checkers.scope.ScopeTreeChecker.ERR_SCOPE_HAS_NO_PARENT;

import java.util.HashMap;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.safetycritical.annotate.DefineScope;

import checkers.SCJMission;
import checkers.SCJSchedulable;
import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.SourceChecker;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;

/**
 * This visitor is responsible for retrieving Scope and RunsIn annotations from
 * classes and methods and making sure they are valid. This information is
 * stored into a context object so that the ScopeVisitor doesn't have to deal
 * with retrieving this information.
 */
public class ScopeTreeVisitor extends SCJVisitor<Void, Void> {
    private ScopeCheckerContext ctx;

    private ScopeTree scopeTree;
    private HashMap<String, Tree> errorScopes;

    public ScopeTreeVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        this.ctx = ctx;

        scopeTree = ctx.getScopeTree();
    }

    void pln(String str) {System.out.println("\t" + str);}

    @Override
    public Void visitClass(ClassTree node, Void p) {

        TypeElement t = TreeUtils.elementFromDeclaration(node);
        DefineScope ds = t.getAnnotation(DefineScope.class);

        if (ds != null) {
            String scope = ds.name();
            String parent = ds.parent();
            if (scopeTree.errorScopes.containsKey(parent)) {
                //pln("\n ERROR NODE:" + t.toString());
                //pln("DS:" + ds);

                Tree n = scopeTree.getErrorScopes().get(parent);
                if (n.equals(node)) {
                   // pln("node equals!");
                    fail(ERR_SCOPE_HAS_NO_PARENT, node, scope, parent);
                }
            }
        }


        return super.visitClass(node, p);
    }

}
