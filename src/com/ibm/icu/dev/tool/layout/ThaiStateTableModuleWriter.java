/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 09, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ThaiStateTableModuleWriter.java,v $
 * $Date: 2003/12/17 04:54:39 $
 * $Revision: 1.2 $
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