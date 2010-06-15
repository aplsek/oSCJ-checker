package ovm.core.repository;

import ovm.core.repository.TypeName;
import s3.core.S3Base;

/**
 * S3ExceptionHandler refers to an entry in an exception table with
 * starting program counter, ending program counter, catch typename,
 * and handler program counter.
 * 
 * @author Jan Vitek
 * @author Christian Grothoff
 **/
public class ExceptionHandler extends S3Base {

    static final ExceptionHandler[] EMPTY_ARRAY = new ExceptionHandler[0];
    private final TypeName.Scalar catchTypeName_;
    private final int endPC_;
    private final int handlerPC_;
    private final int startPC_;

    /**
     * Create a S3Exception.
     * @param spc   starting pc.
     * @param epc   ending pc.
     * @param hpc   handler's pc.
     * @param cName repository typename for the handler type.
     **/
    public ExceptionHandler(
        char spc,
        char epc,
        char hpc,
        TypeName.Scalar cName) {
        this.startPC_ = spc;
        this.endPC_ = epc;
        this.handlerPC_ = hpc;
        this.catchTypeName_ = cName;
    }
    public ExceptionHandler(
        int spc,
        int epc,
        int hpc,
        TypeName.Scalar cName) {
        this.startPC_ = spc;
        this.endPC_ = epc;
        this.handlerPC_ = hpc;
        this.catchTypeName_ = cName;
    }


    public String toString() {
        return (
            "(["
                + getStartPC()
                + ", "
                + getEndPC()
                + "), "
                + getCatchTypeName()
                + " => "
                + getHandlerPC()
                + ")");
    }

    /**
     * A visitor pattern accept method.
     * @param v the visitor.
     */
    public void accept(RepositoryProcessor v) {
        v.visitException(this);
    }
    /**
     * Return the repository typename id for the handler type.
     **/
    public TypeName.Scalar getCatchTypeName() {
        return catchTypeName_;
    }
    /**
     * Return the ending pc.
     **/
    public int getEndPC() {
        return endPC_;
    }

    public boolean matchesAtPC(int pc) {
        return ((pc >= startPC_) && (pc < endPC_));
    }

    /**
     * Return the handler pc.
     **/
    public int getHandlerPC() {
        return handlerPC_;
    }

    /**
     * Return the starting pc.
     **/
    public int getStartPC() {
        return startPC_;
    }

} // end of ExceptionHandler
