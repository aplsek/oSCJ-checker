/**
 * @author Jan Vitek
 * @file  ovm/util/CommandLine.java
 **/
package ovm.util;
import java.io.FileReader;
import java.io.IOException;

import s3.util.PragmaTransformCallsiteIR.BCbootTime;
import s3.util.PragmaTransformCallsiteIR.BCdead;


/**
 * Simple command line processing.  This class provides functionality for
 * parsing command line arguments that is coded over and over again in main
 * methods.  The model is that command line arguments have the form:
 * <pre>      option_args* free_form* </pre>
 * where each element in option_args is an option starting with a '-'
 * character and each element in free_form is a string.  Option arguments
 * have the syntax:
 * <pre>     '-'NAME[=VALUE] </pre>
 * where NAME is the option identifier and VALUE is the string argument for
 * that option.<p>
 *
 * An example use of the class is as follows:
 * <pre>
 * static void main(String[] args) {
 *    CommandLine cl = new CommandLine();
 *    cl.parse(args);
 *    if (cl.getOption("verbose") != null) ...
 *    String file = cl.getArgument(0);
 *    String path = cl.getOption("classpath");
 * </pre>
 * @author Jan Vitek
 **/
public class CommandLine extends Options {

    //-----------INSTANCE FIELDS---------------------------

    /**
     * The array of commands
     **/
    private String[] commands_;


    /**
     * The offset of the first argument within the argument array
     **/
    private int offsetOfFirstArgument_;

    /**
     * Create a new CommandLine object.
     * @param args parse these arguments
     **/
    public CommandLine(String[] args) {
        parse(args);
    }

    public CommandLine() {
    }

    //-----------INSTANCE METHODS---------------------------
    /**
     * Parse the command line arguments and extracts options. The current
     * implementation allows the same command line instance to parse
     * several argument lists, the results will be merged.
     * @param s the array of arguments to be parsed
     **/
    public void parse(String[] s) {
        offsetOfFirstArgument_ = 0;
        commands_ = s;

        for (int i = 0; i < commands_.length; i++) {
            String str = commands_[i];
// 	    if (str.equals("--")) {
// 		offsetOfFirstArgument_++;
// 		return;
// 	    }
            if (str.startsWith("-")) {
                int eqPos = str.indexOf("=");
                String opt = null;
                String arg = "";
                if (eqPos > 0) {
                    arg = str.substring(eqPos + 1);
                    opt = str.substring(1, eqPos);
                } else
                    opt = str.substring(1);
                options_.put(opt, arg);
                offsetOfFirstArgument_++;
            } else {
                // We have seen all options. Bail out.
                return;
            }
        }
    }

    /**
     * Return a new string representation of this commandline's
     * current state.  The array includes options that have been added
     * with setOption, but excludes options that have been removed
     * with consumeOption.
     *
     * @return an argument vector
     */
    public String[] unparse() {
	int nPos = commands_.length - offsetOfFirstArgument_;
	boolean needMM =  (nPos > 0 && getArgument(0).startsWith("-"));
	int nOpt = options_.size();
	String[] ret = new String[nOpt + (needMM ? 1 : 0) + nPos];
	int i = 0;
	for (Iterator it = options_.entrySet().iterator();
	     it.hasNext();
	     ) {
	    Map.Entry ent = (Map.Entry) it.next();
	    String k = (String) ent.getKey();
	    String v = (String) ent.getValue();
	    ret[i++] = (v == ""
			?  ('-' + k)
			:  ('-' + k + '=' + v));
	}
	if (needMM)
	    ret[i++] = "--";
	if (nPos > 0)
	    System.arraycopy(commands_, offsetOfFirstArgument_,
			     ret, i, nPos);
	return ret;
    }

    /**
     * Return the String at offset i in the unparsed argument array or null
     * if invalid.
     * @param i the offset in the argument array.
     * @return a string or null.
     **/
    public String getStringAt(int i) {
        if (i < 0 || i >= commands_.length) return null;
        return commands_[i];
    }

    /**
     * Return the <code>i</code>th unparsed argument String. Leading
     * options are not counted.
     * @param i the position
     * @return a string or null.
     **/
    public String getArgument(int i) {
        int p = i + offsetOfFirstArgument_;
        if (p < 0 || p >= commands_.length) return null;
        return commands_[p];
    }

    /**
     * Return the number of non-option arguments.
     * @return the number of non-option arguments
     **/
    public int argumentCount() {
        return commands_.length - offsetOfFirstArgument_;
    }

    /**
     * Returns a new <code>CommandLine</code> with a copy of the options
     * of the current object. Arguments are not copied.
     * @return the new command line
     **/
    public CommandLine copyOptions() {
        CommandLine ret = new CommandLine();
        String[] args = new String[offsetOfFirstArgument_];
        System.arraycopy(commands_, 0, args, 0, offsetOfFirstArgument_);
        ret.parse(args);
        return ret;
    }

    /**
     * Add an argument to this <code>CommandLine</code>.
     * @param newArg the argument to add
     **/
    public void addArgument(String newArg) {
        String[] args = new String[commands_.length + 1];
        System.arraycopy(commands_, 0, args, 0,  commands_.length);
        args[commands_.length] = newArg;
        parse(args);
    }

    /**
     * Add an option to this <code>CommandLine</code>.
     * @param newArg the option to add
     **/
    public void addOption(String newArg) {
        String[] args = new String[commands_.length + 1];
        System.arraycopy(commands_, 0, args, 1,  commands_.length);
        args[0] = "-" + newArg;
        parse(args);
    }

    public boolean getBoolean(String opt) {
	String val = getOption(opt);
	return val == null ?  false : !val.equals("false");
    }

    /**
     * Parse a size specification in the form
     * <em>%lt;integer&gt;&lt;suffix&gt;</em> where <em>&lt;integer&gt;</em> is
     * any decimal, hex, or octal form accepted by
     * {@link Integer#decode(String)} and <em>&lt;suffix&gt;</em> is
     * <strong>k</strong> (kilo, 2<sup>10</sup>),
     * <strong>m</strong> (mega, 2<sup>20</sup)), or
     * <strong>g</strong> (giga, 2<sup>30</sup>). Overflow is not checked;
     * values between 2g and 4g will be returned as negative <code>int</code>
     * values, which will be correct if the integer is later passed to
     * something that will treat it as unsigned.
     * FIXME another plausible place for this method would be InvisibleStitcher
     * as its most likely use will be parsing constructor argument strings.
     * FIXME this should probably return VM_Word, and check for overflow.
     * FIXME BCdead (unusable at runtime) only because we may lack decode-like
     * functionality.
     * @param sizeSpec a size specification string as described
     * @return an integer size
     * @throws NumberFormatException if the string is surprising
     * @throws BCdead (<em>pragma:</em> unusable at runtime)
     * FIXME only because we may lack decode-like functionality.
     **/
    public static int parseSize( String sizeSpec)
      	throws java.lang.NumberFormatException {
      	int scale = 1;
	int length = sizeSpec.length();
	if ( length > 0 ) {
	    length -= 1; // assume suffix
	    switch ( sizeSpec.substring(length).toLowerCase().charAt(0) ) {
	      	case 'g': scale *= 1024;
		case 'm': scale *= 1024;
		case 'k': scale *= 1024;
			  break;
		default:  length += 1; // revisit assumption
	    }
	}
	return scale
	       * Integer.decode( sizeSpec.substring( 0, length)).intValue();
    }
    
    public static int parseAddress(String addrSpec) {
	if (addrSpec.startsWith("0x")) {
	    return Integer.parseInt(addrSpec.substring(2),16);
	} else {
	    return Integer.parseInt(addrSpec);
	}
    }

    public int getInt(String opt) {
	return parseSize(getOption(opt));
    }

    public static String translateLongString(String str) throws IOException, BCbootTime {
	if (str != null && str.startsWith("@")) {
	    StringBuffer buf = new StringBuffer();
	    FileReader r = new FileReader(str.substring(1));
	    for (int c = r.read(); c != -1; c = r.read())
		buf.append((char) c);
	    return buf.toString();
	} else {
	    return str;
	}
    }

    public String getLongString(String opt) throws IOException, BCbootTime {
	return translateLongString(getOption(opt));
    }

    /**
     * Return a copy of the original argument array.
     **/
    public String[] asStringArray() {
	String[] result = new String[commands_.length];
	System.arraycopy(commands_, 0, result, 0, commands_.length);
	return result;
    }
    
    public String toString() {
	StringBuffer buf = new StringBuffer();
	for (int i = 0; i < commands_.length; i++) {
	    buf.append(commands_[i]).append(' ');
	}
	return buf.toString();
    }

} // End of CommandLine

