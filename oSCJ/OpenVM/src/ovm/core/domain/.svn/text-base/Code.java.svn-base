package ovm.core.domain;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.repository.Descriptor;
import ovm.util.HashMap;
import ovm.core.repository.Selector;

/**
 * This class is part of a refactoring of the ByteCodeFragment
 * class proposed by Christian.
 **/
public abstract class Code {
    public static final Code[] EMPTY_ARRAY = new Code[0];
    
    public interface Kind {
    }

    protected final Method itsMethod;
    /**
     * Private to package s3.core.domain.  Visibility to be corrected
     * later.
     **/
    public Code next;

    /**
     * Return {@link ovm.core.repository.TypeCodes TypeCode} of each
     * parameter (including this) followed by return TypeCode, and a
     * trailing ascii NUL.  '[' and 'G' are canonicalized to 'L' in
     * this list, and 'Z', 'B', 'C', and 'S' are canonicalized to 'I'.
     * <p>
     * Because every method in ovm takes a this pointer, the first
     * byte of this string is always 'L'.  Thus, a nullary void method
     * would have the signature code <code>"LV"</code> when viewed
     * from C.
     **/
    protected final byte[] signatureCode;

    /**
     * A function defined according to the C ABI that takes this Code
     * object followed by the method arguments, and evaluates the
     * method call.
     **/
    protected VM_Address foreignEntry;

    static private final byte[] signatureMap = new byte[128];
    static private final HashMap signatureIntern = new HashMap();
    static {
	signatureMap['V'] = 'V';
	signatureMap['Z'] = 'I';
	signatureMap['B'] = 'I';
	signatureMap['C'] = 'I';
	signatureMap['S'] = 'I';
	signatureMap['J'] = 'J';
	signatureMap['F'] = 'D';
	signatureMap['D'] = 'D';
	signatureMap['L'] = 'L';
	signatureMap['G'] = 'L';
	signatureMap['['] = 'L';
    }

    public Code(Code original) {
	signatureCode = original.signatureCode;
	itsMethod = original.itsMethod;
    }

    public Code(Method m) {
	itsMethod = m;
	Descriptor.Method desc = getSelector().getDescriptor();
	StringBuffer sb = new StringBuffer(desc.getArgumentCount() + 2);
	sb.append('L');
	for (int i = 0; i < desc.getArgumentCount(); i++) {
	    char c = desc.getArgumentType(i).getTypeTag();
	    sb.append(signatureMap[c]);
	}
	char c = desc.getType().getTypeTag();
	sb.append(signatureMap[c]);
	String s = sb.toString();
	synchronized (signatureIntern) {
	    byte[] b = (byte[]) signatureIntern.get(s);
	    if (b == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try {
		    b = s.getBytes();
		} finally {
		    MemoryPolicy.the().leave(r);
		}
		signatureIntern.put(s, b);
	    }
	    signatureCode = b;
	}
    }

    public final Method getMethod() {
	return itsMethod;
    }
    public final Selector.Method getSelector() {
	return itsMethod.getSelector();
    }

    /**
     * Return this code as a C function pointer.
     **/
    public final VM_Address getForeignEntry() {
	return foreignEntry;
    }

    public abstract Kind getKind();

    public abstract int getLineNumber(int vpc);

    /**
     * Substitute this piece of code with the equivalent
     * piece of code specified.
     *
     * @param c c.getKind() == getKind() and c.getMethod() == getMethod() are guaranteed
     */
    public abstract void bang(Code c);

}
