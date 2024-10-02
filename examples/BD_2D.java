import java.io.*;
import java.util.*;
import java.math.*;
public class BD_2D implements java.io.Serializable {
  public static final long serialVersionUID = 1L; 
  private BigDecimal[][] bd2D =
                     {{ new BigDecimal("123456789012345.4"), 
                        new BigDecimal("-56789012345678"),
                        new BigDecimal("901234567890123.2") },
                      { new BigDecimal("123456789012345.4"),
                        new BigDecimal("-56789012345678"),
                        new BigDecimal("901234567890123.2") } };
  
  public static void main(String... a) throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("serobj/bd_2D.txt")); 
    oos.writeObject(new BD_2D());
    oos.flush();
    oos.close();
  }
}
