package ovm.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;

/**
 * Abstraction of a location (where an event/constraint happened).
 *
 * Based on code previously in Kacheck/Jamit/Hitsuji.
 **/
public class Location {

    private final String description_;

    public Location(String descr) {
	this.description_ = descr;
    }

    /**
     * Get a (long) description of the location.
     **/
    public String getDescription() {
	return description_;
    }


    public static class Bytecode 
	extends Location {

	private final Selector.Method sel_;
	private final int pc_;
	private final int hc_;

	public Bytecode(Selector.Method sel,
			int pc,
			String opcode) {
	    super(opcode);
	    this.sel_ = sel.makeLBound();
	    this.pc_ = pc;
	    this.hc_ = pc + sel_.hashCode();
	}

	/**
	 * Get the name of the class where the constraint was generated.
	 **/
	public TypeName.Scalar getTypeName() {
	    return (TypeName.Scalar) sel_.getDefiningClass();
	}

	/**
	 * Get a (long) description of the location.
	 **/
	public String getDescription() {
	    return sel_.toString() + " " + pc_ + " (" + super.getDescription()+ ")";
	}
	
	public int getPC() {
	    return pc_;
	}
	
	public int hashCode() {
	    return hc_;
	}

	public boolean equals(Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o instanceof Location.Bytecode) {
		Location.Bytecode l = (Location.Bytecode) o;
		return sel_.equals(l.sel_) && (l.pc_ == pc_);
	    }
	    return false;
	}

	public String getOpcodeName() {
	    return super.toString();
	}

	public Selector.Method getMethodSelector() {
	    return sel_;
	}

	public Selector.Method getSelector() {
	    return sel_;
	}
	
	public String toString() {
	    return "ByteCode: " + getDescription();
	}
	
    } // end of Location.Bytecode
    

    public static class ClassFile 
	extends Location {

	private final TypeName.Scalar className_;
	
	public ClassFile(TypeName.Scalar className,
			 String description) {
	    super(description);
	    this.className_ = className;
	}
	/**
	 * Get the name of the class where the constraint was generated.
	 **/
	public TypeName.Scalar getTypeName() {
	    return className_;
	}
	
	public int hashCode() {
	    return className_.hashCode();
	}

	public boolean equals(Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o instanceof Location.ClassFile) {
		Location.ClassFile l = (Location.ClassFile) o;
		return (l.className_ == className_);
	    }
	    return false;
	}	

	/**
	 * Get a (long) description of the location.
	 **/
	public String getDescription() {
	    return super.toString() + " " + className_.toString();
	}

	public String toString() {
	    return "ClassFile: " + super.toString();
	}
	
    } // Location.ClassFile

} // end of Location
