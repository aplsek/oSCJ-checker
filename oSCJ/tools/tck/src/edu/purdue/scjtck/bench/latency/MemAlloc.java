package edu.purdue.scjtck.bench.latency;

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PrivateMemory;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;
import edu.purdue.scjtck.bench.util.objects.Object4000B;
import edu.purdue.scjtck.bench.util.objects.Object400B;
import edu.purdue.scjtck.bench.util.objects.Object40B;
import edu.purdue.scjtck.bench.util.objects.Object4B;


public class MemAlloc extends Benchmark {
	private MemMission _mission = new MemMission();

	@Override
	protected String report() {
		Class<?>[] clazzes = _mission.clazzes;
		StringBuilder sb = new StringBuilder(2048 + _mission.clazzes.length
				* 32 * _prop._iterations);
		sb.append("-- DataSet: all --\n");
		buildReport(sb, 0);
		sb.append("-- DataSet: with first ");
		sb.append(_prop._dropFirstN);
		sb.append(" dropped --\n");
		buildReport(sb, _prop._dropFirstN);
		sb.append("\n");
		for (int i = 0; i < clazzes.length; i++) {
			String className = clazzes[i].getName();
			buildRawData(sb, className, "Scoped", _mission.scopedAllocations[i]);
			buildRawData(sb, className, "Immortal",
					_mission.immortalAllocations[i]);
		}
		return sb.toString();
	}

	private static void buildRawData(StringBuilder sb, String className,
			String dataLabel, long[] data) {
		sb.append("Raw Data (");
		sb.append(className);
		sb.append(", ");
		sb.append(dataLabel);
		sb.append("):\n");
		for (int i = 0; i < data.length; i++) {
			sb.append(data[i]);
			sb.append("\n");
		}
		sb.append("\n");
	}

	private void buildReport(StringBuilder sb, int startIndex) {
		Class<?>[] clazzes = _mission.clazzes;
		for (int i = 0; i < clazzes.length; i++) {
			String className = clazzes[i].getName();
			buildSubReport(sb, className, "Scoped",
					_mission.scopedAllocations[i], startIndex);
			buildSubReport(sb, className, "Immortal",
					_mission.immortalAllocations[i], startIndex);
		}
	}

	private static void buildSubReport(StringBuilder sb, String className,
			String prefix, long[] times, int startIndex) {
		buildReportLine(sb, className, prefix, Double.toString(Stat.avg(times,
				startIndex)), "avg", "ns");
		buildReportLine(sb, className, prefix, Long.toString(Stat.min(times,
				startIndex)), "min", "ns");
		buildReportLine(sb, className, prefix, Long.toString(Stat.max(times,
				startIndex)), "max", "ns");
		buildReportLine(sb, className, prefix, Double.toString(Stat.stddev(
				times, startIndex)), "std", "");
	}

	private static void buildReportLine(StringBuilder sb, String className,
			String prefix, String data, String dataLabel, String units) {
		sb.append(className);
		sb.append(" (");
		sb.append(prefix);
		sb.append(", ");
		sb.append(dataLabel);
		sb.append("): ");
		sb.append(data);
		sb.append(units);
		sb.append("\n");
	}

	public MissionSequencer getSequencer() {
		return new GeneralSingleMissionSequencer(_mission);
	}

	class MemMission extends GeneralMission {
		Class<?>[] clazzes = new Class<?>[] { Object4B.class, Object40B.class,
				Object400B.class, Object4000B.class };
		long[][] scopedAllocations = new long[clazzes.length][_prop._iterations];
		long[][] immortalAllocations = new long[clazzes.length][_prop._iterations];

		@Override
		public void initialize() {
			createBackgroundThreads();
			new MemHandler();
		}

		private void allocateLoop(final Class<?> clazz, long[] allocations,
				final MemoryArea mem) {
			final long totalTime[] = new long[] { 0 };
			Runnable r = new Runnable() {
				public void run() {
					long startTime = Time.getNanosecondsNative();
					try {
						mem.newInstance(clazz);
					}
					catch (IllegalAccessException e) {
						throw new Error(e);
					}
					catch (InstantiationException e) {
						throw new Error(e);
					}
					totalTime[0] = Time.getNanosecondsNative() - startTime;
				}
			};
			for (int i = 0; i < _prop._iterations; i++) {
				mem.enter(r);
				if (allocations != null) {
					allocations[i] = totalTime[0];
				}
			}
		}

		class MemHandler extends GeneralPeriodicEventHandler {
			@Override
			public void handleEvent() {
				Thread[] workerThreads = new Thread[_prop._threads - 1];
				for (int i = 0; i < workerThreads.length; i++) {
					workerThreads[i] = new FakeMemThread();
				}
				for (int i = 0; i < workerThreads.length; i++) {
					workerThreads[i].start();
				}
				for (int i = 0; i < clazzes.length; i++) {
					allocateLoop(clazzes[i], scopedAllocations[i],
							new PrivateMemory(50000));
					allocateLoop(clazzes[i], immortalAllocations[i],
							ImmortalMemory.instance());
				}
				for (int i = 0; i < workerThreads.length; i++) {
					try {
						workerThreads[i].join();
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				MemMission.this.terminate(false);
			}
		}

		class FakeMemThread extends GeneralManagedThread {
			public void run() {
				for (int i = 0; i < clazzes.length; i++) {
					allocateLoop(clazzes[i], null, new PrivateMemory(51000));
					allocateLoop(clazzes[i], null, ImmortalMemory.instance());
				}
			}
		}
	}
}
