package s3.util;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.domain.Blueprint;

/**
 * Declared that linkage errors are expected in this method's body.
 * Generally, linkage errors occurr because we want them to.  In
 * particular, we routinely block classes in ovm.jar from being loaded
 * into a virtual machine's executive domain.  This leads to linkage
 * errors in methods of other classes, which we would prefer not to
 * hear about during VM generation.<p>
 *
 * Currently, {@link s3.services.bootimage.Inliner} treats
 * PragmaWontLink as equivalent to {@link PragmaNoInline}.  This is
 * because j2c cannot compile methods with linkage errors, and we do
 * not want WontLink-edness to spread to callers.<p>
 *
 * For methods that will never possibly link, see
 * {@link PragmaTransformCallsiteIR.BCdead BCdead}.
 **/
public class PragmaMayNotLink extends PragmaException {
    private static final TypeName.Scalar me =
	register(PragmaMayNotLink.class.getName(), null);

    public static boolean declaredBy(Selector.Method ms, Blueprint bp) {
	return declaredBy(me, ms, bp);
    }
}