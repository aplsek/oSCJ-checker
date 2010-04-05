package ovm.core.services.format;
import ovm.core.repository.TypeName;
import ovm.util.HashMap;


/**
 * Use if e.g. you know you're single-threaded.
 * @author Krzysztof Palacz
 * 
 **/

public class CachingOvmFormat extends OvmFormat {

    private HashMap tnCache = new HashMap();
    
    public TypeName parseTypeName(String source) {
	TypeName result = (TypeName)tnCache.get(source);
	if (result != null) 
	    return result;
	result = super.parseTypeName(source);
	tnCache.put(source, result);
	return result;
    }

    public static final CachingOvmFormat _ = new CachingOvmFormat();
    
}
