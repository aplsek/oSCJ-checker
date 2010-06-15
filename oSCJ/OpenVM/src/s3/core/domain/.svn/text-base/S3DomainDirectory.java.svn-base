package s3.core.domain;

import ovm.core.domain.Domain;
import ovm.core.domain.DomainDirectory;
import ovm.core.domain.ExecutiveDomain;
import ovm.core.domain.Type;
import ovm.core.domain.UserDomain;
import ovm.core.repository.TypeName;
import ovm.util.Iterator;
import s3.core.S3Base;
import ovm.core.stitcher.InvisibleStitcher;


public class S3DomainDirectory
    extends S3Base
    implements DomainDirectory.Interface, ovm.util.PragmaLiveClass {
    
    protected /*final*/ S3ExecutiveDomain executiveDomain;

    // temporary
    protected UserDomain userDomain;
    protected String userBootClassPath;
    protected String userClassPath;

    private boolean edRegistered = false;

    String reflectiveMethodTrace;
    String reflectiveClassTrace;

    public S3DomainDirectory(String xdpath, 
			     String userBootClasspath,
			     String userClasspath,
			     String reflectiveMethodTrace,
			     String reflectiveClassTrace) {
	executiveDomain = new S3ExecutiveDomain(xdpath);
	this.userBootClassPath = userBootClasspath;
        this.userClassPath = userClasspath;
	this.reflectiveMethodTrace = reflectiveMethodTrace;
	this.reflectiveClassTrace = reflectiveClassTrace;
    }

    /**
     * Empty: no further actions needed after the invisible stitcher
     * creates this object.
     **/
    public void initialize() {
    }

    public UserDomain createUserDomain(TypeName.Scalar mainClassName) {
        userDomain = new S3JavaUserDomain(mainClassName, 
					  userBootClassPath,
					  userClassPath);
        return userDomain;
    }

    public ExecutiveDomain getExecutiveDomain() {
        return executiveDomain;
    }

    public UserDomain getUserDomain(int domainID) {
        assert(domainID == 1);
        return userDomain;
    }

    public Iterator domains() {
	return new Iterator() {
		int state = 0;
		public boolean hasNext() {
		    return state < 2;
		}
		public Object next() {
		    return (state++ == 0
			    ? (Domain) executiveDomain
			    : (Domain) userDomain);
		}
		public void remove() {
		    throw new UnsupportedOperationException();
		}
	    };
    }

    static private Type.Context[] allContexts = new Type.Context[4];
    static private int firstFree = 0;

    static synchronized int registerContext(S3TypeContext ctx) {
	while (firstFree < allContexts.length
	       && allContexts[firstFree] != null)
	    firstFree++;
	if (firstFree == allContexts.length) {
	    Type.Context[] nc = new Type.Context[allContexts.length * 2];
	    System.arraycopy(allContexts, 0, nc, 0, allContexts.length);
	    allContexts = nc;
	}
	allContexts[firstFree] = ctx;
	return firstFree++;
    }

    static synchronized void unregisterContext(S3TypeContext ctx) {
	if (ctx.getUID() < firstFree)
	    firstFree = ctx.getUID();
	allContexts[ctx.getUID()] = null;
    }

    public int maxContextID() {
	return allContexts.length - 1;
    }

    public Type.Context getContext(int id) {
	return allContexts[id];
    }
}


