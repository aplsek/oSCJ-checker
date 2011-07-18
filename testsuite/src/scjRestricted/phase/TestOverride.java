package scjRestricted.phase;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed(members=true)
public class TestOverride implements Safelet  {

    public TestOverride() {
    }

    // The following three methods implement the Safelet interface
    @SCJAllowed(SUPPORT)
    @SCJRestricted(INITIALIZATION)
    public MissionSequencer getSequencer() {
      return null;
    }

    @Override
    @SCJAllowed(SUPPORT)
    public long immortalMemorySize() {
        // TODO Auto-generated method stub
        return 0;
    }

}
