package checkers.scope;

import static checkers.scope.SchedulableChecker.*;

import java.util.HashSet;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
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
 * This visitor is checking that SChedulables are instantiated in Mission.init
 * methods and in these methods only.
 *
 * TODO: each schedulable can be instantiated only once! Need to chatch the
 * instantiations in loops.!
 */
public class SchedulableVisitor extends SCJVisitor<Void, Void> {
    private ScopeCheckerContext ctx;

    HashSet<TypeMirror> schedulables;

    public SchedulableVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        this.ctx = ctx;
        schedulables = new HashSet();
    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        TypeElement t = TreeUtils.elementFromDeclaration(node);

        if (!(isSchedulable(t) || isMissionSequencer(t) || isCyclicExecutive(t))) {
            return super.visitClass(node, p);
        }

        if (isSchedulable(t) || isMissionSequencer(t)) {
            DefineScope df = getDefineScope(t);
            ScopeInfo scope = checkClassScope(t, df, node, node);

            if (isSchedulable(t))
                checkRunsIn(t, scope, df, node);
        }

        return super.visitClass(node, p);
    }

    private ScopeInfo checkClassScope(TypeElement t, DefineScope df,
            ClassTree node, Tree errNode) {
        debugIndentIncrement("checkClassScope: " + t);
        if (ctx.getClassScope(t) == null || ctx.getClassScope(t).isCaller())
            fail(ERR_NO_SCOPE, node);

        ScopeInfo scope = scopeOfClassDefinition(t);
        ScopeInfo dfParent = new ScopeInfo(df.parent());

        if (!scope.equals(dfParent)) {
            fail(ERR_SCOPE_DEFINESCOPE_MISMATCH, node);
        }

        return scope;
    }

    private DefineScope getDefineScope(TypeElement t) {
        return t.getAnnotation(DefineScope.class);
    }

    private void checkRunsIn(TypeElement t, ScopeInfo scope, DefineScope df,
            ClassTree node) {
        ScopeInfo runsIn = getSchedulableRunsIn(t);

        if (runsIn == null || runsIn.isReservedScope()) {
            fail(ERR_SCHEDULABLE_NO_RUNS_IN, node);
            return;
        }

        ScopeInfo child = new ScopeInfo(df.name());
        if (!runsIn.equals(child)) {
            fail(ERR_SCHEDULABLE_RUNS_IN_MISMATCH, node, child, runsIn);
        }
    }

    private ScopeInfo getSchedulableRunsIn(TypeElement t) {
        switch (SCJSchedulable.fromMethod(t.asType(), elements, types)) {
        case PEH:
        case APEH:
            return ctx.getMethodRunsIn(t.toString(),
                    SCJSchedulable.PEH.signature);
        case MANAGED_THREAD:
            return ctx.getMethodRunsIn(t.toString(),
                    SCJSchedulable.MANAGED_THREAD.signature);
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
            isInitialization = true;
            break;
        case MISSION:
            isInitialization = true;
            checkMissionInit(node);
            break;
        case MS_NEXT_MISSION:
            checkMSnextMission(node);
            break;
        default:
            break;
        }

        Void res = super.visitMethod(node, p);
        if (isInitialization)
            isInitialization = false;

        return res;
    }

    /**
     * Mission.initialize() CANNOT override @RunsIn.
     */
    private void checkMissionInit(MethodTree node) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        ScopeInfo runsIn = ctx.getMethodRunsIn(m.getEnclosingElement()
                .toString(), SCJMission.MISSION.signature);
        if (runsIn == null)
            throw new RuntimeException(
                    "ERR: Every method must have @RunsIn value. Bug.");
        if (!runsIn.isThis())
            fail(ERR_MISSION_INIT_RUNS_IN_MISMATCH, node, runsIn);
    }

    private void checkMSnextMission(MethodTree node) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        ScopeInfo runsIn = ctx.getMethodRunsIn(m.getEnclosingElement()
                .toString(), SCJMission.MS_NEXT_MISSION.signature);
        if (runsIn == null)
            throw new RuntimeException(
                    "ERR: Every method must have @RunsIn value. Bug.");
        TypeElement e = (TypeElement) m.getEnclosingElement();
        DefineScope df = getDefineScope(e);
        if (df == null)
            throw new RuntimeException("ERR: @DefineScope on class expected");

        if (!df.name().equals(runsIn.toString())) {
            fail(ERR_MISSION_SEQUENCER_RUNS_IN, node, runsIn, df.name());
        }
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {

        ExecutableElement ctorElement = TreeUtils.elementFromUse(node);
        TypeMirror tt = Utils.getMethodClass(ctorElement).asType();
        switch (SCJSchedulable.fromMethod(Utils.getMethodClass(ctorElement)
                .asType(), elements, types)) {
        case PEH:
        case APEH:
        case MANAGED_THREAD:
            if (!isInitialization)
                fail(ERR_SCHED_INIT_OUT_OF_INIT_METH, node);
            else if (schedulables.contains(tt))
                fail(ERR_SCHEDULABLE_MULTI_INIT, node);
            else
                schedulables.add(tt);
        default:
        }

        return super.visitNewClass(node, p);
    }
}
