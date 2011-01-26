package checkers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;

// MultiPassChecker differs from AggregateChecker in that it runs each checker separately, rather than
// running each checker on one compilation unit at a time. It accomplishes this by overriding
// typeProcessingOver to run all but the first checker, while typeProcess runs the first checker alone.
public class MultiPassChecker extends SourceChecker {
    private ArrayList<SourceChecker>             checkers = new ArrayList<SourceChecker>();
    private LinkedHashMap<TypeElement, TreePath> types    = new LinkedHashMap<TypeElement, TreePath>();

    protected void addPass(SourceChecker checker) {
        checkers.add(checker);
    }

    @Override
    public void typeProcess(TypeElement element, TreePath tree) {
        if (checkers.size() > 0) {
            checkers.get(0).typeProcess(element, tree);
            types.put(element, tree);
        }
    }

    @Override
    public void typeProcessingOver() {
        for (int i = 1; i < checkers.size(); i++) {
            for (Map.Entry<TypeElement, TreePath> e : types.entrySet()) {
                checkers.get(i).typeProcess(e.getKey(), e.getValue());
            }
        }
        for (SourceChecker c : checkers) {
            c.typeProcessingOver();
        }
    }

    @Override
    public final void init(ProcessingEnvironment env) {
        super.init(env);
        for (SourceChecker checker : checkers) {
            checker.init(env);
        }
    }

    @Override
    public final Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>();
        for (SourceChecker checker : checkers) {
            options.addAll(checker.getSupportedOptions());
        }
        return options;
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root) {
        return null;
    }
}
