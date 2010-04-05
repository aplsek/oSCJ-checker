package s3.services.simplejit.bootimage;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import s3.services.bootimage.Analysis;
import s3.services.simplejit.SimpleJITAnalysis;

public class SimpleJITBootImageAnalysis extends SimpleJITAnalysis {
	private Analysis _anal;
	public SimpleJITBootImageAnalysis(Object anal) {
		super(anal);
		_anal = (Analysis) anal;
	}
    public Blueprint blueprintFor(Type t) { 
    	return t.getDomain().blueprintFor(t);
    }
    public Method getTarget(Blueprint recv, Method m) { 
    	return _anal.getTarget(recv, m);
    }
}
