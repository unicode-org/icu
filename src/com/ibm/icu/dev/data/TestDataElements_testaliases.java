/**
*******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/data/TestDataElements_testaliases.java,v $
* $Date: 2003/10/07 17:24:18 $
* $Revision: 1.7 $
*
*******************************************************************************
*/
package com.ibm.icu.dev.data;

import com.ibm.icu.impl.ICUListResourceBundle;

public class TestDataElements_testaliases extends ICUListResourceBundle {

    public TestDataElements_testaliases  () {
          super.contents = data;
    }
    static final Object[][] data = new Object[][] { 
                {
                    "CollationElements",
                    new ICUListResourceBundle.Alias("/ICUDATA/uk"),
                },
// Circular aliases test moved to TestCircularAliases
//               {
//                   "aaa",
//                    new ICUListResourceBundle.Alias("testaliases/aab"),
//                },
//                {
//                    "aab",
//                    new ICUListResourceBundle.Alias("testaliases/aaa"),
//                },
                {
                    "anotheralias",
                    new ICUListResourceBundle.Alias("/ICUDATA/uk/CollationElements"),
                },
// Moved to TestNonExisting
//                {
//                    "nonexisting",
//                    new ICUListResourceBundle.Alias("/ICUDATA/uk"),
//                },
                {
                    "referencingalias",
                    new ICUListResourceBundle.Alias("testaliases/anotheralias/Sequence"),
                },
                {
                    "simplealias",
                    new ICUListResourceBundle.Alias("testtypes/menu/file/open"),
                },
                {
                    "testGetStringByIndexAliasing",
                    new Object[]{
                        new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/0"),
                        new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/1"),
                        new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/4"),
                        new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/5"),

                    },
                },
                {
                    "testGetStringByKeyAliasing",
                    new Object[][]{
                        {
                            "KeyAlias0PST",
                            new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/0"),
                        },
                        {
                            "KeyAlias1PacificStandardTime",
                            new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/1"),
                        },
                        {
                            "KeyAlias2PDT",
                            new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/4"),
                        },
                        {
                            "KeyAlias3LosAngeles",
                            new ICUListResourceBundle.Alias("/ICUDATA/en/zoneStrings/0/5"),
                        },
                    },
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
