package checkers;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import com.sun.source.tree.CompilationUnitTree;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;

@SupportedOptions("debug")
public class TraceChecker extends SourceChecker {

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new TraceVisitor<Void, Void>(this, root);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        Utils.DEBUG = processingEnv.getOptions().containsKey("debug");
        super.init(processingEnv);
    }

    public static class TraceVisitor<R, P> extends SCJVisitor<R, P> {
        public TraceVisitor(SourceChecker checker, CompilationUnitTree root) {
            super(checker, root);
        }

        @Override
        public R visitAnnotatedType(com.sun.source.tree.AnnotatedTypeTree node, P p) {
            try {
                debugIndentIncrement("visitAnnotatedType");
                return super.visitAnnotatedType(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitAnnotation(com.sun.source.tree.AnnotationTree node, P p) {
            try {
                debugIndentIncrement("visitAnnotation");
                return super.visitAnnotation(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitArrayAccess(com.sun.source.tree.ArrayAccessTree node, P p) {
            try {
                debugIndentIncrement("visitArrayAccess");
                return super.visitArrayAccess(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitArrayType(com.sun.source.tree.ArrayTypeTree node, P p) {
            try {
                debugIndentIncrement("visitArrayType");
                return super.visitArrayType(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitAssert(com.sun.source.tree.AssertTree node, P p) {
            try {
                debugIndentIncrement("visitAssert");
                return super.visitAssert(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitAssignment(com.sun.source.tree.AssignmentTree node, P p) {
            try {
                debugIndentIncrement("visitAssignment");
                return super.visitAssignment(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitBinary(com.sun.source.tree.BinaryTree node, P p) {
            try {
                debugIndentIncrement("visitBinary");
                return super.visitBinary(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitBlock(com.sun.source.tree.BlockTree node, P p) {
            try {
                debugIndentIncrement("visitBlock");
                return super.visitBlock(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitBreak(com.sun.source.tree.BreakTree node, P p) {
            try {
                debugIndentIncrement("visitBreak");
                return super.visitBreak(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitCase(com.sun.source.tree.CaseTree node, P p) {
            try {
                debugIndentIncrement("visitCase");
                return super.visitCase(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitCatch(com.sun.source.tree.CatchTree node, P p) {
            try {
                debugIndentIncrement("visitCatch");
                return super.visitCatch(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitClass(com.sun.source.tree.ClassTree node, P p) {
            try {
                debugIndentIncrement("visitClass " + node.getSimpleName());
                return super.visitClass(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitCompilationUnit(CompilationUnitTree node, P p) {
            try {
                debugIndentIncrement("visitCompilationUnit");
                return super.visitCompilationUnit(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitCompoundAssignment(com.sun.source.tree.CompoundAssignmentTree node, P p) {
            try {
                debugIndentIncrement("visitCompoundAssignment");
                return super.visitCompoundAssignment(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitConditionalExpression(com.sun.source.tree.ConditionalExpressionTree node, P p) {
            try {
                debugIndentIncrement("visitConditionalExpression");
                return super.visitConditionalExpression(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitContinue(com.sun.source.tree.ContinueTree node, P p) {
            try {
                debugIndentIncrement("visitContinue");
                return super.visitContinue(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitDoWhileLoop(com.sun.source.tree.DoWhileLoopTree node, P p) {
            try {
                debugIndentIncrement("visitDoWhileLoop");
                return super.visitDoWhileLoop(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitEmptyStatement(com.sun.source.tree.EmptyStatementTree node, P p) {
            try {
                debugIndentIncrement("visitEmptyStatement");
                return super.visitEmptyStatement(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitEnhancedForLoop(com.sun.source.tree.EnhancedForLoopTree node, P p) {
            try {
                debugIndentIncrement("visitEnhancedForLoop");
                return super.visitEnhancedForLoop(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitErroneous(com.sun.source.tree.ErroneousTree node, P p) {
            try {
                debugIndentIncrement("visitErroneous");
                return super.visitErroneous(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitExpressionStatement(com.sun.source.tree.ExpressionStatementTree node, P p) {
            try {
                debugIndentIncrement("visitExpressionStatement");
                return super.visitExpressionStatement(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitForLoop(com.sun.source.tree.ForLoopTree node, P p) {
            try {
                debugIndentIncrement("visitForLoop");
                return super.visitForLoop(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitIdentifier(com.sun.source.tree.IdentifierTree node, P p) {
            try {
                debugIndentIncrement("visitIdentifier");
                return super.visitIdentifier(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitIf(com.sun.source.tree.IfTree node, P p) {
            try {
                debugIndentIncrement("visitIf");
                return super.visitIf(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitImport(com.sun.source.tree.ImportTree node, P p) {
            try {
                debugIndentIncrement("visitImport");
                return super.visitImport(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitInstanceOf(com.sun.source.tree.InstanceOfTree node, P p) {
            try {
                debugIndentIncrement("visitInstanceOf");
                return super.visitInstanceOf(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitLabeledStatement(com.sun.source.tree.LabeledStatementTree node, P p) {
            try {
                debugIndentIncrement("visitLabeledStatement");
                return super.visitLabeledStatement(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitLiteral(com.sun.source.tree.LiteralTree node, P p) {
            try {
                debugIndentIncrement("visitLiteral");
                return super.visitLiteral(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitMemberSelect(com.sun.source.tree.MemberSelectTree node, P p) {
            try {
                debugIndentIncrement("visitMemberSelect");
                return super.visitMemberSelect(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitMethod(com.sun.source.tree.MethodTree node, P p) {
            try {
                debugIndentIncrement("visitMethod");
                return super.visitMethod(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitMethodInvocation(com.sun.source.tree.MethodInvocationTree node, P p) {
            try {
                debugIndentIncrement("visitMethodInvocation");
                return super.visitMethodInvocation(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitModifiers(com.sun.source.tree.ModifiersTree node, P p) {
            try {
                debugIndentIncrement("visitModifiers");
                return super.visitModifiers(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitNewArray(com.sun.source.tree.NewArrayTree node, P p) {
            try {
                debugIndentIncrement("visitNewArray");
                return super.visitNewArray(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitNewClass(com.sun.source.tree.NewClassTree node, P p) {
            try {
                debugIndentIncrement("visitNewClass");
                return super.visitNewClass(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitOther(com.sun.source.tree.Tree node, P p) {
            try {
                debugIndentIncrement("visitOther");
                return super.visitOther(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitParameterizedType(com.sun.source.tree.ParameterizedTypeTree node, P p) {
            try {
                debugIndentIncrement("visitParameterizedType");
                return super.visitParameterizedType(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitParenthesized(com.sun.source.tree.ParenthesizedTree node, P p) {
            try {
                debugIndentIncrement("visitParenthesized");
                return super.visitParenthesized(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitPrimitiveType(com.sun.source.tree.PrimitiveTypeTree node, P p) {
            try {
                debugIndentIncrement("visitPrimitiveType");
                return super.visitPrimitiveType(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitReturn(com.sun.source.tree.ReturnTree node, P p) {
            try {
                debugIndentIncrement("visitReturn");
                return super.visitReturn(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitSwitch(com.sun.source.tree.SwitchTree node, P p) {
            try {
                debugIndentIncrement("visitSwitch");
                return super.visitSwitch(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitSynchronized(com.sun.source.tree.SynchronizedTree node, P p) {
            try {
                debugIndentIncrement("visitSynchronized");
                return super.visitSynchronized(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitThrow(com.sun.source.tree.ThrowTree node, P p) {
            try {
                debugIndentIncrement("visitThrow");
                return super.visitThrow(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitTry(com.sun.source.tree.TryTree node, P p) {
            try {
                debugIndentIncrement("visitTry");
                return super.visitTry(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitTypeCast(com.sun.source.tree.TypeCastTree node, P p) {
            try {
                debugIndentIncrement("visitTypeCast");
                return super.visitTypeCast(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitTypeParameter(com.sun.source.tree.TypeParameterTree node, P p) {
            try {
                debugIndentIncrement("visitTypeParameter");
                return super.visitTypeParameter(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitUnary(com.sun.source.tree.UnaryTree node, P p) {
            try {
                debugIndentIncrement("visitUnary");
                return super.visitUnary(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitVariable(com.sun.source.tree.VariableTree node, P p) {
            try {
                debugIndentIncrement("visitVariable");
                return super.visitVariable(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitWhileLoop(com.sun.source.tree.WhileLoopTree node, P p) {
            try {
                debugIndentIncrement("visitWhileLoop");
                return super.visitWhileLoop(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
        @Override
        public R visitWildcard(com.sun.source.tree.WildcardTree node, P p) {
            try {
                debugIndentIncrement("visitWildcard");
                return super.visitWildcard(node, p);
            } finally {
                debugIndentDecrement();
            }
        }
    }
}
