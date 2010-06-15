package gnu.java.nio.charset.iconv;

class LibraryGlue {
    static RuntimeException die() {
	throw new IllegalArgumentException("iconv not available");
    }

    static void openIconv(IconvEncoder _, String __) { die(); }
    static int encode(IconvEncoder _, char[] in, byte[] out,
		      int posIn, int remIn, int posOut, int remOut) {
	throw die();
    }
    static void closeIconv(IconvEncoder _) { die(); }

    static void openIconv(IconvDecoder _, String __) { die(); }
    static int decode(IconvDecoder _, byte[] in, char[] out,
	       int posIn, int remIn, int posOut, int remOut) {
	throw die();
    }
    static void closeIconv(IconvDecoder _) { die(); }
}
