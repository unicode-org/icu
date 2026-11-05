// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.CompactByteArray;
import com.ibm.icu.util.CompactCharArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @since release 2.2
 */
@RunWith(JUnit4.class)
public final class CompactArrayTest extends CoreTestFmwk {
    @Test
    @SuppressWarnings("SelfEquals")
    public void TestByteArrayCoverage() {
        CompactByteArray cba = new CompactByteArray();
        cba.setElementAt((char) 0x5, (byte) 0xdf);
        cba.setElementAt((char) 0x105, (byte) 0xdf);
        cba.setElementAt((char) 0x205, (byte) 0xdf);
        cba.setElementAt((char) 0x305, (byte) 0xdf);
        CompactByteArray cba2 = new CompactByteArray((byte) 0xdf);
        if (cba.equals(cba2)) {
            errln("unequal byte arrays compare equal");
        }
        CompactByteArray cba3 = cba.clone();

        logDebug("equals null: " + cba.equals(null));
        logDebug("equals self: " + cba.equals(cba));
        logDebug("equals clone: " + cba.equals(cba3));
        logDebug("equals bogus: " + cba.equals(new Object()));
        logDebug("hash: " + cba.hashCode());

        cba.compact(true);
        cba.compact(true);

        char[] xa = cba.getIndexArray();
        byte[] va = cba.getValueArray();
        CompactByteArray cba4 = new CompactByteArray(xa, va);

        String xs = Utility.arrayToRLEString(xa);
        String vs = Utility.arrayToRLEString(va);
        CompactByteArray cba5 = new CompactByteArray(xs, vs);

        logDebug("equals: " + cba4.equals(cba5));
        logDebug("equals: " + cba.equals(cba4));

        cba4.compact(false);
        logDebug("equals: " + cba4.equals(cba5));

        cba5.compact(true);
        logDebug("equals: " + cba4.equals(cba5));

        cba.setElementAt((char) 0x405, (byte) 0xdf); // force expand
        logDebug("modified equals clone: " + cba.equals(cba3));

        cba3.setElementAt((char) 0x405, (byte) 0xdf); // equivalent modification
        logDebug("modified equals modified clone: " + cba.equals(cba3));

        cba3.setElementAt((char) 0x405, (byte) 0xee); // different modification
        logDebug("different mod equals: " + cba.equals(cba3));

        cba.compact();
        CompactByteArray cba6 = cba.clone();
        logDebug("cloned compact equals: " + cba.equals(cba6));

        cba6.setElementAt((char) 0x405, (byte) 0xee);
        logDebug("modified clone: " + cba3.equals(cba6));

        cba6.setElementAt((char) 0x100, (char) 0x104, (byte) 0xfe);
        for (int i = 0x100; i < 0x105; ++i) {
            cba3.setElementAt((char) i, (byte) 0xfe);
        }
        logDebug("double modified: " + cba3.equals(cba6));
    }

    @Test
    @SuppressWarnings("SelfEquals")
    public void TestCharArrayCoverage() {
        // v1.8 fails with extensive compaction, so set to false
        final boolean EXTENSIVE = false;

        CompactCharArray cca = new CompactCharArray();
        cca.setElementAt((char) 0x5, (char) 0xdf);
        cca.setElementAt((char) 0x105, (char) 0xdf);
        cca.setElementAt((char) 0x205, (char) 0xdf);
        cca.setElementAt((char) 0x305, (char) 0xdf);
        CompactCharArray cca2 = new CompactCharArray((char) 0xdf);
        if (cca.equals(cca2)) {
            errln("unequal char arrays compare equal");
        }
        CompactCharArray cca3 = cca.clone();

        logDebug("equals null: " + cca.equals(null));
        logDebug("equals self: " + cca.equals(cca));
        logDebug("equals clone: " + cca.equals(cca3));
        logDebug("equals bogus: " + cca.equals(new Object()));
        logDebug("hash: " + cca.hashCode());

        cca.compact(EXTENSIVE);
        cca.compact(EXTENSIVE);

        char[] xa = cca.getIndexArray();
        char[] va = cca.getValueArray();
        CompactCharArray cca4 = new CompactCharArray(xa, va);

        String xs = Utility.arrayToRLEString(xa);
        String vs = Utility.arrayToRLEString(va);
        CompactCharArray cca5 = new CompactCharArray(xs, vs);

        logDebug("equals: " + cca4.equals(cca5));
        logDebug("equals: " + cca.equals(cca4));

        cca4.compact(false);
        logDebug("equals: " + cca4.equals(cca5));

        cca5.compact(EXTENSIVE);
        logDebug("equals: " + cca4.equals(cca5));

        cca.setElementAt((char) 0x405, (char) 0xdf); // force expand
        logDebug("modified equals clone: " + cca.equals(cca3));

        cca3.setElementAt((char) 0x405, (char) 0xdf); // equivalent modification
        logDebug("modified equals modified clone: " + cca.equals(cca3));

        cca3.setElementAt((char) 0x405, (char) 0xee); // different modification
        logDebug("different mod equals: " + cca.equals(cca3));

        // after setElementAt isCompact is set to false
        cca3.compact(true);
        logDebug("different mod equals: " + cca.equals(cca3));

        cca3.setElementAt((char) 0x405, (char) 0xee); // different modification
        logDebug("different mod equals: " + cca.equals(cca3));
        // after setElementAt isCompact is set to false
        cca3.compact();
        logDebug("different mod equals: " + cca.equals(cca3));

        // v1.8 fails with extensive compaction, and defaults extensive, so don't compact
        // cca.compact();
        CompactCharArray cca6 = cca.clone();
        logDebug("cloned compact equals: " + cca.equals(cca6));

        cca6.setElementAt((char) 0x405, (char) 0xee);
        logDebug("modified clone: " + cca3.equals(cca6));

        cca6.setElementAt((char) 0x100, (char) 0x104, (char) 0xfe);
        for (int i = 0x100; i < 0x105; ++i) {
            cca3.setElementAt((char) i, (char) 0xfe);
        }
        logDebug("double modified: " + cca3.equals(cca6));
    }
}
