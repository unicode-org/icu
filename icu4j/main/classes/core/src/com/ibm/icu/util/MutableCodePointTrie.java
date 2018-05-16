// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// created: 2018may04 Markus W. Scherer

package com.ibm.icu.util;

// TODO: Add intermediate abstract class MutableCodePointMap with abstract set() & setRange()?

/**
 * Mutable Unicode code point trie.
 * Fast map from Unicode code points (U+0000..U+10FFFF) to 32-bit integer values.
 * For details see http://site.icu-project.org/design/struct/utrie
 *
 * <p>Setting values (especially ranges) and lookup is fast.
 * The mutable trie is only somewhat space-efficient.
 * It builds a compacted, immutable {@link CodePointTrie}.
 *
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 */
public final class MutableCodePointTrie extends CodePointMap {
    public MutableCodePointTrie(int initialValue, int errorValue) {
    }

    @Override
    public MutableCodePointTrie clone() {
        try {
            MutableCodePointTrie builder = (MutableCodePointTrie) super.clone();
            // TODO
            return builder;
        } catch (CloneNotSupportedException ignored) {
            // Unreachable: Cloning *is* supported.
            return null;
        }
    }

    public MutableCodePointTrie fromCodePointMap(CodePointMap map) {
        return null;
    }

    // with range check
    @Override
    public int get(int c) {
        return c & 3;
    }

    @Override
    public boolean getRange(int start, CodePointTrie.HandleValue handleValue,
            CodePointTrie.Range range) {
        range.start = range.end = start;
        range.value = 0;
        return true;
    }

    public void set(int c, int value) {

    }

    public void setRange(int start, int end, int value) {

    }

    public CodePointTrie buildImmutable(CodePointTrie.Type type, CodePointTrie.ValueWidth valueWidth) {
        return null;
    }
}
