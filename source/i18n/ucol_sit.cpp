/*
*******************************************************************************
*   Copyright (C) 2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ucol_sit.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* Modification history
* Date        Name      Comments
* 03/12/2004  weiv      Creation
*/

#include "utracimp.h"
#include "ucol_imp.h"
#include "unormimp.h"


/*
struct FromShortString {
    UVersion UCAVersion;
    char locale[256];
    UChar variableTop[256];

};
*/

enum ActionType {
    UCOL_SIT_GET_ATTRIBUTE_VALUE = 0,
    UCOL_SIT_GET_VARIABLE_TOP,
    UCOL_SIT_GET_UCA_VERSION,
    UCOL_SIT_GET_LOCALE_ELEMENT,
    UCOL_SIT_GET_RFC3166BIS_LOCALE
};

struct ShortStringOptions {
    char optionStart;
    ActionType action;
    int32_t attr;
    char chars[7];
};

static ShortStringOptions options[] = {
    { 'L', UCOL_SIT_GET_LOCALE_ELEMENT, 0, "" }, // language
    { 'Z', UCOL_SIT_GET_LOCALE_ELEMENT, 0, "" }, // script
    { 'R', UCOL_SIT_GET_LOCALE_ELEMENT, 0, "" }, // region
    { 'V', UCOL_SIT_GET_LOCALE_ELEMENT, 0, "" }, // variant
    { 'K', UCOL_SIT_GET_LOCALE_ELEMENT, 0, "" }, // keyword
    { 'X', UCOL_SIT_GET_RFC3166BIS_LOCALE, 0, "" }, // rfc3166bis locale name
    { 'S', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_STRENGTH,  "1234ID" }, // strength   1, 2, 3, 4, I, D
    { 'E', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_CASE_LEVEL,  "OXD"  }, // case level O, X, D
    { 'C', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_CASE_FIRST,  "LUXD" }, // case first L, U, X, D
    { 'D', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_NUMERIC_COLLATION,  "OXD" }, // codan      O, X, D
    { 'A', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_ALTERNATE_HANDLING,  "NSD" }, // alternate  N, S, D 
    { 'N', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_NORMALIZATION_MODE,  "OXD" }, // norm       O, X, D
    { 'F', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_FRENCH_COLLATION,  "OXD" }, // french     O, X, D
    { 'H', UCOL_SIT_GET_ATTRIBUTE_VALUE, UCOL_HIRAGANA_QUATERNARY_MODE,  "OXD" }, // hiragana   O, X, D
    { 'T', UCOL_SIT_GET_VARIABLE_TOP, 0, "" },
    { 'U', UCOL_SIT_GET_UCA_VERSION, 0, "" }
};


/** 
 * Open a collator defined by a short form string.
 * The structure and the syntax of the string is defined in the "Naming collators"
 * section of the users guide: 
 * http://oss.software.ibm.com/icu/userguide/Collate_Concepts.html#Naming_Collators
 * The call to this function is equivalent to a call to ucol_open, followed by a 
 * series of calls to ucol_setAttribute and ucol_setVariableTop.
 * @param definition A short string containing a locale and a set of attributes. 
 *                   Attributes not explicitly mentioned are left at the default
 *                   state for a locale.
 * @param parseError if not NULL, structure that will get filled with error's pre
 *                   and post context in case of error.
 * @param status     Error code. Apart from regular error conditions connected to 
 *                   instantiating collators (like out of memory or similar), this
 *                   API will return an error if an invalid attribute or attribute/value
 *                   combination is specified.
 * @return           A pointer to a UCollator or 0 if an error occured (including an 
 *                   invalid attribute).
 * @see ucol_open
 * @see ucol_setAttribute
 * @see ucol_setVariableTop
 * @draft ICU 3.0
 *
 */
U_CAPI UCollator* U_EXPORT2
ucol_openFromShortString( const char *definition,
                          UParseError *parseError,
                          UErrorCode *status)
{
    UTRACE_ENTRY_OC(UTRACE_UCOL_OPEN_FROM_SHORT_STRING);
    UTRACE_DATA1(UTRACE_INFO, "short string = \"%s\"", definition);

    if(U_FAILURE(*status)) return 0;

    char loc[256];
    
    // first we want to pick stuff out of short string.
    // we'll end up with an UCA version, locale and a bunch of
    // settings

    // analyse the string in order to get everything we need.

    const UCollator* UCA = ucol_initUCA(status);

    UCollator *result = ucol_open(loc, status);
    
    UTRACE_EXIT_PTR_STATUS(result, *status);
    return result;
}

U_CDECL_BEGIN
static UBool U_CALLCONV
_processContractions(const void *context, UChar32 start, UChar32 limit, uint32_t value) {
    UErrorCode status = U_ZERO_ERROR;
    USet *unsafe = (USet *)context;
    UChar contraction[256];
    if(value > UCOL_NOT_FOUND && getCETag(value) == CONTRACTION_TAG) { 
        // this is a contraction
        // we want to add the code point for sure
        while(start < limit) {
            //uset_add(unsafe, start);
            contraction[0] = (UChar)start;
            // get the rest of the contraction string from the data structure
            start++;
        }
        // check if there is anything else to add - if these lead 
        // to a longer contraction
    }
    if(U_FAILURE(status)) {
    return FALSE;
    } else {
    return TRUE;
    }
}
U_CDECL_END

static int32_t U_CALLCONV
_getTrieFoldingOffset(uint32_t data) {
    return (int32_t)(data&0xFFFFFF);
}

U_CAPI int32_t U_EXPORT2
ucol_getUnsafeSet( const UCollator *coll,
                  USet *unsafe,
                  UErrorCode *status)
{
    uset_clear(unsafe);
    // add Thai/Lao prevowels
    uset_addRange(unsafe, 0xe40, 0xe44);
    uset_addRange(unsafe, 0xec0, 0xec4);
    // add lead/trail surrogates
    uset_addRange(unsafe, 0xd800, 0xdfff);


    // add FCD things
    const uint16_t *fcdTrieIndex=unorm_getFCDTrie(status);
    int32_t i = 0;

    // add unsafe BMPs
    uint16_t fcd, leadFCD;
    UChar32 c;
    for(c = 0; c < 0xffff; c++) {
        if(c==0xd800) {
            c=0xe000;
        }
        fcd = unorm_getFCD16(fcdTrieIndex, (UChar)c);
        if (fcd != 0) {
            uset_add(unsafe, c);
        }
    }

    // add unsafe supplementaries
    for(c = 0x10000; c < 0x110000; ) {
        leadFCD=unorm_getFCD16(fcdTrieIndex, U16_LEAD(c));
        if(leadFCD==0) {
            c+=0x400;
        } else {
            for(i=0; i<0x400; ++c, ++i) {
                // either i or U16_TRAIL(c) can be used because only the lower 10 bits are relevant
                fcd = unorm_getFCD16FromSurrogatePair(fcdTrieIndex, U16_LEAD(c), U16_TRAIL(c));
                if (fcd != 0) {
                    uset_add(unsafe, c);
                } 
            }
        }
    }



    return uset_size(unsafe);
}


/**
 * Get a set containing the contractions defined by the collator. The set includes
 * both the UCA contractions and the contractions defined by the collator
 * @param coll collator 
 * @param conts the set to hold the result
 * @param status to hold the error code
 * @return the size of the contraction set
 *
 * @draft ICU 3.0
 */
U_CAPI int32_t U_EXPORT2
ucol_getContractions( const UCollator *coll,
                  USet *contractions,
                  UErrorCode *status) 
{
    // add contractions from the UCA
    int32_t width = coll->UCA->image->contractionUCACombosWidth;
    int32_t size = coll->UCA->image->contractionUCACombosSize;
    UChar *conts = (UChar *)((uint8_t *)coll->UCA->image + coll->UCA->image->contractionUCACombos);
    int32_t i = 0;
    while(i < size * width) {
        if(*(conts + i + 2)) {
            uset_addString(contractions, conts+i, 3);
        } else {
            uset_addString(contractions, conts+i, 2);
        }

        i += 3;
    }
    // This is collator specific. Add contractions from a collator
    coll->mapping->getFoldingOffset = _getTrieFoldingOffset;
    utrie_enum(coll->mapping, NULL, _processContractions, contractions);

    return uset_size(contractions);

}

U_CAPI uint32_t U_EXPORT2
ucol_collatorToIdentifier(const UCollator *coll,
                          UErrorCode *status) {
    return 0;
}

U_CAPI UCollator* U_EXPORT2
ucol_openFromIdentifier(uint32_t identifier,
                        UErrorCode *status) {
    return NULL;
}

U_CAPI int32_t U_EXPORT2
ucol_identifierToShortString(uint32_t identifier,
                             char *buffer,
                             int32_t capacity,
                             UErrorCode *status) {
    return 0;
}
