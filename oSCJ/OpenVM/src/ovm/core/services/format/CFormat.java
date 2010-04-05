package ovm.core.services.format;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.TypeCodes;
import ovm.util.OVMError.Unimplemented;

/**
 * The format used by the struct generator.
 * This format renders Gemeinsam typenames differently from their instance
 * counterparts (so the struct generator can generate both).
 * @author Krzysztof Palacz
 **/

public class CFormat extends Format {

    public StringBuffer format(TypeName tn, StringBuffer buf) {
        return format(tn, buf, true);
    }

    public String format(TypeName tn, boolean needsStruct) {
        return format(tn, new StringBuffer(), needsStruct).toString();
    }

    private StringBuffer format(
        TypeName tn,
        StringBuffer buf,
        boolean needsStruct) {
        if (tn.isPrimitive()) {
            return buf.append(formatVariable(tn.asPrimitive()));
        }
        if (needsStruct) {
            buf.append("struct ");
        }
        if (tn.isArray()) {
            TypeName.Array atn = tn.asArray();
            for (int i = atn.getDepth(); i > 0; i--) {
                buf.append("arr_");
            }
            format(atn.getInnermostComponentTypeName(), buf, false);
        } else {
            if (tn instanceof TypeName.Gemeinsam)
                buf.append("gmn_");
	    // Yes, it does make sense to treat unicode string
	    // as a C identifier.  No charset problems here.
            buf.append(
                tn.asCompound().toClassInfoString().replace('/', '_').replace(
                    '$',
                    '_'));
        }
        return buf;
    }

    public StringBuffer format(
        UnboundSelector sel,
        StringBuffer buf) {
        if (sel instanceof UnboundSelector.Field) {
            UnboundSelector.Field fsel =
                (UnboundSelector.Field) sel;
            if (fsel.getDescriptor().isPrimitive()) {
                buf.append(
                    formatField(fsel.getDescriptor().getType().asPrimitive()));
            } else {
                format(fsel.getDescriptor().getType(), buf);
                buf.append('*');
            }
            buf.append(' ');
            buf.append(fsel.getName());

        } else {
            throw new Unimplemented();
        }
        return buf;
    }

    private String formatVariable(TypeName.Primitive tn) {
        switch (tn.getTypeTag()) {
            case TypeCodes.BOOLEAN :
                return "jboolean";
            case TypeCodes.INT :
                return "jint";
            case TypeCodes.SHORT :
                return "jshort";
            case TypeCodes.BYTE :
                return "jbyte";
            case TypeCodes.CHAR :
                return "jchar";
            case TypeCodes.LONG :
                return "jlong";
            case TypeCodes.FLOAT :
                return "jfloat";
            case TypeCodes.DOUBLE :
                return "jdouble";
            case TypeCodes.VOID :
                return "void";
            case TypeCodes.OBJECT :
            case TypeCodes.GEMEINSAM :
            case TypeCodes.ARRAY :
            default :
                throw failure("should not happen");
        }
    }

    private String formatField(TypeName.Primitive tn) {
        switch (tn.getTypeTag()) {
            case TypeCodes.BOOLEAN :
                return "zint";
            case TypeCodes.SHORT :
                return "sint";
            case TypeCodes.BYTE :
                return "bint";
            case TypeCodes.CHAR :
                return "cint";
            default :
                return formatVariable(tn);
        }
    }

    public StringBuffer format(Selector sel, StringBuffer buf) {
        throw new Unimplemented();
    }

    public final static CFormat _ = new CFormat();

}
