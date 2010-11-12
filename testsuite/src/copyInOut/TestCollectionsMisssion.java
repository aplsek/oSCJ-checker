package copyInOut;

import java.util.Iterator;
import java.util.LinkedList;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@Scope("copyInOut.TestSingletonMission")
public class TestCollectionsMisssion extends Mission {

	private LinkedList list; 
	
	
	public LinkedList getList() {
		return list;   											// ERROR????
	}
	
	public Iterator getIterator() {
		
		Iterator iter = list.iterator();
		Iterator res = new ListIterator(iter);
		
		return iter;
	}
	
	protected void initialize() { 
        new MyHandler(null, null, null, 0);
    }
	
	@Scope("copyInOut.TestCollections")
	@RunsIn("copyInOut.MyHandler")
	class MyHandler extends PeriodicEventHandler {

		public MyHandler(PriorityParameters priority,
				PeriodicParameters parameters, StorageParameters scp,
				long memSize) {
			super(priority, parameters, scp, memSize);
		}

		@Override
		public StorageParameters getThreadConfigurationParameters() {
			return null;
		}

		@Override
		public void handleEvent() {
			TestCollectionsMisssion mission = (TestCollectionsMisssion) Mission.getCurrentMission();
			LinkedList<Foo> list = mission.getList();
			Iterator<Foo> iter = list.iterator();
			while (iter.hasNext()) {
				Foo foo = iter.next();
				foo.method();
			}
		}
	}
	
	
	@Override
	public long missionMemorySize() {
		return 0;
	}
	
	class Foo {
		public void method() {
		}
	}
}
