// $Header: /p/sss/cvs/OpenVM/src/ovm/services/memory/fifo/VM_FIFOArea.java,v 1.6 2004/04/19 01:32:13 pizlofj Exp $

package ovm.services.memory.fifo;

import ovm.core.services.memory.*;
import ovm.core.domain.*;

/**
 * An interface for managing a FIFO area.  A FIFO area contains two VM_Areas in it:
 * one for the 'future' write area and one for the 'past' read area.  This interface
 * is intentionally designed so that you can shoot yourself in the foot, or implement
 * the FIFO area model, depending on your mood and creativity.
 *
 * @author Filip Pizlo
 */
public interface VM_FIFOArea {
    
    /**
     * Returns the <code>VM_Area</code> that represents the future.
     */
    public VM_Area getFuture();
    
    /**
     * Returns the <code>VM_Area</code> that represents the past.
     */
    public VM_Area getPast();
    
    /**
     * Enter the past with intent to dequeue.  The results are undefined
     * if there is nothing to dequeue, or if you had already called this
     * but have not yet called dequeue().
     */
    public void enterForDequeue();
    
    /**
     * Leave the past and dequeue.  The results are undefined if you had
     * not called enterForDequeue().
     */
    public void dequeue();
    
    /**
     * Enter the future with intent to enqueue.  The results are undefined
     * if you had already called this method but had not yet called enqueue().
     * Will throw OOM if there is not enough memory to enqueue.
     */
    public void enterForEnqueue();
    
    /**
     * Leave the future and dequeue.
     */
    public void enqueue();
    
    /**
     * Reset the area to start from scratch.
     */
    public void reset();
    
    /**
     * Returns the maximum number of slices that this thing can support.
     */
    public int maxNumSlices();
    
    /**
     * Returns the size of the area
     */
    public int getSize();
    
    /**
     * Returns the number of bytes available for enqueueing
     */
    public int getAvailable();
    
    /**
     * Returns the mirror.
     */
    public Oop getMirror();
    
}

