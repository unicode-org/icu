/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.impl;

public class InvalidFormatException extends Exception {
    public InvalidFormatException(){}
    public InvalidFormatException(String message){
        super(message);
    }
}
