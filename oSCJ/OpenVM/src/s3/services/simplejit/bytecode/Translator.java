package s3.services.simplejit.bytecode;

import ovm.core.repository.Attribute;
import ovm.core.repository.Constants;
import ovm.core.repository.Descriptor;
import ovm.core.repository.RepositoryProcessor;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.VM_Area;
import ovm.services.bytecode.ByteCodeGen;
import ovm.services.bytecode.ByteCodeGen2;
import ovm.services.bytecode.CodeExceptionGen;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionHandle;
import ovm.services.bytecode.InstructionList;
import ovm.services.bytecode.InstructionTargeter;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.SpecificationIR;
import ovm.services.bytecode.TargetLostException;
import ovm.services.bytecode.Instruction.ACONST_NULL;
import ovm.services.bytecode.Instruction.AFIAT;
import ovm.services.bytecode.Instruction.ANEWARRAY;
import ovm.services.bytecode.Instruction.ANEWARRAY_QUICK;
import ovm.services.bytecode.Instruction.ARRAYLENGTH;
import ovm.services.bytecode.Instruction.ATHROW;
import ovm.services.bytecode.Instruction.ArithmeticInstruction;
import ovm.services.bytecode.Instruction.ArrayAccess;
import ovm.services.bytecode.Instruction.BranchInstruction;
import ovm.services.bytecode.Instruction.CHECKCAST;
import ovm.services.bytecode.Instruction.CHECKCAST_QUICK;
import ovm.services.bytecode.Instruction.ConstantLoad;
import ovm.services.bytecode.Instruction.ConstantPoolLoad;
import ovm.services.bytecode.Instruction.Conversion;
import ovm.services.bytecode.Instruction.DCMPG;
import ovm.services.bytecode.Instruction.DCMPL;
import ovm.services.bytecode.Instruction.DFIAT;
import ovm.services.bytecode.Instruction.DUP;
import ovm.services.bytecode.Instruction.DUP2;
import ovm.services.bytecode.Instruction.DUP2_X1;
import ovm.services.bytecode.Instruction.DUP2_X2;
import ovm.services.bytecode.Instruction.DUP_X1;
import ovm.services.bytecode.Instruction.DUP_X2;
import ovm.services.bytecode.Instruction.Visitor;
import ovm.services.bytecode.Instruction.ExceptionThrower;
import ovm.services.bytecode.Instruction.FCMPG;
import ovm.services.bytecode.Instruction.FCMPL;
import ovm.services.bytecode.Instruction.FFIAT;
import ovm.services.bytecode.Instruction.FieldAccess;
import ovm.services.bytecode.Instruction.FieldAccess_Quick;
import ovm.services.bytecode.Instruction.FlowEnd;
import ovm.services.bytecode.Instruction.GETFIELD;
import ovm.services.bytecode.Instruction.GETFIELD2_QUICK;
import ovm.services.bytecode.Instruction.GETFIELD_QUICK;
import ovm.services.bytecode.Instruction.GOTO;
import ovm.services.bytecode.Instruction.GotoInstruction;
import ovm.services.bytecode.Instruction.IADD;
import ovm.services.bytecode.Instruction.IConstantLoad;
import ovm.services.bytecode.Instruction.IFIAT;
import ovm.services.bytecode.Instruction.IINC;
import ovm.services.bytecode.Instruction.INSTANCEOF;
import ovm.services.bytecode.Instruction.INSTANCEOF_QUICK;
import ovm.services.bytecode.Instruction.INVOKESTATIC;
import ovm.services.bytecode.Instruction.INVOKE_NATIVE;
import ovm.services.bytecode.Instruction.INVOKE_SYSTEM;
import ovm.services.bytecode.Instruction.If;
import ovm.services.bytecode.Instruction.Invocation;
import ovm.services.bytecode.Instruction.Invocation_Quick;
import ovm.services.bytecode.Instruction.JsrInstruction;
import ovm.services.bytecode.Instruction.LCMP;
import ovm.services.bytecode.Instruction.LFIAT;
import ovm.services.bytecode.Instruction.LocalAccess;
import ovm.services.bytecode.Instruction.LocalRead;
import ovm.services.bytecode.Instruction.LocalWrite;
import ovm.services.bytecode.Instruction.MONITORENTER;
import ovm.services.bytecode.Instruction.MONITOREXIT;
import ovm.services.bytecode.Instruction.MULTIANEWARRAY;
import ovm.services.bytecode.Instruction.MULTIANEWARRAY_QUICK;
import ovm.services.bytecode.Instruction.NEW;
import ovm.services.bytecode.Instruction.NEWARRAY;
import ovm.services.bytecode.Instruction.NEW_QUICK;
import ovm.services.bytecode.Instruction.NOP;
import ovm.services.bytecode.Instruction.POLLCHECK;
import ovm.services.bytecode.Instruction.POP;
import ovm.services.bytecode.Instruction.POP2;
import ovm.services.bytecode.Instruction.PUTFIELD;
import ovm.services.bytecode.Instruction.PUTFIELD2_QUICK;
import ovm.services.bytecode.Instruction.PUTFIELD_QUICK;
import ovm.services.bytecode.Instruction.PUTSTATIC;
import ovm.services.bytecode.Instruction.PrimFiat;
import ovm.services.bytecode.Instruction.READ_BARRIER;
import ovm.services.bytecode.Instruction.REF_GETFIELD_QUICK;
import ovm.services.bytecode.Instruction.RET;
import ovm.services.bytecode.Instruction.ROLL;
import ovm.services.bytecode.Instruction.ReturnInstruction;
import ovm.services.bytecode.Instruction.SINGLEANEWARRAY;
import ovm.services.bytecode.Instruction.SWAP;
import ovm.services.bytecode.Instruction.StackManipulation;
import ovm.services.bytecode.Instruction.Switch;
import ovm.services.bytecode.Instruction.Synchronization;
import ovm.services.bytecode.Instruction.WIDE;
import ovm.services.bytecode.Instruction.WIDE_RET;
import ovm.services.bytecode.Instruction.WidePrimFiat;
import ovm.services.bytecode.JVMConstants.DereferenceOps;
import ovm.services.bytecode.JVMConstants.WordOps;
import ovm.util.BitSet;
import ovm.util.ArrayList;
import ovm.util.HTint2Object;
import ovm.util.HashMap;
import ovm.util.IdentityHashMap;
import ovm.util.OVMError;
import ovm.util.Vector;
import ovm.util.Set;
import ovm.util.Iterator;
import ovm.core.domain.Method;
import s3.core.domain.S3ByteCode;
import s3.services.bytecode.ovmify.NativeCallGenerator;
import s3.services.simplejit.SimpleJIT;

/*
 * This program translates Java bytecode into another form of Java bytecode 
 * that is suitable for virtual register allocation and so on. The output 
 * bytecode has the following property:
 * 
 * 1. There are no subroutines - Subroutine bodies are duplicated so that 
 * there is one copy of subroutine body for each subroutine call site. JSRs 
 * and RETs are replaced by GOTOs.
 * 
 * 2. All intermediate values are stored once into a local variable - A bytecode 
 * instruction that produces a value on the operand stack is immediately followed 
 * by a store instruction which stores the produced value into a local variable. In 
 * a similar manner, a bytecode instruction that consumes a value or more on the 
 * operand stack is immediately preceded by a sequence of the exact number of load 
 * instructions for the consumed values. If a consumed value is a constant, a constant 
 * pusing instruction may appear instead of a load instruction. Consequently, at every 
 * instruction point (except loads or stores) the operand stack contains only the inputs 
 * to the instruction. This property enables the bytecode to be treated as a register based 
 * code where a local variable corresponds to a register. Note that IINC and stack instructions 
 * (SWAP, DUP, etc) are removed.
 * 
 * 3. The stack height is zero at the beginning and the end of every basic blocks 
 * (except for exception handler entries, subroutine entries and right after exception edges). 
 * 
 * 4. Locals are mono-typed - each local variable holds values of a specific type (I, J, F, D, A) for 
 * the entire method.
 * 
 * @author Hiroshi Yamauchi
 */
public class Translator {

    private static final char[] emptyCharArray = new char[0];
    private static final char[] I = new char[] { TypeCodes.INT };
    private static final char[] A = new char[] { TypeCodes.REFERENCE };
    private static final char[] F = new char[] { TypeCodes.FLOAT };
    private static final char[] D = new char[] { TypeCodes.DOUBLE };
    private static final char[] J = new char[] { TypeCodes.LONG };
    private static final char[] II = new char[] { TypeCodes.INT, TypeCodes.INT };
    private static final char[] AA = new char[] { TypeCodes.REFERENCE, TypeCodes.REFERENCE };
    private static final char[] AAI = new char[] { TypeCodes.REFERENCE, TypeCodes.REFERENCE, TypeCodes.INT};
    private static final char[] IA = new char[] { TypeCodes.INT, TypeCodes.REFERENCE };
    private static final char[] IAA = new char[] { TypeCodes.INT, TypeCodes.REFERENCE, TypeCodes.REFERENCE };

    
    private static String charArrayToString(char[] arr) {
        String ret = "[ ";
        for(int i = 0; i < arr.length; i++)
            ret += arr[i] + " ";
        return ret + "]";
    }
    
    private static void ensureSize(ArrayList list, int minimum) {
	int addCount = minimum - list.size();
	for(int i = 0; i < addCount; i++) {
	    list.add(null);
	}
    }

    private static boolean bitSetInclude(BitSet a, BitSet b) {
	BitSet _a = (BitSet)a.clone();
	_a.or(b);
	return _a.equals(a);
    }

    private static class Assume {

        public static class AssumptionException extends RuntimeException {
            public AssumptionException() {
                super();
            }
            public AssumptionException(String s) {
                super(s);
            }
            public AssumptionException(Throwable t) {
                super(t);
            }
        }
        
        public static void that(boolean condition) {
            if (! condition)
                throw new AssumptionException(); 
        }
        
        public static void that(boolean condition, String msg) {
            if (! condition) {
                //BasicIO.err.println("Assumption did not hold : " + msg);
                throw new AssumptionException(msg);
            }
        }
        
    }
    private static interface TypeCodes extends ovm.core.repository.TypeCodes { 
        char LONG_UPPER = 'j';
        char DOUBLE_UPPER = 'd';
    }

    /**
     * The class that handles commandline options. There are two types of options: boolean and value.
     * Options are registered with the register methods before the actual options are read.
     *  
     * @author yamauchi
     */
    private static class CommandLine {

        private abstract static class Option {
            String name;

            String desc;

            protected Option(String name, String desc) {
                this.name = name;
                this.desc = desc;
            }
        }

        /**
         * The boolean type option - an option which is either true or false.
         */
        private static class BooleanOption extends Option {
            boolean value;

            BooleanOption(String name, boolean value, String desc) {
                super(name, desc);
                this.value = value;
            }
        }

        /**
         * The value type option - an option which can take a string value.
         */
        private static class ValueOption extends Option {
            String value;

            ValueOption(String name, String value, String desc) {
                super(name, desc);
                this.value = value;
            }
        }

        HashMap options;

        String[] args;

        CommandLine(String[] args) {
            this.args = args;
            options = new HashMap();
        }

        public void registerBooleanOption(String name, boolean defaultValue,
                String description) {
            options.put(name,
                    new BooleanOption(name, defaultValue, description));
        }

        public void registerValueOption(String name, String defaultValue,
                String description) {
            options.put(name, new ValueOption(name, defaultValue, description));
        }

        private void printOptions() {
            Set keySet = options.keySet();
            String usage = "Usage: java Translator <options> <classname>...\n";
            usage += "Options:\n";
            for (Iterator it = keySet.iterator(); it.hasNext();) {
                Object key = it.next();
                Option option = (Option) options.get(key);
                String desc = option.desc;
                usage += " -" + option.name + " : " + option.desc + "\n";
            }
            BasicIO.err.println(usage);
        }

        private void printErrorMessageAndExit(String name) {
            BasicIO.err.println("Invalid option : " + name);
            printOptions();
            System.exit(1);
        }

        public void read() {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (!arg.startsWith("-"))
                    continue;
                StringBuffer sb = new StringBuffer(arg);
                int eqIdx = sb.indexOf("=");

                if (eqIdx >= 0) { // value option
                    String name = sb.substring(1, eqIdx);
                    if (options.get(name) == null) {
                        printErrorMessageAndExit(name);
                    }
                    String value = sb.substring(eqIdx + 1);
                    Object type = options.get(name);
                    if (type instanceof ValueOption) {
                        ValueOption vo = (ValueOption) type;
                        vo.value = value;
                    } else {
                        printErrorMessageAndExit(name);
                    }
                } else { // boolean option
                    String name = sb.substring(1);
                    if (options.get(name) == null) {
                        printErrorMessageAndExit(name);
                    }
                    Object type = options.get(name);
                    if (type instanceof BooleanOption) {
                        BooleanOption bo = (BooleanOption) type;
                        bo.value = true;
                    } else {
                        printErrorMessageAndExit(name);
                    }
                }
            }
        }

        public boolean isTrue(String name) {
            return ((BooleanOption) options.get(name)).value;
        }

        public String getValue(String name) {
            return ((ValueOption) options.get(name)).value;
        }

    }

        
    /* The utility methods */

    /**
     * Convert the upper half word type of Long and Double into the lower half word type.
     * TypeCodes.LONG_UPPER -> TypeCodes.LONG
     * TypeCodes.DOUBLE_UPPER -> TypeCodes.DOUBLE
     * _ -> _
     */
    private static char toLowerHalf(char t) {
        if (t == TypeCodes.LONG_UPPER)
            return TypeCodes.LONG;
        else if (t == TypeCodes.DOUBLE_UPPER)
            return TypeCodes.DOUBLE;
        else
            throw new Error();
    }

    /**
     * Convert the lower half word type of Long and Double into the upper half word type.
     * TypeCodes.LONG -> TypeCodes.LONG_UPPER
     * TypeCodes.DOUBLE -> TypeCodes.DOUBLE_UPPER
     * _ -> _
     */
    private static char toUpperHalf(char t) {
        if (t == TypeCodes.LONG)
            return TypeCodes.LONG_UPPER;
        else if (t == TypeCodes.DOUBLE)
            return TypeCodes.DOUBLE_UPPER;
        else
            throw new Error();
    }
    
    private static int typeCode2Size(char t) {
        if (t == TypeCodes.LONG || t == TypeCodes.DOUBLE)
            return 2;
        else if (t == TypeCodes.INT || t == TypeCodes.FLOAT || t == TypeCodes.REFERENCE
                || t == TypeCodes.SHORT || t == TypeCodes.CHAR || t == TypeCodes.BYTE 
                || t == TypeCodes.BOOLEAN 
                || t == TypeCodes.OBJECT 
                || t == TypeCodes.ARRAY) 
            return 1;
        else
            throw new Error();
    }
    
    /**
     * Convert the set of Use's or Def's into a String.
     * @param set
     * @return the String representation of the set
     */
    private static String dataflowSetToString(IdentityHashSet set) {
        if (set == null)
            return "Null";
        String r = " [ ";
        for (Iterator it = set.iterator(); it.hasNext();) {
            Operand op = (Operand) it.next();

            r += op.toString() + " ";
        }
        return r + "]";
    }

    /**
     * Convert the set of Def's into a String.
     * @return the String representation of the set
     */
    private static String dataflowSetToString(DefSet dset, 
					      BitSet set) {
        if (set == null)
            return "Null";
        String r = " [ ";
	for(int i = set.nextSetBit(0); i != -1; i = set.nextSetBit(i + 1)) {
	    Def d = dset.getDef(i);
	    if (d != null) {
		r += d.toString() + " ";
	    }
        }
        return r + "]";
    }

    /**
     * Convert the set of Use's into a String.
     * @return the String representation of the set
     */
    private static String dataflowSetToString(UseSet uset, 
					      BitSet set) {
        if (set == null)
            return "Null";
        String r = " [ ";
	for(int i = set.nextSetBit(0); i != -1; i = set.nextSetBit(i + 1)) {
	    Use u = uset.getUse(i);
	    if (u != null) {
		r += u.toString() + " ";
	    }
        }
        return r + "]";
    }

    /**
     * Convert the set of Def's (or Use's) into a String.
     * @return the String representation of the set
     */
    private static String dataflowSetToString(BitSet set) {
        if (set == null)
            return "Null";
        String r = " [ ";
	for(int i = set.nextSetBit(0); i != -1; i = set.nextSetBit(i + 1)) {
	    r += i + " ";
        }
        return r + "]";
    }

    /**
     * Given a method, add appropriate RegisterTable.Entry objects 
     * for the argument of the method to the given Vector.
     * @param mg
     * @param registerTableEntries
     */
    private static void computeRegisterTableForArguments(ByteCodeGen mg,
            Vector registerTableEntries) {
        char[] argTypes = mg.getArgumentTypes();
        //if (!mg.isStatic()) { -- Static methods have a receiver in Ovm
            registerTableEntries.add(new RegisterTable.Entry(
                    TypeCodes.REFERENCE, 0));
        //}
        for (int i = 0; i < argTypes.length; i++) {
            char t = argTypes[i];
            registerTableEntries.add(new RegisterTable.Entry(
                    type2StackMapType(t), 0));
            if (typeCode2Size(t) == 2) {
                registerTableEntries.add(new RegisterTable.Entry(
                        TypeCodes.VOID, 0));
            }
        }
    }

    /**
     * @param mg
     * @return the word size of the arguments of the method
     */
    private static int computeArgWordSize(ByteCodeGen mg) {
        char[] argTypes = mg.getArgumentTypes();
        int argSize = 0;
        //if (!mg.isStatic()) { -- Static methods have a receiver in Ovm
            argSize++;
        //}
        for (int i = 0; i < argTypes.length; i++) {
            char t = toBasicType(argTypes[i]);
            argSize += typeCode2Size(t);
        }
        return argSize;
    }

    /**
     * @param mg
     * @return the number of arguments of the method
     */
    private static int computeArgNum(ByteCodeGen mg) {
        char[] argTypes = mg.getArgumentTypes();
        return /*mg.isStatic() ? argTypes.length : */ argTypes.length + 1;
    }

    /**
     * @param a
     * @param b
     * @return a ^ b (a to the power of b)
     */
    private static int power(int a, int b) {
        int p = 1;
        for (int i = 0; i < b; i++) {
            p *= a;
        }
        return p;
    }

    /**
     * @param from
     * @param to
     * @return true if "to" can be reached from "from" using the links between
     *         InstructionHandles, or from == to. false, otherwise.
     */
    private static boolean ih_precedes(InstructionHandle from,
            InstructionHandle to) {
        while (from != null) {
            if (from == to)
                return true;
            from = from.getNext();
        }
        return false;
    }

    /**
     * @param t
     * @return true if t is a 32 bit type. 
     */
    private static boolean isCategory1Type(char t) {
        return t == TypeCodes.INT || t == TypeCodes.FLOAT || t == TypeCodes.REFERENCE;
    }

    /**
     * @param t
     * @return true if t is a 64 bit type.
     */
    private static boolean isCategory2Type(char t) {
        return t == TypeCodes.LONG || t == TypeCodes.DOUBLE;
    }

    private static boolean isUpperType(char t) {
	return t == TypeCodes.LONG_UPPER || t == TypeCodes.DOUBLE_UPPER;
    }

    /**
     * Converts a Type object into either of INT, FLOAT, DOUBLE, LONG, and
     * REFERENCE.
     */
    private static char toBasicType(char type) {
        if (type == TypeCodes.REFERENCE || type == TypeCodes.OBJECT || type == TypeCodes.ARRAY)
            return TypeCodes.REFERENCE;
        else if (type == TypeCodes.BOOLEAN || type == TypeCodes.BYTE || type == TypeCodes.CHAR
                || type == TypeCodes.SHORT)
            return TypeCodes.INT;
        else {
            try {
                Assume.that(type == TypeCodes.INT || type == TypeCodes.FLOAT
                        || type == TypeCodes.DOUBLE || type == TypeCodes.LONG);
            } catch (Exception e) {
                BasicIO.out.println("type = " + type);
                throw new Error(e.toString());
            }
            return type;
        }
    }

    private static char specValue2TypeCode(SpecificationIR.Value v) {
        if (v instanceof SpecificationIR.IntValue) {
            return TypeCodes.INT;
        } else if (v instanceof SpecificationIR.FloatValue) {
            return TypeCodes.FLOAT;
        } else if (v instanceof SpecificationIR.RefValue) {
            return TypeCodes.REFERENCE;
        } else if (v instanceof SpecificationIR.LongValue) {
            return TypeCodes.LONG;
        } else if (v instanceof SpecificationIR.DoubleValue) {
            return TypeCodes.DOUBLE;
        } else
            throw new Error("Unexpected " + v);
    }
    
    /**
     * Convert the given t into a corresponding StackMap type. 
     * @param t
     * @return the corresponding StackMap type.
     */
    private static char type2StackMapType(char t) {
        char type = toBasicType(t);
        if (type == TypeCodes.REFERENCE)
            return TypeCodes.REFERENCE;
        else if (type == TypeCodes.INT)
            return TypeCodes.INT;
        else if (type == TypeCodes.LONG)
            return TypeCodes.LONG;
        else if (type == TypeCodes.FLOAT)
            return TypeCodes.FLOAT;
        else if (type == TypeCodes.DOUBLE)
            return TypeCodes.DOUBLE;
        else
            throw new Error();
    }

    /**
     * @param type
     * @return a type signature (I, J, F, D, A) of the given type.
     */
    private static String toTypeSignature(char type) {
        return Character.toString(type);
    }

    /**
     * Reverse the array.
     * @param arr
     */
    private static void reverseArray(Object[] arr) {
        Object[] tmp = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            tmp[tmp.length - i - 1] = arr[i];
        }
        for (int i = 0; i < arr.length; i++) {
            arr[i] = tmp[i];
        }
    }

    private static void reverseArray(char[] arr) {
        char[] tmp = new char[arr.length];
        for (int i = 0; i < arr.length; i++) {
            tmp[tmp.length - i - 1] = arr[i];
        }
        for (int i = 0; i < arr.length; i++) {
            arr[i] = tmp[i];
        }
    }

    /**
     * Sort an array of Web's based on their weighted reference counts 
     * using the quicksort algorithm. The result is in the descending order - 
     * webs[0] will have the web with the most reference count.
     * @param webs
     */
    private static void sortWebs(Web[] webs) {
        sortWebs(webs, 0, webs.length - 1);
    }

    // Quick sort
    private static void sortWebs(Web[] webs, int low0, int high0) {
        int low = low0;
        int high = high0;
        if (low >= high)
            return;
        else if (low == high - 1) {
            if (webs[low].getWeightedRefCount() > webs[high]
                    .getWeightedRefCount()) {
                Web w = webs[low];
                webs[low] = webs[high];
                webs[high] = w;
            }
            return;
        }
        Web pivot = webs[(low + high) / 2];
        webs[(low + high) / 2] = webs[high];
        webs[high] = pivot;
        while (low < high) {
            while (webs[low].getWeightedRefCount() <= pivot
                    .getWeightedRefCount()
                    && low < high) {
                low++;
            }
            while (pivot.getWeightedRefCount() <= webs[high]
                    .getWeightedRefCount()
                    && low < high) {
                high--;
            }
            if (low < high) {
                Web w = webs[low];
                webs[low] = webs[high];
                webs[high] = w;
            }
        }

        webs[high0] = webs[high];
        webs[high] = pivot;
        sortWebs(webs, low0, low - 1);
        sortWebs(webs, high + 1, high0);
    }

    /**
     * Sort an array of Web.Set's based on their combined weighted reference counts 
     * using the quicksort algorithm. The result is in the descending order - 
     * webSets[0] will have the web set with the most reference count.
     * @param webSets
     */
    private static void sortWebSets(Web.Set[] webSets) {
        sortWebSets(webSets, 0, webSets.length - 1);
    }

    // Quick sort
    private static void sortWebSets(Web.Set[] webSets, int low0, int high0) {
        int low = low0;
        int high = high0;
        if (low >= high)
            return;
        else if (low == high - 1) {
            if (webSets[low].getWeightedRefCount() > webSets[high]
                    .getWeightedRefCount()) {
                Web.Set w = webSets[low];
                webSets[low] = webSets[high];
                webSets[high] = w;
            }
            return;
        }
        Web.Set pivot = webSets[(low + high) / 2];
        webSets[(low + high) / 2] = webSets[high];
        webSets[high] = pivot;
        while (low < high) {
            while (webSets[low].getWeightedRefCount() <= pivot
                    .getWeightedRefCount()
                    && low < high) {
                low++;
            }
            while (pivot.getWeightedRefCount() <= webSets[high]
                    .getWeightedRefCount()
                    && low < high) {
                high--;
            }
            if (low < high) {
                Web.Set w = webSets[low];
                webSets[low] = webSets[high];
                webSets[high] = w;
            }
        }

        webSets[high0] = webSets[high];
        webSets[high] = pivot;
        sortWebSets(webSets, low0, low - 1);
        sortWebSets(webSets, high + 1, high0);
    }

    public static S3ByteCode translate(S3ByteCode bc, Method method, VM_Area compileArea) {
        Main m = new Main(bc, method, RegisterAllocator.INCREMENTAL_GRAPH_COLORING, compileArea);
        return m.translate();
    }

    public static S3ByteCode addLivenessAttribute(S3ByteCode bc, VM_Area compileArea) {
        Main m = new Main(bc, null, -1, compileArea);
        return m.addLivenessAttribute();
    }

    public static void verifyLivenessAttribute(S3ByteCode bc, VM_Area compileArea) {
        Main m = new Main(bc, null, -1, compileArea);
        m.verifyLivenessAttribute();
    }

    public static S3ByteCode performDataflowOpt(S3ByteCode bc, int maxCPIteration, VM_Area compileArea) {
        Main m = new Main(bc, null, -1, compileArea);
        return m.performDataflowOpt(maxCPIteration);
    }
    
    /**
     * The Main class which handles the commandline options and drive the translation.
     * @author yamauchi
     */
    private static class Main {

	private static final int OFFLINE_MAX_LENGTH_FOR_DATAFLOW_OPT = 2048;
	private static final int ONLINE_MAX_LENGTH_FOR_DATAFLOW_OPT = 0;

        private ByteCodeGen mg;
        
        private int registerAllocator;
        
        private int preorder;

        private int rpostorder;

	private Method method;

	private VM_Area compileArea;
	
        private Main(S3ByteCode bc, Method method, int registerAllocator, VM_Area compileArea) {
            this.mg = new ByteCodeGen2(bc, compileArea);
            this.registerAllocator = registerAllocator;
	    this.method = method;
	    this.compileArea = compileArea;
        }

	// set by firstScan()
        private boolean containsSubroutine = false;
        private boolean containsStackInstructionOrIINC = false;
        private boolean containsBranch = false;

        /**
         * Translate a method
         * @return the translated method
         */
        public S3ByteCode translate() {

	    firstScan(mg);

            /* Because LineNumber and LocalVariable are currently not correctly translated, 
             * remove them from the method. */
            mg.removeLocalVariables();
            mg.removeLineNumbers();
            //mg.getMethod();

            /* Phase 1. Subroutines free & unreachable code free. 
             * 
             * 1. Eliminate subroutines.
             * 
             * 2. Shrink ranges of the exception handlers. Let the instructions
             * that will not throw an exception be at the edges of 
             * exception handler ranges. This is necessary for minimizing the web 
             * interferences by reducing the number of exception control edges.
             * 
             * 3. Optimize the branches. This is for removing the redundant GOTO's that 
             * are the results of the subroutine elimination.
             * 
             * 4. Eliminate unreachable code (including unreachable exception handlers).
             * 
             * 5. Optimize the branches that result from unrechable code elimination.
             */
	    if (containsSubroutine)
		eliminateSubroutines(mg);
            shrinkExceptionRanges(mg);
	    if (containsBranch)
		optimizeBranches(mg);
            eliminateUnreachableCode(mg);
	    if (containsBranch)
		optimizeBranches(mg);

            /* Phase 2. Stack height 0 at BB entries */
            enforceZeroStackHeight(mg);
            //checkZeroStackHeight(mg); // TODO: this check can be optional

            /* Phase 3. Store all intermediate values into locals 
             * 
             * 1. Eliminate stack instructions (SWAP, DUP, etc) and IINC 
             * so that the next subphase will work.
             * 
             * 2. Convert into a register based code.
             */
	    if (containsStackInstructionOrIINC)
		eliminateStackInstructionsAndIINC(mg);
            storeAllIntermediateValuesOnce(mg);

            /* Phase 4. Mono-typed locals
             * 
             * 1. Locals are enforced to be mono-typed.
             * 
             * 2. Shift locals so that the locals that correspond to arguments are not
             * used for the register allocation. Insert copies for arguments at the 
             * beginning of the method.
             */
            monoTypeLocals(mg);
            shiftLocals(mg);

            /* Phase 5. Register allocation (including necesary dataflow analyses, etc)
             * 
             * 1. Constant propagation (Moving constant pushing instructions 
             * right before the instruction that uses the constant). This was needed for 
             * one big method in mpegaudio to fit within the max bytecode length.
             * 
             * 2. Register allocation.  
             */
	    if (mg.getInstructionList().getLength() < OFFLINE_MAX_LENGTH_FOR_DATAFLOW_OPT) {
		eliminateDeadAssignments(mg);
		while (propagateCopies(mg))
		    ;
	    }
            propagateConstants(mg);
            allocateRegisters(mg, registerAllocator);

            return mg.getByteCode();
        }

	/**
	 * A first scan to find if the method contains certain
	 * instructions such as subroutines, stack instructions (swap,
	 * dup, etc), IINC, and branches (goto, if, etc).
	 */
	private void firstScan(ByteCodeGen mg) {
	    new FirstScanVisitor(mg).run();
	}

	/**
	 * Used by firstScan()
	 */
	private class FirstScanVisitor extends Visitor {
	    ByteCodeGen mg;
	    InstructionList il;
	    InstructionHandle ih;
	    
	    public FirstScanVisitor(ByteCodeGen mg) {
		this.mg = mg;
		this.il = mg.getInstructionList();
	    }
	    
	    public void run() {
		ih = il.getStart();
		while (ih != null) {
		    InstructionHandle next = ih.getNext();
		    ih.accept(this);
		    ih = next;
		}
	    }
	    
	    // RET is implied by JSR
	    public void visit(JsrInstruction o) {
		containsSubroutine = true;
	    }
	    public void visit(StackManipulation o) {
		containsStackInstructionOrIINC = true;
	    }
	    public void visit(ROLL o) {
		containsStackInstructionOrIINC = true;
	    }
	    // WIDE_IINC is a subclass of IIN
	    public void visit(IINC o) {
		containsStackInstructionOrIINC = true;
	    }
	    public void visit(GotoInstruction o) {
		containsBranch = true;
	    }
	    public void visit(If o) {
		containsBranch = true;
	    }

	}

        /**
         * The register allocation phase.
         * @param mg
         * @param registerAllocator the register allocation algorithm 
         */
        private void allocateRegisters(ByteCodeGen mg, int registerAllocator) {
	    if (SimpleJIT.compileScenario == SimpleJIT.SCENARIO_ONR
		|| SimpleJIT.compileScenario == SimpleJIT.SCENARIO_ONRI) {
		SimpleJIT.startCompileTimer(method);
	    }

            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
	    BasicBlock[] basicBlockList = v.getBasicBlockList();
        
            // Loop detection
            depthFirstSearch(basicBlockList);
            detectLoops(basicBlockList);
        
            // Dataflow analyses
	    Operand.resetOpCounter();
            UseSet uset = new UseSet();
            DefSet dset = new DefSet();
            computeReachingDefinition(mg, basicBlockList, dset);
            computeLiveness(mg, basicBlockList, dset, uset);
        
            // Construct use-def and def-use chains
            computeUDAndDUChains(abstractStates, dset, uset);
        
            // Compute webs
	    Webs webs = computeWebs(dset, uset);
        
            /* A dead web is a web which consists of only one Use or Def and will not be 
             * considered to be assigned to a register.
             *
             * If it is one Use, the web consists of a use of a undefined variable 
             * (ignore this case), or of a use of an argument in the copy sequence at the 
             * beginning of the method (this case is fine because we will not assign registers 
             * for argument locals).
             *  
             * If it is one Def, the web consists of a definition of a value which is not used 
             * (ie dead code). We will not assign registers to a definition of a dead value.
             */
            IdentityHashSet deadWebs = new IdentityHashSet();
            eliminateDeadWebs(webs, deadWebs);
        
            // Compute the interferences between webs based on the liveness information
            computeWebInterferences(webs, uset, dset);
        
            // Map localIndex to a weighted ref count and a type
            Vector registerTableEntries = new Vector(); 
        
            // Assign registers and update local indices
            int initialLocalNum = computeArgWordSize(mg);
            computeRegisterTableForArguments(mg, registerTableEntries);
        
            // Run the register allocator
            RegisterAllocator rah = new RegisterAllocator(webs,
                    initialLocalNum, registerAllocator, registerTableEntries);
            rah.run();
        
            // Update the maxLocals
            int newMaxLocals = rah.getMaxLocals();
            mg.setMaxLocals(newMaxLocals);
        
            // Update local indices using the register allocation results
            updateCodeWithAllocationResults(mg, webs, deadWebs, uset, dset);
        
	    if (SimpleJIT.compileScenario == SimpleJIT.SCENARIO_ONR
		|| SimpleJIT.compileScenario == SimpleJIT.SCENARIO_ONRI) {
		SimpleJIT.stopCompileTimer(method);
	    }

            // Encode register ranking as attributes
            RegisterTable.Entry[] rtentries = 
                new RegisterTable.Entry[registerTableEntries.size()];
            registerTableEntries.toArray(rtentries);
            RegisterTable rt = RegisterTable.make(rtentries);
        
            // Add the RegisterTable attribute
            mg.addAttribute(rt);

            mg.getByteCode();
        
            // Compute and add Liveness attribute
            computeLivenessAttribute(mg);
        }

        public S3ByteCode addLivenessAttribute() {
	    eliminateUnreachableCode(mg);
	    computeLivenessAttribute(mg);
	    return mg.getByteCode();
        }

        public void verifyLivenessAttribute() {
	    verifyLivenessAttribute(mg);
        }

        public S3ByteCode performDataflowOpt(int maxCPIteration) {
	    if (mg.getInstructionList().getLength() < ONLINE_MAX_LENGTH_FOR_DATAFLOW_OPT) {
		eliminateDeadAssignments(mg);
		/*
		int i = 0;
		while (i < maxCPIteration && propagateCopies(mg)) {
		    i++;
		}
		*/
		propagateCopies(mg);
		propagateConstants(mg);
	    }
	    return mg.getByteCode();
        }
        
        /**
         * Print the basic blocks.
         * @param blocks
         * @param cp
         */
        private void printBasicBlocks(BasicBlock[] blocks, Constants cp) {
            depthFirstSearch(blocks);
            for (int i = 0; i < blocks.length; i++) {
                BasicBlock b = blocks[i];
                BasicIO.out.println("\t" + b.toDetailString());
                InstructionHandle limit = b.lastInstructionHandle.getNext();
                AbstractState as = b.firstAbstractState;
                InstructionHandle ih = b.firstInstructionHandle;
                while (ih != limit) {
                    BasicIO.out.println("\t" + ih.getPosition() + ": "
                            + ih.getInstruction().toString(cp) + "\t"
                            + as.toString());
                    ih = ih.getNext();
                    as = as.next;
                }
            }
        }

        /**
         * Print the code. This runs the abstract interpreter.
         * @param mg
         */
        private void printCode(ByteCodeGen mg) {
            mg.getByteCode();
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
            BasicBlock[] basicBlockList = v.getBasicBlockList();
        
            BasicIO.out.println(mg.toString());
            printBasicBlocks(basicBlockList, mg.getConstantPool());
        
            CodeExceptionGen[] cegs = mg.getExceptionHandlers();
            if (cegs.length > 0)
                BasicIO.out
                        .println("\tException handlers: start \t end \t handler \t type");
            for (int i = 0; i < cegs.length; i++) {
                CodeExceptionGen ceg = cegs[i];
                TypeName.Scalar type = ceg.getCatchType();
                BasicIO.out.println("\t\t\t\t" + ceg.getStartPC().getPosition()
                        + "\t" + ceg.getEndPC().getPosition() + "\t"
                        + ceg.getHandlerPC().getPosition() + "\t"
                        + (type == null ? "Any" : type.toString()));
            }
        }


	private static Liveness getLivenessAttribute(ByteCodeGen bcgen) {
		Attribute[] attrs = bcgen.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i] instanceof Liveness) {
				return (Liveness) attrs[i];
			}
		}
		return null;
	}

	private void verifyLivenessAttribute(ByteCodeGen mg) {
	    int maxLocals = mg.getMaxLocals();
	    Liveness liveness = getLivenessAttribute(mg);
	    Liveness.Entry[] livenessEntries = liveness.getEntries();
            ControlFlowAbstractInterpretationVisitor v = 
		new ControlFlowAbstractInterpretationVisitor(mg);
            v.run();
            BasicBlock[] basicBlockList = v.getBasicBlockList();
	    IdentityHashMap b2li = new IdentityHashMap();
	    int lp = 0;
	    for(int i = 0; i < basicBlockList.length; i++) {
		BasicBlock b = basicBlockList[i];
		int pc = b.startPC();
		while (livenessEntries[lp].position() != pc) {
		    lp++;
		}
		b2li.put(b, livenessEntries[lp].liveness());
	    }
	    IdentityHashMap b2lo = new IdentityHashMap();
	    for(int j = 0; j < basicBlockList.length; j++) {
		BasicBlock b = basicBlockList[j];
		boolean[] liveIn = (boolean[]) b2li.get(b);
		for(Iterator it = b.getInEdges().iterator();
		    it.hasNext(); ) {
		    BasicBlock pred = (BasicBlock) it.next();
		    boolean[] predlo = (boolean[]) b2lo.get(pred);
		    if (predlo == null) {
			predlo = new boolean[maxLocals];
			b2lo.put(pred, predlo);
		    }
		    for(int i = 0; i < maxLocals; i++) {
			predlo[i] = predlo[i] || liveIn[i];
		    }
		}
	    }
	    for(int j = 0; j < basicBlockList.length; j++) {
		BasicBlock b = basicBlockList[j];
		boolean[] liveIn = (boolean[]) b2li.get(b);
		boolean[] liveOut = (boolean[]) b2lo.get(b);
		if (liveOut == null) { // blocks that end control flow (return or throw)
		    liveOut = new boolean[maxLocals];
		}
		InstructionHandle ih = b.lastInstructionHandle;
		InstructionHandle limit = b.firstInstructionHandle.getPrev();
		while (ih != limit) {
		    Instruction inst = ih.getInstruction();
		    if (inst instanceof LocalWrite) {
			int localIndex = ((LocalWrite) inst).getLocalVariableOffset();
			liveOut[localIndex] = false;
		    } else if (inst instanceof LocalRead) {
			int localIndex = ((LocalRead) inst).getLocalVariableOffset();
			liveOut[localIndex] = true;
		    }
		    ih = ih.getPrev();
		}
		for(int i = 0; i < maxLocals; i++) {
		    if (liveIn[i] != liveOut[i]) {
			BasicIO.out.println("Liveness verification failed at PC " + b.startPC());
			BasicIO.out.println("liveIn " + boolArrayToString(liveIn));
			BasicIO.out.println("liveOut " + boolArrayToString(liveOut));
			BasicIO.out.println("Liveness " + liveness);
			BasicIO.out.println(mg.getInstructionList().toString());
			throw new Error("Liveness verification failed");
		    }
		}
	    }
	}

	private static String boolArrayToString(boolean[] b) {
	    String r = "";
	    for(int i = 0; i < b.length; i++) {
		if (b[i]) {
		    r += "1";
		} else {
		    r += "0";
		}
	    }
	    return r;
	}

        /**
         * Compute the Liveness attribute to a method. This re-runs a dataflow analyzer.
         * @param mg
         */
        private void computeLivenessAttribute(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
            BasicBlock[] basicBlockList = v.getBasicBlockList();

	    Operand.resetOpCounter();
            UseSet uset = new UseSet();
            DefSet dset = new DefSet();
            computeReachingDefinition(mg, basicBlockList, dset);
            computeLiveness(mg, basicBlockList, dset, uset);

            Vector lentries = new Vector();

            int maxLocals = mg.getMaxLocals();
	    for (int j = 0; j < basicBlockList.length; j++) {
		BasicBlock b = basicBlockList[j];
                boolean[] liveness = new boolean[maxLocals];
                InstructionHandle firstIH = b.firstInstructionHandle;
		BitSet liveInBitSet = b.liveInBitSet; // a set of Uses
		for(int i = liveInBitSet.nextSetBit(0); 
		    i != -1; 
		    i = liveInBitSet.nextSetBit(i + 1)) {
                    Use u = (Use) uset.number2Use.get(i);
                    InstructionHandle ih = u.getInstructionHandle();
                    LocalRead load = (LocalRead)ih.getInstruction();
                    liveness[load.getLocalVariableOffset()] = true;
                }
                lentries.add(new Liveness.Entry(firstIH.getPosition(),
                        liveness, maxLocals, true));
                
                // Record liveness for every instructions (except for local reads/writes) 
                // to ease code generation, these shouldn't been stored with bytecode under the offline scenario
                Vector vec = new Vector();
                InstructionHandle ih = b.lastInstructionHandle;
                for(AbstractState as = b.lastAbstractState;
                    as != b.firstAbstractState;
                    as = as.prev, ih = ih.getPrev()) {
                    Instruction inst = ih.getInstruction();
                    //if ((! (inst instanceof LocalRead))
                    //    && (! (inst instanceof LocalWrite))) {
                        boolean[] _liveness = new boolean[maxLocals];
                        BitSet _liveInBitSet = as.liveInBitSet;
			for(int i = _liveInBitSet.nextSetBit(0); 
			    i != -1; 
			    i = _liveInBitSet.nextSetBit(i + 1)) {
			    Use u = (Use) uset.number2Use.get(i);
                            InstructionHandle _ih = u.getInstructionHandle();
                            LocalRead load = (LocalRead)_ih.getInstruction();
                            _liveness[load.getLocalVariableOffset()] = true;
                        }
                        vec.add(new Liveness.Entry(ih.getPosition(),
                                _liveness, maxLocals, false));
                    //}
                }
                for(int i = vec.size() - 1; i >= 0; i--) {// reorder them in the PC order
                    lentries.add(vec.get(i));
                }
            }

            Liveness.Entry[] livenessEntries = new Liveness.Entry[lentries
                    .size()];
            lentries.toArray(livenessEntries);

            Liveness liveness = Liveness.make(livenessEntries);
            mg.addAttribute(liveness);

            mg.getByteCode();

        }

        /* Phase 5 code */

        /**
         * A very simple non-iterative constant propagation.
         * TODO fixpoint CP, apply to LDC
         */
        private void propagateConstants(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
            BasicBlock[] basicBlockList = v.getBasicBlockList();

            depthFirstSearch(basicBlockList);
            //detectLoops(basicBlockList);

	    Operand.resetOpCounter();
            UseSet uset = new UseSet();
            DefSet dset = new DefSet();
            computeReachingDefinition(mg, basicBlockList, dset);
            computeLiveness(mg, basicBlockList, dset, uset);
            computeUDAndDUChains(abstractStates, dset, uset);

            InstructionList il = mg.getInstructionList();
            /*
             * For each def D, if D's prev instruction is a constant pushing instruction (CPI) and 
             * its use has only one def (ie D), then the use can be replaced with CPI.
             * If all of D's uses are replaced, D and CPI can be removed.
             */
            for (Iterator it = dset.getDefSet().iterator(); it.hasNext();) {
                Def d = (Def) it.next();
                InstructionHandle store = d.getInstructionHandle();
                BitSet us = d.chains();
                InstructionHandle prev_store = store.getPrev();
                Assume.that(prev_store != null);
                Instruction prev_store_ins = prev_store.getInstruction();
                if (prev_store_ins instanceof ConstantLoad) {
                    boolean canRemoveDef = true;
		    for(int j = us.nextSetBit(0);
			j != -1;
			j = us.nextSetBit(j + 1)) {
                        Use u = (Use) uset.number2Use.get(j);
                        BitSet ds = u.chains();
                        if (ds.cardinality() == 1) {
                            //BasicIO.out.println("CP : PC " + store.getPosition());
                            Assume.that(ds.nextSetBit(0) == d.serialNumber);
                            InstructionHandle load = u.getInstructionHandle();
                            InstructionHandle push = il.insert(load,
                                    prev_store_ins);
                            try {
                                il.delete(load);
                            } catch (TargetLostException e) {
                                InstructionHandle[] targets = e.getTargets();
                                for (int i = 0; i < targets.length; i++) {
                                    InstructionTargeter[] targeters = targets[i]
                                            .getTargeters();
                                    for (int l = 0; l < targeters.length; l++) {
                                        targeters[l].updateTarget(targets[i],
                                                push);
                                    }
                                }
                            }
                        } else {
                            canRemoveDef = false;
                        }
                    }
                    if (canRemoveDef) {
                        //BasicIO.out.println("CP REMOVING STORE : PC " + store.getPosition());
                        InstructionHandle next_store = store.getNext();
                        Assume.that(next_store != null);
                        try {
                            il.delete(prev_store, store);
                        } catch (TargetLostException e) {
                            InstructionHandle[] targets = e.getTargets();
                            for (int i = 0; i < targets.length; i++) {
                                InstructionTargeter[] targeters = targets[i]
                                        .getTargeters();
                                for (int j = 0; j < targeters.length; j++) {
                                    targeters[j].updateTarget(targets[i],
                                            next_store);
                                }
                            }
                        }
                    }
                }
            }
            mg.getByteCode();
        }

        /**
         * A very simple non-iterative copy propagation.
         * TODO fixpoint
         */
        private boolean propagateCopies(ByteCodeGen mg) {
	    boolean changed = false;
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
	    BasicBlock[] basicBlockList = v.getBasicBlockList();

            depthFirstSearch(basicBlockList);
            //detectLoops(basicBlockList);

            UseSet uset = new UseSet();
            DefSet dset = new DefSet();
            computeReachingDefinition(mg, basicBlockList, dset);
            computeLiveness(mg, basicBlockList, dset, uset);
            computeUDAndDUChains(abstractStates, dset, uset);

            InstructionList il = mg.getInstructionList();

	    IdentityHashMap ih2as = new IdentityHashMap();
	    InstructionHandle ih = il.getStart();
	    for(int i = 0; i < abstractStates.length; i++) {
		ih2as.put(ih, abstractStates[i]);
		ih = ih.getNext();
	    }
	    Assume.that(ih == null);

	    IdentityHashSet argMapStores = new IdentityHashSet();
	    ih = il.getStart();
	    final int argCount = mg.getSelector().getDescriptor().getArgumentCount() + 1;
	    for(int i = 0; i < argCount * 2; i++) {
		if (ih.getInstruction() instanceof LocalWrite) {
		    argMapStores.add(ih);
		}
		ih = ih.getNext();
	    }

	    IdentityHashSet touchedLoads = new IdentityHashSet();
	    IdentityHashSet touchedStores = new IdentityHashSet();

            /*
             * For each def D, if D's prev instruction is a load L (LocalRead) and 
	     * a use U corresponding to D has only one def (ie, D) and the L's local index
	     * is not overwritten on the way to U from L (ie, all the defs of L are reaching 
	     * in at U), then the local index of U can be replaced with L's.
             * If all of D's uses are replaced, L and D can be removed.
             */
            for (Iterator it = dset.getDefSet().iterator(); it.hasNext();) {
                Def d = (Def) it.next();
                InstructionHandle store = d.getInstructionHandle();
		LocalWrite store_ins = (LocalWrite) store.getInstruction();
		if (touchedStores.contains(store)) {
		    continue;
		}
		touchedStores.add(store);
		if (argMapStores.contains(store)) {
		    continue; // do not copy propagate the load-store for argument copying
		}
                BitSet us = d.chains();
                InstructionHandle prev_store = store.getPrev();
                Assume.that(prev_store != null);
                Instruction prev_store_ins = prev_store.getInstruction();
                if (prev_store_ins instanceof LocalRead) {
		    LocalRead load = (LocalRead) prev_store_ins;
		    if (store_ins.getTypeCode() != load.getTypeCode()) {
			// this happens in unsafe code
			continue;
		    }
		    if (touchedLoads.contains(prev_store)) {
			continue;
		    }
		    touchedLoads.add(prev_store);
		    BitSet load_def = uset.getUse(prev_store).chains();
		    boolean shouldContinue = false;
		    for(int j = load_def.nextSetBit(0);
			j != -1;
			j = load_def.nextSetBit(j + 1)) {
                        Def ld = (Def) dset.number2Def.get(j);
			InstructionHandle ldih = ld.getInstructionHandle();
			if (touchedStores.contains(ldih)) {
			    shouldContinue = true;
			    break;
			}
			touchedStores.add(ldih);
		    }
		    if (shouldContinue)
			continue;
                    boolean canRemoveDef = true;
		    for(int j = us.nextSetBit(0);
			j != -1;
			j = us.nextSetBit(j + 1)) {
                        Use u = (Use) uset.number2Use.get(j);
			InstructionHandle use = u.getInstructionHandle();
			Assume.that(use.getInstruction() instanceof LocalRead);
			LocalRead use_ins = (LocalRead) use.getInstruction();
			if (store_ins.getTypeCode() != use_ins.getTypeCode()
			    || load.getTypeCode() != use_ins.getTypeCode()) {
			    // this happens in unsafe code (eg GC)
			    canRemoveDef = false;
			    break;
			}
			if (touchedLoads.contains(use)) {
			    canRemoveDef = false;
			    break;
			}
			touchedLoads.add(use);
			AbstractState use_as = (AbstractState) ih2as.get(use);
			BitSet reachingInBitSet = use_as.reachingInBitSet;
			boolean otherReachingDefExists = false;
			for(int k = reachingInBitSet.nextSetBit(0);
			    k != -1;
			    k = reachingInBitSet.nextSetBit(k + 1)) {
			    Def ld = (Def) dset.number2Def.get(k);
			    LocalWrite w = (LocalWrite) ld.getInstructionHandle().getInstruction();
			    if (w.getLocalVariableOffset() == load.getLocalVariableOffset()
				&& ! load_def.get(ld.serialNumber)) {
				otherReachingDefExists = true;
				break;
			    }
			}
			BitSet ds = u.chains();
                        if (ds.cardinality() == 1 
			    && ! otherReachingDefExists
			    && bitSetInclude(reachingInBitSet, load_def)) {
                            //BasicIO.out.println("CP : PC " + store.getPosition());
                            Assume.that(ds.nextSetBit(0) == d.serialNumber);
			    use.setInstruction(Instruction.LocalRead.make(use_ins.getTypeCode(),
					load.getLocalVariableOffset()));
			    changed = true;
                        } else {
                            canRemoveDef = false;
                        }
                    }
                    if (canRemoveDef) {
			changed = true;
                        //BasicIO.out.println("CP REMOVING COPY : PC " + store.getPosition());
                        InstructionHandle next_store = store.getNext();
                        Assume.that(next_store != null);
                        try {
                            il.delete(prev_store, store);
                        } catch (TargetLostException e) {
                            InstructionHandle[] targets = e.getTargets();
                            for (int i = 0; i < targets.length; i++) {
                                InstructionTargeter[] targeters = targets[i]
                                        .getTargeters();
                                for (int j = 0; j < targeters.length; j++) {
                                    targeters[j].updateTarget(targets[i],
                                            next_store);
                                }
                            }
                        }
                    }
                }
            }
            mg.getByteCode();
	    return changed;
        }

        /**
         * A very simple non-iterative dead assignment elimination.
	 * Replace LocalWrite with POP if the local is dead and the previous instruction
	 * has side effect.
	 * Remove a sequence of LocalRead, LocalWrite where the LocalWrite is dead.
         * TODO fixpoint
         */
        private void eliminateDeadAssignments(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
            BasicBlock[] basicBlockList = v.getBasicBlockList();

            depthFirstSearch(basicBlockList);
            //detectLoops(basicBlockList);

	    Operand.resetOpCounter();
            UseSet uset = new UseSet();
            DefSet dset = new DefSet();
            computeReachingDefinition(mg, basicBlockList, dset);
            computeLiveness(mg, basicBlockList, dset, uset);
            computeUDAndDUChains(abstractStates, dset, uset);

            InstructionList il = mg.getInstructionList();

	    IdentityHashMap ih2as = new IdentityHashMap();
	    InstructionHandle ih = il.getStart();
	    for(int i = 0; i < abstractStates.length; i++) {
		ih2as.put(ih, abstractStates[i]);
		ih = ih.getNext();
	    }
	    Assume.that(ih == null);

	    IdentityHashSet argMapStores = new IdentityHashSet();
	    ih = il.getStart();
	    final int argCount = mg.getSelector().getDescriptor().getArgumentCount() + 1;
	    for(int i = 0; i < argCount * 2; i++) {
		if (ih.getInstruction() instanceof LocalWrite) {
		    argMapStores.add(ih);
		}
		ih = ih.getNext();
	    }

            /*
	     * For each def D, if the local written by D is dead, replace D with a POP or POP2
	     * except for the argument copying sequence.
             */
            for (Iterator it = dset.getDefSet().iterator(); it.hasNext();) {
                Def d = (Def) it.next();
                InstructionHandle store = d.getInstructionHandle();
		LocalWrite write = (LocalWrite) store.getInstruction();
		if (argMapStores.contains(store)) {
		    continue; // do not copy propagate the load-store for argument copying
		}
                BitSet us = d.chains();
		if (us.cardinality() == 0) {
		    //BasicIO.out.println("DAE REPLACING WRITE WITH POP : PC " + store.getPosition());
                    if (write.getTypeCode() == TypeCodes.DOUBLE
			|| write.getTypeCode() == TypeCodes.LONG) {
                        store.setInstruction(POP2.make());
                    } else {
                        store.setInstruction(POP.make());
                    }
		    InstructionHandle prev_store = store.getPrev();
		    if (prev_store.getInstruction() instanceof LocalRead) {
                        //BasicIO.out.println("DAE REMOVING REDUNDANT COPY : PC " + store.getPosition());
                        InstructionHandle next_store = store.getNext();
                        Assume.that(next_store != null);
                        try {
                            il.delete(prev_store, store);
                        } catch (TargetLostException e) {
                            InstructionHandle[] targets = e.getTargets();
                            for (int i = 0; i < targets.length; i++) {
                                InstructionTargeter[] targeters = targets[i]
                                        .getTargeters();
                                for (int j = 0; j < targeters.length; j++) {
                                    targeters[j].updateTarget(targets[i],
                                            next_store);
                                }
                            }
                        }

		    }
                }
            }
            mg.getByteCode();
        }

        /**
         * Update the code (local indices) using the register allocation results
         * @param mg
         * @param webs
         * @param deadWebs
         */
        private void updateCodeWithAllocationResults(ByteCodeGen mg,
                Webs webs, IdentityHashSet deadWebs, UseSet uset, DefSet dset) {
            InstructionList il = mg.getInstructionList();
            int argNum = computeArgNum(mg);

	    int len = webs.number2Web.size();
            for (int i = 0; i < len; i++) {
                Web w = (Web) webs.number2Web.get(i);
		if (w == null)
		    continue;
                int localIndex = w.getLocalIndex();
                Assume.that(localIndex >= 0);
                DefUseBitSet du = w.getDefUses();
		BitSet d = du.defs;
		BitSet u = du.uses;
		for (int j = d.nextSetBit(0);
		     j != -1;
		     j = d.nextSetBit(j + 1)) {
		    Def def = (Def) dset.number2Def.get(j);
                    InstructionHandle ih = def.getInstructionHandle();
                    LocalAccess lvi = (LocalAccess)ih.getInstruction();
                    lvi.setLocalVariableOffset(localIndex);
                }
		for (int j = u.nextSetBit(0);
		     j != -1;
		     j = u.nextSetBit(j + 1)) {
		    Use use = (Use) uset.number2Use.get(j);
                    InstructionHandle ih = use.getInstructionHandle();
                    LocalAccess lvi = (LocalAccess)ih.getInstruction();
                    lvi.setLocalVariableOffset(localIndex);
                }
            }

            for (Iterator it = deadWebs.iterator(); it.hasNext();) {
                Web w = (Web) it.next();
                Assume.that(w.getLocalIndex() == -1);
                DefUseBitSet du = w.getDefUses();
		BitSet d = du.defs;
		BitSet u = du.uses;
                Assume.that(d.cardinality() + u.cardinality() == 1);
		int di = d.nextSetBit(0);
		int ui = u.nextSetBit(0);

                // Note: how to handle dead webs 
                // if a web is for a use or a def for the argument moves at the beginning of the code,
                //   def -> replace it with pop or pop2
                //   use -> keep it (but if the result is used by a pop, delete the use and the pop)
                // else
                //   def -> replace it with pop or pop2
                //   use -> shouldn't happen !

		if (di != -1) {
		    Def def = (Def) dset.number2Def.get(di);
		    InstructionHandle ih = def.getInstructionHandle();
		    LocalWrite lvi = (LocalWrite) ih.getInstruction();
                    InstructionHandle pop = null;
                    if (typeCode2Size(specValue2TypeCode(lvi.stackIns[0])) == 2) {
                        pop = il.insert(ih, POP2.make());
                    } else {
                        pop = il.insert(ih, POP.make());
                    }
                    try {
                        il.delete(ih);
                    } catch (TargetLostException e) {
                        InstructionHandle[] targets = e.getTargets();
                        for (int i = 0; i < targets.length; i++) {
                            InstructionTargeter[] targeters = targets[i]
                                    .getTargeters();
                            for (int j = 0; j < targeters.length; j++) {
                                targeters[j].updateTarget(targets[i], pop);
                            }
                        }
                    }
		} else if (ui != -1) {
		    Use use = (Use) uset.number2Use.get(ui);
		    InstructionHandle ih = use.getInstructionHandle();
		    LocalRead lvi = (LocalRead) ih.getInstruction();
                    int numPrevInsts = computeArgNum(mg) * 2;
                    InstructionHandle[] ils = il.getInstructionHandles();
                    boolean loadForArgumentCopying = false;
                    for (int i = 0; i < numPrevInsts; i++) {
                        if (ih == ils[i])
                            loadForArgumentCopying = true;
                    }
                    if (!loadForArgumentCopying) {
                        BasicIO.err
                                .println("found a load not for arg copying at PC "
                                        + ih.getPosition());
                    }
                    Assume.that(loadForArgumentCopying);
		} else throw new Error();

            }

            mg.getByteCode();
        }

        /**
         * Some branch optimizations.
         * 
         * 1. A GOTO to a GOTO can be replaced by a branch to the latter. 
         * 
         * 2. A GOTO to the next instruction can be removed.
         * 
         * 3. A conditional branch to a GOTO can be replaced by a corresponding
         * conditional branch to the GOTO's target.
         * 
         * @param mg
         */
        private void optimizeBranches(ByteCodeGen mg) {
            boolean changed = true;
            while (changed) {
                changed = false;
                InstructionList il = mg.getInstructionList();
                for (InstructionHandle ih = il.getStart(); ih != null; ih = ih
                        .getNext()) {
                    Instruction ins = ih.getInstruction();
                    if (ins instanceof GotoInstruction) {
                        GotoInstruction gotoIns = (GotoInstruction) ins;
                        InstructionHandle target = gotoIns.getTargetHandle();
                        Instruction targetIns = target.getInstruction();
                        InstructionHandle next = ih.getNext();
                        if (next != null && target == next) { // goto to the next
                            // instruction
                            changed = true;
                            InstructionTargeter[] targeters = ih.getTargeters();
                            if (targeters != null) {
                                for (int j = 0; j < targeters.length; j++) {
                                    InstructionTargeter it = targeters[j];
                                    if (it instanceof CodeExceptionGen) {
                                        InstructionHandle prev = ih.getPrev();
                                        CodeExceptionGen ceg = (CodeExceptionGen) it;
                                        InstructionHandle start = ceg
                                                .getStartPC();
                                        InstructionHandle end = ceg.getEndPC();
                                        InstructionHandle handler = ceg
                                                .getHandlerPC();
                                        if (ih == handler) {
                                            it.updateTarget(ih, next);
                                        } else if (ih == start) {
                                            if (start == end) // range of length
                                                              // 1
                                                mg
                                                        .removeExceptionHandler((CodeExceptionGen) it);
                                            else
                                                it.updateTarget(ih, next);
                                        } else if (ih == end) {
                                            if (start == end) // range of length
                                                              // 1
                                                mg
                                                        .removeExceptionHandler((CodeExceptionGen) it);
                                            else {
                                                if (prev != null) {
                                                    it.updateTarget(ih, prev);
                                                } else {
                                                    mg
                                                            .removeExceptionHandler((CodeExceptionGen) it);
                                                }
                                            }
                                        } else {
                                            BasicIO.err.println("ih = " + ih);
                                            BasicIO.err.println("start = "
                                                    + start);
                                            BasicIO.err.println("end = " + end);
                                            BasicIO.err.println("handler = "
                                                    + handler);
                                            throw new Error();
                                        }
                                    } else if (it instanceof BranchInstruction) {
                                        it.updateTarget(ih, next);

//                                    } else if (it instanceof LocalVariableGen) {
//                                        mg
//                                                .removeLocalVariable((LocalVariableGen) it);
//                                    } else if (it instanceof LineNumberGen) {
//                                        mg.removeLineNumber((LineNumberGen) it);

                                    } else
                                        throw new Error();
                                }
                            }
                            try {
                                il.delete(ih);
                            } catch (TargetLostException e) {
                            }
                        } else if (targetIns instanceof GotoInstruction) {
                            changed = true;
                            GotoInstruction gotoIns2 = (GotoInstruction) targetIns;
                            InstructionHandle target2 = gotoIns2.getTargetHandle();
                            gotoIns.setTargetHandle(target2);
                        }
                    } else if (ins instanceof If) {
                        If ifIns = (If) ins;
                        InstructionHandle target = ifIns.getTargetHandle();
                        Instruction targetIns = target.getInstruction();
                        if (targetIns instanceof GotoInstruction) {
                            changed = true;
                            GotoInstruction gotoIns2 = (GotoInstruction) targetIns;
                            InstructionHandle target2 = gotoIns2.getTargetHandle();
                            ifIns.setTargetHandle(target2);
                        }
                    }
                }
                mg.getByteCode();
		//il.setPositions();
            }
        }

        
        private static final boolean NARROW_EXCEPTION_RANGES_DEBUG_PRINT = false;

        /**
         * Shrink the ranges of exception handlers so that the first instruction and
         * the last instruction in each range are an exception thrower.
         * 
         * @param mg
         */
        private void shrinkExceptionRanges(ByteCodeGen mg) {
            CodeExceptionGen[] cegs = mg.getExceptionHandlers();
            InstructionList il = mg.getInstructionList();
            for (int i = 0; i < cegs.length; i++) {
                CodeExceptionGen ceg = cegs[i];
                if (NARROW_EXCEPTION_RANGES_DEBUG_PRINT)
                    BasicIO.err.println("Checking handler " + ceg);
                InstructionHandle start = ceg.getStartPC();
                InstructionHandle end = ceg.getEndPC();

                Assume.that(start != null && end != null && il.contains(start) && il.contains(end));

                while ((!(start.getInstruction() instanceof ExceptionThrower) || ((start
                        .getInstruction() instanceof ExceptionThrower) && ((ExceptionThrower) start
                        .getInstruction()).getThrowables().length == 0))
                        && start != end) {
                    start = start.getNext();
                }
                ceg.setStartPC(start);

                while ((!(end.getInstruction() instanceof ExceptionThrower) || ((end
                        .getInstruction() instanceof ExceptionThrower) && ((ExceptionThrower) end
                        .getInstruction()).getThrowables().length == 0))
                        && end != start) {
                    end = end.getPrev();
                }
                ceg.setEndPC(end);
            }
            mg.getByteCode();
	    //il.setPositions();
        }

        
        private static final boolean ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT = false;

        /**
         * Remove unreachable code.
         * @param mg
         */
        private void eliminateUnreachableCode(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
            BasicBlock[] basicBlockList = v.getBasicBlockList();

            depthFirstSearch(basicBlockList);
            InstructionList il = mg.getInstructionList();
            
            int i = 0;
            for (InstructionHandle ih = il.getStart(); ih != null;) {
                InstructionHandle prev = ih.getPrev();
                InstructionHandle next = ih.getNext();
                if (abstractStates[i] == null) {
                    InstructionHandle first = ih, last = ih;
                    InstructionTargeter[] targeters = ih.getTargeters();
                    if (targeters != null) {
                        for (int k = 0; k < targeters.length; k++) {
                            InstructionTargeter it = targeters[k];
                            if (it instanceof CodeExceptionGen) {
                                CodeExceptionGen ceg = (CodeExceptionGen) it;
                                InstructionHandle start = ceg.getStartPC();
                                InstructionHandle end = ceg.getEndPC();
                                InstructionHandle handler = ceg.getHandlerPC();
                                if (first == handler) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("first == handler : Removing "
                                                        + ceg);
                                    // this is the beginning of a handler
                                    mg.removeExceptionHandler(ceg);
                                } else if (ih_precedes(first, start)
                                        && ih_precedes(end, last)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("first <= start && end <= last : Removing "
                                                        + ceg);
                                    // the range is with this block
                                    mg.removeExceptionHandler(ceg);
                                } else if (ih_precedes(first, start)
                                        && ih_precedes(last, end)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("first <= start && last <= end : Updating target to "
                                                        + next);
                                    // the beginning of the range overlaps with
                                    // this block
                                    Assume.that(next != null);
                                    it.updateTarget(ih, next);
                                } else if (ih_precedes(start, first)
                                        && ih_precedes(end, last)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("start <= first && end <= last : Updating target to "
                                                        + prev);
                                    // the end of the range overlaps with this
                                    // block
                                    it.updateTarget(ih, prev);
                                } else if (ih_precedes(start, first)
                                        && ih_precedes(last, end)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("start <= first && last <= end : Doing nothing");
                                    // the range includes this block, do nothing
                                } else {
                                    BasicIO.out.println("start = "
                                            + start.getPosition());
                                    BasicIO.out.println("end = "
                                            + end.getPosition());
                                    BasicIO.out.println("first = "
                                            + first.getPosition());
                                    BasicIO.out.println("last = "
                                            + last.getPosition());
                                    throw new Error();
                                }
                            } else if (it instanceof BranchInstruction) {
                                // Should only be the branches within the
                                // unreachable code
                                it.updateTarget(ih, null);
                                /*
                                 * } else if (it instanceof LocalVariableGen) {
                                 * mg.removeLocalVariable((LocalVariableGen)
                                 * it); } else if (it instanceof LineNumberGen) {
                                 * mg.removeLineNumber((LineNumberGen) it);
                                 */
                            } else
                                throw new Error(it.toString());
                        }
                    }

                    try {
                        il.delete(ih);
                    } catch (TargetLostException e) {
                    }

                }
                ih = next;
                i++;
            }

/*
 * mg.getByteCode(); v = new AbstractInterpretationVisitor(mg); v.run();
 * abstractStates = v.getAbstractStates(); basicBlocks = v.getBasicBlocks();
 * 
 * depthFirstSearch(basicBlocks);
 */
            for (int k = 0; k < basicBlockList.length; k++) {
		BasicBlock b = basicBlockList[k];
                if (!b.isReachable()) {
                    //BasicIO.out.println("Removing unreachable " + b);
                    InstructionHandle first = b.firstInstructionHandle;
                    InstructionHandle last = b.lastInstructionHandle;
                    InstructionHandle prev = first.getPrev();
                    InstructionHandle next = last.getNext();
                    Assume.that(prev != null);

                    for (InstructionHandle ih = first; ih != last.getNext(); ih = ih
                            .getNext()) {
                        InstructionTargeter[] targeters = ih.getTargeters();
                        if (targeters == null)
                            continue;
                        for (int j = 0; j < targeters.length; j++) {
                            InstructionTargeter it = targeters[j];
                            if (it instanceof CodeExceptionGen) {
                                CodeExceptionGen ceg = (CodeExceptionGen) it;
                                InstructionHandle start = ceg.getStartPC();
                                InstructionHandle end = ceg.getEndPC();
                                InstructionHandle handler = ceg.getHandlerPC();
                                if (first == handler) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("first == handler : Removing "
                                                        + ceg);
                                    // this is the beginning of a handler
                                    mg.removeExceptionHandler(ceg);
                                } else if (ih_precedes(first, start)
                                        && ih_precedes(end, last)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("first <= start && end <= last : Removing "
                                                        + ceg);
                                    // the range is with this block
                                    mg.removeExceptionHandler(ceg);
                                } else if (ih_precedes(first, start)
                                        && ih_precedes(last, end)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("first <= start && last <= end : Updating target to "
                                                        + next);
                                    // the beginning of the range overlaps with this block 
                                    Assume.that(next != null);
                                    it.updateTarget(ih, next);
                                } else if (ih_precedes(start, first)
                                        && ih_precedes(end, last)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("start <= first && end <= last : Updating target to "
                                                        + prev);
                                    // the end of the range overlaps with this block
                                    it.updateTarget(ih, prev);
                                } else if (ih_precedes(start, first)
                                        && ih_precedes(last, end)) {
                                    if (ELIMINATE_UNREACHABLE_CODE_DEBUG_PRINT)
                                        BasicIO.err
                                                .println("start <= first && last <= end : Doing nothing");
                                    // the range includes this block, do nothing
                                } else {
                                    BasicIO.out.println("start = "
                                            + start.getPosition());
                                    BasicIO.out.println("end = "
                                            + end.getPosition());
                                    BasicIO.out.println("first = "
                                            + first.getPosition());
                                    BasicIO.out.println("last = "
                                            + last.getPosition());
                                    throw new Error();
                                }
                            } else if (it instanceof BranchInstruction) {
                                // Should only be the branches within the unreachable code
                                it.updateTarget(ih, null);
                                /*
                            } else if (it instanceof LocalVariableGen) {
                                mg.removeLocalVariable((LocalVariableGen) it);
                            } else if (it instanceof LineNumberGen) {
                                mg.removeLineNumber((LineNumberGen) it);
                                */
                            } else
                                throw new Error(it.toString());
                        }
                    }

                    try {
                        il.delete(first, last);
                    } catch (TargetLostException e) {
                        //Assume.that(targets.length == 0);
                    }
                }
            }
            mg.getByteCode();
	    //il.setPositions();
        }

        
        
        private static final boolean ELIMINATE_DEAD_WEBS_DEBUG_PRINT = false;

        /**
         * Remove "dead" webs (which are defined to have only one Def) from the web set.
         */
        private void eliminateDeadWebs(Webs webs, IdentityHashSet deadWebs) {
	    int len = webs.number2Web.size();
            for (int i = 0; i < len; i++) {
                Web w = (Web) webs.number2Web.get(i);
		if (w == null)
		    continue;
                DefUseBitSet defUses = w.defUses;
		int nuse = defUses.uses.cardinality();
		int ndef = defUses.defs.cardinality();
                Assume.that(nuse + ndef > 0);
                if (ndef + nuse == 1) {
		    // o usually should be a Def (a Def without any Uses)
		    // o could be a Use in a unreachable code (eg unreachable exception handlers)
                    deadWebs.add(w);
                }
            }
            if (ELIMINATE_DEAD_WEBS_DEBUG_PRINT) {
                BasicIO.out.println("Dead webs:");
		for(Iterator it = deadWebs.iterator(); it.hasNext(); ) {
                    Web w = (Web) it.next();
                    BasicIO.out.println(w);
                }
            }

	    for(Iterator it = deadWebs.iterator(); it.hasNext(); ) {
		Web w = (Web) it.next();
		webs.removeWeb(w.serialNumber);
	    }
        }

        
        private static final boolean COMPUTE_WEB_INTERFERENCES_DEBUG_PRINT = false;     
        /**
         * Compute the interferences between webs  based on the interference relations between Uses and Defs in computeUDAndDUChains(). 
         * @param webs  the set of webs
         */
        private void computeWebInterferences(Webs webs, UseSet uset, DefSet dset) {
	    int len = webs.number2Web.size();
	    for(int i = 0; i < len; i++) {
		Web w = (Web) webs.number2Web.get(i);
		if (w == null)
		    continue;
		BitSet defs = w.defUses.defs;
		BitSet uses = w.defUses.uses;
		for(int j = defs.nextSetBit(0);
		    j != -1;
		    j = defs.nextSetBit(j + 1)) {
		    Def d = (Def) dset.number2Def.get(j);
		    for(int k = d.interferences().nextSetBit(0);
			k != -1;
			k = d.interferences().nextSetBit(k + 1)) {
			Use u = (Use) uset.number2Use.get(k);
                        Web iw = u.getWeb();
                        Assume.that(iw != null && w != iw);
                        w.interferences.set(iw.serialNumber);
                        iw.interferences.set(w.serialNumber);
                    }
                }
		for(int j = uses.nextSetBit(0);
		    j != -1;
		    j = uses.nextSetBit(j + 1)) {
		    Use u = (Use) uset.number2Use.get(j);
		    for(int k = u.interferences().nextSetBit(0);
			k != -1;
			k = u.interferences().nextSetBit(k + 1)) {
			Def d = (Def) dset.number2Def.get(k);
                        Web iw = d.getWeb();
                        Assume.that(iw != null && w != iw);
                        w.interferences.set(iw.serialNumber);
                        iw.interferences.set(w.serialNumber);
                    }
                }
            }

            if (COMPUTE_WEB_INTERFERENCES_DEBUG_PRINT) {
                BasicIO.out.println("Web interferences:");
		int len0 = webs.number2Web.size();
		for (int i = 0; i < len0; i++) {
		    Web w = (Web) webs.number2Web.get(i);
		    if (w != null) {
			BasicIO.out.println(w + " : " + w.getWeightedRefCount()
					   + " " + w.interferences);
		    }
                }
            }
        }

        /**
         * A helper for computeWebs()
         */
        private void computeWebsLoop(boolean processUse,
				     Webs webs,
				     BitSet useList,
				     BitSet defList,
				     UseSet uset, 
				     DefSet dset) {
	    BitSet workList = processUse ? useList : defList;
	    for(int i = workList.nextSetBit(0);
		i != -1;
		i = workList.nextSetBit(i + 1)) {
                Operand o = processUse ? 
		    (Operand) uset.number2Use.get(i)
		    : (Operand) dset.number2Def.get(i);
                if (o == null || o.getWeb() != null)
                    continue;
                if (COMPUTE_WEBS_DEBUG_PRINT)
                    BasicIO.out.println(o + " : ");
                Web w = webs.makeWeb(o.getType());
                DefUseBitSet closure = computeDefUseClosure(o, uset, dset);
                if (COMPUTE_WEBS_DEBUG_PRINT)
                    BasicIO.out.println("CLOSURE : " + closure);
                w.setDefUses(closure);
                int weightedRefCount = 0;
		for(int j = closure.uses.nextSetBit(0);
		    j != -1;
		    j = closure.uses.nextSetBit(j + 1)) {
		    Use u = (Use) uset.number2Use.get(j);
		    useList.clear(u.serialNumber);
                    if (COMPUTE_WEBS_DEBUG_PRINT)
                        BasicIO.out.println("Assiging " + w + " to " + u);
                    weightedRefCount += u.isReachable() ? 1 * power(10, u
                            .getLoopDepth()) : 0;
                    Assume.that(u.getWeb() == null);
                    u.setWeb(w);
                }
		for(int j = closure.defs.nextSetBit(0);
		    j != -1;
		    j = closure.defs.nextSetBit(j + 1)) {
		    Def d = (Def) dset.number2Def.get(j);
		    defList.clear(d.serialNumber);
                    if (COMPUTE_WEBS_DEBUG_PRINT)
                        BasicIO.out.println("Assiging " + w + " to " + d);
                    weightedRefCount += d.isReachable() ? 1 * power(10, d
                            .getLoopDepth()) : 0;
                    Assume.that(d.getWeb() == null);
                    d.setWeb(w);
                }
                w.setRefCount(closure.uses.cardinality()
			      + closure.defs.cardinality());
                w.setWeightedRefCount(weightedRefCount);
            }
        }

        private static final boolean COMPUTE_WEBS_DEBUG_PRINT = false;

        /**
         * Compute webs based on the computed use-def and def-use chains.
         * @return  the web set
         */
        private Webs computeWebs(DefSet dset, UseSet uset) {
            if (COMPUTE_WEBS_DEBUG_PRINT) {
                BasicIO.out.println("uses : " + uset.uses);
                BasicIO.out.println("defs : " + dset.defs);
            }

	    Webs webs = new Webs();

	    BitSet uworkList = new BitSet();
	    BitSet dworkList = new BitSet();
	    int ulen = uset.number2Use.size();
	    int dlen = dset.number2Def.size();
	    for(int i = 0; i < ulen; i++) {
		if (uset.number2Use.get(i) != null)
		    uworkList.set(i);
	    }
	    for(int i = 0; i < dlen; i++) {
		if (dset.number2Def.get(i) != null)
		    dworkList.set(i);
	    }
	    computeWebsLoop(true, webs, uworkList, dworkList, uset, dset);
	    computeWebsLoop(false, webs, uworkList, dworkList, uset, dset);

            if (COMPUTE_WEBS_DEBUG_PRINT) {
                BasicIO.out.println("Web assignments:");
		int len = webs.number2Web.size();
                for (int i = 0; i < len; i++) {
                    Web w = (Web) webs.number2Web.get(i);
		    if (w != null)
			BasicIO.out.println(w + " : " + w.getDefUses());
                }
            }
            return webs;
        }

        /**
         * Compute the transitive closure of def-use and use-def chains including o.
         * @param o
         * @return the closure
         */
        private DefUseBitSet computeDefUseClosure(Operand o, UseSet uset, DefSet dset) {
	    BitSet uclosure = new BitSet();
	    BitSet dclosure = new BitSet();
	    BitSet uworkset = new BitSet();
	    BitSet dworkset = new BitSet();
	    if (o instanceof Use) {
		uworkset.set(o.serialNumber);
		uclosure.set(o.serialNumber);
	    } else if (o instanceof Def) {
		dworkset.set(o.serialNumber);
		dclosure.set(o.serialNumber);
	    } else throw new Error();
            while (uworkset.nextSetBit(0) != -1
		   || dworkset.nextSetBit(0) != -1) {
		int ui = uworkset.nextSetBit(0);
		int di = dworkset.nextSetBit(0);
		if (ui != -1) {
		    uworkset.clear(ui);
		    BitSet chains = ((Operand) uset.number2Use.get(ui)).chains();
		    // if there is at least one def in chains that is not already in dclosure
		    if (! bitSetInclude(dclosure, chains)) {
			dworkset.or(chains);
			dclosure.or(chains);
		    }
		} else if (di != -1) {
		    dworkset.clear(di);
		    BitSet chains = ((Operand) dset.number2Def.get(di)).chains();
		    // if there is at least one use in chains that is not already in uclosure
		    if (! bitSetInclude(uclosure, chains)) {
			uworkset.or(chains);
			uclosure.or(chains);
		    }
		} else throw new Error();
            }
	    DefUseBitSet defUseBitSet = new DefUseBitSet(dclosure, uclosure);
            return defUseBitSet;
        }

        /**
         * Compute use-def and def-use chains based on the reaching definition and liveness information.
         * Fill Def.interferences() and Use.interferences().
         */
        private void computeUDAndDUChains(AbstractState[] abstractStates,
                DefSet dset, UseSet uset) {
            for (int i = 0; i < abstractStates.length; i++) {
                AbstractState as = abstractStates[i];
                if (as == null)
                    continue;
                InstructionHandle ih = as.instructionHandle;
                Instruction inst = ih.getInstruction();
                if (inst instanceof LocalRead) {
                    Use use = uset.getUse(ih);
                    LocalRead load = (LocalRead) inst;
		    BitSet reachingInBitSet = as.reachingInBitSet;
		    for(int j = reachingInBitSet.nextSetBit(0);
			j != -1;
			j = reachingInBitSet.nextSetBit(j + 1)) {
                        Def def = (Def) dset.number2Def.get(j);
                        LocalWrite store = (LocalWrite) def.ih
                                .getInstruction();
                        if (load.getLocalVariableOffset() == store.getLocalVariableOffset()) {
                            Assume.that(load.getTypeCode() == store.getTypeCode());
                            use.defs().set(j);
                            def.uses().set(use.serialNumber); // to establish the def-use chains of ParamInstructionHandles
                        }
                    }
                } else if (inst instanceof LocalWrite) {
                    Def def = dset.getDef(ih);
                    LocalWrite store = (LocalWrite) inst;
                    BitSet liveOutBitSet = as.liveOutBitSet;
		    for(int j = liveOutBitSet.nextSetBit(0);
			j != -1;
			j = liveOutBitSet.nextSetBit(j + 1)) {
                        Use use = (Use) uset.number2Use.get(j);
                        LocalRead load = (LocalRead) use.ih
                                .getInstruction();
                        if (load.getLocalVariableOffset() == store.getLocalVariableOffset()) {
                            Assume.that(load.getTypeCode() == store.getTypeCode());
                            def.uses().set(j);
                            use.defs().set(def.serialNumber); // probably unnecessary
                        } else {
                            def.interferences().set(use.serialNumber);
                            use.interferences().set(def.serialNumber);
                        }
                    }
                }
            }

        }

        private static final boolean COMPUTE_LIVENESS_DEBUG_PRINT = false;

        /**
         * Compute liveness
         */
        private void computeLiveness(ByteCodeGen mg,
                BasicBlock[] basicBlockList, DefSet dset, UseSet uset) {

            BlockWorkSet workset = new BlockWorkSet(basicBlockList.length);
	    //IdentityHashSet workset = new IdentityHashSet();
	    for (int i = 0; i < basicBlockList.length; i++) {
		BasicBlock b = basicBlockList[i];
                b.resetLiveInOutSets();
		//workset.add(b);
                workset.insert(b);

		// Create all Uses now
                InstructionHandle ih = b.firstInstructionHandle;
                AbstractState as = b.firstAbstractState;
                while (ih != b.lastInstructionHandle.getNext()) {
		    Instruction ins = ih.getInstruction();
                    if (ins instanceof LocalRead) {
                        Use use = uset.makeUse(ih,
					       b.enclosingLoopEntries.size(), 
					       b.isReachable());
                    }
                    ih = ih.getNext();
                    as = as.next;
                }

            }

            while (!workset.empty()) {
		//		BasicIO.out.println(mg + " : [ " + workset.toString() + "]");
		BasicBlock b = workset.removeLast();
                //BasicBlock b = (BasicBlock) workset.remove();

                BitSet oldIn = (BitSet) b.liveInBitSet.clone();
                BitSet in = null;
                BitSet out = null;
                BitSet gen = null;

                // Assume that there is at least one instruction in every block
                Assume.that(b.firstInstructionHandle != null
                        && b.lastInstructionHandle != null);

                /* For each instruction,
                 * 
                 * OLD_IN = IN
                 * 
                 * OUT = UNION(IN[s]) where s is a successor of this instruction
                 * 
                 * INT = UNION(GEN, OUT - KILL)
                 * 
                 * if OLD_IN != IN then add the predecessors to the workset
                 */
                InstructionHandle ih = b.lastInstructionHandle;
                AbstractState as = b.lastAbstractState;
                InstructionHandle end = b.firstInstructionHandle.getPrev();
                while (ih != end) {
                    BitSet uses = new BitSet();
                    if (ih.getInstruction() instanceof LocalRead) {
                        Use u = uset.getUse(ih);
                        uses.set(u.serialNumber);
                    }

                    BitSet defs = new BitSet();
                    if (ih.getInstruction() instanceof LocalWrite) {
                        Def d = dset.getDef(ih);
                        defs.set(d.serialNumber);
                    }

                    if (ih == b.lastInstructionHandle) { // Last instruction
                        out = new BitSet();
                        for (Iterator it = b.getOutEdges().iterator(); it
                                .hasNext();) {
                            BasicBlock succ = (BasicBlock) it.next();
                            out.or(succ.liveInBitSet);
                        }
                        b.liveOutBitSet.or(out);
                        as.liveOutBitSet.or(out);
                    } else { // otherwise
                        out = as.next.liveInBitSet;
                        as.liveOutBitSet.or(out);
                    }
                    // GEN  : uses from this inst
                    gen = (BitSet) uses.clone();
                    // IN = union(GEN, OUT - KILL)
                    in = new BitSet();
                    in.or(gen);

                    BitSet copyOut = (BitSet) out.clone();
		    for (int i1 = copyOut.nextSetBit(0); i1 != -1; i1 = copyOut.nextSetBit(i1 + 1)) {
                        Use oneOfOut = (Use) uset.number2Use.get(i1);
                        boolean to_be_removed = false;
			for (int i2 = defs.nextSetBit(0); i2 != -1; i2 = defs.nextSetBit(i2 + 1)) {
                            Def d = (Def) dset.number2Def.get(i2);
			    if (oneOfOut.localIndex == d.localIndex) {
                                to_be_removed = true;
                            }
                        }
                        if (to_be_removed)
			    copyOut.clear(i1); // remove oneOfOut from copyOut
                    }
                    in.or(copyOut);
                    as.liveInBitSet.or(in);
                    if (ih == b.firstInstructionHandle) {
                        b.liveInBitSet.or(in);
                    }

                    ih = ih.getPrev();
                    as = as.prev;
                }

                // if oldIn != newIn, add predecessors to the work list
                if (!oldIn.equals(in)) {
		    for(Iterator it = b.getInEdges().iterator(); it.hasNext();) {
		    	workset.insert((BasicBlock) it.next());
		    }
		    //workset.union(b.getInEdges());
                }
            }

	    // TODO: this verification can be optional
            // Verify that the liveIn at the first BB only contains parameters
            char[] argTypes = mg.getArgumentTypes();
            AbstractState ias = basicBlockList[0].firstAbstractState;

            Assume.that(ias.liveInBitSet.equals(basicBlockList[0].liveInBitSet));
            Assume.that(ias.liveInBitSet != basicBlockList[0].liveInBitSet);

            BitSet toRemoved = new BitSet();
            int localSlotIndex = 0;
            //if (!mg.isStatic()) { -- Static methods have a receiver in Ovm
	    for(int i = ias.liveInBitSet.nextSetBit(0);
		i != -1;
		i = ias.liveInBitSet.nextSetBit(i + 1)) {
		Use u = (Use) uset.number2Use.get(i);
		LocalAccess lvi = (LocalAccess) u.ih.getInstruction();
		if (lvi.getLocalVariableOffset() == localSlotIndex
		    && lvi.getTypeCode() == TypeCodes.REFERENCE) {
		    toRemoved.set(i);
		}
	    }
	    localSlotIndex++;
            //}

            for (int i = 0; i < argTypes.length; i++) {
                char t = toBasicType(argTypes[i]);
		for(int j = ias.liveInBitSet.nextSetBit(0);
		    j != -1;
		    j = ias.liveInBitSet.nextSetBit(j + 1)) {
		    //		    for (Iterator it = ias.liveIn.iterator(); it.hasNext();) {
                    Use u = (Use) uset.number2Use.get(j);
                    LocalAccess lvi = (LocalAccess) u.ih.getInstruction();
                    if (lvi.getLocalVariableOffset() == localSlotIndex
                            && lvi.getTypeCode() == t) {
                        toRemoved.set(j);
                    }
                }
                localSlotIndex += typeCode2Size(t);
            }

            Assume.that(ias.liveInBitSet.equals(toRemoved));
            Assume.that(basicBlockList[0].liveInBitSet.equals(toRemoved));
        }

	/**
	 * A set of BasicBlocks indexed by the reverse postorders
	 * used for dataflow analyses.
	 */
	private static class BlockWorkSet {
	    private BasicBlock[] internal;
	    private IdentityHashSet unreachables;
	    BlockWorkSet(int nblocks) {
		internal = new BasicBlock[nblocks + 1];
		unreachables = new IdentityHashSet();
	    }
	    BasicBlock removeFirst() {
		for(int i = 0; i < internal.length; i++) {
		    BasicBlock f = internal[i];
		    if (f != null) {
			internal[i] = null;
			return f;
		    }
		}
		return (BasicBlock) unreachables.remove();
	    }
	    BasicBlock removeLast() {
		if (! unreachables.empty())
		    return (BasicBlock) unreachables.remove();
		for(int i = internal.length - 1; i >= 0; i--) {
		    BasicBlock l = internal[i];
		    if (l != null) {
			internal[i] = null;
			return l;
		    }
		}
		return null;
	    }
	    void insert(BasicBlock b) {
		if (b.getReversePostOrder() == 0) {
		    unreachables.add(b);
		    return;
		}
		internal[b.getReversePostOrder()] = b;
	    }
	    boolean empty() {
		for(int i = 0; i < internal.length; i++) {
		    if (internal[i] != null) {
			return false;
		    }
		}
		return unreachables.empty();
	    }
	    public String toString() {
		String r = "";
		for(int i = 0; i < internal.length; i++) {
		    if (internal[i] != null) {
			BasicBlock b = internal[i];
			r +=  b + "(" + b.getReversePostOrder() + ") ";
		    }
		}
		r += " unreachables: ";
		for(Iterator it = unreachables.iterator(); it.hasNext(); ) {
		    BasicBlock b = (BasicBlock) it.next();
		    r +=  b + "(" + b.getReversePostOrder() + ") ";
		}
		return r;
	    }
	}

        /**
         * Compute reaching definition
         */
        private static final boolean COMPUTE_REACHING_DEFINITION_DEBUG_PRINT = false;

        private void computeReachingDefinition(ByteCodeGen mg,
                BasicBlock[] basicBlockList, DefSet dset) {
	    BlockWorkSet workset = new BlockWorkSet(basicBlockList.length);
	    //IdentityHashSet workset = new IdentityHashSet();

	    for (int i = 0; i < basicBlockList.length; i++) {
		BasicBlock b = basicBlockList[i];
                b.resetReachingInOutSets();
		workset.insert(b);
		//workset.add(b);

		// Create all Defs now
                InstructionHandle ih = b.firstInstructionHandle;
                AbstractState as = b.firstAbstractState;
                while (ih != b.lastInstructionHandle.getNext()) {
		    Instruction ins = ih.getInstruction();
                    if (ins instanceof LocalWrite) {
                        Def def = dset.makeDef(ih,
					       b.enclosingLoopEntries.size(), 
					       b.isReachable());
                    }
                    ih = ih.getNext();
                    as = as.next;
                }
            }

            while (!workset.empty()) {
		BasicBlock b = workset.removeFirst();
                //BasicBlock b = (BasicBlock) workset.remove();
                if (COMPUTE_REACHING_DEFINITION_DEBUG_PRINT)
                    BasicIO.out.println("Processing " + b);

                BitSet oldOut = (BitSet) b.reachingOutBitSet.clone();
                BitSet in = null;
                BitSet out = null;
                BitSet gen = null;

                if (COMPUTE_REACHING_DEFINITION_DEBUG_PRINT)
                    BasicIO.out.println("original out "
                            + dataflowSetToString(dset, oldOut));

                // Assume that there is at least one instruction in every block
                Assume.that(b.firstInstructionHandle != null
                        && b.lastInstructionHandle != null);

                /*
                 * For each instruction,
                 * 
                 * OLD_OUT = OUT
                 * 
                 * IN = UNION(OUT[p]) where p is a predecessor of this instruction
                 * 
                 * OUT = UNION(GEN, IN - KILL) if
                 * 
                 * OLD_OUT != OUT then add the successsors to the workset
                 */
                InstructionHandle ih = b.firstInstructionHandle;
                AbstractState as = b.firstAbstractState;
                while (ih != b.lastInstructionHandle.getNext()) {
                    if (COMPUTE_REACHING_DEFINITION_DEBUG_PRINT)
                        BasicIO.out.println("Processing IH" + ih.getPosition());

                    gen = new BitSet();
                    if (ih.getInstruction() instanceof LocalWrite) {
                        Def def = dset.getDef(ih);
                        gen.set(def.serialNumber);
                    }

                    if (COMPUTE_REACHING_DEFINITION_DEBUG_PRINT)
                        BasicIO.out.println("gen " + dataflowSetToString(dset, gen));

                    if (ih == b.firstInstructionHandle) { // First instruction
                        in = (BitSet) b.reachingInBitSet.clone();
                        for (Iterator it = b.getInEdges().iterator(); it
                                .hasNext();) {
                            BasicBlock p = (BasicBlock) it.next();
                            in.or(p.reachingOutBitSet);
                        }
                        b.reachingInBitSet.or(in);
                        as.reachingInBitSet.or(in);
                    } else { // otherwise
                        in = as.prev.reachingOutBitSet;
                        as.reachingInBitSet.or(in);
                    }

                    if (COMPUTE_REACHING_DEFINITION_DEBUG_PRINT)
                        BasicIO.out.println("in " + dataflowSetToString(dset, in));

                    // KILL & OUT : remove from IN all defs wrapping the
                    // same temporary as GEN except GEN and union the
                    // resulting IN and GEN
                    out = new BitSet();
                    out.or(gen);
                    BitSet copyIn = (BitSet) in.clone();
		    for (int i1 = copyIn.nextSetBit(0); i1 != -1; i1 = copyIn.nextSetBit(i1 + 1)) {
                        Def oneOfIn = (Def) dset.number2Def.get(i1);
                        boolean to_be_removed = false;
			for (int i2 = gen.nextSetBit(0); i2 != -1; i2 = gen.nextSetBit(i2 + 1)) {
                            Def d = (Def) dset.number2Def.get(i2);
                            if (oneOfIn == d)
                                continue;
			    if (oneOfIn.localIndex == d.localIndex) {
                                to_be_removed = true;
				//                                Assume
				//                                        .that(((LocalAccess) oneOfIn.ih
				//                                                .getInstruction()).getTypeCode() == ((LocalAccess) d.ih
				//                                                .getInstruction()).getTypeCode());
                            }
                        }
                        if (to_be_removed)
			    copyIn.clear(i1); // remove oneOfIn from copyIn
                    }
		    out.or(copyIn);
		    as.reachingOutBitSet.or(out);

                    if (COMPUTE_REACHING_DEFINITION_DEBUG_PRINT)
                        BasicIO.out.println("out " + dataflowSetToString(dset, in));

                    if (ih == b.lastInstructionHandle)
                        b.reachingOutBitSet.or(out);

                    ih = ih.getNext();
                    as = as.next;
                }

                // if oldOut != newOut, add successors to the work list
                if (!oldOut.equals(out)) {
		    for(Iterator it = b.getOutEdges().iterator(); it.hasNext(); ) {
		    	workset.insert((BasicBlock) it.next());
		    }
		    //workset.union(b.getOutEdges());
                }
	    }
	}	    

        /**
         * Perform a depth first search in the control flow graph
         * Mark unreachable blocks
         * 
         * @param basicBlockList
         */
        private void depthFirstSearch(BasicBlock[] basicBlockList) {
            preorder = 0;
	    for (int i = 0; i < basicBlockList.length; i++) {
		BasicBlock b = basicBlockList[i];
                b.setPreorder(0);
                b.setReversePostOrder(0);
            }
            rpostorder = basicBlockList.length;
            DFS(basicBlockList[0]);
        }

        /**
         * The recursive helper method called by depthFirstSearch
         * 
         * @param b
         */
        private void DFS(BasicBlock b) {
            b.setReachable(true);
            b.setPreorder(preorder);
            preorder++;
            for (Iterator it = b.getOutEdges().iterator(); it.hasNext();) {
                BasicBlock s = (BasicBlock) it.next();
                if (s.getPreorder() == 0) {
                    b.setOutEdgeType(s, BasicBlock.TREE_EDGE);
                    DFS(s);
                } else if (s.getReversePostOrder() == 0) {
                    b.setOutEdgeType(s, BasicBlock.BACK_EDGE);
                } else if (b.getPreorder() < s.getPreorder()) {
                    b.setOutEdgeType(s, BasicBlock.FORWARD_EDGE);
                } else {
                    b.setOutEdgeType(s, BasicBlock.CROSS_EDGE);
                }
            }
            b.setReversePostOrder(rpostorder);
            rpostorder--;
        }

        /**
         * Detect loops (fill in BasicBlock.blocksInThisLoop and
         * BasicBlock.enclosingLoopEntries) based on the edge type information
         * computed in depthFirstSearch().
         * 
         * @param basicBlockList
         */
        private void detectLoops(BasicBlock[] basicBlockList) {
            for (int i = 0; i < basicBlockList.length; i++) {
                BasicBlock b = basicBlockList[i];
                IdentityHashSet loop = findSingleEntryLoopStartingAt(b);
                if (loop.size() > 0) {
                    loop.add(b);
                    b.setBlocksInThisLoop(loop);
                    b.addEnclosingLoopEntry(b);
                }
                for (Iterator it = loop.iterator(); it.hasNext();) {
                    BasicBlock lb = (BasicBlock) it.next();
                    lb.addEnclosingLoopEntry(b);
                }
            }
        }

        /**
         * A helper method for detectLoops().
         * 
         * @param b
         * @return the blocks in the loop whose entry block is b if b is a loop
         *         entry at all (exclusing b).
         */
        private IdentityHashSet findSingleEntryLoopStartingAt(BasicBlock b) {
            IdentityHashSet loop = new IdentityHashSet();
            IdentityHashSet queue = new IdentityHashSet();
            for (Iterator it = b.getInEdges().iterator(); it.hasNext();) {
                BasicBlock p = (BasicBlock) it.next();
                if (p.getOutEdgeType(b) == BasicBlock.BACK_EDGE) {
		    if (p == b) { // one block loop
			loop.add(p);
		    }
                    if (!loop.contains(p) && p != b) {
                        queue.add(p);
                        loop.add(p);
                    }
                }
            }
            while (!queue.empty()) {
                BasicBlock x = (BasicBlock) queue.remove();
                for (Iterator it = x.getInEdges().iterator(); it.hasNext();) {
                    BasicBlock p = (BasicBlock) it.next();
                    if (p != b && !loop.contains(p)) {
                        queue.add(p);
                        loop.add(p);
                    }
                }
            }
            return loop;
        }

        /* Phase 4 code */

        /** Shift local variables so that the local variables for arguments 
         * are copied to another set of local variables at the beginning of 
         * the code and will never be used for the rest of the code. 
         */
        private void shiftLocals(ByteCodeGen mg) {
            ShiftLocalsVisitor v = new ShiftLocalsVisitor(mg);
            v.run();
        }

        /**
         * Enforce mono-typed local variables by splitting each poly-typed local variable.
         * @param mg
         */
        private void monoTypeLocals(ByteCodeGen mg) {
            MonoTypeLocalsVisitor v = new MonoTypeLocalsVisitor(mg);
            v.run();
        }

        /**
         * Convert into register based stack code.
         * 
         * All intermediate values are stored once into a local variable - A
         * bytecode instruction that produces a value on the operand stack is
         * immediately followed by a store instruction which stores the produced
         * value into a local variable. In a similar manner, a bytecode
         * instruction that consumes a value or more on the operand stack is
         * immediately preceded by a sequence of the exact number of load
         * instructions for the consumed values. If a consumed value is a
         * constant, a constant pusing instruction may appear instead of a load
         * instruction. Consequently, at every instruction point (except loads
         * or stores) the operand stack contains only the inputs to the
         * instruction. This property enables the bytecode to be treated as a
         * register based code where a local variable corresponds to a register.
         * Note that IINC and stack instructions (SWAP, DUP, etc) are removed.
         * 
         * @param mg
         */
        private void storeAllIntermediateValuesOnce(ByteCodeGen mg) {
            StoreAllIntermediateValuesOnceVisitor saivov = 
                new StoreAllIntermediateValuesOnceVisitor(mg);
            saivov.run();
        }

        /**
         * Eliminate all StackInstructions (e.g. DUP, SWAP) and IINC
         * 
         * @param mg
         */
        private void eliminateStackInstructionsAndIINC(ByteCodeGen mg) {
            StackInstructionAndIINCEliminatorVisitor siev = 
                new StackInstructionAndIINCEliminatorVisitor(mg);
            siev.run();
        }

        /**
         * Check if the operand stack height is zero at the beginning of every basic block.
         * @param mg
         */
        private void checkZeroStackHeight(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();

            /* Check that the operand stack is empty at each basic block boundary, 
             * except for: 
             * 
             * 1. Exception handler entries
             * 
             * 2. Subroutine entries
             * 
             * 3. The basic blocks that start after an exception edges and does not have 
             * any incoming control edge. (The result of the last instruction in the 
             * previous block are on the operand stack if there is not an exception thrown).
             * 
             */
            for (int i = 0; i < abstractStates.length; i++) {
                if (abstractStates[i] != null
                        && abstractStates[i].block != null
                        && !abstractStates[i].block.isAfterExceptionEdge
                        && !abstractStates[i].block.isHandlerEntry
                        && !abstractStates[i].block.isSubroutineEntry()
                        && abstractStates[i].stack.length > 0)
                    throw new Error(
                            "A non-empty stack is found at instruction number "
                                    + i);
            }
        }

        /**
         * Convert so that the operand stack height is zero at the beginning of every basic block.
         * @param mg
         */
        private void enforceZeroStackHeight(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();
            BasicBlock[] basicBlockList = v.getBasicBlockList();
            IdentityHashSet handledPredecessors = new IdentityHashSet();
            InstructionList il = mg.getInstructionList();
            final int maxLocals = mg.getMaxLocals();
            int newMaxLocals = maxLocals;

	    for(int k = 0; k < basicBlockList.length; k++) {
		BasicBlock b = basicBlockList[k];
                // Assume that there is no subroutines in the method
                Assume.that(b.isSubroutineEntry() == false);

                AbstractState state = b.firstAbstractState;
                char[] stack = state.stack;
                if (stack.length <= 0 || b.isAfterExceptionEdge)
		    continue;
                if (b.isHandlerEntry) {
                    // If the first instruction in b is not a LocalWrite or a POP
                    // insert a LocalWrite and a LocalRead before the first instruction
                    if ((! (b.firstInstructionHandle.getInstruction() instanceof LocalWrite))
                    && (! (b.firstInstructionHandle.getInstruction() instanceof POP))) {
                    //BasicIO.out.println(mg.toString());
                    InstructionHandle updatedFirstIH = b.firstInstructionHandle;
                    InstructionHandle insertionPoint = b.firstInstructionHandle;
                    updatedFirstIH = il.insert(insertionPoint,
                                   LocalWrite.make(TypeCodes.REFERENCE, 
                                          maxLocals + 0));
                    il.insert(insertionPoint, LocalRead
                          .make(TypeCodes.REFERENCE, maxLocals + 0));
                    InstructionTargeter[] targeters = b.firstInstructionHandle
                                    .getTargeters();
                    if (targeters != null) {
                        for (int i = 0; i < targeters.length; i++) {
                            if (targeters[i] instanceof CodeExceptionGen) {
                                targeters[i].updateTarget(b.firstInstructionHandle,
                                              updatedFirstIH);
                            } else {
                                //BasicIO.out.println("Found : BranchInstruction pointing to the handler entry " + mg.toString());
                            }
                        }
                    }
                    if (newMaxLocals < maxLocals + 1)
                        newMaxLocals = maxLocals + 1;
                    }
                            continue;
                }

                if (newMaxLocals < maxLocals + stack.length)
                    newMaxLocals = maxLocals + stack.length;

                // Handle the beginning of this block
                {
                    InstructionHandle updatedFirstIH = b.firstInstructionHandle;
                    InstructionHandle insertionPoint = b.firstInstructionHandle;
                    for (int i = 0; i < stack.length; i++) {
                        char t = stack[i];
                        if (t == TypeCodes.INT || t == TypeCodes.FLOAT
                                || t == TypeCodes.REFERENCE) {
                            if (i == 0)
                                updatedFirstIH = il.insert(insertionPoint,
                                        LocalRead.make(t, maxLocals + i));
                            else
                                il.insert(insertionPoint, LocalRead
                                        .make(t, maxLocals + i));
                        } else if (t == TypeCodes.LONG || t == TypeCodes.DOUBLE) {
                            if (i == 0)
                                updatedFirstIH = il.insert(insertionPoint,
                                        LocalRead.make(t,
                                                maxLocals + i));
                            else
                                il.insert(insertionPoint, LocalRead
                                        .make(t, maxLocals + i));
                            i++; // skip the upper half of the value
                        } else
                            throw new Error("unexpected : " + t);
                    }
                    InstructionTargeter[] targeters = b.firstInstructionHandle
                            .getTargeters();
                    if (targeters != null) {
                        for (int i = 0; i < targeters.length; i++) {
			    if (targeters[i] instanceof BranchInstruction) {
                            targeters[i].updateTarget(b.firstInstructionHandle,
                                    updatedFirstIH);
			    }
                        }
                    }
                }

                // Handle the end of the predecessors of this block
                IdentityHashSet inEdges = b.getInEdges();
                for (Iterator it = inEdges.iterator(); it.hasNext();) {
                    BasicBlock pred = (BasicBlock) it.next();
                    if (handledPredecessors.contains(pred))
                        continue;
                    handledPredecessors.add(pred);
                    Instruction ipi = pred.lastInstructionHandle
                            .getInstruction();

                    if (ipi instanceof If || ipi instanceof Switch
                            || ipi instanceof GotoInstruction) {
			// If the last instruction is an If, Select, or Goto, insert
			// the stores before it
                        AbstractState predASBeforeLast = pred.lastAbstractState;
                        char[] predStackBeforeLast = predASBeforeLast.stack;
                        if (newMaxLocals < maxLocals
                                + predStackBeforeLast.length)
                            newMaxLocals = maxLocals
                                    + predStackBeforeLast.length;

                        InstructionHandle insertionPoint = pred.lastInstructionHandle;
                        for (int i = predStackBeforeLast.length - 1; i >= 0; i--) {
                            char t = predStackBeforeLast[i];
                            if (t == TypeCodes.INT || t == TypeCodes.FLOAT
                                    || t == TypeCodes.REFERENCE) {
                                Instruction ins = LocalWrite
                                        .make(t, maxLocals + i);
                                il.insert(insertionPoint, ins);
                            } else if (t == TypeCodes.LONG_UPPER) {
                                i--; // skip TypeCodes.LONG
                                Instruction ins = LocalWrite
                                        .make(TypeCodes.LONG, maxLocals + i);
                                il.insert(insertionPoint, ins);
                            } else if (t == TypeCodes.DOUBLE_UPPER) {
                                i--; // skip TypeCodes.DOUBLE
                                Instruction ins = LocalWrite
                                        .make(TypeCodes.DOUBLE, maxLocals + i);
                                il.insert(insertionPoint, ins);
                            } else
                                throw new Error("unexpected : " + t);
                        }
                        // When the last instruction consumes from the stack, reload
                        // the operands
                        if (predStackBeforeLast.length > stack.length) {
                            for (int i = stack.length; i < predStackBeforeLast.length; i++) {
                                char t = predStackBeforeLast[i];
                                if (typeCode2Size(t) == 1) {
                                    il.insert(insertionPoint,
                                            LocalRead.make(t,
                                                    maxLocals + i));
                                } else if (typeCode2Size(t) == 2) {
                                    il.insert(insertionPoint,
                                            LocalRead.make(t,
                                                    maxLocals + i));
                                    i++; // skip the upper half of the value
                                } else
                                    throw new Error("unexpected : " + t);
                            }
                        }
                    } else {
			// If the last instruction in the predecessor is not an If,
			// Goto, or Select, insert the stores after the last
			// instruction
                        InstructionHandle insertionPoint = pred.lastInstructionHandle
                                .getNext();
                        for (int i = stack.length - 1; i >= 0; i--) {
                            char t = stack[i];
                            if (t == TypeCodes.INT || t == TypeCodes.FLOAT
                                    || t == TypeCodes.REFERENCE) {
                                if (insertionPoint != null)
                                    il.insert(insertionPoint,
                                            LocalWrite.make(t,
                                                    maxLocals + i));
                                else
                                    il.append(LocalWrite.make(t,
                                            maxLocals + i));
                            } else if (t == TypeCodes.LONG_UPPER) {
                                i--; // skip the upper half of the value
                                if (insertionPoint != null)
                                    il.insert(insertionPoint,
                                            LocalWrite.make(
                                                    TypeCodes.LONG, maxLocals + i));
                                else
                                    il.append(LocalWrite.make(
                                            TypeCodes.LONG, maxLocals + i));
                            } else if (t == TypeCodes.DOUBLE_UPPER) {
                                i--; // skip the upper half of the value
                                if (insertionPoint != null)
                                    il
                                            .insert(
                                                    insertionPoint,
                                                    LocalWrite
                                                            .make(
                                                                    TypeCodes.DOUBLE,
                                                                    maxLocals
                                                                            + i));
                                else
                                    il.append(LocalWrite.make(
                                            TypeCodes.DOUBLE, maxLocals + i));
                            } else
                                throw new Error("unexpected : " + t);
                        }
                    }
                }
            }

            mg.setMaxLocals(newMaxLocals);
            mg.getByteCode(); // finalize the transformation
	    //il.setPositions();
        }

        /**
         * Eliminate subroutines by duplicating the subroutine bodies and replacing 
         * the JSR and RET with GOTO's.
         */
        private void eliminateSubroutines(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            //AbstractState[] abstractStates = v.getAbstractStates();
            BasicBlock[] basicBlockList = v.getBasicBlockList();

            // Check that all blocks within a subroutine are consecutive and the
            // last instruction is a RET
	    for(int k = 0; k < basicBlockList.length; k++) {
		BasicBlock b = basicBlockList[k];
                if (b != null && b.isSubroutineEntry()) {
                    BasicBlock entry = b;
                    BasicBlock exit = b.matchingSubroutineExit;
                    Assume
                            .that(exit.lastInstructionHandle.getInstruction() instanceof RET);
                    //BasicIO.out.println("Looking at subroutine entry : " + entry +
                    // ", exit : " + exit);
                    Assume.that(exit != null);
                    if (entry == exit)
                        continue;
                    for (Iterator it = entry.getOutEdges().iterator(); it
                            .hasNext();) {
                        BasicBlock succ = (BasicBlock) it.next();
                        Assume
                                .that(
                                        (entry.startPC <= succ.startPC && succ.startPC <= exit.startPC)
                                                || succ.isHandlerEntry,
                                        "There is an out-of-subroutine outgoing edge : "
                                                + entry);
                    }
                    for (Iterator it = exit.getInEdges().iterator(); it
                            .hasNext();) {
                        BasicBlock pred = (BasicBlock) it.next();
                        Assume.that(entry.startPC <= pred.startPC
                                && pred.startPC <= exit.startPC,
                                "There is an out-of-subroutine incoming edge : "
                                        + exit);
                    }
                    for (BasicBlock b1 = entry.nextBlock; b1 != exit; b1 = b1.nextBlock) {
                        for (Iterator it = b1.getOutEdges().iterator(); it
                                .hasNext();) {
                            BasicBlock succ = (BasicBlock) it.next();
                            Assume
                                    .that(
                                            (entry.startPC <= succ.startPC && succ.startPC <= exit.startPC)
                                                    || succ.isHandlerEntry,
                                            "There is an out-of-subroutine outgoing edge : "
                                                    + b1);
                        }
                        for (Iterator it = b1.getInEdges().iterator(); it
                                .hasNext();) {
                            BasicBlock pred = (BasicBlock) it.next();
                            Assume.that(entry.startPC <= pred.startPC
                                    && pred.startPC <= exit.startPC,
                                    "There is an out-of-subroutine incoming edge : "
                                            + b1);
                        }
                    }
                }
            }

            InstructionList il = mg.getInstructionList();
            BasicBlock subEntry = searchForOneSubroutineEntryBlock(basicBlockList[0]);

            // If a subroutine comes at the end of the bytecode stream,
            // subBodyEnd.getNext() should give a next instruction
            if (subEntry != null)
                il.append(NOP.make());

            while (subEntry != null) {
                BasicBlock entry = subEntry;
                BasicBlock exit = entry.matchingSubroutineExit;

                InstructionHandle store = entry.firstInstructionHandle;
                InstructionHandle ret = exit.lastInstructionHandle;
                Assume.that(store.getInstruction() instanceof LocalWrite
                        && ret.getInstruction() instanceof RET);

                IdentityHashSet handlerRecords = new IdentityHashSet();
                CodeExceptionGen[] handlers = mg.getExceptionHandlers();

                for (int i = 0; i < handlers.length; i++) {
                    InstructionHandle startPC = handlers[i].getStartPC();
                    InstructionHandle endPC = handlers[i].getEndPC();
                    InstructionHandle handlerPC = handlers[i].getHandlerPC();

                    /*
                     * Record the exception handlers within the subroutine and their
                     * relative offsets from subBodyStart
                     */
                    if (store.getPosition() <= startPC.getPosition()
                            && endPC.getPosition() <= ret.getPosition()) {
                        boolean isHandlerWithinSub = store.getPosition() <= handlerPC
                                .getPosition()
                                && handlerPC.getPosition() <= ret.getPosition();
                        SubroutineExceptionHandlerRecord record = null;
                        InstructionHandle subBodyStart = store.getNext();
                        if (isHandlerWithinSub) {
                            record = new SubroutineExceptionHandlerRecord(
                                    handlers[i], startPC.getPosition()
                                            - subBodyStart.getPosition(), endPC
                                            .getPosition()
                                            - subBodyStart.getPosition(),
                                    handlerPC.getPosition()
                                            - subBodyStart.getPosition(),
                                    handlers[i].getCatchType());
                        } else {
                            record = new SubroutineExceptionHandlerRecord(
                                    handlers[i], startPC.getPosition()
                                            - subBodyStart.getPosition(), endPC
                                            .getPosition()
                                            - subBodyStart.getPosition(),
                                    handlerPC, handlers[i].getCatchType());
                        }
                        //BasicIO.out.println("Recorded " + handlers[i]);
                        handlerRecords.add(record);
                    }
                }

                InstructionHandle tempJSRTarget = il.insert(store,
                        NOP.make());
                /*
                 * This NOP is going to keep the branch targets within the subroutine 
                 * (eg branch to the RET)
                 */
                InstructionHandle retPlaceHolder = il.insert(ret,
                        NOP.make());
                /*
                 * This NOP is going to keep the instruction targets for the exception handlers 
                 * whose startPC and end PC are not both within the subroutine.
                 */
                InstructionHandle insertionPoint = il.insert(ret,
                        NOP.make());

                // skip the store and the RET instruction
                InstructionHandle subBodyStart = store.getNext();
                InstructionHandle subBodyEnd = retPlaceHolder;

                // delete the store and the RET instruction
                try {
                    il.delete(store);
                } catch (TargetLostException e) {
                    // Redirect the branch targets to the deleted store to the next
                    // instruction
                    InstructionHandle[] targets = e.getTargets();
                    for (int i = 0; i < targets.length; i++) {
                        InstructionTargeter[] targeters = targets[i]
                                .getTargeters();
                        for (int j = 0; j < targeters.length; j++) {
                            targeters[j]
                                    .updateTarget(targets[i], tempJSRTarget);
                        }
                    }
                }
                try {
                    il.delete(ret);
                } catch (TargetLostException e) {
                    // Redirect the branch targets to the deleted RET to the
                    // previous instruction
                    InstructionHandle[] targets = e.getTargets();
                    for (int i = 0; i < targets.length; i++) {
                        InstructionTargeter[] targeters = targets[i]
                                .getTargeters();
                        for (int j = 0; j < targeters.length; j++) {
                            InstructionTargeter it = targeters[j];
                            if (it instanceof BranchInstruction) {
                                it.updateTarget(targets[i], retPlaceHolder);
                            } else if (targeters[j] instanceof CodeExceptionGen) {
                                it.updateTarget(targets[i], insertionPoint);
                                /*
                            } else if (it instanceof LocalVariableGen) {
                                mg.removeLocalVariable((LocalVariableGen) it);
                            } else if (it instanceof LineNumberGen) {
                                mg.removeLineNumber((LineNumberGen) it);
                                */
                            } else
                                throw new Error();
                        }
                    }
                }

                // Copy the subroutine body bytecode
                byte[] wholeMethod = il.getByteCode();
                int subBodyStartPC = subBodyStart.getPosition();
                int subBodyEndPC = subBodyEnd.getNext().getPosition();
                byte[] subBodyBC = new byte[subBodyEndPC - subBodyStartPC];
                System.arraycopy(wholeMethod, subBodyStartPC, subBodyBC, 0,
                        subBodyBC.length);
                InstructionList subList = new InstructionList(subBodyBC);

                // Delete the original subroutine body
                try {
                    il.delete(subBodyStart, subBodyEnd);
                } catch (TargetLostException e) {
                    // Ignore because all the targets should be within the deleted
                    // code region
                }

                IdentityHashSet inEdges = entry.getInEdges();
                IdentityHashSet outEdges = exit.getOutEdges();
                Assume.that(inEdges.size() == outEdges.size());

                for (Iterator it = inEdges.iterator(); it.hasNext();) {
                    InstructionHandle top = il.insert(insertionPoint, subList.copy());
                    BasicBlock jsrB = (BasicBlock) it.next();
                    BasicBlock returnB = jsrB.nextBlock;
                    Assume.that(outEdges.contains(returnB));
                    outEdges.remove(returnB);
                    Assume
                            .that(jsrB.lastInstructionHandle.getInstruction() 
                                    instanceof JsrInstruction);
                    InstructionHandle returnPoint = jsrB.lastInstructionHandle
                            .getNext();

                    // Insert the incoming goto
                    InstructionHandle ingoto = il.insert(returnPoint, GOTO.make(top));

                    //il.setPositions();
                    //BasicIO.out.println("About to delete the JSR at " +
                    // jsrB.lastInstructionHandle.getPosition());
                    Assume
                            .that(jsrB.lastInstructionHandle.getInstruction() 
                                    instanceof JsrInstruction);

                    //BasicIO.out.println(il.toString(true));

                    // Delete the JSR
                    try {
                        il.delete(jsrB.lastInstructionHandle);
                    } catch (TargetLostException e) {
                        InstructionHandle[] targets = e.getTargets();
                        for (int i = 0; i < targets.length; i++) {
                            InstructionTargeter[] targeters = targets[i]
                                    .getTargeters();
                            for (int j = 0; j < targeters.length; j++) {
                                targeters[j].updateTarget(targets[i], ingoto);
                            }
                        }
                    }

                    // Insert the outgoing goto
                    InstructionHandle outgoto = il.insert(insertionPoint, GOTO.make(returnPoint));
                    
                    // Remove the NOP at the end of the copy of the subroutine body
                    Assume.that(outgoto.getPrev().getInstruction() instanceof NOP);         
                    try {
                        il.delete(outgoto.getPrev());
                    } catch (TargetLostException e) {
                        InstructionHandle[] targets = e.getTargets();
                        for (int i = 0; i < targets.length; i++) {
                            InstructionTargeter[] targeters = targets[i]
                                    .getTargeters();
                            for (int j = 0; j < targeters.length; j++) {
                                Assume.that(targeters[j] instanceof BranchInstruction);
                                targeters[j].updateTarget(targets[i], outgoto);
                            }
                        }
                    }
                    
                    // Adjust PCs
                    il.setPositions();

                    // Update exception handlers for the subroutine copy
                    int[] positions = il.getInstructionPositions();
                    InstructionHandle[] ihs = il.getInstructionHandles();
                    int topPos = top.getPosition();
                    for (Iterator it2 = handlerRecords.iterator(); it2
                            .hasNext();) {
                        SubroutineExceptionHandlerRecord r = (SubroutineExceptionHandlerRecord) it2
                                .next();
                        if (r.handlerWithinSubroutine) {
                            int startPos = topPos + r.relStartPC;
                            int endPos = topPos + r.relEndPC;
                            int handlerPos = topPos + r.relHandlerPC;
                            InstructionHandle startH = null;
                            InstructionHandle endH = null;
                            InstructionHandle handlerH = null;
                            for (int i = 0; i < positions.length; i++) {
                                if (startPos == positions[i]) {
                                    startH = ihs[i];
                                }
                                if (endPos == positions[i]) {
                                    endH = ihs[i];
                                }
                                if (handlerPos == positions[i]) {
                                    handlerH = ihs[i];
                                }
                            }
                            Assume.that(startH != null && endH != null
                                    && handlerH != null);
                            mg.addExceptionHandler(startH, endH, handlerH,
                                    r.catchType);
                        } else {
                            int startPos = topPos + r.relStartPC;
                            int endPos = topPos + r.relEndPC;
                            InstructionHandle startH = null;
                            InstructionHandle endH = null;
                            InstructionHandle handlerH = r.handlerHandle;
                            for (int i = 0; i < positions.length; i++) {
                                if (startPos == positions[i]) {
                                    startH = ihs[i];
                                }
                                if (endPos == positions[i]) {
                                    endH = ihs[i];
                                }
                            }
                            if (startH == null)
                                BasicIO.out.println("startH null");
                            if (endH == null)
                                BasicIO.out.println("endH null");
                            if (handlerH == null)
                                BasicIO.out.println("handlerH null");
                            Assume.that(startH != null && endH != null
                                    && handlerH != null);
                            mg.addExceptionHandler(startH, endH, handlerH,
                                    r.catchType);
                        }
                    }
                }

                // Delete old exception handlers
                for (Iterator it2 = handlerRecords.iterator(); it2.hasNext();) {
                    SubroutineExceptionHandlerRecord r = (SubroutineExceptionHandlerRecord) it2
                            .next();
                    mg.removeExceptionHandler(r.original);
                }

                // Remove the NOP at the insertion point
                InstructionHandle prev_insertionPoint = insertionPoint.getPrev();
                try {
                    il.delete(insertionPoint);
                } catch (TargetLostException e) {
                    InstructionHandle[] targets = e.getTargets();
                    for (int i = 0; i < targets.length; i++) {
                        InstructionTargeter[] targeters = targets[i]
                                .getTargeters();
                        for (int j = 0; j < targeters.length; j++) {
                            Assume.that(targeters[j] instanceof CodeExceptionGen);
                            targeters[j].updateTarget(targets[i], prev_insertionPoint);
                        }
                    }
                }
                
                mg.getByteCode(); // finalize the editing
		//il.setPositions();

                v = new AbstractInterpretationVisitor(mg);
                v.run();
                basicBlockList = v.getBasicBlockList();
                il = mg.getInstructionList();
                subEntry = searchForOneSubroutineEntryBlock(basicBlockList[0]);
            }

            // Remove the NOP if it was inserted at the end of the code
            InstructionHandle end = il.getEnd();
            InstructionHandle prev_end = end.getPrev();
            if (end.getInstruction() instanceof NOP) {
                try {
                    il.delete(end);
                } catch (TargetLostException e) {
                    InstructionHandle[] targets = e.getTargets();
                    for (int i = 0; i < targets.length; i++) {
                        InstructionTargeter[] targeters = targets[i]
                                .getTargeters();
                        for (int j = 0; j < targeters.length; j++) {
                            targeters[j].updateTarget(targets[i], prev_end);
                        }
                    }
                }
            }
	    mg.getByteCode();
        }

        /**
         * A helper for eliminateSubroutines. Searches for one subroutine entry at a time.
         * @param top
         */
        private BasicBlock searchForOneSubroutineEntryBlock(BasicBlock top) {
            for (BasicBlock b = top; b != null; b = b.nextBlock) {
                if (b.isSubroutineEntry()) {
                    return b;
                }
            }
            return null;
        }

        /**
         * Compute the abstract state at each instruction.
         */
        private AbstractState[] computeAbstractStates(ByteCodeGen mg) {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            AbstractState[] abstractStates = v.getAbstractStates();

            /*
            // Check that every instruction is reachable
            for (int i = 0; i < abstractStates.length; i++) {
                if (abstractStates[i] == null)
                    throw new Error(
                            "A null abstract state is found at instruction number "
                                    + i);
            }
            */
            
            return abstractStates;
        }
    }

    /**
     * The RegisterTable attribute.
     */
    public static final class RegisterTable extends Attribute /*implements Node*/ {

        private final static String name = "RegisterTable";
        private static int nameIndex;

        static {
            nameIndex = RepositoryUtils.asUTF(name);
        }
        
        public static final class Entry implements Cloneable {
            /** The size of one Entry in bytes */
            static final int SIZE = 5;

            private int score;

            /** Uses the type IDs for StackMaps */
            private char type;

            public Entry(char type, int score) {
                this.score = score;
                this.type = type;
            }

            public char type() { return type; }
            public int score() { return score; }
	    public void setScore(int ns) { score = ns; }
            
            public final String toString() {
                return "(type=" + type + ",score="
                        + score + ")";
            }

            public Entry copy() {
                try {
                    return (Entry) clone();
                } catch (CloneNotSupportedException e) {
                    throw new Error(e.toString());
                }
            }

        }

        private int map_length;

        private Entry[] map;

        public static RegisterTable make(Entry[] map) {
            return new RegisterTable(map);
        }

        private RegisterTable(Entry[] map) {
            setRegisterTable(map);
        }

        public final void accept(RepositoryProcessor visitor) {
            throw new Error();
        }
        public final int getNameIndex() {
            return nameIndex;
        }
        
        public final Entry[] getEntries() {
            return map;
        }

        public final void setRegisterTable(Entry[] map) {
            this.map = map;

            map_length = (map == null) ? 0 : map.length;
        }

        public final String toString() {
            StringBuffer buf = new StringBuffer("RegisterTable(");

            for (int i = 0; i < map_length; i++) {
                buf.append(map[i].toString());

                if (i < map_length - 1)
                    buf.append(", ");
            }

            buf.append(')');

            return buf.toString();
        }

        public Attribute copy() {
            RegisterTable c = null;
            
            try {
                c = (RegisterTable) clone();
            } catch(CloneNotSupportedException e) {
                throw new Error(e.toString());
            }

            c.map = new Entry[map_length];
            for (int i = 0; i < map_length; i++)
                c.map[i] = map[i].copy();

            return c;
        }

        public final int getMapLength() {
            return map_length;
        }

    }

    /**
     * The Liveness attribute.
     */
    public static final class Liveness extends Attribute /*implements Node*/ {

        private final static String name = "Liveness";
        private static int nameIndex;

        static {
            nameIndex = RepositoryUtils.asUTF(name);
        }
        
        public static final class Entry implements Cloneable {
            /** The size of one Entry in bytes */
            public static int getSize(int maxLocals) {
                return 2 + maxLocals;
            }

            private int position; /* PC */

            private boolean[] liveness;

            private int maxLocals;

	    private boolean isBlockStart;

            public Entry(int position, boolean[] liveness, int maxLocals, boolean isBlockStart) {
                this.position = position;
                this.maxLocals = maxLocals;
                this.liveness = liveness;
		this.isBlockStart = isBlockStart;
            }

	    public boolean isBlockStart() { return isBlockStart; }
            public int position() { return position; }
            public boolean[] liveness() { return liveness; }
            
            public final String toString() {
                String r = "(pc=" + position + ",liveness=";
                for (int i = 0; i < maxLocals; i++) {
                    if (liveness[i])
                        r += "1";
                    else
                        r += "0";
                }
                r += ")";
                return r;
            }

            public Entry copy() {
                try {
                    return (Entry) clone();
                } catch (CloneNotSupportedException e) {
                    throw new Error(e.toString());
                }
            }

        }

        private int map_length;

        private Entry[] map;

        public static Liveness make(Entry[] map) {
            return new Liveness(map);
        }

        private Liveness(Entry[] map) {
            setLivenessVector(map);
        }

        public Entry[] getEntries() { return map; }
        
        public final void setLivenessVector(Entry[] map) {
            this.map = map;

            map_length = (map == null) ? 0 : map.length;
        }

        
        public final void accept(RepositoryProcessor visitor) {
            throw new Error();
        }
        public final int getNameIndex() {
            return nameIndex;
        }
        
        public final String toString() {
            StringBuffer buf = new StringBuffer("Liveness(");

            for (int i = 0; i < map_length; i++) {
                buf.append(map[i].toString());

                if (i < map_length - 1)
                    buf.append(", ");
            }

            buf.append(')');

            return buf.toString();
        }

        public Attribute copy(Constants constant_pool) {
            Liveness c = null;
            
            try {
                c = (Liveness) clone();
            } catch(CloneNotSupportedException e) {
                throw new Error(e.toString());
            }

            c.map = new Entry[map_length];
            for (int i = 0; i < map_length; i++)
                c.map[i] = map[i].copy();

            return c;
        }

        public final int getMapLength() {
            return map_length;
        }

    }

    /**
     * The register allocator.
     */
    public static class RegisterAllocator {
        /* Available allocation heuristics */
        private static final String[] ALLOCATOR_NAMES = new String[] { "LP",
                "GP", "EGC", "IGC", "GC" };

        public static final int LINEAR_PACKING = 0;

        public static final int GREEDY_PACKING = 1;

        public static final int EXACT_GRAPH_COLORING = 2;

        public static final int INCREMENTAL_GRAPH_COLORING = 3;

        public static final int GRAPH_COLORING = 4; // standard non-cumulative allocation

        // When GRAPH_COLORING is used, the number of available registers for each type 
        private static final int GRAPH_COLORING_NREG_A = 7;

        private static final int GRAPH_COLORING_NREG_I = 7;

        private static final int GRAPH_COLORING_NREG_J = 7;

        private static final int GRAPH_COLORING_NREG_F = 7;

        private static final int GRAPH_COLORING_NREG_D = 7;

        /** The register allocation heursitic used */
        private int registerAllocator;

        private String registerAllocatorName;

        // a vector of Web.Set's for each type
        private Vector wssI = new Vector();

        private Vector wssJ = new Vector();

        private Vector wssF = new Vector();

        private Vector wssD = new Vector();

        private Vector wssA = new Vector();

        // a vector of Web's for each type
        private Vector wsI = new Vector();

        private Vector wsJ = new Vector();

        private Vector wsF = new Vector();

        private Vector wsD = new Vector();

        private Vector wsA = new Vector();

        private HTint2Object t2wss = new HTint2Object();

        private HTint2Object t2ws = new HTint2Object();

        int localNum;

        boolean ran = false;

        // a vector of spilled Web.Set's for each type (used for GRAPH_COLORING)
        private Vector swssI = new Vector();

        private Vector swssJ = new Vector();

        private Vector swssF = new Vector();

        private Vector swssD = new Vector();

        private Vector swssA = new Vector();

        private HTint2Object t2swss = new HTint2Object();

        private Vector registerTableEntries;

	private Webs webs;

        RegisterAllocator(Webs webs, int initialLocalNum,
                int registerAllocator, Vector registerTableEntries) {
	    this.webs = webs;
            this.registerTableEntries = registerTableEntries;
            this.registerAllocator = registerAllocator;
            registerAllocatorName = ALLOCATOR_NAMES[registerAllocator];

            t2wss.put(TypeCodes.INT, wssI);
            t2wss.put(TypeCodes.LONG, wssJ);
            t2wss.put(TypeCodes.FLOAT, wssF);
            t2wss.put(TypeCodes.DOUBLE, wssD);
            t2wss.put(TypeCodes.REFERENCE, wssA);
            t2ws.put(TypeCodes.INT, wsI);
            t2ws.put(TypeCodes.LONG, wsJ);
            t2ws.put(TypeCodes.FLOAT, wsF);
            t2ws.put(TypeCodes.DOUBLE, wsD);
            t2ws.put(TypeCodes.REFERENCE, wsA);
            // Store webs into type-separated vectors
	    int len = webs.number2Web.size();
            for (int i = 0; i < len; i++) {
                Web w = (Web) webs.number2Web.get(i);
		if (w == null)
		    continue;
                Vector v = (Vector) t2ws.get(w.getType());
                v.add(w);
            }
            this.localNum = initialLocalNum;
            t2swss.put(TypeCodes.INT, swssI);
            t2swss.put(TypeCodes.LONG, swssJ);
            t2swss.put(TypeCodes.FLOAT, swssF);
            t2swss.put(TypeCodes.DOUBLE, swssD);
            t2swss.put(TypeCodes.REFERENCE, swssA);
        }

        private void run() {
            if (ran)
                throw new Error("Shouldn't be run more than once.");
            ran = true;
            switch (registerAllocator) {
            case LINEAR_PACKING:
                doLinearPacking();
                break;
            case GREEDY_PACKING:
                doGreedyPacking();
                break;
            case EXACT_GRAPH_COLORING:
                doExactGraphColoring();
                break;
            case INCREMENTAL_GRAPH_COLORING:
                doIncrementalGraphColoring();
                break;
            case GRAPH_COLORING:
                doGraphColoring(GRAPH_COLORING_NREG_A, GRAPH_COLORING_NREG_I,
                        GRAPH_COLORING_NREG_J, GRAPH_COLORING_NREG_F,
                        GRAPH_COLORING_NREG_D);
                break;
            default:
                throw new Error();
            }
            assignLocalNumbers2Webs();
        }

        private void assignLocalNumbers2Webs() {
            assignLocalNumbers2Webs_helper(TypeCodes.REFERENCE, t2wss);
            assignLocalNumbers2Webs_helper(TypeCodes.INT, t2wss);
            assignLocalNumbers2Webs_helper(TypeCodes.LONG, t2wss);
            assignLocalNumbers2Webs_helper(TypeCodes.FLOAT, t2wss);
            assignLocalNumbers2Webs_helper(TypeCodes.DOUBLE, t2wss);

            assignLocalNumbers2Webs_helper(TypeCodes.REFERENCE, t2swss);
            assignLocalNumbers2Webs_helper(TypeCodes.INT, t2swss);
            assignLocalNumbers2Webs_helper(TypeCodes.LONG, t2swss);
            assignLocalNumbers2Webs_helper(TypeCodes.FLOAT, t2swss);
            assignLocalNumbers2Webs_helper(TypeCodes.DOUBLE, t2swss);
        }

        private void assignLocalNumbers2Webs_helper(char t, HTint2Object t2) {
            Vector wss = (Vector) t2.get(t);
            int size = wss.size();
            for (int i = 0; i < size; i++) {
                Web.Set ws = (Web.Set) wss.get(i);
                Assume.that(ws != null);
                Assume.that(ws.getType() == t);
                ws.setLocalIndex(localNum);
                registerTableEntries.add(new RegisterTable.Entry(
                        type2StackMapType(t), ws.getWeightedRefCount()));
                Assume.that(localNum + 1 == registerTableEntries.size());
                if (typeCode2Size(t) == 2)
                    registerTableEntries.add(new RegisterTable.Entry(
                            TypeCodes.VOID, -1));
                localNum += typeCode2Size(t);
            }
        }

        private Vector getSpilledWebSetSet(char t) {
            Vector v = (Vector) t2swss.get(t);
            Assume.that(v != null);
            return v;
        }

        private Vector getWebSetSet(char t) {
            Vector v = (Vector) t2wss.get(t);
            Assume.that(v != null);
            return v;
        }

        private Vector getWebSet(char t) {
            Vector v = (Vector) t2ws.get(t);
            Assume.that(v != null);
            return v;
        }

        private void printWebSetSets() {
            printWebSetSet(TypeCodes.REFERENCE);
            printWebSetSet(TypeCodes.INT);
            printWebSetSet(TypeCodes.LONG);
            printWebSetSet(TypeCodes.FLOAT);
            printWebSetSet(TypeCodes.DOUBLE);
        }

        private void printWebSetSet(char t) {
            Vector wss = getWebSetSet(t);
            int wssSize = wss.size();
            BasicIO.out.println("WSS:" + toTypeSignature(t));
            for (int i = 0; i < wssSize; i++) {
                BasicIO.out.println(wss.get(i));
            }
        }

        private int getMaxLocals() {
            return localNum;
        }

        private Web findSimplifiableWeb(BitSet webBitSet, int K) {
	    for(int i = webBitSet.nextSetBit(0);
		i != -1;
		i = webBitSet.nextSetBit(i + 1)) {
                Web w = (Web) webs.number2Web.get(i);
                if (w.getScratchInterferences().cardinality() < K)
                    return w;
            }
            return null;
        }

        /*
         * Sort the Web.Set's in webSets (which possibly has null elements)
         * and store the sorted Web.Set's into wss.
         */
        private void sortAndStoreWebSets(Vector webSets, Vector wss) {
            Vector vwss = new Vector();
            int result_size = webSets.size();
            for (int i = 0; i < result_size; i++) {
                Object o = webSets.get(i);
                if (o != null)
                    vwss.add(o);
            }
            Web.Set[] wsa = new Web.Set[vwss.size()];
            vwss.toArray(wsa);
            sortWebSets(wsa);

            for (int i = wsa.length - 1; i >= 0; i--) {
                Web.Set webset = wsa[i];
                wss.add(webset);
            }
        }

        private void doGraphColoring(int KA, int KI, int KJ, int KF, int KD) {
            doGraphColoring_helper(TypeCodes.REFERENCE, KA);
            doGraphColoring_helper(TypeCodes.INT, KI);
            doGraphColoring_helper(TypeCodes.LONG, KJ);
            doGraphColoring_helper(TypeCodes.FLOAT, KF);
            doGraphColoring_helper(TypeCodes.DOUBLE, KD);
        }

        private void doGraphColoring_helper(char t, int K) {
            Vector ws = getWebSet(t);

            int nwebs = ws.size();

            Vector webSets = new Vector();
            Vector spilledWebSets = new Vector();
            doGraphColoring_helper2(ws, t, K, webSets, spilledWebSets);

            //checkNumberOfWebs(nwebs, webSets, spilledWebSets);

            Vector wss = getWebSetSet(t);
            sortAndStoreWebSets(webSets, wss);
            Vector swss = getSpilledWebSetSet(t);
            sortAndStoreWebSets(spilledWebSets, swss);
        }

        /**
         * Throw an error if the total number of webs in wss1 and wss2 is not equal to nwebs
         * @param nwebs
         * @param wss1 A vector of Web.Sets
         * @param wss2 A vector of Web.Sets
         */
        private void checkNumberOfWebs(int nwebs, Vector wss1, Vector wss2) {
            int nwebs_after = 0;
            for (int i = 0; i < wss1.size(); i++) {
                Web.Set wst = (Web.Set) wss1.get(i);
                if (wst != null) {
                    int sz = wst.size();
                    nwebs_after += sz;
                }
            }
            for (int i = 0; i < wss2.size(); i++) {
                Web.Set wst = (Web.Set) wss2.get(i);
                if (wst != null) {
                    int sz = wst.size();
                    nwebs_after += sz;
                }
            }
            Assume.that(nwebs == nwebs_after);
        }

        private void resetScratchColors(BitSet webSet) {
	    for(int i = webSet.nextSetBit(0);
		i != -1;
		i = webSet.nextSetBit(i + 1)) {
                Web w = (Web) webs.number2Web.get(i);
		w.setScratchColor(-1);
            }
        }

        /**
         * Apply traditional graph coloring register allocation based on optimistic
         * coloring with K virtual registers of the given type.
         * 
         * @param t
         *            the type
         * @param K
         *            the number of given virtual registers
         * @param resultWebSets -
         *            a vector of Web.Set's. This is the result of the register
         *            allocation.
         * @param spilledWebSets -
         *           a vector of Web.Set's for spilled webs.
         */
        private void doGraphColoring_helper2(Vector ws, char t, final int K,
                Vector resultWebSets, Vector spilledWebSets) {

            BitSet igraph = new BitSet();
	    for(int i = 0; i < ws.size(); i++) {
		Web w = (Web) ws.get(i);
		igraph.set(w.serialNumber);
	    }

            resetScratchColors(igraph);
            doIncrementalGraphColoring_helper2(t, 0, K, igraph, resultWebSets);

            if (igraph.nextSetBit(0) != -1) {
                resetScratchColors(igraph);
                doIncrementalGraphColoring_helper2(t, 0, igraph.cardinality(), igraph,
                        spilledWebSets);
            }

            Assume.that(igraph.nextSetBit(0) == -1);
        }

        private void doIncrementalGraphColoring() {
            doIncrementalGraphColoring_helper(TypeCodes.REFERENCE);
            doIncrementalGraphColoring_helper(TypeCodes.INT);
            doIncrementalGraphColoring_helper(TypeCodes.LONG);
            doIncrementalGraphColoring_helper(TypeCodes.FLOAT);
            doIncrementalGraphColoring_helper(TypeCodes.DOUBLE);
        }

        private void doIncrementalGraphColoring_helper(char t) {
            int regCounter = 0;
            Vector ws = getWebSet(t);
            BitSet igraph = new BitSet();
	    for(int i = 0; i < ws.size(); i++) {
		Web w = (Web) ws.get(i);
		igraph.set(w.serialNumber);
	    }
            Vector webSets = new Vector();
            //webSets.setSize(ws.size() + 1);

            resetScratchColors(igraph);
            while (igraph.nextSetBit(0) != -1) {
                doIncrementalGraphColoring_helper2(t, regCounter, 1, igraph,
                        webSets);
                regCounter++;
            }

            Vector wss = getWebSetSet(t);
            sortAndStoreWebSets(webSets, wss);
        }

        /**
         * Apply incremental graph coloring register allocation based on
         * optimistic coloring with nreg virtual register of the given
         * type.  Try allocate nreg register to the most important web,
         * but will leave other webs.
         * @param type the typeCode
         * @param start_reg the virtual register to start allocation with
         * @param K the number of the virtual register starting at start_reg
         */
        private void doIncrementalGraphColoring_helper2(char type,
                int start_reg, int K, BitSet igraph,
                Vector resultWebSets) {
            Stack webStack = new Stack();

            if (start_reg + K > resultWebSets.size()) {
                resultWebSets.setSize(start_reg + K);
            }

	    BitSet colorSet = new BitSet();
            for (int i = 0; i < K; i++) {
                Integer li = new Integer(i + start_reg);
                colorSet.set(i + start_reg);
            }

            // Initialize scratch interference lists and reset virtual
            // registers in webs.
	    for (int i = igraph.nextSetBit(0);
		 i != -1;
		 i = igraph.nextSetBit(i + 1)) {
                Web w = (Web) webs.number2Web.get(i);
		w.copyInterferencesToScratchForSubset(igraph);
            }

            // SIMPLIFY and SPILL phases
	    while (igraph.nextSetBit(0) != -1) {
                Web simplifiable = findSimplifiableWeb(igraph, K);
                if (simplifiable != null) {
                    // SIMPLIFY
                    webStack.push(simplifiable);
                    igraph.clear(simplifiable.serialNumber);
                    // Remove interference edges
		    for (int i = igraph.nextSetBit(0);
			 i != -1;
			 i = igraph.nextSetBit(i + 1)) {
			Web w = (Web) webs.number2Web.get(i);
			w.removeScratchInterference(simplifiable);
                    }
                } else {
                    // SPILL
                    // search for the smallest spillcost/degree
                    Web potentialSpill = null;
		    for (int i = igraph.nextSetBit(0);
			 i != -1;
			 i = igraph.nextSetBit(i + 1)) {
			Web w = (Web) webs.number2Web.get(i);
                        if (potentialSpill == null) {
                            potentialSpill = w;
                        } else if ((w.getWeightedRefCount() * potentialSpill
                                .getScratchInterferences().cardinality()) < (potentialSpill
                                .getWeightedRefCount() * w
                                .getScratchInterferences().cardinality())) {
                            potentialSpill = w;
                        }
                    }
                    Assume
                            .that(potentialSpill != null
                                    && potentialSpill.getScratchInterferences()
                                            .cardinality() >= K);

                    webStack.push(potentialSpill);
                    igraph.clear(potentialSpill.serialNumber);
                    // Remove interference edges
		    for (int i = igraph.nextSetBit(0);
			 i != -1;
			 i = igraph.nextSetBit(i + 1)) {
			Web w = (Web) webs.number2Web.get(i);
			w.removeScratchInterference(potentialSpill);
                    }
                }
            }

            // SELECT phase
            while (!webStack.empty()) {
                Web w = (Web) webStack.pop();
                igraph.set(w.serialNumber);
		for (int i = igraph.nextSetBit(0);
		     i != -1;
		     i = igraph.nextSetBit(i + 1)) {
		    Web wi = (Web) webs.number2Web.get(i);
		    wi.copyInterferencesToScratchForSubset(igraph);
                }
                BitSet scratchInterferers = w.getScratchInterferences();

                BitSet availColorSet = (BitSet) colorSet.clone();
		for (int i = scratchInterferers.nextSetBit(0);
		     i != -1;
		     i = scratchInterferers.nextSetBit(i + 1)) {
		    Web wi = (Web) webs.number2Web.get(i);
                    int li = wi.getScratchColor();
                    if (li != -1)
                        availColorSet.clear(li);
                }

                if (availColorSet.cardinality() > 0) { // found a color
		    int li = availColorSet.nextSetBit(0);
		    availColorSet.clear(li);
                    w.setScratchColor(li);

                    // Put the mapping from the web to the register into
                    // the hash map
                    Web.Set li_wset = (Web.Set) resultWebSets.get(li);
                    if (li_wset == null) {
                        li_wset = new Web.Set(w.getType());
                        resultWebSets.set(li, li_wset);
                    }
                    li_wset.add(w);
                }
                // keep going even if spill heuristic fails
            }

            // remove webs with colors from igraph
            BitSet toRemove = new BitSet();
	    for (int i = igraph.nextSetBit(0);
		 i != -1;
		 i = igraph.nextSetBit(i + 1)) {
		Web w = (Web) webs.number2Web.get(i);
                if (w.getScratchColor() != -1)
                    toRemove.set(w.serialNumber);
            }
	    for (int i = toRemove.nextSetBit(0);
		 i != -1;
		 i = toRemove.nextSetBit(i + 1)) {
		igraph.clear(i);
            }
        }

        private void doExactGraphColoring() {
            doExactGraphColoring_helper(TypeCodes.REFERENCE);
            doExactGraphColoring_helper(TypeCodes.INT);
            doExactGraphColoring_helper(TypeCodes.LONG);
            doExactGraphColoring_helper(TypeCodes.FLOAT);
            doExactGraphColoring_helper(TypeCodes.DOUBLE);
        }

        private void doExactGraphColoring_helper(char t) {
            boolean success = false;
            boolean success_ever = false;
            Vector ws = getWebSet(t);

            int nweb = ws.size() + 1; // for a complete interference graph, this
            // is needed...

            //BasicIO.out.println(toTypeSignature(t));

            Vector lastSuccessWebSets = null;
            if (nweb < 4) { // linear search
                for (int i = nweb; i >= 0; i--) {
                    Vector webSets = new Vector();
                    webSets.setSize(nweb);
                    success = doExactGraphColoring_helper2(ws, t, i, webSets);
                    if (success)
                        lastSuccessWebSets = webSets;
                    success_ever |= success;
                    if (!success)
                        break;
                }
            } else { // binary search
                int high = nweb;
                int low = 0;
                while (high - low > 1) {
                    int medium = (high + low) / 2;
                    //BasicIO.out.println("binarysearch : " + medium);
                    Vector webSets = new Vector();
                    webSets.setSize(nweb);
                    success = doExactGraphColoring_helper2(ws, t, medium,
                            webSets);
                    if (success)
                        lastSuccessWebSets = webSets;
                    success_ever |= success;
                    if (success)
                        high = medium;
                    else
                        low = medium;
                }
            }

            Assume.that(success_ever, "Register allocation failed");

            Vector wss = getWebSetSet(t);
            sortAndStoreWebSets(lastSuccessWebSets, wss);
        }

        /**
         * Apply no-spill graph coloring register allocation based on optimistic
         * coloring with K virtual registers of the given type.
         * 
         * @param t
         *            the type
         * @param K
         *            the number of given virtual registers
         * @param resultWebSets -
         *            a vector of Web.Set's. This is the result of the register
         *            allocation.
         * @return true if the allocation performed without spills, false
         *         otherwise.
         */
        private boolean doExactGraphColoring_helper2(Vector ws, char t,
                final int K, Vector resultWebSets) {
            Stack webStack = new Stack();

            BitSet colorSet = new BitSet();
            for (int i = 0; i < K; i++) {
                colorSet.set(i);
            }

            BitSet igraph = new BitSet();
	    for(int i = 0; i < ws.size(); i++) {
		Web w = (Web) ws.get(i);
		igraph.set(w.serialNumber);
	    }

            // Initialize scratch interference lists and reset virtual
            // registers in webs.
	    for (int i = igraph.nextSetBit(0);
		 i != -1;
		 i = igraph.nextSetBit(i + 1)) {
                Web w = (Web) webs.number2Web.get(i);
		w.copyInterferencesToScratch();
		w.setScratchColor(-1);
            }

            // SIMPLIFY and SPILL phases

            while (igraph.nextSetBit(0) != -1) {
                Web simplifiable = findSimplifiableWeb(igraph, K);
                if (simplifiable != null) {
                    //BasicIO.out.println("SIMPLIFY : " + igraph.cardinality());
                    // SIMPLIFY
                    //simplifiable.setPotentialSpillFlag(false);
                    webStack.push(simplifiable);
                    igraph.clear(simplifiable.serialNumber);
                    // Remove interference edges
		    for (int i = igraph.nextSetBit(0);
			 i != -1;
			 i = igraph.nextSetBit(i + 1)) {
			Web w = (Web) webs.number2Web.get(i);
			w.removeScratchInterference(simplifiable);
                    }
                } else {
                    //BasicIO.out.println("SPILL : " + igraph.cardinality());
                    // SPILL
                    // search for the smallest spillcost/degree
                    Web potentialSpill = null;
		    for (int i = igraph.nextSetBit(0);
			 i != -1;
			 i = igraph.nextSetBit(i + 1)) {
			Web w = (Web) webs.number2Web.get(i);
                        if (potentialSpill == null) {
                            potentialSpill = w;
                        } else if ((w.getWeightedRefCount() * potentialSpill
                                .getScratchInterferences().cardinality()) < (potentialSpill
                                .getWeightedRefCount() * w
                                .getScratchInterferences().cardinality())) {
                            potentialSpill = w;
                        }
                    }
                    Assume
                            .that(potentialSpill != null
                                    && potentialSpill.getScratchInterferences()
                                            .cardinality() >= K);

                    webStack.push(potentialSpill);
                    igraph.clear(potentialSpill.serialNumber);
                    // Remove interference edges
		    for (int i = igraph.nextSetBit(0);
			 i != -1;
			 i = igraph.nextSetBit(i + 1)) {
			Web w = (Web) webs.number2Web.get(i);
			w.removeScratchInterference(potentialSpill);
                    }
                }
            }

            // SELECT phase
            while (!webStack.empty()) {
                //BasicIO.out.println("SELECT : " + webStack.size());
                Web w = (Web) webStack.pop();
                igraph.set(w.serialNumber);
		for (int i = igraph.nextSetBit(0);
		     i != -1;
		     i = igraph.nextSetBit(i + 1)) {
		    Web wi = (Web) webs.number2Web.get(i);
		    wi.copyInterferencesToScratchForSubset(igraph);
                }
                BitSet scratchInterferers = w.getScratchInterferences();

                BitSet availColorSet = (BitSet) colorSet.clone();
		for (int i = scratchInterferers.nextSetBit(0);
		     i != -1;
		     i = scratchInterferers.nextSetBit(i + 1)) {
		    Web wi = (Web) webs.number2Web.get(i);
                    int li = wi.getScratchColor();
                    if (li != -1)
                        availColorSet.clear(li);
                }

                if (availColorSet.cardinality() > 0) { // found a color
		    int li = availColorSet.nextSetBit(0);
		    availColorSet.clear(li);
                    w.setScratchColor(li);

                    // Put the mapping from the web to the register into
                    // the hash map
                    Web.Set li_wset = (Web.Set) resultWebSets
                            .get(li);
                    if (li_wset == null) {
                        li_wset = new Web.Set(w.getType());
                        resultWebSets.set(li, li_wset);
                    }
                    li_wset.add(w);
                } else { // actual spill
                    return false;
                }
            }

            // Allocation succeeded
            return true;
        }

        private void doGreedyPacking() {
            doGreedyPacking_helper(TypeCodes.REFERENCE);
            doGreedyPacking_helper(TypeCodes.INT);
            doGreedyPacking_helper(TypeCodes.LONG);
            doGreedyPacking_helper(TypeCodes.FLOAT);
            doGreedyPacking_helper(TypeCodes.DOUBLE);
        }

        private void doGreedyPacking_helper(char t) {
            Vector ws = getWebSet(t);
            Web[] webs = new Web[ws.size()];
            ws.toArray(webs);
            sortWebs(webs);
            Vector vwss = new Vector();

            // Pack (aggresive) O(n^2)
            for (int i = webs.length - 1; i >= 0; i--) {
                Web wi = webs[i];
                if (wi == null)
                    continue;
                Web.Set webSet = new Web.Set(t);
                webSet.add(wi);

                for (int j = i - 1; j >= 0; j--) {
                    Web wj = webs[j];
                    if (wj == null)
                        continue;
                    if (!webSet.getInterferences().get(wj.serialNumber)) {
                        //d("merging " + wi + " and " + wj);
                        webSet.add(wj);
                        webs[j] = null;
                    }
                }
                vwss.add(webSet);
            }

            Vector wss = getWebSetSet(t);
            sortAndStoreWebSets(vwss, wss);
        }

        private void doLinearPacking() {
            doLinearPacking_helper(TypeCodes.REFERENCE);
            doLinearPacking_helper(TypeCodes.INT);
            doLinearPacking_helper(TypeCodes.LONG);
            doLinearPacking_helper(TypeCodes.FLOAT);
            doLinearPacking_helper(TypeCodes.DOUBLE);
        }

        private void doLinearPacking_helper(char t) {
            Vector ws = getWebSet(t);
            Web[] webs = new Web[ws.size()];
            ws.toArray(webs);
            sortWebs(webs);
            Vector vwss = new Vector();
            // Pack (contiguous) O(n)
            {
                int i = webs.length - 1;
                while (i >= 0) {
                    Web wi = webs[i];
                    Web.Set webSet = new Web.Set(t);
                    webSet.add(wi);

                    int j = i - 1;
                    while (j >= 0) {
                        Web wj = webs[j];
                        if (!webSet.getInterferences().get(wj.serialNumber)) {
                            webSet.add(wj);
                        } else {
                            break;
                        }
                        j--;
                    }
                    vwss.add(webSet);
                    i = j;
                }
            }

            Vector wss = getWebSetSet(t);
            sortAndStoreWebSets(vwss, wss);
        }

    }

    private static class DefUseBitSet {
	final BitSet uses;
	final BitSet defs;
	private DefUseBitSet(BitSet d, BitSet u) {
	    defs = d;
	    uses = u;
	}
    }

    private static class Webs {
	private BitSet webBitSet;
	private ArrayList number2Web;
	private IdentityHashSet webs;
	private Webs() {
	    webBitSet = new BitSet();
	    number2Web = new ArrayList();
	    webs = new IdentityHashSet();
	    Web.reset();
	}
	private Web getWeb(int number) {
	    return (Web) number2Web.get(number);
	}
	private void removeWeb(int number) {
	    webs.remove(number2Web.get(number));
	    number2Web.set(number, null);
	    webBitSet.clear(number);
	}
	private Web makeWeb(char t) {
	    Web w = new Web(t);
	    ensureSize(number2Web, w.serialNumber + 1);
	    number2Web.set(w.serialNumber, w);
	    webBitSet.set(w.serialNumber);
	    webs.add(w);
	    return w;
	}
    }

    /**
     * Web. A web is a union of intersecting Def's and Use's.
     */
    private static class Web {
	private static int webCounter = 1;
	private static void reset() {
	    webCounter = 1;
	}

	final int serialNumber;

        private DefUseBitSet defUses;

        private char type;

        private int weightedRefCount;

        private int refCount;

        private BitSet interferences;

        private int localIndex;

        // Used by graph coloring allocators
        private int scratch_color;

        private BitSet scratch_interferences;

        private Web(char t) {
	    serialNumber = webCounter++;
            type = t;
            interferences = new BitSet();
            localIndex = -1;
            scratch_color = -1;
        }

        char getType() {
            return type;
        }

        void copyInterferencesToScratch() {
            scratch_interferences = (BitSet) interferences.clone();
        }

        BitSet getScratchInterferences() {
            return scratch_interferences;
        }

        void removeScratchInterference(Web w) {
            scratch_interferences.clear(w.serialNumber);
        }

        void setScratchColor(int li) {
            scratch_color = li;
        }

        int getScratchColor() {
            return scratch_color;
        }

        /**
         * Copy the original interefer set to the scatch interefer
         * set, but only copy the interfering webs in the given web
         * set. Used by the graph coloring allocator.
         */
        public void copyInterferencesToScratchForSubset(BitSet webs) {
            scratch_interferences = (BitSet) webs.clone();
	    scratch_interferences.and(interferences);
	}

        void setDefUses(DefUseBitSet du) {
            defUses = du;
        }

        void setRefCount(int rc) {
            refCount = rc;
        }

        void setWeightedRefCount(int wrc) {
            weightedRefCount = wrc;
        }

        int getWeightedRefCount() {
            return weightedRefCount;
        }

        public String toString() {
            return "W" + serialNumber + type
                    + (localIndex != -1 ? localIndex + "" : "");
        }

        public DefUseBitSet getDefUses() {
            return defUses;
        }

        BitSet getInterferences() {
            return interferences;
        }

        void setLocalIndex(int li) {
            localIndex = li;
        }

        int getLocalIndex() {
            return localIndex;
        }

        private static class Set {
	    private IdentityHashSet internal;

            private char type;

            private int localIndex;

            public Set(char type) {
                internal = new IdentityHashSet();
                this.type = type;
                localIndex = -1;
            }

            public void add(Web w) {
                if (w.getType() != type)
                    throw new Error();
                internal.add(w);
            }

            public int size() {
                return internal.size();
            }

            public Iterator iterator() {
                return internal.iterator();
            }

            public boolean contains(Web w) {
                return internal.contains(w);
            }

            public int getWeightedRefCount() {
                int rc = 0;
                for (Iterator it = internal.iterator(); it.hasNext();) {
                    Web w = (Web) it.next();
                    rc += w.getWeightedRefCount();
                }
                return rc;
            }

            public char getType() {
                return type;
            }

            public void setLocalIndex(int li) {
                localIndex = li;
                for (Iterator it0 = internal.iterator(); it0.hasNext();) {
                    Web w0 = (Web) it0.next();
                    w0.setLocalIndex(li);
                }
            }

            public int getLocalIndex() {
                return localIndex;
            }

            public String toString() {
                return "WebSet[WRC=" + getWeightedRefCount() + "," + "LI="
                        + localIndex + "," + internal.toString() + "]";
            }

            public BitSet getInterferences() {
                BitSet interfererSet = new BitSet();
                for (Iterator it0 = internal.iterator(); it0.hasNext();) {
                    Web w0 = (Web) it0.next();
                    BitSet interferers = w0.getInterferences();
                    interfererSet.or(interferers);
                }
                return interfererSet;
            }

            public String getInterferersAsLocalsAsString(Webs webs) {
                IdentityHashSet interferersLocalSet = getInterferersAsLocals(webs);
                String interferersLocalList = "{ ";
                for (Iterator it = interferersLocalSet.iterator(); it.hasNext();) {
                    Integer l = (Integer) it.next();
                    interferersLocalList += l.intValue() + " ";
                }
                interferersLocalList += "}";
                return interferersLocalList;
            }

            public IdentityHashSet getInterferersAsLocals(Webs webs) {
                IdentityHashSet interferersLocalSet = new IdentityHashSet();
                for (Iterator it0 = internal.iterator(); it0.hasNext();) {
                    Web w0 = (Web) it0.next();
                    BitSet interferers = w0.getInterferences();
		    for (int i = interferers.nextSetBit(0);
			 i != -1;
			 i = interferers.nextSetBit(i + 1)) {
                        Web w1 = (Web) webs.number2Web.get(i);
                        interferersLocalSet
                                .add(new Integer(w1.getLocalIndex()));
                    }
                }
                return interferersLocalSet;
            }

            public String getInterferersAsString() {
                BitSet interfererSet = getInterferences();
                String interferersList = "{ ";
		for(int i = interfererSet.nextSetBit(0);
		    i != -1;
		    i = interfererSet.nextSetBit(i + 1)) {
                    interferersList += i + " ";
                }
                interferersList += "}";
                return interferersList;
            }
        }

    }

    /** A super class of Def and Use */
    private static abstract class Operand {
	private static int opCounter = 1;
	static void resetOpCounter() {
	    opCounter = 1;
	}

	final int serialNumber;

        BitSet chains;

        InstructionHandle ih;

        int loopDepth;

        boolean isReachable;

        Web web;

        BitSet interferences;

	final int localIndex;

        Operand(InstructionHandle d, int ld, boolean isReachable) {
	    serialNumber = opCounter++;
            ih = d;
            loopDepth = ld;
            chains = new BitSet();
            interferences = new BitSet();
            this.isReachable = isReachable;
	    localIndex = ((LocalAccess) ih.getInstruction()).getLocalVariableOffset();
        }

        InstructionHandle getInstructionHandle() {
            return ih;
        }

        public boolean isReachable() {
            return isReachable;
        }

        public BitSet interferences() {
            return interferences;
        }

        public void setWeb(Web w) {
            web = w;
        }

        public Web getWeb() {
            return web;
        }

        public int getLoopDepth() {
            return loopDepth;
        }

        public BitSet chains() {
            return chains;
        }

        public char getType() {
            LocalAccess lvi = (LocalAccess) ih.getInstruction();
            return lvi.getTypeCode();
        }

        public String toString() {
            LocalAccess lvi = (LocalAccess) ih
                    .getInstruction();
            char typeString = lvi.getTypeCode();
            String UorD = null;
            if (this instanceof Def)
                UorD = "D";
            else if (this instanceof Use)
                UorD = "U";
            else
                throw new Error();
            return lvi.getLocalVariableOffset() + typeString + UorD + ih.getPosition();
        }
    }

    /** The set of Def */
    private static class DefSet {
	// map from Operand.serialNumber to Def
	ArrayList number2Def;

        /** The map from InstructionHandles for LocalWrites to Def objects */
        private IdentityHashMap storeIH2d;

        /** The set of all Def objects */
        private IdentityHashSet defs;

        private IdentityHashSet getDefSet() {
            return defs;
        }

        private DefSet() {
            storeIH2d = new IdentityHashMap();
	    number2Def = new ArrayList();
            defs = new IdentityHashSet();
        }

	private Def getDef(int number) {
            Def p = (Def) number2Def.get(number);
            return p;
	}

        private Def getDef(InstructionHandle ih) {
            Def p = (Def) storeIH2d.get(ih);
            Assume.that(p != null);
            return p;
        }

        private Def makeDef(InstructionHandle ih, 
			    int loopDepth,
			    boolean isReachable) {
            Assume.that(storeIH2d.get(ih) == null);
            Def d = new Def(ih, 
			    loopDepth, isReachable);
            storeIH2d.put(ih, d);
	    ensureSize(number2Def, d.serialNumber + 1);
	    number2Def.set(d.serialNumber, d);
            defs.add(d);
            return d;
        }

        private Def makeOrGetDef(InstructionHandle ih, 
				 int loopDepth,
				 boolean isReachable) {
            Def p = (Def) storeIH2d.get(ih);
            if (p != null) {
                Assume.that(loopDepth == p.loopDepth);
                return p;
            } else {
                Def d = new Def(ih, loopDepth, isReachable);
                storeIH2d.put(ih, d);
		ensureSize(number2Def, d.serialNumber + 1);
		number2Def.set(d.serialNumber, d);
                defs.add(d);
                return d;
            }
        }

    }

    /** A definition point - wrapper for a store instruction handle. */
    private static class Def extends Operand {

        private BitSet uses() {
            return chains;
        }

        private Def(InstructionHandle d, int ld, boolean isReachable) {
            super(d, ld, isReachable);
            Assume.that(d.getInstruction() instanceof LocalWrite);
        }
    }

    /** The set of Uses */
    private static class UseSet {
	// map from Operand.serialNumber to Use
	private ArrayList number2Use;

        private UseSet() {
            loadIH2u = new IdentityHashMap();
	    number2Use = new ArrayList();
            uses = new IdentityHashSet();
        }

        /** The map from InstructionHandles for LocalReads to Use objects */
        private IdentityHashMap loadIH2u;

        /** The set of all Use objects */
        private IdentityHashSet uses;

        private IdentityHashSet getUseSet() {
            return uses;
        }

        private Use getUse(int number) {
            Use p = (Use) number2Use.get(number);
            return p;
        }

        private Use getUse(InstructionHandle ih) {
            Use p = (Use) loadIH2u.get(ih);
            Assume.that(p != null);
            return p;
        }

        private Use makeUse(InstructionHandle ih, int loopDepth,
                boolean isReachable) {
            Assume.that(loadIH2u.get(ih) == null);
            Use u = new Use(ih, loopDepth, isReachable);
            loadIH2u.put(ih, u);
	    ensureSize(number2Use, u.serialNumber + 1);
	    number2Use.set(u.serialNumber, u);
            uses.add(u);
            return u;
        }

        private Use makeOrGetUse(InstructionHandle ih, int loopDepth,
                boolean isReachable) {
            Use p = (Use) loadIH2u.get(ih);
            if (p != null) {
                Assume.that(loopDepth == p.loopDepth);
                return p;
            } else {
                Use u = new Use(ih, loopDepth, isReachable);
                loadIH2u.put(ih, u);
		ensureSize(number2Use, u.serialNumber + 1);
		number2Use.set(u.serialNumber, u);
                uses.add(u);
                return u;
            }
        }

    }

    /** A use point - wrapper for a load instruction handle. */
    private static class Use extends Operand {
        private BitSet defs() {
            return chains;
        }

        private Use(InstructionHandle u, int ld, boolean isReachable) {
            super(u, ld, isReachable);
            Assume.that(u.getInstruction() instanceof LocalRead);
        }
    }

    /**
     * Used by shiftLocals()
     */
    private static class ShiftLocalsVisitor extends Visitor {
        ByteCodeGen mg;

        InstructionList il;

        InstructionHandle ih;

        final int maxLocals;

        final int maxStack;

        int newMaxLocals;

        HashMap p2nCache;

        Vector argTypeList;

        int argSize;

        public ShiftLocalsVisitor(ByteCodeGen mg) {
            this.mg = mg;
            this.il = mg.getInstructionList();
            maxLocals = mg.getMaxLocals();
            maxStack = mg.getMaxStack();
            newMaxLocals = maxLocals;
            this.p2nCache = new HashMap();
            argTypeList = new Vector();
        }

        /**
         * Convert PC to the instruction number
         */
        private int p2n(int pos) {
            Integer cache = (Integer) p2nCache.get(new Integer(pos));
            if (cache != null) {
                return cache.intValue();
            } else {
                int[] instPositions = il.getInstructionPositions();
                for (int i = 0; i < instPositions.length; i++) {
                    if (instPositions[i] == pos) {
                        p2nCache.put(new Integer(pos), new Integer(i));
                        return i;
                    }
                }
            }
            throw new Error();
        }

        public void run() {
            char[] argTypes = mg.getArgumentTypes();
            //if (!mg.isStatic()) {
                argTypeList.add(new Character(TypeCodes.REFERENCE));
            //}
            for (int i = 0; i < argTypes.length; i++) {
                char t = toBasicType(argTypes[i]);
                if (typeCode2Size(t) == 1) {
                    argTypeList.add(new Character(t));
                } else if (typeCode2Size(t) == 2) {
                    argTypeList.add(new Character(t));
                    argTypeList.add(new Character(toUpperHalf(t)));
                } else
                    throw new Error();
            }

            argSize = argTypeList.size();

            ih = il.getStart();
            while (ih != null) {
                InstructionHandle next = ih.getNext();
                ih.accept(this);
                ih = next;
            }

            InstructionHandle orgStart = il.getStart();
            int localIndex = 0;
            int maxSize = 0;
            //if (!mg.isStatic()) {
                il.insert(orgStart, LocalRead.make(TypeCodes.REFERENCE,
                        0));
                il.insert(orgStart, LocalWrite.make(TypeCodes.REFERENCE,
                        argSize));
                localIndex++;
                maxSize = 1;
            //}
            for (int i = 0; i < argTypes.length; i++) {
                char t = toBasicType(argTypes[i]);
                il.insert(orgStart, LocalRead.make
                        (t, localIndex));
                il.insert(orgStart, LocalWrite.make(t,
                        localIndex + argSize));
                int size = typeCode2Size(t);
                localIndex += size;
                if (maxSize < size)
                    maxSize = size;
            }

            mg.setMaxLocals(maxLocals + argSize);
            if (maxSize > maxStack)
                mg.setMaxStack(maxSize);
            mg.getByteCode();
        }

        public void visit(LocalWrite o) {
	    if (argSize > 0) {
		ih.setInstruction(LocalWrite.make(o.getTypeCode(), o.getLocalVariableOffset() + argSize));
	    }
	}

        public void visit(LocalRead o) {
	    if (argSize > 0) {
		ih.setInstruction(LocalRead.make(o.getTypeCode(), o.getLocalVariableOffset() + argSize));
	    }
	}
    }

    /**
     * Used by monoTypeLocals()
     */
    private static class MonoTypeLocalsVisitor extends Visitor {
        ByteCodeGen mg;

        InstructionList il;

        InstructionHandle ih;

        final int maxLocals;

        int newMaxLocals;

        HashMap p2nCache;

        Vector localTypeList;

        /**
         * A map from locals (original index, type) to a new index
         */
        HashMap local2NewIndex;

        private static class Local {
            int oldIndex;

            char type;

            Local(int oi, char t) {
                oldIndex = oi;
                type = t;
            }

            public int hashCode() {
                return oldIndex + (int)type;
            }

            public boolean equals(Object o) {
                if (o == null || !(o instanceof Local))
                    return false;
                Local other = (Local) o;
                return oldIndex == other.oldIndex && type == other.type;
            }
        }

        public MonoTypeLocalsVisitor(ByteCodeGen mg) {
            this.mg = mg;
            this.il = mg.getInstructionList();
            maxLocals = mg.getMaxLocals();
            newMaxLocals = maxLocals;
            this.p2nCache = new HashMap();
            local2NewIndex = new HashMap();
            localTypeList = new Vector();

        }

        /**
         * Convert PC to the instruction number
         */
        private int p2n(int pos) {
            Integer cache = (Integer) p2nCache.get(new Integer(pos));
            if (cache != null) {
                return cache.intValue();
            } else {
                int[] instPositions = il.getInstructionPositions();
                for (int i = 0; i < instPositions.length; i++) {
                    if (instPositions[i] == pos) {
                        p2nCache.put(new Integer(pos), new Integer(i));
                        return i;
                    }
                }
            }
            throw new Error();
        }

        public Vector getLocalTypeList() {
            return localTypeList;
        }

        public void run() {
            char[] argTypes = mg.getArgumentTypes();
            //if (!mg.isStatic()) {
                localTypeList.add(new Character(TypeCodes.REFERENCE));
            //}
            for (int i = 0; i < argTypes.length; i++) {
                char t = toBasicType(argTypes[i]);
                if (typeCode2Size(t) == 1) {
                    localTypeList.add(new Character(t));
                } else if (typeCode2Size(t) == 2) {
                    localTypeList.add(new Character(t));
                    localTypeList.add(new Character(toUpperHalf(t)));
                } else
                    throw new Error();
            }

            // here argList contains the local types for arguments
            // now put them into local2NewIndex
            for (int i = 0; i < localTypeList.size(); i++) {
                local2NewIndex.put(new Local(i, ((Character)localTypeList.get(i)).charValue()),
                        new Integer(i));
            }

            localTypeList.setSize(maxLocals);

            ih = il.getStart();
            while (ih != null) {
                InstructionHandle next = ih.getNext();
                ih.accept(this);
                ih = next;
            }

            mg.setMaxLocals(newMaxLocals);
            mg.getByteCode();
        }

        // Loads, Stores
        public void visit(LocalAccess o) {
	    Assume.that(o instanceof LocalRead || o instanceof LocalWrite);
	    boolean isRead = o instanceof LocalRead;
            char type = toBasicType(o.getTypeCode());
            int index = o.getLocalVariableOffset();
            Integer newIndexObj = (Integer) local2NewIndex.get(new Local(index,
                    type));
            if (newIndexObj != null) { // arguments or the ones that are already
                // met
                int newIndex = newIndexObj.intValue();
                if (index != newIndex) {
		    ih.setInstruction(isRead ? 
			(LocalAccess)LocalRead.make(o.getTypeCode(), newIndex) :
			(LocalAccess)LocalWrite.make(o.getTypeCode(), newIndex));
                }
            } else {
                int newIndex = newMaxLocals;
                if (typeCode2Size(type) == 1) {
                    Character ot = (Character)localTypeList.get(index);
                    if (ot == null) {
                        localTypeList.set(index, new Character(type));
                        local2NewIndex.put(new Local(index, type), new Integer(
                                index));
                    } else if (ot.charValue() == type) {
                        ;
                    } else {
                        newMaxLocals++;
                        localTypeList.setSize(newMaxLocals);
                        localTypeList.set(newIndex, new Character(type));
                        local2NewIndex.put(new Local(index, type), new Integer(
                                newIndex));
		    ih.setInstruction(isRead ? 
			(LocalAccess)LocalRead.make(o.getTypeCode(), newIndex) :
			(LocalAccess)LocalWrite.make(o.getTypeCode(), newIndex));
                    }
                } else if (typeCode2Size(type) == 2) {
                    Character ot = (Character)localTypeList.get(index);
                    Character ot2 = (Character)localTypeList.get(index + 1);
		    // ot2 need not be null, maybe this is the first
		    // use of ot and the second use of ot2.  (This can
		    // easily happen if the inliner previously
		    // assigned ot's slot to a shared state, skipped
		    // the shared state store.)
                    if (ot == null /*&& ot2 == null*/) {
                        localTypeList.set(index, new Character(type));
                        local2NewIndex.put(new Local(index, type), new Integer(
                                index));
                        localTypeList.set(index + 1, new Character(toUpperHalf(type)));
                        local2NewIndex.put(new Local(index + 1,
                                toUpperHalf(type)), new Integer(index + 1));
                    } else if (ot.charValue() == type) {
                        ;
                    } else {
                        newMaxLocals += 2;
                        localTypeList.setSize(newMaxLocals);
                        localTypeList.set(newIndex, new Character(type));
                        localTypeList.set(newIndex + 1, new Character(toUpperHalf(type)));
                        local2NewIndex.put(new Local(index, type), new Integer(
                                newIndex));
                        local2NewIndex.put(new Local(index + 1,
                                toUpperHalf(type)), new Integer(newIndex + 1));
		    ih.setInstruction(isRead ? 
			(LocalAccess)LocalRead.make(o.getTypeCode(), newIndex) :
			(LocalAccess)LocalWrite.make(o.getTypeCode(), newIndex));
                    }
                } else
                    throw new Error();
            }
        }

    }

    /**
     * Insert a store after a value is produced and a load before the value is
     * consumed.
     * 
     * e.g.
     * 
     * ICONST_1 ICONST_2 IADD
     *  =>
     * 
     * ICONST_1 ISTORE n ICONST_2 ISTORE n+1 ILOAD n ILOAD n+1 IADD
     */
    private static class StoreAllIntermediateValuesOnceVisitor extends Visitor {
        ByteCodeGen mg;

        InstructionList il;

        InstructionHandle ih;

        final int maxLocals;

        int newMaxLocals;

        AbstractState[] abstractStates;

        HashMap p2nCache;

        Constants cp;
        
        /**
         * Rough live ranges of sythetic local variables.
         */
        Vector localRanges;

        public StoreAllIntermediateValuesOnceVisitor(ByteCodeGen mg) {
            this.mg = mg;
            this.il = mg.getInstructionList();
            maxLocals = mg.getMaxLocals();
            newMaxLocals = maxLocals;
            this.p2nCache = new HashMap();
            localRanges = new Vector();
            this.cp = mg.getConstantPool();
        }

        private static class LocalRange {
            int localNumber;

            int start;

            int end;

            LocalRange(int n, int s, int e) {
                Assume.that(s <= e);
                start = s;
                end = e;
                localNumber = n;
            }

            boolean in(int pc) {
                return start <= pc && pc <= end;
            }

            boolean doesNotOverlap(int from, int to) {
                Assume.that(from <= to);
                return to < start || end < from;
            }
        }

        private int pickFreeSyntheticLocal(int from_pc, int to_pc, char t) {
            if (typeCode2Size(t) == 1) {
                for (int i = maxLocals; i < newMaxLocals; i++) {
                    boolean i_is_free = true;
                    for (int j = 0; j < localRanges.size(); j++) {
                        LocalRange r = (LocalRange) localRanges.get(j);
                        if (i == r.localNumber
                                && !r.doesNotOverlap(from_pc, to_pc)) {
                            i_is_free = false;
                            break;
                        }
                    }
                    if (i_is_free) {
                        localRanges.add(new LocalRange(i, from_pc, to_pc));
                        return i;
                    }
                }
            } else if (typeCode2Size(t) == 2) {
                for (int i = maxLocals; i < newMaxLocals - 1; i++) {
                    boolean i_is_free = true;
                    boolean i_plus_one_is_free = true;
                    for (int j = 0; j < localRanges.size(); j++) {
                        LocalRange r = (LocalRange) localRanges.get(j);
                        if (i == r.localNumber
                                && !r.doesNotOverlap(from_pc, to_pc)) {
                            i_is_free = false;
                            break;
                        }
                        if (i + 1 == r.localNumber
                                && !r.doesNotOverlap(from_pc, to_pc)) {
                            i_plus_one_is_free = false;
                            break;
                        }
                    }
                    if (i_is_free && i_plus_one_is_free) {
                        localRanges.add(new LocalRange(i, from_pc, to_pc));
                        localRanges.add(new LocalRange(i + 1, from_pc, to_pc));
                        return i;
                    }
                }
            } else
                throw new Error();

            // increase the number of locals
            int newIndex = newMaxLocals;
            newMaxLocals += typeCode2Size(t);
            if (typeCode2Size(t) == 1) {
                localRanges.add(new LocalRange(newIndex, from_pc, to_pc));
            } else if (typeCode2Size(t) == 2) {
                localRanges.add(new LocalRange(newIndex, from_pc, to_pc));
                localRanges.add(new LocalRange(newIndex + 1, from_pc, to_pc));
            } else
                throw new Error();
            return newIndex;
        }

        /**
         * Convert PC to the instruction number
         */
        private int p2n(int pos) {
            Integer cache = (Integer) p2nCache.get(new Integer(pos));
            if (cache != null) {
                return cache.intValue();
            } else {
                int[] instPositions = il.getInstructionPositions();
                for (int i = 0; i < instPositions.length; i++) {
                    if (instPositions[i] == pos) {
                        p2nCache.put(new Integer(pos), new Integer(i));
                        return i;
                    }
                }
            }
            throw new Error();
        }

        public void run() {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            abstractStates = v.getAbstractStates();

            ih = il.getStart();
            while (ih != null) {
                InstructionHandle next = ih.getNext();
                if (abstractStates[p2n(ih.getPosition())] != null)
                    ih.accept(this);
                ih = next;
            }

            mg.setMaxLocals(newMaxLocals);
            mg.getByteCode();
	    //il.setPositions();
        }

        private void processInputs(Instruction o) {
            SpecificationIR.Value[] stackIns = o.stackIns;
            Vector v = new Vector();
            for(int i = 0; i < stackIns.length; i++) {
                if (stackIns[i] instanceof SpecificationIR.SecondHalf)
                    continue;
                v.add(new Character(specValue2TypeCode(stackIns[i])));
            }
            char[] inputs = new char[v.size()];
            for(int i = 0; i < inputs.length; i++) {
                inputs[i] = ((Character)v.get(i)).charValue();
            }
            processInputs(o, inputs);
        }
        
        private void processInputs(Instruction o, char[] inputs) {
            AbstractState as = abstractStates[p2n(ih.getPosition())];
            char[] stack = as.stack;
            InstructionHandle[] producers = as.producers;
            //Assume.that(o.consumeStack(cp) > 0, "unexpected " + o);
            /*
             * Looking at the last input (the top of the operand stack) first,
             * 
             * o: current instr, p: producer
             * 
             * if o is a store_n if there is no use or def of local n between o
             * and p, move o to right after p else insert store_x right after p
             * and insert load_x before o else if p is a load_m && there is no
             * def of local m between o and p, move p to before the consecutive
             * loads followed by o else insert store_x right after p and insert
             * load_x before the consecutive loads followed by o
             */
            int pi = producers.length - 1;
            for (int k = 0; k < inputs.length; k++) {
                InstructionHandle p = producers[pi];
                if (p == null) { // the exception object on the stack at the
                    // beginning of an exception handler
                    continue;
                }
                if (o instanceof LocalWrite) {
                    LocalWrite s = (LocalWrite) o;
                    char st = s.getTypeCode();
                    if (checkNoUseOrDefOfLocalBetween(st, s.getLocalVariableOffset(), p, ih)
                            && p == ih.getPrev()) {
                        //BasicIO.out.println("A");
                        // move o right after p
                        Assume.that(p.getNext() != null);
                        Assume.that(ih.getPrev() != null);
                        il.insert(p.getNext(), o);
                        InstructionHandle ihPrev = ih.getPrev();
                        try {
                            il.delete(ih);
                        } catch (TargetLostException e) {
                            InstructionHandle[] targets = e.getTargets();
                            for (int i = 0; i < targets.length; i++) {
                                InstructionTargeter[] targeters = targets[i]
                                        .getTargeters();
                                for (int j = 0; j < targeters.length; j++) {
                                    targeters[j].updateTarget(targets[i],
                                            ihPrev);
                                }
                            }
                        }
                    } else {
                        //BasicIO.out.println("B");
                        // insert store_x right after p and insert load_x before
                        // o
                        Assume.that(typeCode2Size(st) > 0);
                        int newIndex = pickFreeSyntheticLocal(p.getPosition(),
                                ih.getPosition(), st);
                        Assume.that(p.getNext() != null);
                        il.insert(p.getNext(), LocalWrite.make(
                                st, newIndex));
                        il.insert(ih, LocalRead.make(st,
                                newIndex));
                    }
                } else if (p.getInstruction() instanceof LocalRead
                        && checkNoDefOfLocalBetween(((LocalRead) p
                                .getInstruction()).getTypeCode(),
                                ((LocalRead) p.getInstruction())
                                        .getLocalVariableOffset(), p, ih)) {
                    if (!allLocalReadsBetween(p, ih)) {
                        //BasicIO.out.println("C");
                        LocalRead l = (LocalRead) p
                                .getInstruction();
                        // move p to before the consecutive loads followed by o
                        Assume.that(p.getNext() != null);
                        // search for the previous instruction of the
                        // consecutive laods followed by o
                        InstructionHandle insertionPoint = ih.getPrev();
                        while (insertionPoint.getInstruction() instanceof LocalRead)
                            insertionPoint = insertionPoint.getPrev();
                        il.insert(insertionPoint.getNext(), l);
                        InstructionHandle pNext = p.getNext();
                        try {
                            il.delete(p);
                        } catch (TargetLostException e) {
                            InstructionHandle[] targets = e.getTargets();
                            for (int i = 0; i < targets.length; i++) {
                                InstructionTargeter[] targeters = targets[i]
                                        .getTargeters();
                                for (int j = 0; j < targeters.length; j++) {
                                    targeters[j]
                                            .updateTarget(targets[i], pNext);
                                }
                            }
                        }
                    }
                } else {
                    //BasicIO.out.println("D");
                    // insert store_x right after p
                    // and insert load_x before the consecutive loads followed
                    // by o
                    char tos = inputs[k];
                    Assume.that(tos != TypeCodes.LONG_UPPER
                            && tos != TypeCodes.DOUBLE_UPPER);
                    int newIndex = pickFreeSyntheticLocal(p.getPosition(), ih
                            .getPosition(), tos);
                    Assume.that(p.getNext() != null);
                    il.insert(p.getNext(), LocalWrite.make(tos,
                            newIndex));
                    InstructionHandle insertionPoint = ih.getPrev();
                    while (insertionPoint.getInstruction() instanceof LocalRead)
                        insertionPoint = insertionPoint.getPrev();
                    il.insert(insertionPoint.getNext(), LocalRead
                            .make(tos, newIndex));
                }
                if (typeCode2Size(inputs[k]) == 2)
                    pi -= 2;
                else
                    pi--;
            }
        }

        /**
         * @param from
         *            (exclusive)
         * @param to
         *            (exclusive)
         * @return true if all the instructions between from and to are all load
         *         instructions Note from and to must be connected via IH links.
         */
        private boolean allLocalReadsBetween(InstructionHandle from,
                InstructionHandle to) {
            if (from == to)
                return true;
            InstructionHandle h = from.getNext();
            while (h != to) {
                Assume.that(h != null);
                if (!(h.getInstruction() instanceof LocalRead))
                    return false;
                h = h.getNext();
            }
            return true;
        }

        /**
         * @param t
         *            the type of local n
         * @param n
         *            local variable number
         * @param from
         *            (exclusive)
         * @param to
         *            (exclusive)
         * @return true if there is no use or def of local n between from and
         *         to. Note from and to must be connected via IH links.
         */
        private boolean checkNoUseOrDefOfLocalBetween(char t, int n,
                InstructionHandle from, InstructionHandle to) {
            if (from == to)
                return true;
            InstructionHandle h = from.getNext();
            while (h != to) {
                Assume.that(h != null);
                if (h.getInstruction() instanceof LocalAccess) {
                    LocalAccess lvi = (LocalAccess) h
                            .getInstruction();
                    char lt = lvi.getTypeCode();
                    int index = lvi.getLocalVariableOffset();
                    if (lt == TypeCodes.LONG || lt == TypeCodes.DOUBLE) {
                        if (t == TypeCodes.LONG || t == TypeCodes.DOUBLE) {
                            if (index == n || index + 1 == n || n + 1 == index)
                                return false;
                        } else {
                            if (index == n || index + 1 == n)
                                return false;
                        }
                    } else {
                        if (t == TypeCodes.LONG || t == TypeCodes.DOUBLE) {
                            if (index == n || n + 1 == index)
                                return false;
                        } else {
                            if (index == n)
                                return false;
                        }
                    }
                }
                h = h.getNext();
            }
            return true;
        }

        /**
         * @param t
         *            the type of local n
         * @param n
         *            local variable number
         * @param from
         *            (exclusive)
         * @param to
         *            (exclusive)
         * @return true if there is no def of local n between from and to. Note
         *         from and to must be connected via IH links.
         */
        private boolean checkNoDefOfLocalBetween(char t, int n,
                InstructionHandle from, InstructionHandle to) {
            if (from == to)
                return true;
            InstructionHandle h = from.getNext();
            while (h != to) {
                Assume.that(h != null);
                Instruction ins = h.getInstruction();
                if (ins instanceof LocalWrite) {
                    LocalAccess lvi = (LocalAccess) h
                            .getInstruction();
                    char lt = lvi.getTypeCode();
                    int index = lvi.getLocalVariableOffset();
                    if (lt == TypeCodes.LONG || lt == TypeCodes.DOUBLE) {
                        if (t == TypeCodes.LONG || t == TypeCodes.DOUBLE) {
                            if (index == n || index + 1 == n || n + 1 == index)
                                return false;
                        } else {
                            if (index == n || index + 1 == n)
                                return false;
                        }
                    } else {
                        if (t == TypeCodes.LONG || t == TypeCodes.DOUBLE) {
                            if (index == n || n + 1 == index)
                                return false;
                        } else {
                            if (index == n)
                                return false;
                        }
                    }
                }
                h = h.getNext();
            }
            return true;
        }


	// Note:  This method will be reached through a chain of calls
	// to super.visit(o).  This method will be skipped if
	// super.visit() is not called, but so will visit(ExceptionThrower).
        public void visit(Instruction o) {
            processInputs(o);
        }

	/*        
        private void visitNormal(Instruction o) {
            processInputs(o);
        }
        public void visit(ArithmeticInstruction o) { visitNormal(o); }
        public void visit(AFIAT o) { visitNormal(o); }
        public void visit(ANEWARRAY_QUICK o) { visitNormal(o); }
        public void visit(CHECKCAST_QUICK o) { visitNormal(o); }
        public void visit(INSTANCEOF_QUICK o) { visitNormal(o); }
        public void visit(SINGLEANEWARRAY o) { visitNormal(o); }
        public void visit(ANEWARRAY o) { visitNormal(o); }
        public void visit(CHECKCAST o) { visitNormal(o); }
	*/        

        public void visit(FieldAccess o) { 
            Descriptor.Field desc = o.getSelector(ih, cp).getDescriptor();
            visitField(o, desc.getType().getTypeTag(), o instanceof PUTFIELD || o instanceof PUTSTATIC,
                    o instanceof PUTFIELD || o instanceof GETFIELD);
        }
        
        private void visitField(Instruction o, char type, boolean isPut, boolean receiver) {
            char[] input = null;
            type = toBasicType(type);
            if (receiver) {
                if (isPut) {
                    input = new char[] { type, TypeCodes.REFERENCE };
                } else {
                    input = A;
                }
            } else {
                if (isPut) {
                    input = new char[] { type };
                } else {
                    input = emptyCharArray;
                }
            }
            processInputs(o, input);
        }
        
        public void visit(Invocation o) {
            Descriptor.Method desc = o.getSelector(ih, cp).getDescriptor();
            visitInvoke(o, desc);
        }

        public void visit(Invocation_Quick o) {
            Descriptor.Method desc = o.getSelector(ih, cp).getDescriptor();
            visitInvoke(o, desc);
        }

        private void visitInvoke(Instruction o, Descriptor.Method desc) {
            char[] args = null;
            if (o instanceof INVOKESTATIC
		|| o instanceof INVOKE_NATIVE
		|| o instanceof INVOKE_SYSTEM)
	    {
                args = new char[desc.getArgumentCount()];
                for(int i = 0; i < desc.getArgumentCount(); i++) {
                    args[i] = toBasicType(desc.getArgumentType(i).getTypeTag());
                }
            } else {
                args = new char[desc.getArgumentCount() + 1];
                args[0] = TypeCodes.REFERENCE;
                for(int i = 0; i < desc.getArgumentCount(); i++) {
                    args[i + 1] = toBasicType(desc.getArgumentType(i).getTypeTag());
                }
            }
            reverseArray(args);
            processInputs(o, args);
        }
        
        public void visit(FieldAccess_Quick o) {
            char type = o instanceof GETFIELD_QUICK
                ? TypeCodes.INT : o instanceof GETFIELD2_QUICK ? TypeCodes.LONG :
                        o instanceof REF_GETFIELD_QUICK ?
                        TypeCodes.REFERENCE : o instanceof PUTFIELD_QUICK
                        ? TypeCodes.INT : o instanceof PUTFIELD2_QUICK ? TypeCodes.LONG
                                : TypeCodes.REFERENCE;
            boolean isPut = o instanceof PUTFIELD_QUICK || o instanceof PUTFIELD2_QUICK;
            visitField(o, type, isPut, true);
        }
        
        public void visit(INVOKE_NATIVE o) {
            int mindex = o.getMethodIndex();
            UnboundSelector.Method sel =
		NativeCallGenerator.invokeNativeType(mindex);
            Descriptor.Method desc = sel.getDescriptor();
            visitInvoke(o, desc);
        }

        public void visit(IFIAT o) {
	    processInputs(o, A);
        }
        public void visit(FFIAT o) {
	    processInputs(o, I);
        }
        public void visit(LFIAT o) {
	    processInputs(o, D);
        }
        public void visit(DFIAT o) {
	    processInputs(o, J);
        }

        public void visit(INVOKE_SYSTEM o) {
            int mindex = o.getMethodIndex();
            int optype = o.getOpType();
	    UnboundSelector.Method sel =
		NativeCallGenerator.invokeSystemType(mindex, optype);
	    visitInvoke(o, sel.getDescriptor());
        }
        
        public void visit(MULTIANEWARRAY o) {
            visitMultianewarray(o, o.getDimensions());
        }
        
        private void visitMultianewarray(Instruction o, int dimension) {
            char[] input = new char[dimension];
            for(int i = 0; i < input.length; i++)
                input[i] = TypeCodes.INT;
            processInputs(o, input);
        }
        
        public void visit(MULTIANEWARRAY_QUICK o) {
            visitMultianewarray(o, o.getDimensions());
        }
        
	// Should have been eliminated by this point
        public void visit(ROLL o) { 
	    throw new Error("Should have been eliminated : " + o); 
        }
        public void visit(StackManipulation o) { 
	    throw new Error("Should have been eliminated : " + o); 
	}
        public void visit(JsrInstruction o) { 
	    throw new Error("Should have been eliminated : " + o); 
	}

        // Control instructions

        public void visit(Switch o) { processInputs(o, I); }

        //IF_ACMPEQ, IF_ACMPNE, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
        // IF_ICMPLT, IF_ICMPNE,
        //IFEQ, IFGE, IFGT, IFLE, IFLT, IFNE, IFNONNULL, IFNULL
        public void visit(If o) {
            InstructionHandle target = o.getTargetHandle();
            int pos = ih.getPosition();
            // Handle the branch target
            int target_pos = target.getPosition();
            switch (o.getOpcode()) {
            case JVMConstants.Opcodes.IF_ACMPEQ:
            case JVMConstants.Opcodes.IF_ACMPNE:
                processInputs(o, AA);
                break;
            case JVMConstants.Opcodes.IF_ICMPEQ:
            case JVMConstants.Opcodes.IF_ICMPNE:
            case JVMConstants.Opcodes.IF_ICMPGT:
            case JVMConstants.Opcodes.IF_ICMPGE:
            case JVMConstants.Opcodes.IF_ICMPLE:
            case JVMConstants.Opcodes.IF_ICMPLT:
                processInputs(o, II);
                break;
            case JVMConstants.Opcodes.IFEQ:
            case JVMConstants.Opcodes.IFNE:
            case JVMConstants.Opcodes.IFGT:
            case JVMConstants.Opcodes.IFGE:
            case JVMConstants.Opcodes.IFLE:
            case JVMConstants.Opcodes.IFLT:
                processInputs(o, I);
                break;
            case JVMConstants.Opcodes.IFNULL:
            case JVMConstants.Opcodes.IFNONNULL:
                processInputs(o, A);
                break;
            default:
                throw new Error();
            }
        }     
    }

    private static class StackInstructionAndIINCEliminatorVisitor
	extends Visitor
    {
        ByteCodeGen mg;

        InstructionList il;

        InstructionHandle ih;

        final int maxLocals;

        int newMaxLocals;

        AbstractState[] abstractStates;

        HashMap p2nCache;

        boolean wasThereAnyIINC;

        public StackInstructionAndIINCEliminatorVisitor(ByteCodeGen mg) {
            this.mg = mg;
            this.il = mg.getInstructionList();
            maxLocals = mg.getMaxLocals();
            newMaxLocals = maxLocals;
            this.p2nCache = new HashMap();
            wasThereAnyIINC = false;
        }

        /**
         * Convert PC to the instruction number
         */
        private int p2n(int pos) {
            Integer cache = (Integer) p2nCache.get(new Integer(pos));
            if (cache != null) {
                return cache.intValue();
            } else {
                int[] instPositions = il.getInstructionPositions();
                for (int i = 0; i < instPositions.length; i++) {
                    if (instPositions[i] == pos) {
                        p2nCache.put(new Integer(pos), new Integer(i));
                        return i;
                    }
                }
            }
            throw new Error("p2n: PC " + pos + " does not exist");
        }

        public void run() {
            AbstractInterpretationVisitor v = new AbstractInterpretationVisitor(mg);
            v.run();
            abstractStates = v.getAbstractStates();

            ih = il.getStart();
            while (ih != null) {
                InstructionHandle next = ih.getNext();
                ih.accept(this);
                ih = next;
            }

            if (wasThereAnyIINC && mg.getMaxStack() <= 1) {
                mg.setMaxStack(2);
            }

            mg.setMaxLocals(newMaxLocals);
            mg.getByteCode();
	    //il.setPositions();
        }

        //DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, POP, POP2, SWAP, ROLL, IINC, WIDE_IINC

        private void incMaxLocals(int by) {
            if (newMaxLocals < maxLocals + by) {
                newMaxLocals = maxLocals + by;
            }
        }

	public void visit(ROLL o) {
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            int span = o.getSpan();
            int count = o.getCount();
	    char[] extendedLocals = new char[span];
	    InstructionHandle fi = null; // the first inserted instruction
	    incMaxLocals(span);
	    // Pop the stack slots of length span to the new locals
	    for(int i = 0; i < span; i++) {
		int stackPtr = stack.length - 1 - i;
		char t = stack[stackPtr];
		if (isUpperType(t)) { // Long or Double
		    char lower = stack[stackPtr - 1];
		    if (fi == null)
			fi = il.insert(ih, LocalWrite.make(lower, maxLocals + i));
		    else
			il.insert(ih, LocalWrite.make(lower, maxLocals + i));
		    extendedLocals[i] = t;
		    extendedLocals[i+1] = lower;
		    i++; // skip the lower slot
		} else {
		    if (fi == null)
			fi = il.insert(ih, LocalWrite.make(t, maxLocals + i));
		    else
			il.insert(ih, LocalWrite.make(t, maxLocals + i));
		    extendedLocals[i] = t;
		}
	    }
	    int ncount = 0;
	    if (count >= 0) { // bottom will move to top
		ncount = count;
	    } else {
		ncount = span + count; // note count < 0
	    }

	    // Push the <i>ncount</i> slots to the stack
	    for(int i = ncount - 1; i >= 0; i--) {
		if (isCategory2Type(extendedLocals[i])) {
		    if (fi == null)
			fi = il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i - 1));
		    else
			il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i - 1));
		    i--;
		} else {
		    if (fi == null)
			fi = il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i));
		    else
			il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i));
		}
	    }
	    // Push the rest of the slots
	    for(int i = span - 1; i >= ncount; i--) {
		if (isCategory2Type(extendedLocals[i])) {
		    if (fi == null)
			fi = il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i - 1));
		    else
			il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i - 1));
		    i--;
		} else {
		    if (fi == null)
			fi = il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i));
		    else
			il.insert(ih, LocalRead.make(extendedLocals[i], maxLocals + i));
		}
	    }

	    Assume.that(fi != null);
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
	}

	// IINC and WIDE_IINC
        public void visit(IINC o) {
            wasThereAnyIINC = true;
            incMaxLocals(2);
            InstructionHandle fi = il.insert(ih, IConstantLoad.makeIConstantLoad(o.getValue()));
            il.insert(ih, LocalWrite.make(TypeCodes.INT, maxLocals));
            il.insert(ih, LocalRead.make(TypeCodes.INT, maxLocals));
            il.insert(ih, LocalRead.make(TypeCodes.INT, o.getLocalVariableOffset()));
            il.insert(ih, IADD.make());
            il.insert(ih, LocalWrite.make(TypeCodes.INT, o.getLocalVariableOffset()));
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(DUP o) {
            incMaxLocals(1);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            Assume.that(isCategory1Type(top), "PC = " + ih.getPosition()
                    + " type = " + top);
            InstructionHandle fi = il.insert(ih, LocalWrite
                    .make(top, maxLocals));
            il.insert(ih, LocalRead.make(top, maxLocals));
            il.insert(ih, LocalRead.make(top, maxLocals));
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(DUP_X1 o) {
            incMaxLocals(2);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            Assume.that(isCategory1Type(top));
            Assume.that(isCategory1Type(top2));
            InstructionHandle fi = il.insert(ih, LocalWrite
                    .make(top, maxLocals));
            il.insert(ih, LocalWrite.make(top2, maxLocals + 1));
            il.insert(ih, LocalRead.make(top, maxLocals));
            il.insert(ih, LocalRead.make(top2, maxLocals + 1));
            il.insert(ih, LocalRead.make(top, maxLocals));
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(DUP_X2 o) {
            incMaxLocals(3);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            char top3 = stack[stack.length - 3];
            Assume.that(isCategory1Type(top));
            InstructionHandle fi = null;
            if (isCategory1Type(top3)) {
                fi = il.insert(ih, LocalWrite.make(top,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top2,
                        maxLocals + 1));
                il.insert(ih, LocalWrite.make(top3,
                        maxLocals + 2));
                il.insert(ih, LocalRead.make(top, maxLocals));
                il.insert(ih, LocalRead
                        .make(top3, maxLocals + 2));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));

            } else if (isCategory2Type(top3)) {
                fi = il.insert(ih, LocalWrite.make(top,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top3,
                        maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
                il.insert(ih, LocalRead
                        .make(top3, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
            } else
                throw new Error();
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(DUP2 o) {
            incMaxLocals(2);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            InstructionHandle fi = null;
            if (isCategory1Type(top2)) {
                fi = il.insert(ih, LocalWrite.make(top,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top2,
                        maxLocals + 1));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));

            } else if (isCategory2Type(top2)) {
                fi = il.insert(ih, LocalWrite.make(top2,
                        maxLocals));
                il.insert(ih, LocalRead.make(top2, maxLocals));
                il.insert(ih, LocalRead.make(top2, maxLocals));
            } else
                throw new Error();
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(DUP2_X1 o) {
            incMaxLocals(3);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            char top3 = stack[stack.length - 3];
            InstructionHandle fi = null;
            Assume.that(isCategory1Type(top3));
            if (isCategory1Type(top2)) {
                fi = il.insert(ih, LocalWrite.make(top,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top2,
                        maxLocals + 1));
                il.insert(ih, LocalWrite.make(top3,
                        maxLocals + 2));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
                il.insert(ih, LocalRead
                        .make(top3, maxLocals + 2));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
            } else if (isCategory2Type(top2)) {
                fi = il.insert(ih, LocalWrite.make(top2,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top3,
                        maxLocals + 2));
                il.insert(ih, LocalRead.make(top2, maxLocals));
                il.insert(ih, LocalRead
                        .make(top3, maxLocals + 2));
                il.insert(ih, LocalRead.make(top2, maxLocals));
            } else
                throw new Error();
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(DUP2_X2 o) {
            incMaxLocals(4);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            char top3 = stack[stack.length - 3];
            char top4 = stack[stack.length - 4];
            InstructionHandle fi = null;
            //Assume.that(top3 == TypeCodes.INT || top3 == TypeCodes.FLOAT || top3 ==
            // TypeCodes.REFERENCE);
            if (isCategory1Type(top2) && isCategory1Type(top4)) {
                fi = il.insert(ih, LocalWrite.make(top,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top2,
                        maxLocals + 1));
                il.insert(ih, LocalWrite.make(top3,
                        maxLocals + 2));
                il.insert(ih, LocalWrite.make(top4,
                        maxLocals + 3));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
                il.insert(ih, LocalRead
                        .make(top4, maxLocals + 3));
                il.insert(ih, LocalRead
                        .make(top3, maxLocals + 2));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));

            } else if (isCategory2Type(top2) && isCategory1Type(top4)) {
                fi = il.insert(ih, LocalWrite.make(top2,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top3,
                        maxLocals + 2));
                il.insert(ih, LocalWrite.make(top4,
                        maxLocals + 3));
                il.insert(ih, LocalWrite.make(top2, maxLocals));
                il.insert(ih, LocalRead
                        .make(top4, maxLocals + 3));
                il.insert(ih, LocalRead
                        .make(top3, maxLocals + 2));
                il.insert(ih, LocalRead.make(top2, maxLocals));
            } else if (isCategory1Type(top2) && isCategory2Type(top4)) {
                fi = il.insert(ih, LocalWrite.make(top,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top2,
                        maxLocals + 1));
                il.insert(ih, LocalWrite.make(top4,
                        maxLocals + 2));
                il.insert(ih, LocalRead
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
                il.insert(ih, LocalRead
                        .make(top4, maxLocals + 2));
                il.insert(ih, LocalRead 
                        .make(top2, maxLocals + 1));
                il.insert(ih, LocalRead.make(top, maxLocals));
            } else if (isCategory2Type(top2) && isCategory2Type(top4)) {
                fi = il.insert(ih, LocalWrite.make(top2,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top4,
                        maxLocals + 2));
                il.insert(ih, LocalRead.make(top2, maxLocals));
                il.insert(ih, LocalRead
                        .make(top4, maxLocals + 2));
                il.insert(ih, LocalRead.make(top2, maxLocals));
            } else
                throw new Error();
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(POP o) {
            incMaxLocals(1);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            InstructionHandle fi = null;
            Assume.that(isCategory1Type(top));
            fi = il.insert(ih, LocalWrite.make(top, maxLocals));
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(POP2 o) {
            incMaxLocals(2);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            InstructionHandle fi = null;
            if (isCategory1Type(top2)) {
                fi = il.insert(ih, LocalWrite.make(top,
                        maxLocals));
                il.insert(ih, LocalWrite.make(top2,
                        maxLocals + 1));
            } else if (isCategory2Type(top2)) {
                fi = il.insert(ih, LocalWrite.make(top2,
                        maxLocals));
            } else
                throw new Error();
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

        public void visit(SWAP o) {
            incMaxLocals(2);
            char[] stack = abstractStates[p2n(ih.getPosition())].stack;
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            InstructionHandle fi = null;
            Assume.that(isCategory1Type(top));
            fi = il.insert(ih, LocalWrite.make(top, maxLocals));
            il.insert(ih, LocalWrite.make(top2, maxLocals + 1));
            il.insert(ih, LocalRead.make(top, maxLocals));
            il.insert(ih, LocalRead.make(top2, maxLocals + 1));
            try {
                il.delete(ih);
            } catch (TargetLostException e) {
                InstructionHandle[] targets = e.getTargets();
                for (int i = 0; i < targets.length; i++) {
                    InstructionTargeter[] targeters = targets[i].getTargeters();
                    for (int j = 0; j < targeters.length; j++) {
                        targeters[j].updateTarget(targets[i], fi);
                    }
                }
            }
        }

    }

    /* Used by the eliminateSubroutine() */
    private static class SubroutineExceptionHandlerRecord {
        CodeExceptionGen original;

        int relStartPC; // startPC relative to the start of the subroutine

        int relEndPC; // endPC relative to the start of the subroutine

        boolean handlerWithinSubroutine;

        int relHandlerPC; // handlerPC relative to the start of the subroutine

        // when handlerWithinSubroutine == true

        InstructionHandle handlerHandle; // handlerHandle when

        // handlerWithinSubroutine == false

        TypeName.Scalar catchType;

        SubroutineExceptionHandlerRecord(CodeExceptionGen org, int rs, int re,
                int rh, TypeName.Scalar ct) {
            original = org;
            handlerWithinSubroutine = true;
            relStartPC = rs;
            relEndPC = re;
            relHandlerPC = rh;
            catchType = ct;
        }

        SubroutineExceptionHandlerRecord(CodeExceptionGen org, int rs, int re,
                InstructionHandle hh, TypeName.Scalar ct) {
            original = org;
            handlerWithinSubroutine = false;
            relStartPC = rs;
            relEndPC = re;
            handlerHandle = hh;
            catchType = ct;
        }

        public String toString() {
            return "SubroutineExceptionHandlerRecord["
                    + original
                    + ", "
                    + relStartPC
                    + ", "
                    + relEndPC
                    + ", "
                    + (handlerWithinSubroutine ? Integer.toString(relHandlerPC)
                            : handlerHandle.toString()) + ", " + catchType
                    + "]";
        }
    }

    /** An abstract state for each instruction */
    private static class AbstractState {
        /** The instruction handle of the instruction */
        private InstructionHandle instructionHandle;

        /** The PC of the instruction */
        private int pc;

        /** The operand stack */
        private char[] stack;

        /** What instruction pushes the item on the operand stack */
        private InstructionHandle[] producers;

        /**
         * The basic block which the instruction starts. Non null iff this PC is
         * the start of an basic block
         */
        private BasicBlock block;

        /** The previous and next AbstractStates within the same basic block */
        private AbstractState prev;

        private AbstractState next;

        /**
         * The reaching IN set of this instruction (a set of Defs)
         */
        //private IdentityHashSet reachingIn;
        private BitSet reachingInBitSet;

        /**
         * The reaching OUT set of this instruction (a set of Defs)
         */
        //private IdentityHashSet reachingOut;
        private BitSet reachingOutBitSet;

        /** The live IN set of this instruction (a set of Uses) */
        //private IdentityHashSet liveIn;
        private BitSet liveInBitSet;

        /**
         * The reaching OUT set of this instruction (a set of Uses) */
        //private IdentityHashSet liveOut;
        private BitSet liveOutBitSet;

        AbstractState(int pc, char[] stack, InstructionHandle[] producers) {
            this.pc = pc;
            this.stack = stack;
            this.producers = producers;
            Assume.that(stack.length == producers.length);
        }

        private String stackToString() {
            String r = stack.length + " [ ";
            for (int i = 0; i < stack.length; i++) {
                r += stack[i]
                        + "("
                        + (producers[i] != null ? producers[i].getPosition()
                                + "" : "null") + ")" + " ";
            }
            return r + "]";
        }

        public String toString() {
            return "[AbState: PC= " + pc + " SH=" + stackToString() + " RI"
                    + dataflowSetToString(reachingInBitSet) + " RO"
                    + dataflowSetToString(reachingOutBitSet) + " LI"
                    + dataflowSetToString(liveInBitSet) + " LO"
                    + dataflowSetToString(liveOutBitSet) + "] ";

        }

        public void setInstructionHandle(InstructionHandle ih) {
            instructionHandle = ih;
        }

        public void setBasicBlock(BasicBlock b) {
            block = b;
        }

        public void link(AbstractState as) {
            next = as;
            as.prev = this;
        }

        /** Peek the top of the stack */
        public char peek() {
            Assume.that(stack.length > 0);
            return stack[stack.length - 1];
        }

        public AbstractState nop(int newpc) {
            char[] stackCopy = new char[stack.length];
            System.arraycopy(stack, 0, stackCopy, 0, stack.length);
            return new AbstractState(newpc, stackCopy, producers);
        }

        /** Make a copy of the state with the argument pushed onto it */
        public AbstractState push(int newpc, char t, InstructionHandle p) {
            Assume.that(t != TypeCodes.LONG && t != TypeCodes.DOUBLE);
            char[] stackCopy = new char[stack.length + 1];
            System.arraycopy(stack, 0, stackCopy, 0, stack.length);
            stackCopy[stackCopy.length - 1] = t;
            InstructionHandle[] producersCopy = new InstructionHandle[producers.length + 1];
            System.arraycopy(producers, 0, producersCopy, 0, producers.length);
            producersCopy[producersCopy.length - 1] = p;
            return new AbstractState(newpc, stackCopy, producersCopy);
        }

        public AbstractState pushLong(int newpc, InstructionHandle p) {
            char[] stackCopy = new char[stack.length + 2];
            System.arraycopy(stack, 0, stackCopy, 0, stack.length);
            stackCopy[stackCopy.length - 2] = TypeCodes.LONG;
            stackCopy[stackCopy.length - 1] = TypeCodes.LONG_UPPER;
            InstructionHandle[] producersCopy = new InstructionHandle[producers.length + 2];
            System.arraycopy(producers, 0, producersCopy, 0, producers.length);
            producersCopy[producersCopy.length - 1] = p;
            producersCopy[producersCopy.length - 2] = p;
            return new AbstractState(newpc, stackCopy, producersCopy);
        }

        public AbstractState pushDouble(int newpc, InstructionHandle p) {
            char[] stackCopy = new char[stack.length + 2];
            System.arraycopy(stack, 0, stackCopy, 0, stack.length);
            stackCopy[stackCopy.length - 2] = TypeCodes.DOUBLE;
            stackCopy[stackCopy.length - 1] = TypeCodes.DOUBLE_UPPER;
            InstructionHandle[] producersCopy = new InstructionHandle[producers.length + 2];
            System.arraycopy(producers, 0, producersCopy, 0, producers.length);
            producersCopy[producersCopy.length - 1] = p;
            producersCopy[producersCopy.length - 2] = p;
            return new AbstractState(newpc, stackCopy, producersCopy);
        }

        /** Make a copy of the state with the top item popped off */
        public AbstractState pop(int newpc) {
            Assume.that(stack.length > 0);
            char top = stack[stack.length - 1];
            Assume.that(top != TypeCodes.LONG_UPPER
                    && top != TypeCodes.DOUBLE_UPPER);
            char[] stackCopy = new char[stack.length - 1];
            System.arraycopy(stack, 0, stackCopy, 0, stackCopy.length);
            InstructionHandle[] producersCopy = new InstructionHandle[producers.length - 1];
            System.arraycopy(producers, 0, producersCopy, 0,
                    producersCopy.length);
            return new AbstractState(newpc, stackCopy, producersCopy);
        }

        public AbstractState popLong(int newpc) {
            Assume.that(stack.length > 0);
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            Assume.that(top == TypeCodes.LONG_UPPER && top2 == TypeCodes.LONG);
            char[] stackCopy = new char[stack.length - 2];
            System.arraycopy(stack, 0, stackCopy, 0, stackCopy.length);
            InstructionHandle[] producersCopy = new InstructionHandle[producers.length - 2];
            System.arraycopy(producers, 0, producersCopy, 0,
                    producersCopy.length);
            return new AbstractState(newpc, stackCopy, producersCopy);
        }

        public AbstractState popDouble(int newpc) {
            Assume.that(stack.length > 0);
            char top = stack[stack.length - 1];
            char top2 = stack[stack.length - 2];
            Assume.that(top == TypeCodes.DOUBLE_UPPER
                    && top2 == TypeCodes.DOUBLE);
            char[] stackCopy = new char[stack.length - 2];
            System.arraycopy(stack, 0, stackCopy, 0, stackCopy.length);
            InstructionHandle[] producersCopy = new InstructionHandle[producers.length - 2];
            System.arraycopy(producers, 0, producersCopy, 0,
                    producersCopy.length);
            return new AbstractState(newpc, stackCopy, producersCopy);
        }

        public AbstractState popAppropriate(int newpc) {
            Assume.that(stack.length > 0);
            char top = stack[stack.length - 1];
            if (top == TypeCodes.LONG_UPPER) {
                return popLong(newpc);
            } else if (top == TypeCodes.DOUBLE_UPPER) {
                return popDouble(newpc);
            } else {
                return pop(newpc);
            }
        }
    }

    /**
     * Basic block.
     */
    private static class BasicBlock {
        private static class EdgeType {
            String name;

            EdgeType(String name) {
                this.name = name;
            }

            public String toString() {
                return name;
            }
        }

        private static EdgeType TREE_EDGE = new EdgeType("tree edge");

        private static EdgeType BACK_EDGE = new EdgeType("back edge");

        private static EdgeType FORWARD_EDGE = new EdgeType("forward edge");

        private static EdgeType CROSS_EDGE = new EdgeType("cross edge");

        /** The PC of the first instruction in this block */
        private int startPC;

        /** control predecessor blocks */
        private IdentityHashSet inEdges;

        /** control successor blocks */
        private IdentityHashSet outEdges;

        /** True if this block is a beginning of an exception handler */
        private boolean isHandlerEntry;

        /** The handle to the first instruction in this block */
        private InstructionHandle firstInstructionHandle;

        /** The handle to the last instruction in this block */
        private InstructionHandle lastInstructionHandle;

        /** The abstract state of at the first instruction in this block */
        private AbstractState firstAbstractState;

        /** The abstract state of at the last instruction in this block */
        private AbstractState lastAbstractState;

        /** The previous block in the program */
        private BasicBlock prevBlock;

        /** The next block in the program */
        private BasicBlock nextBlock;

        /**
         * If this block is the beginning of a subroutine, a set of basic blocks
         * where this subroutine should return
         */
        private IdentityHashSet subroutineReturnPoints;

        /**
         * If this block is the beginning of a subroutine, the end block of this
         * subroutine (where the RET is)
         */
        private BasicBlock matchingSubroutineExit;

        /** true if this block is the end block of an subroutine */
        private boolean isSubroutineExit;

        /**
         * If this block is the end of a subroutine, the begin block of this
         * subroutine (where the JSR jumps to)
         */
        private BasicBlock matchingSubroutineEntry;

        /**
         * True if this basic block is not the same block as the previous basic
         * block only because of an exception edge going out to an exception
         * handler
         */
        private boolean isAfterExceptionEdge;

        /** The preorder number of this basic block in the depth first search */
        private int preorder;

        /**
         * The reverse post order number of this basic block in the depth first
         * search
         */
        private int rpostorder;

        /**
         * Maps a successor basic block to the type of the edge between this
         * block to the block
         */
        private IdentityHashMap outEdgeTypeMap;

        /**
         * When isLoopEntry == true, the set of basic blocks in the loop whose
         * entry is this block
         */
        private IdentityHashSet blocksInThisLoop;

        /** The set of the entry blocks of the loop that contains this block */
        private IdentityHashSet enclosingLoopEntries;

        /**
         * The reaching IN set of this block (a set of Defs)
         */
        //private IdentityHashSet reachingIn;
	private BitSet reachingInBitSet;

        /**
         * The reaching OUT set of this block (a set of Defs)
         */
        //private IdentityHashSet reachingOut;
	private BitSet reachingOutBitSet;

        /** The live IN set of this block (a set of Uses) */
        //private IdentityHashSet liveIn;
	private BitSet liveInBitSet;

        /**
         * The live OUT set of this block (a set of Uses)
         */
        //private IdentityHashSet liveOut;
	private BitSet liveOutBitSet;

        /**
         * If this block is reachable from the entry block
         */
        private boolean isReachable;

        BasicBlock(int startPC) {
            this.startPC = startPC;
            inEdges = new IdentityHashSet();
            outEdges = new IdentityHashSet();
            isHandlerEntry = false;
            isAfterExceptionEdge = false;
            subroutineReturnPoints = new IdentityHashSet();
            isSubroutineExit = false;
            outEdgeTypeMap = new IdentityHashMap();
            enclosingLoopEntries = new IdentityHashSet();
        }

	public int startPC() { return startPC; }

        public void setReachable(boolean b) {
            isReachable = b;
        }

        public boolean isReachable() {
            return isReachable;
        }

        public void setBlocksInThisLoop(IdentityHashSet set) {
            blocksInThisLoop = set;
        }

        public void addEnclosingLoopEntry(BasicBlock entry) {
            Assume.that(entry.isLoopEntry());
            enclosingLoopEntries.add(entry);
        }

        public boolean isLoopEntry() {
            return blocksInThisLoop != null && blocksInThisLoop.size() > 0;
        }

        public int getPreorder() {
            return preorder;
        }

        public void setPreorder(int p) {
            preorder = p;
        }

        public int getReversePostOrder() {
            return rpostorder;
        }

        public void setReversePostOrder(int r) {
            rpostorder = r;
        }

        public void setOutEdgeType(BasicBlock b, EdgeType e) {
            Assume.that(outEdges.contains(b));
            outEdgeTypeMap.put(b, e);
        }

        public EdgeType getOutEdgeType(BasicBlock b) {
            Assume.that(outEdges.contains(b));
            return (EdgeType) outEdgeTypeMap.get(b);
        }

        void link(BasicBlock b) {
            this.nextBlock = b;
            b.prevBlock = this;
        }

        void setFirstAbstractState(AbstractState s) {
            firstAbstractState = s;
        }

        void setLastAbstractState(AbstractState s) {
            lastAbstractState = s;
        }

        void setInstructionHandles(InstructionHandle first,
                InstructionHandle last) {
            firstInstructionHandle = first;
            lastInstructionHandle = last;
        }

        void setMatchingSubroutineExit(BasicBlock b) {
            Assume.that(isSubroutineEntry());
            Assume.that(b.isSubroutineExit);
            matchingSubroutineExit = b;
        }

        void setMatchingSubroutineEntry(BasicBlock b) {
            Assume.that(isSubroutineExit);
            Assume.that(b.isSubroutineEntry());
            matchingSubroutineEntry = b;
        }

        void addEdgeTo(BasicBlock b) {
            outEdges.add(b);
            b.inEdges.add(this);
        }

        IdentityHashSet getOutEdges() {
            return outEdges;
        }

        IdentityHashSet getInEdges() {
            return inEdges;
        }

        void setIsHandlerEntry(boolean ihe) {
            isHandlerEntry = ihe;
        }

        void setIsAfterExceptionEdge(boolean iaee) {
            isAfterExceptionEdge = iaee;
        }

        void setIsSubroutineExit(boolean ise) {
            isSubroutineExit = ise;
        }

        void declareSubroutineEntry(BasicBlock subroutineReturnPoint) {
            subroutineReturnPoints.add(subroutineReturnPoint);
        }

        boolean isSubroutineEntry() {
            return !subroutineReturnPoints.empty();
        }

        public String toString() {
            return "BB" + startPC;
        }

        public String toDetailString() {
            return "BB"
                    + startPC
                    + (isHandlerEntry ? ":H" : "")
                    + (isSubroutineEntry() ? ":S" : "")
                    + (isSubroutineExit ? ":R" : "")
                    + (isAfterExceptionEdge ? ":IAEE" : "")
                    + (isReachable ? "" : ":NR")
                    + " P"
                    + inEdges
                    + " S"
                    + outEdges
                    + " RANGE["
                    + firstInstructionHandle.getPosition()
                    + ","
                    + lastInstructionHandle.getPosition()
                    + "]"
                    + (isLoopEntry() ? " LoopEntryOf" + blocksInThisLoop : " ")
                    + (enclosingLoopEntries.size() > 0 ? " EncLoopEntries"
                            + enclosingLoopEntries : "") + " RI"
                    + dataflowSetToString(reachingInBitSet) + " RO"
                    + dataflowSetToString(reachingOutBitSet) + " LI"
                    + dataflowSetToString(liveInBitSet) + " LO"
                    + dataflowSetToString(liveOutBitSet);
        }

        public void resetReachingInOutSets() {
	    //            reachingIn = new IdentityHashSet();
	    //            reachingOut = new IdentityHashSet();
	    reachingInBitSet = new BitSet();
	    reachingOutBitSet = new BitSet();
            AbstractState as = firstAbstractState;
            while (as != null) {
		//                as.reachingIn = new IdentityHashSet();
		//                as.reachingOut = new IdentityHashSet();
                as.reachingInBitSet = new BitSet();
                as.reachingOutBitSet = new BitSet();
                as = as.next;
            }
        }

        public void resetLiveInOutSets() {
	    //            liveIn = new IdentityHashSet();
	    //            liveOut = new IdentityHashSet();
	    liveInBitSet = new BitSet();
	    liveOutBitSet = new BitSet();
            AbstractState as = firstAbstractState;
            while (as != null) {
		//                as.liveIn = new IdentityHashSet();
		//                as.liveOut = new IdentityHashSet();
                as.liveInBitSet = new BitSet();
                as.liveOutBitSet = new BitSet();
                as = as.next;
            }
        }

    }

    public static class Stack {
        private ArrayList internal;
        public Stack() {
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

    private static class AbstractInterpretationVisitor extends Visitor {

        AbstractState[] abstractStates;

        BasicBlock[] basicBlocks;

        ByteCodeGen mg;

        InstructionList il;

        IdentityHashSet workSet;

        InstructionHandle ih;

        HashMap p2nCache;

        char[] localTypes;

        Constants cp;
        
        public AbstractInterpretationVisitor(ByteCodeGen mg) {
            this.mg = mg;
            this.il = mg.getInstructionList();
            this.p2nCache = new HashMap();
            this.cp = mg.getConstantPool();

            localTypes = new char[mg.getMaxLocals()];
        }

        /**
         * Convert PC to the instruction number
         */
        private int p2n(int pos) {
            Integer cache = (Integer) p2nCache.get(new Integer(pos));
            if (cache != null) {
                return cache.intValue();
            } else {
                int[] instPositions = il.getInstructionPositions();
                for (int i = 0; i < instPositions.length; i++) {
                    if (instPositions[i] == pos) {
                        p2nCache.put(new Integer(pos), new Integer(i));
                        return i;
                    }
                }
            }
            throw new Error();
        }

        public void run() {
            IdentityHashSet rememberSet = new IdentityHashSet();
            workSet = new IdentityHashSet();

            abstractStates = new AbstractState[il.size()];
            basicBlocks = new BasicBlock[il.size()];

            // Add the initial PC to the work set
            abstractStates[0] = new AbstractState(0, new char[0],
                    new InstructionHandle[0]);
            basicBlocks[0] = new BasicBlock(0);
            workSet.add(il.getStart());

            // Add exception handlers to the work set
            CodeExceptionGen[] handlers = mg.getExceptionHandlers();
            for (int i = 0; i < handlers.length; i++) {
                InstructionHandle hh = handlers[i].getHandlerPC();
                int hpc = hh.getPosition();
                workSet.add(hh);
                abstractStates[p2n(hpc)] = new AbstractState(hpc,
                        new char[] { TypeCodes.REFERENCE },
                        new InstructionHandle[] { null });
                basicBlocks[p2n(hpc)] = new BasicBlock(hpc);
                basicBlocks[p2n(hpc)].setIsHandlerEntry(true);
            }

            char[] atypes = mg.getArgumentTypes();
            /*if (mg.isStatic()) {
                for (int i = 0; i < atypes.length; i++) {
                    localTypes[i] = convert2VMType(atypes[i]);
                }
            } else {*/
                localTypes[0] = TypeCodes.REFERENCE;
                for (int i = 0; i < atypes.length; i++) {
                    localTypes[i + 1] = toBasicType(atypes[i]);
                }
            //}

            InstructionHandle[] instructionHandles = il.getInstructionHandles();

            while (!workSet.empty()) {
                ih = (InstructionHandle) workSet.remove();
                if (rememberSet.contains(ih))
                    continue;
                //BasicIO.out.println("processing pc " + ih.getPosition());
                rememberSet.add(ih);
                try {
                    ih.accept(this);
                } catch (Exception e) {
                    for (int i = 0; i < abstractStates.length; i++) {
                        if (abstractStates[i] != null) {
                            BasicIO.err.println(abstractStates[i] + " : "
                                    + abstractStates[i].stack.hashCode());
                        }
                    }
                    throw new Error(e.toString());
                }
            }

            // Link abstractStates with InstructionHandles
            for (int i = 0; i < il.size(); i++) {
                if (abstractStates[i] != null)
                    abstractStates[i].setInstructionHandle(instructionHandles[i]);
            }

            // Link abstractStates[i] with basicBlocks[i]
            // Set BasicBlock.firstAbstractState
            // Set instructionHandles for basic blocks
            for (int i = 0; i < il.size(); i++) {
                if (basicBlocks[i] != null) {
                    abstractStates[i].setBasicBlock(basicBlocks[i]);
                    basicBlocks[i].setFirstAbstractState(abstractStates[i]);
                    InstructionHandle first = instructionHandles[i];
                    int j = i + 1;
                    while (j < il.size() && basicBlocks[j] == null 
                            && abstractStates[j] != null)
                        j++;
                    int lastINum = j - 1;
                    InstructionHandle last = instructionHandles[lastINum];
                    basicBlocks[i].setInstructionHandles(first, last);
                }
            }

            // Establish a linked list of abstractStates within the same basic
            // block
            for (int i = 0; i < il.size() - 1; i++) {
                if (abstractStates[i] != null && abstractStates[i + 1] != null) {
                    if (abstractStates[i + 1].block == null)
                        abstractStates[i].link(abstractStates[i + 1]);
                }
            }

            // Set BasicBlock.lastAbstractState
            for (int i = 0; i < il.size(); i++) {
                if (basicBlocks[i] != null) {
                    AbstractState as = abstractStates[i];
                    AbstractState prev = as;
                    while (as != null) {
                        prev = as;
                        as = as.next;
                    }
                    basicBlocks[i].setLastAbstractState(prev);
                }
            }

            // Establish control edges between basic blocks
            new ControlEdgeLinkerVisitor().run();
        }

        public AbstractState[] getAbstractStates() {
            return abstractStates;
        }

	/*
	 * Indexed by the bytecode PC, meaning there are null elements
	 * in the array for PCs between instructions.
	 */
        public BasicBlock[] getBasicBlocks() {
            return basicBlocks;
        }

	/*
	 * Indexed by the block #, there are no null elements.
	 */
        public BasicBlock[] getBasicBlockList() {
	    ArrayList bs = new ArrayList();
	    for(BasicBlock b = basicBlocks[0]; b != null; b = b.nextBlock) {
		bs.add(b);
	    }
	    BasicBlock[] blockList = new BasicBlock[bs.size()];
	    bs.toArray(blockList);
            return blockList;
        }

        private void addNextToWorkSet() {
            InstructionHandle next = ih.getNext();
            if (next != null) {
                workSet.add(next);
            }
        }

	// Note:  This method will be reached through a chain of calls
	// to super.visit(o).  This method will be skipped if
	// super.visit() is not called, but so will visit(ExceptionThrower).
        public void visit(Instruction o) {
            propagateState(o);
            addNextToWorkSet();
        }
        
        public void visit(FieldAccess o) {
	    visit((ExceptionThrower) o);
            Descriptor.Field desc = o.getSelector(ih, cp).getDescriptor();
            visitField(o, desc.getType().getTypeTag(), o instanceof PUTFIELD || o instanceof PUTSTATIC,
                    o instanceof PUTFIELD || o instanceof GETFIELD);
        }
        
        private void visitField(Instruction o, char type, boolean isPut, boolean receiver) {
            char[] input = null; char[] output = null;
            type = toBasicType(type);
            if (receiver) {
                if (isPut) {
                    input = new char[] { type, TypeCodes.REFERENCE };
                    output = emptyCharArray;
                } else {
                    input = A;
                    output = new char[] { type };
                }
            } else {
                if (isPut) {
                    input = new char[] { type };
                    output = emptyCharArray;
                } else {
                    input = emptyCharArray;
                    output = new char[] { type };
                }
            }
            propagateState(input, output);
            addNextToWorkSet();
        }
        
        public void visit(Invocation o) {
	    visit((ExceptionThrower) o);
            Descriptor.Method desc = o.getSelector(ih, cp).getDescriptor();
            visitInvoke(o, desc);
        }

        public void visit(Invocation_Quick o) {
	    visit((ExceptionThrower) o);
            Descriptor.Method desc = o.getSelector(ih, cp).getDescriptor();
            visitInvoke(o, desc);
        }

        private void visitInvoke(Instruction o, Descriptor.Method desc) {
            char[] args = null;
            if (o instanceof INVOKESTATIC
		|| o instanceof INVOKE_NATIVE
		|| o instanceof INVOKE_SYSTEM)
	    {
                args = new char[desc.getArgumentCount()];
                for(int i = 0; i < desc.getArgumentCount(); i++) {
                    args[i] = toBasicType(desc.getArgumentType(i).getTypeTag());
                }
            } else {
                args = new char[desc.getArgumentCount() + 1];
                args[0] = TypeCodes.REFERENCE;
                for(int i = 0; i < desc.getArgumentCount(); i++) {
                    args[i + 1] = toBasicType(desc.getArgumentType(i).getTypeTag());
                }
            }
            reverseArray(args);
            char rt = desc.getType().getTypeTag();
            propagateState(args, rt == TypeCodes.VOID 
                    ? emptyCharArray : new char[] { toBasicType(rt) });
            addNextToWorkSet();
        }
        
        public void visit(FieldAccess_Quick o) {
	    visit((ExceptionThrower) o);
            if (o instanceof GETFIELD_QUICK) {
                visitField(o, TypeCodes.INT, false, true);
            } else if (o instanceof GETFIELD2_QUICK) {
                visitField(o, TypeCodes.LONG, false, true);                
            } else if (o instanceof REF_GETFIELD_QUICK) {
                visitField(o, TypeCodes.REFERENCE, false, true);                                
            } else if (o instanceof PUTFIELD_QUICK) {
                int pos = ih.getPosition();
                if (ih.getNext() != null) {
                    int next_pos = ih.getNext().getPosition();
                    AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                    as = as.popAppropriate(next_pos); // value
                    as = as.pop(next_pos); // receiver
                    abstractStates[p2n(next_pos)] = as;
		    addNextToWorkSet();
                }
            } else if (o instanceof PUTFIELD2_QUICK) {
                int pos = ih.getPosition();
                if (ih.getNext() != null) {
                    int next_pos = ih.getNext().getPosition();
                    AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                    as = as.popAppropriate(next_pos); // value
                    as = as.pop(next_pos); // receiver
                    abstractStates[p2n(next_pos)] = as;
		    addNextToWorkSet();
                }
            } else
                throw new Error();
        }
        
        public void visit(INVOKE_NATIVE o) {
            int mindex = o.getMethodIndex();
            UnboundSelector.Method sel =
		NativeCallGenerator.invokeNativeType(mindex);
            Descriptor.Method desc = sel.getDescriptor();
            visitInvoke(o, desc);
        }

        public void visit(IFIAT o) {
            int pos = ih.getPosition();
            if (ih.getNext() != null) {
                int next_pos = ih.getNext().getPosition();
                AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                as = as.popAppropriate(next_pos);
                as = as.push(next_pos, TypeCodes.INT, ih);
                abstractStates[p2n(next_pos)] = as;
		addNextToWorkSet();
            }
        }
        public void visit(FFIAT o) {
            int pos = ih.getPosition();
            if (ih.getNext() != null) {
                int next_pos = ih.getNext().getPosition();
                AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                as = as.popAppropriate(next_pos);
                as = as.push(next_pos, TypeCodes.FLOAT, ih);
                abstractStates[p2n(next_pos)] = as;
		addNextToWorkSet();
            }
        }
        public void visit(LFIAT o) {
            int pos = ih.getPosition();
            if (ih.getNext() != null) {
                int next_pos = ih.getNext().getPosition();
                AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                as = as.popAppropriate(next_pos);
                as = as.pushLong(next_pos, ih);
                abstractStates[p2n(next_pos)] = as;
		addNextToWorkSet();
            }
        }
        public void visit(DFIAT o) {
            int pos = ih.getPosition();
            if (ih.getNext() != null) {
                int next_pos = ih.getNext().getPosition();
                AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                as = as.popAppropriate(next_pos);
                as = as.pushDouble(next_pos, ih);
                abstractStates[p2n(next_pos)] = as;
		addNextToWorkSet();
            }
        }

        public void visit(INVOKE_SYSTEM o) {
	    visit((ExceptionThrower) o);
            int mindex = o.getMethodIndex();
            int optype = o.getOpType();
	    UnboundSelector.Method sel =
		NativeCallGenerator.invokeSystemType(mindex, optype);
	    visitInvoke(o, sel.getDescriptor());
        }
        
        public void visit(MULTIANEWARRAY o) {
	    visit((ExceptionThrower) o);
            visitMultianewarray(o.getDimensions());
        }
        
        private void visitMultianewarray(int dimension) {
            char[] input = new char[dimension];
            for(int i = 0; i < input.length; i++)
                input[i] = TypeCodes.INT;
            propagateState(input, new char[] { TypeCodes.REFERENCE} );
            addNextToWorkSet();
        }
        
        public void visit(MULTIANEWARRAY_QUICK o) {
	    // visit((ExceptionThrower) o);
            visitMultianewarray(o.getDimensions());
        }
        
        public void visit(ROLL o) {
            int span = o.getSpan();
            int count = o.getCount();
            int pos = ih.getPosition();
            InstructionHandle[] bp = abstractStates[p2n(pos)].producers;
            InstructionHandle[] ap = new InstructionHandle[bp.length];
            char[] bs = abstractStates[p2n(pos)].stack;
            char[] as = new char[bs.length];
            System.arraycopy(bs, 0, as, 0, bs.length);
            System.arraycopy(bp, 0, ap, 0, bp.length);
            char[] tmps = new char[span];
            InstructionHandle[] tmpp = new InstructionHandle[span]; 
            
            for (int j = span - 1; j >= 0; j--) {
                tmps[j] = bs[bs.length - span + j];
                tmpp[j] = bp[bp.length - span + j];
            }
            for (int j = 0; j < span; j++) {
                as[as.length - span + j] = tmps[(j + span - count) % span];
                ap[ap.length - span + j] = tmpp[(j + span - count) % span];
            }
            int next_pos = ih.getNext().getPosition();
            //BasicIO.err.println("ROLL: before " + charArrayToString(bs));
            //BasicIO.err.println("ROLL: after " + charArrayToString(as));
            abstractStates[p2n(next_pos)] = new AbstractState(next_pos, as, ap);
            addNextToWorkSet();
        }

        //DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, POP, POP2, SWAP
        public void visit(StackManipulation o) {
            int pos = ih.getPosition();
            char[] bs = abstractStates[p2n(pos)].stack;
            char[] as = null;
            InstructionHandle[] bp = abstractStates[p2n(pos)].producers;
            InstructionHandle[] ap = null;
            if (o instanceof DUP) {
                as = new char[bs.length + 1];
                ap = new InstructionHandle[bp.length + 1];
                System.arraycopy(bs, 0, as, 0, bs.length);
                System.arraycopy(bp, 0, ap, 0, bp.length);
                as[as.length - 1] = bs[bs.length - 1];
                ap[ap.length - 1] = bp[bp.length - 1];
            } else if (o instanceof DUP_X1) {
                as = new char[bs.length + 1];
                ap = new InstructionHandle[bp.length + 1];
                System.arraycopy(bs, 0, as, 0, bs.length - 2);
                System.arraycopy(bp, 0, ap, 0, bp.length - 2);
                as[as.length - 1] = bs[bs.length - 1];
                as[as.length - 2] = bs[bs.length - 2];
                as[as.length - 3] = bs[bs.length - 1];
                ap[ap.length - 1] = bp[bp.length - 1];
                ap[ap.length - 2] = bp[bp.length - 2];
                ap[ap.length - 3] = bp[bp.length - 1];
            } else if (o instanceof DUP_X2) {
                as = new char[bs.length + 1];
                ap = new InstructionHandle[bp.length + 1];
                System.arraycopy(bs, 0, as, 0, bs.length - 3);
                System.arraycopy(bp, 0, ap, 0, bp.length - 3);
                as[as.length - 1] = bs[bs.length - 1];
                as[as.length - 2] = bs[bs.length - 2];
                as[as.length - 3] = bs[bs.length - 3];
                as[as.length - 4] = bs[bs.length - 1];
                ap[ap.length - 1] = bp[bp.length - 1];
                ap[ap.length - 2] = bp[bp.length - 2];
                ap[ap.length - 3] = bp[bp.length - 3];
                ap[ap.length - 4] = bp[bp.length - 1];
            } else if (o instanceof DUP2) {
                as = new char[bs.length + 2];
                ap = new InstructionHandle[bp.length + 2];
                System.arraycopy(bs, 0, as, 0, bs.length);
                System.arraycopy(bp, 0, ap, 0, bp.length);
                as[as.length - 1] = bs[bs.length - 1];
                as[as.length - 2] = bs[bs.length - 2];
                ap[ap.length - 1] = bp[bp.length - 1];
                ap[ap.length - 2] = bp[bp.length - 2];
            } else if (o instanceof DUP2_X1) {
                as = new char[bs.length + 2];
                ap = new InstructionHandle[bp.length + 2];
                System.arraycopy(bs, 0, as, 0, bs.length - 3);
                System.arraycopy(bp, 0, ap, 0, bp.length - 3);
                as[as.length - 1] = bs[bs.length - 1];
                as[as.length - 2] = bs[bs.length - 2];
                as[as.length - 3] = bs[bs.length - 3];
                as[as.length - 4] = bs[bs.length - 1];
                as[as.length - 5] = bs[bs.length - 2];
                ap[ap.length - 1] = bp[bp.length - 1];
                ap[ap.length - 2] = bp[bp.length - 2];
                ap[ap.length - 3] = bp[bp.length - 3];
                ap[ap.length - 4] = bp[bp.length - 1];
                ap[ap.length - 5] = bp[bp.length - 2];
            } else if (o instanceof DUP2_X2) {
                as = new char[bs.length + 2];
                ap = new InstructionHandle[bp.length + 2];
                System.arraycopy(bs, 0, as, 0, bs.length - 4);
                System.arraycopy(bp, 0, ap, 0, bp.length - 4);
                as[as.length - 1] = bs[bs.length - 1];
                as[as.length - 2] = bs[bs.length - 2];
                as[as.length - 3] = bs[bs.length - 3];
                as[as.length - 4] = bs[bs.length - 4];
                as[as.length - 5] = bs[bs.length - 1];
                as[as.length - 6] = bs[bs.length - 2];
                ap[ap.length - 1] = bp[bp.length - 1];
                ap[ap.length - 2] = bp[bp.length - 2];
                ap[ap.length - 3] = bp[bp.length - 3];
                ap[ap.length - 4] = bp[bp.length - 4];
                ap[ap.length - 5] = bp[bp.length - 1];
                ap[ap.length - 6] = bp[bp.length - 2];
            } else if (o instanceof POP) {
                as = new char[bs.length - 1];
                ap = new InstructionHandle[bp.length - 1];
                System.arraycopy(bs, 0, as, 0, bs.length - 1);
                System.arraycopy(bp, 0, ap, 0, bp.length - 1);
            } else if (o instanceof POP2) {
                as = new char[bs.length - 2];
                ap = new InstructionHandle[bp.length - 2];
                System.arraycopy(bs, 0, as, 0, bs.length - 2);
                System.arraycopy(bp, 0, ap, 0, bp.length - 2);
            } else if (o instanceof SWAP) {
                as = new char[bs.length];
                ap = new InstructionHandle[bp.length];
                System.arraycopy(bs, 0, as, 0, bs.length - 2);
                System.arraycopy(bp, 0, ap, 0, bp.length - 2);
                as[as.length - 1] = bs[bs.length - 2];
                as[as.length - 2] = bs[bs.length - 1];
                ap[ap.length - 1] = bp[bp.length - 2];
                ap[ap.length - 2] = bp[bp.length - 1];
            } else {
                throw new Error();
            }
            int next_pos = ih.getNext().getPosition();
            abstractStates[p2n(next_pos)] = new AbstractState(next_pos, as, ap);
            addNextToWorkSet();
        }

        // Control instructions

        public void visit(ATHROW o) {
	    // skip ExceptionThrower and Instruction visits
        }
        public void visit(ReturnInstruction o) {
	    // skip ExceptionThrower and Instruction visits
        }
        public void visit(RET o) {
	    // skip Instruction visit
        }
        public void visit(WIDE_RET o) {
	    // skip Instruction visit
        }

        public void visit(JsrInstruction o) {
            InstructionHandle target = o.getTargetHandle();
            int pos = ih.getPosition();
            // Handle the branch target
            int target_pos = target.getPosition();
            if (abstractStates[p2n(target_pos)] == null) {
                abstractStates[p2n(target_pos)] = abstractStates[p2n(pos)]
                        .push(target_pos, TypeCodes.REFERENCE, ih);
            }
            workSet.add(target);
            if (basicBlocks[p2n(target_pos)] == null) {
                basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
            } else {
                basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
            }

            // Handle the instruction after the JSR
            InstructionHandle physicalSucc = ih.getNext();

            if (physicalSucc != null) {
                int next_pos = physicalSucc.getPosition();
                if (abstractStates[p2n(next_pos)] == null) {
                    abstractStates[p2n(next_pos)] = abstractStates[p2n(pos)]
                            .nop(next_pos);
                }
                workSet.add(physicalSucc);
                if (basicBlocks[p2n(next_pos)] == null) {
                    basicBlocks[p2n(next_pos)] = new BasicBlock(next_pos);
                } else {
                    basicBlocks[p2n(next_pos)].setIsAfterExceptionEdge(false);
                }

                basicBlocks[p2n(target_pos)]
                        .declareSubroutineEntry(basicBlocks[p2n(next_pos)]);
            } else
                throw new Error();

        }

        public void visit(Switch o) {
            int pos = ih.getPosition();
            InstructionHandle defaultTarget = o.getTargetHandle();
            InstructionHandle[] targets = o.getTargetHandles();
            // default taget
            int dtarget_pos = defaultTarget.getPosition();
            if (abstractStates[p2n(dtarget_pos)] == null) {
                abstractStates[p2n(dtarget_pos)] = abstractStates[p2n(pos)]
                        .popAppropriate(dtarget_pos);
            }
            workSet.add(defaultTarget);
            if (basicBlocks[p2n(dtarget_pos)] == null) {
                basicBlocks[p2n(dtarget_pos)] = new BasicBlock(dtarget_pos);
            } else {
                basicBlocks[p2n(dtarget_pos)].setIsAfterExceptionEdge(false);
            }

            // other targets
            for (int i = 0; i < targets.length; i++) {
                int target_pos = targets[i].getPosition();
                if (abstractStates[p2n(target_pos)] == null) {
                    abstractStates[p2n(target_pos)] = abstractStates[p2n(pos)]
                            .popAppropriate(target_pos);
                }
                workSet.add(targets[i]);
                if (basicBlocks[p2n(target_pos)] == null) {
                    basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
                } else {
                    basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
                }
            }
        }

        //IF_ACMPEQ, IF_ACMPNE, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
        // IF_ICMPLT, IF_ICMPNE,
        //IFEQ, IFGE, IFGT, IFLE, IFLT, IFNE, IFNONNULL, IFNULL
        public void visit(If o) {
            InstructionHandle target = o.getTargetHandle();
            int pos = ih.getPosition();
            // Handle the branch target
            int target_pos = target.getPosition();
            switch (o.getOpcode()) {
            case JVMConstants.Opcodes.IF_ACMPEQ:
            case JVMConstants.Opcodes.IF_ACMPNE:
                if (abstractStates[p2n(target_pos)] == null) {
                    abstractStates[p2n(target_pos)] = abstractStates[p2n(pos)]
                            .pop(target_pos).pop(target_pos);
                }
                propagateState(AA, emptyCharArray);
                break;
            case JVMConstants.Opcodes.IF_ICMPEQ:
            case JVMConstants.Opcodes.IF_ICMPNE:
            case JVMConstants.Opcodes.IF_ICMPGT:
            case JVMConstants.Opcodes.IF_ICMPGE:
            case JVMConstants.Opcodes.IF_ICMPLE:
            case JVMConstants.Opcodes.IF_ICMPLT:
                if (abstractStates[p2n(target_pos)] == null) {
                    abstractStates[p2n(target_pos)] = abstractStates[p2n(pos)]
                            .pop(target_pos).pop(target_pos);
                }
                propagateState(II, emptyCharArray);
                break;
            case JVMConstants.Opcodes.IFEQ:
            case JVMConstants.Opcodes.IFNE:
            case JVMConstants.Opcodes.IFGT:
            case JVMConstants.Opcodes.IFGE:
            case JVMConstants.Opcodes.IFLE:
            case JVMConstants.Opcodes.IFLT:
                if (abstractStates[p2n(target_pos)] == null) {
                    abstractStates[p2n(target_pos)] = abstractStates[p2n(pos)]
                            .pop(target_pos);
                }
                propagateState(I, emptyCharArray);
                break;
            case JVMConstants.Opcodes.IFNULL:
            case JVMConstants.Opcodes.IFNONNULL:
                if (abstractStates[p2n(target_pos)] == null) {
                    abstractStates[p2n(target_pos)] = abstractStates[p2n(pos)]
                            .pop(target_pos);
                }
                propagateState(A, emptyCharArray);
                break;
            default:
                throw new Error();
            }

            workSet.add(target);
            workSet.add(ih.getNext());
            if (basicBlocks[p2n(target_pos)] == null) {
                basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
            } else {
                basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
            }
            if (basicBlocks[p2n(ih.getNext().getPosition())] == null) {
                basicBlocks[p2n(ih.getNext().getPosition())] = new BasicBlock(
                        ih.getNext().getPosition());
            } else {
                basicBlocks[p2n(ih.getNext().getPosition())]
                        .setIsAfterExceptionEdge(false);
            }
        }

        public void visit(GotoInstruction o) {
            InstructionHandle target = o.getTargetHandle();
            int pos = ih.getPosition();
            // Handle the branch target
            int target_pos = target.getPosition();
            if (abstractStates[p2n(target_pos)] == null) {
                abstractStates[p2n(target_pos)] = abstractStates[p2n(pos)]
                        .nop(target_pos);
            }
            workSet.add(target);
            if (basicBlocks[p2n(target_pos)] == null) {
                basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
            } else {
                basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
            }
        }

        public void visit(ExceptionThrower o) {
            if (o instanceof ATHROW || o instanceof ReturnInstruction)
                throw new Error("how'd that happend?");
            if (o.getThrowables().length == 0)
                return;
            int pos = ih.getPosition();
            CodeExceptionGen[] handlers = mg.getExceptionHandlers();
            for (int i = 0; i < handlers.length; i++) {
                InstructionHandle startPC = handlers[i].getStartPC();
                InstructionHandle endPC = handlers[i].getEndPC();

                if (startPC.getPosition() <= pos && pos <= endPC.getPosition()) {
                    int next_pos = ih.getNext().getPosition();
                    if (basicBlocks[p2n(next_pos)] == null) {
                        basicBlocks[p2n(next_pos)] = new BasicBlock(next_pos);
                        basicBlocks[p2n(next_pos)]
                                .setIsAfterExceptionEdge(true);
                    }
                }
            }
        }

        // Private helper methods
        
        /**
         * @param ins We examine {@link Instruction#stackIns} and
         *            {@link Instruction#stackOuts} to determine its
         *            effect on the stack. 
         */
        private void propagateState(Instruction ins) {
            SpecificationIR.Value[] popped = ins.stackIns; 
            SpecificationIR.Value[] pushed = ins.stackOuts;
            int pos = ih.getPosition();
            if (ih.getNext() != null) {
                int next_pos = ih.getNext().getPosition();
                AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                for (int i = 0; i < popped.length; i++) {
                    if (popped[i] instanceof SpecificationIR.SecondHalf)
                        continue;
                    char t = specValue2TypeCode(popped[i]);
                    if (t == TypeCodes.DOUBLE)
                        as = as.popDouble(next_pos);
                    else if (t == TypeCodes.LONG)
                        as = as.popLong(next_pos);
                    else
                        as = as.pop(next_pos);
                }
		for(int i = pushed.length - 1; i >= 0; i--) {
                    if (pushed[i] instanceof SpecificationIR.SecondHalf)
                        continue;
                    char t = specValue2TypeCode(pushed[i]);
                    if (t == TypeCodes.DOUBLE)
                        as = as.pushDouble(next_pos, ih);
                    else if (t == TypeCodes.LONG)
                        as = as.pushLong(next_pos, ih);
                    else
                        as = as.push(next_pos, t, ih);
                }
                abstractStates[p2n(next_pos)] = as;
            }
        }

        private void propagateState(char[] popped, char[] pushed) {
            int pos = ih.getPosition();
            if (ih.getNext() != null) {
                int next_pos = ih.getNext().getPosition();
                AbstractState as = abstractStates[p2n(pos)].nop(next_pos);
                for (int i = 0; i < popped.length; i++) {
                    char t = popped[i];
                    if (t == TypeCodes.DOUBLE)
                        as = as.popDouble(next_pos);
                    else if (t == TypeCodes.LONG)
                        as = as.popLong(next_pos);
                    else
                        as = as.pop(next_pos);
                }
                for (int i = 0; i < pushed.length; i++) {
                    char t = pushed[i];
                    if (t == TypeCodes.DOUBLE)
                        as = as.pushDouble(next_pos, ih);
                    else if (t == TypeCodes.LONG)
                        as = as.pushLong(next_pos, ih);
                    else
                        as = as.push(next_pos, t, ih);
                }
                abstractStates[p2n(next_pos)] = as;
            }
        }

        private BasicBlock searchNearestBasicBlockBackwards(int inum) {
            for (int n = inum; n >= 0; n--) {
                if (basicBlocks[n] != null)
                    return basicBlocks[n];
            }
            throw new Error();
        }

        private BasicBlock searchNearestBasicBlockForwards(int inum) {
            for (int n = inum; n < basicBlocks.length; n++) {
                if (basicBlocks[n] != null)
                    return basicBlocks[n];
            }
            throw new Error();
        }

        /**
         * Search for the entry of the current subroutine level. Assume that
         * BasicBlock.isSubroutineEntry and .isSubroutineExit are properly set.
         * 
         * @param block
         * @return the subroutine entry
         */
        private BasicBlock searchForCurrentLevelSubroutineEntryBackwards(
                BasicBlock block) {
            IdentityHashSet seenSet = new IdentityHashSet();
            IdentityHashSet workSet = new IdentityHashSet();
            HashMap block2SubroutineLevel = new HashMap(); // relative to the
            // current level
            workSet.add(block);
            block2SubroutineLevel.put(block, new Integer(0));

            while (!workSet.empty()) {
                BasicBlock b = (BasicBlock) workSet.remove();
                seenSet.add(b);
                int level = ((Integer) block2SubroutineLevel.get(b)).intValue();
                if (b.isSubroutineEntry() && level == 0) {
                    return b;
                }
                IdentityHashSet inEdges = b.getInEdges();
                for (Iterator it = inEdges.iterator(); it.hasNext();) {
                    BasicBlock pred = (BasicBlock) it.next();
                    if (!seenSet.contains(pred)) {
                        int predlevel = level;
                        if (b.isSubroutineEntry()) {
                            predlevel--;
                        }
                        if (pred.isSubroutineExit) {
                            predlevel++;
                        }
                        block2SubroutineLevel.put(pred, new Integer(predlevel));
                        workSet.add(pred);
                    }
                }
            }

            return null; // probably because the return point to RET link of the
            // nested subroutine is not established yet
        }

        private class ControlEdgeLinkerVisitor extends Visitor {
            InstructionHandle ih;

            BasicBlock currentBlock;

            ControlEdgeLinkerVisitor() {
            }

            void run() {
                // Established control edges by calling the visit methods.
                // Link blocks with previous and next blocks.
                currentBlock = basicBlocks[0];
                ih = il.getStart();
                while (ih != null) {
                    if (abstractStates[p2n(ih.getPosition())] != null)
                        ih.accept(this);
                    
                    ih = ih.getNext();
                    if (ih != null) {
                        BasicBlock nextBlock = basicBlocks[p2n(ih.getPosition())];
                        if (nextBlock != null) {
                            currentBlock.link(nextBlock);
                            currentBlock = nextBlock;
                        }
                    }
                }

                // Mark subroutine exit blocks
                for (BasicBlock b = basicBlocks[0]; b != null; b = b.nextBlock) {
                    if (b.lastInstructionHandle.getInstruction() instanceof RET) {
                        b.setIsSubroutineExit(true);
                    }
                }

                // Link RET and return points
                boolean repeatOnceMore = true;
                while (repeatOnceMore) {
                    repeatOnceMore = false;
                    BasicBlock b = basicBlocks[0];
                    while (b != null) {
                        if (b.isSubroutineExit) {
                            if (b.getOutEdges().size() == 0) {
                                BasicBlock entry = searchForCurrentLevelSubroutineEntryBackwards(b);
                                if (entry != null) {
                                    entry.setMatchingSubroutineExit(b);
                                    b.setMatchingSubroutineEntry(entry);
                                    for (Iterator it = entry.subroutineReturnPoints
                                            .iterator(); it.hasNext();) {
                                        b.addEdgeTo((BasicBlock) it.next());
                                    }
                                } else {
                                    repeatOnceMore = true;
                                }
                            }
                        }
                        b = b.nextBlock;
                    }
                }
                
                // Assert that all subroutine entries have a matching exit
                for(BasicBlock b = basicBlocks[0]; b != null; b = b.nextBlock) {
                    if (b.isSubroutineEntry()) {
                        Assume.that(b.matchingSubroutineExit != null);
                    }
                }
            }

            private void addEdgeToNext() {
                InstructionHandle next = ih.getNext();
                if (next == null)
                    return;
                int next_pos = ih.getNext().getPosition();
                BasicBlock nextB = basicBlocks[p2n(next_pos)];
                if (nextB != null) {
                    currentBlock.addEdgeTo(nextB);
                }
            }

            public void visit(Instruction o) {
                addEdgeToNext();
            }
            
            public void visit(ATHROW o) {
		visit((ExceptionThrower) o);
            }

            public void visit(ReturnInstruction o) {
		visit((ExceptionThrower) o);
            }

            public void visit(RET o) {
            }

            public void visit(WIDE_RET o) {
            }
            
            public void visit(JsrInstruction o) {
                InstructionHandle target = o.getTargetHandle();
                int pos = ih.getPosition();
                // Handle the branch target
                int target_pos = target.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
            }

            public void visit(Switch o) {
                int pos = ih.getPosition();
                InstructionHandle defaultTarget = o.getTargetHandle();
                InstructionHandle[] targets = o.getTargetHandles();
                // default taget
                int dtarget_pos = defaultTarget.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(dtarget_pos)]);

                // other targets
                for (int i = 0; i < targets.length; i++) {
                    int target_pos = targets[i].getPosition();
                    currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
                }
            }

            public void visit(If o) {
                InstructionHandle target = o.getTargetHandle();
                int pos = ih.getPosition();
                // Handle the branch target
                int target_pos = target.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
                currentBlock.addEdgeTo(basicBlocks[p2n(ih.getNext()
                        .getPosition())]);
            }

            public void visit(GotoInstruction o) {
                InstructionHandle target = o.getTargetHandle();
                int pos = ih.getPosition();
                // Handle the branch target
                int target_pos = target.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
            }

            public void visit(ExceptionThrower o) {
                if (o.getThrowables().length == 0) {
                    return;
                }
                int pos = ih.getPosition();
                CodeExceptionGen[] handlers = mg.getExceptionHandlers();
                for (int i = 0; i < handlers.length; i++) {
                    InstructionHandle startPC = handlers[i].getStartPC();
                    InstructionHandle endPC = handlers[i].getEndPC();
                    InstructionHandle handlerPC = handlers[i].getHandlerPC();

                    if (startPC.getPosition() <= pos
                            && pos <= endPC.getPosition()) {
                        int next_pos = ih.getNext().getPosition();
                        if (!(o instanceof ReturnInstruction)
                                && !(o instanceof ATHROW))
                            currentBlock.addEdgeTo(basicBlocks[p2n(next_pos)]);
                        currentBlock.addEdgeTo(basicBlocks[p2n(handlerPC
                                .getPosition())]);
                    }
                }
            }
        }
    }

    private static class ControlFlowAbstractInterpretationVisitor extends Visitor {

        BasicBlock[] basicBlocks;

        ByteCodeGen mg;

        InstructionList il;

        IdentityHashSet workSet;

        InstructionHandle ih;

        HashMap p2nCache;

        public ControlFlowAbstractInterpretationVisitor(ByteCodeGen mg) {
            this.mg = mg;
            this.il = mg.getInstructionList();
            this.p2nCache = new HashMap();
	}

        /**
         * Convert PC to the instruction number
         */
        private int p2n(int pos) {
            Integer cache = (Integer) p2nCache.get(new Integer(pos));
            if (cache != null) {
                return cache.intValue();
            } else {
                int[] instPositions = il.getInstructionPositions();
                for (int i = 0; i < instPositions.length; i++) {
                    if (instPositions[i] == pos) {
                        p2nCache.put(new Integer(pos), new Integer(i));
                        return i;
                    }
                }
            }
            throw new Error();
        }

        public void run() {
            IdentityHashSet rememberSet = new IdentityHashSet();
            workSet = new IdentityHashSet();

            basicBlocks = new BasicBlock[il.size()];

            // Add the initial PC to the work set
            basicBlocks[0] = new BasicBlock(0);
            workSet.add(il.getStart());

            // Add exception handlers to the work set
            CodeExceptionGen[] handlers = mg.getExceptionHandlers();
            for (int i = 0; i < handlers.length; i++) {
                InstructionHandle hh = handlers[i].getHandlerPC();
                int hpc = hh.getPosition();
                workSet.add(hh);
                basicBlocks[p2n(hpc)] = new BasicBlock(hpc);
                basicBlocks[p2n(hpc)].setIsHandlerEntry(true);
            }

            InstructionHandle[] instructionHandles = il.getInstructionHandles();

            while (!workSet.empty()) {
                ih = (InstructionHandle) workSet.remove();
                if (rememberSet.contains(ih))
                    continue;
                //BasicIO.out.println("processing pc " + ih.getPosition());
                rememberSet.add(ih);
                try {
                    ih.accept(this);
                } catch (Exception e) {
                    throw new Error(e.toString());
                }
            }

            // Set instructionHandles for basic blocks
            for (int i = 0; i < il.size(); i++) {
                if (basicBlocks[i] != null) {
                    InstructionHandle first = instructionHandles[i];
                    int j = i + 1;
                    while (j < il.size() && basicBlocks[j] == null)
                        j++;
                    int lastINum = j - 1;
                    InstructionHandle last = instructionHandles[lastINum];
                    basicBlocks[i].setInstructionHandles(first, last);
                }
            }

            // Establish control edges between basic blocks
            new ControlEdgeLinkerVisitor().run();
        }

        public BasicBlock[] getBasicBlocks() {
            return basicBlocks;
        }

	/*
	 * Indexed by the block #, there are no null elements.
	 */
        public BasicBlock[] getBasicBlockList() {
	    ArrayList bs = new ArrayList();
	    for(BasicBlock b = basicBlocks[0]; b != null; b = b.nextBlock) {
		bs.add(b);
	    }
	    BasicBlock[] blockList = new BasicBlock[bs.size()];
	    bs.toArray(blockList);
            return blockList;
        }

        private void addNextToWorkSet() {
            InstructionHandle next = ih.getNext();
            if (next != null) {
                workSet.add(next);
            }
        }

        public void visit(Instruction o) {
            addNextToWorkSet();
        }
        
        public void visit(FieldAccess o) {
	    visit((ExceptionThrower) o);
            addNextToWorkSet();
        }
        
        public void visit(Invocation o) {
	    visit((ExceptionThrower) o);
            addNextToWorkSet();
        }

        public void visit(Invocation_Quick o) {
	    visit((ExceptionThrower) o);
            addNextToWorkSet();
        }

        public void visit(FieldAccess_Quick o) {
	    visit((ExceptionThrower) o);
	    addNextToWorkSet();
        }
        
        public void visit(INVOKE_NATIVE o) {
	    addNextToWorkSet();
        }

        public void visit(IFIAT o) {
            if (ih.getNext() != null) {
		addNextToWorkSet();
            }
        }
        public void visit(FFIAT o) {
            if (ih.getNext() != null) {
		addNextToWorkSet();
            }
        }
        public void visit(LFIAT o) {
            if (ih.getNext() != null) {
		addNextToWorkSet();
            }
        }
        public void visit(DFIAT o) {
            if (ih.getNext() != null) {
		addNextToWorkSet();
            }
        }

        public void visit(INVOKE_SYSTEM o) {
	    visit((ExceptionThrower) o);
	    addNextToWorkSet();
        }
        
        public void visit(MULTIANEWARRAY o) {
	    visit((ExceptionThrower) o);
            addNextToWorkSet();
        }
        
        public void visit(MULTIANEWARRAY_QUICK o) {
	    // visit((ExceptionThrower) o);
            addNextToWorkSet();
        }
        
        public void visit(ROLL o) {
            addNextToWorkSet();
        }

        //DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, POP, POP2, SWAP
        public void visit(StackManipulation o) {
            addNextToWorkSet();
        }

        // Control instructions

        public void visit(ATHROW o) {
	    // skip ExceptionThrower and Instruction visits
        }
        public void visit(ReturnInstruction o) {
	    // skip ExceptionThrower and Instruction visits
        }
        public void visit(RET o) {
	    // skip Instruction visit
        }
        public void visit(WIDE_RET o) {
	    // skip Instruction visit
        }

        public void visit(JsrInstruction o) {
            InstructionHandle target = o.getTargetHandle();
            int pos = ih.getPosition();
            // Handle the branch target
            int target_pos = target.getPosition();
            workSet.add(target);
            if (basicBlocks[p2n(target_pos)] == null) {
                basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
            } else {
                basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
            }

            // Handle the instruction after the JSR
            InstructionHandle physicalSucc = ih.getNext();

            if (physicalSucc != null) {
                int next_pos = physicalSucc.getPosition();
                workSet.add(physicalSucc);
                if (basicBlocks[p2n(next_pos)] == null) {
                    basicBlocks[p2n(next_pos)] = new BasicBlock(next_pos);
                } else {
                    basicBlocks[p2n(next_pos)].setIsAfterExceptionEdge(false);
                }

                basicBlocks[p2n(target_pos)]
                        .declareSubroutineEntry(basicBlocks[p2n(next_pos)]);
            } else
                throw new Error();

        }

        public void visit(Switch o) {
            int pos = ih.getPosition();
            InstructionHandle defaultTarget = o.getTargetHandle();
            InstructionHandle[] targets = o.getTargetHandles();
            // default taget
            int dtarget_pos = defaultTarget.getPosition();
            workSet.add(defaultTarget);
            if (basicBlocks[p2n(dtarget_pos)] == null) {
                basicBlocks[p2n(dtarget_pos)] = new BasicBlock(dtarget_pos);
            } else {
                basicBlocks[p2n(dtarget_pos)].setIsAfterExceptionEdge(false);
            }

            // other targets
            for (int i = 0; i < targets.length; i++) {
                int target_pos = targets[i].getPosition();
                workSet.add(targets[i]);
                if (basicBlocks[p2n(target_pos)] == null) {
                    basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
                } else {
                    basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
                }
            }
        }

        //IF_ACMPEQ, IF_ACMPNE, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
        // IF_ICMPLT, IF_ICMPNE,
        //IFEQ, IFGE, IFGT, IFLE, IFLT, IFNE, IFNONNULL, IFNULL
        public void visit(If o) {
            InstructionHandle target = o.getTargetHandle();
            int pos = ih.getPosition();
            // Handle the branch target
            int target_pos = target.getPosition();
            workSet.add(target);
            workSet.add(ih.getNext());
            if (basicBlocks[p2n(target_pos)] == null) {
                basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
            } else {
                basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
            }
            if (basicBlocks[p2n(ih.getNext().getPosition())] == null) {
                basicBlocks[p2n(ih.getNext().getPosition())] = new BasicBlock(
                        ih.getNext().getPosition());
            } else {
                basicBlocks[p2n(ih.getNext().getPosition())]
                        .setIsAfterExceptionEdge(false);
            }
        }

        public void visit(GotoInstruction o) {
            InstructionHandle target = o.getTargetHandle();
            int pos = ih.getPosition();
            // Handle the branch target
            int target_pos = target.getPosition();
            workSet.add(target);
            if (basicBlocks[p2n(target_pos)] == null) {
                basicBlocks[p2n(target_pos)] = new BasicBlock(target_pos);
            } else {
                basicBlocks[p2n(target_pos)].setIsAfterExceptionEdge(false);
            }
        }

        public void visit(ExceptionThrower o) {
            if (o instanceof ATHROW || o instanceof ReturnInstruction)
                throw new Error("how'd that happend?");
            if (o.getThrowables().length == 0)
                return;
            int pos = ih.getPosition();
            CodeExceptionGen[] handlers = mg.getExceptionHandlers();
            for (int i = 0; i < handlers.length; i++) {
                InstructionHandle startPC = handlers[i].getStartPC();
                InstructionHandle endPC = handlers[i].getEndPC();

                if (startPC.getPosition() <= pos && pos <= endPC.getPosition()) {
                    int next_pos = ih.getNext().getPosition();
                    if (basicBlocks[p2n(next_pos)] == null) {
                        basicBlocks[p2n(next_pos)] = new BasicBlock(next_pos);
                        basicBlocks[p2n(next_pos)]
                                .setIsAfterExceptionEdge(true);
                    }
                }
            }
        }

        // Private helper methods
        
        private BasicBlock searchNearestBasicBlockBackwards(int inum) {
            for (int n = inum; n >= 0; n--) {
                if (basicBlocks[n] != null)
                    return basicBlocks[n];
            }
            throw new Error();
        }

        private BasicBlock searchNearestBasicBlockForwards(int inum) {
            for (int n = inum; n < basicBlocks.length; n++) {
                if (basicBlocks[n] != null)
                    return basicBlocks[n];
            }
            throw new Error();
        }

        /**
         * Search for the entry of the current subroutine level. Assume that
         * BasicBlock.isSubroutineEntry and .isSubroutineExit are properly set.
         * 
         * @param block
         * @return the subroutine entry
         */
        private BasicBlock searchForCurrentLevelSubroutineEntryBackwards(
                BasicBlock block) {
            IdentityHashSet seenSet = new IdentityHashSet();
            IdentityHashSet workSet = new IdentityHashSet();
            HashMap block2SubroutineLevel = new HashMap(); // relative to the
            // current level
            workSet.add(block);
            block2SubroutineLevel.put(block, new Integer(0));

            while (!workSet.empty()) {
                BasicBlock b = (BasicBlock) workSet.remove();
                seenSet.add(b);
                int level = ((Integer) block2SubroutineLevel.get(b)).intValue();
                if (b.isSubroutineEntry() && level == 0) {
                    return b;
                }
                IdentityHashSet inEdges = b.getInEdges();
                for (Iterator it = inEdges.iterator(); it.hasNext();) {
                    BasicBlock pred = (BasicBlock) it.next();
                    if (!seenSet.contains(pred)) {
                        int predlevel = level;
                        if (b.isSubroutineEntry()) {
                            predlevel--;
                        }
                        if (pred.isSubroutineExit) {
                            predlevel++;
                        }
                        block2SubroutineLevel.put(pred, new Integer(predlevel));
                        workSet.add(pred);
                    }
                }
            }

            return null; // probably because the return point to RET link of the
            // nested subroutine is not established yet
        }

        private class ControlEdgeLinkerVisitor extends Visitor {
            InstructionHandle ih;

            BasicBlock currentBlock;

            ControlEdgeLinkerVisitor() {
            }

            void run() {
                // Established control edges by calling the visit methods.
                // Link blocks with previous and next blocks.
                currentBlock = basicBlocks[0];
                ih = il.getStart();
                while (ih != null) {
		    ih.accept(this);
                    
                    ih = ih.getNext();
                    if (ih != null) {
                        BasicBlock nextBlock = basicBlocks[p2n(ih.getPosition())];
                        if (nextBlock != null) {
                            currentBlock.link(nextBlock);
                            currentBlock = nextBlock;
                        }
                    }
                }

                // Mark subroutine exit blocks
                for (BasicBlock b = basicBlocks[0]; b != null; b = b.nextBlock) {
                    if (b.lastInstructionHandle.getInstruction() instanceof RET) {
                        b.setIsSubroutineExit(true);
                    }
                }

                // Link RET and return points
                boolean repeatOnceMore = true;
                while (repeatOnceMore) {
                    repeatOnceMore = false;
                    BasicBlock b = basicBlocks[0];
                    while (b != null) {
                        if (b.isSubroutineExit) {
                            if (b.getOutEdges().size() == 0) {
                                BasicBlock entry = searchForCurrentLevelSubroutineEntryBackwards(b);
                                if (entry != null) {
                                    entry.setMatchingSubroutineExit(b);
                                    b.setMatchingSubroutineEntry(entry);
                                    for (Iterator it = entry.subroutineReturnPoints
                                            .iterator(); it.hasNext();) {
                                        b.addEdgeTo((BasicBlock) it.next());
                                    }
                                } else {
                                    repeatOnceMore = true;
                                }
                            }
                        }
                        b = b.nextBlock;
                    }
                }
                
                // Assert that all subroutine entries have a matching exit
                for(BasicBlock b = basicBlocks[0]; b != null; b = b.nextBlock) {
                    if (b.isSubroutineEntry()) {
                        Assume.that(b.matchingSubroutineExit != null);
                    }
                }
            }

            private void addEdgeToNext() {
                InstructionHandle next = ih.getNext();
                if (next == null)
                    return;
                int next_pos = ih.getNext().getPosition();
                BasicBlock nextB = basicBlocks[p2n(next_pos)];
                if (nextB != null) {
                    currentBlock.addEdgeTo(nextB);
                }
            }

            public void visit(Instruction o) {
                addEdgeToNext();
            }
            
            public void visit(ATHROW o) {
		visit((ExceptionThrower) o);
            }

            public void visit(ReturnInstruction o) {
		visit((ExceptionThrower) o);
            }

            public void visit(RET o) {
            }

            public void visit(WIDE_RET o) {
            }
            
            public void visit(JsrInstruction o) {
                InstructionHandle target = o.getTargetHandle();
                int pos = ih.getPosition();
                // Handle the branch target
                int target_pos = target.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
            }

            public void visit(Switch o) {
                int pos = ih.getPosition();
                InstructionHandle defaultTarget = o.getTargetHandle();
                InstructionHandle[] targets = o.getTargetHandles();
                // default taget
                int dtarget_pos = defaultTarget.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(dtarget_pos)]);

                // other targets
                for (int i = 0; i < targets.length; i++) {
                    int target_pos = targets[i].getPosition();
                    currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
                }
            }

            public void visit(If o) {
                InstructionHandle target = o.getTargetHandle();
                int pos = ih.getPosition();
                // Handle the branch target
                int target_pos = target.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
                currentBlock.addEdgeTo(basicBlocks[p2n(ih.getNext()
                        .getPosition())]);
            }

            public void visit(GotoInstruction o) {
                InstructionHandle target = o.getTargetHandle();
                int pos = ih.getPosition();
                // Handle the branch target
                int target_pos = target.getPosition();
                currentBlock.addEdgeTo(basicBlocks[p2n(target_pos)]);
            }

            public void visit(ExceptionThrower o) {
                if (o.getThrowables().length == 0) {
                    return;
                }
                int pos = ih.getPosition();
                CodeExceptionGen[] handlers = mg.getExceptionHandlers();
                for (int i = 0; i < handlers.length; i++) {
                    InstructionHandle startPC = handlers[i].getStartPC();
                    InstructionHandle endPC = handlers[i].getEndPC();
                    InstructionHandle handlerPC = handlers[i].getHandlerPC();

                    if (startPC.getPosition() <= pos
                            && pos <= endPC.getPosition()) {
                        int next_pos = ih.getNext().getPosition();
                        if (!(o instanceof ReturnInstruction)
                                && !(o instanceof ATHROW))
                            currentBlock.addEdgeTo(basicBlocks[p2n(next_pos)]);
                        currentBlock.addEdgeTo(basicBlocks[p2n(handlerPC
                                .getPosition())]);
                    }
                }
            }
        }
    }


}

