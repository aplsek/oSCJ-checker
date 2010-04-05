package s3.services.j2c;

import ovm.core.domain.Oop;
import ovm.core.repository.RepositoryString;
import ovm.core.services.memory.VM_Address;
import ovm.util.HashMap;
import ovm.util.IdentityHashMap;
import ovm.util.Iterator;
import ovm.util.Map;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;


/**
 * The interface by which non-aspect code can communicate with
 * aspects.  We use aspects to add dispatch tables to blueprints and an
 * array of object references to each domain.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 */
public abstract class FieldMagic {
    public static final FieldMagic _ = new Magic();

    public abstract VM_Address[] getVTable(S3Blueprint bp);
    public abstract VM_Address[] getIfTable(S3Blueprint bp);

    public abstract int findRoot(S3Domain d, Oop value);
    public abstract void saveRoots();

    static class Magic extends FieldMagic {
	HashMap domains = new HashMap();

	public VM_Address[] getVTable(S3Blueprint bp) {
	    return bp.j2cVTable;
	}
	public VM_Address[] getIfTable(S3Blueprint bp) {
	    return bp.j2cIfTable;
	}
	
	public int findRoot(S3Domain d, Oop value) {
	    IdentityHashMap root2offset = (IdentityHashMap) domains.get(d);
	    if (root2offset == null) {
		root2offset = new IdentityHashMap();
		domains.put(d, root2offset);
	    }
	    Integer off = (Integer) root2offset.get(value);
	    if (off == null) {
		off = new Integer(root2offset.size());
		root2offset.put(value, off);
	    }
	    return off.intValue();
	}

	public void saveRoots() {
	    for (Iterator dit = domains.entrySet().iterator();
		 dit.hasNext(); ) {
		Map.Entry dent = (Map.Entry) dit.next();
		S3Domain d = (S3Domain) dent.getKey();
		IdentityHashMap root2offset
		    = (IdentityHashMap) dent.getValue();
		if (d.j2cRoots != null
		    && d.j2cRoots.length == root2offset.size())
		    continue;

		d.j2cRoots = new Oop[root2offset.size()];
		for (Iterator it = root2offset.entrySet().iterator();
		     it.hasNext(); ) {
		    Map.Entry ent = (Map.Entry) it.next();
		    Oop key = (Oop) ent.getKey();
		    if (VM_Address.fromObject(key).asObject() instanceof RepositoryString)
			continue;
		    Integer idx = (Integer) ent.getValue();
		    d.j2cRoots[idx.intValue()] = key;
		}
	    }
	}
    }
}
