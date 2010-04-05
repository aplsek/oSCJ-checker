package s3.util.queues;

import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
import s3.util.Visitor;
/**
 * Test program for the SingleLinkDeltaQueue and related interfaces/classes
 *
 */
// not public because we don't want it in javadoc
class DQDriver {

    static class TestNode extends SingleLinkDeltaNode {

        static int nextId;
        public final int id;
        public final int origValue; // the original count to wait for

        public TestNode(int val) {
            origValue = val;
            synchronized(TestNode.class) {
                id = nextId++;
            }
        }

        public String toString() {
            return "(id=" + id + ",D=" + getDelta() + ",O=" + origValue + ")";
        }
    }

    static int counter = 0;  // the 'tick' count

    static SingleLinkDeltaQueue empty, one, two, ten;

    static void resetEmpty() {
        empty = new SingleLinkDeltaQueue();
        assert empty.isEmpty() : "resetEmpty";
        assert checkSums(empty.head);
    }

    static void resetOne() {
        one = new SingleLinkDeltaQueue();
        one.insert(n[5], n[5].origValue);
        assert one.contains(n[5]) : "resetOne";
        assert checkSums(one.head);
        assert !one.isEmpty() : "resetOne";
    }

    static void resetTwo() {
        two = new SingleLinkDeltaQueue();
        two.insert(n[5], n[5].origValue);
        assert two.contains(n[5]) : "resetTwo";
        two.insert(n[7], n[7].origValue);
        assert two.contains(n[7]) : "resetTwo";
        assert checkSums(two.head);
        assert !two.isEmpty() : "resetTwo";
    }

    static void resetTen() {
        ten  = new SingleLinkDeltaQueue();
        for (int i = 1; i < n.length; i++) {
            ten.insert(n[i], n[i].origValue);
        }
        assert checkSums(ten.head);
        assert !ten.isEmpty() : "resetTen";
    }

    static void emptyTen() {
        for (int i = 0; i < n.length; i++) {
            ten.remove(n[i]);
        }
    }

    // set of test nodes

    static TestNode[] n = {
        null,  // dummy to keep index == origValue
        new TestNode(1),
        new TestNode(2),
        new TestNode(3),
        new TestNode(4),
        new TestNode(5),
        new TestNode(6),
        new TestNode(7),
        new TestNode(8),
        new TestNode(9),
        new TestNode(10),
    };

    // set of duplicates

    static TestNode[] na = {
        null,  // dummy to keep index == origValue
        new TestNode(1),
        new TestNode(2),
        new TestNode(3),
        new TestNode(4),
        new TestNode(5),
        new TestNode(6),
        new TestNode(7),
        new TestNode(8),
        new TestNode(9),
        new TestNode(10),
    };


    // set of second duplicates

    static TestNode[] nb = {
        null,  // dummy to keep index == origValue
        new TestNode(1),
        new TestNode(2),
        new TestNode(3),
        new TestNode(4),
        new TestNode(5),
        new TestNode(6),
        new TestNode(7),
        new TestNode(8),
        new TestNode(9),
        new TestNode(10),
    };

    /* Note: we always add the main node then first duplicate, then
       second duplicate. This maintains the invariant that duplicates
       always have a larger id value than their predecesor.
       Duplicates are inserted FIFO ie a node with the same delta as an
       existing node will be inserted behind it.
    */


    public static void main(String[] args) {
	boolean enabled = false;
	assert enabled = true;
	if (!enabled)
	    throw new Error("run this test with -enableassertions!");

        Visitor vis = new Visitor() {
                public void visit(Object o) {
                    TestNode t = (TestNode) o;
		    assert t.origValue == counter:
                        "Visit error: counter = " + counter +
			" orig value = " + t.origValue;
                }
            };

        BasicIO.err.println("Starting tests");

        resetEmpty();

        // check illegal insertion
        try {
            empty.insert( new TestNode(0), 0);
            assert false : "illegal insertion failed";
        }
        catch(IllegalArgumentException ex) {
            assert empty.head == null : "empty.head==null";
        }

        // empty queue tests

        BasicIO.out.println("empty queue tests");
        resetEmpty();

        doUpdates(empty, 1, vis, true);
        assert empty.isEmpty() : "update empty";
        resetEmpty();

        // insert into empty queue
        checkSums(empty.head);
        empty.insert(n[5], n[5].origValue);
        empty.dump(BasicIO.out);
        assert empty.head == n[5] : "empty.head == n[5]";
        assert empty.head.getDelta() == n[5].origValue : "delta == origValue";
        assert empty.contains(n[5]) : "contains(n[5])";
        checkSums(empty.head);
        empty.dump(BasicIO.out);
        assert empty.remove(n[5]) : "remove(n[5])";
        empty.dump(BasicIO.out);
        assert !empty.contains(n[5]) : "!contains(n[5])";
        assert empty.isEmpty() : "isEmpty";

        // now check duplicates
        empty.insert(n[5], n[5].origValue);
        empty.insert(na[5], na[5].origValue);
        empty.dump(BasicIO.out);
        checkDuplicates(empty.head);
        empty.insert(nb[5], nb[5].origValue);
        empty.dump(BasicIO.out);
        checkDuplicates(empty.head);
        assert empty.remove(n[5]) : "remove)(n[5])";
        assert empty.remove(na[5]) : "remove)(na[5])";
        assert empty.remove(nb[5]) : "remove)(nb[5])";
        assert empty.isEmpty() : "isEmpty";


        BasicIO.out.println("one element queue tests");

        // create one element queue
        resetOne();

        doUpdates(one, 5, vis, true);
        assert one.isEmpty() : "update one";
        resetOne();

        // insert at head of one element list
        one.insert(n[3], n[3].origValue);
        one.dump(BasicIO.out);
        assert one.head == n[3] : "head == n[3]";
        assert one.head.getNextDelta() == n[5] : "next == n[5]";
        assert one.head.getDelta() == n[3].origValue : "head delta = origValue";
        checkSums(one.head);

        // now look for each item and remove head
        assert one.contains(n[5]) : "contains(n[5])";
        assert one.contains(n[3]) : "contains(n[3])";
        assert one.remove(n[3]) : "remove(n[3])";
        assert one.contains(n[5]) : "contains(n[5])";
        assert !one.contains(n[3]) : "!contains(n[3])";
        one.dump(BasicIO.out);

        // now check duplicates of head
        one.insert(n[3], n[3].origValue);
        one.insert(na[3], na[3].origValue);
        one.dump(BasicIO.out);
        checkDuplicates(one.head);
        one.insert(nb[3], nb[3].origValue);
        one.dump(BasicIO.out);
        checkDuplicates(one.head);
        assert one.remove(n[3]) : "remove(n[3])";
        assert one.remove(na[3]) : "remove(na[3])";
        assert one.remove(nb[3]) : "remove(nb[3])";

        // now check duplicates of tail
        one.insert(n[3], n[3].origValue);
        one.insert(na[5], na[5].origValue);
        one.dump(BasicIO.out);
        checkDuplicates(one.head);
        one.insert(nb[5], nb[5].origValue);
        one.dump(BasicIO.out);
        checkDuplicates(one.head);
        assert one.remove(n[3]) : "remove(n[3])";
        assert one.remove(na[5]) : "remove(na[5])";
        assert one.remove(nb[5]) : "remove(nb[5])";


        // we're back at a one element queue, so insert at tail
        one.insert(n[7], n[7].origValue);
        one.dump(BasicIO.out);
        assert one.head == n[5] : "head == n[5]";
        assert one.head.getNextDelta() == n[7] : "next == n[7]";
        assert one.head.getDelta() == n[5].origValue : "head delta = origValue";
        checkSums(one.head);
        // now look for each item and remove tail
        assert one.contains(n[5]) : "contains(n[5])";
        assert one.contains(n[7]) : "contains(n[7])";
        assert one.remove(n[7]) : "remove(n[7])";
        one.dump(BasicIO.out);
        assert !one.contains(n[7]) : "!contains(n[7])";
        assert one.contains(n[5]) : "contains(n[5])";

        // no need for further duplicate tests 

        // clear 'one'
        assert one.remove(n[5]) && !one.contains(n[5]) : "clear n[5]";
        assert one.isEmpty() : "one empty";


        // now for a two element queue

        BasicIO.out.println("two element queue tests");        
        resetTwo();

        // insert at head of two element list
        two.insert(n[3], n[3].origValue);
        two.dump(BasicIO.out);
        assert two.head == n[3] : "head == n[3]";
        assert two.head.getNextDelta() == n[5] : "next == n[5]";
        assert two.head.getNextDelta().getNextDelta() == n[7] : "next.next == n[7]";
        assert two.head.getDelta() == n[3].origValue : "head delta = origValue";
        checkSums(two.head);

        // now look for each item and remove head
        assert two.contains(n[5]) : "contains(n[5])";
        assert two.contains(n[7]) : "contains(n[7])";
        assert two.contains(n[3]) : "contains(n[3])";
        assert two.remove(n[3]) : "remove(n[3])";
        checkSums(two.head);
        assert two.contains(n[5]) : "contains(n[5])";
        assert two.contains(n[7]) : "contains(n[7])";
        assert !two.contains(n[3]) : "!contains(n[3])";
        two.dump(BasicIO.out);

        // put back new head to check duplicates

        two.insert(n[3], n[3].origValue);

        // now check duplicates of head
        two.insert(na[3], na[3].origValue);
        two.dump(BasicIO.out);
        checkDuplicates(two.head);
        two.insert(nb[3], nb[3].origValue);
        two.dump(BasicIO.out);
        checkDuplicates(two.head);
        assert two.remove(na[3]) : "remove(na[3])";
        assert two.remove(nb[3]) : "remove(nb[3])";

        // now check duplicates of tail
        two.insert(na[7], na[7].origValue);
        two.dump(BasicIO.out);
        checkDuplicates(two.head);
        two.insert(nb[7], nb[7].origValue);
        two.dump(BasicIO.out);
        checkDuplicates(two.head);
        assert two.remove(na[7]) : "remove(na[7])";
        assert two.remove(nb[7]) : "remove(nb[7])";

        // now check duplicates of middle
        two.insert(na[5], na[5].origValue);
        two.dump(BasicIO.out);
        checkDuplicates(two.head);
        two.insert(nb[5], nb[5].origValue);
        two.dump(BasicIO.out);
        checkDuplicates(two.head);
        assert two.remove(na[5]) : "remove(na[5])";
        assert two.remove(nb[5]) : "remove(nb[5])";

        // return to original queue
        assert two.remove(n[3]) : "remove(n[3])";
        assert two.head == n[5] && two.head.getNextDelta() == n[7] : "reset two";

        // we're back at a two element queue, so insert at tail
        two.insert(n[9], n[9].origValue);
        two.dump(BasicIO.out);
        assert two.head == n[5] : "head == n[5]";
        assert two.head.getNextDelta() == n[7] : "next == n[7]";
        assert two.head.getNextDelta().getNextDelta() == n[9] : "next.next == n[9]";
        assert two.head.getDelta() == n[5].origValue : "head delta = origValue";
        checkSums(two.head);

        // now look for each item and remove tail
        assert two.contains(n[5]) : "contains(n[5])";
        assert two.contains(n[7]) : "contains(n[7])";
        assert two.contains(n[9]) : "contains(n[9])";
        assert two.remove(n[9]) : "remove(n[9])";
        two.dump(BasicIO.out);
        assert !two.contains(n[9]) : "!contains(n[9])";
        checkSums(two.head);
        assert two.contains(n[5]) : "contains(n[5])";
        assert two.contains(n[7]) : "contains(n[7])";

        // back at two element queue so insert in middle

        two.insert(n[6], n[6].origValue);
        two.dump(BasicIO.out);
        assert two.head == n[5] : "head == n[5]";
        assert two.head.getNextDelta() == n[6] : "next == n[6]";
        assert two.head.getNextDelta().getNextDelta() == n[7] : "next.next == n[7]";
        assert two.head.getDelta() == n[5].origValue : "head delta = origValue";
        checkSums(two.head);

        // now look for each item and remove middle
        assert two.contains(n[5]) : "contains(n[5])";
        assert two.contains(n[7]) : "contains(n[7])";
        assert two.contains(n[6]) : "contains(n[6])";
        assert two.remove(n[6]) : "remove(n[6])";
        checkSums(two.head);
        two.dump(BasicIO.out);
        assert !two.contains(n[6]) : "!contains(n[6])";
        assert two.contains(n[5]) : "contains(n[5])";
        assert two.contains(n[7]) : "contains(n[7])";

        // now do some updates

        // First enough to remove head
        doUpdates(two, 5, vis, true);
        assert (two.head == n[7] && 
                two.head.getDelta() == n[7].origValue-5):
	    "update to remove head";
        // now enough to remove last element too
        doUpdates(two, 5, vis, false); // 5 is more than we need
        assert two.isEmpty() : "empty after update";

        resetTwo();

        // insert duplicate at head
        two.insert(na[5], na[5].origValue);

        // now update to remove first two values together.
        doUpdates(two, 5, vis, true);
        assert (two.head == n[7] && 
                two.head.getDelta() == n[7].origValue-5):
	    "update to remove head";

        // clear queue
        assert two.remove(n[7]) && two.isEmpty() : "clearing two";

        // now set up two entries with same delta
        two.insert(n[2], n[2].origValue); 
        two.insert(na[2], na[2].origValue);
        checkSums(two.head);

        // now update to remove both together, leaving queue empty
        doUpdates(two,2,vis, true);
        assert two.isEmpty() : "two empty";


        // now do some bulk tests
        BasicIO.out.println("Doing Bulk Tests");
        for (int i = 1; i <= 10;i++) {
            resetTen();
            ten.update(i, vis);
            emptyTen();
        }

        // now do random stress test
        final SingleLinkDeltaQueue q = new SingleLinkDeltaQueue();
        int nElems = 10000;
        int minVal = 10;
        int maxVal = 100;
        int updateInterval = 50;
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < nElems; i++) {
            TestNode nd = new TestNode(rand.nextInt(maxVal-minVal)+minVal);
            q.insert(nd, nd.origValue);
            assert q.contains(nd) : "contains(n)";
            if (i % updateInterval == 0) {
                q.update(rand.nextInt(maxVal/4)+1, vis);
                q.dump(BasicIO.out);
            }
            if (rand.nextInt(10) == 5) { // randomly remove
                q.remove(nd);
                assert !q.contains(nd) : "!contains(n)";
            }
        }
        // now empty it
        while (!q.isEmpty()) {
            q.update(rand.nextInt(maxVal)+1, vis);
            q.dump(BasicIO.out);
        }

        BasicIO.err.println("Tests Complete");        
    }


    // perform update son the given queue, the given number of times
    static void doUpdates(SingleLinkDeltaQueue queue, int count, Visitor v, boolean reset) {
        if (reset) {
            counter = 0; // reset global counter
        }
        BasicIO.out.println("Doing updates:");
        for(int i = 1; i <= count; i++) {
            int nd = queue.update(1, v);
            counter++;
            assert checkSums(queue.head,counter);
            BasicIO.out.print("\t"+ i + ") [" + nd + "] ");
            queue.dump(BasicIO.out);
        }
    }

    // check relationship between deltas. counter tracks the number of updates
    // that have been done.
    static boolean checkSums(SingleLinkDeltaElement head, int cnter) {
        int sum = 0;
        for (TestNode curr = (TestNode) head; 
             curr != null;
             curr = (TestNode)curr.getNextDelta() ) {
            sum += curr.getDelta();
	    assert sum + cnter == curr.origValue:
                "sum = " +  sum + " counter = " + cnter +
		" orig = " +  curr.origValue;
        }
	return true;
    }

    // as above but works on queues for which update has not occurred yet
    static boolean checkSums(SingleLinkDeltaElement head) {
        return checkSums(head, 0);
    }

    // check the order of duplicate entries
    static boolean checkDuplicates(SingleLinkDeltaElement head) {
        for (TestNode curr = (TestNode) head; 
             curr != null;
             curr = (TestNode)curr.getNextDelta() ) {
	    assert (curr.getNextDelta() == null ||
                    curr.getDelta() != curr.getNextDelta().getDelta() ||
                    curr.id < ((TestNode)curr.getNextDelta()).id):
		"wrong order: " +  curr + ", " + curr.getNextDelta();
        }
	return true;
    }
}


