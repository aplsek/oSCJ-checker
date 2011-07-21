package checkers;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.safetycritical.Mission;
import javax.safetycritical.annotate.Scope;

import checkers.scope.ScopeInfo;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.TypesUtils;

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
    protected final TypeMirror interruptServiceRoutine = Utils.getTypeMirror(
            elements, "javax.realtime.InterruptServiceRoutine");
    protected final TypeMirror managedEventHandler = Utils.getTypeMirror(
            elements, "javax.safetycritical.ManagedEventHandler");
    protected final TypeMirror managedMemory = Utils.getTypeMirror(elements,
            "javax.safetycritical.ManagedMemory");
    protected final TypeMirror mission = Utils.getTypeMirror(elements,
            "javax.safetycritical.Mission");
    protected final TypeMirror noHeapRealtimeThread = Utils.getTypeMirror(
            elements, "javax.realtime.NoHeapRealtimeThread");
    protected final TypeMirror missionSequencer = Utils.getTypeMirror(elements,
            "javax.safetycritical.MissionSequencer");
    protected final TypeMirror cyclicExecutive = Utils.getTypeMirror(elements,
            "javax.safetycritical.CyclicExecutive");

    protected final TypeMirror safetycriticalSchedulable = Utils.getTypeMirror(
            elements, "javax.safetycritical.Schedulable");

    protected final TypeMirror realtimeSchedulable = Utils.getTypeMirror(
            elements, "javax.realtime.Schedulable");

    protected final TypeMirror safelet = Utils.getTypeMirror(elements,
            "javax.safetycritical.Safelet");

    protected final TypeMirror runnable = Utils.getTypeMirror(elements,
            "java.lang.Runnable");
    protected final TypeMirror managedThread = Utils.getTypeMirror(elements,
            "javax.safetycritical.ManagedThread");

    protected boolean alwaysImplicitlyDefinesScope(TypeElement t) {
        TypeMirror m = t.asType();
        return isSubtype(m, noHeapRealtimeThread)
                || isSubtype(m, managedEventHandler)
                || isSubtype(m, missionSequencer)
                || isSubtype(m, cyclicExecutive)
                || isSubtype(m, interruptServiceRoutine)
                || isSubtype(m, realtimeSchedulable);
    }

    protected boolean implicitlyDefinesScope(TypeElement t) {
        TypeMirror m = t.asType();
        return alwaysImplicitlyDefinesScope(t);
    }

    protected boolean isSafelet(TypeElement t) {
        TypeMirror m = t.asType();

        return isSubtype(m, safelet);
    }

    protected boolean isSubtypeOfRunnable(TypeElement t) {
        TypeMirror m = t.asType();
        return isSubtype(m, runnable);
    }

    protected boolean isSchedulable(TypeElement t) {
        TypeMirror m = t.asType();

        //pln("\t t:" + m);
        //pln("\t t:" + t.getInterfaces());
        //pln("\t sch:" + realtimeSchedulable);

        return isSubtype(m, realtimeSchedulable);
    }

    protected boolean isMissionSequencer(TypeElement t) {
        TypeMirror m = t.asType();

        return isSubtype(m, missionSequencer);
    }

    protected boolean isCyclicExecutive(TypeElement t) {
        TypeMirror m = t.asType();
        return isSubtype(m, cyclicExecutive);
    }

    protected boolean needsDefineScope(TypeElement t) {
        return implementsAllocationContext(t);
    }

    protected boolean needsDefineScope(TypeMirror t) {
        return isSubtype(t, allocationContext);
    }

    protected boolean isManagedMemoryType(TypeElement t) {
        return isSubtype(t.asType(), managedMemory);
    }

    protected boolean implementsAllocationContext(TypeElement t) {
        return isSubtype(t.asType(), allocationContext);
    }

    protected boolean isRunnable(TypeElement t) {
        TypeMirror m = t.asType();
        return isSameType(m, runnable);
    }

    protected boolean isRunnableSubtype(TypeElement t) {
        TypeMirror m = t.asType();
        return isSubtype(m, runnable);
    }

    protected boolean isManagedThread(TypeElement t) {
        TypeMirror m = t.asType();
        return isSubtype(m, managedThread);
    }

    protected static ScopeInfo scopeOfClassDefinition(TypeElement t) {
        Scope scopeAnn = t.getAnnotation(Scope.class);
        return scopeAnn != null ? new ScopeInfo(scopeAnn.value())
                : ScopeInfo.CALLER;
    }

    public boolean isSubtype(TypeMirror t, TypeMirror sup) {
        return types.isSubtype(types.erasure(t), types.erasure(sup));
    }

    public boolean isSameType(TypeMirror t, TypeMirror sup) {
        return types.isSameType(types.erasure(t), types.erasure(runnable));

    }


    /**
     * Determines if the type is for an anonymous type or not
     *
     * @param type  type to be checked
     * @return  true iff type is an anonymous type
     */
    public boolean isAnonymousType(AnnotatedTypeMirror type) {
        return TypesUtils.isAnonymousType(type.getUnderlyingType());
    }

    // DEBUG:
    protected static void pln(String str) {
        System.out.println("\t" + str);
    }
}
