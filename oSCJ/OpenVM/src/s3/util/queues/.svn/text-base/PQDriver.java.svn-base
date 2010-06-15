package s3.util.queues;

import java.util.Comparator;

import ovm.core.OVMBase;
import ovm.core.services.io.BasicIO;
/**
 * Test program for SingleLinkPriorityQueue
 *
 */
// not public because we don't want it in the javadoc
class PQDriver {

    static class TestNode extends SingleLinkNode {

        static int nextId;
        public final int id;
        public int val;

        public TestNode(int val) {
            synchronized(TestNode.class) {
                id = nextId++;
            }
            this.val = val;
        }

        public String toString() {
            return "(id=" + id + ", val=" + val + ")";
        }
    }

    // our comparator: higher val == higher priority
    static Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((TestNode)o1).val - ((TestNode)o2).val;
            }
        };


    // set of test nodes

    static TestNode[] n = {
        new TestNode(0),
        new TestNode(1),
        new TestNode(2),
        new TestNode(3),
        new TestNode(4),
        new TestNode(5),
        new TestNode(6),
        new TestNode(7),
        new TestNode(8),
        new TestNode(9),
    };

    // duplicates 
    static TestNode[] na = {
        new TestNode(0),
        new TestNode(1),
        new TestNode(2),
        new TestNode(3),
        new TestNode(4),
        new TestNode(5),
        new TestNode(6),
        new TestNode(7),
        new TestNode(8),
        new TestNode(9),
    };

    // duplicates 
    static TestNode[] nb = {
        new TestNode(0),
        new TestNode(1),
        new TestNode(2),
        new TestNode(3),
        new TestNode(4),
        new TestNode(5),
        new TestNode(6),
        new TestNode(7),
        new TestNode(8),
        new TestNode(9),
    };

    public static void main(String[] args) {

        BasicIO.out.println("Starting Tests");
        // start with empty queue
        SingleLinkPriorityQueue q = new SingleLinkPriorityQueue(comp);
        assert q.isEmpty() && q.head()==null && q.tail()==null && q.size()==0  : "isEmpty";
        for (int i = 0; i < n.length; i++) {
            assert !q.contains(n[i]) && !q.remove(n[i])  : "element found";
        }

        BasicIO.out.println("Adding elements in order");
        // now add elements in order ie we know each new element will go at
        // the head
        for (int i = 0; i < n.length; i++) {
            q.add(n[i]);
//            q.dump(BasicIO.out);
            assert q.size() == (i+1) : "size";
            assert !q.isEmpty()  : "not empty";
            assert q.head() == n[i]  : "head";
            assert q.tail() == n[0]  : "tail unchanged";
            assert q.contains(n[i])  : "contains";
            checkOrder(q, true); // values differ by 1
        }

        BasicIO.out.println("take()ing elements");
        // now take and check
        for (int i = n.length-1; i >= 0; i--) {
            SingleLinkElement t1 = q.head();
            SingleLinkElement t2 = q.take();
//            q.dump(BasicIO.out);
            assert q.size() == i : "size";
            assert t1 == t2 && t2 == n[i]  : "head removed";
            assert !q.contains(n[i])  : "not contains";
            for (int j = 1; j < i; j++) {
                assert q.contains(n[j])  : "contains others";
            }
            assert q.tail() == (i == 0 ? null : n[0])  : "tail unchanged";
        }

        assert q.isEmpty() && q.head()==null && q.tail()==null && q.size()==0 : "isEmpty";

        BasicIO.out.println("Adding in specific positions");
        // now insert elements into different positions

        q.add(n[5]);  // starting element
        assert q.size() == 1 : "size";
        assert q.head()==n[5]  : "head == n[5]";
        assert q.tail()==n[5]  : "tail == n[5]";               
        assert q.contains(n[5])  : "contains";
        checkOrder(q); // values monotonic decreasing
        q.add(n[7]); // add at head
        assert q.size() == 2 : "size";
        assert q.head()==n[7]  : "head == n[7]";        
        assert q.tail()==n[5]  : "tail == n[5]";        
        assert q.contains(n[7])  : "contains";
        checkOrder(q);
        q.add(n[3]); // add at tail
        assert q.size() == 3 : "size";
        assert q.head()==n[7]  : "head == n[7]";        
        assert q.tail()==n[3]  : "tail == n[3]";        
        assert q.contains(n[3])  : "contains";
        checkOrder(q);
        q.add(n[6]); // add in middle
        assert q.size() == 4 : "size";
        assert q.head()==n[7]  : "head == n[7]";        
        assert q.tail()==n[3]  : "tail == n[3]";        
        assert q.contains(n[6])  : "contains";
        checkOrder(q);
        q.add(n[4]); // add in middle
        assert q.size() == 5 : "size";
        assert q.head()==n[7]  : "head == n[7]";        
        assert q.tail()==n[3]  : "tail == n[3]";        
        assert q.contains(n[4])  : "contains";
        checkOrder(q);
        q.add(n[1]); // add tail
        assert q.size() == 6 : "size";
        assert q.head()==n[7]  : "head == n[7]";        
        assert q.tail()==n[1]  : "tail == n[1]";        
        assert q.contains(n[1])  : "contains";
        checkOrder(q);
        q.add(n[9]); // add head
        assert q.size() == 7 : "size";
        assert q.head()==n[9]  : "head == n[9]";        
        assert q.tail()==n[1]  : "tail == n[1]";        
        assert q.contains(n[9])  : "contains";
        checkOrder(q);
        q.add(n[8]); // add in middle
        assert q.size() == 8 : "size";
        assert q.head()==n[9]  : "head == n[9]";        
        assert q.tail()==n[1]  : "tail == n[1]";        
        assert q.contains(n[8])  : "contains";
        checkOrder(q);
        q.add(n[2]); // add in middle
        assert q.size() == 9 : "size";
        assert q.head()==n[9]  : "head == n[9]";        
        assert q.tail()==n[1]  : "tail == n[1]";        
        assert q.contains(n[2])  : "contains";
        checkOrder(q);
        q.add(n[0]); // add itail
        assert q.size() == 10 : "size";
        assert q.head()==n[9]  : "head == n[9]";        
        assert q.tail()==n[0]  : "tail == n[0]";        
        assert q.contains(n[0])  : "contains";
        checkOrder(q);


        BasicIO.out.println("inserting duplicates");
        // now insert the duplicates, checking that they go in FIFO
        // ie. the lowest id should be first
        for (int i = 0; i < na.length; i++) {
            q.add(na[i]);
            assert q.size() == 10+(i+1) : "size";
            assert q.contains(na[i])  : "contains";
            checkOrder(q);
        }

        for (int i = 0; i < nb.length; i++) {
            q.add(nb[i]);
            assert q.size() == 10+na.length+(i+1) : "size";
            assert q.contains(nb[i])  : "contains";
            checkOrder(q);
        }

        BasicIO.out.println("removing all elements");
        // now pull the queue apart again using remove
        for (int i = 0; i < n.length; i++) {
            assert q.remove(n[i]) && !q.contains(n[i])  : "removing";
            assert q.size() == 10+na.length+nb.length-(i*3+1) : "size";
            checkOrder(q);
            assert q.remove(na[i]) && !q.contains(na[i])  : "removing";
            assert q.size() == 10+na.length+nb.length-(i*3+2) : "size";
            checkOrder(q);
            assert q.remove(nb[i]) && !q.contains(nb[i])  : "removing";
            assert q.size() == 10+na.length+nb.length-(i*3+3) : "size";
            checkOrder(q);
        }
        assert q.isEmpty() && q.size()==0 : "empty";


        BasicIO.out.println("Refilling");        
        // Now  test remove in a more controlled way : head, tail, middle etc
        for (int i = 0; i < n.length; i++) {
            q.add(n[i]);
            checkOrder(q);
        }
        assert q.size() == n.length : "size";
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
        BasicIO.out.println("controlled removal");
        for (int i = 0; i < n.length; i++) {
            assert q.contains(n[indices[i]])  : "contains";
            assert q.remove(n[indices[i]])  : "remove";
            assert q.size() == n.length-(i+1) : "size";
            assert !q.contains(n[indices[i]])  : "!contains";
            for (int j = i+1; j < n.length; j++) {
                assert q.contains(n[indices[j]])  : "contains others";
            }
        }
        assert q.isEmpty() && q.head()==null && q.tail()==null && q.size()==0 : "isEmpty";

        // Now test merging off queues.
        BasicIO.out.println("Doing merge tests");
        mergeTests();
        BasicIO.out.println("Tests Completed");
    }


    private static void mergeTests() {
        /* Test coverage here requires testing a range of sizes for the source
           queue with a range of sizes with the target queue. So we'll iterate
           through two queue creation loops. We'll use random creation of 
           elements, but check that we test the interesting cases: merging with
           a queue that is strictly greater or one that is strictly less.

           FIX ME: this needs to be more structured to test all cases in the
                   code. In particular we are not testing merging with 
                   different comparators. DH - Jan 17, 2003
        */
        java.util.Random rand = new java.util.Random();
        int maxIters = 50; // random merges
        int maxSize = 10;  // largest queue to use

        SingleLinkPriorityQueue a = null;  // the main queue
        SingleLinkPriorityQueue b = null;  // the other queue

        int size_a = -1;
        int size_b = -1;

        for (int asize = 0; asize <= maxSize; asize++) {
            BasicIO.out.println("a size = " + asize);

            // now create different size queues to merge with this size

            for (int bsize = 0; bsize <= maxSize; bsize++) {
                BasicIO.out.println("\tb size = " + bsize);

                /* NOTE: *VERY IMPORTANT*
                   Queue A must be filled first so that any queue B
                   elements with the same priority as one in A will have
                    a larger id (and hence are 'younger'). This will comply
                   with the assertion in checkOrder.
                */

                // we need to cover certain cases as well as general testing
                // so we create other queues with certain characteristics, then
                // ones with random contents.

                // for each element of A create a queue B such that merging
                // with B will put all of B before that element of A - this
                // is done with no duplicates in A to make it easier
                // This covers the strictly greater case

                int spacer = 5; // spread out priority values for easy insert
                for (int i = 0; i < asize; i++) {
                    BasicIO.out.println("testing merging before element["+(asize-1-i)+"]");
                    a = new SingleLinkPriorityQueue(comp);
                    // fill in elements of distinct priorities
                    for (int j = 0; j < asize && j < n.length; j++) {
                        a.add(new TestNode(j*spacer)); // space out priorities
                        checkOrder(a);
                    }
                    BasicIO.out.print("Queue A: "); a.dump(BasicIO.out);

                    // now create B and fill with elements whose priority is
                    // between the low and high boundaries.

                    b = new SingleLinkPriorityQueue(comp);
                    for (int j = 0; j < bsize; j++) {
                        TestNode nd = new TestNode(rand.nextInt(spacer-1)+(spacer*i)+1);
                        assert nd.val > i*spacer && nd.val < (i+1)*spacer  : "priority range";
                        b.add(nd);
                        checkOrder(b);
                    }
                    BasicIO.out.print("Queue B: "); b.dump(BasicIO.out);

                    // now merge
                    size_a = a.size();
                    size_b = b.size();
                    a.merge(b);
                    BasicIO.out.print("Merged A: "); a.dump(BasicIO.out);
                    if (asize > 0 || bsize > 0) {
                        assert !a.isEmpty(): "a empty after merge";
                    }
                    checkOrder(a);
                    assert a.size() == size_a+size_b:
			"size: " + a.size() + " != " + size_a + " + " + size_b;
                    assert b.isEmpty() && b.size()==0: "b empty";
                }

                // now do the same test as above but after the last element, so
                // that we test the strictly less case
                BasicIO.out.println("Testing merging after last element");
                a = new SingleLinkPriorityQueue(comp);
                // fill in elements of distinct priorities
                for (int j = 0; j < asize && j < n.length; j++) {
                    a.add(new TestNode(j));
                    checkOrder(a);
                }
                BasicIO.out.print("Queue A: "); a.dump(BasicIO.out);

                // now create B and fill with elements whose priority is
                // lower than 0

                b = new SingleLinkPriorityQueue(comp);
                for (int j = 0; j < bsize; j++) {
                    b.add(new TestNode(rand.nextInt(bsize)-bsize));
                    checkOrder(b);
                }
                BasicIO.out.print("Queue B: "); b.dump(BasicIO.out);

                // now merge
                size_a = a.size();
                size_b = b.size();
                a.merge(b);
                BasicIO.out.print("Merged A: "); a.dump(BasicIO.out);
                if (asize > 0 || bsize > 0) {
                    assert !a.isEmpty()  : "a not empty after merge";
                }
                checkOrder(a);
                assert a.size() == size_a+size_b : "size";
                assert b.isEmpty() && b.size()==0  : "b empty";


                
                // random merging
                BasicIO.out.println("Doing random merge tests");

                // to ensure duplicates make sure prio range -> 0 -  size/2
                for(int i = 0; i < maxIters ; i++) {
                    BasicIO.out.println("\t\t iteration= " + i);
                    // create both queues and fill in with random elements

                    a = new SingleLinkPriorityQueue(comp);
                    // fill in elements with random priorities
                    for (int j = 0; j < asize; j++) {
                        a.add(new TestNode(rand.nextInt(asize/2+2)));
                        checkOrder(a);
                    }
                    BasicIO.out.print("Queue A: "); a.dump(BasicIO.out);
                    b = new SingleLinkPriorityQueue(comp);
                    // fill in elements with random priorities
                    for (int j = 0; j < bsize; j++) {
                        b.add(new TestNode(rand.nextInt(bsize/2+2)));
                        checkOrder(b);
                    }
                    BasicIO.out.print("Queue B: "); b.dump(BasicIO.out);
                    // now merge
                    size_a = a.size();
                    size_b = b.size();
                    a.merge(b);
                    BasicIO.out.print("Merged A: "); a.dump(BasicIO.out);
                    if (asize > 0 || bsize > 0) {
                        assert !a.isEmpty()  : "a not empty after merge";
                    }
                    checkOrder(a);
                    assert a.size() == size_a+size_b : "size";
                    assert b.isEmpty() && b.size()==0  : "b empty";

                }
            }
        }
    }


    private static void checkOrder(SingleLinkPriorityQueue q) {
        checkOrder(q,false);
    }
    private static void checkOrder(SingleLinkPriorityQueue q, boolean strict) {
        for (SingleLinkElement curr = q.head;
             curr != null;
             curr = curr.getNext()) {
            if (curr.getNext() != null) {
                boolean diffByOne = ((TestNode)curr).val - 1 == ((TestNode)curr.getNext()).val;
                boolean equalOlder = ((TestNode)curr).val == ((TestNode)curr.getNext()).val && ((TestNode)curr).id < ((TestNode)curr.getNext()).id;
                boolean inOrder = ((TestNode)curr).val > ((TestNode)curr.getNext()).val;
                if (strict) { // differ by one
                    assert  diffByOne || equalOlder  : "order";
                }
                else { // monotonic decreasing
                    assert inOrder || equalOlder  : "order";
                }

            }
        }
    }

}




