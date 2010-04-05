package org.ovmj.transact;
import org.ovmj.util.PragmaNoPollcheck;
/** * 
 * @author jmanson, jvitek
 */
public class Transaction {   
    public static void start(int size, boolean commitOnOverflow)  throws PragmaNoPollcheck {LibraryImports.start(size, commitOnOverflow);}
    public static void start()      throws PragmaNoPollcheck { LibraryImports.start();       }
    public static void commit()     throws PragmaNoPollcheck { LibraryImports.commit();      }
    public static void undo()       throws PragmaNoPollcheck { LibraryImports.undo();       }
    public static void retry()      throws PragmaNoPollcheck { LibraryImports.retry();       }
    public static void abort() {} 
    public static int logSize()     throws PragmaNoPollcheck { return LibraryImports.logSize(); }    
}
 