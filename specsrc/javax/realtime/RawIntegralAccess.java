package javax.realtime;
import javax.safetycritical.annotate.SCJAllowed;
import static javax.safetycritical.annotate.Level.LEVEL_1;


@SCJAllowed(LEVEL_1)
public interface RawIntegralAccess {

  @SCJAllowed(LEVEL_1)
  public byte getByte(long offset);
/*         throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
  
  @SCJAllowed(LEVEL_1)
  public void getBytes(long offset, byte[] bytes, int low, int number);
/*         throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
         
  @SCJAllowed(LEVEL_1)
  public int getInt(long offset);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
  
  @SCJAllowed(LEVEL_1)
  public void getInts(long offset, int[] ints, int low, int number);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
         
  @SCJAllowed(LEVEL_1)
  public long getLong(long offset);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
  
  @SCJAllowed(LEVEL_1)
  public void getLongs(long offset, long[] longs, int low, int number);
 /*        throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/

  @SCJAllowed(LEVEL_1)
  public short getShort(long offset);
 /*        throws javax.realtime.OffsetOutOfBoundsException,
                javax.realtime.SizeOutOfBoundsException;*/
                
  @SCJAllowed(LEVEL_1)
  public void getShorts(long offset, short[] shorts, int low, int number);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
      
  @SCJAllowed(LEVEL_1)   
  public void setByte(long offset, byte value);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
         
  @SCJAllowed(LEVEL_1)      
  public void setBytes(long offset, byte[] bytes, int low, int number);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/

  @SCJAllowed(LEVEL_1)
  public void setInt(long offset, int value);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
  @SCJAllowed(LEVEL_1) 
  public void setInts(long offset, int[] its, int low, int number);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/

  @SCJAllowed(LEVEL_1)
  public void setByte(long offset, long value);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
     
  @SCJAllowed(LEVEL_1)    
  public void setLongs(long offset, long[] longs, int low, int number);
   /*      throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
  
  @SCJAllowed(LEVEL_1)   
  public void setShort(long offset, short value);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
  
  @SCJAllowed(LEVEL_1)  
  public void setShorts(long offset, short[] shorts, int low, int number);
  /*       throws javax.realtime.OffsetOutOfBoundsException,
         javax.realtime.SizeOutOfBoundsException;*/
}
