package ovm.core.services.memory;

import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;

/**
 * The memory policy must be consulted throughout OVM code, including
 * the repository.  This dummy memory policy can be used at
 * image-build time, when there is no notion of a VM_Area, and can
 * even be used in standalone applications, such Christian's analysis
 * tools that once lived in s3.tools.
 **/
public class StandaloneMemoryPolicy extends MemoryPolicy {
    static MemoryPolicy _ = new StandaloneMemoryPolicy();
    
    public void leave(Object data)			{ }
    public Object enterHeap()				{ return null; }
    public Object enterExceptionSafeArea()		{ return null; }
    public Object enterAreaForMonitor(Oop oop)		{ return null; }
    public Object enterAreaForMirror(Oop oop)		{ return null; }
    public Object enterMetaDataArea(Type.Context ctx)	{ return null; }
    public Object enterInternedStringArea(Domain d)	{ return null; }
    public boolean isInternable(Domain d, Oop str)	{ return true; }
    public Object enterClinitArea(Type.Context ctx)	{ return null; }
    public Object enterServiceInstanceArea()	        { return null; }
    public Object enterRepositoryQueryArea()		{ return null; }
    public Object enterRepositoryDataArea()		{ return null; }
    public Object enterScratchPadArea()			{ return null; }
}
