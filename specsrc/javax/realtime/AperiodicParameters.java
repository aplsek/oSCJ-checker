package javax.realtime;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;


@SCJAllowed(LEVEL_1)
public class AperiodicParameters extends ReleaseParameters
{
	public AperiodicParameters() {
		super(null, null, null, null);
	}
}
