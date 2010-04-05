package s3.services.simplejit;

import ovm.util.ArrayList;

public class StackHeight {

    private StackHeight dependingSH;
    private int heightDiff;
    private int absoluteHeight;
    private boolean finalized;
    private boolean linked;
    private ArrayList multipleDependencies;

    public StackHeight() {
	this.dependingSH = null;
	this.heightDiff = 0;
	this.finalized = false;
	this.absoluteHeight = -1;
	this.linked = false;
    }
	
    public StackHeight(StackHeight dependingSH, int heightDiff) {
	this.dependingSH = dependingSH;
	this.heightDiff = heightDiff;
	this.finalized = false;
	this.absoluteHeight = -1;
	this.linked = true;
    }

    public StackHeight(int absoluteHeight) {
	this.dependingSH = null;
	this.heightDiff = 0;
	this.absoluteHeight = absoluteHeight;
	this.finalized = true;
	this.linked = true;
    }

    public boolean linked() {
	return this.linked;
    }

    private class Pair {
	StackHeight sh;
	int diff;
	Pair(StackHeight sh, int diff) {
	    this.sh = sh;
	    this.diff = diff;
	}
    }

    public void checkMultipleDependencies(int pc) {
	if (this.multipleDependencies == null)
	    return;
	Object[] pairs = this.multipleDependencies.toArray();
	int firstHeight = getHeight();
	for(int i = 0; i < pairs.length; i++) {
	    Pair p = (Pair)pairs[i];
	    int eachHeight = p.sh.getHeight() + p.diff;
	    if (firstHeight != eachHeight) {
		throw new Error("Inconsistent stack height found : " + 
				firstHeight + " vs. " + eachHeight + 
				" @ PC " + pc);
	    }
	}
    }

    public void link(StackHeight dsh, int hd) {
	if (this.linked) {
	    this.multipleDependencies = new ArrayList();
	    this.multipleDependencies.add(new Pair(dependingSH, heightDiff));
	}
	if (! this.finalized && ! this.linked) {
	    this.dependingSH = dsh;
	    this.heightDiff = hd;
	    this.linked = true;
	}
    }

    public int getHeight() {
	if (! this.linked)
	    throw new Error("Tried to determine the stack height of a unlinked StackHeight");
	if (this.finalized) {
	    return absoluteHeight;
	} else {
	    this.absoluteHeight = dependingSH.getHeight() + heightDiff;
	    this.finalized = true;
	    return absoluteHeight;
	}
    }
}
