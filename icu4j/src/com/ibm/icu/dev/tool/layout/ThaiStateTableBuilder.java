/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 09, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ThaiStateTableBuilder.java,v $
 * $Date: 2003/12/09 23:55:16 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.impl.Utility;
import java.io.*;
import java.util.*;
import java.lang.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
