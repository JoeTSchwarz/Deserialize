import java.io.*;
import java.util.*;

public class MixObj implements java.io.Serializable {
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
 private String s2d[][] = {{ "Joe1", "Kriftel1", "Retiree1", "J1.jpg",
                              "Joe2", "Kriftel2", "Retiree2", "J2.jpg",
                              "Joe3", "Kriftel3", "Retiree3", "J3.jpg" },
                            { "Joe4", "Kriftel4", "Retiree1", "J4.jpg",
                              "Joe5", "Kriftel5", "Retiree2", "J5.jpg",
                              "Joe6", "Kriftel6", "Retiree3", "J6.jpg" }
                           };
  public static void main(String... a) throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("serobj/MixObj.txt")); 
    oos.writeObject(new MixObj());
    oos.flush();
    oos.close();
  }
}
