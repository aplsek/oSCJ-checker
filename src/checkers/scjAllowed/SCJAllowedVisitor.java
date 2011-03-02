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
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_PROTECTED_CALL;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_SUBCLASS;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_SUPPORT;
import static checkers.scjAllowed.SCJAllowedChecker.ERR_SCJALLOWED_BAD_USER_LEVEL;
import static javax.safetycritical.annotate.Level.HIDDEN;
import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;
import static javax.safetycritical.annotate.Level.LEVEL_0;
import static javax.safetycritical.annotate.Level.LEVEL_1;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;

import java.util.Map;
import java.util.Stack;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

import checkers.SCJVisitor;
import checkers.Utils;
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
public class SCJAllowedVisitor<R, P> extends SCJVisitor<R, P> {
    private final AnnotatedTypeFactory atf;
    private final AnnotatedTypes ats;
    private final Stack<Level> scjAllowedStack = new Stack<Level>();

    public SCJAllowedVisitor(SourceChecker checker, CompilationUnitTree root) {
        super(checker, root);
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
    }

    /**
     * Verifying that the class has an appropriate SCJAllowed level.
     */
    @Override
    public R visitClass(ClassTree node, P p) {
        debugIndentIncrement("visitClass " + node.getSimpleName());

        TypeElement t = TreeUtils.elementFromDeclaration(node);

        if (!isSCJAllowed(t) && !isEnclosingSCJAllowed(t.getEnclosingElement())) {
            debugIndentDecrement();
            return null;
        }

        if (isEscaped(t.toString()) || escapeEnum(node)
                || escapeAnnotation(node)) {
            debugIndentDecrement();
            return null;
        }

        // if (hasSuppressSCJAnnotation(t)) return null;
        Level level = scjAllowedLevel(t);

        if (Utils.isUserLevel(t) && level.compareTo(SUPPORT) >= 0) {
            debugIndentDecrement();
            fail(ERR_SCJALLOWED_BAD_USER_LEVEL, node);
            return null;
        }

        if (!scjAllowedStack.isEmpty() && topLevel().compareTo(level) > 0)
            /** tested by FakeSCJ */
            fail(ERR_SCJALLOWED_BAD_ENCLOSED, node);

        debugIndent("level : " + level);
        debugIndent("type  :" + t);

        TypeElement s = Utils.superType(t);
        while (s != null
                && !EscapeMap.isEscaped(s.getQualifiedName().toString())) {
            if (scjAllowedLevel(s).compareTo(level) > 0) {
                fail(ERR_SCJALLOWED_BAD_SUBCLASS, node);
            }
            s = Utils.superType(s);
        }
        scjAllowedStack.push(level);
        R r = super.visitClass(node, p);
        scjAllowedStack.pop();

        debugIndentDecrement();
        return r;
    }

    @Override
    public R visitMemberSelect(MemberSelectTree node, P p) {
        Element f = TreeUtils.elementFromUse(node);

        if (f.getEnclosingElement() != null
                && isEscaped(f.getEnclosingElement().toString()))
            return super.visitMemberSelect(node, p);

        if (f.getKind() == ElementKind.FIELD
                && scjAllowedLevel(f, node).compareTo(topLevel()) > 0)
            /** Tested by SCJAllowedTest */
            fail(ERR_SCJALLOWED_BAD_FIELD_ACCESS, node, topLevel());
        return super.visitMemberSelect(node, p);
    }

    /**
     * Errors: - we can not override a method by a method with a higher
     * SCJallowed level
     */
    @Override
    public R visitMethod(MethodTree node, P p) {
        debugIndentIncrement("visitMethod " + node.getName());

        ExecutableElement m = TreeUtils.elementFromDeclaration(node);

        checkSCJSupport(m, node);

        Level level = LEVEL_0;
        if (isDefaultConstructor(node)) {
            debugIndentDecrement();
            return null;
        } else
            level = scjAllowedLevel(m, node);

        debugIndent("checking overrides ---------------------");

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
                .overriddenMethods(m);
        for (ExecutableElement override : overrides.values()) {
            if (Utils.isUserLevel(m)
                    && !isLegalOverride(overrides, node)
                    && scjAllowedLevel(override, node)
                            .compareTo(INFRASTRUCTURE) >= 0)
                fail(ERR_SCJALLOWED_BAD_INFRASTRUCTURE_OVERRIDE, node);

            if (!isEscaped(override.getEnclosingElement().toString())
                    && level.compareTo(scjAllowedLevel(override, node)) > 0)
                /** Tested by OverrideTest */
                fail(ERR_SCJALLOWED_BAD_OVERRIDE, node);
        }

        if (topLevel().compareTo(level) > 0)
            /** tested by FakeSCJ */
            fail(ERR_SCJALLOWED_BAD_ENCLOSED, node);

        scjAllowedStack.push(level);
        R r = super.visitMethod(node, p);
        scjAllowedStack.pop();

        debugIndentDecrement();
        return r;
    }

    /**
     * A legal sequence of overrides is Level 1 (user code) ---> SUPPORT (scj
     * code) ---> INFRASTRUCTURE (scj code)
     *
     * a illegal sequence is: Level 1 (user code) ---> INFRASTRUCTURE (scj code)
     *
     * --> so by this we rule out all cases where an INFRASTRUCTURE method was
     * overriden by SUPPORT or lower level in the SCJ packages -------> since
     * the methods from SCJ of this type can be overriden by a user code
     *
     * @assumption - that all the classes involved will be verified since we do
     *             not report the case Level 1 (user code) ---> SUPPORT (scj
     *             code) ---> INFRASTRUCTURE (scj code) --> SUPPORT (scj code)
     *             ---> which is wrong but it should be reported in the class
     *             where INFRASTRUCTURE overrides SUPPORT
     *
     * @param overrides
     * @return
     */
    private boolean isLegalOverride(
            Map<AnnotatedDeclaredType, ExecutableElement> overrides,
            MethodTree node) {
        for (ExecutableElement override : overrides.values())
            if (scjAllowedLevel(override, node).compareTo(SUPPORT) <= 0)
                return true;
        return false;
    }

    private boolean isDefaultConstructor(MethodTree node) {
        if (node.toString().trim().startsWith("public <init>("))
            return true;
        return false;
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        debugIndentIncrement("visit method invocation :" + node);

        ExecutableElement m = TreeUtils.elementFromUse(node);

        if (isEscaped(m.getEnclosingElement().toString())) {
            debugIndentDecrement();
            return super.visitMethodInvocation(node, p);
        }

        checkSCJInternalCall(m, node);

        if (!isSCJInternal(m, node)) {
            if (scjAllowedLevel(m, node).compareTo(topLevel()) > 0)
                /** Tested by SCJAllowedTest */
                fail(ERR_SCJALLOWED_BAD_METHOD_CALL, node, topLevel());

            if (topLevel() == HIDDEN
                    && scjAllowedLevel(m, node).compareTo(HIDDEN) < 0)
                fail(ERR_HIDDEN_TO_SCJALLOWED, node, topLevel());
        }

        debugIndentDecrement();
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public R visitNewClass(NewClassTree node, P p) {
        // debugPrint(node);

        if (node.toString().contains("@SuppressSCJ"))
            return null;

        ExecutableElement ctor = TreeUtils.elementFromUse(node);

        if (isEscaped(ctor.getEnclosingElement().toString()))
            return super.visitNewClass(node, p);

        if (checkSCJSupport(ctor, node)
                && scjAllowedLevel(ctor, node).compareTo(topLevel()) > 0)
            /** tested by SuppressTest */
            fail(ERR_SCJALLOWED_BAD_NEW_CALL, node, topLevel());

        return super.visitNewClass(node, p);
    }

    @Override
    public R visitVariable(VariableTree node, P p) {
        VariableElement v = TreeUtils.elementFromDeclaration(node);

        // if (hasSuppressSCJAnnotation(variable)) return null;
        Level level = scjAllowedLevel(v, node);
        if (topLevel().compareTo(level) > 0)
            /** tested by FakeSCJ */
            fail(ERR_SCJALLOWED_BAD_ENCLOSED, node);
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
    private Level getGuardLevel(IfTree node) {
        if (node.toString().startsWith("if (Safelet.getDeploymentLevel() == 0"))
            return LEVEL_0;
        else if (node.toString().startsWith(
                "if (Safelet.getDeploymentLevel() == 1"))
            return LEVEL_1;
        else if (node.toString().startsWith(
                "if (Safelet.getDeploymentLevel() == 2"))
            return LEVEL_2;
        return SUPPORT; // TODO: Why was this support?
    }

    /**
     * Extracting SCJAllowed LEVEL value from the executable element Default
     * returns HIDDEN.
     */
    private Level scjAllowedLevel(ExecutableElement m, Tree node) {
        TypeElement t = Utils.getMethodClass(m);
        if (isEscaped(t.getQualifiedName().toString()))
            return EscapeMap.escape.get(t.getQualifiedName().toString());

        if (node.toString().equals("super()"))
            if (!isSCJAllowed(m))
                return scjAllowedLevel(t);
        /*
         *
         * TODO: we can not distinguish between class that has no default
         * constructor and a class that has public HighResolutionTime() { } -->
         * in this case the level should be hidden!!!! NO?
         *
         * System.out.println("elements OUT:" +
         * method.getEnclosingElement().getEnclosedElements());
         * System.out.println("elements IN:" + method.getEnclosedElements());
         * System.out.println("method:" + method); System.out.println("annot : "
         * + method.getAnnotation(SCJAllowed.class));
         * System.out.println("annotated list:" +
         * method.getAnnotationMirrors());
         *
         * int level =
         * scjAllowedLevel(method.getEnclosingElement().getAnnotationMirrors());
         *
         * System.out.println("class level:" + level);
         */

        if (m.getSimpleName().toString().equals("<init>"))
            if (Utils.isPublic(m.getModifiers()))
                if (isSCJAllowed(m))
                    return scjAllowedLevel(m);
                else
                    return scjAllowedLevel(t);

        if (isSCJAllowed(m))
            return scjAllowedLevel(m);

        for (ExecutableElement override : ats.overriddenMethods(m).values())
            if (!isEscaped(override.getEnclosingElement().toString())
                    && isSCJAllowed(override))
                return scjAllowedLevel(override);
        return getEnclosingLevel(m);
    }

    /**
     * extracting SCJAllowed LEVEL value from the Field element Default returns
     * HIDDEN.
     */
    private Level scjAllowedLevel(Element f, Tree node) {
        if (isSCJAllowed(f))
            return scjAllowedLevel(f);
        return getEnclosingLevel(f);
    }

    /**
     * extracting SCJAllowed LEVEL value from a list of annotations if
     * SCJAllowed non-specified we are level 0? if in SCJ API returns 0 if not
     * in SCJ API returns HIDDEN if no annotation: if in SCJ returns HIDDEN else
     * returns 0
     */
    private Level scjAllowedLevel(Element e) {
        SCJAllowed a = e.getAnnotation(SCJAllowed.class);
        return a == null ? HIDDEN : a.value();
    }

    /**
     * @assert: should be called only on methods in SCJ package
     * @recursive: is recursive, searches through enclosing elements until the
     *             SCJAllowed is find, otherwise returns HIDDEN
     * @return if the enclosing member has SCJAllowed(members=true) we return
     *         its LEVEL as the level of the methodElement else we return HIDDEN
     */
    private Level getEnclosingLevel(Element m) {
        Element e = m.getEnclosingElement();

        if (e == null)
            return HIDDEN;

        if (isSCJAllowed(e))
            if (scjAllowedMembers(e))
                return scjAllowedLevel(e);
            else
                return HIDDEN;
        return getEnclosingLevel(e);
        // recursive call, this enclosing was not allocated, so we search even
        // higher
    }

    /**
     * extracting SCJAllowed MEMBERS value from the element
     */
    private boolean scjAllowedMembers(Element e) {
        SCJAllowed a = e.getAnnotation(SCJAllowed.class);
        return a == null ? false : a.members();
    }

    /**
     * Returns true if element is annotated by SCJAllowed
     */
    private boolean isSCJAllowed(Element e) {
        return e.getAnnotation(SCJAllowed.class) != null;
    }

    private boolean isSCJInternal(Element e, Tree node) {
        if (e.getAnnotation(SCJAllowed.class) != null)
            return scjAllowedLevel(e, node) == INFRASTRUCTURE
                    || scjAllowedLevel(e, node) == SUPPORT;
        return false;
    }

    private boolean isSCJSupport(Element e, Tree node) {
        if (e.getAnnotation(SCJAllowed.class) != null)
            if (scjAllowedLevel(e, node) == SUPPORT)
                return true;
        return false;
    }

    private boolean checkSCJSupport(ExecutableElement m, Tree node) {
        boolean isValid = !isSCJSupport(m, node) || !Utils.isUserLevel(m);

        if (!isValid) {
            // If we're in the user level with an SUPPORT annotation, we have
            // to see if the method overrides something in SCJ.
            for (AnnotatedDeclaredType a : ats.overriddenMethods(m).keySet()) {
                TypeElement t = Utils.getTypeElement(a.getUnderlyingType());
                if (!Utils.isUserLevel(t)) {
                    isValid = true;
                    break;
                }
            }
        }
        if (!isValid)
            /** Tested by TestAllowedProtectedClash */
            fail(ERR_SCJALLOWED_BAD_SUPPORT, node);

        if (isSCJSupport(m, node) && isValid)
            Utils.debugPrintln(">>is SCJ SUPPORT");

        return isValid;
    }

    private boolean checkSCJInternalCall(ExecutableElement m, Tree node) {
        boolean isValid = !isSCJInternal(m, node) || !Utils.isUserLevel(m);

        debugIndent("is protected " + isSCJInternal(m, node));

        if (!isValid)
            /** Tested by TestAllowedProtectedClash */
            fail(ERR_SCJALLOWED_BAD_PROTECTED_CALL, node);
        return isValid;
    }

    /**
     * returns true if any enclosing element is SCJAllowed
     */
    private boolean isEnclosingSCJAllowed(Element e) {
        if (isSCJAllowed(e))
            return true;
        else if (e.getEnclosingElement() != null)
            return isEnclosingSCJAllowed(e.getEnclosingElement());
        else
            return false;
    }

    private Level topLevel() {
        return scjAllowedStack.peek();
    }
}
