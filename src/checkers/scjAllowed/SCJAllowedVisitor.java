package checkers.scjAllowed;

import static checkers.scjAllowed.EscapeMap.escapeAnnotation;
import static checkers.scjAllowed.EscapeMap.escapeEnum;
import static checkers.scjAllowed.EscapeMap.isEscaped;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_HIDDEN_TO_SCJALLOWED;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_ENCLOSED;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_FIELD_ACCESS;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_INFRASTRUCTURE_OVERRIDE;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_METHOD_CALL;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_NEW_CALL;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_OVERRIDE;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_PROTECTED;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_PROTECTED_CALL;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_SUBCLASS;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_SUPPORT;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_USER_LEVEL;

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

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypes;
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
public class SCJAllowedVisitor<R, P> extends SCJVisitor<R, P> {
    private final AnnotatedTypeFactory atf;
    private final AnnotatedTypes ats;
    private final String pkg;
    private final Stack<Integer> scjAllowedStack = new Stack<Integer>();
    private static final Map<String, Integer> annoValueMap = new HashMap<String, Integer>();

    private static final int LEVEL0 = 0;
    private static final int LEVEL1 = 1;
    private static final int LEVEL2 = 2;
    private static final int SUPPORT = 3;
    private static final int INFRASTRUCTURE = 4;
    private static final int HIDDEN = 5;


    static {
        annoValueMap.put("javax.safetycritical.annotate.Level.LEVEL_0", LEVEL0);
        annoValueMap.put("javax.safetycritical.annotate.Level.LEVEL_1", LEVEL1);
        annoValueMap.put("javax.safetycritical.annotate.Level.LEVEL_2", LEVEL2);
        annoValueMap.put("javax.safetycritical.annotate.Level.SUPPORT", SUPPORT);
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
        debugIndentIncrement("\nvisitClass " + node.getSimpleName());

        TypeElement t = TreeUtils.elementFromDeclaration(node);

        if (!isSCJAllowed(t) && !isEnclosingSCJAllowed(t.getEnclosingElement())) {
            debugIndentDecrement();
            return null;
        }

        if (isEscaped(t.toString()) || escapeEnum(node) || escapeAnnotation(node) ) {
            debugIndentDecrement();
            return null;
        }

        // if (hasSuppressSCJAnnotation(t)) return null;
        int level = scjAllowedLevel(t.getAnnotationMirrors());

        if (isUserLevel() && level >= SUPPORT ) {
            debugIndentDecrement();
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_USER_LEVEL), node);
            return null;
        }

        if (!scjAllowedStack.isEmpty() && scjAllowedStack.peek() > level) {
            /** tested by FakeSCJ */
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_ENCLOSED), node);
        }

        debugIndent("level : " + level);
        debugIndent("type  :" + t);

        TypeElement superType = Utils.superType(t);
        while (superType != null
                && !EscapeMap
                .isEscaped(superType.getQualifiedName().toString())) {
            if (scjAllowedLevel(superType.getAnnotationMirrors()) > level) {
                System.err.println(superType.getQualifiedName());
                System.err.println(scjAllowedLevel(superType
                        .getAnnotationMirrors())
                        + " > " + level);
                checker.report(Result.failure(ERR_SCJALLOWED_BAD_SUBCLASS), node);
            }
            superType = Utils.superType(superType);
        }
        scjAllowedStack.push(level);
        R r = super.visitClass(node, p);
        scjAllowedStack.pop();

        debugIndentDecrement();
        return r;
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
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_FIELD_ACCESS,
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
        debugIndentIncrement("\nvisitMethod " + node.getName());

        ExecutableElement methodElement = TreeUtils
        .elementFromDeclaration(node);

        checkSCJSupport(methodElement, node);
        checkSCJProtected(methodElement, node);

        int level = 0;
        if (isDefaultConstructor(node)) {
            debugIndentDecrement();
            return null;
        }
        else
            level = scjAllowedLevel(methodElement, node);

        debugIndent("\nchecking overrides ---------------------");

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
        .overriddenMethods(methodElement);
        for (ExecutableElement override : overrides.values()) {

            if (isUserLevel() && !isLegalOverride(overrides, node) && scjAllowedLevel(override, node) >= INFRASTRUCTURE ) {
                //System.out.println("error: " + override);
                //System.out.println("error: " + override.getEnclosingElement());
                checker.report(Result.failure(ERR_SCJALLOWED_BAD_INFRASTRUCTURE_OVERRIDE), node);
            }
            // TODO: what if at SCJ level INFRASTRUCTURE is overriden by a SUPPORT and then we can override it
            // at user level as well?
            // TODO: this is only temporary, we need to see what is the level of the first override that remains in SCJ package


            if (!isEscaped(override.getEnclosingElement().toString())
                    && level > scjAllowedLevel(override, node)) {
                /** Tested by OverrideTest */
                checker.report(Result.failure(ERR_SCJALLOWED_BAD_OVERRIDE), node);
            }
        }

        if (scjAllowedStack.peek() > level) {
            /** tested by FakeSCJ */
            // debugPrint("level: "+level);
            // debugPrint("peek: : "+scjAllowedStack.peek());
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_ENCLOSED), node);
        }

        scjAllowedStack.push(level);
        R r = super.visitMethod(node, p);
        scjAllowedStack.pop();

        debugIndentDecrement();
        return r;
    }

    /**
     * A legal sequence of overrides is
     *    Level 1 (user code)  ---> SUPPORT (scj code) ---> INFRASTRUCTURE (scj code)
     *
     * a illegal sequence is:
     *  Level 1 (user code)  ---> INFRASTRUCTURE (scj code)
     *
     * --> so by this we rule out all cases where an INFRASTRUCTURE method was overriden by SUPPORT or lower level in the SCJ packages
     * -------> since the methods from SCJ of this type can be overriden by a user code
     *
     * @assumption - that all the classes involved will be verified since we do not report the case
     *  Level 1 (user code)  ---> SUPPORT (scj code) ---> INFRASTRUCTURE (scj code)  --> SUPPORT (scj code)
     * ---> which is wrong but it should be reported in the class where INFRASTRUCTURE overrides SUPPORT
     *
     * @param overrides
     * @return
     */
    private boolean isLegalOverride(
            Map<AnnotatedDeclaredType, ExecutableElement> overrides,MethodTree node) {
        for (ExecutableElement override : overrides.values()) {
            if (scjAllowedLevel(override, node) <= SUPPORT)
                return true;
        }
        return false;
    }


    private boolean isDefaultConstructor(MethodTree node) {
        if (node.toString().trim().startsWith("public <init>("))
            return true;
        return false;
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        debugIndentIncrement("\nvisit method invocation :" + node);

        ExecutableElement method = TreeUtils.elementFromUse(node);

        if (isEscaped(method.getEnclosingElement().toString())) {
            debugIndentDecrement();
            return super.visitMethodInvocation(node, p);
        }

        debugIndent("peek is: " + scjAllowedStack.peek());
        debugIndent("method is: " + method);
        debugIndent("peek is: " + scjAllowedLevel(method, node));
        debugIndent("peek is: " + checkSCJProtectedCall(method, node));

        checkSCJProtectedCall(method, node);

        if (!isSCJProtected(method, node)) {
            debugIndent("peek is: " + scjAllowedStack.peek());

            debugIndent("i is: " + scjAllowedLevel(method, node));

            if (scjAllowedLevel(method, node) > scjAllowedStack.peek()) {
                /** Tested by SCJAllowedTest */
                checker.report(Result.failure(ERR_SCJALLOWED_BAD_METHOD_CALL,
                        scjAllowedStack.peek()), node);
            }

            if (scjAllowedStack.peek() == HIDDEN &&  scjAllowedLevel(method, node) < HIDDEN) {

                checker.report(Result.failure(ERR_HIDDEN_TO_SCJALLOWED,
                        scjAllowedStack.peek()), node);
            }
        }

        debugIndentDecrement();
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
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_NEW_CALL,
                    scjAllowedStack.peek()), node);
        }

        return super.visitNewClass(node, p);
    }

    @Override
    public R visitVariable(VariableTree node, P p) {
        //debugPrint(node);

        VariableElement variable = TreeUtils.elementFromDeclaration(node);

        // if (hasSuppressSCJAnnotation(variable)) return null;
        int level = scjAllowedLevel(variable, node);
        if (scjAllowedStack.peek() > level) {
            /** tested by FakeSCJ */
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_ENCLOSED), node);
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

        if (node.toString().equals("super()")) {
            if (!isSCJAllowed(method))
                return scjAllowedLevel(method.getEnclosingElement().getAnnotationMirrors());
            /*
             *
             * TODO: we can not distinguis between class that has no default constructor and a
             * class that has
             * public HighResolutionTime() {
             *       }
             * --> in this case the level should be hidden!!!! NO?
             *
            System.out.println("elements OUT:" + method.getEnclosingElement().getEnclosedElements());
            System.out.println("elements IN:" + method.getEnclosedElements());
            System.out.println("method:" + method);
            System.out.println("annot : " + method.getAnnotation(SCJAllowed.class));
            System.out.println("annotated list:" + method.getAnnotationMirrors());

            int level = scjAllowedLevel(method.getEnclosingElement().getAnnotationMirrors());

            System.out.println("class level:" + level);
             */
        }

        if (method.getSimpleName().toString().equals("<init>")) {  // we are calling constructor
            if (method.getModifiers().toString().equals("[public]"))  // and its public, so its "default"
                if (isSCJAllowed(method))
                    return scjAllowedLevel(method.getAnnotationMirrors());
                else
                    return scjAllowedLevel(method.getEnclosingElement().getAnnotationMirrors());

            //System.out.println("method:" + method);
            //System.out.println("elements OUT:" + method.getEnclosingElement().getEnclosedElements());
        }

        if (isSCJAllowed(method))
            return scjAllowedLevel(method.getAnnotationMirrors());

        for (ExecutableElement override : ats.overriddenMethods(method)
                .values()) {
            if (!isEscaped(override.getEnclosingElement().toString())
                    && isSCJAllowed(override)) {

                return scjAllowedLevel(override.getAnnotationMirrors());
            }
        }
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


                debugIndent("method:" + vals);

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
        debugIndent("annotated:" + element);
        debugIndent("annot : " + element.getAnnotation(SCJAllowed.class));
        debugIndent("annotated list:" + element.getAnnotationMirrors());
        return element.getAnnotation(SCJAllowed.class) != null;
    }

    /**
     * Returns true if element is annotated by SCJProtected
     *
     * TODO: @SCJProtected is no longer supported
     */
    private boolean isSCJProtected(Element element, Tree node) {
        if (element.getAnnotation(SCJAllowed.class) != null)
            if (scjAllowedLevel(element, node) == INFRASTRUCTURE
                    || scjAllowedLevel(element, node) == SUPPORT)
                return true;
        return false;
    }

    /**
     * Returns true if element is annotated by SCJProtected
     *
     * TODO: @SCJProtected is no longer supported
     */
    private boolean isSCJSupport(Element element, Tree node) {
        if (element.getAnnotation(SCJAllowed.class) != null)
            if (scjAllowedLevel(element, node) == SUPPORT)
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
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_PROTECTED), node);
        }
        return isValid;
    }

    /**
     *
     * @return true if currently checked class is at user level (outside of SCJ packages)
     */
    private boolean isUserLevel() {
        if (pkg.startsWith("javax.safetycritical") || pkg
        .startsWith("javax.realtime"))
            return false;
        return true;
    }

    /**
     * If the method is SCJProtected we verify that it is in these packages:
     * javax.safetycritical javax.realtime If not, we must report
     * "scjallowed.badprotectedcall" error.
     *
     * @return false - if its ilegal to call SCJProtected
     */
    private boolean checkSCJSupport(ExecutableElement methodElement, Tree node) {
        boolean isValid = !isSCJSupport(methodElement, node)
        || (pkg.startsWith("javax.safetycritical") || pkg
                .startsWith("javax.realtime"));
        if (!isValid) {
            /** Tested by TestAllowedProtectedClash */
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_SUPPORT), node);
        }

        if (isSCJSupport(methodElement, node) && isValid) {
            Utils.debugPrintln(">>is SCJ SUPPORT");
        }

        return isValid;
    }

    private boolean checkSCJlevel(ExecutableElement methodElement, Tree node) {
        boolean isValid = !isSCJlibraryLevel(methodElement, node)
        || (pkg.startsWith("javax.safetycritical") || pkg
                .startsWith("javax.realtime"));
        if (!isValid) {
            /** Tested by TestAllowedProtectedClash */
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_USER_LEVEL), node);
        }

        return isValid;
    }

    private boolean isSCJlibraryLevel(ExecutableElement element, Tree node) {
        if (element.getAnnotation(SCJAllowed.class) != null)
            if (scjAllowedLevel(element, node) >= SUPPORT)
                return true;
        return false;
    }

    private boolean checkSCJProtectedCall(ExecutableElement methodElement, Tree node) {
        boolean isValid = !isSCJProtected(methodElement, node)
        || (pkg.startsWith("javax.safetycritical") || pkg
                .startsWith("javax.realtime"));

        debugIndent("is protected "+isSCJProtected(methodElement, node));

        if (!isValid) {
            /** Tested by TestAllowedProtectedClash */
            checker.report(Result.failure(ERR_SCJALLOWED_BAD_PROTECTED_CALL), node);
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
