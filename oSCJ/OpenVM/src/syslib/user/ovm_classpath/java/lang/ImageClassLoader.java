package java.lang;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.io.File;

import org.ovmj.java.Opaque;


class ImageClassLoader extends URLClassLoader {
    final Opaque peer;
    // The use of String.split and regular expressions in general
    // within the bootstrap classloader's constructor leads to
    // circularity.
    //
    // Also, avoid using the <code>path.seperator</code> property,
    // because we don't want to trigger more static initialization
    // just yet.
    //
    // Currently path.seperator is hard-coded as ':', but we should
    // probably inherit these properties from the host VM with
    // RuntimeExports.defineVMProperty().
    //
    // Of course, if we can ever build under cygwin, it might be nice
    // to retain our Unix flavor.  Calling the Windows version of java
    // from a shell script is never any fun.
    private static String[] mySplit(String path) {
	int count = 1;
	int cur = -1;
	while ((cur = path.indexOf(':', cur+1)) != -1)
	    count++;
	String[] ret = new String[count];
	cur = -1;
	for (int i = 0; i < count; i++) {
	    int next = path.indexOf(':', cur+1);
	    ret[i] = path.substring(cur+1, next == -1 ? path.length() : next);
	    cur = next;
	}
	return ret;
    }

    private static URL[] toURLs(String path) {
	String[] files = mySplit(path);
 	URL[] ret = new URL[files.length];

	for (int i = 0; i < ret.length; i++)
	    try {
		if (!files[i].endsWith("/")
		    && new File(files[i]).isDirectory())
		    ret[i] = new URL("file", null, -1, files[i] + "/");
		else
		    ret[i] = new URL("file", null, -1, files[i]);
	    } catch (MalformedURLException e) {
		throw new Error("malformed URL in path " + files[i], e);
	    }
 	return ret;
     }

    public ImageClassLoader(ClassLoader parent, Opaque peer, String path) {
	super(new URL[0], parent);
	this.peer = peer;
	LibraryImports.setPeer(peer, this);
    }

    void addURLs(String path) {
	URL[] urls = toURLs(path);
	for (int i = 0; i < urls.length; i++)
	    addURL(urls[i]);
    }

    // Export this method to java.lang
    protected Class findClass(String name) throws ClassNotFoundException {
	return super.findClass(name);
    }
}
