package ovm.services.bytecode.reader;

import java.lang.reflect.Field;

import ovm.core.repository.RepositoryUtils;
import ovm.services.bytecode.JVMConstants;
import ovm.util.OVMError;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;

/** 
 * Constants occuring in Java class files; strings and
 * other non-inlinable constants.
 * @author Jan Vitek
 **/
public interface ByteCodeConstants extends JVMConstants {

    /** 
     * Symbolic names of the tags.      
     **/
    public final static String[] CONSTANT_NAMES =
        {
            "",
            "CONSTANT_Utf8",
            "",
            "CONSTANT_Integer",
            "CONSTANT_Float",
            "CONSTANT_Long",
            "CONSTANT_Double",
            "CONSTANT_Class",
            "CONSTANT_String",
            "CONSTANT_Fieldref",
            "CONSTANT_Methodref",
            "CONSTANT_InterfaceMethodref",
            "CONSTANT_NameAndType" };

    public interface Attributes {
        int SourceFile         = 0;
        int ConstantValue      = 1;
        int Code               = 2;
        int Exceptions         = 3;
        int LineNumberTable    = 4;
        int LocalVariableTable = 5;
        int InnerClasses       = 6;
        int Synthetic          = 7;
        int Deprecated         = 8;
    }

    public final int[] attributeNames = new int[Attributes.Deprecated + 1];

    public final String[] names = new String[512];
 
    public static final X x = new X(); // we need to trigger initialization
    // there ought to be a better place to do this, especially we should not
    // expose that in the interface
    public static class X { // a hack to initialize the fields names and attributes
        static {
            reflect(JVMConstants.Opcodes.class);
            reflect(Attributes.class);
        }

        private static void reflect(Class cl) throws BCbootTime {
            Field[] fields = cl.getDeclaredFields();
            try {
                for (int i = 0; i < fields.length; i++) {
                    Class type = fields[i].getType();
                    if ( !type.isPrimitive() ) continue;
                    if ( type == int.class ) {
                        int j = ((Number)fields[i].get(cl)).intValue();
                        if (cl == JVMConstants.Opcodes.class)
                            names[j] = fields[i].getName().toLowerCase();
                        else
                            attributeNames[j] = RepositoryUtils.asUTF(fields[i].getName());
                    }
                }
            } catch (IllegalAccessException e) {
                throw new OVMError("in Enum(" + cl + ") to field " + e);
            }
        }
    }
}