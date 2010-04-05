package edu.purdue.scjtck.bench.latency;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class MissionLag extends Benchmark {

	int _nMissions = _prop._iterations + 1;

	long[] _startTimeStamps = new long[_nMissions];
	long[] _endTimeStamps = new long[_nMissions];
	int _counter;

	@Override
	protected String report() {
		long[] rawData = new long[_prop._iterations];
		for (int i = 0; i < _prop._iterations; i++)
			rawData[i] = _startTimeStamps[i + 1] - _endTimeStamps[i];

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
				_startTimeStamps[_counter] = Time.getNanosecondsNative();

				createBackgroundThreads();

				new GeneralPeriodicEventHandler() {
					@Override
					public void handleEvent() {
						terminate(false);
					}
				};
			}

			@Override
			public void cleanup() {
				if (_counter >= _prop._iterations) {
					requestSequenceTermination();
					super.cleanup();
				}
				_endTimeStamps[_counter++] = Time.getNanosecondsNative();
			}
		});
	}
}
