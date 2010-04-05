package edu.purdue.scjtck.bench.latency;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class DispatchingNHRT extends Benchmark {

	long[][] _startTimeStamps = new long[_prop._threads][_prop._iterations];
	long[][] _runTimeStamps = new long[_prop._threads][_prop._iterations];
	int _counter;

	public String report() {
		long[][] rawData = new long[_prop._threads][_prop._iterations];

		for (int i = 0; i < _prop._threads; i++)
			for (int j = 0; j < _prop._iterations; j++)
				rawData[i][j] = _runTimeStamps[i][j] - _startTimeStamps[i][j];

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
			public void cleanup() {
				if (_counter >= _prop._iterations)
					super.cleanup();
			}

			@Override
			public void initialize() {

				GeneralManagedThread[] NHRTs = new GeneralManagedThread[_prop._threads];

				createBackgroundThreads();

				for (int i = 0; i < _prop._threads; i++) {
					final int index = i;
					NHRTs[index] = new GeneralManagedThread() {
						public void run() {
							_runTimeStamps[index][_counter] = Time
									.getNanosecondsNative();
						}
					};
				}
				for (int i = 0; i < _prop._threads; i++) {
					_startTimeStamps[i][_counter] = Time.getNanosecondsNative();
					NHRTs[i].start();
				}
				try {
					for (int i = 0; i < _prop._threads; i++)
						NHRTs[i].join();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				_counter++;
				new GeneralPeriodicEventHandler() {
					@Override
					public void handleEvent() {
						if (_counter < _prop._iterations)
							terminate(false);
						else {
							requestSequenceTermination();
						}
					}
				};
			}
		});
	}
}
