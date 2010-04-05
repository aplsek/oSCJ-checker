package edu.purdue.scjtck.bench.throughput;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class IntegerOps extends Benchmark {

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
					int n1 = -300943;
					int n2 = 3945;
					int n3 = -102094;
					int result;

					@Override
					public void handleEvent() {
						if (_counter >= _prop._iterations) {
							requestSequenceTermination();
							return;
						}

						long start = Time.getNanosecondsNative();

						result = n1 * n2;
						result /= n3;
						result += n2;
						result *= -354675;
						result *= 7467;
						result -= n2;
						result /= 7467;
						result += n1;
						result -= 940827;
						result /= -30702;

						result += n1;
						result /= n3;
						result += n1;
						result *= 34875;
						result *= -067;
						result -= n1;
						result /= -57467;
						result += n2;
						result -= -7467;
						result /= 300702;

						result -= n3;
						result /= n3;
						result += n3;
						result *= -4675;
						result *= 9867;
						result -= n3;
						result /= 7567;
						result += n3;
						result -= -867;
						result /= 30702;

						_execTimes[_counter++] = Time.getNanosecondsNative()
								- start;
					}
				};
			}
		});
	}

}
