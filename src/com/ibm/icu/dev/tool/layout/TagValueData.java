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

public abstract class TagValueData
{
    abstract public int getMinValue();
    abstract public int getMaxValue();
    
    abstract public String getName(int value);
    abstract public String getTag(int value);
    abstract public String getTagLabel(int value);
    abstract public String makeTag(int value);
};
