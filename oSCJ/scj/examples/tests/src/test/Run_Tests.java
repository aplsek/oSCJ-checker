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
import javax.safetycritical.MissionManager;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.Safelet;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.Terminal;

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

public class Run_Tests  {

    public static void main(final String[] args) {

     
        System.out.println("\n[oSCJ] Testing the oSCJ.");

        Safelet safelet;
        
        System.out.println("TEST 1 : HelloWorld test:");
        safelet = new HelloWorld();
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();
        System.out.println("----------end of TEST 1---------- \n");
      
        System.out.println("TEST 2 : Raw Memory Access Test:");
        safelet = new Test_EnterPrivateMemory();
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();
        System.out.println("----------end of TEST 2---------- \n");
        
        System.out.println("\nTEST 3 : Testing Exceptions:");
        safelet = new Test_Exceptions();
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();
        System.out.println("----------end of TEST 3---------- \n");
        
        System.out.println("\nTEST 4: Raw Memory Access Test:");
        safelet = new Test_RawMemoryAccess();
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();
        System.out.println("----------end of TEST 4---------- \n");

        System.out.println("\nTEST 5: Rapita Support");
        safelet = new Test_RapitaSupport();
        safelet.setUp();
        safelet.getSequencer().start();
        safelet.tearDown();
        System.out.println("----------end of TEST 5---------- \n");
        
        
    }
}

