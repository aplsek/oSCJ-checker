package checkers;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;

public class SCJVisitor<R, P> extends SourceVisitor<R, P> {
    public SCJVisitor(SourceChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    protected void debugIndentDecrement() {
        Utils.decreaseIndent();
    }

    protected void debugIndentIncrement(String s) {
        Utils.debugPrintln(s);
        Utils.increaseIndent();
    }

    protected void debugIndent(String s) {
        Utils.debugPrintln(s);
    }

    protected void fail(String msg, Tree src, Object... msgParams) {
        checker.report(Result.failure(msg, msgParams), src);
    }

    protected void warn(String msg, Tree src, Object... msgParams) {
        checker.report(Result.warning(msg, msgParams), src);
    }

    protected final TypeMirror allocationContextMirror = Utils.getTypeMirror(
            elements, "javax.realtime.AllocationContext");
    protected final TypeMirror memoryAreaMirror = Utils.getTypeMirror(elements,
            "javax.realtime.MemoryArea");
    protected final TypeMirror managedMemoryMirror = Utils.getTypeMirror(
            elements, "javax.safetycritical.ManagedMemory");

    //Our SCJ-lib does not implement "javax.realtime.ScopeAllocationContext".
    //protected final TypeMirror scopeAllocationContextMirror = Utils.getTypeMirror(
    //        elements, "javax.realtime.ScopeAllocationContext");

    protected boolean needsDefineScope(TypeElement t) {
        return implementsAllocationContext(t);
    }

    protected boolean isMemoryAreaType(TypeElement t) {
        return types.isSubtype(t.asType(), memoryAreaMirror);
    }

    protected boolean implementsAllocationContext(TypeElement t) {
        return types.isSubtype(t.asType(), allocationContextMirror);
    }

    //protected boolean implementsScopedAllocationContext(TypeElement t) {
    //    return types.isSubtype(t.asType(), scopeAllocationContextMirror);
    // }

    protected boolean isManagedMemoryType(TypeElement t) {
        return types.isSubtype(t.asType(), managedMemoryMirror);
    }
}
