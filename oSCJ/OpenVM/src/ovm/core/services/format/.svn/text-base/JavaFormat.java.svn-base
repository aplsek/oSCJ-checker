package ovm.core.services.format;
import java.io.UnsupportedEncodingException;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.repository.UnboundSelector;
import ovm.util.UnicodeBuffer;
import ovm.util.OVMError.Unimplemented;

/**
 * Hereby "Java Format" for type names is defined as whatever
 * Class.getName() returns as determined empirically in  JDK 1.4.1
 * examples: "int", "Foo", "Foo$Bar", "[I", "[Ljava.lang.Object;", 
 * "[[LFoo$Bar;"
 * @author Krzysztof Palacz
 **/

public class JavaFormat extends Format {

    private char getPrimitiveTag(String cname) {
	if (cname.equals("int"))
	    return TypeCodes.INT;
	else if (cname.equals("char"))
	    return TypeCodes.CHAR;
	else if (cname.equals("float"))
	    return TypeCodes.FLOAT;
	else if (cname.equals("double"))
	    return TypeCodes.DOUBLE;
	else if (cname.equals("long"))
	    return TypeCodes.LONG;
	else if (cname.equals("byte"))
	    return TypeCodes.BYTE;
	else if (cname.equals("boolean"))
	    return TypeCodes.BOOLEAN;
	else if (cname.equals("short"))
	    return TypeCodes.SHORT;
	else if (cname.equals("void"))
	    return TypeCodes.VOID;
	else 
	    return TypeCodes.NONE;
    }

    private UnicodeBuffer wrap(String name) {
	try {
	    return UnicodeBuffer.factory().wrap(name.getBytes("UTF-8"));
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException(e);
	}
    }

    final UnicodeBuffer INT = wrap("int");
    final UnicodeBuffer CHAR = wrap("char");
    final UnicodeBuffer FLOAT = wrap("float");
    final UnicodeBuffer DOUBLE = wrap("double");
    final UnicodeBuffer LONG = wrap("long");
    final UnicodeBuffer BYTE = wrap("byte");
    final UnicodeBuffer BOOLEAN = wrap("boolean");
    final UnicodeBuffer SHORT = wrap("short");
    final UnicodeBuffer VOID = wrap("void");
    
    private char getPrimitiveTag(UnicodeBuffer cname) {
	if (cname.equals(INT))
	    return TypeCodes.INT;
	else if (cname.equals(CHAR))
	    return TypeCodes.CHAR;
	else if (cname.equals(FLOAT))
	    return TypeCodes.FLOAT;
	else if (cname.equals(DOUBLE))
	    return TypeCodes.DOUBLE;
	else if (cname.equals(LONG))
	    return TypeCodes.LONG;
	else if (cname.equals(BYTE))
	    return TypeCodes.BYTE;
	else if (cname.equals(BOOLEAN))
	    return TypeCodes.BOOLEAN;
	else if (cname.equals(SHORT))
	    return TypeCodes.SHORT;
	else if (cname.equals(VOID))
	    return TypeCodes.VOID;
	else 
	    return TypeCodes.NONE;
    }

    /** A Java-format name doesn't include enough information to determine whether
     *  to build an L- or G-typename.  This method will always return an L-name.
     **/
    public TypeName parseTypeName(String string) {
	char tag = getPrimitiveTag(string);
	if (tag != TypeCodes.NONE) {
	    return OvmFormat._.parseTypeName("" + tag);
        } else {
            if (string.charAt(0) != TypeCodes.ARRAY) {
                string = new StringBuffer( 2 + string.length())
                        .append( TypeCodes.OBJECT)
                        .append( string.replace( '.', '/'))
                        .append( ';')
                        .toString();
	    } else {
                string = string.replace('.', '/');
	    }
	    return OvmFormat._.parseTypeName(string);
        }
    }


    public TypeName parseTypeName(UnicodeBuffer dotted,
				  boolean parseJavaPrimitives) {
	if (parseJavaPrimitives) {
	    char tag = getPrimitiveTag(dotted);
	    if (tag != TypeCodes.NONE)
		return TypeName.Primitive.make(tag);
	}
	byte[] slashedArr = new byte[dotted.byteCount()];
	int i = 0;
	dotted.rewind();
	while (dotted.hasMore()) {
	    byte b = (byte) dotted.getByte();
	    slashedArr[i++] = (byte) (b == '.' ? '/' : b);
	}
	UnicodeBuffer slashed = UnicodeBuffer.factory().wrap(slashedArr, 0, i);
	return TypeName.Compound.parseClassInfo(slashed);
    }

    private String fmt(TypeName.Primitive tn) {
	switch (tn.getTypeTag()) {
	case TypeCodes.BOOLEAN:
	    return "boolean";
	case TypeCodes.INT:
	    return "int";
	case TypeCodes.SHORT:
	    return "short";
	case TypeCodes.BYTE:
	    return "byte";
	case TypeCodes.CHAR:
	    return "char";
	case TypeCodes.LONG:
	    return "long";
	case TypeCodes.FLOAT:
	    return "float";
	case TypeCodes.DOUBLE: 
	    return "double";
	case TypeCodes.VOID:
	    return "void";
	case TypeCodes.OBJECT:
        case TypeCodes.GEMEINSAM:
	case TypeCodes.ARRAY: 
	default:
	    throw failure("should not happen");
	}
    }
    
    /**
     * <strong>Warning:</strong> This method will produce the same output for a
     * Gemeinsam typename as for the corresponding instance typename.
     **/
    public StringBuffer format(TypeName tn, StringBuffer buf) { 
	if (tn.isCompound()) {
	    return 
		buf.append(tn.asCompound().toClassInfoString().replace('/', '.'));
	} else {
	    return buf.append(fmt(tn.asPrimitive()));
	}
    }

    public UnicodeBuffer formatUnicode(TypeName tn) {
	if (tn.isScalar()) {
	    UnicodeBuffer n = UTF8Store._.getUtf8(tn.getShortNameIndex());
	    byte[] bytes;
	    int i = 0;

	    if (tn.getPackageNameIndex() != 0) {
		UnicodeBuffer p = UTF8Store._.getUtf8(tn.getPackageNameIndex());
		bytes = new byte[p.byteCount() + 1 + n.byteCount()];

		p.rewind();
		while (p.hasMore()) {
		    byte b = (byte) p.getByte();
		    bytes[i++] = (byte) (b == '/' ? '.' : b);
		}
		bytes[i++] = '.';
	    } else {
		bytes = new byte[n.byteCount()];
	    }

	    n.rewind();
	    while (n.hasMore())
		bytes[i++] = (byte) n.getByte();

	    return UnicodeBuffer.factory().wrap(bytes);
	} else if (tn.isArray()) {
	    String ci = tn.asCompound().toClassInfoString();
	    return UnicodeBuffer.factory().wrap(ci.replace('/', '.'));
	} else {
	    switch (tn.getTypeTag()) {
	    case TypeCodes.BOOLEAN:
		return BOOLEAN;
	    case TypeCodes.INT:
		return INT;
	    case TypeCodes.SHORT:
		return SHORT;
	    case TypeCodes.BYTE:
		return BYTE;
	    case TypeCodes.CHAR:
		return CHAR;
	    case TypeCodes.LONG:
		return LONG;
	    case TypeCodes.FLOAT:
		return FLOAT;
	    case TypeCodes.DOUBLE: 
		return DOUBLE;
	    case TypeCodes.VOID:
		return VOID;
	    case TypeCodes.OBJECT:
	    case TypeCodes.GEMEINSAM:
	    case TypeCodes.ARRAY: 
	    default:
		throw failure("should not happen");
	    }
	}
    }
	
	    
    public StringBuffer format(Selector selector, StringBuffer buf) {
	throw new Unimplemented();
    }

    public StringBuffer format(UnboundSelector selector, 
			       StringBuffer buf) {
	throw new Unimplemented();
    }

    /**
     * Oh, this is sooo cute.
     **/
    public static final JavaFormat _ = new JavaFormat();

}
