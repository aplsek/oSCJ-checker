// $Header: /p/sss/cvs/OpenVM/src/ovm/core/services/process/ForkManager.java,v 1.3 2004/05/23 20:15:07 baker29 Exp $

package ovm.core.services.process;

import ovm.util.*;
import ovm.core.execution.*;
import s3.util.PragmaAtomic;

/**
 * Manages forking.  Some OVM stuff needs to do special processing before or after a fork.
 * This here thing manages those callbacks.
 * <p>
 * By the way, I realize that I could have made this into some sort of service with an
 * interface and an implementation...  But this would not have bought me anything since
 * there is really only one way to do what this code does.
 *
 * @author Filip Pizlo
 */
public class ForkManager {
    
    private ForkManager() {}
    
    public static interface BeforeHandler {
        /** should never throw exceptions.  will be called when interrupts are disabled. */
        public void before();

        /** should never throw exceptions.  will be called when interrupts are disabled. */
        public void afterNeverHappened();
    }
    
    public static interface AfterHandler {
        /** should never throw exceptions.  will be called when interrupts are disabled. */
        public void afterInChild();

        /** should never throw exceptions.  will be called when interrupts are disabled. */
        public void afterInParent(int childPid);
    }
    
    static ArrayList before=new ArrayList();
    static ArrayList after=new ArrayList();
    
    /**
     * @return a negative errno if an error happened.  otherwise same as the syscall.
     */
    public static int fork() throws PragmaAtomic {
        for (int i=0;
             i<before.size();
             ++i) {
            ((BeforeHandler)before.get(i)).before();
        }
        int res=Native.fork();
        if (res<0) {
            res=-Native.getErrno();
            for (int i=0;
                 i<before.size();
                 ++i) {
                ((BeforeHandler)before.get(i)).afterNeverHappened();
            }
        } else if (res==0) {
            for (int i=0;
                 i<after.size();
                 ++i) {
                ((AfterHandler)after.get(i)).afterInChild();
            }
        } else {
            for (int i=0;
                 i<after.size();
                 ++i) {
                ((AfterHandler)after.get(i)).afterInParent(res);
            }
        }
        return res;
    }
    
    public static void addBefore(BeforeHandler h)
	throws PragmaAtomic
    {
        before.add(h);
    }
    
    public static void addAfter(AfterHandler h)
	throws PragmaAtomic
    {
        after.add(h);
    }
    
    public static void removeBefore(BeforeHandler h)
	throws PragmaAtomic
    {
        for (;;) {
            int i=before.indexOf(h);
            if (i<0) {
                break;
            }
            before.remove(i);
        }
    }
    
    public static void removeAfter(AfterHandler h)
	throws PragmaAtomic
    {
        for (;;) {
            int i=after.indexOf(h);
            if (i<0) {
                break;
            }
            after.remove(i);
        }
    }
}

