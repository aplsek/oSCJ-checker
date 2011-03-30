package checkers.scope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class VariableScopeTable {
    private LinkedList<LexicalBlock> blocks = new LinkedList<LexicalBlock>();

    public void pushBlock() {
        blocks.addLast(new LexicalBlock());
    }

    public void popBlock() {
        blocks.removeLast();
    }

    public void addVariableScope(String var, ScopeInfo scope) {
        if (scope == null)
            throw new RuntimeException("Cannot add null scoped variable");
        LexicalBlock last = blocks.getLast();
        if (last.contains(var))
            // If the block has already defined this variable, this table is
            // somehow being used incorrectly.
            throw new RuntimeException("Variable already defined in block");
        last.put(var, scope);
    }

    public ScopeInfo getVariableScope(String var) {
        Iterator<LexicalBlock> iter = blocks.descendingIterator();
        while (iter.hasNext()) {
            LexicalBlock b = iter.next();
            ScopeInfo scope = b.get(var);
            if (scope != null)
                return scope;
        }
        throw new RuntimeException("Variable not defined in scope table: "
                + var);
    }

    public void addParentRelation(String childVar, String parentVar) {
        LexicalBlock last = blocks.getLast();
        last.setRelation(new Relation(childVar, parentVar, RelationKind.PARENT));
    }

    public void addSameRelation(String var1, String var2) {
        LexicalBlock last = blocks.getLast();
        last.setRelation(new Relation(var1, var2, RelationKind.SAME));
    }

    public boolean hasParentRelation(String childVar, String parentVar) {
        Iterator<LexicalBlock> iter = blocks.descendingIterator();
        while (iter.hasNext()) {
            LexicalBlock b = iter.next();
            Relation r = b.relation;
            if (r != null && r.isParentRelation(childVar, parentVar))
                return true;
        }
        return false;
    }

    public boolean hasSameRelation(String var1, String var2) {
        Iterator<LexicalBlock> iter = blocks.descendingIterator();
        while (iter.hasNext()) {
            LexicalBlock b = iter.next();
            Relation r = b.relation;
            if (r != null && r.isSameRelation(var1, var2))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String ret = "";
        Iterator<LexicalBlock> iter = blocks.descendingIterator();
        while (iter.hasNext()) {
            ret += "Block:\n" + iter.next() + "\n";
        }
        return ret;
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
            if (relation != null)
                throw new RuntimeException("Relation already set for block");
            relation = r;
        }

        @Override
        public String toString() {
            String ret = "";
            for (Map.Entry<String, ScopeInfo> e : scopes.entrySet())
                ret += e.getKey() + " = @Scope(" + e.getValue() + ")\n";
            ret += "Relation: " + relation + "\n";
            return ret;
        }
    }

    private static enum RelationKind {
        SAME, PARENT;
    }

    private static class Relation {
        private String var1;
        private String var2;
        private RelationKind kind;

        Relation(String var1, String var2, RelationKind kind) {
            this.var1 = var1;
            this.var2 = var2;
            this.kind = kind;
        }

        boolean isParentRelation(String child, String parent) {
            return (this.kind == RelationKind.PARENT && this.var1.equals(child) && this.var2
                    .equals(parent)) || isSameRelation(child, parent);
        }

        boolean isSameRelation(String var1, String var2) {
            return this.kind == RelationKind.SAME
                    && ((this.var1.equals(var1) && this.var2.equals(var2) || this.var1
                            .equals(var2) && this.var2.equals(var1)));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof Relation) {
                Relation r = (Relation) obj;
                return var1.equals(var1) && var2.equals(r.var2)
                        && kind == r.kind;
            }
            return false;
        }

        @Override
        public String toString() {
            if (kind == RelationKind.SAME)
                return var1 + " is in the same scope as " + var2;
            else
                return var1 + " is in a child scope of " + var2;
        }
    }
}
