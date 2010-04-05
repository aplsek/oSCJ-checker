package ovm.core.repository;


/**
 * Instances of this type represent an unresolved/unlinked String.
 * They are resolved by the VM at runtime to java.lang.String
 * instances of the appropriate domain and type.
 * 
 * @author Christian Grothoff
 */
public class RepositoryString {
	
    private final int utf8Index_;

    public RepositoryString(int utf8Index) {
	this.utf8Index_ = utf8Index;
    }

    public RepositoryString(String s) {
	utf8Index_ = RepositoryUtils.asUTF(s);
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        try {
            RepositoryString b = (RepositoryString) o;
	    return (b.utf8Index_ == utf8Index_);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    public String toString() {
        return UTF8Store._.getUtf8(utf8Index_).toString();
    }

    public int hashCode() {
        return utf8Index_;
    }

    public int getUtf8Index() {
	return utf8Index_;
    }

    public ConstantPool.UTF8Index getUTF8Index() {
	return new ConstantPool.UTF8Index(utf8Index_);
    }

}  // end of RepositoryString
