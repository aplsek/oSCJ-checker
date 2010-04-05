package checkers.scope;

import java.util.Properties;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import checkers.Utils;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import com.sun.source.tree.CompilationUnitTree;

@SupportedOptions("debug")
public class ScopeChecker extends SourceChecker {
    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new ScopeVisitor<Void, Void>(this, root);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put("bad.assignment.scope", "Cannot assign to a field in a different scope.");
        p.put("scope.runs.in.disagreement", "@RunsIn annotations must be a sub-scope of @Scope annotations.");
        p.put("escaping.nonannotated.field", "escaping.nonannotated.field");
        p.put("scope.on.method", "@Scope annotations not allowed on methods.");
        p.put("bad.return.type", "Method return types must have a @Scope annotation.");
        p.put("runnable.without.runsin", "Runnable used with executeInArea() without @RunsIn.");
        p.put("bad.executeInArea.or.enter", "Runnable and PrivateMemory scopes disagree.");
        p.put("bad.executeInArea.target", "executeInArea() must target a parent scope.");
        p.put("bad.enter.target", "enter() must target a child scope.");
        //p.put("bad.method.invoke", "Methods called must be in the same scope or in a parent scope and be @AllocFree.");
        p.put("bad.method.invoke", "Illegal invocation of method of object in scope %s while in scope %s.");
        p.put("bad.allocation", "Object allocation in a context (%s) other than its designated scope (%s).");
        p.put("static.not.immortal", "Static fields types must be @Scope(\"immortal\").");
        p.put("bad.field.scope", "Field must be in the same or parent scope as its owning type.");
        p.put("bad.variable.scope", "Variables of type %s are not allowed in this allocation context (%s).");
        p.put("runs.in.on.ctor", "@RunsIn annotations not allowed on constructors.");
        p.put("bad.runs.in.method", "Methods must run in the same scope or a child scope of their owning type.");
        p.put("bad.runs.in.override", "@RunsIn annotations must agree with their overridden annotations.");
        p.put("bad.annotation", "@%s annotations must agree with their overridden annotations.");
        p.put("interface.annotation.mismatch", "One or more interfaces has a mismatching @%s annotation.");
        p.put("nonexistent.scope", "Scope %s does not exist.");
        p.put("bad.newInstance", "Cannot allocate objects of type %s inside scope %s.");
        p.put("unresolvable.scopedef", "Unable to resolve declared scope for expression.");
        return p;
    }
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        Utils.DEBUG = processingEnv.getOptions().containsKey("debug");
        super.init(processingEnv);
    }
}
