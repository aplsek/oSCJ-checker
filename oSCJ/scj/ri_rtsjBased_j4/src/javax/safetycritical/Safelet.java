/*---------------------------------------------------------------------*\
 *
 * aicas GmbH, Karlsruhe, Germany 2007
 *
 * This code is provided to the JSR 302 group for evaluation purpose
 * under the LGPL 2 license from GNU.  This notice must appear in all
 * derived versions of the code and the source must be made available
 * with any binary version.  Viewing this code does not prejudice one
 * from writing an independent version of the classes within.
 *
 * $Source: /home/cvs/jsr302/scj/specsrc/javax/safetycritical/Safelet.java,v $
 * $Revision: 1.3 $
 * $Author: kelvin $
 * Contents: Java source of HIJA Safety Critical Java class Safelet
 *
\*---------------------------------------------------------------------*/

package javax.safetycritical;

// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.Level;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

import javax.safetycritical.annotate.Level;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public interface Safelet {

// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public Level getLevel();

// (annotations turned off to work with Java 1.4) 	@SCJAllowed
	public MissionSequencer getSequencer();

	public void setup();

	public void teardown();
}
