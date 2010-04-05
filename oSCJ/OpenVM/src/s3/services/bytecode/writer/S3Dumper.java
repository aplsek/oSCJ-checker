package s3.services.bytecode.writer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ovm.core.repository.Attribute;
import ovm.core.repository.Bytecode;
import ovm.core.repository.ConstantPool;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryProcessor;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.repository.UnboundSelector;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.editor.ClassCleaner;
import ovm.services.bytecode.writer.Dumper;
import ovm.util.CommandLine;
import ovm.util.HTObject2int;
import ovm.util.OVMError;
import org.ovmj.util.Runabout;
import s3.core.S3Base;
import s3.services.bytecode.S3ClassProcessor;

/**
 * Dump an OVM structure to Java <code>.class</code> format.
 *
 * @author KP
 * @author Christian Grothoff
 **/
public class S3Dumper 
    extends S3Base 
    implements Dumper {

    /**
     * Stream that we are currently writing the dump to.
     **/
    protected DataOutputStream out;
    
    /**
     * The class that this dumper writes to an output stream.
     **/
    private final RepositoryClass cls;

    /**
     * The AttributeDumper (helper class to dump attributes)
     **/
    private AttributeDumper attributeDumper;
    
    /**
     * Don't we all like symbolic constants? 
     **/
    private final static int SIZEOF_SHORT = 2;

    /**
     * Context information.
     **/
    private final Dumper.Context ctx;
  
 
    /**
     * Create a dumper to write cls.
     **/
    public S3Dumper(Dumper.Context ctx,
		    RepositoryClass cls) {
	this.cls = new ClassCleaner().clean(cls); 
	this.ctx = ctx;
	// this makes sure that there is only one constant pool for the
	// entire class and not one per method!
    }
     
    /**
     * @param isClean pass true here if the class has only one constant pool
     *        and does not need a cleanup-phase
     * FIXME if true is passed, we may still want to check that it's actually true.
     **/
    public S3Dumper(Dumper.Context ctx,
		    RepositoryClass cls,
		    boolean isClean) {
	if (isClean)
	    this.cls = cls;
	else
	    this.cls = new ClassCleaner().clean(cls); 
	this.ctx = ctx;
    }
    
    /**
     * Dump the class to the appropriate ".class" file in the directory
     * prefix.
     * @param prefix the name of the directory where the class file is written
     **/
    public synchronized void dump(String prefix) 
	throws IOException, 
	       ConstantPool.AccessException,
	       FileNotFoundException {
	String name = cls.getName().toClassInfoString()
	    .replace('/', File.separatorChar) + ".class";
	File f = new File(prefix, name);
	if (f.exists()) 
	    f.delete();	
	File dir = new File(f.getParent());
	dir.mkdirs();
	if (! dir.exists()) 
	    throw new OVMError("Couldn't create directory: " + dir);	
	BufferedOutputStream buf = 
	    new BufferedOutputStream(new FileOutputStream(f), 4*1024);
	out = new DataOutputStream(buf);
	dumpInternal();
	buf.flush();
	out.flush();
	out.close();
    }
    
    /**
     * Write the .class file to the specified output stream.
     **/
    public synchronized void dump(DataOutputStream o) 
	throws IOException, ConstantPool.AccessException {
	this.out = o;
	dumpInternal();
    }

    /* ********************internal stuff ******************** */

    private void growFullCP() {
	Object[] tmp = new Object[fullCp.length*2];
	System.arraycopy(fullCp, 0,
			 tmp, 0,
			 fullCp.length);
	fullCp = tmp;
    }

    /**
     * Increment fullCpUsedCount to refer to the next free slot
     * in fullCp. Also grows fullCp if there is no free slot left.
     **/
    private void findNextFreeSlot() {
	if (fullCp.length == fullCpUsedCount) 
	    growFullCP();
	while (fullCp[fullCpUsedCount] != null) {
	    if ( (fullCp[fullCpUsedCount] instanceof Double) ||
		 (fullCp[fullCpUsedCount] instanceof Long) )
		fullCpUsedCount++; // skip 2 for long or double
	    fullCpUsedCount++;
	    if (fullCpUsedCount == fullCp.length)
		growFullCP();
	}
    }

    /**
     * The actual "main" method: dumps the class to the out stream.
     **/
    private void dumpInternal() 
	throws IOException, ConstantPool.AccessException {
	fullCp = cls.getConstantPool().getValues();
	if (fullCp.length == 0)
	    fullCp = new Object[4]; // may not start with size 0
	for (int i=0;i<fullCp.length;i++)
	    if (fullCp[i] != null)
		map_.put(fullCp[i],i);
	findNextFreeSlot();

	DataOutputStream real_out = this.out;
	ByteArrayOutputStream bas = new ByteArrayOutputStream();
	DataOutputStream temp_out = new DataOutputStream(bas);

	// write everything but CP to "temp_out" -- builds CP at the side!
	this.out = temp_out;
	this.attributeDumper = new AttributeDumper();	
	dumpClassInfo();
	dumpInterfaces();
	dumpFields();
	dumpMethods();
	dumpAttributes();

	ByteArrayOutputStream bas2 = new ByteArrayOutputStream();
	DataOutputStream temp2_out = new DataOutputStream(bas2);

	// write constant pool to "temp2_out" -- builds rest of CP
	this.out = temp2_out;
 	dumpConstantPool();

	// write class-file header to "out"
	this.out = real_out;
	out.writeInt(JVMConstants.CLASSFILE_MAGIC);
	out.writeChar(cls.getMajorVersion()); 
	out.writeChar(cls.getMinorVersion());	
	out.writeChar(fullCpUsedCount); // only after dumpConstantPool we know the true number of constants!

	// finally append "bas2" and "bas" to "out"
	bas2.writeTo(out);
	bas.writeTo(out);
    }

    /* **************** generic dumping of the class components ************ */

    
    private void dumpClassInfo() throws IOException {
	// FIXME here we force the ACC_SUPER -- CG: what's wrong with that?
	out.writeChar(cls.getAccessMode().getMode() | JVMConstants.ACC_SUPER);
	dumpCPIndex(addConstant(cls.getName()));
	int super_name;
	if (cls.hasSuper()) 
	    super_name = addConstant(cls.getSuper());
	else 
	    super_name = 0;		
	dumpCPIndex(super_name);
    }

    private void dumpInterfaces() throws IOException {
	TypeName.Scalar[] ifaces = cls.getInterfaces();
	int length = ifaces.length;
	out.writeChar(length);
	for (int i = 0; i < length; i++) 
	    dumpCPIndex(addConstant(ifaces[i]));	
    }
    
    private void dumpFields() throws IOException {
	RepositoryMember.Field[] staticFields = cls.getStaticFields();
	RepositoryMember.Field[] instanceFields = cls.getInstanceFields();
	out.writeChar(staticFields.length + instanceFields.length);
	for (int i = 0; i < staticFields.length; i++) 
	    dumpField(staticFields[i]);	
	for (int i = 0; i < instanceFields.length; i++) 
	    dumpField(instanceFields[i]);
    }
    
    private void dumpMethods() throws IOException {
	RepositoryMember.Method[] staticMethods = cls.getStaticMethods();
	RepositoryMember.Method[] instanceMethods = cls.getInstanceMethods();
	out.writeChar(staticMethods.length + instanceMethods.length);
	for (int i = 0; i < staticMethods.length; i++) 
	    dumpMethod(staticMethods[i]);	
	for (int i = 0; i < instanceMethods.length; i++) 
	    dumpMethod(instanceMethods[i]);	
    }

    private void dumpField(RepositoryMember.Field field) throws IOException {
 	out.writeChar(field.getMode().getMode());
	dumpCPIndex(addUtf8Constant(field.getName()));
	dumpCPIndex(addUtf8Constant(field.getDescriptor().toString()));
	attributeDumper.visitField(field);
    }
    
    private void dumpMethod(RepositoryMember.Method method) throws IOException {
	out.writeChar(method.getMode().getMode());
	dumpCPIndex(addUtf8Constant(method.getName()));
	dumpCPIndex(addUtf8Constant(method.getDescriptor().toString()));
	attributeDumper.visitMethod(method);
    }

    private void dumpAttributes() throws IOException {
	Attribute[] attributes = cls.getAttributes();
	out.writeChar(attributes.length);
	attributeDumper.visitClass(cls);	
    }

    /**
     * Visitor that writes Attributes to the stream.
     * 
     * @author KP
     **/
    private class AttributeDumper 
	extends RepositoryProcessor {

	public void visitAttrInnerClasses(Attribute.InnerClasses a) {
	    try {
		ByteArrayOutputStream bas = new ByteArrayOutputStream();
		DataOutputStream oout = new DataOutputStream(bas);
		int number_of_classes = a.size();
		oout.writeChar(number_of_classes);
		for (int i = 0; i < number_of_classes; i++ ) {
		    TypeName.Scalar innerClass = a.getInnerClass(i);
		    if (innerClass != null) {
			oout.writeChar(addConstant(innerClass));
		    } else {
			oout.writeChar(0);
		    }
		    TypeName.Scalar outerClass = a.getOuterClass(i);
		    if (outerClass != null) {
			oout.writeChar(addConstant(outerClass));
		    } else {
			oout.writeChar(0);
		    }
		    int nameIndex = a.getInnerNameIndex(i);
		    if (nameIndex != 0) {
			String name = UTF8Store._.getUtf8(nameIndex).toString();
			oout.writeChar(addUtf8Constant(name));
		    } else {
			oout.writeChar(0);
		    }
		    // let's assume we don't deal with old code
		    oout.writeChar(a.getMode(i).getMode()|JVMConstants.ACC_SUPER);
		}
		int attribute_name_index = addUtf8Constant("InnerClasses");
		out.writeChar(attribute_name_index);
		int attribute_length = bas.size();
		out.writeInt(attribute_length);
		bas.writeTo(out);
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
	public void visitAttrSourceFile(Attribute.SourceFile sf) {
	    int attribute_name_index = addUtf8Constant("SourceFile");
	    assert(attribute_name_index > 0);
	    try {		    
		out.writeChar(attribute_name_index);
		out.writeInt(2);
		out.writeChar(addUtf8Constant(sf.getSourceFileName()));
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
	public void visitAttrDeprecated(Attribute.Deprecated.Method notused) {
	    int attribute_name_index = addUtf8Constant("Deprecated");
	    assert(attribute_name_index > 0);
	    try {
		out.writeChar(attribute_name_index);
		out.writeInt(0);
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
	public void visitAttrDeprecated(Attribute.Deprecated.Class notused) {
	    int attribute_name_index = addUtf8Constant("Deprecated");
	    assert(attribute_name_index > 0);
	    try {
		out.writeChar(attribute_name_index);
		out.writeInt(0);
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
	public void visitAttrDeprecated(Attribute.Deprecated.Field notused) {
	    int attribute_name_index = addUtf8Constant("Deprecated");
	    assert(attribute_name_index > 0);
	    try {
		out.writeChar(attribute_name_index);
		out.writeInt(0);
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
        public void visitAttrSynthetic(Attribute.Synthetic.Class notused) {
            int attribute_name_index = addUtf8Constant("Synthetic");
            assert(attribute_name_index > 0);
            try {
            out.writeChar(attribute_name_index);
            out.writeInt(0);
            } catch(IOException e) {
            throw new OVMError(e);
            }
        }
    
        public void visitAttrSynthetic(Attribute.Synthetic.Field notused) {
            int attribute_name_index = addUtf8Constant("Synthetic");
            assert(attribute_name_index > 0);
            try {
            out.writeChar(attribute_name_index);
            out.writeInt(0);
            } catch(IOException e) {
            throw new OVMError(e);
            }
        }
    
        public void visitAttrSynthetic(Attribute.Synthetic.Method notused) {
            int attribute_name_index = addUtf8Constant("Synthetic");
            assert(attribute_name_index > 0);
            try {
            out.writeChar(attribute_name_index);
            out.writeInt(0);
            } catch(IOException e) {
            throw new OVMError(e);
            }
        }

	public void visitAttrLineNumberTable(Attribute.LineNumberTable lt) {
	    int attribute_name_index = addUtf8Constant("LineNumberTable");
	    int[] start_pc = lt.getStartPCTable();
	    int[] line_number = lt.getLineNumberTable();
	    assert(attribute_name_index > 0);
	    int line_number_table_length = start_pc.length;
	    try {
		out.writeChar(attribute_name_index);
		out.writeInt(SIZEOF_SHORT + (2*SIZEOF_SHORT)*line_number_table_length);
		out.writeChar(line_number_table_length);
		for (int i = 0; i < line_number_table_length; i++) {
		    out.writeChar((char)start_pc[i]);
		    out.writeChar((char)line_number[i]);
		}
	    } catch(IOException e) {
		throw new OVMError(e);
	    }
	}
	
	public void visitAttrLocalVariableTable(Attribute.LocalVariableTable lt) {
	    int attribute_name_index = addUtf8Constant("LocalVariableTable");
	    assert(attribute_name_index > 0);

	    int local_variable_table_length = lt.size();
	    try {
		out.writeChar(attribute_name_index);
		out.writeInt(SIZEOF_SHORT +
				    local_variable_table_length*SIZEOF_SHORT*5);
		out.writeChar(local_variable_table_length);
		for (int i = 0; i < local_variable_table_length; i++) {
		    out.writeChar(lt.getStartPC(i));
		    out.writeChar(lt.getLength(i));
		    out.writeChar(addUtf8Constant(lt.getVariableName(i)));
		    out.writeChar(addUtf8Constant(lt.getDescriptor(i).toString()));
		    out.writeChar(lt.getIndex(i));
		}
	    } catch(IOException e) {
		throw new OVMError(e);
	    }
	}

    public void visitAttrThirdParty(Attribute.ThirdParty.Class a) {
        int attribute_name_index = addUtf8Constant(a.getName());
        try {
        out.writeChar(attribute_name_index);
        byte[] contents = a.getContent();
        out.writeInt(contents.length);
        out.write(contents);
        } catch (IOException e) {
        throw new OVMError(e);
        }
    }
    
    public void visitAttrThirdParty(Attribute.ThirdParty.Field a) {
        int attribute_name_index = addUtf8Constant(a.getName());
        try {
        out.writeChar(attribute_name_index);
        byte[] contents = a.getContent();
        out.writeInt(contents.length);
        out.write(contents);
        } catch (IOException e) {
        throw new OVMError(e);
        }
    }
    
    public void visitAttrThirdParty(Attribute.ThirdParty.Method a) {
        int attribute_name_index = addUtf8Constant(a.getName());
        try {
        out.writeChar(attribute_name_index);
        byte[] contents = a.getContent();
        out.writeInt(contents.length);
        out.write(contents);
        } catch (IOException e) {
        throw new OVMError(e);
        }
    }
    
    public void visitAttrThirdParty(Attribute.ThirdParty.ByteCodeFragment a) {
        int attribute_name_index = addUtf8Constant(a.getName());
        try {
        out.writeChar(attribute_name_index);
        byte[] contents = a.getContent();
        out.writeInt(contents.length);
        out.write(contents);
        } catch (IOException e) {
        throw new OVMError(e);
        }
    }
    
	public void visitClass(RepositoryClass rclass) {
	    Attribute[] attrs = rclass.getAttributes();
	    for (int i = 0; i < attrs.length; i++) {
		attrs[i].accept(this);
	    }
	}
	
	public void visitField(RepositoryMember.Field field) {
	    Attribute[] attrs = field.getAttributes();
	    try {
		int attribute_count = attrs.length;
		Object constantValue = field.getConstantValue();
		if (constantValue != null) 
		    attribute_count++;
		out.writeChar(attribute_count);
		if (constantValue != null) {
		    out.writeChar(addUtf8Constant("ConstantValue"));
		    out.writeInt(SIZEOF_SHORT);
		    out.writeChar(addConstant(constantValue));
		}
		if (attrs.length > 0) 
		    visit(attrs);
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
	void visit(Attribute[] attrs) {
	    for (int i = 0; i < attrs.length; i++) 
		attrs[i].accept(this);	    
	}

	public void visitByteCodeFragment(Bytecode cf) {
	    try {
		if (CodeIndex == 0) 
		    CodeIndex = addUtf8Constant("Code");		
		out.writeChar(CodeIndex);
		ByteArrayOutputStream bas = new ByteArrayOutputStream();
		DataOutputStream saved = out;
		out = new DataOutputStream(bas);
		out.writeChar(cf.getMaxStack());
		out.writeChar(cf.getMaxLocals());
		byte[] code = cf.getCode();
		out.writeInt(code.length);
		out.write(code);
		ExceptionHandler[] handlers = cf.getExceptionHandlers();
		out.writeChar(handlers.length);
		for (int i = 0; i < handlers.length; i++) {
		    out.writeChar(handlers[i].getStartPC());
		    out.writeChar(handlers[i].getEndPC());
		    out.writeChar(handlers[i].getHandlerPC());
		    TypeName.Compound catchTypeName = handlers[i].getCatchTypeName();
		    if (catchTypeName != null) 
			out.writeChar(addConstant(catchTypeName));
		    else 
			out.writeChar(0);		    
		}
		Attribute[] attributes = cf.getAttributes();
		out.writeChar(attributes.length);
		visit(attributes);
		// d("attribute size " + out.size());
		saved.writeInt(out.size());
		bas.writeTo(saved);
		out = saved;
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
	public void visitMethod(RepositoryMember.Method method) {
	    Attribute[] attrs = method.getAttributes();
	    int attribute_count = attrs.length;
	    Bytecode cf = method.getCodeFragment();
	    if (!(method.getMode().isNative() 
		  || method.getMode().isAbstract())) 
		attribute_count++;
	    TypeName.Scalar[] thrownExceptions = method.getThrownExceptions();
	    if (thrownExceptions.length > 0) 
		attribute_count++;
	    try {
		// d("attribute length " + attribute_count);
		out.writeChar(attribute_count);
		if (attribute_count == 0) 
		    return;
		
		if (cf != null) 
		    cf.accept(this);		
		if (thrownExceptions.length > 0) {
		    if (ExceptionsIndex == 0) 
			ExceptionsIndex = addUtf8Constant("Exceptions");
		    out.writeChar(ExceptionsIndex);
		    int attribute_length = SIZEOF_SHORT + SIZEOF_SHORT*(thrownExceptions.length);
		    out.writeInt(attribute_length);
		    out.writeChar(thrownExceptions.length);
		    for (int i = 0; i < thrownExceptions.length; i++) {
			out.writeChar(addConstant(thrownExceptions[i]));
		    }
		}
		
		visit(attrs);
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

    } // end of S3Dumper.AttributeDumper


    /* ******************* constant pool handling ********************* */

    /**
     * The full constant pool that we are building.
     **/
    private Object[] fullCp; 

    /**
     * Lowest unused index in fullCp.
     **/
    private int fullCpUsedCount = 1;   

    /**
     * Constant pool entry for the utf "Code"
     **/
    private int CodeIndex = 0;

    /**
     * Constant pool entry for the utf "Exceptions"
     **/
    private int ExceptionsIndex = 0;  

    /**
     * Hashtable mapping constant-objects to indices 
     **/
    private HTObject2int map_ = new HTObject2int(65536);

    /**
     * Add a constant to the constant pool.
     * @param o the constant to add
     * @return the index of the constant
     **/
    protected int addConstant(Object o) {
	int i = map_.get(o);
	if (i != HTObject2int.NOTFOUND)
	    return i;
	/* we may have a hole of only one entry in the CP which would
	   not be suitable for a Double or Long. So in that case, find
	   a bigger hole! */
	if ( (o instanceof Double) ||
	     (o instanceof Long) ) {
	    i = fullCpUsedCount;
	    if (i >= fullCp.length - 2)
		growFullCP();
	    while ( (fullCp[i] != null) ||
		    (fullCp[i+1] != null) ) {
		i++;
		if (i >= fullCp.length - 2)
		    growFullCP();
	    }
	} else
	    i = fullCpUsedCount;
	map_.put(o,i);
	fullCp[i] = o;
	findNextFreeSlot();
	return i;
    }

    /**
     * Add a Utf8 string constant. This method must be used to distinguish
     * adding a String constant from adding a Utf8 String constant.
     **/
    protected int addUtf8Constant(String constant) {
	ConstantPool.UTF8Index utf = new ConstantPool.UTF8Index
	    (RepositoryUtils.asUTF(constant));
	return addConstant(utf);
    }
      
    /**
     * Helper method that writes a constant pool index into the stream
     * and checks that the index is valid.
     **/
    private void dumpCPIndex(int cpIndex) 
	throws IOException {
	if ( (fullCp.length <= cpIndex) || 
	     (cpIndex < 0) )
	    throw new OVMError("fatal: dumping bad constant pool index: " + 
			       cpIndex + "<0 or >=" + fullCp.length);
	out.writeChar((char)cpIndex);
    }
    
    private void dumpConstantPool() 
	throws IOException, ConstantPool.AccessException {	
	while ( (fullCpUsedCount<fullCp.length) &&
		(fullCp[fullCpUsedCount] != null) ) {
	    if ( (fullCp[fullCpUsedCount] instanceof Double) ||
		 (fullCp[fullCpUsedCount] instanceof Long) )
		fullCpUsedCount++; // skip 2 for long or double
	    fullCpUsedCount++;
	}
	Runabout cw = makeConstantWriter();
	for (int i = 1; i < fullCpUsedCount; i++) 
	    if (fullCp[i] != null) {
		// d("Dumping " + i + " at " + out.size());
		cw.visitAppropriate(fullCp[i]);	    
	    }

	// check if CP is valid (has not empty entries left after fullCpUsedCount)
	for (int i=fullCpUsedCount;i<fullCp.length;i++)
	    if (fullCp[i] != null) {		
		throw new OVMError("FATAL: constant pool not compact enough for"+
				   " dumping, run dumper with cleaning! ("
				   + i + " not null but max is " + fullCpUsedCount);
	    }
    } 

    protected Runabout makeConstantWriter() {
	return new ConstantWriter();
    }

    /**
     * Runabout that writes the constants to the stream. 
     *
     * @author Christian 'Runabout' Grothoff
     **/
    public class ConstantWriter
	extends Runabout {

	public void visitDefault(Object o) {
	    throw new OVMError("Unsupported constant: " + o);
	}

	public void visit(Integer i) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_Integer);
		out.writeInt(i.intValue());
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	
	public void visit(Float f) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_Float);
		out.writeFloat(f.floatValue());
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(Double d) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_Double);
		out.writeDouble(d.doubleValue());
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(Long l) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_Long);
		out.writeLong(l.longValue());
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(ConstantPool.UTF8Index utf) {
	    try {
		int index = utf.getIndex();
		out.writeByte(JVMConstants.CONSTANT_Utf8);
		out.writeChar(UTF8Store._.getUtf8Length(index));
		UTF8Store._.writeUtf8(out, index);	    
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(RepositoryString s) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_String);
		out.writeChar(addConstant(s.getUTF8Index()));
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(TypeName.Compound t) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_Class);
		out.writeChar(addUtf8Constant(t.toClassInfoString()));
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(UnboundSelector.Method rus) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_NameAndType);
		out.writeChar(addUtf8Constant(rus.getName()));
		out.writeChar(addUtf8Constant(rus.getDescriptor().toString()));
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(UnboundSelector.Field rus) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_NameAndType);
		out.writeChar(addUtf8Constant(rus.getName()));
		out.writeChar(addUtf8Constant(rus.getDescriptor().toString()));
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}
	    
	public void visit(Selector.Field sel) {
	    try {
		out.writeByte(JVMConstants.CONSTANT_Fieldref);
		out.writeChar(addConstant(sel.getDefiningClass()));
		out.writeChar(addConstant(sel.getUnboundSelector()));
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

	public void visit(Selector.Method sel) {
	    try {
		if (ctx.isInterface(sel.getDefiningClass().asScalar())) {
		    out.writeByte(JVMConstants.CONSTANT_InterfaceMethodref);
		} else {
		    out.writeByte(JVMConstants.CONSTANT_Methodref);
		}
		out.writeChar(addConstant(sel.getDefiningClass()));
		out.writeChar(addConstant(sel.getUnboundSelector()));
	    } catch (IOException e) {
		throw new OVMError(e);
	    }
	}

    } // end of S3Dumper.ConstantWriter
    

    /**
     * Factory to make the PackageEnvironment mechanism work.
     * @author Jan Vitek
     **/
    static public class Factory 
	implements ovm.services.bytecode.writer.Dumper.Factory {

	public Dumper makeDumper(Dumper.Context ctx,
				 RepositoryClass cls) {
	    return  new S3Dumper( ctx, cls);
	}
    }  // End of S3Dumper.Factory


    /**
     * Helper method to run the dumper for testing.
     **/
    public static void main(String[] args) throws Exception {
	CommandLine cLine = new CommandLine();
	cLine.parse(args);
	final Dumper.Context ctx = new Dumper.Context() {
		public boolean isInterface(TypeName.Scalar tn) {
		    return false;
		}
	    };
	S3ClassProcessor proc = new S3ClassProcessor() {
		public void process(RepositoryClass clz) 
		    throws Exception {
		    new S3Dumper( ctx, clz).dump("../mods");
		}
	    };
	
	proc.runOnClasspath(cLine.getArgument(0), true);
    } // end of S3Dumper.main

} // end of S3Dumper
