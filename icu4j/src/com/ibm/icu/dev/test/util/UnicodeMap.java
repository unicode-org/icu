package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
/**
 * Class for mapping Unicode characters to values
 * Much smaller storage than using HashMap.
 * @author Davis
 */
// TODO Optimize using range map
public final class UnicodeMap implements Cloneable {
    static final boolean ASSERTIONS = false;
    static final long GROWTH_PERCENT = 200; // 100 is no growth!
    static final long GROWTH_GAP = 10; // extra bump!

    private int length = 2;
    private int[] transitions = {0,0x110000,0,0,0,0,0,0,0,0};
    private Object[] values = new Object[10];
    {
        values[1] = "TERMINAL"; // just for debugging
    }
    private int lastIndex = 0;
    
    /* Boilerplate */
    public boolean equals(Object other) {
        if (other == null) return false;
        try {
            UnicodeMap that = (UnicodeMap) other;
            if (length != that.length || !equator.equals(that.equator)) return false;
            for (int i = 0; i < length-1; ++i) {
                if (transitions[i] != that.transitions[i]) return false;
                if (!equator.isEqual(values[i], that.values[i])) return false;
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
    
    public int hashCode() {
        int result = length;
        // TODO might want to abbreviate this for speed.
        for (int i = 0; i < length-1; ++i) {
            result = 37*result + transitions[i];
            result = 37*result + equator.getHashCode(values[i]);
        }
        return result;
    }
    
    /**
     * Standard clone. Warning, as with Collections, does not do deep clone.
     */
    public Object clone() {
        UnicodeMap that = new UnicodeMap();
        that.length = length;
        that.transitions = (int[]) transitions.clone();
        that.values = (Object[]) values.clone();
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
            if (equator.isEqual(values[i-1], values[i])) {
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
    
    public interface Equator {
        /**
          * Comparator function. If overridden, must handle case of null,
          * and compare any two objects in the array
          * @param a
          * @param b
          * @return
          */
         public boolean isEqual(Object a, Object b);

        /**
         * @param object
         * @return
         */
        public int getHashCode(Object object);
    }
    
    public static final class SimpleEquator implements Equator {
        public boolean isEqual(Object a, Object b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
        public int getHashCode(Object a) {
            if (a == null) return 0;
            return a.hashCode();
        }
    }
    private static Equator SIMPLE = new SimpleEquator(); 
    private Equator equator = SIMPLE;
 
    /**
     * Finds an index such that inversionList[i] <= codepoint < inversionList[i+1]
     * Assumes that 0 <= codepoint <= 0x10FFFF
     * @param codepoint
     * @return
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
        if (equator.isEqual(values[baseIndex], value)) return this;
        int baseCP = transitions[baseIndex];
        int limitCP = transitions[limitIndex];
        // we now start walking through the difference case,
        // based on whether we are at the start or end of range
        // and whether the range is a single character or multiple
        
        if (baseCP == codepoint) {
            // CASE: At very start of range
            boolean connectsWithPrevious = 
                baseIndex != 0 && equator.isEqual(value, values[baseIndex-1]);               
                
            if (limitCP == codepoint + 1) {
                // CASE: Single codepoint range
                boolean connectsWithFollowing =
                    baseIndex < length - 1 && equator.isEqual(value, values[limitIndex]);
                
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
                baseIndex < length - 1 && equator.isEqual(value, values[limitIndex]);

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
    /**
     * Sets the codepoint value.
     * @param codepoint
     * @param value
     * @return
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
     * @return this, for chaining
     */
    public UnicodeMap putAll(UnicodeSet codepoints, Object value) {
        // TODO optimize
        UnicodeSetIterator it = new UnicodeSetIterator(codepoints);
        while (it.next()) {
            _put(it.codepoint, value);
        }
        return this;
    }
    
    /**
     * Adds bunch o' codepoints; otherwise like add.
     * @param codepoints
     * @param value
     * @return this, for chaining
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
     * @param prop
     * @return
     */
    public UnicodeMap putAll(UnicodeProperty prop) {
        // TODO optimize
        for (int i = 0; i <= 0x10FFFF; ++i) {
            _put(i, prop.getValue(i));
        }
        return this;
    }
    
    /**
     * Set the currently unmapped Unicode code points to the given value.
     * @param value
     * @return
     */
    public UnicodeMap setMissing(Object value) {
        for (int i = 0; i < length; ++i) {
            if (values[i] == null) values[i] = value;
        }
        return this;
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
            if (equator.isEqual(value, values[i])) {
                result.add(transitions[i], transitions[i+1]-1);
            } 
        }
        return result;
    }
    public UnicodeSet getSet(Object value) {
        return getSet(value,null);
    }
    /**
     * Returns the list of possible values. Deposits each non-null value into
     * result. Creates result if it is null. Remember to clear result if
     * you are not appending to existing collection.
     * @param result
     * @return
     */
    public Collection getAvailableValues(Collection result) {
        if (result == null) result = new ArrayList(1);
        for (int i = 0; i < length - 1; ++i) {
            Object value = values[i];
            if (value == null) continue;
            if (result.contains(value)) continue;
            result.add(value);
        }
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
     * @return
     */
    public Object getValue(int codepoint) {
        if (codepoint < 0 || codepoint > 0x10FFFF) {
            throw new IllegalArgumentException("Codepoint out of range: " + codepoint);
        }
        return values[_findIndex(codepoint)];
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
}