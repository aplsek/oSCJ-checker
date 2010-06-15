package s3.services.simplejit.bytecode;

import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryUtils;
import s3.util.EphemeralBureau;

/**
 * A type map to enable some types to be treated as other types.
 *
 * A primary use is for VM_Address and VM_Word to be treated as
 * integers in the abstract interpretation. This map is needed for
 * that purpose until all of the traces of VM_Address and VM_Word will
 * be gone.
 *
 * It is important not to deal with type interchaneability in an ad
 * hoc way, but to factor it out in a centralized place.
 *
 * @author Hiroshi Yamauchi
 */
public class EphemeralTypeMap {

    protected Domain dom;
    protected Type.Context tcontext;

    EphemeralTypeMap(Domain dom) {
	this.dom = dom;
	this.tcontext = dom.getApplicationTypeContext();
    }

    protected Type typeFor(String from) {
	try {
	    return tcontext.typeFor(RepositoryUtils.makeTypeName(from));
	} catch(LinkageException e) {
	    throw e.unchecked();
	}
    }

    /**
     * @return the Type to which the input Type is mapped if there is
     * a registered mapping, or the input Type itself if there is no
     * such mapping.
     */
    public Type map(Type from) {
	return EphemeralBureau.typeFor( from);
    }
}
