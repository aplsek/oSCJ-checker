/* VMSystemProperties.java -- Allow the VM to set System properties.
   Copyright (C) 2004 Free Software Foundation

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package gnu.classpath;

import org.ovmj.java.NativeConstants;
import java.util.Properties;
import java.io.UnsupportedEncodingException;

class VMSystemProperties
{
    static void rawPrint(String msg) {
         int len = msg.length();
         for(int i = 0; i < len; i++)
             LibraryImports.printCharAsByte(msg.charAt(i));
     }

    /**
     * Get the system properties. This is done here, instead of in System,
     * because of the bootstrap sequence. Note that the native code should
     * not try to use the Java I/O classes yet, as they rely on the properties
     * already existing. The only safe method to use to insert these default
     * system properties is {@link Properties#setProperty(String, String)}.
     *
     * <p>These properties MUST include:
     * <dl>
     * <dt>java.version         <dd>Java version number
     * <dt>java.vendor          <dd>Java vendor specific string
     * <dt>java.vendor.url      <dd>Java vendor URL
     * <dt>java.home            <dd>Java installation directory
     * <dt>java.vm.specification.version <dd>VM Spec version
     * <dt>java.vm.specification.vendor  <dd>VM Spec vendor
     * <dt>java.vm.specification.name    <dd>VM Spec name
     * <dt>java.vm.version      <dd>VM implementation version
     * <dt>java.vm.vendor       <dd>VM implementation vendor
     * <dt>java.vm.name         <dd>VM implementation name
     * <dt>java.specification.version    <dd>Java Runtime Environment version
     * <dt>java.specification.vendor     <dd>Java Runtime Environment vendor
     * <dt>java.specification.name       <dd>Java Runtime Environment name
     * <dt>java.class.version   <dd>Java class version number
     * <dt>java.class.path      <dd>Java classpath
     * <dt>java.library.path    <dd>Path for finding Java libraries
     * <dt>java.io.tmpdir       <dd>Default temp file path
     * <dt>java.compiler        <dd>Name of JIT to use
     * <dt>java.ext.dirs        <dd>Java extension path
     * <dt>os.name              <dd>Operating System Name
     * <dt>os.arch              <dd>Operating System Architecture
     * <dt>os.version           <dd>Operating System Version
     * <dt>file.separator       <dd>File separator ("/" on Unix)
     * <dt>path.separator       <dd>Path separator (":" on Unix)
     * <dt>line.separator       <dd>Line separator ("\n" on Unix)
     * <dt>user.name            <dd>User account name
     * <dt>user.home            <dd>User home directory
     * <dt>user.dir             <dd>User's current working directory
     * <dt>gnu.cpu.endian       <dd>"big" or "little"
     * </dl>
     *
     * @param properties the Properties object to insert the system properties into
     */
    static void preInit(Properties p) {
        // the first thing we do is read the default platform encoding.
        // We know this string has to be expressed in the Portable Character
        // Set and so we can just convert byte to char directly
        byte[] charset = new byte[32]; // should be big enough ?
	byte[] lang = new byte[4];     // should *always* be big enough
	byte[] region = new byte[4];   // should *always* be big enough
	byte[] variant = new byte[32]; // should be big enough?
        int rc = LibraryGlue.get_locale(charset, charset.length,
					lang, lang.length,
					region, region.length,
					variant, variant.length);
        while (rc == org.ovmj.java.NativeConstants.ERANGE) {
            charset = new byte[charset.length*2];
	    lang = new byte[lang.length*2];
	    region = new byte[region.length*2];
	    variant = new byte[variant.length*2];
            rc = LibraryGlue.get_locale(charset, charset.length,
					lang, lang.length,
					region, region.length,
					variant, variant.length);
        }

	final String DEFAULT_CHARSET = "8859_1";  // classpath default
        String charsetName = makeRawString(charset);
	try {
	    if (charsetName.length() == 0)
		charsetName = DEFAULT_CHARSET;
	    new String(new byte[0], charsetName);
	} catch (Throwable _) {
	    rawPrint("bad charset: " + charsetName +
		     ", using: " + DEFAULT_CHARSET + "\n");
	    charsetName = DEFAULT_CHARSET;
	}

	// Let's make sure to be consistent about this
	p.setProperty("file.encoding", charsetName);
	if (lang[0] != 0)
	    p.setProperty("user.language", makeRawString(lang));
	if (region[0] != 0)
	    p.setProperty("user.region", makeRawString(region));
	if (variant[0] != 0)
	    p.setProperty("user.variant", makeRawString(variant));

        // FIXEME: is this our internal version number or the Java Platform
        // version we support?
        p.setProperty("java.version", "1.4"); 
        p.setProperty("java.vendor", "Purdue University"); 
        p.setProperty("java.vendor.url", "http://ovmj.org"); 
        p.setProperty("java.vm.specification.version", "2.0"); 
        p.setProperty("java.vm.specification.vendor", "Sun Microsystems Inc.");
        p.setProperty("java.vm.specification.name", "Java Virtual Machine Specification"); 

        // FIXME: this should be tied to our build/version numbers
        p.setProperty("java.vm.version", "0.01"); 
        p.setProperty("java.vm.vendor", "Purdue University"); 
        p.setProperty("java.vm.name", "OVM"); 
        p.setProperty("java.specification.version", "1.4"); 
        p.setProperty("java.specification.vendor", "Sun Microsystems Inc."); 
        p.setProperty("java.specification.name", "Java Platform API Specification"); 
        p.setProperty("java.class.version", "47.0"); 
    
        // The JDK leaves this unset
        //p.setProperty("java.compiler", ""); 

        // FIXME: These should be system specific but we don't run on win32
        p.setProperty("file.separator", "/"); 
        p.setProperty("path.separator", ":"); 
        p.setProperty("line.separator", "\n");

        // These next properties are all dynamic and must be found at runtime

        // the native query functions try to balance space versus time issues.
        // we could ask for all values at once and require lots of temporary
        // array space; or we could ask for one at a time and require more
        // native calls and a much bigger API. We comprise by grouping things
        // that are related and/or the info is retrieved by a single system
        // call.

        // temp arrays: short strings < 256 chars; long < 4096
        // short for simple names, long for paths (4096 is _PATH_MAX on
        // some linux systems)

        byte[] sstr = new byte[256];

//	HACK: reduced so that it's guaranteed contiguous with arraylets
//        byte[] lstr1 = new byte[4096];

        byte[] lstr1 = new byte[2000];
        
//	HACK: reduced so that it's guaranteed contiguous with arraylets        
//        byte[] lstr2 = new byte[4096];
        byte[] lstr2 = new byte[2000];


        // if there are any "hard" errors each buffer reads "<unascertainable>"
        // Truncation is still possible in most cases.

        LibraryGlue.get_user_info(sstr, sstr.length, 
                                  lstr1, lstr1.length,
                                  lstr2, lstr2.length);

        p.setProperty("user.name", makeString(sstr, charsetName));
        p.setProperty("user.home", makeString(lstr1, charsetName));
        p.setProperty("user.dir", makeString(lstr2, charsetName));

        LibraryGlue.get_system_info(sstr, sstr.length, 
                                    lstr1, lstr1.length,
                                    lstr2, lstr2.length);
        p.setProperty("os.name", makeString(sstr, charsetName));
        p.setProperty("os.version", makeString(lstr1, charsetName));
        p.setProperty("os.arch", makeString(lstr2, charsetName));

        LibraryGlue.get_temp_directory(lstr1, lstr1.length);
        p.setProperty("java.io.tmpdir", makeString(lstr1, charsetName));

        // an error here means truncated output
        LibraryGlue.get_ovm_home(lstr1, lstr1.length);
        p.setProperty("java.home", makeString(lstr1, charsetName));

        
        LibraryGlue.get_default_timezone_id(sstr, sstr.length);
        String tz = makeString(sstr, charsetName);
        if (tz.length() > 0)
            p.setProperty("user.timezone", tz);

	p.setProperty("gnu.cpu.endian",
		      NativeConstants.BYTE_ORDER == NativeConstants.BIG_ENDIAN
		      ? "big"
		      : "little");
	int i = 0;
	for (String k = LibraryImports.VMPropertyName(i);
	     k != null;
	     k = LibraryImports.VMPropertyName(++i))
	    p.setProperty(k, LibraryImports.VMPropertyValue(i));


        // If -classpath or -Djava.class.path was provided as an
        // argument, it better not have been provided to gen-ovm, and
        // it will be set here
        JavaVirtualMachine.processJVMArgs();

        // Otherwise, we can try $CLASSPATH, or just use "."
        if (p.getProperty("java.class.path") == null) {
            String cp = System.getenv("CLASSPATH");
            if (cp == null)
                cp = ".";
            p.setProperty("java.class.path", cp);
        }

        // FIXME these are runtime properties but we don't support them yet anyway
        p.setProperty("java.library.path", "");
        p.setProperty("java.ext.dirs", "");

    }

    /**
     * Creates a new String by copying the bytes in data, up to the first
     * NUL (if present), into a character array that is used to construct 
     * a String. 
     * <p>We can't use the byte[] constructor for String because that will 
     * invoke the EncodingManager and we're using this method to define
     * the default character set encoding. The bytes are known to be simple
     * single-byte characters as they must be in the Portable Character Set
     *
     * <p>This method is not thread-safe and is only intended to be called
     * by insertSystemProperties.
     */
    private static String makeRawString(byte[] data) {
        char[] temp = new char[data.length];
        int i = 0;
        for (i = 0; i < temp.length; i++) {
            if (data[i] == '\0')
                break;
            temp[i] = (char) data[i];
        }
        if (i == 0)
            return "";
        else 
            return new String(temp, 0, i);
    }

    /**
     * Creates a new String using the bytes in data, up to the first
     * NUL (if present), using the platforms default decoding.
     *
     * <p>This method is not thread-safe and is only intended to be called
     * by insertSystemProperties.
     */
    private static String makeString(byte[] data, String encoding) {
        int i;
        for (i = 0; i < data.length; i++)
            if (data[i] == '\0')
                break;
        if (i == 0)
            return "";
        else
	    try {
		return new String(data, 0, i, encoding);
	    } catch (UnsupportedEncodingException e) {
		// Can't happen, we already verified it!
		throw new Error("bad default encoding", e);
	    }
    }


    /**
     * Here you get a chance to overwrite some of the properties set by
     * the common SystemProperties code. For example, it might be
     * a good idea to process the properties specified on the command
     * line here.
     */
    static void postInit(Properties properties)
    {
	LibraryImports.printString("done initing sys props\n");
    }
}
