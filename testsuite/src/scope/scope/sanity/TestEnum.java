package scope.scope.sanity;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@SCJAllowed(value = LEVEL_2, members = true)
@Scope("A")
public class TestEnum {

    EnumType enumType;

    EnumType[] enumTypeArray;


    @Scope(IMMORTAL)
    @SCJAllowed(value = LEVEL_2, members = true)
    enum EnumType {
        REQUEST_READ, REQUEST_WRITE,
        REQUEST_WRITE_SOCKET, REQUEST_READ_SOCKET,
        CLOSE_SOCKET, READ_COMPLETED, WRITE_COMPLETED,
        SOCKET_COMPLETED,
    };

    public TestEnum(int num_buffers) {
        enumTypeArray = new EnumType[num_buffers];
        enumTypeArray[0] = EnumType.REQUEST_READ;
        enumType = EnumType.REQUEST_READ;

        enumType = this.enumType;

        switch (enumType) {
        case REQUEST_READ:
            break;
        default:
            break;
        }
    }

    @Scope(IMMORTAL)
    @DefineScope(name = "A", parent = IMMORTAL)
    static abstract class X extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public X() {
            super(null, null);
        }
    }


    @Scope("A")
    @SCJAllowed(value = LEVEL_2, members = true)
    class TT {

        TestEnum ee;

        void method() {
           EnumType tt = ee.enumType;
           Class cc = TestEnum.class;
        }
    }
}

