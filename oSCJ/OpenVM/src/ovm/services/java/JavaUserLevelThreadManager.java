/* 
 * JavaUserLevelThreadManager.java
 *
 * Created 11 December, 2001 14:10
 *
 * $Header: /p/sss/cvs/OpenVM/src/ovm/services/java/JavaUserLevelThreadManager.java,v 1.4 2004/02/20 08:48:41 jthomas Exp $
 *
 */
package ovm.services.java;

import ovm.services.threads.TimedSuspensionThreadManager;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.Ordered;
/**
 * This interface defines the properties of user-level Java threads by 
 * combining the relevant thread manager interfaces that apply. 
 * It adds no new functionality of its own.
 *
 * @see ovm.core.services.threads.ThreadManager
 * @see ovm.services.threads.UserLevelThreadManager
 * @see ovm.services.threads.TimedSuspensionThreadManager
 *
 */
public interface JavaUserLevelThreadManager 
    extends UserLevelThreadManager,TimedSuspensionThreadManager, Ordered
{
}
