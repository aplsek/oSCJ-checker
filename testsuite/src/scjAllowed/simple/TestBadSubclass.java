package scjAllowed.simple;

import javax.safetycritical.annotate.Level;
import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(Level.LEVEL_1)
public class TestBadSubclass { }

@SCJAllowed
//## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_SUBCLASS
class TestBadSubClassX extends TestBadSubclass { }
