package checkers.scope;

import java.util.Properties;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import checkers.Utils;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import com.sun.source.tree.CompilationUnitTree;

@SupportedOptions("debug")
public class ScopeChecker extends SourceChecker {
    private ScopeCheckerContext context;

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return new ScopeVisitor<Void, Void>(this, root, context);
    }

    @Override
    public Properties getMessages() {
        Properties p = new Properties();
        p.put("bad.assignment.private.mem", "Cannot assign to a private memory with a different @DefineScope.");
        p.put("bad.assignment.scope", "Cannot assign expression in scope %s to variable in scope %s.");
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
        p.put("bad.newInstance", "Cannot allocate objects of type %s inside scope %s."); // TODO: merge
        p.put("unresolvable.scopedef", "Unable to resolve declared scope for expression.");
        p.put("unannotated.class", "Class %s has no @Scope annotation.");
        
        p.put("bad.enterPrivateMem.defineScope", "The variable referencing getCurrentManagedMemory must have" +
        		" a @DefineScope with name that equals to the current allocation context." +
        		"\n\t Allocation-Context: %s " +
        		"\n\t DefineScope name: %s");
        p.put("bad.enterPrivateMem.defineScope.parent", "The @DefineScope variable is not consistent with its scope definition, check the parent of the scope!" +
                "\n\t Allocation-Context: %s " +
                "\n\t DefineScope name: %s");
        
        p.put("bad.enterPrivateMem.no.defineScope", "Reference obtained from getCurrentManagedMemory must have a @DefineScope annotation." +
                "\n\t Expected Define-Scope name =  %s " );  // this should be detected by the DefineScopeChecker
        
        p.put("scope.Tree.not.consistent", "Scope Definitions are not consistent: \n\t%s");
        
        p.put("scope.cast", "Class Cast Error : The class being casted must have a scope (@Scope=%s) that is the same as the scope of the target class (@Scope=%s).");
        
        
        
        p.put("checker.bug", "Error of the checker, please, report this to the authors of the checker: %s");
        
        
        return p;
    }
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Utils.DEBUG = processingEnv.getOptions().containsKey("debug");
        //Utils.DEBUG = true; // TODO: how to set up that debug parameter??!!
        context = new ScopeCheckerContext(this);
    }
}
