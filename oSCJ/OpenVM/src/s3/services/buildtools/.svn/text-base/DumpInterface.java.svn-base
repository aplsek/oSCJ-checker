package s3.services.buildtools;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import ovm.util.CommandLine;

public class DumpInterface {
    static void usage(String mesg) {
	System.err.println("DumpInterface: " + mesg);
	System.err.println("usage: DumpInterface OPTION* INTERFACE-NAME");
	System.err.println("options:");
	System.err.println("\t-output=<name>\tredirect output to file");
	System.err.println("\t-prefix=<str>\tprefix for C symbols");
	System.exit(-1);
    }

    public static void main(String[] args) {
	CommandLine cl = new CommandLine(args);

	try {
	    String outName = cl.getOption("output");
	    PrintWriter outWriter = (outName == null
				     ? new PrintWriter(System.out)
				     : new PrintWriter(new FileWriter(outName)));

	    String prefix = cl.getOption("prefix");
	    prefix = (prefix == null ? ""
		      : prefix.endsWith("_") ? prefix
		      : (prefix + "_"));

	    if (cl.argumentCount() != 1)
		usage("no interface specified");
	    
	    String cname = cl.getArgument(0);
	    Class c = Class.forName(cname);
	    outWriter.println("/* THIS IS AN AUTOMATICALLY GENERATED FILE, DO NOT EDIT");
	    outWriter.print(" * created by s3.buildTools.DumpInterface");
	    for (int i = 0; i < args.length; i++)
		outWriter.print(" " + args[i]);
	    outWriter.println("");
	    outWriter.println(" */");
	    outWriter.println("");
	    new DumpInterface(outWriter, prefix, c);
	    outWriter.close();
	} catch (IOException e) {
	    usage(e.getMessage());
	} catch (ClassNotFoundException e) {
	    usage(e.getMessage());
	}
    }

    DumpInterface(PrintWriter w, String prefix, Class c) {
	int mods = Modifier.PUBLIC|Modifier.STATIC|Modifier.FINAL;
	Field[] field = c.getFields();
	for (int i = 0; i < field.length; i++) {
	    if ((field[i].getModifiers() & mods) != mods)
		continue;
	    Class ft = field[i].getType();
	    if (ft != int.class
		&& ft != short.class && ft != char.class
		&& ft != byte.class)
		continue;
	    w.print("#define ");
	    w.print(prefix);
	    w.print(field[i].getName().toUpperCase());
	    w.print(" 0x");
	    try {
		w.println(Integer.toHexString(field[i].getInt(null)));
	    } catch (IllegalAccessException _) {
		throw new RuntimeException("impossible!");
	    }
	}
    }
}
