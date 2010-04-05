package edu.purdue.scjtck.bench.latency;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PrivateMemory;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class EnterExitMemory extends Benchmark {
	private EnterExitMission _mission = new EnterExitMission();

	@Override
	public String report() {
		StringBuilder sb = new StringBuilder(512 + 32 * _prop._iterations);
		sb.append("-- DataSet: all --\n");
		buildSubReport(sb, "Enter Time", _mission._enterTimes, 0);
		buildSubReport(sb, "Exit Time ", _mission._exitTimes, 0);
		sb.append("-- DataSet: with first ");
		sb.append(_prop._dropFirstN);
		sb.append(" dropped --\n");
		buildSubReport(sb, "Enter Time", _mission._enterTimes, _prop._dropFirstN);
		buildSubReport(sb, "Exit Time ", _mission._exitTimes, _prop._dropFirstN);
		sb.append("\n");
		buildRawData(sb, "Enter Time", _mission._enterTimes);
		buildRawData(sb, "Exit Time", _mission._exitTimes);
		return sb.toString();
	}

	private static void buildRawData(StringBuilder sb, String dataLabel,
			long[] data) {
		sb.append("Raw Data (");
		sb.append(dataLabel);
		sb.append("):\n");
		for (int i = 0; i < data.length; i++) {
			sb.append(data[i]);
			sb.append("\n");
		}
		sb.append("\n");
	}

	private static void buildSubReport(StringBuilder sb, String prefix,
			long[] data, int startIndex) {
		buildReportLine(sb, prefix, "avg", Double.toString(Stat.avg(data, startIndex)), "ns");
		buildReportLine(sb, prefix, "min", Long.toString(Stat.min(data, startIndex)), "ns");
		buildReportLine(sb, prefix, "max", Long.toString(Stat.max(data, startIndex)), "ns");
		buildReportLine(sb, prefix, "std", Double.toString(Stat.stddev(data, startIndex)),
				"");
	}

	private static void buildReportLine(StringBuilder sb, String prefix,
			String dataLabel, String data, String units) {
		sb.append(prefix);
		sb.append(" (");
		sb.append(dataLabel);
		sb.append("): ");
		sb.append(data);
		sb.append(units);
		sb.append("\n");
	}

	public MissionSequencer getSequencer() {
		return new GeneralSingleMissionSequencer(_mission);
	}

	class EnterExitMission extends GeneralMission {
		private long[] _enterTimes = new long[_prop._iterations];
		private long[] _exitTimes = new long[_prop._iterations];

		class EnterExitHandler extends PeriodicEventHandler {
			public EnterExitHandler() {
				super(new PriorityParameters(_prop._priority),
						new PeriodicParameters(new RelativeTime(_prop._iDelay,
								0), new RelativeTime(1000000, 0)),
						_prop._schedObjMemSize);
			}

			public void handleEvent() {
				// Start worker threads that do similar computation
				FakeEnterExitThread[] workerThreads = new FakeEnterExitThread[_prop._threads - 1];
				for (int i = 0; i < workerThreads.length; i++) {
					workerThreads[i] = new FakeEnterExitThread();
				}
				for (int i = 0; i < workerThreads.length; i++) {
					workerThreads[i].start();
				}

				// Do computation in the event handler thread
				PrivateMemory memory = new PrivateMemory(5000);

				for (int i = 0; i < _prop._iterations; i++) {
					final int iter = i;
					Runnable r = new Runnable() {
						public void run() {
							_enterTimes[iter] = Time.getNanosecondsNative()
									- _enterTimes[iter];
							_exitTimes[iter] = Time.getNanosecondsNative();
						}
					};
					_enterTimes[i] = Time.getNanosecondsNative();
					memory.enter(r);
					_exitTimes[i] = Time.getNanosecondsNative() - _exitTimes[i];
				}

				// Join and terminate
				for (int i = 0; i < workerThreads.length; i++) {
					try {
						workerThreads[i].join();
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				EnterExitMission.this.terminate(false);
			}
		}

		class FakeEnterExitThread extends GeneralManagedThread {
			public void run() {
				PrivateMemory memory = new PrivateMemory(5000);
				for (int i = 0; i < _prop._iterations; i++) {
					final int iter = i;
					Runnable r = new Runnable() {
						public void run() {
							long a = Time.getNanosecondsNative();
							long b = Time.getNanosecondsNative();
							b -= a;
						}
					};
					long a = Time.getNanosecondsNative();
					memory.enter(r);
					long b = Time.getNanosecondsNative();
					b -= a;
				}
			}
		}

		@Override
		public void initialize() {
			createBackgroundThreads();
			new EnterExitHandler();
		}
	}
}
