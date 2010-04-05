package ovm.core.repository;

import ovm.util.OVMException;
/**
 * Exceptions thrown from the repository should inherit from 
 * RepositoryException.
 **/
public class RepositoryException extends OVMException {
    /**
     * Constructor.
     * @param message Error message for repository exception
     **/
    public RepositoryException(String message) {
	super(message);
    }

    /**
     * Constructor. Used when this exception is caused by
     * catching some other <code>Throwable</code>
     * @param message Error message for this repository exception
     * @param cause Throwable that is causing this exception to be thrown
     **/
    public RepositoryException(String message, Throwable cause) {
	super(message, cause);
    }
 }