package checkers.scope;

import static checkers.scope.DefineScopeChecker.ERR_BAD_SCOPE_NAME;
import static checkers.scope.DefineScopeChecker.ERR_CYCLICAL_SCOPES;
import static checkers.scope.DefineScopeChecker.ERR_DUPLICATE_SCOPE_NAME;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.DefineScope;

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.SourceChecker;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

public class DefineScopeVisitor<R, P> extends SCJVisitor<R, P> {
    private ScopeTree scopeTree;
    private final TypeMirror managedMemoryMirror;

    public DefineScopeVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        scopeTree = ctx.getScopeTree();
        managedMemoryMirror = Utils.getTypeMirror(elements,
                "javax.safetycritical.ManagedMemory");
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        DefineScope ds = t.getAnnotation(DefineScope.class);

        if (ds != null) {
            checkNewScope(ds.name(), ds.parent(), node);
        }
        return super.visitClass(node, p);
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        ExecutableElement m = TreeUtils.elementFromUse(node);
        TypeElement t = (TypeElement) m.getEnclosingElement();

        if (types.isSubtype(t.asType(), managedMemoryMirror)
                && m.getSimpleName().toString().equals("enterPrivateMemory")) {
            ExpressionTree runnable = node.getArguments().get(1);
            TypeElement t2 = Utils.getTypeElement(InternalUtils
                    .typeOf(runnable));
            DefineScope ds = t2.getAnnotation(DefineScope.class);

            if (ds != null) {
                checkNewScope(ds.name(), ds.parent(), node);
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    void checkNewScope(String child, String parent, Tree node) {
        if (child == null || parent == null) {
            fail(ERR_BAD_SCOPE_NAME, node);
        } else {
            ScopeInfo childScope = new ScopeInfo(child);
            ScopeInfo parentScope = new ScopeInfo(parent);
            if (childScope.isReservedScope()) {
                fail(ERR_BAD_SCOPE_NAME, node);
            } else if (scopeTree.hasScope(childScope)) {
                fail(ERR_DUPLICATE_SCOPE_NAME, node);
            } else if (scopeTree.isParentOf(parentScope, childScope)) {
                fail(ERR_CYCLICAL_SCOPES, node);
            } else {
                scopeTree.put(childScope, parentScope, node);
            }
        }
    }
}
