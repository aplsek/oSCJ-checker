package checkers.scope;

import static checkers.scope.DefineScopeChecker.ERR_BAD_SCOPE_NAME;
import static checkers.scope.DefineScopeChecker.ERR_CYCLICAL_SCOPES;
import static checkers.scope.DefineScopeChecker.ERR_DUPLICATE_SCOPE_NAME;

import java.util.LinkedList;
import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.DefineScope;

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

public class DefineScopeVisitor<R, P> extends SCJVisitor<R, P> {
    private AnnotatedTypeFactory atf;
    private ScopeTree scopeTree;

    public DefineScopeVisitor(SourceChecker checker, CompilationUnitTree root, ScopeCheckerContext ctx) {
        super(checker, root);
        atf = checker.createFactory(root);
        scopeTree = ctx.getScopeTree();
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        DefineScope d = t.getAnnotation(DefineScope.class);

        if (d != null) {
            ScopeInfo name = new ScopeInfo(d.name());
            ScopeInfo parent = new ScopeInfo(d.parent());
            //System.out.println("scope def: " + d.name() + " par:" + d.parent());
            if (d.name() == null || d.parent() == null) {
                fail(ERR_BAD_SCOPE_NAME, node);
            } else if (d.name() != null && d.parent() != null) {
                if (name.isImmortal()) {
                    fail(ERR_BAD_SCOPE_NAME, node);
                } else if (scopeTree.hasScope(name)) {
                    fail(ERR_DUPLICATE_SCOPE_NAME, node);
                } else if (scopeTree.isParentOf(parent, name)) {
                    fail(ERR_CYCLICAL_SCOPES, node);
                    // TODO: doesn't reserve implicitly defined scopes
                } else {
                    scopeTree.put(name, parent, node);
                }
            }
        }
        return super.visitClass(node, p);
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        ExecutableElement m = TreeUtils.elementFromUse(node);
        TypeElement t = (TypeElement) m.getEnclosingElement();

        if (isSubtype(t, "javax.safetycritical.ManagedMemory")
                && m.getSimpleName().toString().equals("enterPrivateMemory")) {
            ExpressionTree runnable = node.getArguments().get(1);
            TypeElement t2 = Utils.getTypeElement(InternalUtils.typeOf(runnable));
            DefineScope ds = t2.getAnnotation(DefineScope.class);

            if (ds != null) {
                if (ds.name() == null || ds.parent() == null) {
                    fail(ERR_BAD_SCOPE_NAME, node);
                } else {
                    ScopeInfo name = new ScopeInfo(ds.name());
                    ScopeInfo parent = new ScopeInfo(ds.parent());

                    if (name.isReservedScope()) {
                        fail(ERR_BAD_SCOPE_NAME, node);
                    } else if (scopeTree.hasScope(name)) {
                        fail(ERR_DUPLICATE_SCOPE_NAME, node);
                    } else if (scopeTree.isParentOf(parent, name)) {
                        fail(ERR_CYCLICAL_SCOPES, node);
                    } else {
                        scopeTree.put(name, parent, node);
                    }
                }
            }
            // checker.report(
            // Result.failure("Runnables used with enterPrivateMemory must have a @DefineScope annotation"),
            // node);
        }
        return super.visitMethodInvocation(node, p);
    }

    private boolean isSubtype(TypeElement t, String superTypeName) {
        LinkedList<TypeElement> workList = new LinkedList<TypeElement>();
        workList.add(t);
        while (!workList.isEmpty()) {
            TypeElement s = workList.removeFirst();
            if (s.getQualifiedName().toString().equals(superTypeName)) {
                return true;
            } else {
                if (s.getKind() == ElementKind.CLASS) {
                    TypeElement u = Utils.superType(s);
                    if (u != null) {
                        workList.add(u);
                    }
                }
                for (TypeMirror i : s.getInterfaces()) {
                    workList.add((TypeElement) ((DeclaredType) i).asElement());
                }
            }
        }
        return false;
    }
}
