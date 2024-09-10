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
          if (e > 0) {
            int f = tmp.lastIndexOf("[");
             type = (f >= 0? tmp.substring(0, f+1):"")+tmp.substring(e+1, tmp.length());
          } else if (tmp.charAt(0) == '[') {
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
      if (t.indexOf("[") < 0) {
        switch (t) {
          case "I": // int
            int I = getInt();
            if (array) pojo.add(I);
            else {
              vFields.put(n, I);
              tFields.replace(n, "int");
            }
            nRef.add(ref);
            vRef.put(ref++, I);
            continue;
          case "D": // double
            double d = ByteBuffer.wrap(bb, p, 8).getDouble();
            if (array) pojo.add(d);
            else {
              vFields.put(n, d);
              tFields.replace(n, "double");
            }
            p += 8;
            nRef.add(ref);
            vRef.put(ref++, d);
            continue;
          case "J": // long
            long L = getLong();
            if (array) pojo.add(L);
            else {
              vFields.put(n, L);
              tFields.replace(n, "long");
            }
            nRef.add(ref);
            vRef.put(ref++, L);
            continue;
          case "F": // float
            float f = ByteBuffer.wrap(bb, p, 4).getFloat();
            if (array) pojo.add(f);
            else {
              vFields.put(n, f);
              tFields.replace(n, "float");
            }
            p += 4;
            nRef.add(ref);
            vRef.put(ref++, f);
            continue;
          case "S": // short
            short s = getShort();
            if (array) pojo.add(s);
            else {
              vFields.put(n, s);
              tFields.replace(n, "short");
            }
            nRef.add(ref);
            vRef.put(ref++, s);
            continue;
          case "String":
            if (bb[p] == (byte)0x74) ++p;
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
            continue;
          case "B": // byte
            if (array) pojo.add(bb[p]);
            else {
              tFields.replace(n, "byte");
              vFields.put(n, bb[p]);
            }
            nRef.add(ref);
            vRef.put(ref++, bb[p++]);
            continue;
          case "Z": // boolean
            if (array) pojo.add(bb[p] == (byte)1);
            else {
              tFields.replace(n, "boolean");
              vFields.put(n, bb[p] == (byte)1);
            }
            nRef.add(ref);
            vRef.put(ref++, bb[p++] == (byte)1);
            continue;
          case "C": // char
            char C = ByteBuffer.wrap(bb, p, 2).getChar();
            if (array) pojo.add(C);
            else {
              tFields.replace(n, "char");
              vFields.put(n, C);
            }
            p += 2;
            nRef.add(ref);
            vRef.put(ref++, C);
            continue;
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
                tFields.put(n, ov.getClassName());
                vFields.put(n, ov.getObject());
              }
              p = ov.getIndex();
            }
          }
      } else {
        p = getArrayType(n);
        switch (t) {
        case "[I":
            int I[] = new int[le];
            for (int a = 0; a < le; ++a) I[a] = getInt();
            if (array) pojo.add(I);
            else {
              tFields.replace(n, "int["+le+"]");
              vFields.put(n, I);  
            }
            continue;
        case "[[I":
            int II[][] = new int[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) II[a][b] = getInt();
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(II);
            else {
              tFields.replace(n, "int["+dim1+"]["+le+"]");
              vFields.put(n, II); 
            }
            continue;
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
            if (array) pojo.add(III);
            else {
              tFields.replace(n, "int["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, III);
            }                
            continue;
        case "[J":
            long J[] = new long[le];
            for (int a = 0; a < le; ++a) J[a] = getLong();
            if (array) pojo.add(J);
            else {
              tFields.replace(n, "long["+le+"]");
              vFields.put(n, J); 
            }
            continue;
        case "[[J":
            long JJ[][] = new long[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) JJ[a][b] = getLong();
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(JJ);
            else {
              tFields.replace(n, "long["+dim1+"]["+le+"]");
              vFields.put(n, JJ);
            }
            continue;
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
            if (array) pojo.add(JJJ);
            else {
              tFields.replace(n, "long["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, JJJ);
            }
            continue;
        case "[D":
            double D[] = new double[le];
            for (int a = 0; a < le; ++a) {
              D[a] = ByteBuffer.wrap(bb, p, 8).getDouble();
              p += 8;
            }
            if (array) pojo.add(D);
            else {
              tFields.replace(n, "double["+le+"]");
              vFields.put(n, D);
            }
            continue;
        case "[[D":
            double DD[][] = new double[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                DD[a][b] = ByteBuffer.wrap(bb, p, 8).getDouble();
                p += 8;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(DD);
            else {
              tFields.replace(n, "double["+dim1+"]["+le+"]");
              vFields.put(n, DD);
            }
            continue;
        case "[[[D":
            double DDD[][][] = new double[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  DDD[a][b][c] = ByteBuffer.wrap(bb, p, 8).getDouble();
                  p += 8;
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(DDD);
            else {
              tFields.replace(n, "double["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, DDD);
            }                
            continue;
        case "[String":
            String T[] = new String[le];
            for (int a = 0; a < le; ++a) {
              ++p; // ignore 0x74
              int l = getShort();
              T[a] =  new String(bb, p, l, charset);
              p += l;
            }
            if (array) pojo.add(T);
            else {
              vFields.put(n, T); 
              tFields.replace(n, "String["+le+"]");
            }
            continue;
        case "[[String":
            String TT[][] = new String[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                ++p; // ignore 0x74
                int l = getShort();
                TT[a][b] =  new String(bb, p, l, charset);
                p += l;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(TT);
            else {
              tFields.replace(n, "String["+dim1+"]["+le+"]");
              vFields.put(n, TT); 
            }
            continue;
        case "[[[String":
            String TTT[][][] = new String[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  ++p; // ignore 0x74
                  int l = getShort();
                  TTT[a][b][c] =  new String(bb, p, l, charset);
                  p += l;
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(TTT);
            else {
              tFields.replace(n, "String["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, TTT);
            }
            continue;
        case "[S":
            short S[] = new short[le];
            for (int a = 0; a < le; ++a) S[a] = getShort();
            if (array) pojo.add(S);
            else {
              tFields.replace(n, "short["+le+"]");
              vFields.put(n, S);  
            }
            continue;
        case "[[S":
            short SS[][] = new short[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) SS[a][b] = getShort();
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(SS);
            else {
              tFields.replace(n, "short["+dim1+"]["+le+"]");
              vFields.put(n, SS);
            }
            continue;
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
            if (array) pojo.add(SSS);
            else {
              tFields.replace(n, "short["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, SSS);
            }
            continue;
        case "[F":
            float F[] = new float[le];
            for (int a = 0; a < le; ++a) {
              F[a] = ByteBuffer.wrap(bb, p, 4).getFloat();
              p += 4;
            }
            if (array) pojo.add(F);
            else {
              tFields.replace(n, "float["+le+"]");
              vFields.put(n, F); 
            }
            continue;
          case "[[F":
            float FF[][] = new float[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                FF[a][b] = ByteBuffer.wrap(bb, p, 8).getFloat();
                p += 4;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(FF);
            else {
              tFields.replace(n, "float["+dim1+"]["+le+"]");
              vFields.put(n, FF); 
            }
            continue;            
        case "[[[F":
            float FFF[][][] = new float[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  FFF[a][b][c] = ByteBuffer.wrap(bb, p, 8).getFloat();
                  p += 4;
                }
                p += 10;
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(FFF);
            else {
              tFields.replace(n, "float["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, FFF); 
            }
            continue; 
        case "[B":
            byte B[] = new byte[le];
            for (int a = 0; a < le; ++a) B[a] = bb[p++];
            if (array) pojo.add(B);
            else {
              tFields.replace(n, "byte["+le+"]");
              vFields.put(n, B);  
            }
            continue;
        case "[[B":
            byte BB[][] = new byte[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) BB[a][b] = bb[p++];
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(BB);
            else {
              tFields.replace(n, "byte["+dim1+"]["+le+"]");
              vFields.put(n, BB);
            }
            continue;
        case "[[[B":
            byte BBB[][][] = new byte[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) BBB[a][b][c] = bb[p++];
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(BBB);
            else {
              tFields.replace(n, "byte["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, BBB);
            }
            continue;
        case "[C":
            char C[] = new char[le];
            for (int a = 0; a < le; ++a) {
              C[a] = ByteBuffer.wrap(bb, p, 2).getChar();
              p += 2;
            }
            if (array) pojo.add(C);
            else {
              tFields.replace(n, "char["+le+"]");
              vFields.put(n, C); 
            }
            continue;
        case "[[C":
            char CC[][] = new char[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) {
                CC[a][b] = ByteBuffer.wrap(bb, p, 2).getChar();
                p += 2;
              }
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(CC);
            else {
              tFields.replace(n, "char["+dim1+"]["+le+"]");
              vFields.put(n, CC);
            }
            continue;
        case "[[[C":
            char CCC[][][] = new char[dim1][dim2][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < dim2; ++b) {
                for (int c = 0; c < le; ++c) {
                  CCC[a][b][c] = ByteBuffer.wrap(bb, p, 2).getChar();
                  p += 2;
                }
              }
              p += 10;
            }
            p -= 20;
            if (array) pojo.add(CCC);
            else {
              tFields.replace(n, "char["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, CCC);
            }
            continue;
        case "[Z":
            boolean Z[] = new boolean[le];
            for (int a = 0; a < le; ++a) Z[a] = (int)bb[p++] == 1;
            if (array) pojo.add(Z);
            else {
              tFields.replace(n, "boolean["+le+"]");
              vFields.put(n, Z);
            }
            continue;
        case"[[Z":
            boolean ZZ[][] = new boolean[dim1][le];
            for (int a = 0; a < dim1; ++a) {
              for (int b = 0; b < le; ++b) ZZ[a][b] = (int)bb[p++] == 1;
              p += 10;
            }
            p -= 10;
            if (array) pojo.add(ZZ);
            else {
              tFields.replace(n, "boolean["+dim1+"]["+le+"]");
              vFields.put(n, ZZ);
            }
            continue;
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
            if (array) pojo.add(ZZZ);
            else {
              tFields.replace(n, "boolean["+dim1+"]["+dim2+"]["+le+"]");
              vFields.put(n, ZZZ);
            }
            continue;
        default:
          int len = le;
          int d1 = dim1;
          int d2 = dim2;
          t = t.replace(";", "");
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
          if (t.equals("[L"+nT)) {
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
          } else if (t.equals("[[L"+nT)) {
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
  private int getArrayType(String name) {
    int q = p;
    if (bb[p] == (byte)0x75 && bb[p+1] == (byte)0x72) {
      q = p+2;
      int le = ((int)(bb[q++]&0xFF)<<8)|(int)(bb[q++]&0xFF);
      p = q+le;
      if (bb[p] != (byte)0x00) {
        while (p < bb.length)
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
          if (bb[p] != (byte)0x75 && bb[p+1] != (byte)0x71) {
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
    for (; p < bb.length; ++p) {
      if (bb[p] == (byte)0x78 && bb[p+1] == (byte)0x70) {
        p += 2;
        if (bb[p+4] == (byte)0x75 && bb[p+5] == (byte)0x71) {
          for (; p < bb.length; p += 6) {
            if (dim1 < 0) dim1 = getInt();
            else if (dim2 < 0) dim2 = getInt();
            else le = getInt();
            if (bb[p] != (byte)0x75 && bb[p+1] != (byte)0x71) {
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
