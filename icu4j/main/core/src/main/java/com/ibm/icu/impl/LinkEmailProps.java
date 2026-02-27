// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.impl;

import com.ibm.icu.util.CodePointTrie;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Link_Email binary property loaded from ulinkemail.icu.
 * Implements the Link_Email property (UTS #58 / Unicode 17.0).
 *
 * <p>A code point has Link_Email=Yes if it may appear in an email local part.
 * All other code points have Link_Email=No (the default, stored as 0).
 */
public final class LinkEmailProps {

    // Indexes into the binary data indexes[] array (see linkemailprops.h).
    private static final int IX_COUNT      = 0;
    private static final int IX_CPTRIE_TOP = 1;

    // "LnkE"
    private static final int DATA_FORMAT = 0x4C6E6B45;

    private static final ICUBinary.Authenticate IS_ACCEPTABLE =
            version -> version[0] == 1;

    public static final LinkEmailProps INSTANCE = new LinkEmailProps();

    private final CodePointTrie.Fast8 cpTrie;

    private LinkEmailProps() {
        ByteBuffer bytes = ICUBinary.getRequiredData("ulinkemail.icu");
        try {
            ICUBinary.readHeaderAndDataVersion(bytes, DATA_FORMAT, IS_ACCEPTABLE);
            int startPos = bytes.position();

            // indexes[0] = number of entries in the indexes array.
            int indexCount = bytes.getInt();
            if (indexCount < 2) {
                throw new ICUUncheckedIOException("ulinkemail.icu: indexes too short");
            }
            int[] inIndexes = new int[indexCount];
            inIndexes[IX_COUNT] = indexCount;
            for (int i = 1; i < indexCount; i++) {
                inIndexes[i] = bytes.getInt();
            }

            // The UCPTrie starts immediately after the indexes[] array and
            // ends at inIndexes[IX_CPTRIE_TOP] (a byte offset from startPos).
            cpTrie = CodePointTrie.Fast8.fromBinary(bytes);
            int pos = bytes.position() - startPos;
            ICUBinary.skipBytes(bytes, inIndexes[IX_CPTRIE_TOP] - pos);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    /**
     * Returns true if the code point has Link_Email=Yes,
     * i.e., it is allowed in an email local part.
     *
     * @param c a Unicode code point
     * @return true if {@code c} has Link_Email=Yes
     */
    public boolean contains(int c) {
        return cpTrie.get(c) != 0;
    }
}
