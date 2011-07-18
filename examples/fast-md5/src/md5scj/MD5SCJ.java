package md5scj;

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

import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.safetycritical.CyclicExecutive;
import javax.safetycritical.CyclicSchedule;
import javax.safetycritical.Mission;
import javax.safetycritical.PeriodicEventHandler;
import javax.safetycritical.StorageParameters;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

import com.twmacinta.util.MyMD5Input;


@SCJAllowed(members=true)
@Scope("Level0App")
@DefineScope(name="MD5SCJ", parent="Level0App")
public class MD5SCJ extends PeriodicEventHandler {

    @SCJRestricted(INITIALIZATION)
    public MD5SCJ(long psize, int count) {
        super(null, null, new StorageParameters(psize,null, 0,0));
        count_ = count;
    }

    private int count_;

    /**
     *
     * Testing Enter Private Memory
     *
     */
    @Override
    @SCJAllowed(SUPPORT)
    @RunsIn("MD5SCJ")
    public void handleAsyncEvent() {
        long start = System.nanoTime();
        doMD5work();
        long end = System.nanoTime();

        if (count_-- == 0)
            Mission.getCurrentMission().requestSequenceTermination();
    }

    @RunsIn("MD5SCJ")
    private void doMD5work() {
        for (String in : Constants.input) {
            MyMD5Input myMD = new MyMD5Input();
            myMD.run(in);
            //myMD.finalHash(in);
        }
    }

    @Override
    @SCJAllowed(SUPPORT)
    @SCJRestricted(CLEANUP)
    public void cleanUp() {
    }
}
