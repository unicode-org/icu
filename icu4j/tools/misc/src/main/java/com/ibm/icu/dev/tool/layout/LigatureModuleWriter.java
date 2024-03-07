// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
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
