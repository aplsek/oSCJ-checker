package s3.services.bootimage;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.Descriptor;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.format.CFormat;
import ovm.core.services.format.Format;
import ovm.util.OVMError.Unimplemented;
import ovm.util.HTString2int;

/**
 * @author jcf, kp
 **/

public class JNIFormat extends Format {

    /**
     * Encode using <code>encode(String, StringBuffer)</code>.
     **/

    public String encode(String src) {
	return encode(src, new StringBuffer()).toString();
    }

    
    /**
     * A first take on the name-mangling in the JNI spec. For maximum
     * strictness, this should probably be split into methods to apply to the
     * identifier-part or descriptor-part alone, applying only the
     * transformations appropriate to each. In its current laxness, this method
     * can be applied to either name or descriptor and ought to do the right
     * thing, but will be limited in its ability to detect malformed input. KP:
     * refactored from OVM2JDK.jniEncode()
     */

    private StringBuffer encode(String src, StringBuffer dst) {
 
        char[] schars = src.toCharArray();
        for (int i = 0; i < schars.length; ++i) {
            char c = schars[i];
            if (c == '.' || c == '/' || c == '$') {
                dst.append('_');
            } else if (c == '_') {
                dst.append("_1");
            } else if (c == ';') {
                dst.append("_2");
            } else if (c == '[') {
                dst.append("_3");
            } else if (Character.isLetterOrDigit(c)
                    && Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
                dst.append(c);
            } else {
                dst.append("_0");
                String hxs = Integer.toHexString(c);
                for (int j = 4 - hxs.length(); j > 0; --j)
                    dst.append('0');
                dst.append(hxs);
            }
        }
        return dst;
    }


    /**
     * Return a boolean array corresponding to an array of method selectors,
     * true where the corresponding selector is overloaded.
     * <p>
     * FIXME Currently uses a definition of overloading that might be subtly
     * inequivalent to the real one: equality of name (disregarding parameter
     * signature, but <em>including</em> defining class.) That assumes the
     * current behavior of S3Dispatch.getVTableSelectors() in constructing
     * the input array: all selectors will be bound to the <em>same</em>
     * class <em>except</em> for hidden methods in a superclass. Therefore
     * we will properly mark non-hidden overloaded methods as overloaded, but
     * will not mark any method overloaded simply because it <em>hides</em> a
     * method of the same name--it is unnecessary (and ugly) to produce a
     * long-form JNI name in that case.
     * @param sels the array of selectors whose overloaded status is to be
     *             marked
     * @return an array of boolean values; true if the corresponding
     *         input selector is overloaded, else false.
     **/
    public static boolean[] markOverloaded( Selector.Method[] sels) {

        boolean[] result = new boolean [ sels.length ];

        HTString2int map = new HTString2int();

        for ( int i = 0; i < sels.length; ++ i ) {
            String name = sels [ i ].getDefiningClass().toClassInfoString() +
                            '.' + sels [ i ].getName(); // see above
            int other = map.get( name);
            if ( other != HTString2int.NOTFOUND )
                result [ other ] = result [ i ] = true;
            else
                map.put( name, i);
        }
        return result;
    }

    /**
     * <strong>Warning:</strong> this method will produce the same output for a
     * Gemeinsam typename as for its corresponding instance typename.
     * FIXME is this desirable?
     **/
    public StringBuffer format(TypeName tn, StringBuffer buf) {
	if (!tn.isCompound()) {
	    return CFormat._.format(tn, buf);
	}
	return encode(tn.asCompound().toClassInfoString(), buf);
    }


    /**
     * Convert a method selector to a method name following
     * C naming conventions as advocated by JNI/javah.
     * <p>
     * This method follows the conventions as given in the
     * JNI specification in section 11.3 <em>Linking Native
     * Methods</em> (Page 151 in the book). 
     * Refactored from OVM2JDK.selector2jniName().
     * @param sel the method selector to convert
     * @param overloaded true if this selector is overloaded, else false
     * @param result the string buffer into which the string should be
     * appended
     * @return the converted method name, as a string in <tt>result</tt>
     * @see <a href="http://java.sun.com/j2se/1.4/docs/guide/jni/spec/design.doc.html">JNI Specification, section 11.3</a>
     **/

    public StringBuffer format(Selector.Method sel, 
			       boolean overloaded,
			       StringBuffer result) {
	result.append( "Java_");
        format(sel.getDefiningClass(), result);
	result.append( '_');
	encode(sel.getName(), result);
	
        if ( overloaded ) {
            result.append( "__");
	    
            Descriptor.Method md = sel.getDescriptor();
            String wholeDescriptor = md.toString();
	    
            int index = wholeDescriptor.indexOf( ')');
	    
            encode( wholeDescriptor.substring( 1, index), result);
        }
        return result;
    }


    public StringBuffer shortFormat(TypeName tn, StringBuffer result) {
	if (tn.isCompound()) {
	    if (tn.isArray()) {
		result.append("jarray");
	    } else {
		result.append("jobject");
	    }
	} else {
	    CFormat._.format(tn, result);
	}
	return result;
    }

    public StringBuffer formatHeader(Selector.Method sel,
				     boolean overloaded,
				     StringBuffer result) {
	Descriptor.Method desc = sel.getDescriptor();
	shortFormat(desc.getType(), result);
	result.append(' ');
	format(sel, overloaded, result);
	result.append('(');
	for (int i = 0; i < desc.getArgumentCount(); i++) {
	    TypeName arg = desc.getArgumentType(i);
	    shortFormat(arg, result);
	    result.append(" arg" + i);
	    if (i != desc.getArgumentCount() - 1) {
		result.append(", ");
	    }
	}
	result.append(")");
	return result;
    }
    

    // temporary
    public StringBuffer format(Selector sel, StringBuffer buf) {
	if (sel instanceof Selector.Method) {
	    return format((Selector.Method)sel, true, buf); // FIXME
	} else {
	    throw new Unimplemented("can't yet format " + sel);
	}
    }

    public TypeName parseTypeName(String source) {
	throw new Unimplemented("parseTypeName");
    }

    public Selector parseSelector(String source) {
	throw new Unimplemented("parseSelector");
    }

    public StringBuffer format(UnboundSelector selector, 
			       StringBuffer buf) {
	throw new Unimplemented();
    }

    public UnboundSelector parseUnboundSelector(String source) {
	throw new Unimplemented();
    }

    public static final JNIFormat _ = new JNIFormat();
    
}
