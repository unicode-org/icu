/*
 *******************************************************************************
 *
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  sprpimpl.h
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#ifndef SPRPIMPL_H
#define SPRPIMPL_H

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/parseerr.h"

#if !UCONFIG_NO_IDNA

enum{
    UIDNA_NO_VALUE          = 0x0000 ,
    UIDNA_UNASSIGNED        = 0x0001 , 
    UIDNA_PROHIBITED        = 0x0002 , 
    UIDNA_MAP_NFKC          = 0x0003 , 
    UIDNA_LABEL_SEPARATOR   = 0x0004 
};
enum{
    _IDNA_LENGTH_IN_MAPPING_TABLE = 0x0003 /*11*/
};
/* indexes[] value names */
enum {
    _IDNA_INDEX_TRIE_SIZE,             /* number of bytes in normalization trie */
    _IDNA_INDEX_MAPPING_DATA_SIZE,     /* The array that contains the mapping   */
    _IDNA_INDEX_TOP=3                  /* changing this requires a new formatVersion */
};

enum {
    _IDNA_MAPPING_DATA_SIZE = 2000,
    _IDNA_MAP_TO_NOTHING = 0x7FF
};

#if defined(XP_CPLUSPLUS)
static inline 
void uprv_syntaxError(const UChar* rules, 
                 int32_t pos,
                 int32_t rulesLen,
                 UParseError* parseError)
{
    if(parseError == NULL){
        return;
    }
    if(pos == rulesLen && rulesLen >0){
        pos--;
    }
    parseError->offset = pos;
    parseError->line = 0 ; // we are not using line numbers 
    
    // for pre-context
    int32_t start = (pos <=U_PARSE_CONTEXT_LEN)? 0 : (pos - (U_PARSE_CONTEXT_LEN-1));
    int32_t stop  = pos;
    
    u_memcpy(parseError->preContext,rules+start,stop-start);
    //null terminate the buffer
    parseError->preContext[stop-start] = 0;
    
    //for post-context
    start = pos;
    if(start<rulesLen) {
        U16_FWD_1(rules, start, rulesLen);
    }

    stop  = ((pos+U_PARSE_CONTEXT_LEN)<= rulesLen )? (pos+(U_PARSE_CONTEXT_LEN)) : 
                                                            rulesLen;
    if(start < stop){
        u_memcpy(parseError->postContext,rules+start,stop-start);
        //null terminate the buffer
        parseError->postContext[stop-start]= 0;
    }
    
}
#endif

/* error codes for prototyping 
#define U_IDNA_ERROR_START                      U_ERROR_LIMIT
#define U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR ((UErrorCode)(U_IDNA_ERROR_START + 1))
#define U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR ((UErrorCode)(U_IDNA_ERROR_START + 2))
#define U_IDNA_CHECK_BIDI_ERROR                 ((UErrorCode)(U_IDNA_ERROR_START + 3))
#define U_IDNA_STD3_ASCII_RULES_ERROR           ((UErrorCode)(U_IDNA_ERROR_START + 4))
#define U_IDNA_ACE_PREFIX_ERROR                 ((UErrorCode)(U_IDNA_ERROR_START + 5))
#define U_IDNA_VERIFICATION_ERROR               ((UErrorCode)(U_IDNA_ERROR_START + 6))
#define U_IDNA_LABEL_TOO_LONG_ERROR                  ((UErrorCode)(U_IDNA_ERROR_START + 8))   
*/

#endif /* #if !UCONFIG_NO_IDNA */

#endif

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
