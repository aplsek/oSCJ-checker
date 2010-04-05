package edu.purdue.scjtck.bench.jitter;

import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.annotate.Level;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class JitterFrame extends Benchmark {

	private int _nFrames = _prop._iterations + 1;
	private int[] _counter = new int[_prop._threads];
	private long[][] _timeStamps = new long[_prop._threads][_nFrames];

	private CyclicExecutive ce = new MyCyclicExecutive();

	@Override
	protected String report() {
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
		return ce.getSequencer();
	}

	class MyCyclicExecutive extends CyclicExecutive {
		@Override
		public void initialize() {

			for (int j = 0; j < _prop._threads; j++) {
				final int index = j;
				new GeneralPeriodicEventHandler() {
					public void handleEvent() {
						_timeStamps[index][_counter[index]] = Time
								.getNanosecondsNative();
						if (++_counter[index] >= _nFrames)
							requestSequenceTermination();
					}
				};
			}
		}

		public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
			CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[_nFrames];

			for (int i = 0; i < _nFrames; i++)
				frames[i] = new CyclicSchedule.Frame(new RelativeTime(
						_prop._period, 0), handlers);

			CyclicSchedule schedule = new CyclicSchedule(frames);

			return schedule;
		}

		public long missionMemorySize() {
			return _prop._missionMemSize;
		}

		public void teardown() {
		}

		public Level getLevel() {
			return Level.LEVEL_0;
		}

		public void setup() {
		}

		@Override
		protected void cleanup() {
			_launcher.interrupt();
		}
	}

	public Level getLevel() {
		return ce.getLevel();
	}
}
