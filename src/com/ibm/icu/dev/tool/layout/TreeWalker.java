/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/TreeWalker.java,v $
 * $Date: 2003/12/09 01:18:11 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

public abstract class TreeWalker
{
    abstract void down(int ch);
    abstract void up();
    abstract void ligature(int lig);
    abstract void done();
}
