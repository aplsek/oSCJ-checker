package checkers.scjAllowed;

import static checkers.scjAllowed.EscapeMap.isEscaped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.safetycritical.annotate.SCJAllowed;
import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypes;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.TreeUtils;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * Questions: if it is not annotated then its HIDDEN. If is is annotated by
 * SCLAllowed, its LEVEL 0
 * 
 * @author dtang, plsek
 */
@SuppressWarnings("restriction")
public class SCJAllowedVisitor<R, P> extends SourceVisitor<R, P> {
    private final AnnotatedTypeFactory atf;
    private final AnnotatedTypes ats;
    private final String pkg;
    private final Stack<Integer> scjAllowedStack = new Stack<Integer>();
    private static final Map<String, Integer> annoValueMap = new HashMap<String, Integer>();

    private static final int LEVEL0 = 0;
    private static final int LEVEL1 = 1;
    private static final int LEVEL2 = 2;
    private static final int INFRASTRUCTURE = 3;
    private static final int HIDDEN = 4;
    

    static {
        annoValueMap.put("javax.safetycritical.annotate.Level.LEVEL_0", LEVEL0);
        annoValueMap.put("javax.safetycritical.annotate.Level.LEVEL_1", LEVEL1);
        annoValueMap.put("javax.safetycritical.annotate.Level.LEVEL_2", LEVEL2);
        annoValueMap.put("javax.safetycritical.annotate.Level.INFRASTRUCTURE", INFRASTRUCTURE);
        annoValueMap.put("javax.safetycritical.annotate.Level.HIDDEN", HIDDEN);
    }

    public SCJAllowedVisitor(SourceChecker checker, CompilationUnitTree root) {
        super(checker, root);
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
        pkg = root.getPackageName() == null ? "" : root.getPackageName()
                .toString();
    }

    /**
     * Verifying that the class has an appropriate SCJAllowed level.
     */
    @Override
    public R visitClass(ClassTree node, P p) {
        //debugPrint(node);

        TypeElement t = TreeUtils.elementFromDeclaration(node);

        if (!isSCJAllowed(t) && !isEnclosingSCJAllowed(t.getEnclosingElement()))
            return null;

        if (isEscaped(t.toString()))
            return null;

        // if (hasSuppressSCJAnnotation(t)) return null;
        int level = scjAllowedLevel(t.getAnnotationMirrors());
        if (!scjAllowedStack.isEmpty() && scjAllowedStack.peek() > level) {
            /** tested by FakeSCJ */
            checker.report(Result.failure("scjallowed.badenclosed"), node);
        }
        TypeElement superType = Utils.superType(t);
        while (superType != null
                && !EscapeMap
                        .isEscaped(superType.getQualifiedName().toString())) {
            if (scjAllowedLevel(superType.getAnnotationMirrors()) > level) {
                System.err.println(superType.getQualifiedName());
                System.err.println(scjAllowedLevel(superType
                        .getAnnotationMirrors())
                        + " > " + level);
                checker.report(Result.failure("scjallowed.badsubclass"), node);
            }
            superType = Utils.superType(superType);
        }
        scjAllowedStack.push(level);
        R r = super.visitClass(node, p);
        scjAllowedStack.pop();
        return r;
    }

    private void debugPrint(Tree node) {
        if (!Utils.DEBUG)
            return;

        if (scjAllowedStack.isEmpty()) {
            //System.out.println("Node:" + node.toString());
            return;
        }

        int level = scjAllowedStack.pop();
        scjAllowedStack.push(level);

    }

    @Override
    public R visitMemberSelect(MemberSelectTree node, P p) {
        Element fieldElement = TreeUtils.elementFromUse(node);
        
        //System.out.println("Member Select:" + node + "  " + fieldElement.getEnclosingElement());
        
        if (fieldElement.getEnclosingElement() != null && isEscaped(fieldElement.getEnclosingElement().toString())) {
            return super.visitMemberSelect(node, p);
        }
        
        if (fieldElement.getKind() == ElementKind.FIELD
                && scjAllowedLevel(fieldElement, node) > scjAllowedStack.peek()) {
            /** Tested by SCJAllowedTest */
            checker.report(Result.failure("scjallowed.badfieldaccess",
                    scjAllowedStack.peek()), node);
        }
        return super.visitMemberSelect(node, p);
    }

    /**
     * Errors: - we can not override a method by a method with a higher
     * SCJallowed level
     */
    @Override
    public R visitMethod(MethodTree node, P p) {

        ExecutableElement methodElement = TreeUtils
                .elementFromDeclaration(node);

        checkSCJProtected(methodElement, node);
        
        int level = 0;
        if (isDefaultConstructor(node)) 
            //System.out.println("level333 :" + level);
            return null;
        else
            level = scjAllowedLevel(methodElement, node);
        
        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
                .overriddenMethods(methodElement);
        for (ExecutableElement override : overrides.values())
            if (!isEscaped(override.getEnclosingElement().toString())
                    && level > scjAllowedLevel(override, node)) {
                /** Tested by OverrideTest */
                checker.report(Result.failure("scjallowed.badoverride"), node);
            }
        if (scjAllowedStack.peek() > level) {
            /** tested by FakeSCJ */
            // debugPrint("level: "+level);
            // debugPrint("peek: : "+scjAllowedStack.peek());
            checker.report(Result.failure("scjallowed.badenclosed"), node);
        }

        scjAllowedStack.push(level);
        R r = super.visitMethod(node, p);
        scjAllowedStack.pop();
        return r;
    }

    
    private boolean isDefaultConstructor(MethodTree node) {
        if (node.toString().trim().startsWith("public <init>("))
            return true;
        return false;
    }
    
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        debugPrint(node);

        ExecutableElement method = TreeUtils.elementFromUse(node);

        if (isEscaped(method.getEnclosingElement().toString())) {
            return super.visitMethodInvocation(node, p);
        }

        checkSCJProtectedCall(method, node);
        
        // System.out.println("method :" + method);
        //System.out.println("peeek :" + scjAllowedStack.peek());
        //System.out.println("peeek :" +scjAllowedLevel(method, node) );
        
        if (scjAllowedLevel(method, node) > scjAllowedStack.peek()) {
            /** Tested by SCJAllowedTest */
            checker.report(Result.failure("scjallowed.badmethodcall",
                    scjAllowedStack.peek()), node);
        }
        
        if (scjAllowedStack.peek() == HIDDEN &&  scjAllowedLevel(method, node) < HIDDEN) { 
            //System.out.println("\nmethod :" + method);
            //System.out.println("node :" + node);
            //System.out.println("peeek :" + scjAllowedStack.peek());
            //System.out.println("invoked level :" + scjAllowedLevel(method, node));
            
            
            checker.report(Result.failure("hidden.to.scjallowed",
                    scjAllowedStack.peek()), node);
        }

        return super.visitMethodInvocation(node, p);
    }

    @Override
    public R visitNewClass(NewClassTree node, P p) {
        // debugPrint(node);

        if (node.toString().contains("@SuppressSCJ"))
            return null;

        ExecutableElement ctorElement = TreeUtils.elementFromUse(node);

        if (isEscaped(ctorElement.getEnclosingElement().toString())) {
            return super.visitNewClass(node, p);
        }

        if (checkSCJProtected(ctorElement, node)
                && scjAllowedLevel(ctorElement, node) > scjAllowedStack.peek()) {
            /** tested by SuppressTest */
            checker.report(Result.failure("scjallowed.badnewcall",
                    scjAllowedStack.peek()), node);
        }

        return super.visitNewClass(node, p);
    }

    public R visitVariable(VariableTree node, P p) {
        //debugPrint(node);

        VariableElement variable = TreeUtils.elementFromDeclaration(node);
        
        // if (hasSuppressSCJAnnotation(variable)) return null;
        int level = scjAllowedLevel(variable, node);
        if (scjAllowedStack.peek() > level) {
            /** tested by FakeSCJ */
            checker.report(Result.failure("scjallowed.badenclosed"), node);
        }
        return super.visitVariable(node, p);
    }

    /**
     * Evaluates if there is a GUARD in the if statement. (really a simple
     * implementation : just comparing strings)
     */
    @Override
    public R visitIf(IfTree node, P p) {
        ExpressionTree cond = node.getCondition();
        StatementTree thenStm = node.getThenStatement();
        StatementTree elseStm = node.getElseStatement();
        if (isGuard(node)) {
            cond.accept(this, p);
            scjAllowedStack.push(getGuardLevel(node));
            thenStm.accept(this, p);
            scjAllowedStack.pop();
            if (elseStm != null)
                return elseStm.accept(this, p);
            else
                return null;
        } else
            return super.visitIf(node, p);
    }

    /**
     * Returns true if Guard is there.
     */
    private boolean isGuard(IfTree node) {
        if (node.toString().startsWith("if (Safelet.getDeploymentLevel() == "))
            return true;
        else
            return false;
    }

    /**
     * Returns the level value of the guard
     */
    private int getGuardLevel(IfTree node) {
        if (node.toString().startsWith("if (Safelet.getDeploymentLevel() == 0"))
            return 0;
        else if (node.toString().startsWith(
                "if (Safelet.getDeploymentLevel() == 1"))
            return 1;
        else if (node.toString().startsWith(
                "if (Safelet.getDeploymentLevel() == 2"))
            return 2;
        return 3;
    }

    /**
     * Extracting SCJAllowed LEVEL value from the executable element Default
     * returns HIDDEN.
     */
    private int scjAllowedLevel(ExecutableElement method, Tree node) {
        if (isEscaped(method.getEnclosingElement().toString()))
            return EscapeMap.escape
                    .get(method.getEnclosingElement().toString());
        
        if (isSCJAllowed(method))
            return scjAllowedLevel(method.getAnnotationMirrors());

        for (ExecutableElement override : ats.overriddenMethods(method)
                .values())
            if (!isEscaped(override.getEnclosingElement().toString())
                    && isSCJAllowed(override))
                return scjAllowedLevel(override.getAnnotationMirrors());
        
        return getEnclosingLevel(method);
    }

    /**
     * extracting SCJAllowed LEVEL value from the Field element Default returns
     * HIDDEN.
     */
    private int scjAllowedLevel(Element field, Tree node) {
        if (isSCJAllowed(field))
            return scjAllowedLevel(field.getAnnotationMirrors());
        return getEnclosingLevel(field);
    }

    /**
     * extracting SCJAllowed LEVEL value from a list of annotations if
     * SCJAllowed non-specified we are level 0? if in SCJ API returns 0 if not
     * in SCJ API returns HIDDEN if no annotation: if in SCJ returns HIDDEN else
     * returns 0
     */
    private int scjAllowedLevel(List<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror anote : annotations) {
            if (anote.getAnnotationType().toString().equals(
                    "javax.safetycritical.annotate.SCJAllowed")) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = anote
                        .getElementValues();
                if (vals.isEmpty())
                    return LEVEL0;
                

               // System.out.println("method:" + vals);
                
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : anote
                        .getElementValues().entrySet()) {
                    if ("value".equals(entry.getKey().getSimpleName()
                            .toString()))
                        return annoValueMap.get(entry.getValue().toString());
                }
                return 0;
            }
        }
        return HIDDEN;
    }

    /**
     * @assert: should be called only on methods in SCJ package
     * @recursive: is recursive, searches through enclosing elements until the
     *             SCJAllowed is find, otherwise returns HIDDEN
     * @return if the enclosing member has SCJAllowed(members=true) we return
     *         its LEVEL as the level of the methodElement else we return HIDDEN
     */
    private int getEnclosingLevel(Element methodElement) {
        Element enclosing = methodElement.getEnclosingElement();
        
        if (enclosing == null)
            return HIDDEN;

        //Utils.debugPrintln("enclo :" + enclosing + "level : " + scjAllowedLevel(enclosing.getAnnotationMirrors()));
        
        
        
        if (isSCJAllowed(enclosing)) {
            if (scjAllowedMembers(enclosing.getAnnotationMirrors()))
                return scjAllowedLevel(enclosing.getAnnotationMirrors());
            else
                return HIDDEN;
        }
        return getEnclosingLevel(enclosing);
        // recursive call, this enclosing was not allocated, so we search even
        // higher
    }

    /**
     * extracting SCJAllowed MEMBERS value from the element
     */
    private boolean scjAllowedMembers(
            List<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror annotation : annotations) {
            if (annotation.getAnnotationType().toString().equals(
                    "javax.safetycritical.annotate.SCJAllowed")) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation
                        .getElementValues().entrySet()) {
                    if ("members".equals(entry.getKey().getSimpleName()
                            .toString()))
                        return getBoolean(entry.getValue().toString());
                }
            }

        }
        return false;
    }

    /**
     * Returns true if element is annotated by SCJAllowed
     */
    private boolean isSCJAllowed(Element element) {
        Utils.debugPrintln("annotated:" + element);
        return element.getAnnotation(SCJAllowed.class) != null;
    }

    /**
     * Returns true if element is annotated by SCJProtected
     * 
     * TODO: @SCJProtected is no longer supported
     */
    private boolean isSCJProtected(Element element, Tree node) {
        if (element.getAnnotation(SCJAllowed.class) != null) 
            if (scjAllowedLevel(element, node) == INFRASTRUCTURE)
                return true;
        return false;
    }

    /**
     * If the method is SCJProtected we verify that it is in these packages:
     * javax.safetycritical javax.realtime If not, we must report
     * "scjallowed.badprotectedcall" error.
     * 
     * @return false - if its ilegal to call SCJProtected
     */
    private boolean checkSCJProtected(ExecutableElement methodElement, Tree node) {
        boolean isValid = !isSCJProtected(methodElement, node)
                || (pkg.startsWith("javax.safetycritical") || pkg
                        .startsWith("javax.realtime"));
        if (!isValid) {
            /** Tested by TestAllowedProtectedClash */
            checker.report(Result.failure("scjallowed.badprotected"), node);
        }
        return isValid;
    }
    
    private boolean checkSCJProtectedCall(ExecutableElement methodElement, Tree node) {
        boolean isValid = !isSCJProtected(methodElement, node)
                || (pkg.startsWith("javax.safetycritical") || pkg
                        .startsWith("javax.realtime"));
        if (!isValid) {
            /** Tested by TestAllowedProtectedClash */
            checker.report(Result.failure("scjallowed.badprotectedcall"), node);
        }
        return isValid;
    }

    /**
     * Returns true if the String is "true"
     */
    private boolean getBoolean(String bool) {
        return bool.equals("true");
    }

    /**
     * returns true if any enclosing element is @SCJAllowed
     */
    private boolean isEnclosingSCJAllowed(Element element) {
        if (isSCJAllowed(element))
            return true;
        else if (element.getEnclosingElement() != null)
            return isEnclosingSCJAllowed(element.getEnclosingElement());
        else
            return false;
    }
}
