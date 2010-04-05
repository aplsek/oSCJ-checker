/**
 * @file ovm/services/bytecode/verifier/Verifier.java 
 **/
package ovm.services.bytecode.verifier;

import ovm.core.repository.RepositoryClass;

/**
 * Bytecode-Verifier for OVM.  This interface describes a bytecode
 * verifier.
 *
 * @author Christian Grothoff
 **/
public interface Verifier {

    /**
     * Verify the given class.
     * @param rc the class to verify
     * @throws VerificationError in case of errors
     **/
    public void verify(RepositoryClass rc)
	throws VerificationError;    

    interface Factory {
	Verifier makeVerifier();
    }

} // end of Verifier
