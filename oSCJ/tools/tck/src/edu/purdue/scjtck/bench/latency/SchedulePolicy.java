package edu.purdue.scjtck.bench.latency;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;


/*
 * We probably don't need to bench this. Since SCJ only 
 * allows priority ceiling protocol, the overhead to 
 * boost one thread's priority would be hidden in 
 * synchronization?
 */
public class SchedulePolicy extends Benchmark {

	@Override
	protected String report() {
		// TODO Auto-generated method stub
		return null;
	}

	public MissionSequencer getSequencer() {
		return null;
	}
}
