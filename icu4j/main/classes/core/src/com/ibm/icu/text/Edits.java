// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.text;

import java.nio.BufferOverflowException;
import java.util.Arrays;

/**
 * Records lengths of string edits but not replacement text.
 * Supports replacements, insertions, deletions in linear progression.
 * Does not support moving/reordering of text.
 *
 * @draft ICU 59
 * @provisional This API might change or be removed in a future release.
 */
public final class Edits {
    // 0000uuuuuuuuuuuu records u+1 unchanged text units.
    private static final int MAX_UNCHANGED_LENGTH = 0x1000;
    private static final int MAX_UNCHANGED = MAX_UNCHANGED_LENGTH - 1;

    // 0wwwcccccccccccc with w=1..6 records ccc+1 replacements of w:w text units.
    // No length change.
    private static final int MAX_SHORT_WIDTH = 6;
    private static final int MAX_SHORT_CHANGE_LENGTH = 0xfff;
    private static final int MAX_SHORT_CHANGE = 0x6fff;

    // 0111mmmmmmnnnnnn records a replacement of m text units with n.
    // m or n = 61: actual length follows in the next edits array unit.
    // m or n = 62..63: actual length follows in the next two edits array units.
    // Bit 30 of the actual length is in the head unit.
    // Trailing units have bit 15 set.
    private static final int LENGTH_IN_1TRAIL = 61;
    private static final int LENGTH_IN_2TRAIL = 62;

    private static final int STACK_CAPACITY = 100;
    private char[] array;
    private int length;
    private int delta;
    private int numChanges;

    /**
     * Constructs an empty object.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public Edits() {
        array = new char[STACK_CAPACITY];
    }

    /**
     * Resets the data but may not release memory.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public void reset() {
        length = delta = numChanges = 0;
    }

    private void setLastUnit(int last) {
        array[length - 1] = (char)last;
    }
    private int lastUnit() {
        return length > 0 ? array[length - 1] : 0xffff;
    }

    /**
     * Adds a record for an unchanged segment of text.
     * Normally called from inside ICU string transformation functions, not user code.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public void addUnchanged(int unchangedLength) {
        if(unchangedLength < 0) {
            throw new IllegalArgumentException(
                    "addUnchanged(" + unchangedLength + "): length must not be negative");
        }
        // Merge into previous unchanged-text record, if any.
        int last = lastUnit();
        if(last < MAX_UNCHANGED) {
            int remaining = MAX_UNCHANGED - last;
            if (remaining >= unchangedLength) {
                setLastUnit(last + unchangedLength);
                return;
            }
            setLastUnit(MAX_UNCHANGED);
            unchangedLength -= remaining;
        }
        // Split large lengths into multiple units.
        while(unchangedLength >= MAX_UNCHANGED_LENGTH) {
            append(MAX_UNCHANGED);
            unchangedLength -= MAX_UNCHANGED_LENGTH;
        }
        // Write a small (remaining) length.
        if(unchangedLength > 0) {
            append(unchangedLength - 1);
        }
    }

    /**
     * Adds a record for a text replacement/insertion/deletion.
     * Normally called from inside ICU string transformation functions, not user code.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public void addReplace(int oldLength, int newLength) {
        if(oldLength == newLength && 0 < oldLength && oldLength <= MAX_SHORT_WIDTH) {
            // Replacement of short oldLength text units by same-length new text.
            // Merge into previous short-replacement record, if any.
            ++numChanges;
            int last = lastUnit();
            if(MAX_UNCHANGED < last && last < MAX_SHORT_CHANGE &&
                    (last >> 12) == oldLength && (last & 0xfff) < MAX_SHORT_CHANGE_LENGTH) {
                setLastUnit(last + 1);
                return;
            }
            append(oldLength << 12);
            return;
        }

        if(oldLength < 0 || newLength < 0) {
            throw new IllegalArgumentException(
                    "addReplace(" + oldLength + ", " + newLength +
                    "): both lengths must be non-negative");
        }
        if (oldLength == 0 && newLength == 0) {
            return;
        }
        ++numChanges;
        int newDelta = newLength - oldLength;
        if (newDelta != 0) {
            if ((newDelta > 0 && delta >= 0 && newDelta > (Integer.MAX_VALUE - delta)) ||
                    (newDelta < 0 && delta < 0 && newDelta < (Integer.MIN_VALUE - delta))) {
                // Integer overflow or underflow.
                throw new IndexOutOfBoundsException();
            }
            delta += newDelta;
        }

        int head = 0x7000;
        if (oldLength < LENGTH_IN_1TRAIL && newLength < LENGTH_IN_1TRAIL) {
            head |= oldLength << 6;
            head |= newLength;
            append(head);
        } else if ((array.length - length) >= 5 || growArray()) {
            int limit = length + 1;
            if(oldLength < LENGTH_IN_1TRAIL) {
                head |= oldLength << 6;
            } else if(oldLength <= 0x7fff) {
                head |= LENGTH_IN_1TRAIL << 6;
                array[limit++] = (char)(0x8000 | oldLength);
            } else {
                head |= (LENGTH_IN_2TRAIL + (oldLength >> 30)) << 6;
                array[limit++] = (char)(0x8000 | (oldLength >> 15));
                array[limit++] = (char)(0x8000 | oldLength);
            }
            if(newLength < LENGTH_IN_1TRAIL) {
                head |= newLength;
            } else if(newLength <= 0x7fff) {
                head |= LENGTH_IN_1TRAIL;
                array[limit++] = (char)(0x8000 | newLength);
            } else {
                head |= LENGTH_IN_2TRAIL + (newLength >> 30);
                array[limit++] = (char)(0x8000 | (newLength >> 15));
                array[limit++] = (char)(0x8000 | newLength);
            }
            array[length] = (char)head;
            length = limit;
        }
    }

    private void append(int r) {
        if(length < array.length || growArray()) {
            array[length++] = (char)r;
        }
    }

    private boolean growArray() {
        int newCapacity;
        if (array.length == STACK_CAPACITY) {
            newCapacity = 2000;
        } else if (array.length == Integer.MAX_VALUE) {
            throw new BufferOverflowException();
        } else if (array.length >= (Integer.MAX_VALUE / 2)) {
            newCapacity = Integer.MAX_VALUE;
        } else {
            newCapacity = 2 * array.length;
        }
        // Grow by at least 5 units so that a maximal change record will fit.
        if ((newCapacity - array.length) < 5) {
            throw new BufferOverflowException();
        }
        array = Arrays.copyOf(array, newCapacity);
        return true;
    }

    /**
     * How much longer is the new text compared with the old text?
     * @return new length minus old length
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public int lengthDelta() { return delta; }
    /**
     * @return true if there are any change edits
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public boolean hasChanges()  { return numChanges != 0; }

    /**
     * @return the number of change edits
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    public int numberOfChanges() { return numChanges; }

    /**
     * Access to the list of edits.
     * @see #getCoarseIterator
     * @see #getFineIterator
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public static final class Iterator {
        private final char[] array;
        private int index;
        private final int length;
        private int remaining;
        private final boolean onlyChanges_, coarse;

        private boolean changed;
        private int oldLength_, newLength_;
        private int srcIndex, replIndex, destIndex;

        private Iterator(char[] a, int len, boolean oc, boolean crs) {
            array = a;
            length = len;
            onlyChanges_ = oc;
            coarse = crs;
        }

        private int readLength(int head) {
            if (head < LENGTH_IN_1TRAIL) {
                return head;
            } else if (head < LENGTH_IN_2TRAIL) {
                assert(index < length);
                assert(array[index] >= 0x8000);
                return array[index++] & 0x7fff;
            } else {
                assert((index + 2) <= length);
                assert(array[index] >= 0x8000);
                assert(array[index + 1] >= 0x8000);
                int len = ((head & 1) << 30) |
                        ((array[index] & 0x7fff) << 15) |
                        (array[index + 1] & 0x7fff);
                index += 2;
                return len;
            }
        }

        private void updateIndexes() {
            srcIndex += oldLength_;
            if (changed) {
                replIndex += newLength_;
            }
            destIndex += newLength_;
        }

        private boolean noNext() {
            // No change beyond the string.
            changed = false;
            oldLength_ = newLength_ = 0;
            return false;
        }

        /**
         * Advances to the next edit.
         * @return true if there is another edit
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public boolean next() {
            return next(onlyChanges_);
        }

        private boolean next(boolean onlyChanges) {
            // We have an errorCode in case we need to start guarding against integer overflows.
            // It is also convenient for caller loops if we bail out when an error was set elsewhere.
            updateIndexes();
            if (remaining > 0) {
                // Fine-grained iterator: Continue a sequence of equal-length changes.
                --remaining;
                return true;
            }
            if (index >= length) {
                return noNext();
            }
            int u = array[index++];
            if (u <= MAX_UNCHANGED) {
                // Combine adjacent unchanged ranges.
                changed = false;
                oldLength_ = u + 1;
                while (index < length && (u = array[index]) <= MAX_UNCHANGED) {
                    ++index;
                    oldLength_ += u + 1;
                }
                newLength_ = oldLength_;
                if (onlyChanges) {
                    updateIndexes();
                    if (index >= length) {
                        return noNext();
                    }
                    // already fetched u > MAX_UNCHANGED at index
                    ++index;
                } else {
                    return true;
                }
            }
            changed = true;
            if (u <= MAX_SHORT_CHANGE) {
                if (coarse) {
                    int w = u >> 12;
                    int len = (u & 0xfff) + 1;
                    oldLength_ = newLength_ = len * w;
                } else {
                    // Split a sequence of equal-length changes that was compressed into one unit.
                    oldLength_ = newLength_ = u >> 12;
                    remaining = u & 0xfff;
                    return true;
                }
            } else {
                assert(u <= 0x7fff);
                oldLength_ = readLength((u >> 6) & 0x3f);
                newLength_ = readLength(u & 0x3f);
                if (!coarse) {
                    return true;
                }
            }
            // Combine adjacent changes.
            while (index < length && (u = array[index]) > MAX_UNCHANGED) {
                ++index;
                if (u <= MAX_SHORT_CHANGE) {
                    int w = u >> 12;
                    int len = (u & 0xfff) + 1;
                    len = len * w;
                    oldLength_ += len;
                    newLength_ += len;
                } else {
                    assert(u <= 0x7fff);
                    int oldLen = readLength((u >> 6) & 0x3f);
                    int newLen = readLength(u & 0x3f);
                    oldLength_ += oldLen;
                    newLength_ += newLen;
                }
            }
            return true;
        }

        /**
         * Finds the edit that contains the source index.
         * The source index may be found in a non-change
         * even if normal iteration would skip non-changes.
         * Normal iteration can continue from a found edit.
         *
         * <p>The iterator state before this search logically does not matter.
         * (It may affect the performance of the search.)
         *
         * <p>The iterator state after this search is undefined
         * if the source index is out of bounds for the source string.
         *
         * @param i source index
         * @return true if the edit for the source index was found
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public boolean findSourceIndex(int i) {
            return findIndex(i, true) == 0;
        }

        /**
         * Finds the edit that contains the destination index.
         * The destination index may be found in a non-change
         * even if normal iteration would skip non-changes.
         * Normal iteration can continue from a found edit.
         *
         * <p>The iterator state before this search logically does not matter.
         * (It may affect the performance of the search.)
         *
         * <p>The iterator state after this search is undefined
         * if the source index is out of bounds for the source string.
         *
         * @param i destination index
         * @return true if the edit for the destination index was found
         * @draft ICU 60
         * @provisional This API might change or be removed in a future release.
         */
        public boolean findDestinationIndex(int i) {
            return findIndex(i, false) == 0;
        }

        /** @return -1: error or i<0; 0: found; 1: i>=string length */
        private int findIndex(int i, boolean findSource) {
            if (i < 0) { return -1; }
            int spanStart, spanLength;
            if (findSource) {  // find source index
                spanStart = srcIndex;
                spanLength = oldLength_;
            } else {  // find destination index
                spanStart = destIndex;
                spanLength = newLength_;
            }
            if (i < spanStart) {
                // Reset the iterator to the start.
                index = remaining = oldLength_ = newLength_ = srcIndex = replIndex = destIndex = 0;
            } else if (i < (spanStart + spanLength)) {
                // The index is in the current span.
                return 0;
            }
            while (next(false)) {
                if (findSource) {
                    spanStart = srcIndex;
                    spanLength = oldLength_;
                } else {
                    spanStart = destIndex;
                    spanLength = newLength_;
                }
                if (i < (spanStart + spanLength)) {
                    // The index is in the current span.
                    return 0;
                }
                if (remaining > 0) {
                    // Is the index in one of the remaining compressed edits?
                    // spanStart is the start of the current span, before the remaining ones.
                    int len = (remaining + 1) * spanLength;
                    if (i < (spanStart + len)) {
                        int n = (i - spanStart) / spanLength;  // 1 <= n <= remaining
                        len = n * spanLength;
                        srcIndex += len;
                        replIndex += len;
                        destIndex += len;
                        remaining -= n;
                        return 0;
                    }
                    // Make next() skip all of these edits at once.
                    oldLength_ = newLength_ = len;
                    remaining = 0;
                }
            }
            return 1;
        }

        /**
         * Returns the destination index corresponding to the given source index.
         * If the source index is inside a change edit (not at its start),
         * then the destination index at the end of that edit is returned,
         * since there is no information about index mapping inside a change edit.
         *
         * <p>(This means that indexes to the start and middle of an edit,
         * for example around a grapheme cluster, are mapped to indexes
         * encompassing the entire edit.
         * The alternative, mapping an interior index to the start,
         * would map such an interval to an empty one.)
         *
         * <p>This operation will usually but not always modify this object.
         * The iterator state after this search is undefined.
         *
         * @param i source index
         * @return destination index; undefined if i is not 0..string length
         * @draft ICU 60
         * @provisional This API might change or be removed in a future release.
         */
        public int destinationIndexFromSourceIndex(int i) {
            int where = findIndex(i, true);
            if (where < 0) {
                // Error or before the string.
                return 0;
            }
            if (where > 0 || i == srcIndex) {
                // At or after string length, or at start of the found span.
                return destIndex;
            }
            if (changed) {
                // In a change span, map to its end.
                return destIndex + newLength_;
            } else {
                // In an unchanged span, offset 1:1 within it.
                return destIndex + (i - srcIndex);
            }
        }

        /**
         * Returns the source index corresponding to the given destination index.
         * If the destination index is inside a change edit (not at its start),
         * then the source index at the end of that edit is returned,
         * since there is no information about index mapping inside a change edit.
         *
         * <p>(This means that indexes to the start and middle of an edit,
         * for example around a grapheme cluster, are mapped to indexes
         * encompassing the entire edit.
         * The alternative, mapping an interior index to the start,
         * would map such an interval to an empty one.)
         *
         * <p>This operation will usually but not always modify this object.
         * The iterator state after this search is undefined.
         *
         * @param i destination index
         * @return source index; undefined if i is not 0..string length
         * @draft ICU 60
         * @provisional This API might change or be removed in a future release.
         */
        public int sourceIndexFromDestinationIndex(int i) {
            int where = findIndex(i, false);
            if (where < 0) {
                // Error or before the string.
                return 0;
            }
            if (where > 0 || i == destIndex) {
                // At or after string length, or at start of the found span.
                return srcIndex;
            }
            if (changed) {
                // In a change span, map to its end.
                return srcIndex + oldLength_;
            } else {
                // In an unchanged span, offset within it.
                return srcIndex + (i - destIndex);
            }
        }

        /**
         * @return true if this edit replaces oldLength() units with newLength() different ones.
         *         false if oldLength units remain unchanged.
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public boolean hasChange() { return changed; }
        /**
         * @return the number of units in the original string which are replaced or remain unchanged.
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public int oldLength() { return oldLength_; }
        /**
         * @return the number of units in the modified string, if hasChange() is true.
         *         Same as oldLength if hasChange() is false.
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public int newLength() { return newLength_; }

        /**
         * @return the current index into the source string
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public int sourceIndex() { return srcIndex; }
        /**
         * @return the current index into the replacement-characters-only string,
         *         not counting unchanged spans
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public int replacementIndex() { return replIndex; }
        /**
         * @return the current index into the full destination string
         * @draft ICU 59
         * @provisional This API might change or be removed in a future release.
         */
        public int destinationIndex() { return destIndex; }
    };

    /**
     * Returns an Iterator for coarse-grained changes for simple string updates.
     * Skips non-changes.
     * @return an Iterator that merges adjacent changes.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public Iterator getCoarseChangesIterator() {
        return new Iterator(array, length, true, true);
    }

    /**
     * Returns an Iterator for coarse-grained changes and non-changes for simple string updates.
     * @return an Iterator that merges adjacent changes.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public Iterator getCoarseIterator() {
        return new Iterator(array, length, false, true);
    }

    /**
     * Returns an Iterator for fine-grained changes for modifying styled text.
     * Skips non-changes.
     * @return an Iterator that separates adjacent changes.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public Iterator getFineChangesIterator() {
        return new Iterator(array, length, true, false);
    }

    /**
     * Returns an Iterator for fine-grained changes and non-changes for modifying styled text.
     * @return an Iterator that separates adjacent changes.
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public Iterator getFineIterator() {
        return new Iterator(array, length, false, false);
    }

    /**
     * Merges the two input Edits and appends the result to this object.
     *
     * <p>Consider two string transformations (for example, normalization and case mapping)
     * where each records Edits in addition to writing an output string.<br>
     * Edits ab reflect how substrings of input string a
     * map to substrings of intermediate string b.<br>
     * Edits bc reflect how substrings of intermediate string b
     * map to substrings of output string c.<br>
     * This function merges ab and bc such that the additional edits
     * recorded in this object reflect how substrings of input string a
     * map to substrings of output string c.
     *
     * <p>If unrelated Edits are passed in where the output string of the first
     * has a different length than the input string of the second,
     * then an IllegalArgumentException is thrown.
     *
     * @param ab reflects how substrings of input string a
     *     map to substrings of intermediate string b.
     * @param bc reflects how substrings of intermediate string b
     *     map to substrings of output string c.
     * @return this, with the merged edits appended
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    public Edits mergeAndAppend(Edits ab, Edits bc) {
        // Picture string a --(Edits ab)--> string b --(Edits bc)--> string c.
        // Parallel iteration over both Edits.
        Iterator abIter = ab.getFineIterator();
        Iterator bcIter = bc.getFineIterator();
        boolean abHasNext = true, bcHasNext = true;
        // Copy iterator state into local variables, so that we can modify and subdivide spans.
        // ab old & new length, bc old & new length
        int aLength = 0, ab_bLength = 0, bc_bLength = 0, cLength = 0;
        // When we have different-intermediate-length changes, we accumulate a larger change.
        int pending_aLength = 0, pending_cLength = 0;
        for (;;) {
            // At this point, for each of the two iterators:
            // Either we are done with the locally cached current edit,
            // and its intermediate-string length has been reset,
            // or we will continue to work with a truncated remainder of this edit.
            //
            // If the current edit is done, and the iterator has not yet reached the end,
            // then we fetch the next edit. This is true for at least one of the iterators.
            //
            // Normally it does not matter whether we fetch from ab and then bc or vice versa.
            // However, the result is observably different when
            // ab deletions meet bc insertions at the same intermediate-string index.
            // Some users expect the bc insertions to come first, so we fetch from bc first.
            if (bc_bLength == 0) {
                if (bcHasNext && (bcHasNext = bcIter.next())) {
                    bc_bLength = bcIter.oldLength();
                    cLength = bcIter.newLength();
                    if (bc_bLength == 0) {
                        // insertion
                        if (ab_bLength == 0 || !abIter.hasChange()) {
                            addReplace(pending_aLength, pending_cLength + cLength);
                            pending_aLength = pending_cLength = 0;
                        } else {
                            pending_cLength += cLength;
                        }
                        continue;
                    }
                }
                // else see if the other iterator is done, too.
            }
            if (ab_bLength == 0) {
                if (abHasNext && (abHasNext = abIter.next())) {
                    aLength = abIter.oldLength();
                    ab_bLength = abIter.newLength();
                    if (ab_bLength == 0) {
                        // deletion
                        if (bc_bLength == bcIter.oldLength() || !bcIter.hasChange()) {
                            addReplace(pending_aLength + aLength, pending_cLength);
                            pending_aLength = pending_cLength = 0;
                        } else {
                            pending_aLength += aLength;
                        }
                        continue;
                    }
                } else if (bc_bLength == 0) {
                    // Both iterators are done at the same time:
                    // The intermediate-string lengths match.
                    break;
                } else {
                    throw new IllegalArgumentException(
                            "The ab output string is shorter than the bc input string.");
                }
            }
            if (bc_bLength == 0) {
                throw new IllegalArgumentException(
                        "The bc input string is shorter than the ab output string.");
            }
            //  Done fetching: ab_bLength > 0 && bc_bLength > 0

            // The current state has two parts:
            // - Past: We accumulate a longer ac edit in the "pending" variables.
            // - Current: We have copies of the current ab/bc edits in local variables.
            //   At least one side is newly fetched.
            //   One side might be a truncated remainder of an edit we fetched earlier.

            if (!abIter.hasChange() && !bcIter.hasChange()) {
                // An unchanged span all the way from string a to string c.
                if (pending_aLength != 0 || pending_cLength != 0) {
                    addReplace(pending_aLength, pending_cLength);
                    pending_aLength = pending_cLength = 0;
                }
                int unchangedLength = aLength <= cLength ? aLength : cLength;
                addUnchanged(unchangedLength);
                ab_bLength = aLength -= unchangedLength;
                bc_bLength = cLength -= unchangedLength;
                // At least one of the unchanged spans is now empty.
                continue;
            }
            if (!abIter.hasChange() && bcIter.hasChange()) {
                // Unchanged a->b but changed b->c.
                if (ab_bLength >= bc_bLength) {
                    // Split the longer unchanged span into change + remainder.
                    addReplace(pending_aLength + bc_bLength, pending_cLength + cLength);
                    pending_aLength = pending_cLength = 0;
                    aLength = ab_bLength -= bc_bLength;
                    bc_bLength = 0;
                    continue;
                }
                // Handle the shorter unchanged span below like a change.
            } else if (abIter.hasChange() && !bcIter.hasChange()) {
                // Changed a->b and then unchanged b->c.
                if (ab_bLength <= bc_bLength) {
                    // Split the longer unchanged span into change + remainder.
                    addReplace(pending_aLength + aLength, pending_cLength + ab_bLength);
                    pending_aLength = pending_cLength = 0;
                    cLength = bc_bLength -= ab_bLength;
                    ab_bLength = 0;
                    continue;
                }
                // Handle the shorter unchanged span below like a change.
            } else {  // both abIter.hasChange() && bcIter.hasChange()
                if (ab_bLength == bc_bLength) {
                    // Changes on both sides up to the same position. Emit & reset.
                    addReplace(pending_aLength + aLength, pending_cLength + cLength);
                    pending_aLength = pending_cLength = 0;
                    ab_bLength = bc_bLength = 0;
                    continue;
                }
            }
            // Accumulate the a->c change, reset the shorter side,
            // keep a remainder of the longer one.
            pending_aLength += aLength;
            pending_cLength += cLength;
            if (ab_bLength < bc_bLength) {
                bc_bLength -= ab_bLength;
                cLength = ab_bLength = 0;
            } else {  // ab_bLength > bc_bLength
                ab_bLength -= bc_bLength;
                aLength = bc_bLength = 0;
            }
        }
        if (pending_aLength != 0 || pending_cLength != 0) {
            addReplace(pending_aLength, pending_cLength);
        }
        return this;
    }
}
