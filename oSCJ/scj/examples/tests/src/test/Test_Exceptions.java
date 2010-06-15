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

package test;

import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.ManagedMemory;
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.Terminal;

import edu.purdue.scj.utils.Utils;

public class Test_Exceptions  extends CyclicExecutive {

    public Test_Exceptions () {
        super(null);
    }

    public static void main(final String[] args) {

         Utils.debugPrint("[OK] TEST: exceptions");
        // VMSupport.setCurrentArea(VMSupport.getImmortalArea());
        // Utils.debugPrint("[OK] immportal... ok.");
        //
        // Utils.debugPrint("[OK] priority... " + VMSupport.getMaxRTPriority());

        // Utils.debugPrint("[OK] Class is a subclass of Safelet");

        Safelet safelet = new Test_Exceptions ();
       // Utils.debugPrint("[OK] Class is a subclass of Safelet");

        
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();

      
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
        new WordHandler(2000, "Testing Exceptions\n", 5);
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
       // Terminal.getTerminal().write("setup ...\n");
       // Terminal.getTerminal().write(
       //         "Current thread is " + Thread.currentThread() + "\n");

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
         * Testing the exceptions here
         * 
         */
        public void handleEvent() {
            Terminal.getTerminal().write(getName());
         
            MyRunnable runnable = new MyRunnable();
            ManagedMemory.getCurrentManagedMemory().enterPrivateMemory(300, runnable);
            
            Utils.debugPrint("[SCJ] Private Mem consumed:"
                    + RealtimeThread.getCurrentMemoryArea().memoryConsumed());
            
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

