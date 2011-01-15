/*
 *******************************************************************************
 * Copyright (C) 2011, Google, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.icu.impl.BytesTrie;
import com.ibm.icu.impl.BytesTrie.Result;
import com.ibm.icu.impl.BytesTrieBuilder;
import com.ibm.icu.impl.StringTrieBuilder.Option;
import com.ibm.icu.impl.Utility;


// would be nice to have a BytesTrieBuilder.add(aByte);
// question: can bytetrie store <"",x>?
// can you store the same string twice, eg add(bytes1, value), add(bytes1, value)? What happens? If an error,
// should happen on add, not on build.
// the BytesTrieBuilder.build should create a BytesTrie, not a raw array. For the latter, use buildArray or something.
// need class description; examples of usage; which method can/should be called after which others.


public class TrieMap<V> implements Iterable<Entry<CharSequence,V>>{
    private static final boolean DEBUG = true;
    private static final boolean COLLAPSE_EQUAL_VALUES = false;

    private final BytesTrie bytesTrie;
    private final V[] intToValue;
    private final int size;

    private TrieMap(BytesTrie bytesTrie, V[] intToValue, int size) {
        this.bytesTrie = bytesTrie;
        this.intToValue = intToValue;
        this.size = size;
    }

    public int keyByteSize() {
        return size;
    }

    public V get(CharSequence test) {
        int length = test.length();
        if (length == 0) {
            return null;
        }
        bytesTrie.reset();
        Result result = null;
        byte[] bytes = new byte[3];
        for (int i = 0; i < length; ++i) {
            char c = test.charAt(i);
            int limit = ByteConverter.getBytes(c, bytes, 0);
            for (int j = 0; j < limit; ++j) {
                result = bytesTrie.next(bytes[j]&0xFF);
                if (!result.matches()) {
                    return null;
                }
            }
        }
        return result.hasValue() ? intToValue[bytesTrie.getValue()] : null;
    }



    /**
     * Warning: the entry contents are only valid until the next next() call!!
     */
    public Iterator<Entry<CharSequence, V>> iterator() {
        // TODO Auto-generated method stub
        return new TrieIterator();
    }

    private class TrieIterator implements Iterator<Entry<CharSequence, V>> {
        BytesTrie.Iterator iterator = bytesTrie.iterator();
        TrieEntry entry = new TrieEntry();

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Entry<CharSequence, V> next() {
            entry.bytesEntry = iterator.next();
            return entry;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private class TrieEntry implements Entry<CharSequence, V> {
        public com.ibm.icu.impl.BytesTrie.Entry bytesEntry;
        StringBuilder buffer = new StringBuilder();

        public CharSequence getKey() {
            buffer.setLength(0);
            ByteConverter.getChars(bytesEntry, buffer);
            return buffer;
        }

        public V getValue() {
            return intToValue[bytesEntry.value];
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    public Matcher<V> getMatcher() {
        return Matcher.of(this);
    }

    public static class Matcher<V> {
        private TrieMap<V> map;
        private CharSequence text = "";
        private int start = 0;
        private int current = 0;
        private byte[] bytes = new byte[3];

        private V value = null;

        public static <V> Matcher<V>of(TrieMap<V> map) {
            Matcher<V> result = new Matcher<V>();
            result.map = map;
            return result;
        }

        public void set(CharSequence text, int start) {
            this.text  = text;
            this.start = start;
            this.current = start;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return current;
        }

        /**
         * Finds the next match. Returns false when there are no possible further matches from the current start point.
         * Once that happens, call nextStart(); 
         * Call getValue to get the current value.
         * @return false when done. There may be a value, however.
         */
        public boolean next() {
            while (current < text.length()) {
                char c = text.charAt(current++);
                int limit = ByteConverter.getBytes(c, bytes, 0);
                for (int j = 0; j < limit; ++j) {
                    Result result = map.bytesTrie.next(bytes[j]);
                    if (result.hasValue()) {
                        if (j < limit - 1) {
                            throw new IllegalArgumentException("Data corrupt");
                        }
                        value = map.intToValue[map.bytesTrie.getValue()];
                        return result.hasNext();
                    } else if (!result.matches()) {
                        value = null;
                        return false;
                    }
                }
            }
            value = null;
            return false;
        }

        public boolean nextStart() {
            if (start >= text.length()) {
                return false;
            }
            ++start;
            current = start;
            map.bytesTrie.reset();
            return true;
        }

        public V getValue() {
            return value;
        }
    }

    Collection<Matcher<V>> getMatches(CharSequence text, int offset) {
        return null;
    }

    public static class Builder<V> {
        BytesTrieBuilder btBuilder = new BytesTrieBuilder();
        List<V> intToValueTemp = new ArrayList<V>();
        Map<V, Integer> valueToIntegerTemp = new HashMap<V, Integer>();
        byte[] bytes = new byte[200];
        List<String> debugBytes = DEBUG ? new ArrayList<String>() : null;

        static public <V> Builder<V> make() {
            return new Builder<V>();
        }

        static public <V> Builder<V> of(Map<CharSequence, V> keyValuePairs) {
            Builder<V> result = make();
            return result.addAll(keyValuePairs);
        }

        static public <V> Builder<V> of(CharSequence key, V value) {
            Builder<V> result = make();
            return result.add(key, value);
        }

        public Builder<V> add(CharSequence key, V value) {
            // traverse the values, and get a mapping of a byte string to list of
            // integers, and a mapping from those integers to a set of values
            Integer index;
            if (COLLAPSE_EQUAL_VALUES) {
                index = valueToIntegerTemp.get(value);
                if (index == null) {
                    index = intToValueTemp.size();
                    intToValueTemp.add(value);
                    valueToIntegerTemp.put(value, index);
                }
            } else {
                index = intToValueTemp.size();
                intToValueTemp.add(value);
            }
            // dumb implementation for now
            // the buffer size is at most 3 * number_of_chars
            if (bytes.length < key.length()*3) {
                bytes = new byte[64 + key.length()*3];
            }
            int limit = 0;
            for (int i = 0; i < key.length(); ++i) {
                char c = key.charAt(i);
                limit = ByteConverter.getBytes(c, bytes, limit);
            }
            try {
                btBuilder.add(bytes,limit,index);
                return this;
            } catch (Exception e) {
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < limit; ++i) {
                    list.add(Utility.hex(bytes[i]));
                }
                throw new IllegalArgumentException("Failed to add " + value + ", " + key + "=" + list, e);
            }
        }

        public <K extends CharSequence> Builder<V> addAll(Map<K, V> keyValuePairs) {
            for (Entry<K, V> entry : keyValuePairs.entrySet()) {
                add(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public TrieMap<V> build() {
            // can't use 
            // BytesTrie bytesTrie = btBuilder.build(Option.SMALL);
            ByteBuffer buffer = btBuilder.buildByteBuffer(Option.SMALL);
            int size = buffer.remaining();
            byte[] bytes = new byte[size];
            buffer.get(bytes, 0, size);
            BytesTrie bytesTrie = new BytesTrie(bytes, 0);
            @SuppressWarnings("unchecked")
            V[] intToValueArray = intToValueTemp.toArray((V[])(new Object[intToValueTemp.size()]));
            return new TrieMap<V>(bytesTrie, intToValueArray, size);
        }
    }

    /**
     * Supports the following format for encoding chars (Unicode 16-bit code units). The format is slightly simpler and more compact than UTF8, but also maintains ordering. It is not, however
     * self-synchronizing, and is not intended for general usage
     * <pre>
     * 0000..007F - 0xxx xxxx
     * 0000..7E00 - 1yyy yyyy xxxx xxxx
     * 4000..FFFF - 1111 1111 yyyy yyyy xxxx xxxx
     * </pre>
     */
    static class ByteConverter {
        public static int getBytes(char source, byte[] bytes, int limit) {
            if (source < 0x80) {
                bytes[limit++] = (byte)source;
            } else if (source < 0x7E00) {
                bytes[limit++] = (byte)(0x80 | (source>>8));
                bytes[limit++] = (byte)source;
            } else {
                bytes[limit++] = (byte)-1;
                bytes[limit++] = (byte)(source>>8);
                bytes[limit++] = (byte)source;
            }
            return limit;
        }

        /**
         * Transform the string into a sequence of bytes, appending them after start, and return the new limit.
         */
        public static int getBytes(CharSequence source, byte[] bytes, int limit) {
            for (int i = 0; i < source.length(); ++i) {
                limit = getBytes(source.charAt(i), bytes, limit);
            }
            return limit;
        }

        /**
         * Transform a sequence of bytes into a string, according to the format in getBytes. No error checking.
         */
        public static String getChars(byte[] bytes, int start, int limit) {
            StringBuilder buffer = new StringBuilder();
            char[] output = new char[1];
            for (int i = start; i < limit;) {
                i = getChar(bytes, i, output);
                buffer.append(output[0]);
            }
            return buffer.toString();
        }

        public static int getChar(byte[] bytes, int start, char[] output) {
            byte b = bytes[start++];
            if (b >= 0) {
                output[0] = (char)b;
            } else if (b != (byte)-1) { // 2 bytes
                int b1 = 0x7F & b;
                int b2 = 0xFF & bytes[start++];
                output[0] = (char)((b1 << 8) | b2);
            } else {
                int b2 = 0xFF & bytes[start++];
                int b3 = 0xFF & bytes[start++];
                output[0] = (char)((b2 << 8) | b3);
            }
            return start;
        }


        private static void getChars(BytesTrie.Entry entry, StringBuilder stringBuilder) {
            int len = entry.bytesLength();
            for (int i = 0; i < len; ) {
                byte b = entry.byteAt(i++);
                if (b >= 0) {
                    stringBuilder.append((char)b);
                } else if (b != (byte)-1) { // 2 bytes
                    int b1 = 0x7F & b;
                    int b2 = 0xFF & entry.byteAt(i++);
                    stringBuilder.append((char)((b1 << 8) | b2));
                } else {
                    int b2 = 0xFF & entry.byteAt(i++);
                    int b3 = 0xFF & entry.byteAt(i++);
                    stringBuilder.append((char)((b2 << 8) | b3));
                }
            }
        }
    }

    public  String toString() {
        return toString(bytesTrie, " : ", "\n");
    }

    public static String toString(BytesTrie bytesTrie2) {
        return toString(bytesTrie2, " : ", "\n");
    }

    public static String toString(BytesTrie bytesTrie2, String keyValueSeparator, String itemSeparator) {
        StringBuilder buffer = new StringBuilder();
        BytesTrie.Iterator iterator = bytesTrie2.iterator();
        while (iterator.hasNext()) {
            BytesTrie.Entry bytesEntry = iterator.next();
            int len = bytesEntry.bytesLength();
            byte[] bytes = new byte[len];
            bytesEntry.copyBytesTo(bytes, 0);
            buffer.append(Utility.hex(bytes, 0, len, " "))
            .append(keyValueSeparator)
            .append(bytesEntry.value)
            .append(itemSeparator);
        }
        return buffer.toString();
    }
}

