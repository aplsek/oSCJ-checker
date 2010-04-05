package test;

public class TestShifts extends TestBase {
  int id(int number) { return number; }
  
  public void run() {
    setModule("masking of shift count");
    COREassert(-1  << id(33) == -2, "masking of ishl count");
    COREassert( 3  >> id(33) ==  1, "masking of ishr count");
    COREassert(-1 >>> id(33) == 0x7fffffff, "masking of iushr count");
    COREassert(-1L  << id(65) == -2L, "masking of lshl count");
    COREassert( 3L  >> id(65) ==  1L, "masking of lshr count");
    COREassert(-1L >>> id(65) == 0x7fffffffffffffffL, "masking of lushr count");
  }

  public TestShifts(Harness domain) { super("Shifts", domain); }
}
