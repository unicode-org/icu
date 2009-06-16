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

public abstract class TreeWalker
{
    abstract void down(int ch);
    abstract void up();
    abstract void ligature(int lig);
    abstract void done();
}
