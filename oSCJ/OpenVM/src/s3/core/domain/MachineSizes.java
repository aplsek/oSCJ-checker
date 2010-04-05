package s3.core.domain;
/**
 * @author Krzysztof Palacz
 **/
public interface MachineSizes {
    public final static int BYTES_IN_WORD = 4; // VM_Word.widthInBytes() ?
    public final static int BYTES_IN_DOUBLE_WORD = BYTES_IN_WORD*2;
    public final static int BITS_IN_WORD = 32;
    public final static int BITS_IN_DOUBLE_DWORD = BITS_IN_WORD*2;
    public final static int BYTES_IN_ADDRESS = BYTES_IN_WORD; // VM_Address.w.i.b?
}
