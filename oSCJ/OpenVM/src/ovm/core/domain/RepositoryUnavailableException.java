/**
 * @file ovm/core/domain/RepositoryUnavaiableException.java
 **/
package ovm.core.domain;


/**
 * This exception is thrown when the repository is unavailable for
 * any reason.
 **/
public class RepositoryUnavailableException extends RuntimeException {

    /**
     * Create a new RepositoryUnavailableException with an
     * error message.
     * @param message the message to be associated with the exception
     **/
    public RepositoryUnavailableException(String message) {
	super(message);
    }
    
}
