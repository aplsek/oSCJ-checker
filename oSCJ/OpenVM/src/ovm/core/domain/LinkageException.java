package ovm.core.domain;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.util.OVMException;
import ovm.util.OVMRuntimeException;
import ovm.core.services.format.JavaFormat;
import ovm.util.UnicodeBuffer;
import ovm.services.bytecode.JVMConstants.Throwables;

/**
 * Thrown whenever symbolic names are resolved 
 **/
public class LinkageException extends OVMException {
    public LinkageException() {
    }
    

    public LinkageException(String message) {
	super(message);
    }

    public LinkageException(String message, Throwable cause) {
	super(message);
	initCause(cause);
    }
    
    public OVMRuntimeException unchecked() {
	return new Runtime(this);
    }

    protected int getCode() { return Throwables.LINKAGE_ERROR; }
    protected Oop getMessage(JavaDomain dom) {
	if (getMessage() == null)
	    return null;
	return dom.makeString(UnicodeBuffer.factory().wrap(getMessage()));
    }

    public Oop toLinkageError(JavaDomain dom) {
	return dom.makeThrowable(getCode(), getMessage(dom), null);
    }

    public static class Runtime extends OVMRuntimeException {
	public Runtime(LinkageException checked) {
	    super(checked);
	}
    }

    public static class User extends LinkageException {
	Oop userError;

	public User(Oop userError) {
	    super();
	    this.userError = userError;
	}

	public Oop toLinkageError(JavaDomain _) { return userError; }
    }
    
    public static class NoClassDef extends LinkageException {
	private final TypeName tn;

	public NoClassDef(TypeName tn) { this.tn = tn; }
	public NoClassDef(String msg)  { super(msg); tn = null; }
	public int getCode() { return Throwables.NO_CLASS_DEF_FOUND_ERROR; }
	public String getMessage() {
	    if (super.getMessage() != null)
		return super.getMessage();
	    return tn.toString();
	}
	public Oop getMessage(JavaDomain dom) {
	    String explicit = super.getMessage();
	    return (explicit == null
		    ? dom.makeString(JavaFormat._.formatUnicode(tn))
		    : dom.makeString(UnicodeBuffer.factory().wrap(explicit)));
	}
    }

    public static class ClassFormat extends LinkageException {
	public ClassFormat(String msg) { super(msg); }
	public ClassFormat(String msg, Throwable cause) { super(msg, cause); }
	public int getCode() { return Throwables.CLASS_FORMAT_ERROR; }
    }

    public static class CyclicInheritance extends LinkageException {
	private final TypeName.Scalar tn;

	public CyclicInheritance(TypeName.Scalar tn) { this.tn = tn; }
	public int getCode() { return Throwables.CLASS_CIRCULARITY_ERROR; }
	public String getMessage() { return tn.toString(); }
	public Oop getMessage(JavaDomain dom) {
	    return dom.makeString(JavaFormat._.formatUnicode(tn));
	}
    }

    public static class Verification extends LinkageException {
	public Verification(String msg) { super(msg); }
	public int getCode() {
	    return Throwables.VERIFY_ERROR;
	}
    }

    public static class ClassChange extends LinkageException {
	public ClassChange(String msg) { super(msg); }
	public int getCode() {
	    return Throwables.INCOMPATIBLE_CLASS_CHANGE_ERROR;
	}
    }

    // And all the subtypes for fields, methods, abstract methods,
    //constructors, ...

    //-----------------------------------------------------------------
    //-----------------------UndefinedMember---------------------------
    //-----------------------------------------------------------------

    /**
     * Exception thrown if code tries to access a non-existant member.
     **/
    public static class UndefinedMember extends LinkageException {
        UnboundSelector us;
        TypeName tn;
        public UndefinedMember(TypeName tn, UnboundSelector us, String s) {
            super( tn + "." + us + s);
            this.tn = tn;
            this.us = us;
        }
        public UndefinedMember(TypeName tn, UnboundSelector us) {
            this( tn, us, "");
        }
        public UndefinedMember(Selector sel, String s) {
            super(sel + s);
            this.us = (sel instanceof Selector.Method
		       ? (UnboundSelector) sel.asMethod().getUnboundSelector()
		       : (UnboundSelector)  sel.asField().getUnboundSelector());
            this.tn = sel.getDefiningClass();
        }
        public UndefinedMember(Selector sel) {
            this( sel, "");
        }
        public UnboundSelector getSelector() {
            return us;
        }

        public String toString() {
            return "UndefinedMember: " + tn + "." + us;
        }
	public OVMRuntimeException unchecked() {
	    return new Runtime(this);
	}

	public class Runtime extends OVMRuntimeException {
	    public Runtime(UndefinedMember e) { 
		super(e);
	    }
	}

    } // end of Undefined Member

    public static class UnsatisfiedLink extends LinkageException {
	public UnsatisfiedLink(String msg) { super(msg); }
	public int getCode() { return Throwables.UNSATISFIED_LINK_ERROR; }
    }

    private static final UnicodeBuffer DOMAIN_FROZEN =
	// NOTE: this is a 7 bit string, so we don't have to specify UTF-8
	UnicodeBuffer.factory().wrap("Domain frozen".getBytes());

    public static class DomainFrozen extends LinkageException {
	String msg;
	public DomainFrozen(TypeName type) {
	    this.msg = "DomainFrozen while adding " + type;
	}
	public String getMessage() {
	    return msg;
	}
	public Oop getMessage(JavaDomain dom) {
	    return dom.makeString(
UnicodeBuffer.factory().wrap(msg.getBytes()));                                  
                                  //DOMAIN_FROZEN);
	}
	public int getCode() { return Throwables.CLASS_FORMAT_ERROR; }
    } 
}







