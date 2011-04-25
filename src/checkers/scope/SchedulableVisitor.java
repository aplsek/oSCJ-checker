package checkers.scope;


import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_NO_RUNS_IN;
import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_NO_SCOPE;
import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_RUNS_IN_MISMATCH;
import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_SCOPE_DEFINESCOPE_MISMATCH;
import static checkers.scope.SchedulableChecker.ERR_SCHED_INIT_OUT_OF_INIT_METH;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.safetycritical.annotate.DefineScope;

import checkers.SCJMission;
import checkers.SCJSchedulable;
import checkers.SCJVisitor;
import checkers.Utils;
import checkers.source.SourceChecker;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;

/**
 * This visitor is responsible for retrieving Scope and RunsIn annotations from
 * classes and methods and making sure they are valid. This information is
 * stored into a context object so that the ScopeVisitor doesn't have to deal
 * with retrieving this information.
 */
public class SchedulableVisitor extends SCJVisitor<Void, Void> {
    private ScopeCheckerContext ctx;

    public SchedulableVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        this.ctx = ctx;
    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);

        if (!isSchedulable(t))
            return super.visitClass(node, p);


        DefineScope df = getDefineScope(t);
        ScopeInfo scope = checkClassScope(t,df,node,node);

        checkRunsIn(t,scope,df,node);

        return super.visitClass(node, p);
    }

    private ScopeInfo checkClassScope(TypeElement t, DefineScope df, ClassTree node, Tree errNode) {
        debugIndentIncrement("checkClassScope: " + t);
        if (ctx.getClassScope(t) == null )
            fail(ERR_SCHEDULABLE_NO_SCOPE,node);

        ScopeInfo scope = scopeOfClassDefinition(t);
        ScopeInfo dfParent = new ScopeInfo(df.parent());

        if (!scope.equals(dfParent)) {
            fail(ERR_SCHEDULABLE_SCOPE_DEFINESCOPE_MISMATCH,node);
        }

        return scope;
    }

    private DefineScope getDefineScope(TypeElement t) {
        return t.getAnnotation(DefineScope.class);
    }

    private void checkRunsIn(TypeElement t,ScopeInfo scope, DefineScope df, ClassTree node) {
        ScopeInfo runsIn = getSchedulableRunsIn(t);

        if (runsIn == null) {
            fail(ERR_SCHEDULABLE_NO_RUNS_IN,node);
            return;
        }

        ScopeInfo child = new ScopeInfo(df.name());

        if (!runsIn.equals(child))
            fail(ERR_SCHEDULABLE_RUNS_IN_MISMATCH,node,child,runsIn);
    }



    private ScopeInfo getSchedulableRunsIn(TypeElement t) {
        switch (SCJSchedulable.fromMethod(t.asType(), elements, types)) {
        case PEH:
        case APEH:
            return ctx.getMethodRunsIn(t.toString(), SCJSchedulable.PEH.signature);
        case MANAGED_THREAD:
            return ctx.getMethodRunsIn(t.toString(), SCJSchedulable.MANAGED_THREAD.signature);
        default:
            return null;
        }
    }

    private boolean isInitialization = false;

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        switch (SCJMission.fromMethod(m, elements, types)) {
        case CYCLIC_EXECUTIVE:
        case MISSION:
            isInitialization = true;
        default:
        }

        Void res = super.visitMethod(node, p);
        if (isInitialization)
            isInitialization = false;

        return res;
    }


    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        if (!isInitialization) {
            ExecutableElement ctorElement = TreeUtils.elementFromUse(node);
            Utils.getMethodClass(ctorElement).asType();
            switch (SCJSchedulable.fromMethod(Utils.getMethodClass(ctorElement)
                    .asType(), elements, types)) {
            case PEH:
            case APEH:
            case MANAGED_THREAD:
                fail(ERR_SCHED_INIT_OUT_OF_INIT_METH, node);
            default:
            }
        }
        return super.visitNewClass(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        //boolean oldInitialization = isInitialization;
        //isInitialization = false;
        if (isInitialization) {
            // TODO:
        }


        Void res = super.visitMethodInvocation(node, p);
        //isInitialization = oldInitialization;
        return res;
    }
}
