/*
 * Visitor.java
 *
 */
package s3.util;


/**
 * A generic Visitor interface
 *
 */
public interface Visitor {

    /**
     * Visit the specified object.
     * @param o the object to visit
     *
     */
    void visit(Object o);
}
