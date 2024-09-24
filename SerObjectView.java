import java.io.*;
import java.nio.*;
import java.util.*;
import java.math.*;
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
    byte[] ID = { (byte)0xAC, (byte)0xED, (byte)0x00, (byte)0x05, TC_OBJECT, TC_CLASSDESC };  
    for (p = 0; p < ID.length; ++p) 
    if (bb[p] != ID[p]) throw new Exception("Byte array is not a serialized object");
    pojo.clear(); 
    nRef.clear(); 
    vRef.clear();
    fNames.clear(); 
    pNames.clear();
    pFields.clear();
    vFields.clear();
    tFields.clear();
    ptFields.clear();
    //
    ref = cnt = nFields = 0;
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
    if (clsName.indexOf("$") > 0) { // inner classes
      while ((p+1) < bb.length) if (bb[p] == TC_ENDBLOCKDATA) {
        if (bb[p+1] == TC_NULL) {
          p += 2;
          break;
        } else if (bb[p+1] == TC_CLASSDESC) {
          p += 2;
          clsName = getString();
        } else ++p;
      } else ++p;
    }
    serID = getLong();
    if (bb[p++] != (byte)0x02) throw new Exception("Object has NO serializable field");
    nFields = getShort();
    for (int i = 0; i < nFields && p < bb.length; ++i) {
      String type = ""+(char)bb[p++];
      String fName = getString(); // Field name
      fNames.add(fName);
      //
      if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) break;
      if (bb[p] == TC_STRING) {
        ++p;
        String tmp = getString().replace(";", "");
        int e = tmp.lastIndexOf("/");
        if (e > 0) {
          int f = tmp.lastIndexOf("[");
          type = (f >= 0? tmp.substring(0, f+1):"")+tmp.substring(e+1, tmp.length());
        } else if (tmp.charAt(0) == '[') {
          if (tmp.lastIndexOf("[") > 2) throw new Exception(tmp+" has more than 3 dimensions");
          type = tmp; // array
        } else type = "Object"; // customer Object
      } else if (bb[p] == TC_REFERENCE && bb[p+1] == (byte)0x00 && bb[p+2] == TC_ENUM) {
        p += 3; // TC_reference
        ref = getShort();
        type = "String";
      }
      tFields.put(fName, type);
      ptFields.put(fName, type);
    }
    //
    if (!array) while ((p+1) < bb.length) {
      if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) {
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
         (bb[p] == TC_REFERENCE && bb[p+1] == (byte)0x00 && bb[p+2] == TC_ENUM ||
          bb[p] == TC_OBJECT && bb[p+1] == TC_REFERENCE && bb[p+2] == (byte)0x00)) {
        p += bb[p] == TC_REFERENCE? 3:4;
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
            setValue(array, n, getInt(), "int");
            break;
          case "D": // double
            setValue(array, n, getDouble(), "double");
            break;
          case "J": // long
            setValue(array, n, getLong(), "long");
            break;
          case "F": // float
            setValue(array, n, getFloat(), "float");
            break;
          case "S": // short
            setValue(array, n, getShort(), "short");
            break;
          case "String":
            if (bb[p] == TC_STRING) ++p;
            setValue(array, n, getString(), "String");
            break;
          case "B": // byte
            setValue(array, n, bb[p++], "byte");
            break;
          case "Z": // boolean
            setValue(array, n, bb[p++] == (byte)0x01, "boolean");
            break;
          case "C": // char
            setValue(array, n, getChar(), "char");
            break;
          default:
            if (bb[p] == TC_OBJECT && bb[p+1] == TC_CLASSDESC) { // nested Object
              if (t.endsWith("Integer") || t.endsWith("Decimal") || t.endsWith("List") || 
                  t.endsWith("Double") || t.endsWith("Float") || t.endsWith("Long") ||
                  t.endsWith("Short")) {
                loadAPI(t);
                if (!t.endsWith("List")) setValue(array, n, object, t);
                else setValue(array, n, object, t.substring(t.lastIndexOf(".")+1)+"<"+listType+">");
              } else {
                SerObjectView ov = new SerObjectView(bb, p+2, ref, array);
                String cls = ov.getClassName();
                pFields.put(cls, ov.getOTFields());         
                pNames.put(cls, ov.getFieldNames());
                tFields.put(n, "Object");
                vFields.put(n, ov);
                p = ov.getIndex();
              }
            } else if (t.endsWith("Integer") || t.endsWith("Decimal") || t.endsWith("List") || 
                       t.endsWith("Double") || t.endsWith("Float") || t.endsWith("Long") ||
                       t.endsWith("Short")) {
              loadAPI(t);
              if (!t.endsWith("List")) setValue(array, n, object, t);
              else setValue(array, n, object, t.substring(t.lastIndexOf(".")+1)+"<"+listType+">");
            }
          }
      } else {
        p = getArrayType(n);
        switch (t) {
        case "[I":
            int I[] = new int[le];
            for (int a = 0; a < le; ++a) I[a] = getInt();
            setPojo(array, n, I, "int["+le+"]");
            break;
        case "[[I":
            int II[][] = new int[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) II[a][b] = getInt();
              p += 10;
            }
            p -= 10;
            setPojo(array, n, II, "int["+dim1+"]["+le+"]");
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
            setPojo(array, n, III, "int["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[J":
            long J[] = new long[le];
            for (int a = 0; a < le; ++a) J[a] = getLong();
            setPojo(array, n, J, "long["+le+"]");
            break;
        case "[[J":
            long JJ[][] = new long[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) JJ[a][b] = getLong();
              p += 10;
            }
            p -= 10;
            setPojo(array, n, JJ, "long["+dim1+"]["+le+"]");
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
            setPojo(array, n, JJJ, "long["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[D":
            double D[] = new double[le];
            for (int a = 0; a < le; ++a) {
              D[a] = getDouble();
            }
            setPojo(array, n, D, "double["+le+"]");
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
            setPojo(array, n, DD, "double["+dim1+"]["+le+"]");
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
            setPojo(array, n, DDD, "double["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[String":
            String T[] = new String[le];
            for (int a = 0; a < le; ++a) {
              ++p; // ignore 0x74
              T[a] = getString();
            }
            setPojo(array, n, T, "String["+le+"]");
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
            setPojo(array, n, TT, "String["+dim1+"]["+le+"]");
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
            setPojo(array, n, TTT, "String["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[S":
            short S[] = new short[le];
            for (int a = 0; a < le; ++a) S[a] = getShort();
            setPojo(array, n, S, "short["+le+"]");
            break;
        case "[[S":
            short SS[][] = new short[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) SS[a][b] = getShort();
              p += 10;
            }
            p -= 10;
            setPojo(array, n, SS, "short["+dim1+"]["+le+"]");
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
            setPojo(array, n, SSS, "short["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[F":
            float F[] = new float[le];
            for (int a = 0; a < le; ++a) F[a] = getFloat();
            setPojo(array, n, F, "float["+le+"]");
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
            setPojo(array, n, FF, "float["+dim1+"]["+le+"]");
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
            setPojo(array, n, FFF, "float["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[B":
            byte B[] = new byte[le];
            for (int a = 0; a < le; ++a) B[a] = bb[p++];
            setPojo(array, n, B, "byte["+le+"]");
            break;
        case "[[B":
            byte BB[][] = new byte[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) BB[a][b] = bb[p++];
              p += 10;
            }
            p -= 10;
            setPojo(array, n, BB, "byte["+dim1+"]["+le+"]");
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
            setPojo(array, n, BBB, "byte["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[C":
            char C[] = new char[le];
            for (int a = 0; a < le; ++a) {
              C[a] = getChar();
            }
            setPojo(array, n, C, "char["+le+"]");
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
            setPojo(array, n, CC, "char["+dim1+"]["+le+"]");
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
            setPojo(array, n, CCC, "char["+dim1+"]["+dim2+"]["+le+"]");
            break;
        case "[Z":
            boolean Z[] = new boolean[le];
            for (int a = 0; a < le; ++a) Z[a] = (int)bb[p++] == 1;
            setPojo(array, n, Z, "boolean["+le+"]");
            break;
        case"[[Z":
            boolean ZZ[][] = new boolean[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) ZZ[a][b] = (int)bb[p++] == 1;
              p += 10;
            }
            p -= 10;
            setPojo(array, n, ZZ, "boolean["+dim1+"]["+le+"]");
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
            setPojo(array, n, ZZZ, "boolean["+dim1+"]["+dim2+"]["+le+"]");
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
          if (bb[p] == TC_OBJECT && bb[p+1] == TC_CLASSDESC) {
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
            if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) p += 2;
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
              if (p < bb.length && bb[p] == TC_OBJECT && bb[p+1] == TC_REFERENCE) p += 6;
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
                if (p < bb.length && bb[p] == TC_OBJECT && bb[p+1] == TC_REFERENCE) p += 6;
                pojo.clear();
                ++cnt;
              }
              if (p < bb.length && bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) p += 16;
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
                  if (p < bb.length && bb[p] == TC_OBJECT && bb[p+1] == TC_REFERENCE) p += 6;
                  pojo.clear();
                  ++cnt;
                }
                if (p < bb.length && bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) p += 16;
              }
              if (p < bb.length && bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) p += 16;
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
  private String getArrayType() {
    return listType;
  }
  //
  private int getRef() {
    return ref;
  }
  //
  @SuppressWarnings("unchecked")
  private void loadAPI(String type) throws Exception {
    if (type.startsWith("BigD")) { // BigDecimal
      int scale = 0, sign = 1;
      while ((p+6) < bb.length)
      if (bb[p] == (byte)'s' && bb[p+1] == (byte)'c' && bb[p+2] == (byte)'a' &&
          bb[p+3] == (byte)'l' && bb[p+4] == (byte)'e') {
        while (p < bb.length) if (bb[p] == TC_ENDBLOCKDATA) {
          if (bb[p+1] == TC_REFERENCE && bb[p+3] == TC_ENUM) { // with Ref
            p += 6; break; // scale value
          } else if (bb[p+1] == TC_NULL) {
            p += 2; break; // scale value
          } else ++p;
        } else ++p;
        scale = getInt();
      } else if (bb[p] == (byte)0xFF) {
        if (bb[p+19] == (byte)0xFF) sign = -1;
        p += 39;
        break;
      } else ++p;
      le = getInt();
      byte[] val = new byte[le];
      System.arraycopy(bb, p, val, 0, le);
      object = new BigDecimal(new BigInteger(sign, val), scale);
      p += le;
    } else if (type.startsWith("BigI")) { // BigInteger
      int sign = 1;
      byte by = bb[p+1];
      while (p < bb.length) if (bb[p] == (byte)0xFF) {
        if (bb[p+19] == (byte)0xFF) sign = -1;
        p += by == TC_ENDBLOCKDATA?26:39;
        break;
      } else ++p;
      le = getInt();
      byte[] val = new byte[le];
      System.arraycopy(bb, p, val, 0, le);
      object = new BigInteger(sign, val);
      p += le;
    } else if (type.startsWith("In")) {
      object = getInt();
    } else if (type.startsWith("Do")) {
      skip();
      object = getDouble();
    } else if (type.startsWith("Lo")) {
      object = getLong();
    } else if (type.startsWith("Fl")) {
      object = getFloat();
    } else if (type.startsWith("Sh")) {
      object = getShort();
    } else if (type.endsWith("List")) {
      skip();
      int cap = getInt();
      if (bb[p] == TC_BLOCKDATA && bb[p+1] == (byte)0x04) {
        p += 2;
        cap = getInt();
      }
      boolean b = false;
      listType = "String";
      if (bb[p] == TC_OBJECT && bb[p+1] == TC_CLASSDESC) {
        p += 2;
        String s = getString(); // get Object type
        if (s.startsWith("java")) listType = s.substring(s.lastIndexOf(".")+1);
        while ((p+1) < bb.length) if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) {
          b = true;
          p += 2;
          break;
        } else ++p; 
      }
      ArrayList list = new ArrayList(cap);
      while (p < bb.length) {
        byte by = bb[p];
        if (by == TC_STRING) {
          ++p; // String
          list.add(getString());
        } else if (by == TC_OBJECT || b) {
          if (by == TC_OBJECT && bb[p+1] == TC_REFERENCE) p += 6;
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
          case "Character":
            list.add(getChar());
            break;
          case "Boolean":
            list.add(bb[p++] == (byte)0x01);
            break;
          default:
            throw new Exception(String.format("ArrayList<%s> is unsupported.\n", listType));
          }
          b = false;
        } else if (bb[p] == TC_ENDBLOCKDATA) { // end Data block
          ++p;
          break;
        } else throw new Exception(String.format("Unknown code format: %02X\n", by));
      }
      object = list;
    } else throw new Exception("API:"+type+" is not supported.");
  }
  //
  private void setPojo(boolean array, String oN, Object oV, String oT) {
    if (array) pojo.add(oV);
    else {
      tFields.replace(oN, oT);
      vFields.put(oN, oV);
    }
  }
  //
  private void setValue(boolean array, String oN, Object oV, String oT) {
    vRef.put(ref, oV);
    nRef.add(ref++);
    if (array) pojo.add(oV);
    else {
      tFields.replace(oN, oT);
      vFields.put(oN, oV);
    }
  }
  //
  private void skip() {
    while ((p+1) < bb.length) if (bb[p] == TC_ENDBLOCKDATA) {
      if (bb[p+1] == TC_NULL) { 
        p += 2;
        return;
      }
      ++p;
    } else ++p;
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
    if (bb[p] == TC_ARRAY && bb[p+1] == TC_CLASSDESC) {
      q = p+2;
      int le = ((int)(bb[q++]&0xFF)<<8)|(int)(bb[q++]&0xFF);
      p = q+le;
      if (bb[p] != (byte)0x00) {
        while ((p-1) < bb.length)
        if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) break;
        else ++p;
      }    
      return getDimensions();
    }
    dim1 = -1; dim2 = -1; le = -1;
    if (bb[p] == TC_ARRAY && bb[p+1] == TC_REFERENCE) {
        for (p += 6; p < bb.length; p += 6) {
          if (dim1 < 0) dim1 = getInt();
          else if (dim2 < 0) dim2 = getInt();
          else le = getInt();
          if ((p+1) < bb.length && bb[p] != TC_ARRAY && bb[p+1] != TC_REFERENCE) {
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
      if (bb[p] == TC_ENDBLOCKDATA && bb[p+1] == TC_NULL) {
        p += 2;
        if (bb[p+4] == TC_ARRAY && bb[p+5] == TC_REFERENCE) {
          for (; p < bb.length; p += 6) {
            if (dim1 < 0) dim1 = getInt();
            else if (dim2 < 0) dim2 = getInt();
            else le = getInt();
            if ((p+1) < bb.length && bb[p] != TC_ARRAY && bb[p+1] != TC_REFERENCE) {
              if (le < 0) {
                le = dim1;
                dim1 = dim2;
                dim2 = -1;
              }
              return p;
            }
          }
          return q;
        } else if (bb[p+4] != TC_ARRAY || bb[p+5] != TC_CLASSDESC ) {
          le = getInt();
          // is referencing?
          if (bb[p] == TC_OBJECT && bb[p+1] == TC_REFERENCE ||
              bb[p] == TC_REFERENCE && bb[p+1] == (byte)0x00 && bb[p+2] == TC_ENUM) {
              p += (bb[p] == TC_OBJECT ? 4:3);
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
  final static short STREAM_MAGIC = (short)0xACED;
  final static short STREAM_VERSION = 5;
  final static byte TC_NULL = (byte)0x70;
  final static byte TC_REFERENCE = (byte)0x71;
  final static byte TC_CLASSDESC = (byte)0x72;
  final static byte TC_OBJECT = (byte)0x73;
  final static byte TC_STRING = (byte)0x74;
  final static byte TC_ARRAY = (byte)0x75;
  final static byte TC_CLASS = (byte)0x76;
  final static byte TC_BLOCKDATA = (byte)0x77;
  final static byte TC_ENDBLOCKDATA = (byte)0x78;
  final static byte TC_RESET = (byte)0x79;
  final static byte TC_BLOCKDATALONG = (byte)0x7A;
  final static byte TC_EXCEPTION = (byte)0x7B;
  final static byte TC_LONGSTRING = (byte) 0x7C;
  final static byte TC_PROXYCLASSDESC = (byte) 0x7D;
  final static byte TC_ENUM = (byte) 0x7E;
  final static int   baseWireHandle = 0x7E0000;
  
  final static byte SC_WRITE_METHOD = 0x01; //if SC_SERIALIZABLE
  final static byte SC_BLOCK_DATA = 0x08;    //if SC_EXTERNALIZABLE
  final static byte SC_SERIALIZABLE = 0x02;
  final static byte SC_EXTERNALIZABLE = 0x04;
  final static byte SC_ENUM = 0x10;
  //
  private byte[] bb;
  private long serID;
  private Object object;
  private String clsName, objName, listType;
  private int nFields, p, dim1, dim2, le, ref, cnt;
  //
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
