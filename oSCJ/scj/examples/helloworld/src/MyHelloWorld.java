
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

import javax.realtime.ImmortalMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.Terminal;

import edu.purdue.scj.BackingStoreID;
import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

public class MyHelloWorld extends CyclicExecutive {

    public MyHelloWorld() {
        super(null);
    }

    public static void main(final String[] args) {
        Utils.debugPrintln("[OK] Main entered...");
        
        Safelet safelet = new MyHelloWorld();
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();
        
        Utils.debugPrintln("[OK] MyHelloWorld  : test done.");
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
        new WordHandler(20000, "\n HelloWorld!\n ", 5);
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

    public void setUp() {     
        Terminal.getTerminal().write("teardown ...\n"); 
    }

    public void tearDown() {
        Terminal.getTerminal().write("teardown ...\n");
    }

    public void cleanUp() {
        Terminal.getTerminal().write("cleanUp ...\n");
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
      
             
            MemoryArea immortal = ImmortalMemory.instance();
            long im = immortal.memoryConsumed();
            long curr = RealtimeThread.getCurrentMemoryArea().memoryConsumed();
            long im_VM =  VMSupport.memoryConsumed( VMSupport.getImmortalArea());
            long curr_VM = VMSupport.memoryConsumed(VMSupport.getCurrentArea());
            Terminal.getTerminal().write("------- mem test  \n");
            Terminal.getTerminal().write("IMMORTAL consumed [oSCJ]: " + im + "\n");
            Terminal.getTerminal().write("current consumed [oSCJ]: " + curr + "\n"); 
            Terminal.getTerminal().write("immortal size [oSCJ]: " + immortal.size() + "\n"); 
            
            Terminal.getTerminal().write("immortal [VMSupport]: " +  im_VM + "\n"); 
            Terminal.getTerminal().write("immortal size [VMSupport]: " +  VMSupport.getScopeSize(VMSupport.getImmortalArea()) + "\n"); 
            Terminal.getTerminal().write("current consumed [VMSupport]: " +  curr_VM + "\n"); 
            Terminal.getTerminal().write("current size [VMSupport]: " +   VMSupport.getScopeSize(VMSupport.getCurrentArea()) + "\n");
            Terminal.getTerminal().write("\n"); 
            
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



class ImmortalRun implements Runnable {

    @Override
    public void run() {
       try {
        
        System.out.println("Data allocation in Immortal:");
        new Data();
        new Data();
        
        System.out.println("IMMORTAL MEMORY TEST:");
        
        BackingStoreID curr = VMSupport.getCurrentArea();
        if (curr == null)
            System.out.println("ERROR: VMSupport.getCurrentArea returns null\n");   // ---> this should be printed
        else
            System.out.println("curr is OK\n");
        
        /*
        MemoryArea m = (MemoryArea) VMSupport.getNote(curr);
           if (m == null)
               System.out.println("m is null\n");
           else
               System.out.println("m is OK\n");
           
         BackingStoreID im = VMSupport.getImmortalArea();
         if (im == null)
             System.out.println("imm is null\n");
         else
             if (im == curr) 
                 System.out.println("we are in immortal\n");
             else
                 System.out.println("we are NOT IN IMMORTAL!!!\n");
         */
        
        // IMM memory consumed:
        long im = ImmortalMemory.instance().memoryConsumed();
        System.out.println("Immortal mem consumed : " + im);
        
    
       } catch (Exception e) {
           // catching all the NullPointerException 
           System.out.println("Exception: " + e.getStackTrace());
           e.printStackTrace();
       }
    
    }
}

class Data {
    
    long data;
    Data next;
    
}




class MyRunnable implements Runnable {
    
    public void run() {
        Terminal.getTerminal().write(">>>>>> EnterPrivate MEMORY OK.  In HelloWorld runnable! YES!");
        MemoryArea mem = RealtimeThread.getCurrentMemoryArea();
        
        long consumed = mem.memoryConsumed(); 
        Terminal.getTerminal().write("Memory Consumed:" + consumed);
    }
    
}