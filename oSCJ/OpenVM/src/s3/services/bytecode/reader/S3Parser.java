/** 
 **/
package s3.services.bytecode.reader;

import ovm.core.domain.LinkageException;
import ovm.core.repository.Attribute;
import ovm.core.repository.Bytecode;
import ovm.core.repository.ConstantClass;
import ovm.core.repository.ConstantPool;
import ovm.core.repository.ConstantPoolBuilder;
import ovm.core.repository.Descriptor;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.JavaNames;
import ovm.core.repository.Mode;
import ovm.core.repository.RepositoryBuilder;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryString;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import ovm.core.repository.UnboundSelector;
import ovm.services.bytecode.reader.ByteCodeConstants;
import ovm.services.bytecode.reader.Parser;
import ovm.util.ByteBuffer;
import ovm.util.UnicodeBuffer;

/** 
 * The S3Parser parses the bytecode of a Java class to create repository
 * objects representing this class. Typically usage is as follows:<pre>
 *   Parser cfp = new S3Parser();
 *   FileReader cfr = new S3FileReader();
 *   RepositoryClass clz = null;
 *   try { 
 *     byte[] bytes = cfr.getBytes(someFile);
 *     clz    = cfp.parse( bytes, bytes.length );
 *  } catch (LinkageException.ClassFormat err) { ...
 *  } 
 *</pre>
 *  
 * The result of the <code>parse()</code> method is an unverified
 * <code>S3Class</code>. It has not been installed in the repository,
 * though the Utf8's, <code>String</code>s and <code>Selectors</code>
 * it includes have been installed. (This is a bit optimistic, we
 * assume that the class will pass verification and be installed as
 * well. This should be the common case. A DOS attack would be to
 * throw a lot of ill formed classes at the OVM.)<p>
 *
 * See JVM Spec Chapter 4.
 * 
 * @author Vitek, Palacz, Grothoff, Liang, Pawlak, Razafimahefa
 **/
public class S3Parser
    extends s3.core.S3Base
    implements ByteCodeConstants, Parser {

    /**
     * How many bytes did this parser parse? Merely used for
     * statistical purposes.
     **/
    public static long stats_parsed_bytes;

    protected ByteBuffer stream_;
    private int streamLength_;

    /**
     * Class name given by loader. This name is used only for error
     * message, the actual name in the class file is used for everything
     * else.
     **/
    private String className_;    


    protected ConstantPool cp_;
    protected ConstantPoolBuilder cpBuilder_;
    private final RepositoryClass.Builder classBuilder_;
    private final RepositoryMember.Method.Builder methodBuilder_;
    private final RepositoryMember.Field.Builder fieldBuilder_;
    private final Bytecode.Builder codeBuilder_;

    private int current_class_cp_ix; // remember to reinit

    /* Wether the constant pool will be cleaned by the parser */
    private boolean willCleanConstantPoolOnLoad = true;

    public S3Parser() {
        classBuilder_ = new RepositoryClass.Builder();
        methodBuilder_ = new RepositoryMember.Method.Builder();
        fieldBuilder_ = new RepositoryMember.Field.Builder();
        codeBuilder_ = new Bytecode.Builder();
    }

    /**
     * For debugging purposes, it is usefull to keep the constant pool
     * complete, i.e. don't clean it up.
     *
     * This method give a mean to tell the Parser whether we want such a
     * cleanup to be done when the class is loaded.
     * @param b
     **/
    public void doConstantPoolCleanupOnLoad(boolean b){
	willCleanConstantPoolOnLoad = b;
    }

    /**
     * Parse the given Java class file and return an object that represents
     * the contained data, i.e., constants, methods, fields and commands.
     * A <em>LinkageException.ClassFormat</em> is raised, if the file is not a
     * valid .class file. (This does not include verification of the byte
     * code as it is performed by the java interpreter).
     * @param name name of the class
     * @param byteStream the bytecode
     * @param length length of the bytecode
     * @return <code>RepositoryClass</code> object representing the 
     * parsed class file.
     * @exception LinkageException.ClassFormat if the class file is invalid.
     **/
    public final RepositoryClass parse(
        String name,
        ByteBuffer byteStream,
        int length)
        throws LinkageException.ClassFormat {
        className_ = name;
        return parse(byteStream, length);
    }

    /**
     * Parse the given Java class file and return an object that
     * represents the contained data, i.e., constants, methods, fields
     * and commands.  A <em>LinkageException.ClassFormat</em> is raised, if
     * the file is not a valid .class file. (This does not include
     * verification of the byte code as it is performed by the java
     * interpreter).
     * @param byteStream the bytecode
     * @param length length of the bytecode
     * @return <code>RepositoryClass</code> object representing the parsed 
     * class file.
     * @exception LinkageException.ClassFormat if the class file is invalid.
     **/
    public final RepositoryClass parse(ByteBuffer byteStream, int length)
        throws LinkageException.ClassFormat {
        stream_ = byteStream;
        streamLength_ = length;
        return parse();
    }

    /* **************** actual parsing code ***************** */

    /**
     * Parse a class file (JVM Spec chapter 4)
     *
     * basic layout as described there:
     *
     * ClassFile {
     *    	u4 magic;
     *   	u2 minor_version;
     *   	u2 major_version;
     *   	u2 constant_pool_count;
     *   	cp_info constant_pool[constant_pool_count-1];
     *   	u2 access_flags;
     *   	u2 this_class;
     *   	u2 super_class;
     *   	u2 interfaces_count;
     *   	u2 interfaces[interfaces_count];
     *   	u2 fields_count;
     *   	field_info fields[fields_count];
     *   	u2 methods_count;
     *   	method_info methods[methods_count];
     *   	u2 attributes_count;
     *   	attribute_info attributes[attributes_count];
     * }
     *
     * Does not depend on the source of the data (i.e. file, zip, net),
     * expects that the <code>code_</code> byte array has been initialized.
     **/
    private final synchronized RepositoryClass parse()
        throws LinkageException.ClassFormat {
        stats_parsed_bytes += streamLength_;

        current_class_cp_ix = 0;
        /****************** Read headers ********************************/
        // Check magic tag of class file
        stream_.getInt();// should check that fileTag == JVMConstants.CLASSFILE_MAGIC 
        classBuilder_.setVersions(stream_.getChar(), stream_.getChar());
        classBuilder_.setSize(streamLength_);
        try {
            /****************** Read constant pool and related **************/
            // Read constant pool entries
            byte[] tags = parseConstantPool();
            // Get class information
            parseClassInfo();
            // Get interface information, i.e., implemented interfaces
            parseInterfaces();

            /****************** Read class fields and methods ***************/
            // Read class fields, i.e., the variables of the class
            parseFields();

            // Read class methods, i.e., the functions in the class
            parseMethods();

            // Read class attributes
            parseAttributes(classBuilder_);

	    if (isSynthetic(classBuilder_)) {
		Mode.Class m = classBuilder_.getAccessMode().makeSynthetic();
		classBuilder_.setAccessMode(m);
	    }

            //check that there is no garbage past the end of the array.
            checkOffset(streamLength_);

            cleanConstantPool(tags);

            // Return the information we have gathered in a new object
            RepositoryClass result = classBuilder_.build();
            return result;
        } catch (ConstantPool.AccessException ae) {
            throw new LinkageException.ClassFormat(className_ + ": " +  ae.getMessage(), ae);						   
        } catch (RuntimeException e) {
            throw new LinkageException.ClassFormat("runtime exception parsing " + className_, e);
        } finally {
            classBuilder_.reset();
        }
    }

    /**
     * Read information about the class and its super class.
     * See JVM Spec 4.1
     *
     * The class information read by this method corresponds to the
     * following fields of the ClassFile structure:
     *   	u2 access_flags;
     *   	u2 this_class;
     *   	u2 super_class;
     **/
    private final void parseClassInfo()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int access_flags = stream_.getChar();
        classBuilder_.setAccessMode(Mode.makeClass(access_flags));
        current_class_cp_ix = stream_.getChar();
        int super_name_ix = stream_.getChar();
        // Ensure this class is a child of Object if no other parent given
        if (current_class_cp_ix == super_name_ix)
            super_name_ix = 0;
        classBuilder_.setName(getCurrentClassName());
	ConstantClass superclass = cp_.getClassAt(super_name_ix);
	if (superclass == null) {
	    classBuilder_.setSuperClassName(null);
	} else {
	    classBuilder_.setSuperClassName
		(superclass.asTypeName().asScalar());
	}
    }

    /**
     * Read information about the interfaces implemented by this class.
     * See JVM Spec 4.1
     *
     * Each Interface is structured this way:
     *    CONTANT_Class_info {
     *    	u1 tag;
     *    	u2 name_index;
     *    }
     **/
    private final void parseInterfaces()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int interfacesCount = stream_.getChar();
        for (int i = 0; i < interfacesCount; i++) {
            int cpIfaceIx = stream_.getChar();
            classBuilder_.declareInterface(cp_.getClassAt(cpIfaceIx)
					   .asTypeName().asScalar());
        }
    }

    /**
     * Read constants from given file stream.  The constant pool is
     * processed in three passes.  The first pass figures out the
     * position of entries in the bytecode array code_ and reads
     * primitive constants.
     *
     * Pass 1 gets all the tags and the offsets of all entries (poses),
     * and parses utf8 and constant values (int, float, long, double).
     *
     * Pass 2 parses Class, String and NameAndType (UnboundSelector) entries.
     *
     * Pass 3 parses (bound) Selector entries.
     *
     * At the end of this pass the ConstantPool is
     * completely build.
     *
     * See JVM Spec 4.4.
     * @exception Exception if the byte code is not valid (some Errors are probably
     *            also possible)
     **/
    protected byte[] parseConstantPool()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int size = stream_.getChar();

        cpBuilder_ = new ConstantPoolBuilder();
        cpBuilder_.realloc(size);
        byte[] tags = new byte[size];
        int[] poses = new int[size];
        cpParsePhase1(tags, poses);
        int end_position = stream_.position();
        cpParsePhase2(tags, poses);
        cpParsePhase3(tags, poses);
        stream_.position(end_position);
        return tags;
    }

    protected void cpParsePhase1(byte[] tags, int[] poses)
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int size = tags.length;

        for (int i = 1; i < size; i++) {
            byte tag = stream_.get();
            tags[i] = tag;
            poses[i] = stream_.position();
	    // d("Pos " + i + " is " + tag  + " at " +  poses[i]);
            switch (tag) {
                case CONSTANT_String :
                case CONSTANT_Class :
                    stream_.advance(2);
                    break;
                case CONSTANT_Fieldref :
                case CONSTANT_NameAndType :
                case CONSTANT_InterfaceMethodref :
                case CONSTANT_Methodref :
                    stream_.advance(4);
                    break;
                case CONSTANT_Integer :
                    cpBuilder_.makeIntValueAt(stream_.getInt(), i);
                    break;
                case CONSTANT_Float :
                    cpBuilder_.makeFloatValueAt(stream_.getFloat(), i);
                    break;
                case CONSTANT_Long :
                    cpBuilder_.makeLongValueAt(stream_.getLong(), i);
                    /* JVM spec: "All eight byte constants take up two spots in
                     * the constant pool. If this is the n'th byte in the
                     * constant pool, then the next item will be numbered n+2"
                     * Thus the index counter is incremented.
                     **/
                    i++;
                    break;
                case CONSTANT_Double :
                    cpBuilder_.makeDoubleValueAt(stream_.getDouble(), i);
                    /* JVM spec: "All eight byte constants take up two spots in
                     * the constant pool. If this is the n'th byte in the
                     * constant pool, then the next item will be numbered n+2"
                     * Thus the index counter is incremented.
                     **/
                    i++;
                    break;
                case CONSTANT_Utf8 :
                    int length = stream_.getChar();
		    UnicodeBuffer buf = UnicodeBuffer.factory().wrap
			(stream_.array(), stream_.position(), length);
		    stream_.position(stream_.position() + length);
                    cpBuilder_.makeUtf8At(buf, i);
                    break;
                default :
                    unexpectedTag(tag, i, size);
                    break;
            }
        }
        cp_ = cpBuilder_.build(); // the cp_ is partially constructed
    }

    protected void unexpectedTag(byte tag, int i, int size)
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        throw new ClassParsingException(
            "Invalid CP tag: " + tag + " at " + i + "/" + size);
    }

    protected void cpParsePhase2(byte[] tags, int[] poses)
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int size = tags.length;
        for (int i = 1; i < size; i++) {
            switch (tags[i]) {
                case CONSTANT_Class :
                    stream_.position(poses[i]);
                    int cpName = stream_.getChar();
                    int utf_ix = cp_.getUtf8IndexAt(cpName);
                    cpBuilder_.makeTypeNameAt(utf_ix, i);// side effect repository
                    break;
                case CONSTANT_String :
                    stream_.position(poses[i]);
                    int cpName2 = stream_.getChar();
                    cpBuilder_.makeUnresolvedStringAt(cp_.getUtf8IndexAt(cpName2), i);

                    break;
                case CONSTANT_NameAndType :
                    stream_.position(poses[i]);
                    int name_ix2 = stream_.getChar();
                    int descriptor_ix2 = stream_.getChar();
                    cpBuilder_.makeUnboundSelectorAt(
                        cp_.getUtf8IndexAt(name_ix2),
                        cp_.getUtf8IndexAt(descriptor_ix2),
                        i);
                    break;
                case CONSTANT_Double :
                case CONSTANT_Long :
                    i++;
            }
        }
        cp_ = cpBuilder_.build();
    }

    protected void cpParsePhase3(byte[] tags, int[] poses)
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int size = tags.length;
        for (int i = 1; i < size; i++) {
            switch (tags[i]) {
                case CONSTANT_Fieldref :
                    stream_.position(poses[i]);
                    int class_ix = stream_.getChar();
                    int name_and_type_ix = stream_.getChar();
                    TypeName.Compound defClass = cp_.getClassAt(class_ix)
			.asTypeName().asCompound();
                    UnboundSelector.Field usel =
                        (
                            UnboundSelector
                                .Field) cp_
                                .getUnboundSelectorAt(
                            name_and_type_ix);
                    cpBuilder_.makeFieldAt(defClass, usel, i);
                    break;
                case CONSTANT_InterfaceMethodref :
                case CONSTANT_Methodref :
                    stream_.position(poses[i]);
                    int class_ix2 = stream_.getChar();
                    int name_and_type_ix2 = stream_.getChar();
                    TypeName.Compound defClass2 = cp_.getClassAt(class_ix2)
			.asTypeName().asCompound();
                    UnboundSelector.Method usel2 =
                        (
                            UnboundSelector
                                .Method) cp_
                                .getUnboundSelectorAt(
                            name_and_type_ix2);
                    cpBuilder_.makeMethodAt(defClass2, usel2, i, tags[i]);
                    break;
                case CONSTANT_Double :
                case CONSTANT_Long :
                    i++;
            }
        }
        cp_ = cpBuilder_.build(); // the cp_ is fully constructed
    }

    /**
     * Remove no longer needed entries from the CP and declare it.
     **/
    protected void cleanConstantPool(byte[] tags) {
        /* clean up constant pool: delete redundant, never directly
           referenced entries */
	if(willCleanConstantPoolOnLoad)
	    for (int i = 1; i < tags.length; i++) {
		switch (tags[i]) {
		case CONSTANT_Utf8 :
		case CONSTANT_NameAndType :
		    cpBuilder_.deleteEntryAt(i);
		    break;
		default :
		    break;
		}
	    }
        cp_ = cpBuilder_.build();
        classBuilder_.declareConstantPool(cp_);
    }

    private Descriptor parseDescriptor(int idx) {
	return Descriptor.parse(UTF8Store._.getUtf8(idx));
    }
    /**
     * Read information about the fields of the class, i.e., its variables.
     * See JVM Spec 4.5.
     *
     * Each Field is structured this way:
     *    field_info {
     *    	u2 access_flags;
     *    	u2 name_index;
     *    	u2 descriptor_index;
     *    	u2 attributes_count;
     *    	attribute_info attributes[attributes_count];
     *    }
     *
     * @throws  LinkageException.ClassFormat
     */
    private final void parseFields()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int fieldsCount = stream_.getChar();
        for (int i = 0; i < fieldsCount; i++) {
            int mode = stream_.getChar();
            int cpName = stream_.getChar();
            int cpDesc = stream_.getChar();
            fieldBuilder_.setMode(Mode.makeField(mode));
            int name_ix = cp_.getUtf8IndexAt(cpName);
            fieldBuilder_.setName(name_ix);
            int desc_ix = cp_.getUtf8IndexAt(cpDesc);
	    fieldBuilder_.setTypeName(getCurrentClassName());

            fieldBuilder_.setDescriptor
                ((Descriptor.Field) parseDescriptor(desc_ix));
            parseAttributes(fieldBuilder_);
	    if (isSynthetic(fieldBuilder_))
		fieldBuilder_.setMode(fieldBuilder_.getMode().makeSynthetic());
            classBuilder_.declareField(fieldBuilder_.build());
            fieldBuilder_.reset();
        }
    }

    /**
     * Read information about the methods of the class.
     * See JVM Spec 4.6
     *
     * Each Method is structured this way:
     *    method_info {
     *    	u2 access_flags;
     *    	u2 name_index;
     *    	u2 descriptor_index;
     *    	u2 attributes_count;
     *    	attribute_info attributes[attributes_count];
     *    }
     **/
    private final void parseMethods()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {

           int methods = stream_.getChar();
         
            for (int i = 0; i < methods; i++) {
                methodBuilder_.reset();
                codeBuilder_.reset();
                int mode = stream_.getChar();
                int cpName = stream_.getChar();
                int cpDesc = stream_.getChar();
                methodBuilder_.setMode(Mode.makeMethod(mode));
                int name_ix = cp_.getUtf8IndexAt(cpName);
                int desc_ix = cp_.getUtf8IndexAt(cpDesc);
		methodBuilder_.setName(name_ix);
                Descriptor.Method desc =
		    (Descriptor.Method) parseDescriptor(desc_ix);

                methodBuilder_.setDescriptor(desc);
                codeBuilder_.declareSelector(
                    getCurrentClassName(),
                    name_ix,
                    desc);
                Mode.Method mmode = methodBuilder_.getMode();
		if (mmode.isStatic())
		    methodBuilder_.setTypeName(getCurrentClassName().getGemeinsamTypeName());
		else
		    methodBuilder_.setTypeName(getCurrentClassName());
                parseAttributes(methodBuilder_);
		if (isSynthetic(methodBuilder_)) {
		    Mode.Method m = methodBuilder_.getMode().makeSynthetic();
		    methodBuilder_.setMode(m);
		}
                if (codeBuilder_.getCode() != null)
                    // did we find a code fragment?
                    methodBuilder_.addFragment(codeBuilder_.build());
                classBuilder_.declareMethod(methodBuilder_.build());
            }
    }

    boolean isSynthetic(RepositoryBuilder rb) {
	Attribute[] att = rb.getAttributes();
	for (int i = 0; i < att.length; i++) {
	    if (att[i] instanceof Attribute.Synthetic)
		return true;
	}
	return false;
    }

    /**
     * See JVM Spec 4.7
     *
     * Each attribute is structured this way:
     *    attribute_info {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u1 info[attribute_length];
     *    }
     **/
    private final void parseAttributes(RepositoryBuilder rb)
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int attr_cnt = stream_.getChar();

        for (int i = 0; i < attr_cnt; i++) {
            int cp_aname = stream_.getChar();
            int name_ix;
            if (cp_aname == 0)
                name_ix = 0; /* "null" */
            else
                name_ix = cp_.getUtf8IndexAt(cp_aname);
            int length = stream_.getInt();
            int expectedPos = stream_.position() + length;

            if (name_ix == attributeNames[Attributes.Code])
                parseCodeFragment();
            else if (name_ix == attributeNames[Attributes.LineNumberTable])
                parseLineNumberTableAttribute();
            else if (name_ix == attributeNames[Attributes.LocalVariableTable])
                parseLocalVariableTableAttributes();
            else if (name_ix == attributeNames[Attributes.Exceptions])
                parseExceptionAttributes();
            else if (name_ix == attributeNames[Attributes.ConstantValue])
                parseConstantValueAttribute();
            else if (name_ix == attributeNames[Attributes.InnerClasses])
                parseInnerClassesAttribute();
            else if (name_ix == attributeNames[Attributes.SourceFile])
                parseSourceFileAttribute();
            else if (name_ix == attributeNames[Attributes.Synthetic])		
                parseSyntheticAttribute(rb);
            else if (name_ix == attributeNames[Attributes.Deprecated])
                parseDeprecatedAttribute(rb);
            else {
                byte[] arr = new byte[length];
                stream_.get(arr);
                rb.declareAttribute(name_ix, arr);
            }
            checkOffset(expectedPos);
        }
    }

    /**
     * See JVM Spec 4.7.2
     *
     * Each ConstantValue attribute is structured this way:
     *    ConstantValue_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u2 constantvalue_index;
     *    }
     **/
    private final void parseConstantValueAttribute()
        throws ClassParsingException, ConstantPool.AccessException {
        int constantIx = stream_.getChar();
        byte tag = cp_.getTagAt(constantIx);
        switch (tag) {
            case CONSTANT_Integer :
            case CONSTANT_Float :
                fieldBuilder_.setConstantValueBits(cp_.getValueAt(constantIx));
                break;
            case CONSTANT_Long :
            case CONSTANT_Double :
                fieldBuilder_.setConstantValueBits(
                    cp_.getWideValueAt(constantIx));
                break;
            case CONSTANT_String :
                RepositoryString st = cp_.getUnresolvedStringAt(constantIx);
                int utf8index = st.getUtf8Index();
                fieldBuilder_.setConstantValueBits(utf8index);
                break;
            default :
                throw new ClassParsingException("cannot deal with tag " + tag);
        }
    }

    /**
     * See JVM Spec 4.7.3
     *
     * Each Code attribute is structured this way:
     *    Code_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u2 max_stack;
     *    	u2 max_locals;
     *    	u4 code_length;
     *    	u1 code[code_length];
     *    	u2 exception_table_length;
     *    	{    	u2 start_pc;
     *    	      	u2 end_pc;
     *    	      	u2  handler_pc;
     *    	      	u2  catch_type;
     *    	}	exception_table[exception_table_length];
     *    	u2 attributes_count;
     *    	attribute_info attributes[attributes_count];
     *    }
     **/
    private final void parseCodeFragment()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        char maxStack = stream_.getChar();
        char maxLocals = stream_.getChar();
        codeBuilder_.declareTemporaries(maxStack, maxLocals);

        int codeLength = stream_.getInt();
        ByteBuffer code = ByteBuffer.allocate(codeLength);
        stream_.get(code.array());
        codeBuilder_.setCode(code);
        codeBuilder_.setConstantPool(cp_);

        int excpTblLen = stream_.getChar();
        //	codeBuilder_.hintExceptionTableLength(excpTblLen);
        for (int j = 0; j < excpTblLen; j++) {
            char startPc = stream_.getChar();
            char endPc = stream_.getChar();
            char handlerPc = stream_.getChar();
            char catchTypeIx = stream_.getChar();

	    ConstantClass catchTypeCC = cp_.getClassAt(catchTypeIx);
            TypeName.Scalar catchType = null;
            // per Christian's request
	    if (catchTypeCC == null) {
		catchType = JavaNames.java_lang_Throwable;
	    } else {
		catchType = catchTypeCC.asTypeName().asScalar();
	    }
            codeBuilder_.declareExceptionHandler(new ExceptionHandler(startPc,
								      endPc,
								      handlerPc,
								      catchType));
        }
        parseAttributes(codeBuilder_); // recursive
    }

    /**
     * See JVM Spec 4.7.4
     *
     * The Exception attribute is structured this way:
     *    Exceptions_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u2 number_of_exceptions;
     *    	u2 exception_index_table[number_of_exceptions];
     *    }
     **/
    private final void parseExceptionAttributes()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int numExcp = stream_.getChar();
        for (int j = 0; j < numExcp; j++) {
            int excpIx = stream_.getChar();
            TypeName.Scalar excpTypeName = cp_.getClassAt(excpIx)
		.asTypeName().asScalar();
            methodBuilder_.declareThrownException(excpTypeName);
        }
    }

    /**
     * See JVM Spec 4.7.5
     *
     * The InnerClasses attribute is structured this way:
     *    InnerClasses_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u2 number_of_classes;
     *    	{  u2 inner_class_info_index;	     
     *    	   u2 outer_class_info_index;	     
     *    	   u2 inner_name_index;	     
     *    	   u2 inner_class_access_flags;	     
     *    	} classes[number_of_classes];
     *    }
     **/
    private final void parseInnerClassesAttribute()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int class_cnt = stream_.getChar();
        TypeName.Scalar[] inner = new TypeName.Scalar[class_cnt];
        TypeName.Scalar[] outer = new TypeName.Scalar[class_cnt];
        int[] innername = new int[class_cnt];
        Mode.Class[] mode = new Mode.Class[class_cnt];
        for (int j = 0; j < class_cnt; j++) {
            int inner_class_info_ix = stream_.getChar();
	    ConstantClass inner_cc = cp_.getClassAt(inner_class_info_ix);
            inner[j] = 
		inner_cc == null 
		? null
		: inner_cc.asTypeName().asScalar();
            int outer_class_info_ix = stream_.getChar();
	    ConstantClass outer_cc = cp_.getClassAt(outer_class_info_ix);
            outer[j] = 
		outer_cc == null
		? null
		: outer_cc.asTypeName().asScalar();
            int inner_name_ix = stream_.getChar();
            if (inner_name_ix != 0) { // not anonymous class
                innername[j] = cp_.getUtf8IndexAt(inner_name_ix);
            }
            int inner_class_access_flags = stream_.getChar();
            mode[j] = Mode.makeClass(inner_class_access_flags);

            if (inner_class_info_ix == current_class_cp_ix) {
		classBuilder_.setOuterClassName(outer[j]);
            } else if (outer_class_info_ix == current_class_cp_ix) {
                if (mode[j].isInnerStatic())
                    classBuilder_.declareStaticInnerClass(inner[j]);
                else
                    classBuilder_.declareInstanceInnerClass(inner[j]);
            }
        }
        classBuilder_.declareInnerClasses(inner, outer, innername, mode);
    }

    /**
     * See JVM Spec 4.7.6
     *
     * The SyntheticAttribute is structured this way:
     *    Synthetic_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    }
     **/
    private final void parseSyntheticAttribute(RepositoryBuilder rb) {
        rb.declareSynthetic();
    }

    /**
     * See JVM Spec 4.7.7
     *
     * The SourceFile Attribute is structured this way:
     *    SourceFile_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u2 sourcefile_index;
     *    }
     **/
    private final void parseSourceFileAttribute()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int cpFileNameIx = stream_.getChar();
        assert cpFileNameIx != 0 : "SourceFile CP index is 0";
        int fileNameIx = cp_.getUtf8IndexAt(cpFileNameIx);
        classBuilder_.declareSourceFile(fileNameIx);
    }

    /**
     * See JVM Spec 4.7.8
     *
     * The LineNumberTable attribute is structured this way:
     *    LineNumberTable_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u2 line_number_table_length;
     *    	{  u2 start_pc;	     
     *    	   u2 line_number;	     
     *    	} line_number_table[line_number_table_length];
     *    }
     **/
    private final void parseLineNumberTableAttribute() {
        int line_number_table_length = stream_.getChar();
        int[] start_pc = new int[line_number_table_length];
        int[] line_number = new int[line_number_table_length];
        for (int j = 0; j < line_number_table_length; j++) {
            start_pc[j] = stream_.getChar();
            line_number[j] = stream_.getChar();
        }
        codeBuilder_.declareAttribute(Attribute.LineNumberTable.make(start_pc, line_number));
    }

    /**
     * See JVM Spec 4.7.9
     *
     * The LocalVariable attribute is structured this way:
     *    LocalVariableTable_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    	u2 local_variable_table_length;
     *    	{  u2 start_pc;
     *    	    u2 length;
     *    	    u2 name_index;
     *    	    u2 descriptor_index;
     *    	    u2 index;
     *    	} local_variable_table[local_variable_table_length];
     *    }
     **/
    private final void parseLocalVariableTableAttributes()
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        char length = stream_.getChar();
        // these tables contain local variable's values. 
        char startPCs[] = new char[length]; // start_pc
        char lengths[] = new char[length]; // length
        int nameIndices[] = new int[length]; // name_ix
        Descriptor.Field descs[] =
            new Descriptor.Field[length];
        // desc_ix
        char indices[] = new char[length]; // index   
        for (int j = 0; j < length; j++) {
            startPCs[j] = stream_.getChar();
            lengths[j] = stream_.getChar();
            int cpName = stream_.getChar();
            int cpDesc = stream_.getChar();
            nameIndices[j] = cp_.getUtf8IndexAt(cpName);
            int desc_ix = cp_.getUtf8IndexAt(cpDesc);
            descs[j] =  (Descriptor.Field) parseDescriptor(desc_ix);
            indices[j] = stream_.getChar();
        }
        codeBuilder_.declareAttribute
	    (new Attribute.LocalVariableTable
	     (startPCs,
	      lengths,
	      nameIndices,
	      descs,
	      indices));
    }

    /**
     * See JVM Spec 4.7.10
     *
     * The Deprecated attribute is structured this way:
     *
     *    Deprecated_attribute {
     *    	u2 attribute_name_index;
     *    	u4 attribute_length;
     *    }
     **/
    private final void parseDeprecatedAttribute(RepositoryBuilder rb) {
        rb.declareDeprecated();
    }

    /* ***************** helper methods **************** */

    /**
     * After parsing the bytecode we will check that there is no trailing
     * garbage, ie. unread data past the point where we think the end of
     * the file should be. Also we report an error if we somehow managed to
     * read more (this can occur only if we use an array that is physically
     * longer than the bytecode file, ie. to save having to allocate.)
     * @exception LinkageException.ClassFormat if the bytecode file is longer
     * than expected.
     **/
    private final void checkOffset(int expectedPosition)
        throws ClassParsingException {
        if (stream_.position() != expectedPosition)
            throw new ClassParsingException(
                "trailing garbage ("
                    + stream_.position()
                    + "/"
                    + expectedPosition
                    + ")");
    }

    private TypeName.Scalar getCurrentClassName()
        throws ConstantPool.AccessException {
        return cp_.getClassAt(current_class_cp_ix)
	    .asTypeName().asScalar();
    }

    /* ************** Inner classes ***************** */

    /**
     * Some exception...
     **/
    public class ClassParsingException extends LinkageException.ClassFormat {
        public ClassParsingException(String message) {
            super(message);
        }
        public ClassParsingException(String m, Throwable e) {
            super(m, e);
        }
        public String getMessage() {
            return ((className_ != null) ? className_ + ": " : "")
                + super.getMessage();
        }
    } // End of S3Parser.ClassParsingException

    /**
     * @author Jan Vitek
     **/
    public static class Factory
        implements ovm.services.bytecode.reader.Parser.Factory {

        public Parser makeParser() {
            return new S3Parser();
        }
    } // End of S3Parser.Factory

} // End of S3Parser
