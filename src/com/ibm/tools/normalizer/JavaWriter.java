/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/normalizer/Attic/JavaWriter.java,v $ 
 * $Date: 2000/07/12 16:41:26 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.tools.normalizer;

//import com.ibm.text.*;
import com.ibm.util.Utility;
import com.ibm.util.CompactCharArray;
import com.ibm.util.CompactByteArray;
import java.io.*;

/**
 * JavaWriter knows how to write data structures out to a Java source file
 */
class JavaWriter extends SourceWriter {
    PrintWriter out;

    public JavaWriter(String name) throws FileNotFoundException {
        // Find the class name
        int cIndex = name.lastIndexOf('/');
        String cName = (cIndex >= 0) ? name.substring(cIndex+1) : name;

        out = new PrintWriter(new FileOutputStream(name + ".java"));

        writeHeader(out);
        out.println("class " + cName + " {");
    }

    public void close() {
        out.println("}");
        out.close();
        out = null;
    }

    public void write(String name, short value) {
        out.println("    static final short " + name + " = " + value + ";");
    }

    public void write(String name, int value) {
        out.println("    static final int " + name + " = " + value + ";");
    }

    public void writeHex(String name, char value) {
        out.println("    static final char " + name + " = 0x" + Integer.toString((int)value,16) + ";");
    }

    public void writeHex(String name, int value) {
        out.println("    static final int " + name + " = 0x" + Integer.toString(value,16) + ";");
    }

    public void write(String name, CompactCharArray array) {
        array.compact(false);
        out.println("");
        out.println("    static final CompactCharArray " + name + " = new CompactCharArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getIndexArray())));
        out.println("        ," );
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getValueArray())));
        out.println("        );" );
    }

    public void write(String name, CompactByteArray array) {
        array.compact(false);
        out.println("");
        out.println("    static final CompactByteArray " + name + " = new CompactByteArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getIndexArray())));
        out.println("        ," );
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array.getValueArray())));
        out.println("        );" );
    }

    public void write(String name, StringBuffer str) {
        out.println("");
        out.println("    static final String " + name + " = ");
        out.println(Utility.formatForSource(str.toString()));
        out.println("    ;");
    }

    public void write(String name, char[] array) {
        out.println("");
        out.println("    static final char[] " + name + " = Utility.RLEStringToCharArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array)));
        out.println("    );");
    }

    public void write(String name, int[] array) {
        out.println("");
        out.println("    static final int[] " + name + " = Utility.RLEStringToIntArray(");
        out.println(Utility.formatForSource(Utility.arrayToRLEString(array)));
        out.println("    );");
    }

    void writeHeader(PrintWriter out) {
        super.writeHeader(out);
        out.println("");
        out.println("package com.ibm.text;");
        out.println("import com.ibm.util.*;");
        out.println("");
    }
}

