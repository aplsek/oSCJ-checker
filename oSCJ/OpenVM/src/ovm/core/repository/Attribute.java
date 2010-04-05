package ovm.core.repository;

import ovm.core.repository.Mode;
import ovm.core.repository.TypeName;
import ovm.services.bytecode.reader.ByteCodeConstants;
import ovm.util.OVMError;
import s3.core.S3Base;

/**
 * The abstract class to be extended by specific attribute classes.
 * The implemented attribute types are specified by subclasses of this class
 * and represent the following kinds of attributes:
 * <ul>
 *     <li><b><code>Deprecated</code></b>: This is an optional
 *     attribute to be used when classes, fields, or interfaces, or methods 
 *     are superseded. </li>
 *
 *     <li><b><code>InnerClasses</code></b>: Each indexed entry in this
 *     attribute refers to a class or interface referenced in the current 
 *     class's constant pool that is not a member of a package 
 *     (i.e. an inner class). </li>
 *
 *     <li><b><code>LineNumberTable</code></b>: This attribute is used to map 
 *     source file line numbers to the VM code array. </li>
 *
 *     <li><b><code>LocalVariableTable</code>:</b> This attribute, if present,
 *     maps a method's local variables to current information about the 
 *     variable (name, descriptor, index of variable in the current frame, 
 *     index into the code array, and so on).</li>
 *
 *     <li><b><code>SourceFile</code></b>: This optional attribute refers 
 *     to the source file from which a class file was derived.  </li>
 *
 *     <li><b><code>Synthetic</code></b>: Class members that do not appear 
 *     in the source code are labelled with a Synthetic attribute.</li>
 *
 *     <li><b>Third party attributes</b>: Outside attributes not specified
 *     by the JVM specification.</li>
 * </ul>
 *      
 * <p>(These attributes are specified and described in greater detail in 
 * section 4.7 of the JVM Spec.)
 *
 * @author Michel Pawlak, Chrislain Razafimahefa
 **/
public abstract class Attribute extends S3Base implements ByteCodeConstants {

    /**
     * The <code>deprecated</code> attribute. This is kept as a
     * singleton by {@link Attribute}. This is an optional attribute 
     * to be used when classes, fields, or interfaces, or methods 
     * are superseded.
     **/
    public static abstract class Deprecated extends NonCritical {

        
        // The subclasses to specify whether the <code>deprecated</code>
        // attribute belongs to a class, method, or field.
        //
        
        /**
         * <code>deprecated</code> attribute for classes. 
         * @see Attribute.Deprecated
         */   
        public static class Class extends Attribute.Deprecated {

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrDeprecated(this);
            }
        }
        
		/**
		 * <code>deprecated</code> attribute for fields. 
		 * @see Attribute.Deprecated
		 */   
        public static class Field extends Attribute.Deprecated {

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrDeprecated(this);
            }
        }
        
		/**
		 * <code>deprecated</code> attribute for methods. 
		 * @see Attribute.Deprecated
		 */   
        public static class Method extends Attribute.Deprecated {

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrDeprecated(this);
            }
        }
        
        public abstract void accept(RepositoryProcessor visitor);

		/**
		 * Returns the repository utf8string id of this attribute.
		 * @return the repository utf8string id of this attribute
		 **/
        public int getNameIndex() {
            return ByteCodeConstants.attributeNames[Attributes.Deprecated];
        }
    }

    /**
     * The <code>InnerClasses</code> attribute, which contains
     * an inner class table. Each indexed entry in the table refers to
     * a class or interface referenced in the current class's
     * constant pool that is not a member of a package (an
     * inner class).
     **/
    public static class InnerClasses extends Attribute {

        /**
         * The empty set of inner classes
         **/
        final static Attribute.InnerClasses[] EMPTY_AARRAY =
            new Attribute.InnerClasses[0];

        // ----------------- the Inner Class Table -------------
        // The following fields constitute an inner class
        // table.

        /**
         * The array of inner class type names for each
         * inner class
         **/
        private TypeName.Scalar[] innerClass_;
        /**
         * The repository utfString index of the simple name for
         * each inner class entry
         **/
        private int[] innerNameIndex_; // repositoryIndex
        /**
         * The modifiers for each inner class in the table
         **/
        private Mode.Class[] mode_;
        /**
         * The array of type names for the class of which each
         * individual inner class is a member
         **/
        private TypeName.Scalar[] outerClass_;

		// -------------- End of the Inner Class Table ----------

        /**
         * Constructor which creates an inner classes attribute;
         * contains an inner class table for the class that owns
         * this attribute. This table contains an entry for each
         * declared or inherited member class. This entry consists of
         * the type name of the inner class, the type name of its
         * declaring class, the utf8string index of its simple name
         * (as declared in the source code), and its modifiers
         * object.
         * @param innerClass the array of type names for the inner class 
         *                   entries
         * @param outerClass the array of type names for the declaring
         *                   class of each entry
         * @param innerNameIndex the utf8string repository indices for
         *                       each entry's simple name
         * @param mode the modifiers objects for each entry
         **/
        public InnerClasses(
            TypeName.Scalar[] innerClass,
            TypeName.Scalar[] outerClass,
            int[] innerNameIndex,
            Mode.Class[] mode) {

            innerClass_ = innerClass;
            int size = innerClass_.length;
            if (outerClass.length != size) {
                throw new OVMError("oops"); // FIXME better exception
            }
            outerClass_ = outerClass;
            if (innerNameIndex.length != size) {
                throw new OVMError("oops"); // FIXME better exception
            }
            innerNameIndex_ = innerNameIndex;
            if (mode.length != size) {
                throw new OVMError("oops"); // FIXME better exception
            }
            mode_ = mode;
        }
        public final void accept(RepositoryProcessor visitor) {
            visitor.visitAttrInnerClasses(this);
        }
        
		/**
		 * Return the type name of an inner class at a given index
		 * in this attribute's table of inner classes.
		 * @param entryIndex the index of the desired inner class entry
		 * @return the type name of the desired innerclass
		 **/
        public TypeName.Scalar getInnerClass(int entryIndex) {
            return innerClass_[entryIndex];
        }
        
		/**
		 * Get the utf8string id of the name of the inner class at
		 * a given index in this attribute's table of inner classes
		 * @param entryIndex the index of the desired inner class entry
		 * @return the repository utf8string id of the innerclass
		 **/
        public int getInnerNameIndex(int entryIndex) {
            return innerNameIndex_[entryIndex];
        }

		/**
		 * Get the modifiers of the inner class entry at a
		 * given index in this attribute's table of inner classes
		 * @param entryIndex the index of the desired inner class entry
		 * @return the mode of the innerclass
		 **/
        public Mode.Class getMode(int entryIndex) {
            return mode_[entryIndex];
        }

		/**
		 * Returns the repository utf8string id of this attribute
		 * @return the repository utf8string id of this attribute
		 **/
        public int getNameIndex() {
            return ByteCodeConstants.attributeNames[Attributes.InnerClasses];
        }

		/**
		 * Get the outer class of the inner class at a given index in this
		 * attribute's table of inner classes
		 * @param entryIndex the index of the desired inner class entry
		 * @return the type name of the outerclass of the innerclass
		 **/
        public TypeName.Scalar getOuterClass(int entryIndex) {
            return outerClass_[entryIndex];
        }

		/**
		 * Get the number of inner class entries represented by this
		 * attribute
		 * @return number of inner class entries represented by this
		 *         attribute
		 */
        public int size() {
            return innerClass_.length;
        }
    } // end of Attribute.InnerClasses

    /**
     * The <code>LineNumberTable</code> attribute, which is used to 
     * map source file line numbers to the VM code array.
     **/
    public static abstract class LineNumberTable extends NonCritical {
	protected abstract int tableStartPC(int idx);
	protected abstract int tableLineNumber(int idx);
	protected abstract int tableSize();
	
        protected LineNumberTable() { }

        public final void accept(RepositoryProcessor visitor) {
            visitor.visitAttrLineNumberTable(this);
        }

	private int lastIdx_;

	/**
	 * Get the line number corresponding to an offset in the
	 * bytecode.
	 * @param offset the code offset 
         * @return        -1 in case of an error
         **/
        public int getLineNumber(int offset) {
	    int min = 0;
	    int max = tableSize();
	    int lastIdx = lastIdx_; // avoid races
	    
	    // Avoid any search in a common case: looking up line
	    // number of each instruction of a single method
	    // sequentially.  Building stack traces may be a more
	    // common case, but I think this cache is lightweight
	    // enough to be OK.
	    if (lastIdx + 1 < max) {
		if (tableStartPC(lastIdx) <= offset
		    && tableStartPC(lastIdx + 1) > offset)
		    return tableLineNumber(lastIdx);
		else if (tableStartPC(lastIdx + 1) <= offset
			 && (lastIdx + 2 == max
			     || tableStartPC(lastIdx + 2) > offset)) {
		    lastIdx++;
		    lastIdx_ = lastIdx;
		    return tableLineNumber(lastIdx);
		}
	    } else if (lastIdx < max && tableStartPC(lastIdx) <= offset)
		return tableLineNumber(lastIdx);

            // Search through the line number table for the first
            // start PC greater than the offset. The index we want
            // is the one just before that one.
            //
            // If we have less than 10 entries in the table, do
            // linear search. Otherwise, go for binary search.
            //
            if (max <= min) {
                return -1;
            }
            while (max > min) {
                if (max - min < 10) {
                    for (int i = min; i < max; i++) {
                        if (tableStartPC(i) == offset) {
			    lastIdx_ = i;
                            return tableLineNumber(i);
                        } else if (tableStartPC(i) > offset) {
                            if (i > 0) {
				lastIdx_ = i - 1;
                                return tableLineNumber(i - 1);
                            } else {
                                return tableLineNumber(0);
                            }
                        }
                    }
                    return tableLineNumber(max - 1);
                } else {
                    int mid = (max + min) / 2;
                    if (tableStartPC(mid) == offset) {
                        return tableLineNumber(mid);
                    } else if (tableStartPC(mid) > offset) {
                        max = mid - 1;
                    } else {
                        min = mid + 1;
                    }
                }
            }
            return tableLineNumber(max - 1);
        }
        
	/**
	 * Get the line number table for this attribute. The line number 
	 * table contains a mapping from the starting points in the 
	 * byte code array for individual lines of source code to
	 * the line numbers for that code.
	 * @return the line number table for this attribute.
	 **/
        public int[] getLineNumberTable() {
	    int[] ret = new int[tableSize()];
	    for (int i = 0; i < ret.length; i++)
		ret[i] = tableLineNumber(i);
            return ret;
        }

		/**
		 * Returns the repository utf8string id of this attribute
		 * @return the repository utf8string id of this attribute
		 **/
        public int getNameIndex() {
            return ByteCodeConstants.attributeNames[Attributes.LineNumberTable];
        }

		/**
		 * Get the start PC table for this attribute. This start PC table is
		 * an array of indices into the byte code array at which the
		 * code for a new line in the source file code begins.
		 * @return the start PC table for this attribute
		 **/
        public int[] getStartPCTable() {
	    int[] ret = new int[tableSize()];
	    for (int i = 0; i < ret.length; i++)
		ret[i] = tableStartPC(i);
            return ret;
        }

        /**
         * Convert this <code>LineNumberTable</code> to a <code>String</code>
         * representation.
         * @return the string representation of this LineNumberTable
         */
        public String toString() {
	    String ret = "LineNumberTable { ";
	    for (int i=0;i<tableSize();i++)
		ret = ret + tableStartPC(i) + "=>"+tableLineNumber(i)+" ";
	    return ret + "}";
        }

	public static LineNumberTable make(int[] startPC,
					   int[] lineNumber) {
	    int lineNumberBase = Integer.MAX_VALUE;
	    int lineNumberMax = 0;
	    int startPCMax = 0;
	    for (int i = 0; i < lineNumber.length; i++) {
		if (lineNumber[i] < lineNumberBase)
		    lineNumberBase = lineNumber[i];
		if (lineNumber[i] > lineNumberMax)
		    lineNumberMax = lineNumber[i];
	    }
	    int lineDelta = lineNumberMax - lineNumberBase;
	    for (int i = 0; i < startPC.length; i++)
		if (startPC[i] > startPCMax)
		    startPCMax = startPC[i];
	    if (lineDelta < 256 && startPCMax < 256)
		return new LineNumberTable_8_d8(startPC, lineNumber,
						lineNumberBase);
	    else if (lineDelta < 65536 && startPCMax < 65536)
		return new LineNumberTable_16_d16(startPC, lineNumber,
						  lineNumberBase);
	    else
		return new LineNumberTable_32_32(startPC, lineNumber);
	}
    } // end of Attribute.LineNumberTable

    static class LineNumberTable_8_d8 extends LineNumberTable {
	private final byte[] startPC;
	private final byte[] lineNumberDelta;
	private final int lineNumberBase;

	protected int tableSize() {
	    return startPC.length;
	}
	protected int tableStartPC(int idx) {
	    return startPC[idx] & 0xff;
	}
	protected int tableLineNumber(int idx) {
	    return lineNumberBase + (lineNumberDelta[idx] & 0xff);
	}
	LineNumberTable_8_d8(int[] startPC, int[] lineNumber,
			     int lineNumberBase) {
	    this.lineNumberBase = lineNumberBase;
	    this.startPC = new byte[startPC.length];
	    this.lineNumberDelta = new byte[startPC.length];
	    for (int i = 0; i < startPC.length; i++) {
		this.startPC[i] = (byte) startPC[i];
		lineNumberDelta[i] = (byte) (lineNumber[i] - lineNumberBase);
	    }
	}
    }

    static class LineNumberTable_16_d16 extends LineNumberTable {
	private final char[] startPC;
	private final char[] lineNumberDelta;
	private final int lineNumberBase;

	protected int tableSize() {
	    return startPC.length;
	}
	protected int tableStartPC(int idx) {
	    return startPC[idx];
	}
	protected int tableLineNumber(int idx) {
	    return lineNumberBase + lineNumberDelta[idx];
	}
	LineNumberTable_16_d16(int[] startPC, int[] lineNumber,
			     int lineNumberBase) {
	    this.lineNumberBase = lineNumberBase;
	    this.startPC = new char[startPC.length];
	    this.lineNumberDelta = new char[startPC.length];
	    for (int i = 0; i < startPC.length; i++) {
		this.startPC[i] = (char) startPC[i];
		lineNumberDelta[i] = (char) (lineNumber[i] - lineNumberBase);
	    }
	}
    }

    /**
     * The general <code>LineNumberTable</code> attribute, which is
     * used to  map source file line numbers to the VM code array.
     **/
    static class LineNumberTable_32_32 extends LineNumberTable {

        // ---------- the Line Number Table ------------------
	// The following two arrays together constitute a line number
	// table
	/**
	 * The table of starting PC indices. This is
	 * an array of indices into the byte code array at which the
	 * code for a new line in the source file code begins.
	 **/
	private final int[] tableStartPC_;
        
        /**
         * The corresponding source code line numbers for the start
         * PCs indices
         **/
        private final int[] tableLineNumber_;

	// --------- End of the Line Number Table --------------
	
        /**
         * Constructor, which builds the line number table for the
         * attribute
         * @param tableStartPC the table of byte code array indices at
         *                     which the corresponding source code lines
         *                     begin
         * @param tableLineNumber the source code line numbers which correspond
         *                        to the start PCs
         **/
        public LineNumberTable_32_32(int[] tableStartPC,
				     int[] tableLineNumber) {
            tableStartPC_ = tableStartPC;
            tableLineNumber_ = tableLineNumber;
        }

	protected int tableSize() {
	    return tableStartPC_.length;
	}
	protected int tableStartPC(int idx) {
	    return tableStartPC_[idx];
	}
	protected int tableLineNumber(int idx) {
	    return tableLineNumber_[idx];
	}
        public int[] getLineNumberTable() {
            return tableLineNumber_;
        }

        public int[] getStartPCTable() {
            return tableStartPC_;
        }
    }

    /**
     * The <code>LocalVariableTable</code> attribute which, if present,
     * maps a method's local variables to current information about 
     * the variable (name, descriptor, index of variable in the 
     * current frame, index into the code array, and so on).
     **/
    public static final class LocalVariableTable extends NonCritical {

		// --------------- the Local Variable Table -----------------
		// The following fields comprise the local variable table

		/**
		 * The start PC table - these are the minimum byte code
		 * array indices at which each variable entry must have a value.
		 **/
		private char[] tableStartPC_;
		/**
		 * The table of bytecode array index offsets 
		 * which, when added to the corresponding start PC value,
		 * gives the endpoint of the range of bytecode array offsets
		 * over which a given entry variable must have a value.
		 **/
		private char[] tableLength_;
		/**
		 * The corresponding indices of each entry in the local variable
		 * table of the current frame
		 **/
		private char[] tableIndex_;
		/**
		 * The table of repository utf8string indices which correspond
		 * to variable names for each entry
		 **/
		private int[] tableNameIndex_;
        /**
         * The corresponding descriptors for each table entry
         **/
        private Descriptor.Field[] descriptors_;

        // -------------- End of the Local Variable Table ------------

        /**
         * Constructor which builds the local variable table for this
         * attribute.
         * @param tableStartPC start PC array; these are the minimum byte code
         *                     array indices at which each variable entry 
         *                     must have a value
         * @param tableLength  array of bytecode array offsets from the
         *                     start PC offset of each variable which
         *                     are used to compute the interval a variable
         *                     must have a value
         * @param tableNameIndex array of repository utf8string indices for
         *                       each variable name
         * @param descriptors array of variable descriptors
         * @param tableIndex array of indices of each variable in the
         *                   local variable table of the current frame
         **/
        public LocalVariableTable(
            char[] tableStartPC,
            char[] tableLength,
            int[] tableNameIndex,
            Descriptor.Field[] descriptors,
            char[] tableIndex) {

            tableStartPC_ = tableStartPC;
            tableLength_ = tableLength;
            tableNameIndex_ = tableNameIndex;
            descriptors_ = descriptors;
            tableIndex_ = tableIndex;
        }

        public final void accept(RepositoryProcessor visitor) {
            visitor.visitAttrLocalVariableTable(this);
        }

	/**
	 * Returns the repository utf8string id of this attribute.
	 * @return the repository utf8string id of this attribute
	 **/			
        public int getNameIndex() {
            return ByteCodeConstants.attributeNames[Attributes.LocalVariableTable];
        }
    
         public Descriptor.Field getDescriptor(int i) {
             return descriptors_[i];
         }

         public int getIndex(int i) {
             return tableIndex_[i];
         }

         public int getLength(int i) {
             return tableLength_[i];
         }

         public int getStartPC(int i) {
             return tableStartPC_[i];
         }

         public String getVariableName(int i) {
             return UTF8Store._.getUtf8(tableNameIndex_[i]).toString();
         }

         public int getVariableNameIndex(int i) {
             return tableNameIndex_[i];
         }

	
	
	/**
	 * Get the descriptor for the given local variable
	 * @param i the local variable table index at which the desired
	 *          variable is located
	 * @param pc for which PC
	 * @return the descriptor for the desired variable
	 **/
        public Descriptor.Field getDescriptor(int i,
					      int pc) {
	    for (int j=0;j<descriptors_.length;j++) 
		if ( (tableIndex_[j] == i) &&
		     (tableStartPC_[j] <= pc) &&
		     (pc - tableStartPC_[j] < tableLength_[j]) )
		    return descriptors_[j];
	    return null; // not found
        }

        public int getVariableNameIndex(int i,
                                        int pc) {
            for (int j=0;j<tableNameIndex_.length;j++) 
                if ((tableIndex_[j] == i)
                    && (tableStartPC_[j] <= pc)
                    && (pc - tableStartPC_[j] < tableLength_[j]))
                    return tableNameIndex_[j];
            return -1; // not found
        }

        /**
         * Get the <code>String</code> representation of the name
         * of the variable at a given index in the local variable
         * table.
         * @param i the local variable table index at which the desired
         *          variable is located
         * @return the String representation of the variable's name
         **/
        public String getVariableName(int i, int pc) {
            int idx = getVariableNameIndex(i, pc);
            return (idx == -1 ? null
                    : UTF8Store._.getUtf8(idx).toString());
        }
	/**
	 * Get the size of the local variable table
	 * @return the size of this attribute's local variable table
	 **/
        public int size() {
            return tableIndex_.length;
        }

	public String toString() {
	    StringBuffer buf = new StringBuffer("LocalVariableTable {\n");
	    for (int i = 0; i < tableIndex_.length; i++) {
		buf.append("  [");
		buf.append((int) tableStartPC_[i]);
		buf.append(", ");
		buf.append((int) (tableStartPC_[i] + tableLength_[i]));
		buf.append("): ");
		buf.append((int) tableIndex_[i]);
		buf.append(" = ");
		buf.append(descriptors_[i]);
		buf.append(" ");
		buf.append(UTF8Store._.getUtf8(tableNameIndex_[i]));
		buf.append("\n");
	    }
	    buf.append("}");
	    return buf.toString();
	}
    } // end of Attribute.LocalVariableTable

    /**
     * The root class of all non-critical attributes. These attributes
     * can safely be ignored, when convenient.
     **/
    static abstract class NonCritical extends Attribute {
    }

    /**
     * The <code>SourceFile</code> attribute, which gives information about
     * the source file from which a class derives.
     **/
    public static class SourceFile extends NonCritical {

        /**
         * The repository utf8string index of the source file name
         **/
        private int srcFileNameIndex_;

        /**
         * Constructor - sets the utf8string index of name of the associated
         *               source file
         * @param srcFileNameIndex the utf8string index of the name
         **/
        public SourceFile(int srcFileNameIndex) {
            srcFileNameIndex_ = srcFileNameIndex;
        }

        public final void accept(RepositoryProcessor visitor) {
            visitor.visitAttrSourceFile(this);
        }

		/**
		 * Returns the repository utf8string id of this attribute.
		 * @return the repository utf8string id of this attribute
		 **/
        public int getNameIndex() {
            return ByteCodeConstants.attributeNames[Attributes.SourceFile];
        }

		/**
		 * Get the <code>String</code> representation of the name
		 * of the source file specified by this attribute
		 * @return the name of the sourcefile as a string
		 **/
        public String getSourceFileName() {
            return UTF8Store._.getUtf8(getSourceFileNameIndex()).toString();
        }

		/**
		 * Get the repository utf8string index of the name of
		 * the source file specified by this attribute
		 * @return the repository utf8string id of the sourcefile 
		 **/
        public int getSourceFileNameIndex() {
            return srcFileNameIndex_;
        }

    } // end of Attribute.SourceFile

    /**
     * The <code>synthetic</code> attribute. This is kept as a
     * singleton by {@link Attribute}. Class members that do 
     * not appear in the source code are labelled with a 
     * <code>Synthetic</code> attribute. (See the JVM Spec, 4.7)
     **/
    public static abstract class Synthetic extends NonCritical {

		// The subclasses to specify whether the <code>synthetic</code>
		// attribute belongs to a class, method, or field.
		//
		/**
		 * The <code>synthetic</code> attribute for classes. 
		 * @see Attribute.Synthetic
		 */
        public static class Class extends Attribute.Synthetic {
	    
	    public static Class SINGLETON = new Class();

	    private Class() {}

		// The subclasses to specify whether the <code>synthetic</code>
            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrSynthetic(this);
            }
        }
        
		/**
		 * The <code>synthetic</code> attribute for fields
		 * @see Attribute.Synthetic
		 */
        public static class Field extends Attribute.Synthetic {

	    public static Field SINGLETON = new Field();

	    private Field() {}

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrSynthetic(this);
            }
        }
        
		/**
		 * The <code>synthetic</code> attribute for methods. 
		 * @see Attribute.Synthetic
		 */
        public static class Method extends Attribute.Synthetic {

	    public static Method SINGLETON = new Method();

	    private Method() {}

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrSynthetic(this);
            }
        }

	/**
	 * The <code>synthetic</code> attribute for code.
	 * @see Attribute.Synthetic
	 */
        public static class Bytecode extends Attribute.Synthetic {

	    public static Bytecode SINGLETON = new Bytecode();

	    private Bytecode() {}

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrSynthetic(this);
            }
        }
        
        public abstract void accept(RepositoryProcessor visitor);

		/**
		 * Returns the repository utf8string id of this attribute.
		 * @return the repository utf8string id of this attribute
		 **/
        public int getNameIndex() {
            return ByteCodeConstants.attributeNames[Attributes.Synthetic];
        }

    } // end of Attribute.Synthetic

    /**
     * Indicates a third party attribute.
     **/
    public static abstract class ThirdParty extends NonCritical {
    	
    	/**
    	 * Attribute for third-party byte code fragments.
    	 */
        public static class ByteCodeFragment extends Attribute.ThirdParty {

			/**
			 * Constructor. Sets the attribute name index and content
			 * (byte code) for this third=party attribute.
			 * @param nameIndex the utf8string id of the name of this 
			 *                  attribute.
			 * @param content the byte array containing the contents of this
			 *                byte code fragment
			 */
            public ByteCodeFragment(int nameIndex, byte[] content) {
                super(nameIndex, content);
            }

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrThirdParty(this);
            }
        }

		// The subclasses to specify whether the third-party
		// attribute belongs to a class, method, or field.
		//

		/**
	     * The third-party attribute for classes. 
		 * @see Attribute.ThirdParty
		 */       
        public static class Class extends Attribute.ThirdParty {
			
            public Class(int nameIndex, byte[] content) {
                super(nameIndex, content);
            }

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrThirdParty(this);
            }
        }
        
		/**
		 * The third-party attribute for fields. 
		 * @see Attribute.ThirdParty
		 */
        public static class Field extends Attribute.ThirdParty {

            public Field(int nameIndex, byte[] content) {
                super(nameIndex, content);
            }

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrThirdParty(this);
            }
        }
        
		/**
		 * The third-party attribute for methods. 
		 * @see Attribute.ThirdParty
		 */
        public static class Method extends Attribute.ThirdParty {

            public Method(int nameIndex, byte[] content) {
                super(nameIndex, content);
            }

            public final void accept(RepositoryProcessor visitor) {
                visitor.visitAttrThirdParty(this);
            }
        }

        /**
         * Content of this attribute. This byte array contains all
         * information about the attribute as specified in the JVM
         * specifications (see 4.7), but without the attribute's name and
         * length.
         */
        private byte[] attributeContent_;

        /**
         * Attribute name utf8string id.
         **/
        private final int attributeNameIndex_;

        /**
         * <code>ThirdPartyAttribute</code> constructor.
         * @param  nameIndex  attribute's name index in the repository
         * @param  content    byte array of the attribute's content
         */
        public ThirdParty(int nameIndex, byte[] content) {
            attributeNameIndex_ = nameIndex;
            attributeContent_ = content;
            /* d("Attribute " + repository_.getUtf8(nameIndex)
               + " size " + content.length);
            */
        }

        public abstract void accept(RepositoryProcessor visitor);

        /**
         * Returns this attribute's content
         * @return byte array of attribute content
         */
        public byte[] getContent() {
            return attributeContent_;
        }
        
		/**
		 * Returns the repository utf8string id of this attribute.
		 * @return the repository utf8string id of this attribute
		 **/
        public int getNameIndex() {
            return attributeNameIndex_;
        }

    } // end of Attribute.ThirdParty

    /**
     * Indicates the <code>deprecated</code> attribute for a class (should be a singleton)
     **/
    public static final Deprecated deprecatedClass = new Deprecated.Class();
    /**
     * Indicates the <code>deprecated</code> attribute for a field (should be a singleton)
     **/
    public static final Deprecated deprecatedField = new Deprecated.Field();
    /**
     * Indicates the <code>deprecated</code> for a method attribute (should be a singleton)
     **/
    public static final Deprecated deprecatedMethod = new Deprecated.Method();

    /**
     * The empty set of attributes.
     **/
    final static Attribute[] EMPTY_ARRAY = new Attribute[0];

    /**
     * Constructor.
     **/
    protected Attribute() {
        //d("Built " + repository_.getUtf8(getNameIndex()));
    }

    /**
     * A visitor pattern accept method which is required for each attribute.
     * @param visitor the visitor object to accept
     **/
    abstract public void accept(RepositoryProcessor visitor);

    /**
     * Return the name of this attribute as a <code>String</code>
     * @return the String representation of this attribute's name
     **/
    public String getName() {
        return UTF8Store._.getUtf8(getNameIndex()).toString();
    }

    /**
     * Returns the repository utf8string id of this attribute.
     * @return the repository utf8string id of this attribute
     **/
    public abstract int getNameIndex();

} // end of Attribute
