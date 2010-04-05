package edu.purdue.scjtck.bench.memory;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;


public class SCJVMRuntimeSize extends Benchmark {

	@Override
	protected String report() {
		return "SCJVMRuntimeSize finished\n";
	}

	public MissionSequencer getSequencer() {
		return new GeneralSingleMissionSequencer(new GeneralMission() {

			@Override
			public void initialize() {
				new GeneralPeriodicEventHandler() {
					@Override
					public void handleEvent() {
						try {
							Thread.sleep(_prop._period);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						requestSequenceTermination();
					}
				};
			}
		});
	}
}
