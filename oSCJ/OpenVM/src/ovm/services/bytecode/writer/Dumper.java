package ovm.services.bytecode.writer;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import ovm.core.repository.TypeName;
import ovm.core.repository.ConstantPool;
import ovm.core.repository.RepositoryClass;

/**
 * Dump an OVM Repository class back to Java <code>.class</code> format.
 * @author KP
 **/
public interface Dumper {
    
    public void dump(DataOutputStream out) 
	throws IOException, ConstantPool.AccessException;


    public void dump(String parentDirectory) 
	throws IOException, 
	ConstantPool.AccessException, FileNotFoundException;

    /**
     * Interface to allow the Dumper to ask about information on other classes.
     **/
    public interface Context {
	/**
	 * Is the specified type an interface?
	 **/
	public boolean isInterface(TypeName.Scalar tn);
    } // end of Dumper.Context
    

    interface Factory {
	Dumper makeDumper(Dumper.Context ctx,
			  RepositoryClass cls);
    }

} // end of Dumper
