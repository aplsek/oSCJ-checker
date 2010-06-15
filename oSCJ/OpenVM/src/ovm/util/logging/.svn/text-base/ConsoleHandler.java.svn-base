package ovm.util.logging;
import ovm.core.services.io.BasicIO;
// Are we allowed to used String concatenation in here? --jv 
// If we want to be able to log from essentially anywhere at runtime then we 
// should not perform any allocation or synchronization - this prohibits string
// concatenation on both counts. -- dh
public class ConsoleHandler extends Handler {

    private boolean hideRepeats;
    private int repeatCount;
    private String previousMessage;

    public ConsoleHandler(boolean hideRepeats) {
        this.hideRepeats = hideRepeats;
    }

    public ConsoleHandler() {
        this(true);
    }



    // warning: a glitch in the build process seems to allow us to
    //          retain a reference to a JDK String in previousMessage.
    //          By some fluke at runtime we can perform equals() on it
    //          but trying to print it or append it to another string will
    //          cause a crash of some kind. - DH 21 Nov. 2003
    // Update: adding a boot_() method to the Logger causes a new instance of
    //         the ConsoleHandler to be created at runtime, thus avoiding the
    //         "lingering string reference" problem. - DH 24 Nov. 2003

    void publish(Level level, String loggername, String message) {
	if (shouldIgnore(level)) {
	    return;
	}

        if (hideRepeats && previousMessage != null && 
            previousMessage.equals(message) ) {
            repeatCount++;
            return;
	}
	
        BasicIO.PrintWriter err = BasicIO.err;
        
        // finish off a repeated message first - yes there is a bug if
        // the last message to be logged is a repeated message as we'll not
        // see this message telling us that it was repeated. --DH
        // then again it is a small price to pay in comparison to having to
        // look at 8542 occurrences of the same meaningless message ;-) --jv
	if (repeatCount > 0) {
             err.print("\t << previous message repeated ");
             err.print(repeatCount);
             err.println(" more times >>");
        }

        if (hideRepeats) {
            repeatCount = 0;
            previousMessage = message;
        }

        // fresh message
	if (loggername != null && !"".equals(loggername)) {
	    err.print(loggername); // ok, no concat
	    err.print(": ");
	} 
	err.println(message);	    
    }
}
