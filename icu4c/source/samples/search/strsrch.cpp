/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*  03/22/2000   helena      Creation.
**********************************************************************
*/

#include  <memory.h>
#include "unicode/coleitr.h"
#include "unicode/schriter.h"
#include "strsrch.h"
/**
 * <code>StringSearch</code> is a <code>SearchIterator</code> that provides
 * language-sensitive text searching based on the comparison rules defined
 * in a {@link RuleBasedCollator} object.
 * Instances of <code>StringSearch</code> function as iterators
 * maintain a current position and scan over text returning the index of
 * characters where the pattern occurs and the length of each match.
 * <p>
 * <code>StringSearch</code> uses a version of the fast Boyer-Moore search
 * algorithm that has been adapted to work with the large character set of
 * Unicode.  See "Efficient Text Searching in Java", to be published in
 * <i>Java Report</i> in February, 1999, for further information on the algorithm.
 * <p>
 * Consult the <code>SearchIterator</code> documentation for information on
 * and examples of how to use instances of this class to implement text
 * searching.  <code>SearchIterator</code> provides all of the necessary
 * API; this class only provides constructors and internal implementation
 * methods.
 *
 * @see SearchIterator
 * @see RuleBasedCollator
 *
 * @author Laura Werner
 * @version 1.0
 */

char  StringSearch::fgClassID = 0; // Value is irrelevant       // class id
/* to be removed */
void StringSearch::dumpTables() {
    int i;
    for (i = 0; i < 256; i++) {
        if (shiftTable[i] != minLen) {
//            debug("shift[" + Integer.toString(i,16) + "] = " + shiftTable[i]);
        }
    }
    for (i = 0; i < 256; i++) {
        if (backShiftTable[i] != minLen) {
//            debug("backShift[" + Integer.toString(i,16) + "] = " + backShiftTable[i]);
        }
    }
}

StringSearch::StringSearch(const UnicodeString& pat, 
                CharacterIterator* target,
                RuleBasedCollator* coll, 
                BreakIterator* breaker,
                UErrorCode& status) :
    SearchIterator(target, breaker),
    strength(coll->getStrength()),
    pattern(pat),
    valueList(NULL),
    valueListLen(0),
    normLen(0),        // num. of collation elements in pattern.
    minLen(0),         // Min of composed, decomposed versions
    maxLen(0),         // Max
    it(NULL)

{
    if (U_FAILURE(status)) return;
    collator = (RuleBasedCollator*)(coll->clone());
    iter = collator->createCollationElementIterator(*target);
    it = collator->createCollationElementIterator(pat);
     
    initialize(status);   // Initialize the Boyer-Moore tables
}

/**
 * Construct a <code>StringSearch</code> object using a specific collator.
 * <p>
 * @param pattern   The text for which this object will search.
 *
 * @param target    The text in which to search for the pattern.
 *
 * @param collator  A <code>RuleBasedCollator</code> object which defines the
 *                  language-sensitive comparison rules used to determine 
 *                  whether text in the pattern and target matches.
 */
StringSearch::StringSearch(const UnicodeString& pat,
                 CharacterIterator* target,
                 RuleBasedCollator* collator,
                 UErrorCode& status) :
    SearchIterator(),
    strength(collator->getStrength()),
    pattern(pat),
    valueList(NULL),
    valueListLen(0),
    normLen(0),        // num. of collation elements in pattern.
    minLen(0),         // Min of composed, decomposed versions
    maxLen(0),          // Max
    it(NULL)
{
    if (U_FAILURE(status)) return;
    this->adoptTarget(target);
    this->collator = (RuleBasedCollator*)(collator->clone());
    this->iter = collator->createCollationElementIterator(*target);
    this->it = collator->createCollationElementIterator(pat);
    initialize(status);
}

/**
 * Construct a <code>StringSearch</code> object using the collator and
 * character boundary detection rules for a given locale
 * <p>
 * @param pattern   The text for which this object will search.
 *
 * @param target    The text in which to search for the pattern.
 *
 * @param loc       The locale whose collation and break-detection rules
 *                  should be used.
 *
 * @exception       ClassCastException thrown if the collator for the specified
 *                  locale is not a RuleBasedCollator.
 */
StringSearch::StringSearch(const StringSearch& that) :
    SearchIterator(that),    
    valueList(NULL),
    valueListLen(that.valueListLen),
    normLen(that.normLen),        // num. of collation elements in pattern.
    minLen(that.minLen),          // Min of composed, decomposed versions
    maxLen(that.maxLen),
    collator(that.collator),
    strength(that.strength),
    iter(NULL),
    it(NULL)
{
    valueList = new int32_t[valueListLen];
    memcpy(valueList, that.valueList, valueListLen*sizeof(int32_t));    
    iter = that.collator->createCollationElementIterator(that.getTarget());
    it = that.collator->createCollationElementIterator(that.pattern);
}

StringSearch::StringSearch(const UnicodeString& pat, 
                 CharacterIterator* target, 
                 const Locale& loc,
                 UErrorCode& status) :
    SearchIterator(),
    pattern(pat),
    valueList(NULL),
    valueListLen(0),
    normLen(0),        // num. of collation elements in pattern.
    minLen(0),         // Min of composed, decomposed versions
    maxLen(0)          // Max
{
    if (U_FAILURE(status)) return;
    this->adoptTarget(target);
    collator = (RuleBasedCollator*)Collator::createInstance(loc, status);
    iter = collator->createCollationElementIterator(*target);
    it = collator->createCollationElementIterator(pat);

    strength = collator->getStrength(); 

    initialize(status);
}

bool_t
StringSearch::operator==(const SearchIterator& that) const
{
    if (that.getDynamicClassID() != getDynamicClassID())
        return FALSE;
    if (!SearchIterator::operator==(that))
        return FALSE;
    const StringSearch& that2 = (const StringSearch&)that;
    if (*that2.iter != *iter) return FALSE;
    else if (*that2.collator != *collator) return FALSE;
    else if (that2.strength != strength) return FALSE;
    else if (that2.valueListLen != valueListLen) return FALSE;
    else if (memcmp(that2.valueList, valueList, valueListLen*sizeof(int32_t)) != 0) return FALSE;
    else if (that2.pattern != pattern) return FALSE;
    else if (that2.normLen != normLen) return FALSE;
    else if (that2.minLen != minLen) return FALSE;
    else if (that2.maxLen != maxLen) return FALSE;
    else return TRUE;
}

SearchIterator* 
StringSearch::clone(void) const
{
    return new StringSearch(*this);
}

/**
 * Construct a <code>StringSearch</code> object using the collator for the default
 * locale
 * <p>
 * @param pattern   The text for which this object will search.
 *
 * @param target    The text in which to search for the pattern.
 *
 * @param collator  A <code>RuleBasedCollator</code> object which defines the
 *                  language-sensitive comparison rules used to determine 
 *                  whether text in the pattern and target matches.
 */
StringSearch::StringSearch(const UnicodeString& pat, 
                 const UnicodeString& newText,
                 UErrorCode& status) :
    SearchIterator(),
    pattern(pat),
    valueList(NULL),
    valueListLen(0),
    normLen(0),        // num. of collation elements in pattern.
    minLen(0),         // Min of composed, decomposed versions
    maxLen(0)          // Max
{
    StringCharacterIterator *s = new StringCharacterIterator(newText);
    collator = (RuleBasedCollator*)Collator::createInstance(Locale::getDefault(), status);
    strength = collator->getStrength(); 
    iter = collator->createCollationElementIterator(newText);
    it = collator->createCollationElementIterator(pat);
    this->adoptTarget(s);
    initialize(status);
}

StringSearch::~StringSearch(void)
{
    if (valueList != NULL) {
        delete [] valueList;
        valueList = 0;
    }
    if (iter != NULL) {
        delete iter;
        iter = 0;
    }
    if (collator != NULL) {
        delete collator;
        collator = 0;
    }
    if (it != NULL) {
        delete it;
        it = 0;
    }
}
//-------------------------------------------------------------------
// Getters and Setters
//-------------------------------------------------------------------

/**
 * Sets this object's strength property. The strength determines the
 * minimum level of difference considered significant during a
 * search.  Generally, {@link Collator#TERTIARY} and 
 * {@link Collator#IDENTICAL} indicate that all differences are
 * considered significant, {@link Collator#SECONDARY} indicates
 * that upper/lower case distinctions should be ignored, and
 * {@link Collator#PRIMARY} indicates that both case and accents
 * should be ignored.  However, the exact meanings of these constants
 * are determined by individual Collator objects.
 * <p>
 * @see Collator#PRIMARY
 * @see Collator#SECONDARY
 * @see Collator#TERTIARY
 * @see Collator#IDENTICAL
 */
void StringSearch::setStrength(Collator::ECollationStrength newStrength, UErrorCode& status) {
    if (U_FAILURE(status))
    {
        return;
    }
    strength = newStrength;
    
    // Due to a bug (?) in CollationElementIterator, we must set the
    // collator's strength as well, since the iterator is going to
    // mask out the portions of the collation element that are not
    // relevant for the collator's current strength setting
    // Note that this makes it impossible to share a Collator among
    // multiple StringSearch objects if you adjust Strength settings.
    collator->setStrength(strength);
    initialize(status);
}


/**
 * Returns this object's strength property, which indicates what level
 * of differences are considered significant during a search.
 * <p>
 * @see #setStrength
 */
Collator::ECollationStrength StringSearch::getStrength() const
{
    return strength;
}

/**
 * Set the collator to be used for this string search.  Also changes
 * the search strength to match that of the new collator.
 * <p>
 * This method causes internal data such as Boyer-Moore shift tables
 * to be recalculated, but the iterator's position is unchanged.
 * <p>
 * @see #getCollator
 */
void StringSearch::setCollator(const RuleBasedCollator *coll, UErrorCode& status) 
{
    delete iter;
    delete collator;
    collator = (RuleBasedCollator*)coll->clone();
    strength = collator->getStrength();
    // Also need to recompute the pattern and get a new target iterator
    iter = collator->createCollationElementIterator(getTarget());
    initialize(status);
}

/**
 * Return the RuleBasedCollator being used for this string search.
 */
const RuleBasedCollator& StringSearch::getCollator(void) const 
{
    return *collator;
}

/**
 * Set the pattern for which to search.  
 * This method causes internal data such as Boyer-Moore shift tables
 * to be recalculated, but the iterator's position is unchanged.
 */
void StringSearch::setPattern(const UnicodeString& pat, UErrorCode& status) 
{
    pattern = pat;
    initialize(status);
}

/**
 * Returns the pattern for which this object is searching.
 */
const UnicodeString& StringSearch::getPattern() const
{
    return pattern;
}

/**
 * Set the target text which should be searched and resets the
 * iterator's position to point before the start of the new text.
 * This method is useful if you want to re-use an iterator to
 * search for the same pattern within a different body of text.
 */
void StringSearch::adoptTarget(CharacterIterator* target) 
{
    UErrorCode status = U_ZERO_ERROR;
    SearchIterator::adoptTarget(target);
    
    // fix me: Skipped the error code
    // Since we're caching a CollationElementIterator, recreate it
    iter->setText(*target, status);
}
void StringSearch::setTarget(const UnicodeString& newText) 
{
    UErrorCode status = U_ZERO_ERROR;
    SearchIterator::setTarget(newText);
    // Since we're caching a CollationElementIterator, recreate it
    iter->setText(newText, status);
}

void StringSearch::reset(void)
{
    SearchIterator::reset();
    iter->reset();
}//-------------------------------------------------------------------
// Privates
//-------------------------------------------------------------------

/**
 * Search forward for matching text, starting at a given location.
 * Clients should not call this method directly; instead they should call
 * {@link SearchIterator#next}.
 * <p>
 * If a match is found, this method returns the index at which the match
 * starts and calls {@link SearchIterator#setMatchLength}
 * with the number of characters in the target
 * text that make up the match.  If no match is found, the method returns
 * <code>DONE</code> and does not call <tt>setMatchLength</tt>.
 * <p>
 * @param start The index in the target text at which the search starts.
 *
 * @return      The index at which the matched text in the target starts, or DONE
 *              if no match was found.
 * <p>
 * @see SearchIterator#next
 * @see SearchIterator#DONE
 */
int32_t StringSearch::handleNext(int32_t start, UErrorCode& status)
{
    if (U_FAILURE(status)) 
    { 
        return SearchIterator::DONE; 
    }
    const CharacterIterator& target = getTarget();
    
    int mask = getMask(strength);
    int done = CollationElementIterator::NULLORDER & mask;
#if 0
    if (DEBUG) {
        debug("-------------------------handleNext-----------------------------------");
        debug("");
        debug("strength=" + strength + ", mask=" + Integer.toString(mask,16)
            + ", done=" + Integer.toString(done,16));
        debug("decomp=" + collator.getDecomposition());
        
        debug("target.begin=" + getTarget().getBeginIndex());
        debug("target.end=" + getTarget().getEndIndex());
        debug("start = " + start);
    }
#endif
    int32_t index = start + minLen;
    int32_t matchEnd = 0;

    while (index <= target.endIndex())
    {
        int32_t patIndex = normLen;
        int32_t tval = 0, pval = 0;
        bool_t getP = TRUE;

        iter->setOffset(index, status);
        matchEnd = index;
        
        //if (DEBUG) debug(" outer loop: patIndex=" + patIndex + ", index=" + index);
        
        while ((patIndex > 0 || getP == false) && iter->getOffset() > start)
        {
#if 0
            if (DEBUG) {
                debug("  inner loop: patIndex=" + patIndex + " iter=" + iter.getOffset());
                debug("   getP=" + getP);
            }
#endif
            
            // Get the previous character in both the pattern and the target
            tval = iter->previous(status) & mask;
            if (U_FAILURE(status)) 
            {
                return SearchIterator::DONE;
            }
            
            if (getP) pval = valueList[--patIndex];
            getP = TRUE;
            
            // (DEBUG) debug("   pval=" + Integer.toString(pval,16) + ", tval=" + Integer.toString(tval,16));
            
            if (tval == 0) {       // skip tval, use same pval
                // (DEBUG) debug("   tval is ignorable");
                getP = FALSE;
            }
            else if (pval != tval) {    // Mismatch, skip ahead
                // (DEBUG) debug("   mismatch: skippping " + getShift(tval, patIndex));
                
                index += getShift(tval, patIndex);
                break;
            }
            else if (patIndex == 0) {
                // The values matched, and we're at the beginning of the pattern,
                // which means we matched the whole thing.
                start = iter->getOffset();
                setMatchLength(matchEnd - start);
                // if (DEBUG) debug("Found match at index "+ start );
                return start;
            }
        }
#if 0
        if (DEBUG) debug(" end of inner loop: patIndex=" + patIndex + " iter=" + iter.getOffset());
        if (DEBUG) debug("   getP=" + getP);
#endif   
        if (iter->getOffset() <= start) {
            // We hit the beginning of the text being searched, which is
            // possible if it contains lots of ignorable characters.
            // Advance one character and try again.
            // if (DEBUG) debug("hit beginning of target; advance by one");
            index++;
        }
    }
    // if (DEBUG) debug("Fell off end of outer loop; returning DONE");
    return SearchIterator::DONE;
}

/**
 * Search backward for matching text ,starting at a given location.
 * Clients should not call this method directly; instead they should call
 * <code>SearchIterator.previous()</code>, which this method overrides.
 * <p>
 * If a match is found, this method returns the index at which the match
 * starts and calls {@link SearchIterator#setMatchLength}
 * with the number of characters in the target
 * text that make up the match.  If no match is found, the method returns
 * <code>DONE</code> and does not call <tt>setMatchLength</tt>.
 * <p>
 * @param start The index in the target text at which the search starts.
 *
 * @return      The index at which the matched text in the target starts, or DONE
 *              if no match was found.
 * <p>
 * @see SearchIterator#previous
 * @see SearchIterator#DONE
 */
int32_t StringSearch::handlePrev(int32_t start, UErrorCode& status)
{
    if (U_FAILURE(status))
    {
        return SearchIterator::DONE;
    }
    int patLen = normLen;
    int index = start - minLen;

    int mask = getMask(strength);
    int done = CollationElementIterator.NULLORDER & mask;
#if 0
    if (DEBUG) {
        debug("-------------------------handlePrev-----------------------------------");
        debug("");
        debug("strength=" + strength + ", mask=" + Integer.toString(mask,16)
            + ", done=" + Integer.toString(done,16));
        debug("decomp=" + collator.getDecomposition());
        
        debug("target.begin=" + getTarget().getBeginIndex());
        debug("target.end=" + getTarget().getEndIndex());
    }
#endif
    
    while (index >= 0) {
        int patIndex = 0;
        int tval = 0, pval = 0;
        bool_t getP = TRUE;

        iter->setOffset(index, status);
        if (U_FAILURE(status))
        {
            return SearchIterator::DONE;
        }


        // if (DEBUG) debug(" outer loop: patIndex=" + patIndex + ", index=" + index);
        
        while ((patIndex < patLen || !getP) && iter->getOffset() < start)
        {
        /*    if (DEBUG) {
                debug("  inner loop: patIndex=" + patIndex + " iter=" + iter.getOffset());
            }
            */
            tval = iter->next(status) & mask;
            if (U_FAILURE(status))
            {
                return SearchIterator::DONE;
            }
            if (getP) pval = valueList[patIndex++];
            getP = TRUE;

            //if (DEBUG) debug("   pval=" + Integer.toString(pval,16) + ", tval=" + Integer.toString(tval,16));

            if (tval == done) {
              //  if (DEBUG) debug("   end of target; no match");
                return DONE;
            }
            else if (tval == 0) {
                // if (DEBUG) debug("   tval is ignorable");
                getP = false;
            }
            else if (pval != tval) {
                // We didn't match this pattern.  Skip ahead
                // if (DEBUG) debug("   mismatch: skippping " + getBackShift(tval, patIndex));
                
                int shift = getBackShift(tval, patIndex);
                index -= shift;
                break;
            }
            else if (patIndex == patLen) {
                // The elements matched and we're at the end of the pattern,
                // which means we matched the whole thing.
                setMatchLength(iter->getOffset() - index);
                return index;
            }
        }
        if (iter->getOffset() >= start) {
            // We hit the end of the text being searched, which is
            // possible if it contains lots of ignorable characters.
            // Back up one character and try again.
            // if (DEBUG) debug("hit end of target; back by one");
            index--;
        }
    }
    return SearchIterator::DONE;
}

/**
 * Return a bitmask that will select only the portions of a collation 
 * element that are significant at the given strength level.
 */
int32_t StringSearch::getMask(Collator::ECollationStrength strength)  
{
    switch (strength) {
    case Collator::PRIMARY:
        return 0xFFFF0000;
    case Collator::SECONDARY:
        return 0xFFFFFF00;
    default:
        return 0xFFFFFFFF;
    }
}


void StringSearch::initialize(UErrorCode& status) {
    /*
    if (DEBUG)  {
        debug("-------------------------initialize-----------------------------------");
        debug("pattern=" + pattern);
    }
    */
    it->setText(pattern, status);
    if (U_FAILURE(status)) {
        delete it;
        return;
    }

    int mask = getMask(strength);

    // See how many non-ignorable collation keys are in the text
    normLen = 0;
    int32_t elem;
    while ((elem = it->next(status)) != CollationElementIterator::NULLORDER)
    {
        if (U_FAILURE(status)) {
            return;
        }
        if ((elem & mask) != 0) {
            normLen++;
        }
    }

    // Save them all
    valueList = new int32_t[normLen];
    int expandLen = 0;
    it->reset();
    
    for (int32_t i = 0; i < normLen; i++)
    {
        elem = it->next(status);
        if (U_FAILURE(status)) {
            return;
        }

        if ((elem & mask) != 0) {
            valueList[i] = elem & mask;
            
        }
        // Keep track of whether there are any expanding-character
        // sequences that can result in one of the characters that's in
        // the pattern.  If there are, we have to reduce the shift
        // distances calculated below to account for it.
        expandLen += it->getMaxExpansion(elem) - 1;
    }

    //
    // We need to remember the size of the composed and decomposed
    // versions of the string.  Standard Boyer-Moore shift calculations
    // can be wrong by an amount up to that difference, since a small
    // small number of characters in the pattern can map to a larger
    // number in the text being searched, or vice-versa.
    //
    int uniLen = pattern.length();
    maxLen = uprv_max(normLen, uniLen);
    minLen = uprv_min(normLen, uniLen) - expandLen; 


    /*
    if (DEBUG) debug("normLen=" + normLen + ", expandLen=" + expandLen
                    + ", maxLen=" + maxLen + ", minLen=" + minLen);
    */
    // Now initialize the shift tables
    //
    // NOTE: This is the most conservative way to build them.  If we had a way
    // of knowing that there were no expanding/contracting chars in the rules,
    // we could get rid of the "- 1" in the shiftTable calculations.
    // But all of the default collators have at least one expansion or
    // contraction, so it probably doesn't matter anyway.
    //
    for (i = 0; i < 256; i++) {
        shiftTable[i] = backShiftTable[i] = minLen;
    }

    for (i = 0; i < normLen-1; i++) {
        shiftTable[hash(valueList[i])] = uprv_max(minLen - i - 1, 1);
    }
    shiftTable[hash(valueList[normLen-1])] = 1;
    
    for (i = normLen - 1; i > 0; i--) {
        backShiftTable[hash(valueList[i])] = i;
    }
    backShiftTable[hash(valueList[0])] = 1;
    
    /* dumpTables(); */
}

/**
 * Method used by StringSearch to determine how far to the right to
 * shift the pattern during a Boyer-Moore search.  
 *
 * @param curValue  The current value in the target text
 * @param curIndex  The index in the pattern at which we failed to match
 *                  curValue in the target text.
 */
int32_t StringSearch::getShift( int32_t curValue, int32_t curIndex ) const
{
    int32_t shiftAmt = shiftTable[hash(curValue)];

    if (minLen != maxLen) {
        int adjust = normLen - curIndex;
        if (shiftAmt > adjust + 1) {
//            if (DEBUG) debug("getShift: adjusting by " + adjust);
            shiftAmt -= adjust;
        }
    }
    return shiftAmt;
}

/**
 * Method used by StringSearch to determine how far to the left to
 * shift the pattern during a reverse Boyer-Moore search.  
 *
 * @param curValue  The current value in the target text
 * @param curIndex  The index in the pattern at which we failed to match
 *                  curValue in the target text.
 */
int32_t StringSearch::getBackShift( int32_t curValue, int32_t curIndex ) const 
{
    int shiftAmt = backShiftTable[hash(curValue)];

    if (minLen != maxLen) {
        int adjust = normLen - (minLen - curIndex);
        if (shiftAmt > adjust + 1) {
            // if (DEBUG) debug("getBackShift: adjusting by " + adjust);
            shiftAmt -= adjust;
        }
    }
    return shiftAmt;
}

/**
 * Hash a collation element from its full size (32 bits) down into a
 * value that can be used as an index into the shift tables.  Right
 * now we do a modulus by the size of the hash table.
 *
 * TODO: At some point I should experiment to see whether a slightly
 * more complicated hash function gives us a better distribution
 * on multilingual text.  I doubt it will have much effect on
 * performance, though.
 */
int32_t StringSearch::hash(int32_t order) 
{
    return CollationElementIterator::primaryOrder(order) % 256;
}

