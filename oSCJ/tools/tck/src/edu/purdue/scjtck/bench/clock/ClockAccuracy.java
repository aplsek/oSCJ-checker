package edu.purdue.scjtck.bench.clock;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class ClockAccuracy extends Benchmark {

	private long[] _scjTimeStamps = new long[_prop._iterations + 1];
	private long[] _nativeTimeStamps = new long[_prop._iterations + 1];
	private long[] _sleepPeriods = new long[_prop._iterations + 1];

	@Override
	public void setup() {
		super.setup();
		for (int i = 0; i < _prop._iterations + 1; i++) {
			_sleepPeriods[i] = (long) (Math.random() * 100 + 1) % 100;
		}
	}

	public String report() {
		long[] rawData = new long[_prop._iterations];

		// get raw data
		for (int i = 0; i < _prop._iterations; i++) {
			_scjTimeStamps[i] = _scjTimeStamps[i + 1] - _scjTimeStamps[i];
			_nativeTimeStamps[i] = _nativeTimeStamps[i + 1]
					- _nativeTimeStamps[i];
			rawData[i] = Math.abs(_scjTimeStamps[i] - _nativeTimeStamps[i]);
		}

		// process and report
		String result = "";
		result += "-- DataSet: all --\n";
		result += processRawData(rawData);
		result += "\n";
		result += "-- DataSet: with first " + _prop._dropFirstN
				+ " dropped --\n";
		result += processRawData(Stat.copyOfRange(rawData, _prop._dropFirstN,
				rawData.length));
		result += "\n";
		result += "Raw Data: \n";
		for (int i = 0; i < _prop._iterations; i++)
			result += rawData[i] + "\n";

		return result;
	}

	public MissionSequencer getSequencer() {
		return new GeneralSingleMissionSequencer(new GeneralMission() {

			@Override
			public void initialize() {
				new GeneralManagedThread() {
					@Override
					public void run() {
						for (int i = 0; i < _prop._iterations + 1; i++) {
							_scjTimeStamps[i] = Time.currentTimeToLong();
							_nativeTimeStamps[i] = Time.getNanosecondsNative();
							try {
								Thread.sleep(_sleepPeriods[i]);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						requestSequenceTermination();
					}
				}.start();
			}
		});
	}
}
