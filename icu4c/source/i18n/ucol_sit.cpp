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
#include "cmemory.h"
#include "cstring.h"


/*
struct FromShortString {
    UVersion UCAVersion;
    char locale[256];
    UChar variableTop[256];

};
*/


enum OptionsList {
    UCOL_SIT_LANGUAGE = 0,
    UCOL_SIT_SCRIPT,
    UCOL_SIT_REGION,
    UCOL_SIT_VARIANT,
    UCOL_SIT_KEYWORD,
    UCOL_SIT_RFC3166BIS,
    UCOL_SIT_STRENGTH,
    UCOL_SIT_CASE_LEVEL,
    UCOL_SIT_CASE_FIRST,
    UCOL_SIT_NUMERIC_COLLATION,
    UCOL_SIT_ALTERNATE_HANDLING,
    UCOL_SIT_NORMALIZATION_MODE,
    UCOL_SIT_FRENCH_COLLATION,
    UCOL_SIT_HIRAGANA_QUATERNARY,
    UCOL_SIT_VARIABLE_TOP,
    UCOL_SIT_ITEMS_COUNT
};

const int32_t locElementCount = 5;
const int32_t locElementLen = 32;

struct CollatorSpec {
    char locElements[locElementCount][locElementLen];
    char locale[512];
    UColAttributeValue options[UCOL_ATTRIBUTE_COUNT];
    uint32_t variableTopValue;
    UChar variableTopString[locElementLen];
};


U_CDECL_BEGIN
typedef const char* U_CALLCONV
ActionFunction(CollatorSpec *spec, uint32_t value1, const char* string,
               UErrorCode *status);
U_CDECL_END

struct AttributeConversion {
    char letter;
    UColAttributeValue value;
};

static AttributeConversion conversions[12] = {
    { '1', UCOL_PRIMARY },
    { '2', UCOL_SECONDARY },
    { '3', UCOL_TERTIARY },
    { '4', UCOL_QUATERNARY },
    { 'D', UCOL_DEFAULT },
    { 'I', UCOL_IDENTICAL },
    { 'L', UCOL_LOWER_FIRST },
    { 'N', UCOL_NON_IGNORABLE },
    { 'O', UCOL_ON },
    { 'S', UCOL_SHIFTED },
    { 'U', UCOL_UPPER_FIRST },
    { 'X', UCOL_OFF }
};

struct ShortStringOptions {
    char optionStart;
    ActionFunction *action;
    uint32_t attr;
};


U_CDECL_BEGIN
static const char* U_CALLCONV
_processLocaleElement(CollatorSpec *spec, uint32_t value, const char* string,
                      UErrorCode *status) {
    int32_t len = 0;
    do {
        spec->locElements[value][len++] = *string;
    } while(*(++string) != '_' && *string && len < locElementLen);
    if(len >= locElementLen) {
        *status = U_BUFFER_OVERFLOW_ERROR;
        return string;
    }
    // skip the underscore at the end
    return ++string;
}
U_CDECL_END

U_CDECL_BEGIN
static const char* U_CALLCONV
_processRFC3166Locale(CollatorSpec *spec, uint32_t value1, const char* string,
                      UErrorCode *status) {
    return string;
}
U_CDECL_END

U_CDECL_BEGIN
static const char* U_CALLCONV
_processCollatorOption(CollatorSpec *spec, uint32_t option, const char* string,
                       UErrorCode *status) {
    int32_t i = 0;
    for(i = 0; i < (int32_t)(sizeof(conversions)/sizeof(conversions[0])); i++) {
        if(*string == conversions[i].letter) {
            spec->options[option] = conversions[i].value;
            if(*(++string) != '_' && *string) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return string;
            }
            return ++string;
        }
    }
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return string;
}
U_CDECL_END


static UChar
readHexCodeUnit(const char **string, UErrorCode *status) {
    UChar result = 0;
    int32_t value = 0;
    char c;
    int32_t noDigits = 0;
    while((c = **string) != 0 && noDigits < 4) {
        if( c >= '0' && c <= '9') {
            value = c - '0';
        } else if ( c >= 'a' && c <= 'f') {
            value = c - 'a' + 10;
        } else if ( c >= 'A' && c <= 'F') {
            value = c - 'A' + 10;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        result = (result << 4) | value;
        (*string)++;
    }

}

U_CDECL_BEGIN
static const char* U_CALLCONV
_processVariableTop(CollatorSpec *spec, uint32_t value1, const char* string, UErrorCode *status) {
    // get four digits
    return string;
}
U_CDECL_END

static ShortStringOptions options[UCOL_SIT_ITEMS_COUNT] =
{
/* 00 UCOL_SIT_LANGUAGE */              { 'L', _processLocaleElement, 0 }, // language
/* 01 UCOL_SIT_SCRIPT */                { 'Z', _processLocaleElement, 1 }, // script
/* 02 UCOL_SIT_REGION */                { 'R', _processLocaleElement, 2 }, // region
/* 03 UCOL_SIT_VARIANT */               { 'V', _processLocaleElement, 3 }, // variant
/* 04 UCOL_SIT_KEYWORD */               { 'K', _processLocaleElement, 4 }, // keyword
/* 05 UCOL_SIT_RFC3166BIS */            { 'X', _processRFC3166Locale, 0 }, // rfc3166bis locale name
/* 06 UCOL_SIT_STRENGTH */              { 'S', _processCollatorOption, UCOL_STRENGTH }, // strength   1, 2, 3, 4, I, D
/* 07 UCOL_SIT_CASE_LEVEL */            { 'E', _processCollatorOption, UCOL_CASE_LEVEL }, // case level O, X, D
/* 08 UCOL_SIT_CASE_FIRST */            { 'C', _processCollatorOption, UCOL_CASE_FIRST }, // case first L, U, X, D
/* 09 UCOL_SIT_NUMERIC_COLLATION */     { 'D', _processCollatorOption, UCOL_NUMERIC_COLLATION }, // codan      O, X, D
/* 10 UCOL_SIT_ALTERNATE_HANDLING */    { 'A', _processCollatorOption, UCOL_ALTERNATE_HANDLING }, // alternate  N, S, D
/* 11 UCOL_SIT_NORMALIZATION_MODE */    { 'N', _processCollatorOption, UCOL_NORMALIZATION_MODE }, // norm       O, X, D
/* 12 UCOL_SIT_FRENCH_COLLATION */      { 'F', _processCollatorOption, UCOL_FRENCH_COLLATION }, // french     O, X, D
/* 13 UCOL_SIT_HIRAGANA_QUATERNARY] */  { 'H', _processCollatorOption, UCOL_HIRAGANA_QUATERNARY_MODE }, // hiragana   O, X, D
/* 14 UCOL_SIT_VARIABLE_TOP */          { 'T', _processCollatorOption, 0 }
};


static
const char* ucol_sit_readOption(const char *start, CollatorSpec *spec,
                            UErrorCode *status) {
  int32_t i = 0;

  for(i = 0; i < UCOL_SIT_ITEMS_COUNT; i++) {
      if(*start == options[i].optionStart) {
          return options[i].action(spec, options[i].attr, start+1, status);
      }
  }
  *status = U_ILLEGAL_ARGUMENT_ERROR;
  return start;
}

static
void ucol_sit_initCollatorSpecs(CollatorSpec *spec) {
    // reset everything
    uprv_memset(spec, 0, sizeof(CollatorSpec));
    // set collation options to default
    int32_t i = 0;
    for(i = 0; i < UCOL_ATTRIBUTE_COUNT; i++) {
        spec->options[i] = UCOL_DEFAULT;
    }
}

/**
 * Open a collator defined by a short form string.
 * The structure and the syntax of the string is defined in the "Naming collators"
 * section of the users guide:
 * http://oss.software.ibm.com/icu/userguide/Collate_Concepts.html#Naming_Collators
 * The call to this function is equivalent to a call to ucol_open, followed by a
 * series of calls to ucol_setAttribute and ucol_setVariableTop.
 * Attributes are overriden by the subsequent attributes. So, for "S2_S3", final
 * strength will be 3. 3066bis locale overrides individual locale parts.
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

    // first we want to pick stuff out of short string.
    // we'll end up with an UCA version, locale and a bunch of
    // settings

    // analyse the string in order to get everything we need.
    int32_t definitionLen = uprv_strlen(definition);
    const char *definitionEnd = definition+definitionLen;
    const char *string = definition;
    CollatorSpec s;
    ucol_sit_initCollatorSpecs(&s);
    while(U_SUCCESS(*status) && string < definitionEnd) {
        string = ucol_sit_readOption(string, &s, status);
    }

    if(U_FAILURE(*status)) {
        parseError->line = 0;
        parseError->offset = string - definition;
        // perhaps just stuff chars in UChar[]?
        parseError->preContext[0] = 0;
        parseError->postContext[0] = 0;
    }

    // put the locale together, unless we have a done
    // locale
    int32_t i = 0;
    if(s.locale[0] == 0) {
        // first the language
        uprv_strcat(s.locale, s.locElements[0]);
        // then the script, if present
        if(*(s.locElements[1])) {
            uprv_strcat(s.locale, "_");
            uprv_strcat(s.locale, s.locElements[1]);
        }
        // then the region, if present
        if(*(s.locElements[2])) {
            uprv_strcat(s.locale, "_");
            uprv_strcat(s.locale, s.locElements[2]);
        } else if(*(s.locElements[3])) { // if there is a variant, we need an underscore
            uprv_strcat(s.locale, "_");
        }
        // add variant, if there
        if(*(s.locElements[3])) {
            uprv_strcat(s.locale, "_");
            uprv_strcat(s.locale, s.locElements[3]);
        }

        // if there is a collation keyword, add that too
        if(*(s.locElements[4])) {
            uprv_strcat(s.locale, "@collation=");
            uprv_strcat(s.locale, s.locElements[4]);
        }
    }

    const UCollator* UCA = ucol_initUCA(status);

    UCollator *result = ucol_open(s.locale, status);

    for(i = 0; i < UCOL_ATTRIBUTE_COUNT; i++) {
        if(s.options[i] != UCOL_DEFAULT) {
            ucol_setAttribute(result, (UColAttribute)i, s.options[i], status);
        }
    }
    if(U_FAILURE(*status)) { // here it can only be a bogus value
        ucol_close(result);
        result = NULL;
    }

    UTRACE_EXIT_PTR_STATUS(result, *status);
    return result;
}

U_CAPI int32_t U_EXPORT2
ucol_getShortDefinitionString(const UCollator *coll,
                              const char *locale,
                              char *buffer,
                              int32_t capacity,
                              UErrorCode *status)
{
    if(U_FAILURE(*status)) return 0;
    CollatorSpec s;
    ucol_sit_initCollatorSpecs(&s);

    if(locale) {
        uprv_strcpy(s.locale, locale);
        uloc_getCountry(locale, s.locElements[0], locElementLen, status);
        uloc_getScript(locale, s.locElements[1], locElementLen, status);
        uloc_getVariant(locale, s.locElements[2], locElementLen, status);
        uloc_getKeywordValue(locale, "collation", s.locElements[3], locElementLen, status);
    }

    int32_t i = 0;
    for(i = 0; i < UCOL_ATTRIBUTE_COUNT; i++) {
        s.options[i] = ucol_getAttribute(coll, (UColAttribute)i, status);
    }
    s.variableTopValue = ucol_getVariableTop(coll, status);

    return 0;
}

U_CAPI int32_t U_EXPORT2
ucol_normalizeShortDefinitionString(const char *source,
                                    char *destination,
                                    int32_t capacity,
                                    UParseError *parseError,
                                    UErrorCode *status)
{
    return 0;
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
