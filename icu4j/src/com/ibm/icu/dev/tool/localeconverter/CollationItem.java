/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;


/**
 * A CollationItem represents a single entry in a collation sequence.
 */
public class CollationItem {
    private static final char[] OP_CHARS = {'=', '<', ';', ',', '&' };
    private static final char[] SPECIAL_CHARS = { '&', '@' , '=', '<', ';', ',' };
    public static final int NONE = 0;
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public static final int TERTIARY = 3;
    private static final int FIRST = 4;
    public int op;
    public String item;
    public String expansion;
    public String comment;
    
    public static final CollationItem FORWARD = new CollationItem(NONE, "") {
        public String toString() { return ""; }
    };
    public static final CollationItem BACKWARD = new CollationItem(NONE, "") {
        public String toString() { return "@"; }
    };
    
    public CollationItem(String item) {
        this.op = FIRST;
        this.item = cleanString(item);
        this.expansion = "";
    }
    
    public CollationItem(int op, String item) {
        this(op, item, null);
    }
    
    public CollationItem(int op, String item, String expansion) {
        this.op = Math.abs(op);
        if (this.op > TERTIARY) this.op = TERTIARY;
        this.item = cleanString(PosixCollationBuilder.unescape(item));
        this.expansion = cleanString(PosixCollationBuilder.unescape(expansion));
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String toString() {
        if (expansion.length() == 0) {
            return ""+OP_CHARS[op]+item;
        } else {
            return "&"+expansion+OP_CHARS[op]+item;
        }
    }
    
    private String cleanString(String source) {
        if (source == null) return "";
        String result = source;
        for (int i = 0; i < result.length(); i++) {
            final char c = result.charAt(i);
            if ((c == '@') || (c == '\t') || (c == '\n')
                    || (c == '\f') || (c =='\r') || (c == '\013')
                    || ((c <= '\u002F') && (c >= '\u0020'))
                    || ((c <= '\u003F') && (c >= '\u003A'))
                    || ((c <= '\u0060') && (c >= '\u005B'))
                    || ((c <= '\u007E') && (c >= '\u007B'))) {
                if (i < result.length()-1) {
                    result = result.substring(0, i)
                            + "\\" + c 
                            + result.substring(i+1);
                } else {
                    result = result.substring(0, i)
                            + "\\" + c;
                }
                i += 2; //skip the two characters we inserted
            }
        }
        return result;
    }
}