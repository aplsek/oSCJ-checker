package s3.services.bootimage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.StringTokenizer;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;

import ovm.core.OVMBase;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;
import ovm.util.OVMRuntimeException;

/**
 * This is a superclass of all classes in
 * <code>s3.services.bootimage</code>, and provides static methods
 * for selectively logging messages as the image build progresses.
 * <b>NOTE</b>: This class is only public to keep the javadoc in this
 * package readable.  It should not be extended outside of
 * <code>s3.services.bootimage</code>.
 **/
public class BootBase extends OVMBase implements Ephemeral.Void {

    private static String log_list = "The following log options are defined:\n";
    private static final String space_pad = "                    ";
    
    private static void defFlag(String name, String desc) {
	log_list += (name + space_pad.substring(name.length(), 20) + desc + "\n");
    }

    public static final int LOG_ALL = -1;
    static { defFlag("all", "enable all types of logging"); }
  
    /** print a message when a bytecode can't be rewritten **/
    public static final int LOG_REWRITE_ERRORS = 1;
    static { defFlag("rewrite-errors",
		     "errors during bytecode transformation"); }
    /**
     * print a message when a user-level native method is unimplemented.
     **/
    public static final int LOG_MISSING_NATIVE = 2;
    static {
	defFlag("missing-native",
		"list classpath native methods not covered by LibraryGlue");
    }

    /**
     * print a message describing how each user-level native method is
     * resolved
     **/
    public static final int LOG_NATIVE = 4;
    static { defFlag("native", "transformations on native methods"); }

    /** print a message describing each field with an incomplete type **/
    public static final int LOG_INCOMPLETE_TYPES = 8;
    static { defFlag("incomplete-types", "list fields with incomplete types"); }

    /** show write-barrier insertion progress **/
    public static final int LOG_BARRIERS = 16;
    static { defFlag("barriers", "insertion of write barriers"); }

    /** show pollcheck insertion progress **/
    public static final int LOG_POLLCHECKS = 32;
    static { defFlag("pollchecks", "insertion of GC/preemption safe points"); }

    /** enable verbose trace of bytecode rewriting **/
    public static final int LOG_REWRITING = 64;
    static { defFlag("rewriting",
		     "general messages during bytecode transformation"); }
    
    /**
     * The default mask enables all log messages, but this value is
     * overridden by a call to parseLogOptions
     **/
    public static int logMask = -1;

    /**
     * Print a log string iff the corresponding log type is enabled.
     **/
    public static void d(int kind, String exp) {
	if ((logMask & kind) != 0)
	    d(exp);
    }

    /**
     * Convenience method for converting ints to strings.  Doing the
     * conversion and calling a multi-argument <code>d</code> method
     * is still more efficient than building a string with
     * <code>+</code> that never gets used.
     **/
    public static String I(int i) {
	return Integer.toString(i);
    }
    /**
     * If the logging type kind is enabled, print the string that
     * results from concatenating the remaining arguments.
     **/
    public static void d(int kind, Object o1, Object o2) {
	if ((logMask & kind) != 0)
	    d(o1.toString() + o2);
    }
    /**
     * If the logging type kind is enabled, print the string that
     * results from concatenating the remaining arguments.
     **/
    public static void d(int kind, Object o1, Object o2, Object o3) {
	if ((logMask & kind) != 0)
	    d(o1.toString() + o2 + o3);
    }

    public static void d(int kind, Object o1, Object o2, Object o3,
			 Object o4) {
	if ((logMask & kind) != 0)
	    d(o1.toString() + o2 + o3 + o4);
    }

    public static void d(int kind, Object o1, Object o2, Object o3,
			 Object o4, Object o5) {
	if ((logMask & kind) != 0)
	    d(o1.toString() + o2 + o3 + o4 + o5);
    }

    public static void d(int kind, Object o1, Object o2, Object o3,
			 Object o4, Object o5, Object o6) {
	if ((logMask & kind) != 0)
	    d(o1.toString() + o2 + o3 + o4 + o5 + o6);
    }

    /**
     * Parse a command line's -log option.  The option is of the form
     * <code>-log=&lt;log-opt&gt;{&lt;sep&gt;&lt;log-opt&gt;}*</code>,
     * where:
     * <dl>
     * <dt><code>&lt;sep&gt;</code></dt>
     *     <dd>is a sequence of 1 or more whitespace or comma
     *         chars</dd>
     * <dt><code>&lt;log-opt&gt;</code></dt>
     *     <dd>is the command line form of one of the LOG_
     *         constants defined in <code>BootBase</code></dd>
     * </dl>
     *
     * We obtain the set of desired log options reflectively as
     * follows.  Each command-line option is converted to upper case,
     * hyphens are replaced by underscores, and the resulting string
     * is prefixed with <code>"LOG_"</code>.<p>
     *
     * For example,
     * <code>parseLogOptions("missing-blueprint,native");<code>
     * is equivalent to
     * <code>logMask = LOG_INCOMPLETE_TYPES|LOG_NATIVE;</code><p>
     *
     * The bootimage driver calls this method with the
     * <code>-log</code> option from the command line.<p>
     **/
    static void parseLogOptions(String _opts) throws BCbootTime {
	if (_opts == null)
	    return;
	if (_opts.equals("list")) {
	    System.out.print(log_list);
	    System.exit(0);
	}
	logMask = 0;
	StringTokenizer opts = new StringTokenizer(_opts, ", \t\n\r\f");
	while (opts.hasMoreTokens()) {
	    String opt = opts.nextToken();
	    opt = "LOG_" + opt.toUpperCase().replace('-', '_');
	    try {
		Field f = BootBase.class.getField(opt);
		if ((f.getModifiers() & Modifier.STATIC) == 0)
		    throw new NoSuchFieldException("not static");
		logMask |= f.getInt(null);
	    } catch (NoSuchFieldException e) {
		System.err.print(log_list);
		throw new MisconfiguredException("unknown log option " + opt);
	    } catch (IllegalAccessException e) {
		throw new OVMRuntimeException(e);
	    }
	}
    }
}
