/*
*******************************************************************************
*   Copyright (C) 2001-2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.text;

/**
 * Thrown by ArabicShaping when there is a shaping error.
 * @stable ICU 2.0
 */
public final class ArabicShapingException extends Exception {
    /**
     * Constuct the exception with the given message
     * @param msg the error message for this exception
     * 
     * @draft ICU 3.6
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public ArabicShapingException(String message) {
        super(message);
    }
}
