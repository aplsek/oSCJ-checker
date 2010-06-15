package s3.services.simplejit.util;

import ovm.util.ArrayList;

public class SimpleJITStack {
    private ArrayList internal;
    public SimpleJITStack() {
	internal = new ArrayList();
    }
    public Object pop() {
	return internal.remove(internal.size() - 1);
    }
    public void push(Object o) {
	internal.add(o);
    }
    public boolean empty() {
	return internal.size() == 0;
    }
}
