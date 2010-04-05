package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import static javax.safetycritical.annotate.Level.LEVEL_2;

// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.BlockFree;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public interface Schedulable extends Runnable {

// (annotations turned off to work with Java 1.4) 	@BlockFree
// (annotations turned off to work with Java 1.4) 	@SCJAllowed(LEVEL_2)
	public StorageConfigurationParameters getThreadConfigurationParameters();
}
