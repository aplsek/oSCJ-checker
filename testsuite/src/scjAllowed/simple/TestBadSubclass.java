package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(LEVEL_1)
public class TestBadSubclass { }

@SCJAllowed
//## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_SUBCLASS
class TestBadSubClassX extends TestBadSubclass { }
