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
    clsName = getString();
    //
    if (bb[p] > (byte)0) {
      while (true) {
        if (bb[p] == (byte)0x78) {
          if (bb[p+1] == (byte)0x70) break;
          else if (bb[p+1] == (byte)0x72) {
            p += 2;
            clsName = getString();
          } else ++p;
        } else ++p;
      }
      p += 2;      
      loadAPI( ); // over 0x7870
      nFields = 0;
      return;
    } else {
      serID = getLong();
      if (bb[p++] != (byte)0x02) throw new Exception("Object has NO serializable field");
      nFields = getShort();
      for (int i = 0; i < nFields && p < bb.length; ++i) {
        String type = ""+(char)bb[p++];
        String fName = getString(); // Field name
        fNames.add(fName);
        //
        if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) break;
        if (bb[p] == (byte)0x74) {
          ++p;
          String tmp = getString().replace(";", "");
          int e = tmp.lastIndexOf("/");
          if (e > 0) {
            if (tmp.indexOf("java") >= 0 && tmp.indexOf("List") < 0 &&
                tmp.indexOf("String") < 0 ) throw new Exception(tmp+" API is not supported");
            int f = tmp.lastIndexOf("[");
            type = (f >= 0? tmp.substring(0, f+1):"")+tmp.substring(e+1, tmp.length());
          } else if (tmp.charAt(0) == '[') {
            if (tmp.lastIndexOf("[") > 2) throw new Exception(tmp+" has more than 3 dimensions");
            type = tmp; // array
          } else type = "Object"; // customer Object
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
    if (!array) while ((p+1) < bb.length) {
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
    Object objValue = null;
    String trueType = null;
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
      if (t.indexOf("[") < 0) {
        switch (t) {
          case "I": // int
            objValue = getInt();
            trueType = "int";
            break;
          case "D": // double
            objValue = getDouble();
            trueType = "double";
            break;
          case "J": // long
            objValue = getLong();
            trueType = "long";
            break;
          case "F": // float
            objValue = getFloat();
            trueType = "float";
            break;
          case "S": // short
            objValue = getShort();
            trueType = "short";
            break;
          case "String":
            if (bb[p] == (byte)0x74) ++p;
            objValue = getString();
            trueType = "String";
            break;
          case "B": // byte
            objValue = bb[p++];
            trueType = "byte";
            break;
          case "Z": // boolean
            objValue = bb[p++] == (byte)0x01;
            trueType = "boolean";
            break;
          case "C": // char
            objValue = getChar();
            trueType = "char";
            break;
          default:
            if (bb[p] == (byte)0x73 && bb[p+1] == (byte)0x72) { // nested Object
              SerObjectView ov = new SerObjectView(bb, p+2, ref, array);
              if (ov.getSize() > 0) {
                String cls = ov.getClassName();
                pFields.put(cls, ov.getOTFields());         
                pNames.put(cls, ov.getFieldNames());
                tFields.put(n, "Object");
                vFields.put(n, ov);
              } else {
                t = ov.getClassName();
                if (!t.startsWith("java")) tFields.put(n, t);
                else tFields.put(n, t.substring(t.lastIndexOf(".")+1)+"<"+ov.getArrayType()+">");
                vFields.put(n, ov.getObject());
              }
              p = ov.getIndex();
            }
            continue;
          }
          vRef.put(ref, objValue);
          nRef.add(ref++);
      } else {
        p = getArrayType(n);
        switch (t) {
        case "[I":
            int I[] = new int[le];
            for (int a = 0; a < le; ++a) I[a] = getInt();
            trueType = "int["+le+"]";
            objValue = I;
            break;
        case "[[I":
            int II[][] = new int[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) II[a][b] = getInt();
              p += 10;
            }
            p -= 10;
            trueType = "int["+dim1+"]["+le+"]";
            objValue = II;
            break;
        case "[[[I":
            int III[][][] = new int[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) III[a][b][c] = getInt();
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            trueType = "int["+dim1+"]["+dim2+"]["+le+"]";
            objValue = III;
            break;
        case "[J":
            long J[] = new long[le];
            for (int a = 0; a < le; ++a) J[a] = getLong();
            trueType = "long["+le+"]";
            objValue = J;
            break;
        case "[[J":
            long JJ[][] = new long[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) JJ[a][b] = getLong();
              p += 10;
            }
            p -= 10;
            trueType = "long["+dim1+"]["+le+"]";
            objValue = JJ;
            break;
        case "[[[J":
            long JJJ[][][] = new long[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) JJJ[a][b][c] = getLong();
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            trueType = "long["+dim1+"]["+dim2+"]["+le+"]";
            objValue = JJJ;
            break;
        case "[D":
            double D[] = new double[le];
            for (int a = 0; a < le; ++a) {
              D[a] = getDouble();
            }
            trueType = "double["+le+"]";
            objValue = D;
            break;
        case "[[D":
            double DD[][] = new double[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                DD[a][b] = getDouble();
              }
              p += 10;
            }
            p -= 10;
            trueType = "double["+dim1+"]["+le+"]";
            objValue = DD;
            break;
        case "[[[D":
            double DDD[][][] = new double[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  DDD[a][b][c] = getDouble();
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            trueType = "double["+dim1+"]["+dim2+"]["+le+"]";
            objValue = DDD;                
            break;
        case "[String":
            String T[] = new String[le];
            for (int a = 0; a < le; ++a) {
              ++p; // ignore 0x74
              T[a] = getString();
            }
            trueType = "String["+le+"]";
            objValue = T;
            break;
        case "[[String":
            String TT[][] = new String[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                ++p; // ignore 0x74
                TT[a][b] = getString();
              }
              p += 10;
            }
            p -= 10;
            trueType = "String["+dim1+"]["+le+"]";
            objValue = TT;
            break;
        case "[[[String":
            String TTT[][][] = new String[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  ++p; // ignore 0x74
                  TTT[a][b][c] = getString();
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            trueType = "String["+dim1+"]["+dim2+"]["+le+"]";
            objValue = TTT;
            break;
        case "[S":
            short S[] = new short[le];
            for (int a = 0; a < le; ++a) S[a] = getShort();
            trueType = "short["+le+"]";
            objValue = S;
            break;
        case "[[S":
            short SS[][] = new short[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) SS[a][b] = getShort();
              p += 10;
            }
            p -= 10;
            trueType = "short["+dim1+"]["+le+"]";
            objValue = SS;
            break;
        case "[[[S":
            short SSS[][][] = new short[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) SSS[a][b][c] = getShort();
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            trueType = "short["+dim1+"]["+dim2+"]["+le+"]";
            objValue = SSS;
            break;
        case "[F":
            float F[] = new float[le];
            for (int a = 0; a < le; ++a) F[a] = getFloat();
            trueType = "float["+le+"]";
            objValue = F;
            break;
          case "[[F":
            float FF[][] = new float[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                FF[a][b] = getFloat();
              }
              p += 10;
            }
            p -= 10;
            trueType = "float["+dim1+"]["+le+"]";
            objValue = FF;
            break;            
        case "[[[F":
            float FFF[][][] = new float[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  FFF[a][b][c] = getFloat();
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            trueType = "float["+dim1+"]["+dim2+"]["+le+"]";
            objValue = FFF;
            break; 
        case "[B":
            byte B[] = new byte[le];
            for (int a = 0; a < le; ++a) B[a] = bb[p++];
            trueType = "byte["+le+"]";
            objValue = B;
            break;
        case "[[B":
            byte BB[][] = new byte[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) BB[a][b] = bb[p++];
              p += 10;
            }
            p -= 10;
            trueType = "byte["+dim1+"]["+le+"]";
            objValue = BB;
            break;
        case "[[[B":
            byte BBB[][][] = new byte[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) BBB[a][b][c] = bb[p++];
              }
              p += 10;
            }
            p -= 20;
            trueType = "byte["+dim1+"]["+dim2+"]["+le+"]";
            objValue = BBB;
            break;
        case "[C":
            char C[] = new char[le];
            for (int a = 0; a < le; ++a) {
              C[a] = getChar();
            }
            trueType = "char["+le+"]";
            objValue = C;
            break;
        case "[[C":
            char CC[][] = new char[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                CC[a][b] = getChar();
              }
              p += 10;
            }
            p -= 10;
            trueType = "char["+dim1+"]["+le+"]";
            objValue = CC;
            break;
        case "[[[C":
            char CCC[][][] = new char[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  CCC[a][b][c] = getChar();
                }
              }
              p += 10;
            }
            p -= 20;
            trueType = "char["+dim1+"]["+dim2+"]["+le+"]";
            objValue = CCC;
            break;
        case "[Z":
            boolean Z[] = new boolean[le];
            for (int a = 0; a < le; ++a) Z[a] = (int)bb[p++] == 1;
            trueType = "boolean["+le+"]";
            objValue = Z;
            break;
        case"[[Z":
            boolean ZZ[][] = new boolean[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) ZZ[a][b] = (int)bb[p++] == 1;
              p += 10;
            }
            p -= 10;
            trueType = "boolean["+dim1+"]["+le+"]";
            objValue = ZZ;
            break;
        case "[[[Z":
            boolean ZZZ[][][] = new boolean[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  ZZZ[a][b][c] = (int)bb[p++] == 1;
                }
              }
              p += 10;
            }
            p -= 20;
            trueType = "boolean["+dim1+"]["+dim2+"]["+le+"]";
            objValue = ZZZ;
            break;
        default:
          int len = le;
          int d1 = dim1;
          int d2 = dim2;
          t = t.replace(";", "");
          ArrayList<String> nTmp = null;
          HashMap<String, String> tTmp = null;
          HashMap<String, Object> vTmp = null;
          ArrayList<String> nSaved = new ArrayList<>(fNames);
          HashMap<String, String> tSaved = new HashMap<>(tFields);
          HashMap<String, Object> vSaved = new HashMap<>(vFields);
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
            int r = ov.getRef(); // new Ref?
            if (ref < r) ref = r;
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
          if (t.startsWith("[L")) {
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
          } else if (t.startsWith("[[L")) {
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
          continue;
        }              
      }            
      if (array) pojo.add(objValue);
      else {
        tFields.replace(n, trueType);
        vFields.put(n, objValue);
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
  private String getArrayType() {
    return listType;
  }
  //
  private int getRef() {
    return ref;
  }
  //
  @SuppressWarnings("unchecked")
  private void loadAPI() throws Exception {
    int cap = getInt();
    if (bb[p] == (byte)0x77 && bb[p+1] == (byte)0x04) {
      p += 2;
      cap = getInt();
    }
    boolean b = false;
    listType = "String";
    if (bb[p] == (byte)0x73 && bb[p+1] == (byte)0x72) {
      p += 2;
      String s = getString(); // get Object type
      if (s.startsWith("java")) listType = s.substring(s.lastIndexOf(".")+1);
      while ((p+1) < bb.length) if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) {
        b = true;
        p += 2;
        break;
      } else ++p; 
    }
    ArrayList list = new ArrayList(cap);
    while (p < bb.length) {
      byte by = bb[p];
      if (by == (byte)0x74) {
        ++p; // String
        list.add(getString());
      } else if (by == (byte)0x73 || b) {
        if (by == (byte)0x73 && bb[p+1] == (byte)0x71) p += 6;
        switch (listType) {
        case "Integer":
          list.add(getInt());
          break;
        case "Double":
          list.add(getDouble());
          break;
        case "Long":
          list.add(getLong());
          break;
        case "Float":
          list.add(getFloat());
          break;
       case "Short":
          list.add(getShort());
          break;
        case "Byte":
          list.add(bb[p++]);
          break;
        case "Char":
          list.add(getChar());
          break;
        case "Boolean":
          list.add(bb[p++] == (byte)0x01);
          break;
        default:
          throw new Exception(String.format("ArrayList<%s> is unsupported.\n", listType));
        }
        b = false;
      } else if (bb[p] == (byte)0x78) { // end Data block
        ++p;
        break;
      } else throw new Exception(String.format("Unknown code format: %02X\n", by));
    }
    object = list;
  }
  //
  private String getString() {
    int l = getShort();
    String s =  new String(bb, p, l, charset);
    p += l;
    return s;
  }
  //
  private int getInt() {
    return ((int)(bb[p++]&0xFF)<<24)|((int)(bb[p++]&0xFF)<<16)|
           ((int)(bb[p++]&0xFF)<<8)|((int)(bb[p++]&0xFF));
  }
  //
  private double getDouble() {
    double d = ByteBuffer.wrap(bb, p, 8).getDouble();
    p += 8;
    return d;
  }
  //
  private float getFloat() {
    float f = ByteBuffer.wrap(bb, p, 4).getFloat();
    p += 4;
    return f;
  }
  //
  private char getChar() {
    char c = ByteBuffer.wrap(bb, p, 2).getChar();
    p += 2;
    return c;
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
  //
  private int getArrayType(String name) {
    int q = p;
    if (bb[p] == (byte)0x75 && bb[p+1] == (byte)0x72) {
      q = p+2;
      int le = ((int)(bb[q++]&0xFF)<<8)|(int)(bb[q++]&0xFF);
      p = q+le;
      if (bb[p] != (byte)0x00) {
        while ((p-1) < bb.length)
        if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) break;
        else ++p;
      }    
      return getDimensions();
    }
    dim1 = -1; dim2 = -1; le = -1;
    if (bb[p] == (byte)0x75 && bb[p+1] == (byte)0x71) {
        for (p += 6; p < bb.length; p += 6) {
          if (dim1 < 0) dim1 = getInt();
          else if (dim2 < 0) dim2 = getInt();
          else le = getInt();
          if ((p+1) < bb.length && bb[p] != (byte)0x75 && bb[p+1] != (byte)0x71) {
            if (le < 0) {
              le = dim1;
              dim1 = dim2;
              dim2 = -1;
            }
            return p;
          }
        }
    }
    return q;
  }
  //
  private int getDimensions() {
    int q = p;
    dim1 = -1; dim2 = -1; le = -1;
    for (; (p+1) < bb.length; ++p) {
      if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) {
        p += 2;
        if (bb[p+4] == (byte)0x75 && bb[p+5] == (byte)0x71) {
          for (; p < bb.length; p += 6) {
            if (dim1 < 0) dim1 = getInt();
            else if (dim2 < 0) dim2 = getInt();
            else le = getInt();
            if ((p+1) < bb.length && bb[p] != (byte)0x75 && bb[p+1] != (byte)0x71) {
              if (le < 0) {
                le = dim1;
                dim1 = dim2;
                dim2 = -1;
              }
              return p;
            }
          }
          return q;
        } else if (bb[p+4] != (byte)0x75 || bb[p+5] != (byte)0x72 ) {
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
  private String clsName, listType;
  private int nFields, p, dim1, dim2, le, ref, cnt;
  private ArrayList<Object> pojo = new ArrayList<>(); 
  private ArrayList<Integer> nRef = new ArrayList<>(); 
  private Charset charset = StandardCharsets.US_ASCII;
  private ArrayList<String> fNames = new ArrayList<>(); 
  private HashMap<Integer, Object> vRef = new HashMap<>();
  private HashMap<String, Object> vFields = new HashMap<>();
  private HashMap<String, String> tFields = new HashMap<>();
  // POJO data
  private HashMap<String, String> ptFields = new HashMap<>();
  private HashMap<String,ArrayList<String>> pNames = new HashMap<>();
  private HashMap<String, HashMap<String, String>> pFields = new HashMap<>();
}
