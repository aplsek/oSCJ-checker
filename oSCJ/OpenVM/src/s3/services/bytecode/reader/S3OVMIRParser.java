/** 
 **/
package s3.services.bytecode.reader;

import ovm.core.domain.LinkageException;
import ovm.core.repository.ConstantPool;
import ovm.services.bytecode.reader.ByteCodeConstants;
import ovm.services.bytecode.reader.Parser;

/** 
 * The S3OVMIRParser parses .ovm files, an extended form of Java .class files.
 *
 * Is this code still valid?  There are plenty of new constant pool
 * tags, but most (if not all) of them are for resolved entries.
 * -- JB 16 Feb. 2005
 * 
 * @author Christian Grothoff
 **/
public class S3OVMIRParser extends S3Parser implements ByteCodeConstants {

    public S3OVMIRParser() {
        super();
    }

    protected void unexpectedTag(byte tag, int i, int size)
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        switch (tag) {
            case CONSTANT_SharedState :
                stream_.advance(2);
                break;
            case CONSTANT_Binder :
                stream_.advance(2);
                int length = stream_.getChar();
                stream_.advance(length * 2);
                break;
            default :
                throw new ClassParsingException(
                    "Invalid CP tag: " + tag + " at " + i + "/" + size);
        }
    }

    protected void cpParsePhase2(byte[] tags, int[] poses)
        throws LinkageException.ClassFormat, ConstantPool.AccessException {
        int size = tags.length;
        for (int i = 1; i < size; i++) {
            switch (tags[i]) {
                case CONSTANT_SharedState :
                    stream_.position(poses[i]);
                    int cpName = stream_.getChar();
                    int utf_ix = cp_.getUtf8IndexAt(cpName);
                    cpBuilder_.makeUnresolvedSharedStateAt(utf_ix, i);
                    break;
                case CONSTANT_Binder :
                    stream_.position(poses[i]);
                    int cpName2 = stream_.getChar();
                    int utf_ix2 = cp_.getUtf8IndexAt(cpName2);
                    int length = stream_.getChar();
                    int[] utfs = new int[length];
                    for (int j = 0; j < length; j++)
                        utfs[j] = cp_.getUtf8IndexAt(stream_.getChar());
                    cpBuilder_.makeUnresolvedBinderAt(utf_ix2, utfs, i);
                    break;
            }
        }
        super.cpParsePhase2(tags, poses);
    }

    /**
     * @author Jan Vitek
     **/
    public static class Factory
        implements ovm.services.bytecode.reader.Parser.Factory {

        public Parser makeParser() {
              return new S3OVMIRParser();
        }
    } // End of S3OVMIRParser.Factory

} // End of S3OVMIRParser
