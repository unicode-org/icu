// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.util;

import java.util.Iterator;

/**
 * Unicode code point trie builder.
 * Fast map from Unicode code points (U+0000..U+10FFFF) to 32-bit integer values.
 * For details see http://site.icu-project.org/design/struct/utrie
 *
 * <p>Setting values (especially ranges) and lookup is fast.
 * The builder is only somewhat space-efficient.
 * It builds a compacted, immutable {@link CodePointTrie}.
 *
 * @internal ICU 62 technology preview
 * @provisional This API might change or be removed in a future release.
 */
public final class CodePointTrieBuilder implements Cloneable, Iterable<CodePointTrie.Range> {
    public CodePointTrieBuilder(int initialValue, int errorValue) {
    }

    @Override
    public CodePointTrieBuilder clone() {
        try {
            CodePointTrieBuilder builder = (CodePointTrieBuilder) super.clone();
            // TODO
            return builder;
        } catch (CloneNotSupportedException ignored) {
            // Unreachable: Cloning *is* supported.
            return null;
        }
    }

    public CodePointTrieBuilder fromCodePointTrie(CodePointTrie trie) {
        return null;
    }

    // with range check
    public int get(int c) {
        return c & 3;
    }

    public boolean getRange(int start, CodePointTrie.HandleValue handleValue,
            CodePointTrie.Range range) {
        range.start = range.end = start;
        range.value = 0;
        return true;
    }

    @Override
    public Iterator<CodePointTrie.Range> iterator() {
        return null;
    }

    public Iterator<CodePointTrie.Range> iterator(int start, CodePointTrie.HandleValue handleValue) {
        return null;
    }

    public void set(int c, int value) {

    }

    public void setRange(int start, int end, int value) {

    }

    public CodePointTrie build(CodePointTrie.Type type, CodePointTrie.ValueBits valueBits) {
        return null;
    }
}
