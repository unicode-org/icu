/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 09, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ThaiStateTableModuleWriter.java,v $
 * $Date: 2003/12/09 23:55:16 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.impl.Utility;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

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