/*
*******************************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.text;

/**
 * Thrown by ArabicShaping when there is a shaping error.
 */
public final class ArabicShapingException extends Exception {
    ArabicShapingException(String message) {
        super(message);
    }
}
