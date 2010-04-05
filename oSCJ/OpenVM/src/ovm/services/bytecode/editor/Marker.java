/**
 * @file ovm/services/bytecode/editor/Marker.java 
 **/
package ovm.services.bytecode.editor;

import ovm.util.ByteBuffer;


/**
 * Describes a location in a code stream. Markers are used to refer to
 * generated code as well as the original code of a method.
 * @author Christian Grothoff
 **/
public class Marker 
    extends Cursor.InstructionBuilder {
    
    private Cursor cursor_;
    private int predicted_;

    /**
     * Makes an unbound marker. This must be bound to a cursor before use.
     * @return a fresh marker
     */
    static public Marker makeUnbound() { return new Marker(null); }
    
    Marker(Cursor cursor) { this.cursor_ = cursor; }
    
    /** 
     * This method is used solely by Cursor to bind a marker to a new cursor.
     */
    void setCursor(Cursor c) { cursor_ = c; }
    
    int predictSize() {
	if (cursor_== null) return 0;
	if (predicted_ != cursor_.offset_)
	    cursor_.editor_.loopAgain();
	predicted_ = cursor_.offset_;
	return 0; // or 1 for nop
    }
    /**
     * Get the Cursor that this position points into.
     * @return never null
     **/
    public Cursor getCursor() {
	return cursor_;
    }
    
    /**
     * Get the offset of this marker in the codestream.
     **/
    public int getOffset() {
	return predicted_;
    }
    
    void write(ByteBuffer dummy) {}
    
} // end of Marker

