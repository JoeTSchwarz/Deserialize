import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
public class MyList implements java.io.Serializable {
  public static final long serialVersionUID = 1L; 
  ArrayList<String> list;
  public MyList(String[] mylist) {
    list = new ArrayList<>(mylist.length);
    for (String s:mylist) list.add(s);
  }
  public void print() {
    System.out.println(list);
  }
  //
  public static void main(String... a) throws Exception {
    MyList ml = new MyList(a.length == 0? new String[] { "Hello", "World" }:a);
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("serobj/MyList.txt"));
    oos.writeObject(ml);
    oos.flush();
    oos.close();
  }
}
