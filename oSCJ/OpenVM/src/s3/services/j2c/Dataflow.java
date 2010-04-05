package s3.services.j2c;

import ovm.util.Arrays;
import ovm.util.ArrayList;

public abstract class Dataflow {
    abstract boolean run(BBSpec block);

    final protected MethodCompiler mc;
    final protected boolean[] changed;
    final protected BBSpec[][] pred;

    public Dataflow(MethodCompiler mc) {
	this.mc = mc;
	changed = new boolean[mc.block.length];
	Arrays.fill(changed, true);

	Object[] pred = new Object[mc.block.length];
	for (int i = 0; i < pred.length; i++) {
	    BBSpec block = mc.block[i];
	    for (int j = 0; j < block.next.length; j++)
		addPred(pred, block, block.next[j]);
	    for (int j = 0; j < block.handler.length; j++)
		addPred(pred, block, block.handler[j]);
	}
	this.pred = new BBSpec[pred.length][];
	for (int i = 0; i < pred.length; i++) {
	    if (pred[i] == null)
		this.pred[i] = BBSpec.NO_NEXT_BLOCK;
	    else if (pred[i] instanceof BBSpec)
		this.pred[i] = new BBSpec[] { (BBSpec) pred[i] };
	    else {
		ArrayList l = (ArrayList) pred[i];
		this.pred[i] = new BBSpec[l.size()];
		l.toArray(this.pred[i]);
	    }
	}
    }

    private void addPred(Object[] pred, BBSpec p, BBSpec s) {
	int i = s.number;
	if (pred[i] == null)
	    pred[i] = p;
	else {
	    if (pred[i] instanceof BBSpec) {
		ArrayList l = new ArrayList(2);
		l.add(pred[i]);
		pred[i] = l;
	    }
	    ((ArrayList) pred[i]).add(p);
	}
    }

    static public abstract class Backward extends Dataflow {
	public Backward(MethodCompiler mc) { super(mc); }
	
	public void run() {
	    boolean changedBack;
	    do {
		changedBack = false;
		for (int i = mc.block.length; i --> 0; )
		    if (changed[i]) {
			changed[i] = false;
			if (run(mc.block[i]))
			    for (int j = pred[i].length; j --> 0; ) {
				int k = pred[i][j].number;
				changed[k] = true;
				changedBack |= k >= i;
			    }
		    }
	    } while (changedBack);
	}
    }

    static public abstract class Forward extends Dataflow {
	public Forward(MethodCompiler mc) { super(mc); }
	    
	public void run() {
	    boolean changedBack;
	    do {
		changedBack = false;
		for (int i = 0; i < mc.block.length; i++) {
		    BBSpec block = mc.block[i];
		    if (changed[i]) {
			changed[i] = false;
			if (run(block)) {
			    for (int j = 0; j < block.next.length; j++) {
				int k = block.next[j].number;
				changed[k] = true;
				changedBack |= k <= i;
			    }
			    for (int j = 0; j < block.handler.length; j++) {
				int k = block.handler[j].number;
				changed[k] = true;
				changedBack |= k <= i;
			    }
			}
		    }
		}
	    } while (changedBack);
	}
    }
}
