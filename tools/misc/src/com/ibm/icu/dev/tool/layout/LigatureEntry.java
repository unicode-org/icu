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
