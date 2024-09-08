# Deserialize
Deserialize of JAVA serialized objects.
This API is used to reconstruct a serialized object (via a file or a byte array) without having to know/have its bytecode class. Due to the sheer number of JAVA APIs, this SerObjecrView is limited to primitives (including String), custom serialized objects and ArrayList. Arrays with more than 3 dimensions are also not supported. When working with Java Object DB, the objects are usually simple POJOs (Plain Old Java Object) and with SerObjectView you can access the data of the variables (fields) and process them outside of the object.
- Folder examples contains 3 serializable files: MyList.java, DeMyList.java and Ser_Obj.java
- subfolder serobj of examples contains 2 serialized files: MyList.txt and Ser_obj.txt
Just run "java ViewSerObject serobj/serfile.txt" to see the dump.
