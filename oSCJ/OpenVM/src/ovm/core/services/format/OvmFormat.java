package ovm.core.services.format;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.util.UnicodeBuffer;

/**
 * @author Krzysztof Palacz
 **/

public class OvmFormat extends Format {

    

    public StringBuffer format(TypeName typeName, StringBuffer buf) {
	buf.append(typeName.toString());
	return buf;
    }

    public StringBuffer format(Selector selector, StringBuffer buf) {
	buf.append(selector.toString());
	return buf;
    }

    public StringBuffer format(UnboundSelector selector, 
			       StringBuffer buf) {

	buf.append(selector.toString());
	return buf;
    }

    public TypeName parseTypeName(String source) {
	return TypeName.parse(UnicodeBuffer.factory().wrap(source));
    
    }

    public UnboundSelector parseUnboundSelector(String source) {
	return UnboundSelector.parse(UnicodeBuffer.factory().wrap(source));
    }
    /**
     * Oh, this is sooo cute.
     **/
    public static final OvmFormat _ = new OvmFormat();
    
}
