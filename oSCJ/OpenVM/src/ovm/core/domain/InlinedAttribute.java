package ovm.core.domain;

import ovm.core.repository.Attribute;
import ovm.core.repository.RepositoryProcessor;
import ovm.core.repository.RepositoryUtils;

public class InlinedAttribute extends Attribute {
    public void accept(RepositoryProcessor _) {
	// nothing to do here, really
    }

    private int[] startPC;
    private int[] length;
    private Method[] method;

    /**
     * The inlined method table should be sorted in ascending order of
     * startPC, and for methods with the same startPC, in descending
     * order of length.  This order ensures that if m1 is inlined
     * within m2, which itself is inlined within m3, m1 will appear
     * after m2 in m3's table.
     */
    public InlinedAttribute(int[] startPC,
			    int[] length,
			    Method[] method) {
	this.startPC = startPC;
	this.length = length;
	this.method = method;
    }

    public int getNestedMethodIndex(int pc, int depth) {
	for (int i = startPC.length - 1; i --> 0; ) {
	    if (startPC[i] <= pc && startPC[i] + length[i] > pc)
		if (depth-- == 0)
		    return i;
	}
	return -1;
    }
    /**
     * For a given program point, return the inlined method depth-many
     * steps up the logical hierarchy from the innermost method.
     * Returns null if <code>pc</code> does not contain inlined
     * methods at the desired depth.
     * <p>
     * For example, if bytes 10-30 of method <code>m1</code> are an
     * inlined defintion of method <code>m2</code>, and bytes 15-25
     * are a inlined definition of <code>m3</code> (which is called
     * from m2):
     * <ul>
     *    <li> <code>getNestedMethod(20, 0)</code> => <code>m3</code>.
     *    <li> <code>getNestedMethod(20, 1)</code> => <code>m2</code>.
     *    <li> <code>getNestedMethod(20, 2)</code> => <code>null</code>.
     * </ul>
     */
    public Method getNestedMethod(int pc, int depth) {
	int i = getNestedMethodIndex(pc, depth);
	return i == -1 ? null : method[i];
    }

    public int getInnerMostMethodIndex(int pc) {
	return getNestedMethodIndex(pc, 0);
    }

    /**
     * Return the most nested method inlined method at a given pc, or
     * null.  This method is equivalent to
     * {@link #getNestedMethod}(pc, 0).
     */
    public Method getInnerMostMethod(int pc) {
	return getNestedMethod(pc, 0);
    }

    public int size() { return startPC.length; }
    public int getStartPC(int i) { return startPC[i]; }
    public int getLength(int i) { return length[i]; }
    public Method getMethod(int i) { return method[i]; }
    
    public static final int nameIndex =
	RepositoryUtils.asUTF("org.ovmj.Inlined");
    public int getNameIndex() { return nameIndex; }

    public String toString() {
	StringBuffer ret = new StringBuffer("Inlined Methods {");
	for (int i = 0; i < length.length; i++) {
	    ret.append("\n  [");
	    ret.append(startPC[i]);
	    ret.append(",");
	    ret.append(startPC[i] + length[i]);
	    ret.append(") = ");
	    ret.append(method[i].getSelector());
	}
	ret.append("\n}\n");
	return ret.toString();
    }
}
