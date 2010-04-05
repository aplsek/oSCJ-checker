package checkers.scope;

import static checkers.Utils.debugPrintln;
import static checkers.Utils.getAnnotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypes;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;

// TODO: Aliasing of PrivateMemory
// TODO: Only allow @ScopeDef on PrivateMemory

@SuppressWarnings("restriction")
public class ScopeDefVisitor<R, P> extends SourceVisitor<R, P> {
    public static final String   SCOPE_DEF = "javax.safetycritical.annotate.ScopeDef";
    private AnnotatedTypeFactory atf;
    private AnnotatedTypes       ats;

    public ScopeDefVisitor(SourceChecker checker, CompilationUnitTree root) {
        super(checker, root);

        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
    }

    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        AnnotatedTypeMirror varType = atf.getAnnotatedType(node);
        if (TypesUtils.isDeclaredOfName(varType.getUnderlyingType(), "javax.safetycritical.PrivateMemory")) {
            Element rhsElem = TreeUtils.elementFromUse(node.getExpression());
            switch (rhsElem.getKind()) {
            case FIELD:
            case LOCAL_VARIABLE:
            case PARAMETER:
                AnnotatedTypeMirror rhsType = atf.getAnnotatedType(rhsElem);
                AnnotationMirror varScopeDef = varType.getAnnotation("javax.safetycritical.annotate.ScopeDef");
                AnnotationMirror rhsScopeDef = rhsType.getAnnotation("javax.safetycritical.annotate.ScopeDef");
                if (!varScopeDef.equals(rhsScopeDef)) {
                    checker.report(Result.failure("bad.privatememory.assignment"), node);
                }
                break;
            case CONSTRUCTOR:
                break;
            default:
                checker.report(Result.failure("Cannot assign to a PrivateMemory from anything other than a variable"),
                    node);
            }
        }
        return super.visitAssignment(node, p);
    }

    private TypeElement superType(TypeElement type) {
        if (TypesUtils.isObject(type.asType())) { return null; }
        return (TypeElement) ((DeclaredType) type.getSuperclass()).asElement();
    }

    @Override
    public R visitClass(ClassTree node, P p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);
        if (t.getKind() == ElementKind.CLASS) {
            TypeElement superType = superType(t);
            while (!TypesUtils.isObject(superType.asType())) {
                String className = superType.getQualifiedName().toString();
                if (TypesUtils.isDeclaredOfName(superType.asType(), "javax.safetycritical.Mission")
                        || TypesUtils.isDeclaredOfName(superType.asType(), "javax.safetycritical.ManagedEventHandler")) {
                    // TODO: This needs to check superclasses
                    String scope = Utils.scope(t.getAnnotationMirrors());
                    if (scope == null) {
                        ScopeTree.put(t.getQualifiedName().toString(), "immortal");
                    } else {
                        ScopeTree.put(t.getQualifiedName().toString(), scope);
                    }
                    break;
                }
                superType = superType(superType);
            }
            for (TypeElement iface : Utils.getAllInterfaces(t)) {
                if (TypesUtils.isDeclaredOfName(iface.asType(), "javax.safetycritical.ManagedEventHandler")) {
                    String scope = Utils.scope(t.getAnnotationMirrors());
                    if (scope == null) {
                        ScopeTree.put(t.getQualifiedName().toString(), "immortal");
                    } else {
                        ScopeTree.put(t.getQualifiedName().toString(), scope);
                    }
                    break;
                }
            }

            if (getAnnotation(t.getAnnotationMirrors(), SCOPE_DEF) != null) {
                checker.report(Result.failure("bad.scopedef.location"), node);
            }
        }
        return super.visitClass(node, p);
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        if (getAnnotation(TreeUtils.elementFromDeclaration(node).getAnnotationMirrors(), SCOPE_DEF) != null) {
            checker.report(Result.failure("bad.scopedef.location"), node);
        }
        return super.visitMethod(node, p);
    }

    @Override
    public R visitVariable(VariableTree node, P p) {
        VariableElement var = TreeUtils.elementFromDeclaration(node);
        AnnotationMirror scopeDef = getAnnotation(var.getAnnotationMirrors(), SCOPE_DEF);

        if (scopeDef != null) {
            Map<String, String> map = scopeDefMap(scopeDef);
            String name = map.get("name");
            String parent = map.get("parent");
            String reportedParent = ScopeTree.get(name);
            if ("immortal".equals(name)) {
                checker.report(Result.failure("bad.scope.name"), node);
            } else if (reportedParent != null && !reportedParent.equals(parent)) {
                checker.report(Result.failure("duplicate.scope.name"), node);
            } else if (ScopeTree.isParentOf(parent, name)) {
                checker.report(Result.failure("cyclical.scopes"), node);
            } else if (reportedParent == null) {
                debugPrintln("Added " + name + " scope with parent " + parent);
                ScopeTree.put(name, parent);
            }
        }
        return super.visitVariable(node, p);
    }

    /**
     * @param scopeDef
     * @return
     */
    public static Map<String, String> scopeDefMap(AnnotationMirror scopeDef) {
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : scopeDef.getElementValues()
            .entrySet()) {
            String key = e.getKey().toString();
            if (key.equals("name()") || key.equals("parent()")) {
                String newKey = key.replace("()", "");
                map.put(newKey, (String) e.getValue().getValue());
            }
        }
        return map;
    }

    /**
     * Returns annotation Value for given annotation...
     * 
     * @param annotations
     * @param annotation
     * @return
     */
    public String annotationValue(Collection<? extends AnnotationMirror> annotations, String annotation) {
        for (AnnotationMirror a : annotations) {
            String aType = a.getAnnotationType().toString();
            if (aType.equals(annotation)) {
                debugPrintln("getting value for" + annotation);
                debugPrintln("annotations=" + annotations.toString());
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = a.getElementValues();
                for (AnnotationValue av : vals.values()) {
                    debugPrintln("value is" + (String) av.getValue());
                    return (String) av.getValue();
                }
            }
        }
        return null;
    }
}
