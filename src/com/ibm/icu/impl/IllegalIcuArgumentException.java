//##header
/*
 *******************************************************************************
 * Copyright (C) 2009, Google, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

/**
 * @author markdavis
 *
 */
public class IllegalIcuArgumentException extends IllegalArgumentException {
    private static final long serialVersionUID = 3789261542830211225L;

    public IllegalIcuArgumentException(String errorMessage) {
        super(errorMessage);
    }
    
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
    public synchronized Throwable initCause(Throwable cause) {
    return super.initCause(cause);
    }
//#endif 
}
