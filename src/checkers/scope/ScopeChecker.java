package checkers.scope;

import java.util.Properties;

import javax.annotation.processing.SupportedOptions;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;


@SupportedOptions("debug")
public class ScopeChecker extends SinglePassChecker {
    // Please keep alphabetized
    public static final String ERR_BAD_ALLOCATION = "bad.allocation";
    public static final String ERR_BAD_ANNOTATION = "bad.annotation";
    public static final String ERR_BAD_ASSIGNMENT_SCOPE = "bad.assignment.scope";
    public static final String ERR_BAD_ASSIGNMENT_PRIVATE_MEM = "bad.assignment.private.mem";
    public static final String ERR_BAD_ENTER_PARAM = "bad.enter.param";
    public static final String ERR_BAD_ENTER_PRIVATE_MEM_NO_RUNS_IN = "bad.enterPrivateMem.no.runsIn";
    public static final String ERR_BAD_ENTER_PRIVATE_MEM_NO_SCOPE_ON_RUNNABLE = "bad.enterPrivateMem.no.Scope.on.Runnable";
    public static final String ERR_BAD_ENTER_PRIVATE_MEM_RUNS_IN_NO_MATCH = "bad.enterPrivateMem.runsIn.no.match";
    public static final String ERR_BAD_ENTER_TARGET = "bad.enter.target";
    public static final String ERR_BAD_EXECUTE_IN_AREA_OR_ENTER = "bad.executeInArea.or.enter";
    public static final String ERR_BAD_EXECUTE_IN_AREA_TARGET = "bad.executeInArea.target";
    public static final String ERR_BAD_FIELD_SCOPE = "bad.field.scope";
    public static final String ERR_BAD_GUARD_ARGUMENT = "bad.guard.argument";
    public static final String ERR_BAD_GUARD_NO_FINAL = "bad.guard.no.final";
    public static final String ERR_BAD_METHOD_INVOKE = "bad.method.invoke";
    public static final String ERR_BAD_NEW_INSTANCE = "bad.newInstance";
    public static final String ERR_BAD_RETURN_SCOPE = "bad.return.scope";
    public static final String ERR_BAD_RETURN_TYPE = "bad.return.type";
    public static final String ERR_BAD_RUNS_IN_METHOD = "bad.runs.in.method";
    public static final String ERR_BAD_RUNS_IN_ON_CTOR = "runs.in.on.ctor";
    public static final String ERR_BAD_RUNS_IN_OVERRIDE = "bad.runs.in.override";
    public static final String ERR_BAD_VARIABLE_SCOPE = "bad.variable.scope";
    public static final String ERR_CHECKER_BUG = "checker.bug";
    public static final String ERR_DEFAULT_BAD_ENTER_PARAMETER = "default.bad.enter.parameter";
    public static final String ERR_DEFINE_SCOPE_ON_METHOD = "define.scope.on.method";
    public static final String ERR_ESCAPING_NONANNOTATED_FIELD = "escaping.nonannotated.field";
    public static final String ERR_INTERFACE_ANNOTATION_MISMATCH = "interface.annotation.mismatch";
    public static final String ERR_NONEXISTENT_SCOPE = "nonexistent.scope";
    public static final String ERR_RUNNABLE_WITHOUT_RUNSIN = "runnable.without.runsin";
    public static final String ERR_SCOPE_CAST = "scope.cast";
    public static final String ERR_SCOPE_ON_METHOD_PRIMITIVE_RETURN = "scope.on.method.primitive.return";
    public static final String ERR_SCOPE_ON_VOID_METHOD = "scope.on.void.method";
    public static final String ERR_SCOPE_RUNS_IN_DISAGREEMENT = "scope.runs.in.disagreement";
    public static final String ERR_SCOPE_TREE_NOT_CONSISTENT = "scope.Tree.not.consistent";
    public static final String ERR_STATIC_NOT_IMMORTAL = "static.not.immortal";
    public static final String ERR_TYPE_CAST_BAD_ENTER_PARAMETER = "type.cast.bad.enter.parameter";
    public static final String ERR_UNANNOTATED_CLASS = "unannotated.class";
    public static final String ERR_UNRESOLVED_SCOPEDEF = "unresolvable";
    public static final String ERR_VAR_AND_TYPE_SCOPE_ANNOTATION_MISMATCH = "error.var.and.type.scope.annotation.mismatch";

    private ScopeCheckerContext ctx;

    public ScopeChecker(ScopeCheckerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new ScopeVisitor<Void>(this, root, ctx);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        // Please keep alphabetized
        p.put(ERR_BAD_ALLOCATION,
                "Object allocation in a context (%s) other than its designated scope (%s).");
        p.put(ERR_BAD_ANNOTATION,
                "@%s annotations must agree with their overridden annotations.");
        p.put(ERR_BAD_ASSIGNMENT_PRIVATE_MEM,
                "Cannot assign to a private memory with a different @DefineScope.");
        p.put(ERR_BAD_ASSIGNMENT_SCOPE,
                "Cannot assign expression in scope %s to variable in scope %s.");
        p.put(ERR_BAD_ENTER_TARGET, "enter() must target a child scope.");
        p.put(ERR_BAD_ENTER_PRIVATE_MEM_NO_RUNS_IN,
                "The Runnable passed into the enterPrivateMemory() call must have a run() method with a @RunsIn annotation.");
        p.put(ERR_BAD_ENTER_PRIVATE_MEM_NO_SCOPE_ON_RUNNABLE,
                "The Runnable class must have a matching @Scope annotation.");
        p.put(ERR_BAD_ENTER_PRIVATE_MEM_RUNS_IN_NO_MATCH,
                "The Runnable's @RunsIn must be a child scope of the CurrentScope\n\t @RunsIn: %s\n\t Current Scope: %s");
        p.put(ERR_BAD_EXECUTE_IN_AREA_OR_ENTER,
                "Runnable and PrivateMemory scopes disagree.");
        p.put(ERR_BAD_EXECUTE_IN_AREA_TARGET,
                "executeInArea() must target a parent scope.");
        p.put(ERR_BAD_FIELD_SCOPE,
                "Field must be in the same or parent scope as its owning type.");
        p.put(ERR_BAD_GUARD_ARGUMENT,
                "Only single variables can be passed as arguments into guards, no other expressions are allowed, eg. %s.");
        p.put(ERR_BAD_GUARD_NO_FINAL,
                "The variables passed into the GUARD statement must be final. The argument %s is not.");
        p.put(ERR_BAD_METHOD_INVOKE,
                "Illegal invocation of method of object in scope %s while in scope %s.");
        p.put(ERR_BAD_NEW_INSTANCE,
                "Cannot allocate objects of type %s inside scope %s.");
        p.put(ERR_BAD_RETURN_SCOPE,
                "Cannot return expression in scope %s in a method that has @Scope annotation: %s.");
        p.put(ERR_BAD_RETURN_TYPE,
                "Method return types must have a @Scope annotation.");
        p.put(ERR_BAD_RUNS_IN_METHOD,
                "Methods must run in the same scope or a child scope of their owning type.");
        p.put(ERR_BAD_RUNS_IN_ON_CTOR,
                "@RunsIn annotations not allowed on constructors.");
        p.put(ERR_BAD_RUNS_IN_OVERRIDE,
                "@RunsIn annotations must agree with their overridden annotations.");
        p.put(ERR_BAD_VARIABLE_SCOPE,
                "Variables of type %s are not allowed in this allocation context (%s).");
        p.put(ERR_CHECKER_BUG,
                "Error of the checker, please, report this to the authors of the checker: %s");
        p.put(ERR_DEFINE_SCOPE_ON_METHOD,
                "@DefineScope annotations not allowed on methods.");
        p.put(ERR_ESCAPING_NONANNOTATED_FIELD, ERR_ESCAPING_NONANNOTATED_FIELD);
        p.put(ERR_INTERFACE_ANNOTATION_MISMATCH,
                "One or more interfaces has a mismatching @%s annotation.");
        p.put(ERR_NONEXISTENT_SCOPE, "Scope %s does not exist.");
        p.put(ERR_RUNNABLE_WITHOUT_RUNSIN,
                "Runnable used with executeInArea() without @RunsIn.");
        p.put(ERR_SCOPE_CAST,
                "Class Cast Error: The class being casted must have a scope (@Scope=%s) that is the same as the scope of the target class (@Scope=%s).");
        p.put(ERR_SCOPE_ON_METHOD_PRIMITIVE_RETURN,
                "A method that returns a primitive type cannot have @Scope annotation.");
        p.put(ERR_SCOPE_ON_VOID_METHOD,
                "A method that returns void cannot have @Scope annotation.");
        p.put(ERR_SCOPE_RUNS_IN_DISAGREEMENT,
                "@RunsIn annotations must be a sub-scope of @Scope annotations.");
        p.put(ERR_SCOPE_TREE_NOT_CONSISTENT,
                "Scope Definitions are not consistent: \n\t%s");
        p.put(ERR_STATIC_NOT_IMMORTAL,
                "Static fields types must be @Scope(IMMORTAL) or nothing at all.");
        p.put(ERR_UNANNOTATED_CLASS, "Class %s has no @Scope annotation.");
        p.put(ERR_UNRESOLVED_SCOPEDEF,
                "Unable to resolve declared scope for expression.");
        p.put(ERR_VAR_AND_TYPE_SCOPE_ANNOTATION_MISMATCH,
                "Variable is annotated with @Scope annotation that does not correspond to @Scope annotation of its type.");

        return p;
    }
}
