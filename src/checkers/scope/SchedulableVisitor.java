package checkers.scope;


import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_NO_SCOPE;
import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_SCOPE_DEFINESCOPE_MISMATCH;
import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_NO_RUNS_IN;
import static checkers.scope.SchedulableChecker.ERR_SCHEDULABLE_RUNS_IN_MISMATCH;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.safetycritical.annotate.DefineScope;

import checkers.SCJMission;
import checkers.SCJMethod;
import checkers.SCJSchedulable;
import checkers.SCJVisitor;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypes;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * This visitor is responsible for retrieving Scope and RunsIn annotations from
 * classes and methods and making sure they are valid. This information is
 * stored into a context object so that the ScopeVisitor doesn't have to deal
 * with retrieving this information.
 */
public class SchedulableVisitor extends SCJVisitor<Void, Void> {
    private ScopeCheckerContext ctx;
    private ScopeTree scopeTree;
    private AnnotatedTypeFactory atf;
    private AnnotatedTypes ats;

    public SchedulableVisitor(SourceChecker checker, CompilationUnitTree root,
            ScopeCheckerContext ctx) {
        super(checker, root);
        this.ctx = ctx;
        scopeTree = ctx.getScopeTree();
        atf = checker.createFactory(root);
        ats = new AnnotatedTypes(checker.getProcessingEnvironment(), atf);
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
            pln("\n\n scope:" + scope + ", dfparent:" + dfParent );
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
        switch (SCJSchedulable.fromMethod(t, elements, types)) {
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
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        switch (SCJMission.fromMethod(TreeUtils.elementFromUse(node), elements, types)) {
        case CYCLIC_EXECUTIVE:
        case MISSION:
            isInitialization = true;
        default:
        }

        Void res = super.visitMethodInvocation(node, p);
        if (isInitialization)
            isInitialization = false;

        return res;

    }


    private void pln(String str) {System.out.println(str);}

    /**
     * Get a method or field's owning class.
     */
    private ScopeInfo getEnclosingClassScope(Element e) {
        return ctx.getClassScope((TypeElement) e.getEnclosingElement());
    }
}
