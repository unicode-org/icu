/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unicode/cpdtrans.h"
#include "unicode/unifilt.h"
#include "unicode/unifltlg.h"

#define ID_DELIM ((UChar)0x003B) /*;*/

/**
 * Constructs a new compound transliterator given an array of
 * transliterators.  The array of transliterators may be of any
 * length, including zero or one, however, useful compound
 * transliterators have at least two components.
 * @param transliterators array of <code>Transliterator</code>
 * objects
 * @param filter the filter.  Any character for which
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 */
CompoundTransliterator::CompoundTransliterator(
                           Transliterator* const transliterators[],
                           int32_t count,
                           UnicodeFilter* adoptedFilter) :
    Transliterator(joinIDs(transliterators, count), adoptedFilter),
    trans(0), count(0), filters(0) {
    setTransliterators(transliterators, count);
}

/**
 * Splits an ID of the form "ID;ID;..." into a compound using each
 * of the IDs. 
 * @param ID of above form
 * @param forward if false, does the list in reverse order, and
 * takes the inverse of each ID.
 */
CompoundTransliterator::CompoundTransliterator(const UnicodeString& ID,
                              Transliterator::Direction direction,
                              UnicodeFilter* adoptedFilter) :
    Transliterator(ID, 0), // set filter to 0 here!
    filters(0) {
    UnicodeString* list = split(ID, ID_DELIM, count);
    trans = new Transliterator*[count];
    for (int32_t i = 0; i < count; ++i) {
        trans[i] = createInstance(list[direction==FORWARD ? i : (count-1-i)],
                                  direction);
    }
    delete[] list;
    computeMaximumContextLength();
    adoptFilter(adoptedFilter);
}

/**
 * Return the IDs of the given list of transliterators, concatenated
 * with ID_DELIM delimiting them.  Equivalent to the perlish expression
 * join(ID_DELIM, map($_.getID(), transliterators).
 */
UnicodeString CompoundTransliterator::joinIDs(Transliterator* const transliterators[],
                                              int32_t count) {
    UnicodeString id;
    for (int32_t i=0; i<count; ++i) {
        if (i > 0) {
            id.append(ID_DELIM);
        }
        id.append(transliterators[i]->getID());
    }
    return id; // Return temporary
}

/**
 * Splits a string, as in JavaScript
 */
UnicodeString* CompoundTransliterator::split(const UnicodeString& s,
                                             UChar divider,
                                             int32_t& count) {
    // changed MED
    // see how many there are
    count = 1;
	int32_t i;
    for (i = 0; i < s.length(); ++i) {
        if (s.charAt(i) == divider) ++count;
    }
    
    // make an array with them
    UnicodeString* result = new UnicodeString[count];
    int32_t last = 0;
    int32_t current = 0;
    
    for (i = 0; i < s.length(); ++i) {
        if (s.charAt(i) == divider) {
            s.extractBetween(last, i, result[current++]);
            last = i+1;
        }
    }
    s.extractBetween(last, i, result[current]);
    return result;
}

/**
 * Copy constructor.
 */
CompoundTransliterator::CompoundTransliterator(const CompoundTransliterator& t) :
    Transliterator(t), trans(0), count(0), filters(0) {
    *this = t;
}

/**
 * Destructor
 */
CompoundTransliterator::~CompoundTransliterator() {
    freeTransliterators();
}

void CompoundTransliterator::freeTransliterators(void) {
    for (int32_t i=0; i<count; ++i) {
        delete trans[i];
        if (filters != 0) {
            delete filters[i];
        }
    }
    delete[] trans;
    delete[] filters;
    trans = 0;
    filters = 0;
    count = 0;
}

/**
 * Assignment operator.
 */
CompoundTransliterator& CompoundTransliterator::operator=(
                                             const CompoundTransliterator& t) {
    Transliterator::operator=(t);
    int32_t i;
    for (i=0; i<count; ++i) {
        delete trans[i];
        trans[i] = 0;
        if (filters != 0) {
            delete filters[i];
            filters[i] = 0;
        }
    }
    if (t.count > count) {
        delete[] trans;
        trans = new Transliterator*[t.count];
        delete[] filters;
        filters = (t.filter == 0) ? 0 : new UnicodeFilter*[t.count];
    }
    count = t.count;
    for (i=0; i<count; ++i) {
        trans[i] = t.trans[i]->clone();
        if (t.filters != 0) {
            filters[i] = t.filters[i]->clone();
        }
    }
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* CompoundTransliterator::clone(void) const {
    return new CompoundTransliterator(*this);
}

/**
 * Returns the number of transliterators in this chain.
 * @return number of transliterators in this chain.
 */
int32_t CompoundTransliterator::getCount(void) const {
    return count;
}

/**
 * Returns the transliterator at the given index in this chain.
 * @param index index into chain, from 0 to <code>getCount() - 1</code>
 * @return transliterator at the given index
 */
const Transliterator& CompoundTransliterator::getTransliterator(int32_t index) const {
    return *trans[index];
}

void CompoundTransliterator::setTransliterators(Transliterator* const transliterators[],
                                                int32_t transCount) {
    Transliterator** a = new Transliterator*[transCount];
    for (int32_t i=0; i<transCount; ++i) {
        a[i] = transliterators[i]->clone();
    }
    adoptTransliterators(a, transCount);
}

void CompoundTransliterator::adoptTransliterators(Transliterator* adoptedTransliterators[],
                                                  int32_t transCount) {
    // First free trans[] and set count to zero.  Once this is done,
    // orphan the filter.  Set up the new trans[], and call
    // adoptFilter() to fix up the filters in trans[].
    freeTransliterators();
    UnicodeFilter *f = orphanFilter();
    trans = adoptedTransliterators;
    count = transCount;
    computeMaximumContextLength();
    adoptFilter(f);
}

/**
 * Override Transliterator.  Modify the transliterators that make up
 * this compound transliterator so their filters are the logical AND
 * of this transliterator's filter and their own.  Original filters
 * are kept in the filters array.
 */
void CompoundTransliterator::adoptFilter(UnicodeFilter* f) {
    /**
     * If there is a filter F for the compound transliterator as a
     * whole, then we need to modify every non-null filter f in
     * the chain to be f' = F & f.
     *
     * There are two possible states:
     * 1. getFilter() != 0
     *    original filters in filters[]
     *    createAnd() filters in trans[]
     * 2. getFilter() == 0
     *    filters[] either unallocated or empty
     *    original filters in trans[]
     * This method must insure that we stay in one of these states.
     */
    if (count > 0) {
        if (f == 0) {
            // Restore original filters
            if (getFilter() != 0 && filters != 0) {
                for (int32_t i=0; i<count; ++i) {
                    trans[i]->adoptFilter(filters[i]);
                    filters[i] = 0;
                }
            }
        } else {
            // If the previous filter is 0, then the component filters
            // are in trans[i], and need to be pulled out into filters[].
            if (getFilter() == 0) {
                if (filters == 0) {
                    filters = new UnicodeFilter*[count];
                }
                for (int32_t i=0; i<count; ++i) {
                    filters[i] = trans[i]->orphanFilter();
                }
            }
            for (int32_t i=0; i<count; ++i) {
                trans[i]->adoptFilter(UnicodeFilterLogic::createAnd(f, filters[i]));
            }
        }
    }
    Transliterator::adoptFilter(f);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void CompoundTransliterator::handleTransliterate(Replaceable& text, Position& index,
                                                 bool_t incremental) const {
    /* Call each transliterator with the same start value and
     * initial cursor index, but with the limit index as modified
     * by preceding transliterators.  The cursor index must be
     * reset for each transliterator to give each a chance to
     * transliterate the text.  The initial cursor index is known
     * to still point to the same place after each transliterator
     * is called because each transliterator will not change the
     * text between start and the initial value of cursor.
     *
     * IMPORTANT: After the first transliterator, each subsequent
     * transliterator only gets to transliterate text committed by
     * preceding transliterators; that is, the cursor (output
     * value) of transliterator i becomes the limit (input value)
     * of transliterator i+1.  Finally, the overall limit is fixed
     * up before we return.
     *
     * Assumptions we make here:
     * (1) start <= cursor <= limit    ;cursor valid on entry
     * (2) cursor <= cursor' <= limit' ;cursor doesn't move back
     * (3) cursor <= limit'            ;text before cursor unchanged
     * - cursor' is the value of cursor after calling handleKT
     * - limit' is the value of limit after calling handleKT
     */

    /**
     * Example: 3 transliterators.  This example illustrates the
     * mechanics we need to implement.  S, C, and L are the start,
     * cursor, and limit.  gl is the globalLimit.
     *
     * 1. h-u, changes hex to Unicode
     *
     *    4  7  a  d  0      4  7  a
     *    abc/u0061/u    =>  abca/u    
     *    S  C       L       S   C L   gl=f->a
     *
     * 2. upup, changes "x" to "XX"
     *
     *    4  7  a       4  7  a
     *    abca/u    =>  abcAA/u    
     *    S  CL         S    C   
     *                       L    gl=a->b
     * 3. u-h, changes Unicode to hex
     *
     *    4  7  a        4  7  a  d  0  3
     *    abcAA/u    =>  abc/u0041/u0041/u    
     *    S  C L         S              C
     *                                  L   gl=b->15
     * 4. return
     *
     *    4  7  a  d  0  3
     *    abc/u0041/u0041/u    
     *    S C L
     */

    if (count < 1) {
        return; // Short circuit for empty compound transliterators
    }

	int32_t i;
    int32_t cursor = index.cursor;
    int32_t limit = index.limit;
    int32_t globalLimit = limit;
    /* globalLimit is the overall limit.  We keep track of this
     * since we overwrite index.limit with the previous
     * index.cursor.  After each transliteration, we update
     * globalLimit for insertions or deletions that have happened.
     */
    
    for (i=0; i<count; ++i) {
        index.cursor = cursor; // Reset cursor
        index.limit = limit;
        
        trans[i]->handleTransliterate(text, index, incremental);
        
        // Adjust overall limit for insertions/deletions
        globalLimit += index.limit - limit;
        limit = index.cursor; // Move limit to end of committed text
    }
    // Cursor is good where it is -- where the last
    // transliterator left it.  Limit needs to be put back
    // where it was, modulo adjustments for deletions/insertions.
    index.limit = globalLimit;
}

/**
 * Sets the length of the longest context required by this transliterator.
 * This is <em>preceding</em> context.
 */
void CompoundTransliterator::computeMaximumContextLength(void) {
    int32_t max = 0;
    for (int32_t i=0; i<count; ++i) {
        int32_t len = trans[i]->getMaximumContextLength();
        if (len > max) {
            max = len;
        }
    }
    setMaximumContextLength(max);
}
