// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.BytesTrie;

public class TrieTest {
    @Test
    public void testSimple() {
        Trie trie = new Trie();
        trie.root().with("answer", t -> t.putPrefixAndValue(42));
        assertThat(getRawTrieTable(trie.toByteArray())).containsExactly("answer", 42);
    }

    @Test
    public void testSubSpan() {
        Trie trie = new Trie();
        trie.root().with("foo", foo -> foo.with("bar", fooBar -> fooBar.putPrefixAndValue(42)));
        assertThat(getRawTrieTable(trie.toByteArray())).containsExactly("foo-bar", 42);
    }

    @Test
    public void testHierarchyAndOrdering() {
        Trie trie = new Trie();
        trie.root().with("foo", foo -> {
            foo.with("two", sub -> sub.putPrefixAndValue(3));
            foo.with("one", sub -> sub.putPrefixAndValue(2));
            foo.with("*", sub -> sub.putPrefixAndValue(1));
        });
        trie.root().with("bar", bar -> bar.with("baz", baz -> baz.with("quux", quux -> quux.putPrefixAndValue(0))));

        // Order is by "subtag" (left-to-right) with lexicographical order of tags (other
        // than '*' which is always sorted first).
        assertThat(getRawTrieTable(trie.toByteArray()))
                .containsExactly(
                        "bar-baz-quux", 0,
                        "foo-*", 1,
                        "foo-one", 2,
                        "foo-two", 3)
                .inOrder();
    }

    @Test
    public void testStarOrdering() {
        Trie trie = new Trie();
        // Use '$' which has a lower byte value that '*' in ASCII, but when it terminates a prefix,
        // it has bit-7 set which makes it sort higher than '*'.
        // In other tests it's not clear that '*' is sorted specially since '*' < [a-z] anyway.
        trie.root().with("$", foo -> {
            // A single '$' sorts after '*' because '$' will have bit-7 set, and '*' will not.
            foo.with("$", sub -> sub.putPrefixAndValue(5));
            // '$$' sorts below * because the leading '$' won't have bit-7 set.
            foo.with("$$", sub -> sub.putPrefixAndValue(3));
            foo.with("*", sub -> sub.putPrefixAndValue(4));
        });
        trie.root().with("*", foo -> {
            foo.with("$", sub -> sub.putPrefixAndValue(2));
            foo.with("*", sub -> sub.putPrefixAndValue(1));
        });
        trie.root().with("*", sub -> sub.putPrefixAndValue(0));

        // Star is definitely sorted before other entries.
        assertThat(getRawTrieTable(trie.toByteArray()))
                .containsExactly(
                        "*", 0,
                        "*-*", 1,
                        "*-$", 2,
                        "$-$$", 3,
                        "$-*", 4,
                        "$-$", 5)
                .inOrder();
    }

    @Test
    public void testBadTrie_BadValue() {
        Trie trie = new Trie();
        IllegalArgumentException e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> trie.root().with("foo", t -> t.putPrefixAndValue(-1)));
        assertThat(e).hasMessageThat().contains("bad trie value");
        assertThat(e).hasMessageThat().contains("-1");
    }

    @Test
    public void testBadTrie_NoPrefix() {
        Trie trie = new Trie();
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> trie.root().putPrefixAndValue(23));
        assertThat(e).hasMessageThat().contains("missing prefix");
        assertThat(e).hasMessageThat().contains("23");
    }

    @Test
    public void testBadTrie_BadPrefix() {
        Trie trie = new Trie();
        IllegalArgumentException e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> trie.root().with("ümlaut", t -> t.putPrefixAndValue(0)));
        assertThat(e).hasMessageThat().contains("invalid trie character");
        assertThat(e).hasMessageThat().contains("ü");
    }

    @Test
    public void testBadTrie_NoStarInPrefix() {
        Trie trie = new Trie();
        IllegalArgumentException e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> trie.root().with("foo*bar", t -> t.putPrefixAndValue(0)));
        assertThat(e).hasMessageThat().contains("must not contain '*'");
        assertThat(e).hasMessageThat().contains("foo*bar");
    }

    @Test
    public void testBadTrie_TooLong() {
        Trie trie = new Trie();
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> infiniteRecursion(trie.root()));
        assertThat(e).hasMessageThat().contains("span too long");
    }

    private static void infiniteRecursion(Trie.Span span) {
        span.with("!", TrieTest::infiniteRecursion);
    }

    private static ImmutableMap<String, Integer> getRawTrieTable(byte[] data) {
        // We rebuild the Trie from the byte[] data.
        BytesTrie trie = new BytesTrie(data, 0);

        // Mostly copied from LikelySubtags (since the necessary constructor is private).
        // Main change is the this no longer uses a TreeMap, since we want to test order.
        Map<String, Integer> map = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        for (BytesTrie.Entry entry : trie) {
            sb.setLength(0);
            int length = entry.bytesLength();
            for (int i = 0; i < length; i++) {
                byte b = entry.byteAt(i);
                sb.append((char) (b & 0x7f));
                if (b < 0 || b == '*') {
                    // end of subtag (high bit set or special '*' wildcard)
                    sb.append("-");
                }
            }
            checkState(sb.length() > 0 && sb.charAt(sb.length() - 1) == '-');
            sb.setLength(sb.length() - 1);
            map.put(sb.toString(), entry.value);
        }
        return ImmutableMap.copyOf(map);
    }
}
