import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.charset.*;
import java.nio.file.Files;
/**
Serialized Object Viewer without knowing/having its binary byte-codes class
<br>Note:
<br>- Array of Java APIs are NOT supported (except String)
<br>- Embedded Java APIs (e.g. HashMap, Stack, etc.) are NOT supported -except: String, ArrayList
<br>- Arrays of more than 3 dimensions are NOT supported.
@author Joe T. Schwarz
*/
public class SerObjectView {
  /**
  Constructor.
  */
  public SerObjectView( ) { }
  /**
  Constructor
  @param fileName  String, serialized object file
  @exception Exception thrown by JAVA (e.g. IOException, etc.)
  */
  public SerObjectView(String fileName) throws Exception {
    try {
      view(Files.readAllBytes((new File(fileName)).toPath()));
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new Exception(fileName+" is not an object file");
    }
  }
  /**
  Constructor
  @param bb  byte array of serialized object
  @exception Exception thrown by JAVA (e.g. IOException, etc.)
  */
   public SerObjectView(byte[] bb) throws Exception {
   view(bb);
  }
  /**
  view the content of a serialized object
  @param bb  byte array of serialized object 
  @exception Exception thrown by JAVA (e.g. IOException, etc.)
  */
  public void view(byte[] bb) throws Exception {
    // Java Serialized Object Signature
    byte[] ID = { (byte)0xAC, (byte)0xED, (byte)0x00, (byte)0x05, (byte)0x73, (byte)0x72 };  
    for (p = 0, ref = 0; p < ID.length; ++p) 
    if (bb[p] != ID[p]) throw new Exception("Byte array is not a serialized object");
    view(bb, p, false, false);
  }
  /**
  setCharset
  @param charset Charset, default: StandardCharsets.US_ASCII
  */
  public void setCharset(Charset charset) {
    this.charset = charset;
  }
  /**
  getSerialID 
  @return long SerialID of this object
  */
  public long getSerialID() {
    return serID;
  }
  /**
  getSize returns the number of fields
  @return int number of fields
  */
  public int getSize() {
    return nFields;
  }
  /**
  getClassName
  @return String Object class name
  */
  public String getClassName() {
    return clsName;
  }
  /**
  getFieldNames
  @return ArrayList of String containing all field names
  */
  public ArrayList<String> getFieldNames() {
    return new ArrayList<>(fNames);
  }
  /**
  getFieldType returns the field type 
  @param fName String, field name
  @return String I for Integer, D for Double, S for Short, etc. NULL if unknown
  */
  public String getFieldType(String fName) {
    return tFields.get(fName);
  }
  /**
  getFieldValue returns the content of the field with the given field name
  @param fName String, field name
  @return Object must be cast to I, D, String, etc.
  */
  public Object getFieldValue(String fName) {
    return vFields.get(fName);
  }
  // private constructor
  private SerObjectView(byte[] bb, int q, int ref, boolean array) throws Exception {
    this.ref = ref;
    view(bb, q, array, true);
  }
  //
  private int getIndex() {
    return p;
  }
  //
  private void view(byte[] bb, int q, boolean array, boolean embedded) throws Exception {   
    this.p  = q;
    this.bb = bb;
    le = getShort();
    clsName = new String(bb, p, le, charset);
    p += le;
    //
    if (bb[p] > (byte)0) {
      while (true) {
        if (bb[p] == (byte)0x78) {
          if (bb[p+1] == (byte)0x70) break;
          else if (bb[p+1] == (byte)0x72) {
            p += 2;
            le = getShort();
            clsName =  new String(bb, p, le, charset);
          } else ++p;
        } else ++p;
      } 
      loadAPI(clsName, bb, p+2); // over 0x7870
      nFields = 0;
      return;
    } else {
      serID = getLong();
      if (bb[p++] != (byte)0x02) throw new Exception("Object has NO serializable field");
      nFields = getShort();
      for (int i = 0; i < nFields && p < bb.length; ++i) {
        String type = ""+(char)bb[p++];
        le = getShort();
        String fName =  new String(bb, p, le, charset); // Field name
        fNames.add(fName);
        //
        p += le;
        if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) break;
        if (bb[p] == (byte)0x74) {
          ++p;
          le = getShort();
          String tmp =  new String(bb, p, le, charset).replace(";", "");
          int e = tmp.lastIndexOf("/");
          if (e > 0) type = tmp.substring(e+1, tmp.length()-1); // java object
          else if (bb[p] == (byte)'[') {
            if (tmp.lastIndexOf("[") > 2) throw new Exception(tmp+" has more than 3 dimensions");
            type = tmp; // array
          } else type = "Object"; // customer Object
          p += le;
        } else if (bb[p] == (byte)0x71 && bb[p+1] == (byte)0x00 && bb[p+2] == (byte)0x7E) {
          p += 3;
          ref = getShort();
          type = "String";
        }
        tFields.put(fName, type);
        ptFields.put(fName, type);
      }
    }
    //
    if (!array) while (p < bb.length) {
      if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) {
        p += 2;
        if (p < bb.length) {
          getValues(false);
          if (embedded) return;
        }
      } else ++p;
    }
  }
  //
  @SuppressWarnings("unchecked")
  private void getValues(boolean array) throws Exception {
    int mx = fNames.size();
    for (int i = 0; i < mx && p < bb.length; ++i) {
      String n = fNames.get(i);
      String t = tFields.get(n);
      if ((p+2) < bb.length && // check for Reference
         (bb[p] == (byte)0x71 && bb[p+1] == (byte)0x00 && bb[p+2] == (byte)0x7E ||
          bb[p] == (byte)0x73 && bb[p+1] == (byte)0x71 && bb[p+2] == (byte)0x00)) {
        p += bb[p] == (byte)0x71? 3:4;
        int x = getShort()+cnt;
        if (nRef.contains(x)) {
          if (array) pojo.add(vRef.get(x));
          else vFields.put(n, vRef.get(x));
          continue;
        }
      }
      if ("I".equals(t)) { // int
        int I = getInt();
        if (array) pojo.add(I);
        else {
          vFields.put(n, I);
          tFields.replace(n, "int");
        }
        nRef.add(ref);
        vRef.put(ref++, I);
      } else if ("D".equals(t)) { // double
        double d = ByteBuffer.wrap(bb, p, 8).getDouble();
        if (array) pojo.add(d);
        else {
          vFields.put(n, d);
          tFields.replace(n, "double");
        }
        p += 8;
        nRef.add(ref);
        vRef.put(ref++, d);
      } else if ("J".equals(t)) { // long
        long L = getLong();
        if (array) pojo.add(L);
        else {
          vFields.put(n, L);
          tFields.replace(n, "long");
        }
        nRef.add(ref);
        vRef.put(ref++, L);
      } else if ("F".equals(t)) { // float
        float f = ByteBuffer.wrap(bb, p, 4).getFloat();
        if (array) pojo.add(f);
        else {
          vFields.put(n, f);
          tFields.replace(n, "float");
        }
        p += 4;
        nRef.add(ref);
        vRef.put(ref++, f);
      } else if ("S".equals(t)) { // short
        short s = getShort();
        if (array) pojo.add(s);
        else {
          vFields.put(n, s);
          tFields.replace(n, "short");
        }
        nRef.add(ref);
        vRef.put(ref++, s);
      } else if (bb[p] == (byte)0x74) { // String
        ++p;
        le = getShort();
        String val = new String(bb, p, le, charset);
        if (array) pojo.add(val);
        else {
          vFields.put(n, val);
          tFields.replace(n, "String");
        }
        p += le;
        nRef.add(ref);
        vRef.put(ref++, val);
      } else if ("B".equals(t)) { // byte
        if (array) pojo.add(bb[p]);
        else {
          tFields.replace(n, "byte");
          vFields.put(n, bb[p]);
        }
        nRef.add(ref);
        vRef.put(ref++, bb[p++]);
      } else if ("Z".equals(t)) { // boolean
        if (array) pojo.add(bb[p] == (byte)1);
        else {
          tFields.replace(n, "boolean");
          vFields.put(n, bb[p] == (byte)1);
        }
        nRef.add(ref);
        vRef.put(ref++, bb[p++] == (byte)1);
      } else if ("C".equals(t)) { // char
        char C = ByteBuffer.wrap(bb, p, 2).getChar();
        if (array) pojo.add(C);
        else {
          tFields.replace(n, "char");
          vFields.put(n, C);
        }
        p += 2;
        nRef.add(ref);
        vRef.put(ref++, C);
      } else if (bb[p] == (byte)0x73 && bb[p+1] == (byte)0x72) { // nested Object
        SerObjectView ov = new SerObjectView(bb, p+2, ref, array);
        if (ov.getSize() > 0) {
          String cls = ov.getClassName();
          pFields.put(cls, ov.getOTFields());         
          pNames.put(cls, ov.getFieldNames());
          tFields.put(n, "Object");
          vFields.put(n, ov);
        } else {
          tFields.put(n, ov.getClassName());
          vFields.put(n, ov.getObject());
        }
        p = ov.getIndex();
      } else if (bb[p] == (byte)0x75 && bb[p+1] == (byte)0x72) {
        String at = getArrayType();
        p = getDimensions(); // getDimensions to data
        if (at.indexOf("[I") >= 0) { // int
          if ("[I".equals(at)) {
            int A[] = new int[le];
            for (int a = 0; a < le; ++a) A[a] = getInt();
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "int["+le+"]");
              vFields.put(n, A);  
            }
          } else if ("[[I".equals(at)) {
            int A[][] = new int[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) A[a][b] = getInt();
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "int["+dim1+"]["+le+"]");
              vFields.put(n, A); 
            }                
          } else {
            int A[][][] = new int[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) A[a][b][c] = getInt();
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "int["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }                
          }
        } else if (at.indexOf("[J")>= 0) { // long
          if ("[J".equals(at)) {
            long A[] = new long[le];
            for (int a = 0; a < le; ++a) A[a] = getLong();
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "long["+le+"]");
              vFields.put(n, A); 
            }
          } else if ("[[J".equals(at)) {
            long A[][] = new long[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) A[a][b] = getLong();
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "long["+dim1+"]["+le+"]");
              vFields.put(n, A);
            }
          } else {
            long A[][][] = new long[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) A[a][b][c] = getLong();
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "long["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }
          }
        } else if (at.indexOf("[D") >= 0) { // double
          if ("[D".equals(at)) {
            double A[] = new double[le];
            for (int a = 0; a < le; ++a) {
              A[a] = ByteBuffer.wrap(bb, p, 8).getDouble();
              p += 8;
            }
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "double["+le+"]");
              vFields.put(n, A);
            }                
          } else if ("[[D".equals(at)) {
            double A[][] = new double[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                A[a][b] = ByteBuffer.wrap(bb, p, 8).getDouble();
                p += 8;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "double["+dim1+"]["+le+"]");
              vFields.put(n, A);
            }                
          } else {
            double A[][][] = new double[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  A[a][b][c] = ByteBuffer.wrap(bb, p, 8).getDouble();
                  p += 8;
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "double["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }                
          }
        } else if (at.indexOf("[St") >= 0) { // String
          if ("[String".equals(at)) {
            String A[] = new String[le];
            for (int a = 0; a < le; ++a) {
              ++p; // ignore 0x74
              int l = getShort();
              A[a] =  new String(bb, p, l, charset);
              p += l;
            }
            if (array) pojo.add(A);
            else {
              vFields.put(n, A); 
              tFields.replace(n, "String["+le+"]");
            }
          } else if ("[[String".equals(at)) {
            String A[][] = new String[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                ++p; // ignore 0x74
                int l = getShort();
                A[a][b] =  new String(bb, p, l, charset);
                p += l;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "String["+dim1+"]["+le+"]");
              vFields.put(n, A); 
            }                
          } else {
            String A[][][] = new String[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  ++p; // ignore 0x74
                  int l = getShort();
                  A[a][b][c] =  new String(bb, p, l, charset);
                  p += l;
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "double["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }
          }
        } else if (at.indexOf("[S") >= 0) { // short  
          if ("[S".equals(at)) {
            short A[] = new short[le];
            for (int a = 0; a < le; ++a) A[a] = getShort();
            tFields.replace(n, "short["+le+"]");
            vFields.put(n, A);  
          } else if ("[[S".equals(at)) {
            short A[][] = new short[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) A[a][b] = getShort();
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "short["+dim1+"]["+le+"]");
              vFields.put(n, A);
            }
          } else {
            short A[][][] = new short[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) A[a][b][c] = getShort();
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "short["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }
          }
        } else if (at.indexOf("[F") >= 0) { // float  
          if ("[F".equals(at)) {
            float A[] = new float[le];
            for (int a = 0; a < le; ++a) {
              A[a] = ByteBuffer.wrap(bb, p, 4).getFloat();
              p += 4;
            }
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "float["+le+"]");
              vFields.put(n, A); 
            }                
          } else if ("[[F".equals(at)) {
            float A[][] = new float[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                A[a][b] = ByteBuffer.wrap(bb, p, 8).getFloat();
                p += 4;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "float["+dim1+"]["+le+"]");
              vFields.put(n, A); 
            }                
          } else {
            float A[][][] = new float[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  A[a][b][c] = ByteBuffer.wrap(bb, p, 8).getFloat();
                  p += 4;
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "double["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A); 
            }
          } 
        } else if (at.indexOf("[B") >= 0) { // byte 
          if ("[B".equals(at)) {
            byte A[] = new byte[le];
            for (int a = 0; a < le; ++a) A[a] = bb[p++];
            tFields.replace(n, "byte["+le+"]");
            vFields.put(n, A);  
          } else if ("[[B".equals(at)) {
            byte A[][] = new byte[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) A[a][b] = bb[p++];
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "byte["+dim1+"]["+le+"]");
              vFields.put(n, A);
            }                
          } else {
            byte A[][][] = new byte[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) A[a][b][c] = bb[p++];
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "byte["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }
          }
        } else if (at.indexOf("[C") >= 0) { // char 
          if ("[C".equals(at)) {
            char A[] = new char[le];
            for (int a = 0; a < le; ++a) {
              A[a] = ByteBuffer.wrap(bb, p, 2).getChar();
              p += 2;
            }
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "char["+le+"]");
              vFields.put(n, A); 
            }                  
          } else if ("[[C".equals(at)) {
            char A[][] = new char[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                A[a][b] = ByteBuffer.wrap(bb, p, 2).getChar();
                p += 2;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "char["+dim1+"]["+le+"]");
              vFields.put(n, A);
            }                
          } else {
            char A[][][] = new char[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  A[a][b][c] = ByteBuffer.wrap(bb, p, 2).getChar();
                  p += 2;
                }
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "char["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }
          }
        } else if (at.indexOf("[Z") > 0) { // boolean
          if ("[Z".equals(at)) {
            boolean A[] = new boolean[le];
            for (int a = 0; a < le; ++a) A[a] = (int)bb[p++] == 1;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "boolean["+le+"]");
              vFields.put(n, A);
            }                
          } else if ("[[Z".equals(at)) {
            boolean A[][] = new boolean[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) A[a][b] = (int)bb[p++] == 1;
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "boolean["+dim1+"]["+le+"]");
              vFields.put(n, A);
            }                
          } else {
            boolean A[][][] = new boolean[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  A[a][b][c] = (int)bb[p++] == 1;
                }
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(A);
            else {
              tFields.replace(n, "boolean["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, A);
            }
          } 
        } else {
          int len = le;
          int d1 = dim1;
          int d2 = dim2;
          at = at.replace(";", "");
          tSaved = new HashMap<>(tFields);
          vSaved = new HashMap<>(vFields);
          nSaved = new ArrayList<>(fNames);
          String nT = t.substring(t.indexOf("[L")+2).replace(";","");
          if (bb[p] == (byte)0x73 && bb[p+1] == (byte)0x72) {
            SerObjectView ov = new SerObjectView(bb, p+2, ref, true);
            String cls = ov.getClassName();
            pFields.put(cls, ov.getOTFields());
            pNames.put(cls, ov.getFieldNames());
            nTmp = new ArrayList(ov.getFieldNames());
            tTmp = new HashMap(ov.getTFields());
            vTmp = new HashMap(ov.getVFields());
            nRef = new ArrayList(ov.getNRef());
            vRef = new HashMap(ov.getVRef());
            ref = ov.getRef();
            p = ov.getIndex();
            if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) p += 2;
          } else {
            HashMap<String, String> tfs = pFields.get(nT);
            tTmp = new HashMap<>(tfs == null? tFields:tfs);
            ArrayList<String> nfs = pNames.get(nT);
            nTmp = new ArrayList(nfs == null? fNames:nfs);
            vTmp = new HashMap<>(vFields);
          }
          cnt = 0;
          pojo.clear();
          tFields = new HashMap<>(tTmp);
          vFields = new HashMap<>(vTmp);
          fNames = new ArrayList<>(nTmp);
          if (at.equals("[L"+nT)) {
            ArrayList A[] = new ArrayList[len];
            for (int l = 0; l < len; ++l) {
              getValues(true);
              A[l] = new ArrayList<>(pojo);
              if (p < bb.length && bb[p] == (byte)0x73 && bb[p+1] == (byte)0x71) p += 6;
              pojo.clear();
              ++cnt;                  
            }
            tSaved.replace(n, nT+"["+len+"]");
            vSaved.put(n, A);
          } else if (at.equals("[[L"+nT)) {
            ArrayList A[][] = new ArrayList[d1][len];
            for (int a = 0; a < d1; ++a, cnt = 0) {
              for (int l = 0; l < len; ++l) {
                getValues(true);
                A[a][l] = new ArrayList<>(pojo);
                if (p < bb.length && bb[p] == (byte)0x73 && bb[p+1] == (byte)0x71) p += 6;
                pojo.clear();
                ++cnt;
              }
              if (p < bb.length && bb[p] == (byte)0x75 && bb[p+1] == (byte)0x71) p += 16;
            }
            tSaved.replace(n, nT+"["+d1+"]["+le+"]");
            vSaved.put(n, A);
          } else {
            ArrayList A[][][] = new ArrayList[d2][d1][len];
            for (int a = 0; a < d1; ++a) {
              for (int b = 0; b < d2; ++b, cnt = 0) {
                for (int l = 0; l < len; ++l) {
                  getValues(true);
                  A[a][b][l] = new ArrayList<>(pojo);
                  if (p < bb.length && bb[p] == (byte)0x73 && bb[p+1] == (byte)0x71) p += 6;
                  pojo.clear();
                  ++cnt;
                }
                if (p < bb.length && bb[p] == (byte)0x75 && bb[p+1] == (byte)0x71) p += 16;
              }
              if (p < bb.length && bb[p] == (byte)0x75 && bb[p+1] == (byte)0x71) p += 16;
            }
            tSaved.replace(n, nT+"["+d1+"]["+d2+"]["+len+"]");
            vSaved.put(n, A);
          }
          tFields = new HashMap<>(tSaved);
          vFields = new HashMap<>(vSaved);
          fNames = new ArrayList<>(nSaved);
        }              
      }            
    }
  }
  //
  private HashMap<String, String> getOTFields() {
    return ptFields;
  }
  //
  private HashMap<String, String> getTFields() {
    return new HashMap<>(tFields);
  }
  //
  private HashMap<String, Object> getVFields() {
    return new HashMap<>(vFields);
  }
  private ArrayList<Integer> getNRef() {
    return new ArrayList<>(nRef);
  }
  //
  private HashMap<Integer, Object> getVRef() {
    return new HashMap<>(vRef);
  }
  //
  private Object getObject() {
    return object;
  }
  //
  private int getRef() {
    return ref;
  }
  //
  @SuppressWarnings("unchecked")
  private void loadAPI(String api, byte[] bb, int q) throws Exception {
    this.p = q;
    this.bb = bb;
    int cap = getInt();
    if (bb[p] == (byte)0x77 && bb[p+1] == (byte)0x04) {
      p += 2;
      cap = getInt();
    }
    if (api.indexOf("List") > 0) {
      ArrayList list = new ArrayList(cap);
      while (p < bb.length) {
        byte by = bb[p++];
        if (by == (byte)0x74) { // String
          le = getShort();
          list.add(new String(bb, p, le, charset));
          p += le;
        } else if (by == (byte)0x49) { // I
          list.add(getInt());
        }
      }
      object = list;
    } else throw new Exception("Unsupported API "+api);
  }
  //
  private int getInt() {
    return ((int)(bb[p++]&0xFF)<<24)|((int)(bb[p++]&0xFF)<<16)|
           ((int)(bb[p++]&0xFF)<<8)|((int)(bb[p++]&0xFF));
  }
  //
  private short getShort() {
    return (short)(((int)(bb[p++]&0xFF)<<8)|(int)(bb[p++]&0xFF));
  }
  //
  private long getLong() {
    return ((long)(bb[p++]&0xFF)<<56)|((long)(bb[p++]&0xFF)<<48)|
           ((long)(bb[p++]&0xFF)<<40)|((long)(bb[p++]&0xFF)<<32)|
           ((long)(bb[p++]&0xFF)<<24)|((long)(bb[p++]&0xFF)<<16)|
           ((long)(bb[p++]&0xFF)<<8)|((long)(bb[p++]&0xFF));
  }
  private String getArrayType() {
    if (bb[p] == (byte)0x75 && bb[p+1] == (byte)0x72) {
      int q = p+2;
      int le = ((int)(bb[q++]&0xFF)<<8)|(int)(bb[q++]&0xFF);
      String at = new String(bb, q, le, charset).replace(";", "");
      int a = at.indexOf("Ljava");
      if (a > 0) { // only API name
        int e = at.lastIndexOf(".")+1;
        at = at.replace(at.substring(a, e), "");
      }
      p = q+le;
      return at;
    }
    return null;
  }
  private int getDimensions() {
    int q = p;
    dim1 = -1; dim2 = 0;
    for (; p < bb.length; ++p) {
      if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) {
        p += 2;
        if (bb[p+4] != (byte)0x75 || bb[p+5] != (byte)0x72) {
          le = getInt();
          // is referencing?
          if (bb[p] == (byte)0x73 && bb[p+1] == (byte)0x71 ||
              bb[p] == (byte)0x71 && bb[p+1] == (byte)0x00 && bb[p+2] == (byte)0x7E) {
              p += (bb[p] == (byte)0x73 ? 4:3);
              ref += getShort();
          }
          return p;
        }
        if (dim1 < 0) dim1 = getInt();
        else dim2 = getInt();
      }
    }
    return q;
  }
  //
  private byte[] bb;
  private long serID;
  private Object object;
  private String clsName;
  private int nFields, p, dim1, dim2, le, ref, cnt;
  private ArrayList<Object> pojo = new ArrayList<>(); 
  private ArrayList<Integer> nRef = new ArrayList<>(); 
  private Charset charset = StandardCharsets.US_ASCII;
  private HashMap<Integer, Object> vRef = new HashMap<>();
  private ArrayList<String> nTmp, nSaved, fNames = new ArrayList<>(); 
  private HashMap<String, Object> vTmp, vSaved, vFields = new HashMap<>();
  private HashMap<String, String> tTmp, tSaved, tFields = new HashMap<>();
  // POJO data
  private HashMap<String, String> ptFields = new HashMap<>();
  private HashMap<String,ArrayList<String>> pNames = new HashMap<>();
  private HashMap<String, HashMap<String, String>> pFields = new HashMap<>();
}
