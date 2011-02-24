package checkers;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;

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

    protected void enableDebug() {     
        Utils.DEBUG_OLD = Utils.DEBUG;
        Utils.DEBUG = true;     
    } 
    
    protected void disableDebug() {     
        Utils.DEBUG = Utils.DEBUG_OLD;        
    }
    
    protected void fail(String msg, Tree src, Object... msgParams) {
        checker.report(Result.failure(msg, msgParams), src);
    }

    protected void warn(String msg, Tree src, Object... msgParams) {
        checker.report(Result.warning(msg, msgParams), src);
    }
}
