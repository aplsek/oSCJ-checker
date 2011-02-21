package checkers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import checkers.source.SourceChecker;
import checkers.source.SourceVisitor;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;

// MultiPassChecker differs from AggregateChecker in that it runs each checker separately, rather than
// running each checker on one compilation unit at a time. It accomplishes this by overriding
// typeProcessingOver to run all but the first checker, while typeProcess runs the first checker alone.
public class MultiPassChecker extends SourceChecker {
    private ArrayList<SinglePassChecker>         checkers = new ArrayList<SinglePassChecker>();
    private LinkedHashMap<TypeElement, TreePath> types    = new LinkedHashMap<TypeElement, TreePath>();

    protected void addPass(SinglePassChecker checker) {
        checkers.add(checker);
    }

    @Override
    public void typeProcess(TypeElement element, TreePath tree) {
        if (checkers.size() > 0) {
            types.put(element, tree);
        }
    }

    @Override
    public void typeProcessingOver() {
        for (SinglePassChecker c : checkers) {
            for (Map.Entry<TypeElement, TreePath> e : types.entrySet()) {
                c.typeProcess(e.getKey(), e.getValue());
            }
            c.typeProcessingOver();
            if (c.hasErrors()) {
                break;
            }
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
