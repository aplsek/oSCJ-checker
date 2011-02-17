package checkers.scope;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

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

import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import static checkers.scope.DefineScopeChecker.*;

// TODO: Verify tree structure after construction

@SuppressWarnings("restriction")
public class DefineScopeVisitor<R, P> extends SourceVisitor<R, P> {
    private AnnotatedTypeFactory atf;

    public DefineScopeVisitor(SourceChecker checker, CompilationUnitTree root) {
        super(checker, root);

        atf = checker.createFactory(root);
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);

        DefineScope d = t.getAnnotation(DefineScope.class);
        if (d != null) {
            //System.out.println("scope def: " + d.name() + " par:" + d.parent());
            if (d.name() == null || d.parent() == null) {
                checker.report(Result.failure(ERR_BAD_SCOPE_NAME), node);
            } else if (d.name() != null && d.parent() != null) {
                if (IMMORTAL.equals(d.name())) {
                    checker.report(Result.failure(ERR_BAD_SCOPE_NAME), node);
                } else if (ScopeTree.hasScope(d.name())) {
                    checker.report(Result.failure(ERR_DUPLICATE_SCOPE_NAME), node);
                } else if (ScopeTree.isParentOf(d.parent(), d.name())) {
                    checker.report(Result.failure(ERR_CYCLICAL_SCOPES), node);
                    // TODO: doesn't reserve implicitly defined scopes
                }
                else
                    ScopeTree.put(d.name(), d.parent(), node);
            }
        }

        //System.out.println("\nScope Visit:");
        // ScopeTree.printTree();
        return super.visitClass(node, p);
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        ExecutableElement m = TreeUtils.elementFromUse(node);
        TypeElement t = (TypeElement) m.getEnclosingElement();
        if (isSubtype(t, "javax.safetycritical.ManagedMemory")
                && m.getSimpleName().toString().equals("enterPrivateMemory")) {
            ExpressionTree runnable = node.getArguments().get(1);
            AnnotatedTypeMirror t2 = atf.getAnnotatedType(runnable);
            AnnotationMirror def = t2.getAnnotation(DefineScope.class);
            if (def != null) {
                String name = null, parent = null;
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : def
                        .getElementValues().entrySet()) {
                    if ("name()".equals(entry.getKey().toString())) {
                        name = (String) entry.getValue().getValue();
                    } else if ("parent()".equals(entry.getKey().toString())) {
                        parent = (String) entry.getValue().getValue();
                    }
                }
                if (name != null && parent != null) {
                    if (IMMORTAL.equals(name)) {
                        checker.report(Result.failure(ERR_BAD_SCOPE_NAME), node);
                        //
                        // TODO: ales, this is disabled, we allow this...
                    } else if (ScopeTree.hasScope(name)) {
                        checker.report(Result.failure(ERR_DUPLICATE_SCOPE_NAME),
                                node);
                    } else if (ScopeTree.isParentOf(parent, name)) {
                        checker.report(Result.failure(ERR_CYCLICAL_SCOPES), node);
                    } else {
                        ScopeTree.put(name, parent, node);
                        return super.visitMethodInvocation(node, p);
                    }
                    // TODO: doesn't reserve implicitly defined scopes
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
