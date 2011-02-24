package checkers.scope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class VariableScopeTable {
    private LinkedList<Map<String, ScopeInfo>> blocks =
        new LinkedList<Map<String, ScopeInfo>>();

    public void pushBlock() {
        blocks.addLast(new HashMap<String, ScopeInfo>());
    }

    public void popBlock() {
        blocks.removeLast();
    }

    public void addVariableScope(String var, ScopeInfo scope) {
        if (scope == null) {
            throw new RuntimeException("Cannot add null scoped variable");
        }
        Map<String, ScopeInfo> last = blocks.getLast();
        if (last.containsKey(var)) {
            // If the block has already defined this variable, this table is
            // somehow being used incorrectly.
            throw new RuntimeException("Variable already defined in block");
        }
        last.put(var, scope);
    }

    public ScopeInfo getVariableScope(String var) {
        Iterator<Map<String, ScopeInfo>> iter = blocks.descendingIterator();
        while (iter.hasNext()) {
            Map<String, ScopeInfo> b = iter.next();
            ScopeInfo scope = b.get(var);
            if (scope != null) {
                return scope;
            }
        }
        throw new RuntimeException("Variable not defined in scope table");
    }
}
