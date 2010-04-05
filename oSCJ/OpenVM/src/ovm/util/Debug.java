/**
 * @file ovm/util/Debug.java
 * @author Michel Pawlak
 **/
package ovm.util;

import java.io.PrintStream;

/**
 * Simple class providing methods for printing debug informations
 **/
public class Debug extends ovm.core.OVMBase {

    private String tab_ = new String("");
    private String buffer_ = "";
    private String label_ = "";
    private String tabbingAmount = new String("    ");
    private boolean bufferize_ = false;
    private PrintStream out;

    public Debug() {
        out = System.out;
    }

    public void setTab(String s) {
        throw new Error();
    }
    public Debug(PrintStream out) {
        this.out = out;
    }

    /**
     * The output will be stored in a string.
     **/
    public void bufferizeOutput(boolean flag) {
        bufferize_ = flag;
    }

    /**
     * Retrieve the buffered output.
     **/
    public String getResults() {
        return buffer_;
    }

    /**
     * Sets a label. To remove any label call this method
     * with a <code>null</code> parameter.
     **/
    public void setLabel(String label) {
        if (label != null)
            label_ = "[" + label + "] ";
        else
            label_ = "";
    }

    /**
     * Sets the tabulation . To remove any tabulation call this method
     * with a <code>null</code> parameter.
     **/
    public void setTabbingAmount(String ta) {
        tabbingAmount = ta;
    }

    /**
     * Reset
     **/
    public void reset() {
        tab_ = new String("");
        buffer_ = "";
        label_ = "";
        bufferize_ = false;
    }

    /**
     * Prints a tabulation
     **/
    public void tab() {
        if (bufferize_)
            buffer_ += label_ + tab_;
        else
            out.print(label_ + tab_);
    }

    /**
     * --> Indentation in (grows tabulation)
     **/

    public void indentIn() {
        tab_ += tabbingAmount;
    }

    /**
     * <-- Indentation out (shrinks tabulation)
     **/
    public void indentOut() {
        if (tab_.length() >= tabbingAmount.length())
            tab_ = tab_.substring(0, tab_.length() - tabbingAmount.length());
    }

    /**
     * Prints a string
     **/
    public void print(String s) {
        if (bufferize_)
            buffer_ += label_ + s;
        else
            out.print(label_ + s);
    }

    /**
     * Prints a String and then prints a \n
     **/
    public void println(String s) {
        if (bufferize_)
            buffer_ += label_ + s + "\n";
        else
            out.println(label_ + s);
    }

} // Debug
