package md5scj;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed(members=true)
public class Constants {

	public static int MAX = 10;

	 public static int NUMBER_OF_PLANES = 60;

	 public static int MISSION_MEMORY =  330000;
	 public static int PRIVATE_MEMORY =  90000;

	 public static int RUNS = 1000;

	 public static long          PERIOD                          = 50;


	 public static String[] input = {
		  "The quick brown fox jumps over the lazy dog",
		  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  "The quick brown fox jumps over the lazy dog",
                  "The quick brown fox jumps over the lazy dog.",
		  };
}
