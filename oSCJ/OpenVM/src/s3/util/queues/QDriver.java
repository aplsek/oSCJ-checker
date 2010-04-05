package s3.util.queues;

import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
/**
 * Test program for SingleLinkQueue
 *
 */
// not public because we don't want it in javadoc
class QDriver {

    static class TestNode extends SingleLinkNode {

        static int nextId;
        public final int id;

        public TestNode() {
            synchronized(TestNode.class) {
                id = nextId++;
            }
        }

        public String toString() {
            return "(id=" + id + ")";
        }
    }

    // set of test nodes

    static TestNode[] n = {
        new TestNode(),
        new TestNode(),
        new TestNode(),
        new TestNode(),
        new TestNode(),
        new TestNode(),
        new TestNode(),
        new TestNode(),
        new TestNode(),
        new TestNode(),
    };

    public static void main(String[] args) {

        BasicIO.out.println("Starting Tests");
        // start with empty queue
        SingleLinkQueue q = new SingleLinkQueue();
        assert q.isEmpty() && q.head()==null && q.tail()==null && q.size() == 0  : "isEmpty";
        for (int i = 0; i < n.length; i++) {
            assert !q.contains(n[i]) && !q.remove(n[i])  : "element found";
        }

        // now add elements checking the order etc
        for (int i = 0; i < n.length; i++) {
            q.add(n[i]);
            q.dump(BasicIO.out);
            assert q.size() == (i+1) : "size";
            assert !q.isEmpty()  : "not empty";
            assert q.tail() == n[i]  : "tail";
            assert q.head() == n[0]  : "head unchanged";
            assert q.head() != n[i] || q.head() == q.tail()  : "not head";
            assert q.contains(n[i])  : "contains";
            // check order
            for (SingleLinkElement curr = q.head;
                 curr != null;
                 curr = curr.getNext()) {
                if (curr.getNext() != null) {
                    assert  ((TestNode)curr).id + 1 == ((TestNode)curr.getNext()).id  : "order";
                }
            }
        }

        // now take and check
        for (int i = 0; i < n.length; i++) {
            SingleLinkElement t1 = q.head();
            SingleLinkElement t2 = q.take();
            q.dump(BasicIO.out);
            assert q.size() == (n.length - (i+1)) : "size";
            assert t1 == t2 && t2 == n[i]  : "head removed";
            assert !q.contains(n[i])  : "not contains";
            for (int j = i+1; j < n.length; j++) {
                assert q.contains(n[j])  : "contains others";
            }
            assert q.tail() == (i == n.length-1 ? null : n[n.length-1])  : "tail unchanged";
        }

        assert q.isEmpty() && q.head()==null && q.tail()==null && q.size()==0  : "isEmpty";


        // finally test remove: head, tail, middle etc
        for (int i = 0; i < n.length; i++) {
            q.add(n[i]);
        }

        int[] indices = { 0, // head
                          9, // tail
                          5, // middle
                          3, // middle
                          7, // middle
                          1, // head
                          8, // tail
                          4, // middle
                          2, // head
                          6, // tail
        };
        assert n.length == indices.length  : "arrays";
        for (int i = 0; i < n.length; i++) {
            assert q.contains(n[indices[i]])  : "contains";
            assert q.remove(n[indices[i]])  : "remove";
            assert q.size() == (n.length - (i+1)) : "size";
            assert !q.contains(n[indices[i]])  : "!contains";
            for (int j = i+1; j < n.length; j++) {
                assert q.contains(n[indices[j]])  : "contains others";
            }
        }
        assert q.isEmpty() && q.head()==null && q.tail()==null  : "isEmpty";


        BasicIO.out.println("Tests Completed");
    }
}




