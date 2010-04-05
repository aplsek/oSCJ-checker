package s3.services.bytecode.analysis;

/**
 * A Label is a marker for a variable in the code.
 * Examples are arguments, receiver, return values,
 * allocation sites, field names, and so on.
 * 
 * The Label class is mostly used to wrap around
 * the specific object that indicates any one of
 * these sites.
 * 
 * @see LabeledAbstractValue
 * @author Christian Grothoff
 */
public class Label 
    implements Handle {

    private Object siteIdentifier_;

    public Label(Object siteIdentifier) {
	this.siteIdentifier_ = siteIdentifier;
    }

    public String toString() {
	return "<" + siteIdentifier_ + ">";
    }

    public int hashCode() {
	if (siteIdentifier_ != null)
	    return siteIdentifier_.hashCode();
	else
	    return 0;
    }

    public boolean equals(Object o) {
	if (o instanceof Label) {
	    Object oh = ((Label)o).siteIdentifier_;
	    if (oh == siteIdentifier_)
		return true;
	    if (siteIdentifier_ == null)
		return false;
	    return siteIdentifier_.equals(oh);
	} else 
	    return false;
    }

    public Object getSiteIdentifier() {
	return siteIdentifier_;
    }

} // end of Label