/*
*******************************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.text;

/**
 * Thrown by ArabicShaping when there is a shaping error.
 * @stable ICU 2.0
 */
public final class ArabicShapingException extends Exception {
    ArabicShapingException(String message) {
        super(message);
    }
}
