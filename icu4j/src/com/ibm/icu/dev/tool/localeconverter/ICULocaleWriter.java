/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

public class ICULocaleWriter extends LocaleWriter {
    public ICULocaleWriter(PrintStream out) {
        super(out);
    }
    public ICULocaleWriter(PrintStream out, PrintStream err) {
        super(out, err);
    }
    protected void open(Locale locale) {
        print(locale.toString());
        println(" {");
        indent();
    }
    protected void write(String tag, Object o) {
        if ("collations".equals(tag)) {
            if(o instanceof Object[][]){
                writeTagged(tag,(Object[][])o);
            }else{
               write(tag,(String)o);
            }
        } else if (!(o instanceof CollationItem[])) {
            super.write(tag, o);
        } else {
            CollationItem[] items = (CollationItem[])o;
            if(items[0]!=null){
            print("Sequence");
            println(" { ");
            for (int i = 0; i < items.length; i++) {
                if(items[i]!=null){
                    printString(items[i].toString());
                    if (items[i].comment != null) {
                        tabTo(30);
                        print("//");
                        println(items[i].comment);
                    }
                }
            }
            println("}");
            }
        }
    }
    protected void write(String tag, String value) {
        print(tag);
        print(" { ");
        printString(value);
        println(" }");
    }
    protected void writeIntVector(String tag, String[] value) {
        if (tag != null) {
            print(tag);
            println(":intvector { ");
        } else {
            println(":intvector{");
        }
        indent();
            for (int i = 0; i < value.length; i++) {
                printUnquotedString(value[i]);
                println(",");
            }
        outdent();
        println("}");
    }
    protected void write(String tag, String[] value) {
        if(tag !=null && tag.equals("DateTimeElements")){
            writeIntVector(tag,value);
            return;
        }
        if (tag != null) {
            print(tag);
            println(" { ");
        } else {
            println("{");
        }
        indent();
            for (int i = 0; i < value.length; i++) {
                printString(value[i]);
                println(",");
            }
        outdent();
        println("}");
    }
    protected void write2D(String tag, String[][] value) {
        print(tag);
        println(" { ");
        indent();
            for (int i = 0; i < value.length; i++) {
                write(null, value[i]);
            }
        outdent();
        println("}");
    }
    protected void writeTagged(String tag, Object[][] value) {
        print(tag);
        println(" { ");
        indent();
            for (int i = 0; i < value.length; i++) {
                write((String)value[i][0], value[i][1]);
            }
        outdent();
        println("}");
    }
    protected void writeTagged(String tag, String[][] value) {
        print(tag);
        println(" { ");
        indent();
            for (int i = 0; i < value.length; i++) {
                write(value[i][0], value[i][1]);
            }
        outdent();
        println("}");
    }
    protected void close() {
        outdent();
        println("}");
       // super.closeFileHandle();
    }

    protected String getStringJoiningCharacter() {
        return "";
    }
}
