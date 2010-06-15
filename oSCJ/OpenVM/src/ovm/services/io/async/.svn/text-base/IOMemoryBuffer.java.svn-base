
package ovm.services.io.async;

import ovm.core.Executive;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.core.execution.Native;

import s3.services.io.clients.PosixIOImpl;

import s3.util.PragmaInline;
/**
 * A memory buffer for stuff you read or write using async IO.  It
 * creates a byte[] and allows you to use it in IO operations as
 * many times as you want.
 * @author Filip Pizlo
 */
public class IOMemoryBuffer implements AsyncMemoryCallback {
    private static Blueprint.Array ebp;
    static {
        // surely there is a more direct way to do this?
	ebp = (Blueprint.Array)
	    VM_Address.fromObject(new byte[0]).asOop().getBlueprint();
    }
    
    private byte[] data;
    public byte[] getData() throws PragmaNoBarriers, PragmaInline {
        return data;
    }
    void setData(byte[] d) throws PragmaNoBarriers, PragmaInline {
        data=d;
    }

    VM_Area area;
    void setArea(VM_Area area) throws PragmaNoBarriers, PragmaInline {
        this.area = area;
    }

    int offset=0;
    
    public IOMemoryBuffer(int size) {
	// FIXME: this is totally wrong.  I should be using malloc.
	
	if (MemoryManager.the().usesArraylets()) {
	  setArea(MemoryManager.the().makeExplicitArea(
	    MemoryManager.the().continuousArrayBytesToData(ebp, size)+size ));
	} else {
          setArea(MemoryManager.the().makeExplicitArea(ebp.byteOffset(size)));
        }
        VM_Area prev=MemoryManager.the().setCurrentArea(area);
        try {
          
            byte[] data = null;
        
            if (MemoryManager.the().usesArraylets()) {
              data = MemoryManager.the().allocateContinuousByteArray(size);
            } else {
              data = new byte[size];
            }
            if (false) {
              Native.print_string("Allocated buffer in IOMemoryBuffer: ");
              Native.print_ptr(VM_Address.fromObject(data));
              Native.print_string("\n");
            }
            setData(data);
        } finally {
            MemoryManager.the().setCurrentArea(prev);
        }
    }
    
    public void grow(int addSize) {
        int newSize=getData().length+addSize;

        VM_Area newArea=null;
        if (MemoryManager.the().usesArraylets()) {
          newArea = MemoryManager.the().makeExplicitArea(
            MemoryManager.the().continuousArrayBytesToData(ebp, newSize)+newSize);
        } else {
          newArea = MemoryManager.the().makeExplicitArea(ebp.byteOffset(newSize));
        }
        VM_Area prev=MemoryManager.the().setCurrentArea(newArea);
        try {
            byte[] newData=null;
            
            if (MemoryManager.the().usesArraylets()) {
              newData = MemoryManager.the().allocateContinuousByteArray(newSize);
            } else {
              newData = new byte[newSize];
            }
            
            if (false) {
              Native.print_string("Allocated buffer in IOMemoryBuffer, grow: ");
              Native.print_ptr(VM_Address.fromObject(newData));
              Native.print_string("\n");
            }
            
            System.arraycopy(getData(), 0,
                             newData, 0,
                             getData().length);
            setData(newData);
        } finally {
            MemoryManager.the().setCurrentArea(prev);
        }
        free();
        setArea(newArea);
    }
    
    public void setOffset(int o) {
        if (offset+o > getData().length) {
            throw Executive.panic("setOffset() call would set offset beyond buffer bound.");
        }
        offset=o;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void incOffset(int o) {
        if (offset+o > getData().length) {
            throw Executive.panic("incOffset() call would set offset beyond buffer bound.");
        }
        offset+=o;
    }
    
    public int getSize() {
        return getData().length;
    }
    
    public int getRemaining() {
        return getData().length-offset;
    }
    
    public VM_Address getBuffer(int cnt,
      boolean mayNeedBufferForAnUnboundedPeriodOfTime) {    
      
      pinBuffer();
      return getBufferInternal(cnt, mayNeedBufferForAnUnboundedPeriodOfTime);
    }
    
    public VM_Address getBufferInternal(int cnt,
                                boolean mayNeedBufferForAnUnboundedPeriodOfTime) { // ? pinning
        // this code is broken for area==null, since then we may have to do pinning. 

        Oop oop=VM_Address.fromObject(getData()).asOop();
        
        if (MemoryManager.the().usesArraylets()) {
          VM_Address ret = VM_Address.fromObjectNB(oop).add( MemoryManager.the().continuousArrayBytesToData(ebp, getSize())+offset );
          if (true) {
            MemoryManager.the().checkAccess(ret);
            MemoryManager.the().checkAccess(ret.add(cnt-1));            
          }
          return ret;
        } else {
          return ((Blueprint.Array)oop.getBlueprint()).addressOfElement(oop,offset);
        }
    }
    
    public void doneBuffer(VM_Address buf,
                           int cnt) {
       unpinBuffer();  
       PosixIOImpl.handleReplication( VM_Address.fromObject(getData()).asOop(), offset, cnt, buf);
    }
    
    public void free() {
        if (area==null) {
            return;
        }
        area.destroy();
        area = null;   // set null in case destroy obliterated the area itself
    }
    
    public void pinBuffer() {
      MemoryManager.the().pinNewLocation( VM_Address.fromObject(getData()).asOop() );
    }
    
    public void unpinBuffer() {
      MemoryManager.the().unpin( VM_Address.fromObject(getData()).asOop() );
    }
    
    public VM_Address getContiguousBuffer( int length, int offset ) {
      return getBufferInternal(length,false).add(offset);  // but nobody should call this with offset nonzero
    }
    
    public int getLastContiguousBufferLength() {
      return data.length;
    }
}

