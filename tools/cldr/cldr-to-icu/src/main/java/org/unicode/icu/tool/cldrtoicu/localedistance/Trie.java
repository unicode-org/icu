// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import com.ibm.icu.impl.locale.LocaleDistance;
import com.ibm.icu.util.BytesTrieBuilder;

/**
 * Trie constructed by adding "spans" of data representing prefix
 * sequences with mapped values.
 *
 * <p>When a prefix needs to be added to a Trie, a new span is created to
 * represents the additional data. If a final value is added to a span, then
 * the current prefix data is committed to the underlying Trie as its key.
 *
 * <p>Typical use might look like:
 * <pre>{@code
 * Trie trie = new Trie();
 * mappedData.forEach(
 *     (prefix, subValues) -> trie.root().with(prefix, subSpan -> process(subSpan, subValues));
 * byte[] bytes = trie.toByteArray();
 * }
 * }</pre>
 * where the {@code process} method may create more sub-spans, and eventually
 * calls {@link Span#putPrefixAndValue(int)} to commit the current sequence
 * of prefixes and the given value to the Trie.
 *
 * <p>Since spans share a common buffer for prefix data, it is important
 * that extended spans are consumed before the parent span is used again.
 * This is one reason why the API requires a consumer to be passed when a
 * span is extended.
 */
final class Trie {
    private final BytesTrieBuilder trieBuilder = new BytesTrieBuilder();
    private final byte[] spanBytes = new byte[24];

    /**
     * Represents a sequence of prefixes to be added to the underlying Trie
     * when a value is specified.
     *
     * <p>The position of a span cannot be modified, but they are not thread
     * safe (since they share the same underlying buffer).
     */
    final class Span {
        // The index *after* the last prefix was added.
        private final int index;

        // The root span.
        private Span() {
            this.index = 0;
        }

        // An extended span with the given prefix included.
        private Span(int index, String prefix) {
            checkArgument(index >= 0, "bad index: %s", index);
            checkState(!prefix.isEmpty(), "invalid subtag: %s", prefix);
            checkState(index + prefix.length() <= spanBytes.length, "span too long");
            if (prefix.equals("*")) {
                spanBytes[index++] = '*';
            } else {
                checkArgument(!prefix.contains("*"), "prefix must not contain '*': %s", prefix);
                for (int i = 0; i < prefix.length(); i++) {
                    char c = prefix.charAt(i);
                    checkArgument(c < LocaleDistance.END_OF_SUBTAG, "invalid trie character: %s", c);
                    spanBytes[index++] = (byte) c;
                }
                // Mark the final character as a terminator to avoid overlap matches.
                spanBytes[index - 1] |= (byte) LocaleDistance.END_OF_SUBTAG;
            }
            this.index = index;
        }

        /**
         * Extends the current span by creating a new span with the given ASCII
         * prefix data, and passing it to the given consumer. The original span is
         * not modified, but must not be used again until the consumer is finished.
         *
         * <p>The prefix string must contain only 7-bit ASCII characters.
         */
        public void with(String prefix, Consumer<Span> withFn) {
            withFn.accept(new Span(index, prefix));
        }

        /**
         * Commits the current prefix data and the given value to the underlying Trie.
         */
        public void putPrefixAndValue(int value) {
            checkArgument(value >= 0, "bad trie value: %s", value);
            checkState(index > 0, "missing prefix for value: %s", value);
            trieBuilder.add(spanBytes, index, value);
        }
    }

    /** Returns the root span with no current prefix data. */
    public Span root() {
        return new Span();
    }

    /** Serializes the underlying Trie data to a byte array (see also {@link BytesTrieBuilder}). */
    public byte[] toByteArray() {
        ByteBuffer buffer = trieBuilder.buildByteBuffer(BytesTrieBuilder.Option.SMALL);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
