package ovm.core.services.events;

import ovm.core.services.io.*;

public class PollcheckManager {
    
    public static abstract class Settings {
	public boolean supports(Settings other) {
	    return other instanceof PlainPollcheck;
	}
	
	public boolean supportsMaxCount() { return false; }
	public void setMaxCount(short maxCount) {}
	
	public abstract String fastPathInC();
	public abstract String slowPathInC();
    }
    
    public static class PlainPollcheck extends Settings {
	public String fastPathInC() {
	    return "(!eventUnion.oneTrueWord)";
	}

	public String slowPathInC() {
	    return "eventPollcheck()";
	}
    }
    
    private static Settings settings=new PlainPollcheck();
    
    public static void setSettings(Settings settings) {
	if (!settings.supports(PollcheckManager.settings)) {
	    throw new Error("Cannot change pollcheck settings to "+settings+" because they do "+
			    "not support the capabilities of our previous settings, which "+
			    "were "+PollcheckManager.settings);
	}
	BasicIO.out.println("PollcheckManager: changing settings to "+settings);
	PollcheckManager.settings=settings;
    }
    
    public static Settings getSettings() {
	return settings;
    }
    
}


