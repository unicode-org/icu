//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.*;
import java.util.*;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Freezable;
/**
 * Class for mapping Unicode characters to values
 * Much smaller storage than using HashMap, and much faster and more compact than
 * a list of UnicodeSets.
 * @author Davis
 */

public final class UnicodeMap implements Cloneable, Freezable, Externalizable {
    /**
     * For serialization
     */
    private static final long serialVersionUID = -6540936876295804105L;
    static final boolean ASSERTIONS = false;
    static final long GROWTH_PERCENT = 200; // 100 is no growth!
    static final long GROWTH_GAP = 10; // extra bump!

    private int length;
    // two parallel arrays to save memory. Wish Java had structs.
    private int[] transitions;
    private Object[] values;
    
    private LinkedHashSet availableValues = new LinkedHashSet();
    private transient boolean staleAvailableValues;

    private transient boolean errorOnReset;
    private transient boolean locked;
    private int lastIndex;
    
    { clear(); }
    
    public UnicodeMap clear() {
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
        length = 2;
        transitions = new int[] {0,0x110000,0,0,0,0,0,0,0,0};
        values = new Object[10];
        
        availableValues.clear();
        staleAvailableValues = false;

        errorOnReset = false;
        lastIndex = 0;
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
    
    public int getHashCode(Object o) {
        return o.hashCode();
        //equator.getHashCode
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
            result = 37*result + getHashCode(values[i]);
        }
        return result;
    }
    
    /**
     * Standard clone. Warning, as with Collections, does not do deep clone.
     */
    public Object cloneAsThawed() {
        UnicodeMap that = new UnicodeMap();
        that.length = length;
        that.transitions = (int[]) transitions.clone();
        that.values = (Object[]) values.clone();
        that.availableValues = new LinkedHashSet(availableValues);
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
        // TODO use binary search
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
        Object[] oldvalues = values;
        if (newLength > transitions.length) {
            int allocation = (int) (GROWTH_GAP + (newLength * GROWTH_PERCENT) / 100);
            transitions = new int[allocation];
            values = new Object[allocation];
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
     * @param codepoint
     * @param value
     * @return this, for chaining
     */
    private UnicodeMap _put(int codepoint, Object value) {
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
        if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
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
                    baseIndex < length - 1 && areEqual(value, values[limitIndex]);
                
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
                baseIndex < length - 1 && areEqual(value, values[limitIndex]);

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
    private UnicodeMap _putAll(int startCodePoint, int endCodePoint, Object value) {
        for (int i = startCodePoint; i <= endCodePoint; ++i) {
            _put(i, value);
        }
        return this;
    }
    /**
     * Sets the codepoint value.
     * @param codepoint
     * @param value
     * @return this (for chaining)
     */
    public UnicodeMap put(int codepoint, Object value) {
        if (codepoint < 0 || codepoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: " + codepoint);
        }
        _put(codepoint, value);
        if (ASSERTIONS) _checkInvariants();
        return this;
    }
    /**
     * Adds bunch o' codepoints; otherwise like put.
     * @param codepoints
     * @param value
     * @return this (for chaining)
     */
    public UnicodeMap putAll(UnicodeSet codepoints, Object value) {
        // TODO optimize
        UnicodeSetIterator it = new UnicodeSetIterator(codepoints);
        while (it.nextRange()) {
            _putAll(it.codepoint, it.codepointEnd, value);
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
    public UnicodeMap putAll(int startCodePoint, int endCodePoint, Object value) {
        if (startCodePoint < 0 || endCodePoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: "
             + Utility.hex(startCodePoint) + ".." + Utility.hex(endCodePoint));
        }
        // TODO optimize
        for (int i = startCodePoint; i <= endCodePoint; ++i) {
            _put(i, value);
        }
        return this;
    }
    /**
     * Add all the (main) values from a Unicode property
     * @param prop the property to add to the map
     * @return this (for chaining)
     */
    public UnicodeMap putAll(UnicodeProperty prop) {
        // TODO optimize
        for (int i = 0; i <= 0x10FFFF; ++i) {
            _put(i, prop.getValue(i));
        }
        return this;
    }

    /**
     * Add all the (main) values from a Unicode property
     * @param prop the property to add to the map
     * @return this (for chaining)
     */
    public UnicodeMap putAll(UnicodeMap prop) {
        // TODO optimize
        for (int i = 0; i <= 0x10FFFF; ++i) {
            _put(i, prop.getValue(i));
        }
        return this;
    }

    /**
     * Set the currently unmapped Unicode code points to the given value.
     * @param value the value to set
     * @return this (for chaining)
     */
    public UnicodeMap setMissing(Object value) {
        // fast path, if value not yet present
        if (!getAvailableValues().contains(value)) {
            staleAvailableValues = true;
            availableValues.add(value);
            for (int i = 0; i < length; ++i) {
                if (values[i] == null) values[i] = value;
            }
            return this;
        } else {
            return putAll(getSet(null), value);
        }
    }
    /**
     * Returns the set associated with a given value. Deposits into
     * result if it is not null. Remember to clear if you just want
     * the new values.
     * @param value
     * @param result
     * @return result
     */
    public UnicodeSet getSet(Object value, UnicodeSet result) {
        if (result == null) result = new UnicodeSet();
        for (int i = 0; i < length - 1; ++i) {
            if (areEqual(value, values[i])) {
                result.add(transitions[i], transitions[i+1]-1);
            } 
        }
        return result;
    }

    public UnicodeSet getSet(Object value) {
        return getSet(value,null);
    }

    public UnicodeSet keySet() {
        return getSet(null,null).complement();
    }
    /**
     * Returns the list of possible values. Deposits each non-null value into
     * result. Creates result if it is null. Remember to clear result if
     * you are not appending to existing collection.
     * @param result
     * @return result
     */
    public Collection getAvailableValues(Collection result) {
        if (staleAvailableValues) {
            // collect all the current values
            // retain them in the availableValues
            Set temp = new HashSet();
            for (int i = 0; i < length - 1; ++i) {
                if (values[i] != null) temp.add(values[i]);
            }
            availableValues.retainAll(temp);
            staleAvailableValues = false;
        }
        if (result == null) result = new ArrayList(availableValues.size());
        result.addAll(availableValues);
        return result;
    }
    
    /**
     * Convenience method
     */
    public Collection getAvailableValues() {
        return getAvailableValues(null);
    }
    /**
     * Gets the value associated with a given code point.
     * Returns null, if there is no such value.
     * @param codepoint
     * @return the value
     */
    public Object getValue(int codepoint) {
        if (codepoint < 0 || codepoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: " + codepoint);
        }
        return values[_findIndex(codepoint)];
    }
    
    /**
     * Change a new string from the source string according to the mappings. For each code point cp, if getValue(cp) is null, append the character, otherwise append getValue(cp).toString()
     * @param source
     * @return
     */
    public String fold(String source) {
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
          cp = UTF16.charAt(source, i);
          Object mResult = getValue(cp);
          if (mResult != null) {
            result.append(mResult);
          } else {
            UTF16.append(result, cp);
          }
        }
        return result.toString();
      }
    
    public interface Composer {
        Object compose(int codePoint, Object a, Object b);
    }
    
    public UnicodeMap composeWith(UnicodeMap other, Composer composer) {
        for (int i = 0; i <= 0x10FFFF; ++i) {
            Object v1 = getValue(i);
            Object v2 = other.getValue(i);
            Object v3 = composer.compose(i, v1, v2);
            if (v1 != v3 && (v1 == null || !v1.equals(v3))) put(i, v3);
        }
        return this;
    }
    
    public UnicodeMap composeWith(UnicodeSet set, Object value, Composer composer) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
            int i = it.codepoint;
            Object v1 = getValue(i);
            Object v3 = composer.compose(i, v1, value);
            if (v1 != v3 && (v1 == null || !v1.equals(v3))) put(i, v3);
        }
        return this;
    }
    
    /**
     * Follow the style used by UnicodeSetIterator
     */
    public static class MapIterator {
        public int codepoint;
        public int codepointEnd;
        public Object value;
        
        private UnicodeMap map;
        private int index;
        private int startRange;
        private int endRange;
        private Object lastValue;
        
        public MapIterator(UnicodeMap map) {
            reset(map);
        }
        // note: length of 2 means {0, 110000}. Only want to index up to 0!
        public boolean nextRange() {
            if (index < 0 || index >= map.length - 1) return false;
            value = map.values[index];
            codepoint = startRange = map.transitions[index++];
            codepointEnd = endRange = map.transitions[index] - 1; // -1 to make limit into end
            return true;
        }
        public boolean next() {
            if (startRange > endRange) {
                //System.out.println("***" + Utility.hex(startRange) + ".." + Utility.hex(endRange));
                if (!nextRange()) return false;
                // index now points AFTER the start of the range
                lastValue = map.values[index-1];
                //System.out.println("***" + Utility.hex(codepoint) + ".." + Utility.hex(codepointEnd) + " => " + lastValue);
            }
            value = lastValue;
            codepoint = codepointEnd = startRange++; // set to first, and iterate
            return true;
        }

        public MapIterator reset() {
            index = 0;
            startRange = 0;
            endRange = -1;
            return this;
        }
        public MapIterator reset(UnicodeMap newMap) {
            this.map = newMap;
            return reset();
        }
    }
    
    public String toString() {
        return toString(null);
    }
    public String toString(Comparator collected) {
        StringBuffer result = new StringBuffer();       
        if (collected == null) {
            for (int i = 0; i < length-1; ++i) {
                Object value = values[i];
                if (value == null) continue;
                int start = transitions[i];
                int end = transitions[i+1]-1;
                result.append(Utility.hex(start));
                if (start != end) result.append("..")
                .append(Utility.hex(end));
                result.append("\t=> ")
                .append(values[i] == null ? "null" : values[i].toString())
                .append("\r\n");
            }
        } else {
            Set set = (Set) getAvailableValues(new TreeSet(collected));
            for (Iterator it = set.iterator(); it.hasNext();) {
                Object value = it.next();
                UnicodeSet s = getSet(value);
                result.append(value)
                .append("\t=> ")
                .append(s.toPattern(true))
                .append("\r\n");
            }
        }
        return result.toString();
    }
    /**
     * @return Returns the errorOnReset.
     */
    public boolean getErrorOnReset() {
        return errorOnReset;
    }
    /**
     * @param errorOnReset The errorOnReset to set.
     */
    public void setErrorOnReset(boolean errorOnReset) {
        this.errorOnReset = errorOnReset;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.util.Lockable#isLocked()
     */
    public boolean isFrozen() {
        // TODO Auto-generated method stub
        return locked;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.util.Lockable#lock()
     */
    public Object freeze() {
        locked = true;
        return this;
    }
    
    static final boolean DEBUG_WRITE = false;
    
    // TODO Fix to serialize more than just strings.
    // Only if all the items are strings will we do the following compression
    // Otherwise we'll just use Java Serialization, bulky as it is
    public void writeExternal(ObjectOutput out1) throws IOException {
        DataOutputCompressor sc = new DataOutputCompressor(out1);
        // if all objects are strings
        Collection availableVals = getAvailableValues();
        boolean allStrings = allAreString(availableVals);
        sc.writeBoolean(allStrings);
        Map object_index = new LinkedHashMap();
        if (allAreString(availableVals)) {
            sc.writeStringSet(new TreeSet(availableVals), object_index);
        } else {
            sc.writeCollection(availableVals, object_index);           
        }
        sc.writeUInt(length);
        int lastTransition = -1;
        int lastValueNumber = 0;
        if (DEBUG_WRITE) System.out.println("Trans count: " + length);
        for (int i = 0; i < length; ++i) {
            int valueNumber = ((Integer)object_index.get(values[i])).intValue();
            if (DEBUG_WRITE) System.out.println("Trans: " + transitions[i] + ",\t" + valueNumber);
            
            int deltaTransition = transitions[i] - lastTransition;
            lastTransition = transitions[i];
            int deltaValueNumber = valueNumber - lastValueNumber;
            lastValueNumber = valueNumber;
            
            deltaValueNumber <<= 1; // make room for one bit
            boolean canCombine = deltaTransition == 1;
            if (canCombine) deltaValueNumber |= 1;
            sc.writeInt(deltaValueNumber);
            if (DEBUG_WRITE) System.out.println("deltaValueNumber: " + deltaValueNumber);
            if (!canCombine) {
                sc.writeUInt(deltaTransition);
                if (DEBUG_WRITE) System.out.println("deltaTransition: " + deltaTransition);
            }
        }
        sc.flush();
    }

    /**
     * 
     */
    private boolean allAreString(Collection availableValues2) {
        //if (true) return false;
        for (Iterator it = availableValues2.iterator(); it.hasNext();) {
            if (!(it.next() instanceof String)) return false;
        }
        return true;
    }

    public void readExternal(ObjectInput in1) throws IOException, ClassNotFoundException {
        DataInputCompressor sc = new DataInputCompressor(in1);
        boolean allStrings = sc.readBoolean();
        Object[] valuesList;
        availableValues = new LinkedHashSet();
        if (allStrings) {
            valuesList = sc.readStringSet(availableValues);
        } else {
            valuesList = sc.readCollection(availableValues);            
        }
        length = sc.readUInt();
        transitions = new int[length];
        if (DEBUG_WRITE) System.out.println("Trans count: " + length);
        values = new Object[length];
        int currentTransition = -1;
        int currentValue = 0;
        int deltaTransition;
        for (int i = 0; i < length; ++i) {
            int temp = sc.readInt();
            if (DEBUG_WRITE) System.out.println("deltaValueNumber: " + temp);
            boolean combined = (temp & 1) != 0;
            temp >>= 1;
            values[i] = valuesList[currentValue += temp];
            if (!combined) {
                deltaTransition = sc.readUInt();
                if (DEBUG_WRITE) System.out.println("deltaTransition: " + deltaTransition);
            } else {
                deltaTransition = 1;
            }
            transitions[i] = currentTransition += deltaTransition; // delta value
            if (DEBUG_WRITE) System.out.println("Trans: " + transitions[i] + ",\t" + currentValue);
        }
    }

    /**
     * 
     */
    static int findCommon(String last, String s) {
        int minLen = Math.min(last.length(), s.length());
        for (int i = 0; i < minLen; ++i) {
            if (last.charAt(i) != s.charAt(i)) return i;
        }
        return minLen;
    }


//    /**
//     * @param sc
//     * @throws IOException
//     * 
//     */
//    private void showSize(String title, ObjectOutput out, StreamCompressor sc) throws IOException {
//        sc.showSize(this, title, out);
//    }
//    //public void readObject(ObjectInputStream in) throws IOException {
//    public static class StreamCompressor {
//        transient byte[] buffer = new byte[1];
//        transient StringBuffer stringBuffer = new StringBuffer();
//        
//        transient byte[] readWriteBuffer = new byte[8];
//        int position = 0;
//        DataOutput out;
//        DataInput in;
//
//        /**
//         * Format is:
//         * @throws IOException
//         */
//        public void writeInt(int i) throws IOException {
//            while (true) {
//                if (position == readWriteBuffer.length) {
//                    out.write(readWriteBuffer);
//                    position = 0;
//                }
//                if ((i & ~0x7F) == 0) {
//                    readWriteBuffer[position++] = (byte)i;
//                    break;
//                }
//                readWriteBuffer[position++] = (byte)(0x80 | i);
//                i >>>= 7;
//            }
//        }
//        /**
//         * @throws IOException
//         * 
//         */
//        public int readNInt(ObjectInput in) throws IOException {
//            int result = readInt(in);
//            boolean negative = (result & 1) != 0;
//            result >>>= 1;
//            if (negative) result = ~result;
//            return result;
//        }
//        /**
//         * @throws IOException
//         * 
//         */
//        public void writeNInt(int input) throws IOException {
//            int flag = 0;
//            if (input < 0) {
//                input = ~input;
//                flag = 1;
//            }
//            input = (input << 1) | flag;
//            writeInt(out, input);
//        }
//        /**
//         * @throws IOException
//         * 
//         */
//        public void flush() throws IOException {
//            out.write(readWriteBuffer);
//            position = 0;
//        }
//        
//        int readPosition = readWriteBuffer.length;
//        
//        public int readInt(ObjectInput in) throws IOException {
//            int result = 0;
//            int offset = 0;
//            while (true) {
//                if (readPosition == readWriteBuffer.length) {
//                    in.read(readWriteBuffer);
//                    readPosition = 0;
//                }
//                //in.read(buffer);
//                int input = readWriteBuffer[readPosition++]; // buffer[0];
//                result |= (input & 0x7F) << offset;
//                if ((input & 0x80) == 0) {
//                    return result;
//                }
//                offset += 7;
//            }  
//        }
//
//        /**
//         * @throws IOException
//         * 
//         */
//        public void writeString(String s) throws IOException {
//            writeInt(UTF16.countCodePoint(s));
//            writeCodePoints(s);
//        }
//        /**
//         * 
//         */
//        private void writeCodePoints(String s) throws IOException {
//            int cp = 0;
//            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
//                cp = UTF16.charAt(s, i);
//                writeInt(cp);
//            }
//        }
//        /**
//         * @throws IOException
//         * 
//         */
//        public String readString() throws IOException {
//            int len = readInt(in);
//            return readCodePoints(in, len);
//        }
//        /**
//         * 
//         */
//        private String readCodePoints(int len) throws IOException {
//            stringBuffer.setLength(0);
//            for (int i = 0; i < len; ++i) {
//                int cp = readInt(in);
//                UTF16.append(stringBuffer, cp);
//            }
//            return stringBuffer.toString();
//        }
//        /**
//         * @param this
//         * @throws IOException
//         * 
//         */
//        private void showSize(UnicodeMap map, String title, ObjectOutput out) throws IOException {
//            out.flush();
//            System.out.println(title + ": " + (map.debugOut.size() + position));
//        }
//    }
}
//#endif
