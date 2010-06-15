package gnu.classpath;

class LibraryGlue {
    static native int get_locale(byte[] charset, int len,
				 byte[] lang, int langlen,
				 byte[] region, int regionlen,
				 byte[] variant, int variantlen);
    static native int get_user_info(byte[] user, int userlen,
                                    byte[] home, int homelen,
                                    byte[] pwd, int pwdlen);
    static native int get_system_info(byte[] os, int oslen,
                                      byte[] version, int versionlen,
                                      byte[] arch, int archlen);
    // Hmm.  We really should have at least user-domain jars in some
    // location relative to java.home whether we are installed or not.
    // As it stands, layout is completely different depending on
    // whether or not we have run install, and we can't do things like
    // wrap javac to supply the appropriate -bootclasspath, or
    // define a useful value of java.home.
    static native int get_ovm_home(byte[] dir, int dirlen);
    static native int get_temp_directory(byte[] temp, int templen);
    static native int get_default_timezone_id(byte[] tz, int tzlen);
}