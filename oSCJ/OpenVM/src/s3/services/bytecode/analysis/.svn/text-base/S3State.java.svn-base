/**
 * A State is a collection of Frames that are important for the
 * current run of the Verifier.
 *
 * @file s3/services/bytecode/verifier/S3State.java 
 **/
package s3.services.bytecode.analysis;

import ovm.services.bytecode.analysis.Frame;
import ovm.services.bytecode.analysis.State;

/**
 * Collects the frames encountered during abstract
 * interpretation. Depending on the iteration strategy a state may
 * collect one or more <code>Frame</code> objects. For a standard
 * execution a state simply associates a single frame to the current
 * program counter. For bytecode verification, frames must be
 * associated to multiple program counters. Typically, a verifier
 * would keep one or more frames per control flow merge (ie program
 * counter which can be reached along multiple control flow paths).<p>
 *
 * The state is responsible of storing and retrieving frames, it does
 * perform merges. This is responsibility of the ProgramPoint
 * class.<p>
 *
 * For performance reasons State has an interface specific to the case
 * where there is a single frame associated with a given PC. This
 * should be used with care as some of the single-frame method may
 * throw errors in some implementations. The iterator should always be
 * ok.
 *
 * @see S3AbstractInterpreter
 * @author Christian Grothoff
 * @author Jan Vitek (jv@cs.purdue.edu)
 **/
public class S3State 
    implements State {

    /**
     * Map from pc to the list of Frames at that PC.
     **/
    protected HTint2S3State_FrameList map_;

    public S3State() {
	this.map_ = new HTint2S3State_FrameList();
    }

    public S3State(int size) {
	this.map_ = new HTint2S3State_FrameList(size);
    }

    public String toString() {
	String res = "STATE:\n";
	int[] pcs = map_.keys();
	for (int i=0;i<pcs.length;i++) {
	    FrameList fl = map_.get(pcs[i]);
	    if (fl != null)
		res = res + "At PC " + pcs[i] + ": " + fl.toString() + "\n";
	}
	res = res + "END STATE";
	return res;
    }

    /**
     * Replace the <code>Frame</code> at program counter <code>pc</code>
     * with the argument. If the State holds several Frames, then this
     * method should raise a run time error. (The alternative is to set a
     * random Frame which makes little sense.)
     * @param pc program counter
     * @param nf new frame
     **/
    public void setFrameAt(int pc, Frame nf) {
	map_.put(pc, new FrameList(null, nf));
    }

    public void deleteAllFramesAt(int pc) {
	map_.remove(pc);
    }

    public void deleteFrameAt(int pc,
			      Frame f) {
	State.Iterator it
	    = iterator(pc);
	while (it.currentFrame() != f)
	    it.nextFrame();
	it.removeFrame();	
    }

    /**
     * Adds the <code>Frame</code> argument to frames at program counter
     * <code>pc</code>. In implementations that do not allow multiple
     * states to be stored at the same pc, this is equivalent to
     * setFrameAt.
     * @param pc program counter
     * @param nf new frame
     **/
    public void addFrameAt(int pc, Frame nf) {
	FrameList fl = map_.get(pc);
	if (fl == null)
	    setFrameAt(pc, nf);
	else
	    map_.put(pc, new FrameList(fl, nf));
    }

    /**
     * Read the <code>Frame</code> at program counter <code>pc</code>.  If
     * the State does not contain a Frame at that pc, <code>null</code>
     * will be returned. If the states holds several Frames are that pc,
     * then one of them will be returned.
     * @param pc program counter
     * @return a Frame or null.
     **/
    public Frame getFrameAt(int pc) {
	FrameList fl = map_.get(pc);
	if (fl == null)
	    return null;
	else
	    return fl.getFrame();
    }

    /**
     * Returns the number of frames associated to the pc.
     * @param pc program counter
     * @return frame count 
     **/
    public int getFrameCountAt(int pc) {
	FrameList fl = map_.get(pc);
	int count = 0;
	while (fl != null) {
	    count++;
	    fl = fl.getNext();
	}
	return count;
    }

    /**
     * Obtain an iterator over the frames at this pc. Implementations may
     * assume that a single iterator is active at a time.
     * @param pc the program counter
     * @return an iterator
     **/
    public State.Iterator iterator(int pc) {
	return new Iterator(pc);
    }

    /**
     * Iterates over Frames at a pc.  
     *
     * @author Christian Grothoff
     **/
    public class Iterator 
	implements State.Iterator {
	private int pc_;
	private FrameList pos_;

	public Iterator(int pc) {
	    this.pc_ = pc;
	    this.pos_ = map_.get(pc);
	}

	/**
	 * Replace the current frame with the new frame. This method is not
	 * thread safe.
	 * @param nf new frame
	 **/
	public void replaceFrame(Frame nf) {
	    pos_.setFrame(nf);
	}
	/**
	 * Removes the current frame, returning the next frame.
	 * @return null if this was the last frame
	 **/
	public Frame removeFrame() {
	    if (pos_ == null)
		throw new Error("no current frame to remove!");
	    FrameList first = map_.get(pc_);
	    if (first == pos_) {
		pos_ = pos_.getNext();
		map_.put(pc_,pos_);
		if (pos_ != null)
		    return pos_.getFrame();
		else
		    return null;
	    }
	    FrameList last = null;
	    while (first != pos_) {
		last = first;
		first = first.getNext();
	    }
	    pos_ = pos_.getNext();
	    last.setNext(pos_);
	    if (pos_ != null)
		return pos_.getFrame();
	    else
		return null;
	}
	/**
	 * Get the next frame. 
	 * @return null if there is no next frame
	 **/
	public Frame nextFrame() {
	    if (pos_ == null)
		return null;
	    else {
		pos_ = pos_.getNext();
		if (pos_ == null)
		    return null;
		else
		    return pos_.getFrame();
	    }
	}
	/** Get the current frame. **/ 
	public Frame currentFrame() {
	    if (pos_ == null)
		return null;
	    else
		return pos_.getFrame();
	}
    } // End of Iterator    

    /**
     * List of Frames with the same PC (it is possible that we
     * can not merge all frames at the same PC because we don't
     * have the complete type hierarchy and thus can not merge
     * AbstractValues with References; in fact, depending on
     * the analysis, we may not even want to). 
     *
     * @author Christian Grothoff
     **/
    public static class FrameList {
	private FrameList next_;
	private Frame frame_;
	public FrameList(FrameList next,
		  Frame frame) {
	    this.next_ = next;
	    this.frame_ = frame;
	}
	public String toString() {
	    String res = "FRAMELIST:\n";
	    FrameList pos = this;
	    while (pos != null) {
		res = res + pos.frame_.toString() + "\n";
		pos = pos.next_;
	    }
	    return res + "END FRAMELIST";	    
	}
	public Frame getFrame() {
	    return frame_;
	}
	public FrameList getNext() {
	    return next_;
	}
	public 	void setFrame(Frame f) {
	    this.frame_ = f;
	}
	public void setNext(FrameList next) {
	    this.next_ = next;
	}
    }

} // end of S3State

