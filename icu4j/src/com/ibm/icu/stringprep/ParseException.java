/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/stringprep/Attic/ParseException.java,v $
 * $Date: 2003/08/21 23:40:39 $
 * $Revision: 1.1 $ 
 *
 *****************************************************************************************
 */
package com.ibm.icu.stringprep;

/**
 * @author ram
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ParseException extends Exception {
    
    public static final int INVALID_CHAR_FOUND      = 0;
    public static final int ILLEGAL_CHAR_FOUND      = 1;
    public static final int PROHIBITED_ERROR        = 2;
    public static final int UNASSIGNED_ERROR        = 3;
    public static final int CHECK_BIDI_ERROR        = 4;
    public static final int STD3_ASCII_RULES_ERROR  = 5;
    public static final int ACE_PREFIX_ERROR        = 6;
    public static final int VERIFICATION_ERROR      = 7;
    public static final int LABEL_TOO_LONG_ERROR    = 8;
    public static final int BUFFER_OVERFLOW_ERROR   = 9;
    
    public ParseException(String message,int error){
        super(message);
        this.error = error;
    }
    public ParseException(String message,int error, String rules, int pos){
        super(message);
        this.error = error;
        setContext(rules,pos);    
    }
    
    public boolean equals(Object other){
        if(!(other instanceof ParseException)){
            return false;
        }
        return ((ParseException)other).error == this.error;
    }
    public String toString(){
        StringBuffer buf = new StringBuffer();
        buf.append(super.getMessage());
        buf.append(". preContext:  ");
        buf.append(preContext);
        buf.append(". postContext: ");
        buf.append(postContext);
        buf.append("\n");
        return buf.toString();
    }

    private int error;
    /**
     * The line on which the error occured.  If the parse engine
     * is not using this field, it should set it to zero.  Otherwise
     * it should be a positive integer. The default value of this field
     * is -1. It will be set to 0 if the code populating this struct is not
     * using line numbers.
     * @stable ICU 2.0    
     */
    private int line;

    /**
     * The character offset to the error.  If the line field is
     * being used, then this offset is from the start of the line.
     * If the line field is not being used, then this offset is from
     * the start of the text.The default value of this field
     * is -1. It will be set to appropriate value by the code that 
     * populating the struct.
     * @stable ICU 2.0   
     */
    private int    offset;

    /**
     * Textual context before the error.  Null-terminated.
     * May be the empty string if not implemented by parser.
     * @stable ICU 2.0   
     */
    private StringBuffer preContext = new StringBuffer();

    /**
     * Textual context after the error.  Null-terminated.
     * May be the empty string if not implemented by parser.
     * @stable ICU 2.0   
     */
    private StringBuffer postContext =  new StringBuffer();
    
    public static final int PARSE_CONTEXT_LEN = 16;
    
    public void setOffset(int offset){
        this.offset = offset;
    }
    public int getOffset(){
        return offset;
    }
    public int getLineNumber(){
        return line;
    }
    public int setLineNumber(int lineNumber){
        return line;
    }
    public String getPreContext(){
        return preContext.toString();
    }
    public String getPostContext(){
        return postContext.toString();
    }
    
    public void setPreContext(String str, int pos){
        setPreContext(str.toCharArray(),pos);
    }
    public void setPreContext(char[] str, int pos){
        int start = (pos <= PARSE_CONTEXT_LEN)? 0 : (pos - (PARSE_CONTEXT_LEN-1));
        int len = (start <= PARSE_CONTEXT_LEN)? start : PARSE_CONTEXT_LEN;
        preContext.append(str,start,len);
 
    }
    public void setPostContext(String str, int pos){
        setPostContext(str.toCharArray(),pos);
    }
    public void setPostContext(char[] str, int pos){
        int start = pos;
        int len  = str.length - start; 
        postContext.append(str,start,len);

    }
    public void setContext(char[]str,int pos){
        setPreContext(str,pos);
        setPostContext(str,pos);
    }
    public void setContext(String str,int pos){
        setPreContext(str,pos);
        setPostContext(str,pos);
    }
}
