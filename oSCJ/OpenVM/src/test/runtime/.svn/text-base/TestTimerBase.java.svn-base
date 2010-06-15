
package test.runtime;

import ovm.core.services.timer.TimerManager;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.TimerServicesFactory;
import test.common.TestBase;

/**
 *
 * @author Filip Pizlo
 */
public abstract class TestTimerBase extends TestBase {
    /** Current timer manager */
    protected TimerManager timerMan;

    protected void init() {
        timerMan = ((TimerServicesFactory)ThreadServiceConfigurator.config
                    .getServiceFactory(TimerServicesFactory.name)).getTimerManager();
    }
    
    TestTimerBase(String description) {
        super(description);
    }
}

