/********************************************************************
 * Copyright (c) 2001, 
 * International Business Machines Corporation and others. 
 * All Rights Reserved.
 ********************************************************************
 * File usrchtst.c
 * Modification History:
 * Name           Date             Description
 * synwee         July 19 2001     creation
 ********************************************************************/

#include "unicode/usearch.h"
#include "unicode/ustring.h"
#include "ccolltst.h"
#include "cmemory.h"
#include <stdio.h>
#include "usrchdat.c"

static UBool      TOCLOSE_ = TRUE;
static UCollator *EN_US_; 
static UCollator *FR_FR_;
static UCollator *DE_;
static UCollator *ES_;
static UBreakIterator *EN_WORDBREAKER_;
static UBreakIterator *EN_CHARACTERBREAKER_;

/**
* Opening all static collators and break iterators
*/
static void open()
{
    if (TOCLOSE_) {
        UErrorCode  status = U_ZERO_ERROR;
        UChar      rules[1024];
        int        rulelength = 0;

        EN_US_ = ucol_open("en_US", &status);
        FR_FR_ = ucol_open("fr_FR", &status);
        DE_ = ucol_open("de_DE", &status);
        ES_ = ucol_open("es_ES", &status);
    
        u_strcpy(rules, ucol_getRules(DE_, &rulelength));
        u_unescape(EXTRACOLLATIONRULE, rules + rulelength, 1024 - rulelength);
    
        ucol_close(DE_);

        DE_ = ucol_openRules(rules, u_strlen(rules), UNORM_NFD, UCOL_TERTIARY,
                             (UParseError *)NULL, &status);
        u_strcpy(rules, ucol_getRules(ES_, &rulelength));
        u_unescape(EXTRACOLLATIONRULE, rules + rulelength, 1024 - rulelength);
    
        ucol_close(ES_);
        ES_ = ucol_openRules(rules, u_strlen(rules), UNORM_NFD, UCOL_TERTIARY,
                             NULL, &status); 
        EN_WORDBREAKER_     = ubrk_open(UBRK_WORD, "en_US", NULL, 0, &status);
        EN_CHARACTERBREAKER_ = ubrk_open(UBRK_CHARACTER, "en_US", NULL, 0, 
                                        &status);
        TOCLOSE_ = TRUE;
    }
}

/**
* Start opening all static collators and break iterators
*/
static void TestStart()
{
    open();
    TOCLOSE_ = FALSE;
}

/**
* Closing all static collators and break iterators
*/
static void close()
{
    if (TOCLOSE_) {
        ucol_close(EN_US_);
        ucol_close(FR_FR_);
        ucol_close(DE_);
        ucol_close(ES_);
        ubrk_close(EN_WORDBREAKER_);
        ubrk_close(EN_CHARACTERBREAKER_);
    }
    TOCLOSE_ = FALSE;
}

/**
* End closing all static collators and break iterators
*/
static void TestEnd()
{
    TOCLOSE_ = TRUE;
    close();
}

/**
* output UChar strings for printing.
*/
static char *toCharString(const UChar* unichars)
{
    static char result[1024];
    char *temp   = result;
    int   count  = 0;
    int   length = u_strlen(unichars);

    for (; count < length; count ++) {
        UChar ch = unichars[count];
        if (ch >= 0x20 && ch <= 0x7e) {
            *temp ++ = (char)ch;
        }
        else {
            char digit[5];
            int  zerosize;
            *temp = 0;
            strcat(temp, "\\u");
            temp = temp + 2;
            sprintf(digit, "%x", ch);
            zerosize = 4 - strlen(digit);
            while (zerosize != 0) {
                *temp ++ = '0';
                zerosize --;
            }
            *temp = 0;
            strcat(temp, digit);
            temp = temp + strlen(digit);
        }
    }
    *temp = 0;

    return result;
}

/**
* Getting the collator
*/
static UCollator *getCollator(const char *collator) 
{
    if (collator == NULL) {
        return EN_US_;
    }
    if (strcmp(collator, "fr") == 0) {
        return FR_FR_;
    }
    else if (strcmp(collator, "de") == 0) {
        return DE_;
    }
    else if (strcmp(collator, "es") == 0) {
        return ES_;
    }
    else {
        return EN_US_;
    }
}

/**
* Getting the breakiterator
*/
static UBreakIterator *getBreakIterator(const char *breaker) 
{
    if (breaker == NULL) {
        return NULL;
    }
    if (strcmp(breaker, "wordbreaker") == 0) {
        return EN_WORDBREAKER_;
    }
    else {
        return EN_CHARACTERBREAKER_;
    }
}

static void TestOpenClose() 
{
          UErrorCode      status    = U_ZERO_ERROR;
          UStringSearch  *result;
    const UChar           pattern[32];
    const UChar           text[128] ;
          UBreakIterator *breakiter = ubrk_open(UBRK_WORD, "en_US", 
                                                text, 6, &status);
    /* testing null arguments */
    result = usearch_open(NULL, 0, NULL, 0, NULL, NULL, &status);
    if (U_SUCCESS(status) || result != NULL) {
        log_err("Error: NULL arguments should produce an error and a NULL result\n");
    }
    status = U_ZERO_ERROR;
    result = usearch_openFromCollator(NULL, 0, NULL, 0, NULL, NULL, &status);
    if (U_SUCCESS(status) || result != NULL) {
        log_err("Error: NULL arguments should produce an error and a NULL result\n");
    }
    
    status = U_ZERO_ERROR;
    result = usearch_open(pattern, 3, NULL, 0, NULL, NULL, &status);
    if (U_SUCCESS(status) || result != NULL) {
        log_err("Error: NULL arguments should produce an error and a NULL result\n");
    }
    status = U_ZERO_ERROR;
    result = usearch_openFromCollator(pattern, 3, NULL, 0, NULL, NULL, 
                                      &status);
    if (U_SUCCESS(status) || result != NULL) {
        log_err("Error: NULL arguments should produce an error and a NULL result\n");
    }
    
    status = U_ZERO_ERROR;
    result = usearch_open(pattern, 3, text, 6, NULL, NULL, &status);
    if (U_SUCCESS(status) || result != NULL) {
        log_err("Error: NULL arguments should produce an error and a NULL result\n");
    }
    status = U_ZERO_ERROR;
    result = usearch_openFromCollator(pattern, 3, text, 6, NULL, NULL, 
                                      &status);
    if (U_SUCCESS(status) || result != NULL) {
        log_err("Error: NULL arguments should produce an error and a NULL result\n");
    }
    
    status = U_ZERO_ERROR;
    result = usearch_open(pattern, 3, text, 6, "en_US", NULL, &status);
    if (U_FAILURE(status) || result == NULL) {
        log_err("Error: NULL break iterator is valid for opening search\n");
    }
    else {
        usearch_close(result);
    }
    open();
    status = U_ZERO_ERROR;
    result = usearch_openFromCollator(pattern, 3, text, 6, EN_US_, NULL, 
                                      &status);
    if (U_FAILURE(status) || result == NULL) {
        log_err("Error: NULL break iterator is valid for opening search\n");
    }
    else {
        usearch_close(result);
    }

    status = U_ZERO_ERROR;
    result = usearch_open(pattern, 3, text, 6, "en_US", breakiter, &status);
    if (U_FAILURE(status) || result == NULL) {
        log_err("Error: Break iterator is valid for opening search\n");
    }
    else {
        usearch_close(result);
    }
    status = U_ZERO_ERROR;
    result = usearch_openFromCollator(pattern, 3, text, 6, EN_US_, breakiter, 
                                      &status);
    if (U_FAILURE(status) || result == NULL) {
        log_err("Error: Break iterator is valid for opening search\n");
    }
    else {
        usearch_close(result);
    }
    ubrk_close(breakiter);
    close();
}

static void TestInitialization() 
{
          UErrorCode      status    = U_ZERO_ERROR;
          UChar           pattern[512];
    const UChar           text[128];
          UStringSearch  *result;

    /* simple test on the pattern ce construction */
    pattern[0] = 0x41;
    pattern[1] = 0x42;
    open();
    result = usearch_openFromCollator(pattern, 2, text, 3, EN_US_, NULL, 
                                      &status);
    if (U_FAILURE(status)) {
        log_err("Error opening search %s\n", u_errorName(status));
    }
    usearch_close(result);

    /* testing if an extremely large pattern will fail the initialization */
    uprv_memset(pattern, 0x41, 512);
    result = usearch_openFromCollator(pattern, 512, text, 3, EN_US_, NULL, 
                                      &status);
    if (U_FAILURE(status)) {
        log_err("Error opening search %s\n", u_errorName(status));
    }
    usearch_close(result);
    close();
}

static UBool assertEqualWithUStringSearch(      UStringSearch *strsrch,
                                          const SearchData     search)
{
    int         count       = 0;
    UErrorCode  status      = U_ZERO_ERROR;
    UTextOffset matchindex  = search.offset[count];
    int32_t     textlength;
    UChar       matchtext[128];

    if (usearch_getMatchedStart(strsrch) != USEARCH_DONE ||
        usearch_getMatchedLength(strsrch) != 0) {
        log_err("Error with the initialization of match start and length\n");
    }
    /* start of following matches */
    while (U_SUCCESS(status) && matchindex >= 0) {
        uint32_t matchlength = search.size[count];
        usearch_next(strsrch, &status);
        if (matchindex != usearch_getMatchedStart(strsrch) || 
            matchlength != (uint32_t)usearch_getMatchedLength(strsrch)) {
            char *str = toCharString(usearch_getText(strsrch, &textlength));
            log_err("Text: %s\n", str);
            str = toCharString(usearch_getPattern(strsrch, &textlength));
            log_err("Pattern: %s\n", str);
            log_err("Error following match found at %d %d\n", 
                    usearch_getMatchedStart(strsrch), 
                    usearch_getMatchedLength(strsrch));
            return FALSE;
        }
        count ++;
        
        if (usearch_getMatchedText(strsrch, matchtext, 128, &status) !=
            (int32_t) matchlength || U_FAILURE(status) ||
            memcmp(matchtext, 
                   usearch_getText(strsrch, &textlength) + matchindex,
                   matchlength * sizeof(UChar)) != 0) {
            log_err("Error getting following matched text\n");
        }

        matchindex = search.offset[count];
    }
    usearch_next(strsrch, &status);
    if ((uint32_t)usearch_getMatchedStart(strsrch) != USEARCH_DONE ||
        usearch_getMatchedLength(strsrch) != 0) {
        char *str = toCharString(usearch_getText(strsrch, &textlength));
        log_err("Text: %s\n", str);
        str = toCharString(usearch_getPattern(strsrch, &textlength));
        log_err("Pattern: %s\n", str);
        log_err("Error following match found at %d %d\n", 
                    usearch_getMatchedStart(strsrch), 
                    usearch_getMatchedLength(strsrch));
        return FALSE;
    }
    /* start of preceding matches */
    count = count == 0 ? 0 : count - 1;
    matchindex = search.offset[count];
    while (U_SUCCESS(status) && matchindex >= 0) {
        uint32_t matchlength = search.size[count];
        usearch_previous(strsrch, &status);
        if (matchindex != usearch_getMatchedStart(strsrch) || 
            matchlength != (uint32_t)usearch_getMatchedLength(strsrch)) {
            char *str = toCharString(usearch_getText(strsrch, &textlength));
            log_err("Text: %s\n", str);
            str = toCharString(usearch_getPattern(strsrch, &textlength));
            log_err("Pattern: %s\n", str);
            log_err("Error preceding match found at %d %d\n", 
                    usearch_getMatchedStart(strsrch), 
                    usearch_getMatchedLength(strsrch));
            return FALSE;
        }
        
        if (usearch_getMatchedText(strsrch, matchtext, 128, &status) !=
            (int32_t) matchlength || U_FAILURE(status) ||
            memcmp(matchtext, 
                   usearch_getText(strsrch, &textlength) + matchindex,
                   matchlength * sizeof(UChar)) != 0) {
            log_err("Error getting preceding matched text\n");
        }

        matchindex = count > 0 ? search.offset[count - 1] : -1;
        count --;
    }
    usearch_previous(strsrch, &status);
    if ((uint32_t)usearch_getMatchedStart(strsrch) != USEARCH_DONE ||
        usearch_getMatchedLength(strsrch) != 0) {
        char *str = toCharString(usearch_getText(strsrch, &textlength));
        log_err("Text: %s\n", str);
        str = toCharString(usearch_getPattern(strsrch, &textlength));
        log_err("Pattern: %s\n", str);
        log_err("Error preceding match found at %d %d\n", 
                    usearch_getMatchedStart(strsrch), 
                    usearch_getMatchedLength(strsrch));
        return FALSE;
    }
    return TRUE;
}

static UBool assertEqual(const SearchData search)
{
    UErrorCode      status      = U_ZERO_ERROR;
    UChar           pattern[32];
    UChar           text[128];
    UCollator      *collator = getCollator(search.collator);
    UBreakIterator *breaker  = getBreakIterator(search.breaker);
    UStringSearch  *strsrch; 
    
    u_unescape(search.text, text, 128);
    u_unescape(search.pattern, pattern, 32);
    ucol_setStrength(collator, search.strength);
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, collator, 
                                       breaker, &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        return FALSE;
    }   
    
    if (!assertEqualWithUStringSearch(strsrch, search)) {
        ucol_setStrength(collator, UCOL_TERTIARY);
        usearch_close(strsrch);
        return FALSE;
    }
    ucol_setStrength(collator, UCOL_TERTIARY);
    usearch_close(strsrch);
    return TRUE;
}

static UBool assertCanonicalEqual(const SearchData search)
{
    UErrorCode      status      = U_ZERO_ERROR;
    UChar           pattern[32];
    UChar           text[128];
    UCollator      *collator = getCollator(search.collator);
    UBreakIterator *breaker  = getBreakIterator(search.breaker);
    UStringSearch  *strsrch; 
    
    u_unescape(search.text, text, 128);
    u_unescape(search.pattern, pattern, 32);
    ucol_setStrength(collator, search.strength);
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, collator, 
                                       breaker, &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON,
                         &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        return FALSE;
    }   
    
    if (!assertEqualWithUStringSearch(strsrch, search)) {
        ucol_setStrength(collator, UCOL_TERTIARY);
        usearch_close(strsrch);
        return FALSE;
    }
    ucol_setStrength(collator, UCOL_TERTIARY);
    usearch_close(strsrch);
    return TRUE;
}

static UBool assertEqualWithAttribute(const SearchData            search, 
                                            USearchAttributeValue canonical,
                                            USearchAttributeValue overlap)
{
    UErrorCode      status      = U_ZERO_ERROR;
    UChar           pattern[32];
    UChar           text[128];
    UCollator      *collator = getCollator(search.collator);
    UBreakIterator *breaker  = getBreakIterator(search.breaker);
    UStringSearch  *strsrch; 
    
    u_unescape(search.text, text, 128);
    u_unescape(search.pattern, pattern, 32);
    ucol_setStrength(collator, search.strength);
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, collator, 
                                       breaker, &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, canonical, 
                         &status);
    usearch_setAttribute(strsrch, USEARCH_OVERLAP, overlap, &status);
    
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        return FALSE;
    }   
    
    if (!assertEqualWithUStringSearch(strsrch, search)) {
            ucol_setStrength(collator, UCOL_TERTIARY);
            usearch_close(strsrch);
            return FALSE;
    }
    ucol_setStrength(collator, UCOL_TERTIARY);
    usearch_close(strsrch);
    return TRUE;
}

static void TestBasic() 
{
    int count = 0;
    open();
    while (BASIC[count].text != NULL) {
        if (!assertEqual(BASIC[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    close();
}

static void TestNormExact() 
{
    int count = 0;
    UErrorCode status = U_ZERO_ERROR;
    open();
    ucol_setAttribute(EN_US_, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
    if (U_FAILURE(status)) {
        log_err("Error setting collation normalization %s\n", 
            u_errorName(status));
    }
    while (BASIC[count].text != NULL) {
        if (!assertEqual(BASIC[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    count = 0;
    while (NORMEXACT[count].text != NULL) {
        if (!assertEqual(NORMEXACT[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    ucol_setAttribute(EN_US_, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
    count = 0;
    while (NONNORMEXACT[count].text != NULL) {
        if (!assertEqual(NONNORMEXACT[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    close();
}

static void TestStrength() 
{
    int count = 0;
    open();
    while (STRENGTH[count].text != NULL) {
        if (!assertEqual(STRENGTH[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    close();
}

static void TestBreakIterator() {
    UErrorCode      status      = U_ZERO_ERROR;
    UStringSearch  *strsrch; 
    UChar           text[128];
    UChar           pattern[32];
    int             count = 0;

    open();
    u_unescape(BREAKITERATOR[0].text, text, 128);
    u_unescape(BREAKITERATOR[0].pattern, pattern, 32);
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, NULL, 
                                       &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        goto ENDTESTBREAKITERATOR;
    }
    
    usearch_setBreakIterator(strsrch, NULL, &status);
    if (U_FAILURE(status) || usearch_getBreakIterator(strsrch) != NULL) {
        log_err("Error usearch_getBreakIterator returned wrong object");
        goto ENDTESTBREAKITERATOR;
    }

    usearch_setBreakIterator(strsrch, EN_CHARACTERBREAKER_, &status);
    if (U_FAILURE(status) || 
        usearch_getBreakIterator(strsrch) != EN_CHARACTERBREAKER_) {
        log_err("Error usearch_getBreakIterator returned wrong object");
        goto ENDTESTBREAKITERATOR;
    }
    
    usearch_setBreakIterator(strsrch, EN_WORDBREAKER_, &status);
    if (U_FAILURE(status) || 
        usearch_getBreakIterator(strsrch) != EN_WORDBREAKER_) {
        log_err("Error usearch_getBreakIterator returned wrong object");
        goto ENDTESTBREAKITERATOR;
    }

    usearch_close(strsrch);

    count = 0;
    while (count < 4) {
        const SearchData     *search   = &(BREAKITERATOR[count]);     
              UCollator      *collator = getCollator(search->collator);
              UBreakIterator *breaker  = getBreakIterator(search->breaker);
    
        u_unescape(search->text, text, 128);
        u_unescape(search->pattern, pattern, 32);
        ucol_setStrength(collator, search->strength);
        
        strsrch = usearch_openFromCollator(pattern, -1, text, -1, collator, 
                                           breaker, &status);
        if (U_FAILURE(status) || 
            usearch_getBreakIterator(strsrch) != breaker) {
            log_err("Error setting break iterator\n");
            if (strsrch != NULL) {
                usearch_close(strsrch);
            }
        }
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
            ucol_setStrength(collator, UCOL_TERTIARY);
            usearch_close(strsrch);
            goto ENDTESTBREAKITERATOR;
        }
        search   = &(BREAKITERATOR[count + 1]);
        breaker  = getBreakIterator(search->breaker);
        usearch_setBreakIterator(strsrch, breaker, &status);
        if (U_FAILURE(status) || 
            usearch_getBreakIterator(strsrch) != breaker) {
            log_err("Error setting break iterator\n");
            usearch_close(strsrch);
            goto ENDTESTBREAKITERATOR;
        }
        usearch_reset(strsrch);
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
             log_err("Error at test number %d\n", count);
             goto ENDTESTBREAKITERATOR;
        }
        usearch_close(strsrch);
        count += 2;
    }
    count = 0;
    while (BREAKITERATOR[count].text != NULL) {
         if (!assertEqual(BREAKITERATOR[count])) {
             log_err("Error at test number %d\n", count);
             goto ENDTESTBREAKITERATOR;
         }
         count ++;
    }
    
ENDTESTBREAKITERATOR:
    close();
}

static void TestVariable() 
{
    int count = 0;
    UErrorCode status = U_ZERO_ERROR;
    open();
    ucol_setAttribute(EN_US_, UCOL_ALTERNATE_HANDLING, UCOL_SHIFTED, &status);
    if (U_FAILURE(status)) {
        log_err("Error setting collation alternate attribute %s\n", 
            u_errorName(status));
    }
    while (VARIABLE[count].text != NULL) {
        log_verbose("variable %d\n", count);
        if (!assertEqual(VARIABLE[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    ucol_setAttribute(EN_US_, UCOL_ALTERNATE_HANDLING, 
                      UCOL_NON_IGNORABLE, &status);
    close();
}

static void TestOverlap()
{
    int count = 0;
    open();
    while (OVERLAP[count].text != NULL) {
        if (!assertEqualWithAttribute(OVERLAP[count], USEARCH_OFF, 
                                      USEARCH_ON)) {
            log_err("Error at overlap test number %d\n", count);
        }
        count ++;
    }    
    count = 0;
    while (NONOVERLAP[count].text != NULL) {
        if (!assertEqual(NONOVERLAP[count])) {
            log_err("Error at non overlap test number %d\n", count);
        }
        count ++;
    }

    count = 0;
    while (count < 1) {
              UChar           pattern[32];
              UChar           text[128];
        const SearchData     *search   = &(OVERLAP[count]);     
              UCollator      *collator = getCollator(search->collator);
              UStringSearch  *strsrch; 
              UErrorCode      status   = U_ZERO_ERROR;
    
        u_unescape(search->text, text, 128);
        u_unescape(search->pattern, pattern, 32);
        strsrch = usearch_openFromCollator(pattern, -1, text, -1, collator, 
                                           NULL, &status);
        usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_ON, &status);
        if (U_FAILURE(status) ||
            usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_ON) {
            log_err("Error setting overlap option\n");
        }
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
            usearch_close(strsrch);
            return;
        }
        search   = &(NONOVERLAP[count]);
        usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_OFF, &status);
        if (U_FAILURE(status) ||
            usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_OFF) {
            log_err("Error setting overlap option\n");
        }
        usearch_reset(strsrch);
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
            usearch_close(strsrch);
            log_err("Error at test number %d\n", count);
         }

        count ++;
        usearch_close(strsrch);
    }
    close();
}

static void TestCollator() 
{
    /* test collator that thinks "o" and "p" are the same thing */
          UChar          rules[32];
          UCollator     *tailored = NULL; 
          UErrorCode     status   = U_ZERO_ERROR;
          UChar          pattern[32];
          UChar          text[128];
          UStringSearch *strsrch; 
          
    open();
    u_unescape(COLLATOR[0].text, text, 128);
    u_unescape(COLLATOR[0].pattern, pattern, 32);

    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                       NULL, &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, COLLATOR[0])) {
        goto ENDTESTCOLLATOR;
    }
    
    u_unescape(TESTCOLLATORRULE, rules, 32);
    tailored = ucol_openRules(rules, -1, UNORM_NFD, COLLATOR[1].strength, 
                              NULL, &status);
    if (U_FAILURE(status)) {
        log_err("Error opening rule based collator %s\n", u_errorName(status));
    }

    usearch_setCollator(strsrch, tailored, &status);
    if (U_FAILURE(status) || usearch_getCollator(strsrch) != tailored) {
        log_err("Error setting rule based collator\n");
    }
    usearch_reset(strsrch);
    if (!assertEqualWithUStringSearch(strsrch, COLLATOR[1])) {
        goto ENDTESTCOLLATOR;
    }
        
    usearch_setCollator(strsrch, EN_US_, &status);
    usearch_reset(strsrch);
    if (U_FAILURE(status) || usearch_getCollator(strsrch) != EN_US_) {
        log_err("Error setting rule based collator\n");
    }
    if (!assertEqualWithUStringSearch(strsrch, COLLATOR[0])) {
        goto ENDTESTCOLLATOR;
    }
    
ENDTESTCOLLATOR:
    usearch_close(strsrch);
    if (tailored != NULL) {
        ucol_close(tailored);
    }
    close();
}

static void TestPattern()
{
          UStringSearch *strsrch; 
          UChar          pattern[32];
          UChar          bigpattern[512];
          UChar          text[128];
    const UChar         *temp;
          int32_t        templength;
          UErrorCode     status = U_ZERO_ERROR;

    open();
    u_unescape(PATTERN[0].text, text, 128);
    u_unescape(PATTERN[0].pattern, pattern, 32);

    ucol_setStrength(EN_US_, PATTERN[0].strength);
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                       NULL, &status);

    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        goto ENDTESTPATTERN;
    }
    temp = usearch_getPattern(strsrch, &templength);
    if (u_strcmp(pattern, temp) != 0) {
        log_err("Error setting pattern\n");
    }
    if (!assertEqualWithUStringSearch(strsrch, PATTERN[0])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(PATTERN[1].pattern, pattern, 32);
    usearch_setPattern(strsrch, pattern, -1, &status);
    temp = usearch_getPattern(strsrch, &templength);
    if (u_strcmp(pattern, temp) != 0) {
        log_err("Error setting pattern\n");
        goto ENDTESTPATTERN;
    }
    usearch_reset(strsrch);
    if (U_FAILURE(status)) {
        log_err("Error setting pattern %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, PATTERN[1])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(PATTERN[0].pattern, pattern, 32);
    usearch_setPattern(strsrch, pattern, -1, &status);
    temp = usearch_getPattern(strsrch, &templength);
    if (u_strcmp(pattern, temp) != 0) {
        log_err("Error setting pattern\n");
        goto ENDTESTPATTERN;
    }
    usearch_reset(strsrch);
    if (U_FAILURE(status)) {
        log_err("Error setting pattern %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, PATTERN[0])) {
        goto ENDTESTPATTERN;
    }
    /* enormous pattern size to see if this crashes */
    for (templength = 0; templength != 512; templength ++) {
        bigpattern[templength] = 0x61;
    }
    bigpattern[511] = 0;
    usearch_setPattern(strsrch, bigpattern, -1, &status);
    if (U_FAILURE(status)) {
        log_err("Error setting pattern with size 512, %s \n", 
            u_errorName(status));
    }
ENDTESTPATTERN:
    ucol_setStrength(EN_US_, UCOL_TERTIARY);
    if (strsrch != NULL) {
        usearch_close(strsrch);
    }
    close();
}

static void TestText() 
{
          UStringSearch *strsrch; 
          UChar          pattern[32];
          UChar          text[128];
    const UChar         *temp;
          int32_t        templength;
          UErrorCode     status = U_ZERO_ERROR;

    u_unescape(TEXT[0].text, text, 128);
    u_unescape(TEXT[0].pattern, pattern, 32);

    open();
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                       NULL, &status);

    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        goto ENDTESTPATTERN;
    }
    temp = usearch_getText(strsrch, &templength);
    if (u_strcmp(text, temp) != 0) {
        log_err("Error setting text\n");
    }
    if (!assertEqualWithUStringSearch(strsrch, TEXT[0])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(TEXT[1].text, text, 32);
    usearch_setText(strsrch, text, -1, &status);
    temp = usearch_getText(strsrch, &templength);
    if (u_strcmp(text, temp) != 0) {
        log_err("Error setting text\n");
        goto ENDTESTPATTERN;
    }
    if (U_FAILURE(status)) {
        log_err("Error setting text %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, TEXT[1])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(TEXT[0].text, text, 32);
    usearch_setText(strsrch, text, -1, &status);
    temp = usearch_getText(strsrch, &templength);
    if (u_strcmp(text, temp) != 0) {
        log_err("Error setting text\n");
        goto ENDTESTPATTERN;
    }
    if (U_FAILURE(status)) {
        log_err("Error setting pattern %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, TEXT[0])) {
        goto ENDTESTPATTERN;
    }
ENDTESTPATTERN:
    if (strsrch != NULL) {
        usearch_close(strsrch);
    }
    close();
}

static void TestCompositeBoundaries() 
{
    int count = 0;
    open();
    while (COMPOSITEBOUNDARIES[count].text != NULL) { 
        log_verbose("composite %d\n", count);
        if (!assertEqual(COMPOSITEBOUNDARIES[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    } 
    close();
}

static void TestGetSetOffset()
{
    int            index   = 0;
    UChar          pattern[32];
    UChar          text[128];
    UErrorCode     status  = U_ZERO_ERROR;
    UStringSearch *strsrch;

    open();
    strsrch = usearch_openFromCollator(pattern, 16, text, 32, EN_US_, NULL, 
                                       &status);
    /* testing out of bounds error */
    usearch_setOffset(strsrch, -1, &status);
    if (U_SUCCESS(status)) {
        log_err("Error expecting set offset error\n");
    }
    usearch_setOffset(strsrch, 128, &status);
    if (U_SUCCESS(status)) {
        log_err("Error expecting set offset error\n");
    }
    while (BASIC[index].text != NULL) {
        int         count       = 0;
        SearchData  search      = BASIC[index ++];
        UTextOffset matchindex  = search.offset[count];
        int32_t     textlength;
    
        u_unescape(search.text, text, 128);
        u_unescape(search.pattern, pattern, 32);
        status = U_ZERO_ERROR;
        usearch_setText(strsrch, text, -1, &status);
        usearch_setPattern(strsrch, pattern, -1, &status);
        while (U_SUCCESS(status) && matchindex >= 0) {
            uint32_t matchlength = search.size[count];
            usearch_next(strsrch, &status);
            if (matchindex != usearch_getMatchedStart(strsrch) || 
                matchlength != (uint32_t)usearch_getMatchedLength(strsrch)) {
                char *str = toCharString(usearch_getText(strsrch, 
                                                         &textlength));
                log_err("Text: %s\n", str);
                str = toCharString(usearch_getPattern(strsrch, &textlength));
                log_err("Pattern: %s\n", str);
                log_err("Error match found at %d %d\n", 
                        usearch_getMatchedStart(strsrch), 
                        usearch_getMatchedLength(strsrch));
                return;
            }
            matchindex = search.offset[count + 1] == -1 ? -1 : 
                         search.offset[count + 2];
            if (search.offset[count + 1] != -1) {
                usearch_setOffset(strsrch, search.offset[count + 1] + 1, 
                                  &status);
                if (usearch_getOffset(strsrch) != search.offset[count + 1] + 1) {
                    log_err("Error setting offset\n");
                    return;
                }
            }
            
            count += 2;
        }
        usearch_next(strsrch, &status);
        if ((uint32_t)usearch_getMatchedStart(strsrch) != USEARCH_DONE) {
            char *str = toCharString(usearch_getText(strsrch, &textlength));
            log_err("Text: %s\n", str);
            str = toCharString(usearch_getPattern(strsrch, &textlength));
            log_err("Pattern: %s\n", str);
            log_err("Error match found at %d %d\n", 
                        usearch_getMatchedStart(strsrch), 
                        usearch_getMatchedLength(strsrch));
            return;
        }
    }
    usearch_close(strsrch);
    close();
}

static void TestGetSetAttribute() 
{
          UErrorCode      status    = U_ZERO_ERROR;
          UChar           pattern[32];
    const UChar           text[128];
          UStringSearch  *strsrch;
          
    open();
    strsrch = usearch_openFromCollator(pattern, 16, text, 32, EN_US_, NULL, 
                                       &status);
    if (U_FAILURE(status)) {
        log_err("Error opening search %s\n", u_errorName(status));
        return;
    }

    usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_DEFAULT, &status);
    if (U_FAILURE(status) || 
        usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_OFF) {
        log_err("Error setting overlap to the default\n");
    }
    usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_ON, &status);
    if (U_FAILURE(status) || 
        usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_ON) {
        log_err("Error setting overlap true\n");
    }
    usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_OFF, &status);
    if (U_FAILURE(status) || 
        usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_OFF) {
        log_err("Error setting overlap false\n");
    }
    usearch_setAttribute(strsrch, USEARCH_OVERLAP, 
                         USEARCH_ATTRIBUTE_VALUE_COUNT, &status);
    if (U_SUCCESS(status)) {
        log_err("Error setting overlap to illegal value\n");
    }
    status = U_ZERO_ERROR;
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_DEFAULT, 
                         &status);
    if (U_FAILURE(status) || 
        usearch_getAttribute(strsrch, USEARCH_CANONICAL_MATCH) != 
                                                        USEARCH_OFF) {
        log_err("Error setting canonical match to the default\n");
    }
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (U_FAILURE(status) || 
        usearch_getAttribute(strsrch, USEARCH_CANONICAL_MATCH) != 
                                                         USEARCH_ON) {
        log_err("Error setting canonical match true\n");
    }
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_OFF, 
                         &status);
    if (U_FAILURE(status) || 
        usearch_getAttribute(strsrch, USEARCH_CANONICAL_MATCH) != 
                                                        USEARCH_OFF) {
        log_err("Error setting canonical match false\n");
    }
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, 
                         USEARCH_ATTRIBUTE_VALUE_COUNT, &status);
    if (U_SUCCESS(status)) {
        log_err("Error setting canonical match to illegal value\n");
    }
    status = U_ZERO_ERROR;
    usearch_setAttribute(strsrch, USEARCH_ATTRIBUTE_COUNT, USEARCH_DEFAULT, 
                         &status);
    if (U_SUCCESS(status)) {
        log_err("Error setting illegal attribute success\n");
    }

    usearch_close(strsrch);
    close();
}

static void TestGetMatch()
{
    int            count       = 0;
    UErrorCode     status      = U_ZERO_ERROR;
    UChar          text[128];
    UChar          pattern[32];
    SearchData     search      = MATCH[0];
    UTextOffset    matchindex  = search.offset[count];
    UStringSearch *strsrch;
    int32_t        textlength;
    UChar          matchtext[128];
    
    open();
    u_unescape(search.text, text, 128);
    u_unescape(search.pattern, pattern, 32);
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                       NULL, &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        if (strsrch != NULL) {
            usearch_close(strsrch);
        }
        return;
    }
    
    while (U_SUCCESS(status) && matchindex >= 0) {
        int32_t matchlength = search.size[count];
        usearch_next(strsrch, &status);
        if (matchindex != usearch_getMatchedStart(strsrch) || 
            matchlength != usearch_getMatchedLength(strsrch)) {
            char *str = toCharString(usearch_getText(strsrch, &textlength));
            log_err("Text: %s\n", str);
            str = toCharString(usearch_getPattern(strsrch, &textlength));
            log_err("Pattern: %s\n", str);
            log_err("Error match found at %d %d\n", 
                    usearch_getMatchedStart(strsrch), 
                    usearch_getMatchedLength(strsrch));
            return;
        }
        count ++;
        
        status = U_ZERO_ERROR;
        if (usearch_getMatchedText(NULL, matchtext, 128, &status) != 
            USEARCH_DONE || U_SUCCESS(status)){
            log_err("Error expecting errors with NULL string search\n");
        }
        status = U_ZERO_ERROR;
        if (usearch_getMatchedText(strsrch, NULL, 128, &status) != 
            (int32_t)matchlength || U_SUCCESS(status)){
            log_err("Error pre-flighting match length\n");
        }
        status = U_ZERO_ERROR;
        if (usearch_getMatchedText(strsrch, matchtext, 0, &status) != 
            (int32_t)matchlength || U_SUCCESS(status)){
            log_err("Error getting match text with buffer size 0\n");
        }
        status = U_ZERO_ERROR;
        if (usearch_getMatchedText(strsrch, matchtext, matchlength, &status) 
            != (int32_t)matchlength || matchtext[matchlength - 1] == 0 ||
            U_FAILURE(status)){
            log_err("Error getting match text with exact size\n");
        }
        status = U_ZERO_ERROR;
        if (usearch_getMatchedText(strsrch, matchtext, 128, &status) !=
            (int32_t) matchlength || U_FAILURE(status) ||
            memcmp(matchtext, 
                   usearch_getText(strsrch, &textlength) + matchindex,
                   matchlength * sizeof(UChar)) != 0 ||
            matchtext[matchlength] != 0) {
            log_err("Error getting matched text\n");
        }

        matchindex = search.offset[count];
    }
    status = U_ZERO_ERROR;
    usearch_next(strsrch, &status);
    if (usearch_getMatchedStart(strsrch)  != USEARCH_DONE || 
        usearch_getMatchedLength(strsrch) != 0) {
        log_err("Error end of match not found\n");
    }
    status = U_ZERO_ERROR;
    if (usearch_getMatchedText(strsrch, matchtext, 128, &status) != 
        USEARCH_DONE) {
        log_err("Error getting null matches\n");
    }
    usearch_close(strsrch);
    close();
}

static void TestSetMatch()
{
    int            count       = 0;
    
    open();
    while (MATCH[count].text != NULL) {
        SearchData     search = MATCH[count];
        int            size   = 0;
        int            index = 0;
        UChar          text[128];
        UChar          pattern[32];
        UStringSearch *strsrch;
        UErrorCode status = U_ZERO_ERROR;
        u_unescape(search.text, text, 128);
        u_unescape(search.pattern, pattern, 32);
        strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                           NULL, &status);
        if (U_FAILURE(status)) {
            log_err("Error opening string search %s\n", u_errorName(status));
            if (strsrch != NULL) {
                usearch_close(strsrch);
            }
            return;
        }

        size = 0;
        while (search.offset[size] != -1) {
            size ++;
        }

        if (usearch_first(strsrch, &status) != search.offset[0] ||
            U_FAILURE(status)) {
            log_err("Error getting first match\n");
        }
        if (usearch_last(strsrch, &status) != search.offset[size -1] ||
            U_FAILURE(status)) {
            log_err("Error getting last match\n");
        }
        
        while (index < size) {
            if (index + 2 < size) {
                if (usearch_following(strsrch, search.offset[index + 2] - 1,
                                      &status) != search.offset[index + 2] ||
                    U_FAILURE(status)) {
                    log_err("Error getting following match at index %d\n", 
                            search.offset[index + 2] - 1);
                }
            }
            if (index + 1 < size) {
                if (usearch_preceding(strsrch, search.offset[index + 1] + 
                                               search.size[index + 1] + 1, 
                                      &status) != search.offset[index + 1] ||
                    U_FAILURE(status)) {
                    log_err("Error getting preceeding match at index %d\n", 
                            search.offset[index + 1] + 1);
                }
            }
            index += 2;
        }
        status = U_ZERO_ERROR;
        if (usearch_following(strsrch, u_strlen(text), &status) != 
            USEARCH_DONE) {
            log_err("Error expecting out of bounds match\n");
        }
        if (usearch_preceding(strsrch, 0, &status) != USEARCH_DONE) {
            log_err("Error expecting out of bounds match\n");
        }
        count ++;
        usearch_close(strsrch);
    }
    close();
}

static void TestReset()
{
    UErrorCode     status      = U_ZERO_ERROR;
    UChar          text[128];
    UChar          pattern[32];
    UStringSearch *strsrch;
    
    open();
    strsrch = usearch_openFromCollator(pattern, 16, text, 32, 
                                                      EN_US_, NULL, &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        if (strsrch != NULL) {
            usearch_close(strsrch);
        }
        return;
    }
    usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_ON, &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    usearch_setOffset(strsrch, 10, &status);
    if (U_FAILURE(status)) {
        log_err("Error setting attributes and offsets\n");
    }
    else {
        usearch_reset(strsrch);
        if (usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_OFF ||
            usearch_getAttribute(strsrch, USEARCH_CANONICAL_MATCH) != 
                                 USEARCH_OFF ||
            usearch_getOffset(strsrch) != 0 ||
            usearch_getMatchedLength(strsrch) != 0 ||
            usearch_getMatchedStart(strsrch) != USEARCH_DONE) {
            log_err("Error resetting string search\n");
        }
    }
    usearch_close(strsrch);
    close();
}

static void TestSupplementary()
{
    int count = 0;
    open();
    while (SUPPLEMENTARY[count].text != NULL) {
        if (!assertEqual(SUPPLEMENTARY[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    close();
}

static void TestContraction() 
{
    UChar          rules[128];
    UChar          pattern[128];
    UChar          text[128];
    UCollator     *collator;
    UErrorCode     status = U_ZERO_ERROR;
    int            count = 0;
    UStringSearch *strsrch;

    u_unescape(CONTRACTIONRULE, rules, 128);
    collator = ucol_openRules(rules, u_strlen(rules), UNORM_NFD, 
                              UCOL_TERTIARY, NULL, &status); 
    if (U_FAILURE(status)) {
        log_err("Error opening collator %s\n", u_errorName(status));
    }
    strsrch = usearch_openFromCollator(pattern, 1, text, 1, collator, NULL, 
                                       &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
    }   
    
    while (CONTRACTION[count].text != NULL) {
        u_unescape(CONTRACTION[count].text, text, 128);
        u_unescape(CONTRACTION[count].pattern, pattern, 128);
        usearch_setText(strsrch, text, -1, &status);
        usearch_setPattern(strsrch, pattern, -1, &status);
        if (!assertEqualWithUStringSearch(strsrch, CONTRACTION[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    usearch_close(strsrch);
    ucol_close(collator);
}

static void TestIgnorable() 
{
    UChar          rules[128];
    UChar          pattern[128];
    UChar          text[128];
    UCollator     *collator;
    UErrorCode     status = U_ZERO_ERROR;
    UStringSearch *strsrch;
    uint32_t       count = 0;

    u_unescape(IGNORABLERULE, rules, 128);
    collator = ucol_openRules(rules, u_strlen(rules), UNORM_NFD, 
                              IGNORABLE[count].strength, NULL, &status); 
    if (U_FAILURE(status)) {
        log_err("Error opening collator %s\n", u_errorName(status));
    }
    strsrch = usearch_openFromCollator(pattern, 1, text, 1, collator, NULL, 
                                       &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
    }   
    
    while (IGNORABLE[count].text != NULL) {
        u_unescape(IGNORABLE[count].text, text, 128);
        u_unescape(IGNORABLE[count].pattern, pattern, 128);
        usearch_setText(strsrch, text, -1, &status);
        usearch_setPattern(strsrch, pattern, -1, &status);
        if (!assertEqualWithUStringSearch(strsrch, IGNORABLE[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    usearch_close(strsrch);
    ucol_close(collator);
}

static void TestCanonical()
{
    int count = 0;
    open();
    while (BASICCANONICAL[count].text != NULL) {
        if (!assertCanonicalEqual(BASICCANONICAL[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    close();
}

static void TestNormCanonical() 
{
    int count = 0;
    UErrorCode status = U_ZERO_ERROR;
    open();
    ucol_setAttribute(EN_US_, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
    count = 0;
    while (NORMCANONICAL[count].text != NULL) {
        if (!assertCanonicalEqual(NORMCANONICAL[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    ucol_setAttribute(EN_US_, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
    close();
}

static void TestStrengthCanonical() 
{
    int count = 0;
    open();
    while (STRENGTHCANONICAL[count].text != NULL) {
        if (!assertCanonicalEqual(STRENGTHCANONICAL[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    close();
}

static void TestBreakIteratorCanonical() {
    UErrorCode      status      = U_ZERO_ERROR;
    int             count = 0;

    open();
    while (count < 4) {
              UChar           pattern[32];
              UChar           text[128];
        const SearchData     *search   = &(BREAKITERATORCANONICAL[count]);     
              UCollator      *collator = getCollator(search->collator);
              UBreakIterator *breaker  = getBreakIterator(search->breaker);
              UStringSearch  *strsrch; 
    
        u_unescape(search->text, text, 128);
        u_unescape(search->pattern, pattern, 32);
        ucol_setStrength(collator, search->strength);
        
        strsrch = usearch_openFromCollator(pattern, -1, text, -1, collator, 
                                           breaker, &status);
        usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                             &status);
        if (U_FAILURE(status) || 
            usearch_getBreakIterator(strsrch) != breaker) {
            log_err("Error setting break iterator\n");
            if (strsrch != NULL) {
                usearch_close(strsrch);
            }
        }
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
            ucol_setStrength(collator, UCOL_TERTIARY);
            usearch_close(strsrch);
            goto ENDTESTBREAKITERATOR;
        }
        search   = &(BREAKITERATOR[count + 1]);
        breaker  = getBreakIterator(search->breaker);
        usearch_setBreakIterator(strsrch, breaker, &status);
        if (U_FAILURE(status) || 
            usearch_getBreakIterator(strsrch) != breaker) {
            log_err("Error setting break iterator\n");
            usearch_close(strsrch);
            goto ENDTESTBREAKITERATOR;
        }
        usearch_reset(strsrch);
        usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                             &status);
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
             log_err("Error at test number %d\n", count);
             goto ENDTESTBREAKITERATOR;
        }
        usearch_close(strsrch);
        count += 2;
    }
    count = 0;
    while (BREAKITERATORCANONICAL[count].text != NULL) {
         if (!assertEqual(BREAKITERATORCANONICAL[count])) {
             log_err("Error at test number %d\n", count);
             goto ENDTESTBREAKITERATOR;
         }
         count ++;
    }
    
ENDTESTBREAKITERATOR:
    close();
}

static void TestVariableCanonical() 
{
    int count = 0;
    UErrorCode status = U_ZERO_ERROR;
    open();
    ucol_setAttribute(EN_US_, UCOL_ALTERNATE_HANDLING, UCOL_SHIFTED, &status);
    if (U_FAILURE(status)) {
        log_err("Error setting collation alternate attribute %s\n", 
            u_errorName(status));
    }
    while (VARIABLE[count].text != NULL) {
        log_verbose("variable %d\n", count);
        if (!assertCanonicalEqual(VARIABLE[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    ucol_setAttribute(EN_US_, UCOL_ALTERNATE_HANDLING, 
                      UCOL_NON_IGNORABLE, &status);
    close();
}

static void TestOverlapCanonical()
{
    int count = 0;
    open();
    while (OVERLAPCANONICAL[count].text != NULL) {
        if (!assertEqualWithAttribute(OVERLAPCANONICAL[count], USEARCH_ON, 
                                      USEARCH_ON)) {
            log_err("Error at overlap test number %d\n", count);
        }
        count ++;
    }    
    count = 0;
    while (NONOVERLAP[count].text != NULL) {
        if (!assertCanonicalEqual(NONOVERLAPCANONICAL[count])) {
            log_err("Error at non overlap test number %d\n", count);
        }
        count ++;
    }

    count = 0;
    while (count < 1) {
              UChar           pattern[32];
              UChar           text[128];
        const SearchData     *search   = &(OVERLAPCANONICAL[count]);     
              UCollator      *collator = getCollator(search->collator);
              UStringSearch  *strsrch; 
              UErrorCode      status   = U_ZERO_ERROR;
    
        u_unescape(search->text, text, 128);
        u_unescape(search->pattern, pattern, 32);
        strsrch = usearch_openFromCollator(pattern, -1, text, -1, collator, 
                                           NULL, &status);
        usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                             &status);
        usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_ON, &status);
        if (U_FAILURE(status) ||
            usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_ON) {
            log_err("Error setting overlap option\n");
        }
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
            usearch_close(strsrch);
            return;
        }
        search   = &(NONOVERLAPCANONICAL[count]);
        usearch_setAttribute(strsrch, USEARCH_OVERLAP, USEARCH_OFF, &status);
        if (U_FAILURE(status) ||
            usearch_getAttribute(strsrch, USEARCH_OVERLAP) != USEARCH_OFF) {
            log_err("Error setting overlap option\n");
        }
        usearch_reset(strsrch);
        if (!assertEqualWithUStringSearch(strsrch, *search)) {
            usearch_close(strsrch);
            log_err("Error at test number %d\n", count);
         }

        count ++;
        usearch_close(strsrch);
    }
    close();
}

static void TestCollatorCanonical() 
{
    /* test collator that thinks "o" and "p" are the same thing */
          UChar          rules[32];
          UCollator     *tailored = NULL; 
          UErrorCode     status = U_ZERO_ERROR;
          UChar          pattern[32];
          UChar          text[128];
          UStringSearch *strsrch; 
          
    open();
    u_unescape(COLLATORCANONICAL[0].text, text, 128);
    u_unescape(COLLATORCANONICAL[0].pattern, pattern, 32);

    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                       NULL, &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, COLLATORCANONICAL[0])) {
        goto ENDTESTCOLLATOR;
    }
    
    u_unescape(TESTCOLLATORRULE, rules, 32);
    tailored = ucol_openRules(rules, -1, UNORM_NFD, 
                              COLLATORCANONICAL[1].strength, NULL, &status);
    if (U_FAILURE(status)) {
        log_err("Error opening rule based collator %s\n", u_errorName(status));
    }

    usearch_setCollator(strsrch, tailored, &status);
    if (U_FAILURE(status) || usearch_getCollator(strsrch) != tailored) {
        log_err("Error setting rule based collator\n");
    }
    usearch_reset(strsrch);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (!assertEqualWithUStringSearch(strsrch, COLLATORCANONICAL[1])) {
        goto ENDTESTCOLLATOR;
    }
        
    usearch_setCollator(strsrch, EN_US_, &status);
    usearch_reset(strsrch);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (U_FAILURE(status) || usearch_getCollator(strsrch) != EN_US_) {
        log_err("Error setting rule based collator\n");
    }
    if (!assertEqualWithUStringSearch(strsrch, COLLATORCANONICAL[0])) {
        goto ENDTESTCOLLATOR;
    }
    
ENDTESTCOLLATOR:
    usearch_close(strsrch);
    if (tailored != NULL) {
        ucol_close(tailored);
    }
    close();
}

static void TestPatternCanonical()
{
          UStringSearch *strsrch; 
          UChar          pattern[32];
          UChar          text[128];
    const UChar         *temp;
          int32_t        templength;
          UErrorCode     status = U_ZERO_ERROR;

    open();
    u_unescape(PATTERNCANONICAL[0].text, text, 128);
    u_unescape(PATTERNCANONICAL[0].pattern, pattern, 32);

    ucol_setStrength(EN_US_, PATTERNCANONICAL[0].strength);
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                       NULL, &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        goto ENDTESTPATTERN;
    }
    temp = usearch_getPattern(strsrch, &templength);
    if (u_strcmp(pattern, temp) != 0) {
        log_err("Error setting pattern\n");
    }
    if (!assertEqualWithUStringSearch(strsrch, PATTERNCANONICAL[0])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(PATTERNCANONICAL[1].pattern, pattern, 32);
    usearch_setPattern(strsrch, pattern, -1, &status);
    temp = usearch_getPattern(strsrch, &templength);
    if (u_strcmp(pattern, temp) != 0) {
        log_err("Error setting pattern\n");
        goto ENDTESTPATTERN;
    }
    usearch_reset(strsrch);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (U_FAILURE(status)) {
        log_err("Error setting pattern %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, PATTERNCANONICAL[1])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(PATTERNCANONICAL[0].pattern, pattern, 32);
    usearch_setPattern(strsrch, pattern, -1, &status);
    temp = usearch_getPattern(strsrch, &templength);
    if (u_strcmp(pattern, temp) != 0) {
        log_err("Error setting pattern\n");
        goto ENDTESTPATTERN;
    }
    usearch_reset(strsrch);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (U_FAILURE(status)) {
        log_err("Error setting pattern %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, PATTERNCANONICAL[0])) {
        goto ENDTESTPATTERN;
    }
ENDTESTPATTERN:
    ucol_setStrength(EN_US_, UCOL_TERTIARY);
    if (strsrch != NULL) {
        usearch_close(strsrch);
    }
    close();
}

static void TestTextCanonical() 
{
          UStringSearch *strsrch; 
          UChar          pattern[32];
          UChar          text[128];
    const UChar         *temp;
          int32_t        templength;
          UErrorCode     status = U_ZERO_ERROR;

    u_unescape(TEXTCANONICAL[0].text, text, 128);
    u_unescape(TEXTCANONICAL[0].pattern, pattern, 32);

    open();
    strsrch = usearch_openFromCollator(pattern, -1, text, -1, EN_US_, 
                                       NULL, &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);

    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
        goto ENDTESTPATTERN;
    }
    temp = usearch_getText(strsrch, &templength);
    if (u_strcmp(text, temp) != 0) {
        log_err("Error setting text\n");
    }
    if (!assertEqualWithUStringSearch(strsrch, TEXTCANONICAL[0])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(TEXTCANONICAL[1].text, text, 32);
    usearch_setText(strsrch, text, -1, &status);
    temp = usearch_getText(strsrch, &templength);
    if (u_strcmp(text, temp) != 0) {
        log_err("Error setting text\n");
        goto ENDTESTPATTERN;
    }
    if (U_FAILURE(status)) {
        log_err("Error setting text %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, TEXTCANONICAL[1])) {
        goto ENDTESTPATTERN;
    }

    u_unescape(TEXTCANONICAL[0].text, text, 32);
    usearch_setText(strsrch, text, -1, &status);
    temp = usearch_getText(strsrch, &templength);
    if (u_strcmp(text, temp) != 0) {
        log_err("Error setting text\n");
        goto ENDTESTPATTERN;
    }
    if (U_FAILURE(status)) {
        log_err("Error setting pattern %s\n", u_errorName(status));
    }
    if (!assertEqualWithUStringSearch(strsrch, TEXTCANONICAL[0])) {
        goto ENDTESTPATTERN;
    }
ENDTESTPATTERN:
    if (strsrch != NULL) {
        usearch_close(strsrch);
    }
    close();
}

static void TestCompositeBoundariesCanonical() 
{
    int count = 0;
    open();
    while (COMPOSITEBOUNDARIESCANONICAL[count].text != NULL) { 
        log_verbose("composite %d\n", count);
        if (!assertCanonicalEqual(COMPOSITEBOUNDARIESCANONICAL[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    } 
    close();
}

static void TestGetSetOffsetCanonical()
{
    int            index   = 0;
    UChar          pattern[32];
    UChar          text[128];
    UErrorCode     status  = U_ZERO_ERROR;
    UStringSearch *strsrch;

    open();
    strsrch = usearch_openFromCollator(pattern, 16, text, 32, EN_US_, NULL, 
                                       &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    /* testing out of bounds error */
    usearch_setOffset(strsrch, -1, &status);
    if (U_SUCCESS(status)) {
        log_err("Error expecting set offset error\n");
    }
    usearch_setOffset(strsrch, 128, &status);
    if (U_SUCCESS(status)) {
        log_err("Error expecting set offset error\n");
    }
    while (BASICCANONICAL[index].text != NULL) {
        int         count       = 0;
        SearchData  search      = BASICCANONICAL[index ++];
        UTextOffset matchindex  = search.offset[count];
        int32_t     textlength;

        if (BASICCANONICAL[index].text == NULL) {
            /* skip the last one */
            break;
        }
        
        u_unescape(search.text, text, 128);
        u_unescape(search.pattern, pattern, 32);
        status = U_ZERO_ERROR;
        usearch_setText(strsrch, text, -1, &status);
        usearch_setPattern(strsrch, pattern, -1, &status);
        while (U_SUCCESS(status) && matchindex >= 0) {
            uint32_t matchlength = search.size[count];
            usearch_next(strsrch, &status);
            if (matchindex != usearch_getMatchedStart(strsrch) || 
                matchlength != (uint32_t)usearch_getMatchedLength(strsrch)) {
                char *str = toCharString(usearch_getText(strsrch, 
                                                         &textlength));
                log_err("Text: %s\n", str);
                str = toCharString(usearch_getPattern(strsrch, &textlength));
                log_err("Pattern: %s\n", str);
                log_err("Error match found at %d %d\n", 
                        usearch_getMatchedStart(strsrch), 
                        usearch_getMatchedLength(strsrch));
                return;
            }
            matchindex = search.offset[count + 1] == -1 ? -1 : 
                         search.offset[count + 2];
            if (search.offset[count + 1] != -1) {
                usearch_setOffset(strsrch, search.offset[count + 1] + 1, 
                                  &status);
                if (usearch_getOffset(strsrch) != search.offset[count + 1] + 1) {
                    log_err("Error setting offset\n");
                    return;
                }
            }
            
            count += 2;
        }
        usearch_next(strsrch, &status);
        if ((uint32_t)usearch_getMatchedStart(strsrch) != USEARCH_DONE) {
            char *str = toCharString(usearch_getText(strsrch, &textlength));
            log_err("Text: %s\n", str);
            str = toCharString(usearch_getPattern(strsrch, &textlength));
            log_err("Pattern: %s\n", str);
            log_err("Error match found at %d %d\n", 
                        usearch_getMatchedStart(strsrch), 
                        usearch_getMatchedLength(strsrch));
            return;
        }
    }
    usearch_close(strsrch);
    close();
}

static void TestSupplementaryCanonical()
{
    int count = 0;
    open();
    while (SUPPLEMENTARYCANONICAL[count].text != NULL) {
        if (!assertCanonicalEqual(SUPPLEMENTARYCANONICAL[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    close();
}

static void TestContractionCanonical() 
{
    UChar          rules[128];
    UChar          pattern[128];
    UChar          text[128];
    UCollator     *collator;
    UErrorCode     status = U_ZERO_ERROR;
    int            count = 0;
    UStringSearch *strsrch;

    u_unescape(CONTRACTIONRULE, rules, 128);
    collator = ucol_openRules(rules, u_strlen(rules), UNORM_NFD, 
                              UCOL_TERTIARY, NULL, &status); 
    if (U_FAILURE(status)) {
        log_err("Error opening collator %s\n", u_errorName(status));
    }
    strsrch = usearch_openFromCollator(pattern, 1, text, 1, collator, NULL, 
                                       &status);
    usearch_setAttribute(strsrch, USEARCH_CANONICAL_MATCH, USEARCH_ON, 
                         &status);
    if (U_FAILURE(status)) {
        log_err("Error opening string search %s\n", u_errorName(status));
    }   
    
    while (CONTRACTIONCANONICAL[count].text != NULL) {
        u_unescape(CONTRACTIONCANONICAL[count].text, text, 128);
        u_unescape(CONTRACTIONCANONICAL[count].pattern, pattern, 128);
        usearch_setText(strsrch, text, -1, &status);
        usearch_setPattern(strsrch, pattern, -1, &status);
        if (!assertEqualWithUStringSearch(strsrch, 
                                              CONTRACTIONCANONICAL[count])) {
            log_err("Error at test number %d\n", count);
        }
        count ++;
    }
    usearch_close(strsrch);
    ucol_close(collator);
}

void addSearchTest(TestNode** root)
{
    addTest(root, &TestStart, "tscoll/usrchtst/TestStart");
    addTest(root, &TestOpenClose, "tscoll/usrchtst/TestOpenClose");
    addTest(root, &TestInitialization, "tscoll/usrchtst/TestInitialization");
    addTest(root, &TestBasic, "tscoll/usrchtst/TestBasic");
    addTest(root, &TestNormExact, "tscoll/usrchtst/TestNormExact");
    addTest(root, &TestStrength, "tscoll/usrchtst/TestStrength");
    addTest(root, &TestBreakIterator, "tscoll/usrchtst/TestBreakIterator");
    addTest(root, &TestVariable, "tscoll/usrchtst/TestVariable");
    addTest(root, &TestOverlap, "tscoll/usrchtst/TestOverlap");
    addTest(root, &TestCollator, "tscoll/usrchtst/TestCollator");
    addTest(root, &TestPattern, "tscoll/usrchtst/TestPattern");
    addTest(root, &TestText, "tscoll/usrchtst/TestText"); 
    addTest(root, &TestCompositeBoundaries, 
                                  "tscoll/usrchtst/TestCompositeBoundaries");
    addTest(root, &TestGetSetOffset, "tscoll/usrchtst/TestGetSetOffset");
    addTest(root, &TestGetSetAttribute, 
                                      "tscoll/usrchtst/TestGetSetAttribute");
    addTest(root, &TestGetMatch, "tscoll/usrchtst/TestGetMatch");
    addTest(root, &TestSetMatch, "tscoll/usrchtst/TestSetMatch");
    addTest(root, &TestReset, "tscoll/usrchtst/TestReset");
    addTest(root, &TestSupplementary, "tscoll/usrchtst/TestSupplementary");
    addTest(root, &TestContraction, "tscoll/usrchtst/TestContraction");
    addTest(root, &TestIgnorable, "tscoll/usrchtst/TestIgnorable");
    addTest(root, &TestCanonical, "tscoll/usrchtst/TestCanonical");
    addTest(root, &TestNormCanonical, "tscoll/usrchtst/TestNormCanonical");
    addTest(root, &TestStrengthCanonical, 
                                    "tscoll/usrchtst/TestStrengthCanonical");
    addTest(root, &TestBreakIteratorCanonical, 
                               "tscoll/usrchtst/TestBreakIteratorCanonical");
    addTest(root, &TestVariableCanonical, 
                                    "tscoll/usrchtst/TestVariableCanonical");
    addTest(root, &TestOverlapCanonical, 
                                     "tscoll/usrchtst/TestOverlapCanonical");
    addTest(root, &TestCollatorCanonical, 
                                    "tscoll/usrchtst/TestCollatorCanonical");
    addTest(root, &TestPatternCanonical, 
                                     "tscoll/usrchtst/TestPatternCanonical");
    addTest(root, &TestTextCanonical, "tscoll/usrchtst/TestTextCanonical"); 
    addTest(root, &TestCompositeBoundariesCanonical, 
                         "tscoll/usrchtst/TestCompositeBoundariesCanonical");
    addTest(root, &TestGetSetOffsetCanonical, 
                                "tscoll/usrchtst/TestGetSetOffsetCanonical");
    addTest(root, &TestSupplementaryCanonical, 
                               "tscoll/usrchtst/TestSupplementaryCanonical");
    addTest(root, &TestContractionCanonical, 
                                 "tscoll/usrchtst/TestContractionCanonical");
    addTest(root, &TestEnd, "tscoll/usrchtst/TestEnd");
}

