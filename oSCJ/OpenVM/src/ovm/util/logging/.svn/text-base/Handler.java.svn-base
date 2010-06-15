package ovm.util.logging;

import ovm.core.OVMBase;

public abstract class Handler 
    extends OVMBase {
    
    private Level level = Level.ALL;
    
    public void setLevel(Level level) {
	this.level = level;
    }

    protected boolean shouldIgnore(Level declared) {
	if (level.intValue() > declared.intValue()) {
	    return true;
	} else {
	    return false;
	}
    }

    // package scope, original handle 
    abstract void publish(Level l, String loggername, String message);

}
