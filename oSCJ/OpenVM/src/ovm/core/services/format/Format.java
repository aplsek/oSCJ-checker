package ovm.core.services.format;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;

/**
 * Convert TypeNames and Selectors from and to strings.
 * Loosely based on java.text.Format.
 * Not convinced about the package.
 **/ 
public abstract class Format extends ovm.core.OVMBase {
       
    public final String format(TypeName typeName) {
	return format(typeName, new StringBuffer()).toString();
    }
    
    public final String format(Selector selector) {
	return format(selector, new StringBuffer()).toString();
    }

    public final String format(UnboundSelector selector) {
	return format(selector, new StringBuffer()).toString();
    }

    /**
     * @return buf
     **/
    public abstract StringBuffer format(TypeName typeName, StringBuffer buf);

    /**
     * @return buf
     **/
    public abstract StringBuffer format(Selector selector, StringBuffer buf);

    /**
     * @return buf
     **/
    public abstract StringBuffer format(UnboundSelector selector, 
					StringBuffer buf);

}
