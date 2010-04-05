// $Header: /p/sss/cvs/OpenVM/src/ovm/core/stitcher/AsyncIOServicesFactory.java,v 1.5 2004/04/08 18:39:57 pizlofj Exp $

package ovm.core.stitcher;

/**
 *
 * @author Filip Pizlo
 */
public abstract class AsyncIOServicesFactory implements ServiceFactory {
    
    public static final String name = "AsyncIOServices";
    
    public abstract ovm.services.io.async.FileDescriptorWrapifier getFileDescriptorWrapifier();
    public abstract ovm.services.io.async.StdIOManager getStdIOManager();
    public abstract ovm.services.io.async.PipeManager getPipeManager();
    public abstract ovm.services.io.async.FileManager getFileManager();
    public abstract ovm.services.io.async.SocketManager getSocketManager();
    public abstract ovm.services.io.async.SelectorManager getSelectorManager();
    public abstract ovm.services.io.async.HostLookupManager getHostLookupManager();
    
    public ovm.services.ServiceInstance[] getServiceInstances() {
        return new ovm.services.ServiceInstance[]{getFileDescriptorWrapifier(),
                                                  getStdIOManager(),
                                                  getPipeManager(),
                                                  getFileManager(),
                                                  getSocketManager(),
                                                  getSelectorManager(),
                                                  getHostLookupManager()};
    }
    
}

