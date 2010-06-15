package ovm.core.services.memory;

import ovm.services.bytecode.JVMConstants.InvokeSystemArguments;
import s3.util.PragmaTransformCallsiteIR;

/**
 * 
 * @author jv
 *
 * Provide atomic operations which could live in VM_Address, but don't.
 *
 * <p>This class provides methods for atomically updating memory locations.
 * These memory locations should be either int fields, or reference fields.
 * The resulting implementation uses native hardware instructions to
 * perform the atomic operations.
 * <p>Due to the lack of an extended-compare-and-swap, or 64-bit load-linked &
 * store-conditional, support on the PPC architecture, there are no 64-bit 
 * operations defined at this level.
 * 
 */
public final class AtomicOps {

    /**
     * Compare and swap style operation.
     * @param target
     * @param original
     * @param update
     * @return true if cas suceeded
     * @throws BCcas32
     */
    static public boolean attemptUpdate(
        VM_Address target,
        VM_Word original,
        VM_Word update)
        throws BCcas32 {

        return false;
    }

    /**
       * Compare and swap style operation.
       * @param target
       * @param original
       * @param update
       * @return true if cas suceeded
       * @throws BCcas32
       */
    static public boolean attemptUpdate(
        VM_Address target,
        int original,
        int update)
        throws BCcas32 {

        return false;
    }

//      /**
//         * Compare and swap style operation.
//         * @param target
//         * @param original
//         * @param update
//         * @return true if cas suceeded
//         * @throws BCcas64
//         */
//      static public boolean attemptUpdate(
//          VM_Address target,
//          long original,
//          long update)
//          throws BCcas64 {

//          return false;
//      }

    /**
     * Parent pragma.
      */
    static class BC extends PragmaTransformCallsiteIR {
        static {
            r(
                "cas32",
                new byte[] {
                    (byte) INVOKE_SYSTEM,
                    (byte) InvokeSystemArguments.CAS32,
                    0 });
//              r(
//                  "cas64",
//                  new byte[] {
//                      (byte) INVOKE_SYSTEM,
//                      (byte) InvokeSystemArguments.CAS64,
//                      0 });

        }

        /** OVM-specific: implementation detail: register substitute bytecode
          *  for a subpragma of this pragma.
          **/
        protected static void r(String name, Object bc) {
            // SYNCHRONIZE THIS STRING WITH CLASS/PACKAGE NAME CHANGES
            register("ovm.core.services.memory.AtomicOps$BC" + name, bc);
        }

    }

    /**
     * Pragma for 32 bit compare-and-swap.
     */
    static final class BCcas32 extends BC {
    }

    /**
     *  Pragma for 64 bit compare-and-swap.
     */
    static final class BCcas64 extends BC {
    }
}



