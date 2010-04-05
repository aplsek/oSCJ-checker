package java.lang;

import ovm.core.Executive;

public class AssertionError extends Error {
    public AssertionError() {
	Executive.panicOnException(this);
    }
    public AssertionError(Object detailMessage) {
	super(String.valueOf(detailMessage));
	Executive.panicOnException(this);
    }
    public AssertionError(boolean detailMessage) {
	this(String.valueOf(detailMessage));
    }
    public AssertionError(char detailMessage) {
	this(String.valueOf(detailMessage));
    }
    public AssertionError(double detailMessage) {
	this(String.valueOf(detailMessage));
    }
    public AssertionError(float detailMessage) {
	this(String.valueOf(detailMessage));
    }
    public AssertionError(int detailMessage) {
	this(String.valueOf(detailMessage));
    }
    public AssertionError(long detailMessage) {
	this(String.valueOf(detailMessage));
    }
}
