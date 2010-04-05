/**
 * @file ovm/core/domain/ReferenceVisitor.java
 **/
package ovm.core.domain;

/**
 * Interface for visitors that will visit the reference fields within
 * an <code>Oop</code>
 * @author Palacz, Grothoff
 **/
public interface ReferenceVisitor {
    /**
     * Process the reference at offset <code>offset</code>
     * within <code>Oop</code> oop.
     * @param oop the oop being visited
     * @param offset the offset of the reference from the beginning of the oop
     **/
    void process(Oop oop, int offset);
} // End of ReferenceVisitor
