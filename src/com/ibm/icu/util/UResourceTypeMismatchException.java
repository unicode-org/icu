/*
******************************************************************************
* Copyright (C) 2004, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.util;

/**
 * Exception thrown when the requested resource type 
 * is not the same type as the available resource
 * @author ram
 * @draft ICU 3.0
 * @deprecated This is a draft API and might change in a future release of ICU.
 */
public class UResourceTypeMismatchException extends RuntimeException {
    private String message;
    
    /**
     * Constuct the exception with the given message
     * @param msg the error message for this exception
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public UResourceTypeMismatchException(String msg){
        message = msg;       
    }
    /**
     * Returns the error message stored in this exception
     * @return String the error message string
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String toString(){
        return message;   
    }
}
