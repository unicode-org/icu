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

public class ThaiStateTableModuleWriter extends ModuleWriter
{
    public ThaiStateTableModuleWriter()
    {
        super();
    }

    public void writeTables()
    {
        writeHeader(null, includeFiles);
        
        ThaiCharacterClasses.writeClassTable(output);
        ThaiStateTable.writeStateTable(output);
        
        writeTrailer();
    }

    private static final String[] includeFiles = {"LETypes.h", "ThaiShaping.h"};
}