package copyInOut;

import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.Terminal;
import javax.safetycritical.annotate.Scope;

@Scope("copyInOut.TestTerminal")
public class TestTerminal extends Mission  {

	public class WordHandler extends PeriodicEventHandler {

		private int count_;
		private Terminal myTerminal;

		private WordHandler(long psize, String name, int count) {
			super(null, null, null);
			count_ = count;
		}

		public void handleAsyncEvent() {
			Terminal.getTerminal().write("HelloWorld");					// print to terminal	
			myTerminal = Terminal.getTerminal();						// TODO: store terminal reference?

			if (count_-- == 0)
				getCurrentMission().requestSequenceTermination();      // OK: getCurrentMission
		}

		public void cleanUp() {
		}

		public StorageParameters getThreadConfigurationParameters() {
			return null;
		}
	}

	@Override
	public long missionMemorySize() {
		return 0;
	}

	@Override
	protected void initialize() {
	}
}
