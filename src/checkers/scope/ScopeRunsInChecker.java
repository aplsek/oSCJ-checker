package checkers.scope;


import static checkers.scope.ScopeRunsInChecker.ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE;

import java.util.Properties;

import checkers.SinglePassChecker;
import checkers.source.SourceVisitor;

import com.sun.source.tree.CompilationUnitTree;

public class ScopeRunsInChecker extends SinglePassChecker {
    public static final String ERR_BAD_LIBRARY_ANNOTATION = "bad.library.annotation";
    public static final String ERR_BAD_SCOPE_NAME = "bad.scope.name";
    public static final String ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE = "illegal.class.scope.override";
    public static final String ERR_ILLEGAL_FIELD_SCOPE = "illegal.field.scope";
    public static final String ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE = "illegal.method.runs.in.override";
    public static final String ERR_ILLEGAL_METHOD_NAMED_RUNS_IN_OVERRIDE = "illegal.method.named.runs.in.override";
    public static final String ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE_RESTATE = "illegal.method.runs.in.override.restate";
    public static final String ERR_ILLEGAL_METHOD_RESERVED_RUNS_IN_OVERRIDE = "illegal.method.reserved.runs.in.override";

    public static final String ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE = "illegal.method.scope.override";
    public static final String ERR_ILLEGAL_STATIC_FIELD_SCOPE = "illegal.static.field.scope";
    public static final String ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE = "illegal.variable.scope.override";
    public static final String ERR_MEMORY_AREA_NO_DEFINE_SCOPE = "err.mem.no.def.scope";
    public static final String ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT = "err.mem.type.def.scope.not.consistent";
    public static final String ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE = "err.mem.type.def.scope.not.consistent.with.scope";
    public static final String ERR_RUNS_IN_ON_CLASS = "err.runs.in.on.class";
    public static final String ERR_RUNS_IN_ON_CONSTRUCTOR = "err.runs.in.on.constructor";
    public static final String ERR_SCOPE_ON_VOID_OR_PRIMITIVE_RETURN = "scope.on.void.or.primitive.return";

    private ScopeCheckerContext ctx;

    public ScopeRunsInChecker(ScopeCheckerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new ScopeRunsInVisitor(this, root, ctx);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put(ERR_BAD_LIBRARY_ANNOTATION, "Library superclass has malformed annotations.");
        p.put(ERR_BAD_SCOPE_NAME, "Scope %s does not exist.");
        p.put(ERR_ILLEGAL_CLASS_SCOPE_OVERRIDE, "Class %s illegal overrides Scope annotation on superclass or interface %s.");
        p.put(ERR_ILLEGAL_FIELD_SCOPE, "Field scope %s must be the same or an ancestor to class's scope %s.");
        p.put(ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE, "Non-SUPPORT level methods may not override RunsIn annotations.");
        p.put(ERR_ILLEGAL_METHOD_RUNS_IN_OVERRIDE_RESTATE, "SUPPORT method may overrived the @RunsIn annotation but must explicitly use the @RunsIn annotation.");
        p.put(ERR_ILLEGAL_METHOD_NAMED_RUNS_IN_OVERRIDE, "Illegal @RunsIn override: Overriden @RunsIn(named-scope) annotation is not allowed to be re-overriden again by a child class.");
        p.put(ERR_ILLEGAL_METHOD_RESERVED_RUNS_IN_OVERRIDE, "Illegal @RunsIn override: Runnable run() method's @RunsIn may be overriden only by a named scope.");
        p.put(ERR_ILLEGAL_METHOD_SCOPE_OVERRIDE, "Illegal @RunsIn override: Non-SUPPORT level methods may not override Scope annotations, nor the run() methods that do not direclty override Runnable.run().");
        p.put(ERR_ILLEGAL_STATIC_FIELD_SCOPE, "Static fields must be in the immortal scope, not %s.");
        p.put(ERR_ILLEGAL_VARIABLE_SCOPE_OVERRIDE, "Variable scope %s may not override its type's scope %s.");
        p.put(ERR_MEMORY_AREA_NO_DEFINE_SCOPE, "MemoryArea type does not have a @DefineScope annotation.");
        p.put(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT, "MemoryArea field @DefineScope annotation is not consistent with the @DefineScope annotations on classes.");
        p.put(ERR_MEMORY_AREA_DEFINE_SCOPE_NOT_CONSISTENT_WITH_SCOPE, "MemoryArea field @DefineScope annotation is not consistent with the @Scope annotations of the field. (Field's scope is %s, @DefineScope requires %s) ");
        p.put(ERR_RUNS_IN_ON_CLASS, "RunsIn annotations are ignored on classes.");
        p.put(ERR_RUNS_IN_ON_CONSTRUCTOR, "RunsIn annotations are not allowed on constructors. %s");
        p.put(ERR_SCOPE_ON_VOID_OR_PRIMITIVE_RETURN, "@Scope annotation ignored on methods that return void or primitives.");

        return p;
    }
}
