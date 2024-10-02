import java.io.*;
import java.math.*;
import java.util.*;
public class BigMix implements java.io.Serializable {
  public static final long serialVersionUID = 1L; 
  private BigDecimal bdec;
  private long lv;
  private BigInteger bint;
  private ArrayList<Integer> alst;
  //
  public BigMix(String val1, String val2, int i1, int i2, long l) {
    bdec = new BigDecimal(val1);
    int a = val1.lastIndexOf(".")+1;
    if (a > 1) bdec.setScale(val1.length()-a);
    bint = new BigInteger(val2);
    alst = new ArrayList<>();
    alst.add(i1); alst.add(i2);
    lv = l;
  }
  //
  public String toString() {
    return "BigMix:"+bint.toString()+", long Value:"+lv;
  }
  //
  public static void main(String... a) throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("serobj/bigmix.txt")); 
    oos.writeObject(new BigMix("1234567890987654.321",
                               "9876543211234567890",
                               12345, 67890,
                               123456789L));
    oos.flush();
    oos.close();
    BigDecimal bd = new BigDecimal("1234567890987654.321");
    System.out.println("---->"+bd.toString()+
                       "\n---->"+(new BigInteger("9876543211234567890")).toString());
  }
}
