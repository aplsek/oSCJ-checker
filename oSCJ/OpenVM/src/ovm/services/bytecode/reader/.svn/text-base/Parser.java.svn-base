/** 
 * @file ovm/services/bytecode/reader/Parser.java 
 * @version 2.1.0 10/05/01 
 **/
package ovm.services.bytecode.reader;

import ovm.core.domain.LinkageException;
import ovm.core.repository.RepositoryClass;
import ovm.util.ByteBuffer;

/** 
 * The Parser parses the bytecode of a Java class to create repository
 * objects representing this class. Typically usage is as follows:<pre>
 *   Parser cfp = new Parser();
 *   FileReader cfr = new FileReader();
 *   RepositoryClass clz = null;
 *   try { 
 *     byte[] bytes = cfr.getBytes(someFile);
 *     clz    = cfp.parse( bytes, bytes.length );
 *  } catch (IOError err) { ...
 *  } catch (LinkageException.ClassFormat err) { ...  } 
 *</pre>
 * The result of the <code>parse()</code> method is an unverified
 * <code>Class</code>. It has not been installed in the repository, though
 * the Utf8's, <code>Strings</code> and <code>Selectors</code> it includes
 * have been installed.
 * @author Jan Vitek
 **/
public interface Parser {
    /**
     * Parse the given Java class file and return an object that represents
     * its data, i.e., constants, methods, fields and commands.  A
     * <em>LinkageException.ClassFormat</em> is raised, if the file is not a
     * syntactically valid <code>.class</code> file. This does not include
     * verification of the byte code.
     * @param name name of the class.
     * @param byteStream the bytecode.
     * @param length length of the bytecode.
     * @return <code>RepositoryClass</code> object representing the 
     * parsed class file.
     * @exception IOException 
     * @exception LinkageException.ClassFormat if the class file is invalid.
     **/
    RepositoryClass parse(String name, ByteBuffer byteStream, 
			  int length) throws LinkageException.ClassFormat;

    /**
     * Parse the given Java class file and return an object that represents
     * its data, i.e., constants, methods, fields and commands.  A
     * <em>LinkageException.ClassFormat</em> is raised, if the file is not a
     * syntactically valid <code>.class</code> file. This does not include
     * verification of the byte code.
     * @param byteStream the bytecode
     * @param length length of the bytecode
     * @return <code>RepositoryClass</code> object representing the parsed 
     * class file.
     * @exception LinkageException.ClassFormat if the class file is invalid.
     **/
    RepositoryClass parse(ByteBuffer byteStream, int length) 
	throws LinkageException.ClassFormat;


    interface Factory {
	Parser makeParser();
    }   // End of Factory

    /**
     * For debugging purposes, it is usefull to keep the constant pool
     * complete, i.e. don't clean it up.
     *
     * This method give a mean to tell the Parser whether we want such a
     * cleanup to be done when the class is loaded.
     * @param b
     **/
    void doConstantPoolCleanupOnLoad(boolean b);

} // End of Parser


