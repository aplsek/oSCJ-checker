package edu.purdue.scjtck.bench.latency;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class SyncOverhead extends Benchmark {
	private SyncMission _mission = new SyncMission();

	@Override
	protected String report() {
		StringBuilder sb = new StringBuilder(2048 + 64 * _prop._iterations);
		sb.append("-- DataSet: all --\n");
		buildReport(sb, 0);
		sb.append("-- DataSet: with first ");
		sb.append(_prop._dropFirstN);
		sb.append(" dropped --\n");
		buildReport(sb, _prop._dropFirstN);
		sb.append("\n");
		buildRawData(sb, "Enter Time", _mission.enterTime);
		buildRawData(sb, "Exit Time", _mission.exitTime);
		buildRawData(sb, "Nested Enter Time", _mission.nestedEnterTime);
		buildRawData(sb, "Nested Exit Time", _mission.nestedExitTime);
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

	private void buildReport(StringBuilder sb, int startIndex) {
		buildSubReport(sb, "Enter Time", _mission.enterTime, startIndex);
		buildSubReport(sb, "Exit Time ", _mission.exitTime, startIndex);
		buildSubReport(sb, "Nested Enter Time", _mission.nestedEnterTime,
				startIndex);
		buildSubReport(sb, "Nested Exit Time ", _mission.nestedExitTime,
				startIndex);
	}

	private static void buildSubReport(StringBuilder sb, String prefix,
			long[] data, int startIndex) {
		buildReportLine(sb, prefix, "avg", Double.toString(Stat.avg(data,
				startIndex)), "ns");
		buildReportLine(sb, prefix, "min", Long.toString(Stat.min(data,
				startIndex)), "ns");
		buildReportLine(sb, prefix, "max", Long.toString(Stat.max(data,
				startIndex)), "ns");
		buildReportLine(sb, prefix, "std", Double.toString(Stat.stddev(data,
				startIndex)), "");
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

	class SyncMission extends GeneralMission {
		private long[] enterTime = new long[_prop._iterations];
		private long[] exitTime = new long[_prop._iterations];
		private long[] nestedEnterTime = new long[_prop._iterations];
		private long[] nestedExitTime = new long[_prop._iterations];

		@Override
		public void initialize() {
			createBackgroundThreads();
			new SyncHandler();
		}

		class A {
			public synchronized void foo(int i, boolean nest) {
				long enter = Time.getNanosecondsNative();
				if (nest) {
					enterTime[i] = enter - enterTime[i];
					nestedEnterTime[i] = Time.getNanosecondsNative();
					foo(i, false);
					nestedExitTime[i] = Time.getNanosecondsNative()
							- nestedExitTime[i];
					exitTime[i] = Time.getNanosecondsNative();
				}
				else {
					nestedEnterTime[i] = enter - nestedEnterTime[i];
					nestedExitTime[i] = Time.getNanosecondsNative();
				}
			}
		}

		class SyncHandler extends GeneralPeriodicEventHandler {
			@Override
			public void handleEvent() {
				FakeSyncThread[] workerThreads = new FakeSyncThread[_prop._threads - 1];
				for (int i = 0; i < workerThreads.length; i++) {
					workerThreads[i] = new FakeSyncThread();
				}
				for (int i = 0; i < workerThreads.length; i++) {
					workerThreads[i].start();
				}

				A a = new A();
				for (int i = 0; i < _prop._iterations; i++) {
					enterTime[i] = Time.getNanosecondsNative();
					a.foo(i, true);
					exitTime[i] = Time.getNanosecondsNative() - exitTime[i];
				}

				for (int i = 0; i < workerThreads.length; i++) {
					try {
						workerThreads[i].join();
					}
					catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				SyncMission.this.terminate(false);
			}
		}
	}

	class FakeSyncThread extends GeneralManagedThread {
		class B {
			public synchronized void foo(int i, boolean nest) {
				long enter = Time.getNanosecondsNative();
				if (nest) {
					long a = Time.getNanosecondsNative() - enter;
					long b = Time.getNanosecondsNative();
					foo(i, false);
					long c = Time.getNanosecondsNative() - b;
					long d = Time.getNanosecondsNative();
				}
				else {
					long a = enter - Time.getNanosecondsNative();
					long b = Time.getNanosecondsNative();
				}
			}
		}

		public void run() {
			B b = new B();
			for (int i = 0; i < _prop._iterations; i++) {
				long a = Time.getNanosecondsNative();
				b.foo(i, true);
				long c = Time.getNanosecondsNative();
				c -= a;
			}
		}
	}
}
