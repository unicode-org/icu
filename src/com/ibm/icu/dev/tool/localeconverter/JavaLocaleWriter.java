/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

public class JavaLocaleWriter extends LocaleWriter {
    public JavaLocaleWriter(PrintStream out) {
        super(out);
    }
    public JavaLocaleWriter(PrintStream out, PrintStream err) {
        super(out, err);
    }
    public void open(Locale locale) {
        println(HEADER);
        indent(3);
    }
    protected void write(String tag, Object o) {
        if ("collations".equals(tag)) {
             writeTagged(tag,(Object[][])o);
        } else if (!(o instanceof CollationItem[])) {
            super.write(tag, o);
        } else {
            CollationItem[] items = (CollationItem[])o;
            print("{ ");
            printString("collations");
            println(", ");
            for (int i = 0; i < items.length; i++) {
                if(items[i]!=null){
                    printString(items[i].toString());
                    if (items[i].comment != null) {
                        print("+");
                        tabTo(30);
                        print("//");
                        println(items[i].comment);
                    } else {
                        println("+");
                    }
                }
            }
            println("\"\"");
            println(" },");
        }
    }

    public void write(String tag, String value) {
        print("{ ");
        printString(tag);
        print(", ");
        printString(value);
        println(" },");
    }
    public void write(String tag, String[] value) {
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
    public void write2D(String tag, String[][] value) {
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
    public void writeTagged(String tag, String[][] value) {
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
    public void writeTagged(String tag, Object[][] value) {
        print("{ ");
        printString(tag);
        println(",");
        indent();
            println("new String[][] {");
            indent();
                for (int i = 0; i < value.length; i++) {
                    write((String)value[i][0], value[i][1]);
                }
            outdent();
            println("}");
        outdent();
        println("},");
    }
    public void close() {
        outdent(3);
        print(FOOTER);
        println("");
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
    
    private static final String HEADER = 
        "package java.text.resources;\n"+
        "import java.util.ListResourceBundle;\n"+
        "public class TestLocaleElements extends ListResourceBundle {\n"+
        "    public Object[][] getContents() {\n"+
        "        return new Object[][] {";
        
    private static final String FOOTER = 
        "        };\n"+
        "    }\n"+
        "}";
    //{{DECLARE_CONTROLS
    //}}
}
