/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/LigatureEntry.java,v $
 * $Date: 2003/12/09 01:18:11 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.util.*;
import java.lang.*;

public class LigatureEntry
{
    private int[] componentChars;
    private int ligature;
    
    public LigatureEntry(int ligature, int[] componentChars, int componentCount)
    {
        this.componentChars = new int[componentCount];
        this.ligature = ligature;
        System.arraycopy(componentChars, 0, this.componentChars, 0, componentCount);
}
    
    public int getComponentCount()
    {
        return componentChars.length;
    }
    
    public int getComponentChar(int componentIndex)
    {
        return componentChars[componentIndex];
    }
    
    public int getLigature()
    {
        return ligature;
    }
}
