package s3.services.simplejit;

import java.io.PrintStream;

public class SimpleJITLogger {

    static final boolean logging = true;

    StringBuffer log;
    String who;
    int logsize;
    boolean limitedSize;
    boolean directOutput;
    PrintStream ps;

    public SimpleJITLogger(String who, int logsize) {
	this.logsize = logsize;
	this.limitedSize = true;
	this.directOutput = false;
	this.who = who;
	this.log = new StringBuffer(logsize);
    }

    public SimpleJITLogger(String who) {
	this.who = who;
	this.limitedSize = false;
	this.directOutput = false;
	this.log = new StringBuffer();
    }

    public SimpleJITLogger(String who, PrintStream ps) {
	this.who = who;
	this.directOutput = true;
	this.ps = ps;
    }

    public String getLog() {
	return log.toString();
    }
    public void addLog(String l) {
	if (! logging)
	    return;
	String message = "[" + who + "] " + l +"\n";
	if (directOutput) {
	    ps.print(message);
	} else {
	    if (limitedSize) {
		int size = message.length();
		int current_size = log.length();
		if (size + current_size >= logsize)
		    log.delete(0, size);
	    }
	    log.append(message);
	}
    }
    public void printToStdErr() {
	if (log != null)
	    System.err.println(log.toString());
    }
}

