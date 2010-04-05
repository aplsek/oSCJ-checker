// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/signals/AgnosticIOSignalManager.java,v 1.2 2004/10/18 21:32:49 baker29 Exp $

package ovm.services.io.signals;

/**
 * An IOSignalManager that doesn't care about what operation you're doing.
 * Call to addCallbackForRead(), addCallbackForWrite(), or addCallbackForExcept()
 * will all do the same thing -- they will all call addCallback().
 *
 * @author Filip Pizlo
 */
public interface AgnosticIOSignalManager
    extends IOSignalManager {
    
    public void addCallback(int fd, IOSignalManager.Callback cback);
}

