package s3.services.bootimage;

import ovm.core.domain.LinkageException;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.core.repository.TypeName.Scalar;
import ovm.services.io.ResourcePath;
import ovm.util.ByteBuffer;
import ovm.util.HTint2Object;
import ovm.util.HashSet;
import ovm.util.OVMException;

/**
 * The VM-generation time equivalent to {@code java.lang.ClassLoader}.
 * This class searches for class files along a resource path, and when
 * calls
 * {@link ovm.core.domain.Type.Context#defineType Type.Context.defineType()}
 * to construct the corresponding {@link Type Types} from them.<p>
 *
 * This class supports several methods to prevent specific types from
 * being loaded.
 **/
public class BuildTimeLoader implements Type.Loader {
    final Type.Context ctx;
    final BuildTimeLoader parent;
    final ResourcePath path;

    final HashSet excludedClasses = new HashSet();
    final HTint2Object excludedPackages = new HTint2Object();
    final HashSet includedClasses = new HashSet();
    
    public BuildTimeLoader(Type.Context ctx,
			   BuildTimeLoader parent,
			   ResourcePath path) {
	this.ctx = ctx;
	this.parent = parent;
	this.path = path;
	ctx.setLoader(this);
    }

    public BuildTimeLoader(Type.Context ctx,
			   BuildTimeLoader parent,
			   String path) {
	this(ctx, parent, new ResourcePath(path == null ? "" : path));
    }
    
    public Type loadType(TypeName.Scalar name) throws LinkageException {
	Type ret;
        try {
	   if (parent != null && (ret = parent.ctx.typeFor(name)) != null)
	        return ret;
        } catch (LinkageException _) { }

	if (excludedClasses.contains(name)
	    || (excludedPackages.get(name.getPackageNameIndex()) != null
		&& !includedClasses.contains(name)))
	    throw new LinkageException.NoClassDef(name);

	ByteBuffer buf;
	try {
	    buf = path.getResourceContents(name.toClassInfoString(),
					   ".class");
	} catch (OVMException.IO e) {
	    throw new LinkageException("reading " + name, e);
	}
	if (buf == null)
	    throw new LinkageException.NoClassDef(name);

	ret = ctx.defineType(name, buf);
	return ret;
    }

    public Oop getMirror() { return null; }

    public void excludeClasses(TypeName[] tn) {
	for (int i = 0; i < tn.length; i++)
	    excludedClasses.add(tn[i]);
    }

    public void excludePackage(String name) {
	excludedPackages.put(RepositoryUtils.asUTF(name), Boolean.TRUE);
    }

    public void includeClass(TypeName tn) {
	includedClasses.add(tn);
    }

    public void excludeClass(Scalar scalar) {
	excludedClasses.add(scalar);
    }
}
