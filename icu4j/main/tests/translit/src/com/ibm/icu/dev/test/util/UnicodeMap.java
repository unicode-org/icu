/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Freezable;

/**
 * Class for mapping Unicode characters and strings to values, optimized for single code points, 
 * where ranges of code points have the same value.
 * Much smaller storage than using HashMap, and much faster and more compact than
 * a list of UnicodeSets. The API design mimics Map<String,T> but can't extend it due to some
 * necessary changes (much as UnicodeSet mimics Set<String>). Note that nulls are not permitted as values;
 * that is, a put(x,null) is the same as remove(x).<br>
 * At this point "" is also not allowed as a key, although that may change.
 * @author markdavis
 */

public final class UnicodeMap<T> implements Cloneable, Freezable, StringTransform, Iterable<String> {
    /**
     * For serialization
     */
    //private static final long serialVersionUID = -6540936876295804105L;
    static final boolean ASSERTIONS = false;
    static final long GROWTH_PERCENT = 200; // 100 is no growth!
    static final long GROWTH_GAP = 10; // extra bump!

    private int length;
    // two parallel arrays to save memory. Wish Java had structs.
    private int[] transitions;
    /* package private */ T[] values;

    private LinkedHashSet<T> availableValues = new LinkedHashSet<T>();
    private transient boolean staleAvailableValues;

    private transient boolean errorOnReset;
    private transient boolean locked;
    private int lastIndex;
    private Map<String,T> stringMap;

    { clear(); }

    public UnicodeMap<T> clear() {
        if (locked) {
            throw new UnsupportedOperationException("Attempt to modify locked object");
        }
        length = 2;
        transitions = new int[] {0,0x110000,0,0,0,0,0,0,0,0};
        values = (T[]) new Object[10];

        availableValues.clear();
        staleAvailableValues = false;

        errorOnReset = false;
        lastIndex = 0;
        stringMap = null;
        return this;
    }

    /* Boilerplate */
    public boolean equals(Object other) {
        if (other == null) return false;
        try {
            UnicodeMap that = (UnicodeMap) other;
            if (length != that.length) return false;
            for (int i = 0; i < length-1; ++i) {
                if (transitions[i] != that.transitions[i]) return false;
                if (!areEqual(values[i], that.values[i])) return false;
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public static boolean areEqual(Object a , Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public int hashCode() {
        int result = length;
        // TODO might want to abbreviate this for speed.
        for (int i = 0; i < length-1; ++i) {
            result = 37*result + transitions[i];
            result = 37*result + values[i].hashCode();
        }
        return result;
    }

    /**
     * Standard clone. Warning, as with Collections, does not do deep clone.
     */
    public UnicodeMap<T> cloneAsThawed() {
        UnicodeMap<T> that = new UnicodeMap<T>();
        that.length = length;
        that.transitions = (int[]) transitions.clone();
        that.values = (T[]) values.clone();
        that.availableValues = new LinkedHashSet<T>(availableValues);
        that.locked = false;
        return that;
    }

    /* for internal consistency checking */

    void _checkInvariants() {
        if (length < 2
                || length > transitions.length
                || transitions.length != values.length) {
            throw new IllegalArgumentException("Invariant failed: Lengths bad");
        }
        for (int i = 1; i < length-1; ++i) {
            if (areEqual(values[i-1], values[i])) {
                throw new IllegalArgumentException("Invariant failed: values shared at " 
                        + "\t" + Utility.hex(i-1) + ": <" + values[i-1] + ">"
                        + "\t" + Utility.hex(i) + ": <" + values[i] + ">"
                );
            }
        }
        if (transitions[0] != 0 || transitions[length-1] != 0x110000) {
            throw new IllegalArgumentException("Invariant failed: bounds set wrong");
        }
        for (int i = 1; i < length-1; ++i) {
            if (transitions[i-1] >= transitions[i]) {
                throw new IllegalArgumentException("Invariant failed: not monotonic"
                        + "\t" + Utility.hex(i-1) + ": " + transitions[i-1]
                                                                       + "\t" + Utility.hex(i) + ": " + transitions[i]
                );
            }
        }
    }

    /**
     * Finds an index such that inversionList[i] <= codepoint < inversionList[i+1]
     * Assumes that 0 <= codepoint <= 0x10FFFF
     * @param codepoint
     * @return the index
     */
    private int _findIndex(int c) {
        int lo = 0;
        int hi = length - 1;
        int i = (lo + hi) >>> 1;
        // invariant: c >= list[lo]
        // invariant: c < list[hi]
        while (i != lo) {
            if (c < transitions[i]) {
                hi = i;
            } else {
                lo = i;
            }
            i = (lo + hi) >>> 1;
        }
        if (ASSERTIONS) _checkFind(c, lo);
        return lo;
    }

    private void _checkFind(int codepoint, int value) {
        int other = __findIndex(codepoint);
        if (other != value) {
            throw new IllegalArgumentException("Invariant failed: binary search"
                    + "\t" + Utility.hex(codepoint) + ": " + value
                    + "\tshould be: " + other);            
        }
    }

    private int __findIndex(int codepoint) {
        for (int i = length-1; i > 0; --i) {
            if (transitions[i] <= codepoint) return i;
        }
        return 0;
    }

    /*
     * Try indexed lookup

    static final int SHIFT = 8;
    int[] starts = new int[0x10FFFF>>SHIFT]; // lowest transition index where codepoint>>x can be found
    boolean startsValid = false;
    private int findIndex(int codepoint) {
        if (!startsValid) {
            int start = 0;
            for (int i = 1; i < length; ++i) {

            }
        }
        for (int i = length-1; i > 0; --i) {
           if (transitions[i] <= codepoint) return i;
       }
       return 0;
   }
     */

    /**
     * Remove the items from index through index+count-1.
     * Logically reduces the size of the internal arrays.
     * @param index
     * @param count
     */
    private void _removeAt(int index, int count) {
        for (int i = index + count; i < length; ++i) {
            transitions[i-count] = transitions[i];
            values[i-count] = values[i];
        }
        length -= count;
    }
    /**
     * Add a gap from index to index+count-1.
     * The values there are undefined, and must be set.
     * Logically grows arrays to accomodate. Actual growth is limited
     * @param index
     * @param count
     */
    private void _insertGapAt(int index, int count) {
        int newLength = length + count;
        int[] oldtransitions = transitions;
        T[] oldvalues = values;
        if (newLength > transitions.length) {
            int allocation = (int) (GROWTH_GAP + (newLength * GROWTH_PERCENT) / 100);
            transitions = new int[allocation];
            values = (T[]) new Object[allocation];
            for (int i = 0; i < index; ++i) {
                transitions[i] = oldtransitions[i];
                values[i] = oldvalues[i];
            }
        } 
        for (int i = length - 1; i >= index; --i) {
            transitions[i+count] = oldtransitions[i];
            values[i+count] = oldvalues[i];
        }
        length = newLength;
    }

    /**
     * Associates code point with value. Removes any previous association.
     * All code that calls this MUST check for frozen first!
     * @param codepoint
     * @param value
     * @return this, for chaining
     */
    private UnicodeMap _put(int codepoint, T value) {
        // Warning: baseIndex is an invariant; must
        // be defined such that transitions[baseIndex] < codepoint
        // at end of this routine.
        int baseIndex;
        if (transitions[lastIndex] <= codepoint 
                && codepoint < transitions[lastIndex+1]) {
            baseIndex = lastIndex;
        } else { 
            baseIndex = _findIndex(codepoint);
        }
        int limitIndex = baseIndex + 1;
        // cases are (a) value is already set
        if (areEqual(values[baseIndex], value)) return this;
        if (locked) {
            throw new UnsupportedOperationException("Attempt to modify locked object");
        }
        if (errorOnReset && values[baseIndex] != null) {
            throw new IllegalArgumentException("Attempt to reset value for " + Utility.hex(codepoint)
                    + " when that is disallowed. Old: " + values[baseIndex] + "; New: " + value);
        }

        // adjust the available values
        staleAvailableValues = true;
        availableValues.add(value); // add if not there already      

        int baseCP = transitions[baseIndex];
        int limitCP = transitions[limitIndex];
        // we now start walking through the difference case,
        // based on whether we are at the start or end of range
        // and whether the range is a single character or multiple

        if (baseCP == codepoint) {
            // CASE: At very start of range
            boolean connectsWithPrevious = 
                baseIndex != 0 && areEqual(value, values[baseIndex-1]);               

            if (limitCP == codepoint + 1) {
                // CASE: Single codepoint range
                boolean connectsWithFollowing =
                    baseIndex < length - 2 && areEqual(value, values[limitIndex]); // was -1

                if (connectsWithPrevious) {
                    // A1a connects with previous & following, so remove index
                    if (connectsWithFollowing) {
                        _removeAt(baseIndex, 2);
                    } else {
                        _removeAt(baseIndex, 1); // extend previous
                    }
                    --baseIndex; // fix up
                } else if (connectsWithFollowing) {
                    _removeAt(baseIndex, 1); // extend following backwards
                    transitions[baseIndex] = codepoint; 
                } else {
                    // doesn't connect on either side, just reset
                    values[baseIndex] = value;
                }
            } else if (connectsWithPrevious) {             
                // A.1: start of multi codepoint range
                // if connects
                ++transitions[baseIndex]; // extend previous
            } else {
                // otherwise insert new transition
                transitions[baseIndex] = codepoint+1; // fix following range
                _insertGapAt(baseIndex, 1);
                values[baseIndex] = value;
                transitions[baseIndex] = codepoint;
            }
        } else if (limitCP == codepoint + 1) {
            // CASE: at end of range        
            // if connects, just back up range
            boolean connectsWithFollowing =
                baseIndex < length - 2 && areEqual(value, values[limitIndex]); // was -1

            if (connectsWithFollowing) {
                --transitions[limitIndex]; 
                return this;                
            } else {
                _insertGapAt(limitIndex, 1);
                transitions[limitIndex] = codepoint;
                values[limitIndex] = value;
            }
        } else {
            // CASE: in middle of range
            // insert gap, then set the new range
            _insertGapAt(++baseIndex,2);
            transitions[baseIndex] = codepoint;
            values[baseIndex] = value;
            transitions[baseIndex+1] = codepoint + 1;
            values[baseIndex+1] = values[baseIndex-1]; // copy lower range values
        }
        lastIndex = baseIndex; // store for next time
        return this;
    }

    private UnicodeMap _putAll(int startCodePoint, int endCodePoint, T value) {
        // TODO optimize
        for (int i = startCodePoint; i <= endCodePoint; ++i) {
            _put(i, value);
            if (ASSERTIONS) _checkInvariants();
        }
        return this;
    }

    /**
     * Sets the codepoint value.
     * @param codepoint
     * @param value
     * @return this (for chaining)
     */
    public UnicodeMap<T> put(int codepoint, T value) {
        if (codepoint < 0 || codepoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: " + codepoint);
        }
        _put(codepoint, value);
        if (ASSERTIONS) _checkInvariants();
        return this;
    }

    /**
     * Sets the codepoint value.
     * @param codepoint
     * @param value
     * @return this (for chaining)
     */
    public UnicodeMap<T> put(String string, T value) {
        int v = UnicodeSet.getSingleCodePoint(string);
        if (v == Integer.MAX_VALUE) {
            if (value != null) {
                if (stringMap == null) {
                    stringMap = new TreeMap<String,T>();
                }
                stringMap.put(string, value);
                staleAvailableValues = true;
            } else if (stringMap != null) {
                if (stringMap.remove(string) != null) {
                    staleAvailableValues = true;
                }
            }
            return this;
        }
        return put(v, value);
    }

    /**
     * Adds bunch o' codepoints; otherwise like put.
     * @param codepoints
     * @param value
     * @return this (for chaining)
     */
    public UnicodeMap<T> putAll(UnicodeSet codepoints, T value) {
        UnicodeSetIterator it = new UnicodeSetIterator(codepoints);
        while (it.nextRange()) {
            _putAll(it.codepoint, it.codepointEnd, value);
        }
        for (String key : codepoints.strings()) {
            put(key, value);
        }
        return this;
    }

    /**
     * Adds bunch o' codepoints; otherwise like add.
     * @param startCodePoint
     * @param endCodePoint
     * @param value
     * @return this (for chaining)
     */
    public UnicodeMap<T> putAll(int startCodePoint, int endCodePoint, T value) {
        if (locked) {
            throw new UnsupportedOperationException("Attempt to modify locked object");
        }
        if (startCodePoint < 0 || endCodePoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: "
                    + Utility.hex(startCodePoint) + ".." + Utility.hex(endCodePoint));
        }
        return _putAll(startCodePoint, endCodePoint, value);
    }

    /**
     * Add all the (main) values from a UnicodeMap
     * @param unicodeMap the property to add to the map
     * @return this (for chaining)
     */
    public UnicodeMap<T> putAll(UnicodeMap<T> unicodeMap) {    
        for (int i = 0; i < unicodeMap.length; ++i) {
            T value = unicodeMap.values[i];
            if (value != null) {
                _putAll(unicodeMap.transitions[i], unicodeMap.transitions[i+1]-1, value);
            }
            if (ASSERTIONS) _checkInvariants();
        }
        if (unicodeMap.stringMap != null && !unicodeMap.stringMap.isEmpty()) {
            if (stringMap == null) {
                stringMap = new TreeMap<String,T>();
            }
            stringMap.putAll(unicodeMap.stringMap);
        }
        return this;
    }

    /**
     * Add all the (main) values from a Unicode property
     * @param prop the property to add to the map
     * @return this (for chaining)
     */
    public UnicodeMap<T> putAllFiltered(UnicodeMap<T> prop, UnicodeSet filter) {
        // TODO optimize
        for (UnicodeSetIterator it = new UnicodeSetIterator(filter); it.next();) {
            if (it.codepoint != UnicodeSetIterator.IS_STRING) {
                T value = prop.getValue(it.codepoint);
                if (value != null) {
                    _put(it.codepoint, value);
                }
            }
        }
        // now do the strings
        for (String key : filter.strings()) {
            T value = prop.get(key);
            if (value != null) {
                put(key, value);
            }
        }
        return this;
    }

    /**
     * Set the currently unmapped Unicode code points to the given value.
     * @param value the value to set
     * @return this (for chaining)
     */
    public UnicodeMap<T> setMissing(T value) {
        // fast path, if value not yet present
        if (!getAvailableValues().contains(value)) {
            staleAvailableValues = true;
            availableValues.add(value);
            for (int i = 0; i < length; ++i) {
                if (values[i] == null) values[i] = value;
            }
            return this;
        } else {
            return putAll(keySet(null), value);
        }
    }
    /**
     * Returns the keyset consisting of all the keys that would produce the given value. Deposits into
     * result if it is not null. Remember to clear if you just want
     * the new values.
     */
    public UnicodeSet keySet(T value, UnicodeSet result) {
        if (result == null) result = new UnicodeSet();
        for (int i = 0; i < length - 1; ++i) {
            if (areEqual(value, values[i])) {
                result.add(transitions[i], transitions[i+1]-1);
            } 
        }
        if (value != null && stringMap != null) {
            for (String key : stringMap.keySet()) {
                T newValue = stringMap.get(key);
                if (value.equals(newValue)) {
                    result.add((String)key);
                }
            }
        }
        return result;
    }

    /**
     * Returns the keyset consisting of all the keys that would produce the given value.
     * the new values.
     */
    public UnicodeSet keySet(T value) {
        return keySet(value,null);
    }
    
    /**
     * Returns the keyset consisting of all the keys that would produce (non-null) values.
     */
    public UnicodeSet keySet() {
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < length - 1; ++i) {
            if (values[i] != null) {
                result.add(transitions[i], transitions[i+1]-1);
            } 
        }
        if (stringMap != null) {
            result.addAll(stringMap.keySet());
        }
        return result;
    }

    /**
     * Returns the list of possible values. Deposits each non-null value into
     * result. Creates result if it is null. Remember to clear result if
     * you are not appending to existing collection.
     * @param result
     * @return result
     */
    public <U extends Collection<T>> U values(U result) {
        if (staleAvailableValues) {
            // collect all the current values
            // retain them in the availableValues
            Set<T> temp = new HashSet<T>();
            for (int i = 0; i < length - 1; ++i) {
                if (values[i] != null) temp.add(values[i]);
            }
            availableValues.retainAll(temp);
            if (stringMap != null) {
                availableValues.addAll(stringMap.values());
            }
            staleAvailableValues = false;
        }
        if (result == null) {
            result = (U) new ArrayList<T>(availableValues.size());
        }
        result.addAll(availableValues);
        return result;
    }

    /**
     * Convenience method
     */
    public Collection<T> values() {
        return getAvailableValues(null);
    }
    /**
     * Gets the value associated with a given code point.
     * Returns null, if there is no such value.
     * @param codepoint
     * @return the value
     */
    public T get(int codepoint) {
        if (codepoint < 0 || codepoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: " + codepoint);
        }
        return values[_findIndex(codepoint)];
    }

    /**
     * Gets the value associated with a given code point.
     * Returns null, if there is no such value.
     * @param codepoint
     * @return the value
     */
    public T get(String value) {
        if (UTF16.hasMoreCodePointsThan(value, 1)) {
            if (stringMap == null) {
                return null;
            }
            return stringMap.get(value);
        }
        return getValue(UTF16.charAt(value, 0));
    }


    /**
     * Change a new string from the source string according to the mappings.
     * For each code point cp, if getValue(cp) is null, append the character, otherwise append getValue(cp).toString()
     * TODO: extend to strings
     * @param source
     * @return
     */
    public String transform(String source) {
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            T mResult = getValue(cp);
            if (mResult != null) {
                result.append(mResult);
            } else {
                UTF16.append(result, cp);
            }
        }
        return result.toString();
    }

    /**
     * Used to add complex values, where the value isn't replaced but in some sense composed
     * @author markdavis
     */
    public abstract static class Composer<T> {
        /**
         * This will be called with either a string or a code point. The result is the new value for that item.
         * If the codepoint is used, the string is null; if the string is used, the codepoint is -1.
         * @param a
         * @param b
         */
        public abstract T compose(int codePoint, String string, T a, T b);
    }

    public UnicodeMap<T> composeWith(UnicodeMap<T> other, Composer<T> composer) {
        for (T value : other.getAvailableValues()) {
            UnicodeSet set = other.keySet(value);
            composeWith(set, value, composer);
        }
        return this;
    }

    public UnicodeMap<T> composeWith(UnicodeSet set, T value, Composer<T> composer) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
            int i = it.codepoint;
            if (i == UnicodeSetIterator.IS_STRING) {
                String s = it.string;
                T v1 = getValue(s);
                T v3 = composer.compose(-1, s, v1, value);
                if (v1 != v3 && (v1 == null || !v1.equals(v3))) {
                    put(s, v3);
                }                
            } else {
                T v1 = getValue(i);
                T v3 = composer.compose(i, null, v1, value);
                if (v1 != v3 && (v1 == null || !v1.equals(v3))) {
                    put(i, v3);
                }
            }
        }
        return this;
    }

    public String toString() {
        return toString(null);
    }

    public String toString(Comparator<T> collected) {
        StringBuffer result = new StringBuffer();       
        if (collected == null) {
            for (int i = 0; i < length-1; ++i) {
                T value = values[i];
                if (value == null) continue;
                int start = transitions[i];
                int end = transitions[i+1]-1;
                result.append(Utility.hex(start));
                if (start != end) result.append("-").append(Utility.hex(end));
                result.append("=").append(value.toString()).append("\n");
            }
            if (stringMap != null) {
                for (String s : stringMap.keySet()) {
                    result.append(Utility.hex(s)).append("=").append(stringMap.get(s).toString()).append("\n");
                }
            }
        } else {
            Set<T> set = values(new TreeSet<T>(collected));
            for (Iterator<T> it = set.iterator(); it.hasNext();) {
                T value = it.next();
                UnicodeSet s = keySet(value);
                result.append(value).append("=").append(s.toString()).append("\n");
            }
        }
        return result.toString();
    }
    /**
     * @return Returns the errorOnReset value.
     */
    public boolean getErrorOnReset() {
        return errorOnReset;
    }
    /**
     * Puts the UnicodeMap into a state whereby new mappings are accepted, but changes to old mappings cause an exception.
     * @param errorOnReset The errorOnReset to set.
     */
    public UnicodeMap<T> setErrorOnReset(boolean errorOnReset) {
        this.errorOnReset = errorOnReset;
        return this;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.util.Freezable#isFrozen()
     */
    public boolean isFrozen() {
        // TODO Auto-generated method stub
        return locked;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.util.Freezable#lock()
     */
    public UnicodeMap<T> freeze() {
        locked = true;
        return this;
    }

    /**
     * Utility to find the maximal common prefix of two strings.
     * TODO: fix supplemental support
     */
    static public int findCommonPrefix(String last, String s) {
        int minLen = Math.min(last.length(), s.length());
        for (int i = 0; i < minLen; ++i) {
            if (last.charAt(i) != s.charAt(i)) return i;
        }
        return minLen;
    }

    /**
     * Get the number of ranges; used for getRangeStart/End. The ranges together cover all of the single-codepoint keys in the UnicodeMap. Other keys can be gotten with getStrings().
     */
    public int getRangeCount() {
        return length-1;
    }

    /**
     * Get the start of a range. All code points between start and end are in the UnicodeMap's keyset.
     */
    public int getRangeStart(int range) {
        return transitions[range];
    }

    /**
     * Get the start of a range. All code points between start and end are in the UnicodeMap's keyset.
     */
    public int getRangeEnd(int range) {
        return transitions[range+1] - 1;
    }

    /**
     * Get the value for the range.
     */
    public T getRangeValue(int range) {
        return values[range];
    }

    /**
     * Get the strings that are not in the ranges. Returns null if there are none.
     * @return
     */
    public Set<String> getNonRangeStrings() {
        if (stringMap == null || stringMap.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableSet(stringMap.keySet());
    }

    static final boolean DEBUG_WRITE = false;

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(String key) {
        return getValue(key) != null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(int key) {
        return getValue(key) != null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(T value) {
        // TODO Optimize
        return getAvailableValues().contains(value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public UnicodeMap<T> putAll(Map<? extends String, ? extends T> map) {
        for (String key : map.keySet()) {
            put(key,map.get(key));
        }
        return this;
    }

    /**
     * Utility for extracting map
     */
    public UnicodeMap<T> putAllIn(Map<? super String, ? super T> map) {
        for (String key : keySet()) {
            map.put(key, get(key));
        }
        return this;
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public UnicodeMap<T> remove(String key) {
        return put(key, null);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public UnicodeMap<T> remove(int key) {
        return put(key, null);
    }

    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        int result = stringMap.size();
        for (int i = 0; i < length-1; ++i) {
            T value = values[i];
            if (value == null) continue;
            result += transitions[i+1] - transitions[i];
        }
        return result;
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Iterable<Entry<String,T>> entrySet() {
        return new EntrySetX();
    }

    private class EntrySetX implements Iterable<Entry<String, T>> {
        public Iterator<Entry<String, T>> iterator() {
            return new IteratorX();
        }
        public String toString() {
            StringBuffer b = new StringBuffer();
            for (Iterator it = iterator(); it.hasNext();) {
                Object item = it.next();
                b.append(item.toString()).append(' ');
            }
            return b.toString();
        }
    }

    private class IteratorX implements Iterator<Entry<String, T>> {
        Iterator<String> iterator = keySet().iterator();

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public Entry<String, T> next() {
            String key = iterator.next();
            return new ImmutableEntry(key, get(key));
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<String> iterator() {
        return keySet().iterator();
    }

    /**
     * Old form for compatibility
     */
    public T getValue(String key) {
        return get(key);
    }

    /**
     * Old form for compatibility
     */
    public T getValue(int key) {
        // TODO Auto-generated method stub
        return get(key);
    }

    /**
     * Old form for compatibility
     */
    public Collection<T> getAvailableValues() {
        return values();
    }

    /**
     * Old form for compatibility
     */
    public <U extends Collection<T>> U getAvailableValues(U result) {
        return values(result);
    }

    /**
     * Old form for compatibility
     */
    public UnicodeSet getSet(T value) {
        return keySet(value);
    }

    /**
     * Old form for compatibility
     */
    public UnicodeSet getSet(T value, UnicodeSet result) {
        return keySet(value, result);
    }

    // This is to support compressed serialization. It works; just commented out for now as we shift to Generics
    // TODO Fix once generics are cleaned up.
    //    // TODO Fix to serialize more than just strings.
    //    // Only if all the items are strings will we do the following compression
    //    // Otherwise we'll just use Java Serialization, bulky as it is
    //    public void writeExternal(ObjectOutput out1) throws IOException {
    //        DataOutputCompressor sc = new DataOutputCompressor(out1);
    //        // if all objects are strings
    //        Collection<T> availableVals = getAvailableValues();
    //        boolean allStrings = allAreString(availableVals);
    //        sc.writeBoolean(allStrings);
    //        Map object_index = new LinkedHashMap();
    //        if (allAreString(availableVals)) {
    //            sc.writeStringSet(new TreeSet(availableVals), object_index);
    //        } else {
    //            sc.writeCollection(availableVals, object_index);           
    //        }
    //        sc.writeUInt(length);
    //        int lastTransition = -1;
    //        int lastValueNumber = 0;
    //        if (DEBUG_WRITE) System.out.println("Trans count: " + length);
    //        for (int i = 0; i < length; ++i) {
    //            int valueNumber = ((Integer)object_index.get(values[i])).intValue();
    //            if (DEBUG_WRITE) System.out.println("Trans: " + transitions[i] + ",\t" + valueNumber);
    //
    //            int deltaTransition = transitions[i] - lastTransition;
    //            lastTransition = transitions[i];
    //            int deltaValueNumber = valueNumber - lastValueNumber;
    //            lastValueNumber = valueNumber;
    //
    //            deltaValueNumber <<= 1; // make room for one bit
    //            boolean canCombine = deltaTransition == 1;
    //            if (canCombine) deltaValueNumber |= 1;
    //            sc.writeInt(deltaValueNumber);
    //            if (DEBUG_WRITE) System.out.println("deltaValueNumber: " + deltaValueNumber);
    //            if (!canCombine) {
    //                sc.writeUInt(deltaTransition);
    //                if (DEBUG_WRITE) System.out.println("deltaTransition: " + deltaTransition);
    //            }
    //        }
    //        sc.flush();
    //    }
    //
    //    /**
    //     * 
    //     */
    //    private boolean allAreString(Collection<T> availableValues2) {
    //        //if (true) return false;
    //        for (Iterator<T> it = availableValues2.iterator(); it.hasNext();) {
    //            if (!(it.next() instanceof String)) return false;
    //        }
    //        return true;
    //    }
    //
    //    public void readExternal(ObjectInput in1) throws IOException, ClassNotFoundException {
    //        DataInputCompressor sc = new DataInputCompressor(in1);
    //        boolean allStrings = sc.readBoolean();
    //        T[] valuesList;
    //        availableValues = new LinkedHashSet();
    //        if (allStrings) {
    //            valuesList = sc.readStringSet(availableValues);
    //        } else {
    //            valuesList = sc.readCollection(availableValues);            
    //        }
    //        length = sc.readUInt();
    //        transitions = new int[length];
    //        if (DEBUG_WRITE) System.out.println("Trans count: " + length);
    //        values = (T[]) new Object[length];
    //        int currentTransition = -1;
    //        int currentValue = 0;
    //        int deltaTransition;
    //        for (int i = 0; i < length; ++i) {
    //            int temp = sc.readInt();
    //            if (DEBUG_WRITE) System.out.println("deltaValueNumber: " + temp);
    //            boolean combined = (temp & 1) != 0;
    //            temp >>= 1;
    //        values[i] = valuesList[currentValue += temp];
    //        if (!combined) {
    //            deltaTransition = sc.readUInt();
    //            if (DEBUG_WRITE) System.out.println("deltaTransition: " + deltaTransition);
    //        } else {
    //            deltaTransition = 1;
    //        }
    //        transitions[i] = currentTransition += deltaTransition; // delta value
    //        if (DEBUG_WRITE) System.out.println("Trans: " + transitions[i] + ",\t" + currentValue);
    //        }
    //    }
}
