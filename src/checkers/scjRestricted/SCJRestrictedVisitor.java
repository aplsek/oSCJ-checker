package checkers.scjRestricted;

import static javax.lang.model.util.ElementFilter.methodsIn;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.annotate.Restrict;
import javax.safetycritical.annotate.SCJRestricted;
import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypes;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;

// Verifies the @BlockFree annotation
// Currently checked rules:
// 1. No method call may be made from a @BlockFree method to a non @BlockFree method

@SuppressWarnings("restriction")
public class SCJRestrictedVisitor<R, P> extends SourceVisitor<R, P> {
    private AnnotatedTypeFactory atf;
    private AnnotatedTypes       ats;
    private boolean              currentBlockFree    = false;
    private boolean              currentAllocFree    = false;
    private Restrict             currentWhen;
    private EnumSet<Restrict>    allocRestricts      = EnumSet.of(Restrict.ALLOCATE_FREE, Restrict.MAY_ALLOCATE);
    private EnumSet<Restrict>    blockRestricts      = EnumSet.of(Restrict.BLOCK_FREE, Restrict.MAY_BLOCK);
    private EnumSet<Restrict>    whenRestricts       = EnumSet.of(Restrict.ANY_TIME, Restrict.CLEANUP,
                                                         Restrict.EXECUTION, Restrict.INITIALIZATION);

    public SCJRestrictedVisitor(SCJRestrictedChecker checker, CompilationUnitTree root) {
        super(checker, root);
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
    }

    private EnumSet<Restrict> getSCJRestrictionsNoRecurse(ExecutableElement m) {
        SCJRestricted s = m.getAnnotation(SCJRestricted.class);
        if (s != null) {
            return EnumSet.copyOf(Arrays.asList(s.value()));
        } else {
            return EnumSet.noneOf(Restrict.class);
        }
    }

    private EnumSet<Restrict> getSCJRestrictions(ExecutableElement m, MethodTree errorNode) {
        EnumSet<Restrict> rs = EnumSet.noneOf(Restrict.class);
        for (ExecutableElement o : orderedOverriddenMethods(m)) {
            rs.addAll(getSCJRestrictionsNoRecurse(o));
        }
        EnumSet<Restrict> currentRs = getSCJRestrictionsNoRecurse(m);
        if (errorNode != null) {
            if (rs.contains(Restrict.ALLOCATE_FREE) && currentRs.contains(Restrict.MAY_ALLOCATE)) {
                checker.report(Result.failure("illegal.override", "ALLOCATE_FREE", "MAY_ALLOCATE"), errorNode);
            }
            if (rs.contains(Restrict.BLOCK_FREE) && currentRs.contains(Restrict.MAY_BLOCK)) {
                checker.report(Result.failure("illegal.override", "BLOCK_FREE", "MAY_BLOCK"), errorNode);
            }
            if (containsAny(currentRs, whenRestricts)) {
                if (rs.contains(Restrict.CLEANUP) && (currentRs.contains(Restrict.INITIALIZATION) || currentRs.contains(Restrict.EXECUTION))) {
                    checker.report(Result.failure("illegal.override", "CLEANUP", "INITIALIZATION or EXECUTION"), errorNode);
                }
                if (rs.contains(Restrict.EXECUTION) && (currentRs.contains(Restrict.CLEANUP) || currentRs.contains(Restrict.INITIALIZATION))) {
                    checker.report(Result.failure("illegal.override", "EXECUTION", "CLEANUP or INITIALIZATION"), errorNode);
                }
                if (rs.contains(Restrict.INITIALIZATION) && (currentRs.contains(Restrict.CLEANUP) || currentRs.contains(Restrict.EXECUTION))) {
                    checker.report(Result.failure("illegal.override", "INITIALIZATION", "CLEANUP or EXECUTION"), errorNode);
                }
                if (rs.contains(Restrict.ANY_TIME) && !currentRs.contains(Restrict.ANY_TIME)) {
                    checker.report(Result.failure("illegal.override", "ANY_TIME", "CLEANUP, EXECUTION, or INITIALIZATION"), errorNode);
                }
            }
        }
        addFirstOrDefault(currentRs, rs, allocRestricts, Restrict.MAY_ALLOCATE);
        addFirstOrDefault(currentRs, rs, blockRestricts, Restrict.MAY_BLOCK);
        addFirstOrDefault(currentRs, rs, whenRestricts, Restrict.ANY_TIME);
        return currentRs;
    }

    private void addFirstOrDefault(EnumSet<Restrict> currentRs, EnumSet<Restrict> rs, EnumSet<Restrict> addFrom,
            Restrict defVal) {
        if (!containsAny(currentRs, addFrom)) {
            for (Restrict r : addFrom) {
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

    private void checkSanity(MethodTree node, EnumSet<Restrict> rs) {
        if (rs.containsAll(allocRestricts)) {
            checker.report(Result.failure("too.many.values", "ALLOCATE_FREE, MAY_ALLOCATE"), node);
        }
        if (rs.containsAll(blockRestricts)) {
            checker.report(Result.failure("too.many.values", "BLOCK_FREE, MAY_BLOCK"), node);
        }

        boolean anyTime = rs.contains(Restrict.ANY_TIME), cleanup = rs.contains(Restrict.CLEANUP), initialize = rs
            .contains(Restrict.INITIALIZATION);
        // At least two phase restrictions
        if ((anyTime && (cleanup || initialize)) || (cleanup && initialize)) {
            checker.report(Result.failure("too.many.values", "ANY_TIME, CLEANUP, INITIALIZATION"), node);
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
    
    public R visitAnnotation(AnnotationTree node, P p) {
        boolean oldAllocFree = currentAllocFree;
        currentAllocFree = false;
        R r = super.visitAnnotation(node, p);
        currentAllocFree = oldAllocFree;
        return r;
    }

    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        // TODO: need to check for string concatenation
        if (currentAllocFree) {
            Element varElem = TreeUtils.elementFromUse(node.getVariable());
            if (!isAllocFree(varElem, node.getExpression()))
            /** Tested by tests/allocfree/AutoboxAlloc.java */
            checker.report(Result.failure("unallowed.allocation"), node.getExpression());
        }
        return super.visitAssignment(node, p);
    }

    // Compound assignment is apparently i += 2, and so on. Easy check, just see if it's an autoboxed type.
    @Override
    public R visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        if (currentAllocFree) {
            Element varElem = TreeUtils.elementFromUse(node.getVariable());
            if (TypesUtils.isBoxedPrimitive(varElem.asType()))
            /** Tested by the CompoundAssignementTest */
            checker.report(Result.failure("unallowed.allocation"), node.getExpression());
        }
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public R visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        /** Tested by tests/allocfree/ForeachAlloc */
        if (currentAllocFree) checker.report(Result.failure("unallowed.foreach"), node);
        return super.visitEnhancedForLoop(node, p);
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        EnumSet<Restrict> rs = getSCJRestrictions(m, node);
        currentBlockFree = rs.contains(Restrict.BLOCK_FREE);
        currentAllocFree = rs.contains(Restrict.ALLOCATE_FREE);
        currentWhen = Restrict.ANY_TIME;
        for (Restrict r : rs) {
            if (whenRestricts.contains(r)) {
                currentWhen = r;
                break;
            }
        }
        checkSanity(node, rs); // not necessary?

        R r = super.visitMethod(node, p);
        currentBlockFree = currentAllocFree = false;
        currentWhen = null;
        return r;
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        ExecutableElement methodElement = TreeUtils.elementFromUse(node);
        EnumSet<Restrict> rs = getSCJRestrictions(methodElement, null);
        if (currentBlockFree && rs.contains(Restrict.MAY_BLOCK)) {
            checker.report(Result.failure("unallowed.methodcall", "MAY_BLOCK", "BLOCK_FREE"), node);
        }
        if (currentAllocFree && rs.contains(Restrict.MAY_ALLOCATE)) {
            checker.report(Result.failure("unallowed.methodcall", "MAY_ALLOCATE", "ALLOCATE_FREE"), node);
        }
        if (currentWhen == Restrict.CLEANUP && (rs.contains(Restrict.EXECUTION) || rs.contains(Restrict.INITIALIZATION))) {
            checker.report(Result.failure("unallowed.methodcall", "EXECUTION or INITIALIZATION", "CLEANUP"), node);
        }
        if (currentWhen == Restrict.EXECUTION && (rs.contains(Restrict.CLEANUP) || rs.contains(Restrict.INITIALIZATION))) {
            checker.report(Result.failure("unallowed.methodcall", "CLEANUP or INITIALIZATION", "EXECUTION"), node);
        }
        if (currentWhen == Restrict.INITIALIZATION && (rs.contains(Restrict.CLEANUP) || rs.contains(Restrict.EXECUTION))) {
            checker.report(Result.failure("unallowed.methodcall", "CLEANUP or EXECUTION", "INITIALIZATION"), node);
        }
        if (currentWhen == Restrict.ANY_TIME) {
            if (rs.contains(Restrict.CLEANUP) || rs.contains(Restrict.EXECUTION) || rs.contains(Restrict.INITIALIZATION)) {
                checker.report(Result.failure("unallowed.methodcall", "CLEANUP, EXECUTION, or INITIALIZATION", "ANY_TIME"), node);
            }
        }
        List<? extends VariableElement> parameters = methodElement.getParameters();
        List<? extends ExpressionTree> arguments = node.getArguments();
        for (int i = 0; i < parameters.size(); i++) {
            if (!isAllocFree(parameters.get(i), arguments.get(i))) checker.report(Result
                .failure("unallowed.allocation"), arguments.get(i));
        }
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public R visitNewArray(NewArrayTree node, P p) {
        /** Tested by tests/allocfree/NewAlloc.java */
        if (currentAllocFree) checker.report(Result.failure("unallowed.allocation"), node);
        return super.visitNewArray(node, p);
    }

    @Override
    public R visitNewClass(NewClassTree node, P p) {
        /**
         * Tested by tests/allocfree/MethodCallsAbstract.java and tests/allocfree/MethodCallsInterface.java and
         * tests/allocfree/NewAlloc.java
         */
        if (currentAllocFree) checker.report(Result.failure("unallowed.allocation"), node);
        return super.visitNewClass(node, p);
    }

    @Override
    public R visitVariable(VariableTree node, P p) {
        if (currentAllocFree) {
            Element varElem = TreeUtils.elementFromDeclaration(node);
            if (!isAllocFree(varElem, node.getInitializer()))
            /** Tested by tests/allocfree/AutoboxAlloc.java and tests/allocfree/StringAlloc.java */
            checker.report(Result.failure("unallowed.allocation"), node.getInitializer());
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
            HashSet<TypeElement> seenIfaces = new HashSet<TypeElement>();
            addInterfaceOverrides(enclosing, method, overrides, seenIfaces);
            while (!TypesUtils.isObject(superType.asType())) {
                for (ExecutableElement superMethod : methodsIn(superType.getEnclosedElements())) {
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
                for (ExecutableElement workIfaceMethod : methodsIn(workIface.getEnclosedElements())) {
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
                for (ExecutableElement ifaceMethod : methodsIn(ifaceElem.getEnclosedElements())) {
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
