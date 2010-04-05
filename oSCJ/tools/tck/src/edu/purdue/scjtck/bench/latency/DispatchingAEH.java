package edu.purdue.scjtck.bench.latency;

import javax.realtime.AsyncEvent;
import javax.realtime.AsyncEventHandler;
import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class DispatchingAEH extends Benchmark {

	long[] _fireTimeStamps = new long[_prop._iterations];
	long[][] _handleTimeStamps = new long[_prop._threads][_prop._iterations];
	int _counter;

	public String report() {
		long[][] rawData = new long[_prop._threads][_prop._iterations];

		for (int i = 0; i < _prop._threads; i++)
			for (int j = 0; j < _prop._iterations; j++)
				rawData[i][j] = _handleTimeStamps[i][j] - _fireTimeStamps[j];

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

			volatile int _finished;

			@Override
			public void initialize() {

				createBackgroundThreads();

				_counter = 0;
				final AsyncEvent asyncEvent = new AsyncEvent();
				for (int i = 0; i < _prop._threads; i++) {
					final int index = i;
					asyncEvent.addHandler(new AsyncEventHandler() {
						public void handleEvent() {
							_handleTimeStamps[index][_counter] = Time
									.getNanosecondsNative();
							_finished++;
						}
					});
				}

				new GeneralPeriodicEventHandler() {
					@Override
					public void handleEvent() {
						_finished = 0;
						_fireTimeStamps[_counter] = Time.getNanosecondsNative();
						asyncEvent.fire();
						while (_finished != _prop._threads)
							Thread.yield();
						if (++_counter >= _prop._iterations)
							requestSequenceTermination();
					}
				};
			}
		});
	}
}
