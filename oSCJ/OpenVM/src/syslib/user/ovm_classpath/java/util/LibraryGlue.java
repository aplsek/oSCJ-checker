package java.util;

/**
 * Provides redirection of native methods in this package. These can be
 * implemented directly here in Java code, redirected to OVM services via
 * <tt>LibraryImports</tt> methods, or redefined as native for actual native
 * code invocation - or some combination thereof.
 */
class LibraryGlue {

    // Timezone

    /**
     * Returns null. If we have the actual timezone info it has already
     * been set in System.properties which our caller has already read.
     */
    static String getSystemTimeZoneId() {
        return null;
    }
}
