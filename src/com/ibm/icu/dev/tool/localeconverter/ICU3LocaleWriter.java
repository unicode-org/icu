/**
*******************************************************************************
* Copyright (C) 2002-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.dev.tool.localeconverter;
import java.io.*;
import java.util.*;
/**
 * @author ram
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ICU3LocaleWriter extends LocaleWriter {
    public ICU3LocaleWriter(PrintStream out) {
        super(out);
    }
    private ListResourceBundle bundle = null;
    protected void open(Locale locale) {
        print(locale.toString());
        println(" {");
        indent();
    }
    public ICU3LocaleWriter(PrintStream out, PrintStream err) {
        super(out, err);
    }
    public ICU3LocaleWriter(ListResourceBundle bundle,PrintStream out, PrintStream err) {
        super(out, err);
        this.bundle = bundle;
    }
    public void write(Locale locale){
        open(locale);
        Enumeration keys = bundle.getKeys();
        while(keys.hasMoreElements()){
            String key = (String) keys.nextElement();
            Object o = bundle.getObject(key);
            write(key, o);
        }
        close();
    }
    public void write(String key, Object o){
        if(key!=null) print(key);
        if(o instanceof String){
            write((String) o);
        }else if( o instanceof Integer[]){
            write((Integer[]) o);
        }else if(o instanceof byte[]){
            write((byte[]) o);
        }else if(o instanceof Integer){
            write((Integer) o);
        }else if(o instanceof Object[][]){
            write((Object[][]) o);
        }else if(o instanceof Object[]){
            write((Object[]) o);
        }
    }
    protected void write(String str){
        print("{");
        printString(str);
        println("}");
    }
    protected void write(Integer[] o){
        println(":intvector { ");
        indent();
        if(o!=null){
            for(int i=0; i< o.length; i++){
                print(o[i].toString());
                println(",");
            }
        }
        outdent();
        println(" }");   
    }
    private String toHex(byte b){
        int i =(int)((char) (b & 0xFF));
        String temp = Integer.toHexString(i);    
        if(temp.length()<2){
            return "0"+temp;
        }else{
            return temp;
        }
    }
        
    protected void write(byte[] o){
        print(":bin{ ");
        indent();
        if(o!=null){
            for(int i=0; i< o.length; i++){
                print(toHex(o[i]));
            }
        }
        outdent();
        println(" }"); 
    }
    protected void write(Integer o){
        print(":int { ");
        indent();
        if(o!=null){
            print(o.toString());
        }
        outdent();
        println(" }");
    }
    protected void write(Object[] o){
        String key = null;
        println(":array { ");
        indent();
        for(int i=0; i<o.length;i++){
            if(o[i] instanceof String){
                printString((String)o[i]);
            }else{
                write(key,o[i]);   
            }
            println(",");
        }
        outdent();
        println(" }");
    }
    protected void write(Object[][] o){
        //String key = null;
        println(":table {");
        indent();
        for(int i=0; i< o.length;i++){

            if(o[i][1] instanceof String){
                print((String) o[i][0]);
                print("{");
                printString((String) o[i][1]);
                println("}");
            }else{
                write((String) o[i][0], o[i][1]);
                println("");
            }

        }   
        outdent();
        println(" }");
    }
    ///////////////////////////////////////////////////
    /// Only for compatibility with super class 
    /// not required otherwise
    //////////////////////////////////////////////////
    
    /////// BEGIN //////////////////
    
    protected void write(String tag, String value) {
        print(tag);
        print(" { ");
        printString(value);
        println(" }");
    }
    

    protected void write(String tag, String[] value) {
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
    //////////////////// END //////////////////////////////////
    protected void close() {
        outdent();
        println("}");
       // super.closeFileHandle();
    }

    protected String getStringJoiningCharacter() {
        return "";
    }
    
}
