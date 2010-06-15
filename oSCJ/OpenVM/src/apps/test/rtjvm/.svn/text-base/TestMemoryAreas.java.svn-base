package test.rtjvm;

import java.lang.reflect.Constructor;
import javax.realtime.HeapMemory;
import javax.realtime.IllegalAssignmentError;
import javax.realtime.ImmortalMemory;
import javax.realtime.InaccessibleAreaException;
import javax.realtime.LTMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.realtime.ScopedMemory;
import javax.realtime.ThrowBoundaryError;

import test.jvm.TestBase;
import test.jvm.TestSuite;

public class TestMemoryAreas extends TestBase {
    // In j2c, we easily have room for an IllegalAssignmentError in a
    // 1k area, but dropping this down to 512 bytes produces a
    // recursive out-of-memory
    static final boolean FORCE_OUT_OF_MEMORY = false;
    static final int SMALL_AREA = (FORCE_OUT_OF_MEMORY
				   ? 512
				   : (50 * 1024));

    public TestMemoryAreas(Harness h, long flags) {
	super("scoped memory support", h);
	this.flags = flags;
    }

    long flags;
    
    public void run() {
        testMemoryUsage();
	if ((flags & test.rtjvm.TestSuite.NO_GC) == 0) {
	    testHeapRefsFrom(ImmortalMemory.instance());
	    testHeapRefsFrom(new LTMemory(SMALL_AREA, SMALL_AREA));
	}
	testScopeReuse();
	testScopeStoreTo(HeapMemory.instance());
	testScopeStoreTo(ImmortalMemory.instance());
	testFinalizers();
	testClone();
	testArraycopy();
	testPAR();
	testScopeStackInheritence();

	setModule("String.intern()");
	new LTMemory(SMALL_AREA, SMALL_AREA).enter(stringInternTester);
	if ((flags & test.rtjvm.TestSuite.KNOWN_FAILURES) != 0)
	    testStringAliasing();

	if (false)
	    // Assumption holds with MostlyCopyingRegions, but not
	    // MostlyCopyingSplitRegions
	    testTooBigArea();
	new LTMemory(SMALL_AREA, SMALL_AREA).enter(testLookup);
        //ma.run();
    }

    void testScopeStackInheritence() {
	setModule("scope stack inheritence");
	final MemoryArea outer = new LTMemory(SMALL_AREA, SMALL_AREA);
	outer.enter(new Runnable() {
		public void run() {
		    final Object outerObj = new Object();
		    final MemoryArea inner = 
			new LTMemory(SMALL_AREA, SMALL_AREA);
		    RealtimeThread rtt = 
			new RealtimeThread(null,null,null,inner,null,null) {
			    public void run() {
				check_condition(getCurrentMemoryArea() == inner,
						"thread initial area");
				Object _ = new Object[] { outerObj };
			    }
			};
		    rtt.start();
		    try {
			rtt.join();
		    } catch (InterruptedException e) {
			fail(e);
		    }
		}
	    });
    }

    void testPAR() {
	new LTMemory(10000, 10000).enter( new Runnable() {
	   public void run() {
	       // as long as this doesn't crash the VM.
	       try {
		   run2();
	       } catch (Exception e) {
		   System.err.println(e);
	       }
	   }
	   void run2() throws org.ovmj.transact.Atomic, Exception {
	       MemoryArea area = RealtimeThread.getCurrentMemoryArea();
	       area.newInstance(newCell, new Object[] { new Integer(0), new Integer(0) });
	       org.ovmj.transact.Transaction.undo();
	   }
	});
    }

    void testMemoryUsage() {

        // not so much a test as a demonstration that reasonable values
        // get returned:

        final int nElems = 10;

        final MemoryArea[] areas = new MemoryArea[] {
            ImmortalMemory.instance(),
            HeapMemory.instance(),
            new LTMemory(10000, 10000),
        };

        final long[] sizes = new long[areas.length];
        final long[][] cons = new long[areas.length][nElems];
        final long[][] rem = new long[areas.length][nElems];

        for (int i = 0; i < areas.length; i++) {
            System.out.println("About to enter " + areas[i]);
            final int ii = i;
            areas[i].enter( new Runnable() {
                    public void run() {
                        MemoryArea area = RealtimeThread.getCurrentMemoryArea();
			check_condition(area == areas[ii], "enter broken");
                        sizes[ii] = area.size();
                        for (int j = 0; j < nElems; j++) {
                            cons[ii][j] = area.memoryConsumed();
                            rem[ii][j] = area.memoryRemaining();
                            new Object();
                        }
                    }
                });
        }

        for (int i = 0; i < areas.length; i++) {
            System.out.println("Statistics for area " + areas[i].toString());
            System.out.println("\t - original size = " + sizes[i]);
            for (int j = 0; j < nElems; j++) {
                System.out.println("\t - consumed = " + cons[i][j] +
                                   " remaining = " + rem[i][j] +
                                   " diff = " + (j==0 ? 0 : (cons[i][j] - cons[i][j-1])));
            }
        }
    }
                
    /*
     * Test references to the heap from immortal/scoped memory.  We
     * allocate a 100 element linked list on the heap that is only
     * referenced from another memory area.  We then GC twice,
     * allocate a second 100 element list, and verify that the first
     * list is intact.  We have to be very careful that the first list
     * isn't reachable from the stack, especially in a conservative
     * context!
     */

    static final int LIST_LENGTH = 100;
    static Constructor newCell;
    static {
	try {
	    newCell = Cell.class.getDeclaredConstructor(new Class[] {
		int.class, int.class
	    });
	}
	catch (Exception e ) { throw new RuntimeException(e); }
    }
    int seqno = 0;

    public static class Cell {
	public int seqno_;
	Cell next;

	Cell(int seqno, int count) {
	    this.seqno_ = seqno;
	    if (count > 1)
		next = new Cell(seqno + 1, count - 1);
	}
    }

    Runnable heapAllocator = new Runnable() {
	    int startSeq;
	
	    Cell getCells() {
		try {
		    Object ret = HeapMemory.instance().newInstance
			(newCell, new Object[] {  new Integer(seqno),
						  new Integer(LIST_LENGTH) });
		    seqno += LIST_LENGTH;
		    return (Cell) ret;
		}
		catch (RuntimeException e) { throw e; }
		catch (Exception e) { throw new RuntimeException(e); }
	    }

	    Cell[] saveList() {
		startSeq = seqno;
		return new Cell[] { getCells() };
	    }

	    void checkList(Cell[] ref) {
		int i = startSeq;
		int end = i + LIST_LENGTH;
		Cell l = ref[0];
	    
		while (l != null) {
		    if (l.seqno_ != i)
			fail("bad heap-allocated list");
		    i++;
		    l = l.next;
		}
		if (i != end)
		    fail("bad heap-allocated list");
	    }
	
	    public void run() {
		Cell[] ref = saveList();
		System.gc();
		checkList(ref);
		System.gc();
		checkList(ref);
		getCells();
		System.gc();
		checkList(ref);
	    }
	};

    void testHeapRefsFrom(MemoryArea ma) {
	setModule((ma instanceof ImmortalMemory ? "immortal" : "scoped")
		  + " references into the heap");
	ma.enter(heapAllocator);
    }

    /*
     * Scope reuse test.  Repeatedly enter a scoped memory area and
     * allocate more than half the available memory.  We want to be
     * sure that scoped memory area is no larger than we request.  In
     * the mostlyCopying implementation the minimum scope size is
     * generally between 256 bytes and one page.  In the JMTk
     * implementation, the minimum scope size may well be the JMTk
     * chunk size, which I beleive is 16k.
     */

    static final int IN_SCOPE_ITERATIONS = 10;
    static final int REUSE_SCOPE_SIZE = (1 << 15) * IN_SCOPE_ITERATIONS;
    static final int REUSE_ARRAY_SIZE = (1 << 13) - (1 << 8);

    Runnable scopeAllocator = new Runnable() {
	    public void run() {
                for (int n=IN_SCOPE_ITERATIONS;n-->0;) {
                    int[] _ = new int[REUSE_ARRAY_SIZE];
                    for (int i=0;i<REUSE_ARRAY_SIZE;++i) {
                        check_condition(_[i]==0);
                    }
                }
	    }
	};

    void testScopeReuse() {
	setModule("scope reuse");
	MemoryArea ma = new LTMemory(REUSE_SCOPE_SIZE, REUSE_SCOPE_SIZE);
	ma.enter(scopeAllocator);
	ma.enter(scopeAllocator);
	ma.enter(scopeAllocator);
	ma.enter(scopeAllocator);
    }

    static class Box {
	Object val;
    }
    
    void testScopeStoreTo(final MemoryArea outer) {
	setModule("store scoped reference in "
		  + (outer instanceof HeapMemory ? "heap" : "immortal"));
	final MemoryArea inner = new LTMemory(SMALL_AREA, SMALL_AREA);
	try {
	    outer.executeInArea(new Runnable() {
		    public void run() {
			MemoryArea cur = RealtimeThread.getCurrentMemoryArea(); 
			check_condition(cur == outer, "executiveInArea broken");
			final Box b = new Box();
			inner.enter(new Runnable() {
				public void run() {
				    try {
					b.val = new Object();
					fail("putfield " + inner
						 + " in " + outer
						 + " successful");
				    } catch (IllegalAssignmentError _) { }
                                    // now try via reflection
                                    if (false)
                                        try {
                                            java.lang.reflect.Field f = 
                                                Box.class.getDeclaredField("val");
                                            f.set(b, new Object());
                                            fail("Field.set " + inner
						 + " in " + outer
						 + " successful");
                                        } 
                                        catch (IllegalAssignmentError _) { }
                                        catch (Exception e) {
                                            throw (Error) new Error().initCause(e);
                                        }
                                    
				}
			    });
		    
			final Object[] o = new Object[1];
			inner.enter(new Runnable() {
				public void run() {
				    try {
					o[0] = new Object();
					fail("aastore " + inner
						 + " in " + outer
						 + " successful");
				    } catch (IllegalAssignmentError _) { }
				}
			    });
		    }
		});
	} catch (InaccessibleAreaException e) {
	    throw new RuntimeException(e);
	}
    }

    static final Runnable stringInternTester = new Runnable() {
	    public void run() {
		(new String("this is ") + "a test").intern();
	    }
	};

    /**
     * Filip's own memory area test
     */

    // NOTE: this test is invalid with the RTSJ 1.0.1 semantics for
    //       RealtimeThread.getInitialMemoryAreaIndex() and the indexing
    //       scheme for the scope stack. The test assumes current allocation
    //       context is at index 0, while RTSJ 1.0.1 says it is at index
    //       stackDepth-1  - DH Aug 2005
    static class ma {
        
        static void f(String msg) {
            // not using COREfail because:
            // 1) COREfail and all of the other test harness crap is stupid
            // 2) COREfail results in reference operations that would break in scopes
            System.err.println("Failure: "+msg);
            System.exit(1);
        }
        
        static void a(String msg,
                      boolean b) {
            if (b==false) {
                f(msg);
            }
        }
        
        static class MyException extends RuntimeException {
            MyException() {
                super("My Exception!");
            }
        }
        
        static String th(int i) {
            switch (i%10) {
                case 1:
                    return ""+i+"st";
                case 2:
                    return ""+i+"nd";
                case 3:
                    return ""+i+"rd";
                default:
                    return ""+i+"th";
            }
        }
        
        static void checks(String text,
                           MemoryArea[] mas,
                           String[] maNames,
                           int initAreaIndex) {
            a("checking that we are in "+maNames[maNames.length-1]+" when we are in a "+text,
              RealtimeThread.getCurrentMemoryArea()==mas[mas.length-1]);
            
            a("checking that the scope stack height of a "+text+" is "+mas.length,
              RealtimeThread.getMemoryAreaStackDepth()==mas.length);
            
            a("checking that the initial ma index of a "+text+" is "+initAreaIndex,
              RealtimeThread.getInitialMemoryAreaIndex()==initAreaIndex);
            
            a("checking that the initial ma of a "+text+" is "+
              maNames[maNames.length-initAreaIndex-1],
              RealtimeThread.getOuterMemoryArea(
                  RealtimeThread.getInitialMemoryAreaIndex())
              ==mas[mas.length-initAreaIndex-1]);
            
            for (int i=0;i<mas.length;++i) {
                a("checking that the "+th(i)+" outer memory area of a "+text+
                  " is "+maNames[maNames.length-i-1],
                  RealtimeThread.getOuterMemoryArea(i)==mas[mas.length-i-1]);
            }
            
            a("checking the memory area of an object allocated in a "+text,
              MemoryArea.getMemoryArea(new Object())==mas[mas.length-1]);
        }
        
        static void run() {
            try {
                checks("vanilla thread",
                       new MemoryArea[]{HeapMemory.instance()},
                       new String[]{"heap"},
                       0);
                
                ImmortalMemory.instance().executeInArea(new Runnable(){
                    public void run() {
                        checks("vanilla thread that did IM.execInArea()",
                               new MemoryArea[]{ImmortalMemory.instance()},
                               new String[]{"immortal"},
                               0);
                    }
                });
                
                RealtimeThread rt=new RealtimeThread(){
                    public void run() {
                        try {
                            checks("realtime thread with a default configuration",
                                   new MemoryArea[]{HeapMemory.instance()},
                                   new String[]{"heap"},
                                   0);
                            
                            ImmortalMemory.instance().enter(new Runnable(){
                                public void run() {
                                    checks("realtime thread with a default configuration that entered immortal",
                                           new MemoryArea[]{HeapMemory.instance(),
                                                            ImmortalMemory.instance()},
                                           new String[]{"heap", "immortal"},
                                           1);
                                    
                                    final LTMemory scope=new LTMemory(65536, 65536);
                                    scope.enter(new Runnable(){
                                        public void run() {
                                            checks("realtime thread with a default configuration that entered "+
                                                   "immortal and then a scope",
                                                   new MemoryArea[]{HeapMemory.instance(),
                                                                    ImmortalMemory.instance(),
                                                                    scope},
                                                   new String[]{"heap", "immortal", "scope"},
                                                   2);
                                        }
                                    });

                                    checks("realtime thread with a default configuration that entered immortal "+
                                           "and just exited a scope",
                                           new MemoryArea[]{HeapMemory.instance(),
                                                            ImmortalMemory.instance()},
                                           new String[]{"heap", "immortal"},
                                           1);
                                }
                            });

                            checks("realtime thread with a default configuration that just exited immortal",
                                   new MemoryArea[]{HeapMemory.instance()},
                                   new String[]{"heap"},
                                   0);
                        } catch (Throwable e) {
                            try {
                                System.err.println("ERROR IN THREAD: "+e.toString());
                            } catch (Throwable _) {
                                System.err.println("ERROR IN THREAD - unable to print error");
                            }
                            System.exit(1);
                        }
                    }
                };
                
                rt.start();
                rt.join();
                
                final LTMemory scope=new LTMemory(65536, 65536);
                final LTMemory scope2=new LTMemory(65536, 65536);
                rt=new RealtimeThread(null,null,null,scope,null,null){
                    public void run() {
                        try {
			    // Not using check_condition because this
			    // class is stupid.
			    a("initial area broken",
			      scope == getCurrentMemoryArea());
                            checks("realtime thread configured with an initial scope",
                                   new MemoryArea[]{HeapMemory.instance(), scope},
                                   new String[]{"heap", "scope"},
                                   0);
                            
                            try {
                                scope2.enter(new Runnable(){
                                    public void run() {
                                        checks("realtime thread configured with an initial scope that entered "+
                                               "another scope",
                                               new MemoryArea[]{HeapMemory.instance(), scope, scope2},
                                               new String[]{"heap", "scope", "scope2"},
                                               1);
                                        
                                        throw new MyException();
                                    }
                                });
                            } catch (ThrowBoundaryError e) {
                                // ok
                            } catch (MyException e) {
                                f("asserting that a TBE gets thrown");
                            }
                            
                            try {
                                scope2.enter(new Runnable(){
                                    public void run() {
                                        checks("realtime thread configured with an initial scope that entered "+
                                               "another scope",
                                               new MemoryArea[]{HeapMemory.instance(), scope, scope2},
                                               new String[]{"heap", "scope", "scope2"},
                                               1);
                                        
                                        try {
                                            scope.executeInArea(new Runnable(){
                                                public void run() {
                                                    throw new MyException();
                                                }
                                            });
                                        } catch (InaccessibleAreaException e) {
                                            f("got an InaccessibleAreaException when trying to do "+
                                              "scope.execInArea() from inside scope2");
                                        }
                                    }
                                });
                            } catch (MyException e) {
                                // ok
                            } catch (ThrowBoundaryError e) {
                                f("asserting that a TBE does not get thrown");
                            }
                        } catch (Throwable e) {
                            try {
                                System.err.println("ERROR IN THREAD: "+e.toString());
                            } catch (Throwable _) {
                                System.err.println("ERROR IN THREAD - unable to print error");
                            }
                            System.exit(1);
                        }
                    }
                };

                rt.start();
                rt.join();
                
            } catch (Throwable e) {
                System.err.println(e.toString());
                System.exit(1);
            }
        }
    }

    final void testStringAliasing() {
	setModule("alias string contents");
	new LTMemory(SMALL_AREA, SMALL_AREA).enter
	    (new Runnable() {
		    public void run() {
			final String s = new String("this is ") + "a test";
			try {
			    ImmortalMemory.instance().executeInArea
				(new Runnable() {
					public void run() {
					    new String(s);
					}
				    });
			} catch (InaccessibleAreaException e) {
			    fail("immortal inaccesible?");
			}
		    }
		});
    }

    
    static int finalizersRun;
    static final int N_FINALIZERS = 5;

    static class Finalizable {
	public void finalize() {
	    finalizersRun++;
	}
    }
    final Runnable finAllocator = new Runnable() {
	    public void run() {
		for (int i = 0; i < N_FINALIZERS; i++) {
		    new Finalizable();
		}
	    }
	};

    final void testFinalizers() {
	setModule("scoped finalizers");
	finalizersRun  = 0;
	new LTMemory(SMALL_AREA, SMALL_AREA).enter(finAllocator);
	check_condition(finalizersRun == N_FINALIZERS);
    }

    interface PubCloneable extends Cloneable {
	Object clone();
    }

    abstract class ImplicitWriteTester implements Runnable {
	void test(PubCloneable o) {
	    if (!(RealtimeThread.getCurrentMemoryArea() instanceof ScopedMemory))
		throw new IllegalAssignmentError();
	}
	abstract void test(Object[] o);
	abstract String kind();

	
	public void run() {
	    setModule(kind());
	    Runnable aTester = new Runnable() {
		    Object[] data = new Object[] { new Object() };
		    public void run() {
			test(data);
		    }
		};
	    Runnable sTester = new Runnable() {
		    PubCloneable data = new PubCloneable() {
			    public Object o = new Object();
			    public Object clone() {
				try { return super.clone(); }
				catch(CloneNotSupportedException _) {
				    fail("unexpected clone failure");
				}
				return null;
			    }
			};
		    public void run() {
			test(data);
		    }
		};
	    aTester.run();
	    sTester.run();
	    try {
		HeapMemory.instance().executeInArea(aTester);
		fail(kind() + " array allowed from scope to heap");
	    } catch (IllegalAssignmentError e) {
	    }
	    try {
		HeapMemory.instance().executeInArea(sTester);
		fail(kind() + " object allowed from scope to heap");
	    } catch (IllegalAssignmentError e) {
	    }
	    try {
		ImmortalMemory.instance().executeInArea(aTester);
		fail(kind() + " array allowed from immortal to scope");
	    } catch (IllegalAssignmentError e) {
	    }
	    try {
		ImmortalMemory.instance().executeInArea(sTester);
		fail(kind() + " object allowed from immortal to scope");
	    } catch (IllegalAssignmentError e) {
	    }
	}
    }

    final void testClone() {
	Runnable r = new ImplicitWriteTester() {
		public String kind() { return "clone"; }
		public void test(Object[] data) {
		    data.clone();
		}
		public void test(PubCloneable data) {
		    data.clone();
		}
	    };
	new LTMemory(SMALL_AREA, SMALL_AREA).enter(r);
    }

    public void testArraycopy() {
	Runnable r = new ImplicitWriteTester() {
		public String kind() { return "System.arraycopy"; }
		public void test(Object[] src) {
		    Object[] dest = new Object[src.length];
		    System.arraycopy(src, 0, dest, 0, src.length);
		}
	    };
	new LTMemory(SMALL_AREA, SMALL_AREA).enter(r);
    }

    final void testTooBigArea() {
        setModule("MemoryArea allocation failure");
        try {
            byte[] o = new byte[15 * 1024 * 1024];
            new LTMemory(40 * 1024 * 1024, 40 * 1024 * 1024);
            fail("allocated memory area larger than semi-space " + o.length);
        } catch (OutOfMemoryError _) {
        }
    }

    final Runnable testLookup = new Runnable() {
        TestBase dns = new test.jvm.TestDNS(h_, TestSuite.DISABLE_DNS_LATENCY);
        public void run() {
            dns.runTest();
        }
    };
}
