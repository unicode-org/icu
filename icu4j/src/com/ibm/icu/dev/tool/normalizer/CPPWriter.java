package com.ibm.tools.normalizer;

import com.ibm.text.*;
import com.ibm.util.*;
import com.ibm.util.CompactByteArray;
import com.ibm.util.CompactCharArray;
import java.io.*;

/**
 * CPPWriter knows how to write data structures out to C++ source and header files
 */
class CPPWriter extends SourceWriter {
    PrintStream source;
    PrintStream header;

    String className;

    public CPPWriter(String fileName, String cName) throws FileNotFoundException {
        className = cName;

        // Find the class name
        header = new PrintStream(new FileOutputStream(fileName + ".h"));
        header.println(kHeader);
        header.println("#include \"ptypes.h\"");
        header.println("#include \"cmpbytea.h\"");
        header.println("#include \"cmpchara.h\"");
        header.println("");
        header.println("struct " + className + " {");   // "struct" makes everything public

        source = new PrintStream(new FileOutputStream(fileName + ".cpp"));
        source.println(kHeader);
        source.println("#include \"" + fileName + ".h\" ");
        source.println("\n");
    }

    public void close() {
        header.println("};");
        header.close();
        source.close();
        header = null;
        source = null;
    }

    public void write(String name, short value) {
        header.println("    enum { " + name + " = " + value + " };");
    }

    public void write(String name, int value) {
        header.println("    enum { " + name + " = " + value + " };");
    }

    public void writeHex(String name, char value) {
        header.println("    enum { " + name + " = 0x" + UInfo.hex(value) + " };");
    }

    public void writeHex(String name, int value) {
        header.println("    enum { " + name + " = 0x" + Integer.toString(value,16) + " };");
    }

    public void write(String name, CompactCharArray array) {
        array.compact(false);

        String indexName = name + "_index";
        String valueName = name + "_values";

        write(indexName, array.getIndexArray());
        write(valueName, array.getValueArray());

        header.println("");
        header.println("    static const CompactCharArray " + name + ";");

        source.println("");
        source.println("const CompactCharArray " + className + "::" + name + "("
                        + indexName + "," + valueName
                        + ", " + array.getValueArray().length + ");" );
    }

    public void write(String name, CompactByteArray array) {
        array.compact(false);

        String indexName = name + "_index";
        String valueName = name + "_values";

        write(indexName, array.getIndexArray());
        write(valueName, array.getValueArray());

        header.println("");
        header.println("    static CompactByteArray " + name + ";");

        source.println("");
        // TODO: add "const" here when CompactByteArray::get is made const
        source.println("CompactByteArray " + className + "::" + name + "("
                        + "(UniChar*)" + indexName + ", (t_int8*)" + valueName
                        + ", " + array.getValueArray().length + ");" );
    }

    public void write(String name, StringBuffer str) {
        write(name, str.toString().toCharArray());
    }

    public void write(String name, char[] array) {
        header.println("");
        header.println("    static const UniChar " + name + "[];");

        source.println("");
        source.println("const UniChar " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print("\n    ");
            }
            source.print("0x" + UInfo.hex(array[i]) + ", ");
        }
        source.println("};");
    }

    public void write(String name, short[] array) {
        header.println("");
        header.println("    static const t_uint16 " + name + "[];");

        source.println("");
        source.println("const t_uint16 " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print("\n    ");
            }
            source.print("0x" + UInfo.hex((char)array[i]) + ", ");
        }
        source.println("};");
    }

    public void write(String name, int[] array) {
        header.println("");
        header.println("    static const t_int32 " + name + "[];");

        source.println("");
        source.println("const t_int32 " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print("\n    ");
            }
            source.print("0x" + Integer.toString(array[i],16) + ", ");
        }
        source.println("};");
    }

    public void write(String name, byte[] array) {
        header.println("");
        header.println("    static const t_int8 " + name + "[];");

        source.println("");
        source.println("const t_int8 " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print("\n    ");
            }
            int value = array[i];
            if (value < 0) value += 256;
            source.print("(t_int8)0x" + Integer.toString(value,16) + ", ");
        }
        source.println("};");
    }

    static final String kHeader =
         "/*\n"
        +" * @(#)$RCSFile$ $Revision: 1.1 $ $Date: 2000/02/10 06:25:54 $\n"
        +" *\n"
        +" * (C) Copyright IBM Corp. 1997-1998 - All Rights Reserved\n"
        +" *\n"
        +" * The program is provided 'as is' without any warranty express or\n"
        +" * implied, including the warranty of non-infringement and the implied\n"
        +" * warranties of merchantibility and fitness for a particular purpose.\n"
        +" * IBM will not be liable for any damages suffered by you as a result\n"
        +" * of using the Program. In no event will IBM be liable for any\n"
        +" * special, indirect or consequential damages or lost profits even if\n"
        +" * IBM has been advised of the possibility of their occurrence. IBM\n"
        +" * will not be liable for any third party claims against you.\n"
        +" *\n"
        +" * This class is MACHINE GENERATED.  Run NormalizerBuilder to regenerate.\n"
        +" */\n"
        +"\n";
}
