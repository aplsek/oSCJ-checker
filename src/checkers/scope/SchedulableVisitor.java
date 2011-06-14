package checkers.scope;

import static checkers.scope.SchedulableChecker.*;

import java.util.HashSet;
import java.util.Hashtable;

import javax.lang.model.element.Element;
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

    Hashtable<TypeMirror,HashSet> schedulables;

    public SchedulableVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        this.ctx = ctx;
        schedulables = new Hashtable();
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
    TypeMirror currentMission = null;


    @Override
    public Void visitMethod(MethodTree node, Void p) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);

        switch (SCJMission.fromMethod(m, elements, types)) {
        case CYCLIC_EXECUTIVE_INIT:
            isInitialization = true;
            currentMission = Utils.getMethodClass(TreeUtils.elementFromDeclaration(node)).asType();
            checkCyclicExecutiveInit(node);
            break;
        case MISSION_INIT:
            isInitialization = true;
            currentMission = Utils.getMethodClass(TreeUtils.elementFromDeclaration(node)).asType();
            checkMissionInit(node);
            break;
        case MS_NEXT_MISSION:
            checkMSnextMission(node);
            break;
        case SAFELET_GET_SEQUENCER:
        case SAFELET_SET_UP:
        case SAFELET_TEAR_DOWN:
            checkSafeletMethod(node);
        default:
            break;
        }

        Void res = super.visitMethod(node, p);
        if (isInitialization) {
            isInitialization = false;
            currentMission = null;
        }

        return res;
    }

    private void checkSafeletMethod(MethodTree node) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);

        ScopeInfo runsIn = ctx.getMethodRunsIn(m.getEnclosingElement()
                .toString(), m.toString().substring(0,m.toString().length()-2));
        if (!runsIn.isThis() && !runsIn.isImmortal())
            fail(ERR_SAFELET_RUNSIN, node, runsIn);
    }

    private void checkCyclicExecutiveInit(MethodTree node) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        ScopeInfo runsIn = ctx.getMethodRunsIn(m.getEnclosingElement()
                .toString(), SCJMission.CYCLIC_EXECUTIVE_INIT.signature);
        DefineScope ds = getDefineScope(m);
        if (ds == null)
            throw new RuntimeException("ERROR: @DefineScope expected on CyclicExecutive.");
        if (!runsIn.toString().equals(ds.name()))
            fail(ERR_CYCLIC_EXEC_INIT_RUNS_IN_MISMATCH, node, ds.name());
    }

    private DefineScope getDefineScope(ExecutableElement m) {
        Element clazz = m.getEnclosingElement();
        DefineScope ds = Utils.getDefineScope(clazz);
        if (ds == null)
            throw new RuntimeException("ERROR: @DefineScope expected on CyclicExecutive.");
        return ds;
    }

    /**
     * Mission.initialize() CANNOT override @RunsIn.
     */
    private void checkMissionInit(MethodTree node) {
        ExecutableElement m = TreeUtils.elementFromDeclaration(node);
        ScopeInfo runsIn = ctx.getMethodRunsIn(m.getEnclosingElement()
                .toString(), SCJMission.MISSION_INIT.signature);
        if (runsIn == null)
            throw new RuntimeException(
                    "ERR: Every method must have @RunsIn value. Bug.");

        // handling the case when the class "extends Mission implements Safelet"
        if (isMissionAndSafelet(m)) {
            DefineScope ds = getDefineScope(m);
            if (!runsIn.toString().equals(ds.name()))
                fail(ERR_MISSION_INIT_RUNS_IN_MISMATCH, node, ds.name(),runsIn);
        } else if (!runsIn.isThis())
            fail(ERR_MISSION_INIT_RUNS_IN_MISMATCH, node, ScopeInfo.THIS, runsIn);
    }

    private boolean isMissionAndSafelet(ExecutableElement m) {
        TypeMirror t = Utils.getMethodClass(m).asType();
        if (types.isSubtype(t,safelet) )
            return true;
        return false;
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
        switch (SCJSchedulable.fromMethod(tt, elements, types)) {
        case PEH:
        case APEH:
        case MANAGED_THREAD:
            if (!isInitialization)
                fail(ERR_SCHED_INIT_OUT_OF_INIT_METH, node);
            else {
                HashSet ss = schedulables.get(currentMission);
                if (ss != null) {
                    if (ss.contains(tt))
                        fail(ERR_SCHEDULABLE_MULTI_INIT, node);
                    else
                        ss.add(tt);
                } else {
                    ss = new HashSet();
                    ss.add(tt);
                    schedulables.put(currentMission, ss);
                }
            }
        default:
        }

        return super.visitNewClass(node, p);
    }
}
