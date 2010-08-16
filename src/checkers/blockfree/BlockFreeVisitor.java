package checkers.blockfree;

import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.safetycritical.annotate.BlockFree;
import checkers.source.Result;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypes;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.TreeUtils;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;

// Verifies the @BlockFree annotation
// Currently checked rules:
// 1. No method call may be made from a @BlockFree method to a non @BlockFree method

@SuppressWarnings("restriction")
public class BlockFreeVisitor<R, P> extends SourceVisitor<R, P> {
    private AnnotatedTypeFactory atf;
    private AnnotatedTypes       ats;
    private boolean              currentBlockFree = false;

    public BlockFreeVisitor(BlockFreeChecker checker, CompilationUnitTree root) {
        super(checker, root);
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
    }

    public boolean isBlockFree(ExecutableElement methodElem) {
        if (methodElem.getAnnotation(BlockFree.class) != null) return true;
        Map<AnnotatedDeclaredType, ExecutableElement> overrides = ats.overriddenMethods(methodElem);
        for (ExecutableElement override : overrides.values())
            if (override.getAnnotation(BlockFree.class) != null) return true;
        return false;
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        currentBlockFree = isBlockFree(TreeUtils.elementFromDeclaration(node));
        return super.visitMethod(node, p);
    }

    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        if (currentBlockFree) {
            ExecutableElement methodElement = TreeUtils.elementFromUse(node);
            /*Tested by tests.blockfree.BlockFreeTest*/
            if (!isBlockFree(methodElement)) checker.report(Result.failure("unallowed.methodcall"), node);
        }
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public R visitNewClass(NewClassTree node, P p) {
        if (currentBlockFree) {}
        // checker.report(Result.failure("unallowed.allocation"), node);
        return super.visitNewClass(node, p);
    }
}
