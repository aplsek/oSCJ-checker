/**
 **/
package test.runtime;

import ovm.core.repository.Selector;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.memory.VM_Address;
import s3.util.PragmaHierarchy;
import s3.util.PragmaSquawk;
import s3.util.PragmaTransformCallsiteIR;
import test.common.TestBase;
import s3.services.transactions.Transaction;

import ovm.core.domain.Method;

/**
 * Tests the runtime evaluation of pragmas.
 * During image-build, the rewriter should squawk three times: while rewriting
 * the bodies of b() and c(), and while rewriting the invokevirtual of c() in
 * method a(). At run time, the iteration over the methods of this class should
 * count two methods (b and c) that declare PragmaSquawk.
 * @author Chapman Flack
 **/
public class TestPragma
    extends TestBase {

    public TestPragma() {
	super("Pragma");
    }

    public void run() {
	testBooleanPragma();
	testHierarchicalPragma();
	testInlineSubstituteBytecode();
    }
    
    public void testBooleanPragma() {
	setModule("boolean pragma");
	
	VM_Address me = VM_Address.fromObject( this);
//	Oop meAsAnOop = me.asOop();            // hardcoded type, old way
  	Oop meAsAnOop = (Oop)me.asAnyObject(); // general soln, now works
	Blueprint bp = meAsAnOop.getBlueprint();
	
	Type.Scalar ts = bp.getType().asScalar();
	int squawks = 0;
	for ( Method.Iterator it = ts.localMethodIterator(); it.hasNext(); ) {
	    Method m = it.next();
	    Selector.Method ms = m.getSelector();
	    if ( PragmaSquawk.declaredBy( ms, bp) ) {
		++ squawks;
	    }
	}
	int expected = Transaction.the().transactionalMode() ? 4 :2;
	check_condition( squawks == expected, "Squawk miscount");
    }
    
    public void testHierarchicalPragma() {
	setModule("hierarchical pragma");
	Blueprint bp = VM_Address.fromObject( this).asOop().getBlueprint();
	Type.Scalar ts = bp.getType().asScalar();
	int hierks = 0;
	for ( Method.Iterator it = ts.localMethodIterator(); it.hasNext(); ) {
	    Method m = it.next();
	    Selector.Method ms = m.getSelector();
	    String cookie = (String)PragmaHierarchy.descendantDeclaredBy( ms, bp);
	    if ( cookie != null ) {
		++ hierks;
		// p( ms.toString());
		// p( ": ");
		// p( cookie);
		// p( "\n");
	    }
	}
	int expected = Transaction.the().transactionalMode() ? 6 :3;
	check_condition( hierks == expected, "Hierarchical pragma miscount");
    }
    
    public void testInlineSubstituteBytecode() {
      	setModule("PragmaTransformCallsiteIR");
	check_condition( g(1) == 1, "Method g not substituted");
    }
    
    public void a() { c(); d(); e(); f(); }
    
    public void b() throws PragmaSquawk { }
    
    void c() throws PragmaSquawk { }
    
    void d() throws PragmaHierarchy { }
    void e() throws PragmaHierarchy.Foo { }
    void f() throws PragmaHierarchy.Bar { }
    
    int g( int i) throws PragmaTransformCallsiteIR.BCnothing { return 2; }
    
} // end of TestPragma
