/*
**********************************************************************
*   Copyright (C) 2001 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*  07/02/2001   synwee      Creation.
**********************************************************************
*/

#include "unicode/usearch.h"
#include "unicode/ustring.h"
#include "unormimp.h"
#include "unicode/uchar.h"
#include "cmemory.h"
#include "ucol_imp.h"
#include "usrchimp.h"

// internal definition ---------------------------------------------------

#define LAST_BYTE_MASK_           0xFF
#define SECOND_LAST_BYTE_SHIFT_   8
#define SUPPLEMENTARY_MIN_VALUE_  0x10000

static const uint16_t *FCD_ = NULL;

// internal methods -------------------------------------------------

/**
* Getting the mask for collation strength
* @param strength collation strength
* @return collation element mask
*/
inline uint32_t getMask(UCollationStrength strength) 
{
    switch (strength) 
    {
    case UCOL_PRIMARY:
        return UCOL_PRIMARYORDERMASK;
    case UCOL_SECONDARY:
        return UCOL_SECONDARYORDERMASK | UCOL_PRIMARYORDERMASK;
    default:
        return UCOL_TERTIARYORDERMASK | UCOL_SECONDARYORDERMASK | 
               UCOL_PRIMARYORDERMASK;
    }
}

/**
* This is to squeeze the 21bit ces into a 256 table
* @param ce collation element
* @return collapsed version of the collation element
*/
inline int hash(uint32_t ce) 
{
    // the old value UCOL_PRIMARYORDER(ce) % MAX_TABLE_SIZE_ does not work
    // well with the new collation where most of the latin 1 characters
    // are of the value xx000xxx. their hashes will most of the time be 0
    // to be discussed on the hash algo.
    return UCOL_PRIMARYORDER(ce) / MAX_TABLE_SIZE_;
}

/**
* Initializing the fcd tables
* @param status error status if any
*/
inline void initializeFCD(UErrorCode *status) 
{
    if (FCD_ == NULL) {
        FCD_ = unorm_getFCDTrie(status);
    }
}

/**
* Gets the fcd value for a character at the argument index.
* This method takes into accounts of the supplementary characters.
* @param str UTF16 string where character for fcd retrieval resides
* @param offset position of the character whose fcd is to be retrieved, to be 
*               overwritten with the next character position, taking 
*               surrogate characters into consideration.
* @param strlength length of the argument string
* @return fcd value
*/
inline uint16_t getFCD(const UChar   *str, UTextOffset *offset, 
                             int32_t  strlength)
{
    UTextOffset temp = *offset;
    uint16_t    result;
    UChar       ch   = str[temp];
    result = unorm_getFCD16(FCD_, ch);
    temp ++;
    
    if (result != 0 && temp != strlength && UTF_IS_FIRST_SURROGATE(ch)) {
        ch = str[temp];
        if (UTF_IS_SECOND_SURROGATE(ch)) {
            result = unorm_getFCD16FromSurrogatePair(FCD_, result, ch);
            temp ++;
        } else {
            result = 0;
        }
    }
    *offset = temp;
    return result;
}

/**
* Getting the modified collation elements taking into account the collation 
* attributes
* @param strsrch string search data
* @param sourcece 
* @return the modified collation element
*/
inline uint32_t getCE(const UStringSearch *strsrch, uint32_t sourcece)
{
    // note for tertiary we can't use the collator->tertiaryMask, that
    // is a preprocessed mask that takes into account case options. since
    // we are only concerned with exact matches, we don't need that.
    sourcece &= strsrch->ceMask;
    
    if (strsrch->toShift) {
        // alternate handling here, since only the 16 most significant digits
        // is only used, we can safely do a compare without masking
        // if the ce is a variable, we mask and get only the primary values
        // no shifting to quartenary is required since all primary values
        // less than variabletop will need to be masked off anyway.
        if (strsrch->variableTop > sourcece) {
            if (strsrch->strength == UCOL_QUATERNARY) {
                sourcece &= UCOL_PRIMARYORDERMASK;
            }
            else { 
                sourcece = UCOL_IGNORABLE;
            }
        }
    }

    return sourcece;
}

/** 
* Allocate a memory and returns NULL if it failed
* @param size to allocate
* @param status error status if any
* @return newly allocated array, NULL otherwise
*/
inline void * allocateMemory(uint32_t size, UErrorCode *status) 
{
    uint32_t *result = (uint32_t *)uprv_malloc(size);
    if (result == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/**
* Adds a uint32_t value to a destination array.
* Creates a new array if we run out of space. The caller will have to 
* manually deallocate the newly allocated array.
* @param destination target array
* @param offset destination offset to add value
* @param destinationlength target array size, return value for the new size
* @param value to be added
* @param increments incremental size expected
* @param status error status if any
* @return new destination array, destination if there was no new allocation
*/
inline uint32_t * addTouint32_tArray(uint32_t   *destination,       
                                     uint32_t    offset, 
                                     uint32_t   *destinationlength, 
                                     uint32_t    value,
                                     uint32_t    increments, 
                                     UErrorCode *status) 
{
    if (U_FAILURE(*status)) {
        return NULL;
    }

    uint32_t newlength = *destinationlength;
    if (offset + 1 == newlength) {
        newlength += increments;
        uint32_t *temp = (uint32_t *)allocateMemory(
                                         sizeof(uint32_t) * newlength, status);
        if (temp == NULL) {
            return NULL;
        }
        uprv_memcpy(temp, destination, sizeof(uint32_t) * offset);
        *destinationlength = newlength;
        destination        = temp;
    }
    destination[offset] = value;
    return destination;
}

/**
* Initializing the ce table for a pattern.
* Stores non-ignorable collation keys.
* Table size will be estimated by the size of the pattern text. Table 
* expansion will be perform as we go along. Adding 1 to ensure that the table 
* size definitely increases.
* @param strsrch string search data
* @param status error status if any
* @return total number of expansions 
*/
inline uint32_t initializePatternCETable(UStringSearch *strsrch, 
                                         UErrorCode    *status)
{
    if (U_SUCCESS(*status)) {
        UPattern *pattern       = &(strsrch->pattern);
        uint32_t  cetablesize   = INITIAL_ARRAY_SIZE_;
        uint32_t *cetable       = pattern->CEBuffer;
        uint32_t  patternlength = pattern->textLength;

        UCollationElements *coleiter = ucol_openElements(strsrch->collator, 
                                        pattern->text, patternlength, status);

        if (pattern->CE != NULL && pattern->CE != cetable) {
            uprv_free(pattern->CE);
        }
        
        uint32_t  offset      = 0;
        uint32_t  result      = 0;
        uint32_t  ce;

        while ((ce = ucol_next(coleiter, status)) != UCOL_NULLORDER) {
            uint32_t newce = getCE(strsrch, ce);
            if (newce) {
                uint32_t *temp = addTouint32_tArray(cetable, offset, 
                                                    &cetablesize, newce,
                                  patternlength - ucol_getOffset(coleiter) + 1, 
                                                    status);
                if (U_FAILURE(*status)) {
                    return 0;
                }
                offset ++;
                if (cetable != temp && cetable != pattern->CEBuffer) {
                    uprv_free(cetable);
                }
                cetable = temp;
            }
            result += ucol_getMaxExpansion(coleiter, ce) - 1;
        }

        cetable[offset]   = 0;
        pattern->CE       = cetable;
        pattern->CELength = offset;

        if (coleiter != NULL) {
            ucol_closeElements(coleiter);
        }
        
        return result;
    }
    return 0;
}

/**
* Initializes the pattern struct
* @param strsrch       UStringSearch data storage
* @param splitsize     array of size 2 containing 
*                      1) the total number of characters from start to 
*                         the last base character, including any contracting
*                         accents of the last base character.
*                      2) the total number of characters from the 
*                         the first base character, to the end, including any 
*                         contracting accents of the first base character.
* @param status        for errors if it occurs
* @return expansionsize the total expansion size of the pattern
*/ 
inline int32_t initializePattern(UStringSearch *strsrch, UErrorCode *status) 
{
         UPattern   *pattern     = &(strsrch->pattern);
   const UChar      *patterntext = pattern->text;
         int32_t     result      = 0;
         int32_t     length      = pattern->textLength;
         UTextOffset index       = 0;
         // UChar32   firstchar;
         UChar32     lastchar;

    /* FCD changed
    UTF_NEXT_CHAR(patterntext, index, length, firstchar);
    index = length;
    UTF_PREV_CHAR(patterntext, length, length, lastchar);
    pattern->hasPrefixAccents = getFCD(firstchar) >> SECOND_LAST_BYTE_SHIFT_;
    pattern->hasSuffixAccents = getFCD(lastchar) & LAST_BYTE_MASK_;
    */
    pattern->hasPrefixAccents = getFCD(patterntext, &index, length) >> 
                                                    SECOND_LAST_BYTE_SHIFT_;
    index = length;
    UTF_PREV_CHAR(patterntext, 0, index, lastchar);
    pattern->hasSuffixAccents = getFCD(patterntext, &index, length) & 
                                                             LAST_BYTE_MASK_;
    
    result = initializePatternCETable(strsrch, status);   
    return result;
}

/**
* Initializing shift tables, with the default values.
* If a corresponding default value is 0, the shift table is not set.
* @param shift table for forwards shift 
* @param backshift table for backwards shift
* @param cetable table containing pattern ce
* @param cesize size of the pattern ces
* @param expansionsize total size of the expansions
* @param defaultforward the default forward value
* @param defaultbackward the default backward value
*/
inline void setShiftTable(int32_t   shift[], int32_t backshift[], 
                          uint32_t *cetable, int32_t cesize, 
                          int32_t   expansionsize,
                          int32_t   defaultforward,
                          int32_t   defaultbackward)
{
    // estimate the value to shift. to do that we estimate the smallest 
    // number of characters to give the relevant ces, ie approximately
    // the number of ces minus their expansion, since expansions can come 
    // from a character.
    int32_t count;
    for (count = 0; count < MAX_TABLE_SIZE_; count ++) {
        shift[count] = defaultforward;
    }
    for (count = 0; count < cesize - 1; count ++) {
        // number of ces from right of array to the count
        int temp = defaultforward - count - 1;
        shift[hash(cetable[count])] = temp > 1 ? temp : 1;
    }
    shift[hash(cetable[cesize - 1])] = 1;
    // for ignorables we just shift by one. see test examples.
    shift[hash(0)] = 1;
    
    for (count = 0; count < MAX_TABLE_SIZE_; count ++) {
        backshift[count] = defaultbackward;
    }
    for (count = cesize - 1; count > 0; count --) {
        // the original value count does not seem to work
        backshift[hash(cetable[count])] = count > expansionsize ? 
                                          count - expansionsize : 1;
    }
    backshift[hash(cetable[0])] = 1;
    backshift[hash(0)] = 1;
}

/**
* Building of the pattern collation element list and the boyer moore strsrch
* table.
* The canonical match will only be performed after the default match fails.
* For both cases we need to remember the size of the composed and decomposed
* versions of the string. Since the Boyer-Moore shift calculations shifts by
* a number of characters in the text and tries to match the pattern from that 
* offset, the shift value can not be too large in case we miss some 
* characters. To choose a right shift size, we estimate the NFC form of the 
* and use its size as a shift guide. The NFC form should be the small 
* possible representation of the pattern. Anyways, we'll err on the smaller
* shift size. Hence the calculation for minlength.
* Canonical match will be performed slightly differently. We'll split the 
* pattern into 3 parts, the prefix accents (PA), the middle string bounded by 
* the first and last base character (MS), the ending accents (EA). Matches 
* will be done on MS first, and only when we match MS then some processing
* will be required for the prefix and end accents in order to determine if
* they match PA and EA. Hence the default shift values 
* for the canonical match will take the size of either end's accent into 
* consideration. Forwards search will take the end accents into consideration
* for the default shift values and the backwards search will take the prefix
* accents into consideration.
* If pattern has no non-ignorable ce, we return a illegal argument error.
* @param strsrch UStringSearch data storage
* @param status  for errors if it occurs
*/ 
inline void initialize(UStringSearch *strsrch, UErrorCode *status) 
{
    // uint32_t splitsize[2];
    int32_t expandlength = initializePattern(strsrch, /*splitsize,*/ status);   
    if (U_SUCCESS(*status) && strsrch->pattern.CELength > 0) {
        UPattern *pattern       = &strsrch->pattern;
        int32_t   cesize        = pattern->CELength;

        int32_t minlength = cesize > expandlength ? cesize - expandlength : 
                                                                           1;
        pattern->defaultShiftSize    = minlength;
        setShiftTable(pattern->shift, pattern->backShift, pattern->CE,
                          cesize, expandlength, minlength, minlength);
    }
    else {
        strsrch->pattern.defaultShiftSize = 0;
    }
}

/**
* Determine whether the target text in UStringSearch bounded by the offset 
* start and end is one or more whole units of text as 
* determined by the breakiterator in UStringSearch.
* @param strsrch string search data 
* @param start target text start offset
* @param end target text end offset
*/
inline UBool isBreakUnit(const UStringSearch *strsrch, UTextOffset start, 
                               UTextOffset    end)
{
    UBreakIterator *breakiterator = strsrch->search->breakIter;
    if (breakiterator != NULL) {
        UTextOffset startindex = ubrk_first(breakiterator);
        UTextOffset endindex   = ubrk_last(breakiterator);
        
        // out-of-range indexes are never boundary positions
        if (start < startindex || start > endindex ||
            end < startindex || end > endindex) {
            return FALSE;
        }
        // otherwise, we can use following() on the position before the 
        // specified one and return true of the position we get back is the 
        // one the user specified
        return (start == startindex || 
                ubrk_following(breakiterator, start - 1) == start) && 
               (end == endindex || 
                ubrk_following(breakiterator, end - 1) == end);
    }
    return TRUE;
}

/**
* Getting the next base character offset if current offset is an accent, 
* or the current offset if the current character contains a base character. 
* accents the following base character will be returned
* @param text string
* @param textoffset current offset
* @param textlength length of text string
* @return the next base character or the current offset
*         if the current character is contains a base character.
*/
inline UTextOffset getNextBaseOffset(const UChar       *text, 
                                           UTextOffset  textoffset,
                                           int32_t      textlength)
{
    if (textoffset >= textlength) {
        return textlength;
    }
    // UChar32     codepoint; 
    UTextOffset temp = textoffset;
    // UTF_NEXT_CHAR(text, temp, textlength, codepoint);
    if (getFCD(text, &temp, textlength) >> SECOND_LAST_BYTE_SHIFT_) {
        UTextOffset result = temp;
        while (temp < textlength) { 
            result = temp;
            // UTF_NEXT_CHAR(text, temp, textlength, codepoint);
            if ((getFCD(text, &temp, textlength) >> SECOND_LAST_BYTE_SHIFT_) 
                                                                     == 0) {
                return result;
            }
        }
        return result;
    }
    return textoffset;
}

/**
* Gets the next base character offset depending on the string search pattern
* data
* @param strsrch string search data
* @param textoffset current offset, one offset away from the last character
*                   to search for.
* @return start index of the next base character or the current offset
*         if the current character is contains a base character.
*/
inline UTextOffset getNextUStringSearchBaseOffset(UStringSearch *strsrch, 
                                                  UTextOffset    textoffset)
{
    int32_t textlength = strsrch->search->textLength;
    if (strsrch->pattern.hasSuffixAccents) {
              UChar32      codepoint; 
              UTextOffset  temp = textoffset;
        const UChar       *text = strsrch->search->text;
        UTF_PREV_CHAR(text, 0, temp, codepoint);
        if (getFCD(text, &temp, textlength) & LAST_BYTE_MASK_) {
            return getNextBaseOffset(text, textoffset, textlength);
        }
    }
    if (textoffset > textlength) {
        return textlength;
    }
    return textoffset;
}

/**
* Shifting the collation element iterator position forward to prepare for
* a following match. If the last character is a unsafe character, we'll only
* shift by 1 to capture contractions, normalization etc.
* @param text strsrch string search data
* @param textoffset start text position to do search
* @param ce the text ce which failed the match.
* @param patternceindex index of the ce within the pattern ce buffer which
*        failed the match
* @param status error if any
* @return final offset
*/
inline UTextOffset shiftForward(UStringSearch *strsrch,
                                UTextOffset    textoffset,
                                uint32_t       ce,
                                int32_t        patternceindex,
                                UErrorCode    *status)
{
    if (U_SUCCESS(*status)) {
        int32_t  textlength = strsrch->search->textLength;
        if (textoffset < textlength && strsrch->search->isOverlap) {
            textoffset ++;
        }
        else {
            int32_t shift = 
                strsrch->pattern.shift[hash(ce)];
            // this is to adjust for characters in the middle of the substring 
            // for matching that failed.
            int32_t adjust = strsrch->pattern.CELength - patternceindex;
            if (adjust > 1 && shift >= adjust) {
                shift -= adjust - 1;
            }
    
            textoffset += shift;
        }       
        textoffset = getNextUStringSearchBaseOffset(strsrch, textoffset);
        // check for unsafe characters
        // * if it is the start or middle of a contraction: to be done after 
        //   a initial match is found
        // * thai or lao base consonant character: similar to contraction
        // * high surrogate character: similar to contraction
        // * next character is a accent: shift to the next base character
        ucol_setOffset(strsrch->textIter, textoffset, status);
    }
    return textoffset;
}

/**
* sets match not found 
* @param strsrch string search data
* @param status error status if any
*/
inline void setMatchNotFound(UStringSearch *strsrch, UErrorCode *status) 
{
    strsrch->search->matchedIndex = USEARCH_DONE;
    strsrch->search->matchedLength = 0;
    if (strsrch->search->isForwardSearching) {
        ucol_setOffset(strsrch->textIter, strsrch->search->textLength, 
                       status);
    }
    else {
        ucol_setOffset(strsrch->textIter, 0, status);
    }
}

/**
* Gets the offset to the next safe point in text.
* ie. not the middle of a contraction, swappable characters or supplementary
* characters.
* @param collator collation sata
* @param text string to work with
* @param textoffset offset in string
* @param textlength length of text string
* @return offset to the next safe character
*/
inline UTextOffset getNextSafeOffset(const UCollator   *collator, 
                                     const UChar       *text,
                                           UTextOffset  textoffset,
                                           int32_t      textlength)
{
    UTextOffset result = textoffset; // first contraction character
    while (result != textlength && ucol_unsafeCP(text[result], collator)) {
        result ++;
    }
    return result; 
}

/** 
* This checks for accents in the potential match started with a .
* composite character.
* This is really painful... we have to check that composite character do not 
* have any extra accents. We have to normalize the potential match and find 
* the immediate decomposed character before the match.
* The first composite character would have been taken care of by the fcd 
* checks in checkForwardExactMatch.
* This is the slow path after the fcd of the first character and 
* the last character has been checked by checkForwardExactMatch and we 
* determine that the potential match has extra non-ignorable preceding
* ces.
* @param strsrch string search data
* @param start index of the potential unfriendly composite character
* @param end index of the potential unfriendly composite character
* @param status error status if any
* @return TRUE if there is non-ignorable accents before at the beginning
*              of the match, FALSE otherwise.
*/
UBool checkExtraMatchAccents(const UStringSearch *strsrch, UTextOffset  start,
                                   UTextOffset    end,     
                                   UErrorCode    *status)
{
    UBool result = FALSE;
    if (strsrch->pattern.hasPrefixAccents) {
              UTextOffset  length = end - start;
              UChar32      codepoint;
              UTextOffset  offset = 0;
        const UChar       *text   = strsrch->search->text + start;
        
        UTF_NEXT_CHAR(text, offset, length, codepoint);
        // we are only concerned with the first composite character
        if (unorm_quickCheck(text, offset, UNORM_NFD, status) == UNORM_NO) {
            UTextOffset safeoffset = getNextSafeOffset(
                                  strsrch->collator, text, 0, length);
            if (safeoffset != length) {
                safeoffset ++;
            }
            UChar   *norm = NULL;
            UChar    buffer[INITIAL_ARRAY_SIZE_];
            int32_t  size = unorm_normalize(text, safeoffset, UNORM_NFD, 0, 
                                            buffer, INITIAL_ARRAY_SIZE_, 
                                            status);    
            if (size >= INITIAL_ARRAY_SIZE_) {
                norm = (UChar *)allocateMemory((size + 1) * sizeof(UChar),
                                               status);
                if (norm == NULL) {
                    return TRUE;
                }
                size = unorm_normalize(text, safeoffset, UNORM_NFD, 0, norm, 
                                       size, status);
            }
            else {
                norm = buffer;
            }

            // TODO: keeping pattern iterator and setting text here
            UCollationElements *coleiter  = 
              ucol_openElements(strsrch->collator, norm, size, status);
            uint32_t            firstce   = strsrch->pattern.CE[0];
            UBool               ignorable = TRUE;
            uint32_t            ce        = UCOL_IGNORABLE;
            while (U_SUCCESS(*status) && ce != firstce) {
                offset = ucol_getOffset(coleiter);
                if (ce != firstce && ce != UCOL_IGNORABLE) {
                    ignorable = FALSE;
                }
                ce     = ucol_next(coleiter, status);
            }

            ucol_closeElements(coleiter);
            UTF_PREV_CHAR(norm, 0, offset, codepoint);
            result = !ignorable && (u_getCombiningClass(codepoint) != 0);

            if (norm != buffer) {
                uprv_free(norm);
            }
        }
    }

    return result;
}

/**
* Used by exact matches, checks if there are accents before the match. 
* This is really painful... we have to check that composite characters at
* the start of the matches have to not have any extra accents. 
* In order to determine that we have to normalize the first composite 
* character and find the immediate decomposed character before the match to 
* see if it is an non-ignorable accent.
* Now normalizing the first composite character is enough because we ensure 
* that when the match is passed in here with extra beginning ces, the 
* first or last ce that match has to occur within the first character.
* @param strsrch string search data
* @param start offset 
* @param end offset
* @return TRUE if there are accents on either side of the match, 
*         FALSE otherwise
*/
UBool hasAccentsBeforeMatch(const UStringSearch *strsrch, UTextOffset start,
                                  UTextOffset    end) 
{
    // TODO: Add example
    if (strsrch->pattern.hasPrefixAccents) {
        UCollationElements *coleiter  = strsrch->textIter;
        UErrorCode          status    = U_ZERO_ERROR;
        // we have been iterating forwards previously
        uint32_t            ignorable = TRUE;
        uint32_t            firstce   = strsrch->pattern.CE[0];

        ucol_setOffset(coleiter, start, &status);
        uint32_t ce  = getCE(strsrch, ucol_next(coleiter, &status));
        while (ce != firstce) {
            if (ce != UCOL_IGNORABLE) {
                ignorable = FALSE;
            }
            ce = getCE(strsrch, ucol_next(coleiter, &status));
            if (U_FAILURE(status)) {
                return TRUE;
            }
        }
        if (!ignorable && inNormBuf(coleiter)) {
            // within normalization buffer, discontiguous handled here
            return TRUE;
        }

        // within text
        UTextOffset temp = start;
        UBool accent = (getFCD(strsrch->search->text, &temp, 
                               strsrch->search->textLength) >> 
                                                    SECOND_LAST_BYTE_SHIFT_); 
        if (!accent) {
            return checkExtraMatchAccents(strsrch, start, end, &status);
        }
        if (!ignorable) {
            return TRUE;
        }
        if (start > 0) {
            temp = start;
            UChar32 previous;
            UTF_PREV_CHAR(strsrch->search->text, 0, temp, previous);
            if (getFCD(strsrch->search->text, &temp, 
                       strsrch->search->textLength) & LAST_BYTE_MASK_) {
                ucol_setOffset(coleiter, start, &status);
                ce = ucol_previous(coleiter, &status);
                if (U_FAILURE(status) || 
                    (ce != UCOL_NULLORDER && ce != UCOL_IGNORABLE)) {
                    return TRUE;
                }
            }
        }
    }
  
    return FALSE;
}

/**
* Used by exact matches, checks if there are accents bounding the match.
* Note this is the initial boundary check. If the potential match
* starts or ends with composite characters, the accents in those
* characters will be determined later.
* Not doing backwards iteration here, since discontiguos contraction for 
* backwards collation element iterator, use up too many characters.
* @param strsrch string search data
* @param start offset of match
* @param end end offset of the match
* @return TRUE if there are accents on either side of the match, 
*         FALSE otherwise
*/
UBool hasAccentsAfterMatch(const UStringSearch *strsrch, UTextOffset start,               
                                 UTextOffset    end) 
{
    if (strsrch->pattern.hasSuffixAccents) {
        const UChar       *text       = strsrch->search->text;
              UChar32      lastchar   = 0;
              UTextOffset  temp       = end;
              int32_t      textlength = strsrch->search->textLength;
        UTF_PREV_CHAR(text, 0, temp, lastchar);
        if (getFCD(text, &temp, textlength) & LAST_BYTE_MASK_) {
            uint32_t            firstce  = strsrch->pattern.CE[0];
            UCollationElements *coleiter = strsrch->textIter;
            UErrorCode          status   = U_ZERO_ERROR;
            ucol_setOffset(coleiter, start, &status);
            while (getCE(strsrch, ucol_next(coleiter, &status)) != firstce) {
                if (U_FAILURE(status)) {
                    return TRUE;
                }
            }
            int32_t count = 1;
            while (count < strsrch->pattern.CELength) {
                ucol_next(coleiter, &status);
                if (U_FAILURE(status)) {
                    return TRUE;
                }
                count ++;
            }
            uint32_t ce = getCE(strsrch, ucol_next(coleiter, &status));
            if (ce != UCOL_NULLORDER && ce != UCOL_IGNORABLE) {
                if (ucol_getOffset(coleiter) == end) {
                    return TRUE;
                }
                if (getFCD(text, &end, textlength) >> SECOND_LAST_BYTE_SHIFT_) {
                    return TRUE;
                }
            }
        }
    }
    return FALSE;
}

/**
* Checks if the offset runs out of the text string
* @param offset 
* @param textlength of the text string
* @return TRUE if offset is out of bounds, FALSE otherwise
*/
inline UBool isOutOfBounds(int32_t textlength, UTextOffset offset)
{
    return !(offset >= 0 && (offset <= textlength));
}

/**
* Checks for identical match
* @param strsrch string search data
* @param start offset of possible match
* @param end offset of possible match
* @return TRUE if identical match is found
*/
inline UBool checkIdentical(const UStringSearch *strsrch, UTextOffset start, 
                                  UTextOffset    end) 
{
    int32_t length = end - start;
    if (strsrch->strength != UCOL_IDENTICAL) {
        return TRUE;
    }

    if (strsrch->pattern.textLength != length) {
        return FALSE;
    }

    return (uprv_memcmp(strsrch->pattern.text, strsrch->search->text + start, 
                        length * sizeof(UChar)) == 0);
}

/**
* Checks to see if the match is repeated
* @param strsrch string search data
* @param start new match start index
* @param end new match end index
* @return TRUE if the the match is repeated, FALSE otherwise
*/
inline UBool checkRepeatedMatch(UStringSearch *strsrch,
                                UTextOffset    start,
                                UTextOffset    end)
{
    UTextOffset lastmatchindex = strsrch->search->matchedIndex;
    UBool       result;
    if (lastmatchindex == USEARCH_DONE) {
        return FALSE;
    }
    if (strsrch->search->isForwardSearching) {
        result = start <= lastmatchindex;
    }
    else {
        result = start >= lastmatchindex;
    }
    // TODO: example for overlapping case
    if (!strsrch->search->isOverlap) {
        if (strsrch->search->isForwardSearching) {
            result = start < lastmatchindex + strsrch->search->matchedLength;
        }
        else {
            result = end > lastmatchindex;
        }
    }
    return result;
}

/**
* Gets the collation element iterator's current offset.
* @param coleiter collation element iterator
* @param forwards flag TRUE if we are moving in th forwards direction
* @return current offset 
*/
inline UTextOffset getColElemIterOffset(const UCollationElements *coleiter,
                                              UBool               forwards)
{
    UTextOffset result = ucol_getOffset(coleiter);
    // intricacies of the the backwards collation element iterator
    if (!forwards && inNormBuf(coleiter) && !isFCDPointerNull(coleiter)) {
        result ++;
    }
    return result;
}

/**
* Checks and sets the match information if found.
* Checks 
* <ul>
* <li> the potential match does not repeat the previous match
* <li> boundaries are correct
* <li> exact matches has no extra accents
* <li> identical matches
* <li> potential match does not end in the middle of a contraction
* <\ul>
* Otherwise the offset will be shifted to the next character.
* @param strsrch string search data
* @param textoffset offset in the collation element text. the returned value
*        will be the truncated end offset of the match or the new start 
*        search offset.
* @param status error status if any
* @return TRUE if the match is valid, FALSE otherwise
*/
UBool checkNextExactMatch(UStringSearch *strsrch, UTextOffset *textoffset, 
                          UErrorCode    *status)
{
    // to ensure that the start and ends are not composite characters
    if (U_SUCCESS(*status)) { 
        UCollationElements *coleiter   = strsrch->textIter;
        // do next one more time to get the correct starting offset
        int32_t             expansion  = getExpansionPrefix(coleiter);
        UBool               expandflag = expansion > 0;
        UTextOffset         start      = getColElemIterOffset(coleiter, FALSE);        
        UTextOffset         temp       = start;
        ucol_setOffset(coleiter, start, status);

        // preprocessing possible here for possible contraction in pattern.
        while (expansion > 0) {
            // getting rid of the redundant ce
            // since backward contraction/expansion may have extra ces
            // if we are in the normalization buffer, hasAccentsBeforeMatch
            // would have taken care of it.
            // TODO: commments with examples
            ucol_next(coleiter, status);
            if (ucol_getOffset(coleiter) != temp) {
                start = temp;
                temp  = ucol_getOffset(coleiter);
            }
            expansion --;
        }

        uint32_t *patternce       = strsrch->pattern.CE;
        int32_t   patterncelength = strsrch->pattern.CELength;
        int32_t   count           = 0;
        uint32_t  ce; 
        while (count < patterncelength) {
            ce = getCE(strsrch, ucol_next(coleiter, status));
            if (ce == UCOL_IGNORABLE) {
                continue;
            }
            if (expandflag && count == 0 && ucol_getOffset(coleiter) != temp) {
                start = temp;
                temp  = ucol_getOffset(coleiter);
            }
            if (U_FAILURE(*status) || ce != patternce[count]) {
                if ((*textoffset) < strsrch->search->textLength) {
                    *textoffset = getNextUStringSearchBaseOffset(strsrch, 
                                                           (*textoffset) + 1);  
                }
                return FALSE;
            }
            count ++;
        }

        // remove redundant end codepoints
        UTextOffset end = ucol_getOffset(coleiter); 

        // this totally matches, however we need to check if it is repeating
        if (!isBreakUnit(strsrch, start, end) ||
            checkRepeatedMatch(strsrch, start, end) || 
            hasAccentsBeforeMatch(strsrch, start, end) || 
            hasAccentsAfterMatch(strsrch, start, end) ||
            !checkIdentical(strsrch, start, end)) {
            *textoffset = getNextUStringSearchBaseOffset(strsrch, 
                                                           (*textoffset) + 1);  
            return FALSE;
        }
        
        // totally match, we will get rid of the ending ignorables.
        strsrch->search->matchedIndex  = start;
        strsrch->search->matchedLength = *textoffset - start;
        return TRUE;
    }

    return FALSE;
}

/**
* Getting the previous base character offset, or the current offset if the 
* current character is a base character
* @param text string
* @param textoffset one offset after the current character
* @return the offset of the next character after the base character or the first 
*         composed character with accents
*/
inline UTextOffset getPreviousBaseOffset(const UChar       *text, 
                                               UTextOffset  textoffset)
{
    if (textoffset > 0) {
        UChar32     codepoint; 
        UTextOffset result;
        UTextOffset temp;
        while (TRUE) {
            if (textoffset == 0) {
                return 0;
            }
            result = textoffset;
            UTF_PREV_CHAR(text, 0, textoffset, codepoint);
            temp = textoffset;
            uint16_t fcd = getFCD(text, &temp, result);
            if ((fcd >> SECOND_LAST_BYTE_SHIFT_) == 0) {
                if (fcd & LAST_BYTE_MASK_) {
                    return textoffset;
                }
                return result;
            }
        }
    }
    return 0;
}

/**
* Getting the indexes of the accents that are not blocked in the argument
* accent array
* @param accents array of accents in nfd terminated by a 0.
* @param accentsindex array of indexes of the accents that are not blocked
*/
inline int getUnblockedAccentIndex(UChar *accents, UTextOffset *accentsindex)
{
    UTextOffset index     = 0;
    int32_t     length    = u_strlen(accents);
    UChar32     codepoint = 0;
    int         cclass    = 0;
    int         result    = 0;
    UTextOffset temp;
    while (index < length) {
        temp = index;
        UTF_NEXT_CHAR(accents, index, length, codepoint);
        if (u_getCombiningClass(codepoint) != cclass) {
            cclass        = u_getCombiningClass(codepoint);
            accentsindex[result] = temp;
            result ++;
        }
    }
    accentsindex[result] = length;
    return result;
}

/**
* Appends 3 UChar arrays to a destination array.
* Creates a new array if we run out of space. The caller will have to 
* manually deallocate the newly allocated array.
* @param destination target array
* @param destinationlength target array size, returning the appended length
* @param source1 null-terminated first array
* @param source2 second array
* @param source2length length of seond array
* @param source3 null-terminated third array
* @param status error status if any
* @return new destination array, destination if there was no new allocation
*/
inline UChar * addToUCharArray(      UChar      *destination,  
                                     int32_t    *destinationlength, 
                               const UChar      *source1, 
                               const UChar      *source2,
                                     int32_t     source2length, 
                               const UChar      *source3, 
                                     UErrorCode *status) 
{
    if (U_FAILURE(*status)) {
        return NULL;
    }

    int32_t source1length = source1 != NULL ? u_strlen(source1) : 0;
    int32_t source3length = source3 != NULL ? u_strlen(source3) : 0;            
    if (*destinationlength < source1length + source2length + source3length + 
                                                                           1) 
    {
        destination = (UChar *)allocateMemory(
          (source1length + source2length + source3length + 1) * sizeof(UChar),
          status);
        if (destination == NULL) {
            *destinationlength = 0;
            return NULL;
        }
    }
    if (source1length != 0) {
        uprv_memcpy(destination, source1, sizeof(UChar) * source1length);
    }
    if (source2length != 0) {
        uprv_memcpy(destination + source1length, source2, 
                    sizeof(UChar) * source2length);
    }
    if (source3length != 0) {
        uprv_memcpy(destination + source1length + source2length, source3, 
                    sizeof(UChar) * source3length);
    }
    *destinationlength = source1length + source2length + source3length;
    return destination;
}

/**
* Running through a collation element iterator to see if the contents matches
* pattern in string search data
* @param strsrch string search data
* @param coleiter collation element iterator
* @return TRUE if a match if found, false otherwise
*/
inline UBool checkCollationMatch(const UStringSearch      *strsrch, 
                                       UCollationElements *coleiter)
{
    int         patternceindex = strsrch->pattern.CELength;
    uint32_t   *patternce      = strsrch->pattern.CE;
    UErrorCode  status = U_ZERO_ERROR;
    while (patternceindex > 0) {
        uint32_t ce = getCE(strsrch, ucol_next(coleiter, &status));
        if (ce == UCOL_IGNORABLE) {
            continue;
        }
        if (U_FAILURE(status) || ce != *patternce) {
            return FALSE;
        }
        patternce ++;
        patternceindex --;
    }
    return TRUE;
}

/**
* Rearranges the front accents to try matching
* @param strsrch string search match
* @param start first offset of the accents to start searching
* @param end start of the last accent set
* @param status error status if any
* @return USEARCH_DONE if a match is not found, otherwise return the starting
*         offset of the match. Note this start includes all preceding accents.
*/
UTextOffset doNextCanonicalPrefixMatch(UStringSearch *strsrch, 
                                       UTextOffset    start,
                                       UTextOffset    end,     
                                       UErrorCode    *status)
{
    const UChar       *text       = strsrch->search->text;
          int32_t      textlength = strsrch->search->textLength;
          UTextOffset  tempstart  = start;

    if ((getFCD(text, &tempstart, textlength) & LAST_BYTE_MASK_) == 0) {
        // die... failed at a base character
        return USEARCH_DONE;
    }

    UTextOffset offset = getNextBaseOffset(text, tempstart, textlength);
    start = getPreviousBaseOffset(text, tempstart);

    if (U_SUCCESS(*status)) {
        UChar       accents[INITIAL_ARRAY_SIZE_];
        // normalizing the offensive string
        unorm_normalize(text + start, offset - start, UNORM_NFD, 0, accents, 
                        INITIAL_ARRAY_SIZE_, status);    
        
        UTextOffset  accentsindex[INITIAL_ARRAY_SIZE_];      
        UTextOffset accentsize = getUnblockedAccentIndex(accents, 
                                                         accentsindex);
              UTextOffset         count    = (2 << (accentsize - 1)) - 2;  
              UChar               buffer[INITIAL_ARRAY_SIZE_];
        const UCollator          *collator = strsrch->collator;
              UCollationElements *coleiter = 
                  ucol_openElements(collator, buffer, INITIAL_ARRAY_SIZE_, 
                                    status);
        while (U_SUCCESS(*status) && count > 0) {
            UChar *rearrange = strsrch->canonicalPrefixAccents;
            // copy the base characters
            for (int k = 0; k < accentsindex[0]; k ++) {
                *rearrange ++ = accents[k];
            }
            // forming all possible canonical rearrangement by dropping
            // sets of accents
            for (int i = 0; i <= accentsize - 1; i ++) {
                UTextOffset mask = 1 << (accentsize - i - 1);
                if (count & mask) {
                    for (int j = accentsindex[i]; j < accentsindex[i + 1]; j ++) {
                        *rearrange ++ = accents[j];
                    }
                }
            }
            *rearrange = 0;
            int32_t  matchsize = INITIAL_ARRAY_SIZE_;
            UChar   *match     = addToUCharArray(buffer, &matchsize,
                                           strsrch->canonicalPrefixAccents,
                                           strsrch->search->text + offset,
                                           end - offset,
                                           strsrch->canonicalSuffixAccents,
                                           status);
            
            // run the collator iterator through this match
            ucol_setText(coleiter, match, matchsize, status);
            if (U_SUCCESS(*status)) {
                if (checkCollationMatch(strsrch, coleiter)) {
                    ucol_closeElements(coleiter);
                    if (match != buffer) {
                        uprv_free(match);
                    }
                    return start;
                }
            }
            count --;
        }
        ucol_closeElements(coleiter);
    }
    return USEARCH_DONE;
}

/**
* Gets the offset to the safe point in text before textoffset.
* ie. not the middle of a contraction, swappable characters or supplementary
* characters.
* @param collator collation sata
* @param text string to work with
* @param textoffset offset in string
* @param textlength length of text string
* @return offset to the previous safe character
*/
inline uint32_t getPreviousSafeOffset(const UCollator   *collator, 
                                      const UChar       *text,
                                            UTextOffset  textoffset)
{
    UTextOffset result = textoffset; // first contraction character
    while (result != 0 && ucol_unsafeCP(text[result - 1], collator)) {
        result --;
    }
    if (result != 0) {
        // the first contraction character is consider unsafe here
        result --;
    }
    return result; 
}

/**
* Cleaning up after we passed the safe zone
* @param strsrch string search data
* @param safetext safe text array
* @param safebuffer safe text buffer
* @param coleiter collation element iterator for safe text
*/
inline void cleanUpSafeText(const UStringSearch *strsrch, UChar *safetext,
                                  UChar         *safebuffer, 
                                  UCollationElements *coleiter)
{
    if (safetext != safebuffer && safetext != strsrch->canonicalSuffixAccents) 
    {
       uprv_free(safetext);
    }
    if (coleiter != strsrch->textIter) {
        ucol_closeElements(coleiter);
    }
}

/**
* Take the rearranged end accents and tries matching. If match failed at
* a seperate preceding set of accents (seperated from the rearranged on by
* at least a base character) then we rearrange the preceding accents and 
* tries matching again.
* We allow skipping of the ends of the accent set if the ces do not match. 
* However if the failure is found before the accent set, it fails.
* @param strsrch string search data
* @param textoffset of the start of the rearranged accent
* @param status error status if any
* @return USEARCH_DONE if a match is not found, otherwise return the starting
*         offset of the match. Note this start includes all preceding accents.
*/
UTextOffset doNextCanonicalSuffixMatch(UStringSearch *strsrch, 
                                       UTextOffset    textoffset,
                                       UErrorCode    *status)
{
    const UChar              *text           = strsrch->search->text;
    const UCollator          *collator       = strsrch->collator;
          int32_t             safelength     = 0;
          UChar              *safetext;
          int32_t             safetextlength;
          UChar               safebuffer[INITIAL_ARRAY_SIZE_];
          UCollationElements *coleiter;
          UTextOffset         safeoffset     = textoffset;

    if (textoffset != 0 && ucol_unsafeCP(strsrch->canonicalSuffixAccents[0], 
                                         collator)) {
        safeoffset     = getPreviousSafeOffset(collator, text, textoffset);
        safelength     = textoffset - safeoffset;
        safetextlength = INITIAL_ARRAY_SIZE_;
        safetext       = addToUCharArray(safebuffer, &safetextlength, NULL, 
                                         text + safeoffset, safelength, 
                                         strsrch->canonicalSuffixAccents, 
                                         status);
    }
    else {
        safetextlength = u_strlen(strsrch->canonicalSuffixAccents);
        safetext       = strsrch->canonicalSuffixAccents;
    }

    coleiter = ucol_openElements(collator, safetext, safetextlength, status);
    
    uint32_t *ce        = strsrch->pattern.CE;
    uint32_t  celength  = strsrch->pattern.CELength;
    int       ceindex   = celength - 1;
    UBool     isSafe    = TRUE; // indication flag for position in safe zone
    
    while (ceindex >= 0) {
        uint32_t textce = ucol_previous(coleiter, status);
        if (textce == UCOL_NULLORDER) {
            // check if we have passed the safe buffer
            if (coleiter == strsrch->textIter) {
                cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
                return USEARCH_DONE;
            }
            cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
            safetext = safebuffer;
            coleiter = strsrch->textIter;
            ucol_setOffset(coleiter, safeoffset, status);
            isSafe = FALSE;
            continue;
        }
        textce = getCE(strsrch, textce);
        if (textce != UCOL_IGNORABLE && textce != ce[ceindex]) {
            // do the beginning stuff
            UTextOffset failedoffset = getColElemIterOffset(coleiter, FALSE);
            if (isSafe && failedoffset >= safelength) {
                // alas... no hope. failed at rearranged accent set
                cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
                return USEARCH_DONE;
            }
            else {
                if (isSafe) {
                    failedoffset += safeoffset;
                    cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
                }
                
                // try rearranging the front accents
                UTextOffset result = doNextCanonicalPrefixMatch(strsrch, 
                                        failedoffset, textoffset, status);
                if (result != USEARCH_DONE) {
                    ucol_setOffset(strsrch->textIter, result, status);
                }
                return result;
            }
        }
        if (textce == ce[ceindex]) {
            ceindex --;
        }
    }
    // set offset here
    if (isSafe) {
        UTextOffset result     = getColElemIterOffset(coleiter, FALSE);
        // sets the text iterator here with the correct expansion and offset
        int32_t    leftoverces = getExpansionPrefix(coleiter);
        cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
        if (result >= safelength) { 
            result = textoffset;
        }
        else {
            result += safeoffset;
        }
        ucol_setOffset(strsrch->textIter, result, status);
        strsrch->textIter->iteratordata_.toReturn = 
                       setExpansionPrefix(strsrch->textIter, leftoverces);
        return result;
    }
    
    return ucol_getOffset(coleiter);              
}

/**
* Trying out the substring and sees if it can be a canonical match.
* This will try normalizing the end accents and arranging them into canonical
* equivalents and check their corresponding ces with the pattern ce.
* @param strsrch string search data
* @param textoffset end offset in the collation element text that ends with 
*                   the accents to be rearranged
* @param status error status if any
* @return TRUE if the match is valid, FALSE otherwise
*/
UBool doNextCanonicalMatch(UStringSearch *strsrch, 
                           UTextOffset    textoffset, 
                           UErrorCode    *status)
{
    const UChar       *text = strsrch->search->text;
          UChar32      codepoint;
          UTextOffset  temp = textoffset;
    UTF_PREV_CHAR(text, 0, temp, codepoint);
    if ((getFCD(text, &temp, textoffset) & LAST_BYTE_MASK_) == 0) {
        UCollationElements *coleiter = strsrch->textIter;
        UTextOffset         offset   = getColElemIterOffset(coleiter, FALSE);
        if (strsrch->pattern.hasPrefixAccents) {
            offset = doNextCanonicalPrefixMatch(strsrch, offset, textoffset, 
                                                status);
            if (offset != USEARCH_DONE) {
                ucol_setOffset(coleiter, offset, status);
                return TRUE;
            }
        }
        return FALSE;
    }

    if (!strsrch->pattern.hasSuffixAccents) {
        return FALSE;
    }

    UChar       accents[INITIAL_ARRAY_SIZE_];
    // offset to the last base character in substring to search
    UTextOffset baseoffset = getPreviousBaseOffset(text, textoffset);
    // normalizing the offensive string
    unorm_normalize(text + baseoffset, textoffset - baseoffset, UNORM_NFD, 
                               0, accents, INITIAL_ARRAY_SIZE_, status);    
        
    UTextOffset accentsindex[INITIAL_ARRAY_SIZE_];
    UTextOffset size = getUnblockedAccentIndex(accents, accentsindex);

    // 2 power n - 1 minus the full set of accents
    UTextOffset  count = (2 << (size - 1)) - 2;  
    while (U_SUCCESS(*status) && count > 0) {
        UChar *rearrange = strsrch->canonicalSuffixAccents;
        // copy the base characters
        for (int k = 0; k < accentsindex[0]; k ++) {
            *rearrange ++ = accents[k];
        }
        // forming all possible canonical rearrangement by dropping
        // sets of accents
        for (int i = 0; i <= size - 1; i ++) {
            UTextOffset mask = 1 << (size - i - 1);
            if (count & mask) {
                for (int j = accentsindex[i]; j < accentsindex[i + 1]; j ++) {
                    *rearrange ++ = accents[j];
                }
            }
        }
        *rearrange = 0;
        UTextOffset offset = doNextCanonicalSuffixMatch(strsrch, baseoffset, 
                                                        status);
        if (offset != USEARCH_DONE) {
            return TRUE; // match found
        }
        count --;
    }
    return FALSE;
}

/**
* Gets the previous base character offset depending on the string search 
* pattern data
* @param strsrch string search data
* @param textoffset current offset, current character
* @return the offset of the next character after this base character or itself
*         if it is a composed character with accents
*/
inline UTextOffset getPreviousUStringSearchBaseOffset(UStringSearch *strsrch, 
                                                      UTextOffset textoffset)
{
    if (strsrch->pattern.hasPrefixAccents) {
        const UChar       *text = strsrch->search->text;
              UTextOffset  offset = textoffset;
        if (getFCD(text, &offset, strsrch->search->textLength) >> 
                                                   SECOND_LAST_BYTE_SHIFT_) {
            return getPreviousBaseOffset(text, textoffset);
        }
    }
    textoffset = textoffset < 0 ? 0 : textoffset;
    return textoffset;
}

/**
* Checks and sets the match information if found.
* Checks 
* <ul>
* <li> the potential match does not repeat the previous match
* <li> boundaries are correct
* <li> potential match does not end in the middle of a contraction
* <li> identical matches
* <\ul>
* Otherwise the offset will be shifted to the next character.
* @param strsrch string search data
* @param textoffset offset in the collation element text. the returned value
*        will be the truncated end offset of the match or the new start 
*        search offset.
* @param status error status if any
* @return TRUE if the match is valid, FALSE otherwise
*/
UBool checkNextCanonicalMatch(UStringSearch *strsrch, 
                              UTextOffset   *textoffset, 
                              UErrorCode    *status)
{
    // to ensure that the start and ends are not composite characters
    if (U_FAILURE(*status)) { 
        return FALSE;
    }

    UCollationElements *coleiter = strsrch->textIter;
    if (strsrch->pattern.hasPrefixAccents && 
        strsrch->canonicalPrefixAccents[0] != 0) {
        // forward iteration checks has already been done.
        strsrch->search->matchedIndex  = ucol_getOffset(coleiter);
        strsrch->search->matchedLength = *textoffset - 
                                                strsrch->search->matchedIndex;
        return TRUE;
    }

          UTextOffset  start     = getColElemIterOffset(coleiter, FALSE);
          int32_t      expansion = getExpansionPrefix(coleiter);
          UBool        canonical = strsrch->pattern.hasSuffixAccents &&
                                     strsrch->canonicalSuffixAccents[0] != 0;
    const UChar       *text       = strsrch->search->text;
          int32_t      textlength = strsrch->search->textLength;
          UChar       *str;
          UChar        buffer[INITIAL_ARRAY_SIZE_];
    
    ucol_setOffset(coleiter, start, status);
    if (canonical) {
        UTextOffset lastoffset = getPreviousBaseOffset(text, *textoffset);
        int32_t     suffixsize = u_strlen(strsrch->canonicalSuffixAccents);
        int32_t     size       = lastoffset - start + suffixsize;
                        
        str = addToUCharArray(buffer, &size, NULL, text + start, 
                              lastoffset - start, 
                              strsrch->canonicalSuffixAccents, status);
        coleiter = ucol_openElements(strsrch->collator, str, size, 
                                     status);
    }

    UTextOffset temp       = start;
    UBool       expandflag = expansion != 0;
    while (expansion > 0) {
        // getting rid of the redundant ce
        // since backward contraction/expansion may have extra ces
        ucol_next(coleiter, status);
        if (ucol_getOffset(coleiter) != temp) {
            start = temp;
            temp  = ucol_getOffset(coleiter);
        }
        expansion --;
    }

    uint32_t *patternce      = strsrch->pattern.CE;
    int32_t  patterncelength = strsrch->pattern.CELength;
    int32_t  count           = 0;
    uint32_t  ce; 
    while (count < patterncelength) {
        ce = getCE(strsrch, ucol_next(coleiter, status));
        if (ce == UCOL_IGNORABLE) {
            continue;
        }
        if (expandflag && count == 0 && ucol_getOffset(coleiter) != temp) {
            start = temp;
            temp  = ucol_getOffset(coleiter);
        }

        if (count == 0 && ce != patternce[0]) {
            // accents may have extra starting ces, this occurs when a pure
            // accent pattern is matched without rearrangement
            uint32_t    expected = patternce[0]; 
            if (getFCD(text, &start, textlength) & LAST_BYTE_MASK_) {
                ce = getCE(strsrch, ucol_next(coleiter, status));
                while (ce != expected && ce != UCOL_NULLORDER &&
                       ucol_getOffset(coleiter) <= *textoffset) {
                    ce = getCE(strsrch, ucol_next(coleiter, status));
                }
            }
        }

        if (U_FAILURE(*status) || ce != patternce[count]) {
            *textoffset = getNextBaseOffset(text, (*textoffset) + 1, 
                                            textlength);
            return FALSE;
        }
        count ++;
    }

    // remove ignorable codepoints
    if (!canonical) {
        temp = ucol_getOffset(coleiter); 
    }
    else {
        temp = *textoffset;
    }

    start = getPreviousUStringSearchBaseOffset(strsrch, start);
    // this totally matches, however we need to check if it is repeating
    if (checkRepeatedMatch(strsrch, start, temp) || 
        !isBreakUnit(strsrch, start, temp) || 
        !checkIdentical(strsrch, start, temp)) {
        *textoffset = getNextBaseOffset(text, (*textoffset) + 1, textlength);
        if (strsrch->textIter != coleiter) {
            ucol_closeElements(coleiter);
        }
        return FALSE;
    }
    
    if (strsrch->textIter != coleiter) {
        ucol_closeElements(coleiter);
        uprv_free(str);
    }
    strsrch->search->matchedIndex = start;
    strsrch->search->matchedLength = *textoffset - start;
    return TRUE;
}

/**
* Shifting the collation element iterator position forward to prepare for
* a preceding match. If the first character is a unsafe character, we'll only
* shift by 1 to capture contractions, normalization etc.
* @param text strsrch string search data
* @param textoffset start text position to do search
* @param ce the text ce which failed the match.
* @param patternceindex index of the ce within the pattern ce buffer which
*        failed the match
* @param status error if any
* @return final offset
*/
inline UTextOffset reverseShift(UStringSearch *strsrch,
                                UTextOffset    textoffset,
                                uint32_t       ce,
                                int32_t        patternceindex,
                                UErrorCode    *status)
{         
    if (U_SUCCESS(*status)) {
        if (textoffset != 0 && strsrch->search->isOverlap) {
            textoffset --;
        }
        else {
            int32_t shift = strsrch->pattern.backShift[hash(ce)];
            
            // this is to adjust for characters in the middle of the substring 
            // for matching that failed.
            int32_t adjust = patternceindex;
            if (adjust > 1 && shift > adjust) {
                shift -= adjust - 1;
            }
    
            textoffset -= shift;
        }        
        textoffset = getPreviousUStringSearchBaseOffset(strsrch, textoffset);
        ucol_setOffset(strsrch->textIter, textoffset, status);
    }
    return textoffset;
}

/**
* Checks and sets the match information if found.
* Checks 
* <ul>
* <li> the current match does not repeat the last match
* <li> boundaries are correct
* <li> exact matches has no extra accents
* <li> identical matches
* <\ul>
* Otherwise the offset will be shifted to the preceding character.
* @param strsrch string search data
* @param collator 
* @param coleiter collation element iterator
* @param text string
* @param textoffset offset in the collation element text. the returned value
*        will be the truncated start offset of the match or the new start 
*        search offset.
* @param status error status if any
* @return TRUE if the match is valid, FALSE otherwise
*/
UBool checkPreviousExactMatch(UStringSearch *strsrch, UTextOffset *textoffset, 
                              UErrorCode    *status)
{
    // to ensure that the start and ends are not composite characters
    if (U_SUCCESS(*status)) { 
        const UChar              *text       = strsrch->search->text;
              UCollationElements *coleiter   = strsrch->textIter;            
              int32_t             expansion  = getExpansionSuffix(coleiter);
              UBool               expandflag = expansion > 0;
              UTextOffset         end        = getColElemIterOffset(coleiter, 
                                                                    TRUE);        
              UTextOffset         temp       = end;
        
        ucol_setOffset(coleiter, end, status);

        while (expansion > 0) {
            // getting rid of the redundant ce
            // since forward contraction/expansion may have extra ces
            // if we are in the normalization buffer, hasAccentsBeforeMatch
            // would have taken care of it.
            ucol_previous(coleiter, status);
            if (ucol_getOffset(coleiter) != temp) {
                end = temp;
                temp  = ucol_getOffset(coleiter);
            }
            expansion --;
        }

        uint32_t *patternce       = strsrch->pattern.CE;
        int32_t   patterncelength = strsrch->pattern.CELength;
        int32_t   count = patterncelength;
        uint32_t  ce; 
        while (count > 0) {
            ce = getCE(strsrch, ucol_previous(coleiter, status));
            if (ce == UCOL_IGNORABLE) {
                continue;
            }
            if (expandflag && count == 0 && 
                getColElemIterOffset(coleiter, FALSE) != temp) {
                end = temp;
                temp  = ucol_getOffset(coleiter);
            }
            if (U_FAILURE(*status) || ce != patternce[count - 1]) {
                *textoffset = getPreviousBaseOffset(text, *textoffset - 1);
                return FALSE;
            }
            count --;
        }

        // remove ignorable codepoints
        temp = getColElemIterOffset(coleiter, FALSE); 
        if (temp < *textoffset) {
            temp = *textoffset; 
        }
        
        // this totally matches, however we need to check if it is repeating
        // the old match
        if (checkRepeatedMatch(strsrch, temp, end) || 
            !isBreakUnit(strsrch, temp, end) ||
            hasAccentsBeforeMatch(strsrch, temp, end)
            || hasAccentsAfterMatch(strsrch, temp, end) ||
            !checkIdentical(strsrch, temp, end)) {
            *textoffset = getPreviousBaseOffset(text, *textoffset - 1);
            return FALSE;
        }
        strsrch->search->matchedIndex = *textoffset;
        strsrch->search->matchedLength = end - *textoffset;
        return TRUE;
    }
    return FALSE;
}

/**
* Rearranges the end accents to try matching
* @param strsrch string search match
* @param start offset of the first base character
* @param end start of the last accent set
* @param status error status if any
* @return USEARCH_DONE if a match is not found, otherwise return the ending
*         offset of the match. Note this start includes all following accents.
*/
UTextOffset doPreviousCanonicalSuffixMatch(UStringSearch *strsrch, 
                                           UTextOffset    start,
                                           UTextOffset    end,     
                                           UErrorCode    *status)
{
    const UChar       *text       = strsrch->search->text;
          UChar32      codepoint;
          UTextOffset  tempend    = end;

    UTF_PREV_CHAR(text, 0, tempend, codepoint);
    if ((getFCD(text, &tempend, strsrch->search->textLength) & LAST_BYTE_MASK_) 
                                                                      == 0) {
        // die... failed at a base character
        return USEARCH_DONE;
    }
    end = getNextBaseOffset(text, end, strsrch->search->textLength);

    if (U_SUCCESS(*status)) {
        UChar       accents[INITIAL_ARRAY_SIZE_];
        UTextOffset offset = getPreviousBaseOffset(text, end);
        // normalizing the offensive string
        unorm_normalize(text + offset, end - offset, UNORM_NFD, 0, accents, 
                        INITIAL_ARRAY_SIZE_, status);    
        
        UTextOffset accentsindex[INITIAL_ARRAY_SIZE_];      
        UTextOffset accentsize = getUnblockedAccentIndex(accents, 
                                                         accentsindex);
              UTextOffset         count    = (2 << (accentsize - 1)) - 2;  
              UChar               buffer[INITIAL_ARRAY_SIZE_];
        const UCollator          *collator = strsrch->collator;
              UCollationElements *coleiter = 
                  ucol_openElements(collator, buffer, INITIAL_ARRAY_SIZE_, 
                                    status);
        while (U_SUCCESS(*status) && count > 0) {
            UChar *rearrange = strsrch->canonicalSuffixAccents;
            // copy the base characters
            for (int k = 0; k < accentsindex[0]; k ++) {
                *rearrange ++ = accents[k];
            }
            // forming all possible canonical rearrangement by dropping
            // sets of accents
            for (int i = 0; i <= accentsize - 1; i ++) {
                UTextOffset mask = 1 << (accentsize - i - 1);
                if (count & mask) {
                    for (int j = accentsindex[i]; j < accentsindex[i + 1]; j ++) {
                        *rearrange ++ = accents[j];
                    }
                }
            }
            *rearrange = 0;
            int32_t  matchsize = INITIAL_ARRAY_SIZE_;
            UChar   *match     = addToUCharArray(buffer, &matchsize,
                                           strsrch->canonicalPrefixAccents,
                                           strsrch->search->text + start,
                                           offset - start,
                                           strsrch->canonicalSuffixAccents,
                                           status);
            
            // run the collator iterator through this match
            ucol_setText(coleiter, match, matchsize, status);
            if (U_SUCCESS(*status)) {
                if (checkCollationMatch(strsrch, coleiter)) {
                    ucol_closeElements(coleiter);
                    if (match != buffer) {
                        uprv_free(match);
                    }
                    return end;
                }
            }
            count --;
        }
        ucol_closeElements(coleiter);
    }
    return USEARCH_DONE;
}

/**
* Take the rearranged start accents and tries matching. If match failed at
* a seperate following set of accents (seperated from the rearranged on by
* at least a base character) then we rearrange the preceding accents and 
* tries matching again.
* We allow skipping of the ends of the accent set if the ces do not match. 
* However if the failure is found before the accent set, it fails.
* @param strsrch string search data
* @param textoffset of the ends of the rearranged accent
* @param status error status if any
* @return USEARCH_DONE if a match is not found, otherwise return the ending
*         offset of the match. Note this start includes all following accents.
*/
UTextOffset doPreviousCanonicalPrefixMatch(UStringSearch *strsrch, 
                                           UTextOffset    textoffset,
                                           UErrorCode    *status)
{
    const UChar              *text           = strsrch->search->text;
    const UCollator          *collator       = strsrch->collator;
          int32_t             safelength     = 0;
          UChar              *safetext;
          int32_t             safetextlength;
          UChar               safebuffer[INITIAL_ARRAY_SIZE_];
          UCollationElements *coleiter;
          UTextOffset         safeoffset     = textoffset;

    if (textoffset != 0 && ucol_unsafeCP(strsrch->canonicalPrefixAccents[
                                 u_strlen(strsrch->canonicalPrefixAccents) - 1
                                         ], collator)) {
        safeoffset     = getNextSafeOffset(collator, text, textoffset, 
                                           strsrch->search->textLength);
        safelength     = safeoffset - textoffset;
        safetextlength = INITIAL_ARRAY_SIZE_;
        safetext       = addToUCharArray(safebuffer, &safetextlength, 
                                         strsrch->canonicalPrefixAccents, 
                                         text + textoffset, safelength, 
                                         NULL, status);
    }
    else {
        safetextlength = u_strlen(strsrch->canonicalPrefixAccents);
        safetext       = strsrch->canonicalPrefixAccents;
    }

    coleiter = ucol_openElements(collator, safetext, safetextlength, status);
    
    uint32_t *ce           = strsrch->pattern.CE;
    int32_t   celength     = strsrch->pattern.CELength;
    int       ceindex      = 0;
    UBool     isSafe       = TRUE; // safe zone indication flag for position
    int32_t   prefixlength = u_strlen(strsrch->canonicalPrefixAccents);
    
    while (ceindex < celength) {
        uint32_t textce = ucol_next(coleiter, status);
        if (textce == UCOL_NULLORDER) {
            // check if we have passed the safe buffer
            if (coleiter == strsrch->textIter) {
                cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
                return USEARCH_DONE;
            }
            cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
            safetext = safebuffer;
            coleiter = strsrch->textIter;
            ucol_setOffset(coleiter, safeoffset, status);
            isSafe = FALSE;
            continue;
        }
        textce = getCE(strsrch, textce);
        if (textce != UCOL_IGNORABLE && textce != ce[ceindex]) {
            // do the beginning stuff
            UTextOffset failedoffset = ucol_getOffset(coleiter);
            if (isSafe && failedoffset <= prefixlength) {
                // alas... no hope. failed at rearranged accent set
                cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
                return USEARCH_DONE;
            }
            else {
                if (isSafe) {
                    failedoffset = safeoffset - failedoffset;
                    cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
                }
                
                // try rearranging the end accents
                UTextOffset result = doPreviousCanonicalSuffixMatch(strsrch, 
                                        textoffset, failedoffset, status);
                if (result != USEARCH_DONE) {
                    ucol_setOffset(strsrch->textIter, result, status);
                }
                return result;
            }
        }
        if (textce == ce[ceindex]) {
            ceindex ++;
        }
    }
    // set offset here
    if (isSafe) {
        UTextOffset result      = ucol_getOffset(coleiter);
        // sets the text iterator here with the correct expansion and offset
        int32_t     leftoverces = getExpansionSuffix(coleiter);
        cleanUpSafeText(strsrch, safetext, safebuffer, coleiter);
        if (result <= prefixlength) { 
            result = textoffset;
        }
        else {
            result = textoffset + (safeoffset - result);
        }
        ucol_setOffset(strsrch->textIter, result, status);
        setExpansionSuffix(strsrch->textIter, leftoverces);
        return result;
    }
    
    return ucol_getOffset(coleiter);              
}

/**
* Trying out the substring and sees if it can be a canonical match.
* This will try normalizing the starting accents and arranging them into 
* canonical equivalents and check their corresponding ces with the pattern ce.
* @param strsrch string search data
* @param textoffset start offset in the collation element text that starts 
*                   with the accents to be rearranged
* @param status error status if any
* @return TRUE if the match is valid, FALSE otherwise
*/
UBool doPreviousCanonicalMatch(UStringSearch *strsrch, 
                               UTextOffset    textoffset, 
                               UErrorCode    *status)
{
    const UChar       *text       = strsrch->search->text;
          UTextOffset  temp       = textoffset;
          int32_t      textlength = strsrch->search->textLength;
    if ((getFCD(text, &temp, textlength) >> SECOND_LAST_BYTE_SHIFT_) == 0) {
        UCollationElements *coleiter = strsrch->textIter;
        UTextOffset         offset   = ucol_getOffset(coleiter);
        if (strsrch->pattern.hasSuffixAccents) {
            offset = doPreviousCanonicalSuffixMatch(strsrch, textoffset, 
                                                    offset, status);
            if (offset != USEARCH_DONE) {
                ucol_setOffset(coleiter, offset, status);
                return TRUE;
            }
        }
        return FALSE;
    }

    if (!strsrch->pattern.hasPrefixAccents) {
        return FALSE;
    }

    UChar       accents[INITIAL_ARRAY_SIZE_];
    // offset to the last base character in substring to search
    UTextOffset baseoffset = getNextBaseOffset(text, textoffset, textlength);
    // normalizing the offensive string
    unorm_normalize(text + textoffset, baseoffset - textoffset, UNORM_NFD, 
                               0, accents, INITIAL_ARRAY_SIZE_, status);    
        
    UTextOffset accentsindex[INITIAL_ARRAY_SIZE_];
    UTextOffset size = getUnblockedAccentIndex(accents, accentsindex);

    // 2 power n - 1 minus the full set of accents
    UTextOffset  count = (2 << (size - 1)) - 2;  
    while (U_SUCCESS(*status) && count > 0) {
        UChar *rearrange = strsrch->canonicalPrefixAccents;
        // copy the base characters
        for (int k = 0; k < accentsindex[0]; k ++) {
            *rearrange ++ = accents[k];
        }
        // forming all possible canonical rearrangement by dropping
        // sets of accents
        for (int i = 0; i <= size - 1; i ++) {
            UTextOffset mask = 1 << (size - i - 1);
            if (count & mask) {
                for (int j = accentsindex[i]; j < accentsindex[i + 1]; j ++) {
                    *rearrange ++ = accents[j];
                }
            }
        }
        *rearrange = 0;
        UTextOffset offset = doPreviousCanonicalPrefixMatch(strsrch, 
                                                          baseoffset, status);
        if (offset != USEARCH_DONE) {
            return TRUE; // match found
        }
        count --;
    }
    return FALSE;
}

/**
* Checks and sets the match information if found.
* Checks 
* <ul>
* <li> the potential match does not repeat the previous match
* <li> boundaries are correct
* <li> potential match does not end in the middle of a contraction
* <li> identical matches
* <\ul>
* Otherwise the offset will be shifted to the next character.
* @param strsrch string search data
* @param textoffset offset in the collation element text. the returned value
*        will be the truncated start offset of the match or the new start 
*        search offset.
* @param status error status if any
* @return TRUE if the match is valid, FALSE otherwise
*/
UBool checkPreviousCanonicalMatch(UStringSearch *strsrch, 
                                  UTextOffset   *textoffset, 
                                  UErrorCode    *status)
{
    // to ensure that the start and ends are not composite characters
    if (U_FAILURE(*status)) { 
        return FALSE;
    }

    UCollationElements *coleiter = strsrch->textIter;
    if (strsrch->pattern.hasSuffixAccents && 
        strsrch->canonicalSuffixAccents[0] != 0) {
        // forward iteration checks has already been done.
        strsrch->search->matchedIndex  = *textoffset;
        strsrch->search->matchedLength = getColElemIterOffset(coleiter, FALSE) 
                                        - *textoffset;
        return TRUE;
    }

          UTextOffset  end       = ucol_getOffset(coleiter);
          int32_t      expansion = getExpansionSuffix(coleiter);
          UBool        canonical  = strsrch->pattern.hasPrefixAccents &&
                                     strsrch->canonicalPrefixAccents[0] != 0;
    const UChar       *text       = strsrch->search->text;
          int32_t      textlength = strsrch->search->textLength;
          UChar       *str;
          UChar        buffer[INITIAL_ARRAY_SIZE_];

    ucol_setOffset(coleiter, end, status);
    if (canonical) {
        UTextOffset next = *textoffset;
        UChar32     codepoint;
        UTF_NEXT_CHAR(text, next, textlength, codepoint);
        UTextOffset firstoffset = getNextBaseOffset(text, next, textlength);
        int32_t     prefixsize  = u_strlen(strsrch->canonicalPrefixAccents);
        int32_t     size        = end - firstoffset + prefixsize;
                        
        str = addToUCharArray(buffer, &size, strsrch->canonicalPrefixAccents, 
                              text + firstoffset, end - firstoffset, NULL, 
                              status);
        coleiter = ucol_openElements(strsrch->collator, str, size, 
                                     status);
    }

    UTextOffset temp       = end;
    UBool       expandflag = expansion != 0;
    while (expansion > 0) {
        // getting rid of the redundant ce
        // since forward contraction/expansion may have extra ces
        ucol_previous(coleiter, status);
        if (ucol_getOffset(coleiter) != temp) {
            end = temp;
            temp  = ucol_getOffset(coleiter);
        }
        expansion --;
    }

    uint32_t *patternce       = strsrch->pattern.CE;
    uint32_t  patterncelength = strsrch->pattern.CELength;
    uint32_t  count           = patterncelength;
    uint32_t  ce; 
    while (count > 0) {
        ce = getCE(strsrch, ucol_previous(coleiter, status));
        if (ce == UCOL_IGNORABLE) {
            continue;
        }
        if (expandflag && count == 0 && ucol_getOffset(coleiter) != temp) {
            end  = temp;
            temp = ucol_getOffset(coleiter);
        }
        if (count == patterncelength && ce != patternce[patterncelength - 1]) {
            // accents may have extra starting ces, this occurs when a pure
            // accent pattern is matched without rearrangement
            UChar32     codepoint;
            uint32_t    expected = patternce[patterncelength - 1];
            UTF_PREV_CHAR(text, 0, end, codepoint);
            if (getFCD(text, &end, textlength) & LAST_BYTE_MASK_) {
                ce = getCE(strsrch, ucol_previous(coleiter, status));
                while (ce != expected && ce != UCOL_NULLORDER &&
                       ucol_getOffset(coleiter) <= *textoffset) {
                    ce = getCE(strsrch, ucol_previous(coleiter, status));
                }
            }
        }
        if (U_FAILURE(*status) || ce != patternce[count - 1]) {
            *textoffset = getPreviousBaseOffset(text, (*textoffset) - 1);
            return FALSE;
        }
        count --;
    }

    // remove ignorable codepoints
    if (!canonical) {
        temp = getColElemIterOffset(coleiter, FALSE);
    }
    else {
        temp = *textoffset;
    }

    // remove ignorable codepoints
    if (temp < *textoffset) {
        temp = *textoffset; 
    }
        
    end = getNextUStringSearchBaseOffset(strsrch, end);
    // this totally matches, however we need to check if it is repeating
    if (checkRepeatedMatch(strsrch, temp, end) || 
        !isBreakUnit(strsrch, temp, end) || 
        !checkIdentical(strsrch, temp, end)) {
        *textoffset = getPreviousBaseOffset(text, (*textoffset) - 1);
        if (strsrch->textIter != coleiter) {
            ucol_closeElements(coleiter);
        }
        return FALSE;
    }
    
    if (strsrch->textIter != coleiter) {
        ucol_closeElements(coleiter);
        uprv_free(str);
    }
    strsrch->search->matchedIndex  = *textoffset;
    strsrch->search->matchedLength = end - *textoffset;
    return TRUE;
}

// constructors and destructor -------------------------------------------

U_CAPI UStringSearch * U_EXPORT2 usearch_open(const UChar *pattern, 
                                          int32_t         patternlength, 
                                    const UChar          *text, 
                                          int32_t         textlength,
                                    const char           *locale,
                                          UBreakIterator *breakiter,
                                          UErrorCode     *status) 
{
    if (locale == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    }

    if (U_SUCCESS(*status)) {
        UCollator     *collator = ucol_open(locale, status);
        UStringSearch *result;

        if (U_FAILURE(*status)) {
            return NULL;
        }

        result = usearch_openFromCollator(pattern, patternlength, text,
                                          textlength, collator, breakiter,
                                          status);

        if (result == NULL) {
            ucol_close(collator);
        }
        else {
            result->ownCollator = TRUE;
        }
        return result;
    }
    return NULL;
}

U_CAPI UStringSearch * U_EXPORT2 usearch_openFromCollator(
                                  const UChar *pattern, 
                                        int32_t         patternlength,
                                  const UChar          *text, 
                                        int32_t         textlength,
                                  const UCollator      *collator,
                                        UBreakIterator *breakiter,
                                        UErrorCode     *status) 
{
    initializeFCD(status);

    if (pattern == NULL || text == NULL || collator == NULL ||
        patternlength < -1 || patternlength == 0 || textlength == 0) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    }

    if (U_SUCCESS(*status)) {
        UStringSearch *result;
        
        result = (UStringSearch *)uprv_malloc(sizeof(UStringSearch));
        if (result == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }

        result->collator    = collator;
        result->strength    = ucol_getStrength(collator);
        result->toNormalize = ucol_getAttribute(collator, 
                                                  UCOL_NORMALIZATION_MODE,
                                                  status) == UCOL_ON;
        result->ceMask      = getMask(result->strength);
        result->toShift     =  
             ucol_getAttribute(collator, UCOL_ALTERNATE_HANDLING, status) == 
                                                            UCOL_SHIFTED;
        result->variableTop = ucol_getVariableTop(collator, status);

        result->search             = (USearch *)uprv_malloc(sizeof(USearch));
        result->search->text       = text;
        result->search->textLength = textlength == -1 ? u_strlen(text) : 
                                                                  textlength;

        result->pattern.text = pattern;
        result->pattern.textLength = patternlength == -1 ? u_strlen(pattern) :
                                                           patternlength;
        result->pattern.CE = NULL;
        
        result->search->breakIter     = breakiter;
        if (breakiter != NULL) {
            ubrk_setText(breakiter, text, textlength, status);
        }

        result->ownCollator          = FALSE;
        result->search->matchedLength = 0;
        result->search->matchedIndex  = USEARCH_DONE;
        result->textIter             = ucol_openElements(collator, text, 
                                                         textlength, status);
        result->search->isOverlap          = FALSE;
        result->search->isCanonicalMatch   = FALSE;
        result->search->isForwardSearching = TRUE;
        result->search->reset              = TRUE;
        
        initialize(result, status);

        if (U_FAILURE(*status)) {
            usearch_close(result);
            return NULL;
        }

        return result;
    }
    return NULL;
}

U_CAPI void U_EXPORT2 usearch_close(UStringSearch *strsrch)
{
    if (strsrch != NULL) {
        if (strsrch->pattern.CE != NULL && 
            strsrch->pattern.CE != strsrch->pattern.CEBuffer) {
            uprv_free(strsrch->pattern.CE);
        }
        if (strsrch->textIter != NULL) {
            ucol_closeElements(strsrch->textIter);
        }
        if (strsrch->ownCollator && strsrch->collator != NULL) {
            ucol_close((UCollator *)strsrch->collator);
        }
        uprv_free(strsrch->search);
        uprv_free(strsrch);
    }
}

// set and get methods --------------------------------------------------

U_CAPI void U_EXPORT2 usearch_setOffset(UStringSearch *strsrch, 
                                        UTextOffset    position,
                                        UErrorCode    *status)
{
    if (strsrch != NULL) {
        if (isOutOfBounds(strsrch->search->textLength, position)) {
            *status = U_INDEX_OUTOFBOUNDS_ERROR;
        }
        else {
            ucol_setOffset(strsrch->textIter, position, status);
        }
        strsrch->search->matchedIndex  = USEARCH_DONE;
        strsrch->search->matchedLength = 0;
        strsrch->search->reset         = FALSE; 
    }
}

U_CAPI UTextOffset U_EXPORT2 usearch_getOffset(const UStringSearch *strsrch)
{
    if (strsrch != NULL) {
        UTextOffset result = ucol_getOffset(strsrch->textIter);
        if (isOutOfBounds(strsrch->search->textLength, result)) {
            return USEARCH_DONE;
        }
        return result;
    }
    return USEARCH_DONE;
}
    
U_CAPI void U_EXPORT2 usearch_setAttribute(UStringSearch *strsrch, 
                                 USearchAttribute attribute,
                                 USearchAttributeValue value,
                                 UErrorCode *status)
{
    if (strsrch != NULL) {
        switch (attribute)
        {
        case USEARCH_OVERLAP :
            strsrch->search->isOverlap = (value == USEARCH_ON ? TRUE : FALSE);
            break;
        case USEARCH_CANONICAL_MATCH :
            strsrch->search->isCanonicalMatch = (value == USEARCH_ON ? TRUE : 
                                                                      FALSE);
            break;
        case USEARCH_ATTRIBUTE_COUNT :
        default:
            *status = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }
    if (value == USEARCH_ATTRIBUTE_VALUE_COUNT) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    }
}
    
U_CAPI USearchAttributeValue U_EXPORT2 usearch_getAttribute(
                                                const UStringSearch *strsrch,
                                                USearchAttribute attribute)
{
    if (strsrch != NULL) {
        switch (attribute) {
        case USEARCH_OVERLAP :
            return (strsrch->search->isOverlap == TRUE ? USEARCH_ON : 
                                                        USEARCH_OFF);
        case USEARCH_CANONICAL_MATCH :
            return (strsrch->search->isCanonicalMatch == TRUE ? USEARCH_ON : 
                                                               USEARCH_OFF);
        case USEARCH_ATTRIBUTE_COUNT :
            return USEARCH_DEFAULT;
        }
    }
    return USEARCH_DEFAULT;
}

U_CAPI UTextOffset U_EXPORT2 usearch_getMatchedStart(
                                                const UStringSearch *strsrch)
{
    if (strsrch == NULL) {
        return USEARCH_DONE;
    }
    return strsrch->search->matchedIndex;
}


U_CAPI int32_t U_EXPORT2 usearch_getMatchedText(const UStringSearch *strsrch, 
                                            UChar         *result, 
                                            int32_t        resultCapacity, 
                                            UErrorCode    *status)
{
    if (strsrch == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return USEARCH_DONE;
    }

    if (result == NULL || resultCapacity == 0) {
        *status = U_BUFFER_OVERFLOW_ERROR;
    }
    else {
        int32_t     copylength = strsrch->search->matchedLength;
        UTextOffset copyindex  = strsrch->search->matchedIndex;
        if (copyindex == USEARCH_DONE) {
            result[0] = 0;
            return USEARCH_DONE;
        }

        if (resultCapacity < copylength) {
            copylength = resultCapacity;
            *status = U_BUFFER_OVERFLOW_ERROR;
        }
        else if (resultCapacity == copylength) {
            // *status = U_STRING_NOT_NULL_TERMINATED;
        }
        else if (resultCapacity > copylength) {
            result[copylength] = 0;
        }
      
        if (copylength > 0) {
            uprv_memcpy(result, strsrch->search->text + copyindex, 
                        copylength * sizeof(UChar));
        }
    }
    return strsrch->search->matchedLength;
}
    
U_CAPI int32_t U_EXPORT2 usearch_getMatchedLength(
                                              const UStringSearch *strsrch)
{
    if (strsrch != NULL) {
        return strsrch->search->matchedLength;
    }
    return USEARCH_DONE;
}

U_CAPI void U_EXPORT2 usearch_setBreakIterator(UStringSearch  *strsrch, 
                                               UBreakIterator *breakiter,
                                               UErrorCode     *status)
{
    if (strsrch != NULL) {
        strsrch->search->breakIter = breakiter;
        if (breakiter != NULL) {
            ubrk_setText(breakiter, strsrch->search->text, 
                         strsrch->search->textLength, status);
        }
    }
}
    
U_CAPI const U_EXPORT2 UBreakIterator * usearch_getBreakIterator(
                                              const UStringSearch *strsrch)
{
    if (strsrch != NULL) {
        return strsrch->search->breakIter;
    }
    return NULL;
}
    
U_CAPI void U_EXPORT2 usearch_setText(      UStringSearch *strsrch, 
                                      const UChar         *text,
                                            int32_t        textlength,
                                            UErrorCode    *status)
{
    if (strsrch == NULL || text == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    else {
        strsrch->search->text       = text;
        strsrch->search->textLength = textlength == -1 ? u_strlen(text) : 
                                                                  textlength;
        ucol_closeElements(strsrch->textIter);
        strsrch->textIter   = ucol_openElements(strsrch->collator, text, 
                                                textlength, status);
        strsrch->search->matchedIndex  = USEARCH_DONE;
        strsrch->search->matchedLength = 0;
        strsrch->search->reset         = TRUE;
    }
}

U_CAPI const UChar * U_EXPORT2 usearch_getText(const UStringSearch *strsrch, 
                                                     int32_t       *length)
{
    if (strsrch != NULL) {
        *length = strsrch->search->textLength;
        return strsrch->search->text;
    }
    return NULL;
}

U_CAPI void U_EXPORT2 usearch_setCollator(      UStringSearch *strsrch, 
                                          const UCollator     *collator,
                                                UErrorCode    *status)
{
    if (collator == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    if (strsrch != NULL && U_SUCCESS(*status)) {
        if (strsrch->ownCollator) {
            ucol_close((UCollator *)strsrch->collator);
        }
        strsrch->collator    = collator;
        strsrch->ownCollator = FALSE;
        strsrch->strength    = ucol_getStrength(collator);
        strsrch->toNormalize = ucol_getAttribute(collator, 
                                                  UCOL_NORMALIZATION_MODE,
                                                  status) == UCOL_ON;
        strsrch->ceMask      = getMask(strsrch->strength);
        strsrch->toShift     =  
             ucol_getAttribute(collator, UCOL_ALTERNATE_HANDLING, status) == 
                                                            UCOL_SHIFTED;
        strsrch->variableTop = ucol_getVariableTop(collator, status);
        initialize(strsrch, status);
        ucol_closeElements(strsrch->textIter);
        strsrch->textIter    = ucol_openElements(collator, 
                                                 strsrch->search->text, 
                                                 strsrch->search->textLength, 
                                                 status);
    }
}

U_CAPI UCollator * U_EXPORT2 usearch_getCollator(const UStringSearch *strsrch)
{
    if (strsrch != NULL) {
        return (UCollator *)strsrch->collator;
    }
    return NULL;
}

U_CAPI void U_EXPORT2 usearch_setPattern(      UStringSearch *strsrch, 
                                         const UChar         *pattern,
                                               int32_t        patternlength,
                                               UErrorCode    *status)
{
    if (pattern == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    if (U_SUCCESS(*status)) {
        strsrch->pattern.text       = pattern;
        strsrch->pattern.textLength = (patternlength == -1 ? 
                                       u_strlen(pattern) : patternlength);
        initialize(strsrch, status);
    }
}

U_CAPI const U_EXPORT2 UChar * usearch_getPattern(
                                                const UStringSearch *strsrch, 
                                                      int32_t       *length)
{
    if (strsrch != NULL) {
        *length = strsrch->pattern.textLength;
        return strsrch->pattern.text;
    }
    return NULL;
}

// miscellanous methods --------------------------------------------------

U_CAPI UTextOffset U_EXPORT2 usearch_first(UStringSearch *strsrch, 
                                           UErrorCode    *status) 
{
    if (strsrch != NULL && U_SUCCESS(*status)) {
        strsrch->search->isForwardSearching = TRUE;
        usearch_setOffset(strsrch, 0, status);
        return usearch_next(strsrch, status);
    }
    return USEARCH_DONE;
}

U_CAPI UTextOffset U_EXPORT2 usearch_following(UStringSearch *strsrch, 
                                               UTextOffset    position,
                                               UErrorCode    *status)
{
    if (strsrch != NULL && U_SUCCESS(*status)) {
        strsrch->search->isForwardSearching = TRUE;
        usearch_setOffset(strsrch, position, status);
        if (U_SUCCESS(*status)) {
            return usearch_next(strsrch, status);   
        }
    }
    return USEARCH_DONE;
}
    
U_CAPI UTextOffset U_EXPORT2 usearch_last(UStringSearch *strsrch, 
                                          UErrorCode    *status)
{
    if (strsrch != NULL && U_SUCCESS(*status)) {
        strsrch->search->isForwardSearching = FALSE;
        usearch_setOffset(strsrch, strsrch->search->textLength, status);
        return usearch_previous(strsrch, status);
    }
    return USEARCH_DONE;
}

U_CAPI UTextOffset U_EXPORT2 usearch_preceding(UStringSearch *strsrch, 
                                               UTextOffset    position,
                                               UErrorCode    *status)
{
    if (strsrch != NULL && U_SUCCESS(*status)) {
        strsrch->search->isForwardSearching = FALSE;
        usearch_setOffset(strsrch, position, status);
        if (U_SUCCESS(*status)) {
            return usearch_previous(strsrch, status);   
        }
    }
    return USEARCH_DONE;
}
    
/**
* If a direction switch is required, we'll count the number of ces till the 
* beginning of the collation element iterator and iterate forwards that 
* number of times. This is so that we get to the correct point within the 
* string to continue the search in. Imagine when we are in the middle of the
* normalization buffer when the change in direction is request. arrrgghh....
* After searching the offset within the collation element iterator will be
* shifted to the start of the match. If a match is not found, the offset would
* have been set to the end of the text string in the collation element 
* iterator.
* Okay, here's my take on normalization buffer. The only time when there can
* be 2 matches within the same normalization is when the pattern is consists
* of all accents. But since the offset returned is from the text string, we
* should not confuse the caller by returning the second match within the 
* same normalization buffer. If we do, the 2 results will have the same match
* offsets, and that'll be confusing. I'll return the next match that doesn't
* fall within the same normalization buffer. Note this does not affect the 
* results of matches spanning the text and the normalization buffer.
* The position to start searching is taken from the collation element
* iterator. Callers of this API would have to set the offset in the collation
* element iterator before using this method.
*/
U_CAPI UTextOffset U_EXPORT2 usearch_next(UStringSearch *strsrch,
                                          UErrorCode    *status)
{ 
    if (U_SUCCESS(*status) && strsrch != NULL) {
        UTextOffset offset    = usearch_getOffset(strsrch);
        strsrch->search->reset = FALSE;
        if (strsrch->search->isForwardSearching) {
            int32_t     textlength = strsrch->search->textLength;
            UTextOffset matchindex = strsrch->search->matchedIndex;
            if (offset == textlength || 
                (!strsrch->search->isOverlap &&
                offset + strsrch->pattern.defaultShiftSize > textlength) ||
                matchindex == textlength || 
                (!strsrch->search->isOverlap && matchindex != USEARCH_DONE && 
                matchindex + strsrch->search->matchedLength >= textlength)) {
                // not enough characters to match
                setMatchNotFound(strsrch, status);
                return USEARCH_DONE; 
            }
        }
        else {
            // switching direction. 
            // if matchedIndex == USEARCH_DONE, it means that either a 
            // setOffset has been called or that previous ran off the text
            // string. the iterator would have been set to offset 0 if a 
            // match is not found.
            strsrch->search->isForwardSearching = TRUE;
            if (strsrch->search->matchedIndex != USEARCH_DONE) {
                // there's no need to set the collation element iterator
                // the next call to next will set the offset.
                return strsrch->search->matchedIndex;
            }
        }

        if (U_SUCCESS(*status)) {
            if (strsrch->pattern.CELength == 0) {
                strsrch->search->matchedIndex = strsrch->search->matchedIndex == 
                                               USEARCH_DONE ? offset : 
                                             strsrch->search->matchedIndex + 1;
                strsrch->search->matchedLength = 0;
                // skip supplementary character
                ucol_setOffset(strsrch->textIter, 
                               strsrch->search->matchedIndex, status);
                if (strsrch->search->matchedIndex == 
                                                strsrch->search->textLength) {
                    strsrch->search->matchedIndex = USEARCH_DONE;
                }
            }
            else {
                if (strsrch->search->isCanonicalMatch) {
                    // can't use exact here since extra accents are allowed.
                    usearch_handleNextCanonical(strsrch, status);
                }
                else {
                    usearch_handleNextExact(strsrch, status);
                }
            }

            if (U_FAILURE(*status)) {
                return USEARCH_DONE;
            }
            
            return strsrch->search->matchedIndex;
        }
    }
    return USEARCH_DONE;
}

U_CAPI UTextOffset U_EXPORT2 usearch_previous(UStringSearch *strsrch,
                                              UErrorCode *status)
{
    if (U_SUCCESS(*status) && strsrch != NULL) {
        UTextOffset offset;
        if (strsrch->search->reset) {
            offset                             = strsrch->search->textLength;
            strsrch->search->isForwardSearching = FALSE;
            strsrch->search->reset              = FALSE;
        }
        else {
            offset = usearch_getOffset(strsrch);
        }
        
        if (strsrch->search->isForwardSearching == TRUE) {
            // switching direction. 
            // if matchedIndex == USEARCH_DONE, it means that either a 
            // setOffset has been called or that next ran off the text
            // string. the iterator would have been set to offset textLength if 
            // a match is not found.
            strsrch->search->isForwardSearching = FALSE;
            if (strsrch->search->matchedIndex != USEARCH_DONE) {
                return strsrch->search->matchedIndex;
            }
        }
        else {
            UTextOffset matchindex = strsrch->search->matchedIndex;
            if (offset == 0 || (!strsrch->search->isOverlap && 
                offset < strsrch->pattern.defaultShiftSize) ||
                matchindex == 0 ||
                (!strsrch->search->isOverlap && matchindex != USEARCH_DONE && 
                matchindex < strsrch->pattern.defaultShiftSize)) {
                // not enough characters to match
                setMatchNotFound(strsrch, status);
                return USEARCH_DONE; 
            }
        }

        if (U_SUCCESS(*status)) {
            if (strsrch->pattern.CELength == 0) {
                strsrch->search->matchedIndex = 
                      (strsrch->search->matchedIndex == USEARCH_DONE ? offset : 
                      strsrch->search->matchedIndex);
                if (strsrch->search->matchedIndex == 0) {
                    setMatchNotFound(strsrch, status);
                }
                else {
                    strsrch->search->matchedIndex --;
                    ucol_setOffset(strsrch->textIter, 
                                   strsrch->search->matchedIndex, status);
                    strsrch->search->matchedLength = 0;
                }
            }
            else {
                if (strsrch->search->isCanonicalMatch) {
                    // can't use exact here since extra accents are allowed.
                    usearch_handlePreviousCanonical(strsrch, status);
                }
                else {
                    usearch_handlePreviousExact(strsrch, status);
                }
            }

            if (U_FAILURE(*status)) {
                return USEARCH_DONE;
            }
            
            return strsrch->search->matchedIndex;
        }
    }
    return USEARCH_DONE;
}


    
U_CAPI void U_EXPORT2 usearch_reset(UStringSearch *strsrch)
{
    if (strsrch != NULL) {
        strsrch->search->matchedLength      = 0;
        strsrch->search->matchedIndex       = USEARCH_DONE;
        strsrch->search->isOverlap          = FALSE;
        strsrch->search->isCanonicalMatch   = FALSE;
        strsrch->search->isForwardSearching = TRUE;
        ucol_reset(strsrch->textIter);
        strsrch->search->reset              = TRUE;
    }
}

// internal use methods declared in usrchimp.h -----------------------------

UBool usearch_handleNextExact(UStringSearch *strsrch, UErrorCode *status)
{
    UCollationElements *coleiter        = strsrch->textIter;
    int32_t             textlength      = strsrch->search->textLength;
    uint32_t           *patternce       = strsrch->pattern.CE;
    int32_t             patterncelength = strsrch->pattern.CELength;
    UTextOffset         textoffset      = ucol_getOffset(coleiter);

    // shifting it check for setting offset
    // if setOffset is called previously or there was no previous match, we
    // leave the offset as it is.
    if (strsrch->search->matchedIndex != USEARCH_DONE) {
        textoffset = strsrch->search->matchedIndex + 
                     strsrch->search->matchedLength;
    }
    
    textoffset = shiftForward(strsrch, textoffset, UCOL_NULLORDER, 
                              patterncelength, status);
    
    while (U_SUCCESS(*status))
    {
        uint32_t    patternceindex = patterncelength - 1;
        uint32_t    targetce;
        UBool       end            = (textoffset == textlength);
        UBool       found          = FALSE;
        UBool       last           = TRUE;
        uint32_t    lastce         = UCOL_NULLORDER;
        UTextOffset passboundary   = 0;
        
        while (TRUE) {
            // trying to find the last pattern ce
            UBool passed;

            targetce = ucol_previous(coleiter, status);
            if (last || lastce == UCOL_IGNORABLE) {
                lastce = getCE(strsrch, targetce);
                passboundary = getColElemIterOffset(coleiter, FALSE);
            }
            passed = (ucol_getOffset(coleiter) != passboundary);
            // check after getCE at the end
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
            }
            targetce = getCE(strsrch, targetce);
            if (targetce == UCOL_IGNORABLE) {
                continue;
            }
            if (targetce == patternce[patternceindex]) {
                // the first ce can be a contraction
                found = last || !passed;
                break;
            }
            else if (passed) {
                found = FALSE;
                break;
            }
            
            last = FALSE;
        }

        targetce = lastce;
        
        while (found && patternceindex > 0 && U_SUCCESS(*status)) {
            targetce    = ucol_previous(coleiter, status);
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
             }
            targetce    = getCE(strsrch, targetce);
            if (targetce == UCOL_IGNORABLE) {
                continue;
            }

            patternceindex --;
            found = found && targetce == patternce[patternceindex]; 
        }

        if (!found) {
            if (end) {
                setMatchNotFound(strsrch, status);
                break;
            }
            textoffset = shiftForward(strsrch, textoffset, targetce, 
                                      patternceindex, status);
            patternceindex = patterncelength;
            continue;
        }
        
        if (checkNextExactMatch(strsrch, &textoffset, status)) {
            ucol_setOffset(coleiter, textoffset, status);
            return TRUE;
        }
        if (end) {
            setMatchNotFound(strsrch, status);
            break;
        }
        ucol_setOffset(coleiter, textoffset, status);
    }
    ucol_setOffset(coleiter, textoffset, status);
    return FALSE;
}

UBool usearch_handleNextCanonical(UStringSearch *strsrch, UErrorCode *status)
{
    UCollationElements *coleiter        = strsrch->textIter;
    int32_t             textlength      = strsrch->search->textLength;
    uint32_t           *patternce       = strsrch->pattern.CE;
    int32_t             patterncelength = strsrch->pattern.CELength;
    UTextOffset         textoffset      = ucol_getOffset(coleiter);
    UBool               hasPatternAccents = 
       strsrch->pattern.hasSuffixAccents || strsrch->pattern.hasPrefixAccents;
          
    // shifting it check for setting offset
    // if setOffset is called previously or there was no previous match, we
    // leave the offset as it is.
    if (strsrch->search->matchedIndex != USEARCH_DONE) {
        textoffset = strsrch->search->matchedIndex + 
                     strsrch->search->matchedLength;
    }
    
    textoffset = shiftForward(strsrch, textoffset, UCOL_NULLORDER, 
                              patterncelength, status);
    strsrch->canonicalPrefixAccents[0] = 0;
    strsrch->canonicalSuffixAccents[0] = 0;
    
    while (U_SUCCESS(*status))
    {
        int32_t     patternceindex = patterncelength - 1;
        uint32_t    targetce;
        UBool       end            = (textoffset == textlength);
        UBool       found          = FALSE;
        UBool       last           = TRUE;
        uint32_t    lastce         = UCOL_NULLORDER;
        UTextOffset previousoffset = 0;
        
        while (TRUE) {
            // trying to find the last pattern ce
            UBool passed;

            targetce = ucol_previous(coleiter, status);
            if (last || lastce == UCOL_IGNORABLE) {
                lastce = getCE(strsrch, targetce);
                previousoffset = getColElemIterOffset(coleiter, FALSE);
            }
            passed = (ucol_getOffset(coleiter) != previousoffset);
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
            }
            targetce = getCE(strsrch, targetce);
            
            if (targetce == patternce[patternceindex]) {
                // the first ce can be a contraction
                found = last || !passed;
                break;
            }
            else if (passed) {
                // checking for accents in composite character
                found = FALSE;
                break;
            }
            
            last = FALSE;
        }
        
        if (!found) {
            // above loop sometimes runs twice
            ucol_setOffset(coleiter, previousoffset, status);
        }
        targetce = lastce;
        
        while (found && patternceindex > 0 && U_SUCCESS(*status)) {
            targetce    = ucol_previous(coleiter, status);
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
            }
            targetce    = getCE(strsrch, targetce);
            if (targetce == UCOL_IGNORABLE) {
                continue;
            }

            patternceindex --;
            found = found && targetce == patternce[patternceindex]; 
        }

        // initializing the rearranged accent array
        if (hasPatternAccents && !found) {
            strsrch->canonicalPrefixAccents[0] = 0;
            strsrch->canonicalSuffixAccents[0] = 0;
            found = doNextCanonicalMatch(strsrch, textoffset, status);
        }

        if (!found) {
            if (end) {
                setMatchNotFound(strsrch, status);
                return FALSE;
            }
            textoffset = shiftForward(strsrch, textoffset, targetce, 
                                      patternceindex, status);
            patternceindex = patterncelength;
            continue;
        }
        
        if (checkNextCanonicalMatch(strsrch, &textoffset, status)) {
            return TRUE;
        }
        if (end) {
            setMatchNotFound(strsrch, status);
            return FALSE;
        }
        ucol_setOffset(coleiter, textoffset, status);
    }
    return FALSE;
}

UBool usearch_handlePreviousExact(UStringSearch *strsrch, UErrorCode *status)
{
    UCollationElements *coleiter        = strsrch->textIter;
    uint32_t           *patternce       = strsrch->pattern.CE;
    int32_t             patterncelength = strsrch->pattern.CELength;
    UTextOffset         textoffset      = ucol_getOffset(coleiter);

    // shifting it check for setting offset
    // if setOffset is called previously or there was no previous match, we
    // leave the offset as it is.
    if (strsrch->search->matchedIndex != USEARCH_DONE) {
        textoffset = strsrch->search->matchedIndex;
    }
    
    textoffset = reverseShift(strsrch, textoffset, UCOL_NULLORDER, 
                              patterncelength, status);
    
    while (U_SUCCESS(*status))
    {
        int32_t    patternceindex = 1;
        uint32_t    targetce;
        UBool       found          = FALSE;
        UBool       first          = TRUE;
        uint32_t    firstce        = UCOL_NULLORDER;
        UBool       start          = (textoffset == 0);
        UTextOffset passboundary   = 0; 
        
        while (TRUE) {
            // trying to find the last pattern ce
            UBool passed;

            targetce = ucol_next(coleiter, status);
            if (first || firstce == UCOL_IGNORABLE) {
                firstce      = getCE(strsrch, targetce);
                passboundary = ucol_getOffset(coleiter);
            }
            passed = (ucol_getOffset(coleiter) != passboundary);
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
            }
            targetce = getCE(strsrch, targetce);
            if (targetce == UCOL_IGNORABLE) {
                continue;
            }         
            if (targetce == patternce[0]) {
                // the first ce can be a contraction
                found = first || !passed;
                break;
            }
            else if (passed) {
                // checking for accents in composite character
                found = FALSE;
                break;
            }
            
            first = FALSE;
        }

        targetce = firstce;
        
        while (found && (patternceindex < patterncelength) && 
               U_SUCCESS(*status)) {
            targetce    = ucol_next(coleiter, status);
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
            }
            targetce    = getCE(strsrch, targetce);
            if (targetce == UCOL_IGNORABLE) {
                continue;
            }

            found = found && targetce == patternce[patternceindex]; 
            patternceindex ++;
        }

        if (!found) {
            if (start) {
                setMatchNotFound(strsrch, status);
                break;
            }
            textoffset = reverseShift(strsrch, textoffset, targetce, 
                                      patternceindex, status);
            patternceindex = 0;
            continue;
        }
        
        if (checkPreviousExactMatch(strsrch, &textoffset, status)) {
            ucol_setOffset(coleiter, textoffset, status);
            return TRUE;
        }
        if (start) {
            setMatchNotFound(strsrch, status);
            break;
        }
        ucol_setOffset(coleiter, textoffset, status);
    }
    ucol_setOffset(coleiter, textoffset, status);
    return FALSE;
}

UBool usearch_handlePreviousCanonical(UStringSearch *strsrch, 
                                      UErrorCode    *status)
{
    UCollationElements *coleiter        = strsrch->textIter;
    uint32_t           *patternce       = strsrch->pattern.CE;
    int32_t             patterncelength = strsrch->pattern.CELength;
    UTextOffset         textoffset      = ucol_getOffset(coleiter);
    UBool               hasPatternAccents = 
       strsrch->pattern.hasSuffixAccents || strsrch->pattern.hasPrefixAccents;
          
    // shifting it check for setting offset
    // if setOffset is called previously or there was no previous match, we
    // leave the offset as it is.
    if (strsrch->search->matchedIndex != USEARCH_DONE) {
        textoffset = strsrch->search->matchedIndex;
    }
    
    textoffset = reverseShift(strsrch, textoffset, UCOL_NULLORDER, 
                              patterncelength, status);
    strsrch->canonicalPrefixAccents[0] = 0;
    strsrch->canonicalSuffixAccents[0] = 0;
    
    while (U_SUCCESS(*status))
    {
        int32_t     patternceindex = 1;
        uint32_t    targetce;
        UBool       found          = FALSE;
        UBool       first          = TRUE;
        uint32_t    firstce        = UCOL_NULLORDER;
        UTextOffset nextoffset     = 0;
        UBool       start          = (textoffset == 0);
        
        while (TRUE) {
            // trying to find the last pattern ce
            UBool passed;

            targetce = ucol_next(coleiter, status);
            if (first || firstce == UCOL_IGNORABLE) {
                firstce    = getCE(strsrch, targetce);
                nextoffset = ucol_getOffset(coleiter);
            }
            passed = (ucol_getOffset(coleiter) != nextoffset);
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
            }
            targetce = getCE(strsrch, targetce);
            
            if (targetce == patternce[0]) {
                // the first ce can be a contraction
                found = first || !passed;
                break;
            }
            else if (passed) {
                // checking for accents in composite character
                found = FALSE;
                break;
            }
            
            first = FALSE;
        }

        if (!found) {
            // the above loop sometimes loop twice
            ucol_setOffset(coleiter, nextoffset, status);
        }

        targetce = firstce;
        
        while (found && patternceindex < patterncelength && 
               U_SUCCESS(*status)) {
            targetce    = ucol_next(coleiter, status);
            if (targetce == UCOL_NULLORDER) {
                found = FALSE;
                break;
            }
            targetce = getCE(strsrch, targetce);
            if (targetce == UCOL_IGNORABLE) {
                continue;
            }

            found = found && targetce == patternce[patternceindex]; 
            patternceindex ++;
        }

        // initializing the rearranged accent array
        if (hasPatternAccents && !found) {
            strsrch->canonicalPrefixAccents[0] = 0;
            strsrch->canonicalSuffixAccents[0] = 0;
            found = doPreviousCanonicalMatch(strsrch, textoffset, status);
        }

        if (!found) {
            if (start) {
                setMatchNotFound(strsrch, status);
                return FALSE;
            }
            textoffset = reverseShift(strsrch, textoffset, targetce, 
                                      patternceindex, status);
            patternceindex = 0;
            continue;
        }

        if (checkPreviousCanonicalMatch(strsrch, &textoffset, status)) {
            return TRUE;
        }

        if (start) {
            setMatchNotFound(strsrch, status);
            return FALSE;
        }
        ucol_setOffset(coleiter, textoffset, status);
    }
    return FALSE;
}

