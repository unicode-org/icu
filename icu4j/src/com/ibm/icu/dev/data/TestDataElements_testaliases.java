/**
*******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/data/TestDataElements_testaliases.java,v $
* $Date: 2003/06/03 18:49:27 $
* $Revision: 1.6 $
*
*******************************************************************************
*/
package com.ibm.icu.dev.data;

import com.ibm.icu.impl.ICUListResourceBundle;

public class TestDataElements_testaliases extends ICUListResourceBundle {

    public TestDataElements_testaliases  () {
          super.contents = data;
    }
    private static Object[][] data = new Object[][] { 
                {
                    "CollationElements",
                    new ICUListResourceBundle.Alias("/ICUDATA/uk"),
                },

                {
                    "anotheralias",
                    new ICUListResourceBundle.Alias("/ICUDATA/uk/CollationElements"),
                },
                {
                    "nonexisting",
                    new ICUListResourceBundle.Alias("/ICUDATA/uk"),
                },
                {
                    "referencingalias",
                    new ICUListResourceBundle.Alias("testaliases/anotheralias/Sequence"),
                },
                {
                    "simplealias",
                    new ICUListResourceBundle.Alias("testtypes/menu/file/open"),
                },
                {
                    "zoneStrings",
                    new Object[]{
                        new ICUListResourceBundle.Alias("/ICUDATA/en"),
                        new ICUListResourceBundle.Alias("/ICUDATA/en"),

                    },
                },
                {
                    "zoneTests",
                    new Object[][]{
                        {
                            "zoneAlias",
                            new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings"),
                        },
                        {
                            "zoneAlias1",
                            new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/3"),
                        },
                        {
                            "zoneAlias2",
                            new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/3/0"),
                        },
                    },
                },
    };
}
