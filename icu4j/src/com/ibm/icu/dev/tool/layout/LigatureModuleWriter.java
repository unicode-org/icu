/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/LigatureModuleWriter.java,v $
 * $Date: 2003/12/09 01:18:11 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

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