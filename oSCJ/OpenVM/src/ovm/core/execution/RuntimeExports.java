/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package ovm.core.execution;

import ovm.services.rusage.RUsage;
import ovm.core.Executive;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Code;
import ovm.core.domain.Field;
import ovm.core.domain.JavaDomain;
import ovm.core.domain.JavaUserDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.ReflectiveArray;
import ovm.core.domain.ReflectiveConstructor;
import ovm.core.domain.ReflectiveField;
import ovm.core.domain.Type;
import ovm.core.domain.WildcardException;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Mode;
import ovm.core.repository.Descriptor;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.services.timer.TimeConversion;
import ovm.core.stitcher.JavaServicesFactory;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.SignalServicesFactory;
import ovm.core.stitcher.InterruptServicesFactory;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.IOServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.core.stitcher.MonitorServicesFactory;
import ovm.core.stitcher.TimerServicesFactory;
import ovm.core.stitcher.EventServicesFactory;
import ovm.core.services.timer.TimerManager;
import ovm.services.bytecode.JVMConstants.Throwables;
import ovm.services.events.InterruptMonitor;
import ovm.services.events.SignalMonitor;
import ovm.services.io.clients.PosixIO;
import ovm.services.java.JavaDispatcher;
import ovm.services.java.JavaOVMThread;
import ovm.services.java.realtime.RealtimeJavaDispatcher;
import ovm.services.memory.FinalizableArea;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.TimedAbortableConditionQueue;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.ArrayList;
import ovm.util.ByteBuffer;
import ovm.util.IdentityHashMap;
import ovm.util.Mem;
import ovm.util.OVMError;
import ovm.util.UnicodeBuffer;
import ovm.util.UnsafeAccess;
import s3.core.domain.S3Domain;
import s3.core.domain.S3JavaUserDomain;
import s3.core.domain.S3Type;
import s3.core.execution.S3CoreServicesAccess;
import s3.services.java.realtime.RealtimeJavaThreadImpl;
import s3.services.memory.mostlyCopying.SplitRegionManager;
import s3.services.memory.mostlyCopying.SplitRegionManager.ScopedArea;
import s3.services.transactions.EDAbortedException;
import s3.services.transactions.Transaction;
import s3.util.PragmaAtomic;
import s3.util.PragmaForwardCallingContext;
import s3.util.PragmaInline;
import s3.util.PragmaNoPollcheck;
import ovm.util.IdentityHashMap;
import ovm.util.ByteBuffer;
import ovm.core.domain.WildcardException;
import ovm.services.memory.scopes.VM_ScopedArea;
import s3.services.memory.mostlyCopying.SplitRegionManager;
import s3.services.transactions.PragmaPARSafe;
import ovm.core.services.events.EventManager;
import s3.services.java.realtime.RealtimeJavaThreadImpl;

import s3.rawMemory.RawMemoryAccess;
import s3.rpt.RPT;

public final class RuntimeExports extends BaseRuntimeExports implements UnsafeAccess {

    // Java dispatcher
    final JavaDispatcher jd;

    // Java Services factory
    final JavaServicesFactory jsf;

    // Signal monitor
    final SignalMonitor sigMon;
    
    // Interrupt monitor
    final InterruptMonitor intMon;

    UserLevelThreadManager tm;
    PosixIO io;

    Monitor.Factory monitorFactory;
    
    TimerManager timer;
    EventManager em;

    public RuntimeExports(JavaDomain dom) {
        super(dom);
	jsf = (JavaServicesFactory)
            ThreadServiceConfigurator.config.getServiceFactory(
                JavaServicesFactory.name);
	if (jsf != null) {
	    jd = jsf.getJavaDispatcher();
	}
	else
	    jd = null;

	SignalServicesFactory ssf = (SignalServicesFactory)
            ThreadServiceConfigurator.config.getServiceFactory(
                SignalServicesFactory.name);

	if (ssf != null) {
	    sigMon = ssf.getSignalMonitor();
	}
	else
	    sigMon = null;
	    
        InterruptServicesFactory isf = (InterruptServicesFactory)
          ThreadServiceConfigurator.config.getServiceFactory(
                InterruptServicesFactory.name);
                
        if (isf != null) {
          intMon = isf.getInterruptMonitor();
        } else {
          intMon = null;
        }

        io = PosixIO.factory().make(dom);
        // Added to have setReschedulingEnabled()
        tm = (UserLevelThreadManager)
            ((ThreadServicesFactory)ThreadServiceConfigurator.config.
                getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
        if (tm == null) {
            throw new OVMError.Configuration("need a configured thread manager");
        }

        monitorFactory = 
            ((MonitorServicesFactory) ThreadServiceConfigurator.config
             .getServiceFactory(MonitorServicesFactory.name))
            .getMonitorFactory();

        timer =
	    ((TimerServicesFactory) ThreadServiceConfigurator.config
	     .getServiceFactory(TimerServicesFactory.name))
	    .getTimerManager();

	em=((EventServicesFactory) IOServiceConfigurator.config
	    .getServiceFactory(EventServicesFactory.name))
	    .getEventManager();
    }

    /*
     * OK.  enableClassInitialization is
     * closely tied to dynamic loading and belong in the CSA.  
     */
    public void enableClassInitialization() {
	csa.enableClassInitialization();
    }

    /*
     * It might be best to keep the implementation of clone near
     * the other allocation routines (in the CSA).
     */
    public Oop clone(Oop oop) {
	return csa.clone(oop);
    }

    // These are mostly Java user-domain reflection methods and should
    // be in a Java UD specific RTE subclass - DH  19 Feb 2004

//     public void setApplicationContextMirror(Oop loader) {
//         ((JavaUserDomain)dom).setApplicationTypeContextMirror(loader);
//     }

    public Oop nameForClass(Oop classType) {
	Type type = classType.getBlueprint().getInstanceBlueprint().getType();
        assert type.getDomain() == dom : "domain mis-match!";

        // It is not clear exactly what the right process is here. We have
        // two goals:
        // a) create an interned string for the class name
        // b) avoid leaking any temporary objects into the current scope,
        //    or the interned string area
        // This is my attempt at the above - DH Mar 11, 2004

	// A class name string can't be allocated in the scratchpad.
	// the maximum size of such as string is 64k.  Granted, it
	// would be fairly difficult to find a system that can have
	// filenames this long.

	// The following code only creates 1 intermediate copy of the
	// class name - JB Feb 15, 2005
	UnicodeBuffer name =
	    JavaFormat._.formatUnicode(type.getUnrefinedName());
	return dom.internString(name);
    }

    public Oop classFor(Oop o)  {
	Blueprint bp = o.getBlueprint();
	S3Type t = (S3Type) bp.getType();
	return t.getClassMirror();
    }


    public Oop VMClass_getComponentType(Oop klass) {
	Blueprint bp = klass.getBlueprint().getInstanceBlueprint();
	if (!bp.isArray())
	    return null;
	Type.Array at = (Type.Array) bp.getType();
	S3Type ct = (S3Type) at.getComponentType();
	return ct.getClassMirror();
    }
    public int VMClass_getModifiers(Oop klass,
				    boolean ignoreInnerClassesAttribute) {
	Type t = klass.getBlueprint().getInstanceBlueprint().getType();
	return t.getMode().getMode();
    }
    public Oop VMClass_getSuperclass(Oop klass) {
	Type sub = klass.getBlueprint().getInstanceBlueprint().getType();
	Type sup = sub.getSuperclass();
	return (sup == null
		? null
		: sup.getClassMirror());
    }
    public boolean VMClass_isInstance(Oop klass, Oop o) {
	Blueprint lhs = klass.getBlueprint().getInstanceBlueprint();
	Blueprint rhs = o.getBlueprint();
	return rhs.isSubtypeOf(lhs);
    }
    public boolean VMClass_isAssignableFrom(Oop klass, Oop c) {
	Blueprint lhs = klass.getBlueprint().getInstanceBlueprint();
	Blueprint rhs = c.getBlueprint().getInstanceBlueprint();
	return rhs.isSubtypeOf(lhs);
    }
    public boolean VMClass_isInterface(Oop klass) {
	Type t = klass.getBlueprint().getInstanceBlueprint().getType();
	return t.isInterface();
    }
    public boolean VMClass_isPrimitive(Oop klass) {
	return klass.getBlueprint().getInstanceBlueprint().isPrimitive();
    }
    public boolean VMClass_isArray(Oop klass) {
	return klass.getBlueprint().getInstanceBlueprint().isArray();
    }
    
    public Oop VMClass_getInterfaces(Oop c) {
       Blueprint bp = c.getBlueprint().getInstanceBlueprint();
       Type.Reference t = (Type.Reference)bp.getType();
       
       Type.Interface[] ifaces = t.getInterfaces();
       
       Oop ret = classArr.make(ifaces.length);
       for(int i=0; i<ifaces.length ; i++) {
         classArr.setOop(ret, i, type2class( ifaces[i] ));
       }
       
       return ret;
    
    }

    public Oop VMClass_getDeclaringClass(Oop vmType) {
      Type t = vmType.getBlueprint().getInstanceBlueprint().getType();
      if ( t==null || !t.isScalar()) {
        return null;
      } else {
        Type.Scalar s = t.asScalar();
        
        if (s.getOuterName()==null) { // otherwise, getOuterType would cause segfault
          return null;
        }
        
        try {
          return s.getOuterType().getClassMirror();
        } catch (LinkageException le) {
          return null;
        }
      }
    }

    // VMClass_getClassLoader wraps this to do bootstrap classloader
    // translation
    public Oop getClassLoader(Oop klass) {
	Type t = klass.getBlueprint().getInstanceBlueprint().getType();
	return t.getContext().getLoader().getMirror();
    }
    public void initializeClass(Oop clazz) {
	csa.initializeBlueprint(clazz);
    }

    public Oop getSystemContext() {
	return asOop(dom.getApplicationTypeContext());
    }
    public Oop getClassPath() {
	String str = ((JavaUserDomain) dom).getClassPath();
	return dom.makeString(UnicodeBuffer.factory().wrap(str));
    }
    public Oop getBootContext() {
	return asOop(dom.getSystemTypeContext());
    }
    public Oop getBootClassPath() {
	String str = ((JavaUserDomain) dom).getBootClassPath();
	return dom.makeString(UnicodeBuffer.factory().wrap(str));
    }

    public void VMClass_throwException(Oop throwable) {
        throw csa.processThrowable(throwable);
    }

    private IdentityHashMap loaders = new IdentityHashMap();

    /**
     * Called to register bootstap/system classloaders with the
     * builtin type contexts, and to allocate a type context for a new
     * classloader.  setPeer must be called from the ClassLoader
     * constructor, before initiateLoading is called.
     **/
    public void setPeer(Oop _ctx, Oop loader) {
        Type.Context ctx = (Type.Context) _ctx;
	if (ctx == null)
	    ctx = ((JavaUserDomain) dom).makeTypeContext();
	Object r = MemoryPolicy.the().enterMetaDataArea(ctx);
	try { ctx.setLoader(((JavaUserDomain) dom).makeTypeLoader(loader)); }
	finally { MemoryPolicy.the().leave(r); }

	// FIXME: what exactly is the lifetime of a ClassLoader.  this
	// IdentityHashMap is going to keep loaders around
	// indefinitely, but no one has suggested a way do unloading
	r = MemoryPolicy.the().enterMetaDataArea(dom);
	try { loaders.put(loader, ctx); }
	finally { MemoryPolicy.the().leave(r); }
    }


    public void dummy_force_pragmas() throws s3.util.PragmaIgnoreSafePoints {
    }

    /*
     * This method is called by Class.forName to look up class and
     * array types by name.  We cannot call loader.loadClass on
     * arrays, and we should not call it on scalars either.
     * Class.forName calls must initiate class loading in the VM.
     */
    public Oop initiateLoading(Oop loader, Oop _name) {
	Type.Context ctx = (loader == null
			    ? dom.getSystemTypeContext()
			    : (Type.Context)  loaders.get(loader));
	TypeName name = JavaFormat._.parseTypeName(dom.getString(_name), false);
	//((S3JavaUserDomain) dom).observeForName(name);
	try {
	    Type t = ctx.typeFor(name);
	    if (t.getLifecycleState() != Type.State.INITIALIZED)
		((S3JavaUserDomain) dom).observeForName(name);
	    return ((S3Type) t).getClassMirror();
	} catch (LinkageException e) {
	    throw csa.processThrowable(e.toLinkageError(dom));
	}
    }

    public Oop findLoadedClass(Oop loader, Oop name) {
	Type.Context ctx = (Type.Context) loaders.get(loader);
	TypeName tn = JavaFormat._.parseTypeName(dom.getString(name), false);
	if (!tn.isScalar())
	    return null;
	Type t = ctx.loadedTypeFor(tn);
	if (t == null)
	    return null;
	return ((S3Type) t).getClassMirror();
    }

    public Oop defineClass(Oop loader, Oop name,
			   Oop _data, int off, int len) {    
	Type.Context ctx = (Type.Context) loaders.get(loader);
	Object r = MemoryPolicy.the().enterMetaDataArea(ctx);
	TypeName.Scalar tn = null;		 
	try {
	    
	    // FIXME: A lot of temporary allocation is being done in
	    // the MetaDataArea!  Maybe whenever defineClass is
	    // called, we can stop acting as a no-heap realtime thread
	    // and run in the heap?
	    if (name != null) {
		TypeName _tn = JavaFormat._.parseTypeName(dom.getString(name),
							  false);
		if (_tn.isScalar())
		    tn = _tn.asScalar();
		else
		    // We could throw NoClassDef, because we know that
		    // this name won't match a if the bytes are valid
		    throw new LinkageException("bad class name: " + _tn);
	    }
	    // FIXME: We should just wrap the UD array in a ByteBuffer
	    byte[] data = new byte[len];
	    if (true) {
	      Native.print_string("defineClass: "+tn);
	      throw Executive.panic("Fix array allocation and copy in defineClass!");
	    }
	    Blueprint.Array ubp = _data.getBlueprint().asArray();
	    Blueprint.Array ebp = asOop(data).getBlueprint().asArray();
	    Mem.the().cpy(asOop(data), ebp.byteOffset(0),
			  _data, ubp.byteOffset(off),
			  len);
	    Type t = ctx.defineType(tn, ByteBuffer.wrap(data));
	    return ((S3Type) t).getClassMirror();
	} catch (LinkageException e) {
	    BasicIO.out.println("Failing for " + tn + "!!!!!!");
	    throw new WildcardException(e.toLinkageError(dom));
	} finally {
	    MemoryPolicy.the().leave(r);
	}
    }

    // tunnel into string internals from org.ovmj.* packages
    private ReflectiveField.Reference String_value = dom.isExecutive() ? null :
	new ReflectiveField.Reference(dom,
				      JavaNames.arr_char,
				      JavaNames.java_lang_String,
				      "value");

    private ReflectiveField.Integer String_count = dom.isExecutive() ? null :
	new ReflectiveField.Integer(dom,
                                    JavaNames.java_lang_String,
				     "count");

    private ReflectiveField.Integer String_offset = dom.isExecutive() ? null :
	new ReflectiveField.Integer(dom,
                                    JavaNames.java_lang_String,
				     "offset");

    public Oop breakEncapsulation_String_value(Oop str) {
        return String_value.get(str);
    }
    
    public int breakEncapsulation_String_count(Oop str) {
        return String_count.get(str);
    }
    
    public int breakEncapsulation_String_offset(Oop str) {
        return String_offset.get(str);
    }

    static private ArrayList VMPropertyNames = new ArrayList();
    static private ArrayList VMPropertyValues = new ArrayList();

    static public void defineVMProperty(String name, String value) {
	VMPropertyNames.add(name);
	VMPropertyValues.add(value);
    }
    static public void defineVMProperty(String name, boolean value) {
	defineVMProperty(name, value ? "true" : "false");
    }
    static public void defineVMProperty(String name, int value) {
	defineVMProperty(name, String.valueOf(value));
    }
    public Oop VMPropertyName(int i) {
	if (i >= VMPropertyNames.size())
	    return null;
	String s = (String) VMPropertyNames.get(i);
	return dom.internString(UnicodeBuffer.factory().wrap(s));
    }
   public Oop VMPropertyValue(int i) {
	if (i >= VMPropertyNames.size())
	    return null;
	String s = (String) VMPropertyValues.get(i);
	return dom.internString(UnicodeBuffer.factory().wrap(s));
    }
    public boolean isVMProperty(Oop _name) {
	String name = dom.getString(_name).toString();
	for (int i = 0; i < VMPropertyNames.size(); i++)
	    if (VMPropertyNames.get(i).equals(name))
		return true;
	return false;
    }

    public Oop getenv(Oop name) {
	VM_Address val = Native.getenv(dom.getLocalizedCString(name));
	if (val == null)
	    return null;
	else
	    return dom.stringFromLocalizedCString(val); // val can't move because it is outside of GCed heap
    }

    public long length(Oop name) {
	// This is the wrong place to enter the scratchpad
	//Object r1 = MemoryPolicy.the().enterScratchPadArea();
        //try {
	return io.length(name);
	//} finally { MemoryPolicy.the().leave(r1); }
    }

    public int getAvailable(int fd) {
       Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getAvailable(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int setSoReuseAddr(int sock,boolean reuseAddr) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.setSoReuseAddr(sock,reuseAddr);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int getSoReuseAddr(int sock) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getSoReuseAddr(sock);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int setSoKeepAlive(int sock,boolean keepAlive) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.setSoKeepAlive(sock,keepAlive);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int getSoKeepAlive(int sock) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getSoKeepAlive(sock);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int setSoLinger(int sock,Oop linger) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.setSoLinger(sock,linger);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int getSoLinger(int sock,Oop linger) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getSoLinger(sock,linger);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int setSoTimeout(int sock,int timeout) {
	return 0;
    }
    
    public int getSoTimeout(int sock) {
	return 0;
    }
    
    public int setSoOOBInline(int sock,boolean oobInline) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.setSoOOBInline(sock,oobInline);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int getSoOOBInline(int sock) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getSoOOBInline(sock);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int setTcpNoDelay(int sock,boolean noDelay) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.setTcpNoDelay(sock,noDelay);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public int getTcpNoDelay(int sock) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getTcpNoDelay(sock);
        } finally { MemoryPolicy.the().leave(r1); }
    }

    public Oop getPrimitiveClass(char tag) {
        return csa.getPrimitiveClass(tag);
    }
    public int identityHashCode(Oop oop) {
	return oop.getHash();
    }
    public void print(Oop msg) {
	BasicIO.err.print(dom.getString(msg));
    }
    public void fillInStackTrace(Oop throwable) {
	dom.fillInStackTrace(throwable, Context.getCurrentContext());
    }
    public Oop getStackTrace(Oop throwable) {
	return dom.getStackTrace(throwable);
    }
    public Oop getStackTrace(Oop throwable, Oop ctx) {
	dom.fillInStackTrace(throwable, (Context) ctx);
	return dom.getStackTrace(throwable);
    }

    /**
     * This is how java.lang.reflect.Array.getLength(Object) SHOULD be
     * implemented.  We don't handle non-array values because
     * Class.isArray() is cheap.
     **/
    public int arrayLength(Oop array) {
	return ((Blueprint.Array) array.getBlueprint()).getLength(array);
    }

    /**
     * Copy elements within an array.
     **/
    public final void copyOverlapping(Oop array, int soff, int doff, int nelt) {
	MemoryManager.the().copyOverlapping(array, soff, doff, nelt);
    }
    
    /**
     * Unsafe, no checks, just copy. Use to implement
     * System.arraycopy.  
     **/
    public void copyArrayElements(Oop fromArray, int fromOffset,
				  Oop toArray, int toOffset, 
				  int nElems) {
	MemoryManager.the().copyArrayElements(fromArray, fromOffset,
				    toArray, toOffset,
				    nElems);
    }


    /** 
     * Unsafe, unchecked direct store into an array. This is a hack to
     * allow an ED reference to be stored into a UD Opaque[].
     * The array is presumed to be non-heap allocated otherwise it may
     * need pinning.
     */
    public void storeInOpaqueArray(Oop arr, int index, Oop val) {
//        arr.getBlueprint().asArray().
//            addressOfElement(arr, index).
//            setAddress(VM_Address.fromObject(val));
        MemoryManager.the().setReferenceArrayElement(arr, index, val);
    }

    // monitor functions

    /*
     * I don't see why these monitor methods are defined in the CSA.
     * Does it have something to do with aspects?
     * NOPE - the csa already had monitorEnter and exit, it also uses
     * monitorWait and notifyAll as part of initializeBlueprint. So it makes
     * some sense to define all the monitor methods in one place.
     * This is particularly so when considering fast-locks and the role of
     * ensureMonitor
     */

    // unsafe hook for scope memory termination protocol
    public void monitorEnter(Oop obj){// throws PragmaAtomic { //PARBEGIN PAREND
       // csa.monitorEnter(obj);
    }

    // unsafe hook for scope memory termination protocol
    public void monitorExit(Oop obj) {// throws PragmaAtomic { //PARBEGIN PAREND
       // csa.monitorExit(obj);
    }

    // hook for NHRT to scope finalizer thread handoff
    public void monitorTransfer(Oop obj, Oop newOwner) {
       // csa.monitorTransfer(obj, (JavaOVMThread)newOwner);
    }

    // used for Thread.holdsLock
    public boolean currentThreadOwnsMonitor(Oop obj) {
	//return csa.currentThreadOwnsMonitor(obj);
    	return false;
    }


    public void monitorNotify(Oop obj) {// throws PragmaAtomic { //PARBEGIN PAREND
	//csa.monitorSignal(obj);
    }

    public void monitorNotifyAll(Oop obj) {// throws PragmaAtomic { //PARBEGIN PAREND
	//csa.monitorSignalAll(obj);
    }

    public boolean monitorWait(Oop obj)  {//throws PragmaAtomic { //PARBEGIN PAREND
	//return csa.monitorWaitAbortable(obj);
    	return false;
    }

    public boolean monitorTimedWait(Oop obj, long millis, int nanos) 
    throws PragmaAtomic { //PARBEGIN PAREND
       
    	/*
    	int rc = csa.monitorWaitTimedAbortable(
            obj, 
            millis * TimeConversion.NANOS_PER_MILLI + nanos
            );
        if ( rc == TimedAbortableConditionQueue.ABORTED) {
            return false;
        }
        else {
            return true;
        }*/
    	return false;
    }

    // only for RealtimeJavaMonitors
    public boolean monitorAbsoluteTimedWait(Oop obj, long deadline) 
    throws PragmaAtomic { //PARBEGIN PAREND
       /*
    	int rc = csa.monitorWaitAbsoluteTimedAbortable(obj, deadline);
        if ( rc == TimedAbortableConditionQueue.ABORTED) {
            return false;
        }
        else {
            return true;
        }
        */
    	return true;
    }


    // testing functions for monitors

    public Oop monitorOwner(Oop obj) {
	return csa.getMonitorOwner(obj);
    }

    public boolean isUnownedMonitor(Oop mon) 
	{
        return monitorOwner(mon) == null;
    }

    public int getEntryCountForMonitor(Oop mon)
	{
        return ((s3.core.execution.S3CoreServicesAccess)csa).getMonitorEntryCount(mon);
    }

    public void createInterruptHandlerMonitor(Oop obj, int interruptIndex) {
        csa.createInterruptHandlerMonitor(obj, interruptIndex);
    }

    public int getMaxInterruptIndex() {
      return intMon.getMaxInterruptIndex();
    }

    private JavaDispatcher getJavaDispatcher() {
        if (jd != null) {
            return jd;
        }
        throw new OVMError.Configuration(
            "Used Javadispatcher in a configuration that does not support it");
    }

    // temporary way for the Launcher to see if we have a JVM config
    public boolean isJVMConfig() {
	return jsf != null;
    }


    // signal monitoring functions for JVM control-C handler etc

    public boolean canMonitorSignal(int sig) {
        return sigMon.canMonitor(sig);
    }

    public int waitForSignal(int sig) {
        return sigMon.waitForSignal(sig);
    }

    public Oop createSignalWatcher() {
        return asOop(sigMon.newSignalWatcher());
    }

    public void addSignalWatch(Oop watcher, int sig) {
        ((SignalMonitor.SignalWatcher)watcher).addWatch(sig);
    }

    public void removeSignalWatch(Oop watcher, int sig) {
        ((SignalMonitor.SignalWatcher)watcher).removeWatch(sig);
    }

    public void waitForSignal(Oop watcher, Oop intArr) {
        int[] out = ((SignalMonitor.SignalWatcher)watcher).waitForSignal();
        Oop o = asOop(out);
        // this assumes that the incoming array is big enough
	MemoryManager.the().copyArrayElements(o, 0, intArr, 0, OVMSignals.NSIGNALS);
    }


    // Interrupt handling functions

/*    
    public Oop registerInterruptHandler(Oop handler, int interruptIndex) {
  
      return asOop(intMon.registerInterruptHandler( (Runnable)handler, interruptIndex );
    
    }
    
    public Oop unregisterInterruptHandler(int interruptIndex) {
      
      return asOop(intMon.unregisterInterruptHandler(interruptIndex));
      
    }
 */
   public boolean waitForInterrupt(int interruptIndex)   {
     return intMon.waitForInterrupt(interruptIndex);
   }

   public void interruptServed(int interruptIndex) {
     intMon.interruptServed(interruptIndex);
   }

   public void interruptServingStarted(int interruptIndex) {
     intMon.interruptServingStarted(interruptIndex);
   }

   public void enableInterrupt(int interruptIndex) {
     intMon.enableInterrupt(interruptIndex);
   }

   public void disableInterrupt(int interruptIndex) {
     intMon.disableInterrupt(interruptIndex);
   }
   
   public void enableLocalInterrupts() {
     intMon.enableLocalInterrupts();
   }

   public void disableLocalInterrupts() {
     intMon.disableLocalInterrupts();
   }
   
   
   public boolean stopMonitoringInterrupt(int interruptIndex) {
     return intMon.stopMonitoringInterrupt(interruptIndex);
   }

   public boolean startMonitoringInterrupt(int interruptIndex) {
     return intMon.startMonitoringInterrupt(interruptIndex);
   }

   public boolean isMonitoredInterrupt(int interruptIndex) {
     return intMon.isMonitoredInterrupt(interruptIndex);
   }
   
       // Higher-level IO functions
    public int getErrno() {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getErrno();
        } finally { MemoryPolicy.the().leave(r1); }
    }
    
    public boolean errnoIsWouldBlock() {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            int errno=io.getErrno();
	    if (errno==NativeConstants.EWOULDBLOCK ||
		errno==NativeConstants.EAGAIN) {
		return true;
	    } else {
		return false;
	    }
        } finally { MemoryPolicy.the().leave(r1); }
    }

    public int getHErrno() {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getHErrno();
        } finally { MemoryPolicy.the().leave(r1); }
    }

    public Oop getLocalHostname() 
	{
//	byte[] hostname = new byte[1024]; // that should be conservative enough...

        byte[] hostname = MemoryManager.the().allocateContinuousByteArray(1024);
	if (0 != Native.gethostname(hostname, hostname.length))
	    return null; // error!
	if (hostname[hostname.length-1] != 0)
	    return null; // error: hostname longer than 1024 bytes!?
	int len = 0;
	while (hostname[len] != 0)
	    len++;
	if (len == 0)
	    return null; // empty string for hostname!?

	// (Alex) Please note that wrap needs the length of the string
	// as the last parameter, not index of the last character.

	// Host names should always be 7 bit, either plain ascii or
	// punycode (RFC 3492).  Should we try to translate punycode
	// to proper UTF8 or UTF16?
	return dom.makeString(UnicodeBuffer.factory().wrap(hostname, 0, len));
    }    

    public Oop getHostByAddr(Oop ipOop, 
			     int af) {
        // can't use scratchpad here because, among other things, we're
        // returning a string to the user  (but also because the memory
        // usage is not guaranteed to be within 4096 bytes)
        return io.getHostByAddr(ipOop,af);
    }

    /**
     * Return byte[][] of IPs for that host!
     */
    public Oop getHostByName(Oop hostnameUD) {
        // can't use scratch pad because of reflective invocation
        return io.getHostByName(hostnameUD);
    }


    private ReflectiveArray stringArr =
	new ReflectiveArray(dom, JavaNames.java_lang_String);

    public Oop list_directory(Oop name) {
	// FIXME: can't we just return a byte[][]
	byte[] buf = io.list_directory(name); // buf is contiguous, but may be replicated right after being populated
	if (buf == null)
	    return null;
	    
        Oop bufOop = VM_Address.fromObject(buf).asOop();
	MemoryManager.the().pinNewLocation(bufOop);  
	
	int cnt = 0;
	for (int i=buf.length-1;i>=0;i--)
	    if (buf[i] == 0)
		cnt++;

	Oop theDarnArray = stringArr.make(cnt);
	cnt = 0;
	int start = 0;

//	Blueprint.Array bufArr = bufOop.getBlueprint().asArray();
	for (int i=0; i<buf.length; i++) {
	    if (buf[i] == 0) {
//		VM_Address cstr = bufArr.addressOfElement(bufOop, start);
		VM_Address cstr = MemoryManager.the().addressOfElement( VM_Address.fromObject(buf), start, 1 );
		
		Oop udString = dom.stringFromLocalizedCString(cstr);
		if (false) {
		  Native.print_string("list_directory saving result ud string: \"");
		  Native.print_ustring_at_address(VM_Address.fromObject(udString));
		  Native.print_string("\" originating from C string ");
                  int j = 0;
                  while (buf[start+j]!=0) {
                    Native.print_char(buf[start+j]);
                    j++;
                  }
                  Native.print_string("\n");
		  
		}
		stringArr.setOop(theDarnArray, cnt++, udString);
		start = i+1;
	    }
	}
	MemoryManager.the().unpin(bufOop);
	return theDarnArray;
    }

    public boolean access(Oop msg,
			  int mode) {
	// can't use scratch pad because of reflective invocation
	return io.access(msg, mode);
    }

    public int mkdir(Oop msg, 
		     int mode) {
	// can't use scratch pad because of reflective invocation
	return io.mkdir(msg, mode);
    }

    public int unlink(Oop name) {
	// can't use scratch pad because of reflective invocation
	return io.unlink(name);
    }

    public int rmdir(Oop name) {
	// can't use scratch pad because of reflective invocation
	return io.rmdir(name);
    }

    public int getmod(Oop name) {
	// can't use scratch pad because of reflective invocation
	return io.getmod(name);
    }

    public int chmod(Oop name, int mod) {
	// can't use scratch pad because of reflective invocation
	return io.chmod(name, mod);
    }

    public int lock(int fd,
		    long start,
		    long size,
		    boolean shared,
		    boolean wait) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.lock(fd, start, size,shared,wait);
        } finally { MemoryPolicy.the().leave(r1); }
    }

    public int unlock(int fd,
		      long start,
		      long size) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.unlock(fd, start, size);
        } finally { MemoryPolicy.the().leave(r1); }
    }

    public int renameTo(Oop oname, Oop nname) {
	// can't use scratch pad because of reflective invocation
	return io.renameTo(oname, nname);
    }
    public long getLastModified(Oop name) {
	// can't use scratch pad because of reflective invocation
	return io.getLastModified(name);
    }
    public int setLastModified(Oop name, long time) {
	// can't use scratch pad because of reflective invocation
	return io.setLastModified(name, time);
    }

    /**
     * This method intentionally fails to invoke a constructor.  It is
     * used in java.io.ObjectInputStream, and probably should not be
     * used anywhere else.
     **/
    public Oop allocateObject(Oop clazz) {
	return csa.allocateObject((Blueprint.Scalar) class2bp(clazz));
    }

    /**
     * Another peculiar method needed for deserialization
     **/
    public Oop callConstructor(Oop clazz, Oop instance) {
 	Type.Scalar type
 	    = (Type.Scalar) clazz.getBlueprint().getType().getInstanceType();
	Method defaultCtor = type.getMethod(JavaNames.INIT_V);
	// FIXME: should we throw NoSuchMethodErorr, or return it?
	if (defaultCtor == null)
	    csa.generateThrowable(Throwables.NO_SUCH_METHOD_ERROR, 0);

	InvocationMessage imsg = new InvocationMessage(defaultCtor);

	ReturnMessage rmsg = imsg.invoke(instance);
	return rmsg.getException();
    }

    public int open(Oop name, int flags, int mode) {
	// creates an IODescriptor, so can't be in scratchpad
	return io.open(name, flags, mode);
    }
    public int mkstemp(Oop template) {
	// creates an IODescriptor, so can't be in scratchpad
	return io.mkstemp(template);
    }
    /** returns -2 on error and -1 on EOF */
    public int readOneByte(int fd, boolean block) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
	    byte[] oneByte=new byte[1];
	    int res=io.read(fd, VM_Address.fromObject(oneByte).asOop(), 0, 1, block);
	    if (res<0) {
		return -2;
	    }
	    if (res==0) {
		return -1;
	    }
	    if (oneByte[0]<0) {
		return oneByte[0]+256;
	    }
	    return oneByte[0];
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int read(int fd, Oop buf, int byteOffset, int byteCount, boolean block) {
        // read does its own scratchpad magic, if necessary
        return io.read(fd, buf, byteOffset, byteCount, block);
    }
    public long skip(int fd,long offset,boolean block) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.skip(fd,offset,block);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public long length(int fd) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.length(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int ftruncate(int fd,long size) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.ftruncate(fd,size);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int fsync(int fd) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.fsync(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public boolean isValid(int fd) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.isValid(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public void forceValid(int fd) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            io.forceValid(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public void ecrofValid(int fd) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            io.ecrofValid(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public long lseek(int fd,long offset,int whence) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.lseek(fd,offset,whence);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int writeOneByte(int fd,int b,boolean block) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
	    byte[] oneByte=new byte[]{(byte)b};
        return io.write(fd, VM_Address.fromObject(oneByte).asOop(), 0, 1, block);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    

static    byte[] oneByte=new byte[]{(byte)1};

    public int WRITE(int fd,int b,boolean block) {
        
            return Native.write(fd, oneByte, 1);
            }

    public int CLOSE(int fd ) {
        return Native.close(fd);
    }  
    
    public int OPEN2(int f, int m) {
        // | NativeConstants.O_NONBLOCK
       return Native.open("/dev/dsp".getBytes(), f , m);
    }

    public int write(int fd, Oop buf, int byteOffset, int byteCount,boolean block) {
        // write does its own scratchpad magic, if necessary
        return io.write(fd, buf, byteOffset, byteCount, block);
    }
    public int rewind(int fd) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.rewind(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int close(int fd) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.close(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int cancel(int fd) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.cancel(fd);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int pipe(Oop fds) {
	// creates an IODescriptor, so can't be in scratchpad
	return io.pipe(fds);
    }
    public int socketpair(int domain,int type,int protocol,Oop sv) {
	// creates an IODescriptor, so can't be in scratchpad
	return io.socketpair(domain,type,protocol,sv);
    }
    public int socket(int domain,int type,int protocol) {
	// creates an IODescriptor, so can't be in scratchpad
	return io.socket(domain,type,protocol);
    }
    public int bind(int sock,Oop address) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.bind(sock,address);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int listen(int sock,int queueSize) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.listen(sock,queueSize);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int connect(int sock,Oop address) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.connect(sock,address);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int accept(int sock,Oop address,boolean block) {
	// creates an IODescriptor, so can't be in scratchpad
	return io.accept(sock,address,block);
    }
    public int getsockname(int sock,Oop address) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getsockname(sock,address);
        } finally { MemoryPolicy.the().leave(r1); }
    }
    public int getpeername(int sock,Oop address) {
        Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.getpeername(sock,address);
        } finally { MemoryPolicy.the().leave(r1); }
    }

    /**
     * mmap equivalent.  The address returned is the 'RawData', which
     * is not quite used yet in classpath.  So far, we're probably
     * pretty free to define what it is and how it is to be used.
     * VM_Address/Opaque style use is probably the most plausible
     * thing for now.  Note that the GC must know about this thing
     * since there is going to be a Reference to the mmaped
     * area.
     *
     * @param prot prot to pass to mmap
     * @param flags flags to pass to mmap
     */
    public Oop memmap(int fd,
		      int prot,
		      int flags,
		      long position, 
		      int size) {
	VM_Address addr
	    = Native.mmap(null,
			  size,
			  prot,
			  flags,
			  fd,
			  (int) position); // FIXME: broken on 64 bit filesystems!
	
	// FIXME: we probably want to notify the GC here!
	return addr.asOop();
    }

    public int unmap(Oop rawData,
		     int size) {
	// FIXME: unregister with GC
	return Native.munmap(VM_Address.fromObject(rawData),
			     size);
    }
      
    public int select(Oop selectHandle,
		      long timeout,
		      Oop resultSetHandle) {
	throw new Error("Not implemented!");
    }

    public void registerSelector(Oop selectHandle,
				 int fd,
				 int oops,
				 Oop cpCookie) {
	throw new Error("Not implemented!");
    }

    public void unregisterSelector(Oop selectHandle,
				   int fd,
				   Oop cpCookie) {
	throw new Error("Not implemented!");
    }

    public Oop createSelectCookie() {
	throw new Error("Not implemented!");
    }

    public void releaseSelectCookie(Oop selectHandle) {
	throw new Error("Not implemented!");
    }


    // UDP support, see gnu.java.net.LibraryImports
    public int receive(int fd,
		       Oop source_addr,
		       Oop dstArray,
		       int len,
		       boolean blocking) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.receive(fd,
			      source_addr,
			      dstArray,
			      len,
			      blocking);
        } finally { MemoryPolicy.the().leave(r1); }
    }

    public int sendto(Oop address,
		      Oop buf,
		      int off,
		      int len) {
	Object r1 = MemoryPolicy.the().enterScratchPadArea();
        try {
            return io.sendto(address,
			 buf,
			 off, 
			 len);
        } finally { MemoryPolicy.the().leave(r1); }
    }

    // Java threading functions

    public Oop /*VMThread*/ createVMThread(Oop javaThread) 
	{
        return asOop(getJavaDispatcher().createVMThread(javaThread));
    }

    public Oop /*VMThread*/ createVMThread(Oop javaThread, boolean noHeap)  {
        return asOop(getRTJavaDispatcher().createVMThread(javaThread, noHeap));
    }


    public void bindPrimordialJavaThread(Oop javaThread) 
	{
        getJavaDispatcher().bindPrimordialJavaThread(javaThread);
    }

    public Oop getCurrentJavaThread() {
        return asOop(getJavaDispatcher().getCurrentJavaThread());
    }


    public Oop /*VMThread*/ getCurrentVMThread() 
	{
        return asOop(getJavaDispatcher().getCurrentVMThread());    
    }
	
    public void yieldCurrentThread() {
        getJavaDispatcher().yieldCurrentThread();
    }

    public boolean delayCurrentThread(long millis, int nanos)
    {
        return getJavaDispatcher().delayCurrentThread(millis, nanos);
    }

    public void startThread(Oop vmThread) {
        getJavaDispatcher().startThread((JavaOVMThread)vmThread);
    }

    public void interruptThread(Oop vmThread) {
        getJavaDispatcher().interruptThread((JavaOVMThread)vmThread);
    }

    public void terminateCurrentThread() {
        getJavaDispatcher().terminateCurrentThread();
    }

    public boolean isAlive(Oop vmThread) {
        return getJavaDispatcher().isAlive((JavaOVMThread)vmThread);    
    }

    public boolean setPriority(Oop vmThread, int prio) {
        // if the thread is no longer alive when we try to set the priority
        // then an exception will be thrown. We can't propagate the exception
        // to the user domain so we modify the signature to return true on
        // success and false otherwise.
        try {
            getJavaDispatcher().setPriority((JavaOVMThread)vmThread, prio);
            return true;
        }
        catch(OVMError.IllegalState ex) {
            return false;
        }
    }

    public int getPriority(Oop vmThread) {
        return getJavaDispatcher().getPriority((JavaOVMThread)vmThread);
    }


    /** 
     * Queries the maximum allowed priority in the current config.
     * Used by the JVM to find a priority for use by ultra-high priority
     * system threads.
     * What this returns depends on the configuration as a real-time
     * config establishes a different priority range than a non-RT config
     * and a RT dispatcher exposes the maximum RT priority via a different
     * method.
     */
    public int getMaximumPriority() {
        JavaDispatcher d = getJavaDispatcher();
        // Note: we assume that in a RT config we are a RT kernel thread,
        // even if we are not a RT JVM thread
        if (d instanceof RealtimeJavaDispatcher) {
            return ((RealtimeJavaDispatcher)d).getMaxRTPriority();
        }
        else {
            return d.getMaxPriority();
        }
    }

    // Low-level printing interface that can be used early in the 
    // initialisation process either from user or executive domain.
    public void printString(Oop msg) {
  	BasicIO.err.print(dom.getString(msg).toString());
    }


    // raw printing interface that can be used by the user-domain to
    // avoid string conversions and allocations

    public void printInt(int i) {
        Native.print_int(i);
    }

    public void printLong(long l) {
        Native.print_long(l);
    }

    public void printCharAsByte(char c) {
        Native.print_char((byte)c);
    }


    public boolean is_directory(Oop msg) {
	// can't use scratch pad because of reflective invocation
	return io.isDirectory(msg);
    }

    public boolean is_plainfile(Oop msg) {
	// can't use scratch pad because of reflective invocation
	return io.isPlainFile(msg);
    }

    public Oop intern(Oop string) {
	return ((JavaUserDomain) dom).internString(string);
    }

    public void halt(int reason) { 
	Executive.shutdown(reason);
    }


    /** Return the size occupied by a monitor object when it is allocated.
        This has to account for all the memory used by the monitor in the
        memory area of the object to which the monitor belongs.
    */
    public long sizeOfMonitor() {
        return monitorFactory.monitorSize();
    }

     /**
      * Return a size estimate in bytes for an instance of the given type.
      */
    public long sizeOf(Oop classOop) {
         return (long) class2bp(classOop).getFixedSize();
    }

    /**
     * Returns a size estimate in bytes for an array of references of the
     * given length. 
     */
    public long sizeOfReferenceArray(int length) {
        Type compType = dom.commonTypes().java_lang_Object;
        return type2ArrayBp(compType).computeSizeFor(length);    
    }

    /**
     * Returns a size estimate in bytes for an array of the given length for
     * the given primitive type. 
     */
    public long sizeOfPrimitiveArray(int length, Oop primitiveClass) {
        Type compType = class2type(primitiveClass);
        return type2ArrayBp(compType).computeSizeFor(length);
    }

    private Blueprint.Array type2ArrayBp(Type compType) {
	Type.Array arr = ((S3Domain)dom).makeType(compType, 1);
	return dom.blueprintFor(arr).asArray();
    }


    // All this reflection stuff is only for JavaUserDomain - we should have
    // domain specific RTE classes to clean this up - DH

    /**
     * @return an ED array of length <code>length</code> containing elements
     * whose class name is <code>componentClassName</code>
     */
    public Oop createObjectArray(Oop componentClass, int length){
	// Note: if componentClass exists, the type has already been
	// initialized
	Type eltType =
	    componentClass.getBlueprint().getType().getInstanceType();
	Type.Array arrayType = ((S3Domain) dom).makeType(eltType, 1).asArray();
	// FIXME: A cheap hack.  -ud-reflective-classes contains both
	// classes that are referenced reflectively and array types
	// that are allocated reflectively.  However, we never need
	// the Class for an array type to  allocate the array reflectively.
	((S3JavaUserDomain) dom).observeForName(arrayType.getName());
	Blueprint arrayBP = dom.blueprintFor(arrayType);
	csa.initializeBlueprint(arrayBP.getSharedState());
	return csa.allocateArray(arrayBP.asArray(), length);
    }

    // needed by gnu.java.net.LibraryGlue to work around protected field in
    // java.net.SocketImpl
     public Oop hackFieldForName(Oop object, Oop nameOop) {
        String name=UnicodeBuffer.factory().toString(dom.getString(nameOop));
        Field.Iterator iter= object.getBlueprint().getType().asCompound().fieldIterator();
        while (iter.hasNext()) {
            Field f=iter.next();
            if (f.getSelector().getName().equals(name)) {
                return VM_Address.fromObject(f).asOop();
            }
        }
        throw Executive.panic("Asked to look for "+name+", but couldn't find it.");
    }
    // Field.setInt
    public void fieldSetInt(Oop fieldOop, Oop object, int value) {
	Field.Integer field = (Field.Integer)fieldOop;
	field.set(object, value);
    }
    public void fieldSetBoolean(Oop fieldOop, Oop object, boolean value) {
	Field.Boolean field = (Field.Boolean)fieldOop;
	field.set(object, value);
    }
    public void fieldSetByte(Oop fieldOop, Oop object, byte value) {
	Field.Byte field = (Field.Byte)fieldOop;
	field.set(object, value);
    }
    public void fieldSetShort(Oop fieldOop, Oop object, short value) {
	Field.Short field = (Field.Short)fieldOop;
	field.set(object, value);
    }
    public void fieldSetChar(Oop fieldOop, Oop object, char value) {
	Field.Character field = (Field.Character)fieldOop;
	field.set(object, value);
    }
    public void fieldSetFloat(Oop fieldOop, Oop object, float value) {
	Field.Float field = (Field.Float)fieldOop;
	field.set(object, value);
    }
    public void fieldSetLong(Oop fieldOop, Oop object, long value) {
	Field.Long field = (Field.Long)fieldOop;
	field.set(object, value);
    }
    public void fieldSetDouble(Oop fieldOop, Oop object, double value) {
	Field.Double field = (Field.Double)fieldOop;
	field.set(object, value);
    }
    public void fieldSetReference(Oop fieldOop, Oop object, Oop value) {
	Field.Reference field = (Field.Reference)fieldOop;
	field.set(object, value);
    }

    //Field.getxxxxx(Object o)

    public int fieldGetInt(Oop fieldOop, Oop object) {
	Field.Integer field = (Field.Integer)fieldOop;
	return field.get(object);
    }
    public boolean fieldGetBoolean(Oop fieldOop, Oop object) {
	Field.Boolean field = (Field.Boolean)fieldOop;
	return field.get(object);
    }
    public byte fieldGetByte(Oop fieldOop, Oop object) {
	Field.Byte field = (Field.Byte)fieldOop;
	return field.get(object);
    }
    public short fieldGetShort(Oop fieldOop, Oop object) {
	Field.Short field = (Field.Short)fieldOop;
	return field.get(object);
    }
    public char fieldGetChar(Oop fieldOop, Oop object) {
	Field.Character field = (Field.Character)fieldOop;
	return field.get(object);
    }
    public float fieldGetFloat(Oop fieldOop, Oop object) {
	Field.Float field = (Field.Float)fieldOop;
	return field.get(object);
    }
    public long fieldGetLong(Oop fieldOop, Oop object) {
	Field.Long field = (Field.Long)fieldOop;
	return field.get(object);
    }
    public double fieldGetDouble(Oop fieldOop, Oop object) {
	Field.Double field = (Field.Double)fieldOop;
	return field.get(object);
    }

    public Oop fieldGetReference(Oop fieldOop, Oop object) {
	Field.Reference field = (Field.Reference)fieldOop;
        //should we return the value wrapped in the proper type of Object ?
	return field.get(object);
    }


    public Oop nameForField(Oop field_)  {
 	Field field = (Field)field_;
 	return nameForSelector(field.getSelector());
    }
 
    /**
     * Returns the Class object for the type represented by the given field.
     */
    public Oop typeForField(Oop field_) {
 	try {
 	    Field field = (Field)field_;
 	    return type2class(field.getType());
 	} catch (LinkageException e) {
	    csa.processThrowable(e.toLinkageError(dom));
 	    throw Executive.panic("unreachable");
 	}
    }
    
    //----- TEMPORARILY HERE


    public Oop newInstance(Oop constructorOop, Oop argArray, Oop callerClass) throws PragmaPARSafe
	{
	Method method = (Method) constructorOop;
	return ((JavaUserDomain)dom).
            newInstance(method, argArray, callerClass);
    }

    public Oop invokeMethod(Oop receiver, Oop theMethod,
			    Oop argArray, Oop callerClass) {
	Method method = (Method) theMethod;
	return ((JavaUserDomain) dom).invokeMethod(receiver, method,
						   argArray, callerClass);
    }


    // NOTE:  Defining a reflective wrapper in a domain declares that
    // the wrapped types actually exist in that domain.  If the types
    // do not, in fact, exist, bad things can (and often do) happen.
    private ReflectiveConstructor makeField = dom.isExecutive() ? null : 
	new ReflectiveConstructor(dom, JavaNames.java_lang_reflect_Field,
				  new TypeName[] {
				      JavaNames.org_ovmj_java_Opaque,
				      JavaNames.java_lang_Class,
                                      TypeName.INT,
				  });
    private ReflectiveArray fieldArr = dom.isExecutive() ? null :
	new ReflectiveArray(dom, JavaNames.java_lang_reflect_Field);
    
    private ReflectiveConstructor makeMethod = dom.isExecutive() ? null :
	new ReflectiveConstructor(dom, JavaNames.java_lang_reflect_Method,
				  new TypeName[] {
				      JavaNames.org_ovmj_java_Opaque,
				      JavaNames.java_lang_Class,
				      JavaNames.arr_java_lang_Class,
				      JavaNames.arr_java_lang_Class,
				      JavaNames.java_lang_Class,
				      JavaNames.java_lang_String,
				      TypeName.INT
				  });
    private ReflectiveArray methodArr = dom.isExecutive() ? null :
	new ReflectiveArray(dom, JavaNames.java_lang_reflect_Method);

    private ReflectiveConstructor makeConstructor = dom.isExecutive() ? null :
	new ReflectiveConstructor(dom, JavaNames.java_lang_reflect_Constructor,
				  new TypeName[] {
				      JavaNames.org_ovmj_java_Opaque,
				      JavaNames.java_lang_Class,
				      JavaNames.arr_java_lang_Class,
				      JavaNames.arr_java_lang_Class,
				      JavaNames.java_lang_String,
				      TypeName.INT
				  });
    private ReflectiveArray constructorArr = dom.isExecutive() ? null :
	new ReflectiveArray(dom, JavaNames.java_lang_reflect_Constructor);
    
    /**
     * The implementation of java.lang.Class.getDeclaredFields 
     */
    public Oop getDeclaredFields(Oop declaringClass) {
	Blueprint bp = declaringClass.getBlueprint().getInstanceBlueprint();
	Type.Reference declaringType = (Type.Reference)bp.getType();
	ArrayList fieldList = new ArrayList();


	InstantiationMessage msg = makeField.makeMessage();

        // first the instance fields
	for (Field.Iterator it = declaringType.localFieldIterator();
	     it.hasNext();)
	{
	    Field df = it.next();
	    Mode.Field mode = df.getMode();
	    try { // Reflectively call java.lang.reflect.Field's constructor
		msg.getInArgAt(0).setOop(asOop(df));
		msg.getInArgAt(1).setOop(declaringClass);
		msg.getInArgAt(2).setInt(mode.getMode());
		ReturnMessage ret = msg.instantiate();
                ret.rethrowWildcard(); // may not return
		Oop rf = ret.getReturnValue().getOop();
                fieldList.add(rf);
	    } catch(LinkageException e) {
		csa.processThrowable(e.toLinkageError(dom));
	    }
	}

        // now the static fields
	Type.Class sharedStateType = declaringType.getSharedStateType();
	for (Field.Iterator it = sharedStateType.localFieldIterator();
	     it.hasNext();)
	{
	    Field df = it.next();
	    Mode.Field mode = df.getMode();
	    try { // Reflectively call java.lang.reflect.Field's constructor
		msg.getInArgAt(0).setOop(asOop(df));
		msg.getInArgAt(1).setOop(declaringClass);
		msg.getInArgAt(2).setInt(mode.getMode());
		ReturnMessage ret = msg.instantiate();
                ret.rethrowWildcard(); // may not return
		Oop rf = ret.getReturnValue().getOop();
                fieldList.add(rf);
	    } catch(LinkageException e) {
		csa.processThrowable(e.toLinkageError(dom));
	    }
	}


	// Allocate the Field array
	int fieldListSize = fieldList.size();
	Oop fieldListArray = fieldArr.make(fieldListSize);
	for(int i = 0; i < fieldListSize ; i++)
	    fieldArr.setOop(fieldListArray, i, asOop(fieldList.get(i)));
	return fieldListArray;
    }

    /**
     * The implementation of java.lang.Class.getFields
     */
    public Oop getFields(Oop declaringClass) {
	Blueprint bp = declaringClass.getBlueprint().getInstanceBlueprint();
	Type.Reference declaringType = (Type.Reference)bp.getType();
	ArrayList fieldList = new ArrayList();

	InstantiationMessage msg = makeField.makeMessage();

        // first the instance fields
	for(Field.Iterator it = declaringType.fieldIterator(); it.hasNext();) {
	    Field df = it.next();
	    if (!df.getMode().isPublic())
		continue;
	    Mode.Field mode = df.getMode();
	    try { // Reflectively call java.lang.reflect.Field's constructor
		msg.getInArgAt(0).setOop(asOop(df));
		msg.getInArgAt(1).setOop(declaringClass);
		msg.getInArgAt(2).setInt(mode.getMode());
		ReturnMessage ret = msg.instantiate();
                ret.rethrowWildcard(); // may not return
		Oop rf = ret.getReturnValue().getOop();
                fieldList.add(rf);
	    } catch(LinkageException e) {
		csa.processThrowable(e.toLinkageError(dom));
	    }
	}

        // now the static fields
	Type.Class sharedStateType = declaringType.getSharedStateType();
	for (Field.Iterator it = sharedStateType.fieldIterator(); 
             it.hasNext();) {
	    Field df = it.next();
	    if (!df.getMode().isPublic())
		continue;
	    Mode.Field mode = df.getMode();
	    try { // Reflectively call java.lang.reflect.Field's constructor
		msg.getInArgAt(0).setOop(asOop(df));
		msg.getInArgAt(1).setOop(declaringClass);
		msg.getInArgAt(2).setInt(mode.getMode());
		ReturnMessage ret = msg.instantiate();
                ret.rethrowWildcard(); // may not return
		Oop rf = ret.getReturnValue().getOop();
                fieldList.add(rf);
	    } catch(LinkageException e) {
		csa.processThrowable(e.toLinkageError(dom));
	    }
	}


	// Allocate the Field array
	int fieldListSize = fieldList.size();
	Oop fieldListArray = fieldArr.make(fieldListSize);
	for(int i = 0; i < fieldListSize ; i++)
	    fieldArr.setOop(fieldListArray, i, asOop(fieldList.get(i)));
	return fieldListArray;
    }

    /**
     * The implementation of java.lang.Class.getDeclaredMethods 
     */
    public Oop getDeclaredMethods(Oop declaringClass) {
	Blueprint bp = declaringClass.getBlueprint().getInstanceBlueprint();
	Type.Reference declaringType = (Type.Reference)bp.getType();
	ArrayList methodList = new ArrayList();

	InstantiationMessage msg = makeMethod.makeMessage();
	for (Method.Iterator it = declaringType.localMethodIterator();
	     it.hasNext();)
	{
	    Method m = it.next();
	    Mode.Method mode = m.getMode();

	    try { // Reflectively call java.lang.reflect.Method's constructor
		msg.getInArgAt(0).setOop(VM_Address.fromObject(m).asOop());
		msg.getInArgAt(1).setOop(declaringClass);
		msg.getInArgAt(2).setOop(getMethodParameters(m));
		msg.getInArgAt(3).setOop(getMethodExceptions(m));
		msg.getInArgAt(4).setOop(type2class(m.getReturnType()));
		msg.getInArgAt(5).setOop(nameForSelector(m.getExternalSelector()));
		msg.getInArgAt(6).setInt(mode.getMode());

		ReturnMessage ret = msg.instantiate();
                ret.rethrowWildcard(); // may not return
		Oop res = ret.getReturnValue().getOop();
		methodList.add(res);
	    } catch(LinkageException e) {
		csa.processThrowable(e.toLinkageError(dom));
	    }
	}

	/* Quick'n dirty copy paste for the static methods
	 * on the other hand, most of the other methods that are
	 * method/constructor related will soon be removed */
	Type.Class sharedStateType = declaringType.getSharedStateType();
	for(Method.Iterator it = sharedStateType.localMethodIterator();
	    it.hasNext();)
	{
	    Method m = it.next();
	    Mode.Method mode = m.getMode();

	    try { // Reflectively call java.lang.reflect.Method's constructor
		msg.getInArgAt(0).setOop(VM_Address.fromObject(m).asOop());
		msg.getInArgAt(1).setOop(declaringClass);
		msg.getInArgAt(2).setOop(getMethodParameters(m));
		msg.getInArgAt(3).setOop(getMethodExceptions(m));
		msg.getInArgAt(4).setOop(type2class(m.getReturnType()));
		msg.getInArgAt(5).setOop(nameForSelector(m.getExternalSelector()));
		msg.getInArgAt(6).setInt(mode.getMode());

		ReturnMessage ret = msg.instantiate();
                ret.rethrowWildcard(); // may not return
		Oop res = ret.getReturnValue().getOop();
		methodList.add(res);
	    } catch(LinkageException e) {
		csa.processThrowable(e.toLinkageError(dom));
	    }
	}	

	// Allocate the Method array
	int methodListSize = methodList.size();
	Oop methodListArray = methodArr.make(methodListSize);
	for(int i = 0; i < methodListSize ; i++)
	    methodArr.setOop(methodListArray, i, asOop(methodList.get(i)));
	return methodListArray;
    }

    /**
     * The implementation of java.lang.Class.getDeclaredConstructors 
     */
    public Oop getDeclaredConstructors(Oop declaringClass)
    {
	Blueprint bp = declaringClass.getBlueprint().getInstanceBlueprint();
	Type.Reference declaringType = (Type.Reference)bp.getType();
	ArrayList consList = new ArrayList();
	InstantiationMessage msg = makeConstructor.makeMessage();

	for (Method.Iterator it = declaringType.localMethodIterator();
	     it.hasNext(); )
	{
	    Method c = it.next();
	    Mode.Method mode = c.getMode();
	    int cName = c.getExternalNameIndex();

	    // check that we have a constructor, i.e name == "<init>"
	    if (!c.getSelector().isConstructor())
		continue;

	    try { // Reflectively call the j.l.r.Constructor constructor
		msg.getInArgAt(0).setOop(VM_Address.fromObject(c).asOop());
		msg.getInArgAt(1).setOop(declaringClass);
		msg.getInArgAt(2).setOop(getMethodParameters(c));
		msg.getInArgAt(3).setOop(getMethodExceptions(c));
		msg.getInArgAt(4).setOop(dom.internString(cName));
		msg.getInArgAt(5).setInt(mode.getMode()) ;

		ReturnMessage ret = msg.instantiate();
                ret.rethrowWildcard(); // may not return
		Oop res = ret.getReturnValue().getOop();
		consList.add(res);
	    } catch(LinkageException e) {
		csa.processThrowable(e.toLinkageError(dom));
	    }
	}

	// Allocate the Constructor array
	int consListSize = consList.size();
	Oop consListArray = constructorArr.make(consListSize);
	for(int i = 0; i < consListSize ; i++)
	    constructorArr.setOop(consListArray, i, asOop(consList.get(i)));
	return consListArray;
    }


    

    // --------------- helper methods for reflection  --------------- //

    private ReflectiveArray classArr =
	new ReflectiveArray(dom, JavaNames.java_lang_Class);

    /**
     * Return a java.lang.Class array of the parameters of the method.
     */
    private Oop getMethodParameters(ovm.core.domain.Method method)
	throws LinkageException
    {
	Type.Context ctx = method.getDeclaringType().getContext();
	Descriptor.Method desc = method.getExternalSelector().getDescriptor();
	int nb = desc.getArgumentCount();
	Oop ret = classArr.make(nb);
	for (int i=0; i < nb; i++) {
	    Type t = ctx.typeFor(desc.getArgumentType(i));
	    classArr.setOop(ret, i, type2class(t));
	}
	return ret;
    }

    /**
     * Return a java.lang.Class array of the exceptions thrown by the method.
     */
    private Oop getMethodExceptions(ovm.core.domain.Method method)
	throws LinkageException
    {
	int nb = method.getThrownTypeCount();
	Oop ret = classArr.make(nb);
	for (int i=0; i < nb; i++)
	    classArr.setOop(ret, i, type2class(method.getThrownType(i)));
	return ret;
    }

    public final Type class2type(Oop clazz) {
	return clazz.getBlueprint().getType().getInstanceType();
    }

    private Blueprint class2bp(Oop clazz) {
	return clazz.getBlueprint().getInstanceBlueprint();
    }

    /**
     * Return the java.lang.Class object associated with a type.
     * This does not initialize the class.
     * FIXME: What is the story on unresolved types?  Should this
     * throw LinkageException?
     */
    private Oop type2class(Type t) {
	Oop retClass;
	S3Type retType = (S3Type)  t;
	Oop shSt = dom.blueprintFor(retType).getSharedState();
	retClass = retType.getClassMirror(csa, shSt);
	return retClass;
    }

    private Oop nameForSelector(Selector sel) {
	return dom.internString(sel.getNameIndex());
    }


    // -------------------------------------------------------------- //

    ReflectiveField.Reference System_in = dom.isExecutive() ? null :
	new ReflectiveField.Reference(dom,
				      JavaNames.java_io_InputStream,
				      JavaNames.java_lang_System.getGemeinsamTypeName(),
				      "in");
    public void setIn(Oop in) {
	System_in.set(null, in);
    }

    ReflectiveField.Reference System_out = dom.isExecutive() ? null :
	new ReflectiveField.Reference(dom,
				      JavaNames.java_io_PrintStream,
				      JavaNames.java_lang_System.getGemeinsamTypeName(),
				      "out");
    public void setOut(Oop out) {
	System_out.set(null, out);
    }
    ReflectiveField.Reference System_err = dom.isExecutive() ? null :
	new ReflectiveField.Reference(dom,
				      JavaNames.java_io_PrintStream,
				      JavaNames.java_lang_System.getGemeinsamTypeName(),
				      "err");
    public void setErr(Oop out) {
	System_err.set(null, out);
    }

    public long getCurrentTime() {
	return Native.getCurrentTime();
    }

    public long getTimeStamp() {
        return Native.getTimeStamp();
    }
    
    public void ovm_outb( int value, int address ) {
        Native.ovm_outb( value, address );
    }

    public int ovm_inb( int address ) {
        return Native.ovm_inb( address );
    }
    
    // Runtime/GC related
    public int availableProcessors() {
	return Processor.getProcessors().length;
    }

    public long freeMemory()  {
	VM_Area heap = MemoryManager.the().getHeapArea();
	return heap.size() - heap.memoryConsumed();
    }

    public long totalMemory() {
	return MemoryManager.the().getHeapArea().size();
    }
    public long maxMemory() {
	// The maximum size of the heap is given by totalMemory(), but
	// this is not Runtime.maxMemory.  We can consume an arbitrary
	// amount of memory with thread stacks.
	return Long.MAX_VALUE;
    }

    public void gc() {
	MemoryManager.the().garbageCollect();
    }
    
    public void runGCThread() {
	MemoryManager.the().runGCThread();
    }
    
    public boolean canCollect() {
	return MemoryManager.the().canCollect();
    }
    
    public void setShouldPause(boolean shouldPause) {
	MemoryManager.the().setShouldPause(shouldPause);
    }
    
    public boolean needsGCThread() {
	return MemoryManager.the().needsGCThread();
    }
    
    int gcThreadPriority;
    int gcThreadLowPriority;
    int gcTimerMutatorCounts;
    int gcTimerCollectorCounts;
    long gcTimerPeriod;
    
    public void setGCTimerMutatorCounts(int gcTimerMutatorCounts) {
	this.gcTimerMutatorCounts=gcTimerMutatorCounts;
    }
    
    public void setGCTimerCollectorCounts(int gcTimerCollectorCounts) {
	this.gcTimerCollectorCounts=gcTimerCollectorCounts;
    }
    
    public void setGCThreadPriority(int gcThreadPriority) {
        this.gcThreadPriority = gcThreadPriority;
    }    
    
    public void setGCThreadLowPriority(int gcThreadLowPriority) {
        this.gcThreadLowPriority = gcThreadLowPriority;
    }        

    public void setGCTimerPeriod(long gcTimerPeriod) {
        this.gcTimerPeriod = gcTimerPeriod;
    }    
    
    public int getGCTimerMutatorCounts() {
	return gcTimerMutatorCounts;
    }
    
    public int getGCTimerCollectorCounts() {
	return gcTimerCollectorCounts;
    }
    
    public int getGCThreadPriority() {
        return gcThreadPriority;
    }

    public int getGCThreadLowPriority() {
        return gcThreadLowPriority;
    }

    public long getGCTimerPeriod() {
        return gcTimerPeriod;
    }

    // heap based finalization
    public void registerFinalizer(Oop oop) {
	((JavaUserDomain) dom).registerFinalizer(oop);
    }
    public void runFinalization() {
	((JavaUserDomain) dom).runFinalizers();
    }

    public Oop getCurrentClassLoader() {
	return null;
    }

    // count all the UD frames on the stack
    public int getClassContextDepth() {
        // walk the call stack skipping all ED frames
        Context ctx = Context.getCurrentContext();
        int depth = 0;

	// FIXME: the call to clone here is not GC-safe, see
	// https://lists.purdue.edu/mailman/private/ovm/2007-June/008480.html
	for (Activation act = (Activation) ctx.getCurrentActivation().clone();
	     act != null;
	     act = act.caller(act)) {
            Code code = act.getCode();
            Type.Compound declType = code.getMethod().getDeclaringType();
            if (declType.getDomain() != dom)
		continue;
	    Selector.Method sel = code.getSelector();
	    Blueprint bp = dom.blueprintFor(declType);
	    if (PragmaForwardCallingContext.declaredBy(sel, bp))
		continue;
	    
	    depth++;
	}
//      d("class context depth is " + depth);
	return depth;
    }


    // fill in classArray with all the UD classes from the stack frame
    // skipping over the number requested
    public void getClassContext(Oop classArray, int skipFrames) 
        {
        Blueprint.Array arrayBP = classArray.getBlueprint().asArray();
        int length = arrayBP.getLength(classArray);
//         d("Array length is " + length);
//         d("Skip frames is " + skipFrames);
        Context ctx = Context.getCurrentContext();
        int i = 0;
        int throwIndex = -1;

	// FIXME: the call to clone here is not GC-safe, see
	// https://lists.purdue.edu/mailman/private/ovm/2007-June/008480.html
	for (Activation act = (Activation) ctx.getCurrentActivation().clone();
	     act != null;
	     act = act.caller(act)) {
//             d("Next activation");
	    Code code = act.getCode();
//             d("Method object: " + code.getMethod());
	    Type.Compound declType = code.getMethod().getDeclaringType();

	    if (declType.getDomain() != dom)
		continue;

	    Selector.Method sel = code.getSelector();
	    Blueprint bp = dom.blueprintFor(declType);
	    if (PragmaForwardCallingContext.declaredBy(sel, bp))
		continue;

	    if (declType.isSharedState())
		declType = declType.getInstanceType().asCompound();

	    if (skipFrames-- > 0) {
		continue;
	    }
                    
	    if (i >= length) {
		throwIndex = i;
		break;
	    }
                    
	    try {
// 		if (i < 3)
// 		    BasicIO.out.println("classContext[" + i + "] = " + sel);
//		arrayBP.addressOfElement(classArray, i++)
//			.setAddress(VM_Address.fromObject(((S3Type)declType).getClassMirror()));
                MemoryManager.the().setReferenceArrayElement(classArray, i++, ((S3Type)declType).getClassMirror());
	    }
	    catch (OVMError e) {
		//                     d("Error processing " + declType);
		throw e;
	    }
	}
	if (throwIndex == -1) 
	    return;

        // need to throw
        csa.generateThrowable(Throwables.ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION,
			      i);
    }

    public boolean classContextSkipsCaller() {
        Context ctx = Context.getCurrentContext();
	// FIXME: the call to clone here is not GC-safe, see
	// https://lists.purdue.edu/mailman/private/ovm/2007-June/008480.html
	Activation act = (Activation) ctx.getCurrentActivation().clone();
	act = act.caller(act);
	act = act.caller(act);

	Code code = act.getCode();
	Type.Compound declType = code.getMethod().getDeclaringType();
	assert (declType.getDomain() == dom);

	Selector.Method sel = code.getSelector();
	Blueprint bp = dom.blueprintFor(declType);
	boolean ret = PragmaForwardCallingContext.declaredBy(sel, bp);
// 	if (ret)
// 	    BasicIO.out.println("skip caller: " + sel);
	return ret;
    }
	
    public void breakpoint(Oop inspectable) {
        Interpreter.breakpoint( inspectable);
    }

    // Realtime Java VM functions


    private ReflectiveField.Reference thread_vmThread = dom.isExecutive() ? 
        null :
	new ReflectiveField.Reference(dom,
				      JavaNames.java_lang_VMThread,
				      JavaNames.java_lang_Thread,
				      "vmThread");

    private ReflectiveField.Reference vmThread_edThread = dom.isExecutive() ?
	null :
	new ReflectiveField.Reference(dom,
				      JavaNames.org_ovmj_java_Opaque,
				      JavaNames.java_lang_VMThreadBase,
				      "vmThread");

    private ReflectiveField.Boolean vmThread_interrupted = dom.isExecutive() ? 
        null :
	new ReflectiveField.Boolean(dom,
                                    JavaNames.java_lang_VMThreadBase,
                                    "interrupted");


    private RealtimeJavaDispatcher getRTJavaDispatcher() {
        if (jd != null && jd instanceof RealtimeJavaDispatcher) {
            return (RealtimeJavaDispatcher) jd;
        }
        throw new OVMError.Configuration(
            "Used RealtimeJavadispatcher in a configuration that does not support it");
    }


    // no-heap support
    public void disableHeapChecksForTermination(Oop thread) {
        ((ovm.services.realtime.NoHeapRealtimeOVMThread)getVMThread(thread)).enableHeapChecks(false);
	// It is always safe to disable read barriers, but they can
	// only be enabled when MemoryManager.the().reallySupportNHRTT()
	// is true.
	MemoryManager.the().enableReadBarriers(false);
    }

    public Oop getVMThread(Oop thread) {
        return vmThread_edThread.get(thread_vmThread.get(thread));
    }

    public void threadSetInterrupted(Oop thread, boolean set) {
        vmThread_interrupted.set(thread_vmThread.get(thread), set);
    }
        
    public void startThreadDelayed(Oop vmThread, long startTime) {
      getRTJavaDispatcher().startThreadDelayed( (JavaOVMThread)vmThread, startTime);
    }

    public int delayCurrentThreadAbsolute(long nanos) {
        return getRTJavaDispatcher().delayCurrentThreadAbsolute(nanos);
    }

    public int delayCurrentThreadAbsoluteUninterruptible(long nanos) {
        return getRTJavaDispatcher().delayCurrentThreadAbsoluteUninterruptible(nanos);
    }

    public boolean setPriorityIfAllowed(Oop vmThread, int newPrio) {
         return getRTJavaDispatcher().
		  setPriorityIfAllowed((JavaOVMThread) vmThread, newPrio);
    }

    public int getMinRTPriority() {
        return getRTJavaDispatcher().getMinRTPriority();
    }

    public int getMaxRTPriority() {
        return getRTJavaDispatcher().getMaxRTPriority();
    }

    public long getClockResolution() {
	return timer.getTimerInterruptPeriod();
	//return Native.getClockResolution();
    }

    public double parseDouble(Oop s) {
	return Native.strtod_helper(dom.getUTF8CString(s));
    }

    private String[] commandlineArgumentAsExeStrings = null;

    public void setCommandlineArgumentStringArray(String[] arguments) {
	commandlineArgumentAsExeStrings = arguments;
    }
    private volatile Oop commandlineArgumentAsDomainStrings = null;
    
    public Oop getCommandlineArgumentStringArray() {
	if (commandlineArgumentAsDomainStrings != null) {
	    return commandlineArgumentAsDomainStrings;
	} else if (commandlineArgumentAsExeStrings == null) {
	    return null;
	} else {
	    Oop arr = stringArr.make(commandlineArgumentAsExeStrings.length);
	    for (int i = 0;
		 i < commandlineArgumentAsExeStrings.length;
		 i++)
	    {
		String s = commandlineArgumentAsExeStrings[i];
		stringArr.setOop(arr, i, dom.stringFromLocalizedCString(s));
	    }
	    commandlineArgumentAsDomainStrings = arr;
	    return arr;
	}
    }

    private static String defaultMainClass = null;

    public static void setDefaultMainClass(String c) {
	assert(defaultMainClass == null);
	defaultMainClass = c;
    }

    public Oop defaultMainClass() {
	return (defaultMainClass == null
		? null
		: dom.makeString(UnicodeBuffer.factory().wrap(defaultMainClass)));
    }

    /*
     * Region/scope support.
     */
    public Oop getHeapArea() {
	return asOop(MemoryManager.the().getHeapArea());
    }
    public Oop getImmortalArea() {
	return asOop(MemoryManager.the().getImmortalArea());
    }
    public Oop makeArea(Oop mirror, int size)  throws PragmaAtomic { //PARBEGIN PAREND
    	//Native.print_string("############## make area...");
        //Native.print_string("\n");
	return asOop(MemoryManager.the().makeScopedArea(mirror, size));
    }
    public Oop makeExplicitArea(int size)  throws PragmaAtomic { //PARBEGIN PAREND
	return asOop(MemoryManager.the().makeExplicitArea(size));
    }

    public Oop setCurrentArea(Oop area) {
	return asOop(MemoryManager.the().setCurrentArea((VM_Area) area));
    }
    public Oop getCurrentArea() {
        return asOop(MemoryManager.the().getCurrentArea());
    }
    public Oop setCurrentArea(int idx,Oop area) {
	return asOop(MemoryManager.the().setCurrentArea(idx, (VM_Area) area));
    }
    public Oop getCurrentArea(int idx) {
	return asOop(MemoryManager.the().getCurrentArea(idx));
    }
    public void setAllocKind(Oop clazz,int idx) {
	clazz.getBlueprint().getInstanceBlueprint().setAllocKind(idx);
    }
    public void resetArea(Oop area)  throws PragmaAtomic { //PARBEGIN PAREND
	((VM_Area) area).reset();
    }
    public void destroyArea(Oop area)  throws PragmaAtomic { //PARBEGIN PAREND
	
    	//Native.print_string("############## destroy area...");
        //Native.print_string("\n");
    	((VM_Area) area).destroy();
    }
    public boolean reallySupportNHRTT() {
	return MemoryManager.the().reallySupportNHRTT();
    }
    public boolean supportScopeAreaOf() {
	return MemoryManager.the().supportScopeAreaOf();
    }
    public boolean runFinalizers(Oop area) {
	if (area instanceof FinalizableArea) {
	    return ((FinalizableArea) area).runFinalizers();
	} else {
	    return false;
	}
    }

    public void print_int(int value) {
      Native.print_int(value);
    }

    // debugging aid
    public void showAddress(Oop area) {
        Native.print_string("Address is ");
        Native.print_hex_int(VM_Address.fromObject(area).asInt());
        Native.print_string("\n");
    }

    // debugging aid
    public int toAddress(Oop ref) {
         return VM_Address.fromObject(ref).asInt();
    }

    // debugging aid
    public boolean isScope(Oop area) {
	return area instanceof VM_ScopedArea
	    && ((VM_ScopedArea)area).isScope();
    }

    public boolean hasChildArea(Oop area) {
        return((VM_ScopedArea)area).hasChild();
    }

    public boolean hasMultipleChildren(Oop area) {
        return((VM_ScopedArea)area).hasMultipleChildren();
    }

    public void setParentArea(Oop childArea, Oop parentArea)  throws PragmaAtomic { //PARBEGIN PAREND
        ((VM_ScopedArea)childArea).setParent((VM_ScopedArea)parentArea);
    }
    public void resetParentArea(Oop childArea)  throws PragmaAtomic { //PARBEGIN PAREND
        ((VM_ScopedArea)childArea).resetParent();
    }    
    public boolean isProperDescendant(Oop childArea, Oop parentArea) {
        return ((VM_ScopedArea)childArea).isProperDescendantOf((VM_ScopedArea)parentArea);
    }
    public int getHierarchyDepth(Oop area) {
        return ((VM_ScopedArea)area).getHierarchyDepth();
    }


    public Oop getAreaMirror(Oop area) {
        //d("[RuntimeExports - getAreaMirror()] 1 : " + area.toString());
        VM_ScopedArea a = ((VM_ScopedArea) area);
        //d("[RuntimeExports - getAreaMirror()] 2");
        Oop b = a.getMirror();
        //d("[RuntimeExports - getAreaMirror()] 3");
        return ((VM_ScopedArea) area).getMirror();
    }
    public Oop areaOf(Oop ref) {
        return asOop(MemoryManager.the().areaOf(ref));
    }
    public int getAreaSize(Oop area) {
	return ((VM_Area) area).size();
    }
    public int memoryConsumed(Oop area) {
	return ((VM_Area) area).memoryConsumed();
    }
    public int memoryRemaining(Oop area) {
	return ((VM_Area) area).memoryRemaining();
    }

    public Oop enterScratchPad() {
        return asOop(MemoryPolicy.the().enterScratchPadArea());
    }

    public Oop enterAreaForMirror(Oop clazz){
	return asOop(MemoryPolicy.the().enterAreaForMirror(clazz));
    }

    public void leaveArea(Oop area)  throws PragmaAtomic { //PARBEGIN PAREND
	MemoryPolicy.the().leave((Object) area);
    }

    public boolean isReschedulingEnabled() throws
    s3.util.PragmaNoPollcheck {
        return tm.isReschedulingEnabled();
    }

    // testing/debugging aids for PriorityInheritanace mechanism
    public int getInheritanceQueueSize(Oop thread) {
        RealtimeJavaThreadImpl vmThread = 
            (RealtimeJavaThreadImpl)getVMThread(thread);
        if (vmThread == null)
            csa.generateThrowable(Throwables.NULL_POINTER_EXCEPTION, 0);
        return vmThread.getInheritanceQueueSize();
    }

    public boolean checkInheritanceQueueHead(Oop thread, Oop t) {
        RealtimeJavaThreadImpl vmThread = 
            (RealtimeJavaThreadImpl)getVMThread(thread);
        if (vmThread == null)
            csa.generateThrowable(Throwables.NULL_POINTER_EXCEPTION, 0);
        RealtimeJavaThreadImpl other = 
            (RealtimeJavaThreadImpl)getVMThread(t);
        if (other == null)
            csa.generateThrowable(Throwables.NULL_POINTER_EXCEPTION, 0);
        return vmThread.checkInheritanceQueueHead(other);
    }

    public boolean checkInheritanceQueueTail(Oop thread, Oop t) {
        RealtimeJavaThreadImpl vmThread = 
            (RealtimeJavaThreadImpl)getVMThread(thread);
        if (vmThread == null)
            csa.generateThrowable(Throwables.NULL_POINTER_EXCEPTION, 0);
        RealtimeJavaThreadImpl other = 
            (RealtimeJavaThreadImpl)getVMThread(t);
        if (other == null)
            csa.generateThrowable(Throwables.NULL_POINTER_EXCEPTION, 0);
        return vmThread.checkInheritanceQueueTail(other);
    }

    public int getBasePriority(Oop thread) {
        RealtimeJavaThreadImpl vmThread = 
            (RealtimeJavaThreadImpl)getVMThread(thread);
        if (vmThread == null)
            csa.generateThrowable(Throwables.NULL_POINTER_EXCEPTION, 0);
        return vmThread.getBasePriority();
    }

    public int getActivePriority(Oop thread) {
        RealtimeJavaThreadImpl vmThread = 
            (RealtimeJavaThreadImpl)getVMThread(thread);
        if (vmThread == null)
            csa.generateThrowable(Throwables.NULL_POINTER_EXCEPTION, 0);
        return vmThread.getPriority();
    }


// PARBEGIN    
    public static final String dontFillStackTrace = "";
    
    public boolean PARenabled() { return Transaction.the().PARenabled(); }
    public void start()   throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { Transaction.the().start((S3CoreServicesAccess) csa); }
    public void start(int size, boolean commitOnOverflow)   throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers {Transaction.the().start(size, commitOnOverflow, (S3CoreServicesAccess) csa);}
    public void commit()   throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { Transaction.the().commit(); }
    public void undo()   throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers{ Transaction.the().undo();  }
    public void retry()  throws  Throwable, PragmaInline, PragmaNoPollcheck, PragmaNoBarriers{ 
	csa.processThrowable(Transaction.the().getUDException());
    }
    public  int logSize()   throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers{ return Transaction.the().logSize(); }
    public void par_log_arr(Oop o, int offset) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { Transaction.the().par_log_arr(o,offset);}
    public void par_log_arrw(Oop o, int offset) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers{ Transaction.the().par_log_arrw(o,offset);}
    public void par_log(VM_Address o, int offset)  throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { Transaction.the().par_log(o,offset);}
    public void par_logw(VM_Address o, int offset)  throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { Transaction.the().par_logw(o,offset);}
    public void par_read()  throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers { Transaction.the().par_read();}
    public void par_throwNativeCallException(String s) throws  PragmaInline, PragmaNoPollcheck, PragmaNoBarriers {
	Transaction.the().throwNativeCallException(s);
    }
//PAREND 


    public void resetEvManProfileHistograms() {
	em.resetProfileHistograms();
    }
    
    public void disableEvManProfileHistograms() {
	em.disableProfileHistograms();
    }
    
    public void getrusageSelf(Oop array) {
	RUsage.fromSystemToAnyArray(array);
    }
    
    
    
    
    
    
    
    public void RPT_Ipoint(int ipoint) {
    	RPT.RPT_Ipoint(ipoint);
    }
    
    public void RPT_Init() {
    	RPT.RPT_Init();
    }
    
    public void RPT_Output_Trace() {
    	RPT.RPT_Output_Trace();
    }
    
    
    
    
    
    
    /**
	 * Get the byte at the given address with an atomic load.
	 *
	 * @param	address	address of the byte to read
	 * @return	The byte at the given address
	 */
	public byte getByteAtomic(long address) {
		return RawMemoryAccess.getByteAtomic(address);
	}

	/**
	 * Set the byte at the given address with an atomic store.
	 *
	 * @param	address	address of the byte to write
	 * @param	value	Value to write.
	 */
	public void setByteAtomic(long address, long value) {
		RawMemoryAccess.setByteAtomic(address,value);
	}

	/**
	 * Get the short at the given address with an atomic load.
	 *
	 * @param	address	address of the short to read
	 * @return	The short at the given address
	 */
	public short getShortAtomic(long address) {
		return RawMemoryAccess.getShortAtomic(address);
	}

	/**
	 * Set the short at the given address with an atomic store.
	 *
	 * @param	address	address of the short to write
	 * @param	value	Value to write.
	 */
	public void setShortAtomic(long address, short value) {
		RawMemoryAccess.setShortAtomic(address,value);
	}

	/**
	 * Get the int at the given address with an atomic load.
	 *
	 * @param	address	address of the int to read
	 * @return	The int at the given address
	 */
	public int getIntAtomic(long address) {
		return RawMemoryAccess.getIntAtomic(address);
	}

	/**
	 * Set the int at the given address with an atomic store.
	 *
	 * @param	address	address of the int to write
	 * @param	value	Value to write.
	 */
	public void setIntAtomic(long address, int value) { 
		 RawMemoryAccess.setIntAtomic(address,value);
	}

	/**
	 * Get the long at the given address
	 *
	 * @param	address	address of the long to read
	 * @return	The long at the given address
	 */
	public long getLong(long address) {
		return RawMemoryAccess.getLong(address);
	}

	/**
	 * Set the long at the given address
	 *
	 * @param	address	address of the long to write
	 * @param	value	Value to write.
	 */
	public void setLong(long address, long value){ 
		RawMemoryAccess.setLong(address,value);
	}

	/**
	 * Get the Float at the given address with an atomic load.
	 *
	 * @param	address	address of the Float to read
	 * @return	The Float at the given address
	 */
	public float getFloatAtomic(long address){
		return RawMemoryAccess.getFloatAtomic(address);
	}

	/**
	 * Set the Float at the given address with an atomic store.
	 *
	 * @param	address	address of the Float to write
	 * @param	value	Value to write.
	 */
	public void setFloatAtomic(long address, float value) {
		RawMemoryAccess.setFloatAtomic(address,value);
	}

	/**
	 * Get the Double at the given address
	 *
	 * @param	address	address of the Double to read
	 * @return	The Double at the given address
	 */
	public double getDouble(long address) {
		return RawMemoryAccess.getDouble(address);
	}

	/**
	 * Set the Double at the given address
	 *
	 * @param	address	address of the Double to write
	 * @param	value	Value to write.
	 */
	public void setDouble(long address, double value) {
		 RawMemoryAccess.setDouble(address,value);
	}
    
   
}
