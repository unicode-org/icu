//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2002-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.text.ParsePosition;

import com.ibm.icu.text.*;
import com.ibm.icu.lang.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public class Tokenizer {
    protected String source;
    
    protected StringBuffer buffer = new StringBuffer();
    protected long number;
    protected UnicodeSet unicodeSet = null;
    protected int index;
    boolean backedup = false;
    protected int lastIndex = -1;
    protected int nextIndex;
    int lastValue = BACKEDUP_TOO_FAR;
    TokenSymbolTable symbolTable = new TokenSymbolTable();

    private static final char
        QUOTE = '\'',
        BSLASH = '\\';
    private static final UnicodeSet QUOTERS = new UnicodeSet().add(QUOTE).add(BSLASH);
    private static final UnicodeSet WHITESPACE = new UnicodeSet("[" +
        "\\u0009-\\u000D\\u0020\\u0085\\u200E\\u200F\\u2028\\u2029" +
        "]");
    private static final UnicodeSet SYNTAX = new UnicodeSet("[" +
        "\\u0021-\\u002F\\u003A-\\u0040\\u005B-\\u0060\\u007B-\\u007E" +
        "\\u00A1-\\u00A7\\u00A9\\u00AB-\\u00AC\\u00AE" +
        "\\u00B0-\\u00B1\\u00B6\\u00B7\\u00BB\\u00BF\\u00D7\\u00F7" +
        "\\u2010-\\u2027\\u2030-\\u205E\\u2190-\\u2BFF" +
        "\\u3001\\u3003\\u3008-\\u3020\\u3030" +
        "\\uFD3E\\uFD3F\\uFE45\\uFE46" +
        "]").removeAll(QUOTERS).remove('$');
    private static final UnicodeSet NEWLINE = new UnicodeSet("[\\u000A\\u000D\\u0085\\u2028\\u2029]");
    //private static final UnicodeSet DECIMAL = new UnicodeSet("[:Nd:]");
    private static final UnicodeSet NON_STRING = new UnicodeSet()
        .addAll(WHITESPACE)
        .addAll(SYNTAX);
           
    protected UnicodeSet whiteSpace = WHITESPACE;
    protected UnicodeSet syntax = SYNTAX;
    private UnicodeSet non_string = NON_STRING;

    private void fixSets() {
        if (syntax.containsSome(QUOTERS) || syntax.containsSome(whiteSpace)) {
            syntax = ((UnicodeSet)syntax.clone()).removeAll(QUOTERS).removeAll(whiteSpace);
        }
        if (whiteSpace.containsSome(QUOTERS)) {
            whiteSpace = ((UnicodeSet)whiteSpace.clone()).removeAll(QUOTERS);
        }
        non_string = new UnicodeSet(syntax)
            .addAll(whiteSpace);
    }
    
    public Tokenizer setSource(String source) {
        this.source = source;
        this.index = 0;
        return this; // for chaining
    }
    
    public Tokenizer setIndex(int index) {
        this.index = index;
        return this; // for chaining
    }
    
    public static final int 
        DONE = -1, 
        NUMBER = -2, 
        STRING = -3, 
        UNICODESET = -4, 
        UNTERMINATED_QUOTE = -5,
        BACKEDUP_TOO_FAR = -6;
        
    private static final int
        //FIRST = 0,
        //IN_NUMBER = 1,
        //IN_SPACE = 2,
        AFTER_QUOTE = 3,    // warning: order is important for switch statement
        IN_STRING = 4, 
        AFTER_BSLASH = 5, 
        IN_QUOTE = 6;
   
    public String toString(int type, boolean backedupBefore) {
        String s = backedup ? "@" : "*";
        switch(type) {
            case DONE: 
                return s+"Done"+s;
            case BACKEDUP_TOO_FAR:
                return s+"Illegal Backup"+s;
            case UNTERMINATED_QUOTE: 
                return s+"Unterminated Quote=" + getString() + s;
            case STRING:
                return s+"s=" + getString() + s;
            case NUMBER:
                return s+"n=" + getNumber() + s;
            case UNICODESET:
                return s+"n=" + getUnicodeSet() + s;           
            default:
                return s+"c=" + usf.getName(type,true) + s;
        }
    }
    
    private static final BagFormatter usf = new BagFormatter();
    
    public void backup() {
        if (backedup) throw new IllegalArgumentException("backup too far");
        backedup = true;
        nextIndex = index;
        index = lastIndex;
    }
    
    /*
    public int next2() {
        boolean backedupBefore = backedup;
        int result = next();
        System.out.println(toString(result, backedupBefore));
        return result;
    }    
    */
    
    public int next() {
        if (backedup) {
            backedup = false;
            index = nextIndex;
            return lastValue;
        }
        int cp = 0;
        boolean inComment = false;
        // clean off any leading whitespace or comments
        while (true) {
            if (index >= source.length()) return lastValue = DONE;
            cp = nextChar();
            if (inComment) {
                if (NEWLINE.contains(cp)) inComment = false;
            } else {
                if (cp == '#') inComment = true;
                else if (!whiteSpace.contains(cp)) break;
            }
        }
        // record the last index in case we have to backup
        lastIndex = index;
        
        if (cp == '[') {
            ParsePosition pos = new ParsePosition(index-1);
            unicodeSet = new UnicodeSet(source,pos,symbolTable);
            index = pos.getIndex();
            return lastValue = UNICODESET;
        }
        // get syntax character
        if (syntax.contains(cp)) return lastValue = cp;
        
        // get number, if there is one
        if (UCharacter.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
            number = UCharacter.getNumericValue(cp);
            while (index < source.length()) {
                cp = nextChar();
                if (UCharacter.getType(cp) != Character.DECIMAL_DIGIT_NUMBER) {
                    index -= UTF16.getCharCount(cp); // BACKUP!
                    break;
                }
                number *= 10;
                number += UCharacter.getNumericValue(cp);
            }
            return lastValue =  NUMBER;
        }
        buffer.setLength(0);
        int status = IN_STRING;
        main:
        while (true) {
            switch (status) {
                case AFTER_QUOTE: // check for double ''?
                    if (cp == QUOTE) {
                        UTF16.append(buffer, QUOTE);
                        status = IN_QUOTE;
                        break;
                    }
                    // OTHERWISE FALL THROUGH!!!
                case IN_STRING: 
                    if (cp == QUOTE) status = IN_QUOTE;
                    else if (cp == BSLASH) status = AFTER_BSLASH;
                    else if (non_string.contains(cp)) {
                        index -= UTF16.getCharCount(cp); // BACKUP!
                        break main;
                    } else UTF16.append(buffer,cp);
                    break;
                case IN_QUOTE:
                    if (cp == QUOTE) status = AFTER_QUOTE;
                    else UTF16.append(buffer,cp);
                    break;
                case AFTER_BSLASH:
                    switch(cp) {
                        case 'n': cp = '\n'; break;
                        case 'r': cp = '\r'; break;
                        case 't': cp = '\t'; break;
                    }
                    UTF16.append(buffer,cp);
                    status = IN_STRING;
                    break;
                default: throw new IllegalArgumentException("Internal Error");
            }
            if (index >= source.length()) break;
            cp = nextChar();
        }
        if (status > IN_STRING) return lastValue = UNTERMINATED_QUOTE;
        return lastValue =  STRING;
    }
    
    public String getString() {
        return buffer.toString();
    }
    
    public String toString() {
        return source.substring(0,index) + "$$$" + source.substring(index);
    }
    
    public long getNumber() {
        return number;
    }
    
    public UnicodeSet getUnicodeSet() {
        return unicodeSet;
    }
    
    private int nextChar() {
        int cp = UTF16.charAt(source,index);
        index += UTF16.getCharCount(cp);
        return cp;
    }
    public int getIndex() {
        return index;
    }
    public String getSource() {
        return source;
    }
    public UnicodeSet getSyntax() {
        return syntax;
    }
    public UnicodeSet getWhiteSpace() {
        return whiteSpace;
    }
    public void setSyntax(UnicodeSet set) {
        syntax = set;
        fixSets();
    }
    public void setWhiteSpace(UnicodeSet set) {
        whiteSpace = set;
        fixSets();
    }
    
    public Set getLookedUpItems() {
        return symbolTable.itemsLookedUp;
    }
    
    public void addSymbol(String var, String value, int start, int limit) {
        // the limit is after the ';', so remove it
        --limit;
        char[] body = new char[limit - start];
        value.getChars(start, limit, body, 0);
        symbolTable.add(var, body);
    }
    
    public class TokenSymbolTable implements SymbolTable {
        Map contents = new HashMap();
        Set itemsLookedUp = new HashSet();
            
        public void add(String var, char[] body) {
            // start from 1 to avoid the $
            contents.put(var.substring(1), body);
        }
            
        /* (non-Javadoc)
         * @see com.ibm.icu.text.SymbolTable#lookup(java.lang.String)
         */
        public char[] lookup(String s) {
            itemsLookedUp.add('$' + s);
            return (char[])contents.get(s);
        }
    
        /* (non-Javadoc)
         * @see com.ibm.icu.text.SymbolTable#lookupMatcher(int)
         */
        public UnicodeMatcher lookupMatcher(int ch) {
            // TODO Auto-generated method stub
            return null;
        }
    
        /* (non-Javadoc)
         * @see com.ibm.icu.text.SymbolTable#parseReference(java.lang.String, java.text.ParsePosition, int)
         */
        public String parseReference(String text, ParsePosition pos, int limit) {
            int cp;
            int start = pos.getIndex();
            int i;
            for (i = start; i < limit; i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(text, i);
                if (!com.ibm.icu.lang.UCharacter.isUnicodeIdentifierPart(cp)) {
                    break;
                }
            }
            pos.setIndex(i);
            return text.substring(start,i);
        }
        
    }
}

//#endif
