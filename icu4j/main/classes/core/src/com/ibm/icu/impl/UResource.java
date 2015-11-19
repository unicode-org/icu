/*
 *******************************************************************************
 * Copyright (C) 2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.nio.ByteBuffer;

import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceTypeMismatchException;

// Class UResource is named consistently with the public class UResourceBundle,
// in case we want to make it public at some point.

/**
 * ICU resource bundle key and value types.
 */
public final class UResource {
    /**
     * Represents a resource bundle item's key string.
     * Avoids object creations as much as possible.
     * Mutable, not thread-safe.
     * For permanent storage, use clone() or toString().
     */
    public static final class Key implements CharSequence, Cloneable, Comparable<Key> {
        // Stores a reference to the resource bundle key string bytes array,
        // with an offset of the key, to avoid creating a String object
        // until one is really needed.
        // Alternatively, we could try to always just get the key String object,
        // and cache it in the reader, and see if that performs better or worse.
        private byte[] bytes;
        private int offset;
        private int length;
        private String s;

        /**
         * Constructs an empty resource key string object.
         */
        public Key() {}

        private Key(byte[] keyBytes, int keyOffset, int keyLength) {
            bytes = keyBytes;
            offset = keyOffset;
            length = keyLength;
        }

        /**
         * Mutates this key for a new NUL-terminated resource key string.
         * The corresponding ASCII-character bytes are not copied and
         * must not be changed during the lifetime of this key
         * (or until the next setBytes() call)
         * and lifetimes of subSequences created from this key.
         *
         * @param keyBytes new key string byte array
         * @param keyOffset new key string offset
         */
        public void setBytes(byte[] keyBytes, int keyOffset) {
            bytes = keyBytes;
            offset = keyOffset;
            for (length = 0; keyBytes[keyOffset + length] != 0; ++length) {}
            s = null;
        }

        /**
         * Mutates this key to an empty resource key string.
         */
        public void setToEmpty() {
            bytes = null;
            offset = length = 0;
            s = null;
        }

        /**
         * {@inheritDoc}
         * Does not clone the byte array.
         */
        @Override
        public Key clone() {
            try {
                return (Key)super.clone();
            } catch (CloneNotSupportedException cannotOccur) {
                return null;
            }
        }

        // TODO: Java 6: @Override
        public char charAt(int i) {
            assert(0 <= i && i < length);
            return (char)bytes[offset + i];
        }

        // TODO: Java 6: @Override
        public int length() {
            return length;
        }

        // TODO: Java 6: @Override
        public Key subSequence(int start, int end) {
            assert(0 <= start && start < length);
            assert(start <= end && end <= length);
            return new Key(bytes, offset + start, end - start);
        }

        /**
         * Creates/caches/returns this resource key string as a Java String.
         */
        public String toString() {
            if (s == null) {
                s = internalSubString(0, length);
            }
            return s;
        }

        private String internalSubString(int start, int end) {
            StringBuilder sb = new StringBuilder(end - start);
            for (int i = start; i < end; ++i) {
                sb.append((char)bytes[offset + i]);
            }
            return sb.toString();
        }

        /**
         * Creates a new Java String for a sub-sequence of this resource key string.
         */
        public String substring(int start) {
            assert(0 <= start && start < length);
            return internalSubString(start, length);
        }

        /**
         * Creates a new Java String for a sub-sequence of this resource key string.
         */
        public String substring(int start, int end) {
            assert(0 <= start && start < length);
            assert(start <= end && end <= length);
            return internalSubString(start, end);
        }

        private boolean regionMatches(byte[] otherBytes, int otherOffset, int n) {
            for (int i = 0; i < n; ++i) {
                if (bytes[offset + i] != otherBytes[otherOffset + i]) {
                    return false;
                }
            }
            return true;
        }

        private boolean regionMatches(int start, CharSequence cs, int n) {
            for (int i = 0; i < n; ++i) {
                if (bytes[offset + start + i] != cs.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            } else if (this == other) {
                return true;
            } else if (other instanceof Key) {
                Key otherKey = (Key)other;
                return length == otherKey.length &&
                        regionMatches(otherKey.bytes, otherKey.offset, length);
            } else {
                return false;
            }
        }

        public boolean contentEquals(CharSequence cs) {
            if (cs == null) {
                return false;
            }
            return this == cs || (cs.length() == length && regionMatches(0, cs, length));
        }

        public boolean startsWith(CharSequence cs) {
            int csLength = cs.length();
            return csLength <= length && regionMatches(0, cs, csLength);
        }

        public boolean endsWith(CharSequence cs) {
            int csLength = cs.length();
            return csLength <= length && regionMatches(length - csLength, cs, csLength);
        }

        /**
         * @return true if the substring of this key starting from the offset
         *         contains the same characters as the other sequence.
         */
        public boolean regionMatches(int start, CharSequence cs) {
            int csLength = cs.length();
            return csLength == (length - start) && regionMatches(start, cs, csLength);
        }

        @Override
        public int hashCode() {
            // Never return s.hashCode(), so that
            // Key.hashCode() is the same whether we have cached s or not.
            if (length == 0) {
                return 0;
            }

            int h = bytes[offset];
            for (int i = 1; i < length; ++i) {
                h = 37 * h + bytes[offset];
            }
            return h;
        }

        // TODO: Java 6: @Override
        public int compareTo(Key other) {
            return compareTo((CharSequence)other);
        }

        public int compareTo(CharSequence cs) {
            int csLength = cs.length();
            int minLength = length <= csLength ? length : csLength;
            for (int i = 0; i < minLength; ++i) {
                int diff = (int)charAt(i) - (int)cs.charAt(i);
                if (diff != 0) {
                    return diff;
                }
            }
            return length - csLength;
        }
    }

    /**
     * Represents a resource bundle item's value.
     * Avoids object creations as much as possible.
     * Mutable, not thread-safe.
     */
    public static abstract class Value {
        protected Value() {}

        /**
         * @return ICU resource type like {@link UResourceBundle#getType()},
         *     for example, {@link UResourceBundle#STRING}
         */
        public abstract int getType();

        /**
         * @see UResourceBundle#getString()
         * @throws UResourceTypeMismatchException if this is not a string resource
         */
        public abstract String getString();

        /**
         * @throws UResourceTypeMismatchException if this is not an alias resource
         */
        public abstract String getAliasString();

        /**
         * @see UResourceBundle#getInt()
         * @throws UResourceTypeMismatchException if this is not an integer resource
         */
        public abstract int getInt();

        /**
         * @see UResourceBundle#getUInt()
         * @throws UResourceTypeMismatchException if this is not an integer resource
         */
        public abstract int getUInt();

        /**
         * @see UResourceBundle#getIntVector()
         * @throws UResourceTypeMismatchException if this is not an intvector resource
         */
        public abstract int[] getIntVector();

        /**
         * @see UResourceBundle#getBinary()
         * @throws UResourceTypeMismatchException if this is not a binary-blob resource
         */
        public abstract ByteBuffer getBinary();

        /**
         * Only for debugging.
         */
        @Override
        public String toString() {
            switch(getType()) {
            case UResourceBundle.STRING:
                return getString();
            case UResourceBundle.INT:
                return Integer.toString(getInt());
            case UResourceBundle.INT_VECTOR:
                int[] iv = getIntVector();
                StringBuilder sb = new StringBuilder("[");
                sb.append(iv.length).append("]{");
                if (iv.length != 0) {
                    sb.append(iv[0]);
                    for (int i = 1; i < iv.length; ++i) {
                        sb.append(", ").append(iv[i]);
                    }
                }
                return sb.append('}').toString();
            case UResourceBundle.BINARY:
                return "(binary blob)";
            case UResourceBundle.ARRAY:  // should not occur
                return "(array)";
            case UResourceBundle.TABLE:  // should not occur
                return "(table)";
            default:  // should not occur
                return "???";
            }
        }
    }

    /**
     * Sink for ICU resource array contents.
     * The base class does nothing.
     *
     * <p>Nested arrays and tables are stored as nested sinks,
     * never put() as {@link Value} items.
     */
    public static class ArraySink {
        /**
         * Adds a value from a resource array.
         *
         * @param index of the resource array item
         * @param value resource value
         */
        public void put(int index, Value value) {}

        /**
         * Returns a nested resource array at the array index as another sink.
         * Creates the sink if none exists for the key.
         * Returns null if nested arrays are not supported.
         * The default implementation always returns null.
         *
         * @param index of the resource array item
         * @param size number of array items
         * @return nested-array sink, or null
         */
        public ArraySink getOrCreateArraySink(int index, int size) {
            return null;
        }

        /**
         * Returns a nested resource table at the array index as another sink.
         * Creates the sink if none exists for the key.
         * Returns null if nested tables are not supported.
         * The default implementation always returns null.
         *
         * @param index of the resource array item
         * @param initialSize size hint for creating the sink if necessary
         * @return nested-table sink, or null
         */
        public TableSink getOrCreateTableSink(int index, int initialSize) {
            return null;
        }

        /**
         * "Leaves" the array.
         * Indicates that all of the resources and sub-resources of the current array
         * have been enumerated.
         */
        public void leave() {}
    }

    /**
     * Sink for ICU resource table contents.
     * The base class does nothing.
     *
     * <p>Nested arrays and tables are stored as nested sinks,
     * never put() as {@link Value} items.
     */
    public static class TableSink {
        /**
         * Adds a key-value pair from a resource table.
         *
         * @param key resource key string
         * @param value resource value
         */
        public void put(Key key, Value value) {}

        /**
         * Adds a no-fallback/no-inheritance marker for this key.
         * Used for CLDR no-fallback data values of "∅∅∅"
         * when enumerating tables with fallback from the specific resource bundle to root.
         *
         * <p>The default implementation does nothing.
         *
         * @param key to be removed
         */
        public void putNoFallback(Key key) {}

        /**
         * Returns a nested resource array for the key as another sink.
         * Creates the sink if none exists for the key.
         * Returns null if nested arrays are not supported.
         * The default implementation always returns null.
         *
         * @param key resource key string
         * @param size number of array items
         * @return nested-array sink, or null
         */
        public ArraySink getOrCreateArraySink(Key key, int size) {
            return null;
        }

        /**
         * Returns a nested resource table for the key as another sink.
         * Creates the sink if none exists for the key.
         * Returns null if nested tables are not supported.
         * The default implementation always returns null.
         *
         * @param key resource key string
         * @param initialSize size hint for creating the sink if necessary
         * @return nested-table sink, or null
         */
        public TableSink getOrCreateTableSink(Key key, int initialSize) {
            return null;
        }

        /**
         * "Leaves" the table.
         * Indicates that all of the resources and sub-resources of the current table
         * have been enumerated.
         */
        public void leave() {}
    }
}
