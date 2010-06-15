package ovm.util.logging;

import ovm.core.OVMBase;

/**
 * This class is a compatibility placeholder for java.util.logging.Logger
 */
public class Logger extends OVMBase {

    String name;
    Handler[] handlers;
    public Logger(String name) {
        this.name = name;
    }
    
    public synchronized void addHandler(Handler h) {
        if (handlers == null) {
            handlers = new Handler[] { h };
        } else {
            Handler[] newH = new Handler[handlers.length + 1];
            System.arraycopy(handlers, 0, newH, 0, handlers.length + 1);
            handlers = newH;
        }
    }
    
    private Level level = Level.ALL;
    
    public void setLevel(Level level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }
    
    public static Logger global;
    
    // common static init code for hosted and OVM
    private static void init(boolean hideRepeats) {
        global.addHandler(new ConsoleHandler(hideRepeats));
        String severity = System.getProperty("logging.level");
        if (severity == null) {
            global.setLevel(Level.CONFIG);
        } else {
            Level l = Level.parse(severity);
            if (l == null) {
                global.setLevel(Level.CONFIG);
            } else {
                global.setLevel(l);
            }
        }
    }

    // OVM init code - NOTE: it is still possible for the bootstrap
    // process to use the logger at runtime before this boot_ method
    // has been executed, and so still used the hosted instance of the
    // logger. This can cause "<< Above message repeated XXX times>>" to
    // be printed on the first use of the logger.
    private static void boot_() {
        global = new Logger("# OVM Log");
        init(false);
    }

    // Hosted init code
    static {
        global = new Logger("# Log");
        init(true);
    }

    private boolean shouldIgnore(Level declared) {
        if (level.intValue() > declared.intValue()) {
            return true;
        } else {
            return false;
        }
    }
    
    private void report(Level l, String message) {
        if (shouldIgnore(l)) {
            return;
        }
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].publish(l, name, message);
        }
    }
    
    public void fine(String message) {
        report(Level.FINE, message);
    }
    
    public void finer(String message) {
        report(Level.FINER, message);
    }
    
    public void warning(String message) {
        report(Level.WARNING, message);
    }
    
    public void info(String message) {
        report(Level.INFO, message);
    }
    
    public void error(String message) {
        severe(message);
    }
    
    public void severe(String message) {
        report(Level.SEVERE, message);
    }
    
    public void throwing(String message, Throwable thrown) {
        report(Level.FINER, thrown + ": " + message);
    }
    
}
