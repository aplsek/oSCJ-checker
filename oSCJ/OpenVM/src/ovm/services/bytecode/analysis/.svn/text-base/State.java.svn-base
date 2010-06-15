/**
 * @file ovm/services/bytecode/verifier/State.java 
 **/
package ovm.services.bytecode.analysis;


/**
 * Collects the frames encountered during abstract interpretation.
 * Depending on the iteration strategy a state may collect one or more
 * <code>Frame</code> objects. For a standard execution a state simply
 * associates a single frame to the current program counter. For bytecode
 * verification, frames must be associated to multiple program
 * counters. Typically, a verifier would keep one or more frames per
 * control flow merge (ie program counter which can be reached along
 * multiple control flow paths).<p> The state is responsible of storing and
 * retrieving frames, it does perform merges. This is responsibility of the
 * ProgramPoint class.<p> For performance reasons State has an interface
 * specific to the case where there is a single frame associated with a
 * given PC. This should be used with care as some of the single-frame
 * method may throw errors in some implementations. The iterator should
 * always be ok.
 * KLB: not found: ovm.services.bytecode.interpreter.Interpreter - is the following correct?
 * @see ovm.core.execution.Interpreter
 * @author Christian Grothoff
 * @author Jan Vitek (jv@cs.purdue.edu)
 **/
public interface State {

    /**
     * Replace the <code>Frame</code> at program counter <code>pc</code>
     * with the argument. If the State holds several Frames, then this
     * method should raise a run time error. (The alternative is to set a
     * random Frame which makes little sense.)
     * @param pc program counter
     * @param nf new frame
     **/
    public void setFrameAt(int pc, Frame nf);

    /**
     * Delete all Frames accumulated at the given pc.
     * Used to remove no longer needed state.  Use
     * with caution...
     */
    public void deleteAllFramesAt(int pc);

    /**
     * Adds the <code>Frame</code> argument to frames at program counter
     * <code>pc</code>. In implementations that do not allow multiple
     * states to be stored at the same pc, this is equivalent to
     * setFrameAt.
     * @param pc program counter
     * @param nf new frame
     **/
    public void addFrameAt(int pc, Frame nf);

    /**
     * Read the <code>Frame</code> at program counter <code>pc</code>.  If
     * the State does not contain a Frame at that pc, <code>null</code>
     * will be returned. If the states holds several Frames are that pc,
     * then one of them will be returned.
     * @param pc program counter
     * @return a Frame or null.
     **/
    public Frame getFrameAt(int pc);

    /**
     * Returns the number of frames associated to the pc.
     * @param pc program counter
     * @return frame count 
     **/
    public int getFrameCountAt(int pc);
    /**
     * Obtain an iterator over the frames at this pc. Implementations may
     * assume that a single iterator is active at a time.
     * @param pc the program counter
     * @return an iterator
     **/
    public State.Iterator iterator(int pc);

    public void deleteFrameAt(int pc,
			      Frame f);

    /**
     * Iterates over Frames at a pc. Typical use:
     * <code>
     * for (Frame f=it.currentFrame();f!=null;f=it.nextFrame()) { 
     * // do stuff
     * }
     * </code>
     **/
    public interface Iterator {
	/**
	 * Replace the current frame with the new frame. This method is not
	 * thread safe.
	 * @param nf new frame
	 **/
	void replaceFrame(Frame nf);
	/**
	 * Removes the current frame, returning the next frame.
	 * @return null if this was the last frame
	 **/
	Frame removeFrame();
	/**
	 * Get the next frame. 
	 * @return null if there is no next frame
	 **/
	Frame nextFrame();
	/**
	 * Get the current frame.
	 **/ 
	Frame currentFrame();

    } // End of Iterator

    

} // end of State

