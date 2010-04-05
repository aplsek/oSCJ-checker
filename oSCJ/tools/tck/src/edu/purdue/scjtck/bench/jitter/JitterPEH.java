package edu.purdue.scjtck.bench.jitter;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class JitterPEH extends Benchmark {

	private long[][] _timeStamps = new long[_prop._threads][_prop._iterations + 1];
	private int[] _counter = new int[_prop._threads];

	public String report() {
		long[][] rawData = new long[_prop._threads][_prop._iterations];

		// get raw data
		for (int i = 0; i < _prop._threads; i++)
			for (int j = 0; j < _prop._iterations; j++)
				rawData[i][j] = _timeStamps[i][j + 1] - _timeStamps[i][j];

		// process and report
		String result = "";
		for (int i = 0; i < _prop._threads; i++) {
			result += "-- DataSet: all --\n";
			result += processRawData(rawData[i]);
			result += "\n";
			result += "-- DataSet: with first " + _prop._dropFirstN
					+ " dropped --\n";
			result += processRawData(Stat.copyOfRange(rawData[i],
					_prop._dropFirstN, rawData[i].length));
			result += "\n";
		}

		result += "Raw Data: \n";
		for (int i = 0; i < _prop._threads; i++) {
			for (int j = 0; j < _prop._iterations; j++)
				result += rawData[i][j] + "\n";
			result += "\n";
		}

		return result;
	}

	public MissionSequencer getSequencer() {
		return new GeneralSingleMissionSequencer(new GeneralMission() {

			@Override
			public void initialize() {
				createBackgroundThreads();
				for (int i = 0; i < _prop._threads; i++) {
					final int index = i;
					new GeneralPeriodicEventHandler() {
						@Override
						public void handleEvent() {
							_timeStamps[index][_counter[index]] = Time
									.getNanosecondsNative();
							if (++_counter[index] >= _prop._iterations + 1)
								requestSequenceTermination();
						}
					};
				}
			}
		});
	}
}
