/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 09, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;


public class ThaiStateTableBuilder
{
    public static void main(String args[])
    {
        ThaiStateTableModuleWriter writer = new ThaiStateTableModuleWriter();
        
        writer.openFile(args[0]);
        writer.writeTables();
        writer.closeFile();
        
    }
    
}
