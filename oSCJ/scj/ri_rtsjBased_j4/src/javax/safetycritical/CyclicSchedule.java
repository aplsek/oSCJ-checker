package javax.safetycritical;

import javax.realtime.RelativeTime;
// (annotations turned off to work with Java 1.4) import javax.safetycritical.annotate.SCJAllowed;

// (annotations turned off to work with Java 1.4) @SCJAllowed
public class CyclicSchedule {

    private Frame[] _frames;

// (annotations turned off to work with Java 1.4)     @SCJAllowed
    public CyclicSchedule(Frame[] frames) {
        _frames = frames;
    }

    Frame[] getFrames() {
        return _frames;
    }

// (annotations turned off to work with Java 1.4)     @SCJAllowed
    final public static class Frame {

        private RelativeTime _duration;
        private PeriodicEventHandler[] _handlers;

// (annotations turned off to work with Java 1.4)         @SCJAllowed
        public Frame(RelativeTime duration, PeriodicEventHandler[] handlers) {
            _duration = duration;
            _handlers = handlers;
        }

// (annotations turned off to work with Java 1.4)         @SCJAllowed
        public PeriodicEventHandler[] getHandlers() {
            return _handlers;
        }

// (annotations turned off to work with Java 1.4)         @SCJAllowed
        public RelativeTime getDuration() {
            return _duration;
        }
    }
}
