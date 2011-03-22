package scope.level0;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(members=true)
@Scope("Level0App")
@DefineScope(name="WordHandler", parent="Level0App")
public class WordHandler extends PeriodicEventHandler {

    @SCJAllowed()
    @SCJRestricted(INITIALIZATION)
    public WordHandler(long psize) {
        super(null, null, null);
    }

    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("WordHandler")
    public void handleAsyncEvent() {
        // printing HelloWorld!!!!
    }

    @Override
    @SCJAllowed()
    public void cleanUp() {
    }
}
