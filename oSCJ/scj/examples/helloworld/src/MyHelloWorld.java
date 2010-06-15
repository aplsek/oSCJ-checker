/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.PrivateMemory;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.Terminal;

import edu.purdue.scj.utils.Utils;

public class MyHelloWorld extends CyclicExecutive {

    public MyHelloWorld() {
        super(null);
    }

    public static void main(final String[] args) {
    	Utils.debugPrint("[OK] Main entered...");
        
        Safelet safelet = new MyHelloWorld();
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();
        
    	Utils.debugPrint("[OK] MyHelloWorld  : test done.");
    }

    private static void writeln(String msg) {
        Terminal.getTerminal().writeln(msg);
    }

    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[1];
        CyclicSchedule schedule = new CyclicSchedule(frames);
        frames[0] = new CyclicSchedule.Frame(new RelativeTime(200, 0), handlers);
        return schedule;
    }

    public void initialize() {
        new WordHandler(20000, "\n HelloWorld -- FIJI !\n ", 5);
    }

    /**
     * A method to query the maximum amount of memory needed by this mission.
     * 
     * @return the amount of memory needed
     */
    // @Override
    public long missionMemorySize() {
        return 5000000;  // 5MBs
    }

<<<<<<< .mine
    public void setUp() {
        Terminal.getTerminal().write("setup ...\n");
=======
    public void setUp() {
        Terminal.getTerminal().write("setup ... YES\n");
>>>>>>> .r13785
    }

    public void tearDown() {
        Terminal.getTerminal().write("teardown ...\n");
    }

    public void cleanUp() {
        Terminal.getTerminal().write("cleanup ...\n");
    }

    
    
    public class WordHandler extends PeriodicEventHandler {

        private int count_;

        private WordHandler(long psize, String name, int count) {
            super(null, null, null, psize, name);
            count_ = count;
        }

        /**
         * 
         * Testing Enter Private Memory
         * 
         */
        public void handleEvent() {
            Terminal.getTerminal().write(getName());
            //System.out.println("get");
            
            
<<<<<<< .mine
            MyRunnable runnable = new MyRunnable();
           ManagedMemory mem =  ManagedMemory.getCurrentManagedMemory();
=======
           // MyRunnable runnable = new MyRunnable();
           // ManagedMemory mem =  ManagedMemory.getCurrentManagedMemory();
>>>>>>> .r13785
           
<<<<<<< .mine
            long used = RealtimeThread.getCurrentMemoryArea().memoryConsumed();
        
            //System.out.println("used mem:" + used);
            long my_mem = mem.memoryConsumed();
            
            Terminal.getTerminal().write("current used:" + used + "\n");
            Terminal.getTerminal().write("mem used:" + my_mem);
            Terminal.getTerminal().write("\n remains:" + mem.memoryRemaining());
            Terminal.getTerminal().write("\n ------------\n");
            //  long before = mem.memoryConsumed();
=======
           // System.out.println("Current MEmory is:" + mem);
>>>>>>> .r13785
                /**
                 * TESTING the "enterPrivateMemory"
                 */
          // System.out.println("get ok");
          //  System.out.println("entering " + before);
            
            
            	//mem.enterPrivateMemory(3000, runnable);
            	//mem.enterPrivateMemory(4000, runnable);
            	//mem.enterPrivateMemory(3000, runnable);
            	
            	 used = RealtimeThread.getCurrentMemoryArea().memoryConsumed();
                 
                 //System.out.println("used mem:" + used);
                 my_mem = mem.memoryConsumed();
                 
                 Terminal.getTerminal().write("after used:" + my_mem);
                 Terminal.getTerminal().write(" remains:" + mem.memoryRemaining());
                 Terminal.getTerminal().write("\n");
         
            if (count_-- == 0)
                getCurrentMission().requestSequenceTermination();
        }

		
		public void cleanUp() {
		}

	
		public void register() {
		}

		
		public StorageParameters getThreadConfigurationParameters() {
			return null;
		}
    }



}


class MyRunnable implements Runnable {
	
	public void run() {
		System.out.println("______________ EnterPrivate MEMORY OK.  ____________");
	}
	
}