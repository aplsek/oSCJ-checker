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

public class HelloWorld extends CyclicExecutive {

    public HelloWorld() {
        super(null);
    }

    public static void main(final String[] args) {
    	Utils.debugPrint("[OK] Main entered...");
        
        Safelet safelet = new HelloWorld();
       

        
        safelet.setUp();
        // starting at LEvel 0, no thread is created.
        safelet.getSequencer().start();
        
        safelet.tearDown();

        // try {
        // // sleep forever waiting for mission terminates
        // Utils.debugPrint("[OK] going to sleep.");
        // Thread.sleep(50000);
        // Utils.debugPrint("[ERR] sleep finished.");
        // } catch (InterruptedException e) {
        // safelet.teardown();
        // }
        
    	Utils.debugPrint("[OK] HelloWorld 2 : test done.");
    }

    private static void writeln(String msg) {
        Terminal.getTerminal().writeln(msg);
    }

    public CyclicSchedule getSchedule(PeriodicEventHandler[] handlers) {
        CyclicSchedule.Frame[] frames = new CyclicSchedule.Frame[2];
        CyclicSchedule schedule = new CyclicSchedule(frames);
        frames[0] = new CyclicSchedule.Frame(new RelativeTime(200, 0), handlers);
        frames[1] = new CyclicSchedule.Frame(new RelativeTime(800, 0),
                new PeriodicEventHandler[0]);
        return schedule;
    }

    public void initialize() {
        Utils.debugPrint("[SCJ]" + " MyHelloWorld.initialize() - start ");
        Utils.debugPrint("[SCJ] Mission Mem consumed:"
                + RealtimeThread.getCurrentMemoryArea().memoryConsumed());
        
        new WordHandler(2000, "Hello ", -1);
        new WordHandler(2000, "World\n", 10);
    }

    /**
     * A method to query the maximum amount of memory needed by this mission.
     * 
     * @return the amount of memory needed
     */
    // @Override
    public long missionMemorySize() {
        return 5000000;
    }

    public void setup() {
        Terminal.getTerminal().write("setup ...\n");
        Terminal.getTerminal().write(
                "Current thread is " + Thread.currentThread() + "\n");

    }

    public void teardown() {
        Terminal.getTerminal().write("teardown ...\n");
    }

    public void cleanup() {
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
         
            
            
            if (count_-- == 0)
                getCurrentMission().requestSequenceTermination();
        }

		//@Override
		public void cleanUp() {
		}

		//@Override
		public void register() {
			
		}

		//@Override
		public StorageParameters getThreadConfigurationParameters() {
			return null;
		}
    }



    @Override
    public void setUp() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void tearDown() {
        // TODO Auto-generated method stub
        
    }
}

