package ovm.core.domain;

import ovm.core.repository.JavaNames;
/**
 * A <tt>RealtimeJavaUserDomain</tt> is a Java user-domain that supports the 
 * execution of a Java Virtual Machine, that implements the Realtime
 * Specification for Java (RTSJ).
 */
public interface RealtimeJavaUserDomain 
    extends JavaUserDomain, RealtimeJavaDomain {

    /**
     * Return a utility object that holds the common system types  of the
     * Realtime Java domain.
     */
    RealtimeJavaTypes commonRealtimeTypes();


    class RealtimeJavaTypes extends JavaTypes {

        public final Type.Scalar java_lang_VMRealtimeThread;

        /**
         * Create a RealtimeJavaTypes object for the given domain
         */
        public RealtimeJavaTypes(Domain d) {
            super(d);
            if (!dom.isExecutive()) {
                Type.Context tc = dom.getSystemTypeContext();
                java_lang_VMRealtimeThread = tc.
                    typeForKnown(JavaNames.java_lang_VMRealtimeThread).asScalar();
            }
            else
                java_lang_VMRealtimeThread = null;
        }
    }

}
