package com.ibm.text.utility;

final class UnicodeMapInt {
    private int [] index = new int[1];
    private int [] data = new int[1];
    private int len = 1;
    
    /* index array is a set of inflection points; it and the data are always of the form
       index: {MIN_VALUE,        x,                  y,                  ...,    q,                    MAX_VALUE}
       data:  {value for ..x-1,  value for x..y-1,   value for y..z-1,   ...,    value for q..}
        AND no adjacent values are identical!
    */
    
    
    public int put (int cp, int value) {
        int i = findIndex(cp);
        
        // A1. if cp already has the value, return
        if (data[i - 1] == value) return;
        
        int rangeStart = index[i-1];
        int rangeLimit = index[i];
        
        // B. the range has one element
        if (rangeStart + 1 == rangeLimit) {
        // B1. the adjoining ranges have the new value: coelesce 3 into 1
        // B2. one adjoining range has the new value: coelesce 2 into 1
        // B3. otherwise: reset the value
        
        // C. we are at the start of the range
        } else if (cp == rangeStart) {
        // C1. the value is the same as the adjoining: extend the range
            if (data[i-2] == value) {
                ++index[i-1];
            } else {
        // C2. otherwise add one new element/value pair
                insertRange(i-1, 1);
                index[i-1] = cp;
                data[i-1] = value;
            }
        } else if (cp == rangeLimit - 1) {
        // D. we are at the end of the range
        // D1. the value is the same as the adjoining: extend the range
        // D2. otherwise add one new element/value pair
        // E. we are in the middle of the range: insert 2 element/value pairs
        } else {
            insertRange(i-1, 2);
            index[i-1] = cp;
            data[i-1] = value;
            index[i] = cp+1;
            data[i] = data[i-2];
        }
    }
    
    public void put(int cpStart, int cpEnd, int value) {
        for (int cp = cpStart; cp <= cpEnd; ++cp) { // later optimize
            put(cp, value);
        }
    }
    
    public int get(int cp) {
        return data[findIndex(cp) - 1];
    }
    
    /**
     * Returns the set of all characters that have the given value
     */
    public UnicodeSet getMatch(int value) {
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < len; ++i) {
            if (data[i] == value) result.add(index[i], index[i+1]-1);
        }
        return result;
    }
    
    /** Finds the least index with a value greater than cp */
    private int findIndex(cp) {
        if (cp > 0x10FFFF) throw new ArrayIndexOutOfBoundsException("Code point too large: " + cp); // out of bounds!
        int i = -1;
        while (true) {
            if (cp < index[++i]) return i;
        }
         
    }
    
    /*
    public UnicodeSetIterator iterator() {
    }
    */
}
    