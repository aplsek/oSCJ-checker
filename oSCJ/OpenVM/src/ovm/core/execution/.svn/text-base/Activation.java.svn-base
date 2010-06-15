package ovm.core.execution;

import ovm.core.OVMBase;
import ovm.core.domain.Code;
import ovm.core.domain.Oop;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.util.UnsafeAccess;

/**
 * An activation record, or its projection in Java-space.  The original
 * proposal for this class had instance methods just like real Java
 * objects, only for simplicity of presentation, and warned that they might
 * later be optimized into class methods that take an "instance" as an
 * argument. That could still happen. You have been warned. On the other
 * hand, the probability has recently decreased.
 *
 * @author Flack, Grothoff
 **/
public abstract class Activation extends OVMBase 
    implements UnsafeAccess, Cloneable {

    /**
     * Return Code executing in this Activation.  I'm not sure whether
     * this method should be defined here.  For some engines
     * (interpreter and simplejit), we can obtain a code pointer very
     * quickly, but only when the Activation is valid.  For other
     * engines (j2c), obtaining a code pointer involves doing a binary
     * search based on the PC.  Rather than supporting getCode, we
     * could define an interface CodeHint { Code getCode(int pc); },
     * and a method Activation.getCodeHint().  (This interface could
     * be implemented by S3ByteCode, SimpleJITCode, and J2cActivation.)
     **/
    public abstract Code getCode();

    /**
     * Return this activation's program counter.  This method only
     * returns a well-defined value if the program counter remains
     * unchanged fromthe time this activation is taken (by caller or
     * Context.getCurrentActivation) and the time this method is
     * called. The exact representation of a program counter depends
     * on the type of activation, but it is never an index into the
     * original Java bytecode.
     **/
    public abstract int getPC();

    /**
     * Set this Activation object to the topmost activation record in
     * the given native context.  On return from setToCurrent in the
     * current thread, this method refers to the caller's activation.
     **/
    protected abstract void setToCurrent(int nativeContext);
    
    /**
     * Return an Activation that represents this activation's caller,
     * reuse the object a if possible.
     **/
    public abstract Activation caller(Activation a);

    /**
     * Raise a java exception.  Normally, this method will not return,
     * but if interpreter/simplejit or interpreter/j2c configurations
     * are supported, this method may return normally when propagating
     * an exception from compiled code back to interpeted code.
     * <p>
     * <code>processThrowable</code> must enforce several rules when
     * exceptions cross domain boundaries:
     * <ul>
     * <li> INVOKE_SYSTEM.INVOKE instructions may cause a
     * user-domain exception to propagate back to the executive
     * domain.  Catches of ovm.core.domain.WildcardException match all
     * exception types, and Context.makeWildcardException(Oop) should
     * be used to convert an arbitrary (possibly user-domain)
     * throwable to a WildcardException object.
     *
     * <li> CoreServicesAccess and RuntimeExcports calls may cause an
     * executive-domain exception to propagate to the user domain.
     * When this happens, tranlateThrowable(Throwable) should be
     * called on the calling domain's CoreServicesAccess object, and
     * exception dispatch should continue with the value returned.
     * </ul>
     **/
    public abstract void processThrowable(Oop throwable);

    /**
     * FIXME: This method should not exist.
     * <p>
     * Return the hardware representation of this activation.
     * This may be the interpreter's <code>struct Frame *</code>, or a
     * hardware frame pointer.  This method doesn't fully capture an
     * activation's state.
     * <p>
     * This method is called by conservative GC code, and the return
     * value is passed to the native function
     * <code>callerLocalsEnd</code>.  Maybe
     * <code>callerLocalsEnd</code> should take a pointer to a java
     * activation?  Yet another tricky aspect to mixed-mode operations
     * and GC.
     **/
    public abstract int getNativeHandle();
    
    public Object clone() {
	try {
	    return super.clone();
	} catch (CloneNotSupportedException e) {
	    // we are ****'ed
	    throw (Error)null;
	}
    }

    public static abstract class Factory {
	public abstract Activation make();
    }
    static Factory factory() throws PragmaStitchSingleton {
	return (Factory) InvisibleStitcher.singletonFor(Factory.class);
    }
} // end of Activation
