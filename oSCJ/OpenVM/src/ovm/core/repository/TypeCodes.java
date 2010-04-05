/**
 * @file ovm/core/repository/TypeCodes.java
 **/

package ovm.core.repository;

/**
 * Type codes are the character representations of types.
 * In addition to the <i>BaseType</i> characters listed in
 * {@link <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#84645">Table 4.2</a>}
 * of section 4.3.2 of the JVM Spec, there are additional
 * type code characters for other OVM types.
 **/
public interface TypeCodes {

    char NONE = (char) 0;

    /**
     * The typecode for primitive floats
     **/
    char FLOAT = 'F';

    /**
     * The typecode for primitive booleans 
     **/
    char BOOLEAN = 'Z';

    /**
     * The typecode for primitive chars 
     **/
    char CHAR = 'C';

    /**
     * The typecode for primitive shorts 
     **/
    char SHORT = 'S';

    /**
     * The typecode for unsigned shorts  (not currently implemented)
     **/
    char USHORT = 's'; // science fiction

    /**
     * The typecode for primitive bytes
     **/
    char BYTE = 'B';

    /**
     * The typecode for unsigned bytes  (not currently implemented)
     **/
    char UBYTE = 'b'; // science fiction

    /**
     * The typecode for primitive integers 
     **/
    char INT = 'I';

    /**
     * The typecode for unsigned integers  
     * (not currently implemented)
     **/
    char UINT = 'i'; // science fiction

    /**
     * The typecode for primitive longs 
     **/
    char LONG = 'J';

    /**
     * The typecode for unsigned longs  (not currently implemented)
     **/
    char ULONG = 'j'; // science fiction

    /**
     * The typecode for primitive doubles 
     **/
    char DOUBLE = 'D';

    /**
     * The typecode for void types 
     **/
    char VOID = 'V';

    /**
     * The typecode for object types 
     **/
    char OBJECT = 'L';

    /**
     * The typecode for states shared by all instances of the corresponding L type
     **/
    char GEMEINSAM = 'G';

    /**
     * The typecode for a type ("ovm.core.domain.Type"), not a java.lang.Class.
     * The line between 'T' and 'G' is very fine and may even be non-existent
     * in parts of the implementation.
     **/
    char TYPE = 'T';

    /**
     * The typecode for a selector ("ovm.core.repository.Selector")
     **/
    char SELECTOR = 't';

    /**
     * The typecode for control (e.g. a branch).
     */
    char CONTROL = 'c';

    /**
     * The typecode for records 
     * (not currently implemented)
     **/
    char RECORD = 'R'; // science fiction

    /**
     * The typecode for enumerations  (not currently implemented)
     **/
    char ENUM = 'E'; // science fiction

    /**
     * The typecode for arrays 
     **/
    char ARRAY = '[';

    /**
     * Trust me, will be useful (for null).
     **/ 
    char THENULL = 'N';



    /**
     * The type of what Jsr pushes on the stack
     */
    char RETURNADDRESS = 'U';

    /**
     * General reference type including objects, arrays, etc.
     **/
    char REFERENCE = 'A';
}
