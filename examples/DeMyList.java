import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
public class DeMyList {
  ArrayList<String> list;
  public static void main(String... a) throws Exception {
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(a[0]));
    MyList ml = (MyList) ois.readObject( );
    ois.close();
    ml.print();
  }
}
