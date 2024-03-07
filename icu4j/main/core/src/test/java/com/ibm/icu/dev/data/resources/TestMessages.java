// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2010, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.data.resources;

import java.util.ListResourceBundle;

public class TestMessages extends ListResourceBundle {

    @Override
    protected Object[][] getContents() {
        return new Object[][] {
                {"bundleContainer", "TestMessages.class"}
        };
    }

}
