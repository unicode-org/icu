/*
 *******************************************************************************
 * Copyright (C) 2015, Google, Inc., International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.util.LocaleMatcher.LanguageMatcherData;

/**
 * @author markdavis
 *
 */
public class LocaleMatcherShim {
    public static LanguageMatcherData load() {
        // In CLDR, has different value
        return null;
    }
}
