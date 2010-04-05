package edu.purdue.scjtck.bench.throughput;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class Shifting extends Benchmark {

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
					int n1 = 23;
					int n2 = 2;
					int n3 = 14;
					int result = 0;

					@Override
					public void handleEvent() {
						if (_counter >= _prop._iterations) {
							requestSequenceTermination();
							return;
						}

						long start = Time.getNanosecondsNative();

						result = 0x0fffffff;
						result = (result << n1);
						result <<= n3;
						result >>= n2;
						result = (result << -3);
						result = (result << 17);
						result = (result >> n2);
						result <<= 16;
						result >>= n1;
						result = (result >> 8);
						result <<= -3;

						result <<= n2;
						result <<= n3;
						result >>= n1;
						result = (result << 17);
						result = (result << -6);
						result = (result >> n1);
						result <<= -7;
						result >>= n2;
						result = (result >> -6);
						result <<= 20;

						result >>= n3;
						result <<= n3;
						result >>= n3;
						result = (result << -4);
						result = (result << 7);
						result = (result >> n3);
						result <<= 5;
						result >>= n3;
						result = (result >> -6);
						result <<= 2;

						_execTimes[_counter++] = Time.getNanosecondsNative()
								- start;
					}
				};
			}
		});
	}

}
