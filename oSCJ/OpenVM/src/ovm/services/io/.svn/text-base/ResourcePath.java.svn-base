package ovm.services.io;

import ovm.core.OVMBase;
import ovm.util.ByteBuffer;
import ovm.util.OVMException;
import ovm.util.logging.Logger;


/**
 * @author Krzysztof Palacz
 * List of resource containers to search for a resource.
 * 
 **/
public class ResourcePath extends OVMBase {
    static final String PATH_SEPARATOR = ":";
    
    private final ResourceContainer[] path_;
     
     /**
     * Search for classes in given path.
     * @param classpath a string containing a list of directories.
     **/
    public ResourcePath(String classpath) {
	String[] tokens = classpath.split(PATH_SEPARATOR);
        ResourceContainer[] tmp = new ResourceContainer[tokens.length];
	int curr = 0;
	for (int i = 0; i < tokens.length; i++) {
	    if (tokens[i].equals("") == false) {
                try {
                    tmp[curr] = 
			ResourceContainer.makeResourceContainer(tokens[i]);
		    curr++; // KP: prevent incrementing if Zip/Dir excepts
                } catch (OVMException.IO x) {
                    Logger.global.throwing("ResourcePath failed to find " 
					   + tokens[i], x);
                    // keep going!
                }
            }
	}
	if (curr < tokens.length) {
	    path_ = new ResourceContainer[curr];
	    System.arraycopy(tmp, 0, path_, 0, curr);
	} else {
	    path_ = tmp;
	}
    }
    
    public Resource getResource(String name, String suffix)
        throws OVMException.IO {
        for (int i = 0; i < path_.length; i++) {
            Resource cf = path_[i].getResource(name, suffix);
            if (cf != null)
                return cf;
        }
	return null;
    }
    
    public ByteBuffer getResourceContents(String name, String suffix) 
	throws OVMException.IO {
	Resource resouce = getResource(name, suffix);
	if (resouce != null) {
	    return resouce.getContents();
	} else {
	    return null;
	}
    }

    public String toString() {
        String str = "ResourcePath{";
        for (int i = 0; i < path_.length; i++) {
            str += path_[i];
            if (i < path_.length - 1) {
                str += ':';
            } else {
		str += '}';
	    }
        }
        return str;
    }
    
    public int length() {
	return path_.length;
    }
						
    public ResourceContainer resourceContainerAt(int index) {
	return path_[index];
    }
    
    public void forAll(String type, Resource.Action action) throws OVMException {
        for (int i = 0; i < path_.length; i++) {
             path_[i].forAll(type, action);
         }
    }

}
