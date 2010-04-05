struct HEADER {
struct ovm_core_domain_Blueprint* _blueprint_;
struct ovm_services_monitors_Monitor* _monitor_;
};
struct java_lang_Object {
 struct HEADER _parent_;
};
struct ovm_core_OVMBase {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction {
  struct ovm_core_OVMBase _parent_;
  struct arr_ovm_services_bytecode_SpecificationIR_Value* stackIns;
  struct arr_ovm_services_bytecode_SpecificationIR_Value* stackOuts;
  struct arr_ovm_services_bytecode_SpecificationIR_IntValue* istreamIns;
  struct arr_ovm_services_bytecode_SpecificationIR_Value* evals;
  jint opcode_;
};
struct ovm_services_bytecode_Instruction_TwoByteInstruction {
  struct ovm_services_bytecode_Instruction _parent_;
};
struct ovm_services_bytecode_Instruction_LocalWrite2 {
  struct ovm_services_bytecode_Instruction_TwoByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_LSTORE {
  struct ovm_services_bytecode_Instruction_LocalWrite2 _parent_;
};
struct ovm_services_bytecode_Instruction_DSTORE {
  struct ovm_services_bytecode_Instruction_LocalWrite2 _parent_;
};
struct ovm_services_bytecode_Instruction_FSTORE {
  struct ovm_services_bytecode_Instruction_LocalWrite2 _parent_;
};
struct ovm_services_bytecode_Instruction_ISTORE {
  struct ovm_services_bytecode_Instruction_LocalWrite2 _parent_;
};
struct ovm_services_bytecode_Instruction_ASTORE {
  struct ovm_services_bytecode_Instruction_LocalWrite2 _parent_;
};
struct ovm_services_bytecode_Instruction_OneByteInstruction {
  struct ovm_services_bytecode_Instruction _parent_;
};
struct ovm_services_bytecode_Instruction_FlowEnd {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_SHORT_RETURN {
  struct ovm_services_bytecode_Instruction_FlowEnd _parent_;
};
struct ovm_services_bytecode_Instruction_ARETURN {
  struct ovm_services_bytecode_Instruction_SHORT_RETURN _parent_;
};
struct arr_jfloat {
    struct java_lang_Object _parent_;
    int length;
jfloat values[0];
};
struct arr_jboolean {
    struct java_lang_Object _parent_;
    int length;
jboolean values[0];
};
struct arr_jchar {
    struct java_lang_Object _parent_;
    int length;
jchar values[0];
};
struct arr_jshort {
    struct java_lang_Object _parent_;
    int length;
jshort values[0];
};
struct arr_jbyte {
    struct java_lang_Object _parent_;
    int length;
jbyte values[0];
};
struct ovm_services_bytecode_Instruction_LocalWrite {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
  jint localOffset_;
};
struct ovm_services_bytecode_Instruction_ConcreteIStore {
  struct ovm_services_bytecode_Instruction_LocalWrite _parent_;
};
struct ovm_services_bytecode_Instruction_ISTORE_1 {
  struct ovm_services_bytecode_Instruction_ConcreteIStore _parent_;
};
struct arr_jint {
    struct java_lang_Object _parent_;
    int length;
jint values[0];
};
struct arr_jlong {
    struct java_lang_Object _parent_;
    int length;
jlong values[0];
};
struct arr_jdouble {
    struct java_lang_Object _parent_;
    int length;
jdouble values[0];
};
struct ovm_services_bytecode_Instruction_ISTORE_0 {
  struct ovm_services_bytecode_Instruction_ConcreteIStore _parent_;
};
struct ovm_services_bytecode_Instruction_ConcreteAStore {
  struct ovm_services_bytecode_Instruction_LocalWrite _parent_;
};
struct ovm_services_bytecode_Instruction_ASTORE_3 {
  struct ovm_services_bytecode_Instruction_ConcreteAStore _parent_;
};
struct ovm_services_bytecode_Instruction_ASTORE_2 {
  struct ovm_services_bytecode_Instruction_ConcreteAStore _parent_;
};
struct ovm_services_bytecode_Instruction_ASTORE_1 {
  struct ovm_services_bytecode_Instruction_ConcreteAStore _parent_;
};
struct ovm_services_bytecode_Instruction_ASTORE_0 {
  struct ovm_services_bytecode_Instruction_ConcreteAStore _parent_;
};
struct arr_arr_jint {
    struct java_lang_Object _parent_;
    int length;
    struct arr_jint* values[0];
};
struct ovm_services_bytecode_Instruction_RETURN {
  struct ovm_services_bytecode_Instruction_FlowEnd _parent_;
};
struct ovm_services_bytecode_Instruction_LocalRead {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
  jint localOffset_;
};
struct ovm_services_bytecode_Instruction_ConcreteLLoad {
  struct ovm_services_bytecode_Instruction_LocalRead _parent_;
};
struct ovm_services_bytecode_Instruction_LLOAD_3 {
  struct ovm_services_bytecode_Instruction_ConcreteLLoad _parent_;
};
struct ovm_services_bytecode_Instruction_LLOAD_2 {
  struct ovm_services_bytecode_Instruction_ConcreteLLoad _parent_;
};
struct ovm_services_bytecode_Instruction_LLOAD_1 {
  struct ovm_services_bytecode_Instruction_ConcreteLLoad _parent_;
};
struct ovm_services_bytecode_Instruction_LLOAD_0 {
  struct ovm_services_bytecode_Instruction_ConcreteLLoad _parent_;
};
struct ovm_services_bytecode_Instruction_ConcreteDLoad {
  struct ovm_services_bytecode_Instruction_LocalRead _parent_;
};
struct ovm_services_bytecode_Instruction_DLOAD_3 {
  struct ovm_services_bytecode_Instruction_ConcreteDLoad _parent_;
};
struct ovm_services_bytecode_Instruction_FRETURN {
  struct ovm_services_bytecode_Instruction_SHORT_RETURN _parent_;
};
struct arr_arr_arr_jint {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_jint* values[0];
};
struct ovm_services_bytecode_Instruction_LONG_RETURN {
  struct ovm_services_bytecode_Instruction_FlowEnd _parent_;
};
struct ovm_services_bytecode_Instruction_DRETURN {
  struct ovm_services_bytecode_Instruction_LONG_RETURN _parent_;
};
struct ovm_services_bytecode_Instruction_LRETURN {
  struct ovm_services_bytecode_Instruction_LONG_RETURN _parent_;
};
struct java_lang_Comparable {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_IRETURN {
  struct ovm_services_bytecode_Instruction_SHORT_RETURN _parent_;
};
struct ovm_services_bytecode_Instruction_ConcreteFLoad {
  struct ovm_services_bytecode_Instruction_LocalRead _parent_;
};
struct ovm_services_bytecode_Instruction_FLOAD_0 {
  struct ovm_services_bytecode_Instruction_ConcreteFLoad _parent_;
};
struct arr_arr_arr_arr_jint {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_jint* values[0];
};
struct ovm_services_bytecode_Instruction_ConcreteILoad {
  struct ovm_services_bytecode_Instruction_LocalRead _parent_;
};
struct ovm_services_bytecode_Instruction_ILOAD_3 {
  struct ovm_services_bytecode_Instruction_ConcreteILoad _parent_;
};
struct ovm_core_execution_NativeInterface {
  struct java_lang_Object _parent_;
};
struct ovm_core_execution_Native {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_ILOAD_2 {
  struct ovm_services_bytecode_Instruction_ConcreteILoad _parent_;
};
struct ovm_services_bytecode_Instruction_ILOAD_1 {
  struct ovm_services_bytecode_Instruction_ConcreteILoad _parent_;
};
struct ovm_services_bytecode_Instruction_ILOAD_0 {
  struct ovm_services_bytecode_Instruction_ConcreteILoad _parent_;
};
struct java_lang_Boolean {
  struct java_lang_Object _parent_;
  zint value;
};
struct ovm_services_bytecode_Instruction_ConcreteALoad {
  struct ovm_services_bytecode_Instruction_LocalRead _parent_;
};
struct ovm_services_bytecode_Instruction_ALOAD_3 {
  struct ovm_services_bytecode_Instruction_ConcreteALoad _parent_;
};
struct arr_arr_arr_arr_arr_jint {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_arr_jint* values[0];
};
struct ovm_services_bytecode_Instruction_DLOAD_1 {
  struct ovm_services_bytecode_Instruction_ConcreteDLoad _parent_;
};
struct ovm_core_services_io_BasicIO {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_services_bytecode_Instruction_DLOAD_0 {
  struct ovm_services_bytecode_Instruction_ConcreteDLoad _parent_;
};
struct ovm_core_services_io_BasicIO_PrintWriter {
  struct ovm_core_OVMBase _parent_;
  struct ovm_core_execution_Native_FilePtr* fp;
};
struct ovm_services_bytecode_Instruction_FLOAD_3 {
  struct ovm_services_bytecode_Instruction_ConcreteFLoad _parent_;
};
struct ovm_services_bytecode_Instruction_FLOAD_2 {
  struct ovm_services_bytecode_Instruction_ConcreteFLoad _parent_;
};
struct ovm_services_bytecode_Instruction_FLOAD_1 {
  struct ovm_services_bytecode_Instruction_ConcreteFLoad _parent_;
};
struct ovm_services_bytecode_Instruction_LocalRead2 {
  struct ovm_services_bytecode_Instruction_TwoByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_ILOAD {
  struct ovm_services_bytecode_Instruction_LocalRead2 _parent_;
};
struct ovm_services_bytecode_Instruction_ALOAD {
  struct ovm_services_bytecode_Instruction_LocalRead2 _parent_;
};
struct ovm_services_bytecode_Instruction_UNIMPLEMENTED {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_core_services_threads_OVMThreadCoreImpl {
  struct ovm_core_OVMBase _parent_;
  struct ovm_core_services_threads_OVMThreadContext* ctx;
};
struct s3_services_threads_BasicPriorityOVMThreadImpl {
  struct ovm_core_services_threads_OVMThreadCoreImpl _parent_;
  struct s3_util_queues_SingleLinkElement* next;
  jint priority;
  struct ovm_services_monitors_Monitor* waitingMonitor;
};
struct s3_services_realtime_RealtimeOVMThreadImpl {
  struct s3_services_threads_BasicPriorityOVMThreadImpl _parent_;
};
struct ovm_services_bytecode_Instruction_ThreeByteInstruction {
  struct ovm_services_bytecode_Instruction _parent_;
};
struct ovm_services_bytecode_Instruction_FlowChange {
  struct ovm_services_bytecode_Instruction_ThreeByteInstruction _parent_;
  struct ovm_services_bytecode_SpecificationIR_PCValue* jumpTarget;
};
struct ovm_services_bytecode_Instruction_ConditionalJump {
  struct ovm_services_bytecode_Instruction_FlowChange _parent_;
  struct ovm_services_bytecode_SpecificationIR_Value* controlValue;
};
struct ovm_services_bytecode_Instruction_IfCmp {
  struct ovm_services_bytecode_Instruction_ConditionalJump _parent_;
};
struct ovm_services_bytecode_Instruction_If {
  struct ovm_services_bytecode_Instruction_ConditionalJump _parent_;
};
struct ovm_services_bytecode_Instruction_BinOp {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_LongShift {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_services_bytecode_Instruction_BinOpLong {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_services_bytecode_InstructionSet {
  struct java_lang_Object _parent_;
  struct arr_ovm_services_bytecode_Instruction* set;
};
struct ovm_services_bytecode_Instruction_ALOAD_2 {
  struct ovm_services_bytecode_Instruction_ConcreteALoad _parent_;
};
struct s3_core_S3Base {
  struct ovm_core_OVMBase _parent_;
};
struct s3_core_repository_S3Mode {
  struct s3_core_S3Base _parent_;
};
struct ovm_services_bytecode_Instruction_ALOAD_1 {
  struct ovm_services_bytecode_Instruction_ConcreteALoad _parent_;
};
struct ovm_services_bytecode_Instruction_ALOAD_0 {
  struct ovm_services_bytecode_Instruction_ConcreteALoad _parent_;
};
struct ovm_services_bytecode_Instruction_LLOAD {
  struct ovm_services_bytecode_Instruction_LocalRead2 _parent_;
};
struct ovm_services_bytecode_Instruction_DLOAD {
  struct ovm_services_bytecode_Instruction_LocalRead2 _parent_;
};
struct java_lang_Throwable {
  struct java_lang_Object _parent_;
  struct java_lang_String* message;
  struct java_lang_String* trace_;
};
struct java_lang_Exception {
  struct java_lang_Throwable _parent_;
};
struct java_lang_IllegalAccessException {
  struct java_lang_Exception _parent_;
};
struct ovm_services_bytecode_Instruction_FLOAD {
  struct ovm_services_bytecode_Instruction_LocalRead2 _parent_;
};
struct ovm_services_bytecode_Instruction_Invocation_Quick {
  struct ovm_services_bytecode_Instruction_ThreeByteInstruction _parent_;
};
struct ovm_util_ByteOrder {
  struct ovm_core_OVMBase _parent_;
  struct java_lang_String* name;
};
struct ovm_core_Mode_Class_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_FieldAccess_Quick {
  struct ovm_services_bytecode_Instruction_ThreeByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_ConstantLoad {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
  jint n;
};
struct java_lang_Error {
  struct java_lang_Throwable _parent_;
};
struct ovm_util_OVMError {
  struct java_lang_Error _parent_;
  struct java_lang_Throwable* cause;
};
struct ovm_util_ByteBuffer_BufferUnderflowException {
  struct ovm_util_OVMError _parent_;
};
struct ovm_services_bytecode_Instruction_ConcreteDStore {
  struct ovm_services_bytecode_Instruction_LocalWrite _parent_;
};
struct ovm_util_Buffer {
  struct ovm_core_OVMBase _parent_;
  jint mark;
};
struct ovm_util_ByteBuffer {
  struct ovm_util_Buffer _parent_;
  jint pos;
  zint isLittleEndian;
};
struct ovm_util_FixedByteBuffer {
  struct ovm_util_ByteBuffer _parent_;
  struct arr_jbyte* data;
  jint limit;
};
struct ovm_util_GrowableByteBuffer {
  struct ovm_util_FixedByteBuffer _parent_;
  jint pageSize;
};
struct ovm_services_bytecode_Instruction_BinOpDouble {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_services_bytecode_Instruction_BinOpFloat {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_util_ByteBuffer_BufferOverflowException {
  struct ovm_util_OVMError _parent_;
};
struct ovm_services_bytecode_Instruction_BinOpInt {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_core_services_memory_Allocator_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_IConstantLoad {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct java_lang_IllegalMonitorStateException {
  struct java_lang_Exception _parent_;
};
struct arr_arr_arr_arr_java_lang_Object {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_java_lang_Object* values[0];
};
struct java_lang_Cloneable {
  struct java_lang_Object _parent_;
};
struct java_lang_CloneNotSupportedException {
  struct java_lang_Exception _parent_;
};
struct java_lang_StringBuffer {
  struct java_lang_Object _parent_;
  struct arr_jbyte* data;
  jint count;
  jint capacity;
};
struct ovm_services_bytecode_Instruction_ConcreteLStore {
  struct ovm_services_bytecode_Instruction_LocalWrite _parent_;
};
struct arr_arr_arr_arr_arr_java_lang_Object {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_arr_java_lang_Object* values[0];
};
struct ovm_services_bytecode_Instruction_ConcreteFStore {
  struct ovm_services_bytecode_Instruction_LocalWrite _parent_;
};
struct s3_core_services_timer_TimerManagerImpl_Helper {
  struct java_lang_Object _parent_;
};
struct ovm_util_HTObject2Object {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_java_lang_Object* keys_;
  struct arr_java_lang_Object* values_;
  struct arr_ovm_util_HTObject2Object_Binding* collisions_;
  struct ovm_util_HTObject2Object* complementaryView_;
};
struct ovm_util_HTObject2Object_ReadOnly {
  struct ovm_util_HTObject2Object _parent_;
};
struct ovm_services_bytecode_Instruction_ConstantPoolRead {
  struct ovm_services_bytecode_Instruction_ThreeByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_ConstantPoolLoad {
  struct ovm_services_bytecode_Instruction_ConstantPoolRead _parent_;
};
struct ovm_util_OVMError_ClassCast {
  struct ovm_util_OVMError _parent_;
};
struct ovm_util_HTObject2Object_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct ovm_util_HTObject2Object_Binding* currentBinding_;
  zint reachEnd;
  struct ovm_util_HTObject2Object* this$0;
};
struct java_lang_LinkageError {
  struct java_lang_Error _parent_;
};
struct java_lang_NoClassDefFoundError {
  struct java_lang_LinkageError _parent_;
};
struct ovm_util_OVMError_UnsupportedOperation {
  struct ovm_util_OVMError _parent_;
};
struct ovm_services_bytecode_Instruction_Synchronization {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct arr_arr_java_lang_Object {
    struct java_lang_Object _parent_;
    int length;
    struct arr_java_lang_Object* values[0];
};
struct arr_arr_java_lang_String {
    struct java_lang_Object _parent_;
    int length;
    struct arr_java_lang_String* values[0];
};
struct ovm_util_HTObject2Object_1 {
  struct ovm_util_HTObject2Object _parent_;
};
struct java_lang_ClassFormatError {
  struct java_lang_Error _parent_;
};
struct ovm_services_bytecode_Instruction_Allocation {
  struct ovm_services_bytecode_Instruction _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_ArrayAccess {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_util_HTObject2Object_Binding {
  struct ovm_core_OVMBase _parent_;
  struct ovm_util_HTObject2Object_Binding* link;
  struct java_lang_Object* key;
  struct java_lang_Object* value;
};
struct ovm_services_bytecode_Instruction_Resolution {
  struct ovm_services_bytecode_Instruction_ConstantPoolRead _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_FieldAccess {
  struct ovm_services_bytecode_Instruction_Resolution _parent_;
};
struct ovm_util_OVMError_Configuration {
  struct ovm_util_OVMError _parent_;
};
struct java_lang_RuntimeException {
  struct java_lang_Exception _parent_;
};
struct java_lang_ClassCastException {
  struct java_lang_RuntimeException _parent_;
};
struct arr_arr_arr_java_lang_Object {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_java_lang_Object* values[0];
};
struct java_lang_IncompatibleClassChangeError {
  struct java_lang_LinkageError _parent_;
};
struct ovm_util_OVMException {
  struct java_lang_Exception _parent_;
  struct java_lang_Throwable* cause;
};
struct ovm_core_domain_LinkageException {
  struct ovm_util_OVMException _parent_;
};
struct ovm_core_domain_LinkageException_DomainFrozen {
  struct ovm_core_domain_LinkageException _parent_;
  struct java_lang_String* msg;
};
struct ovm_core_domain_LinkageException_RepositoryClassNotFound {
  struct ovm_core_domain_LinkageException _parent_;
  struct ovm_core_TypeName* tn;
};
struct arr_ovm_util_HTObject2Object_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_HTObject2Object_Binding* values[0];
};
struct ovm_services_bytecode_Instruction_LinkSetAccess {
  struct ovm_services_bytecode_Instruction_ThreeByteInstruction _parent_;
};
struct java_lang_VirtualMachineError {
  struct java_lang_Error _parent_;
};
struct ovm_util_OVMRuntimeException {
  struct java_lang_RuntimeException _parent_;
  struct java_lang_Throwable* cause;
};
struct ovm_core_domain_LinkageException_Runtime {
  struct ovm_util_OVMRuntimeException _parent_;
  struct ovm_core_domain_LinkageException* this$0;
};
struct java_lang_String {
  struct java_lang_Object _parent_;
  struct arr_jbyte* data;
  jint count;
  jint hashCode;
};
struct ovm_services_bytecode_Instruction_FourByteInstruction {
  struct ovm_services_bytecode_Instruction _parent_;
};
struct ovm_services_bytecode_Instruction_Switch {
  struct ovm_services_bytecode_Instruction_FlowChange _parent_;
};
struct arr_java_lang_Object {
    struct java_lang_Object _parent_;
    int length;
    struct java_lang_Object* values[0];
};
struct java_lang_NullPointerException {
  struct java_lang_RuntimeException _parent_;
};
struct arr_java_lang_String {
    struct java_lang_Object _parent_;
    int length;
    struct java_lang_String* values[0];
};
struct ovm_services_bytecode_Instruction_UnconditionalJump {
  struct ovm_services_bytecode_Instruction_FlowChange _parent_;
};
struct java_lang_IndexOutOfBoundsException {
  struct java_lang_RuntimeException _parent_;
};
struct java_lang_ArrayIndexOutOfBoundsException {
  struct java_lang_IndexOutOfBoundsException _parent_;
};
struct ovm_services_bytecode_Instruction_IADD {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct ovm_services_bytecode_Instruction_LXOR {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
};
struct ovm_services_bytecode_Instruction_LAND {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
};
struct ovm_services_bytecode_Instruction_LOR {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
};
struct ovm_services_bytecode_Instruction_LREM {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_core_stitcher_OVMStitcher {
  struct ovm_core_OVMBase _parent_;
  struct ovm_util_HTObject2Object* map_;
};
struct ovm_services_bytecode_Instruction_LDIV {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_LMUL {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
};
struct ovm_services_bytecode_Instruction_IXOR {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct ovm_services_bytecode_Instruction_IAND {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct ovm_services_bytecode_Instruction_IOR {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct ovm_services_bytecode_Instruction_IREM {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_IDIV {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_IMUL {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct ovm_services_bytecode_Instruction_ISUB {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct ovm_services_bytecode_Instruction_SWAP {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct java_lang_IllegalThreadStateException {
  struct java_lang_Exception _parent_;
};
struct ovm_services_bytecode_Instruction_POP2 {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_POP {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_DUP2_X2 {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_DUP2_X1 {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_DUP2 {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_DUP_X2 {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_LSUB {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
};
struct ovm_core_Mode_Member {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_LADD {
  struct ovm_services_bytecode_Instruction_BinOpLong _parent_;
};
struct ovm_services_bytecode_Instruction_DREM {
  struct ovm_services_bytecode_Instruction_BinOpDouble _parent_;
};
struct ovm_util_Map {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_DDIV {
  struct ovm_services_bytecode_Instruction_BinOpDouble _parent_;
};
struct ovm_services_bytecode_Instruction_DMUL {
  struct ovm_services_bytecode_Instruction_BinOpDouble _parent_;
};
struct ovm_services_bytecode_Instruction_DSUB {
  struct ovm_services_bytecode_Instruction_BinOpDouble _parent_;
};
struct ovm_services_bytecode_Instruction_DADD {
  struct ovm_services_bytecode_Instruction_BinOpDouble _parent_;
};
struct ovm_services_bytecode_Instruction_L2F {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_L2D {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_L2I {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_core_repository_RepositoryMember_Method_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_I2S {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_I2F {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_util_Set {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_I2L {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_I2D {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_I2C {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_DUP_X1 {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct s3_core_execution_S3BottomFrame {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryClass_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_DUP {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_PUTSTATIC {
  struct ovm_services_bytecode_Instruction_FieldAccess _parent_;
};
struct ovm_services_bytecode_Instruction_PUTFIELD {
  struct ovm_services_bytecode_Instruction_FieldAccess _parent_;
};
struct ovm_core_repository_RepositoryMember_Field_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_GETSTATIC {
  struct ovm_services_bytecode_Instruction_FieldAccess _parent_;
};
struct java_lang_InterruptedException {
  struct java_lang_Exception _parent_;
};
struct ovm_services_bytecode_Instruction_GETFIELD {
  struct ovm_services_bytecode_Instruction_FieldAccess _parent_;
};
struct ovm_core_repository_RepositoryConstantPool_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_NEWARRAY {
  struct ovm_services_bytecode_Instruction_Allocation _parent_;
  struct arr_ovm_core_repository_RepositoryClass* exceptions;
};
struct ovm_services_bytecode_Instruction_MULTIANEWARRAY {
  struct ovm_services_bytecode_Instruction_Allocation _parent_;
};
struct ovm_core_Mode_Method_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_core_Mode_ModeVisitor {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_ARRAYLENGTH {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_ANEWARRAY {
  struct ovm_services_bytecode_Instruction_Resolution _parent_;
};
struct ovm_services_bytecode_Instruction_SIPUSH {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_LDC2_W {
  struct ovm_services_bytecode_Instruction_ConstantPoolLoad _parent_;
};
struct ovm_core_Mode_AllModes {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_I2B {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_util_JDK2OVM {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_services_bytecode_Instruction_F2I {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_F2L {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_F2D {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_core_Mode_AllModes_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_D2L {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_D2I {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_D2F {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_core_Mode_Field_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_ICONST_3 {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_ICONST_2 {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_ICONST_1 {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct arr_java_lang_Class {
    struct java_lang_Object _parent_;
    int length;
    struct java_lang_Class* values[0];
};
struct ovm_services_bytecode_Instruction_ICONST_0 {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_ICONST_M1 {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct ovm_core_stitcher_MonitorServicesFactory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_FCONST_2 {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct ovm_core_stitcher_TimerServicesFactory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_LDC_W {
  struct ovm_services_bytecode_Instruction_ConstantPoolLoad _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_LDC {
  struct ovm_services_bytecode_Instruction_ConstantPoolLoad _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_LCONST_1 {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct java_lang_Class {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_LCONST_0 {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_ICONST_5 {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct ovm_util_Runabout {
  struct java_lang_Object _parent_;
  struct ovm_util_Runabout_HTClass2Runabout_Code* map_;
  zint mapIsPrivate_;
  struct ovm_util_Runabout_Code* noCode_;
};
struct ovm_services_bytecode_Instruction_ICONST_4 {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct s3_core_Constants {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_SASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_core_repository_RepositoryBundle_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_LASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_core_repository_UTF8Store {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_IASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_core_repository_RepositoryDescriptor_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_FASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_core_repository_RepositoryUnboundSelector_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_DASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_core_stitcher_ServiceConfigurator {
  struct ovm_core_OVMBase _parent_;
  struct ovm_core_stitcher_HTString2ServiceFactory* factories;
};
struct ovm_services_bytecode_Instruction_CASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_core_execution_Processor {
  struct ovm_core_OVMBase _parent_;
  jint processor_;
};
struct ovm_services_bytecode_Instruction_FCONST_1 {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct ovm_core_execution_RuntimeExports {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_FCONST_0 {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_DCONST_1 {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_DCONST_0 {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_BIPUSH {
  struct ovm_services_bytecode_Instruction_IConstantLoad _parent_;
};
struct ovm_services_bytecode_Instruction_ACONST_NULL {
  struct ovm_services_bytecode_Instruction_ConstantLoad _parent_;
};
struct arr_ovm_core_execution_Processor {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_execution_Processor* values[0];
};
struct ovm_services_bytecode_Instruction_DALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_FALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_IALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_AALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_DSTORE_3 {
  struct ovm_services_bytecode_Instruction_ConcreteDStore _parent_;
};
struct ovm_services_bytecode_Instruction_DSTORE_2 {
  struct ovm_services_bytecode_Instruction_ConcreteDStore _parent_;
};
struct ovm_services_bytecode_Instruction_BASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_AASTORE {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_LALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_SALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_CALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct s3_util_PragmaException {
  struct java_lang_RuntimeException _parent_;
};
struct s3_util_PragmaInlineSubstituteBytecode {
  struct s3_util_PragmaException _parent_;
};
struct ovm_core_services_memory_VM_Address_BC {
  struct s3_util_PragmaInlineSubstituteBytecode _parent_;
};
struct ovm_core_services_memory_VM_Address_BCgetword {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_services_bytecode_Instruction_BALOAD {
  struct ovm_services_bytecode_Instruction_ArrayAccess _parent_;
};
struct ovm_services_bytecode_Instruction_FSTORE_3 {
  struct ovm_services_bytecode_Instruction_ConcreteFStore _parent_;
};
struct ovm_services_bytecode_Instruction_FSTORE_2 {
  struct ovm_services_bytecode_Instruction_ConcreteFStore _parent_;
};
struct ovm_services_bytecode_Instruction_FSTORE_1 {
  struct ovm_services_bytecode_Instruction_ConcreteFStore _parent_;
};
struct ovm_services_bytecode_Instruction_FSTORE_0 {
  struct ovm_services_bytecode_Instruction_ConcreteFStore _parent_;
};
struct ovm_services_bytecode_Instruction_ISTORE_3 {
  struct ovm_services_bytecode_Instruction_ConcreteIStore _parent_;
};
struct ovm_services_bytecode_Instruction_ISTORE_2 {
  struct ovm_services_bytecode_Instruction_ConcreteIStore _parent_;
};
struct ovm_services_bytecode_Instruction_DSTORE_1 {
  struct ovm_services_bytecode_Instruction_ConcreteDStore _parent_;
};
struct s3_core_stitcher_S3ServiceConfigurator {
  struct ovm_core_stitcher_ServiceConfigurator _parent_;
  struct s3_core_stitcher_S3ServiceConfigurator_BasicThreadSupport* bts;
};
struct ovm_services_bytecode_Instruction_DSTORE_0 {
  struct ovm_services_bytecode_Instruction_ConcreteDStore _parent_;
};
struct ovm_util_AbstractMap {
  struct ovm_core_OVMBase _parent_;
  struct ovm_util_Collection* valueCollection;
  struct ovm_util_Set* keySet;
};
struct ovm_util_IdentityHashMap {
  struct ovm_util_AbstractMap _parent_;
  jint capacity;
  jint size;
  jfloat loadFactor;
  jint threshold;
  struct arr_ovm_util_Bucket* buckets;
  jint modCount;
};
struct ovm_services_bytecode_Instruction_LSTORE_3 {
  struct ovm_services_bytecode_Instruction_ConcreteLStore _parent_;
};
struct ovm_services_bytecode_Instruction_LSTORE_2 {
  struct ovm_services_bytecode_Instruction_ConcreteLStore _parent_;
};
struct ovm_services_bytecode_Instruction_LSTORE_1 {
  struct ovm_services_bytecode_Instruction_ConcreteLStore _parent_;
};
struct ovm_util_OVMRuntimeException_ImageHeadersNotFrozen {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct ovm_services_bytecode_Instruction_LSTORE_0 {
  struct ovm_services_bytecode_Instruction_ConcreteLStore _parent_;
};
struct java_io_OutputStream {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Field_Iterator {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_PUTFIELD2_QUICK {
  struct ovm_services_bytecode_Instruction_FieldAccess_Quick _parent_;
};
struct ovm_core_domain_Method_Iterator {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_REF_GETFIELD_QUICK {
  struct ovm_services_bytecode_Instruction_FieldAccess_Quick _parent_;
};
struct arr_ovm_core_domain_Type_Interface {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_domain_Type_Interface* values[0];
};
struct ovm_services_bytecode_Instruction_GETFIELD_QUICK {
  struct ovm_services_bytecode_Instruction_FieldAccess_Quick _parent_;
};
struct arr_ovm_core_domain_Type_Reference {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_domain_Type_Reference* values[0];
};
struct ovm_services_bytecode_Instruction_INVOKESUPER_QUICK {
  struct ovm_services_bytecode_Instruction_Invocation_Quick _parent_;
};
struct ovm_core_domain_Type_Record {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Builder {
  struct s3_core_S3Base _parent_;
  struct arr_s3_core_repository_S3Attribute* attributes;
  jint position_in_attributes;
  struct s3_core_repository_S3Attribute* first_element;
  struct s3_core_repository_S3Attribute* second_element;
  struct s3_core_repository_S3Attribute* third_element;
  zint freeze_attributes_;
};
struct ovm_core_domain_Type_WidePrimitive {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKENONVIRTUAL2_QUICK {
  struct ovm_services_bytecode_Instruction_Invocation_Quick _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKENONVIRTUAL_QUICK {
  struct ovm_services_bytecode_Instruction_Invocation_Quick _parent_;
};
struct ovm_core_domain_Type_Interface {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKEVIRTUAL_QUICK {
  struct ovm_services_bytecode_Instruction_Invocation_Quick _parent_;
};
struct ovm_core_domain_Type_Reference {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Attribute {
  struct s3_core_S3Base _parent_;
};
struct s3_core_repository_S3Attribute_NonCritical {
  struct s3_core_repository_S3Attribute _parent_;
};
struct s3_core_repository_S3Attribute_ThirdParty {
  struct s3_core_repository_S3Attribute_NonCritical _parent_;
  jint attributeNameIndex_;
  struct arr_jbyte* attributeContent_;
};
struct ovm_services_bytecode_Instruction_ANEWARRAY_QUICK {
  struct ovm_services_bytecode_Instruction_LinkSetAccess _parent_;
};
struct s3_core_repository_S3Attribute_SourceFile {
  struct s3_core_repository_S3Attribute_NonCritical _parent_;
  jint srcFileNameIndex_;
};
struct ovm_services_bytecode_Instruction_RET {
  struct ovm_services_bytecode_Instruction_FlowChange _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_RET {
  struct ovm_services_bytecode_Instruction_RET _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_LSTORE {
  struct ovm_services_bytecode_Instruction_LSTORE _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_DSTORE {
  struct ovm_services_bytecode_Instruction_DSTORE _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_ASTORE {
  struct ovm_services_bytecode_Instruction_ASTORE _parent_;
};
struct ovm_services_bytecode_Instruction_PUTFIELD_QUICK {
  struct ovm_services_bytecode_Instruction_FieldAccess_Quick _parent_;
};
struct java_lang_StringIndexOutOfBoundsException {
  struct java_lang_IndexOutOfBoundsException _parent_;
};
struct ovm_core_execution_CoreServicesAccess {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_services_bytecode_Instruction_MULTIANEWARRAY_QUICK {
  struct ovm_services_bytecode_Instruction_LinkSetAccess _parent_;
};
struct s3_core_repository_S3Attribute_Deprecated {
  struct s3_core_repository_S3Attribute_NonCritical _parent_;
};
struct java_lang_InternalError {
  struct java_lang_Error _parent_;
};
struct ovm_services_bytecode_Instruction_NEW_QUICK {
  struct ovm_services_bytecode_Instruction_LinkSetAccess _parent_;
};
struct arr_s3_core_repository_S3Attribute {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Attribute* values[0];
};
struct ovm_services_bytecode_Instruction_INSTANCEOF_QUICK {
  struct ovm_services_bytecode_Instruction_LinkSetAccess _parent_;
};
struct ovm_services_bytecode_Instruction_CHECKCAST_QUICK {
  struct ovm_services_bytecode_Instruction_LinkSetAccess _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_FLOAD {
  struct ovm_services_bytecode_Instruction_FLOAD _parent_;
};
struct s3_core_repository_S3Field {
  struct s3_core_S3Base _parent_;
  struct ovm_core_Mode_Field* mode_;
  jlong constantValue_;
  struct arr_s3_core_repository_S3Attribute* attributes_;
  struct s3_core_repository_S3UnboundSelector_Field* selector_;
};
struct ovm_services_bytecode_Instruction_WIDE_ILOAD {
  struct ovm_services_bytecode_Instruction_ILOAD _parent_;
};
struct ovm_services_bytecode_Instruction_IINC {
  struct ovm_services_bytecode_Instruction_ThreeByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_IINC {
  struct ovm_services_bytecode_Instruction_IINC _parent_;
};
struct s3_core_repository_S3Attribute_Synthetic {
  struct s3_core_repository_S3Attribute_NonCritical _parent_;
};
struct arr_s3_core_repository_S3ExceptionHandler {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3ExceptionHandler* values[0];
};
struct ovm_services_bytecode_Instruction_WIDE_INVOKESUPER_QUICK {
  struct ovm_services_bytecode_Instruction_INVOKESUPER_QUICK _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_INVOKENONVIRTUAL2_QUICK {
  struct ovm_services_bytecode_Instruction_INVOKENONVIRTUAL2_QUICK _parent_;
};
struct s3_core_repository_S3Attribute_LocalVariableTable {
  struct s3_core_repository_S3Attribute_NonCritical _parent_;
  struct arr_jchar* tableStartPC_;
  struct arr_jchar* tableLength_;
  struct arr_jint* tableNameIndex_;
  struct arr_ovm_core_repository_RepositoryDescriptor_Field* descriptors_;
  struct arr_jchar* tableIndex_;
};
struct arr_s3_core_repository_S3Field {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Field* values[0];
};
struct ovm_services_bytecode_Instruction_WIDE_FSTORE {
  struct ovm_services_bytecode_Instruction_FSTORE _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_ISTORE {
  struct ovm_services_bytecode_Instruction_ISTORE _parent_;
};
struct ovm_services_bytecode_Instruction_WIDE_DLOAD {
  struct ovm_services_bytecode_Instruction_DLOAD _parent_;
};
struct s3_core_repository_S3ExceptionHandler {
  struct s3_core_S3Base _parent_;
  cint startPC_;
  cint endPC_;
  cint handlerPC_;
  struct s3_core_repository_S3TypeName_Scalar* catchTypeName_;
};
struct ovm_services_bytecode_Instruction_WIDE_LLOAD {
  struct ovm_services_bytecode_Instruction_LLOAD _parent_;
};
struct s3_core_repository_S3Attribute_LineNumberTable {
  struct s3_core_repository_S3Attribute_NonCritical _parent_;
  struct arr_jchar* tableStartPC_;
  struct arr_jchar* tableLineNumber_;
};
struct ovm_services_bytecode_Instruction_WIDE_ALOAD {
  struct ovm_services_bytecode_Instruction_ALOAD _parent_;
};
struct ovm_services_bytecode_Instruction_NEW {
  struct ovm_services_bytecode_Instruction_Resolution _parent_;
};
struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_ovm_core_repository_RepositoryUnboundSelector_Method* keys_;
  struct arr_ovm_core_repository_RepositoryMember_Method* values_;
  struct arr_s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_Binding* collisions_;
  struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method* complementaryView_;
};
struct arr_ovm_core_repository_RepositoryBundle {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryBundle* values[0];
};
struct ovm_services_bytecode_Instruction_CHECKCAST {
  struct ovm_services_bytecode_Instruction_Resolution _parent_;
};
struct arr_s3_core_repository_S3Attribute_InnerClasses {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Attribute_InnerClasses* values[0];
};
struct ovm_services_bytecode_Instruction_INSTANCEOF {
  struct ovm_services_bytecode_Instruction_Resolution _parent_;
};
struct arr_s3_core_repository_S3Class {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Class* values[0];
};
struct ovm_services_bytecode_Instruction_NOP {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_ATHROW {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_MONITOREXIT {
  struct ovm_services_bytecode_Instruction_Synchronization _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct ovm_services_bytecode_Instruction_MONITORENTER {
  struct ovm_services_bytecode_Instruction_Synchronization _parent_;
  struct arr_ovm_core_TypeName_Scalar* exceptions_;
};
struct s3_core_repository_LISTTypeName_Scalar {
  struct ovm_core_OVMBase _parent_;
  struct arr_ovm_core_TypeName_Scalar* elements_;
  jint elementsCount_;
};
struct ovm_core_repository_RepositoryBundle {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Attribute_InnerClasses {
  struct s3_core_repository_S3Attribute _parent_;
  struct arr_ovm_core_TypeName_Scalar* innerClass_;
  struct arr_ovm_core_TypeName_Scalar* outerClass_;
  struct arr_jint* innerNameIndex_;
  struct arr_ovm_core_Mode_Class* mode_;
};
struct ovm_services_bytecode_Instruction_WIDE_INVOKENONVIRTUAL_QUICK {
  struct ovm_services_bytecode_Instruction_INVOKENONVIRTUAL_QUICK _parent_;
};
struct s3_core_repository_S3Class {
  struct s3_core_S3Base _parent_;
  struct s3_core_repository_S3TypeName_Scalar* name_;
};
struct ovm_services_bytecode_Instruction_WIDE_INVOKEVIRTUAL_QUICK {
  struct ovm_services_bytecode_Instruction_INVOKEVIRTUAL_QUICK _parent_;
};
struct ovm_core_Selector_Method_Iterator {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_LISTRepositoryMember_Field {
  struct ovm_core_OVMBase _parent_;
  struct arr_ovm_core_repository_RepositoryMember_Field* elements_;
  jint elementsCount_;
};
struct ovm_services_bytecode_Instruction_WIDE {
  struct ovm_services_bytecode_Instruction _parent_;
};
struct ovm_services_bytecode_Instruction_TABLESWITCH {
  struct ovm_services_bytecode_Instruction_Switch _parent_;
};
struct ovm_core_TypeName_WidePrimitive {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_LOOKUPSWITCH {
  struct ovm_services_bytecode_Instruction_Switch _parent_;
};
struct s3_core_repository_S3Class_Impl {
  struct s3_core_repository_S3Class _parent_;
  struct ovm_core_Mode_Class* mode_;
  cint minorVersion_;
  cint majorVersion_;
  struct arr_s3_core_repository_S3TypeName_Compound* linkSet_;
  struct s3_core_repository_S3TypeName_Scalar* super_;
  struct arr_s3_core_repository_S3TypeName_Scalar* interfaces_;
  struct arr_s3_core_repository_S3Method* instanceMethods_;
  struct arr_s3_core_repository_S3Method* staticMethods_;
  struct arr_s3_core_repository_S3Field* instanceFields_;
  struct arr_s3_core_repository_S3Field* staticFields_;
  struct arr_s3_core_repository_S3TypeName_Scalar* instanceClasses_;
  struct arr_s3_core_repository_S3TypeName_Scalar* staticClasses_;
  struct arr_s3_core_repository_S3Attribute* attributes_;
  struct s3_core_repository_S3ConstantPool* constantPool_;
  struct s3_core_repository_S3TypeName_Scalar* outer_;
};
struct ovm_services_bytecode_Instruction_JSRS {
  struct ovm_services_bytecode_Instruction_UnconditionalJump _parent_;
};
struct ovm_services_bytecode_Instruction_JSR_W {
  struct ovm_services_bytecode_Instruction_JSRS _parent_;
};
struct ovm_services_bytecode_Instruction_JSR {
  struct ovm_services_bytecode_Instruction_JSRS _parent_;
};
struct s3_core_repository_LISTRepositoryMember_Field_Iterator {
  struct java_lang_Object _parent_;
  jint pos_;
  struct s3_core_repository_LISTRepositoryMember_Field* this$0;
};
struct ovm_services_bytecode_Instruction_Invocation {
  struct ovm_services_bytecode_Instruction_Resolution _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKESTATIC {
  struct ovm_services_bytecode_Instruction_Invocation _parent_;
};
struct ovm_core_TypeName_Scalar_Iterator {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKEVIRTUAL {
  struct ovm_services_bytecode_Instruction_Invocation _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKESPECIAL {
  struct ovm_services_bytecode_Instruction_Invocation _parent_;
};
struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method* this$0;
};
struct ovm_core_execution_Interpreter {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKEINTERFACE {
  struct ovm_services_bytecode_Instruction_Invocation _parent_;
};
struct ovm_services_bytecode_Instruction_IFEQ {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ICMPGE {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ICMPGT {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_core_execution_LibraryImports {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ICMPLE {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ICMPLT {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ICMPNE {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_services_bytecode_Instruction_IFNULL {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_services_bytecode_Instruction_IFNONNULL {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_core_services_memory_Services {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_IFGE {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_services_bytecode_Instruction_IFGT {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_services_bytecode_Instruction_IFLE {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_services_bytecode_Instruction_IFLT {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_services_bytecode_Instruction_IFNE {
  struct ovm_services_bytecode_Instruction_If _parent_;
};
struct ovm_services_bytecode_Instruction_LSHL {
  struct ovm_services_bytecode_Instruction_LongShift _parent_;
};
struct ovm_services_bytecode_Instruction_LSHR {
  struct ovm_services_bytecode_Instruction_LongShift _parent_;
};
struct ovm_services_bytecode_Instruction_LUSHR {
  struct ovm_services_bytecode_Instruction_LongShift _parent_;
};
struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_ReadOnly {
  struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method _parent_;
};
struct java_io_IOException {
  struct java_lang_Exception _parent_;
};
struct ovm_services_bytecode_Instruction_IUSHR {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct s3_core_execution_PackageEnvironment {
  struct java_lang_Object _parent_;
  struct ovm_core_execution_CoreServicesAccess* csa_;
  struct ovm_core_execution_Context_NativeContextFactory* nativeContextFactory_;
};
struct ovm_services_bytecode_Instruction_ISHR {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct ovm_services_bytecode_Instruction_ISHL {
  struct ovm_services_bytecode_Instruction_BinOpInt _parent_;
};
struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_Binding* link;
  struct ovm_core_repository_RepositoryUnboundSelector_Method* key;
  struct ovm_core_repository_RepositoryMember_Method* value;
};
struct ovm_services_bytecode_Instruction_LCMP {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ICMPEQ {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ACMPNE {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_services_bytecode_Instruction_IF_ACMPEQ {
  struct ovm_services_bytecode_Instruction_IfCmp _parent_;
};
struct ovm_services_bytecode_Instruction_GOTOS {
  struct ovm_services_bytecode_Instruction_UnconditionalJump _parent_;
};
struct ovm_services_bytecode_Instruction_GOTO_W {
  struct ovm_services_bytecode_Instruction_GOTOS _parent_;
};
struct s3_core_execution_S3CoreServicesAccess {
  struct ovm_core_execution_CoreServicesAccess _parent_;
  struct ovm_services_monitors_MonitorMapper* mapper;
  struct ovm_core_services_timer_TimerManager* timer;
  struct ovm_core_services_memory_Allocator* allocator;
  zint traceOn_;
  zint DEBUG_THROW;
  zint DEBUG_GEN;
  zint warningDone;
  zint DEBUG_MONITOR;
};
struct ovm_services_bytecode_Instruction_GOTO {
  struct ovm_services_bytecode_Instruction_GOTOS _parent_;
};
struct arr_s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_Binding* values[0];
};
struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method_1 {
  struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method _parent_;
};
struct ovm_services_bytecode_Instruction_FCMPL {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_services_bytecode_Instruction_FCMPG {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_services_bytecode_Instruction_FREM {
  struct ovm_services_bytecode_Instruction_BinOpFloat _parent_;
};
struct ovm_services_monitors_Monitor_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_FDIV {
  struct ovm_services_bytecode_Instruction_BinOpFloat _parent_;
};
struct ovm_services_bytecode_Instruction_FMUL {
  struct ovm_services_bytecode_Instruction_BinOpFloat _parent_;
};
struct ovm_services_bytecode_Instruction_FSUB {
  struct ovm_services_bytecode_Instruction_BinOpFloat _parent_;
};
struct s3_core_repository_LISTRepositoryUnboundSelector_Field_Iterator {
  struct java_lang_Object _parent_;
  jint pos_;
  struct s3_core_repository_LISTRepositoryUnboundSelector_Field* this$0;
};
struct ovm_services_bytecode_Instruction_DCMPL {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct s3_core_repository_LISTTypeName_Scalar_Iterator {
  struct java_lang_Object _parent_;
  jint pos_;
  struct s3_core_repository_LISTTypeName_Scalar* this$0;
};
struct ovm_services_bytecode_Instruction_DCMPG {
  struct ovm_services_bytecode_Instruction_BinOp _parent_;
};
struct ovm_services_bytecode_Instruction_LNEG {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_DNEG {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_INEG {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct java_lang_System {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_FNEG {
  struct ovm_services_bytecode_Instruction_OneByteInstruction _parent_;
};
struct ovm_core_stitcher_HTString2ServiceFactory {
  struct ovm_core_OVMBase _parent_;
  jint mask;
  jint numElems_;
  struct arr_ovm_core_stitcher_HTString2ServiceFactory_Binding* buckets;
};
struct s3_core_stitcher_S3ServiceConfigurator_BasicThreadSupport {
  struct ovm_core_OVMBase _parent_;
  struct ovm_core_services_threads_ThreadManager* tm;
  struct ovm_core_domain_Services* domainServices;
  struct ovm_services_monitors_MonitorMapper* mapper;
  struct ovm_core_services_threads_OVMThread* primordialThread;
  struct s3_core_stitcher_S3ServiceConfigurator* this$0;
};
struct s3_services_threads_TimedSuspensionOVMThreadImpl {
  struct s3_services_threads_BasicPriorityOVMThreadImpl _parent_;
  struct s3_util_queues_SingleLinkDeltaElement* nextDelta;
  jlong delta;
};
struct s3_services_threads_JLThread {
  struct s3_services_threads_TimedSuspensionOVMThreadImpl _parent_;
  struct java_lang_String* name;
  struct java_lang_Runnable* target;
  jint lifecycleState;
  jint executionState;
  struct java_lang_Object* lock;
  zint interrupted;
};
struct s3_core_stitcher_S3ServiceConfigurator_PrimordialJLThread {
  struct s3_services_threads_JLThread _parent_;
};
struct ovm_core_repository_Services {
  struct java_lang_Object _parent_;
};
struct ovm_core_stitcher_HTString2ServiceFactory_Binding {
  struct ovm_core_OVMBase _parent_;
  struct ovm_core_stitcher_HTString2ServiceFactory_Binding* link;
  struct java_lang_String* key;
  struct ovm_core_stitcher_ServiceFactory* value;
};
struct arr_ovm_core_stitcher_HTString2ServiceFactory_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_stitcher_HTString2ServiceFactory_Binding* values[0];
};
struct ovm_core_stitcher_ThreadServicesFactory {
  struct java_lang_Object _parent_;
};
struct ovm_core_stitcher_ThreadDispatchServicesFactory {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Mode_HT_i_M {
  struct ovm_core_OVMBase _parent_;
  struct arr_s3_core_repository_S3Mode_HT_i_M_Binding* buckets;
};
struct s3_core_repository_S3Mode_HT_i_C {
  struct ovm_core_OVMBase _parent_;
  struct arr_s3_core_repository_S3Mode_HT_i_C_Binding* buckets;
};
struct s3_core_repository_S3Mode_HT_i_F {
  struct ovm_core_OVMBase _parent_;
  struct arr_s3_core_repository_S3Mode_HT_i_F_Binding* buckets;
};
struct s3_core_repository_S3Mode_BasicMode {
  struct ovm_core_OVMBase _parent_;
  jint mode_;
  struct java_lang_String* toString_;
};
struct s3_core_repository_S3Mode_ClassModeImpl {
  struct s3_core_repository_S3Mode_BasicMode _parent_;
};
struct arr_ovm_services_bytecode_Instruction {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_Instruction* values[0];
};
struct s3_core_repository_S3Mode_S3ModeFactory {
  struct ovm_core_OVMBase _parent_;
};
struct s3_core_repository_S3Mode_1 {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Mode_MethodModeImpl {
  struct s3_core_repository_S3Mode_BasicMode _parent_;
};
struct s3_core_repository_S3Mode_FieldModeImpl {
  struct s3_core_repository_S3Mode_BasicMode _parent_;
};
struct ovm_services_bytecode_MethodInformation {
  struct java_lang_Object _parent_;
};
struct ovm_core_TypeName_Nested {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Mode_BasicMode_Builder {
  struct java_lang_Object _parent_;
  jint flags;
};
struct s3_core_repository_S3Mode_MethodModeImpl_Builder {
  struct s3_core_repository_S3Mode_BasicMode_Builder _parent_;
};
struct s3_core_repository_S3Mode_FieldModeImpl_Builder {
  struct s3_core_repository_S3Mode_BasicMode_Builder _parent_;
};
struct s3_core_repository_S3Mode_ClassModeImpl_Builder {
  struct s3_core_repository_S3Mode_BasicMode_Builder _parent_;
};
struct ovm_services_bytecode_JVMConstants_Opcodes {
  struct java_lang_Object _parent_;
};
struct ovm_core_execution_Context {
  struct ovm_core_OVMBase _parent_;
  struct arr_jboolean* flags;
  jint nativeContextHandle_;
  struct ovm_core_execution_Activation* activation_;
  struct ovm_core_execution_CoreServicesAccess* csa_;
};
struct s3_services_bytecode_reader_PackageEnvironment {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_reader_CodeSource_Factory* csourceF;
  struct s3_services_bytecode_reader_S3Parser_Factory* parserF;
  struct s3_services_bytecode_reader_S3FileReader_Factory* freaderF;
  struct s3_services_bytecode_reader_S3Installer_Factory* installerF;
};
struct arr_ovm_core_Mode_Class {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_Mode_Class* values[0];
};
struct s3_services_bytecode_reader_S3CodeSource_StandaloneFactory {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_core_Mode_Class {
  struct java_lang_Object _parent_;
};
struct ovm_core_Mode_Field {
  struct java_lang_Object _parent_;
};
struct ovm_core_Mode_Method {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Mode_HT_i_M_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_S3Mode_HT_i_M_Binding* link;
  jint key;
  struct ovm_core_Mode_Method* value;
};
struct s3_core_services_memory_S3Allocator_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_ServiceInstanceImpl {
  struct ovm_core_OVMBase _parent_;
  zint isInited;
  zint isStarted;
};
struct s3_core_services_timer_TimerManagerImpl {
  struct ovm_services_ServiceInstanceImpl _parent_;
  struct arr_ovm_core_services_timer_TimerInterruptAction* actions;
  jint index;
  struct ovm_core_services_timer_FinalTimerInterruptAction* finalAction;
  zint enabled;
  zint started;
  zint stopped;
  jlong period;
};
struct ovm_core_repository_RepositoryNativeExceptionHandler {
  struct java_lang_Object _parent_;
};
struct java_util_Comparator {
  struct java_lang_Object _parent_;
};
struct arr_s3_core_repository_S3Mode_HT_i_M_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Mode_HT_i_M_Binding* values[0];
};
struct s3_core_services_memory_S3Allocator {
  struct ovm_core_OVMBase _parent_;
  struct arr_jint* memory_;
  jint pos_;
};
struct s3_services_realtime_RealtimeBaseThread {
  struct s3_services_realtime_RealtimeOVMThreadImpl _parent_;
  struct java_lang_Runnable* runnable;
  struct java_lang_String* name;
};
struct arr_ovm_core_repository_RepositoryNativeExceptionHandler {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryNativeExceptionHandler* values[0];
};
struct s3_core_repository_S3Mode_HT_i_C_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_S3Mode_HT_i_C_Binding* link;
  jint key;
  struct ovm_core_Mode_Class* value;
};
struct ovm_services_bytecode_Instruction_INVOKE_NATIVE {
  struct ovm_services_bytecode_Instruction_TwoByteInstruction _parent_;
};
struct s3_core_repository_S3Mode_HT_i_F_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_S3Mode_HT_i_F_Binding* link;
  jint key;
  struct ovm_core_Mode_Field* value;
};
struct ovm_services_bytecode_Instruction_INVOKE_SYSTEM {
  struct ovm_services_bytecode_Instruction_ThreeByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_ROLL {
  struct ovm_services_bytecode_Instruction_FourByteInstruction _parent_;
};
struct ovm_services_bytecode_Instruction_INVOKEINTERFACE_QUICK {
  struct ovm_services_bytecode_Instruction_Invocation_Quick _parent_;
};
struct arr_ovm_services_bytecode_SpecificationIR_IntValue {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_SpecificationIR_IntValue* values[0];
};
struct arr_s3_core_repository_S3Mode_HT_i_C_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Mode_HT_i_C_Binding* values[0];
};
struct arr_s3_core_repository_S3Mode_HT_i_F_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Mode_HT_i_F_Binding* values[0];
};
struct ovm_services_bytecode_SpecificationIR_IfExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_CondExp* cond;
  struct ovm_services_bytecode_SpecificationIR_Value* ifTrue;
  struct ovm_services_bytecode_SpecificationIR_Value* ifFalse;
};
struct ovm_services_bytecode_Services {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_CondExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_Value* lhs;
  struct ovm_services_bytecode_SpecificationIR_Value* rhs;
  struct java_lang_String* operator;
};
struct ovm_services_bytecode_SpecificationIR_Value {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_ValueSource* source;
  jint id;
};
struct ovm_services_bytecode_SpecificationIR_IntValue {
  struct ovm_services_bytecode_SpecificationIR_Value _parent_;
  cint type;
};
struct java_lang_IllegalStateException {
  struct java_lang_RuntimeException _parent_;
};
struct ovm_core_repository_RepositoryAttribute_LineNumberTable {
  struct java_lang_Object _parent_;
};
struct ovm_util_OVMException_IllegalAccess {
  struct ovm_util_OVMException _parent_;
};
struct ovm_util_OVMException_IO {
  struct ovm_util_OVMException _parent_;
};
struct ovm_util_OVMException_FileNotFound {
  struct ovm_util_OVMException_IO _parent_;
};
struct s3_core_domain_Subtyping_BucketDescriptor {
  struct java_lang_Object _parent_;
  struct ovm_util_HashSet* marked;
  struct ovm_util_LinkedList* members;
};
struct arr_ovm_services_bytecode_editor_Marker {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_editor_Marker* values[0];
};
struct ovm_core_repository_RepositoryAttribute {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryVisitor {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_editor_Marker {
  struct java_lang_Object _parent_;
};
struct ovm_util_OVMRuntimeException_IllegalAccess {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct arr_ovm_core_repository_RepositoryAttribute {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryAttribute* values[0];
};
struct java_io_ByteArrayOutputStream {
  struct java_io_OutputStream _parent_;
  struct arr_jbyte* buf;
  jint count;
};
struct ovm_services_bytecode_editor_Cursor {
  struct java_lang_Object _parent_;
};
struct ovm_util_IdentityHashMap_HashMapIterator {
  struct java_lang_Object _parent_;
  jint myType;
  jint knownMods;
  jint position;
  jint bucketIndex;
  struct ovm_util_Bucket_Node* currentNode;
  struct java_lang_Object* currentKey;
  struct ovm_util_IdentityHashMap* this$0;
};
struct s3_services_bytecode_editor_S3Cursor {
  struct ovm_core_OVMBase _parent_;
  struct s3_services_bytecode_editor_S3CodeFragmentEditor* editor_;
  jint pc_;
  jint offset_;
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder* instructionBuilder_;
  struct ovm_services_bytecode_editor_Marker* markerZero_;
  struct ovm_core_repository_RepositoryConstantPool_Builder* cPoolBuilder_;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor_DoubleRemoveError {
  struct java_lang_Error _parent_;
};
struct ovm_util_BasicMapEntry {
  struct java_lang_Object _parent_;
  struct java_lang_Object* key;
  struct java_lang_Object* value;
};
struct ovm_util_Bucket_Node {
  struct ovm_util_BasicMapEntry _parent_;
  struct ovm_util_Bucket_Node* next;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor_IllegalProgramPointError {
  struct java_lang_Error _parent_;
};
struct ovm_util_Bucket {
  struct java_lang_Object _parent_;
  struct ovm_util_Bucket_Node* first;
};
struct ovm_util_IdentityHashMap_IdentityBucket {
  struct ovm_util_Bucket _parent_;
};
struct s3_core_domain_HTSelector_Field2int {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_ovm_core_Selector_Field* keys_;
  struct arr_jint* values_;
  struct arr_s3_core_domain_HTSelector_Field2int_Binding* collisions_;
  struct s3_core_domain_HTSelector_Field2int* complementaryView_;
};
struct s3_core_domain_HTSelector_Field2int_2 {
  struct s3_core_domain_HTSelector_Field2int _parent_;
};
struct arr_ovm_util_Bucket {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_Bucket* values[0];
};
struct ovm_services_bytecode_InstructionVisitor {
  struct ovm_util_Runabout _parent_;
  struct ovm_core_Selector_Method* sel_;
  struct ovm_util_ByteBuffer* code_;
  struct ovm_core_repository_RepositoryConstantPool* cp_;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor_MaxHeightInferenceVisitor {
  struct ovm_services_bytecode_InstructionVisitor _parent_;
  jint maxLocals_;
  struct arr_jint* maxStacks_;
  zint loop;
  jint pc;
};
struct ovm_util_IdentityHashMap_1 {
  struct java_lang_Object _parent_;
};
struct ovm_util_AbstractCollection {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_util_IdentityHashMap_HashMapCollection {
  struct ovm_util_AbstractCollection _parent_;
  struct ovm_util_IdentityHashMap* this$0;
};
struct ovm_services_bytecode_writer_Dumper {
  struct java_lang_Object _parent_;
};
struct ovm_util_IdentityHashMap_HashMapEntry {
  struct ovm_util_Bucket_Node _parent_;
};
struct s3_services_bytecode_editor_HTint2S3Cursor {
  struct java_lang_Object _parent_;
  jint mask;
  struct arr_s3_services_bytecode_editor_HTint2S3Cursor_Binding* buckets;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor_S3ExceptionHandlerList {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_editor_Marker* startPC_;
  struct ovm_services_bytecode_editor_Marker* endPC_;
  struct ovm_services_bytecode_editor_Marker* targetPC_;
  struct ovm_core_TypeName_Scalar* exceptionType_;
  struct s3_services_bytecode_editor_S3CodeFragmentEditor_S3ExceptionHandlerList* next_;
  struct s3_services_bytecode_editor_S3CodeFragmentEditor_S3ExceptionHandlerList* prev_;
  zint deleted_;
};
struct ovm_util_AbstractSet {
  struct ovm_util_AbstractCollection _parent_;
};
struct ovm_util_IdentityHashMap_HashMapSet {
  struct ovm_util_AbstractSet _parent_;
  jint setType;
  struct ovm_util_IdentityHashMap* this$0;
};
struct arr_ovm_core_repository_RepositoryExceptionHandler {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryExceptionHandler* values[0];
};
struct ovm_core_repository_RepositoryCodeFragmentKind {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_core_repository_RepositoryCodeFragmentKind_ByteCodeFragmentKind {
  struct ovm_core_repository_RepositoryCodeFragmentKind _parent_;
};
struct s3_core_domain_S3Type {
  struct s3_core_S3Base _parent_;
  struct ovm_core_domain_Type_Context* context;
  struct arr_ovm_core_domain_Type_Interface* allIfcs_;
};
struct s3_core_domain_S3Type_Reference {
  struct s3_core_domain_S3Type _parent_;
  struct arr_s3_core_domain_S3Method* methods;
};
struct arr_s3_core_domain_HTSelector_Field2int_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_HTSelector_Field2int_Binding* values[0];
};
struct ovm_core_repository_RepositoryByteCodeFragment_Builder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_writer_Dumper_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryExceptionHandler {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_HTSelector_Field2int_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_domain_HTSelector_Field2int_Binding* link;
  struct ovm_core_Selector_Field* key;
  jint value;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor_PredictOffsets {
  struct ovm_services_bytecode_InstructionVisitor _parent_;
  jint pc_;
  struct s3_services_bytecode_editor_S3CodeFragmentEditor* this$0;
};
struct s3_core_domain_HTSelector_Field2int_ReadOnly {
  struct s3_core_domain_HTSelector_Field2int _parent_;
};
struct ovm_core_repository_RepositoryDescriptor_Method {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor_GenerateCode {
  struct ovm_services_bytecode_InstructionVisitor _parent_;
  jint pc_;
  struct s3_services_bytecode_editor_S3CodeFragmentEditor* this$0;
};
struct s3_core_domain_S3Type_Primitive {
  struct s3_core_domain_S3Type _parent_;
  struct ovm_core_TypeName_Primitive* name;
};
struct s3_core_domain_S3Type_WidePrimitive {
  struct s3_core_domain_S3Type_Primitive _parent_;
};
struct ovm_util_AbstractMap_1 {
  struct ovm_util_AbstractSet _parent_;
  struct ovm_util_AbstractMap* this$0;
};
struct ovm_util_AbstractMap_3 {
  struct ovm_util_AbstractCollection _parent_;
  struct ovm_util_AbstractMap* this$0;
};
struct s3_core_domain_S3SystemTypeContext {
  struct s3_core_S3Base _parent_;
  struct ovm_core_repository_RepositoryBundle* bundle;
  struct ovm_core_domain_Domain* domain;
  struct ovm_services_bytecode_reader_Installer* installer;
};
struct s3_core_domain_S3Type_Scalar {
  struct s3_core_domain_S3Type_Reference _parent_;
  struct ovm_core_repository_RepositoryClass* repoClass;
  struct arr_ovm_core_domain_Type_Interface* interfaces;
};
struct s3_core_domain_S3Type_ClassBase {
  struct s3_core_domain_S3Type_Scalar _parent_;
  struct s3_core_domain_HTSelector_Field2Field* fields;
};
struct s3_core_domain_S3Type_Class {
  struct s3_core_domain_S3Type_ClassBase _parent_;
  struct ovm_core_domain_Type_Class* super_;
  struct ovm_core_domain_Type_Class* sharedStateType;
};
struct s3_core_domain_S3Type_Interface {
  struct s3_core_domain_S3Type_Scalar _parent_;
  struct s3_core_domain_S3Type_SharedStateClass* sharedStateType;
};
struct ovm_services_bytecode_JVMConstants {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_S3Type_Array {
  struct s3_core_domain_S3Type_Reference _parent_;
  struct s3_core_domain_HTSelector_Method2Method* methods_;
  struct ovm_core_domain_Type* innermostElemType;
  jint dimension_;
  struct ovm_core_domain_Type* componentType_;
};
struct ovm_core_domain_Member {
  struct java_lang_Object _parent_;
};
struct arr_s3_core_domain_S3Type_Interface {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_S3Type_Interface* values[0];
};
struct ovm_services_bytecode_editor_CodeFragmentEditor_ExceptionHandlerList {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryUtils {
  struct ovm_core_OVMBase _parent_;
};
struct s3_core_domain_Subtyping {
  struct s3_core_S3Base _parent_;
  struct ovm_util_ArrayList* bucketDescriptors;
  struct ovm_util_HTObject2int* bucketAssignment;
  struct ovm_util_HashSet* seen;
  zint isRecursive;
};
struct ovm_core_repository_RepositoryException {
  struct ovm_util_OVMException _parent_;
};
struct ovm_core_repository_RepositoryBundle_Exception {
  struct ovm_core_repository_RepositoryException _parent_;
  struct ovm_core_repository_RepositoryBundle* bundle;
};
struct ovm_core_repository_RepositoryBundle_SealedBundleException {
  struct ovm_core_repository_RepositoryBundle_Exception _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder {
  struct java_lang_Object _parent_;
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder* next_;
};
struct s3_services_bytecode_editor_S3Cursor_1 {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  cint val$span;
  bint val$count;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_FieldBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct ovm_core_Selector_Field* selector_;
  bint opcode_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct ovm_services_bytecode_verifier_Verifier {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_verifier_Frame {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_QuickFieldBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  cint off_;
  bint opcode_;
};
struct s3_services_bytecode_editor_S3Cursor_InvokeBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct ovm_core_Selector_Method* selector_;
  bint opcode_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_2 {
  struct s3_services_bytecode_editor_S3Cursor_InvokeBuilder _parent_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_InvokeSystemBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  bint opcode_;
  bint methodTag_;
};
struct s3_services_bytecode_editor_S3Cursor_S3Marker {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct s3_services_bytecode_editor_S3Cursor* cursor_;
  jint predicted_;
};
struct s3_services_bytecode_editor_S3Cursor_IIncBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  sint value_;
  cint index_;
};
struct s3_services_bytecode_editor_S3Cursor_SimpleOpBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  jint opcode_;
};
struct s3_services_bytecode_editor_S3Cursor_ByteIndexedBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  bint opcode_;
  bint value_;
};
struct ovm_core_repository_ClassFormatException {
  struct ovm_core_repository_RepositoryException _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_Wide4Builder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  bint opcode_;
  cint value_;
};
struct s3_services_bytecode_editor_S3Cursor_OpClassRefBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct ovm_core_TypeName_Compound* class_;
  bint opcode_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct ovm_services_bytecode_verifier_Verifier_Factory {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_GotoBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct ovm_services_bytecode_editor_Marker* marker_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct ovm_core_execution_Native_Utils {
  struct ovm_core_OVMBase _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_IfBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct ovm_services_bytecode_editor_Marker* marker_;
  jint opcode_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_BIPUSHBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  bint value_;
};
struct s3_services_bytecode_editor_S3Cursor_SIPUSHBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  sint value_;
};
struct s3_services_bytecode_editor_S3Cursor_SequenceBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct arr_jbyte* sequence_;
};
struct s3_services_bytecode_editor_S3Cursor_INVOKEVIRTUAL_QUICKBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  cint index;
  bint argCount;
  bint wideArgCount;
};
struct ovm_core_execution_Native_Ptr {
  struct java_lang_Object _parent_;
  zint set;
  jint value;
};
struct ovm_core_execution_Native_FilePtr {
  struct ovm_core_execution_Native_Ptr _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_INVOKEINTERFACE_QUICKBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  cint index;
  bint argCount;
  bint wideArgCount;
};
struct ovm_core_execution_Native_IntPtr {
  struct ovm_core_execution_Native_Ptr _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_INVOKENONVIRTUAL_QUICKBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  cint linkSetIndex;
  cint methodIndex;
  bint argCount;
  bint wideArgCount;
  zint is2;
  zint isSuper;
};
struct ovm_services_bytecode_verifier_Frame_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_util_AbstractList {
  struct ovm_util_AbstractCollection _parent_;
  jint modCount;
};
struct ovm_util_AbstractSequentialList {
  struct ovm_util_AbstractList _parent_;
};
struct ovm_util_LinkedList {
  struct ovm_util_AbstractSequentialList _parent_;
  struct ovm_util_LinkedList_Entry* first;
  struct ovm_util_LinkedList_Entry* last;
  jint size;
};
struct ovm_util_ArrayList {
  struct ovm_util_AbstractList _parent_;
  jint size;
  struct arr_java_lang_Object* data;
};
struct ovm_util_HTObject2int {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_java_lang_Object* keys_;
  struct arr_jint* values_;
  struct arr_ovm_util_HTObject2int_Binding* collisions_;
  struct ovm_util_HTObject2int* complementaryView_;
};
struct arr_s3_services_bytecode_editor_HTint2S3Cursor_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_services_bytecode_editor_HTint2S3Cursor_Binding* values[0];
};
struct ovm_core_execution_Activation_Pool {
  struct ovm_core_OVMBase _parent_;
  struct arr_ovm_core_execution_Activation* pool_;
  jint pos_;
};
struct ovm_core_execution_Activation_1 {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_editor_HTint2S3Cursor_Binding {
  struct java_lang_Object _parent_;
  struct s3_services_bytecode_editor_HTint2S3Cursor_Binding* link;
  jint key;
  struct s3_services_bytecode_editor_S3Cursor* value;
};
struct s3_util_PragmaInlineSubstituteBytecode_BCnothing {
  struct s3_util_PragmaInlineSubstituteBytecode _parent_;
};
struct ovm_core_services_memory_Allocator_BootRequired {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_LdcBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  jint index_;
};
struct s3_services_bytecode_editor_S3Cursor_LDC2_WBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  jint index_;
};
struct s3_services_bytecode_editor_S3Cursor_QuickCharBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  cint quick_;
  bint opcode_;
};
struct s3_services_bytecode_editor_S3Cursor_SwitchBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct ovm_services_bytecode_editor_Marker* defaultTarget_;
  struct arr_jint* indices_;
  struct arr_ovm_services_bytecode_editor_Marker* targets_;
};
struct s3_services_bytecode_editor_S3Cursor_3 {
  struct s3_services_bytecode_editor_S3Cursor_QuickCharBuilder _parent_;
  jint val$dim;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_4 {
  struct s3_services_bytecode_editor_S3Cursor_OpClassRefBuilder _parent_;
  jint val$dim;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_JsrBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  struct ovm_services_bytecode_editor_Marker* marker_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_RetBuilder {
  struct s3_services_bytecode_editor_S3Cursor_InstructionBuilder _parent_;
  cint index_;
};
struct ovm_util_Collections {
  struct ovm_core_OVMBase _parent_;
};
struct s3_services_bytecode_editor_S3Cursor_TABLESWITCHBuilder {
  struct s3_services_bytecode_editor_S3Cursor_SwitchBuilder _parent_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct s3_services_bytecode_editor_S3Cursor_LOOKUPSWITCHBuilder {
  struct s3_services_bytecode_editor_S3Cursor_SwitchBuilder _parent_;
  struct s3_services_bytecode_editor_S3Cursor* this$0;
};
struct ovm_core_repository_RepositoryUnboundSelector_Field {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_reader_BasicIOFileReader {
  struct s3_core_S3Base _parent_;
};
struct ovm_core_repository_RepositoryUnboundSelector {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_repository_RepositoryUnboundSelector_Field {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryUnboundSelector_Field* values[0];
};
struct ovm_core_repository_RepositoryDescriptor {
  struct java_lang_Object _parent_;
};
struct ovm_core_execution_Services {
  struct java_lang_Object _parent_;
};
struct ovm_util_Map_Entry {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_reader_S3Parser {
  struct s3_core_S3Base _parent_;
  struct ovm_util_ByteBuffer* stream_;
  jint streamLength_;
  struct java_lang_String* className_;
  struct ovm_core_repository_RepositoryConstantPool* cp_;
  struct ovm_core_repository_RepositoryConstantPool_Builder* cpBuilder_;
  struct arr_jint* poses;
  struct ovm_core_repository_RepositoryClass_Builder* classBuilder_;
  struct ovm_core_repository_RepositoryMember_Method_Builder* methodBuilder_;
  struct ovm_core_repository_RepositoryMember_Field_Builder* fieldBuilder_;
  struct ovm_core_repository_RepositoryByteCodeFragment_Builder* codeBuilder_;
  struct ovm_core_Mode_Factory* modeBuilder_;
  struct ovm_core_repository_Services* factory_;
  jint current_class_cp_ix;
};
struct ovm_util_Runabout_HTClass2Runabout_Code_Binding {
  struct java_lang_Object _parent_;
  struct ovm_util_Runabout_HTClass2Runabout_Code_Binding* link;
  struct java_lang_Class* key;
  struct ovm_util_Runabout_Code* value;
};
struct s3_services_bytecode_reader_S3Parser_Factory {
  struct java_lang_Object _parent_;
  struct s3_services_bytecode_reader_PackageEnvironment* pke_;
};
struct s3_services_bytecode_reader_S3FileReader_Factory {
  struct java_lang_Object _parent_;
  struct s3_services_bytecode_reader_PackageEnvironment* pke_;
};
struct s3_services_bytecode_reader_S3Installer_Factory {
  struct java_lang_Object _parent_;
  struct s3_services_bytecode_reader_PackageEnvironment* pke_;
};
struct arr_ovm_util_Runabout_HTClass2Runabout_Code_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_Runabout_HTClass2Runabout_Code_Binding* values[0];
};
struct ovm_core_execution_Context_NativeContextFactory {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collection {
  struct java_lang_Object _parent_;
};
struct ovm_util_HashSet {
  struct ovm_util_AbstractSet _parent_;
  struct ovm_util_HashMap* map;
};
struct ovm_util_Runabout_HTClass2Runabout_Code {
  struct java_lang_Object _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_java_lang_Class* keys_;
  struct arr_ovm_util_Runabout_Code* values_;
  struct arr_ovm_util_Runabout_HTClass2Runabout_Code_Binding* collisions_;
};
struct ovm_util_Runabout_4 {
  struct ovm_util_Runabout_HTClass2Runabout_Code _parent_;
};
struct ovm_util_NumberRanges_NumberRangeException {
  struct ovm_util_OVMError _parent_;
  jlong value;
  struct java_lang_String* typeName;
};
struct arr_ovm_core_repository_RepositoryMember_Method {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryMember_Method* values[0];
};
struct ovm_core_repository_TypeCodes {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_repository_RepositoryUnboundSelector_Method {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryUnboundSelector_Method* values[0];
};
struct arr_ovm_util_Runabout_HTClass2HTClass2Runabout_Code_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_Runabout_HTClass2HTClass2Runabout_Code_Binding* values[0];
};
struct ovm_util_Runabout_HTClass2HTClass2Runabout_Code {
  struct java_lang_Object _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_java_lang_Class* keys_;
  struct arr_ovm_util_Runabout_HTClass2Runabout_Code* values_;
  struct arr_ovm_util_Runabout_HTClass2HTClass2Runabout_Code_Binding* collisions_;
};
struct ovm_util_Runabout_3 {
  struct ovm_util_Runabout_HTClass2HTClass2Runabout_Code _parent_;
};
struct ovm_core_repository_RepositoryMember_Method {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryUnboundSelector_Method {
  struct java_lang_Object _parent_;
};
struct s3_core_services_memory_PackageEnvironment {
  struct java_lang_Object _parent_;
  struct ovm_core_services_memory_Allocator_Factory* aF_;
};
struct ovm_util_Runabout_HTClass2HTClass2Runabout_Code_Binding {
  struct java_lang_Object _parent_;
  struct ovm_util_Runabout_HTClass2HTClass2Runabout_Code_Binding* link;
  struct java_lang_Class* key;
  struct ovm_util_Runabout_HTClass2Runabout_Code* value;
};
struct ovm_core_repository_RepositoryDescriptor_Field {
  struct java_lang_Object _parent_;
};
struct ovm_util_HashMap {
  struct ovm_util_AbstractMap _parent_;
  jint capacity;
  jint size;
  jfloat loadFactor;
  jint threshold;
  struct arr_ovm_util_Bucket* buckets;
  jint modCount;
};
struct arr_ovm_core_repository_RepositoryDescriptor_Field {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryDescriptor_Field* values[0];
};
struct ovm_services_realtime_RealtimePriorityDispatcher {
  struct java_lang_Object _parent_;
};
struct ovm_util_Runabout_Code {
  struct java_lang_Object _parent_;
};
struct ovm_util_Runabout_IntCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct ovm_util_Runabout_FloatCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct ovm_util_Runabout_DoubleCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct ovm_util_Runabout_RunaboutException {
  struct java_lang_RuntimeException _parent_;
};
struct ovm_util_Runabout_NoCode {
  struct ovm_util_Runabout_Code _parent_;
};
struct ovm_util_Runabout_Cache {
  struct java_lang_Object _parent_;
  struct java_lang_ClassLoader* loader_;
  struct arr_jbyte* lastName_;
  struct ovm_util_Runabout_HTClass2HTClass2Runabout_Code* cache_;
  struct arr_jbyte* code;
};
struct arr_ovm_util_Runabout_HTClass2Runabout_Code {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_Runabout_HTClass2Runabout_Code* values[0];
};
struct ovm_services_realtime_RealtimeOVMThread {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryMember {
  struct java_lang_Object _parent_;
};
struct ovm_util_Runabout_BooleanCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct ovm_util_Runabout_ShortCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct ovm_util_Runabout_LongCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct ovm_util_Runabout_ByteCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct arr_ovm_util_Runabout_Code {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_Runabout_Code* values[0];
};
struct ovm_util_Runabout_CharCode {
  struct ovm_util_Runabout_Code _parent_;
  struct ovm_util_Runabout* this$0;
};
struct ovm_core_repository_RepositoryClass {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryConstantPool {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_repository_RepositoryClass {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryClass* values[0];
};
struct ovm_core_repository_RepositoryMember_Field {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryByteCodeFragment {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_repository_RepositoryCodeFragment {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryCodeFragment* values[0];
};
struct arr_ovm_core_execution_Activation {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_execution_Activation* values[0];
};
struct arr_ovm_core_repository_RepositoryMember_Field {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryMember_Field* values[0];
};
struct ovm_core_Mode_Factory {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_repository_RepositoryByteCodeFragment {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryByteCodeFragment* values[0];
};
struct ovm_core_repository_RepositoryCodeFragment {
  struct java_lang_Object _parent_;
};
struct ovm_core_execution_Activation {
  struct ovm_core_OVMBase _parent_;
  jint frameHandle_;
  jint nativeContextHandle_;
  struct ovm_core_execution_Activation_Pool* myPool_;
};
struct ovm_core_stitcher_Initializable {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_Allocator {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_VM_Address {
  struct java_lang_Object _parent_;
  jint value;
  struct java_lang_Object* source;
  struct ovm_core_domain_Blueprint* bp;
  zint interesting_;
  struct ovm_core_services_memory_VM_Address_Deferral* deferral_;
};
struct arr_ovm_core_stitcher_Initializable {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_stitcher_Initializable* values[0];
};
struct ovm_core_repository_RepositoryNativeCodeFragment {
  struct java_lang_Object _parent_;
};
struct s3_core_execution_PackageEnvironment_1 {
  struct java_lang_Object _parent_;
  struct s3_core_execution_PackageEnvironment* this$0;
};
struct ovm_services_ServiceInstance {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_editor_CodeFragmentEditor {
  struct java_lang_Object _parent_;
};
struct ovm_util_logging_Handler {
  struct ovm_core_OVMBase _parent_;
  struct ovm_util_logging_Level* level;
};
struct ovm_util_logging_ConsoleHandler {
  struct ovm_util_logging_Handler _parent_;
};
struct ovm_util_logging_Level {
  struct ovm_core_OVMBase _parent_;
  struct java_lang_String* name;
  jint value;
};
struct arr_ovm_util_logging_Handler {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_logging_Handler* values[0];
};
struct ovm_core_repository_Repository {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_editor_ClassCleaner {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_Services* pke_;
};
struct ovm_services_bytecode_editor_CodeFragmentEditor_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_VM_Word {
  struct java_lang_Object _parent_;
  jint value;
};
struct ovm_services_bytecode_reader_CodeSource {
  struct ovm_core_OVMBase _parent_;
};
struct s3_services_bytecode_reader_S3CodeSource {
  struct ovm_services_bytecode_reader_CodeSource _parent_;
};
struct s3_services_bytecode_reader_S3CodeSource_StandaloneDir {
  struct s3_services_bytecode_reader_S3CodeSource _parent_;
  struct java_lang_String* dir;
};
struct s3_util_PragmaSquawk {
  struct s3_util_PragmaException _parent_;
};
struct ovm_core_execution_Context_Factory {
  struct java_lang_Object _parent_;
};
struct s3_util_PragmaHierarchy {
  struct s3_util_PragmaException _parent_;
};
struct java_io_Serializable {
  struct java_lang_Object _parent_;
};
struct s3_util_PragmaHierarchy_Foo {
  struct s3_util_PragmaHierarchy _parent_;
};
struct s3_util_PragmaHierarchy_Bar {
  struct s3_util_PragmaHierarchy _parent_;
};
struct s3_services_bytecode_reader_S3FileReader {
  struct s3_core_S3Base _parent_;
  struct arr_ovm_services_bytecode_reader_CodeSource* paths_;
};
struct s3_services_bytecode_reader_S3Installer {
  struct s3_core_S3Base _parent_;
  struct ovm_services_bytecode_reader_FileReader* cfr_;
  struct ovm_services_bytecode_reader_Parser* cfp_;
  struct ovm_core_repository_RepositoryBundle* bundle_;
};
struct ovm_util_Iterator {
  struct java_lang_Object _parent_;
};
struct ovm_util_ArrayList_ArrayListIterator {
  struct ovm_core_OVMBase _parent_;
  jint index;
  struct ovm_util_ArrayList* this$0;
};
struct s3_core_domain_S3Domain {
  struct s3_core_S3Base _parent_;
  struct ovm_util_Map* types;
  struct ovm_util_Map* blueprints;
  struct ovm_core_domain_Type_Context* context_;
  struct s3_core_domain_Subtyping* subtyping;
  struct ovm_core_domain_Type_Class* ROOT_TYPE;
  struct s3_core_domain_S3Blueprint_Scalar* ROOT_BLUEPRINT;
  struct arr_ovm_core_domain_Type_Interface* arrayInterfaces;
  struct ovm_core_domain_Type_Primitive* BYTE;
  struct ovm_core_domain_Type_Primitive* CHAR;
  struct ovm_core_domain_Type_Primitive* DOUBLE;
  struct ovm_core_domain_Type_Primitive* FLOAT;
  struct ovm_core_domain_Type_Primitive* INT;
  struct ovm_core_domain_Type_Primitive* LONG;
  struct ovm_core_domain_Type_Primitive* SHORT;
  struct ovm_core_domain_Type_Primitive* VOID;
  struct ovm_core_domain_Type_Primitive* BOOLEAN;
};
struct s3_core_domain_ExecutiveDomain {
  struct s3_core_domain_S3Domain _parent_;
  struct s3_core_domain_MemberResolver* resolver;
  struct s3_core_domain_DispatchBuilder* dispatchBuilder;
};
struct arr_s3_core_repository_S3TypeName_Compound {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3TypeName_Compound* values[0];
};
struct arr_s3_core_repository_S3Bundle {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Bundle* values[0];
};
struct arr_s3_core_repository_S3TypeName_Scalar {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3TypeName_Scalar* values[0];
};
struct arr_s3_core_repository_S3TypeName {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3TypeName* values[0];
};
struct s3_core_repository_S3TypeName {
  struct s3_core_S3Base _parent_;
};
struct s3_core_repository_S3TypeName_Primitive {
  struct s3_core_repository_S3TypeName _parent_;
  jint name_;
  cint tag_;
  struct java_lang_String* tagAsString_;
};
struct s3_core_repository_S3TypeName_Compound {
  struct s3_core_repository_S3TypeName _parent_;
};
struct s3_core_repository_S3Descriptor {
  struct s3_core_S3Base _parent_;
  struct s3_core_repository_S3TypeName* return_or_field_type_;
};
struct s3_core_repository_S3Descriptor_Field {
  struct s3_core_repository_S3Descriptor _parent_;
};
struct s3_util_PragmaInlineSubstituteBytecode_Rewriter {
  struct java_lang_Object _parent_;
  struct ovm_core_domain_Blueprint* siteBP;
  struct ovm_services_bytecode_MethodInformation* siteMI;
  jint sitePC;
  struct ovm_services_bytecode_Instruction_Invocation* siteInst;
  struct ovm_core_domain_Blueprint* targetBP;
  struct ovm_core_Selector_Method* targetSel;
  struct ovm_services_bytecode_editor_CodeFragmentEditor* cfe;
  struct ovm_services_bytecode_editor_Cursor* cursor;
  struct s3_core_domain_MemberResolver* resolver;
  struct ovm_util_ByteBuffer* code;
};
struct ovm_core_services_memory_VM_Word_BfRewriter {
  struct s3_util_PragmaInlineSubstituteBytecode_Rewriter _parent_;
  jint widthCheck;
  zint set;
  zint shifted;
  struct ovm_core_repository_RepositoryClass* realTargetRC;
  jint width;
  jint shift;
  jint mask;
};
struct s3_core_repository_S3Bundle {
  struct s3_core_S3Base _parent_;
  struct s3_core_repository_HTS3Class* classes;
  zint isSealed;
  struct arr_ovm_core_repository_RepositoryBundle* parents;
};
struct s3_core_repository_S3TypeName_Scalar {
  struct s3_core_repository_S3TypeName_Compound _parent_;
  jint pack_;
  jint name_;
};
struct ovm_util_HTObject2int_ReadOnly {
  struct ovm_util_HTObject2int _parent_;
};
struct ovm_util_HTObject2int_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct ovm_util_HTObject2int_Binding* currentBinding_;
  zint reachEnd;
  struct ovm_util_HTObject2int* this$0;
};
struct s3_core_repository_RepositoryImpl {
  struct s3_core_S3Base _parent_;
  struct s3_core_repository_S3TypeName_Primitive* FLOAT;
  struct s3_core_repository_S3TypeName_Primitive* BOOLEAN;
  struct s3_core_repository_S3TypeName_Primitive* CHAR;
  struct s3_core_repository_S3TypeName_Primitive* SHORT;
  struct s3_core_repository_S3TypeName_Primitive* BYTE;
  struct s3_core_repository_S3TypeName_Primitive* INT;
  struct s3_core_repository_S3TypeName_WidePrimitive* LONG;
  struct s3_core_repository_S3TypeName_WidePrimitive* DOUBLE;
  struct s3_core_repository_S3TypeName_Primitive* VOID;
  struct s3_core_repository_S3TypeName_Scalar* ROOT;
  struct s3_core_repository_S3TypeName_Scalar* STRING;
  jint FLOAT_NAME_INDEX;
  jint BOOLEAN_NAME_INDEX;
  jint CHAR_NAME_INDEX;
  jint SHORT_NAME_INDEX;
  jint BYTE_NAME_INDEX;
  jint INT_NAME_INDEX;
  jint LONG_NAME_INDEX;
  jint DOUBLE_NAME_INDEX;
  jint VOID_NAME_INDEX;
  struct s3_core_repository_HTS3TypeName* scalarTypeNames_;
  struct s3_core_repository_HTS3TypeName* arrayTypeNames_;
  struct s3_core_repository_S3UTF8Store* utf8s_;
  struct s3_core_repository_HTS3Descriptor* descriptors_;
  struct s3_core_repository_HTS3UnboundSelector* selectors_;
  struct s3_core_repository_HTS3Selector* boundSelectors_;
  struct ovm_util_HTString* strings_;
};
struct ovm_util_HTObject2int_1 {
  struct ovm_util_HTObject2int _parent_;
};
struct s3_core_repository_S3RepositoryIfc {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Descriptor_Method {
  struct s3_core_repository_S3Descriptor _parent_;
  jint precomputedHashCode_;
  struct s3_core_repository_S3TypeName* argument0;
  struct s3_core_repository_S3TypeName* argument1;
  struct s3_core_repository_S3TypeName* argument2;
};
struct s3_core_repository_S3UnboundSelector {
  struct s3_core_S3Base _parent_;
  jint name_;
};
struct s3_core_repository_S3UnboundSelector_Field {
  struct s3_core_repository_S3UnboundSelector _parent_;
  struct ovm_core_repository_RepositoryDescriptor_Field* descriptor_;
};
struct ovm_util_HTObject2int_Binding {
  struct ovm_core_OVMBase _parent_;
  struct ovm_util_HTObject2int_Binding* link;
  struct java_lang_Object* key;
  jint value;
};
struct s3_core_repository_S3UnboundSelector_Method {
  struct s3_core_repository_S3UnboundSelector _parent_;
  struct ovm_core_repository_RepositoryDescriptor_Method* descriptor_;
};
struct ovm_util_logging_Logger {
  struct ovm_core_OVMBase _parent_;
  struct java_lang_String* name;
  struct arr_ovm_util_logging_Handler* handlers;
  struct ovm_util_logging_Level* level;
};
struct ovm_util_StringConversion {
  struct java_lang_Object _parent_;
};
struct arr_ovm_util_HTObject2int_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_HTObject2int_Binding* values[0];
};
struct s3_core_repository_S3TypeName_Array {
  struct s3_core_repository_S3TypeName_Compound _parent_;
  struct s3_core_repository_S3TypeName* element_;
  bint depth_;
};
struct ovm_core_services_memory_VM_Word_BC {
  struct s3_util_PragmaInlineSubstituteBytecode _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfusi {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfssi {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_util_SubList_1 {
  struct java_lang_Object _parent_;
  struct ovm_util_ListIterator* i;
  jint position;
  jint val$index;
  struct ovm_util_SubList* this$0;
};
struct ovm_core_services_memory_VM_Word_BCbfusw {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_util_ConcurrentModificationException {
  struct java_lang_RuntimeException _parent_;
};
struct ovm_core_services_memory_VM_Word_Bitfield {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfusl {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_util_NoSuchElementException {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfssl {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_util_Collections_8 {
  struct java_lang_Object _parent_;
  zint hasNext;
  struct ovm_util_Collections_7* this$0;
};
struct ovm_core_services_memory_VM_Word_FantasticBitfieldException {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct ovm_core_services_memory_VM_Word_BitfieldCastException {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct s3_core_repository_S3Selector {
  struct s3_core_S3Base _parent_;
  struct s3_core_repository_S3TypeName_Compound* definingClassName_;
  struct s3_core_repository_S3UnboundSelector* selector_;
};
struct s3_core_repository_S3Selector_Method {
  struct s3_core_repository_S3Selector _parent_;
};
struct ovm_core_domain_Domain_Factory {
  struct java_lang_Object _parent_;
};
struct arr_s3_core_repository_S3Selector {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Selector* values[0];
};
struct s3_core_repository_S3UTF8Store {
  struct s3_core_S3Base _parent_;
  struct arr_jbyte* utf8s_;
  struct s3_core_repository_S3UTF8Store_HTUtf8* utf8Index_;
  jint cursor_;
  struct java_lang_Object* utf8Lock_;
};
struct s3_core_repository_S3Selector_Field {
  struct s3_core_repository_S3Selector _parent_;
};
struct ovm_core_domain_ObjectHeader {
  struct ovm_core_OVMBase _parent_;
  jint blueprint_offset_;
  jint monitor_offset_;
};
struct ovm_core_services_memory_VM_Word_BCbfsgi {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfugw {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfsgw {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfssw {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct s3_core_domain_PackageEnvironment {
  struct java_lang_Object _parent_;
  struct ovm_core_domain_Domain_Factory* dF;
  struct ovm_core_services_memory_Services* memoryServices_;
  struct ovm_core_domain_ObjectHeader* objHead_;
};
struct ovm_core_services_memory_VM_Word_BCbfugl {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct arr_s3_core_repository_S3UnboundSelector {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3UnboundSelector* values[0];
};
struct ovm_core_services_memory_VM_Word_BCbfsgl {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCbfugi {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_util_Collections_UnmodifiableIterator {
  struct java_lang_Object _parent_;
  struct ovm_util_Iterator* i;
};
struct ovm_util_Collections_12 {
  struct ovm_util_Collections_UnmodifiableIterator _parent_;
  struct ovm_util_Collections_11* this$1;
};
struct ovm_util_Collections_13 {
  struct java_lang_Object _parent_;
  struct ovm_util_Map_Entry* val$e;
  struct ovm_util_Collections_12* this$2;
};
struct ovm_util_Collections_SynchronizedIterator {
  struct java_lang_Object _parent_;
  struct java_lang_Object* sync;
  struct ovm_util_Iterator* i;
};
struct ovm_util_Collections_15 {
  struct ovm_util_Collections_SynchronizedIterator _parent_;
  struct ovm_util_Collections_14* this$1;
};
struct ovm_util_Collections_16 {
  struct java_lang_Object _parent_;
  struct ovm_util_Map_Entry* val$e;
  struct ovm_util_Collections_15* this$2;
};
struct ovm_services_bytecode_reader_Services {
  struct java_lang_Object _parent_;
};
struct ovm_util_AbstractMap_4 {
  struct java_lang_Object _parent_;
  struct ovm_util_Iterator* map_iterator;
  struct ovm_util_AbstractMap_3* this$1;
};
struct ovm_util_AbstractMap_2 {
  struct java_lang_Object _parent_;
  struct ovm_util_Iterator* map_iterator;
  struct ovm_util_AbstractMap_1* this$1;
};
struct ovm_util_Ordered {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_HTS3TypeName {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint numElems_;
  jint maskCollisions_;
  struct arr_s3_core_repository_S3TypeName* keys_;
  struct arr_s3_core_repository_HTS3TypeName_Binding* collisions_;
  struct s3_core_repository_HTS3TypeName* complementaryView_;
};
struct s3_core_repository_HTS3Descriptor {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint numElems_;
  jint maskCollisions_;
  struct arr_s3_core_repository_S3Descriptor* keys_;
  struct arr_s3_core_repository_HTS3Descriptor_Binding* collisions_;
  struct s3_core_repository_HTS3Descriptor* complementaryView_;
};
struct s3_core_repository_S3TypeName_WidePrimitive {
  struct s3_core_repository_S3TypeName_Primitive _parent_;
};
struct s3_core_repository_S3Descriptor_SmallMethod {
  struct s3_core_repository_S3Descriptor_Method _parent_;
};
struct s3_core_repository_S3Descriptor_BigMethod {
  struct s3_core_repository_S3Descriptor_Method _parent_;
  struct arr_s3_core_repository_S3TypeName* arguments_;
};
struct s3_core_repository_HTS3UnboundSelector {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint numElems_;
  jint maskCollisions_;
  struct arr_s3_core_repository_S3UnboundSelector* keys_;
  struct arr_s3_core_repository_HTS3UnboundSelector_Binding* collisions_;
  struct s3_core_repository_HTS3UnboundSelector* complementaryView_;
};
struct s3_core_repository_HTS3Selector {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint numElems_;
  jint maskCollisions_;
  struct arr_s3_core_repository_S3Selector* keys_;
  struct arr_s3_core_repository_HTS3Selector_Binding* collisions_;
  struct s3_core_repository_HTS3Selector* complementaryView_;
};
struct s3_core_repository_S3NativeCodeFragment_Linker {
  struct java_lang_Object _parent_;
};
struct s3_util_queues_SingleLinkQueue {
  struct java_lang_Object _parent_;
  struct s3_util_queues_SingleLinkElement* head;
  struct s3_util_queues_SingleLinkElement* tail;
};
struct s3_util_queues_SingleLinkPriorityQueue {
  struct s3_util_queues_SingleLinkQueue _parent_;
  struct java_util_Comparator* comp;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor {
  struct s3_core_S3Base _parent_;
  struct ovm_core_repository_RepositoryByteCodeFragment* original_;
  struct ovm_core_repository_RepositoryByteCodeFragment_Builder* rbcfb_;
  struct s3_services_bytecode_editor_HTint2S3Cursor* preCursors_;
  struct s3_services_bytecode_editor_HTint2S3Cursor* postCursors_;
  struct arr_jboolean* removed_;
  struct ovm_core_repository_RepositoryConstantPool_Builder* cPoolBuilder_;
  struct arr_jint* predictedOffsets_;
  struct ovm_services_bytecode_InstructionSet* is;
  struct s3_services_bytecode_editor_S3CodeFragmentEditor_S3ExceptionHandlerList* ehl_;
  zint changed;
};
struct test_userlevel_TestBase {
  struct java_lang_Object _parent_;
  struct java_lang_String* description_;
  struct java_lang_String* module_;
};
struct test_userlevel_TestSuite {
  struct test_userlevel_TestBase _parent_;
  struct test_userlevel_TestBase* tfa;
  struct test_userlevel_TestBase* tsf;
};
struct test_runtime_TestBase {
  struct java_lang_Object _parent_;
  struct java_lang_String* description_;
  struct java_lang_String* module_;
};
struct test_runtime_TestIO {
  struct test_runtime_TestBase _parent_;
};
struct s3_core_domain_HTSelector_Method2int {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_ovm_core_Selector_Method* keys_;
  struct arr_jint* values_;
  struct arr_s3_core_domain_HTSelector_Method2int_Binding* collisions_;
  struct s3_core_domain_HTSelector_Method2int* complementaryView_;
};
struct s3_core_domain_HTSelector_Method2int_2 {
  struct s3_core_domain_HTSelector_Method2int _parent_;
};
struct test_runtime_TestThreadsBase {
  struct test_runtime_TestBase _parent_;
  struct ovm_services_threads_UserLevelThreadManager* threadMan;
  struct ovm_core_services_threads_OVMDispatcher* dispatcher;
  struct ovm_services_monitors_MonitorMapper* mapper;
};
struct test_runtime_TestSynchronization {
  struct test_runtime_TestThreadsBase _parent_;
  struct ovm_core_services_threads_OVMThread* current;
};
struct test_runtime_TestSyncBase {
  struct test_runtime_TestThreadsBase _parent_;
};
struct test_runtime_TestThreadScheduling {
  struct test_runtime_TestSyncBase _parent_;
  struct ovm_services_threads_PriorityOVMDispatcher* dispatcher;
  struct test_runtime_TestThreadScheduling_LogEntry* logger;
};
struct test_runtime_TestString {
  struct test_runtime_TestBase _parent_;
};
struct ovm_core_repository_RepositoryUtils_Cache {
  struct java_lang_Object _parent_;
  struct java_lang_String* definingClass;
  struct java_lang_String* selector;
};
struct test_runtime_TestStringBuffer {
  struct test_runtime_TestBase _parent_;
};
struct s3_core_domain_HTSelector_Method2int_ReadOnly {
  struct s3_core_domain_HTSelector_Method2int _parent_;
};
struct test_runtime_TestCreateOVMThread {
  struct test_runtime_TestBase _parent_;
};
struct test_runtime_TestClone {
  struct test_runtime_TestBase _parent_;
};
struct s3_core_domain_HTSelector_Method2int_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_domain_HTSelector_Method2int_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_domain_HTSelector_Method2int* this$0;
};
struct test_runtime_TestConversionToString {
  struct test_runtime_TestBase _parent_;
};
struct test_runtime_TestParsing {
  struct test_runtime_TestBase _parent_;
};
struct java_lang_IllegalArgumentException {
  struct java_lang_RuntimeException _parent_;
};
struct test_runtime_TestRealtimeThreadScheduling {
  struct test_runtime_TestSyncBase _parent_;
  struct ovm_services_threads_PriorityOVMDispatcher* dispatcher;
  struct test_runtime_TestRealtimeThreadScheduling_LogEntry* logger;
};
struct test_runtime_TestBasicPthreads {
  struct test_runtime_TestBase _parent_;
  struct ovm_core_services_threads_Pthreads_MutexPtr* mutex;
  struct ovm_core_services_threads_Pthreads_CondVarPtr* cv;
};
struct test_runtime_TestPragma {
  struct test_runtime_TestBase _parent_;
};
struct s3_core_domain_HTRepositoryUnboundSelector_Method2int_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int_Binding* link;
  struct ovm_core_repository_RepositoryUnboundSelector_Method* key;
  jint value;
};
struct java_lang_NumberFormatException {
  struct java_lang_IllegalArgumentException _parent_;
};
struct s3_core_domain_HTRepositoryUnboundSelector_Method2int {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_ovm_core_repository_RepositoryUnboundSelector_Method* keys_;
  struct arr_jint* values_;
  struct arr_s3_core_domain_HTRepositoryUnboundSelector_Method2int_Binding* collisions_;
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int* complementaryView_;
};
struct s3_core_domain_HTRepositoryUnboundSelector_Method2int_ReadOnly {
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int _parent_;
};
struct arr_s3_core_domain_HTSelector_Method2int_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_HTSelector_Method2int_Binding* values[0];
};
struct java_lang_Math {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_HTSelector_Field2Field {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_ovm_core_Selector_Field* keys_;
  struct arr_ovm_core_domain_Field* values_;
  struct arr_s3_core_domain_HTSelector_Field2Field_Binding* collisions_;
  struct s3_core_domain_HTSelector_Field2Field* complementaryView_;
};
struct s3_core_domain_HTSelector_Field2Field_1 {
  struct s3_core_domain_HTSelector_Field2Field _parent_;
};
struct java_lang_Character {
  struct java_lang_Object _parent_;
};
struct s3_services_bytecode_editor_S3CodeFragmentEditor_Factory {
  struct java_lang_Object _parent_;
  struct s3_services_bytecode_PackageEnvironment* pke_;
};
struct test_runtime_TestAllocation {
  struct test_runtime_TestBase _parent_;
};
struct arr_s3_core_domain_HTRepositoryUnboundSelector_Method2int_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_HTRepositoryUnboundSelector_Method2int_Binding* values[0];
};
struct test_runtime_TestSubtypetest {
  struct test_runtime_TestBase _parent_;
  struct test_runtime_TestSubtypetest_A* a;
  struct test_runtime_TestSubtypetest_B* b;
  struct test_runtime_TestSubtypetest_C* c;
  struct test_runtime_TestSubtypetest_D* d;
  struct test_runtime_TestSubtypetest_E* e;
  struct test_runtime_TestSubtypetest_F* f;
  struct test_runtime_TestSubtypetest_G* g;
  struct java_lang_Object* nullObj;
  struct arr_arr_arr_test_runtime_TestSubtypetest_A* arrA3;
  struct arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_A* arrA5;
  struct arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_B* arrB5;
  struct arr_arr_arr_arr_arr_jint* arrInteger5;
  struct arr_arr_arr_jint* arrInteger3;
  struct arr_arr_arr_java_lang_Object* arrObject3;
  struct arr_arr_arr_arr_arr_java_lang_Object* arrObject5;
  struct arr_arr_arr_test_runtime_TestSubtypetest_I* arrI3;
};
struct test_runtime_TestMethods {
  struct test_runtime_TestBase _parent_;
  struct test_runtime_TestMethods_Class3* c;
};
struct test_runtime_TestExceptions {
  struct test_runtime_TestBase _parent_;
};
struct s3_core_domain_HTSelector_Method2int_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_domain_HTSelector_Method2int_Binding* link;
  struct ovm_core_Selector_Method* key;
  jint value;
};
struct ovm_core_services_threads_ThreadManagerCoreImpl {
  struct ovm_services_ServiceInstanceImpl _parent_;
};
struct ovm_services_threads_UserLevelThreadManagerCoreImpl {
  struct ovm_core_services_threads_ThreadManagerCoreImpl _parent_;
  struct ovm_core_execution_Context* deadContext;
};
struct ovm_services_threads_OrderedUserLevelThreadManager {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_HTRepositoryUnboundSelector_Method2int_2 {
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int _parent_;
};
struct test_runtime_TestConstantPool {
  struct test_runtime_TestBase _parent_;
};
struct arr_arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_A {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_A* values[0];
};
struct s3_tools_jamit_JMode_JFieldModeImpl {
  struct s3_core_repository_S3Mode_FieldModeImpl _parent_;
};
struct s3_tools_jamit_JMode {
  struct s3_core_S3Base _parent_;
};
struct s3_core_domain_HTSelector_Method2Method {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  jint numElems_;
  struct arr_ovm_core_Selector_Method* keys_;
  struct arr_ovm_core_domain_Method* values_;
  struct arr_s3_core_domain_HTSelector_Method2Method_Binding* collisions_;
  struct s3_core_domain_HTSelector_Method2Method* complementaryView_;
};
struct s3_core_domain_HTSelector_Method2Method_1 {
  struct s3_core_domain_HTSelector_Method2Method _parent_;
};
struct arr_test_runtime_TestAllocation_B {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestAllocation_B* values[0];
};
struct java_lang_Integer {
  struct java_lang_Object _parent_;
  jint value;
};
struct java_lang_Long {
  struct java_lang_Object _parent_;
  jlong value;
};
struct java_lang_Float {
  struct java_lang_Object _parent_;
  jfloat value;
};
struct java_lang_Double {
  struct java_lang_Object _parent_;
  jdouble value;
};
struct arr_arr_arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_A {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_A* values[0];
};
struct ovm_util_LinkedList_LinkedListItr {
  struct java_lang_Object _parent_;
  jint knownMod;
  struct ovm_util_LinkedList_Entry* next;
  struct ovm_util_LinkedList_Entry* previous;
  struct ovm_util_LinkedList_Entry* lastReturned;
  jint position;
  struct ovm_util_LinkedList* this$0;
};
struct s3_core_domain_HTSelector_Field2Field_ReadOnly {
  struct s3_core_domain_HTSelector_Field2Field _parent_;
};
struct s3_core_domain_HTSelector_Field2Field_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_domain_HTSelector_Field2Field_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_domain_HTSelector_Field2Field* this$0;
};
struct test_runtime_TestAllocation_A {
  struct java_lang_Object _parent_;
  zint bo;
  jint i;
  jlong l;
  jdouble d;
  jfloat f;
  sint s;
  bint b;
};
struct ovm_services_threads_MonitorTrackingThread {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestAllocation_B {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_HTSelector_Field2Field_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_domain_HTSelector_Field2Field_Binding* link;
  struct ovm_core_Selector_Field* key;
  struct ovm_core_domain_Field* value;
};
struct ovm_util_LinkedList_Entry {
  struct java_lang_Object _parent_;
  struct java_lang_Object* data;
  struct ovm_util_LinkedList_Entry* next;
  struct ovm_util_LinkedList_Entry* previous;
};
struct test_runtime_TestAllocation_E {
  struct test_runtime_TestAllocation_A _parent_;
  jint i;
};
struct s3_util_queues_SingleLinkDeltaElement {
  struct java_lang_Object _parent_;
};
struct arr_arr_arr_arr_test_runtime_TestSubtypetest_A {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_test_runtime_TestSubtypetest_A* values[0];
};
struct arr_arr_arr_arr_test_runtime_TestSubtypetest_B {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_test_runtime_TestSubtypetest_B* values[0];
};
struct arr_arr_arr_arr_test_runtime_TestSubtypetest_C {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_test_runtime_TestSubtypetest_C* values[0];
};
struct arr_arr_arr_test_runtime_TestAllocation_B {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_test_runtime_TestAllocation_B* values[0];
};
struct arr_s3_core_domain_HTSelector_Method2Method_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_HTSelector_Method2Method_Binding* values[0];
};
struct arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_A {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_arr_test_runtime_TestSubtypetest_A* values[0];
};
struct ovm_util_HashMap_Null {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_HTSelector_Method2Method_ReadOnly {
  struct s3_core_domain_HTSelector_Method2Method _parent_;
};
struct arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_B {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_arr_test_runtime_TestSubtypetest_B* values[0];
};
struct ovm_util_HashMap_HashMapIterator {
  struct java_lang_Object _parent_;
  jint myType;
  jint knownMods;
  jint position;
  jint bucketIndex;
  struct ovm_util_Bucket_Node* currentNode;
  struct java_lang_Object* currentKey;
  struct ovm_util_HashMap* this$0;
};
struct arr_arr_arr_arr_arr_test_runtime_TestSubtypetest_C {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_arr_arr_test_runtime_TestSubtypetest_C* values[0];
};
struct s3_core_domain_HTSelector_Method2Method_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_domain_HTSelector_Method2Method_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_domain_HTSelector_Method2Method* this$0;
};
struct ovm_services_threads_PriorityOVMThread_Comparator {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_util_HashMap_HashMapSet {
  struct ovm_util_AbstractSet _parent_;
  jint setType;
  struct ovm_util_HashMap* this$0;
};
struct ovm_util_HashMap_HashMapCollection {
  struct ovm_util_AbstractCollection _parent_;
  struct ovm_util_HashMap* this$0;
};
struct arr_arr_test_runtime_TestAllocation_B {
    struct java_lang_Object _parent_;
    int length;
    struct arr_test_runtime_TestAllocation_B* values[0];
};
struct s3_tools_jamit_JMode_JMethodModeImpl {
  struct s3_core_repository_S3Mode_MethodModeImpl _parent_;
};
struct s3_core_domain_HTSelector_Method2Method_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_domain_HTSelector_Method2Method_Binding* link;
  struct ovm_core_Selector_Method* key;
  struct ovm_core_domain_Method* value;
};
struct ovm_util_HashMap_HashMapEntry {
  struct ovm_util_Bucket_Node _parent_;
};
struct arr_arr_test_runtime_TestSubtypetest_A {
    struct java_lang_Object _parent_;
    int length;
    struct arr_test_runtime_TestSubtypetest_A* values[0];
};
struct arr_arr_arr_test_runtime_TestSubtypetest_I {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_test_runtime_TestSubtypetest_I* values[0];
};
struct arr_arr_test_runtime_TestSubtypetest_B {
    struct java_lang_Object _parent_;
    int length;
    struct arr_test_runtime_TestSubtypetest_B* values[0];
};
struct arr_arr_test_runtime_TestSubtypetest_C {
    struct java_lang_Object _parent_;
    int length;
    struct arr_test_runtime_TestSubtypetest_C* values[0];
};
struct test_runtime_TestSubtypetest_L {
  struct java_lang_Object _parent_;
};
struct arr_arr_arr_test_runtime_TestSubtypetest_A {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_test_runtime_TestSubtypetest_A* values[0];
};
struct arr_arr_test_runtime_TestSubtypetest_I {
    struct java_lang_Object _parent_;
    int length;
    struct arr_test_runtime_TestSubtypetest_I* values[0];
};
struct arr_arr_arr_test_runtime_TestSubtypetest_B {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_test_runtime_TestSubtypetest_B* values[0];
};
struct arr_arr_arr_test_runtime_TestSubtypetest_C {
    struct java_lang_Object _parent_;
    int length;
    struct arr_arr_test_runtime_TestSubtypetest_C* values[0];
};
struct s3_util_queues_SingleLinkElement {
  struct java_lang_Object _parent_;
};
struct ovm_services_monitors_Monitor {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestSubtypetest_A {
  struct java_lang_Object _parent_;
};
struct arr_test_runtime_TestSubtypetest_I {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestSubtypetest_I* values[0];
};
struct test_runtime_TestSubtypetest_B {
  struct test_runtime_TestSubtypetest_A _parent_;
};
struct s3_core_domain_HTSelector_Field2int_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_domain_HTSelector_Field2int_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_domain_HTSelector_Field2int* this$0;
};
struct test_runtime_TestSubtypetest_C {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestSubtypetest_D {
  struct java_lang_Object _parent_;
};
struct s3_tools_jamit_JMode_JMethodModeImpl_Builder {
  struct s3_core_repository_S3Mode_MethodModeImpl_Builder _parent_;
};
struct arr_test_runtime_TestSubtypetest_A {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestSubtypetest_A* values[0];
};
struct test_runtime_TestSubtypetest_I {
  struct java_lang_Object _parent_;
};
struct s3_tools_jamit_JMode_JFieldModeImpl_Builder {
  struct s3_core_repository_S3Mode_FieldModeImpl_Builder _parent_;
};
struct arr_test_runtime_TestSubtypetest_B {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestSubtypetest_B* values[0];
};
struct test_runtime_TestSubtypetest_J {
  struct java_lang_Object _parent_;
};
struct s3_tools_jamit_JMode_JS3ModeFactory {
  struct s3_core_repository_S3Mode_S3ModeFactory _parent_;
};
struct test_runtime_TestSubtypetest_K {
  struct java_lang_Object _parent_;
};
struct java_io_ObjectOutputStream {
  struct java_lang_Object _parent_;
};
struct s3_tools_jamit_JMode_JClassModeImpl {
  struct s3_core_repository_S3Mode_ClassModeImpl _parent_;
};
struct test_runtime_TestSubtypetest_E {
  struct java_lang_Object _parent_;
};
struct java_io_ObjectInputStream {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestSubtypetest_F {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestSubtypetest_G {
  struct test_runtime_TestSubtypetest_D _parent_;
};
struct ovm_util_OVMError_Unimplemented {
  struct ovm_util_OVMError _parent_;
};
struct ovm_core_Selector_Method {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_TypeName_Compound {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_TypeName_Compound* values[0];
};
struct test_userlevel_TestFieldAccess {
  struct test_userlevel_TestBase _parent_;
  jint uninitializedInt;
  jlong unitializedLong;
  struct arr_jboolean* boolArray;
  jint i;
  jfloat f;
  zint z;
  jlong j;
  jdouble d;
  struct java_lang_Object* l;
  bint b;
  cint c;
  sint s;
  struct arr_java_lang_Object* objArray;
  struct arr_jbyte* byteArray;
  struct arr_jchar* charArray;
  struct arr_jshort* shortArray;
  struct arr_jint* intArray;
  struct arr_jfloat* floatArray;
  struct arr_jlong* longArray;
  struct arr_jdouble* doubleArray;
  struct arr_jshort* initializedShortArray;
  struct arr_jlong* myLongArray;
  struct arr_jbyte* myByteArray;
  struct arr_jint* myIntArray;
  struct arr_jchar* myCharArray;
  struct arr_jdouble* myDoubleArray;
  struct arr_java_lang_Object* objectArray;
  struct arr_jfloat* myFloatArray;
};
struct test_userlevel_TestStaticFieldAccess {
  struct test_userlevel_TestBase _parent_;
};
struct arr_ovm_core_Selector_Method {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_Selector_Method* values[0];
};
struct ovm_core_services_threads_OVMThread {
  struct java_lang_Object _parent_;
};
struct ovm_core_TypeName_Compound {
  struct java_lang_Object _parent_;
};
struct ovm_core_Selector_Field {
  struct java_lang_Object _parent_;
};
struct ovm_core_Selector_Field_Iterator {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_Selector_Field {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_Selector_Field* values[0];
};
struct ovm_util_Collections_4 {
  struct java_lang_Object _parent_;
  struct ovm_util_Iterator* val$i;
};
struct s3_core_domain_HTRepositoryUnboundSelector_Method2int_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int* this$0;
};
struct ovm_util_Collections_5 {
  struct ovm_util_AbstractList _parent_;
  jint val$n;
  struct java_lang_Object* val$o;
};
struct ovm_util_Collections_6 {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_7 {
  struct ovm_util_AbstractSet _parent_;
  struct java_lang_Object* val$o;
};
struct ovm_core_repository_CloneClassVisitor {
  struct ovm_core_OVMBase _parent_;
  struct ovm_core_repository_RepositoryClass_Builder* rClassBuilder;
  struct ovm_core_repository_RepositoryMember_Field_Builder* rFieldInfoBuilder;
  struct ovm_core_repository_RepositoryMember_Method_Builder* rMethodInfoBuilder;
  struct ovm_core_repository_RepositoryByteCodeFragment_Builder* rByteCodeFragment;
  struct ovm_core_repository_RepositoryConstantPool_Builder* rConstantPoolBuilder;
  struct ovm_core_repository_RepositoryByteCodeFragment* lastByteCodeFragment;
  struct ovm_core_repository_Services* repositoryFactory;
  struct ovm_core_TypeName_Scalar* typeName;
};
struct ovm_services_bytecode_editor_ClassCleaner_CleanVisitor {
  struct ovm_core_repository_CloneClassVisitor _parent_;
  struct java_util_Vector* methods;
  struct java_util_HashMap* me2code;
  struct ovm_core_repository_RepositoryConstantPool* newCP;
  struct arr_jbyte* newCode;
  struct ovm_services_bytecode_editor_ClassCleaner* this$0;
};
struct ovm_util_HTString {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint numElems_;
  jint maskCollisions_;
  struct arr_java_lang_String* keys_;
  struct arr_ovm_util_HTString_Binding* collisions_;
  struct ovm_util_HTString* complementaryView_;
};
struct ovm_util_HTString_ReadOnly {
  struct ovm_util_HTString _parent_;
};
struct ovm_util_HTString_Binding {
  struct ovm_core_OVMBase _parent_;
  struct ovm_util_HTString_Binding* link;
  struct java_lang_String* key;
};
struct ovm_util_HTString_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct ovm_util_HTString_Binding* currentBinding_;
  zint reachEnd;
  struct ovm_util_HTString* this$0;
};
struct test_runtime_TestSuite {
  struct test_runtime_TestBase _parent_;
  struct test_runtime_TestBase* tcp;
  struct test_runtime_TestBase* tal;
  struct test_runtime_TestBase* tsu;
  struct test_runtime_TestBase* tsm;
  struct test_runtime_TestBase* tex;
  struct test_runtime_TestBase* tst;
  struct test_runtime_TestBase* tsb;
  struct test_runtime_TestBase* tth;
  struct test_runtime_TestBase* tcl;
  struct test_runtime_TestBase* tsc;
  struct test_runtime_TestBase* tio;
  struct test_runtime_TestBase* tsy;
  struct test_runtime_TestBase* tts;
  struct test_runtime_TestBase* rts;
  struct test_runtime_TestBase* pth;
};
struct s3_core_domain_MemberResolver {
  struct s3_core_S3Base _parent_;
  struct s3_core_domain_S3Domain* domain;
};
struct arr_ovm_util_HTString_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_HTString_Binding* values[0];
};
struct ovm_core_TypeName {
  struct java_lang_Object _parent_;
};
struct ovm_services_threads_PriorityOVMDispatcher {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_SynchronizedCollection {
  struct java_lang_Object _parent_;
  struct java_lang_Object* sync;
  struct ovm_util_Collection* c;
};
struct ovm_util_Collections_SynchronizedSet {
  struct ovm_util_Collections_SynchronizedCollection _parent_;
};
struct ovm_util_Collections_SynchronizedSortedSet {
  struct ovm_util_Collections_SynchronizedSet _parent_;
  struct ovm_util_SortedSet* ss;
};
struct ovm_util_Collections_UnmodifiableCollection {
  struct java_lang_Object _parent_;
  struct ovm_util_Collection* c;
};
struct ovm_services_threads_PriorityOVMThread {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_S3Blueprint_1 {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Type_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_threads_TimedSuspensionThreadManager {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_SynchronizedMap {
  struct java_lang_Object _parent_;
  struct java_lang_Object* sync;
  struct ovm_util_Map* m;
};
struct ovm_util_Collections_SynchronizedSortedMap {
  struct ovm_util_Collections_SynchronizedMap _parent_;
  struct ovm_util_SortedMap* sm;
};
struct ovm_services_threads_UserLevelThreadManager {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_SynchronizedList {
  struct ovm_util_Collections_SynchronizedCollection _parent_;
  struct ovm_util_List* l;
};
struct s3_core_domain_S3Blueprint {
  struct s3_core_S3Base _parent_;
  struct s3_core_domain_HTSelector_Field2int* offsets_;
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int* vtableOffsets_;
  struct s3_core_domain_HTSelector_Method2int* nvtableOffsets_;
  struct s3_core_domain_S3Blueprint_Scalar* parent_;
  struct ovm_core_domain_Oop* shared_;
  jint size_;
  struct arr_ovm_core_repository_RepositoryCodeFragment* vTable;
  struct arr_ovm_core_repository_RepositoryCodeFragment* ifTable;
  struct arr_ovm_core_repository_RepositoryByteCodeFragment* nvTable;
  struct arr_s3_core_repository_S3NativeCodeFragment* nativeVTable;
  struct arr_s3_core_repository_S3NativeCodeFragment* nativeNVTable;
  struct arr_s3_core_repository_S3NativeCodeFragment* nativeIfTable;
  struct arr_jbyte* typeDisplay_;
  jint typeBucket_;
  jint typeBucketID_;
  jint arrayDepth_;
  struct arr_s3_core_domain_S3Blueprint* linkSet;
  zint linkSetClosed_;
  struct arr_jbyte* dbg_string;
};
struct s3_core_domain_S3Blueprint_Array {
  struct s3_core_domain_S3Blueprint _parent_;
  jint lengthFieldOffset_;
  struct ovm_core_domain_Type_Array* type_;
  jint componentSize_;
  struct s3_core_domain_S3Blueprint* componentBlueprint_;
};
struct ovm_util_Collections_9 {
  struct ovm_util_AbstractList _parent_;
  struct java_lang_Object* val$o;
};
struct ovm_util_Collections_10 {
  struct ovm_util_AbstractMap _parent_;
  struct java_lang_Object* val$key;
  struct java_lang_Object* val$value;
};
struct s3_core_domain_S3Blueprint_Primitive {
  struct s3_core_domain_S3Blueprint _parent_;
  struct ovm_core_domain_Type_Primitive* type_;
};
struct ovm_core_Selector {
  struct java_lang_Object _parent_;
};
struct java_lang_ArrayStoreException {
  struct java_lang_RuntimeException _parent_;
};
struct ovm_core_domain_Services {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_S3Blueprint_SharedStatePlaceHolder {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_CodeSource_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_Parser_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_FileReader_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_Installer_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_editor_CloneInstructionVisitor {
  struct ovm_services_bytecode_InstructionVisitor _parent_;
  struct ovm_services_bytecode_editor_Cursor* cursor;
  struct ovm_services_bytecode_editor_CodeFragmentEditor* editor;
};
struct s3_core_domain_DispatchBuilder {
  struct s3_core_S3Base _parent_;
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int* vtableOffsets_;
  struct s3_core_domain_HTSelector_Method2int* nvtableOffsets_;
  struct s3_core_domain_S3Blueprint_Scalar* bpt;
  struct ovm_core_domain_Type_Scalar* type;
  struct arr_ovm_core_repository_RepositoryMember_Method* methods;
  struct s3_core_domain_MemberResolver* resolver;
  struct s3_core_domain_HTRepositoryUnboundSelector_Method2int* ifaceOffsets;
  jint lastGlobalInterfaceMethodIndex;
};
struct ovm_util_OVMError_Internal {
  struct ovm_util_OVMError _parent_;
};
struct arr_s3_core_domain_S3Blueprint {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_S3Blueprint* values[0];
};
struct s3_core_domain_S3Blueprint_Scalar {
  struct s3_core_domain_S3Blueprint _parent_;
  struct ovm_core_domain_Type_Scalar* type_;
  struct arr_jint* constantPool_;
  struct s3_core_repository_S3ConstantPool* cpp;
};
struct ovm_util_HTObject {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint numElems_;
  jint maskCollisions_;
  struct arr_java_lang_Object* keys_;
  struct arr_ovm_util_HTObject_Binding* collisions_;
  struct ovm_util_HTObject* complementaryView_;
};
struct ovm_core_services_threads_Pthreads {
  struct java_lang_Object _parent_;
};
struct arr_ovm_services_bytecode_reader_CodeSource {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_reader_CodeSource* values[0];
};
struct ovm_util_AbstractList_AbstractListItr {
  struct java_lang_Object _parent_;
  jint knownMod;
  jint position;
  jint lastReturned;
  jint size;
  struct ovm_util_AbstractList* this$0;
};
struct ovm_util_SubList {
  struct ovm_util_AbstractList _parent_;
  struct ovm_util_AbstractList* backingList;
  jint offset;
  jint size;
};
struct java_lang_Runnable {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_Parser {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_FileReader {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_Installer {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_UnmodifiableSet {
  struct ovm_util_Collections_UnmodifiableCollection _parent_;
};
struct ovm_util_Collections_11 {
  struct ovm_util_Collections_UnmodifiableSet _parent_;
  struct ovm_util_Collections_UnmodifiableMap* this$0;
};
struct ovm_core_services_threads_Pthreads_MutexPtr {
  struct ovm_core_execution_Native_Ptr _parent_;
};
struct ovm_core_services_threads_Pthreads_CondVarPtr {
  struct ovm_core_execution_Native_Ptr _parent_;
};
struct ovm_core_services_threads_Pthreads_TimeSpecPtr {
  struct ovm_core_execution_Native_Ptr _parent_;
};
struct s3_core_domain_MemberResolver_ResolutionInfo {
  struct java_lang_Object _parent_;
  jint offset;
  struct ovm_core_domain_Oop* target;
};
struct ovm_util_Collections_14 {
  struct ovm_util_Collections_SynchronizedSet _parent_;
  struct ovm_util_Collections_SynchronizedMap* this$0;
};
struct ovm_util_Arrays_ListImpl {
  struct ovm_util_AbstractList _parent_;
  struct arr_java_lang_Object* a;
};
struct ovm_util_Arrays_DefaultComparator {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_services_bytecode_verifier_AbstractValue_Double {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3ConstantPoolBuilder {
  struct s3_core_S3Base _parent_;
  struct arr_ovm_core_repository_RepositoryConstantPool_Entry* values_;
  struct arr_jint* primitives_;
  struct arr_jbyte* tags_;
  struct arr_java_lang_Object* references_;
  struct s3_core_repository_S3RepositoryIfc* repository_;
  struct ovm_util_ByteBuffer* conv_;
};
struct ovm_util_Collections_2 {
  struct ovm_util_AbstractList _parent_;
};
struct s3_core_domain_MemberResolver_IllegalAccessException {
  struct ovm_util_OVMException_IllegalAccess _parent_;
  struct ovm_core_repository_RepositoryUnboundSelector* what;
  struct ovm_core_TypeName_Compound* where;
  struct ovm_core_TypeName_Compound* target;
  struct s3_core_domain_MemberResolver* this$0;
};
struct ovm_services_bytecode_verifier_AbstractValue_Long {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_3 {
  struct ovm_util_AbstractMap _parent_;
};
struct s3_core_repository_S3ClassBuilder {
  struct s3_core_repository_S3Builder _parent_;
  cint minorVersion;
  cint majorVersion;
  struct s3_core_repository_S3TypeName_Scalar* name;
  struct s3_core_repository_S3TypeName_Scalar* superName;
  struct s3_core_repository_S3TypeName_Scalar* outerName;
  struct ovm_core_Mode_Class* mode;
  struct ovm_util_List* referencedClasses;
  struct s3_core_repository_S3ConstantPool* constantPool;
  struct ovm_util_List* interfaces;
  struct s3_core_repository_HTRepositoryUnboundSelector_Method2RepositoryMember_Method* methods;
  struct s3_core_repository_LISTRepositoryUnboundSelector_Field* fieldSelectors;
  struct s3_core_repository_LISTRepositoryMember_Field* fieldInfos;
  struct s3_core_repository_LISTTypeName_Scalar* staticClasses;
  struct s3_core_repository_LISTTypeName_Scalar* instanceClasses;
};
struct ovm_util_Collections_SynchronizedListIterator {
  struct ovm_util_Collections_SynchronizedIterator _parent_;
  struct ovm_util_ListIterator* li;
};
struct ovm_services_bytecode_verifier_AbstractValue_Float {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_DispatchBuilder_IMTEntry {
  struct java_lang_Object _parent_;
  jint offset;
  struct ovm_core_repository_RepositoryCodeFragment* code;
};
struct ovm_util_Collections_UnmodifiableSortedSet {
  struct ovm_util_Collections_UnmodifiableSet _parent_;
  struct ovm_util_SortedSet* ss;
};
struct ovm_services_bytecode_verifier_AbstractValue_Null {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3MethodBuilder {
  struct s3_core_repository_S3Builder _parent_;
  struct s3_core_repository_S3Descriptor_Method* descriptor;
  struct ovm_core_Mode_Method* mode;
  struct s3_core_repository_S3CodeFragment* codeFragment;
  struct ovm_util_List* thrownExceptions;
  jint nameIndex;
};
struct ovm_services_bytecode_verifier_AbstractValue_Array {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_EmptyAbstractSet {
  struct ovm_util_AbstractSet _parent_;
};
struct s3_core_repository_S3FieldBuilder {
  struct s3_core_repository_S3Builder _parent_;
  struct s3_core_repository_S3Descriptor_Field* descriptor;
  jint nameIndex;
  struct ovm_core_Mode_Field* mode;
  jlong constantValueBits;
};
struct ovm_util_Collections_UnmodifiableMap {
  struct java_lang_Object _parent_;
  struct ovm_util_Map* m;
};
struct ovm_util_Collections_UnmodifiableSortedMap {
  struct ovm_util_Collections_UnmodifiableMap _parent_;
  struct ovm_util_SortedMap* sm;
};
struct ovm_services_bytecode_verifier_AbstractValue_Int {
  struct java_lang_Object _parent_;
};
struct ovm_util_Collections_UnmodifiableList {
  struct ovm_util_Collections_UnmodifiableCollection _parent_;
  struct ovm_util_List* l;
};
struct ovm_services_bytecode_verifier_AbstractValueError {
  struct java_lang_Error _parent_;
};
struct ovm_util_Arrays {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_util_SortedMap {
  struct java_lang_Object _parent_;
};
struct ovm_util_SortedSet {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_verifier_AbstractValue_Invalid {
  struct java_lang_Object _parent_;
};
struct ovm_util_ListIterator {
  struct java_lang_Object _parent_;
};
struct java_lang_ClassNotFoundException {
  struct java_lang_Exception _parent_;
};
struct ovm_util_Comparator {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_S3Blueprint_Primitive_NoMethodsError {
  struct ovm_util_OVMError _parent_;
};
struct java_lang_UnsupportedOperationException {
  struct java_lang_RuntimeException _parent_;
};
struct ovm_util_Collections_UnmodifiableListIterator {
  struct ovm_util_Collections_UnmodifiableIterator _parent_;
  struct ovm_util_ListIterator* li;
};
struct arr_s3_core_domain_S3Method {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_S3Method* values[0];
};
struct ovm_services_bytecode_verifier_AbstractValue {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_S3Type_SharedStateClass {
  struct s3_core_domain_S3Type_ClassBase _parent_;
};
struct ovm_services_bytecode_verifier_AbstractValue_WidePrimitive {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_S3Method {
  struct s3_core_S3Base _parent_;
  struct ovm_core_repository_RepositoryMember_Method* info;
  struct ovm_core_domain_Type_Compound* declaringType;
};
struct ovm_util_Enumeration {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_verifier_AbstractValue_Primitive {
  struct java_lang_Object _parent_;
};
struct s3_core_domain_S3Type_MethodIterator {
  struct java_lang_Object _parent_;
  struct s3_core_domain_S3Type_Reference* currentType;
  jint nextIndex;
};
struct ovm_services_bytecode_verifier_AbstractValue_Reference {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_timer_TimerManager {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_verifier_AbstractValue_JumpTarget {
  struct java_lang_Object _parent_;
};
struct s3_services_threads_BasicUserLevelThreadManagerImpl {
  struct ovm_services_threads_UserLevelThreadManagerCoreImpl _parent_;
  struct s3_util_queues_SingleLinkPriorityQueue* readyQ;
  jint readySize;
  struct java_util_Comparator* comp;
  zint schedulingEnabled;
};
struct java_lang_InstantiationException {
  struct java_lang_Exception _parent_;
};
struct arr_s3_core_repository_S3NativeCodeFragment {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3NativeCodeFragment* values[0];
};
struct s3_core_repository_S3CodeFragment {
  struct s3_core_S3Base _parent_;
  struct s3_core_repository_S3CodeFragment* next_;
  struct ovm_core_repository_RepositoryCodeFragmentKind* kind_;
  struct ovm_core_repository_RepositoryDescriptor_Method* descriptor_;
};
struct s3_core_repository_S3NativeCodeFragment {
  struct s3_core_repository_S3CodeFragment _parent_;
  struct arr_jbyte* code;
  struct s3_core_repository_S3NativeCodeFragment_Linker* linker;
  jint baseAddress;
  struct arr_ovm_core_repository_RepositoryNativeExceptionHandler* exceptions_;
  zint synchronized_;
  struct ovm_core_repository_RepositoryConstantPool* constantPool_;
  struct arr_jbyte* dbg_selector;
  struct java_lang_String* dbg_selector_string;
};
struct arr_s3_core_repository_S3CodeFragment {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3CodeFragment* values[0];
};
struct s3_services_threads_DispatcherImpl {
  struct ovm_services_ServiceInstanceImpl _parent_;
  struct ovm_services_threads_UserLevelThreadManager* tm;
  jint nThreads;
};
struct s3_services_threads_PriorityDispatcherImpl {
  struct s3_services_threads_DispatcherImpl _parent_;
};
struct ovm_core_repository_RepositoryClass_Builder_RedeclarationError {
  struct ovm_util_OVMError _parent_;
};
struct arr_ovm_core_domain_Method {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_domain_Method* values[0];
};
struct s3_services_bytecode_PackageEnvironment {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_InstructionSet* instructionSet;
  struct ovm_services_bytecode_editor_CodeFragmentEditor_Factory* editorF;
  struct s3_services_bytecode_writer_S3Dumper_Factory* dumperF;
  struct s3_services_bytecode_verifier_S3Frame_Factory* frameF;
  struct ovm_services_bytecode_verifier_Verifier_Factory* verifierF;
  struct ovm_services_bytecode_editor_ClassCleaner* cleaner_;
};
struct ovm_core_domain_Blueprint_Array {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Blueprint_Record {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Blueprint_Primitive {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Method {
  struct java_lang_Object _parent_;
};
struct java_lang_Byte {
  struct java_lang_Object _parent_;
};
struct java_lang_Short {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryCodeFragmentKind_NativeCodeFragmentKind {
  struct ovm_core_repository_RepositoryCodeFragmentKind _parent_;
};
struct ovm_core_services_memory_VM_Address_UnresolvedException {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct ovm_services_bytecode_SpecificationIR_FieldAccessExp {
  struct java_lang_Object _parent_;
  struct ovm_core_Selector_Field* selector;
  struct ovm_services_bytecode_SpecificationIR_NonnulRefValue* obj;
};
struct ovm_services_bytecode_SpecificationIR_LinkSetAccessExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_LinkSetIndexValue* index;
};
struct ovm_core_repository_RepositoryClass_Locator {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_VM_Address_Null {
  struct ovm_core_services_memory_VM_Address _parent_;
};
struct ovm_core_services_memory_VM_Address_Ooped {
  struct ovm_core_services_memory_VM_Address _parent_;
};
struct ovm_services_bytecode_SpecificationIR_MemExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_RefValue* addr;
  struct ovm_services_bytecode_SpecificationIR_IntValue* offset;
};
struct ovm_services_bytecode_SpecificationIR_BlueprintAccessExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_NonnulRefValue* ref;
};
struct ovm_services_bytecode_SpecificationIR_SymbolicConstant {
  struct java_lang_Object _parent_;
  struct java_lang_String* name;
};
struct arr_s3_core_repository_S3Method {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Method* values[0];
};
struct ovm_core_services_memory_VM_Address_BCsetshort {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_services_bytecode_editor_ClassCleaner_CleanVisitor_CleanCodeVisitor {
  struct ovm_services_bytecode_InstructionVisitor _parent_;
  struct ovm_services_bytecode_InstructionSet* is;
  struct ovm_services_bytecode_editor_ClassCleaner_CleanVisitor* this$1;
};
struct ovm_core_services_memory_VM_Address_BCsetbyte {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_core_services_memory_VM_Address_BCeatcast {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_core_services_memory_VM_Address_BCsetblock {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_core_services_memory_VM_Address_BCsetwide {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct s3_core_repository_S3Method {
  struct s3_core_S3Base _parent_;
  struct s3_core_repository_S3Class* definingClass_;
  struct s3_core_repository_S3UnboundSelector_Method* selector_;
  struct ovm_core_Mode_Method* mode_;
  struct s3_core_repository_S3CodeFragment* code_;
  struct arr_s3_core_repository_S3TypeName_Scalar* thrownExceptions_;
  struct arr_s3_core_repository_S3Attribute* attributes_;
};
struct s3_core_domain_S3Domain_Factory {
  struct java_lang_Object _parent_;
  struct ovm_core_domain_Services* pke_;
};
struct ovm_core_services_memory_VM_Address_Deferral {
  struct java_lang_Object _parent_;
  struct ovm_util_Set* deferred;
  zint busy;
  struct ovm_core_services_memory_VM_Address* this$0;
};
struct ovm_core_services_memory_VM_Address_BCcas {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_services_bytecode_SpecificationIR_LinkSetIndexValue {
  struct ovm_services_bytecode_SpecificationIR_IntValue _parent_;
};
struct ovm_core_services_memory_VM_Address_DeferredSetAddress {
  struct java_lang_Object _parent_;
  struct ovm_core_services_memory_VM_Address* location;
  struct ovm_core_services_memory_VM_Address* value;
};
struct java_io_FilterOutputStream {
  struct java_io_OutputStream _parent_;
  struct java_io_OutputStream* out;
};
struct java_io_DataOutputStream {
  struct java_io_FilterOutputStream _parent_;
  jint written;
};
struct java_io_DataOutput {
  struct java_lang_Object _parent_;
};
struct s3_services_monitors_MonitorFieldMonitorMapper {
  struct ovm_services_ServiceInstanceImpl _parent_;
  struct ovm_core_domain_ObjectHeader* headerLayout;
};
struct ovm_services_bytecode_SpecificationIR_SecondHalf {
  struct ovm_services_bytecode_SpecificationIR_Value _parent_;
};
struct ovm_core_domain_Type_Primitive {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Type_Array {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryMember_Field_Iterator {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_HTS3Class {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint numElems_;
  jint maskCollisions_;
  struct arr_s3_core_repository_S3Class* keys_;
  struct arr_s3_core_repository_HTS3Class_Binding* collisions_;
  struct s3_core_repository_HTS3Class* complementaryView_;
};
struct s3_core_repository_S3UTF8Store_HTUtf8 {
  struct ovm_core_OVMBase _parent_;
  jint mask_;
  jint maskCollisions_;
  struct arr_jint* keys_;
  struct arr_jint* values_;
  struct arr_arr_jint* collisions_;
  jint count;
  struct s3_core_repository_S3UTF8Store* this$0;
};
struct s3_core_repository_HTS3TypeName_ReadOnly {
  struct s3_core_repository_HTS3TypeName _parent_;
};
struct s3_core_repository_HTS3TypeName_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_HTS3TypeName_Binding* link;
  struct s3_core_repository_S3TypeName* key;
};
struct s3_services_bytecode_reader_S3Parser_ClassParsingException {
  struct ovm_core_repository_ClassFormatException _parent_;
  struct s3_services_bytecode_reader_S3Parser* this$0;
};
struct s3_core_repository_HTS3TypeName_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_repository_HTS3TypeName_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_repository_HTS3TypeName* this$0;
};
struct arr_s3_core_repository_HTS3TypeName_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_HTS3TypeName_Binding* values[0];
};
struct arr_s3_core_repository_HTS3UnboundSelector_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_HTS3UnboundSelector_Binding* values[0];
};
struct s3_core_repository_HTS3Descriptor_ReadOnly {
  struct s3_core_repository_HTS3Descriptor _parent_;
};
struct s3_core_repository_HTS3Descriptor_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_HTS3Descriptor_Binding* link;
  struct s3_core_repository_S3Descriptor* key;
};
struct ovm_core_services_threads_OVMThreadContext {
  struct ovm_core_execution_Context _parent_;
  struct ovm_core_services_threads_OVMThread* thisThread;
};
struct s3_core_repository_HTS3UnboundSelector_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_HTS3UnboundSelector_Binding* link;
  struct s3_core_repository_S3UnboundSelector* key;
};
struct arr_s3_core_repository_HTS3Descriptor_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_HTS3Descriptor_Binding* values[0];
};
struct s3_core_repository_HTS3UnboundSelector_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_repository_HTS3UnboundSelector_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_repository_HTS3UnboundSelector* this$0;
};
struct s3_services_monitors_BasicMonitorImpl {
  struct ovm_core_OVMBase _parent_;
  struct s3_util_queues_SingleLinkPriorityQueue* entryQ;
  struct java_util_Comparator* comp;
  struct ovm_core_services_threads_OVMThread* owner;
  jint entryCount;
  jint entrySize;
  zint DEBUG;
};
struct s3_services_bytecode_reader_S3FileReader_1 {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_HTS3Descriptor_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_repository_HTS3Descriptor_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_repository_HTS3Descriptor* this$0;
};
struct ovm_services_bytecode_reader_CodeSource_File {
  struct ovm_core_OVMBase _parent_;
};
struct s3_services_bytecode_reader_S3CodeSource_1 {
  struct ovm_services_bytecode_reader_CodeSource_File _parent_;
  struct ovm_util_ByteBuffer* contents;
  struct java_lang_String* val$fname;
  struct s3_services_bytecode_reader_S3CodeSource_StandaloneDir* this$0;
};
struct s3_core_repository_HTS3UnboundSelector_ReadOnly {
  struct s3_core_repository_HTS3UnboundSelector _parent_;
};
struct s3_core_repository_HTS3Selector_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_HTS3Selector_Binding* link;
  struct s3_core_repository_S3Selector* key;
};
struct ovm_core_services_memory_VM_Word_BCsle {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct s3_core_repository_HTS3Selector_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_repository_HTS3Selector_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_repository_HTS3Selector* this$0;
};
struct ovm_core_services_memory_VM_Word_BCsge {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCsgt {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCscmp {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCisub {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCeq0 {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCeq {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_repository_RepositoryUnboundSelector_Field_Iterator {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_HTS3Selector_ReadOnly {
  struct s3_core_repository_HTS3Selector _parent_;
};
struct ovm_core_services_memory_VM_Word_BCne {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCslt {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct arr_s3_core_repository_HTS3Selector_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_HTS3Selector_Binding* values[0];
};
struct ovm_core_services_memory_VM_Word_BCiconst_4 {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCiadd {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Address_BCgetwide {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_core_services_memory_VM_Address_BCgetshort {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_core_TypeName_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_VM_Address_BCgetbyte {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct s3_core_repository_HTS3Class_ReadOnly {
  struct s3_core_repository_HTS3Class _parent_;
};
struct s3_core_repository_HTS3Class_Binding {
  struct ovm_core_OVMBase _parent_;
  struct s3_core_repository_HTS3Class_Binding* link;
  struct s3_core_repository_S3Class* key;
};
struct ovm_core_Selector_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_VM_Address_1 {
  struct s3_util_PragmaInlineSubstituteBytecode_Rewriter _parent_;
};
struct ovm_core_services_memory_VM_Word_BCugt {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct arr_s3_core_repository_HTS3Class_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_HTS3Class_Binding* values[0];
};
struct ovm_core_services_memory_VM_Word_BCucmp {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCult {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_core_services_memory_VM_Word_BCule {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct s3_core_repository_S3ConstantPool {
  struct s3_core_S3Base _parent_;
  struct arr_ovm_core_repository_RepositoryConstantPool_Entry* values_;
  struct arr_jint* primitives_;
  struct arr_jbyte* tags_;
  struct arr_java_lang_Object* references_;
};
struct ovm_core_services_memory_VM_Word_BCuge {
  struct ovm_core_services_memory_VM_Word_BC _parent_;
};
struct ovm_services_bytecode_SpecificationIR_FloatValue {
  struct ovm_services_bytecode_SpecificationIR_Value _parent_;
};
struct ovm_services_bytecode_SpecificationIR_WideValue {
  struct ovm_services_bytecode_SpecificationIR_Value _parent_;
  struct ovm_services_bytecode_SpecificationIR_SecondHalf* secondHalf;
};
struct arr_ovm_services_bytecode_SpecificationIR_DoubleValue {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_SpecificationIR_DoubleValue* values[0];
};
struct ovm_services_bytecode_SpecificationIR_LongValue {
  struct ovm_services_bytecode_SpecificationIR_WideValue _parent_;
};
struct ovm_services_bytecode_SpecificationIR_LocalStore {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_IntValue* index;
  struct ovm_services_bytecode_SpecificationIR_Value* value;
};
struct ovm_services_bytecode_SpecificationIR_ConversionExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_Value* before;
};
struct ovm_services_monitors_MonitorMapper {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_DoubleValue {
  struct ovm_services_bytecode_SpecificationIR_WideValue _parent_;
};
struct arr_ovm_services_bytecode_SpecificationIR_LongValue {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_SpecificationIR_LongValue* values[0];
};
struct ovm_services_bytecode_SpecificationIR_UnaryExp {
  struct java_lang_Object _parent_;
  struct java_lang_String* operator;
  struct ovm_services_bytecode_SpecificationIR_Value* arg;
};
struct ovm_services_bytecode_SpecificationIR_ConcreteFloat {
  struct ovm_services_bytecode_SpecificationIR_FloatValue _parent_;
  struct java_lang_Float* concreteValue;
};
struct ovm_services_bytecode_SpecificationIR_RefValue {
  struct ovm_services_bytecode_SpecificationIR_Value _parent_;
};
struct ovm_services_bytecode_SpecificationIR_NonnulRefValue {
  struct ovm_services_bytecode_SpecificationIR_RefValue _parent_;
};
struct ovm_services_bytecode_SpecificationIR_BinExp {
  struct java_lang_Object _parent_;
  struct java_lang_String* operator;
  struct ovm_services_bytecode_SpecificationIR_Value* lhs;
  struct ovm_services_bytecode_SpecificationIR_Value* rhs;
};
struct ovm_core_repository_RepositoryUnboundSelector_Method_Iterator {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_ConcreteDoubleValue {
  struct ovm_services_bytecode_SpecificationIR_DoubleValue _parent_;
  struct java_lang_Double* concreteValue;
};
struct ovm_services_bytecode_SpecificationIR_ValueList {
  struct ovm_services_bytecode_SpecificationIR_Value _parent_;
  struct ovm_services_bytecode_SpecificationIR_IntValue* valueCount;
};
struct s3_core_repository_S3ByteCodeFragment {
  struct s3_core_repository_S3CodeFragment _parent_;
  struct arr_jbyte* code_;
  cint maxStack_;
  cint maxLocals_;
  struct arr_ovm_core_repository_RepositoryExceptionHandler* exceptions_;
  struct arr_ovm_core_repository_RepositoryAttribute* attributes_;
  struct s3_core_repository_S3ConstantPool* constantPool_;
  zint isSynchronized_;
  struct arr_jbyte* dbg_string;
  struct ovm_core_Selector_Method* selector;
};
struct ovm_services_bytecode_SpecificationIR_CurrentPC {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_ConcreteLongValue {
  struct ovm_services_bytecode_SpecificationIR_LongValue _parent_;
  struct java_lang_Long* concreteValue;
};
struct test_runtime_TestMethods_Class1 {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestMethods_Class2 {
  struct test_runtime_TestMethods_Class1 _parent_;
  jint field;
};
struct test_runtime_TestMethods_Class3 {
  struct test_runtime_TestMethods_Class2 _parent_;
};
struct s3_core_repository_S3ByteCodeFragmentBuilder {
  struct s3_core_repository_S3Builder _parent_;
  struct arr_jbyte* code;
  struct ovm_core_repository_RepositoryConstantPool* constantPool;
  jint maxLocals;
  jint maxStack;
  struct ovm_util_List* handlers;
  struct s3_core_repository_S3Descriptor_Method* descriptor;
  jint nameIndex;
  struct ovm_core_TypeName_Scalar* definingClass;
  zint isSynchronized;
};
struct s3_core_repository_PackageEnvironment {
  struct java_lang_Object _parent_;
  struct s3_core_repository_S3Repository* repository_;
};
struct ovm_services_bytecode_SpecificationIR_CPAccessExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_CPIndexValue* value;
  zint isWide;
};
struct ovm_services_bytecode_SpecificationIR_Padding {
  struct ovm_services_bytecode_SpecificationIR_IntValue _parent_;
};
struct ovm_services_bytecode_SpecificationIR_IntValueList {
  struct ovm_services_bytecode_SpecificationIR_IntValue _parent_;
  struct ovm_services_bytecode_SpecificationIR_IntValue* valueCount;
};
struct test_runtime_TestExceptions_CException {
  struct java_lang_Exception _parent_;
};
struct ovm_util_HTString2int {
  struct java_lang_Object _parent_;
  jint mask_;
  jint maskCollisions_;
  struct arr_java_lang_String* keys_;
  struct arr_jint* values_;
  struct arr_ovm_util_HTString2int_Binding* collisions_;
};
struct ovm_services_bytecode_SpecificationIR_ListElementExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_IntValueList* list;
  struct ovm_services_bytecode_SpecificationIR_IntValue* index;
};
struct ovm_core_domain_Blueprint_Scalar {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestExceptions_AException {
  struct java_lang_Exception _parent_;
};
struct test_runtime_TestExceptions_BException {
  struct test_runtime_TestExceptions_AException _parent_;
};
struct ovm_services_bytecode_SpecificationIR_CSACallExp {
  struct java_lang_Object _parent_;
  struct java_lang_String* fname;
  struct arr_ovm_services_bytecode_SpecificationIR_Value* args;
};
struct ovm_core_services_threads_OVMDispatcher {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestMethods_IFace1 {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_ValueSourceVisitor {
  struct java_lang_Object _parent_;
};
struct ovm_util_HTString2int_Binding {
  struct java_lang_Object _parent_;
  struct ovm_util_HTString2int_Binding* link;
  struct java_lang_String* key;
  jint value;
};
struct ovm_util_HTObject_ReadOnly {
  struct ovm_util_HTObject _parent_;
};
struct test_runtime_TestMethods_IFace3 {
  struct java_lang_Object _parent_;
};
struct ovm_util_HTObject_Binding {
  struct ovm_core_OVMBase _parent_;
  struct ovm_util_HTObject_Binding* link;
  struct java_lang_Object* key;
};
struct test_runtime_TestMethods_IFace2 {
  struct java_lang_Object _parent_;
};
struct ovm_util_HTObject_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct ovm_util_HTObject_Binding* currentBinding_;
  zint reachEnd;
  struct ovm_util_HTObject* this$0;
};
struct s3_util_PragmaInlineSubstituteBytecode_DeadCallsiteReachedError {
  struct ovm_util_OVMError _parent_;
};
struct arr_ovm_util_HTString2int_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_HTString2int_Binding* values[0];
};
struct arr_ovm_util_HTObject_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_util_HTObject_Binding* values[0];
};
struct arr_ovm_core_services_timer_TimerInterruptAction {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_services_timer_TimerInterruptAction* values[0];
};
struct arr_test_runtime_TestConversionToString_1_FloatPair {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestConversionToString_1_FloatPair* values[0];
};
struct s3_core_repository_S3Descriptor_1 {
  struct java_lang_Object _parent_;
};
struct arr_test_runtime_TestConversionToString_1_DoublePair {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestConversionToString_1_DoublePair* values[0];
};
struct ovm_core_services_timer_FinalTimerInterruptAction {
  struct java_lang_Object _parent_;
};
struct arr_test_runtime_TestConversionToString_1_IntegralPair {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestConversionToString_1_IntegralPair* values[0];
};
struct s3_core_repository_S3Descriptor_RetrievableByteArrayOutputStream {
  struct java_io_ByteArrayOutputStream _parent_;
};
struct ovm_core_services_timer_TimerInterruptAction {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestConversionToString_1_FloatPair {
  struct java_lang_Object _parent_;
  jfloat val;
  struct java_lang_String* str_val;
  struct test_runtime_TestConversionToString* this$0;
};
struct ovm_core_TypeName_Array {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestConversionToString_1_DoublePair {
  struct java_lang_Object _parent_;
  jdouble val;
  struct java_lang_String* str_val;
  struct test_runtime_TestConversionToString* this$0;
};
struct s3_util_PragmaException_UnregisteredPragmaException {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct test_runtime_TestConversionToString_1_IntegralPair {
  struct java_lang_Object _parent_;
  jlong val;
  struct java_lang_String* str_val;
  struct test_runtime_TestConversionToString* this$0;
};
struct s3_services_monitors_BasicMonitorImpl_Factory {
  struct ovm_core_OVMBase _parent_;
};
struct test_runtime_TestCreateOVMThread_1 {
  struct ovm_core_services_threads_OVMThreadCoreImpl _parent_;
  struct test_runtime_TestCreateOVMThread* this$0;
};
struct ovm_core_TypeName_Primitive {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestClone_A {
  struct java_lang_Object _parent_;
  zint bo;
  jint i;
  jlong l;
  jdouble d;
  jfloat f;
  sint s;
  bint b;
};
struct ovm_util_List {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestSynchronization_SyncHelper {
  struct java_lang_Object _parent_;
  struct s3_services_monitors_BasicMonitorImpl* mon;
  struct test_runtime_TestSynchronization* this$0;
};
struct ovm_services_bytecode_SpecificationIR_ConcreteIntValue {
  struct ovm_services_bytecode_SpecificationIR_IntValue _parent_;
  struct java_lang_Integer* concreteValue;
};
struct test_runtime_TestSynchronization_1 {
  struct java_lang_Object _parent_;
  struct ovm_core_services_threads_OVMThread* me;
  struct test_runtime_TestSynchronization_SyncHelper* val$lock;
  struct ovm_core_services_threads_OVMThread* val$main;
  struct test_runtime_TestSynchronization* this$0;
};
struct ovm_core_domain_Field {
  struct java_lang_Object _parent_;
};
struct arr_ovm_services_bytecode_SpecificationIR_Value {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_SpecificationIR_Value* values[0];
};
struct ovm_services_bytecode_Instruction_LocalAccess {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_Instruction_Throwing {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_domain_Field {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_domain_Field* values[0];
};
struct ovm_core_domain_Oop {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Type_Class {
  struct java_lang_Object _parent_;
};
struct s3_core_repository_S3Repository {
  struct s3_core_repository_RepositoryImpl _parent_;
  struct arr_s3_core_repository_S3Bundle* otherBundles_;
  jint otherBundlesCount_;
  struct s3_core_repository_S3Bundle* systemBundle_;
  zint isReadonly;
};
struct ovm_core_domain_Blueprint_Factory {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Blueprint {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_ValueSource {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_domain_Type_Class {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_domain_Type_Class* values[0];
};
struct ovm_services_monitors_QueryableMonitor {
  struct java_lang_Object _parent_;
};
struct ovm_services_monitors_RecursiveMonitor {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestThreadScheduling_LogEntry {
  struct java_lang_Object _parent_;
  jint prio;
  jlong timestamp;
  struct test_runtime_TestThreadScheduling_LogEntry* next;
};
struct ovm_core_repository_RepositoryConstantPool_AccessException {
  struct ovm_util_OVMException _parent_;
};
struct test_runtime_TestThreadScheduling_1 {
  struct java_lang_Object _parent_;
  struct test_runtime_TestThreadScheduling* this$0;
};
struct ovm_core_repository_RepositoryConstantPool_Entry {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_repository_RepositoryConstantPool_Entry {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_repository_RepositoryConstantPool_Entry* values[0];
};
struct ovm_core_domain_Type {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Domain {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Type_Scalar {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_Type_Context {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_reader_ByteCodeConstants {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryBuilder {
  struct java_lang_Object _parent_;
};
struct ovm_core_domain_ReferenceVisitor {
  struct java_lang_Object _parent_;
};
struct arr_ovm_core_TypeName_Scalar {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_core_TypeName_Scalar* values[0];
};
struct ovm_core_services_threads_ThreadManager {
  struct java_lang_Object _parent_;
};
struct ovm_core_TypeName_Scalar {
  struct java_lang_Object _parent_;
};
struct test_runtime_TestBasicPthreads_1 {
  struct ovm_core_services_threads_OVMThreadCoreImpl _parent_;
  struct java_lang_String* name;
  struct test_runtime_TestBasicPthreads* this$0;
};
struct ovm_services_bytecode_SpecificationIR_CPIndexValue {
  struct ovm_services_bytecode_SpecificationIR_IntValue _parent_;
  bint expectedTag;
};
struct test_runtime_TestRealtimeThreadScheduling_LogEntry {
  struct java_lang_Object _parent_;
  jint prio;
  jlong timestamp;
  struct test_runtime_TestRealtimeThreadScheduling_LogEntry* next;
};
struct test_runtime_TestRealtimeThreadScheduling_2 {
  struct java_lang_Object _parent_;
  struct test_runtime_TestRealtimeThreadScheduling* this$0;
};
struct test_runtime_TestRealtimeThreadScheduling_1 {
  struct java_lang_Object _parent_;
  jint val$tempMinPrio;
  jint val$tempMaxPrio;
  struct test_runtime_TestRealtimeThreadScheduling* this$0;
};
struct ovm_services_bytecode_SpecificationIR_ArrayAccessExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_NonnulArrayRefValue* arr;
  struct ovm_services_bytecode_SpecificationIR_IntValue* index;
};
struct ovm_core_domain_Type_Compound {
  struct java_lang_Object _parent_;
};
struct s3_services_threads_BasicUserLevelThreadManagerImpl_1 {
  struct java_lang_Object _parent_;
  struct s3_services_threads_BasicUserLevelThreadManagerImpl* this$0;
};
struct ovm_services_bytecode_SpecificationIR_NonnulArrayRefValue {
  struct ovm_services_bytecode_SpecificationIR_NonnulRefValue _parent_;
};
struct ovm_services_bytecode_SpecificationIR_CallExp {
  struct java_lang_Object _parent_;
  struct java_lang_String* fname;
  struct arr_ovm_services_bytecode_SpecificationIR_Value* args;
};
struct ovm_core_repository_RepositoryAttribute_Synthetic {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryAttribute_LocalVariableTable {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryAttribute_ThirdParty {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_ArrayLengthExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_NonnulArrayRefValue* arr;
};
struct java_util_Vector {
  struct java_lang_Object _parent_;
};
struct arr_ovm_services_bytecode_SpecificationIR_LocalExp {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_SpecificationIR_LocalExp* values[0];
};
struct java_lang_OutOfMemoryError {
  struct java_lang_VirtualMachineError _parent_;
};
struct ovm_core_repository_RepositoryAttribute_SourceFile {
  struct java_lang_Object _parent_;
};
struct ovm_core_repository_RepositoryAttribute_InnerClasses {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_AssignmentExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_Value* dest;
  struct ovm_services_bytecode_SpecificationIR_Value* src;
};
struct ovm_util_ReadonlyViewException {
  struct ovm_util_OVMRuntimeException _parent_;
};
struct ovm_services_bytecode_SpecificationIR_NullRefValue {
  struct ovm_services_bytecode_SpecificationIR_RefValue _parent_;
  struct java_lang_String* concreteValue;
};
struct ovm_core_domain_LinkageException_UndefinedMember {
  struct ovm_core_domain_LinkageException _parent_;
  struct ovm_core_Selector* sel;
};
struct ovm_core_repository_RepositoryAttribute_Deprecated {
  struct java_lang_Object _parent_;
};
struct ovm_services_bytecode_SpecificationIR_LocalExp {
  struct java_lang_Object _parent_;
  struct ovm_services_bytecode_SpecificationIR_IntValue* number;
};
struct ovm_services_bytecode_Instruction_GETFIELD2_QUICK {
  struct ovm_services_bytecode_Instruction_FieldAccess_Quick _parent_;
};
struct arr_ovm_services_bytecode_SpecificationIR_FloatValue {
    struct java_lang_Object _parent_;
    int length;
    struct ovm_services_bytecode_SpecificationIR_FloatValue* values[0];
};
struct ovm_services_bytecode_Instruction_DLOAD_2 {
  struct ovm_services_bytecode_Instruction_ConcreteDLoad _parent_;
};
struct ovm_services_bytecode_SpecificationIR_PCValue {
  struct ovm_services_bytecode_SpecificationIR_IntValue _parent_;
  zint relative;
};
struct s3_core_repository_LISTRepositoryUnboundSelector_Field {
  struct ovm_core_OVMBase _parent_;
  struct arr_ovm_core_repository_RepositoryUnboundSelector_Field* elements_;
  jint elementsCount_;
};
struct arr_s3_core_repository_S3Descriptor {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_repository_S3Descriptor* values[0];
};
struct ovm_services_bytecode_Instruction_FADD {
  struct ovm_services_bytecode_Instruction_BinOpFloat _parent_;
};
struct ovm_services_bytecode_Instruction_Generic {
  struct java_lang_Object _parent_;
};
struct ovm_core_services_memory_VM_Address_BCsetword {
  struct ovm_core_services_memory_VM_Address_BC _parent_;
};
struct ovm_util_NumberRanges {
  struct java_lang_Object _parent_;
};
struct s3_Main {
  struct ovm_core_OVMBase _parent_;
};
struct arr_s3_core_domain_HTSelector_Field2Field_Binding {
    struct java_lang_Object _parent_;
    int length;
    struct s3_core_domain_HTSelector_Field2Field_Binding* values[0];
};
struct ovm_core_execution_Context_DefaultNativeContextFactory {
  struct ovm_core_OVMBase _parent_;
};
struct ovm_core_stitcher_ServiceFactory {
  struct java_lang_Object _parent_;
};
struct arr_test_runtime_TestSubtypetest_C {
    struct java_lang_Object _parent_;
    int length;
    struct test_runtime_TestSubtypetest_C* values[0];
};
struct s3_core_repository_HTS3Class_Iterator {
  struct java_lang_Object _parent_;
  jint position_;
  zint iteratingOverKeys_;
  struct s3_core_repository_HTS3Class_Binding* currentBinding_;
  zint reachEnd;
  struct s3_core_repository_HTS3Class* this$0;
};
struct VTBL_ovm_core_execution_CoreServicesAccess {
   struct java_lang_Object _parent_; 
    int length;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_equals;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_toString;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_finalize;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_hashCode;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_clone__;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_allocateArray;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_generateThrowable;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_timerInterruptHook;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_pollingEventHook;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_monitorExit;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_emptyCall;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_getAllocator;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_clone__Lovm_core_domain_Oop_2;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_getClassName;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_print;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_getStackTrace;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_allocateObject;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_processThrowable;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_boot;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_monitorEnter;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_identityHashCode;
   struct s3_core_repository_S3CodeFragment * Java_ovm_core_execution_CoreServicesAccess_allocateMultiArray;
 };
#define IMAGE_MAGIC 0x494e2086
 typedef struct {
	int OVM_MAGIC;
	int OVM_VERSION;
	int usedMemory;
	int baseAddress;
	void * mainObject;
	void * mainMethod;
	void * coreServicesAccess;
	void * bootContext;
	void * bootProcessor;
	void * bottomFrameCode;
	void * bottomFrameObject;
	void * repository;
	void * jitHeader;
	 char data[0];
} ImageFormat;
