package edu.purdue.scjtck.bench.memory;

import javax.realtime.HeapMemory;
import javax.realtime.ImmortalMemory;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;


public class InfraOverhead extends Benchmark {

	private long _immortalConsumedTilGetSequencer;
	private long _heapConsumedTilGetSequencer;

	private long _immortalConsumedByGeneratingOneMission;
	private long _heapConsumedByGeneratingOneMission;

	private long _immortalConsumedByGeneratingOneMissionSequencer;
	private long _heapConsumedByGeneratingOneMissionSequencer;

	private long _immortalConsumedByGeneratingOneAEH;
	private long _heapConsumedByGeneratingOneAEH;
	private long _missionMemConsumedByGeneratingOneAEH;

	private long _immortalConsumedByGeneratingOnePEH;
	private long _heapConsumedByGeneratingOnePEH;
	private long _missionMemConsumedByGeneratingOnePEH;

	private long _immortalConsumedByGeneratingOneNHRT;
	private long _heapConsumedByGeneratingOneNHRT;
	private long _missionMemConsumedByGeneratingOneNHRT;

	@Override
	protected String report() {
		String result = "";
		result += "ImmortalMemory consumed til getSequencer: "
				+ _immortalConsumedTilGetSequencer + "\n";
		result += "HeapMemory consumed til getSequencer: "
				+ _heapConsumedTilGetSequencer + "\n";
		result += "ImmortalMemory / HeapMemory consumed per Mission: "
				+ _immortalConsumedByGeneratingOneMission + " / "
				+ _heapConsumedByGeneratingOneMission + "\n";
		result += "ImmortalMemory / HeapMemory consumed per MissionSequencer: "
				+ _immortalConsumedByGeneratingOneMissionSequencer + " / "
				+ _heapConsumedByGeneratingOneMissionSequencer + "\n";
		result += "ImmortalMemory / HeapMemory / MissionMemory consumed per AEH: "
				+ _immortalConsumedByGeneratingOneAEH
				+ " / "
				+ _heapConsumedByGeneratingOneAEH
				+ " / "
				+ _missionMemConsumedByGeneratingOneAEH + "\n";
		result += "ImmortalMemory / HeapMemory / MissionMemory consumed per PEH: "
				+ _immortalConsumedByGeneratingOnePEH
				+ " / "
				+ _heapConsumedByGeneratingOnePEH
				+ " / "
				+ _missionMemConsumedByGeneratingOnePEH + "\n";
		result += "ImmortalMemory / HeapMemory / MissionMemory consumed per NHRT: "
				+ _immortalConsumedByGeneratingOneNHRT
				+ " / "
				+ _heapConsumedByGeneratingOneNHRT
				+ " / "
				+ _missionMemConsumedByGeneratingOneNHRT + "\n";
		return result;
	}

	public MissionSequencer getSequencer() {
		_immortalConsumedTilGetSequencer = ImmortalMemory.instance()
				.memoryConsumed();
		_heapConsumedTilGetSequencer = HeapMemory.instance().memoryConsumed();

		int nIter = 10;
		long immortalOriginal, heapOriginal;
		PriorityParameters priority = new PriorityParameters(_prop._priority);

		immortalOriginal = ImmortalMemory.instance().memoryConsumed();
		heapOriginal = HeapMemory.instance().memoryConsumed();
		
		for (int i = 0; i < nIter; i++)
			new DummyMission();

		_immortalConsumedByGeneratingOneMission = (ImmortalMemory.instance()
				.memoryConsumed() - immortalOriginal)
				/ nIter;
		_heapConsumedByGeneratingOneMission = (HeapMemory.instance()
				.memoryConsumed() - heapOriginal)
				/ nIter;

		immortalOriginal = ImmortalMemory.instance().memoryConsumed();
		heapOriginal = HeapMemory.instance().memoryConsumed();

		for (int i = 0; i < nIter; i++)
			new DummyMissionSeqnencer(priority);

		_immortalConsumedByGeneratingOneMissionSequencer = (ImmortalMemory
				.instance().memoryConsumed() - immortalOriginal)
				/ nIter;
		_heapConsumedByGeneratingOneMissionSequencer = (HeapMemory.instance()
				.memoryConsumed() - heapOriginal)
				/ nIter;

		return new GeneralSingleMissionSequencer(new GeneralMission() {

			@Override
			public void initialize() {

				int nIter = 10;
				long immortalOriginal, heapOriginal, missinOriginal;

				// ------- calculating NHRT overhead ------
				immortalOriginal = ImmortalMemory.instance().memoryConsumed();
				heapOriginal = HeapMemory.instance().memoryConsumed();
				missinOriginal = RealtimeThread.getCurrentMemoryArea()
						.memoryConsumed();

				for (int i = 0; i < nIter; i++)
					new GeneralManagedThread() {
					};

				_immortalConsumedByGeneratingOneNHRT = (ImmortalMemory
						.instance().memoryConsumed() - immortalOriginal)
						/ nIter;
				_heapConsumedByGeneratingOneNHRT = (HeapMemory.instance()
						.memoryConsumed() - heapOriginal)
						/ nIter;
				_missionMemConsumedByGeneratingOneNHRT = (RealtimeThread
						.getCurrentMemoryArea().memoryConsumed() - missinOriginal)
						/ nIter;

				// ------- calculating AEH overhead ------
				immortalOriginal = ImmortalMemory.instance().memoryConsumed();
				heapOriginal = HeapMemory.instance().memoryConsumed();
				missinOriginal = RealtimeThread.getCurrentMemoryArea()
						.memoryConsumed();

				for (int i = 0; i < nIter; i++)
					new GeneralAperiodicEventHandler() {
						@Override
						public void handleEvent() {
						}
					};

				_immortalConsumedByGeneratingOneAEH = (ImmortalMemory
						.instance().memoryConsumed() - immortalOriginal)
						/ nIter;
				_heapConsumedByGeneratingOneAEH = (HeapMemory.instance()
						.memoryConsumed() - heapOriginal)
						/ nIter;
				_missionMemConsumedByGeneratingOneAEH = (RealtimeThread
						.getCurrentMemoryArea().memoryConsumed() - missinOriginal)
						/ nIter;

				// ------- calculating PEH overhead ------

				immortalOriginal = ImmortalMemory.instance().memoryConsumed();
				heapOriginal = HeapMemory.instance().memoryConsumed();
				missinOriginal = RealtimeThread.getCurrentMemoryArea()
						.memoryConsumed();

				for (int i = 0; i < nIter; i++)
					new GeneralPeriodicEventHandler() {
						@Override
						public void handleEvent() {
							requestSequenceTermination();
						}
					};
				_immortalConsumedByGeneratingOnePEH = (ImmortalMemory
						.instance().memoryConsumed() - immortalOriginal)
						/ nIter;
				_heapConsumedByGeneratingOnePEH = (HeapMemory.instance()
						.memoryConsumed() - heapOriginal)
						/ nIter;
				_missionMemConsumedByGeneratingOnePEH = (RealtimeThread
						.getCurrentMemoryArea().memoryConsumed() - missinOriginal)
						/ nIter;
			}
		});
	}

	class DummyMission extends Mission {

		@Override
		public void initialize() {
		}

		@Override
		public long missionMemorySize() {
			return 0;
		}
	}

	class DummyMissionSeqnencer extends MissionSequencer {

		public DummyMissionSeqnencer(PriorityParameters priority) {
			super(priority,null);
		}

		@Override
		protected Mission getInitialMission() {
			return null;
		}

		@Override
		protected Mission getNextMission() {
			return null;
		}
	}
}
