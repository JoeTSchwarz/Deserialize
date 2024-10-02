import java.io.*;
public class Sarray implements java.io.Serializable {
  public static final long serialVersionUID = 1L; 
                    
  private String s2d[][] = {{ "Joe1", "Kriftel1", "Retiree1", "J1.jpg",
                              "Joe2", "Kriftel2", "Retiree2", "J2.jpg",
                              "Joe3", "Kriftel3", "Retiree3", "J3.jpg" },
                            { "Joe4", "Kriftel4", "Retiree1", "J4.jpg",
                              "Joe5", "Kriftel5", "Retiree2", "J5.jpg",
                              "Joe6", "Kriftel6", "Retiree3", "J6.jpg" }
                           };
                          
  public static void main(String... a) throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("serobj/Sarray.txt")); 
    oos.writeObject(new Sarray());
    oos.flush();
    oos.close();
  }
}
