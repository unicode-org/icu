/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

import com.ibm.icu.impl.UCharacterProperty;

/**
 * A LocaleWriter takes locale data in standard form and
 * writes it to standard output in a form suitable for
 * loading programatically.
 */
public abstract class LocaleWriter {
    private static final String INDENT_CHARS = 
        "                                        "+
        "                                        "+
        "                                        ";
    private static final char EOL_CHARS[] = {'\r', '\n', '\u2028', '\u2029'};
        
    private static final int INDENT_SIZE = 4;
    private static final int MAX_LINE_LENGTH = 80;
    private int indentLevel;
    private String indentString;
    private boolean needsIndent;
    protected StringBuffer lineBuffer = new StringBuffer();
    private int lineLength;
    protected PrintStream out;
    protected PrintStream err;
    //final File outFile = new File(  "cnvLoc.txt");
    //FileOutputStream outFileStream;
    //BufferedWriter outBufWrite;
    //PrintWriter myOut;
    
    public LocaleWriter(PrintStream out) {
        this.out = out;
        this.err = out;
    /*    try{
            outFile.canWrite();
            outFileStream = new FileOutputStream(outFile);
            outBufWrite = new BufferedWriter(new OutputStreamWriter(outFileStream,"UTF8"));                    
        }
        catch(java.io.IOException e){
            System.out.println("Encoding unsupported");
            return;
        }
    */  
    }
    public LocaleWriter(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        /*
        try{
            outFile.canWrite();
            outFileStream = new FileOutputStream(outFile);
            outBufWrite = new BufferedWriter(new OutputStreamWriter(outFileStream,"UTF8"));                    
        }
        catch(java.io.IOException e){
            System.out.println("Encoding unsupported");
            return;
        }
        */  
    }

    public void write(Locale locale, Hashtable localeData) {
        open(locale);
            //sort the key so the tags are in order in the resource file
        SortedVector order = new SortedVector(localeData.keys(), new SortedVector.StringComparator());
        Enumeration e = order.elements();
        while (e.hasMoreElements()) {
            final String key = (String)e.nextElement();
            final Object data = localeData.get(key);
            if (isDuplicateOfInheritedValue(locale, key, data)) {
               println("///* Discarding duplicate data for tag: "+key+" */");
            } else {
                write(key, data);
            }
        }
       
        close();
    }
    /*
    public void closeFileHandle(){
         try{
            outBufWrite.close();
        }
        catch(java.io.IOException excp){
            out.println("could not close the output file");
        }
    }
    */    
    protected void write(String tag, Object o) {
        if (o instanceof String) {
            write(tag, (String)o);
        } else if (o instanceof String[]) {
            write(tag, (String[]) o);
        } else if (o instanceof String[][]) {
            write(tag, (String[][])o);
        } else if (o instanceof Object[]) {
            Object[] data = (Object[])o;
            String[] temp = new String[data.length];
            for (int i = 0; i < data.length; i++) {
                temp[i] = data[i].toString();
            }
            write(tag, temp);
        } else if (o instanceof Object[][]) {
            Object[][] data = (Object[][])o;
            String[][] temp = new String[data.length][];
            for (int i = 0; i < data.length; i++) {
                temp[i] = new String[data[i].length];
                for (int j = 0; j < temp[i].length; j++) {
                    temp[i][j] = data[i][j].toString();
                }
            }
            write(tag, temp);
        } else {
            write(tag, o.toString());
        }
    }
    protected final void write(String tag, String[][] value) {
        if (value.length > 0) {
            if (value[0].length > 2) {
                write2D(tag, value);
            } else {
                writeTagged(tag, value);
            }
        } else {
            writeTagged(tag, value);
        }
    }

    protected abstract void open(Locale locale);
    protected abstract void write(String tag, String value);
    protected abstract void write(String tag, String[] value);
    protected abstract void write2D(String tag, String[][] value);
    protected abstract void writeTagged(String tag, String[][] value);
    protected abstract void close();
    protected abstract String getStringJoiningCharacter();

    protected void tabTo(int pos) {
        if (pos > lineLength) {
            for (int i = lineLength; i < pos; i++) {
                print(" ");
            }
        }
    }
    /*
    protected void writeToFile(String str){
                
        try{
               outBufWrite.write(prependEsc(str));
        }
        catch(java.io.IOException e){
               out.println("Could not write to file");
        }
    }
    */
    protected void print(String val) {
        if (needsIndent) {
            out.print(indentString);
            //writeToFile(indentString);
            lineLength += indentString.length();
            needsIndent = false;
        }
        String tval = PosixCollationBuilder.unescape(val);
        if(tval.length() < val.length()){
            tval=prependEsc(tval);
        }
        if (tval != null) {
            out.print(prependEsc(tval));
            //writeToFile(tval);
            int len = 0;
            for (int i = 0; i < EOL_CHARS.length; i++) {
                len = Math.max(len, tval.lastIndexOf(EOL_CHARS[i]));
            }
            if (len == 0) {
                lineLength += tval.length();
            } else {
                lineLength = tval.length() - len;
            }
        }
    }
    protected String prependEsc(String str){
        StringBuffer myStr =  new StringBuffer();
        for(int i=0;i<str.length();i++){
            char ch = str.charAt(i);
            if(ch > 0x007f || ch < 0x0020){
                if(ch!=0x0009){
                    myStr.append("\\u");
                    myStr.append(toHexString(ch,16,4));
                }else{
                    myStr.append(ch);
                }
            }
            else{
                myStr.append(ch);
            }
        }
        return myStr.toString();
    }
    protected String toHexString(char ch, int radix, int pad){
        final int MAX_DIGITS = 10;
        int length = 0;
        char buffer[] = new char[10];
        int num = 0;
        int digit;
        int j;
        char temp;
        int i = (int)ch;
        do{
            digit = (int)(i % radix);
            buffer[length++]=(char)(digit<=9?(0x0030+digit):(0x0030+digit+7));
            i=(i/radix);
        }while(i>0);

        while (length < pad){   
            buffer[length++] =  0x0030;/*zero padding */
        }
        /* null terminate the buffer */
        if(length<MAX_DIGITS){
            buffer[length] =  0x0000;
        }
        num= (pad>=length) ? pad :length;
              
        /* Reverses the string */
        for (j = 0; j < (num / 2); j++){
            temp = buffer[(length-1) - j];
            buffer[(length-1) - j] = buffer[j];
            buffer[j] = temp;
            }
        return new String(buffer,0,length);
    }
        
    protected void println(String val) {
        print(val);
        out.println();
        //writeToFile("\n");
        lineLength = 0;
        needsIndent = true;
    }

    protected void printString(String val) {
        if (val != null) {
            indent();
            lineBuffer.setLength(0);
            lineBuffer.append("\"");
            final int size = val.length();
            for (int i = 0; i < size; i++) {
                append(val.charAt(i));
                /*if (!append(val.charAt(i))) {
                        lineBuffer.append("\"");
                        lineBuffer.append(getStringJoiningCharacter());
                        println(lineBuffer.toString());
                        lineBuffer.setLength(0);
                        lineBuffer.append("\"");
                }*/
            }
            
            lineBuffer.append("\"");
            print(lineBuffer.toString());
            outdent();
        } else {
            print("\"\"");
        }
    }
    private boolean isSpecialChar(char ch){
                 
        if(((((ch) <= 0x002F) && ((ch) >= 0x0020)) || 
          (((ch) <= 0x003F) && ((ch) >= 0x003A)) || 
          (((ch) <= 0x0060) && ((ch) >= 0x005B)) || 
          (((ch) <= 0x007E) && ((ch) >= 0x007D)) || 
          (ch) == 0x007B)){
            return true;
          }
          return false;
    }
    protected void printRuleString(String src){
     String val = PosixCollationBuilder.unescape(src);
     if (val != null) {
            indent();
            lineBuffer.setLength(0);
            lineBuffer.append("\"");
            final int size = val.length();
            for (int i = 0; i < size; i++) {
                char ch = val.charAt(i);
                if(ch=='\\'){ //escape char
                    if(((i+1) < val.length())){
                        char c2 = val.charAt(i+1);
                        if(isSpecialChar(c2)){
                            // escape the escape and escape the 
                            // special char
                            append('\\');
                            append('\\');
                            append('\\');
                            append(c2);
                        }else{
                            // write the sequence
                            append('\\');
                            append(c2);
                        }
                    }else{
                       // double escape the escape sequence
                       append('\\');
                       append('\\');
                    }
                    i++;
                }else{
                    if(UCharacterProperty.isRuleWhiteSpace(ch)){
                        append('\\');
                    }
                    
                    append(ch);
                }
 
            }
            
            lineBuffer.append("\"");
            print(lineBuffer.toString());
            outdent();
        } else {
            print("\"\"");
        }
    }
    protected void printUnquotedString(String val) {
         if (val != null) {
             indent();
             lineBuffer.setLength(0);
             //lineBuffer.append("\"");
             final int size = val.length();
             for (int i = 0; i < size; i++) {
                 append(val.charAt(i));
                 /*if (!append(val.charAt(i))) {
                         lineBuffer.append("\"");
                         lineBuffer.append(getStringJoiningCharacter());
                         println(lineBuffer.toString());
                         lineBuffer.setLength(0);
                         lineBuffer.append("\"");
                 }*/
             }
            
             //lineBuffer.append("\"");
             print(lineBuffer.toString());
             outdent();
         } else {
             print("");
         }
     }      
    protected boolean append(final char c) {
        boolean escape = isEscapeChar(c);
        if (escape) {
            appendEscapedChar(c, lineBuffer);
        } else {
            lineBuffer.append(c);
        }
        return (lineLength + lineBuffer.length() < MAX_LINE_LENGTH);
    }
    
    protected boolean isEscapeChar(final char c) {
        switch (c) {
        case '"':
        case '\\':
        case '\n':
        case '\r':
        case '\u2028':
        case '\u2029':
            return true;
        default:
            return (c < ' ') || (c > 0x07F);
        }
    }
    
    protected void appendEscapedChar(char c, StringBuffer buffer) {
        if(c>=0x20 && c < 0x7f){
            buffer.append(c);
        }else{
            buffer.append(getEscapeChar());
            int value = ((int)c) & 0xFFFF;
            buffer.append(HEX_DIGIT[(value & 0xF000) >> 12]);
            buffer.append(HEX_DIGIT[(value & 0x0F00) >> 8]);
            buffer.append(HEX_DIGIT[(value & 0x00F0) >> 4]);
            buffer.append(HEX_DIGIT[(value & 0x000F)]);
        }
    }
    
    protected String getEscapeChar() {
        return "\\u";
    }
    
    protected final void indent() {
        indent(1);
    }
    protected void indent(int amount) {
        indentLevel += amount;
        indentString = INDENT_CHARS.substring(0, indentLevel*INDENT_SIZE);
    }
    
    protected final void outdent() {
        outdent(1);
    }
    protected void outdent(int amount) {
        indentLevel -= amount;
        indentString = INDENT_CHARS.substring(0, indentLevel*INDENT_SIZE);
    }

    static final char[] HEX_DIGIT = {'0','1','2','3','4','5','6','7',
                     '8','9','A','B','C','D','E','F'};

    /** Return true if the value for the specified tag is the same
     * as the value inherited from the parent for that tag */
    private boolean isDuplicateOfInheritedValue(final Locale loc, String tag, Object value) {
        if (value == null) return true;
        try {
            final ResourceBundle parentBundle = getParentBundle(loc);
            if (parentBundle == null) return false;
            Object parentValue = parentBundle.getObject(tag);
            if (!objectsAreEqual(value, parentValue)) {
                return false;
            } else {
                return true;
            }
        } catch (java.util.MissingResourceException e) {
            return false;
        }
    }
    
    private boolean objectsAreEqual(final Object item, final Object parentItem) {
        if (item instanceof Object[] && parentItem instanceof Object[]) {
            return arraysAreEqual((Object[])item, (Object[])parentItem);
        } else {
            return item.equals(parentItem);
        }
    }
    
    private boolean arraysAreEqual(final Object[] item, final Object[] parentItem) {
        boolean matches = item.length == parentItem.length;
        for (int i = 0; i < item.length && matches; i++) {
            matches = objectsAreEqual(item[i], parentItem[i]);
        }
        return matches;
    }
        
    private ResourceBundle getParentBundle(final Locale loc) {
        try {
            final String x = loc.toString();
            final int ndx = x.lastIndexOf('_');
            if (ndx < 0) {
                return null;
            } else {
                final String parentLocName = x.substring(0, ndx);
                final Locale parentLoc = localeFromString(parentLocName);
                return ResourceBundle.getBundle("com.ibm.icu.dev.tool.localeconverter.myLocaleElements", parentLoc);
            }
        } catch (MissingResourceException e) {
            return null;
        }
    }
    
    private String replace(String source, String target, String replacement) {
        if (target.equals(replacement)) {
            return source;
        } else {
            StringBuffer result = new StringBuffer();
            int lastNdx = 0;
            int ndx = source.indexOf(target);
            while (ndx >= 0) {
                result.append(source.substring(lastNdx, ndx));
                result.append(replacement);
                ndx += target.length();
                lastNdx = ndx;
                ndx = source.indexOf(target, ndx);
            }
            result.append(source.substring(lastNdx));
            return result.toString();
        }
    }

    public Locale localeFromString(final String localeName) {
        String language = localeName;
        String country = "";
        String variant = "";
        
        int ndx = language.indexOf('_');
        if (ndx >= 0) {
            country = language.substring(ndx+1);
            language = language.substring(0, ndx);
        }
        ndx = country.indexOf('_');
        if (ndx >= 0) {
            variant = country.substring(ndx);
            country = country.substring(0, ndx);
        }
        return new Locale(language, country, variant);
    }
}
