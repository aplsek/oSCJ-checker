package scope.scope.sanity;

import javax.realtime.PriorityParameters;

import javax.safetycritical.NoHeapRealtimeThread;
import javax.safetycritical.StorageParameters;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

@Scope("D")
@DefineScope(name="D", parent=IMMORTAL)
@SCJAllowed(value= LEVEL_2, members=true)
public class TestNHRTParameters extends NoHeapRealtimeThread
{
  private static final long BackingStoreRequirements = 500;
  private static final long NativeStackRequirements = 2000;
  private static final long JavaStackRequirements = 300;

  public TestNHRTParameters(int priority) {
    super(new PriorityParameters(priority),
          new StorageParameters(BackingStoreRequirements,
                                NativeStackRequirements,
                                JavaStackRequirements));
  }

  @Override
  @RunsIn("D")
  public void run() {
  }
}

