package java.nio.channels;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import gnu.java.nio.channels.FileChannelImpl;

/**
 * Native methods for java.nio.channels
 *
 * @author Christian Grothoff
 */
class LibraryGlue {
    
    static FileInputStream newInputStream(Channels _,
					  FileChannelImpl ch) {
	return java.io.LibraryBounce.makeFIS(ch);
    }

    static FileOutputStream newOutputStream(Channels _,
					    FileChannelImpl ch) {
	return java.io.LibraryBounce.makeFOS(ch);
    }

}
