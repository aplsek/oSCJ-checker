package scope.scope.simple;

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.*;
import static javax.safetycritical.annotate.Scope.IMMORTAL;


import javax.realtime.MemoryArea;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.Mission;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@SCJAllowed(members=true)
@Scope(IMMORTAL)
public class TestCaller {


    @Scope(IMMORTAL)
    @SCJAllowed(members=true)
    @DefineScope(name="X", parent=IMMORTAL)
    static abstract class MS extends MissionSequencer {

        @SCJRestricted(INITIALIZATION)
        public MS() {
            super(null, null);
        }
    }


    @Scope("X")
    static class X {

        Server server = new Server();

        public void handleAsyncEvent() {
            server.handleRequest();
        }
    }

    @SCJAllowed(members = true)
    static class Server {

        @RunsIn(CALLER)
        public void handleRequest() {
            //## checkers.scope.ScopeChecker.ERR_BAD_METHOD_INVOKE
            decodePercent("token");
        }

        @RunsIn(THIS)
        private String decodePercent(String str) {
            return "resutl";
        }
    }
}