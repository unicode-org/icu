/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

public class Java1LocaleWriter extends LocaleWriter {
    public Java1LocaleWriter(PrintStream out) {
        super(out);
            //{{INIT_CONTROLS
        //}}
    }
    
    public Java1LocaleWriter(PrintStream out, PrintStream err) {
        super(out, err);
    }
    
    public void write(Locale locale, Hashtable localeData) {
        try {
            Hashtable temp = new NeutralToJ1Converter(locale).convert(localeData);
            super.write(locale, temp);
        } catch (LocaleConverter.ConversionError e) {
            err.println(e);
        }
    }

    protected void open(Locale locale) {
        print(HEADER1);
        print(locale.toString());
        print(HEADER2);
        print(locale.toString());
        println(HEADER3);
        indent(3);
    }
    protected void write(String tag, String value) {
        print("{ ");
        printString(tag);
        print(", ");
        printString(value);
        println(" },");
    }
    protected void write(String tag, String[] value) {
        print("{ ");
        if (tag != null) {
            printString(tag);
            println(",");
        } else {
            println("");
        }
        indent();
            println("new String[] {");
            indent();
                for (int i = 0; i < value.length; i++) {
                    printString(value[i]);
                    println(",");
                }
            outdent();
            println("}");
        outdent();
        println("},");
    }
    protected void write2D(String tag, String[][] value) {
        print("{ ");
        printString(tag);
        println(",");
        indent();
            println("new String[][] {");
            indent();
                for (int i = 0; i < value.length; i++) {
                    println("{");
                    indent();
                    for (int j = 0; j < value[i].length; j++) {
                        printString(value[i][j]);
                        println(",");
                    }
                    outdent();
                    println("},");
                }
            outdent();
            println("}");
        outdent();
        println("},");
    }
    protected void writeTagged(String tag, String[][] value) {
        print("{ ");
        printString(tag);
        println(",");
        indent();
            println("new String[][] {");
            indent();
                for (int i = 0; i < value.length; i++) {
                    write(value[i][0], value[i][1]);
                }
            outdent();
            println("}");
        outdent();
        println("},");
    }
    protected void close() {
        outdent(3);
        print(FOOTER);
    }

    protected void appendEscapedChar(char c, StringBuffer buffer) {
        if (c < '\u0020' || c == '"' || c == '\\') {
            buffer.append('\\');
            buffer.append(HEX_DIGIT[(c & 0700) >> 6]); // HEX_DIGIT works for octal
            buffer.append(HEX_DIGIT[(c & 0070) >> 3]);
            buffer.append(HEX_DIGIT[(c & 0007)]);
        } else {
            super.appendEscapedChar(c, buffer);
        }
    }

    protected String getStringJoiningCharacter() {
        return "+";
    }
    
    private static final String HEADER1 = 
            "package java.text.resources;\n"+
            "public class LocaleElements_";
    private static final String HEADER2 = 
            " extends LocaleData {\n"+
            "    public LocaleElements_";
    private static final String HEADER3 = 
        "() {\n"+
        "        super.init(table);\n"+
        "    }\n"+
        "    static String table []=";
        
    private static final String FOOTER = 
        "    }\n"+
        "}";
    //{{DECLARE_CONTROLS
    //}}
}
