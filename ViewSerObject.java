import java.util.*;
import java.io.*;
import java.math.*;
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
    show(new ODBObjectView(sfName));
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
  private void show(ODBObjectView ov) throws Exception {
    output.write(String.format("Class Name: %s\nSerID: %d\nNumber of Fields: %s\n",
                 ov.getClassName(),ov.getSerialID(), ov.getSize()).getBytes());
    ArrayList<String> fNames = ov.getFieldNames();
    for (String f:fNames) {
      String type = ov.getFieldType(f);
      int a = type.indexOf("][");
      if ("Object".equals(type)) {
        ODBObjectView o = (ODBObjectView) ov.getFieldValue(f);
        output.write("Embedded ".getBytes());
        show(o);
      } else if ("BigInteger".equals(type)) {
        BigInteger bI = (BigInteger)ov.getFieldValue(f);        
        output.write(String.format("Field: %s, type: %s, value/Reference: %s\n", f, type, 
                                  bI.toString()).getBytes());
      } else if ("BigDecimal".equals(type)) {
        BigDecimal bd = (BigDecimal)ov.getFieldValue(f);        
        output.write(String.format("Field: %s, type: %s, value/Reference: %s\n", f, type, 
                                  bd.toString()).getBytes());
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
        } else if (type.startsWith("Integer")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Integer[][] aa = (Integer[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    Integer[%d][%d]: %d\n", i, j, aa[i][j]).getBytes());
            } else {
              Integer[][][] aa = (Integer[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    Integer[%d][%d][%d]: %d\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            Integer[] aa = (Integer[]) obj;
            for (int i = 0; i < aa.length; ++i) 
              output.write(String.format("    Integer[%d]: %d\n", i, aa[i]).getBytes());
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
        } else if (type.startsWith("Long")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Long[][] aa = (Long[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    Long[%d][%d]: %d\n", i, j, aa[i][j]).getBytes());
            } else {
              Long[][][] aa = (Long[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    Long[%d][%d][%d]: %d\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            Long[] aa = (Long[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    Long[%d]: %d\n", i, aa[i]).getBytes());
          }
        } else if (type.indexOf("short") >= 0) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              short[][] aa = (short[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    short[%d][%d]: %d\n", i, j, aa[i][j]).getBytes());
            } else {
              short[][][] aa = (short[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    short[%d][%d][%d]: %d\n", i, j, l, aa[i][j][l]).getBytes());
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
        } else if (type.startsWith("Double")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Double[][] aa = (Double[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    Double[%d][%d]: %.2f\n", i, j, aa[i][j]).getBytes());
            } else {
              Double[][][] aa = (Double[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    Double[%d][%d][%d]: %.2f\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            Double[] aa = (Double[]) obj;
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
                output.write(String.format("    float[%d][%d]: %.2f\n", i, j, aa[i][j]).getBytes());
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
        } else if (type.startsWith("Float")) {
          if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              Float[][] aa = (Float[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                output.write(String.format("    Float[%d][%d]: %.2f\n", i, j, aa[i][j]).getBytes());
            } else {
              Float[][][] aa = (Float[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    Float[%d][%d][%d]: %.2f\n", i, j, l, aa[i][j][l]).getBytes());
            }
          } else {
            Float[] aa = (Float[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    Float[%d]: %.2f\n", i, aa[i]).getBytes());
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
        } else if (type.startsWith("BigI")) {
           if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              BigInteger[][] aa = (BigInteger[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  output.write(String.format("    BigInteger[%d][%d]: %s\n", i, j, aa[i][j]).getBytes());
            } else {
              BigInteger[][][] aa = (BigInteger[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    BigInteger[%d][%d][%d]: %s\n", i, j, l, 
                                           aa[i][j][l]).getBytes());
            }
          } else {
            BigInteger[] aa = (BigInteger[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    BigInteger[%d]: %s\n", i, aa[i]).getBytes());
          }
        } else if (type.startsWith("BigD")) {
           if (a > 0) {
            a = type.indexOf("][", a+2);
            if (a < 0) {
              BigDecimal[][] aa = (BigDecimal[][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  output.write(String.format("    BigDecimal[%d][%d]: %s\n", i, j, aa[i][j]).getBytes());
            } else {
              BigDecimal[][][] aa = (BigDecimal[][][]) obj;
              for (int i = 0; i < aa.length; ++i)
                for (int j = 0; j < aa[i].length; ++j)
                  for (int l = 0; l < aa[i][j].length; ++l)
                output.write(String.format("    BigDecimal[%d][%d][%d]: %s\n", i, j, l, 
                                           aa[i][j][l]).getBytes());
            }
          } else {
            BigDecimal[] aa = (BigDecimal[]) obj;
            for (int i = 0; i < aa.length; ++i)
              output.write(String.format("    BigDecimal[%d]: %s\n", i, aa[i]).getBytes());
          }
        } else { // POJO          
          output.write(String.format("Embedded %s : array %s is unsupported.\n", f, type).getBytes());
        }
      }                                            
    }
  }
}
