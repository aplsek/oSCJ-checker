package javax.realtime;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.MemoryAreaEncloses;

@SCJAllowed
public abstract class MemoryArea
{
  protected MemoryArea() {/* ... */}

  @BlockFree
  @SCJAllowed
  public static MemoryArea getMemoryArea(Object object)
  {
    return null;
  }

  @BlockFree
  @SCJAllowed
  public abstract void enter(Runnable logic);

  
  /**
   * TBD: This method has no object argument, so this commentary is
   * not meaningful.
   *
   * Execute <code>logic</code> in the memory area containing
   * <code>object</code>.
   *
   * "@param" object is the reference for determining the area in which to
   * execute <code>logic</code>.
   *
   * @param logic is the runnable to execute in the memory area
   * containing <code>object</code>.
   */
  @BlockFree
  @Allocate(sameAreaAs={"object"})
  @MemoryAreaEncloses(inner = { "logic" }, outer = { "this" })
  @SCJAllowed(LEVEL_1)
  public void executeInArea(Runnable logic) throws InaccessibleAreaException
  {
  }


  /**
   * TBD: this method has no object argument, so this commentary is
   * not meaningful
   *
   * This method creates an object of type <code>type</code> in the memory
   * area containing <code>object</code>.
   *
   * "@param" object is the reference for determining the area in which to
   * allocate the array.
   *
   * @param type is the type of the object returned.
   *
   * @return a new object of type <code>type</code>
   *
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InstantiationException
   * @throws OutOfMemoryError
   * @throws ExceptionInInitializerError
   * @throws InaccessibleAreaException
   */
  @Allocate(sameAreaAs={"this.area"})
  @BlockFree
  @SCJAllowed
  public Object newInstance(Class type)
    throws IllegalArgumentException, InstantiationException,
	   OutOfMemoryError, InaccessibleAreaException
  {
    return null; // dummy return
  }

 
  /**
   * This method creates an object of type <code>type</code> in the memory
   * area containing <code>object</code>.
   *
   * @param object is the reference for determining the area in which to
   * allocate the array.
   *
   * @param type is the type of the object returned.
   *
   * @return a new object of type <code>type</code>
   */
   @Allocate(sameAreaAs={"object"})
   @SCJAllowed
   public Object newInstanceInArea(Object object, Class type)
   throws IllegalArgumentException, OutOfMemoryError,
          InaccessibleAreaException, InstantiationException
  {
    return getMemoryArea(object).newInstance(type);
  }


  /**
   * This method creates an object of type <code>type</code> in the memory
   * area containing <code>object</code>.
   *
   * @param type is the type of the object returned.
   *
   * @return a new object of type <code>type</code>
   */
  @Allocate(sameAreaAs = {"this.area"})
  @BlockFree
  @SCJAllowed
  public Object newArray(Class type, int size)
  {
    return null; // dummy return
  }

 
  /**
   * This method creates an array of type <code>type</code> in the memory
   * area containing <code>object</code>.
   *
   * @param object is the reference for determining the area in which to
   * allocate the array.
   *
   * @param type is the type of the array element for the returned
   * array.
   *
   * @param size is the size of the array to return.
   *
   * @return a new array of element type <code>type</code> with size
   * <code>size</code>.
   */
  @Allocate(sameAreaAs={"object"})
  @BlockFree
  @SCJAllowed
  public Object newArrayInArea(Object object, Class type, int size)
  {
    return getMemoryArea(object).newArray(type, size);
  }

  @BlockFree
  @SCJAllowed
  public abstract long memoryConsumed();

  @BlockFree
  @SCJAllowed
  public abstract long memoryRemaining();

  @BlockFree
  @SCJAllowed
  public abstract long size();
}
