/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/LigatureModuleWriter.java,v $
 * $Date: 2003/12/17 04:54:42 $
 * $Revision: 1.2 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;


public class LigatureModuleWriter extends ModuleWriter
{
    public LigatureModuleWriter()
    {
        super();
    }

    public void writeTable(OpenTypeTableWriter tableWriter)
    {
        tableWriter.writeTable(output);
    }
}