package scjAllowed.simple;

import static javax.safetycritical.annotate.Level.INFRASTRUCTURE;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(INFRASTRUCTURE)
//## checkers.scjAllowed.SCJAllowedChecker.ERR_BAD_USER_LEVEL
public class TestBadUserLevel { }
