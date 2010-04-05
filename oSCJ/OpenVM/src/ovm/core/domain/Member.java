/**
 * @file ovm/core/domain/Member.java
 **/
package ovm.core.domain;

/**
 * Interface for class member objects (for example, methods and fields)
 **/
public interface Member {
    /**
     * Get the declaring class or interface <code>Type</code> object for
     * this member.
     * @return the Type object for the declaring class
     * @see Type
     **/
    Type.Compound getDeclaringType();
} // end of Member
