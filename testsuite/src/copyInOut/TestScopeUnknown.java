package copyInOut;

import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;

public class TestScopeUnknown extends Mission {

	
	@Scope("copyInOut.TestLLMission")
	@RunsIn("copyInOut.MyHandler")
	class MyHandler extends PeriodicEventHandler {

		@Override
		public StorageParameters getThreadConfigurationParameters() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void handleEvent() {
			// TODO Auto-generated method stub
			
		}
	
	
	
	}
	
}
