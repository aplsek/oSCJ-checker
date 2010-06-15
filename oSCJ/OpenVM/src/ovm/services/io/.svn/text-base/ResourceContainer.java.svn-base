package ovm.services.io;

import ovm.core.OVMBase;
import ovm.util.OVMException;
import java.io.*;
import ovm.util.*;
import java.util.zip.*;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;
/**
 * A hierarchical container for resources. Could be a file system or 
 * an archive file such as a zip or jar file.
 * @author Krzysztof Palacz
 **/
public abstract class ResourceContainer extends OVMBase {

    public static ResourceContainer makeResourceContainer(String path) 
	throws OVMException.IO, BCbootTime {
	if (path.endsWith(".jar") || path.endsWith(".zip")) {
	    return new HostedZip(path);
	} else {
	    return new HostedDir(path);
	}
	// FIXME config, user vs hosted etc
    }

    private static final char FILE_SEPARATOR = '/';
    
    public abstract boolean containsResource(String name, String suffix);
    
    public abstract Resource getResource(String name, String suffix);

    /**
     * Iterate over all resources in container of specified type and perform
     * an action
     * @param suffix file extension specifying resource type
     * @param action what to perform on resource
     * @throws OVMException
     **/
	public abstract void forAll(String suffix, Resource.Action action)
        throws OVMException;

    
    public ByteBuffer getResourceContents(String name, String suffix)
	throws OVMException.IO {
	Resource resource = getResource(name, suffix);
	if (resource == null) {
	    return null;
	} else {
	    return resource.getContents();
	}
    }
    
    public abstract String getPath();

    public String toString() {
	return getPath();
    }

    /**
     * This should be transmogrified into StandaloneDir ...
     **/
    static class HostedDir extends ResourceContainer implements Ephemeral.Void {
	String path;
	File root;
	public HostedDir(String dir) throws BCbootTime {
	    this.path = dir;
	    root = new File(dir);
	}
	
	public String getPath() {
	    return path;
	}

	public boolean containsResource(String name, String suffix) throws BCbootTime {
            File file = new File(path + FILE_SEPARATOR + name + suffix);
            return file.exists() && !file.isDirectory();
        }
	
        public boolean containsDirectory(String name) throws BCbootTime {
            File file = new File(path + FILE_SEPARATOR + name);
            return file.isDirectory();
        }

	public Resource getResource(String name, String suffix) throws BCbootTime {
	    File file = new File(path + FILE_SEPARATOR + name + suffix);
	    if (!file.exists()) {
		return null;
	    }
	    return new Resource.HostedFile(root, file);
	}

	void forAll(File d, String type, Resource.Action act)
	    throws OVMException, BCbootTime
	{
	    File[] f = d.listFiles();
	    if (f == null)
		return;
	    for (int i = 0; i < f.length; i++) {
		if (f[i].isDirectory())
		    forAll(f[i], type, act);
		else if (f[i].getName().endsWith(type))
		    act.process(new Resource.HostedFile(root, f[i]));
	    }
	}
	
	public void forAll(String type, Resource.Action action) 
	    throws OVMException
	{
	    forAll(root, type, action);
        }
    }

    /**
     * This should be transmogrified into StandaloneZip ...
     **/
    
    private static class HostedZip extends ResourceContainer 
	implements Ephemeral.Void {
        private ZipFile zip;
        HostedZip(String path) throws OVMException.IO, BCbootTime {
            try {
                zip = new ZipFile(path);
            } catch (IOException e) {
                throw new OVMException.IO(e);
            }
        }

	public String getPath() {
	    return zip.getName();
	}

        public Resource getResource(String name, String suffix) throws BCbootTime {
            final ZipEntry entry = zip.getEntry(name + suffix);
            if (entry == null)
                return null;
	    return new Resource.HostedZippedFile(zip, entry);
        }

        public boolean containsDirectory(String name) {
            ZipEntry entry = zip.getEntry(name);
            return entry != null && entry.isDirectory();
        }

        public boolean containsResource(String name, String suffix) {
            ZipEntry entry = zip.getEntry(name);
            return entry != null && !entry.isDirectory();
        }

        public String toString() {
            return zip.getName().toString();
        }
        
        public void forAll(String suffix, Resource.Action action) 
           throws OVMException, BCbootTime {
           for (java.util.Enumeration i = zip.entries(); 
                i.hasMoreElements(); ) {
               ZipEntry entry = (ZipEntry)i.nextElement();
               String name = entry.getName();
               if (!entry.isDirectory() && name.endsWith(suffix)) {
                   action.process(getResource(name, ""));
               }
           }
       }
    }
   /* 
    private static class StandaloneDir extends ResourceContainer {
        private String dir;
        StandaloneDir(String dir) {
            assert(dir != null); // failfast
            this.dir = dir;
        }
	
	public String getPath() {
	    return dir;
	}
	
        public Resource getResource(String name, String suffix) {
            final String fname = dir + FILE_SEPARATOR + suffix;
	    return new Resource.StandaloneFile(fname);
        }

        public String toString() {
            return dir;
        }

        public boolean containsDirectory(String name) {
            throw new OVMError.Unimplemented();
        }

        public boolean containsResource(String name, String suffix) {
            throw new OVMError.Unimplemented();
        }
	
        public void forAll(String type, Resource.Action action) 
           throws OVMException {
               throw new OVMError.Unimplemented();
        }
    }*/
}
