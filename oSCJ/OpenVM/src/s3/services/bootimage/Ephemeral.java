package s3.services.bootimage;

/**
 * A class implementing Ephemeral will transformed into a different
 * type.  The code and static fields of an Ephemeral type emulate
 * the new type at image build time.  When the image is written,
 * values of Ephemeral types are included in the image (after
 * suitable transformation), but the Ephemeral subtype itself is
 * dropped as it is no longer needed.
 **/
public interface Ephemeral {
    /**
     * Ephemeral.Void values only exist at image build time.
     * Ephemeral.Void and its subtypes are excluded from the
     * bootimage, and values of ephemeral types are replaced by null.
     **/
    public interface Void extends Ephemeral {
    } 
} // End of Ephemeral
