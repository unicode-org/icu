/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/normalizer/Attic/CPPWriter.java,v $ 
 * $Date: 2000/09/21 22:37:55 $ 
 * $Revision: 1.6 $
 *
 *****************************************************************************************
 */
package com.ibm.tools.normalizer;

import com.ibm.text.*;
import com.ibm.util.*;
import com.ibm.util.CompactByteArray;
import com.ibm.util.CompactCharArray;
import com.ibm.util.Utility;
import java.io.*;

/**
 * CPPWriter knows how to write data structures out to C++ source and header files
 */
class CPPWriter extends SourceWriter {
    PrintWriter source;
    PrintWriter header;

    String className;

    public CPPWriter(String fileName, String cName) throws FileNotFoundException {
        className = cName;
        int i = fileName.lastIndexOf('/');
        String name = (i >= 0) ? fileName.substring(i+1) : fileName;

        // Find the class name
        header = new PrintWriter(new FileOutputStream(fileName + ".h"));
        writeHeader(header);
        header.println("#include \"unicode/utypes.h\"");
        header.println("#include \"ucmp8.h\"");
        header.println("#include \"ucmp16.h\"");
        header.println("");
        header.println("struct " + className + " {");   // "struct" makes everything public

        source = new PrintWriter(new FileOutputStream(fileName + ".cpp"));
        writeHeader(source);
        source.println("#include \"" + name + ".h\" ");
        source.println("");
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
        header.println("    enum { " + name + " = 0x" + Utility.hex(value) + " };");
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
        header.println("    static CompactShortArray _" + name + ";");
        header.println("");
        header.println("    static const CompactShortArray* " + name + ";");

        source.println("");
        source.println("CompactShortArray " +
                       className + "::_" + name + ";");
        source.println("");
        source.println("const CompactShortArray* " +
                       className + "::" + name + " = ");
        source.println("    ucmp16_initAliasWithBlockShift(&" + className + "::_" + name + ",");
        source.println("                                   (uint16_t*)" + indexName + ",");
        source.println("                                   (int16_t*)" + valueName + ",");
        source.println("                                   " +
                       array.getValueArray().length + ", " +
                       "0x0000, " + // default value
                       CompactCharArray.BLOCKSHIFT + ");");
    }

    public void write(String name, CompactByteArray array) {
        array.compact(false);

        String indexName = name + "_index";
        String valueName = name + "_values";

        write(indexName, array.getIndexArray());
        write(valueName, array.getValueArray());

        header.println("");
        header.println("    static CompactByteArray _" + name + ";");
        header.println("");
        header.println("    static const CompactByteArray* " + name + ";");

        source.println("");
        source.println("CompactByteArray " +
                       className + "::_" + name + ";");
        source.println("");
        source.println("const CompactByteArray* " +
                       className + "::" + name + " = ");
        source.println("    ucmp8_initAlias(&" + className + "::_" + name + ",");
        source.println("                    (uint16_t*)" + indexName + ",");
        source.println("                    (int8_t*)" + valueName + ",");
        source.println("                    " + array.getValueArray().length + ");");
    }

    public void write(String name, StringBuffer str) {
        write(name, str.toString().toCharArray());
    }

    public void write(String name, char[] array) {
        header.println("");
        header.println("    static const uint16_t " + name + "[];");

        source.println("");
        source.println("const uint16_t " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print(Utility.LINE_SEPARATOR + "    ");
            }
            source.print("0x" + Utility.hex(array[i]) + ", ");
        }
        source.println("};");
    }

    public void write(String name, short[] array) {
        header.println("");
        header.println("    static const uint16_t " + name + "[];");

        source.println("");
        source.println("const uint16_t " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print(Utility.LINE_SEPARATOR + "    ");
            }
            source.print("0x" + Utility.hex((char)array[i]) + ", ");
        }
        source.println("};");
    }

    public void write(String name, int[] array) {
        header.println("");
        header.println("    static const int32_t " + name + "[];");

        source.println("");
        source.println("const int32_t " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print(Utility.LINE_SEPARATOR + "    ");
            }
            source.print("0x" + Integer.toString(array[i],16) + ", ");
        }
        source.println("};");
    }

    public void write(String name, byte[] array) {
        header.println("");
        header.println("    static const uint8_t " + name + "[];");

        source.println("");
        source.println("const uint8_t " + className + "::" + name + "[] = {");

        source.print("    ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && i % 8 == 0) {
                source.print(Utility.LINE_SEPARATOR + "    ");
            }
            source.print("0x" + hex2(array[i]) + ", ");
        }
        source.println("};");
    }

    private static StringBuffer __buf = new StringBuffer();

    // This method not multithread safe!
    private static final String hex2(int x) {
        __buf.setLength(0);
        __buf.append(hex1(x>>4)).append(hex1(x));
        return __buf.toString();
    }

    private static final char hex1(int x) {
        return "0123456789ABCDEF".charAt(x & 0xF);
    }
}
