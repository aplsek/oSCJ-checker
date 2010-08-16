package checkers.allocfree;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
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

// Verifies the @AllocFree annotation
// Currently checked rules:
// 1. No object or array allocations are allowed in an @AllocFree method
// 2. Method invocations must be @AllocFree if the call site method is @AllocFree
// 3. Autoboxed primitives may not allocate
// 4. Foreach loops may not be used in @AllocFree methods
// 5. String concatenation

@SuppressWarnings("restriction")
public class AllocFreeVisitor<R, P> extends SourceVisitor<R, P> {
    private final AnnotatedTypeFactory atf;
    private final AnnotatedTypes       ats;
    private boolean                    currentAllocFree = false;

    public AllocFreeVisitor(AllocFreeChecker checker, CompilationUnitTree root) {
        super(checker, root);
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
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

    @Override
    public R visitAnnotation(AnnotationTree node, P p) {
        // debugPrint("visitAnnotation");
        return super.visitAnnotation(node, p);
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

    /**
     * compoundAssignment is apparently i += 2, and so on. Easy check, just see if it's an autoboxed type.
     */
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
        currentAllocFree = Utils.isAllocFree(TreeUtils.elementFromDeclaration(node), ats);
        return super.visitMethod(node, p);
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        if (currentAllocFree) {
            ExecutableElement methodElement = TreeUtils.elementFromUse(node);
            /** Tested by tests/allocFree/MethodCalls and tests/allocFree/MethodCallsInheritance */
            if (!Utils.isAllocFree(methodElement, ats)) {
                checker.report(Result.failure("unallowed.methodcall"), node);
            }
            List<? extends VariableElement> parameters = methodElement.getParameters();
            List<? extends ExpressionTree> arguments = node.getArguments();
            for (int i = 0; i < parameters.size(); i++) {
                if (!isAllocFree(parameters.get(i), arguments.get(i))) checker.report(Result
                    .failure("unallowed.allocation"), arguments.get(i));
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public R visitNewArray(NewArrayTree node, P p) {
        if (currentAllocFree)
        /** Tested by tests/allocfree/NewAlloc.java */
        checker.report(Result.failure("unallowed.allocation"), node);
        return super.visitNewArray(node, p);
    }

    @Override
    public R visitNewClass(NewClassTree node, P p) {
        if (currentAllocFree)
        /**
         * Tested by tests/allocfree/MethodCallsAbstract.java and tests/allocfree/MethodCallsInterface.java and
         * tests/allocfree/NewAlloc.java
         */
        checker.report(Result.failure("unallowed.allocation"), node);
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
}
