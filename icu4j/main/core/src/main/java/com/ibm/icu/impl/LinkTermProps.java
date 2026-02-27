// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.impl;

import com.ibm.icu.util.CodePointTrie;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Link termination properties loaded from ulinkterm.icu.
 * Implements the Link_Term property (UTS #58 / proposed Unicode 19.0).
 *
 * <p>Values match the C-side ULinkTerm enum:
 * <ul>
 *   <li>{@link #HARD}    = 0 (default for all unlisted code points)</li>
 *   <li>{@link #INCLUDE} = 1</li>
 *   <li>{@link #SOFT}    = 2</li>
 *   <li>{@link #CLOSE}   = 3</li>
 *   <li>{@link #OPEN}    = 4</li>
 * </ul>
 */
public final class LinkTermProps {

    // Link_Term property values — must match ULinkTerm in linktermprops.h.
    public static final int HARD    = 0;
    public static final int INCLUDE = 1;
    public static final int SOFT    = 2;
    public static final int CLOSE   = 3;
    public static final int OPEN    = 4;

    // Indexes into the binary data indexes[] array (see linktermprops.h).
    private static final int IX_COUNT      = 0;
    private static final int IX_CPTRIE_TOP = 1;

    // "LnkT"
    private static final int DATA_FORMAT = 0x4C6E6B54;

    private static final ICUBinary.Authenticate IS_ACCEPTABLE =
            version -> version[0] == 1;

    public static final LinkTermProps INSTANCE = new LinkTermProps();

    private final CodePointTrie.Fast8 cpTrie;

    private LinkTermProps() {
        ByteBuffer bytes = ICUBinary.getRequiredData("ulinkterm.icu");
        try {
            ICUBinary.readHeaderAndDataVersion(bytes, DATA_FORMAT, IS_ACCEPTABLE);
            int startPos = bytes.position();

            // indexes[0] = number of entries in the indexes array.
            int indexCount = bytes.getInt();
            if (indexCount < 2) {
                throw new ICUUncheckedIOException("ulinkterm.icu: indexes too short");
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
     * Returns the Link_Term value for a code point.
     *
     * @param c a Unicode code point
     * @return one of {@link #HARD}, {@link #INCLUDE}, {@link #SOFT},
     *         {@link #CLOSE}, {@link #OPEN}
     */
    public int get(int c) {
        return cpTrie.get(c);
    }
}
