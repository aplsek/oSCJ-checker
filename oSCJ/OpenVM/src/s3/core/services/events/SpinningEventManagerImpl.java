package s3.core.services.events;

import ovm.core.services.events.*;
import ovm.core.execution.Native;

public class SpinningEventManagerImpl
    extends EventManagerImpl {

    private final static EventManager instance=
	new SpinningEventManagerImpl();
    public static EventManager getInstance() {
	return instance;
    }
    
    protected void setupSignalEventFromThread() {
	Native.makeSignalEventFromThreadSimple();
    }
    
    protected long standardWaitTime() {
	return 0;
    }
}


