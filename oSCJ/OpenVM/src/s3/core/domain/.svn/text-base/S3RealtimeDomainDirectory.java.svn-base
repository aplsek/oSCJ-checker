package s3.core.domain;

import ovm.core.domain.UserDomain;
import ovm.core.repository.TypeName;


public class S3RealtimeDomainDirectory
    extends S3DomainDirectory {
    
    public S3RealtimeDomainDirectory(String xdpath, 
                                     String userBootClasspath,
                                     String userClasspath,
                                     String reflectiveMethodTrace,
                                     String reflectiveClassTrace) {
        super(xdpath, userBootClasspath, userClasspath,
              reflectiveMethodTrace, reflectiveClassTrace);
    }


    /** Overrides <tt>S3DomainDirectory</tt> to create a Real-time
     * Java user domain
     */
    public UserDomain createUserDomain(TypeName.Scalar mainClassName) {
        userDomain = new S3RealtimeJavaUserDomain(mainClassName, 
                                                  userBootClassPath,
                                                  userClassPath);
        return userDomain;
    }
}


