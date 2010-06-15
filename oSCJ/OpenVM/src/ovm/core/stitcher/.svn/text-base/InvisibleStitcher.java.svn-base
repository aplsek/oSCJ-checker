package ovm.core.stitcher;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.util.Properties;

import ovm.core.OVMBase;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.Descriptor;
import ovm.core.repository.TypeName;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.memory.VM_Address;
import ovm.services.bytecode.Instruction;
import ovm.services.io.Resource;
import ovm.services.io.ResourcePath;
import ovm.util.ArrayList;
import ovm.util.Arrays;
import ovm.util.CommandLine;
import ovm.util.HashMap;
import ovm.util.Iterator;
import ovm.util.List;
import ovm.util.Map;
import ovm.util.OVMException;
import ovm.util.OVMRuntimeException;
import ovm.util.OVMException.IO;
import s3.services.bootimage.Ephemeral;
import s3.util.PragmaTransformCallsiteIR;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;
import s3.util.PragmaTransformCallsiteIR.BCdead;

/** Statically-bound stitching as described in OVMStitcherIdeas on the wiki.
 *  @author Chapman Flack
 **/
public class InvisibleStitcher extends OVMBase /*implements Ephemeral.Void*/ {
    /**
     * If true, allow the rewrite machinery to continue from
     * MisconfiguredExceptions: The machinery will print the
     * exception's stack trace, and rewrite the call that triggered
     * the exception with a panic.  Otherwise, treat
     * MisconfigurationExceptions uncovered during bytecode rewriting
     * as fatal errors, and report them to the user.<p>
     *
     * This flag defaults to false, but can be overridden with the
     * -debug command line option.
     **/
    private static boolean debugRewriteFailures = true;

    /**
     * If true, print a warning message with stack trace whenever
     * {@link #singletonFor} allocates a singleton inside the
     * constructor of another singleton.  This kind of recursion is
     * bad, since the second object may turn around and ask for the
     * singleton for of thefirst one's type, leading to a
     * {@code RuntimeException}.  However, recursion within
     * constructors can be avoided by defineing {@link
     * Component#initialize} methods.
     **/
    private static final boolean WARN_ON_CTOR_RECURSION = false;

    /** Used to detect constructor recursion.  Not thread safe. **/
    private static String inSingletonCtor = null;
    
    /**
     * If true, print a message whenever {@link #singletonFor} loads
     * and instaniates a new implementation class.  Nesting is shown
     * by leading spaces
     **/
    private static final boolean TRACE_SINGLETON_FOR = false;

    /** Leading spaces in singleton creation traces.  Not thread safe. **/
    private static String traceSingletonPrefix = "";

    /**
     * Untile {@link #bootstrapComplete} this is an array of all
     * singletons that have been created.  After bootstrapping, this
     * is {@code null}.
     **/
    private static ArrayList bootComponents = new ArrayList();
    
    /** Create an InvisibleStitcher recording the given constructor
     *  and arguments. There is no other reason to instantiate an
     *  InvisibleStitcher (its methods are static), so needing some
     *  class to bind constructors to arguments in the mapping,
     *  InvisibleStitcher is it.
     *  @throws BCdead <em>this is a pragma</em>
     **/
    private InvisibleStitcher(Class ifc, Constructor ctor,
			      String[] args) throws BCdead {
	this.ifc = ifc;
        this.ctor = ctor;
        this.args = args;
    }
    private Class ifc;
    private Constructor ctor;
    private String[] args;

    private MisconfiguredException cantLink(Exception e) throws BCbootTime {
	return new MisconfiguredException("cannot link " + ctor.getDeclaringClass().getName()
	     + " as " + ifc.getName() + ": " + e.getMessage(), e);
    }

    /** Return a new instance created with this constructor and argument list.
     *  @throws BCdead <em>this is a pragma</em>
     **/
    private Object newInstance() throws BCdead {
        try {
            return ctor.newInstance(args);
        } catch (IllegalAccessException e) {
            throw cantLink(e);
        } catch (IllegalArgumentException e) {
            throw cantLink(e);
        } catch (InstantiationException e) {
	    throw cantLink(e);
        } catch (InvocationTargetException e) {
	    Throwable cause = e.getCause();
	    throw (cause instanceof RuntimeException
		   ? (RuntimeException) cause
		   : new RuntimeException("linking " + ifc.getName()
					  + ": " + cause.getMessage(),
					  cause));
        }
    }
    /** Return the class name associated with the constructor.
     *  {@link #implementations()} depends on what this method returns.
     *  @throws BCdead <em>this is a pragma</em>
     **/
    public String toString() throws BCdead {
        return ctor.getDeclaringClass().getName();
    }

    /**
     * After all files have been processed, the CommandLine contains
     * the value of every non-linking property.  We need this to do
     * file substitutions.
     **/
    private static CommandLine cl;

    /** Map from interface/abstract-class name (string) to concrete
     *  implementing class name (InvisibleStitcher instance).  An
     *  InvisibleStitcher instance is essentially a linking property
     *  with the constructor resolved and all arguments evaluated.  I
     *  don't think we should actuall evaluate arguments until we've
     *  finished reading files, but we are a long way from that point.
     **/
    private static Map impls;
    /** Map from interface/abstract-class name (string) to instantiated
     *  singleton of the implementing class
     **/
    private static Map singletons = new HashMap();

    /** A record of the top-level config file's name (null if there was none).
     **/
    private static String topConfig;

    private static ResourcePath configPath;

    public static void printUsage(final PrintStream os) throws BCdead {
	os.println("The following options are supported");
	try {
	    configPath.forAll("", new Resource.Action() {
		    public void process(Resource rsc) throws BCbootTime {
			try {
			    Reader _r
				= new InputStreamReader(rsc.getContentsAsStream());
			    LineNumberReader r = new LineNumberReader(_r);
			    for (String str = r.readLine();
				 str != null;
				 str = r.readLine()) {
				if (str.startsWith("## "))
				    os.println(str.substring(3));
				else if (str.startsWith("##"))
				    os.println(str.substring(2));
			    }
			} catch (IOException _) { }
		    }
		});
	} catch (OVMException e) {
	    throw new RuntimeException(e);
	}
    }

    static private final String identTerm = ",\" \t\n";

    public static void addOption(String name,
				 String value) {
	if (cl.getOption(name) != null)
	    throw new RuntimeException("addOption shadows existing");
	cl.setOption(name, value);
    }

    public static boolean getBoolean(String name) {
	return cl.getBoolean(name);
    }

    public static int getInt(String name) {
	return cl.getInt(name);
    }

    public static String getString(String name) {
	try {
	    return cl.getLongString(name);
	} catch (IOException e) {
	    throw new MisconfiguredException(e.getMessage() +
					   ": while expanding " + name, e);
	}
    }

    /**
     * Perform option-substitution on a file template.  We read the
     * contents of sourceFile, and write it to destFile verbatim,
     * excepted that strings of the form <code>${<i>OPTION</i>}</code>
     * are replaced with the value of OPTION.
     * <p>
     * FIXME: there is no way to include the string "${" in the output file.
     *
     * @param sourceFile input file name
     * @param destFile   output file name
     *
     * @exception IOException if an IO error occurs, or an invalid
     * option reference is found.
     *
     */
    public static void expandOptions(String sourceFile,
				     String destFile)
	throws IOException, BCdead
    {
	Reader r = new FileReader(sourceFile);
	Writer w = new FileWriter(destFile);
	StringBuffer id = new StringBuffer();
	while (true) {
	    int c = r.read();
	    if (c == -1)
		break;
	    else if (c == '$') {
		int c2 = r.read();
		if (c2 == -1) {
		    w.write(c);
		    break;
		} else if (c2 != '{') {
		    w.write(c);
		    w.write(c2);
		} else {
		    id.setLength(0);
		    while (true) {
			c = r.read();
			if (c == -1 || identTerm.indexOf(c) != -1)
			    throw new IOException("In template " +
						  sourceFile +
						  ": premature end of option " +
						  id.toString());
			else if (c == '}')
			    break;
			else
			    id.append((char) c);
		    }
		    String val = cl.getLongString(id.toString());
		    if (val != null)
			w.write(val);
		}
	    } else
		w.write((char) c);
	}
	r.close();
	w.close();
    }

    /** {@link s3.services.bootimage.Driver Driver} should call this method
     *  ASAP after parsing command line.
     *  @param cml the command line
     *  @throws BCdead <em>this is a pragma</em>
     **/
    public static void init(CommandLine cml) throws BCdead {
        if (null != impls)
            OVMBase.fail("multiple InvisibleStitcher init");
	InvisibleStitcher.cl = cml;
	String _configPath = System.getProperty("ovm.stitcher.path", "config");
	configPath = new ResourcePath(_configPath);
	topConfig = System.getProperty("ovm.stitcher.file", "stitchery");
	impls = new HashMap();
	Map included = new HashMap();
	loadFile(topConfig, included, cml, cml.copyOptions());
	if (cml.getBoolean("help") || cml.getBoolean("?")) {
	    printUsage(System.out);
	    System.exit(1);
	}
	debugRewriteFailures = cml.getBoolean("debug");
    }

    private static void loadFile(String configFile, Map loaded,
	    	CommandLine cml, CommandLine explicit) throws BCdead {
        // use 'urls' to track what urls are being included, to: a) avoid redundant
        // includes, and b) throw exceptions for cyclic includes.  First enter a new
        // URL in the map, mapped to itself.  If encountered again in that
        // condition, a cycle has been detected; except.  Otherwise, when done
        // loading that URL, map it to any *other* non-null reference (say, "").
        // If encountered again in *that* condition, it's just a redundant include;
        // ignore. On an exception, leave the URL mapped to itself: means incomplete. 
        Object o = loaded.get(configFile);
        if (o == configFile)
            throw new CyclicIncludeException(configFile);
        else if (null != o)
            return;
        loaded.put(configFile, configFile);
        try {
            loadFile_(configFile, loaded, cml, explicit);
        } catch (java.io.FileNotFoundException e) {
	    throw new MisconfiguredException("config file " + configFile + " not found", e);
        } catch (java.io.IOException e) {
	    throw new MisconfiguredException("error reading config file " + configFile +
		 ": " + e.getMessage(), e);
        } catch (OVMException.IO e) {
	    throw new MisconfiguredException("error reading config file " + configFile +
		 ": " + e.getMessage(),e);
        } catch (MisconfiguredException e) {
            throw new MisconfiguredException("in file " + configFile + ": " + e.getMessage(), e);
        }

        loaded.put(configFile, Boolean.TRUE);
    }

    // the heavy lifting gets done here
    private static void loadFile_(String configFile, Map loaded, CommandLine cml, CommandLine explicit) 
      throws IO, IOException, BCbootTime {
        Resource r = configPath.getResource(configFile, "");
        if (r == null)
            throw new MisconfiguredException("config file " + configFile + " not found");
        InputStream fis = r.getContentsAsStream();
        Properties p = new Properties();
        p.load(fis);
        fis.close();

        // first pass through all entries just to process (and remove)
        // entries that are command-line option defaults.  comma-separated
        // values are alternatives, of which the first non-null will be used.
        //
        // The final option value should be either the value
        // explicitly provided on the command line, or the last
        // default provided, just as property definitions in later
        // files replace those in earlier ones
        for (java.util.Iterator i = p.entrySet().iterator(); i.hasNext();) {
            java.util.Map.Entry me = (java.util.Map.Entry) i.next();
            String key = (String) me.getKey();
            if (!key.startsWith("-"))
                continue;
	    // Note, some versions of IBM's jdk will trash the entry
	    // after a call to remove().
	    String _values = (String) me.getValue();
            i.remove();
            key = key.substring(1);
            if (null == explicit.getOption(key)) {
                String[] values = elements(_values, cml);
                for (int j = 0; j < values.length; ++j)
                    if (null != values[j]) {
                        cml.setOption(key, values[j]);
                        break;
                    }
		if (cml.getOption(key) == null) {
		    pln("no value for -" + key + " in " + me.getValue());
		}
            }
        }

        // next check for a (single) %include property and, if found, process
        // its string elements in textual order.
        String includeExpr = p.getProperty("%include");
        if (null != includeExpr) {
            p.remove("%include");
            String[] includeList = elements(includeExpr, cml);
            for (int i = 0; i < includeList.length; ++i) {
                loadFile(includeList[i], loaded, cml, explicit);
            }
        }
        // now to copy (what's left of) the Properties hashmap into an
        // ovm.util.HashMap to avoid dragging the java.util version into the
        // image. Cannot use the easy copy constructor because neither map type
        // extends the other; must iterate. That's ok as there's per-entry
        // processing anyway.
        for (java.util.Iterator i = p.entrySet().iterator(); i.hasNext(); ) {
            java.util.Map.Entry me = (java.util.Map.Entry) i.next();
            String key = (String) me.getKey();
            impls.put(key, target(key, (String) me.getValue(), cml));
        }
        
    }
    public static void init() throws BCdead {
        init(new CommandLine());
    }

    /** Returns an Iterator over the concrete implementing class names in the
     *  configuration file. {@link s3.services.bootimage.Driver Driver} can
     *  add these classes to the blueprint closure (I would rather add them
     *  only as needed, but that's harder the way things work now, as some
     *  stitcher mapping might not be needed until bytecode rewriting, and
     *  the blueprint closure is frozen by then).
     *  @return Iterator over the concrete implementing class names
     *  @throws BCdead <em>this is a pragma</em>
     **/
    public static Iterator implementations() throws BCdead {
        if (null == impls)
            OVMBase.fail("InvisibleSticher use before init");
        final Iterator vi = impls.values().iterator();
        return new Iterator() {
            public boolean hasNext() {
                return vi.hasNext();
            }
            public void remove() {
                vi.remove();
            }
            public Object next() {
                return ((InvisibleStitcher) vi.next()).toString();
            }
        };
    }

    /** The method to use at hosted (image build) time to obtain the singleton
     *  of the concrete class configured to implement a named interface or
     *  abstract class. See
     *  {@link ovm.core.domain.ObjectModel#getObjectModel()
     *  getObjectModel()} for a usage example.
     *  Sync note: currently accesses singleton map without synchronization.
     *  @param ifcName name of an interface or (usually abstract) class
     *  @return singleton of concrete class specified in the current
     *  configuration for the named interface or class
     *  @throws NotConfiguredException if the configuration file gives no
     *  mapping for the named interface or class
     *  @throws MisconfiguredException if the configured class does not exist,
     *  does not implement/extend the named interface or class, or cannot be
     *  instantiated with a no-argument constructor.
     *  @throws BCdead <em>this is a pragma.</em> Call sites of this method
     *  should be eliminated in rewriting; see {@link PragmaStitchSingleton}.
     **/
    public static Object singletonFor(String ifcName) throws BCdead {
        Object implInstance = singletons.get(ifcName);
	if (implInstance == Boolean.FALSE)
	    throw new RuntimeException("cyclic dependency detected between "+
				       "InvisibleStitcher singletons");
        if (implInstance != null)
            return implInstance;

	if (WARN_ON_CTOR_RECURSION && inSingletonCtor != null) {
	    System.err.println("creating implementation of " + ifcName +
			       " from constructor for " + inSingletonCtor +
			       " would InvisibleStitcher.Component.initialize()"
			       + " help?");
	    new Error().printStackTrace(System.err);
	}
        if (null == impls)
            OVMBase.fail("InvisibleSticher use before init");
	singletons.put(ifcName, Boolean.FALSE);
        InvisibleStitcher is = (InvisibleStitcher) impls.get(ifcName);
        if (null == is)
            throw new NotConfiguredException("no implemenation defined for "
					     + ifcName);
	String pfx = traceSingletonPrefix;
	String wasInSingletonCtor = inSingletonCtor;
	try {
	    if (TRACE_SINGLETON_FOR) {
		System.out.println(pfx + "instantiating " + ifcName);
		traceSingletonPrefix += "  ";
	    }
	    inSingletonCtor = ifcName;
	    implInstance = is.newInstance();
	    singletons.put(ifcName, implInstance);
	} finally {
	    inSingletonCtor = wasInSingletonCtor;
	    traceSingletonPrefix = pfx;
	}
	if (bootComponents != null) {
	    if (!(implInstance instanceof CoreComponent))
		throw new MisconfiguredException(ifcName + " was loaded "+
						 "during bootstrapping but " +
						 "does not implement" +
						 "CoreComponent");
	    else
		bootComponents.add(implInstance);
	} else if (implInstance instanceof Component) {
	    ((Component) implInstance).initialize();
	}
        return implInstance;
    }

    /**
     * Mark the end of the application's bootstrap phase.  This method
     * runs the {@link Component#initialize} method of every {@link
     * CoreComponent} loaded during the bootstrap phase, and enables
     * loading of singletons that do not implement {@code
     * CoreComponent}.<p>
     *
     * In Ovm, the bootstrapping phase ends inside the {@link
     * ovm.core.domain.ExecutiveDomain ExecutiveDomain's} bootstrap
     * {@link s3.core.domain.S3Domain#bootstrap method}.  The call to
     * bootstrapComplete signals that basic types like {@link
     * ovm.core.services.memory.VM_Address} and {@link
     * ovm.core.domain.Oop} work as advertised.
     *
     * @see s3.services.bootimage#bootstrap
     **/
    public static void bootstrapComplete() {
	CoreComponent[] bc = new CoreComponent[bootComponents.size()];
	bootComponents.toArray(bc);
	bootComponents = null;
	for (int i = 0; i < bc.length; i++)
	    bc[i].initialize();
    }

    /**
     * equivalent to singletonFor(ifc.getName())
     */
    public static Object singletonFor(Class ifc) throws BCdead {
	return singletonFor(ifc.getName());
    }
    
    /** Consume and return a string delimited by " (of which cb is positioned
     *  at the first, and will be left just beyond the last), returning the
     *  string with delimitinq quotes removed, and with an internal " wherever
     *  it was doubled ("") in the input. No other character escape processing
     *  is done; see
     *  {@link java.util.Properties#load(InputStream) Properties.load} for the
     *  escape processing already done. Note that <code>Properties.load</code>
     *  strips all backslashes (even before characters it does not treat as
     *  escapes) so you must <em>always</em> double a backslash if you want it
     *  to appear in the string.
     *  @param cb CharBuffer positioned at the opening " of a string.
     *  @return the string with delimiting quotes stripped and containing an
     *  internal " wherever it was doubled ("") in the input.
     *  @throws MisconfiguredException on departures from the intended syntax
     *  @throws BCdead <em>this is a pragma</em>
     **/
    private static String qString(CharBuffer cb) throws BCdead {
        StringBuffer rslt = new StringBuffer();

        if ('"' != cb.get())
            throw syntax(cb, -1);

        while (cb.hasRemaining()) {
            char c = cb.get();
            if ('"' == c) {
                if (!cb.hasRemaining() || ('"' != cb.get(cb.position())))
                    return rslt.toString();
                cb.get();
            }
            rslt.append(c);
        }
        throw syntax(cb, 0);
    }

    /** Consume and return an identifier, defined laxly as consecutive
     *  non-whitespace, non-termChar characters, of which cb is positioned at
     *  the first and will be left just beyond the last.
     *  @param cb CharBuffer positioned at the first character of identifier
     *  @param termChars a list of characters that may follow this identifier
     *  @return the identifier
     *  @throws MisconfiguredException if the identifier comes up zero length
     *  @throws BCdead <em>this is a pragma</em>
     **/
    private static String ident(CharBuffer cb, String termChars)
        throws BCdead {
        StringBuffer rslt = new StringBuffer();

        while (cb.hasRemaining()) {
            char c = cb.get();
            if ((-1 != termChars.indexOf(c)) || Character.isWhitespace(c)) {
                cb.position(cb.position() - 1);
                break;
            }
            rslt.append(c);
        }
        if (0 == rslt.length())
            throw syntax(cb, 0);
        return rslt.toString();
    }

    /** Leave cb positioned at the next nonwhite character, or end.
     *  @throws BCdead <em>this is a pragma</em>
     **/
    private static void consumeWhitespace(CharBuffer cb) throws BCdead {
        while (cb.hasRemaining()) {
            if (!Character.isWhitespace(cb.get())) {
                cb.position(cb.position() - 1);
                break;
            }
        }
    }

    /** Consume an argument list (cb must be positioned at the first '(' and
     *  will be left beyond the corresponding ')') and return the arguments
     *  as an array of String. Any argument that is a quoted string literal
     *  (see {@link #qString(CharBuffer) qString} for syntax) is returned as
     *  found; an unquoted argument is treated as an option name and
     *  looked up in the {@link CommandLine} options, and the associated string
     *  (or null if the option was not specified) becomes the argument.
     *  @param cb CharBuffer positioned at the '(' of an argument list
     *  @param cml {@link CommandLine} options
     *  @return array of String (may be zero length)
     *  @throws MisconfiguredException on departure from expected syntax
     *  @throws BCdead <em>this is a pragma</em>
     **/
    private static String[] arguments(CharBuffer cb, CommandLine cml)
        throws BCdead {
        match(cb, '(');

        String[] args = elements(")", cb, cml);

        match(cb, ')');
        consumeWhitespace(cb);
        if (cb.hasRemaining())
            throw syntax(cb, 0);

        return args;
    }

    private static void match(CharBuffer cb, char c) throws BCdead {
        if (!cb.hasRemaining())
            throw syntax(cb, 0);
        if (c != cb.get())
            throw syntax(cb, -1);
    }

    private static String[] elements(String term, CharBuffer cb, CommandLine cml) throws BCdead {
        List args = new ArrayList();
        String myLocalIdentTerm = ',' + term;

        String value = null;
        boolean incomplete = false;

        while (cb.hasRemaining()) {
            char c = cb.get(cb.position());

            if (Character.isWhitespace(c)) {
                consumeWhitespace(cb);
                continue;
            }

            if (-1 != term.indexOf(c))
                break;

            switch (c) {
                case ',' :
                    cb.get();
                    args.add(value);
                    value = null;
                    break;

                case '"' :
                    value = append(value, qString(cb));
                    break;

                default :
                    value = append(value, evaluate(ident(cb, myLocalIdentTerm), cml));
            }

            incomplete = true;
        }

        if (incomplete)
            args.add(value);

        return (String[]) args.toArray(new String[args.size()]);
    }

    private static String[] elements(String s, CommandLine cml) throws BCdead {
	return elements("", CharBuffer.wrap(s), cml);
    }

    /** Returns null iff both arguments null, else treats one null argument as empty.
     **/
    private static String append(String lhs, String rhs) throws BCdead {
        if (null == lhs)
            return rhs;
        if (null == rhs)
            return lhs;
        return lhs + rhs;
    }

    private static String evaluate(String prim, CommandLine cml) throws BCdead {
        if (!prim.startsWith("%")) {
	    try {
		return cml.getLongString(prim);
	    } catch (IOException e) {
		throw new MisconfiguredException("substituting file " +
						 cml.getOption(prim),
						 e);
	    }
	}
        return System.getProperty(prim.substring(1));
    }
    
    private static InvisibleStitcher target(
        String ifcName,
        String tgt,
        CommandLine cml)
        throws BCdead {
        CharBuffer cb = CharBuffer.wrap(tgt);

        try {
            ClassLoader currentLoader =
                InvisibleStitcher.class.getClassLoader();
            Class cls = Class.forName(ident(cb, "("), false, currentLoader);
            Class ifc = Class.forName(ifcName, false, currentLoader);

            if (!ifc.isAssignableFrom(cls))
                throw new MisconfiguredException("invalid configuration of " + ifc + ": "
		     + cls.getName() + " is not a subtype");
            consumeWhitespace(cb);
            String[] args;
            if (!cb.hasRemaining())
                args = new String[0];
            else if ('(' == cb.get(cb.position()))
                args = arguments(cb, cml);
            else
                throw syntax(cb, 0);
            Class[] types = new Class[args.length];
            Arrays.fill(types, args.getClass().getComponentType());
            Constructor ctor = cls.getConstructor(types);
            return new InvisibleStitcher(ifc, ctor, args);
        } catch (ClassNotFoundException cnfe) {
            throw snafu(cb, 0, cnfe);
        } catch (NoSuchMethodException nsme) {
            throw snafu(cb, 0, nsme);
        } catch (SecurityException se) {
            throw snafu(cb, 0, se);
        }
    }

    private static MisconfiguredException syntax(CharBuffer cb, int rpos)
        throws BCdead {
	return snafu(cb, rpos, null);
    }

    private static MisconfiguredException snafu(
        CharBuffer cb,
        int rpos,
        Throwable cause) throws BCbootTime {
        StringBuffer sb = new StringBuffer("Configuration file error: ");
        sb.append(
            ((CharBuffer) cb.duplicate().position(0)).subSequence(
                0,
                cb.position() + rpos));
        sb.append("<?>");
        sb.append(cb.subSequence(0 + rpos, cb.remaining() - rpos));
	if (cause != null) {
	    sb.append(": ");
	    sb.append(cause.getMessage());
	    return new MisconfiguredException(sb.toString(), cause);
	} else {
	    return new MisconfiguredException(sb.toString());
	}
    }

    /** Pragma that can annotate any method of no arguments that returns
     *  a reference type. Every call site of the annotated method will be
     *  replaced by a load-constant of the singleton instance of the configured
     *  concrete class corresponding to the method's return type (typically an
     *  interface or abstract class). See
     *  {@link ovm.core.domain.ObjectModel#getObjectModel()
     *  getObjectModel()} for a usage example.
     **/
    public static class PragmaStitchSingleton extends PragmaTransformCallsiteIR {
	static {
	    register("ovm.core.stitcher.InvisibleStitcher$PragmaStitchSingleton", new Rewriter() {
		// refine error message with call site
		MisconfiguredException withSite(MisconfiguredException e) {
		    TypeName t = siteBP.getType().getUnrefinedName();
		    return new MisconfiguredException("in " + JavaFormat._.format(t) 
			    + ": " + e.getMessage(), e);
		}

		protected boolean rewrite() {
		    Descriptor.Method rdm = targetSel.getDescriptor();
		    if (rdm.getArgumentCount() != 0 || rdm.isReturnValueVoid() || rdm.isReturnValuePrimitive()) 
			throw new PuntException();
		    TypeName returnType = rdm.getType();
		    String ifcName = JavaFormat._.format(returnType);
		    Object singleton;
		    try {
			singleton = singletonFor(ifcName);
		    } catch (MisconfiguredException e) {
			if (debugRewriteFailures) throw new PuntException(e);
			else throw withSite(e);
		    }

		    addPopAll(); // no args, but receiver if nonstatic
		    cursor.addResolvedRefLoadConstant(singleton);
		    Instruction.CHECKCAST cc = followingCheckCast();
		    if (null != cc) {
			TypeName.Compound castTypeName = cc.getResultTypeName(code, cp);
			Type.Context tc = siteBP.getType().getContext();
			Type lhs;
			try {
			    lhs = tc.typeFor(castTypeName);
			} catch (LinkageException e) {
			    throw e.fatal("can't resolve checkcast??");
			}
			Type rhs = VM_Address.fromObject(singleton).asOop().getBlueprint().getType();
			if (rhs.isSubtypeOf(lhs)) {
			    int around = code.getPC() + cc.size(code);
			    cursor.addFiat(castTypeName);
			    cursor.addGoto(cfe.getMarkerAtPC(around));
			} else {
			    TypeName siteName = siteBP.getType().getUnrefinedName();
			    MisconfiguredException e = 
				new MisconfiguredException("invalid configuration of " + ifcName
				    + ": " + singleton.getClass().getName() + " is not a subtype of "
				    + JavaFormat._.format(castTypeName) + " as required by "
				    + JavaFormat._.format(siteName)+" -- detected on call to "+targetSel+" from "+siteInst+" in "+siteBP);
			    if (debugRewriteFailures) throw new PuntException(e);
			    else throw e;
			}
		    }
		    return true; // delete initial invocation
		}
	    });
	}
    }

    /**
     * An invisibly stitched component should implement this interface to
     * break initialization cycles.  When {@link
     * InvisibleStitcher#singletonFor} constructs a {@code Component},
     * it will call the {@link #initialize} method as soon as both
     * <ul>
     *  <li> the stitcher updates its internal tables
     *  <li> the application has signals the end of its bootstrapping
     *       phase with {@link InvisibleStitcher#bootstrapComplete}.
     * </ul>
     * In most cases, this method is called immediately after the
     * component's constructor returns.  However, for {@link
     * CoreComponent CoreComponents} loaded during the bootstrap
     * phase, this call is delayed until the boostrap phase ends.<p>
     *
     * This method is also a good place to verify that this component
     * is being stitched into a valid configuration.  Two tightly
     * related components can check for each other's presenence here,
     * but cannot do so in their constructors, because {@link
     * InvisibleStitcher#singletonFor} cannot return an object until
     * its constructor returns.
     **/
    public static interface Component {
	/**
	 * Callback to complete initialization.
	 * @throws BCdead This method is called at most once and always
	 *                during VM generation.
	 **/
	void initialize() throws BCdead;
    }

    /**
     * An invisibly stitched component must implement this interface
     * if it is loaded during bootstrapping.  This interface serves as
     * a marker that the component can be loaded early, and the
     * {@link #initialize} method serves as a handy callback for
     * activities that must be performed after
     * {@link InvisibleStitcher#bootstrapComplete} is called.
     **/
    public static interface CoreComponent extends Component {
    }

    /** Thrown when no mapping is found for an interface or class **/
    public static class NotConfiguredException extends MisconfiguredException {
        public NotConfiguredException(String s) {
            super(s);
        }
    }
    /** Thrown when the mapping specifies a class that does not exist, does
     *  not implement/extend the desired class/interface, or cannot be
     *  instantiated.
     **/
    public static class MisconfiguredException extends OVMRuntimeException {
        public MisconfiguredException(String s) {
            super(s);
        }
        public MisconfiguredException(String s, Throwable t) {
            super(s, t);
        }
	public MisconfiguredException(Throwable t) {
	    super(t);
	}
    }

    public static class CyclicIncludeException extends MisconfiguredException {
        public CyclicIncludeException(String u) {
            super(u);
        }
    }

    public static void main(String[] _args) {
	CommandLine args = new CommandLine(_args);
	init(new CommandLine(_args));
	int i = 0;
	while (i < args.argumentCount()) {
	    String cmd = args.getArgument(i++);
	    if (cmd.equals("get-string"))
		System.out.println(getString(args.getArgument(i++)));
	    else if (cmd.equals("get-int"))
		System.out.println(getInt(args.getArgument(i++)));
	    else if (cmd.equals("get-boolean"))
		System.out.println(getBoolean(args.getArgument(i++)));
	    else {
		System.err.println("Unrecognized argument " + cmd);
		System.exit(1);
	    }
	}
    }
}
