package test.common;
import test.common.TestBase;

public class TestShifts extends TestBase {
  int id(int number) { return number; }
  
  public void run() {
    setModule("masking of shift count");
    check_condition(-1  << id(33) == -2, "masking of ishl count");
    check_condition( 3  >> id(33) ==  1, "masking of ishr count");
    check_condition(-1 >>> id(33) == 0x7fffffff, "masking of iushr count");
    check_condition(-1L  << id(65) == -2L, "masking of lshl count");
    check_condition( 3L  >> id(65) ==  1L, "masking of lshr count");
    check_condition(-1L >>> id(65) == 0x7fffffffffffffffL, "masking of lushr count");
  }

  public TestShifts() { super("Shifts"); }
}
