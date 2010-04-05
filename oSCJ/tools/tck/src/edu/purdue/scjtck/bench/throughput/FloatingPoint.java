package edu.purdue.scjtck.bench.throughput;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class FloatingPoint extends Benchmark {

	// time per 30 operations
	private long _execTimes[] = new long[_prop._iterations];
	private int _counter;

	@Override
	protected String report() {
		long[] rawData = _execTimes;

		// process and report
		String result = "(ns / 30ops)";
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

				createBackgroundThreads();

				new GeneralPeriodicEventHandler() {

					// copy from Suramadu
					double x1 = -300943.45;
					double x2 = 3945.34;
					double x3 = -102094.89349;
					double result;

					@Override
					public void handleEvent() {
						if (_counter >= _prop._iterations) {
							requestSequenceTermination();
							return;
						}

						long start = Time.getNanosecondsNative();

						result = x1 * x2;
						result /= x3;
						result += x2;
						result *= -35467.80796875;
						result *= .009857467;
						result -= x2;
						result /= .009857467;
						result += x1;
						result -= 94082885.7467;
						x1 = result /= -3.504060702;

						result += x1;
						result /= x3;
						result += x1;
						result *= 3467.80796875;
						result *= -0.009857467;
						result -= x1;
						result /= -.009857467;
						result += x2;
						result -= -82885.7467;
						x2 = result /= 3.500702;

						result *= x1;
						result /= x3;
						result += x3;
						result *= -467.80796875;
						result *= 98.0857467;
						result -= x3;
						result /= 75.007467;
						result += x3;
						result -= -85.7467;
						x3 = result /= 3045.500702;

						_execTimes[_counter++] = Time.getNanosecondsNative()
								- start;
					}
				};
			}
		});
	}
}
