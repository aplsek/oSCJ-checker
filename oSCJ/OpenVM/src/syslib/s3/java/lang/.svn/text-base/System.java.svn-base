package java.lang;
import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.util.Mem;

/**
 * @author Christian Grothoff
 * @author Hiroshi Yamauchi
 **/
public class System {

    public static void loadLibrary(String s) {
	// not supported!
    }

    
    public static String getProperty(String s) {
	if (s == "logging.level")
	    return "ALL"; // for now, valid: ALL, FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF
	return null;
    }


    public static void arraycopy(Object _src, int src_position,
				 Object _dst, int dst_position,
				 int length) {
	Oop src = OVMBase.asOop(_src);
	Oop dst = OVMBase.asOop(_dst);
	Blueprint _sbp = src.getBlueprint();
	Blueprint _dbp = dst.getBlueprint();

	if (!_sbp.isArray() || !_dbp.isArray())
	    throw new ArrayStoreException();

	Blueprint.Array sbp = _sbp.asArray();
	Blueprint.Array dbp = _dbp.asArray();
	
	if (src_position < 0 || dst_position < 0 || length < 0
	    || src_position + length > sbp.getLength(src)
	    || dst_position + length > dbp.getLength(dst))
	    throw new IndexOutOfBoundsException();

	if (src == dst)
	    MemoryManager.the().copyOverlapping(src, src_position, dst_position, length);
	else if ((dbp == sbp && dbp.getComponentBlueprint().isPrimitive())
		 || ((dbp == sbp || sbp.isSubtypeOf(dbp))
		     && (MemoryManager.the().areaOf(src)
			 == MemoryManager.the().areaOf(dst))))
	    MemoryManager.the().copyArrayElements(src, src_position,
					dst, dst_position,
					length);
	else if (sbp.getComponentBlueprint().isPrimitive()
		 || dbp.getComponentBlueprint().isPrimitive())
	    throw new ArrayStoreException();
	else {
	    Object[] sa = (Object[]) _src;
	    Object[] da = (Object[]) _dst;
	    while (length-- > 0)
		da[dst_position++] = sa[src_position++];
	}
    }



} // end of System
