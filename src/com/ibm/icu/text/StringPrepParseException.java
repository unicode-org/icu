/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.ParseException;

/**
 * Exception that signals an error has occurred while parsing the 
 * input to StringPrep or IDNA. 
 *
 * @author Ram Viswanadha
 * @draft ICU 2.8
 * @deprecated This is a draft API and might change in a future release of ICU.
 */
public class StringPrepParseException extends ParseException {
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int INVALID_CHAR_FOUND      = 0;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int ILLEGAL_CHAR_FOUND      = 1;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int PROHIBITED_ERROR        = 2;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int UNASSIGNED_ERROR        = 3;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int CHECK_BIDI_ERROR        = 4;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int STD3_ASCII_RULES_ERROR  = 5;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int ACE_PREFIX_ERROR        = 6;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int VERIFICATION_ERROR      = 7;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int LABEL_TOO_LONG_ERROR    = 8;
    /**
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int BUFFER_OVERFLOW_ERROR   = 9;
    
    /**
     * Construct a ParseException object with the given message
     * and error code
     * 
     * @param message A string describing the type of error that occurred
     * @param error   The error that has occurred
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public StringPrepParseException(String message,int error){
        super(message, -1);
        this.error = error;
        this.line = 0;
    }
    
    /**
     * Construct a ParseException object with the given message and
     * error code
     * 
     * @param message A string describing the type of error that occurred
     * @param error   The error that has occurred
     * @param rules   The input rules string 
     * @param pos     The position of error in the rules string
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public StringPrepParseException(String message,int error, String rules, int pos){
        super(message, -1);
        this.error = error;
        setContext(rules,pos);  
        this.line = 0;
    }
    /**
     * Construct  a ParseException object with the given message and error code
     * 
     * @param message    A string describing the type of error that occurred
     * @param error      The error that has occurred
     * @param rules      The input rules string 
     * @param pos        The position of error in the rules string
     * @param lineNumber The line number at which the error has occurred. 
     *                   If the parse engine is not using this field, it should set it to zero.  Otherwise
     *                   it should be a positive integer. The default value of this field
     *                   is -1. It will be set to 0 if the code populating this struct is not
     *                   using line numbers.
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public StringPrepParseException(String message, int error, String rules, int pos, int lineNumber){
        super(message, -1);
        this.error = error;
        setContext(rules,pos);   
        this.line = lineNumber;
    }
    /**
     * Compare this ParseException to another and evaluate if they are equal.
     * The comparison works only on the type of error and does not compare
     * the rules strings, if any, for equality.
     * 
     * @param other The exception that this object should be compared to
     * @return true if the objects are equal, false if unequal
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public boolean equals(Object other){
        if(!(other instanceof StringPrepParseException)){
            return false;
        }
        return ((StringPrepParseException)other).error == this.error;
        
    }
    /**
     * Returns the position of error in the rules string
     * 
     * @return String
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
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
     * @draft ICU 2.8  
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    private int line;


    /**
     * Textual context before the error.  Null-terminated.
     * May be the empty string if not implemented by parser.
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    private StringBuffer preContext = new StringBuffer();

    /**
     * Textual context after the error.  Null-terminated.
     * May be the empty string if not implemented by parser.
     * @draft ICU 2.8   
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    private StringBuffer postContext =  new StringBuffer();
    
    private static final int PARSE_CONTEXT_LEN = 16;
    

    
    private void setPreContext(String str, int pos){
        setPreContext(str.toCharArray(),pos);
    }
    
    private void setPreContext(char[] str, int pos){
        int start = (pos <= PARSE_CONTEXT_LEN)? 0 : (pos - (PARSE_CONTEXT_LEN-1));
        int len = (start <= PARSE_CONTEXT_LEN)? start : PARSE_CONTEXT_LEN;
        preContext.append(str,start,len);
 
    }
    
    private void setPostContext(String str, int pos){
        setPostContext(str.toCharArray(),pos);
    }
    
    private void setPostContext(char[] str, int pos){
        int start = pos;
        int len  = str.length - start; 
        postContext.append(str,start,len);

    }
    
    private void setContext(String str,int pos){
        setPreContext(str,pos);
        setPostContext(str,pos);
    }
}
