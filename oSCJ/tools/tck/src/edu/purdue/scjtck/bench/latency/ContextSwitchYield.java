package edu.purdue.scjtck.bench.latency;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class ContextSwitchYield extends Benchmark {

	long[] _beforeTimeStamps = new long[_prop._iterations];
	long[] _afterTimeStamps = new long[_prop._iterations];
	volatile int _counter;

	public String report() {
		long[] rawData = new long[_prop._iterations];

		for (int i = 0; i < _prop._iterations; i++)
			rawData[i] = _afterTimeStamps[i] - _beforeTimeStamps[i];

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

			volatile boolean _beforeIsReady = false;
			volatile boolean _afterIsReady = false;
			volatile boolean _decisionMade = false;

			@Override
			public void initialize() {
				// TODO: add timeout feature to avoid dead loop

				createBackgroundThreads();

				// before thread
				new GeneralManagedThread() {

					@Override
					public void run() {
						long timeVal;
						while (true) {
							_beforeIsReady = true; // 1
							while (!_afterIsReady) { // 5
								Thread.yield();
							}
							if (_counter >= _prop._iterations)
								break;
							// ------------------------------
							timeVal = Time.getNanosecondsNative();
							// suppose to yield second
							Thread.yield(); // 6

							while (!_decisionMade) { // 9
								Thread.yield();
							}
							_beforeTimeStamps[_counter++] = timeVal;
						}
						requestSequenceTermination();
					}
				}.start();

				// after thread
				new GeneralManagedThread() {

					@Override
					public void run() {
						long timeVal;
						while (true) {
							while (!_beforeIsReady) { // 2
								Thread.yield();
							}
							_afterIsReady = true; // 3
							if (_counter >= _prop._iterations)
								break;
							// ------------------------------
							// suppose to yield first
							Thread.yield(); // 4
							timeVal = Time.getNanosecondsNative(); // 7

							_afterTimeStamps[_counter] = timeVal;
							_afterIsReady = false;
							_beforeIsReady = false;
							_decisionMade = true; // 8
						}
					}
				}.start();
			}
		});
	}
}
