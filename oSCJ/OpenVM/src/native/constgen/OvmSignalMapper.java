/*
 * $Header
 */
import java.io.*;
/**
 * Generates the following files that relate to signal management:
 * <ul>
 * <li>??.h for declarations</li>
 * <li>??.c for various signal related constants and in particular
 * the mappping from system signal numbers to their associated OVM constant,
 * and vice-versa.</li>
 * <li>??.java which defines Java constants that map to real signal
 * values via the signalmapper.c data structures. </li>
 * </ul>
 * <p>Usage:
 * <tt><pre>
 *     java OvmSignalMapper [options]
 * where options are:
 *     -cp path for C files (.h file is placed in path/include)
 *     -cn name for C files ( will generate name.c and name.h)
 *     -jp path for Java file
 *     -jn name for Java file and class
 *     -pkg package name for Java class
 *
 *     If -cn is not given then no C files are generated
 *     If -cp is not given then path is .
 *     If -jn is not given then no Java files are generated
 *     If -jp is not given then path is .
 *     If -pkg is not given then no package statement is produced
 * </pre>
 * </tt>
 *
 * @author David Holmes
 */
public class OvmSignalMapper {

    /** 
     * List of all the signals that OVM "supports". This list is based
     * on the signals that the RTSJ requires (which includes POSIX signals
     * plus some Solaris specific ones), and additional signals for the 
     * platforms we run on. Realtime signals need special handling.
     */
    static final String[] sigNames = {
        //  RTSJ signals first
        "SIGIOT", // on some systems SIGIOT == SIGABRT but SIGABRT is the
                  // standard POSIX/ANSI-C signal. So we specify SIGIOT
                  // first so that SIGABRT will override it if necessary
        "SIGABRT",
        "SIGALRM",
        "SIGBUS",
        "SIGCANCEL",
        "SIGCHLD",
        "SIGCLD",
        "SIGCONT",
        "SIGEMT",
        "SIGFPE",
        "SIGFREEZE",
        "SIGHUP",
        "SIGILL",
        "SIGINT",
        "SIGIO",
        "SIGKILL",
        "SIGLOST",
        "SIGLWP",
        "SIGPIPE",
        "SIGPOLL",
        "SIGPROF",
        "SIGPWR",
        "SIGQUIT",
        "SIGSEGV",
        "SIGSTOP",
        "SIGTERM",
        "SIGTHAW",
        "SIGTRAP",
        "SIGTSTP",
        "SIGTTIN",
        "SIGTTOU",
        "SIGURG",
        "SIGUSR1",
        "SIGUSR2",
        "SIGVTALRM",
        "SIGWAITING",
        "SIGWINCH",
        "SIGXCPU",
        "SIGXFSZ",

        // Additional signals
        "SIGINFO",
        "SIGSYS",

    };


    static String C_FILE;
    static String H_FILE;
    static String J_NAME;


    public static void main(String[] args) throws IOException {
        String C_PATH = ".";
        String H_PATH= ".";
        String J_PATH = ".";
        String PKG = null;

        boolean doC = false;
        boolean doJ = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-cn")) {
                if (i+1 < args.length) {
                    if (args[i+1].charAt(0) != '-') {
                        C_FILE = args[i+1] + ".c";
                        H_FILE = args[i+1] + ".h";
                        doC = true;
                        i++; // skip arg we just stole
                    }
                    // hmmm - missing value so try and parse the rest
                }
                // missing value so ignore it
            }
            else if (args[i].equals("-cp")) {
                if (i+1 < args.length) {
                    if (args[i+1].charAt(0) != '-') {
                        C_PATH = args[i+1];
                        i++; // skip arg we just stole
                    }
                    // hmmm - missing value so try and parse the rest
                }
                // missing value so ignore it
            }
            else if (args[i].equals("-jn")) {
                if (i+1 < args.length) {
                    if (args[i+1].charAt(0) != '-') {
                        J_NAME = args[i+1];
                        doJ = true;
                        i++; // skip arg we just stole
                    }
                    // hmmm - missing value so try and parse the rest
                }
                // missing value so ignore it
            }
            else if (args[i].equals("-jp")) {
                if (i+1 < args.length) {
                    if (args[i+1].charAt(0) != '-') {
                        J_PATH = args[i+1];
                        i++; // skip arg we just stole
                    }
                    // hmmm - missing value so try and parse the rest
                }
                // missing value so ignore it
            }
            else if (args[i].equals("-pkg")) {
                if (i+1 < args.length) {
                    if (args[i+1].charAt(0) != '-') {
                         PKG = args[i+1];
                         i++; // skip arg we just stole
                    }
                    // hmmm - missing value so try and parse the rest
                }
                // missing value so ignore it
            }
            else {
                System.err.println("Unexpected arg " + args[i]);
                System.exit(1);
            }
        }


        if (doC) {
            H_PATH = C_PATH + File.separatorChar + "include";

            // open the C header file we will write
            PrintWriter h = new PrintWriter(new FileWriter(H_PATH + File.separatorChar + H_FILE));
            try {
                writeHeaderProlog(h);
                writeStruct(h);
                writeExterns(h);
                writeEnum(h);
                writeHeaderEpilog(h);
            }
            finally {
                h.flush();
                h.close();
            }

            // open the C file we will write
            PrintWriter c = new PrintWriter(new FileWriter(C_PATH + File.separatorChar + C_FILE));
            try {
                writeCProlog(c);
                writeSignalInit(c);
                writeOVMSignalMap(c);
                writeSignalRelease(c);
                writeCEpilog(c);
            }
            finally {
                c.flush();
                c.close();
            }
        }

        if (doJ) {

            // open the Java file that will contain the constants
            PrintWriter j = new PrintWriter(new FileWriter(J_PATH + File.separatorChar + J_NAME + ".java"));
            try {
                writeJavaProlog(j, PKG);
                writeJavaConstants(j);
                writeJavaNames(j);
                writeJavaEpilog(j);
            }
            finally {
                j.flush();
                j.close();
            }
        }
    }
    

    static void writeHeaderProlog(PrintWriter f) {
        String sym = "_" + H_FILE.toUpperCase().replace('.', '_');
        f.println("/* AUTOGENERATED FILE - DO NOT EDIT */\n" +
                  "#ifndef " + sym + "\n" +
                  "#define " + sym + "\n" +
                  "\n#include <signal.h>\n" +
                  "#include <limits.h>\n"
                  );
    }

    static void writeStruct(PrintWriter f) {
        f.println("struct ovmSigInfo {\n" +
                  "\tint ovmSigNum;\n" +
                  "\tconst char * sigName;\n" +
                  "};\n" +
                  "typedef struct ovmSigInfo ovmSigInfo_t;\n"
                  );
    }


    static void writeHeaderEpilog(PrintWriter f) {
        f.println("#endif\n");
    }

    static void writeExterns(PrintWriter f) {
        f.println("extern int signalCount;\n" +
                  "extern int rtSignalCount;\n" +
                  "extern int maxSignalValue;\n" +
                  "extern int minSignalValue;\n" +
                  "extern int sigRTMin;\n" +
                  "extern int sigRTMax;\n" +
                  "extern int ovmSignalCount;\n" +
                  "extern int ovmSig2cSig[];\n" +
                  "extern ovmSigInfo_t* cSig2ovmSig; \n" +
                  "extern int signalMapSize;\n" +
                  "extern int initSignalMaps();\n" +
                  "extern void releaseSignalMaps();\n"
                  );
    }

    static void writeEnum(PrintWriter f) {
        f.println("/* enumeration of all the signals we know about.\n" +
                  "   The values in the enumeration match those defined at\n" +
                  "   the Java level. These are in no particular order.\n" +
                  "*/\n" +
                  "enum ovmSignals {"
                  );
        for (int i = 0; i < sigNames.length; i++) {
            f.println("\tOVM_" + sigNames[i] + 
                      " = " + i + ",");
        }
        f.println("};");
    }

    static void writeCProlog(PrintWriter f) {
        f.println("/* AUTOGENERATED FILE - DO NOT EDIT */\n" +
                  "#include \"" + H_FILE + "\"\n" +
                  "#include <stdlib.h>\n");
    }

    static void writeCEpilog(PrintWriter f) {
        f.println();
    }

    static void writeSignalRelease(PrintWriter f) {
        f.println("/* Free the signal map */\n" +
                  "void releaseSignalMap() {\n" +
                  "\tfree(cSig2ovmSig);\n" +
                  "\tcSig2ovmSig = 0;\n" +
                  "}");
    }

    static void writeSignalInit(PrintWriter f) {
        f.println("/* The number of non-realtime signals in this system */\n" +
                  "int signalCount;\n" +
                  "/* The number of realtime signals in this system */\n" +
                  "int rtSignalCount = 0;\n" +
                  "/* The maximum numeric value of a defined signal */\n" +
                  "int maxSignalValue = 0;\n" +
                  "/* The minimum numeric value of a defined signal */\n" +
                  "int minSignalValue = INT_MAX;\n" +
                  "/* The minimum real-time signal available */\n" +
                  "int sigRTMin = 0;\n" +
                  "/* The maximum real-time signal available */\n" +
                  "int sigRTMax = 0;\n" +
                  "/* The size of the signal map. Use this to create other\n" +
                  "   arrays that can be indexed using the signals value\n" +
                  "*/\n" +
                  "int signalMapSize = 0;\n" +
                  "/* The lookup table from system signals to ovm signals */\n" +
                  "ovmSigInfo_t* cSig2ovmSig = 0;\n" +

                  "\n" +
                  "/* Initialise the system signal to ovm signal lookup table\n"+
                  "   by creating an array that can be indexed by any signal\n" +
                  "   value, and filling in each array entry with its\n" +
                  "   OVMSignal counterpart. Any unused slots are filled\n" +
                  "   with -1.\n" +
                  "   @returns zero on success and -1 if the malloc fails\n" +
                  "*/\n" +
                  "int initSignalMaps() {\n" +
                  "\tint i =0;\n" 
                  );

        for (int i = 0; i < sigNames.length; i++) {
            f.println("#ifdef " + sigNames[i] + "\n" + 
                      "\tsignalCount++;\n" + 
                      "\tif (maxSignalValue < " + sigNames[i] + ")\n" +
                      "\t\tmaxSignalValue = " + sigNames[i] + ";\n" +
                      "\tif (minSignalValue > " + sigNames[i] + ")\n" +
                      "\t\tminSignalValue = " + sigNames[i] + ";\n" +
                      "#endif");
        }
        f.println("#if defined(SIGRTMIN) && defined(SIGRTMAX)\n" +
                  "\tsigRTMin = SIGRTMIN;\n" +
                  "\tsigRTMax = SIGRTMAX;\n" +
                  "\trtSignalCount = (SIGRTMAX-SIGRTMIN+1);\n" +
                  "#endif\n");

        f.println("/* We dynamically allocate the lookup table from system\n" +
                  "   signals to OVM signal values, using the larger of \n" +
                  "   the number of signals and the maximum signal value,\n" +
                  "   plus the number of real-time signals.\n" +
                  "   This should work well as long as signal values are \n" +
                  "   the expected small integers.\n" +
                  "*/\n\n" +
                  "\tif (rtSignalCount > 0)\n" +
                  "\t\tsignalMapSize = sigRTMax + 1;\n" +
                  "\telse\n" + 
                  "\t\tsignalMapSize = maxSignalValue + 1;\n" +
                  "\n" +
                  "\tcSig2ovmSig = malloc(signalMapSize * sizeof(ovmSigInfo_t));\n" +
                  "\tif (cSig2ovmSig == 0) return -1;\n" +
                  "\n" +
                  "\tfor( i = 0; i < signalMapSize; i++) {\n" +
                  "\t\tcSig2ovmSig[i].ovmSigNum = -1;\n" +
                  "\t\tcSig2ovmSig[i].sigName = \"<unknown>\";\n" +
                  "\t}\n\n" +
                  "/* now overwrite each real entry with the right value */\n"
                  );

        for (int i = 0; i < sigNames.length; i++) {
            f.println("#ifdef " + sigNames[i] + "\n" + 
                      "\tcSig2ovmSig[" + sigNames[i] + "].ovmSigNum = OVM_" + 
                      sigNames[i] + ";\n" +
                      "\tcSig2ovmSig[" + sigNames[i] + "].sigName = \"" + 
                      sigNames[i] + "\";\n" +
                      "#endif");
        }
        
        f.println("/* now add some extra info for real-time signals */\n" +
                  "\tfor (i = sigRTMin; i <= sigRTMax; i++) {\n" +
                  "\t\tcSig2ovmSig[i].sigName = \"RT-SIGNAL\";\n" +
                  "\t}\n");

        f.println("return 0;\n}");
    }

        
    static void writeOVMSignalMap(PrintWriter f) {
        f.println("/* The number of signals OVM knows about */\n" +
                  "int ovmSignalCount = " + sigNames.length + ";\n" +
                  "\n" +
                  "/* Map from OVM signal values to system signal values\n" +
                  "   Any undefined signals are set at -1.\n" +
                  "*/\n" +
                  "int ovmSig2cSig[] = {"
                  );
        for (int i = 0; i < sigNames.length; i++) {
            f.println("#ifdef " + sigNames[i] + "\n" + 
                      "\t" + sigNames[i] + ",\n" + 
                      "#else \n" +
                      "\t-1,\n" +
                      "#endif");
        }
        f.println("};");
    }


    static void writeJavaProlog(PrintWriter f, String pkg) {
        f.println("/* AUTOGENERATED FILE - DO NOT EDIT */\n");
        if (pkg != null && pkg != "") 
            f.println("package " + pkg + ";\n\n");
        f.println("/** Defines symbolic constants for all of the signals that the OVM knows about */\n" +
                  "public final class " + J_NAME + " {\n");

    }

    static void writeJavaEpilog(PrintWriter f) {
        f.println("}");
    }

    static void writeJavaConstants(PrintWriter f) {
        for (int i = 0; i < sigNames.length; i++) {
            f.println("\tpublic static final int OVM_" + sigNames[i] + 
                      " = " + i + ";");
        }
        f.println("\n\t/** The number of signals defined in the system */\n" +
                  "\tpublic static final int NSIGNALS = " + sigNames.length + ";");
    }

    static void writeJavaNames(PrintWriter f) {
        f.println("\n\npublic static final String[] sigNames = new String[] {");
        for (int i = 0; i < sigNames.length; i++) {
            f.println("\t\"" + sigNames[i] + "\",");
        }

        f.println("};");
    }

}
