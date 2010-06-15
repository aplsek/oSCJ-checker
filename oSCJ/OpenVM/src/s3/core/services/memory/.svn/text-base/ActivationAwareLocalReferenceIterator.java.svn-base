package s3.core.services.memory;

import ovm.core.services.memory.LocalReferenceIterator;
import ovm.core.execution.Context;
import ovm.core.execution.Activation;
import ovm.core.services.memory.MemoryManager;

public abstract class ActivationAwareLocalReferenceIterator
    extends LocalReferenceIterator {
    
    protected abstract void setThreadAndTopFrame(Context thread,
						 Activation topFrame);

    private Context currentThread;
    private Activation topFrameInCurrentThread;
    
    protected void setThread(Context thread) {
	setThreadAndTopFrame(thread,
			     (thread==currentThread
			      ? topFrameInCurrentThread
			      : thread.getCurrentActivation()));
    }
    
    public void prepareForGC() {
	prepareForGCHook();
	currentThread=Context.getCurrentContext();
	topFrameInCurrentThread=currentThread.getCurrentActivation();
	MemoryManager.the().doGC();
    }
}

