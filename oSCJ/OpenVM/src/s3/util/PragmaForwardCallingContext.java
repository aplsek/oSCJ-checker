package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * Declare that a method should be ignored by stack inspection code.
 * This is useful in user-domain methods such as Class.newInstance and
 * MemoryArea.newInstance that forward calls to objects (such as
 * Constructors) that perform access checks based on the calling
 * class.<p>
 *
 * This pragma also ensures that reflection, class loading, and
 * resource loading APIs work reflectively.  When Class.forName is
 * called reflectively, it should attempt to load a class from the
 * reflective caller's classloader, rather than the classloader for
 * Method.invoke().  We guarantee this behavior by marking
 * Method.invoke and Constructor.newInstance as
 * PragmaForwardCallingContext.<p>
 *
 * @see ovm.core.execution.RuntimeExports#getClassContext
 * @see s3.services.bootimage.CallingClassTransform
 **/
public class PragmaForwardCallingContext extends PragmaException {
    private static final TypeName.Scalar me =
	register(PragmaForwardCallingContext.class.getName(), null);

    public static boolean declaredBy(Selector.Method ms, Blueprint bp) {
	return declaredBy(me, ms, bp);
    }
}
