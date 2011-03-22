package checkers;

import static checkers.Utils.SCJMethod.ALLOC_IN_PARENT;
import static checkers.Utils.SCJMethod.ALLOC_IN_SAME;
import static checkers.Utils.SCJMethod.DEFAULT;
import static checkers.Utils.SCJMethod.ENTER_PRIVATE_MEMORY;
import static checkers.Utils.SCJMethod.EXECUTE_IN_AREA;
import static checkers.Utils.SCJMethod.GET_CURRENT_MANAGED_MEMORY;
import static checkers.Utils.SCJMethod.GET_MEMORY_AREA;
import static checkers.Utils.SCJMethod.IMMORTAL_MEMORY_INSTANCE;
import static checkers.Utils.SCJMethod.NEW_ARRAY;
import static checkers.Utils.SCJMethod.NEW_ARRAY_IN_AREA;
import static checkers.Utils.SCJMethod.NEW_INSTANCE;
import static checkers.Utils.SCJMethod.NEW_INSTANCE_IN_AREA;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import checkers.Utils.SCJMethod;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

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

    protected final TypeMirror allocationContext = Utils.getTypeMirror(
            elements, "javax.realtime.AllocationContext");
    protected final TypeMirror immortalMemory = Utils.getTypeMirror(
            elements, "javax.realtime.ImmortalMemory");
    protected final TypeMirror interruptServiceRoutine = Utils.getTypeMirror(
            elements, "javax.realtime.InterruptServiceRoutine");
    protected final TypeMirror managedEventHandler = Utils.getTypeMirror(
            elements, "javax.safetycritical.ManagedEventHandler");
    protected final TypeMirror managedMemory = Utils.getTypeMirror(
            elements, "javax.safetycritical.ManagedMemory");
    protected final TypeMirror memoryArea = Utils.getTypeMirror(elements,
            "javax.realtime.MemoryArea");
    protected final TypeMirror mission = Utils.getTypeMirror(elements,
            "javax.safetycritical.Mission");
    protected final TypeMirror noHeapRealtimeThread = Utils.getTypeMirror(
            elements, "javax.safetycritical.NoHeapRealtimeThread");
    protected final TypeMirror scjRunnable = Utils.getTypeMirror(
            elements, "javax.safetycritical.SCJRunnable");

    protected boolean alwaysImplicitlyDefinesScope(TypeElement t) {
        TypeMirror m = t.asType();
        return types.isSubtype(m, mission)
                || types.isSubtype(m, noHeapRealtimeThread)
                || types.isSubtype(m, managedEventHandler)
                || types.isSubtype(m, interruptServiceRoutine);
    }

    protected boolean implicitlyDefinesScope(TypeElement t) {
        TypeMirror m = t.asType();
        return alwaysImplicitlyDefinesScope(t)
                || types.isSubtype(m, scjRunnable);
    }

    protected boolean needsDefineScope(TypeElement t) {
        return implementsAllocationContext(t);
    }

    protected boolean needsDefineScope(TypeMirror t) {
        return types.isSubtype(t, allocationContext);
    }

    protected boolean isImmortalMemoryType(TypeElement t) {
        return types.isSameType(t.asType(), immortalMemory);
    }

    protected boolean isMemoryAreaType(TypeElement t) {
        return types.isSubtype(t.asType(), memoryArea);
    }

    protected boolean isManagedMemoryType(TypeElement t) {
        return types.isSubtype(t.asType(), managedMemory);
    }

    protected boolean implementsAllocationContext(TypeElement t) {
        return types.isSubtype(t.asType(), allocationContext);
    }

    protected SCJMethod compareName(ExecutableElement method) {
        TypeElement type = Utils.getMethodClass(method);

        if (isManagedMemoryType(type)) {
            if (Utils.getMethodSignature(method).equals(
                    ENTER_PRIVATE_MEMORY.toString()))
                return ENTER_PRIVATE_MEMORY;
            if (Utils.getMethodSignature(method).equals(
                    ALLOC_IN_SAME.toString()))
                return ALLOC_IN_SAME;
            if (Utils.getMethodSignature(method).equals(
                    ALLOC_IN_PARENT.toString()))
                return ALLOC_IN_PARENT;
            if (Utils.getMethodSignature(method).equals(
                    GET_CURRENT_MANAGED_MEMORY.toString()))
                return GET_CURRENT_MANAGED_MEMORY;
        }
        if (implementsAllocationContext(type)) {
            if (Utils.getMethodSignature(method)
                    .equals(NEW_INSTANCE.toString()))
                return NEW_INSTANCE;
            if (Utils.getMethodSignature(method)
                    .equals(NEW_INSTANCE_IN_AREA.toString()))
                return NEW_INSTANCE_IN_AREA;
            if (Utils.getMethodSignature(method).equals(NEW_ARRAY.toString()))
                return NEW_ARRAY;
            if (Utils.getMethodSignature(method).equals(
                    NEW_ARRAY_IN_AREA.toString()))
                return NEW_ARRAY_IN_AREA;
            if (Utils.getMethodSignature(method).equals(
                    EXECUTE_IN_AREA.toString()))
                return EXECUTE_IN_AREA;
        }

        if (isMemoryAreaType(type)
                && Utils.getMethodSignature(method).equals(
                        GET_MEMORY_AREA.toString()))
            return GET_MEMORY_AREA;

        if (isImmortalMemoryType(type)
                && Utils.getMethodSignature(method).equals(
                        IMMORTAL_MEMORY_INSTANCE.toString()))
            return IMMORTAL_MEMORY_INSTANCE;

        return DEFAULT;
    }
}
