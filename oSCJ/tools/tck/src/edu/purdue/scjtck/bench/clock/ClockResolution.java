package edu.purdue.scjtck.bench.clock;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageConfigurationParameters;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class ClockResolution extends Benchmark {
	private ClockMission[] _missions;

	public ClockResolution() {
		_missions = new ClockMission[] { new ClockMission(1, 0),
				new ClockMission(0, 100000), new ClockMission(0, 10000),
				new ClockMission(0, 1000), };
	}

	public String report() {
		StringBuilder sb = new StringBuilder(2048 + 16 * _missions.length
				* _prop._iterations);
		sb.append("-- DataSet: all --\n");
		for (int i = 0; i < _missions.length; i++) {
			buildSubReport(sb, _missions[i], 0);
		}
		sb.append("-- DataSet: with first ");
		sb.append(_prop._dropFirstN);
		sb.append(" dropped --\n");
		for (int i = 0; i < _missions.length; i++) {
			buildSubReport(sb, _missions[i], _prop._dropFirstN);
		}
		sb.append("\n");
		for (int i = 0; i < _missions.length; i++) {
			buildRawData(sb, _missions[i]);
		}
		return sb.toString();
	}

	private static void buildRawData(StringBuilder sb, ClockMission mission) {
		sb.append("Raw Data (");
		buildPeriod(sb, mission._ms, mission._ns);
		sb.append("):\n");
		for (int i = 0; i < mission._times.length; i++) {
			sb.append(mission._times[i]);
			sb.append("\n");
		}
		sb.append("\n");
	}

	private static void buildSubReport(StringBuilder sb, ClockMission mission,
			int startIndex) {
		buildReportLine(sb, mission, "avg", Double.toString(Stat
				.avg(mission._times, startIndex)), "ns");
		buildReportLine(sb, mission, "min", Long.toString(Stat
				.min(mission._times, startIndex)), "ns");
		buildReportLine(sb, mission, "max", Long.toString(Stat
				.max(mission._times, startIndex)), "ns");
		buildReportLine(sb, mission, "std", Double.toString(Stat
				.stddev(mission._times, startIndex)), "");
	}

	private static void buildReportLine(StringBuilder sb, ClockMission mission,
			String dataLabel, String data, String units) {
		buildPeriod(sb, mission._ms, mission._ns);
		sb.append(", Jitter (");
		sb.append(dataLabel);
		sb.append("): ");
		sb.append(data);
		sb.append(units);
		sb.append("\n");
	}

	private static void buildPeriod(StringBuilder sb, long ms, int ns) {
		sb.append("Period = ");
		sb.append(ms);
		sb.append("ms ");
		sb.append(ns);
		sb.append("ns");
	}

	public MissionSequencer getSequencer() {
		return new ClockSequencer(new PriorityParameters(_prop._priority),
				_missions);
	}

	class ClockSequencer extends MissionSequencer {
		final ClockMission[] _missions;
		int _completed = 0;

		public ClockSequencer(PriorityParameters priority,
				ClockMission[] missions) {
			super(priority, new S3StorageConfigurationParameters());
			_missions = missions;
		}

		@Override
		protected Mission getInitialMission() {
			return _missions[0];
		}

		@Override
		protected Mission getNextMission() {
			_completed++;
			if (_completed < _missions.length) {
				return _missions[_completed];
			}
			else {
				_launcher.interrupt();
				return null;
			}
		}
	}

	class ClockMission extends GeneralMission {
		final long _interval;
		final long _ms;
		final int _ns;
		final long[] _times = new long[_prop._iterations];

		public ClockMission(long ms, int ns) {
			_interval = ms * 1000000 + ns;
			_ms = ms;
			_ns = ns;
		}

		@Override
		public void initialize() {
			createBackgroundThreads();
			new ClockHandler();
		}
		
		@Override
		protected void cleanup() {
		}

		class ClockHandler extends PeriodicEventHandler {
			public ClockHandler() {
				super(new PriorityParameters(_prop._priority),
						new PeriodicParameters(new RelativeTime(_prop._iDelay,
								0), new RelativeTime(_ms, _ns)), 0);
			}

			int _iter = 0;
			long _last = 0;

			@Override
			public void handleEvent() {
				if (_iter < _prop._iterations) {
					if (_last == 0) {
						_last = Time.getNanosecondsNative();
					}
					else {
						long now = Time.getNanosecondsNative();
						long period = now - _last;
						_times[_iter] = period - _interval;
						if (_times[_iter] < 0) {
							_times[_iter] = 0 - _times[_iter];
						}
						_iter++;
						_last = now;
					}
				}
				else {
					terminate(false);
				}
			}

		}
	}
}
