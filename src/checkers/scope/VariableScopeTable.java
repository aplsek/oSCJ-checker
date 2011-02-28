package checkers.scope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;


/**
 *
 *
 * NOTE: this variable TO scopeInfo map does not contain primitive variables
 *       - it also does not contain any entries that has "null" values of ScopeInfo
 *
 */
public class VariableScopeTable {
    private LinkedList<LexicalBlock> blocks =
        new LinkedList<LexicalBlock>();

    public void pushBlock() {
        blocks.addLast(new LexicalBlock());
    }

    public void popBlock() {
        blocks.removeLast();
    }

    public void addVariableScope(String var, ScopeInfo scope) {
        if (scope == null) {
            throw new RuntimeException("Cannot add null scoped variable");
        }
        LexicalBlock last = blocks.getLast();
        if (last.contains(var)) {
            // If the block has already defined this variable, this table is
            // somehow being used incorrectly.
            throw new RuntimeException("Variable already defined in block");
        }
        last.put(var, scope);
    }

    public ScopeInfo getVariableScope(String var) {
        Iterator<LexicalBlock> iter = blocks.descendingIterator();
        while (iter.hasNext()) {
            LexicalBlock b = iter.next();
            ScopeInfo scope = b.get(var);
            if (scope != null) {
                return scope;
            }
        }
        throw new RuntimeException("Variable not defined in scope table");
    }

    public void addParentRelation(String childVar, String parentVar) {
        LexicalBlock last = blocks.getLast();
        last.setRelation(new ParentRelation(childVar, parentVar));
    }

    public void addSameRelation(String var1, String var2) {
        LexicalBlock last = blocks.getLast();
        last.setRelation(new SameRelation(var1, var2));
    }

    public boolean hasParentRelation(String childVar, String parentVar) {
        return false;
    }

    public boolean hasSameRelation(String var1, String var2) {
        return false;
    }

    private static class LexicalBlock {
        Map<String, ScopeInfo> scopes = new HashMap<String, ScopeInfo>();
        Relation relation = null;

        public boolean contains(String var) {
            return scopes.containsKey(var);
        }

        public ScopeInfo get(String var) {
            return scopes.get(var);
        }

        public void put(String var, ScopeInfo scope) {
            scopes.put(var, scope);
        }

        public void setRelation(Relation r) {
            if (relation != null) {
                throw new RuntimeException("Relation already set for block");
            }
            relation = r;;
        }
    }

    private static abstract class Relation {
        protected String var1;
        protected String var2;

        protected Relation(String var1, String var2) {
            this.var1 = var1;
            this.var2 = var2;
        }
    }

    private static class SameRelation extends Relation {
        SameRelation(String var1, String var2) {
            super(var1, var2);
        }
    }

    private static class ParentRelation extends Relation {
        ParentRelation(String childVar, String parentVar) {
            super(childVar, parentVar);
        }
    }
}
