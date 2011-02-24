package checkers.scjRestricted;

import static checkers.scjAllowed.EscapeMap.escapeAnnotation;
import static checkers.scjAllowed.EscapeMap.escapeEnum;
import static checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE;
import static checkers.scjRestricted.SCJRestrictedChecker.ERR_ILLEGAL_OVERRIDE1;
import static checkers.scjRestricted.SCJRestrictedChecker.ERR_TOO_MANY_VALUES;
import static checkers.scjRestricted.SCJRestrictedChecker.ERR_UNALLOWED_ALLOCATION;
import static checkers.scjRestricted.SCJRestrictedChecker.ERR_UNALLOWED_FOREACH;
import static checkers.scjRestricted.SCJRestrictedChecker.ERR_UNALLOWED_METHODCALL;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.Phase;
import javax.safetycritical.annotate.SCJRestricted;

import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.Result;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypes;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;

// Verifies the @BlockFree annotation
// Currently checked rules:
// 1. No method call may be made from a @BlockFree method to a non @BlockFree method

public class SCJRestrictedVisitor<R, P> extends SCJVisitor<R, P> {
    private AnnotatedTypeFactory atf;
    private AnnotatedTypes       ats;
    private boolean              currentBlockFree    = false;
    private boolean              currentAllocFree    = false;
    private Phase             currentWhen;

    private EnumSet<Phase>    whenRestricts       = EnumSet.of(Phase.ALL, Phase.CLEANUP,
                                                         Phase.RUN, Phase.INITIALIZATION);

    public SCJRestrictedVisitor(SCJRestrictedChecker checker, CompilationUnitTree root) {
        super(checker, root);
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
    }

    private EnumSet<Phase> getSCJRestrictionsNoRecurse(ExecutableElement m) {
        SCJRestricted s = m.getAnnotation(SCJRestricted.class);
        if (s != null) {
            return EnumSet.copyOf(Arrays.asList(s.value()));
        } else {
            return EnumSet.noneOf(Phase.class);
        }
    }

    private EnumSet<Phase> getSCJRestrictions(ExecutableElement m, MethodTree errorNode) {
        EnumSet<Phase> rs = EnumSet.noneOf(Phase.class);
        for (ExecutableElement o : orderedOverriddenMethods(m)) {
            rs.addAll(getSCJRestrictionsNoRecurse(o));
        }
        EnumSet<Phase> currentRs = getSCJRestrictionsNoRecurse(m);
        if (errorNode != null) {
            if (containsAny(currentRs, whenRestricts)) {
                if (rs.contains(Phase.CLEANUP) && (currentRs.contains(Phase.INITIALIZATION) || currentRs.contains(Phase.RUN))) {
                    checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE, "CLEANUP", "INITIALIZATION or RUN"), errorNode);
                }
                if (rs.contains(Phase.RUN) && (currentRs.contains(Phase.CLEANUP) || currentRs.contains(Phase.INITIALIZATION))) {
                    checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE, "RUN", "CLEANUP or INITIALIZATION"), errorNode);
                }
                if (rs.contains(Phase.INITIALIZATION) && (currentRs.contains(Phase.CLEANUP) || currentRs.contains(Phase.RUN))) {
                    checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE, "INITIALIZATION", "CLEANUP or RUN"), errorNode);
                }
                if (rs.contains(Phase.ALL) && !currentRs.contains(Phase.ALL)) {
                    checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE, "ALL", "CLEANUP, RUN, or INITIALIZATION"), errorNode);
                }
            }
        }

        addFirstOrDefault(currentRs, rs, whenRestricts, Phase.ALL);
        return currentRs;
    }

    private void addFirstOrDefault(EnumSet<Phase> currentRs, EnumSet<Phase> rs, EnumSet<Phase> addFrom,
            Phase defVal) {
        if (!containsAny(currentRs, addFrom)) {
            for (Phase r : addFrom) {
                if (rs.contains(r)) {
                    currentRs.add(r);
                    return;
                }
            }
            currentRs.add(defVal);
        }
    }

    private <K extends Enum<K>> boolean containsAny(EnumSet<K> s, Collection<K> a) {
        for (K k : a) {
            if (s.contains(k)) { return true; }
        }
        return false;
    }

    private void checkSanity(MethodTree node, EnumSet<Phase> rs) {
        boolean anyTime = rs.contains(Phase.ALL), cleanup = rs.contains(Phase.CLEANUP), initialize = rs
            .contains(Phase.INITIALIZATION);
        // At least two phase restrictions
        if ((anyTime && (cleanup || initialize)) || (cleanup && initialize)) {
            checker.report(Result.failure(ERR_TOO_MANY_VALUES, "ALL, CLEANUP, INITIALIZATION"), node);
        }
    }

    // Check naively to see if an assignment is AllocFree. Anything that can
    // be directly caught (e.g. new objects, new methods, invocation of non-
    // @AllocFree methods) is treated as AllocFree here.
    public boolean isAllocFree(Element varElem, ExpressionTree rhs) {
        if (currentAllocFree) {
            if (rhs != null && TypesUtils.isBoxedPrimitive(varElem.asType())) {
                switch (rhs.getKind()) {
                case IDENTIFIER:
                case MEMBER_SELECT:
                case METHOD_INVOCATION:
                    // TODO: Just type check to make sure that the expression type is the boxed primitive
                    // Method invocations will be visited and checked for an @AllocFree,
                    // so just return true for now. The other kinds are clearly non-allocating.
                    return true;
                }
                return false;
            } else if (rhs != null && TypesUtils.isDeclaredOfName(varElem.asType(), "java.lang.String")) {
                switch (rhs.getKind()) {
                case IDENTIFIER:
                case MEMBER_SELECT:
                case METHOD_INVOCATION:
                case STRING_LITERAL:
                    // Method invocations will be visited and checked for an @AllocFree,
                    // so just return true for now. The other kinds are clearly non-allocating.
                    return true;
                }
                return false;
            }
        }
        // This method just checks for autoboxing; if an object is allocated later,
        // it'll be seen, so just return true for now.
        return true;
    }

    /**
     * Just for Debugging...
     */
    @Override
    public R visitClass(ClassTree node, P p) {
        //System.out.println("class:\n" + node);

        //System.out.println("class:\n" + node.getMembers());

        debugIndentIncrement("\nvisitClass " + node.getSimpleName() );
        if (escapeEnum(node) || escapeAnnotation(node)) {
            debugIndentDecrement();
            return null;
        }

        R r = super.visitClass(node, p);
        debugIndentDecrement();
        return r;
    }

    @Override
    public R visitAnnotation(AnnotationTree node, P p) {
        boolean oldAllocFree = currentAllocFree;
        currentAllocFree = false;
        R r = super.visitAnnotation(node, p);
        currentAllocFree = oldAllocFree;
        return r;
    }

    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        debugIndentIncrement("\n visit assignment " + node );

        // TODO: need to check for string concatenation
        if (currentAllocFree) {

            //System.out.println("\n visit assignment " + node);
            //System.out.println("\n variable " + node.getVariable());

            if (node.getVariable().getKind() == Kind.ARRAY_ACCESS) {
                //TODO:
                //System.out.println("\n visit assignment " + node);
                //System.out.println("\n variable " + node.getVariable());
                //System.out.println("\n variable kind " + node.getVariable().getKind());
                //System.out.println("\n lhs " + lhs);
                //System.out.println("\n lhs " + lhs.getKind());
                //Element varElem = null;
                //if (node.getKind() == Kind.ARRAY_ACCESS)
                //    varElem = TreeUtils.elementFromUse(lhs.);

                debugIndentDecrement();
                return super.visitAssignment(node, p);
            }


            Tree lhs = node.getVariable();
            while (lhs.getKind() == Kind.ARRAY_ACCESS) {
                lhs = ((ArrayAccessTree) lhs).getExpression();
            }

            Element varElem = TreeUtils.elementFromUse(node.getVariable());

            if (!isAllocFree(varElem, node.getExpression()))
            /** Tested by tests/allocfree/AutoboxAlloc.java */
            checker.report(Result.failure(ERR_UNALLOWED_ALLOCATION), node.getExpression());
        }

        debugIndentDecrement();
        return super.visitAssignment(node, p);
    }

    // Compound assignment is apparently i += 2, and so on. Easy check, just see if it's an autoboxed type.
    @Override
    public R visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        if (currentAllocFree) {
            Element varElem = TreeUtils.elementFromUse(node.getVariable());
            if (TypesUtils.isBoxedPrimitive(varElem.asType()))
            /** Tested by the CompoundAssignementTest */
            checker.report(Result.failure(ERR_UNALLOWED_ALLOCATION), node.getExpression());
        }
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public R visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        /** Tested by tests/allocfree/ForeachAlloc */
        if (currentAllocFree) checker.report(Result.failure(ERR_UNALLOWED_FOREACH), node);
        return super.visitEnhancedForLoop(node, p);
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        debugIndentIncrement("\n visitMethod " + node);

        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        EnumSet<Phase> rs = getSCJRestrictions(m, node);
        currentBlockFree = !isSelfSuspend(m,node);
        currentAllocFree = !isMayAllocate(m,node);
        currentWhen = Phase.ALL;


        for (Phase r : rs) {
            if (whenRestricts.contains(r)) {
                currentWhen = r;
                break;
            }
        }
        checkSanity(node, rs); // not necessary?

        R r = super.visitMethod(node, p);
        currentBlockFree = currentAllocFree = false;
        currentWhen = null;

        debugIndentDecrement();
        return r;
    }

    /**
     * Returns if method is selfSuspend
     * - checks that overidden methods have according annotations
     * - if overriden methods have annotations but this method does not, its an error!
     * Inheritance must be restated!
     *
     * @param methodElement
     * @return
     */
    private boolean isSelfSuspend(ExecutableElement methodElement, Tree node) {
        SCJRestricted r;
        boolean result = false;
        if ((r = methodElement.getAnnotation(SCJRestricted.class)) != null)
            if (r.maySelfSuspend())
                result =  true;

        // check overrides:
        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
        .overriddenMethods(methodElement);
        for (ExecutableElement override : overrides.values()) {
            //System.out.println("override: " +override);

            if (r == null && getSelfSuspend(override)) {
                checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE1), node);
                System.out.println("ERROR");
                break;
            }

            if (!result && getSelfSuspend(override)) {
                checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE, "maySelfSuspend=true", "maySelfSuspend=false"), node);
                System.out.println("ERROR");
                break;
            }
        }

        return result;
    }

    private boolean getSelfSuspend(ExecutableElement methodElement) {
        SCJRestricted r;
        if ((r = methodElement.getAnnotation(SCJRestricted.class)) != null) {
            return r.maySelfSuspend();
        }
        return false;
    }


    /**
     * Returns if method is mayAllocate
     * - checks that overridden methods have according annotations
     * - if overridden methods have annotations but this method does not, its an error!
     * Inheritance must be restated!
     *
     * @param methodElement
     * @return
     */
    private boolean isMayAllocate(ExecutableElement methodElement, Tree node) {
        SCJRestricted r;
        boolean result = true;
        if ((r = methodElement.getAnnotation(SCJRestricted.class)) != null)
            if (!r.mayAllocate())
                result = false;

        //check overrides:
        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats
        .overriddenMethods(methodElement);
        for (ExecutableElement override : overrides.values()) {

            //System.out.println("override: " +override);
            //System.out.println("r: " +r);
            //System.out.println("over alloc: " +getMayAllocate(override));
            if (r == null && !getMayAllocate(override)) {
                checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE1), node);
                break;
            }

            if (result && !getMayAllocate(override)) {
                checker.report(Result.failure(ERR_ILLEGAL_OVERRIDE, "mayAllocate=false", "mayAllocate=true"), node);
                break;
            }
        }


        return result;
    }

    private boolean getMayAllocate(ExecutableElement methodElement) {
        SCJRestricted r;
        if ((r = methodElement.getAnnotation(SCJRestricted.class)) != null) {
            if (r.mayAllocate())
                return true;
            else
                return false;
        }
        return true;
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        debugIndentIncrement("\n visitMethodInvocation " + node);

        ExecutableElement methodElement = TreeUtils.elementFromUse(node);
        EnumSet<Phase> rs = getSCJRestrictions(methodElement, null);

        if (currentBlockFree && isSelfSuspend(methodElement, node)) {
            checker.report(Result.failure(ERR_UNALLOWED_METHODCALL, "MAY_BLOCK", "BLOCK_FREE"), node);
        }
        if (currentAllocFree && isMayAllocate(methodElement, node)) {
            checker.report(Result.failure(ERR_UNALLOWED_METHODCALL, "MAY_ALLOCATE", "ALLOCATE_FREE"), node);
        }
        if (currentWhen == Phase.CLEANUP && (rs.contains(Phase.RUN) || rs.contains(Phase.INITIALIZATION))) {
            checker.report(Result.failure(ERR_UNALLOWED_METHODCALL, "RUN or INITIALIZATION", "CLEANUP"), node);
        }
        if (currentWhen == Phase.RUN && (rs.contains(Phase.CLEANUP) || rs.contains(Phase.INITIALIZATION))) {
            checker.report(Result.failure(ERR_UNALLOWED_METHODCALL, "CLEANUP or INITIALIZATION", "RUN"), node);
        }
        if (currentWhen == Phase.INITIALIZATION && (rs.contains(Phase.CLEANUP) || rs.contains(Phase.RUN))) {
            checker.report(Result.failure(ERR_UNALLOWED_METHODCALL, "CLEANUP or RUN", "INITIALIZATION"), node);
        }
        if (currentWhen == Phase.ALL) {
            if (rs.contains(Phase.CLEANUP) || rs.contains(Phase.RUN) || rs.contains(Phase.INITIALIZATION)) {
                checker.report(Result.failure(ERR_UNALLOWED_METHODCALL, "CLEANUP, RUN, or INITIALIZATION", "ALL"), node);
            }
        }
        List<? extends VariableElement> parameters = methodElement.getParameters();
        List<? extends ExpressionTree> arguments = node.getArguments();
        for (int i = 0; i < parameters.size(); i++) {
            if (!isAllocFree(parameters.get(i), arguments.get(i))) checker.report(Result
                .failure(ERR_UNALLOWED_ALLOCATION), arguments.get(i));
        }

        debugIndentDecrement();
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public R visitNewArray(NewArrayTree node, P p) {
        /** Tested by tests/allocfree/NewAlloc.java */
        if (currentAllocFree) checker.report(Result.failure(ERR_UNALLOWED_ALLOCATION), node);
        return super.visitNewArray(node, p);
    }

    @Override
    public R visitNewClass(NewClassTree node, P p) {
        /**
         * Tested by tests/allocfree/MethodCallsAbstract.java and tests/allocfree/MethodCallsInterface.java and
         * tests/allocfree/NewAlloc.java
         */
        if (currentAllocFree) checker.report(Result.failure(ERR_UNALLOWED_ALLOCATION), node);
        return super.visitNewClass(node, p);
    }

    @Override
    public R visitVariable(VariableTree node, P p) {
        if (currentAllocFree) {
            Element varElem = TreeUtils.elementFromDeclaration(node);
            if (!isAllocFree(varElem, node.getInitializer()))
            /** Tested by tests/allocfree/AutoboxAlloc.java and tests/allocfree/StringAlloc.java */
            checker.report(Result.failure(ERR_UNALLOWED_ALLOCATION), node.getInitializer());
        }
        return super.visitVariable(node, p);
    }

    // Like ats.overriddenMethods(), except not a map and is guaranteed to iterate in hierarchical order.
    private Collection<ExecutableElement> orderedOverriddenMethods(ExecutableElement method) {
        TypeElement enclosing = (TypeElement) method.getEnclosingElement();

        if (enclosing.getKind() == ElementKind.INTERFACE) { return orderedOverriddenMethodsInterface(method, enclosing); }

        LinkedList<ExecutableElement> overrides = new LinkedList<ExecutableElement>();
        if (!TypesUtils.isObject(enclosing.asType())) {
            TypeElement superType = Utils.superType(enclosing);
            if (superType == null)
                return overrides;    // BUG - sometimes super type is null

            HashSet<TypeElement> seenIfaces = new HashSet<TypeElement>();
            addInterfaceOverrides(enclosing, method, overrides, seenIfaces);
            while (!TypesUtils.isObject(superType.asType())) {
                for (ExecutableElement superMethod : Utils.methodsIn(superType)) {
                    if (elements.overrides(method, superMethod, superType)) {
                        overrides.add(superMethod);
                        break;
                    }
                }
                addInterfaceOverrides(superType, method, overrides, seenIfaces);
                superType = Utils.superType(superType);
            }
        }
        return overrides;
    }

    // Breadth first search of interfaces to find overridden methods.
    private Collection<ExecutableElement> orderedOverriddenMethodsInterface(ExecutableElement method, TypeElement iface) {
        LinkedList<ExecutableElement> overrides = new LinkedList<ExecutableElement>();
        LinkedList<TypeElement> work = new LinkedList<TypeElement>();
        work.add(iface);
        while (!work.isEmpty()) {
            TypeElement workIface = work.removeFirst();
            List<? extends TypeMirror> workSuperIfaces = workIface.getInterfaces();
            for (TypeMirror superIface : workSuperIfaces) {
                if (superIface.getKind() == TypeKind.DECLARED) {
                    work.addLast((TypeElement) ((DeclaredType) superIface).asElement());
                }
            }
            // Assuming a method overrides itself, we want to skip iface.
            if (workIface != iface) {
                for (ExecutableElement workIfaceMethod : Utils.methodsIn(workIface)) {
                    if (elements.overrides(method, workIfaceMethod, workIface)) {
                        overrides.addLast(workIfaceMethod);
                    }
                }
            }
        }
        return overrides;
    }

    private void addInterfaceOverrides(TypeElement t, ExecutableElement m, Collection<ExecutableElement> overrides,
            HashSet<TypeElement> seenIfaces) {
        for (TypeMirror iface : t.getInterfaces()) {
            TypeElement ifaceElem = (TypeElement) ((DeclaredType) iface).asElement();
            if (seenIfaces.add(ifaceElem)) {
                for (ExecutableElement ifaceMethod : Utils.methodsIn(ifaceElem)) {
                    if (elements.overrides(m, ifaceMethod, ifaceElem)) {
                        overrides.add(ifaceMethod);
                        break;
                    }
                }
                overrides.addAll(orderedOverriddenMethodsInterface(m, ifaceElem));
            }
        }
    }
}
