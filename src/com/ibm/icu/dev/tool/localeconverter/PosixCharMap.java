/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;

public class PosixCharMap {
    private Hashtable table = new Hashtable();
    private Hashtable backTable = null;
    private PosixCharMap parentMap;
    private String encoding;
    
    public PosixCharMap() {
    }
    
    public PosixCharMap(PosixCharMap parent) {
        parentMap = parent;
    }

    public PosixCharMap(String fileName) throws IOException {
        this(new FileReader(fileName));
    }

    public PosixCharMap(String pathName, String fileName) throws IOException {
        this(new FileReader(new File(pathName, fileName)));
    }

    public PosixCharMap(Reader inputReader) throws IOException {
        load(new BufferedReader(inputReader));
    }
    
    public PosixCharMap getParent() {
        return parentMap;
    }
    
    public void setParent(PosixCharMap parent) {
        parentMap = parent;
    }

    public void load(String pathName, String fileName) throws IOException {
        load(new File(pathName, fileName),"");
    }
    public void load(String pathName, String fileName, String enc)throws IOException{
        load(new File(pathName, fileName),enc);
    }
    
    public void load(File file, String enc) throws IOException {
        encoding =enc;
        load(new BufferedReader(new FileReader(file)));
    }
    /* This map must be in ASCENDING ORDER OF THE ESCAPE CODE */
    static private final char[] UNESCAPE_MAP = {
        /*"   0x22, 0x22 */
        /*'   0x27, 0x27 */
        /*?   0x3F, 0x3F */
        /*\   0x5C, 0x5C */
        /*a*/ 0x61, 0x07,
        /*b*/ 0x62, 0x08,
        /*f*/ 0x66, 0x0c,
        /*n*/ 0x6E, 0x0a,
        /*r*/ 0x72, 0x0d,
        /*t*/ 0x74, 0x09,
        /*v*/ 0x76, 0x0b
    };
        /**
     * Convert an escape to a 32-bit code point value.  We attempt
     * to parallel the icu4c unesacpeAt() function.
     * @param offset16 an array containing offset to the character
     * <em>after</em> the backslash.  Upon return offset16[0] will
     * be updated to point after the escape sequence.
     * @return character value from 0 to 10FFFF, or -1 on error.
     */
    public static int unescapeAt(String s, int[] offset16) {
        int c;
        int result = 0;
        int n = 0;
        int minDig = 0;
        int maxDig = 0;
        int bitsPerDigit = 4;
        int dig;
        int i;

        /* Check that offset is in range */
        int offset = offset16[0];
        int length = s.length();
        if (offset < 0 || offset >= length) {
            return -1;
        }

        /* Fetch first UChar after '\\' */
        c = UTF16.charAt(s, offset);
        offset += UTF16.getCharCount(c);

        /* Convert hexadecimal and octal escapes */
        switch (c) {
        case 'u':
            minDig = maxDig = 4;
            break;
        case 'U':
            minDig = maxDig = 8;
            break;
        case 'x':
            minDig = 1;
            maxDig = 2;
            break;
        default:
            dig = UCharacter.digit(c, 8);
            if (dig >= 0) {
                minDig = 1;
                maxDig = 3;
                n = 1; /* Already have first octal digit */
                bitsPerDigit = 3;
                result = dig;
            }
            break;
        }
        if (minDig != 0) {
            while (offset < length && n < maxDig) {
                // TEMPORARY
                // TODO: Restore the char32-based code when UCharacter.digit
                // is working (Bug 66).

                //c = UTF16.charAt(s, offset);
                //dig = UCharacter.digit(c, (bitsPerDigit == 3) ? 8 : 16);
                c = s.charAt(offset);
                dig = Character.digit((char)c, (bitsPerDigit == 3) ? 8 : 16);
                if (dig < 0) {
                    break;
                }
                result = (result << bitsPerDigit) | dig;
                //offset += UTF16.getCharCount(c);
                ++offset;
                ++n;
            }
            if (n < minDig) {
                return -1;
            }
            offset16[0] = offset;
            return result;
        }

        /* Convert C-style escapes in table */
        for (i=0; i<UNESCAPE_MAP.length; i+=2) {
            if (c == UNESCAPE_MAP[i]) {
                offset16[0] = offset;
                return UNESCAPE_MAP[i+1];
            } else if (c < UNESCAPE_MAP[i]) {
                break;
            }
        }

        /* If no special forms are recognized, then consider
         * the backslash to generically escape the next character. */
        offset16[0] = offset;
        return c;
    }

    /**
     * Convert all escapes in a given string using unescapeAt().
     * @exception IllegalArgumentException if an invalid escape is
     * seen.
     */
    public static String unescape(String s) {
        StringBuffer buf = new StringBuffer();
        int[] pos = new int[1];
        for (int i=0; i<s.length(); ) {
            char c = s.charAt(i++);
            if (c == '\\') {
                pos[0] = i;
                int e = unescapeAt(s, pos);
                if (e < 0) {
                    throw new IllegalArgumentException("Invalid escape sequence " +
                                                       s.substring(i-1, Math.min(i+8, s.length())));
                }
                UTF16.append(buf, e);
                i = pos[0];
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public void load(Reader inputReader) throws IOException {
        PosixCharMap oldMap = SymbolTransition.getCharMap();
        SymbolTransition.setCharMap(null);
        try {
            final int TOKEN = 1;
            final int EOF = 2;
            final int EOL = 3;
            final int RANGE = 4;
            final Lex.Transition[][] states1 = {
                { //state 0: start
                    new SpaceTransition(0),
                    new EOLTransition(EOL),
                    new Lex.EOFTransition(EOF),
                    new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
                },
                {   //grab first word
                    new Lex.StringTransition(SpaceTransition.SPACE_CHARS, Lex.IGNORE_CONSUME, TOKEN),
                    new Lex.StringTransition(EOLTransition.EOL_CHARS, Lex.IGNORE_CONSUME, TOKEN),
                    new Lex.EOFTransition(TOKEN),
                    new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, -1)
                }
            };

            final Lex.Transition[][] states2 = {
                {   //These states only return <symbols>.  All
                    //other text is ignored.
                    new Lex.EOFTransition(EOF),
                    new EOLTransition(EOL),
                    new SymbolTransition(TOKEN),
                    new SpaceTransition(0),
                    new RangeTransition(RANGE),
                    new Lex.DefaultTransition(Lex.ACCUMULATE_CONSUME, 0)
                },
            };
        
            PushbackReader input = new PushbackReader(inputReader);
            Lex p = new Lex(states1, input);
            int state;
            do {
                state = p.nextToken();
            } while ((state != EOF) && !p.dataEquals("CHARMAP"));
            p.accept(EOL);
            if (state != EOF ) {
                p = new Lex(states2, input);
                state = p.nextToken();
                while (state != EOF ) {

                    String key = p.getData();
                    if(p.dataEquals("ENDCHARMAP")){
                        break;
                    }
                    state = p.nextToken();
                    while (state == EOL) {
                        if(p.dataEquals("ENDCHARMAP")){
                            break;
                        }
                        String data = unescape(p.getData());
                        data.trim();
                        if (data.startsWith("<U") || data.startsWith("#U")) {
                            String numData = data.substring(2,data.length()-1);
                            int digit = Integer.parseInt(numData, 16);
                            defineMapping(key, ""+(char)digit);
                        }else if(data.startsWith("\\x")){
                            byte[] encData = new byte[100];
                            int num = hexToByte(data,encData);
                            String tData = new String(encData,0,num,encoding);
                            defineMapping(key,tData);
                        }else{
                            defineMapping(key,byteToChar(data,encoding));
                        }
                        state = p.nextToken();
                        key=p.getData();
                     }
                     // we come here only if there is a range transition
                     if( state ==RANGE){
                        
                        String begin = key;
                        
                        state = p.nextToken();
                        String end = p.getData();
                        
                        state = p.nextToken();
                        String data = p.getData();
                        data.trim();
                        byte[] encData = new byte[6];
                        int num = hexToByte(data,encData);
                        String tData = new String(encData,0,num,encoding);
                        String stringVal;
                        int[] val = getInt(begin);
                        int beginRange = 0;
                        int endRange = 0;
                        if(val == null){
                            val = getInt((String)table.get(begin));
                            if(val!=null){
                                beginRange = val[1];
                            }
                        }
                        val = getInt(end);                     
                        if(val == null){
                            val = getInt((String)table.get(end));
                            if(val!=null){
                                endRange = val[1];
                            }
                        }
                        stringVal = key.substring(0,val[0]);
                        int digit = (int)(char)tData.charAt(0);
                        while(beginRange <= endRange){
                            defineMapping((stringVal+beginRange+">"),""+(char)digit++);
                            beginRange++;
                        }
                        
                        state = p.nextToken();
                        key=p.getData();
                     }                       
                        
                    //state = p.nextToken();
                }
            }
        } catch (EOFException e) {
        } finally {
            SymbolTransition.setCharMap(oldMap);
        }
    }
    public int[] getInt(String data){
        if(data == null){
            return null;
        }
        int i=0;
        int[] retVal = new int[2];
        int len =data.length();
        while(i< len){
            if((data.charAt(i))-0x30 < (0x39-0x30)){
                break;
            }
            i++;
        }
        if(i < len){
            String sub =data.substring(i,len-1);
            retVal[0] =i;
            retVal[1]=Integer.parseInt(sub,10);
            return retVal;
        }
        return null;
    }
    public int hexToByte(String data, byte[] retval){
        String tData = data;
        int i=0;
        for(i=0;i < data.length()/4; i++){
            if(tData.charAt(0)=='\\' && tData.charAt(1)=='x'){
                String numData = tData.substring(2,4);
                retval[i] = (byte) Integer.parseInt(numData,16);
                tData = tData.substring(4,tData.length());
            }
        }       
        return i;
    }  
    public String byteToChar(String data, String encoding) 
        throws UnsupportedEncodingException{
        
        byte[] bytes = new byte[data.length()];
        for(int i=0; i<data.length(); i++){
            char ch = data.charAt(i);
            if(ch > 0xFF){
                throw new RuntimeException("Bytes in the string are greater than 0xFF");
            }
            bytes[i] = (byte) ch;        
        }
        return new String(bytes,encoding);
    }      
    public void defineMapping(String from, String to) {
        table.put(from, to);
        backTable = null;
    }

    public void undefineMapping(String from) {
        table.remove(from);
        backTable = null;
    }

    public void swap() {
        Hashtable newTable = new Hashtable();
        Enumeration enum = table.keys();
        while (enum.hasMoreElements()) {
            String key = (String)enum.nextElement();
            String code = (String)table.get(key);
            
            String newKey = toSymbol(code);
            String newCode = toLiteral(key);
            String prevCode = (String)newTable.get(newKey);
            if (prevCode == null || prevCode.compareTo(newCode) > 0) {
                newTable.put(newKey, newCode);
            }
        }
        table = newTable;
    }
    
    private String toLiteral(String code) {
        String data = code.substring(2,code.length()-1);
        int digit = Integer.parseInt(data, 16);
        return "" + (char)digit;
    }
    
    private String toSymbol(String code) {
        StringBuffer escapeBuffer = new StringBuffer();
        escapeBuffer.append(">");
        for (int i = 0; i < code.length(); i++) {
            int value = ((int)code.charAt(i)) & 0xFFFF;
            while ((value > 0) || (escapeBuffer.length() < 5)) {
                char digit = Character.forDigit(value % 16, 16);
                escapeBuffer.append(digit);
                value >>= 4;
            }
        }
        escapeBuffer.append("U<");
        escapeBuffer.reverse();
        return escapeBuffer.toString();
    }
    
    public void dump(PrintStream out) {
        StringBuffer escapeBuffer = new StringBuffer();
        Enumeration enum = table.keys();
        while (enum.hasMoreElements()) {
            String key = (String)enum.nextElement();
            String code = (String)table.get(key);
            out.print(key);
            out.print("       <U");
            for (int i = 0; i < code.length(); i++) {
                int value = ((int)code.charAt(i)) & 0xFFFF;
                escapeBuffer.setLength(0);
                while ((value > 0) || (escapeBuffer.length() < 4)) {
                    char digit = Character.forDigit(value % 16, 16);
                    escapeBuffer.append(digit);
                    value >>= 4;
                }
                escapeBuffer.reverse();
                out.print(escapeBuffer.toString());
            }
            out.println(">");
        }
    }

    public String mapKey(final String key) {
        String result = (String)table.get(key);
        if (result == null) {
            if (parentMap != null) {
                result = parentMap.mapKey(key);
            } else {
                result = key;
            }
        }
        return result;
    }
    
    public String backmapValue(final String value) {
        if (backTable == null) {
            backTable = new Hashtable();
            Enumeration enum = table.keys();
            while (enum.hasMoreElements()) {
                String key = (String)enum.nextElement();
                String val = (String)table.get(key);
                backTable.put(val, key);
            }
        }
        String result = (String)backTable.get(value);
        if (result == null) {
            if (parentMap != null) {
                result = parentMap.backmapValue(value);
            } else {
                result = value;
            }
        }
        return result;      
    }

    public Enumeration keys() {
        return table.keys();
    }
    
    public Enumeration elements() {
        return table.elements();
    }
    
    public static void main(String args[]) {
        try {
            PosixCharMap map1 = new PosixCharMap(
                "C:\\projects\\com\\taligent\\localeconverter\\CharMaps",
                "IBM-1129.UPMAP100.txt");
            map1.swap();
            map1.dump(System.out);
        
            SymbolTransition.setCharMap(map1);
            System.out.println(); System.out.println();
        
            //PosixCharMap map = new PosixCharMap("C:\\projects\\data\\ISO-8859-1.html");
            PosixCharMap map = new PosixCharMap(
                "C:\\projects\\com\\taligent\\localeconverter\\CharMaps",
                "ibm1129.txt");
            map.dump(System.out);
            System.out.println();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
