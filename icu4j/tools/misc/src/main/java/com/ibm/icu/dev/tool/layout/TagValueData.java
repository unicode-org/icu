// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.layout;

public abstract class TagValueData {
    public abstract int getMinValue();

    public abstract int getMaxValue();

    public abstract String getName(int value);

    public abstract String getTag(int value);

    public abstract String getTagLabel(int value);

    public abstract String makeTag(int value);
}
