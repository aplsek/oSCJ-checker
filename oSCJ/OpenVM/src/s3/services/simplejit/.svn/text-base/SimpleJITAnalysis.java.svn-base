package s3.services.simplejit;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Method;
import ovm.core.domain.Type;

/**
 * Wraps s3.services.bootimage.Analysis.
 * Want to avoid sucking it into the boot image.
 * @author yamauchi
 *
 */
public class SimpleJITAnalysis {
	protected Object anal;
	public SimpleJITAnalysis(Object anal) {
		this.anal = anal;
	}
	public Object get() { return anal; }
    public Blueprint blueprintFor(Type t) { return null; }
    public Method getTarget(Blueprint recv, Method m) { return null; }
}
