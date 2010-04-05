package edu.purdue.scj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.realtime.ImmortalMemory;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.Safelet;
import javax.safetycritical.Terminal;

public final class Launcher {

    static class LauncherThread extends Thread {

        static String _className;
        static String _propFileName;

// (annotations turned off to work with Java 1.4)         @Override
        public void run() {
            ImmortalMemory.instance().executeInArea(new Runnable() {
                public void run() {
                    try {
                        Class mainClass = Class.forName(_className);
                        if (Safelet.class.isAssignableFrom(mainClass)) {
                            PropFileReader.setInputStream(new FileInputStream(
                                    new File(_propFileName)));
                            Safelet safelet = (Safelet) (mainClass
                                    .newInstance());

                            safelet.setup();
                            MissionSequencer sequencer = safelet.getSequencer();
                            sequencer.start();
                            try {
                                // sleep forever waiting for mission terminates
                                Thread.sleep(Long.MAX_VALUE);
                            } catch (InterruptedException e) {
                                safelet.teardown();
                            }
                            System.exit(1);
                        } else
                            writeln("Main class >>" + mainClass
                                    + " must be subclass of " + Safelet.class);
                    } catch (ClassNotFoundException e) {
                        writeln("Class >>" + _className + "<< not found: " + e);
                        java.lang.System.exit(1);

                    } catch (InstantiationException e) {
                        writeln("Class >>" + _className
                                + "<< could not be instantiated: " + e);
                        java.lang.System.exit(1);

                    } catch (IllegalAccessException e) {
                        writeln("Class >>" + _className
                                + "<< could not be instantiated: " + e);
                        java.lang.System.exit(1);

                    } catch (FileNotFoundException e1) {
                        writeln(e1.getMessage());
                        System.exit(1);
                    }
                }
            });
        }
    };

    public static void main(final String[] args) {
        String usageInfo = "Usage: <jvm> <safelet class> <config file> \n";

        if (args.length == 0) {
            writeln(usageInfo);
            System.exit(1);
        }

        try {
            LauncherThread thread = (LauncherThread) ImmortalMemory.instance()
                    .newInstance(LauncherThread.class);
            LauncherThread._className = args[0];
            LauncherThread._propFileName = args[1];
            thread.start();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void writeln(String msg) {
        Terminal.getTerminal().writeln(msg);
    }
}
