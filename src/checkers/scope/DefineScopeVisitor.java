package checkers.scope;

import static checkers.scope.DefineScopeChecker.ERR_CYCLICAL_SCOPES;
import static checkers.scope.DefineScopeChecker.ERR_DUPLICATE_SCOPE_NAME;
import static checkers.scope.DefineScopeChecker.ERR_ENTER_PRIVATE_MEMORY_NO_DEFINE_SCOPE;
import static checkers.scope.DefineScopeChecker.ERR_RESERVED_SCOPE_NAME;
import static checkers.scope.DefineScopeChecker.ERR_UNUSED_DEFINE_SCOPE;
import static checkers.scope.DefineScopeChecker.ERR_SCHEDULABLE_NO_DEFINE_SCOPE;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.safetycritical.annotate.DefineScope;

import checkers.SCJMethod;
import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.SourceChecker;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

public class DefineScopeVisitor<R, P> extends SCJVisitor<R, P> {
    private ScopeTree scopeTree;

    public DefineScopeVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        scopeTree = ctx.getScopeTree();
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        DefineScope ds = t.getAnnotation(DefineScope.class);

        // TODO:
        if (implicitlyDefinesScope(t) && ds == null)
            fail(ERR_SCHEDULABLE_NO_DEFINE_SCOPE, node);

        if (ds != null) {
            // check for @DefineScope on the methods that must have a @DS and the we look at all the classes implementing Runnable interface
            if (implicitlyDefinesScope(t) || isSafelet(t))
                checkNewScope(ds.name(), ds.parent(), node,false);
            else if (isSubtypeOfRunnable(t))
                checkNewScope(ds.name(), ds.parent(), node, true);
            else
                fail(ERR_UNUSED_DEFINE_SCOPE, node);
        }

        return super.visitClass(node, p);
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        ExecutableElement m = TreeUtils.elementFromUse(node);
        TypeElement t = Utils.getMethodClass(m);


        if (isManagedMemoryType(t)
                && SCJMethod.fromMethod(m, elements, types) == SCJMethod.ENTER_PRIVATE_MEMORY) {
            ExpressionTree runnable = node.getArguments().get(1);
            TypeElement t2 = Utils.getTypeElement(InternalUtils
                    .typeOf(runnable));
            Tree errNode = trees.getTree(t2);
            DefineScope ds = t2.getAnnotation(DefineScope.class);

            // Report errors on the Runnable being used, if possible, rather
            // than the method invocation site. Error messages from
            // checkNewScope() are much less confusing on classes.
            if (errNode == null)
                errNode = node;
           if (ds == null)
                fail(ERR_ENTER_PRIVATE_MEMORY_NO_DEFINE_SCOPE, errNode);

            // NOTE: the scope was already added in the visitClass.
            //checkNewScope(ds.name(), ds.parent(), errNode,true);
        }


        return super.visitMethodInvocation(node, p);
    }

    /**
     * Ensure that a DefineScope annotation is valid.
     */
    void checkNewScope(String child, String parent, Tree node, boolean isRunnable) {
        // Null scope checks aren't necessary since Java apparently doesn't
        // consider "null" to be a constant expression.
        ScopeInfo childScope = new ScopeInfo(child);
        ScopeInfo parentScope = new ScopeInfo(parent);

        if (checkReservedScopeName(child, parent, node))
            return;

        if (scopeTree.hasScope(childScope)) {
            ScopeInfo expectedParent = scopeTree.getParent(childScope);
            if (expectedParent != null && !expectedParent.equals(parentScope)) {
                if (isRunnable) {
                    // we do not report duplicates for @DefineScope on Runnable
                    return;
                } else
                    fail(ERR_DUPLICATE_SCOPE_NAME, node, childScope);
            }
        }

        if (scopeTree.isAncestorOf(parentScope, childScope))
            fail(ERR_CYCLICAL_SCOPES, node, parentScope, childScope);
        else
            scopeTree.put(childScope, parentScope, node);
    }

    private boolean checkReservedScopeName(String child, String parent,
            Tree node) {
        ScopeInfo childScope = new ScopeInfo(child);
        ScopeInfo parentScope = new ScopeInfo(parent);
        boolean reservedChild = childScope.isReservedScope();
        boolean reservedParent = !parentScope.isValidParentScope();

        boolean fail = false;
        if (reservedChild) {
            fail = true;
            fail(ERR_RESERVED_SCOPE_NAME, node, childScope);
        }
        if (reservedParent) {
            fail = true;
            fail(ERR_RESERVED_SCOPE_NAME, node, parentScope);
        }

        return fail;
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        DefineScope ds = m.getAnnotation(DefineScope.class);
        if (ds != null)
            warn(ERR_UNUSED_DEFINE_SCOPE, node);

        return super.visitMethod(node, p);
    }
}
