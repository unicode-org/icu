/*
*******************************************************************************
* Copyright (C) 2012-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
*******************************************************************************
* CollationRoot.java, ported from collationroot.h/.cpp
*
* C++ version created on: 2012dec17
* created by: Markus W. Scherer
*/

package com.ibm.icu.impl.coll;

import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;

/**
 * Collation root provider.
 */
public final class CollationRoot {  // purely static
    private static final CollationTailoring rootSingleton;
    private static final RuntimeException exception;

    public static final CollationTailoring getRoot() {
        if(exception != null) {
            throw exception;
        }
        return rootSingleton;
    }
    public static final CollationData getData() {
        CollationTailoring root = getRoot();
        return root.data;
    }
    static final CollationSettings getSettings() {
        CollationTailoring root = getRoot();
        return root.settings.readOnly();
    }

    static {  // Corresponds to C++ load() function.
        CollationTailoring t = new CollationTailoring(null);
        String path = ICUResourceBundle.ICU_BUNDLE + "/coll/ucadata.icu";
        InputStream inBytes = ICUData.getRequiredStream(path);
        RuntimeException e2 = null;
        try {
            CollationDataReader.read(null, inBytes, t);
        } catch(IOException e) {
            t = null;
            e2 = new MissingResourceException(
                    "IOException while reading CLDR root data",
                    "CollationRoot", path);
        } catch(RuntimeException e) {
            t = null;
            e2 = e;
        }
        rootSingleton = t;
        exception = e2;
    }
}
