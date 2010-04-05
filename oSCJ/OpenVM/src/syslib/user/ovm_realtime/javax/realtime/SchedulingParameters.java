/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/SchedulingParameters.java,v 1.1 2004/10/15 01:53:12 dholmes Exp $
 */
package javax.realtime;

/**
 *  Subclasses of <code>SchedulingParameters</code> (
 * {@link PriorityParameters},  {@link ImportanceParameters}, and any others 
 * defined for particular schedulers) provide the parameters to be used by 
 * the {@link Scheduler}.
 *
 * Changes to the values in a parameters object affects the scheduling 
 * behaviour of all the {@link Schedulable} objects to which it is bound.
 *
 * <p><b>Caution:</b> Subclasses of this class are explicitly unsafe in 
 * multithreaded situations when they are being changed.  
 * No synchronization is done.  It is assumed that users of this class who 
 * are mutating instances will be doing their own synchronization at a 
 * higher level.
 */
public abstract class SchedulingParameters extends ParameterBase {}

