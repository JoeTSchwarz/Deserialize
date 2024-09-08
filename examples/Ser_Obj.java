
public class Ser_Obj implements java.io.Serializable {
  public static final long serialVersionUID = 1L;  
  public int i1 = 1;
  public double d2 = 2d;
  public long l9 = 9;
  public float f3 = 3f;
  public short s4 = 4;
  public char c5 = 'J';
  public byte b6 = (byte)'E';
  public boolean boo = true;
  public String T7="XinChao";
  public People p = new People( "Joe", 73, "Kriftel", "Retiree", 18000, "J.jpg");
  public int[] I7 = { 5, 6 };
  public double[] D8 = {7, 8};
  public byte[] bb = {(byte)'j', (byte)'o', (byte)'e'};
  public String[] TT = { "hello", "world"};
  private People pep[][] = {{ new People( "Joe1", 73, "Kriftel1", "Retiree1", 18000, "J1.jpg"),
                              new People( "Joe2", 73, "Kriftel2", "Retiree2", 18000, "J2.jpg"),
                              new People( "Joe3", 73, "Kriftel3", "Retiree3", 18000, "J3.jpg") },
                            { new People( "Joe4", 73, "Kriftel4", "Retiree1", 18000, "J4.jpg"),
                              new People( "Joe5", 73, "Kriftel5", "Retiree2", 18000, "J5.jpg"),
                              new People( "Joe6", 73, "Kriftel6", "Retiree3", 18000, "J6.jpg") }                         };
}
