package ovm.core.repository;

import ovm.core.repository.TypeName;
import ovm.core.OVMBase;
import ovm.core.services.format.OvmFormat;
import ovm.util.ByteBuffer;
import ovm.services.io.ResourcePath;
import ovm.services.bytecode.reader.Parser;
import ovm.util.OVMException;
import ovm.services.io.Resource;
import ovm.core.domain.LinkageException;

/**
 * RepositoryLoader services the bundle it is associated with.
 * It knows the resource path.
 **/

public class RepositoryLoader extends OVMBase {

    final ResourcePath resourcePath;
    private final Parser parser;

    public RepositoryLoader(ResourcePath resourcePath) {
        this.resourcePath = resourcePath;
        this.parser =
            ovm
                .services
                .bytecode
                .reader
                .Services
                .getServices()
                .getOVMIRParserFactory()
                .makeParser();

    }

    public String toString() {
        return "RepositoryLoader{" + resourcePath + "}";
    }
    /**
     * @return uninterned repository class.
     **/
    RepositoryClass load(TypeName.Scalar name) throws RepositoryException {
        String fn = asBaseName(name);
	// System.err.println("loading " + fn + " for type " + name);
        try {
            ByteBuffer data = resourcePath.getResourceContents(fn, ".class");
            if (data == null) {
                return null;
            }
            return parser.parse(fn, data, data.remaining());
        } catch (OVMException.IO e) {
            throw new RepositoryException("problems parsing", e);
        } catch (LinkageException e) {
	    throw new RepositoryException("problems parsing", e);
	}
    }

    void loadAll(final Bundle bundle) throws RepositoryException {
        try {
            resourcePath.forAll(".class", new Resource.Action() {
                public void process(Resource resource) throws OVMException {
                    // Force bundle to callback into loader and load
		    TypeName.Scalar tn = asTypeName(resource.getPath());
                    bundle.lookupClass(tn);
                }
            });
        } catch (RepositoryException e) {
            throw e;
        } catch (OVMException e) {
            throw e.unchecked("unexpected exception");
        }
    }

    private TypeName.Scalar asTypeName(String path) {
	int end = path.indexOf('.');
	if (end != -1)
	    path = path.substring(0, end);
        return OvmFormat._.parseTypeName("L" + path + ";").asScalar();
    }

    private String asBaseName(TypeName.Scalar name) {
        int utf8_pack = name.getPackageNameIndex();
        int utf8_name = name.getShortNameIndex();
        if (utf8_pack != 0) {
            return UTF8Store._.getUtf8(utf8_pack)
                + "/"
                + UTF8Store._.getUtf8(utf8_name);
        } else {
            return UTF8Store._.getUtf8(utf8_name).toString();
        }
    }
}
