package edu.purdue.scjtck.bench;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.realtime.SizeEstimator;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PrivateMemory;

import edu.purdue.scjtck.MainSafelet;
import edu.purdue.scjtck.bench.util.Stat;
import edu.purdue.scjtck.bench.util.Time;


public abstract class Benchmark extends MainSafelet {

	protected String getInfo() {
		String info = "";

		info += "*************************************************\n";
		info += " " + getClass() + " " + getLevel() + "\n";
		info += " MissionMem: " + _prop._missionMemSize + "\tSchedObjMem: "
				+ _prop._schedObjMemSize + "\n";
		info += " WKPriority " + _prop._priority + "\tWKThreads "
				+ _prop._threads + "\tWKPeriod " + _prop._period + "\n";
		info += " BGPriority " + _prop._bgPriority + "\tBGThreads "
				+ _prop._bgThreads + "\tBGPeriod " + _prop._bgPeriod + "\n";
		info += " Delay " + _prop._iDelay + "\tIterations " + _prop._iterations
				+ "\n";
		info += "*************************************************";

		return info;
	}

	protected void createBackgroundThreads() {
		for (int i = 0; i < _prop._bgThreads; i++) {
			new PeriodicEventHandler(new PriorityParameters(_prop._bgPriority),
					new PeriodicParameters(
							new RelativeTime(_prop._bgIDelay, 0),
							new RelativeTime(_prop._bgPeriod, 0)),
					_prop._schedObjMemSize) {

				SizeEstimator se = new SizeEstimator();

				@Override
				public void handleEvent() {

					final int nObjects = 500;

					se.reserve(Object.class, nObjects);

					new PrivateMemory(se.getEstimate()).enter(new Runnable() {

						public void run() {
							for (int i = 0; i < nObjects; i++)
								new Object();
						}
					});
				}
			};
		}
	}

	protected String processRawData(long[] data) {
		String result = "";
		result += "Max: " + Stat.max(data) + "\n";
		result += "Min: " + Stat.min(data) + "\n";
		result += "Avg: " + Stat.avg(data) + "\n";
		result += "Std: " + Stat.stddev(data) + "\n";
		return result;
	}

	public void setup() {
		Time.getNanosecondsNative(); // for pre-loading necessary native libs
		super.setup();
	}
}
