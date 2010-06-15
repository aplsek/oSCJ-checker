/**
 **/
package test.runtime;

import ovm.core.domain.Oop;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import test.common.TestBase;

/**
 * 
 * @author Chapman Flack
 **/
public class TestADTtoString
    extends TestBase {

    public TestADTtoString() {
	super("ADTtoString");
    }

    public void run() {
        testWord();	
        testAddress();
        testOop();
    }
    
    public void testWord() {
	setModule("word");
        
        VM_Word w = VM_Word.fromInt( 42);
        check_condition( "#2a".equals( w.toString()));
        check_condition( "#2f".equals( w.add( 5).toString()));	
    }
    
    public void testAddress() {
        setModule("address");
        
        VM_Address a = VM_Address.fromObject( this);
        int asi = a.asInt();
        check_condition( ("@"+Integer.toHexString(asi)).equals( a.toString()));   
    }
    
    public void testOop() {
        setModule("oop");
        
        VM_Address a = VM_Address.fromObject( this);
        Oop o = a.asOop();
        String model = o.getBlueprint().toString() + a.toString();
        Object rslt  = o.metaToString();
        check_condition( rslt instanceof String, "not a String");
        check_condition( model.equals( rslt), model+"!="+(String)rslt);
    }
    
} // end of TestADTtoString
