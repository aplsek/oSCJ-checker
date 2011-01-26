package copyInOut;

import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;
import static javax.safetycritical.annotate.Scope.UNKNOWN;


public class TestIterator extends Mission {
	public MyLinkedList list;

	@Override
	public long missionMemorySize() {
		return 0;
	}
	@Override
	protected void initialize() {
		Iterator iterator = this.list.iterator();

		@Scope("Unknown") Node node = (Node) iterator.getNext();
		while (node != null) {
			//if (mem_node == mem_current)
			//	node.methodNoCS();
			node = (Node) iterator.getNext();
		}
	}
}


@Scope("copyInOut.TestCollections")
@RunsIn("copyInOut.MyHandler")
class MyHandlerIterator extends PeriodicEventHandler {

	public MyHandlerIterator(PriorityParameters priority,
			PeriodicParameters parameters, StorageParameters scp,
			long memSize) {
		super(priority, parameters, scp);
	}

	@Override
	public StorageParameters getThreadConfigurationParameters() {
		return null;
	}

	@Scope("Mission/Unknown") MyLinkedList myList;

	@Override
	public void handleAsyncEvent() {
		TestIterator mission = (TestIterator) Mission.getCurrentMission();
		@Scope("Unknown") MyLinkedList mylist =  mission.list;  			 // LOCAL INFERENCE .....  ///mission.getList();
		this.myList = mission.list;  									// WE PROPOSE @LivesIN to have mission.getList();
		Iterator iterator = this.myList.iterator();  				  // returns object in the current scope!!!!
		@Scope("Unknown") Node node = (Node) iterator.getNext();       // should return the reference to Node living in @Scope("Mission")
		node.method();
	}
}


class MyLinkedList {

	@RunsIn(UNKNOWN)
	   @Scope(UNKNOWN)
	public Node get(int index) {
		return new Node();
	}

    @RunsIn(UNKNOWN)
    @Scope(UNKNOWN)
	public Iterator iterator() {
		return new Iterator(this);
	}

}


class Iterator {
	int index;

	@Scope("Unknown") MyLinkedList list;

    @RunsIn(UNKNOWN)
	public Iterator(MyLinkedList list) {
		this.list = list;
	}

	@Scope(UNKNOWN)
	public Node getNext() {
		@Scope("Unknown") Node node = list.get(index);
		return node;
	}

	public Node _getNext() {
		@Scope("Unknown") Node node = list.get(index);
		return new Node(node);
	}

}


class Node {
	int id;

	@RunsIn(UNKNOWN)
	public Node() {
		//...
	}
	
	@RunsIn(UNKNOWN)
	public Node(Node node) {
		//...
	}

	@RunsIn(UNKNOWN)
	public void method() {
	}

	public void methodNoCS() {
	}
}
