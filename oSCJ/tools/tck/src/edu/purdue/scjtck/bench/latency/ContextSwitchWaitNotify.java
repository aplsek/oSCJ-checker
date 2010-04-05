package edu.purdue.scjtck.bench.latency;

import javax.safetycritical.MissionSequencer;

import edu.purdue.scjtck.bench.Benchmark;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public class ContextSwitchWaitNotify extends Benchmark {

	long[] _beforeTimeStamps = new long[_prop._iterations];
	long[][] _afterTimeStamps = new long[_prop._threads][_prop._iterations];
	volatile int _counter;

	public String report() {
		long[][] rawData = new long[_prop._threads][_prop._iterations];

		// get raw data
		for (int i = 0; i < _prop._threads; i++)
			for (int j = 0; j < _prop._iterations; j++)
				rawData[i][j] = _afterTimeStamps[i][j] - _beforeTimeStamps[j];

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

			volatile boolean _beforeIsReady = false;
			volatile int _afterIsReady = 0;
			volatile int _decisionMade = 0;

			MyLock _lock = new MyLock();

			@Override
			public void initialize() {
				// TODO: add timeout feature to avoid dead loop

				createBackgroundThreads();

				// before thread
				new GeneralManagedThread() {

					@Override
					public void run() {
						long timeVal;
						while (true) {
							_beforeIsReady = true;

							while (_afterIsReady != _prop._threads) {
								Thread.yield();
							}
							if (_counter >= _prop._iterations)
								break;

							// ------------------------------
							// Terminal.getTerminal().writeln("notify");
							timeVal = _lock.doNotifyAll();

							while (_decisionMade != _prop._threads) {
								Thread.yield();
							}
							_beforeTimeStamps[_counter++] = timeVal;
							_afterIsReady = 0;
							_decisionMade = 0;
						}
						requestSequenceTermination();
					}
				}.start();

				// after thread
				for (int i = 0; i < _prop._threads; i++) {
					final int index = i;
					new GeneralManagedThread() {

						@Override
						public void run() {
							long timeVal = 0;
							while (true) {
								while (!_beforeIsReady) {
									Thread.yield();
								}
								_afterIsReady++;

								if (_counter >= _prop._iterations)
									break;
								// ------------------------------
								try {
									// Terminal.getTerminal().writeln("wait");
									timeVal = _lock.doWait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}

								_afterTimeStamps[index][_counter] = timeVal;
								_beforeIsReady = false;
								_decisionMade++;
							}
						}
					}.start();
				}
			}

			class MyLock {
				public synchronized long doWait() throws InterruptedException {
					this.wait();
					return Time.getNanosecondsNative();
				}

				public synchronized long doNotifyAll() {
					long timeVal = Time.getNanosecondsNative();
					this.notifyAll();
					return timeVal;
				}
			}
		});
	}
}
