package com.ibm.icu.dev.test.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.icu.text.UnicodeSet;
/**
 * Class for mapping Unicode characters to values
 * Much smaller storage than using HashMap.
 * @author Davis
 */
// TODO Optimize using range map
public class UnicodeMap {
    // TODO optimize
    private HashMap objectToSet = new HashMap();
    private UnicodeSet missing = new UnicodeSet(0,0x10FFFF);
    
    /**
     * Associates code point with value. Removes any previous association.
     * @param codepoint
     * @param value
     * @return this, for chaining
     */
    public UnicodeMap put(int codepoint, Object value) {
        if (!missing.contains(codepoint)) {
            // remove from wherever it is.
            Iterator it = objectToSet.keySet().iterator();
            while (it.hasNext()) {
                UnicodeSet set = (UnicodeSet) objectToSet.get(it.next());
                if (set.contains(codepoint)) {
                    set.remove(codepoint);
                    break;
                }
            }
            missing.remove(codepoint);
        }
        UnicodeSet set = (UnicodeSet) objectToSet.get(value);
        if (set == null) {
            set = new UnicodeSet();
            objectToSet.put(value,set);
        }
        set.add(codepoint);
        return this;
    }
    /**
     * Adds bunch o' codepoints; otherwise like add.
     * @param codepoints
     * @param value
     * @return this, for chaining
     */
    public UnicodeMap putAll(UnicodeSet codepoints, Object value) {
        if (!missing.containsAll(codepoints)) {
            // remove from wherever it is.
            Iterator it = objectToSet.keySet().iterator();
            while (it.hasNext()) {
                UnicodeSet set = (UnicodeSet) objectToSet.get(it.next());
                set.removeAll(codepoints);
            }
            missing.removeAll(codepoints);
        }
        UnicodeSet set = (UnicodeSet) objectToSet.get(value);
        if (set == null) {
            set = new UnicodeSet();
            objectToSet.put(value,set);
        }
        set.addAll(codepoints);
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
        UnicodeSet set = (UnicodeSet) objectToSet.get(value);
        if (set != null) result.addAll(set);
        return result;
    }
    /**
     * Returns the list of possible values. Deposits into
     * result if it is not null. Remember to clear if you just want
     * @param result
     * @return
     */
    public Collection getAvailableValues(Collection result) {
        if (result == null) result = new HashSet();
        result.addAll(objectToSet.keySet());
        return result;
    }
    /**
     * Gets the value associated with a given code point.
     * Returns null, if there is no such value.
     * @param codepoint
     * @return
     */
    public Object getValue(int codepoint) {
        if (missing.contains(codepoint)) return null;
        Iterator it = objectToSet.keySet().iterator();
        while (it.hasNext()) {
            Object value = it.next();
            UnicodeSet set = (UnicodeSet) objectToSet.get(value);
            if (set.contains(codepoint)) return value;
        }
        return null;
    }
}