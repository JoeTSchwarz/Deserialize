import java.io.*;
import java.util.*;
import java.math.*;
public class BI2Dm implements java.io.Serializable {
  public static final long serialVersionUID = 1L; 
  private BigInteger[][] bi2D =
                     {{ new BigInteger("-1234"), new BigInteger("5678"), new BigInteger("-9012") },
                      { new BigInteger("-3456"), new BigInteger("7890"), new BigInteger("-1234") } };
  
  public static void main(String... a) throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("serobj/bi2Dm.txt")); 
    oos.writeObject(new BI2Dm());
    oos.flush();
    oos.close();
  }
}
