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
    private Iterator iterator = null;

    public LinkedList getList() {
        return list; // ERROR????
    }

    /**
     * TODO : should we return an iterator???
     *
     * @return
     */
    public Iterator getIterator() {
        //
        // Iterator iter = list.iterator();
        // Iterator res = new ListIterator(iter);
        //
        // return iter;

        return null;
    }

    public Foo getNode() {
        if (iterator == null)
            iterator = list.iterator();

        if (!iterator.hasNext())
            return null;

        Foo foo = (Foo) iterator.next();
        return new Foo(foo); // DEEP-COPY Foo!
    }

    @Override
    protected void initialize() {
        new MyHandler(null, null, null, 0);
    }

    @Scope("copyInOut.TestCollections")
    @RunsIn("copyInOut.MyHandler")
    class MyHandler extends PeriodicEventHandler {

        public MyHandler(PriorityParameters priority,
                PeriodicParameters parameters, StorageParameters scp,
                long memSize) {
            super(priority, parameters, scp);
        }

        @Override
        public void handleAsyncEvent() {
            TestCollectionsMisssion mission = (TestCollectionsMisssion) Mission
                    .getCurrentMission();
            // LinkedList<Foo> list = mission.getList();
            // Iterator<Foo> iter = list.iterator();

            Foo node = mission.getNode(); // iterate through the list
            while (node != null) {
                node.method();
                node = mission.getNode();
            }
        }
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    class Foo {
        int id;

        public Foo() {
            id = 0;
        }

        public Foo(Foo foo) {
            id = foo.id;
        }

        public void method() {
        }
    }
}
