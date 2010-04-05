
package javax.safetycritical.annotate;

public class Level {

  int level;
  
  public Level(int level) {
    this.level = level;  
  }
  
  public static final Level LEVEL_0 = new Level(0);
  public static final Level LEVEL_1 = new Level(1);
  public static final Level LEVEL_2 = new Level(2);

}
