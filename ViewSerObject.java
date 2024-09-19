import java.util.*;
import java.io.*;
import java.math.BigInteger;
/**
@author Joe T. Schwarz
*/
public class ViewSerObject {
  public static void main(String... a) throws Exception {
    if (a.length == 0) {
      System.out.println("Usage: java ViewSerObject serializedObjectFile");
      System.exit(0);
    }
    String obj = a[0].replace("/", File.separator);
    if (obj.indexOf(File.separator) < 0) obj = "examples"+File.separator+"serobj"+File.separator+a[0];
    if (a.length > 1) setOutput(new FileOutputStream(a[1]));
    new ViewSerObject(obj);
  }
  /**
  Constructor
  @param sfName   String, serialized file name
  */
  public ViewSerObject(String sfName) throws Exception {
    show(new SerObjectView(sfName));
  }
  /**
  setOutput (default: System.out)
  @param out OutputStream
  */
  public static void setOutput(OutputStream out) {
    output = out;
  }
  //
  private static OutputStream output = System.out;
  //  
  @SuppressWarnings("unchecked")
  private void show(SerObjectView ov) throws Exception {
    output.write(String.format("Class Name: %s\nSerID: %d\nNumber of Fields: %s\n",
                 ov.getClassName(),ov.getSerialID(), ov.getSize()).getBytes());
    ArrayList<String> fNames = ov.getFieldNames();
    for (String f:fNames) {
      String type = ov.getFieldType(f);
      int a = type.indexOf("][");
      if ("Object".equals(type)) {
        output.write("Embedded ".getBytes());
        show((SerObjectView) ov.getFieldValue(f));
      } else if ("BigInteger".equals(type)) {
        output.write(String.format("Field: %s, type: %s, value/Reference: %s\n", f, type, 
                                  ((BigInteger)ov.getFieldValue(f)).toString()).getBytes());
      } else output.write(String.format("Field: %s, type: %s, value/Reference: %s\n",
                          f, type, ov.getFieldValue(f)).getBytes());

      if (type.indexOf("[") > 0) {
        Object obj = ov.getFieldValue(f);
        if (type.indexOf("int") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              int[][] aa = (int[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    int[%d][%d]: %d\n", i, j, aa[i][j]).getBytes());
            } else {
              int[][][] aa = (int[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    int[%d][%d][%d]: %d\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            int[] aa = (int[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              output.write(String.format("    int[%d]: %d\n", i, aa[i]).getBytes());
          }
        } else if (type.indexOf("long") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              long[][] aa = (long[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    long[%d][%d]: %d\n", i, j, aa[i][j]).getBytes());
            } else {
              long[][][] aa = (long[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    long[%d][%d][%d]: %d\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            long[] aa = (long[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    long[%d]: %d\n", i, aa[i]).getBytes());
          }
        } else if (type.indexOf("short") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              short[][] aa = (short[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    int[%d][%d]: %d\n", i, j, aa[i][j]).getBytes());
            } else {
              short[][][] aa = (short[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    int[%d][%d][%d]: %d\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            short[] aa = (short[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    short[%d]: %d\n", i, aa[i]).getBytes());
          }
        } else if (type.indexOf("dou") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              double[][] aa = (double[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    double[%d][%d]: %.2f\n", i, j, aa[i][j]).getBytes());
            } else {
              double[][][] aa = (double[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    double[%d][%d][%d]: %.2f\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            double[] aa = (double[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    double[%d]: %.2f\n", i, aa[i]).getBytes());
          }
        } else if (type.indexOf("flo") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              float[][] aa = (float[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    int[%d][%d]: %.2f\n", i, j, aa[i][j]).getBytes());
            } else {
              float[][][] aa = (float[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    float[%d][%d][%d]: %.2f\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            float[] aa = (float[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    float[%d]: %.2f\n", i, aa[i]).getBytes());
          }
        } else if (type.indexOf("String") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              String[][] aa = (String[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    String[%d][%d]: %s\n", i, j, aa[i][j]).getBytes());
            } else {
              String[][][] aa = (String[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    String[%d][%d][%d]: %s\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            String[] aa = (String[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              output.write(String.format("    String[%d]: %s\n", i, aa[i]).getBytes());
          }
        } else if (type.indexOf("bool") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              boolean[][] aa = (boolean[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    boolean[%d][%d]: %b\n", i, j, aa[i][j]).getBytes());
            } else {
              boolean[][][] aa = (boolean[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    boolean[%d][%d][%d]: %b\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            boolean[] aa = (boolean[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              output.write(String.format(" boolean["+i+"]: %b\n",aa[i]).getBytes());
          }
        } else if (type.indexOf("char") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              char[][] aa = (char[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    char[%d][%d]: %s\n", i, j, ""+aa[i][j]).getBytes());
            } else {
              char[][][] aa = (char[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    char[%d][%d][%d]: %d\n", i, j, l, ""+aa[i][j][l]).getBytes());
            }
          } else {
            char[] aa = (char[]) obj;
            for (int i = 0; i < aa.length; ++i) output.write(String.format("    char[%d]: %s\n", i, ""+aa[i]).getBytes());
          }
        } else if (type.indexOf("byt") >= 0) {
           if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              byte[][] aa = (byte[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  output.write(String.format("    byte[%d][%d]: %s (x%02X)\n", i, j, ""+(char)aa[i][j],aa[i][j]).getBytes());
            } else {
              byte[][][] aa = (byte[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    byte[%d][%d][%d]: %s (x%202C)\n", i, j, l, 
                                  ""+(char)aa[i][j][l], aa[i][j][l]).getBytes());
            }
          } else {
            byte[] aa = (byte[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    byte[%d]: %s (x%2X)\n", i,
                                         ""+(char)aa[i], aa[i]).getBytes());
          }
        } else { // POJO
          output.write(String.format("Embedded %s : %s\n", f, type).getBytes());
          String pojo = type.substring(0, type.indexOf("["));
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              List[][] aa = (ArrayList[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  output.write(String.format("    %s[%d][%d]: %s\n", pojo, i, j, ""+aa[i][j]).getBytes());
            } else {
              List[][][] aa = (ArrayList[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    %s[%d][%d][%d]: %s\n", pojo, i, j, l,""+aa[i][j][l]).getBytes());
            }
          } else {
            List[] aa = (ArrayList[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              output.write(String.format("    %s[%d]: %s\n", pojo, i, ""+aa[i], aa[i]).getBytes());
          }
        }
      }                                            
    }
  }
}
